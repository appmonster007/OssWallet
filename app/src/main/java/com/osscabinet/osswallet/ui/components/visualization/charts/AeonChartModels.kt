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
