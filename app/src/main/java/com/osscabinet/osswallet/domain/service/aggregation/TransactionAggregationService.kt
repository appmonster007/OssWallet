package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.query.AggregatedData
import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.MetricConfig
import com.osscabinet.ossWallet.domain.model.query.StandardMetrics
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
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
        filter: TransactionFilter = TransactionFilter()
    ): Result<Map<String, FinancialSummary>> {
        return queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.CURRENCY),
            metrics = StandardMetrics.defaultSet(),
            filter = filter
        ).map { dataList ->
            dataList.associate { data ->
                val currency = data.dimensions[AggregationDimension.CURRENCY] ?: "Unknown"
                currency to mapToFinancialSummary(data)
            }
        }
    }

    /**
     * Gets summaries grouped by account.
     */
    suspend fun getSummaryByAccount(
        queryService: DatabaseQueryService,
        profileId: Int,
        filter: TransactionFilter = TransactionFilter()
    ): Result<Map<String, FinancialSummary>> {
        return queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.ACCOUNT),
            metrics = StandardMetrics.defaultSet(),
            filter = filter
        ).map { dataList ->
            dataList.associate { data ->
                val accountId = data.dimensions[AggregationDimension.ACCOUNT] ?: "Unknown"
                accountId to mapToFinancialSummary(data)
            }
        }
    }

    private fun mapToFinancialSummary(data: AggregatedData): FinancialSummary {
        val income = data.metrics["income"] ?: BigDecimal.ZERO
        val expense = data.metrics["expense"] ?: BigDecimal.ZERO
        // Ensure expense is negative in FinancialSummary if ABS was used in MetricConfig
        // Our StandardMetrics.EXPENSE_SUM uses ABS transform.
        return FinancialSummary(
            totalAmount = income.subtract(expense), 
            income = income,
            expense = expense.negate(), 
            transactionCount = data.count
        )
    }
}
