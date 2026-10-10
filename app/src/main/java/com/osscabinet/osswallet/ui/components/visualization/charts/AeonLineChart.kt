package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorBlue
import com.osscabinet.ossWallet.ui.theme.ChartDotHighlight
import com.osscabinet.ossWallet.ui.theme.ChartZeroLineColor
import com.osscabinet.ossWallet.ui.theme.ColorTransparent
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletPreviewTheme
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import android.graphics.Paint as AndroidPaint

/**
 * High-fidelity, interactive line chart with area gradients and dashed grids.
 */
@Composable
fun AeonLineChart(
    series: List<AeonLineChartSeries>,
    modifier: Modifier = Modifier,
    currency: String,
    precision: Int = CurrencyService.precision,
    showGrid: Boolean = true,
    showPeriodBars: Boolean = true,
    animate: Boolean = false,
    lineThicknessDp: Float = 0f,
    showXAxisLabels: Boolean = false,
    // When > 0, all series share this x-range denominator so shorter series don't stretch to fill the chart.
    totalXSlots: Int = 0,
    // When non-empty, these labels are used for the x-axis instead of deriving from the first series.
    xAxisLabels: List<String?> = emptyList(),
    markers: List<AeonChartMarker> = emptyList(),
    onNavigateToRecords: ((AeonLineChartPoint) -> Unit)? = null,
    scrollable: Boolean = false,
    visibleSlots: Int = DEFAULT_VISIBLE_BARS
) {
    if (series.isEmpty() || series.all { it.dataPoints.isEmpty() }) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = AppTextStyles.caption)
        }
        return
    }

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current

    val animationProgress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(series) {
        if (animate) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, tween(1000))
        } else {
            animationProgress.snapTo(1f)
        }
    }

    val verticalGuideColor = ChartZeroLineColor
    val dims = AeonTheme.dimensions

    val currencyFormatter = remember { CurrencyReprUi.CurrencyNumberFormatter() }
    val chartSpec =
        remember(precision) { CurrencyDisplaySpec(precision = precision, shortenThreshold = 10) }

    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val leftPadding = with(density) { 48.dp.toPx() }
        val xLabelHeight = with(density) { 16.dp.toPx() }
        val bottomPadding =
            with(density) { dims.spacingXl.toPx() } + if (showXAxisLabels) xLabelHeight else 0f
        val topPadding = with(density) { dims.spacingLg.toPx() }
        val rightPadding = with(density) { dims.spacingMd.toPx() }

        val chartWidth = w - leftPadding - rightPadding
        val chartHeight = h - bottomPadding - topPadding

        // Dynamic scaling logic
        val allPoints = series.flatMap { it.dataPoints.map { p -> p.y } }
        val confidencePoints = series.flatMap { s ->
            s.confidenceIntervals?.flatMap { listOf(it.first, it.second) } ?: emptyList()
        }
        val allValues = allPoints + confidencePoints

        if (allValues.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data available", style = AppTextStyles.caption)
            }
            return@BoxWithConstraints
        }

        val effectiveTotal =
            if (totalXSlots > 0) totalXSlots else (series.firstOrNull()?.dataPoints?.size ?: 1)

        // Scroll mode: `visibleSlots` slots fill the viewport and the rest scrolls in from the left.
        val isScrollable = scrollable && effectiveTotal > visibleSlots.coerceAtLeast(1)
        val scrollState = rememberAeonChartScrollState(effectiveTotal)
        val slotWidth =
            if (isScrollable) chartWidth / visibleSlots.coerceAtLeast(1)
            else chartWidth / effectiveTotal.coerceAtLeast(1)
        val maxScrollPx = if (isScrollable) (slotWidth * effectiveTotal - chartWidth).coerceAtLeast(0f) else 0f
        val scrollX = if (isScrollable) scrollState.offsetPx.coerceIn(0f, maxScrollPx) else 0f
        val yZoom = if (isScrollable) scrollState.yZoom else 1f
        val scrollableState = rememberAeonChartScrollable(scrollState, maxScrollPx) { selectedIndex = -1 }

        // Y-zoom shrinks the visible range around its centre so the data stays in view.
        val dataMinY = allValues.minOf { it }
        val dataMaxY = allValues.maxOf { it }
        val zoomDivisor = BigDecimal.valueOf(yZoom.toDouble())
        // Anchor the zoom on the primary line's latest window (where the chart opens) so it stays put while scrolling.
        val latestFirstIndex = (effectiveTotal - visibleSlots.coerceAtLeast(1)).coerceAtLeast(0)
        // The first series with data is the primary line; the others are comparisons against it.
        val anchorValues = series.firstOrNull { it.dataPoints.isNotEmpty() }
            ?.dataPoints?.drop(latestFirstIndex)?.map { p -> p.y }
            .orEmpty()
            .ifEmpty { allValues }
        val anchorMinY = anchorValues.minOf { it }
        val anchorMaxY = anchorValues.maxOf { it }
        val centerY = (anchorMinY + anchorMaxY).divide(BigDecimal(2), 4, RoundingMode.HALF_UP)
        val minY = if (yZoom == 1f) dataMinY
            else centerY - (centerY - dataMinY).divide(zoomDivisor, 4, RoundingMode.HALF_UP)
        val maxY = if (yZoom == 1f) dataMaxY
            else centerY + (dataMaxY - centerY).divide(zoomDivisor, 4, RoundingMode.HALF_UP)

        // Calculate a "nice" step size based on the data range
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

        // Align boundaries and add 1-step buffer to ensure ticks strictly outside data
        val effectiveMinY = minY.divide(niceStep, 0, RoundingMode.FLOOR).multiply(niceStep)
        val effectiveMaxY = maxY.divide(niceStep, 0, RoundingMode.CEILING).multiply(niceStep)
        val effectiveRangeY =
            (effectiveMaxY - effectiveMinY).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE

        fun valueToY(value: BigDecimal): Float {
            val normalized =
                (value - effectiveMinY).divide(effectiveRangeY, 10, RoundingMode.HALF_UP).toFloat()
            return topPadding + chartHeight - (normalized * chartHeight)
        }

        val zeroY = valueToY(BigDecimal.ZERO)

        val isDark = LocalThemeMode.current.isDarkTheme
        val gridLineColor = AppColors.outlineVariant.copy(alpha = if (isDark) 0.15f else 0.4f)
        val periodBarColor = AppColors.outlineVariant.copy(alpha = if (isDark) 0.05f else 0.1f)
        val labelColor = AppColors.contentSecondary.copy(alpha = 0.6f)
        val axisLabelColor = AppColors.contentSecondary.copy(alpha = 0.8f)

        fun indexToX(index: Float, total: Int): Float {
            return leftPadding + (index + 0.5f) * slotWidth - scrollX
        }

        fun findClosestIndex(offsetX: Float): Int {
            if (series.isEmpty()) return -1

            // Use the first series that has data points
            val referenceSeries = series.firstOrNull { it.dataPoints.isNotEmpty() } ?: return -1
            val totalPoints = referenceSeries.dataPoints.size

            var closestIndex = -1
            var minDistance = Float.MAX_VALUE

            for (i in 0 until totalPoints) {
                val dx = indexToX(i.toFloat(), totalPoints)
                val distance = abs(offsetX - dx)
                if (distance < minDistance) {
                    minDistance = distance
                    closestIndex = i
                }
            }

            return if (minDistance < 100f) closestIndex else -1
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Line chart showing ${series.joinToString { it.name }}. " +
                            if (isScrollable) "Swipe to scroll, tap to explore data points."
                            else "Drag or tap to explore data points."
                }
                .then(
                    if (isScrollable) Modifier.scrollable(scrollableState, Orientation.Horizontal)
                    else Modifier.pointerInput(series) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedIndex = findClosestIndex(offset.x)
                            },
                            onDrag = { change, _ ->
                                selectedIndex = findClosestIndex(change.position.x)
                            },
                            onDragEnd = { selectedIndex = -1 },
                            onDragCancel = { selectedIndex = -1 }
                        )
                    }
                )
                // Keyed on scrollX so the tap handler never maps taps with a stale scroll offset.
                .pointerInput(series, scrollX) {
                    detectTapGestures(
                        onTap = { offset ->
                            selectedIndex = findClosestIndex(offset.x)
                        }
                    )
                }
        ) {
            // 1. Draw Grid System and Y-Axis Labels
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
                    if (yPos >= topPadding - 1f && yPos <= topPadding + chartHeight + 1f) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(leftPadding, yPos),
                            end = Offset(w - rightPadding, yPos),
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

            clipRect(
                left = if (isScrollable) leftPadding else 0f,
                right = if (isScrollable) w - rightPadding else w
            ) {
                // 1.5 Draw Period Background Bars
                if (showPeriodBars) {
                    if (effectiveTotal > 0) {
                        val periodBarWidth = slotWidth * 0.95f
                        for (i in 0 until effectiveTotal) {
                            val centerX = indexToX(i.toFloat(), effectiveTotal)
                            drawRect(
                                color = periodBarColor,
                                topLeft = Offset(centerX - periodBarWidth / 2, topPadding),
                                size = Size(periodBarWidth, chartHeight)
                            )
                        }
                    }
                }

                clipRect(top = if (isScrollable) topPadding else 0f, bottom = if (isScrollable) topPadding + chartHeight else h) {
                    // 2. Draw Data Series
                    series.forEach { s ->
                        // Draw Confidence Interval (Shaded Area)
                        s.confidenceIntervals?.let { intervals ->
                            if (intervals.size == s.dataPoints.size) {
                                val confidencePath = Path()
                                // Top boundary
                                s.dataPoints.forEachIndexed { index, _ ->
                                    val x = indexToX(index.toFloat(), s.dataPoints.size)
                                    val y = valueToY(intervals[index].second)
                                    val animY = zeroY + (y - zeroY) * animationProgress.value
                                    if (index == 0) confidencePath.moveTo(
                                        x,
                                        animY
                                    ) else confidencePath.lineTo(x, animY)
                                }
                                // Bottom boundary (reverse)
                                for (index in (s.dataPoints.size - 1) downTo 0) {
                                    val x = indexToX(index.toFloat(), s.dataPoints.size)
                                    val y = valueToY(intervals[index].first)
                                    val animY = zeroY + (y - zeroY) * animationProgress.value
                                    confidencePath.lineTo(x, animY)
                                }
                                confidencePath.close()
                                drawPath(
                                    path = confidencePath,
                                    color = s.color.copy(alpha = 0.15f)
                                )
                            }
                        }

                        val linePath = Path()
                        val areaPath = Path()
                        val pts = s.dataPoints
                        val n = pts.size

                        pts.forEachIndexed { index, point ->
                            val x = indexToX(index.toFloat(), n)
                            val y = valueToY(point.y)
                            val animY = zeroY + (y - zeroY) * animationProgress.value

                            if (index == 0) {
                                linePath.moveTo(x, animY)
                                areaPath.moveTo(x, zeroY)
                                areaPath.lineTo(x, animY)
                            } else {
                                // Smooth cubic bezier with reduced tension (0.3) — moderately curved
                                val prevX = indexToX((index - 1).toFloat(), n)
                                val prevY = run {
                                    val py = valueToY(pts[index - 1].y)
                                    zeroY + (py - zeroY) * animationProgress.value
                                }
                                val dx = x - prevX
                                val cp1x = prevX + dx * 0.3f
                                val cp2x = x - dx * 0.3f
                                linePath.cubicTo(cp1x, prevY, cp2x, animY, x, animY)
                                areaPath.cubicTo(cp1x, prevY, cp2x, animY, x, animY)
                            }

                            if (index == n - 1) {
                                areaPath.lineTo(x, zeroY)
                                areaPath.close()
                            }
                        }

                        if (s.fillGradient) {
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(s.color.copy(alpha = 0.2f), ColorTransparent),
                                    startY = topPadding,
                                    endY = topPadding + chartHeight
                                )
                            )
                        }

                        val strokePx = if (lineThicknessDp > 0f) {
                            with(density) { lineThicknessDp.dp.toPx() }
                        } else {
                            (dims.spacingXs * 0.9f).toPx()
                        }
                        drawPath(
                            path = linePath,
                            color = s.color,
                            style = Stroke(
                                width = strokePx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = if (s.isDashed) PathEffect.dashPathEffect(
                                    floatArrayOf(
                                        12f,
                                        8f
                                    )
                                ) else null
                            )
                        )

                        if (s.showEndDot && pts.isNotEmpty()) {
                            val lastIdx = pts.size - 1
                            val ex = indexToX(lastIdx.toFloat(), n)
                            val ey = run {
                                val raw = valueToY(pts[lastIdx].y)
                                zeroY + (raw - zeroY) * animationProgress.value
                            }
                            drawCircle(
                                color = ChartDotHighlight,
                                radius = strokePx * 1.5f,
                                center = Offset(ex, ey)
                            )
                            drawCircle(
                                color = s.color,
                                radius = strokePx * 1.5f,
                                center = Offset(ex, ey),
                                style = Stroke(width = strokePx * 0.6f)
                            )
                        }
                    }
                }

                // X-axis labels — prefer explicit xAxisLabels list, fall back to first series labels
                if (showXAxisLabels || xAxisLabels.isNotEmpty()) {
                    val labelPaint = AndroidPaint().apply {
                        isAntiAlias = true
                        color = axisLabelColor.toArgb()
                        textAlign = AndroidPaint.Align.CENTER
                        textSize = 9.sp.toPx()
                    }
                    val labelY = h - bottomPadding + with(density) { 14.dp.toPx() }

                    if (xAxisLabels.isNotEmpty()) {
                        val total = xAxisLabels.size

                        // Find first and last non-null labels to define the range for 7 labels
                        val firstValidIdx = xAxisLabels.indexOfFirst { it != null }
                        val lastValidIdx = xAxisLabels.indexOfLast { it != null }

                        val indicesToShow = mutableSetOf<Int>()
                        if (isScrollable) {
                            // Scrolling charts label every slot.
                            indicesToShow.addAll(xAxisLabels.indices)
                        } else if (firstValidIdx != -1 && lastValidIdx != -1) {
                            indicesToShow.add(firstValidIdx)
                            indicesToShow.add(lastValidIdx)

                            val validRange = lastValidIdx - firstValidIdx
                            if (validRange > 0) {
                                val interval = validRange.toFloat() / 6f
                                for (i in 1..5) {
                                    indicesToShow.add(firstValidIdx + Math.round(i * interval))
                                }
                            }
                        }

                        xAxisLabels.forEachIndexed { index, lbl ->
                            if (lbl != null && indicesToShow.contains(index)) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    lbl,
                                    indexToX(index.toFloat(), total),
                                    labelY,
                                    labelPaint
                                )
                            }
                        }
                    } else {
                        val labelSeries = series.firstOrNull { it.dataPoints.isNotEmpty() }
                        if (labelSeries != null) {
                            val total = labelSeries.dataPoints.size

                            val firstValidIdx = labelSeries.dataPoints.indexOfFirst { it.label != null }
                            val lastValidIdx = labelSeries.dataPoints.indexOfLast { it.label != null }

                            val indicesToShow = mutableSetOf<Int>()
                            if (isScrollable) {
                                indicesToShow.addAll(labelSeries.dataPoints.indices)
                            } else if (firstValidIdx != -1 && lastValidIdx != -1) {
                                indicesToShow.add(firstValidIdx)
                                indicesToShow.add(lastValidIdx)

                                val validRange = lastValidIdx - firstValidIdx
                                if (validRange > 0) {
                                    val interval = validRange.toFloat() / 6f
                                    for (i in 1..5) {
                                        indicesToShow.add(firstValidIdx + Math.round(i * interval))
                                    }
                                }
                            }

                            labelSeries.dataPoints.forEachIndexed { index, point ->
                                val lbl = point.label ?: return@forEachIndexed
                                if (indicesToShow.contains(index)) {
                                    drawContext.canvas.nativeCanvas.drawText(
                                        lbl,
                                        indexToX(index.toFloat(), total),
                                        labelY,
                                        labelPaint
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Draw Markers
                if (markers.isNotEmpty()) {
                    val sortedMarkers = markers.sortedBy { it.x }
                    // Gap below which adjacent marker labels are staggered vertically
                    val markerMinGapPx = with(density) { 80.dp.toPx() }
                    val markerLabelHeightPx = with(density) { 14.dp.toPx() }
                    // Top baseline offset for the first stagger level
                    val markerLabelTopOffsetPx = with(density) { 10.dp.toPx() }
                    // Horizontal nudge between the marker line and its label
                    val markerLabelNudgePx = with(density) { 4.dp.toPx() }
                    // x threshold (fraction of chartWidth) past which label right-aligns
                    val markerEndThreshold = 0.8f

                    val markerLabelPaint = AndroidPaint().apply {
                        textSize = with(density) { 9.sp.toPx() }
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.DEFAULT,
                            android.graphics.Typeface.BOLD
                        )
                    }

                    var lastMarkerX = -1000f
                    var currentLevel = 0

                    sortedMarkers.forEach { marker ->
                        // marker.x is in the same index space as totalXSlots; `total` arg is unused when totalXSlots > 0
                        val x = indexToX(marker.x, totalXSlots.coerceAtLeast(1))
                        val markerColor = marker.color.copy(alpha = 0.6f)

                        currentLevel =
                            if (abs(x - lastMarkerX) < markerMinGapPx) (currentLevel + 1) % 3 else 0
                        lastMarkerX = x

                        drawLine(
                            color = markerColor,
                            start = Offset(x, topPadding),
                            end = Offset(x, topPadding + chartHeight),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (marker.isDashed) PathEffect.dashPathEffect(
                                floatArrayOf(
                                    10f,
                                    10f
                                )
                            ) else null
                        )

                        val isAtEnd = x > leftPadding + chartWidth * markerEndThreshold
                        markerLabelPaint.color = markerColor.toArgb()
                        markerLabelPaint.textAlign =
                            if (isAtEnd) AndroidPaint.Align.RIGHT else AndroidPaint.Align.LEFT

                        val labelY =
                            topPadding + markerLabelTopOffsetPx + (currentLevel * markerLabelHeightPx)
                        drawContext.canvas.nativeCanvas.drawText(
                            marker.label,
                            if (isAtEnd) x - markerLabelNudgePx else x + markerLabelNudgePx,
                            labelY,
                            markerLabelPaint
                        )
                    }
                }

                if (selectedIndex != -1) {
                    val x = indexToX(selectedIndex.toFloat(), series[0].dataPoints.size)
                    drawLine(
                        color = verticalGuideColor,
                        start = Offset(x, topPadding),
                        end = Offset(x, topPadding + chartHeight),
                        strokeWidth = (dims.spacingXs / 4).toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    // Draw focus dots for all series at this index
                    series.forEach { s ->
                        if (selectedIndex < s.dataPoints.size) {
                            val p = s.dataPoints[selectedIndex]
                            val y = valueToY(p.y)
                            drawCircle(
                                color = ChartDotHighlight,
                                radius = (dims.spacingXs * 1.25f).toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = s.color,
                                radius = (dims.spacingXs * 1.25f).toPx(),
                                center = Offset(x, y),
                                style = Stroke(width = (dims.spacingXs / 2).toPx())
                            )
                        }
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

        if (selectedIndex != -1) {
            val referenceSeries = series.firstOrNull { selectedIndex < it.dataPoints.size }
            if (referenceSeries != null) {
                val totalPoints = referenceSeries.dataPoints.size
                val x = indexToX(selectedIndex.toFloat(), totalPoints)
                val label = referenceSeries.dataPoints[selectedIndex].label

                // Boundary-aware multi-series tooltip
                val tooltipWidth = 180.dp
                val tooltipHeight =
                    (40 + series.size * 25).dp // Dynamic height based on number of series
                val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
                val tooltipHeightPx = with(density) { tooltipHeight.toPx() }

                val xPos =
                    (x - tooltipWidthPx / 2).coerceIn(0f, (w - tooltipWidthPx).coerceAtLeast(0f))
                // Show tooltip above the middle of the chart
                val yPos = (topPadding).coerceIn(0f, (h - tooltipHeightPx).coerceAtLeast(0f))

                AeonChartTooltip(
                    modifier = Modifier
                        .offset(x = (xPos / density.density).dp, y = (yPos / density.density).dp)
                        .width(tooltipWidth),
                    title = label
                ) {
                    series.forEach { s ->
                        if (selectedIndex < s.dataPoints.size) {
                            val p = s.dataPoints[selectedIndex]
                            AeonTooltipEntry(
                                label = s.name,
                                value = currencyFormatter.format(
                                    amount = p.y,
                                    currencyCode = currency,
                                    spec = chartSpec,
                                ),
                                color = s.color
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Theme")
@Composable
fun AeonLineChartLightPreview() {
    OssWalletPreviewTheme(theme = "Light") {
        Surface(color = AppColors.background) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(16.dp)
            ) {
                AeonLineChart(
                    series = listOf(
                        AeonLineChartSeries(
                            name = "Balance",
                            color = ChartColorBlue,
                            dataPoints = listOf(
                                AeonLineChartPoint(0f, BigDecimal("1200.00"), "Jan"),
                                AeonLineChartPoint(1f, BigDecimal("1500.00"), "Feb"),
                                AeonLineChartPoint(2f, BigDecimal("1100.00"), "Mar")
                            )
                        )
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
fun AeonLineChartDarkPreview() {
    OssWalletPreviewTheme(theme = "Dark") {
        Surface(color = AppColors.background) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(16.dp)
            ) {
                AeonLineChart(
                    series = listOf(
                        AeonLineChartSeries(
                            name = "Balance",
                            color = ChartColorBlue,
                            dataPoints = listOf(
                                AeonLineChartPoint(0f, BigDecimal("1200.00"), "Jan"),
                                AeonLineChartPoint(1f, BigDecimal("1500.00"), "Feb"),
                                AeonLineChartPoint(2f, BigDecimal("1100.00"), "Mar")
                            )
                        )
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
fun AeonLineChartAmoledPreview() {
    OssWalletPreviewTheme(theme = "Amoled") {
        Surface(color = AppColors.background) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(16.dp)
            ) {
                AeonLineChart(
                    series = listOf(
                        AeonLineChartSeries(
                            name = "Balance",
                            color = ChartColorBlue,
                            dataPoints = listOf(
                                AeonLineChartPoint(0f, BigDecimal("1200.00"), "Jan"),
                                AeonLineChartPoint(1f, BigDecimal("1500.00"), "Feb"),
                                AeonLineChartPoint(2f, BigDecimal("1100.00"), "Mar")
                            )
                        )
                    ),
                    currency = "USD",
                    animate = false
                )
            }
        }
    }
}
