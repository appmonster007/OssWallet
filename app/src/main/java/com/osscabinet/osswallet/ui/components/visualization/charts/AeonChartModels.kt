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
    val isDashed: Boolean = false
)

/**
 * Data model for pie and donut charts.
 */
data class AeonPieChartSlice(
    val value: BigDecimal,
    val label: String,
    val color: Color,
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
    val metadata: Map<String, Any> = emptyMap()
)
