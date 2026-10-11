package com.osscabinet.ossWallet.data.local.query

import androidx.room.InvalidationTracker
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.query.AggregatedData
import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.AggregationLimits
import com.osscabinet.ossWallet.domain.model.query.MetricConfig
import com.osscabinet.ossWallet.domain.model.query.RawAggregateResult
import com.osscabinet.ossWallet.domain.model.query.TimeGranularity
import com.osscabinet.ossWallet.domain.service.query.AggregationPlanner
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/**
 * Optimized implementation of DatabaseQueryService with hybrid rollup capabilities.
 */
class DatabaseQueryServiceImpl(
    private val transactionDao: TransactionDao,
    private val queryBuilder: TransactionQueryBuilder,
    private val planner: AggregationPlanner,
    private val cache: QueryCache,
    private val json: kotlinx.serialization.json.Json,
    invalidationTracker: InvalidationTracker
) : DatabaseQueryService {

    init {
        // Clear the aggregation cache whenever the transactions table is written to.
        // This fires on the same background thread that Room uses to notify Flows,
        // so by the time the reactive chain recomputes, the cache is already empty.
        invalidationTracker.addObserver(object : InvalidationTracker.Observer("transactions") {
            override fun onInvalidated(tables: Set<String>) {
                cache.clear()
            }
        })
    }

    override suspend fun aggregate(
        profileId: Int,
        dimensions: List<AggregationDimension>,
        metrics: List<MetricConfig>,
        filter: TransactionFilter,
        timeGranularity: TimeGranularity?,
        useCase: AggregationPlanner.UseCase,
        excludeInternalTransfers: Boolean
    ): Result<List<AggregatedData>> {
        val cacheKey =
            "agg_${profileId}_${dimensions}_${metrics}_${filter.hashCode()}_${timeGranularity}_${useCase}_$excludeInternalTransfers"

        cache.get<List<AggregatedData>>(cacheKey)?.let {
            return Result.success(it)
        }

        return try {
            val plan = planner.plan(dimensions, metrics, timeGranularity, useCase)
            val query = queryBuilder.buildAggregationQuery(
                profileId, filter, plan, excludeInternalTransfers,
                restrictToIds = resolveRegexMatches(profileId, filter)
            )

            val rawResults = transactionDao.aggregateTransactionsSync(query)
            val processed = processResults(rawResults, plan)

            cache.put(cacheKey, processed)
            Result.success(processed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAggregate(
        profileId: Int,
        dimensions: List<AggregationDimension>,
        metrics: List<MetricConfig>,
        filter: TransactionFilter,
        timeGranularity: TimeGranularity?,
        useCase: AggregationPlanner.UseCase
    ): Flow<Result<List<AggregatedData>>> {
        val plan = planner.plan(dimensions, metrics, timeGranularity, useCase)

        if (FilterSql.hasRegexCriteria(filter)) {
            // Regex can't run in SQL: re-resolve the matching ids whenever transactions change,
            // then aggregate over exactly those rows.
            return transactionDao.getFilteredTransactions(queryBuilder.buildListQuery(profileId, filter))
                .mapLatest { candidates ->
                    val ids = candidates.regexMatchIds(filter)
                    val query = queryBuilder.buildAggregationQuery(
                        profileId, filter, plan, restrictToIds = ids
                    )
                    Result.success(processResults(transactionDao.aggregateTransactionsSync(query), plan))
                }
                .catch { e -> emit(Result.failure(e)) }
        }

        val query = queryBuilder.buildAggregationQuery(profileId, filter, plan)

        return transactionDao.aggregateTransactions(query)
            .map { raw -> Result.success(processResults(raw, plan)) }
            .catch { e -> emit(Result.failure(e)) }
    }

    override fun observeFilteredTransactions(
        profileId: Int, filter: TransactionFilter, limit: Int?
    ): Flow<List<Transaction>> {
        if (FilterSql.hasRegexCriteria(filter)) {
            // The limit has to apply after the regex test, not before it.
            val query = queryBuilder.buildListQuery(profileId, filter, limit = null)
            return transactionDao.getFilteredTransactions(query).map { entities ->
                val matching = entities.filter {
                    FilterSql.matchesRegexCriteria(filter, it.note, it.payee)
                }
                (if (limit != null) matching.take(limit) else matching).map { it.toDomain() }
            }
        }
        val query = queryBuilder.buildListQuery(profileId, filter, limit)
        return transactionDao.getFilteredTransactions(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /** Ids of the rows passing the regex note/payee criteria, or null when there are none. */
    private suspend fun resolveRegexMatches(profileId: Int, filter: TransactionFilter): Set<Long>? {
        if (!FilterSql.hasRegexCriteria(filter)) return null
        return transactionDao
            .getFilteredTransactionsSync(queryBuilder.buildListQuery(profileId, filter))
            .regexMatchIds(filter)
    }

    private fun List<TransactionEntity>.regexMatchIds(filter: TransactionFilter): Set<Long> =
        filter { FilterSql.matchesRegexCriteria(filter, it.note, it.payee) }.mapTo(HashSet()) { it.id }

    override suspend fun getMinTransactionTimestamp(profileId: Int): Long? =
        transactionDao.getMinTransactionTimestamp(profileId)

    private fun processResults(
        raw: List<RawAggregateResult>, plan: AggregationPlanner.AggregationPlan
    ): List<AggregatedData> {
        val mapped = raw.map { row ->
            val dimMap = plan.sqlDimensions.take(AggregationLimits.MAX_DIMENSIONS)
                .mapIndexedNotNull { i, dim ->
                    val value = when (i) {
                        0 -> row.d1; 1 -> row.d2; 2 -> row.d3; 3 -> row.d4; else -> null
                    }
                    dim to (value ?: "unspecified")
                }.toMap()

            val metMap =
                plan.metrics.take(AggregationLimits.MAX_METRICS).mapIndexedNotNull { i, met ->
                        val rawValue = when (i) {
                            0 -> row.m1; 1 -> row.m2; 2 -> row.m3; 3 -> row.m4; 4 -> row.m5
                            5 -> row.m6; 6 -> row.m7; 7 -> row.m8; 8 -> row.m9; 9 -> row.m10
                            else -> null
                        }
                        val decimalValue = try {
                            rawValue?.let { BigDecimal(it) } ?: BigDecimal.ZERO
                        } catch (e: Exception) {
                            BigDecimal.ZERO
                        }
                        met.name to decimalValue
                    }.toMap()

            AggregatedData(dimMap, metMap, row.count)
        }

        // Handle Hybrid Rollup for TAGS (expand multi-value)
        var result = if (plan.sqlDimensions.contains(AggregationDimension.TAGS)) {
            expandAndReaggregate(mapped, AggregationDimension.TAGS)
        } else {
            mapped
        }

        // Handle TAG_COMBINATIONS (clean JSON brackets if needed)
        if (plan.sqlDimensions.contains(AggregationDimension.TAG_COMBINATIONS)) {
            result = result.map { row ->
                val rawVal = row.dimensions[AggregationDimension.TAG_COMBINATIONS] ?: ""
                val tags = if (rawVal.startsWith("[")) {
                    try {
                        json.decodeFromString<Set<String>>(rawVal)
                    } catch (e: Exception) {
                        rawVal.removePrefix("[").removeSuffix("]").split(",")
                            .map { it.trim().removeSurrounding("\"") }.filter { it.isNotEmpty() }
                    }
                } else {
                    rawVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                val cleanVal = tags.sorted().joinToString(",")

                row.copy(dimensions = row.dimensions.toMutableMap().apply {
                    put(AggregationDimension.TAG_COMBINATIONS, cleanVal)
                })
            }
        }

        return result
    }

    private fun expandAndReaggregate(
        data: List<AggregatedData>, target: AggregationDimension
    ): List<AggregatedData> {
        val expanded = mutableListOf<AggregatedData>()

        data.forEach { row ->
            val rawValue = row.dimensions[target] ?: ""
            if (rawValue.isBlank() || rawValue == "unspecified") {
                expanded.add(row)
            } else {
                // Support both legacy comma-separated and new JSON array format
                val tags = if (rawValue.startsWith("[")) {
                    try {
                        json.decodeFromString<Set<String>>(rawValue)
                    } catch (e: Exception) {
                        rawValue.removePrefix("[").removeSuffix("]").split(",")
                            .map { it.trim().removeSurrounding("\"") }.filter { it.isNotEmpty() }
                    }
                } else {
                    rawValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                tags.forEach { tag ->
                    expanded.add(
                        row.copy(
                        dimensions = row.dimensions.toMutableMap().apply { put(target, tag) }))
                }
            }
        }

        // Re-group and sum after tag expansion
        return expanded.groupBy { it.dimensions }.map { (dims, rows) ->
            val mergedMetrics = mutableMapOf<String, BigDecimal>()
            rows.forEach { row ->
                row.metrics.forEach { (name, value) ->
                    mergedMetrics[name] = (mergedMetrics[name] ?: BigDecimal.ZERO).add(value)
                }
            }
            AggregatedData(dims, mergedMetrics, rows.sumOf { it.count })
        }
    }
}
