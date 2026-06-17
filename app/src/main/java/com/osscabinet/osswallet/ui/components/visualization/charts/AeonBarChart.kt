package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorBlue
import com.osscabinet.ossWallet.ui.theme.ChartColorGreen
import com.osscabinet.ossWallet.ui.theme.ChartColorOrange
import com.osscabinet.ossWallet.ui.theme.ChartColorRed
import com.osscabinet.ossWallet.ui.theme.ChartZeroLineColor
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import java.math.RoundingMode
import android.graphics.Paint as AndroidPaint

/**
 * High-fidelity, interactive bar chart supporting both simple and stacked visualizations.
 * Refined with professional grid logic and dynamic scaling.
 */
@Composable
fun AeonBarChart(
    groups: List<AeonBarChartGroup>,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    precision: Int = CurrencyService.precision,
    animate: Boolean = true,
    showGrid: Boolean = true,
    showXAxisLabels: Boolean = true,
    roundedBars: Boolean = false
) {
    if (groups.isEmpty() || groups.all { it.bars.isEmpty() }) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = AppTextStyles.caption)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val dims = AeonTheme.dimensions

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(groups) {
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

        val leftPadding = with(density) { 44.dp.toPx() }
        val rightPadding = with(density) { dims.spacingMd.toPx() }
        val topPadding = with(density) { dims.spacingMd.toPx() }
        val xLabelHeight = if (showXAxisLabels) with(density) { 20.dp.toPx() } else 0f
        val bottomPadding = with(density) { dims.spacingLg.toPx() } + xLabelHeight

        val chartWidth = widthPx - leftPadding - rightPadding
        val chartHeight = heightPx - topPadding - bottomPadding

        // Calculate limits for stacked or simple bars
        val groupTotals = groups.map { g ->
            val pos = g.bars.filter { it.value > BigDecimal.ZERO }.sumOf { it.value }
            val neg = g.bars.filter { it.value < BigDecimal.ZERO }.sumOf { it.value }
            pos to neg
        }

        val minY = groupTotals.minOf { it.second }.coerceAtMost(BigDecimal.ZERO)
        val maxY = groupTotals.maxOf { it.first }.coerceAtLeast(BigDecimal.ONE)

        // Nice step logic from AeonLineChart
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

        fun valueToY(value: BigDecimal): Float {
            val normalized =
                (value - effectiveMinY).divide(effectiveRange, 10, RoundingMode.HALF_UP).toFloat()
            return heightPx - bottomPadding - (normalized * chartHeight)
        }

        fun indexToX(index: Int, total: Int): Float {
            if (total == 0) return leftPadding + chartWidth / 2
            val slotWidth = chartWidth / total
            return leftPadding + (index * slotWidth) + (slotWidth / 2)
        }

        val zeroY = valueToY(BigDecimal.ZERO)
        val currencyFormatter = remember { CurrencyReprUi.CurrencyNumberFormatter() }
        val chartSpec = remember(precision) { CurrencyDisplaySpec(precision = precision, shortenThreshold = 10) }

        val gridLineColor = AppColors.outlineVariant
        val selectionLineColor = ChartZeroLineColor

        fun findIndex(offsetX: Float): Int {
            val total = groups.size
            if (total == 0) return -1
            val slotWidth = chartWidth / total
            return ((offsetX - leftPadding) / slotWidth).toInt().coerceIn(0, total - 1)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription =
                        "Bar chart showing ${groups.size} periods. Drag or tap for details."
                }
                .pointerInput(groups) {
                    detectDragGestures(
                        onDragStart = { offset -> selectedIndex = findIndex(offset.x) },
                        onDrag = { change, _ -> selectedIndex = findIndex(change.position.x) },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(groups) {
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
                    color = android.graphics.Color.argb(140, 158, 158, 158)
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
                            currencyFormatter.format(amount = tick, currencyCode = null, spec = chartSpec),
                            leftPadding - 8.dp.toPx(),
                            yPos + 3.dp.toPx(),
                            labelPaint
                        )
                    }
                    tick = tick.add(niceStep)
                }
            }

            // 2. Zero Line
            drawLine(
                color = ChartZeroLineColor,
                start = Offset(leftPadding, zeroY),
                end = Offset(widthPx - rightPadding, zeroY),
                strokeWidth = (dims.spacingXs / 4).toPx()
            )

            // 3. Draw Bars (Simple or Stacked)
            val slotWidth = chartWidth / groups.size.coerceAtLeast(1)
            val barWidth = slotWidth * 0.7f

            val total = groups.size
            val firstValidIdx = groups.indexOfFirst { it.label.isNotBlank() }.takeIf { it != -1 } ?: 0
            val lastValidIdx = groups.indexOfLast { it.label.isNotBlank() }.takeIf { it != -1 } ?: (total - 1)
            
            val indicesToShow = mutableSetOf<Int>()
            if (total > 0) {
                indicesToShow.add(firstValidIdx)
                indicesToShow.add(lastValidIdx)
                val range = lastValidIdx - firstValidIdx
                if (range > 0) {
                    val interval = range.toFloat() / 6f
                    for (i in 1..5) {
                        indicesToShow.add(firstValidIdx + Math.round(i * interval))
                    }
                }
            }

            groups.forEachIndexed { groupIndex, group ->
                val centerX = indexToX(groupIndex, groups.size)

                var currentPosAccumulator = BigDecimal.ZERO
                var currentNegAccumulator = BigDecimal.ZERO

                group.bars.forEach { bar ->
                    val isPositive = bar.value >= BigDecimal.ZERO

                    val startVal = if (isPositive) currentPosAccumulator else currentNegAccumulator
                    val endVal = startVal + bar.value

                    // Correct stacking animation: both start and end points of the segment are scaled from baseline
                    val animatedStart =
                        startVal.multiply(BigDecimal.valueOf(animationProgress.value.toDouble()))
                    val animatedEnd =
                        endVal.multiply(BigDecimal.valueOf(animationProgress.value.toDouble()))

                    val startY = valueToY(animatedStart)
                    val endY = valueToY(animatedEnd)

                    val top = if (isPositive) endY else startY
                    val bottom = if (isPositive) startY else endY

                    val radiusPx = if (roundedBars) with(density) { 4.dp.toPx() } else 0f
                    drawRoundRect(
                        color = bar.color.copy(alpha = 0.85f),
                        topLeft = Offset(centerX - barWidth / 2, top),
                        size = Size(barWidth, (bottom - top).coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )

                    if (isPositive) currentPosAccumulator = endVal
                    else currentNegAccumulator = endVal
                }

                // 4. X-Axis Labels
                if (showXAxisLabels) {
                    val labelPaint = AndroidPaint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.argb(180, 158, 158, 158)
                        textAlign = AndroidPaint.Align.CENTER
                        textSize = 9.sp.toPx()
                    }

                    if (groupIndex in indicesToShow) {
                        drawContext.canvas.nativeCanvas.drawText(
                            group.label,
                            centerX,
                            heightPx - bottomPadding + 16.dp.toPx(),
                            labelPaint
                        )
                    }
                }
            }

            // 5. Selection Highlight
            selectedIndex?.let { index ->
                if (index in groups.indices) {
                    val x = indexToX(index, groups.size)
                    drawLine(
                        color = selectionLineColor,
                        start = Offset(x, topPadding),
                        end = Offset(x, heightPx - bottomPadding),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }
            }
        }

        // 6. Tooltip
        selectedIndex?.let { index ->
            val group = groups.getOrNull(index) ?: return@let
            val x = indexToX(index, groups.size)

            val tooltipWidth = 180.dp
            val tooltipHeight = (40 + group.bars.size * 25).dp.coerceAtMost(240.dp)
            val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
            val tooltipHeightPx = with(density) { tooltipHeight.toPx() }

            val xPos =
                (x - tooltipWidthPx / 2).coerceIn(0f, (widthPx - tooltipWidthPx).coerceAtLeast(0f))
            val yPos = (topPadding).coerceIn(0f, (heightPx - tooltipHeightPx).coerceAtLeast(0f))

            AeonChartTooltip(
                modifier = Modifier
                    .offset(x = (xPos / density.density).dp, y = (yPos / density.density).dp)
                    .width(tooltipWidth)
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                title = group.label
            ) {
                group.bars.forEach { bar ->
                    AeonTooltipEntry(
                        label = bar.label,
                        value = currencyFormatter.format(
                            amount = bar.value,
                            currencyCode = currency,
                            spec = chartSpec,
                        ),
                        color = bar.color
                    )
                }
            }
        }
    }
}

/**
 * Backward compatible overload for simple bar charts.
 */
@Composable
fun AeonBarChart(
    data: List<Pair<String, BigDecimal>>,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    precision: Int = CurrencyService.precision,
    barColor: Color = ChartColorBlue,
    animate: Boolean = true,
    roundedBars: Boolean = false
) {
    val groups = remember(data, barColor) {
        data.map { (label, value) ->
            AeonBarChartGroup(label, listOf(AeonBarChartBar(value, "Value", barColor)))
        }
    }
    AeonBarChart(
        groups = groups,
        modifier = modifier,
        currency = currency,
        precision = precision,
        animate = animate,
        roundedBars = roundedBars
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Simple Bar Chart")
@Composable
fun AeonBarChartSimplePreview() {
    OssWalletTheme {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(16.dp)) {
            AeonBarChart(
                data = listOf(
                    "Mon" to BigDecimal("120.00"),
                    "Tue" to BigDecimal("450.00"),
                    "Wed" to BigDecimal("-100.00"),
                    "Thu" to BigDecimal("280.00"),
                    "Fri" to BigDecimal("600.00"),
                    "Sat" to BigDecimal("350.00"),
                    "Sun" to BigDecimal("200.00")
                )
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Mixed Stacked Bar Chart"
)
@Composable
fun AeonBarChartMixedPreview() {
    OssWalletTheme {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)) {
            AeonBarChart(
                groups = listOf(
                    AeonBarChartGroup(
                        "Jan", listOf(
                            AeonBarChartBar(BigDecimal("500"), "Income", ChartColorGreen),
                            AeonBarChartBar(BigDecimal("-300"), "Rent", ChartColorRed),
                            AeonBarChartBar(BigDecimal("-100"), "Food", ChartColorOrange)
                        )
                    ),
                    AeonBarChartGroup(
                        "Feb", listOf(
                            AeonBarChartBar(BigDecimal("200"), "Bonus", ChartColorGreen),
                            AeonBarChartBar(BigDecimal("-800"), "Emergency", ChartColorRed),
                            AeonBarChartBar(BigDecimal("-100"), "Misc", ChartColorOrange)
                        )
                    ),
                    AeonBarChartGroup(
                        "Mar", listOf(
                            AeonBarChartBar(BigDecimal("-500"), "Expense", ChartColorRed),
                            AeonBarChartBar(BigDecimal("-400"), "Debt", ChartColorOrange)
                        )
                    )
                )
            )
        }
    }
}
