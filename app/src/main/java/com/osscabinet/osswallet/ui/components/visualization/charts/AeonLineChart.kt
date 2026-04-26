package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * High-fidelity, interactive line chart with area gradients and dashed grids.
 */
@Composable
fun AeonLineChart(
    series: List<AeonLineChartSeries>,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    precision: Int = 2,
    showGrid: Boolean = true,
    animate: Boolean = true,
    onNavigateToRecords: ((AeonLineChartPoint) -> Unit)? = null
) {
    if (series.isEmpty() || series.all { it.dataPoints.isEmpty() }) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    var selectedPoint by remember { mutableStateOf<Pair<AeonLineChartSeries, AeonLineChartPoint>?>(null) }
    val density = LocalDensity.current

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(series) {
        if (animate) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, tween(1000))
        } else {
            animationProgress.snapTo(1f)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val padding = 60f
        
        // Dynamic scaling logic
        val allPoints = series.flatMap { it.dataPoints.map { p -> p.y } }
        val confidencePoints = series.flatMap { s -> s.confidenceIntervals?.flatMap { listOf(it.first, it.second) } ?: emptyList() }
        val allValues = allPoints + confidencePoints
        
        if (allValues.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data available", style = MaterialTheme.typography.bodySmall)
            }
            return@BoxWithConstraints
        }

        val minY = allValues.minOf { it }
        val maxY = allValues.maxOf { it }
        
        // Add 10% padding to the range
        val rangeY = (maxY - minY).abs()
        val padY = if (rangeY.signum() == 0) BigDecimal.ONE else rangeY.multiply(BigDecimal("0.1"))
        val effectiveMinY = minY - padY
        val effectiveMaxY = maxY + padY
        val effectiveRangeY = effectiveMaxY - effectiveMinY

        fun valueToY(value: BigDecimal): Float {
            val normalized = (value - effectiveMinY).divide(effectiveRangeY, 10, RoundingMode.HALF_UP).toFloat()
            return h - padding - (normalized * (h - 2 * padding))
        }

        fun indexToX(index: Int, total: Int): Float {
            if (total <= 1) return w / 2
            return padding + (index.toFloat() / (total - 1)) * (w - 2 * padding)
        }

        fun findClosestPoint(offsetX: Float): Pair<AeonLineChartSeries, AeonLineChartPoint>? {
            var closest: Pair<AeonLineChartSeries, AeonLineChartPoint>? = null
            var minDistance = Float.MAX_VALUE
            series.forEach { s ->
                s.dataPoints.forEachIndexed { index, p ->
                    val dx = indexToX(index, s.dataPoints.size)
                    val distance = abs(offsetX - dx)
                    if (distance < minDistance && distance < 100f) {
                        minDistance = distance
                        closest = s to p
                    }
                }
            }
            return closest
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Line chart showing ${series.joinToString { it.name }}. " +
                            "Drag or tap to explore data points."
                }
                .pointerInput(series) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedPoint = findClosestPoint(offset.x)
                        },
                        onDrag = { change, _ ->
                            selectedPoint = findClosestPoint(change.position.x)
                        },
                        onDragEnd = { selectedPoint = null },
                        onDragCancel = { selectedPoint = null }
                    )
                }
                .pointerInput(series) {
                    detectTapGestures(
                        onTap = { offset ->
                            selectedPoint = findClosestPoint(offset.x)
                        },
                        onLongPress = {
                            // Already handled by parent if needed, but here we consume 
                            // to ensure reordering doesn't start while exploring data
                        }
                    )
                }
        ) {
            // 1. Draw Grid System
            if (showGrid) {
                val gridColor = Color.Gray.copy(alpha = 0.2f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                
                for (i in 0..4) {
                    val y = padding + (i * (h - 2 * padding) / 4)
                    drawLine(
                        color = gridColor,
                        start = Offset(padding, y),
                        end = Offset(w - padding, y),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }
            }

            // 2. Draw Data Series
            series.forEach { s ->
                // Draw Confidence Interval (Shaded Area)
                s.confidenceIntervals?.let { intervals ->
                    if (intervals.size == s.dataPoints.size) {
                        val confidencePath = Path()
                        // Top boundary
                        s.dataPoints.forEachIndexed { index, _ ->
                            val x = indexToX(index, s.dataPoints.size)
                            val y = valueToY(intervals[index].second)
                            val animY = h - padding - (h - padding - y) * animationProgress.value
                            if (index == 0) confidencePath.moveTo(x, animY) else confidencePath.lineTo(x, animY)
                        }
                        // Bottom boundary (reverse)
                        for (index in s.dataPoints.size - 1 downTo 0) {
                            val x = indexToX(index, s.dataPoints.size)
                            val y = valueToY(intervals[index].first)
                            val animY = h - padding - (h - padding - y) * animationProgress.value
                            confidencePath.lineTo(x, animY)
                        }
                        confidencePath.close()
                        drawPath(
                            path = confidencePath,
                            color = s.color.copy(alpha = 0.15f)
                        )
                    }
                }

                val linePath = Path()
                val areaPath = Path()
                
                s.dataPoints.forEachIndexed { index, point ->
                    val x = indexToX(index, s.dataPoints.size)
                    val y = valueToY(point.y)
                    val animY = h - padding - (h - padding - y) * animationProgress.value

                    if (index == 0) {
                        linePath.moveTo(x, animY)
                        areaPath.moveTo(x, h - padding)
                        areaPath.lineTo(x, animY)
                    } else {
                        linePath.lineTo(x, animY)
                    }
                    
                    if (index == s.dataPoints.size - 1) {
                        areaPath.lineTo(x, animY)
                        areaPath.lineTo(x, h - padding)
                        areaPath.close()
                    } else if (index > 0) {
                        areaPath.lineTo(x, animY)
                    }
                }

                if (s.fillGradient) {
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(s.color.copy(alpha = 0.3f), Color.Transparent),
                            startY = padding,
                            endY = h - padding
                        )
                    )
                }

                drawPath(
                    path = linePath,
                    color = s.color,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (s.isDashed) PathEffect.dashPathEffect(floatArrayOf(20f, 10f)) else null
                    )
                )

                s.dataPoints.forEachIndexed { index, point ->
                    val x = indexToX(index, s.dataPoints.size)
                    val y = valueToY(point.y)
                    val animY = h - padding - (h - padding - y) * animationProgress.value
                    
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, animY))
                    drawCircle(color = s.color, radius = 4.dp.toPx(), center = Offset(x, animY), style = Stroke(width = 2.dp.toPx()))
                }
            }

            selectedPoint?.let { (s, p) ->
                val x = indexToX(s.dataPoints.indexOf(p), s.dataPoints.size)
                drawLine(
                    color = s.color.copy(alpha = 0.5f),
                    start = Offset(x, padding),
                    end = Offset(x, h - padding),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }

        selectedPoint?.let { (s, p) ->
            val index = s.dataPoints.indexOf(p)
            val x = indexToX(index, s.dataPoints.size)
            val y = valueToY(p.y)
            
            // Boundary-aware tooltip
            val tooltipWidth = 140.dp
            val tooltipHeight = 80.dp
            val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
            val tooltipHeightPx = with(density) { tooltipHeight.toPx() }

            val xPos = (x - tooltipWidthPx / 2).coerceIn(0f, w - tooltipWidthPx)
            val yPos = (y - tooltipHeightPx - 20f).coerceIn(0f, h - tooltipHeightPx)

            Surface(
                modifier = Modifier
                    .offset(x = (xPos / density.density).dp, y = (yPos / density.density).dp)
                    .width(tooltipWidth)
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, s.color.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(p.label ?: "Point ${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrencyRight(p.y, currency, precision), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = s.color)
                    
                    if (onNavigateToRecords != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onNavigateToRecords(p) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, s.color.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp), tint = s.color)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("See Records", style = MaterialTheme.typography.labelSmall, color = s.color)
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AeonLineChartPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp)) {
            AeonLineChart(
                series = listOf(
                    AeonLineChartSeries(
                        name = "Balance",
                        color = Color(0xFF2979FF),
                        dataPoints = listOf(
                            AeonLineChartPoint(0f, BigDecimal("1200.00"), "Jan"),
                            AeonLineChartPoint(1f, BigDecimal("1500.00"), "Feb"),
                            AeonLineChartPoint(2f, BigDecimal("1100.00"), "Mar"),
                            AeonLineChartPoint(3f, BigDecimal("1800.00"), "Apr"),
                            AeonLineChartPoint(4f, BigDecimal("2100.00"), "May"),
                            AeonLineChartPoint(5f, BigDecimal("1900.00"), "Jun")
                        )
                    )
                )
            )
        }
    }
}
