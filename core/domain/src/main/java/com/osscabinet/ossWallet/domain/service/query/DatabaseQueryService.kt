package com.osscabinet.ossWallet.domain.service.query

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.query.AggregatedData
import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.MetricConfig
import com.osscabinet.ossWallet.domain.model.query.TimeGranularity
import kotlinx.coroutines.flow.Flow

/**
 * Service for high-performance database-level transaction aggregations.
 */
interface DatabaseQueryService {

    /**
     * Executes a multi-level aggregation query.
     * Logic is split between SQL and Kotlin based on dimension complexity.
     */
    suspend fun aggregate(
        profileId: Int,
        dimensions: List<AggregationDimension>,
        metrics: List<MetricConfig>,
        filter: TransactionFilter = TransactionFilter(),
        timeGranularity: TimeGranularity? = null,
        useCase: AggregationPlanner.UseCase = AggregationPlanner.UseCase.STANDARD_REPORT,
        excludeInternalTransfers: Boolean = false
    ): Result<List<AggregatedData>>

    /**
     * Observes a multi-level aggregation query reactively.
     */
    fun observeAggregate(
        profileId: Int,
        dimensions: List<AggregationDimension>,
        metrics: List<MetricConfig>,
        filter: TransactionFilter = TransactionFilter(),
        timeGranularity: TimeGranularity? = null,
        useCase: AggregationPlanner.UseCase = AggregationPlanner.UseCase.STANDARD_REPORT
    ): Flow<Result<List<AggregatedData>>>

    /**
     * Fetches a list of filtered transactions with a limit.
     */
    fun observeFilteredTransactions(
        profileId: Int,
        filter: TransactionFilter,
        limit: Int? = null
    ): Flow<List<Transaction>>

    /** Returns the earliest transaction timestamp for the profile, or null if no transactions exist. */
    suspend fun getMinTransactionTimestamp(profileId: Int): Long?
}
