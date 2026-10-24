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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.osscabinet.ossWallet.ui.theme.AppColors
import java.math.BigDecimal

/**
 * Minimal filled sparkline with no axes.
 *
 * Pass [onSelectedIndexChange] to make it scrubbable: dragging horizontally reports the nearest point
 * index (null when the finger lifts) and the point at [selectedIndex] is marked. Only horizontal drags
 * are claimed, so a vertical swipe still scrolls the screen.
 */
@Composable
fun AeonSparkline(
    points: List<BigDecimal>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelectedIndexChange: ((Int?) -> Unit)? = null,
    contentDescription: String? = null,
) {
    val values = remember(points) { points.map { it.toFloat() } }
    val currentOnChange = rememberUpdatedState(onSelectedIndexChange)
    val guideColor = AppColors.contentSecondary.copy(alpha = 0.6f)

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
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).takeIf { it != 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val topPad = size.height * 0.1f
        val usableH = size.height * 0.8f

        fun yFor(v: Float) = topPad + usableH * (1f - (v - minV) / range)

        val linePath = Path().apply {
            moveTo(0f, yFor(values.first()))
            for (i in 1 until values.size) lineTo(i * stepX, yFor(values[i]))
        }
        val fillPath = Path().apply {
            addPath(linePath)
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
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        selectedIndex?.takeIf { it in values.indices }?.let { i ->
            val x = i * stepX
            drawLine(color = guideColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, yFor(values[i])))
        }
    }
}

/**
 * Minimal sparkline for comparing the current period's cumulative spend so far against the
 * previous period at the same bucket positions, with an optional dashed/tinted continuation of
 * the CURRENT line (not a second series) out to a spent+planned projection. Same axis-less,
 * chrome-free look as [AeonSparkline] — deliberately not [AeonLineChart], which reserves space
 * for a y-axis and grid.
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
) {
    val current = remember(currentPoints) { currentPoints.map { it.toFloat() } }
    val previous = remember(previousPoints) { previousPoints.map { it.toFloat() } }
    val tail = remember(plannedTailEnd) { plannedTailEnd?.toFloat() }

    Canvas(modifier = modifier) {
        if (current.size < 2 || totalBuckets < 2) return@Canvas
        val allValues = current + previous + listOfNotNull(tail)
        val minV = allValues.min()
        val maxV = allValues.max()
        val range = (maxV - minV).takeIf { it != 0f } ?: 1f
        val stepX = size.width / (totalBuckets - 1)
        val topPad = size.height * 0.1f
        val usableH = size.height * 0.8f

        fun yFor(v: Float) = topPad + usableH * (1f - (v - minV) / range)

        if (previous.size >= 2) {
            val previousPath = Path().apply {
                moveTo(0f, yFor(previous.first()))
                for (i in 1 until previous.size) lineTo(i * stepX, yFor(previous[i]))
            }
            drawPath(
                path = previousPath,
                color = previousLineColor.copy(alpha = 0.7f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            )
        }

        val lastCurrentX = (current.size - 1) * stepX
        val currentPath = Path().apply {
            moveTo(0f, yFor(current.first()))
            for (i in 1 until current.size) lineTo(i * stepX, yFor(current[i]))
        }
        val fillPath = Path().apply {
            addPath(currentPath)
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
        drawPath(
            path = currentPath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // The planned tail is a continuation of the SAME line: it starts exactly where the solid
        // path ends, just drawn dashed and tinted — one bend, not two separate lines meeting.
        if (tail != null && totalBuckets > current.size) {
            val bend = Offset(lastCurrentX, yFor(current.last()))
            val end = Offset((totalBuckets - 1) * stepX, yFor(tail))
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = bend)
            drawLine(
                color = lineColor.copy(alpha = 0.55f),
                start = bend,
                end = end,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
            drawCircle(color = lineColor.copy(alpha = 0.45f), radius = 4.dp.toPx(), center = end)
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = end, style = Stroke(width = 1.5.dp.toPx()))
        } else {
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(lastCurrentX, yFor(current.last())))
        }
    }
}
