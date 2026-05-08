package com.osscabinet.ossWallet.data.local.query

import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.query.*
import com.osscabinet.ossWallet.domain.service.query.AggregationPlanner
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

/**
 * Optimized implementation of DatabaseQueryService with hybrid rollup capabilities.
 */
class DatabaseQueryServiceImpl(
    private val transactionDao: TransactionDao,
    private val queryBuilder: TransactionQueryBuilder,
    private val planner: AggregationPlanner,
    private val cache: QueryCache
) : DatabaseQueryService {

    override suspend fun aggregate(
        profileId: Int,
        dimensions: List<AggregationDimension>,
        metrics: List<MetricConfig>,
        filter: TransactionFilter,
        timeGranularity: TimeGranularity?,
        useCase: AggregationPlanner.UseCase
    ): Result<List<AggregatedData>> {
        val cacheKey = "agg_${profileId}_${dimensions}_${metrics}_${filter.hashCode()}_${timeGranularity}_$useCase"
        
        cache.get<List<AggregatedData>>(cacheKey)?.let {
            return Result.success(it)
        }

        return try {
            val plan = planner.plan(dimensions, metrics, timeGranularity, useCase)
            val query = queryBuilder.buildAggregationQuery(profileId, filter, plan)
            
            val rawResults = transactionDao.aggregateTransactionsSync(query)
            val processed = processResults(rawResults, plan)
            
            cache.put(cacheKey, processed)
            Result.success(processed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAggregate(
        profileId: Int,
        dimensions: List<AggregationDimension>,
        metrics: List<MetricConfig>,
        filter: TransactionFilter,
        timeGranularity: TimeGranularity?,
        useCase: AggregationPlanner.UseCase
    ): Flow<Result<List<AggregatedData>>> {
        val plan = planner.plan(dimensions, metrics, timeGranularity, useCase)
        val query = queryBuilder.buildAggregationQuery(profileId, filter, plan)

        return transactionDao.aggregateTransactions(query)
            .map { raw -> Result.success(processResults(raw, plan)) }
            .catch { e -> emit(Result.failure(e)) }
    }

    private fun processResults(
        raw: List<RawAggregateResult>,
        plan: AggregationPlanner.AggregationPlan
    ): List<AggregatedData> {
        val mapped = raw.map { row ->
            val dimMap = plan.sqlDimensions.mapIndexedNotNull { i, dim ->
                val value = when (i) {
                    0 -> row.d1; 1 -> row.d2; 2 -> row.d3; 3 -> row.d4; else -> null
                }
                dim to (value ?: "unspecified")
            }.toMap()

            val metMap = plan.metrics.mapIndexedNotNull { i, met ->
                val value = when (i) {
                    0 -> row.m1; 1 -> row.m2; 2 -> row.m3; 3 -> row.m4; else -> null
                }
                if (value != null) met.name to value else null
            }.toMap()

            AggregatedData(dimMap, metMap, row.count)
        }

        // Handle Hybrid Rollup for TAGS
        return if (plan.sqlDimensions.contains(AggregationDimension.TAGS)) {
            expandAndReaggregate(mapped, AggregationDimension.TAGS)
        } else {
            mapped
        }
    }

    private fun expandAndReaggregate(
        data: List<AggregatedData>,
        target: AggregationDimension
    ): List<AggregatedData> {
        val expanded = mutableListOf<AggregatedData>()
        
        data.forEach { row ->
            val rawTags = row.dimensions[target] ?: ""
            if (rawTags.isBlank() || rawTags == "unspecified") {
                expanded.add(row)
            } else {
                val tags = rawTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                tags.forEach { tag ->
                    expanded.add(row.copy(
                        dimensions = row.dimensions.toMutableMap().apply { put(target, tag) }
                    ))
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
