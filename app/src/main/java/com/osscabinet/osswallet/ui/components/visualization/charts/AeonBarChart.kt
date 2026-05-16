package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * High-fidelity, interactive bar chart for single-axis visualization.
 */
@Composable
fun AeonBarChart(
    data: List<Pair<String, BigDecimal>>,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    precision: Int = 2,
    barColor: Color = ChartColorBlue,
    animate: Boolean = true
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val dims = AeonTheme.dimensions

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        if (animate) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, tween(1000))
        } else {
            animationProgress.snapTo(1f)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val padding = 60f

        val values = data.map { it.second }
        val minY = values.minOf { it }.coerceAtMost(BigDecimal.ZERO)
        val maxY = values.maxOf { it }.coerceAtLeast(BigDecimal.ONE)
        
        val range = (maxY - minY).abs()
        val paddingY = if (range.signum() == 0) BigDecimal.ONE else range.multiply(BigDecimal("0.1"))
        val effectiveMinY = minY - paddingY
        val effectiveMaxY = maxY + paddingY
        val effectiveRange = effectiveMaxY - effectiveMinY

        fun valueToY(value: BigDecimal): Float {
            val normalized = (value - effectiveMinY).divide(effectiveRange, 10, RoundingMode.HALF_UP).toFloat()
            return heightPx - padding - (normalized * (heightPx - 2 * padding))
        }

        fun indexToX(index: Int, total: Int): Float {
            if (total <= 1) return widthPx / 2
            val step = (widthPx - 2 * padding) / total
            return padding + (index * step) + step / 2
        }

        val zeroY = valueToY(BigDecimal.ZERO)

        fun findIndex(offsetX: Float): Int {
            val total = data.size
            val step = (widthPx - 2 * padding) / total
            return ((offsetX - padding) / step).toInt().coerceIn(0, total - 1)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectDragGestures(
                        onDragStart = { offset -> selectedIndex = findIndex(offset.x) },
                        onDrag = { change, _ -> selectedIndex = findIndex(change.position.x) },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(data) {
                    detectTapGestures(
                        onTap = { offset -> selectedIndex = findIndex(offset.x) }
                    )
                }
        ) {
            // Draw Grid
            val gridColor = ChartGridColor
            for (i in 0..4) {
                val y = padding + (i * (heightPx - 2 * padding) / 4)
                drawLine(color = gridColor, start = Offset(padding, y), end = Offset(widthPx - padding, y), strokeWidth = 1f)
            }
            
            // Zero Line
            drawLine(color = ChartZeroLineColor, start = Offset(padding, zeroY), end = Offset(widthPx - padding, zeroY), strokeWidth = (dims.spacingXs / 4).toPx())

            // Draw Bars
            val barWidth = (widthPx - 2 * padding) / data.size * 0.7f
            data.forEachIndexed { index, pair ->
                val x = indexToX(index, data.size)
                val y = valueToY(pair.second)
                
                val animatedY = zeroY - (zeroY - y) * animationProgress.value
                
                val top = if (pair.second >= BigDecimal.ZERO) animatedY else zeroY
                val bottom = if (pair.second >= BigDecimal.ZERO) zeroY else animatedY
                
                drawRoundRect(
                    color = barColor.copy(alpha = 0.8f),
                    topLeft = Offset(x - barWidth / 2, top),
                    size = Size(barWidth, (bottom - top).coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(dims.spacingXs.toPx(), dims.spacingXs.toPx())
                )
            }
        }

        // Tooltip
        selectedIndex?.let { index ->
            val x = indexToX(index, data.size)
            val pair = data[index]
            
            // Boundary-aware tooltip
            val tooltipWidth = 140.dp
            val tooltipHeight = 60.dp
            val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
            val tooltipHeightPx = with(density) { tooltipHeight.toPx() }

            val xPos = (x - tooltipWidthPx / 2).coerceIn(0f, widthPx - tooltipWidthPx)
            val yPos = (padding - 10f).coerceIn(0f, heightPx - tooltipHeightPx)

            Surface(
                modifier = Modifier
                    .offset(x = (xPos / density.density).dp, y = (yPos / density.density).dp)
                    .width(tooltipWidth)
                    .clip(RoundedCornerShape(dims.spacingMd)),
                color = AppColors.insetSurface,
                tonalElevation = dims.spacingSm,
                shadowElevation = dims.spacingSm,
                border = androidx.compose.foundation.BorderStroke((dims.spacingXs / 4), barColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(dims.spacingSm), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(pair.first, style = MaterialTheme.typography.labelSmall, color = AppColors.contentSecondary)
                    Text(formatCurrencyRight(pair.second, currency, precision), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = barColor)
                }
            }
        }
    }
}
