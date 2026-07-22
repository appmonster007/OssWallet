package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorBlue
import com.osscabinet.ossWallet.ui.theme.ChartColorGreen
import com.osscabinet.ossWallet.ui.theme.ChartColorRed
import com.osscabinet.ossWallet.ui.theme.ChartZeroLineColor
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletPreviewTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.formatCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import android.graphics.Paint as AndroidPaint

/**
 * High-fidelity, interactive bar + line chart for dual-axis visualization.
 * Reuses the stacked bar rendering logic from [AeonBarChart].
 *
 * [groups] provides the stacked bar data per X-axis slot.
 * [lineData] provides the net line data points.
 */
@Composable
fun AeonBarLineChart(
    groups: List<AeonBarChartGroup>,
    lineData: List<BigDecimal>,
    modifier: Modifier = Modifier,
    currency: String,
    precision: Int = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision,
    lineColor: Color = ChartColorBlue,
    animate: Boolean = false,
    smaData: List<Double>? = null,
    onNavigateToRecords: ((Int) -> Unit)? = null,
    xLabels: List<String?> = emptyList(),
    showXAxisLabels: Boolean = false,
    showPeriodBars: Boolean = true,
    lineThicknessDp: Float = 0f,
    showGrid: Boolean = true,
    roundedBars: Boolean = false
) {
    if (groups.isEmpty() && lineData.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = AppTextStyles.caption)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val dims = AeonTheme.dimensions

    val animationProgress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(groups, lineData) {
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
        val leftPadding = with(density) { 48.dp.toPx() }
        val topPadding = with(density) { dims.spacingLg.toPx() }
        val rightPadding = with(density) { dims.spacingMd.toPx() }
        val bottomPadding =
            with(density) { dims.spacingXl.toPx() } + with(density) { (if (showXAxisLabels) 16.dp else 0.dp).toPx() }
        val chartWidth = widthPx - leftPadding - rightPadding

        // Calculate limits for stacked bars and line
        val groupTotals = groups.map { g ->
            val pos = g.bars.filter { it.value > BigDecimal.ZERO }.sumOf { it.value }
            val neg = g.bars.filter { it.value < BigDecimal.ZERO }.sumOf { it.value }
            pos to neg
        }

        val allValues = buildList {
            groupTotals.forEach { (pos, neg) ->
                add(pos)
                add(neg)
            }
            if (lineData.isNotEmpty()) {
                addAll(lineData)
            }
        }

        val minY = if (allValues.isEmpty()) BigDecimal.ZERO else allValues.minOf { it }.coerceAtMost(BigDecimal.ZERO)
        val maxY = if (allValues.isEmpty()) BigDecimal.ONE else allValues.maxOf { it }.coerceAtLeast(BigDecimal.ONE)

        // Nice step logic
        val dataRange = (maxY - minY).abs().toDouble().coerceAtLeast(1.0)
        val roughStep = dataRange / 4.0
        val mag =
            Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(roughStep.coerceAtLeast(1e-10))))
        val niceMultiplier = when {
            roughStep / mag <= 1.0 -> 1.0
            roughStep / mag <= 2.0 -> 2.0
            roughStep / mag <= 5.0 -> 5.0
            else -> 10.0
        }
        val niceStep = maxOf(BigDecimal.valueOf(niceMultiplier * mag), BigDecimal.ONE)

        val effectiveMinY = minY.divide(niceStep, 0, RoundingMode.FLOOR).multiply(niceStep)
        val effectiveMaxY = maxY.divide(niceStep, 0, RoundingMode.CEILING).multiply(niceStep)
        val effectiveRange =
            (effectiveMaxY - effectiveMinY).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE

        val chartHeight = heightPx - topPadding - bottomPadding

        val slotWidth = chartWidth / groups.size.coerceAtLeast(1)

        fun valueToY(value: BigDecimal): Float {
            val normalized =
                (value - effectiveMinY).divide(effectiveRange, 10, RoundingMode.HALF_UP).toFloat()
            return heightPx - bottomPadding - (normalized * chartHeight)
        }

        fun indexToX(index: Int, total: Int): Float {
            if (total <= 0) return leftPadding + chartWidth / 2
            val sWidth = chartWidth / total
            return leftPadding + (index + 0.5f) * sWidth
        }

        val zeroY = valueToY(BigDecimal.ZERO)
        val chartSpec = remember(precision) {
            CurrencyDisplaySpec(
                precision = precision,
                shortenThreshold = 10
            )
        }
        val currencyFormatter =
            remember { com.osscabinet.ossWallet.ui.util.CurrencyReprUi.CurrencyNumberFormatter() }

        val isDark = LocalThemeMode.current.isDarkTheme
        val gridLineColor = AppColors.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.4f)
        val periodBarColor = AppColors.outlineVariant.copy(alpha = if (isDark) 0.05f else 0.1f)
        val labelColor = AppColors.contentSecondary.copy(alpha = 0.6f)
        val axisLabelColor = AppColors.contentSecondary.copy(alpha = 0.8f)

        fun findIndex(offsetX: Float): Int {
            val total = groups.size
            if (total <= 0) return -1
            return ((offsetX - leftPadding) / slotWidth).toInt().coerceIn(0, total - 1)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription =
                        "Bar and line chart comparison. Drag or tap to explore values."
                }
                .pointerInput(groups, lineData) {
                    detectDragGestures(
                        onDragStart = { offset -> selectedIndex = findIndex(offset.x) },
                        onDrag = { change, _ -> selectedIndex = findIndex(change.position.x) },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(groups, lineData) {
                    detectTapGestures(
                        onTap = { offset -> selectedIndex = findIndex(offset.x) }
                    )
                }
        ) {
            // 1. Draw Grid + Y-Axis Labels
            if (showGrid) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                val labelPaint = AndroidPaint().apply {
                    isAntiAlias = true
                    color = labelColor.toArgb()
                    textAlign = AndroidPaint.Align.RIGHT
                    textSize = 9.sp.toPx()
                }

                var tick = effectiveMinY
                while (tick <= effectiveMaxY) {
                    val yPos = valueToY(tick)
                    if (yPos >= topPadding - 1f && yPos <= heightPx - bottomPadding + 1f) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(leftPadding, yPos),
                            end = Offset(widthPx - rightPadding, yPos),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            currencyFormatter.format(
                                amount = tick,
                                currencyCode = currency,
                                spec = chartSpec
                            ),
                            leftPadding - dims.spacingSm.toPx(),
                            yPos + 3.dp.toPx(),
                            labelPaint
                        )
                    }
                    tick = tick.add(niceStep)
                }
            }

            // 1.5 Draw Period Background Bars
            if (showPeriodBars && groups.isNotEmpty()) {
                val totalSlots = groups.size
                for (index in groups.indices) {
                    val slotCenterX = indexToX(index, totalSlots)
                    val barWidth = slotWidth * 0.95f
                    drawRect(
                        color = periodBarColor,
                        topLeft = Offset(slotCenterX - barWidth / 2, topPadding),
                        size = Size(barWidth, chartHeight)
                    )
                }
            }

            // Zero Line
            drawLine(
                color = ChartZeroLineColor,
                start = Offset(leftPadding, zeroY),
                end = Offset(widthPx - rightPadding, zeroY),
                strokeWidth = (dims.spacingXs / 4).toPx()
            )

            // 3. Draw Stacked Bars (from AeonBarChart)
            val barWidth = slotWidth * 0.85f
            groups.forEachIndexed { groupIndex, group ->
                val centerX = indexToX(groupIndex, groups.size)
                var currentPosAccumulator = BigDecimal.ZERO
                var currentNegAccumulator = BigDecimal.ZERO

                group.bars.forEach { bar ->
                    val isPositive = bar.value >= BigDecimal.ZERO
                    val startVal = if (isPositive) currentPosAccumulator else currentNegAccumulator
                    val endVal = startVal + bar.value

                    val animatedStart = startVal.multiply(BigDecimal.valueOf(animationProgress.value.toDouble()))
                    val animatedEnd = endVal.multiply(BigDecimal.valueOf(animationProgress.value.toDouble()))

                    val startY = valueToY(animatedStart)
                    val endY = valueToY(animatedEnd)

                    val top = if (isPositive) endY else startY
                    val bottom = if (isPositive) startY else endY

                    val radiusPx = if (roundedBars) with(density) { 4.dp.toPx() } else 0f
                    drawRoundRect(
                        color = bar.color.copy(alpha = 0.8f),
                        topLeft = Offset(centerX - barWidth / 2, top),
                        size = Size(barWidth, (bottom - top).coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )

                    if (isPositive) currentPosAccumulator = endVal
                    else currentNegAccumulator = endVal
                }
            }

            // 4. Draw Net Line (+ gradient area fill)
            if (lineData.isNotEmpty()) {
                val linePath = Path()
                val areaPath = Path()
                var prevX = 0f
                var prevY = 0f
                val lineTotal = lineData.size
                lineData.forEachIndexed { index, value ->
                    val x = indexToX(index, groups.size)
                    val y = valueToY(value)
                    val animatedY = zeroY + (y - zeroY) * animationProgress.value

                    if (index == 0) {
                        linePath.moveTo(x, animatedY)
                        areaPath.moveTo(x, zeroY)
                        areaPath.lineTo(x, animatedY)
                    } else {
                        val dx = x - prevX
                        linePath.cubicTo(prevX + dx * 0.3f, prevY, x - dx * 0.3f, animatedY, x, animatedY)
                        areaPath.cubicTo(prevX + dx * 0.3f, prevY, x - dx * 0.3f, animatedY, x, animatedY)
                    }
                    if (index == lineTotal - 1) {
                        areaPath.lineTo(x, zeroY)
                        areaPath.close()
                    }
                    prevX = x
                    prevY = animatedY
                }

                // Draw area gradient
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.2f), lineColor.copy(alpha = 0f)),
                        startY = topPadding,
                        endY = heightPx - bottomPadding
                    )
                )

                val lineStrokeWidth = if (lineThicknessDp > 0f) with(density) { lineThicknessDp.dp.toPx() } else (dims.spacingXs * 0.75f).toPx()
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = lineStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 5. Draw SMA
            smaData?.let { sma ->
                val smaPath = Path()
                var smaPrevX = 0f
                var smaPrevY = 0f
                sma.forEachIndexed { index, value ->
                    val x = indexToX(index, sma.size)
                    val y = valueToY(value.toBigDecimal())
                    val animatedY = zeroY + (y - zeroY) * animationProgress.value
                    if (index == 0) smaPath.moveTo(x, animatedY)
                    else {
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

            // 6. X-Axis Labels
            if (showXAxisLabels && xLabels.isNotEmpty()) {
                val total = xLabels.size
                val firstValidIdx = xLabels.indexOfFirst { it != null }
                val lastValidIdx = xLabels.indexOfLast { it != null }
                val indicesToShow = mutableSetOf<Int>()
                if (firstValidIdx != -1 && lastValidIdx != -1) {
                    indicesToShow.add(firstValidIdx)
                    indicesToShow.add(lastValidIdx)
                    val range = lastValidIdx - firstValidIdx
                    if (range > 0) {
                        val interval = range.toFloat() / 6f
                        for (i in 1..5) indicesToShow.add(firstValidIdx + Math.round(i * interval))
                    }
                }
                val labelPaint = AndroidPaint().apply {
                    isAntiAlias = true
                    color = axisLabelColor.toArgb()
                    textAlign = AndroidPaint.Align.CENTER
                    textSize = 9.sp.toPx()
                }
                xLabels.forEachIndexed { index, label ->
                    if (label != null && indicesToShow.contains(index)) {
                        val x = indexToX(index, total)
                        drawContext.canvas.nativeCanvas.drawText(
                            label, x, heightPx - bottomPadding + with(density) { 14.dp.toPx() }, labelPaint
                        )
                    }
                }
            }

            // 7. Selection Highlight
            selectedIndex?.let { index ->
                val x = indexToX(index, groups.size)
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
            val group = groups.getOrNull(index) ?: return@let
            val x = indexToX(index, groups.size)
            val tooltipWidth = 180.dp
            val tooltipHeight = (40 + (group.bars.size + 1) * 25).dp.coerceAtMost(240.dp)
            val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
            val tooltipHeightPx = with(density) { tooltipHeight.toPx() }
            val xPos = (x - tooltipWidthPx / 2).coerceIn(0f, (widthPx - tooltipWidthPx).coerceAtLeast(0f))
            val yPos = topPadding.coerceIn(0f, (heightPx - tooltipHeightPx).coerceAtLeast(0f))

            val label = xLabels.getOrNull(index) ?: group.label
            val lineVal = lineData.getOrNull(index)

            AeonChartTooltip(
                modifier = Modifier
                    .offset(x = (xPos / density.density).dp, y = (yPos / density.density).dp)
                    .width(tooltipWidth)
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                title = label.ifBlank { "Period ${index + 1}" }
            ) {
                group.bars.forEach { bar ->
                    AeonTooltipEntry(
                        label = bar.label,
                        value = formatCurrency(bar.value, currency, CurrencyDisplaySpec(precision = precision)),
                        color = bar.color
                    )
                }
                if (lineVal != null) {
                    AeonTooltipEntry(
                        label = "Net",
                        value = formatCurrency(lineVal, currency, CurrencyDisplaySpec(precision = precision)),
                        color = lineColor
                    )
                }
                if (onNavigateToRecords != null) {
                    Button(
                        onClick = { onNavigateToRecords(index) },
                        modifier = Modifier.fillMaxWidth().height(dims.spacing2xl),
                        contentPadding = PaddingValues(horizontal = dims.spacingMd, vertical = 0.dp),
                        shape = RoundedCornerShape(dims.spacingSm),
                        colors = ButtonDefaults.buttonColors(containerColor = lineColor.copy(alpha = 0.1f), contentColor = lineColor)
                    ) {
                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(dims.iconSizeSm), tint = lineColor)
                        Spacer(modifier = Modifier.width(dims.spacingXs))
                        Text("RECORDS", style = AppTextStyles.transactionSwipeAction.copy(fontWeight = FontWeight.Bold), color = lineColor)
                    }
                }
            }
        }
    }
}

/**
 * Backward compatible overload for simple bar+line charts.
 */
@Composable
fun AeonBarLineChart(
    barData: List<BigDecimal>,
    lineData: List<BigDecimal>,
    modifier: Modifier = Modifier,
    currency: String,
    precision: Int = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision,
    barColorPositive: Color = ChartColorGreen,
    barColorNegative: Color = ChartColorRed,
    lineColor: Color = ChartColorBlue,
    animate: Boolean = false,
    xLabels: List<String?> = emptyList(),
    showXAxisLabels: Boolean = false,
    roundedBars: Boolean = false
) {
    val groups = remember(barData, barColorPositive, barColorNegative) {
        barData.mapIndexed { index, value ->
            AeonBarChartGroup(
                label = xLabels.getOrNull(index) ?: "",
                bars = listOf(
                    AeonBarChartBar(
                        value = value,
                        label = "Flow",
                        color = if (value >= BigDecimal.ZERO) barColorPositive else barColorNegative
                    )
                )
            )
        }
    }
    AeonBarLineChart(
        groups = groups,
        lineData = lineData,
        modifier = modifier,
        currency = currency,
        precision = precision,
        lineColor = lineColor,
        animate = animate,
        xLabels = xLabels,
        showXAxisLabels = showXAxisLabels,
        roundedBars = roundedBars
    )
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Theme")
@Composable
fun AeonBarLineChartLightPreview() {
    OssWalletPreviewTheme(theme = "Light") {
        Surface(color = AppColors.background) {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp)) {
                AeonBarLineChart(
                    barData = listOf(BigDecimal("500"), BigDecimal("-200"), BigDecimal("800")),
                    lineData = listOf(BigDecimal("500"), BigDecimal("300"), BigDecimal("1100")),
                    currency = "USD",
                    animate = false
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Dark Theme")
@Composable
fun AeonBarLineChartDarkPreview() {
    OssWalletPreviewTheme(theme = "Dark") {
        Surface(color = AppColors.background) {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp)) {
                AeonBarLineChart(
                    barData = listOf(BigDecimal("500"), BigDecimal("-200"), BigDecimal("800")),
                    lineData = listOf(BigDecimal("500"), BigDecimal("300"), BigDecimal("1100")),
                    currency = "USD",
                    animate = false
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Stacked Bar + Line Mode")
@Composable
fun AeonBarLineChartStackedPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxWidth().height(280.dp).padding(AeonTheme.dimensions.screenPadding)) {
            AeonBarLineChart(
                groups = listOf(
                    AeonBarChartGroup("Jan", listOf(
                        AeonBarChartBar(BigDecimal("500"), "Income", ChartColorGreen),
                        AeonBarChartBar(BigDecimal("-200"), "Expense", ChartColorRed)
                    )),
                    AeonBarChartGroup("Feb", listOf(
                        AeonBarChartBar(BigDecimal("800"), "Income", ChartColorGreen),
                        AeonBarChartBar(BigDecimal("-100"), "Expense", ChartColorRed)
                    ))
                ),
                lineData = listOf(BigDecimal("300"), BigDecimal("700")),
                currency = "USD",
                showXAxisLabels = true
            )
        }
    }
}
