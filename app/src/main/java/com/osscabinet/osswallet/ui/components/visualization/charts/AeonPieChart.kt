package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.labelExtraSmall
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
    animate: Boolean = true,
    onSliceClick: (AeonPieChartSlice) -> Unit = {},
    onDrillInClick: (AeonPieChartSlice) -> Unit = {},
    onNavigateToRecords: (AeonPieChartSlice) -> Unit = {}
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
    val haptic = LocalHapticFeedback.current
    val disclosurePainter = rememberVectorPainter(Icons.Default.AddCircle)

    val animationProgress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(slices, animate) {
        if (animate) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, tween(1000))
        } else {
            animationProgress.snapTo(1f)
        }
    }

    val total = slices.fold(BigDecimal.ZERO) { acc, slice -> acc + slice.value }
    if (total.signum() == 0) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data to display", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val center = Offset(widthPx / 2, heightPx / 2)
        val radius = (widthPx.coerceAtMost(heightPx) / 2) * 0.8f

        fun findSliceAt(offset: Offset): AeonPieChartSlice? {
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
                        return slice
                    }
                    currentAngle += sweepAngle
                }
            }
            return null
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Pie chart showing ${slices.joinToString { it.label }}. Drag or tap slices for details."
                }
                .pointerInput(slices) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchPoint = offset
                            selectedSlice = findSliceAt(offset)
                        },
                        onDrag = { change, _ ->
                            touchPoint = change.position
                            selectedSlice = findSliceAt(change.position)
                        },
                        onDragEnd = { 
                            selectedSlice = null
                            touchPoint = null
                        },
                        onDragCancel = { 
                            selectedSlice = null
                            touchPoint = null
                        }
                    )
                }
                .pointerInput(slices) {
                    detectTapGestures(
                        onTap = { offset ->
                            val slice = findSliceAt(offset)
                            if (slice != null) {
                                if (slice.hasSubcategories) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onSliceClick(slice)
                            }
                            selectedSlice = slice
                            touchPoint = offset
                        },
                        onLongPress = {
                            // Consumes
                        }
                    )
                }
        ) {
            var currentAngle = 0f
            val gapAngle = 2f // Gap between slices in degrees
            
            slices.forEach { slice ->
                val sliceSweep = (slice.value.divide(total, 10, RoundingMode.HALF_UP).toFloat() * 360f)
                val sweepAngle = (sliceSweep - gapAngle).coerceAtLeast(0.1f) * animationProgress.value
                val isSelected = selectedSlice == slice
                
                val explodedRadius = if (isSelected) 12.dp.toPx() else 0f
                val midAngle = (currentAngle + sliceSweep / 2) * (PI / 180).toFloat()
                val offset = Offset(
                    cos(midAngle.toDouble()).toFloat() * explodedRadius,
                    sin(midAngle.toDouble()).toFloat() * explodedRadius
                )

                // Draw the main slice
                drawArc(
                    color = slice.color,
                    startAngle = currentAngle + (gapAngle / 2),
                    sweepAngle = sweepAngle,
                    useCenter = !isDonut,
                    topLeft = Offset(center.x - radius + offset.x, center.y - radius + offset.y),
                    size = Size(radius * 2, radius * 2),
                    style = if (isDonut) {
                        Stroke(
                            width = radius * 0.35f, 
                            cap = StrokeCap.Round
                        )
                    } else {
                        androidx.compose.ui.graphics.drawscope.Fill
                    }
                )

                // Optional: Add a subtle inner shadow/glow if selected
                if (isSelected) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.2f),
                        startAngle = currentAngle + (gapAngle / 2),
                        sweepAngle = sweepAngle,
                        useCenter = !isDonut,
                        topLeft = Offset(center.x - radius + offset.x, center.y - radius + offset.y),
                        size = Size(radius * 2, radius * 2),
                        style = if (isDonut) {
                            Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        } else {
                            Stroke(width = 1.dp.toPx())
                        }
                    )
                }

                // Visual marker for subcategories
                if (slice.hasSubcategories && sliceSweep > 20f) {
                    val markerRadius = if (isDonut) radius else radius * 0.65f
                    val iconSize = (radius * 0.12f).coerceIn(12.dp.toPx(), 24.dp.toPx())
                    
                    val markerX = center.x + cos(midAngle.toDouble()).toFloat() * markerRadius + offset.x - iconSize / 2
                    val markerY = center.y + sin(midAngle.toDouble()).toFloat() * markerRadius + offset.y - iconSize / 2
                    
                    val iconTint = if (slice.color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                    
                    translate(left = markerX, top = markerY) {
                        with(disclosurePainter) {
                            draw(
                                size = Size(iconSize, iconSize),
                                colorFilter = ColorFilter.tint(iconTint)
                            )
                        }
                    }
                }
                
                currentAngle += sliceSweep
            }
        }

        // Tooltip Overlay
        selectedSlice?.let { slice ->
            touchPoint?.let { point ->
                AeonChartInteractionPopup(
                    name = slice.label,
                    value = slice.value,
                    color = slice.color,
                    percentageOfTotal = slice.percentageOfTotal,
                    hasSubcategories = slice.hasSubcategories,
                    subcategoryCount = slice.subcategoryCount,
                    touchPoint = point,
                    chartWidthPx = widthPx,
                    chartHeightPx = heightPx,
                    density = density.density,
                    currency = currency,
                    precision = precision,
                    onDrillInClick = { 
                        selectedSlice = null
                        onDrillInClick(slice) 
                    },
                    onNavigateToRecords = { 
                        selectedSlice = null
                        onNavigateToRecords(slice) 
                    }
                )
            }
        }
    }
}

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
                ),
                animate = false
            )
        }
    }
}
