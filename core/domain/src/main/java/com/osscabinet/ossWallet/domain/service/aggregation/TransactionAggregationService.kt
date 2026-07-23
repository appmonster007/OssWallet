package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.query.AggregatedData
import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.StandardMetrics
import com.osscabinet.ossWallet.domain.model.query.TimeGranularity
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.query.AggregationPlanner
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/**
 * Domain service that provides high-level financial aggregations using the DatabaseQueryService.
 */
object TransactionAggregationService {

    /**
     * Gets a simple financial summary (Total, Income, Expense) for a given filter.
     */
    suspend fun getSummary(
        queryService: DatabaseQueryService,
        profileId: Int,
        filter: TransactionFilter = TransactionFilter()
    ): Result<FinancialSummary> {
        val metrics = StandardMetrics.defaultSet()
        return queryService.aggregate(
            profileId = profileId,
            dimensions = emptyList(), // Grand total
            metrics = metrics,
            filter = filter
        ).map { dataList ->
            val data = dataList.firstOrNull() ?: return@map FinancialSummary.EMPTY
            mapToFinancialSummary(data)
        }
    }

    /**
     * Observes a simple financial summary reactively.
     */
    fun observeSummary(
        queryService: DatabaseQueryService,
        profileId: Int,
        filter: TransactionFilter = TransactionFilter()
    ): Flow<Result<FinancialSummary>> {
        val metrics = StandardMetrics.defaultSet()
        return queryService.observeAggregate(
            profileId = profileId,
            dimensions = emptyList(),
            metrics = metrics,
            filter = filter
        ).map { result ->
            result.map { dataList ->
                val data = dataList.firstOrNull() ?: return@map FinancialSummary.EMPTY
                mapToFinancialSummary(data)
            }
        }
    }

    /**
     * Gets summaries grouped by currency.
     */
    suspend fun getSummaryByCurrency(
        queryService: DatabaseQueryService,
        profileId: Int,
        filter: TransactionFilter = TransactionFilter(),
        excludeInternalTransfers: Boolean = false
    ): Result<Map<String, FinancialSummary>> {
        return queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.CURRENCY),
            metrics = StandardMetrics.defaultSet(),
            filter = filter,
            excludeInternalTransfers = excludeInternalTransfers
        ).map { dataList ->
            dataList.associate { data ->
                val currency = data.dimensions[AggregationDimension.CURRENCY] ?: "Unknown"
                currency to mapToFinancialSummary(data)
            }
        }
    }

    /**
     * Gets summaries grouped by account and currency.
     * Returns Map<AccountId, Map<Currency, FinancialSummary>>
     */
    suspend fun getSummaryByAccountAndCurrency(
        queryService: DatabaseQueryService,
        profileId: Int,
        filter: TransactionFilter = TransactionFilter(),
        excludeInternalTransfers: Boolean = false
    ): Result<Map<String, Map<String, FinancialSummary>>> {
        return queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.ACCOUNT, AggregationDimension.CURRENCY),
            metrics = StandardMetrics.defaultSet(),
            filter = filter,
            excludeInternalTransfers = excludeInternalTransfers
        ).map { dataList ->
            dataList.groupBy { it.dimensions[AggregationDimension.ACCOUNT] ?: "Unknown" }
                .mapValues { (_, rows) ->
                    rows.associate { data ->
                        val currency = data.dimensions[AggregationDimension.CURRENCY] ?: "Unknown"
                        currency to mapToFinancialSummary(data)
                    }
                }
        }
    }

    /**
     * Gets summaries grouped by category and currency.
     * Returns Map<CategoryId, Map<Currency, FinancialSummary>>
     */
    suspend fun getSummaryByCategoryAndCurrency(
        queryService: DatabaseQueryService,
        profileId: Int,
        filter: TransactionFilter = TransactionFilter(),
        excludeInternalTransfers: Boolean = false
    ): Result<Map<Long, Map<String, FinancialSummary>>> {
        return queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.CATEGORY, AggregationDimension.CURRENCY),
            metrics = StandardMetrics.defaultSet(),
            filter = filter,
            excludeInternalTransfers = excludeInternalTransfers
        ).map { dataList ->
            dataList.groupBy { it.dimensions[AggregationDimension.CATEGORY]?.toLongOrNull() ?: -1L }
                .mapValues { (_, rows) ->
                    rows.associate { data ->
                        val currency = data.dimensions[AggregationDimension.CURRENCY] ?: "Unknown"
                        currency to mapToFinancialSummary(data)
                    }
                }
        }
    }

    /**
     * Fetches currency summaries for a date range in a single DB call, partitioned by day.
     * Returns Map<"YYYY-MM-DD" → Map<Currency → FinancialSummary>>.
     * Use this instead of calling getSummaryByCurrency once per bucket.
     */
    suspend fun getSummaryByCurrencyByDay(
        queryService: DatabaseQueryService,
        profileId: Int,
        filter: TransactionFilter,
        excludeInternalTransfers: Boolean = false
    ): Result<Map<String, Map<String, FinancialSummary>>> {
        return queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.CURRENCY, AggregationDimension.TIME),
            metrics = StandardMetrics.defaultSet(),
            filter = filter,
            timeGranularity = TimeGranularity.DAY,
            useCase = AggregationPlanner.UseCase.DASHBOARD,
            excludeInternalTransfers = excludeInternalTransfers
        ).map { dataList ->
            dataList.groupBy { it.dimensions[AggregationDimension.TIME] ?: "" }
                .mapValues { (_, dayRows) ->
                    dayRows.associate { row ->
                        val currency = row.dimensions[AggregationDimension.CURRENCY] ?: "Unknown"
                        currency to mapToFinancialSummary(row)
                    }
                }
        }
    }

    /**
     * Fetches category+currency summaries for a date range in a single DB call, partitioned by day.
     * Returns Map<"YYYY-MM-DD" → Map<CategoryId → Map<Currency → FinancialSummary>>>.
     * Use this instead of calling getSummaryByCategoryAndCurrency once per bucket.
     */
    suspend fun getSummaryByCategoryAndCurrencyByDay(
        queryService: DatabaseQueryService,
        profileId: Int,
        filter: TransactionFilter,
        excludeInternalTransfers: Boolean = false
    ): Result<Map<String, Map<Long, Map<String, FinancialSummary>>>> {
        return queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(
                AggregationDimension.CATEGORY,
                AggregationDimension.CURRENCY,
                AggregationDimension.TIME
            ),
            metrics = StandardMetrics.defaultSet(),
            filter = filter,
            timeGranularity = TimeGranularity.DAY,
            useCase = AggregationPlanner.UseCase.DASHBOARD,
            excludeInternalTransfers = excludeInternalTransfers
        ).map { dataList ->
            dataList.groupBy { it.dimensions[AggregationDimension.TIME] ?: "" }
                .mapValues { (_, dayRows) ->
                    dayRows.groupBy {
                        it.dimensions[AggregationDimension.CATEGORY]?.toLongOrNull() ?: -1L
                    }
                        .mapValues { (_, catRows) ->
                            catRows.associate { row ->
                                val currency =
                                    row.dimensions[AggregationDimension.CURRENCY] ?: "Unknown"
                                currency to mapToFinancialSummary(row)
                            }
                        }
                }
        }
    }

    internal fun mapToFinancialSummary(data: AggregatedData): FinancialSummary {
        val income = data.metrics["income"] ?: BigDecimal.ZERO
        val expense = data.metrics["expense"] ?: BigDecimal.ZERO
        val transferIn = data.metrics["transferIn"] ?: BigDecimal.ZERO
        val transferOut = data.metrics["transferOut"] ?: BigDecimal.ZERO

        // StandardMetrics uses signed SUM in DB, so income is positive and expense is negative.
        return FinancialSummary(
            income = income,
            expense = expense,
            transferIn = transferIn,
            transferOut = transferOut,
            transactionCount = data.count
        )
    }
}
