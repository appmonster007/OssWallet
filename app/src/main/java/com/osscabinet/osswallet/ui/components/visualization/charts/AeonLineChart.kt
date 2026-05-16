package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatShortNumber
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

    var selectedIndex by remember { mutableIntStateOf(-1) }
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

    val verticalGuideColor = ChartZeroLineColor
    val dims = AeonTheme.dimensions
    
    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val leftPadding = with(density) { 50.dp.toPx() } // Keep specific chart layout constant or use dims.spacing3xl + dims.spacingSm
        val bottomPadding = with(density) { dims.spacing2xl.toPx() }
        val topPadding = with(density) { dims.spacingLg.toPx() }
        val rightPadding = with(density) { dims.spacingLg.toPx() }
        
        val chartWidth = w - leftPadding - rightPadding
        val chartHeight = h - bottomPadding - topPadding
        
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
        
        // Add 15% padding to the range for better visual breathing room
        val rangeY = (maxY - minY).abs()
        val padY = if (rangeY.signum() == 0) BigDecimal.ONE else rangeY.multiply(BigDecimal("0.15"))
        val effectiveMinY = minY - padY
        val effectiveMaxY = maxY + padY
        val effectiveRangeY = (effectiveMaxY - effectiveMinY).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE

        fun valueToY(value: BigDecimal): Float {
            val normalized = (value - effectiveMinY).divide(effectiveRangeY, 10, RoundingMode.HALF_UP).toFloat()
            return topPadding + chartHeight - (normalized * chartHeight)
        }

        fun indexToX(index: Int, total: Int): Float {
            if (total <= 1) return leftPadding + chartWidth / 2
            return leftPadding + (index.toFloat() / (total - 1)) * chartWidth
        }

        fun findClosestIndex(offsetX: Float): Int {
            if (series.isEmpty()) return -1
            
            // Use the first series that has data points
            val referenceSeries = series.firstOrNull { it.dataPoints.isNotEmpty() } ?: return -1
            val totalPoints = referenceSeries.dataPoints.size
            
            var closestIndex = -1
            var minDistance = Float.MAX_VALUE
            
            for (i in 0 until totalPoints) {
                val dx = indexToX(i, totalPoints)
                val distance = abs(offsetX - dx)
                if (distance < minDistance) {
                    minDistance = distance
                    closestIndex = i
                }
            }
            
            return if (minDistance < 100f) closestIndex else -1
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
                            selectedIndex = findClosestIndex(offset.x)
                        },
                        onDrag = { change, _ ->
                            selectedIndex = findClosestIndex(change.position.x)
                        },
                        onDragEnd = { selectedIndex = -1 },
                        onDragCancel = { selectedIndex = -1 }
                    )
                }
                .pointerInput(series) {
                    detectTapGestures(
                        onTap = { offset ->
                            selectedIndex = findClosestIndex(offset.x)
                        }
                    )
                }
        ) {
            // 1. Draw Grid System and Y-Axis Labels
            if (showGrid) {
                val gridColor = ChartGridColor
                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(140, 158, 158, 158)
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                    alpha = 140
                }

                for (i in 0..4) {
                    val ratio = i.toFloat() / 4
                    val yValue = effectiveMinY + effectiveRangeY.multiply(BigDecimal(ratio.toString()))
                    val yPos = valueToY(yValue)
                    
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, yPos),
                        end = Offset(w - rightPadding, yPos),
                        strokeWidth = (dims.spacingXs / 8).toPx()
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        formatShortNumber(yValue),
                        leftPadding - dims.spacingMd.toPx(),
                        yPos + dims.spacingXs.toPx(),
                        labelPaint
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
                            val animY = topPadding + chartHeight - (topPadding + chartHeight - y) * animationProgress.value
                            if (index == 0) confidencePath.moveTo(x, animY) else confidencePath.lineTo(x, animY)
                        }
                        // Bottom boundary (reverse)
                        for (index in (s.dataPoints.size - 1) downTo 0) {
                            val x = indexToX(index, s.dataPoints.size)
                            val y = valueToY(intervals[index].first)
                            val animY = topPadding + chartHeight - (topPadding + chartHeight - y) * animationProgress.value
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
                    val animY = topPadding + chartHeight - (topPadding + chartHeight - y) * animationProgress.value

                    if (index == 0) {
                        linePath.moveTo(x, animY)
                        areaPath.moveTo(x, topPadding + chartHeight)
                        areaPath.lineTo(x, animY)
                    } else {
                        linePath.lineTo(x, animY)
                    }
                    
                    if (index == s.dataPoints.size - 1) {
                        areaPath.lineTo(x, animY)
                        areaPath.lineTo(x, topPadding + chartHeight)
                        areaPath.close()
                    } else if (index > 0) {
                        areaPath.lineTo(x, animY)
                    }
                }

                if (s.fillGradient) {
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(s.color.copy(alpha = 0.2f), ColorTransparent),
                            startY = topPadding,
                            endY = topPadding + chartHeight
                        )
                    )
                }

                drawPath(
                    path = linePath,
                    color = s.color,
                    style = Stroke(
                        width = (dims.spacingXs * 0.9f).toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (s.isDashed) PathEffect.dashPathEffect(floatArrayOf(15f, 15f)) else null
                    )
                )

                // Draw end points only if selected or for specific emphasis if needed
                // For now, removing plotted dots as requested for simplicity
            }

            if (selectedIndex != -1) {
                val x = indexToX(selectedIndex, series[0].dataPoints.size)
                drawLine(
                    color = verticalGuideColor,
                    start = Offset(x, topPadding),
                    end = Offset(x, topPadding + chartHeight),
                    strokeWidth = (dims.spacingXs / 4).toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                
                // Draw focus dots for all series at this index
                series.forEach { s ->
                    if (selectedIndex < s.dataPoints.size) {
                        val p = s.dataPoints[selectedIndex]
                        val y = valueToY(p.y)
                        drawCircle(color = ChartDotHighlight, radius = (dims.spacingXs * 1.25f).toPx(), center = Offset(x, y))
                        drawCircle(color = s.color, radius = (dims.spacingXs * 1.25f).toPx(), center = Offset(x, y), style = Stroke(width = (dims.spacingXs / 2).toPx()))
                    }
                }
            }
        }

        if (selectedIndex != -1) {
            val referenceSeries = series.firstOrNull { selectedIndex < it.dataPoints.size }
            if (referenceSeries != null) {
                val totalPoints = referenceSeries.dataPoints.size
                val x = indexToX(selectedIndex, totalPoints)
                val label = referenceSeries.dataPoints[selectedIndex].label
                
                // Boundary-aware multi-series tooltip
                val tooltipWidth = 180.dp
                val tooltipHeight = (40 + series.size * 25).dp // Dynamic height based on number of series
                val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
                val tooltipHeightPx = with(density) { tooltipHeight.toPx() }

                val xPos = (x - tooltipWidthPx / 2).coerceIn(0f, w - tooltipWidthPx)
                // Show tooltip above the middle of the chart
                val yPos = (topPadding).coerceIn(0f, h - tooltipHeightPx)

                Surface(
                    modifier = Modifier
                        .offset(x = (xPos / density.density).dp, y = (yPos / density.density).dp)
                        .width(tooltipWidth)
                        .clip(RoundedCornerShape(dims.spacingMd)),
                    color = AppColors.insetSurface.copy(alpha = 0.95f),
                    tonalElevation = dims.spacingSm,
                    shadowElevation = dims.spacingSm,
                    border = androidx.compose.foundation.BorderStroke((dims.spacingXs / 8), AppColors.border.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(dims.spacingMd)) {
                        if (label != null) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.contentPrimary,
                                modifier = Modifier.padding(bottom = dims.spacingSm)
                            )
                        }
                        
                        series.forEach { s ->
                            if (selectedIndex < s.dataPoints.size) {
                                val p = s.dataPoints[selectedIndex]
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = (dims.spacingXs / 2)),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(dims.spacingSm)
                                                .background(s.color, RoundedCornerShape(dims.spacingXs / 2))
                                        )
                                        Spacer(modifier = Modifier.width(dims.spacingSm))
                                        Text(
                                            text = s.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppColors.contentSecondary
                                        )
                                    }
                                    Text(
                                        text = formatCurrencyRight(p.y, currency, precision),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AppColors.contentPrimary
                                    )
                                }
                            }
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
        Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(AeonTheme.dimensions.screenPadding)) {
            AeonLineChart(
                series = listOf(
                    AeonLineChartSeries(
                        name = "Balance",
                        color = ChartColorBlue,
                        dataPoints = listOf(
                            AeonLineChartPoint(0f, BigDecimal("1200.00"), "Jan"),
                            AeonLineChartPoint(1f, BigDecimal("1500.00"), "Feb"),
                            AeonLineChartPoint(2f, BigDecimal("1100.00"), "Mar"),
                            AeonLineChartPoint(3f, BigDecimal("1800.00"), "Apr"),
                            AeonLineChartPoint(4f, BigDecimal("2100.00"), "May"),
                            AeonLineChartPoint(5f, BigDecimal("1900.00"), "Jun")
                        )
                    ),
                    AeonLineChartSeries(
                        name = "Expense",
                        color = TagPresetColors[0],
                        isDashed = true,
                        fillGradient = false,
                        dataPoints = listOf(
                            AeonLineChartPoint(0f, BigDecimal("500.00"), "Jan"),
                            AeonLineChartPoint(1f, BigDecimal("800.00"), "Feb"),
                            AeonLineChartPoint(2f, BigDecimal("700.00"), "Mar"),
                            AeonLineChartPoint(3f, BigDecimal("900.00"), "Apr"),
                            AeonLineChartPoint(4f, BigDecimal("1200.00"), "May"),
                            AeonLineChartPoint(5f, BigDecimal("1000.00"), "Jun")
                        )
                    )
                )
            )
        }
    }
}
