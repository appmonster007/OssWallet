package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorBlue
import com.osscabinet.ossWallet.ui.theme.ChartColorGreen
import com.osscabinet.ossWallet.ui.theme.ChartColorOrange
import com.osscabinet.ossWallet.ui.theme.ChartColorRed
import com.osscabinet.ossWallet.ui.theme.ChartZeroLineColor
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletPreviewTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import java.math.BigDecimal
import java.math.RoundingMode
import android.graphics.Paint as AndroidPaint

internal const val DEFAULT_VISIBLE_BARS = 7

/**
 * High-fidelity, interactive bar chart supporting both simple and stacked visualizations.
 * Refined with professional grid logic and dynamic scaling.
 */
@Composable
fun AeonBarChart(
    groups: List<AeonBarChartGroup>,
    modifier: Modifier = Modifier,
    currency: String,
    precision: Int = CurrencyService.precision,
    animate: Boolean = false,
    showGrid: Boolean = true,
    showPeriodBars: Boolean = true,
    showXAxisLabels: Boolean = true,
    roundedBars: Boolean = false,
    scrollable: Boolean = false,
    visibleBars: Int = DEFAULT_VISIBLE_BARS
) {
    // A scrollable chart keeps its empty buckets so the x-axis still shows every period.
    if (groups.isEmpty() || (!scrollable && groups.all { it.bars.isEmpty() })) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = AppTextStyles.caption)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val dims = AeonTheme.dimensions

    val animationProgress = remember { Animatable(if (animate) 0f else 1f) }
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

        val leftPadding = with(density) { 48.dp.toPx() }
        val rightPadding = with(density) { dims.spacingMd.toPx() }
        val topPadding = with(density) { dims.spacingLg.toPx() }
        val xLabelHeight = with(density) { 16.dp.toPx() }
        val bottomPadding =
            with(density) { dims.spacingXl.toPx() } + (if (showXAxisLabels) xLabelHeight else 0f)

        val chartWidth = widthPx - leftPadding - rightPadding
        val chartHeight = heightPx - topPadding - bottomPadding

        // Scroll mode: `visibleBars` slots fill the viewport and the rest scrolls in from the left.
        val isScrollable = scrollable && groups.size > visibleBars.coerceAtLeast(1)
        val scrollState = rememberAeonChartScrollState(groups.size)
        val slotWidth =
            if (isScrollable) chartWidth / visibleBars.coerceAtLeast(1)
            else chartWidth / groups.size.coerceAtLeast(1)
        val maxScrollPx = if (isScrollable) (slotWidth * groups.size - chartWidth).coerceAtLeast(0f) else 0f
        val scrollX = if (isScrollable) scrollState.offsetPx.coerceIn(0f, maxScrollPx) else 0f
        val yZoom = if (isScrollable) scrollState.yZoom else 1f
        val scrollableState = rememberAeonChartScrollable(scrollState, maxScrollPx) { selectedIndex = null }

        // Calculate limits for stacked or simple bars
        val groupTotals = groups.map { g ->
            val pos = g.bars.filter { it.value > BigDecimal.ZERO }.sumOf { it.value }
            val neg = g.bars.filter { it.value < BigDecimal.ZERO }.sumOf { it.value }
            pos to neg
        }

        val zoomDivisor = BigDecimal.valueOf(yZoom.toDouble())
        val minY = groupTotals.minOf { it.second }.coerceAtMost(BigDecimal.ZERO)
            .divide(zoomDivisor, 4, RoundingMode.HALF_UP)
        val emptyCeiling = if (isScrollable && groupTotals.all { it.first.signum() == 0 && it.second.signum() == 0 }) {
            BigDecimal.valueOf(EMPTY_CHART_MAX)
        } else BigDecimal.ONE
        val maxY = groupTotals.maxOf { it.first }.coerceAtLeast(emptyCeiling)
            .divide(zoomDivisor, 4, RoundingMode.HALF_UP)
            .coerceAtLeast(BigDecimal.ONE)

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
            return leftPadding + (index * slotWidth) + (slotWidth / 2) - scrollX
        }

        val zeroY = valueToY(BigDecimal.ZERO)
        val currencyFormatter = remember { CurrencyReprUi.CurrencyNumberFormatter() }
        val chartSpec = remember(precision) {
            CurrencyDisplaySpec(
                precision = precision,
                shortenThreshold = 10
            )
        }

        val isDark = LocalThemeMode.current.isDarkTheme
        val gridLineColor = AppColors.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.4f)
        val periodBarColor = AppColors.outlineVariant.copy(alpha = if (isDark) 0.05f else 0.1f)
        val selectionLineColor = ChartZeroLineColor
        val labelColor = AppColors.contentSecondary.copy(alpha = 0.6f)
        val axisLabelColor = AppColors.contentSecondary.copy(alpha = 0.8f)

        fun findIndex(offsetX: Float): Int {
            val total = groups.size
            if (total == 0) return -1
            return ((offsetX - leftPadding + scrollX) / slotWidth).toInt().coerceIn(0, total - 1)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription =
                        if (isScrollable) "Bar chart showing ${groups.size} periods. Swipe to scroll, tap for details."
                        else "Bar chart showing ${groups.size} periods. Drag or tap for details."
                }
                .then(
                    if (isScrollable) Modifier.scrollable(scrollableState, Orientation.Horizontal)
                    else Modifier.pointerInput(groups) {
                        detectDragGestures(
                            onDragStart = { offset -> selectedIndex = findIndex(offset.x) },
                            onDrag = { change, _ -> selectedIndex = findIndex(change.position.x) },
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null }
                        )
                    }
                )
                // Keyed on scrollX so the tap handler never maps taps with a stale scroll offset.
                .pointerInput(groups, scrollX) {
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

            // Everything below scrolls; keep it out of the pinned Y-axis gutter.
            clipRect(
                left = if (isScrollable) leftPadding else 0f,
                right = if (isScrollable) widthPx - rightPadding else widthPx
            ) {
                val plotTop = topPadding
                val plotBottom = heightPx - bottomPadding
                fun isOffscreen(x: Float) = isScrollable && (x + slotWidth < leftPadding || x - slotWidth > widthPx)

                // 1.5 Draw Period Background Bars
                if (showPeriodBars && groups.isNotEmpty()) {
                    val totalSlots = groups.size
                    groups.forEachIndexed { index, _ ->
                        val slotCenterX = indexToX(index, totalSlots)
                        if (isOffscreen(slotCenterX)) return@forEachIndexed
                        val barWidth = slotWidth * 0.95f
                        drawRect(
                            color = periodBarColor,
                            topLeft = Offset(slotCenterX - barWidth / 2, topPadding),
                            size = Size(barWidth, chartHeight)
                        )
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
                val barWidth = slotWidth * 0.85f

                val total = groups.size
                val firstValidIdx =
                    groups.indexOfFirst { it.label.isNotBlank() }.takeIf { it != -1 } ?: 0
                val lastValidIdx =
                    groups.indexOfLast { it.label.isNotBlank() }.takeIf { it != -1 } ?: (total - 1)

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
                    if (isOffscreen(centerX)) return@forEachIndexed

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

                        val rawTop = if (isPositive) endY else startY
                        val rawBottom = if (isPositive) startY else endY
                        // Y-zoom can push bars past the plot area; clamp, and skip the ones fully outside.
                        val top = if (isScrollable) rawTop.coerceIn(plotTop, plotBottom) else rawTop
                        val bottom = if (isScrollable) rawBottom.coerceIn(plotTop, plotBottom) else rawBottom
                        val fullyOutside = isScrollable && (rawBottom < plotTop || rawTop > plotBottom)

                        val radiusPx = if (roundedBars) with(density) { 4.dp.toPx() } else 0f
                        if (!fullyOutside) drawRoundRect(
                            color = bar.color.copy(alpha = 0.85f),
                            topLeft = Offset(centerX - barWidth / 2, top),
                            size = Size(barWidth, (bottom - top).coerceAtLeast(1f)),
                            cornerRadius = CornerRadius(radiusPx, radiusPx)
                        )

                        if (isPositive) currentPosAccumulator = endVal
                        else currentNegAccumulator = endVal
                    }

                    if (showXAxisLabels) {
                        val labelPaint = AndroidPaint().apply {
                            isAntiAlias = true
                            color = axisLabelColor.toArgb()
                            textAlign = AndroidPaint.Align.CENTER
                            textSize = 9.sp.toPx()
                        }

                        // Scrolling charts label every bar; the squeezed chart thins its labels.
                        val showLabel = if (isScrollable) group.label.isNotBlank() else groupIndex in indicesToShow
                        if (showLabel) {
                            drawContext.canvas.nativeCanvas.drawText(
                                group.label,
                                centerX,
                                heightPx - bottomPadding + with(density) { 14.dp.toPx() },
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
                            strokeWidth = (dims.spacingXs / 4).toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }
                }
            }
        }

        if (isScrollable) {
            AeonChartYAxisZoomZone(scrollState, with(density) { leftPadding.toDp() })
            if (yZoom > 1.02f) {
                Text(
                    text = "×${"%.1f".format(yZoom)}",
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary,
                    modifier = Modifier.align(Alignment.TopStart)
                )
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
    currency: String,
    precision: Int = CurrencyService.precision,
    barColor: Color = ChartColorBlue,
    animate: Boolean = false,
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

@androidx.compose.ui.tooling.preview.Preview(name = "Light Theme")
@Composable
fun AeonBarChartLightPreview() {
    OssWalletPreviewTheme(theme = "Light") {
        Surface(color = AppColors.background) {
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
                        "Fri" to BigDecimal("600.00")
                    ),
                    currency = "USD",
                    animate = false
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Dark Theme")
@Composable
fun AeonBarChartDarkPreview() {
    OssWalletPreviewTheme(theme = "Dark") {
        Surface(color = AppColors.background) {
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
                        "Fri" to BigDecimal("600.00")
                    ),
                    currency = "USD",
                    animate = false
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Amoled Theme")
@Composable
fun AeonBarChartAmoledPreview() {
    OssWalletPreviewTheme(theme = "Amoled") {
        Surface(color = AppColors.background) {
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
                        "Fri" to BigDecimal("600.00")
                    ),
                    currency = "USD",
                    animate = false
                )
            }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp)
        ) {
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
                ),
                currency = "USD"
            )
        }
    }
}
