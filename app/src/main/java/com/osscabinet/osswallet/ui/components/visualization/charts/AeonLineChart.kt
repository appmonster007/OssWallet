package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    animate: Boolean = true
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
        val allPoints = series.flatMap { it.dataPoints }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }
        
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

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(series) {
                    detectTapGestures(
                        onPress = { offset ->
                            var closest: Pair<AeonLineChartSeries, AeonLineChartPoint>? = null
                            var minDistance = Float.MAX_VALUE
                            
                            series.forEach { s ->
                                s.dataPoints.forEachIndexed { index, p ->
                                    val dx = indexToX(index, s.dataPoints.size)
                                    val distance = abs(offset.x - dx)
                                    if (distance < minDistance && distance < 50f) {
                                        minDistance = distance
                                        closest = s to p
                                    }
                                }
                            }
                            selectedPoint = closest
                        },
                        onLongPress = {
                            // Consumes long press to prevent card reordering
                        },
                        onTap = {
                            selectedPoint = null
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
            
            Surface(
                modifier = Modifier
                    .offset(x = (x / density.density).dp - 60.dp, y = (y / density.density).dp - 80.dp)
                    .width(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(p.label ?: "Point ${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrencyRight(p.y, currency, precision), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = s.color)
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
