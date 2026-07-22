package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.ui.graphics.Color
import java.math.BigDecimal

/**
 * Common data point for line and area charts.
 */
data class AeonLineChartPoint(
    val x: Float,
    val y: BigDecimal,
    val label: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Configuration for a data series in a line chart.
 */
data class AeonLineChartSeries(
    val name: String,
    val dataPoints: List<AeonLineChartPoint>,
    val color: Color,
    val fillGradient: Boolean = true,
    val isDashed: Boolean = false,
    val showEndDot: Boolean = false,
    val confidenceIntervals: List<Pair<BigDecimal, BigDecimal>>? = null // Min, Max for each data point
)

/**
 * Vertical marker with a label for line charts.
 */
data class AeonChartMarker(
    /** x position in the same index space as [AeonLineChart.totalXSlots]. */
    val x: Float,
    val label: String,
    val color: Color,
    val isDashed: Boolean = true
)

/**
 * Data model for pie and donut charts.
 */
data class AeonPieChartSlice(
    val value: BigDecimal,
    val label: String,
    val color: Color,
    val metadata: Map<String, Any> = emptyMap(),
    val hasSubcategories: Boolean = false,
    val subcategoryCount: Int = 0,
    val percentageOfTotal: Float = 0f,
    val id: Any? = null
)

/**
 * Data model for UpSet plots.
 */
data class AeonUpSetSet(
    val name: String,
    val total: BigDecimal,
    val color: Color = Color.Unspecified,
    val income: BigDecimal = BigDecimal.ZERO,
    val expense: BigDecimal = BigDecimal.ZERO,
    val transfer: BigDecimal = BigDecimal.ZERO,
    val incomeByCurrency: Map<String, BigDecimal> = emptyMap(),
    val expenseByCurrency: Map<String, BigDecimal> = emptyMap(),
    val transactionCount: Int = 0
)

data class AeonUpSetIntersection(
    val setIndices: Set<Int>, // Indices into the list of AeonUpSetSet
    val value: BigDecimal,
    val income: BigDecimal = BigDecimal.ZERO,
    val expense: BigDecimal = BigDecimal.ZERO,
    val transfer: BigDecimal = BigDecimal.ZERO,
    val incomeByCurrency: Map<String, BigDecimal> = emptyMap(),
    val expenseByCurrency: Map<String, BigDecimal> = emptyMap(),
    val count: Int
)

data class AeonUpSetData(
    val sets: List<AeonUpSetSet>,
    val intersections: List<AeonUpSetIntersection>
)

/**
 * Single segment of a bar (for simple or stacked charts).
 */
data class AeonBarChartBar(
    val value: BigDecimal,
    val label: String,
    val color: Color,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * A group representing one slot on the X-axis.
 * Contains one bar (simple) or multiple bars (stacked).
 */
data class AeonBarChartGroup(
    val label: String,
    val bars: List<AeonBarChartBar>
)

/**
 * Data model for interactive chart legends.
 */
data class AeonLegendItemData(
    val color: Color,
    val label: String,
    val isEnabled: Boolean,
    val isDashed: Boolean = false,
    val alpha: Float = 1f,
    val onClick: () -> Unit = {},
    val id: String = "",
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Data model for treemap charts.
 */
data class AeonTreemapNode(
    val name: String,
    val value: BigDecimal,
    val color: Color,
    val children: List<AeonTreemapNode> = emptyList(),
    val metadata: Map<String, Any> = emptyMap(),
    val hasSubcategories: Boolean = false,
    val subcategoryCount: Int = 0,
    val percentageOfTotal: Float = 0f,
    val id: Any? = null
)
