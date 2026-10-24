package com.osscabinet.ossWallet.ui.components.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.osscabinet.ossWallet.ui.theme.AppColors
import java.math.BigDecimal

private val LimitDash = floatArrayOf(4f, 4f)

/**
 * Draws a polyline through (x, yFor(value)) for consecutive [xs]/[values] pairs, splitting any
 * segment that crosses [limitValue] at the exact interpolated crossing point so the portion above
 * the limit renders in [overColor] instead of [normalColor].
 */
private fun DrawScope.drawValueLine(
    xs: List<Float>,
    values: List<Float>,
    yFor: (Float) -> Float,
    limitValue: Float?,
    normalColor: Color,
    overColor: Color,
    strokeWidthPx: Float,
    pathEffect: PathEffect? = null
) {
    fun colorFor(v: Float) = if (limitValue != null && v > limitValue) overColor else normalColor
    for (i in 0 until xs.size - 1) {
        val x0 = xs[i]; val v0 = values[i]; val y0 = yFor(v0)
        val x1 = xs[i + 1]; val v1 = values[i + 1]; val y1 = yFor(v1)
        val crosses = limitValue != null && (v0 > limitValue) != (v1 > limitValue)
        if (!crosses) {
            drawLine(colorFor(v0), Offset(x0, y0), Offset(x1, y1), strokeWidthPx, StrokeCap.Round, pathEffect)
        } else {
            val t = (limitValue!! - v0) / (v1 - v0)
            val xi = x0 + t * (x1 - x0)
            val yi = yFor(limitValue)
            drawLine(colorFor(v0), Offset(x0, y0), Offset(xi, yi), strokeWidthPx, StrokeCap.Round, pathEffect)
            drawLine(colorFor(v1), Offset(xi, yi), Offset(x1, y1), strokeWidthPx, StrokeCap.Round, pathEffect)
        }
    }
}

/**
 * Minimal filled sparkline with no axes.
 *
 * Pass [onSelectedIndexChange] to make it scrubbable: dragging horizontally reports the nearest point
 * index (null when the finger lifts) and the point at [selectedIndex] is marked. Only horizontal drags
 * are claimed, so a vertical swipe still scrolls the screen.
 *
 * Pass [limit] to draw a dashed horizontal line at that value, with the portion of the line above
 * it rendered in [overLimitColor] — skipped entirely (no line, no recoloring) when every point
 * stays at or under it.
 */
@Composable
fun AeonSparkline(
    points: List<BigDecimal>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelectedIndexChange: ((Int?) -> Unit)? = null,
    contentDescription: String? = null,
    limit: BigDecimal? = null,
    overLimitColor: Color = AppColors.error,
) {
    val values = remember(points) { points.map { it.toFloat() } }
    val limitValue = remember(limit) { limit?.toFloat() }
    val currentOnChange = rememberUpdatedState(onSelectedIndexChange)
    val guideColor = AppColors.contentSecondary.copy(alpha = 0.6f)
    val limitLineColor = AppColors.contentSecondary.copy(alpha = 0.6f)

    Canvas(
        modifier = modifier
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .then(
                if (onSelectedIndexChange == null) Modifier
                else Modifier.pointerInput(values.size) {
                    fun indexAt(x: Float): Int =
                        ((x / size.width) * (values.size - 1)).let { Math.round(it) }
                            .coerceIn(0, (values.size - 1).coerceAtLeast(0))

                    detectHorizontalDragGestures(
                        onDragStart = { offset -> currentOnChange.value?.invoke(indexAt(offset.x)) },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            currentOnChange.value?.invoke(indexAt(change.position.x))
                        },
                        onDragEnd = { currentOnChange.value?.invoke(null) },
                        onDragCancel = { currentOnChange.value?.invoke(null) }
                    )
                }
            )
    ) {
        if (values.size < 2) return@Canvas
        val showLimit = limitValue != null && values.any { it > limitValue }
        val scaleValues = if (showLimit) values + limitValue!! else values
        val minV = scaleValues.min()
        val maxV = scaleValues.max()
        val range = (maxV - minV).takeIf { it != 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val topPad = size.height * 0.1f
        val usableH = size.height * 0.8f

        fun yFor(v: Float) = topPad + usableH * (1f - (v - minV) / range)

        val fillPath = Path().apply {
            moveTo(0f, yFor(values.first()))
            for (i in 1 until values.size) lineTo(i * stepX, yFor(values[i]))
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f))
            )
        )

        if (showLimit) {
            drawLine(
                color = limitLineColor,
                start = Offset(0f, yFor(limitValue!!)),
                end = Offset(size.width, yFor(limitValue)),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(LimitDash)
            )
        }

        val xs = values.indices.map { it * stepX }
        drawValueLine(
            xs = xs,
            values = values,
            yFor = ::yFor,
            limitValue = if (showLimit) limitValue else null,
            normalColor = lineColor,
            overColor = overLimitColor,
            strokeWidthPx = 2.dp.toPx()
        )

        selectedIndex?.takeIf { it in values.indices }?.let { i ->
            val x = i * stepX
            val dotColor = if (showLimit && values[i] > limitValue!!) overLimitColor else lineColor
            drawLine(color = guideColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
            drawCircle(color = dotColor, radius = 4.dp.toPx(), center = Offset(x, yFor(values[i])))
        }
    }
}

/**
 * Minimal sparkline for comparing the current period's cumulative spend so far against the
 * previous period at the same bucket positions, with an optional dashed/tinted continuation of
 * the CURRENT line (not a second series) out to a spent+planned projection. Same axis-less,
 * chrome-free look as [AeonSparkline] — deliberately not [AeonLineChart], which reserves space
 * for a y-axis and grid.
 *
 * Pass [limit] to draw a dashed horizontal line at that value, with the "current" solid line and
 * the planned tail recolored to [overLimitColor] wherever they cross it. The muted "previous
 * period" comparison line is never recolored — it's a reference, not a status being flagged for
 * the budget being viewed. Skipped entirely when nothing (including the planned tail) crosses it.
 */
@Composable
fun AeonPeriodComparisonSparkline(
    currentPoints: List<BigDecimal>,
    previousPoints: List<BigDecimal>,
    totalBuckets: Int,
    lineColor: Color,
    modifier: Modifier = Modifier,
    previousLineColor: Color = AppColors.contentSecondary,
    plannedTailEnd: BigDecimal? = null,
    limit: BigDecimal? = null,
    overLimitColor: Color = AppColors.error,
) {
    val current = remember(currentPoints) { currentPoints.map { it.toFloat() } }
    val previous = remember(previousPoints) { previousPoints.map { it.toFloat() } }
    val tail = remember(plannedTailEnd) { plannedTailEnd?.toFloat() }
    val limitValue = remember(limit) { limit?.toFloat() }
    val limitLineColor = AppColors.contentSecondary.copy(alpha = 0.6f)

    Canvas(modifier = modifier) {
        if (current.size < 2 || totalBuckets < 2) return@Canvas
        val allValues = current + previous + listOfNotNull(tail)
        val showLimit = limitValue != null && allValues.any { it > limitValue }
        val scaleValues = if (showLimit) allValues + limitValue!! else allValues
        val minV = scaleValues.min()
        val maxV = scaleValues.max()
        val range = (maxV - minV).takeIf { it != 0f } ?: 1f
        val stepX = size.width / (totalBuckets - 1)
        val topPad = size.height * 0.1f
        val usableH = size.height * 0.8f
        val effectiveLimit = if (showLimit) limitValue else null

        fun yFor(v: Float) = topPad + usableH * (1f - (v - minV) / range)

        if (previous.size >= 2) {
            val previousXs = previous.indices.map { it * stepX }
            drawValueLine(
                xs = previousXs,
                values = previous,
                yFor = ::yFor,
                limitValue = null, // reference line only, never recolored
                normalColor = previousLineColor.copy(alpha = 0.7f),
                overColor = previousLineColor.copy(alpha = 0.7f),
                strokeWidthPx = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        if (showLimit) {
            drawLine(
                color = limitLineColor,
                start = Offset(0f, yFor(limitValue!!)),
                end = Offset(size.width, yFor(limitValue)),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(LimitDash)
            )
        }

        val lastCurrentX = (current.size - 1) * stepX
        val fillPath = Path().apply {
            moveTo(0f, yFor(current.first()))
            for (i in 1 until current.size) lineTo(i * stepX, yFor(current[i]))
            lineTo(lastCurrentX, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.22f), lineColor.copy(alpha = 0f))
            )
        )

        val currentXs = current.indices.map { it * stepX }
        drawValueLine(
            xs = currentXs,
            values = current,
            yFor = ::yFor,
            limitValue = effectiveLimit,
            normalColor = lineColor,
            overColor = overLimitColor,
            strokeWidthPx = 2.5.dp.toPx()
        )

        val lastCurrentColor = if (effectiveLimit != null && current.last() > effectiveLimit) overLimitColor else lineColor

        // The planned tail is a continuation of the SAME line: it starts exactly where the solid
        // path ends, just drawn dashed and tinted — one bend, not two separate lines meeting.
        if (tail != null && totalBuckets > current.size) {
            val bend = Offset(lastCurrentX, yFor(current.last()))
            val end = Offset((totalBuckets - 1) * stepX, yFor(tail))
            drawCircle(color = lastCurrentColor, radius = 4.dp.toPx(), center = bend)
            drawValueLine(
                xs = listOf(bend.x, end.x),
                values = listOf(current.last(), tail),
                yFor = ::yFor,
                limitValue = effectiveLimit,
                normalColor = lineColor.copy(alpha = 0.55f),
                overColor = overLimitColor.copy(alpha = 0.7f),
                strokeWidthPx = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
            val endColor = if (effectiveLimit != null && tail > effectiveLimit) overLimitColor else lineColor
            drawCircle(color = endColor.copy(alpha = 0.45f), radius = 4.dp.toPx(), center = end)
            drawCircle(color = endColor, radius = 4.dp.toPx(), center = end, style = Stroke(width = 1.5.dp.toPx()))
        } else {
            drawCircle(color = lastCurrentColor, radius = 4.dp.toPx(), center = Offset(lastCurrentX, yFor(current.last())))
        }
    }
}
