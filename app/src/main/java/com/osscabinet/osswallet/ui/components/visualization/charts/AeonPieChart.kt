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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity, interactive pie/donut chart with tooltips.
 */
@Composable
fun AeonPieChart(
    slices: List<AeonPieChartSlice>,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    precision: Int = 2,
    isDonut: Boolean = true,
    animate: Boolean = true
) {
    if (slices.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    var selectedSlice by remember { mutableStateOf<AeonPieChartSlice?>(null) }
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        if (animate) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, tween(1000))
        } else {
            animationProgress.snapTo(1f)
        }
    }

    val total = slices.sumOf { it.value }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val center = Offset(widthPx / 2, heightPx / 2)
        val radius = (widthPx.coerceAtMost(heightPx) / 2) * 0.8f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slices) {
                    detectTapGestures(
                        onPress = { offset ->
                            touchPoint = offset
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                            
                            if (distance <= radius && (!isDonut || distance >= radius * 0.6f)) {
                                var angle = atan2(dy, dx) * (180 / PI).toFloat()
                                if (angle < 0) angle += 360f
                                
                                var currentAngle = 0f
                                slices.forEach { slice ->
                                    val sweepAngle = (slice.value.divide(total, 10, RoundingMode.HALF_UP).toFloat() * 360f)
                                    if (angle >= currentAngle && angle <= currentAngle + sweepAngle) {
                                        selectedSlice = slice
                                        return@detectTapGestures
                                    }
                                    currentAngle += sweepAngle
                                }
                            }
                            selectedSlice = null
                        },
                        onTap = {
                            selectedSlice = null
                            touchPoint = null
                        }
                    )
                }
        ) {
            var currentAngle = 0f
            slices.forEach { slice ->
                val sweepAngle = (slice.value.divide(total, 10, RoundingMode.HALF_UP).toFloat() * 360f) * animationProgress.value
                val isSelected = selectedSlice == slice
                
                val explodedRadius = if (isSelected) 10.dp.toPx() else 0f
                val midAngle = (currentAngle + sweepAngle / 2) * (PI / 180).toFloat()
                val offset = Offset(
                    cos(midAngle.toDouble()).toFloat() * explodedRadius,
                    sin(midAngle.toDouble()).toFloat() * explodedRadius
                )

                drawArc(
                    color = slice.color,
                    startAngle = currentAngle,
                    sweepAngle = sweepAngle,
                    useCenter = !isDonut,
                    topLeft = Offset(center.x - radius + offset.x, center.y - radius + offset.y),
                    size = Size(radius * 2, radius * 2),
                    style = if (isDonut) Stroke(width = radius * 0.4f, cap = StrokeCap.Butt) else androidx.compose.ui.graphics.drawscope.Fill
                )
                
                currentAngle += (slice.value.divide(total, 10, RoundingMode.HALF_UP).toFloat() * 360f)
            }
        }

        // Tooltip Overlay
        selectedSlice?.let { slice ->
            touchPoint?.let { point ->
                Surface(
                    modifier = Modifier
                        .offset(
                            x = (point.x / density.density).dp - 60.dp,
                            y = (point.y / density.density).dp - 80.dp
                        )
                        .width(140.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            slice.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatCurrencyRight(slice.value, currency, precision),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = slice.color
                        )
                        val percentage = slice.value.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
                        Text(
                            "$percentage%",
                            style = MaterialTheme.typography.labelExtraSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

private val androidx.compose.material3.Typography.labelExtraSmall
    @Composable
    get() = labelSmall.copy(fontSize = 10.sp)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AeonPieChartPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.size(300.dp).padding(16.dp)) {
            AeonPieChart(
                slices = listOf(
                    AeonPieChartSlice(BigDecimal("450.00"), "Food", Color(0xFFFF5722)),
                    AeonPieChartSlice(BigDecimal("300.00"), "Rent", Color(0xFF2196F3)),
                    AeonPieChartSlice(BigDecimal("150.00"), "Transport", Color(0xFF4CAF50)),
                    AeonPieChartSlice(BigDecimal("100.00"), "Misc", Color(0xFFFFC107))
                )
            )
        }
    }
}
