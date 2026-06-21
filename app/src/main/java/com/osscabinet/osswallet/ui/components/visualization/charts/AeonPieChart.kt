package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorOrange
import com.osscabinet.ossWallet.ui.theme.ChartGlossHighlight
import com.osscabinet.ossWallet.ui.theme.ChartPieInnerShadow
import com.osscabinet.ossWallet.ui.theme.ChartRimHighlight
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.TagPresetColors
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
    precision: Int = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision,
    isDonut: Boolean = true,
    animate: Boolean = false,
    rounded: Boolean = true,
    previousTotal: BigDecimal? = null,
    showComparison: Boolean = false,
    isAllTime: Boolean = true,
    positiveDeltaIsGood: Boolean = false,
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

    val weightTotal = slices.fold(BigDecimal.ZERO) { acc, slice -> acc + slice.value.abs() }
    val displayTotal = slices.fold(BigDecimal.ZERO) { acc, slice -> acc + slice.value }
    
    if (weightTotal.signum() == 0) {
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
                    val sweepAngle =
                        (slice.value.abs().divide(weightTotal, 10, RoundingMode.HALF_UP).toFloat() * 360f)
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
                        contentDescription =
                            "Pie chart showing ${slices.joinToString { it.label }}. Drag or tap slices for details."
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
                    val sliceSweep =
                        (slice.value.abs().divide(weightTotal, 10, RoundingMode.HALF_UP).toFloat() * 360f)
                    val isSelected = selectedSlice == slice

                    // Modern Look: Precise radial gaps
                    val gapAngle = if (rounded) (if (sliceSweep < 1f) 0.1f else 1.5f) else 0f
                    val sweepAngle =
                        (sliceSweep - gapAngle).coerceAtLeast(0.1f) * animationProgress.value

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
                        topLeft = Offset(
                            center.x - radius + offset.x,
                            center.y - radius + offset.y
                        ),
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
                            topLeft = Offset(
                                center.x - radius + offset.x + highlightWidth,
                                center.y - radius + offset.y + highlightWidth
                            ),
                            size = Size(
                                (radius - highlightWidth) * 2,
                                (radius - highlightWidth) * 2
                            ),
                            style = Stroke(
                                width = (dims.spacingXs * 0.4f).toPx(),
                                cap = StrokeCap.Butt
                            )
                        )
                    }

                    // 4. Selection Highlight
                    if (isSelected) {
                        drawArc(
                            color = ChartGlossHighlight,
                            startAngle = currentAngle + (gapAngle / 2),
                            sweepAngle = sweepAngle,
                            useCenter = !isDonut,
                            topLeft = Offset(
                                center.x - radius + offset.x,
                                center.y - radius + offset.y
                            ),
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

                        val startX =
                            center.x + cos(midAngle.toDouble()).toFloat() * (tickRadius) + offset.x
                        val startY =
                            center.y + sin(midAngle.toDouble()).toFloat() * (tickRadius) + offset.y
                        val endX =
                            center.x + cos(midAngle.toDouble()).toFloat() * (tickRadius + tickLength) + offset.x
                        val endY =
                            center.y + sin(midAngle.toDouble()).toFloat() * (tickRadius + tickLength) + offset.y

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

            // Center total for donut charts
            if (isDonut) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(dims.spacingSm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val formattedTotal =
                        formatCurrency(
                            amount = displayTotal,
                            currencyCode = currency,
                            spec = com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec(precision = precision)
                        )

                    Text(
                        text = "TOTAL",
                        style = AppTextStyles.overlineLabel,
                        color = AppColors.contentSecondary
                    )
                    Text(
                        text = formattedTotal,
                        style = AppTextStyles.heroAmount,
                        color = AppColors.contentPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    // Comparison Delta
                    if (showComparison && !isAllTime && previousTotal != null && previousTotal.signum() != 0) {
                        val delta = displayTotal.abs().subtract(previousTotal.abs())
                            .divide(previousTotal.abs(), 8, RoundingMode.HALF_UP)
                            .multiply(BigDecimal("100"))

                        val isIncrease = delta.signum() > 0
                        val isDecrease = delta.signum() < 0

                        val color = when {
                            isIncrease -> if (positiveDeltaIsGood) PositiveAmountColor else NegativeAmountColor
                            isDecrease -> if (positiveDeltaIsGood) NegativeAmountColor else PositiveAmountColor
                            else -> AppColors.contentSecondary
                        }

                        val icon = when {
                            isIncrease -> Icons.Default.ArrowUpward
                            isDecrease -> Icons.Default.ArrowDownward
                            else -> null
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = color
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                text = "${delta.abs().setScale(1, RoundingMode.HALF_UP)}%",
                                style = AppTextStyles.cardDelta,
                                color = color
                            )
                        }
                    }
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
                        onDismissRequest = {
                            selectedSlice = null
                            touchPoint = null
                        },
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
        Box(modifier = Modifier
            .size(300.dp)
            .padding(AeonTheme.dimensions.screenPadding)) {
            AeonPieChart(
                slices = listOf(
                    AeonPieChartSlice(
                        BigDecimal("450.00"),
                        "Food",
                        ChartColorOrange,
                        hasSubcategories = true,
                        subcategoryCount = 3
                    ),
                    AeonPieChartSlice(BigDecimal("300.00"), "Rent", TagPresetColors[5]),
                    AeonPieChartSlice(
                        BigDecimal("150.00"),
                        "Transport",
                        TagPresetColors[10],
                        hasSubcategories = true,
                        subcategoryCount = 2
                    ),
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
