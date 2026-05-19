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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import android.graphics.Paint as AndroidPaint

/**
 * High-fidelity, interactive bar + line chart for dual-axis visualization.
 *
 * When [incomeData] and [expenseData] are both non-empty, draws TWO bars per bucket:
 *   - Income bar: positive, green, left half of the bucket slot
 *   - Expense bar: negative (drawn downward from zero), red, right half
 *
 * When only [barData] is provided (legacy), keeps existing single-bar behavior unchanged.
 *
 * The line uses a cubic bezier curve instead of straight segments for a smooth appearance.
 *
 * When [showXAxisLabels] is true, labels from [xLabels] are drawn below the chart axis.
 * At most every max(1, total/6) labels are shown, always including the last.
 *
 * [lineThicknessDp] overrides the default line stroke width when > 0.
 */
@Composable
fun AeonBarLineChart(
    barData: List<BigDecimal> = emptyList(),
    lineData: List<BigDecimal>,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    precision: Int = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision,
    barColorPositive: Color = ChartColorGreen,
    barColorNegative: Color = ChartColorRed,
    lineColor: Color = ChartColorBlue,
    animate: Boolean = true,
    smaData: List<Double>? = null,
    onNavigateToRecords: ((Int) -> Unit)? = null,
    // New parameters for income/expense dual-bar mode
    incomeData: List<BigDecimal> = emptyList(),
    expenseData: List<BigDecimal> = emptyList(),
    xLabels: List<String?> = emptyList(),
    showXAxisLabels: Boolean = false,
    lineThicknessDp: Float = 0f,
    showGrid: Boolean = true
) {
    val dualBarMode = incomeData.isNotEmpty() && expenseData.isNotEmpty()
    val effectiveBarData = when {
        dualBarMode -> incomeData  // used only for index/total counting
        else -> barData
    }

    if (effectiveBarData.isEmpty() || lineData.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = AppTextStyles.caption)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val dims = AeonTheme.dimensions

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(barData, lineData, incomeData, expenseData) {
        if (animate) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, tween(1000))
        } else {
            animationProgress.snapTo(1f)
        }
    }

    // Extra bottom padding for x-axis labels
    val xLabelExtraDp = if (showXAxisLabels) 14.dp else 0.dp

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val xLabelExtraPx = with(density) { xLabelExtraDp.toPx() }
        val leftPadding = with(density) { 50.dp.toPx() }
        val topPadding = with(density) { dims.spacingMd.toPx() }
        val rightPadding = with(density) { dims.spacingMd.toPx() }
        val bottomPadding = with(density) { dims.spacing2xl.toPx() } + xLabelExtraPx
        val chartWidth = widthPx - leftPadding - rightPadding

        val allValues = if (dualBarMode) {
            incomeData + expenseData.map { it.negate() } + lineData
        } else {
            effectiveBarData + lineData
        }

        if (allValues.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data available", style = AppTextStyles.caption)
            }
            return@BoxWithConstraints
        }

        val minY = allValues.minOf { it }
        val maxY = allValues.maxOf { it }

        // Calculate a "nice" step size based on the data range
        val dataRange = (maxY - minY).abs().toDouble().coerceAtLeast(1.0)
        val roughStep = dataRange / 4.0
        val mag = Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(roughStep.coerceAtLeast(1e-10))))
        val niceMultiplier = when {
            roughStep / mag <= 1.0 -> 1.0
            roughStep / mag <= 2.0 -> 2.0
            roughStep / mag <= 5.0 -> 5.0
            else -> 10.0
        }
        val niceStep = maxOf(BigDecimal.valueOf(niceMultiplier * mag), BigDecimal.ONE)

        // Align boundaries to ticks, ensuring one empty step above and below
        val effectiveMinY = minY.divide(niceStep, 0, RoundingMode.FLOOR).multiply(niceStep)
        val effectiveMaxY = maxY.divide(niceStep, 0, RoundingMode.CEILING).multiply(niceStep)
        val effectiveRange = (effectiveMaxY - effectiveMinY).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE

        val chartHeight = heightPx - topPadding - bottomPadding

        fun valueToY(value: BigDecimal): Float {
            val normalized = (value - effectiveMinY).divide(effectiveRange, 10, RoundingMode.HALF_UP).toFloat()
            return heightPx - bottomPadding - (normalized * chartHeight)
        }

        fun indexToX(index: Int, total: Int): Float {
            if (total <= 1) return widthPx / 2
            return leftPadding + (index.toFloat() / (total - 1)) * chartWidth
        }

        val zeroY = valueToY(BigDecimal.ZERO)

        fun findIndex(offsetX: Float): Int {
            val total = effectiveBarData.size
            val step = chartWidth / (total - 1).coerceAtLeast(1)
            return ((offsetX - leftPadding + step / 2) / step).toInt().coerceIn(0, total - 1)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Bar and line chart comparison. Drag or tap to explore values."
                }
                .pointerInput(barData, lineData, incomeData, expenseData) {
                    detectDragGestures(
                        onDragStart = { offset -> selectedIndex = findIndex(offset.x) },
                        onDrag = { change, _ -> selectedIndex = findIndex(change.position.x) },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(barData, lineData, incomeData, expenseData) {
                    detectTapGestures(
                        onTap = { offset -> selectedIndex = findIndex(offset.x) },
                        onLongPress = {
                            // Consumes to avoid parent reordering
                        }
                    )
                }
        ) {
            // 1. Draw Grid + Y-Axis Labels
            if (showGrid) {
                val gridColor = ChartGridColor.copy(alpha = 0.25f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                val labelPaint = AndroidPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(140, 158, 158, 158)
                    textAlign = AndroidPaint.Align.RIGHT
                    textSize = with(density) { 9.sp.toPx() }
                }

                var tick = effectiveMinY
                val chartTop = topPadding
                val chartBottom = heightPx - bottomPadding
                while (tick <= effectiveMaxY) {
                    val yPos = valueToY(tick)
                    if (yPos >= chartTop - 1f && yPos <= chartBottom + 1f) {
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, yPos),
                            end = Offset(widthPx - rightPadding, yPos),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            formatShortNumber(tick),
                            leftPadding - with(density) { 4.dp.toPx() },
                            yPos + with(density) { 3.dp.toPx() },
                            labelPaint
                        )
                    }
                    tick = tick.add(niceStep)
                }
            }

            // Zero Line
            drawLine(
                color = ChartZeroLineColor,
                start = Offset(leftPadding, zeroY),
                end = Offset(widthPx - rightPadding, zeroY),
                strokeWidth = (dims.spacingXs / 4).toPx()
            )

            // 2. Draw Bars
            if (dualBarMode) {
                val totalBuckets = incomeData.size
                val slotWidth = chartWidth / totalBuckets.coerceAtLeast(1)
                val halfBarWidth = slotWidth * 0.35f  // each half-bar is 35% of the slot

                incomeData.forEachIndexed { index, incomeVal ->
                    val slotCenterX = leftPadding + (index + 0.5f) * slotWidth
                    val incomeX = slotCenterX - halfBarWidth / 2
                    val incomeY = valueToY(incomeVal)
                    val animatedIncomeY = zeroY - (zeroY - incomeY) * animationProgress.value

                    val incomeTop = if (incomeVal >= BigDecimal.ZERO) animatedIncomeY else zeroY
                    val incomeBottom = if (incomeVal >= BigDecimal.ZERO) zeroY else animatedIncomeY

                    drawRoundRect(
                        color = barColorPositive.copy(alpha = 0.8f),
                        topLeft = Offset(incomeX, incomeTop),
                        size = Size(halfBarWidth, (incomeBottom - incomeTop).coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(dims.spacingXs.toPx(), dims.spacingXs.toPx())
                    )
                }

                expenseData.forEachIndexed { index, expenseVal ->
                    val slotCenterX = leftPadding + (index + 0.5f) * slotWidth
                    val expenseX = slotCenterX  // right half of the slot
                    // Expense is stored as positive, drawn as negative (below zero)
                    val negExpense = expenseVal.negate()
                    val expenseY = valueToY(negExpense)
                    // expenseY > zeroY in canvas coords (below the zero line)
                    val animatedExpenseY = zeroY + (expenseY - zeroY) * animationProgress.value

                    // topLeft.y = zeroY (the zero line), bar extends downward to animatedExpenseY
                    drawRoundRect(
                        color = barColorNegative.copy(alpha = 0.8f),
                        topLeft = Offset(expenseX, zeroY),
                        size = Size(halfBarWidth, (animatedExpenseY - zeroY).coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(dims.spacingXs.toPx(), dims.spacingXs.toPx())
                    )
                }
            } else {
                val barWidth = chartWidth / effectiveBarData.size * 0.5f
                effectiveBarData.forEachIndexed { index, value ->
                    val x = indexToX(index, effectiveBarData.size)
                    val y = valueToY(value)

                    val animatedY = zeroY - (zeroY - y) * animationProgress.value
                    val color = if (value >= BigDecimal.ZERO) barColorPositive else barColorNegative

                    val top = if (value >= BigDecimal.ZERO) animatedY else zeroY
                    val bottom = if (value >= BigDecimal.ZERO) zeroY else animatedY

                    drawRoundRect(
                        color = color.copy(alpha = 0.8f),
                        topLeft = Offset(x - barWidth / 2, top),
                        size = Size(barWidth, (bottom - top).coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(dims.spacingXs.toPx(), dims.spacingXs.toPx())
                    )
                }
            }

            // 3. Draw Line (+ gradient area fill in dual-bar mode) using cubic bezier
            // In dual-bar mode, align line points to bar slot centers for visual consistency
            val linePath = Path()
            val areaPath = if (dualBarMode) Path() else null
            var prevX = 0f
            var prevY = 0f
            val lineTotal = lineData.size
            // In dual-bar mode use the full-period slot count (incomeData.size) so a truncated net
            // line shares the same x-spacing as the bars rather than stretching to fill the chart.
            val fullPeriodSlots = if (dualBarMode) incomeData.size else lineTotal
            val lineSlotWidth = if (dualBarMode && fullPeriodSlots > 0) {
                chartWidth / fullPeriodSlots
            } else 0f
            lineData.forEachIndexed { index, value ->
                val x = if (dualBarMode) {
                    leftPadding + (index + 0.5f) * lineSlotWidth
                } else {
                    indexToX(index, lineTotal)
                }
                val y = valueToY(value)
                val animatedY = zeroY + (y - zeroY) * animationProgress.value

                if (index == 0) {
                    linePath.moveTo(x, animatedY)
                    areaPath?.moveTo(x, zeroY)
                    areaPath?.lineTo(x, animatedY)
                } else {
                    val dx = x - prevX
                    linePath.cubicTo(prevX + dx * 0.3f, prevY, x - dx * 0.3f, animatedY, x, animatedY)
                    areaPath?.cubicTo(prevX + dx * 0.3f, prevY, x - dx * 0.3f, animatedY, x, animatedY)
                }
                if (index == lineTotal - 1) {
                    areaPath?.lineTo(x, zeroY)
                    areaPath?.close()
                }
                prevX = x
                prevY = animatedY
            }

            // Draw area gradient fill under the net line (dual-bar mode only)
            areaPath?.let {
                drawPath(
                    path = it,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.2f), lineColor.copy(alpha = 0f)),
                        startY = topPadding,
                        endY = heightPx - bottomPadding
                    )
                )
            }

            val lineStrokeWidth = if (lineThicknessDp > 0f) {
                with(density) { lineThicknessDp.dp.toPx() }
            } else {
                (dims.spacingXs * 0.75f).toPx()
            }

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = lineStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 4. Draw SMA (Smooth Moving Average)
            smaData?.let { sma ->
                val smaPath = Path()
                var smaPrevX = 0f
                var smaPrevY = 0f
                sma.forEachIndexed { index, value ->
                    val x = indexToX(index, sma.size)
                    val y = valueToY(value.toBigDecimal())
                    val animatedY = zeroY + (y - zeroY) * animationProgress.value
                    if (index == 0) {
                        smaPath.moveTo(x, animatedY)
                    } else {
                        val dx = x - smaPrevX
                        smaPath.cubicTo(smaPrevX + dx * 0.3f, smaPrevY, x - dx * 0.3f, animatedY, x, animatedY)
                    }
                    smaPrevX = x
                    smaPrevY = animatedY
                }
                drawPath(
                    path = smaPath,
                    color = lineColor.copy(alpha = 0.5f),
                    style = Stroke(width = (dims.spacingXs * 1.25f).toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 5. Draw X-Axis Labels
            if (showXAxisLabels && xLabels.isNotEmpty()) {
                val total = xLabels.size
                val lastIdx = total - 1
                // Derive jump from the minimum pixel gap a label needs so they never overlap.
                // "dd/MM" at 9sp is ~32dp wide; this scales correctly with chart width and data density.
                val minLabelSpacingPx = with(density) { 32.dp.toPx() }
                val jump = maxOf(1, kotlin.math.ceil(minLabelSpacingPx * total / chartWidth).toInt())
                val threshold = (jump / 2).coerceAtLeast(1)
                val nearestLowerJump = (lastIdx / jump) * jump
                val skipNearestLowerJump = lastIdx % jump != 0 && (lastIdx - nearestLowerJump) <= threshold
                val labelPaint = AndroidPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(220, 180, 180, 180)
                    textAlign = AndroidPaint.Align.CENTER
                    textSize = with(density) { 9.sp.toPx() }
                }
                val labelSlotWidth = if (dualBarMode) chartWidth / total.coerceAtLeast(1) else 0f
                xLabels.forEachIndexed { index, label ->
                    if (label == null) return@forEachIndexed
                    val show = index == 1 || index == lastIdx ||
                        (index > 1 && index % jump == 0 && !(skipNearestLowerJump && index == nearestLowerJump))
                    if (show) {
                        val x = if (dualBarMode) {
                            leftPadding + (index + 0.5f) * labelSlotWidth
                        } else {
                            indexToX(index, total)
                        }
                        drawContext.canvas.nativeCanvas.drawText(label, x, heightPx - xLabelExtraPx * 0.2f, labelPaint)
                    }
                }
            }

            // 6. Draw selection highlight
            selectedIndex?.let { index ->
                val selTotal = effectiveBarData.size
                val selSlotWidth = if (dualBarMode) chartWidth / selTotal.coerceAtLeast(1) else 0f
                val x = if (dualBarMode) leftPadding + (index + 0.5f) * selSlotWidth else indexToX(index, selTotal)
                drawLine(
                    color = ChartZeroLineColor,
                    start = Offset(x, topPadding),
                    end = Offset(x, heightPx - bottomPadding),
                    strokeWidth = (dims.spacingXs / 4).toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }

        // Tooltip
        selectedIndex?.let { index ->
            val ttTotal = effectiveBarData.size
            val ttSlotWidth = if (dualBarMode) chartWidth / ttTotal.coerceAtLeast(1) else 0f
            val x = if (dualBarMode) leftPadding + (index + 0.5f) * ttSlotWidth else indexToX(index, ttTotal)

            val tooltipWidth = 160.dp
            val tooltipHeight = if (dualBarMode) 120.dp else 100.dp
            val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
            val tooltipHeightPx = with(density) { tooltipHeight.toPx() }

            val xPos = (x - tooltipWidthPx / 2).coerceIn(0f, widthPx - tooltipWidthPx)
            val yPos = (topPadding - 10f).coerceIn(0f, heightPx - tooltipHeightPx)

            val label = xLabels.getOrNull(index)
            val barVal = effectiveBarData.getOrNull(index)
            val incomeVal = incomeData.getOrNull(index)
            val expenseVal = expenseData.getOrNull(index)
            val lineVal = lineData.getOrNull(index)

            Surface(
                modifier = Modifier
                    .offset(x = (xPos / density.density).dp, y = (yPos / density.density).dp)
                    .width(tooltipWidth)
                    .clip(RoundedCornerShape(dims.spacingMd)),
                color = AppColors.insetSurface,
                tonalElevation = dims.spacingSm,
                shadowElevation = dims.spacingSm,
                border = androidx.compose.foundation.BorderStroke((dims.spacingXs / 4), lineColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(dims.spacingSm)) {
                    Text(
                        label ?: "Period ${index + 1}",
                        style = AppTextStyles.transactionSwipeAction,
                        color = AppColors.contentSecondary
                    )
                    Spacer(modifier = Modifier.height(dims.spacingXs))

                    if (dualBarMode) {
                        if (incomeVal != null) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Income:", style = AppTextStyles.transactionSwipeAction)
                                Text(
                                    formatCurrencyRight(incomeVal, currency, precision),
                                    style = AppTextStyles.managementItemSubtitle,
                                    color = barColorPositive
                                )
                            }
                        }
                        if (expenseVal != null) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Expense:", style = AppTextStyles.transactionSwipeAction)
                                Text(
                                    formatCurrencyRight(expenseVal, currency, precision),
                                    style = AppTextStyles.managementItemSubtitle,
                                    color = barColorNegative
                                )
                            }
                        }
                    } else if (barVal != null) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Flow:", style = AppTextStyles.transactionSwipeAction)
                            Text(
                                formatCurrencyRight(barVal, currency, precision),
                                style = AppTextStyles.managementItemSubtitle,
                                color = if (barVal >= BigDecimal.ZERO) barColorPositive else barColorNegative
                            )
                        }
                    }

                    if (lineVal != null) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Net:", style = AppTextStyles.transactionSwipeAction)
                            Text(
                                formatCurrencyRight(lineVal, currency, precision),
                                style = AppTextStyles.managementItemSubtitle,
                                color = lineColor
                            )
                        }
                    }

                    if (onNavigateToRecords != null) {
                        Spacer(modifier = Modifier.height(dims.spacingSm))
                        OutlinedButton(
                            onClick = { onNavigateToRecords(index) },
                            modifier = Modifier.height(dims.spacing2xl),
                            contentPadding = PaddingValues(horizontal = dims.spacingMd, vertical = 0.dp),
                            shape = RoundedCornerShape(dims.spacingSm),
                            border = androidx.compose.foundation.BorderStroke((dims.spacingXs / 4), lineColor.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(dims.iconSizeLg / 2), tint = lineColor)
                            Spacer(modifier = Modifier.width(dims.spacingXs))
                            Text("See Records", style = AppTextStyles.transactionSwipeAction, color = lineColor)
                        }
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
        Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(AeonTheme.dimensions.screenPadding)) {
            AeonBarLineChart(
                barData = listOf(BigDecimal("500"), BigDecimal("-200"), BigDecimal("800"), BigDecimal("400")),
                lineData = listOf(BigDecimal("500"), BigDecimal("300"), BigDecimal("1100"), BigDecimal("1500"))
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Dual Bar Mode")
@Composable
fun AeonBarLineChartDualPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxWidth().height(280.dp).padding(AeonTheme.dimensions.screenPadding)) {
            AeonBarLineChart(
                lineData = listOf(BigDecimal.ZERO, BigDecimal("300"), BigDecimal("700"), BigDecimal("300"), BigDecimal("900")),
                incomeData = listOf(BigDecimal.ZERO, BigDecimal("500"), BigDecimal("800"), BigDecimal("400"), BigDecimal("1000")),
                expenseData = listOf(BigDecimal.ZERO, BigDecimal("200"), BigDecimal("100"), BigDecimal("100"), BigDecimal("100")),
                xLabels = listOf(null, "Jan", "Feb", "Mar", "Apr"),
                showXAxisLabels = true,
                lineThicknessDp = 1.5f
            )
        }
    }
}
