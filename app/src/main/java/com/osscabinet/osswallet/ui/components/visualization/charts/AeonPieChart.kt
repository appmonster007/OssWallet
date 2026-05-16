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
import com.osscabinet.ossWallet.ui.theme.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
    rounded: Boolean = true,
    onSliceClick: (AeonPieChartSlice) -> Unit = {},
    onDrillInClick: (AeonPieChartSlice) -> Unit = {},
    onNavigateToRecords: (AeonPieChartSlice) -> Unit = {}
) {
    if (slices.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = AppTextStyles.caption)
        }
        return
    }

    var selectedSlice by remember { mutableStateOf<AeonPieChartSlice?>(null) }
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

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
            Text("No data to display", style = AppTextStyles.caption)
        }
        return
    }

    val dims = AeonTheme.dimensions

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

        Box(modifier = Modifier.fillMaxSize()) {
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
                                // Keep selection for tooltip
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
                
                // 1. Draw Ambient Shadow Ring (Background depth)
                if (isDonut) {
                    drawCircle(
                        color = ChartPieInnerShadow,
                        radius = radius + dims.spacingXs.toPx(),
                        center = center,
                        style = Stroke(width = radius * 0.45f)
                    )
                }

                slices.forEach { slice ->
                    val sliceSweep = (slice.value.divide(total, 10, RoundingMode.HALF_UP).toFloat() * 360f)
                    val isSelected = selectedSlice == slice
                    
                    // Modern Look: Precise radial gaps
                    val gapAngle = if (rounded) (if (sliceSweep < 1f) 0.1f else 1.5f) else 0f
                    val sweepAngle = (sliceSweep - gapAngle).coerceAtLeast(0.1f) * animationProgress.value
                    
                    val explodedRadius = if (isSelected) dims.spacingMd.toPx() else 0f
                    val midAngle = (currentAngle + sliceSweep / 2) * (PI / 180).toFloat()
                    val offset = Offset(
                        cos(midAngle.toDouble()).toFloat() * explodedRadius,
                        sin(midAngle.toDouble()).toFloat() * explodedRadius
                    )

                    // 2. Draw Main Segment
                    drawArc(
                        color = slice.color,
                        startAngle = currentAngle + (gapAngle / 2),
                        sweepAngle = sweepAngle,
                        useCenter = !isDonut,
                        topLeft = Offset(center.x - radius + offset.x, center.y - radius + offset.y),
                        size = Size(radius * 2, radius * 2),
                        style = if (isDonut) {
                            Stroke(width = radius * 0.38f, cap = StrokeCap.Butt)
                        } else {
                            androidx.compose.ui.graphics.drawscope.Fill
                        }
                    )

                    // 3. High-Fidelity Layer: Inner Bevel/Glass Highlight
                    if (isDonut && sliceSweep > 5f) {
                        val highlightWidth = (dims.spacingXs * 0.75f).toPx()
                        drawArc(
                            color = ChartRimHighlight,
                            startAngle = currentAngle + (gapAngle / 2),
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - radius + offset.x + highlightWidth, center.y - radius + offset.y + highlightWidth),
                            size = Size((radius - highlightWidth) * 2, (radius - highlightWidth) * 2),
                            style = Stroke(width = (dims.spacingXs * 0.4f).toPx(), cap = StrokeCap.Butt)
                        )
                    }

                    // 4. Selection Highlight
                    if (isSelected) {
                        drawArc(
                            color = ChartGlossHighlight,
                            startAngle = currentAngle + (gapAngle / 2),
                            sweepAngle = sweepAngle,
                            useCenter = !isDonut,
                            topLeft = Offset(center.x - radius + offset.x, center.y - radius + offset.y),
                            size = Size(radius * 2, radius * 2),
                            style = if (isDonut) {
                                Stroke(width = radius * 0.42f, cap = StrokeCap.Butt)
                            } else {
                                Stroke(width = dims.spacingXs.toPx())
                            }
                        )
                    }

                    // Visual marker for subcategories (Modern Radial Tick)
                    if (slice.hasSubcategories && sliceSweep > 10f) {
                        val tickRadius = radius * 1.05f
                        val tickLength = dims.spacingSm.toPx()
                        
                        val startX = center.x + cos(midAngle.toDouble()).toFloat() * (tickRadius) + offset.x
                        val startY = center.y + sin(midAngle.toDouble()).toFloat() * (tickRadius) + offset.y
                        val endX = center.x + cos(midAngle.toDouble()).toFloat() * (tickRadius + tickLength) + offset.x
                        val endY = center.y + sin(midAngle.toDouble()).toFloat() * (tickRadius + tickLength) + offset.y
                        
                        drawLine(
                            color = slice.color.copy(alpha = 0.8f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = (dims.spacingXs / 2).toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    
                    currentAngle += sliceSweep
                }
            }

            // Dismissal layer for Tooltip
            if (selectedSlice != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                selectedSlice = null
                                touchPoint = null
                            }
                        }
                )
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
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AeonPieChartPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.size(300.dp).padding(AeonTheme.dimensions.screenPadding)) {
            AeonPieChart(
                slices = listOf(
                    AeonPieChartSlice(BigDecimal("450.00"), "Food", ChartColorOrange, hasSubcategories = true, subcategoryCount = 3),
                    AeonPieChartSlice(BigDecimal("300.00"), "Rent", TagPresetColors[5]),
                    AeonPieChartSlice(BigDecimal("150.00"), "Transport", TagPresetColors[10], hasSubcategories = true, subcategoryCount = 2),
                    AeonPieChartSlice(BigDecimal("100.00"), "Utilities", TagPresetColors[13]),
                    AeonPieChartSlice(BigDecimal("80.00"), "Health", TagPresetColors[1]),
                    AeonPieChartSlice(BigDecimal("60.00"), "Entertainment", TagPresetColors[2]),
                    AeonPieChartSlice(BigDecimal("40.00"), "Insurance", TagPresetColors[4]),
                    AeonPieChartSlice(BigDecimal("20.00"), "Misc", TagPresetColors[7])
                ),
                animate = false
            )
        }
    }
}
