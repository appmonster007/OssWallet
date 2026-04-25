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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
 * High-fidelity, interactive bar + line chart for dual-axis visualization.
 */
@Composable
fun AeonBarLineChart(
    barData: List<BigDecimal>,
    lineData: List<BigDecimal>,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    precision: Int = 2,
    barColorPositive: Color = Color(0xFF00C853),
    barColorNegative: Color = Color(0xFFB3261E),
    lineColor: Color = Color(0xFF2979FF),
    animate: Boolean = true
) {
    if (barData.isEmpty() || lineData.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(barData, lineData) {
        if (animate) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, tween(1000))
        } else {
            animationProgress.snapTo(1f)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val padding = 60f

        val allValues = barData + lineData
        val minY = allValues.minOf { it }
        val maxY = allValues.maxOf { it }
        
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
            return padding + (index.toFloat() / (total - 1)) * (widthPx - 2 * padding)
        }

        val zeroY = valueToY(BigDecimal.ZERO)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(barData, lineData) {
                    detectTapGestures(
                        onPress = { offset ->
                            val total = barData.size
                            val step = (widthPx - 2 * padding) / (total - 1).coerceAtLeast(1)
                            val index = ((offset.x - padding + step / 2) / step).toInt().coerceIn(0, total - 1)
                            selectedIndex = index
                        },
                        onTap = {
                            selectedIndex = null
                        }
                    )
                }
        ) {
            // 1. Draw Grid
            val gridColor = Color.Gray.copy(alpha = 0.1f)
            for (i in 0..4) {
                val y = padding + (i * (heightPx - 2 * padding) / 4)
                drawLine(color = gridColor, start = Offset(padding, y), end = Offset(widthPx - padding, y), strokeWidth = 1f)
            }
            
            // Zero Line
            drawLine(color = Color.Gray.copy(alpha = 0.3f), start = Offset(padding, zeroY), end = Offset(widthPx - padding, zeroY), strokeWidth = 1.dp.toPx())

            // 2. Draw Bars
            val barWidth = (widthPx - 2 * padding) / barData.size * 0.5f
            barData.forEachIndexed { index, value ->
                val x = indexToX(index, barData.size)
                val y = valueToY(value)
                
                val animatedY = zeroY - (zeroY - y) * animationProgress.value
                val color = if (value >= BigDecimal.ZERO) barColorPositive else barColorNegative
                
                val top = if (value >= BigDecimal.ZERO) animatedY else zeroY
                val bottom = if (value >= BigDecimal.ZERO) zeroY else animatedY
                
                drawRoundRect(
                    color = color.copy(alpha = 0.8f),
                    topLeft = Offset(x - barWidth / 2, top),
                    size = Size(barWidth, (bottom - top).coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // 3. Draw Line
            val linePath = Path()
            lineData.forEachIndexed { index, value ->
                val x = indexToX(index, lineData.size)
                val y = valueToY(value)
                val animatedY = heightPx - padding - (heightPx - padding - y) * animationProgress.value
                
                if (index == 0) linePath.moveTo(x, animatedY) else linePath.lineTo(x, animatedY)
            }

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 4. Draw selection highlight
            selectedIndex?.let { index ->
                val x = indexToX(index, barData.size)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(x, padding),
                    end = Offset(x, heightPx - padding),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }

        // Tooltip
        selectedIndex?.let { index ->
            val x = indexToX(index, barData.size)
            val barVal = barData[index]
            val lineVal = lineData[index]
            
            Surface(
                modifier = Modifier
                    .offset(x = (x / density.density).dp - 70.dp, y = (padding / density.density).dp - 40.dp)
                    .width(140.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Period ${index + 1}", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Flow:", style = MaterialTheme.typography.labelSmall)
                        Text(formatCurrencyRight(barVal, currency, precision), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (barVal >= BigDecimal.ZERO) barColorPositive else barColorNegative)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Trend:", style = MaterialTheme.typography.labelSmall)
                        Text(formatCurrencyRight(lineVal, currency, precision), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = lineColor)
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AeonBarLineChartPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp)) {
            AeonBarLineChart(
                barData = listOf(BigDecimal("500"), BigDecimal("-200"), BigDecimal("800"), BigDecimal("400")),
                lineData = listOf(BigDecimal("500"), BigDecimal("300"), BigDecimal("1100"), BigDecimal("1500"))
            )
        }
    }
}
