package com.osscabinet.ossWallet.domain.model.query

import java.math.BigDecimal

/**
 * Dimensions available for transaction aggregation.
 * Cardinality helps the planner decide between SQL and Kotlin grouping.
 */
enum class AggregationDimension(
    val column: String,
    val cardinality: Cardinality
) {
    ACCOUNT("accountId", Cardinality.LOW),
    CURRENCY("currency", Cardinality.LOW),
    TRANSACTION_TYPE("transactionType", Cardinality.LOW),
    STATUS("status", Cardinality.LOW),
    PAYMENT_METHOD("paymentMethod", Cardinality.MEDIUM),
    CATEGORY("categoryId", Cardinality.HIGH),
    TIME("timestamp", Cardinality.HIGH),
    TAGS("tags", Cardinality.MEDIUM), // Special: multi-value, handled via Kotlin rollup
    TAG_COMBINATIONS("tags", Cardinality.MEDIUM); // Special: exact tag sets as dimensions

    enum class Cardinality { LOW, MEDIUM, HIGH }
}

/**
 * Granularity for time-based aggregations.
 */
enum class TimeGranularity(val strftime: String) {
    MINUTE("%Y-%m-%d %H:%M"),
    HOUR("%Y-%m-%d %H:00"),
    DAY("%Y-%m-%d"),
    WEEK("%Y-%W"),
    MONTH("%Y-%m"),
    YEAR("%Y")
}

/**
 * SQL aggregation functions.
 */
enum class AggregationFunction { SUM, AVG, MIN, MAX, COUNT }

/**
 * Defines a metric to be calculated during aggregation.
 */
data class MetricConfig(
    val name: String,
    val function: AggregationFunction,
    val column: String = "amount",
    val filter: MetricFilter = MetricFilter.NONE,
    val transform: MetricTransform = MetricTransform.NONE
) {
    enum class MetricFilter {
        INCOME_ONLY,    // transactionType = 'income'
        EXPENSE_ONLY,   // transactionType = 'expense'
        TRANSFER_ONLY,  // transactionType = 'transfer'
        POSITIVE_ONLY,  // amount > 0
        NEGATIVE_ONLY,  // amount < 0
        NONE
    }
    
    enum class MetricTransform { ABS, NEGATE, NONE }
}

/**
 * Industry standard financial metrics.
 */
object StandardMetrics {
    val INCOME_SUM = MetricConfig(
        name = "income",
        function = AggregationFunction.SUM,
        filter = MetricConfig.MetricFilter.POSITIVE_ONLY
    )
    val EXPENSE_SUM = MetricConfig(
        name = "expense",
        function = AggregationFunction.SUM,
        filter = MetricConfig.MetricFilter.NEGATIVE_ONLY,
        transform = MetricConfig.MetricTransform.ABS
    )
    val TRANSFER_SUM = MetricConfig(
        name = "transfer",
        function = AggregationFunction.SUM,
        filter = MetricConfig.MetricFilter.TRANSFER_ONLY,
        transform = MetricConfig.MetricTransform.ABS
    )
    val NET_FLOW = MetricConfig(
        name = "netFlow",
        function = AggregationFunction.SUM,
        // Net flow calculation: income - expense.
        // This is complex for a single SUM if amount is always positive.
        // We handle this via custom SQL in the builder.
    )
    val TOTAL_SUM = MetricConfig(
        name = "total",
        function = AggregationFunction.SUM
        // No filter = all types. No transform = keeps signed DB amounts.
    )
    val COUNT = MetricConfig(
        name = "count",
        function = AggregationFunction.COUNT,
        column = "*"
    )

    fun defaultSet() = listOf(INCOME_SUM, EXPENSE_SUM, TRANSFER_SUM, TOTAL_SUM, COUNT)
}

/**
 * POJO for receiving dynamic SQL results from Room.
 */
data class RawAggregateResult(
    val d1: String? = null,
    val d2: String? = null,
    val d3: String? = null,
    val d4: String? = null,
    val m1: BigDecimal = BigDecimal.ZERO,
    val m2: BigDecimal = BigDecimal.ZERO,
    val m3: BigDecimal = BigDecimal.ZERO,
    val m4: BigDecimal = BigDecimal.ZERO,
    val count: Int = 0
)

/**
 * Domain-mapped aggregation data.
 */
data class AggregatedData(
    val dimensions: Map<AggregationDimension, String>,
    val metrics: Map<String, BigDecimal>,
    val count: Int
)
