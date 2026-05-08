package com.osscabinet.ossWallet.domain.service.query

import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.AggregationFunction
import com.osscabinet.ossWallet.domain.model.query.MetricConfig
import com.osscabinet.ossWallet.domain.model.query.TimeGranularity

/**
 * Plans the aggregation strategy, balancing database efficiency with service-level flexibility.
 */
class AggregationPlanner {
    companion object {
        const val MAX_SQL_LEVELS = 4
    }

    /**
     * Presets for different consumption scenarios.
     */
    enum class UseCase(val sqlLevels: Int) {
        DASHBOARD(3),        // Efficient multi-dim aggregation for cards
        STANDARD_REPORT(2),  // Standard grouping (e.g., Category + Currency)
        AD_HOC(1),           // Flat aggregation with maximum Kotlin flexibility
        CUSTOM(2)
    }

    data class AggregationPlan(
        val sqlDimensions: List<AggregationDimension>,
        val kotlinDimensions: List<AggregationDimension>,
        val metrics: List<MetricConfig>,
        val timeGranularity: TimeGranularity? = null
    )

    /**
     * Creates an execution plan for a set of dimensions and metrics.
     */
    fun plan(
        dimensions: List<AggregationDimension>,
        metrics: List<MetricConfig>,
        timeGranularity: TimeGranularity? = null,
        useCase: UseCase = UseCase.STANDARD_REPORT
    ): AggregationPlan {
        if (metrics.isEmpty()) throw IllegalArgumentException("At least one metric must be provided")

        // 1. Mandatory Currency-First Grouping for Monetary Metrics
        val needsCurrency = metrics.any { 
            it.function != AggregationFunction.COUNT && it.column == "amount" 
        }

        val enrichedDimensions = if (needsCurrency && !dimensions.contains(AggregationDimension.CURRENCY)) {
            // Determine optimal insertion point for Currency based on target depth
            // We aim to keep Currency at a level that fits within the SQL group.
            val insertionIndex = (useCase.sqlLevels - 1).coerceAtMost(dimensions.size).coerceAtLeast(0)
            dimensions.toMutableList().apply { add(insertionIndex, AggregationDimension.CURRENCY) }
        } else {
            dimensions
        }

        // 2. Split between SQL and Kotlin
        // Dimensions beyond MAX_SQL_LEVELS or specifically handled ones move to Kotlin
        val sqlTargetDepth = useCase.sqlLevels.coerceAtMost(MAX_SQL_LEVELS)
        val actualSqlLevels = sqlTargetDepth.coerceAtMost(enrichedDimensions.size)

        val sqlDims = enrichedDimensions.take(actualSqlLevels)
        val kotlinDims = enrichedDimensions.drop(actualSqlLevels)

        return AggregationPlan(
            sqlDimensions = sqlDims,
            kotlinDimensions = kotlinDims,
            metrics = metrics,
            timeGranularity = timeGranularity
        )
    }
}
