package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.osscabinet.ossWallet.domain.model.settings.BarChartGranularity
import kotlin.math.pow

/**
 * Horizontal scroll offset and vertical zoom for a scrollable bar chart.
 *
 * [offsetPx] starts at [Float.MAX_VALUE] so the chart opens at the right end (latest period);
 * callers clamp it against the real maximum once the width is known.
 */
@Stable
internal class AeonChartScrollState {
    var offsetPx by mutableFloatStateOf(Float.MAX_VALUE)
    var yZoom by mutableFloatStateOf(1f)
}

/** Resets to the right end whenever [key] changes (e.g. a different bucket count). */
@Composable
internal fun rememberAeonChartScrollState(key: Any?): AeonChartScrollState =
    remember(key) { AeonChartScrollState() }

@Composable
internal fun rememberAeonChartScrollable(
    state: AeonChartScrollState,
    maxScrollPx: Float,
    onScrolled: () -> Unit,
): ScrollableState {
    val currentMax = rememberUpdatedState(maxScrollPx)
    val currentOnScrolled = rememberUpdatedState(onScrolled)
    return rememberScrollableState { delta ->
        val max = currentMax.value
        val old = state.offsetPx.coerceIn(0f, max)
        val new = (old - delta).coerceIn(0f, max)
        state.offsetPx = new
        if (new != old) currentOnScrolled.value()
        old - new
    }
}

/** Bars shown per screen before the chart starts to scroll. */
fun BarChartGranularity.visibleBars(): Int = when (this) {
    BarChartGranularity.DAY -> 7
    BarChartGranularity.WEEK, BarChartGranularity.MONTH, BarChartGranularity.AUTO -> 6
    BarChartGranularity.QUARTER -> 4
    BarChartGranularity.YEAR -> 5
}

/** Y-axis ceiling used when a scrollable chart has only empty buckets, so the axis still reads sensibly. */
internal const val EMPTY_CHART_MAX = 100L

private const val MAX_Y_ZOOM = 4f
private const val Y_ZOOM_PER_PX = 1.012f

/** Drag vertically on the Y-axis to rescale it; double-tap to reset. */
@Composable
internal fun AeonChartYAxisZoomZone(
    state: AeonChartScrollState,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .pointerInput(state) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    // Dragging up (negative amount) zooms in.
                    state.yZoom = (state.yZoom * Y_ZOOM_PER_PX.pow(-dragAmount))
                        .coerceIn(1f, MAX_Y_ZOOM)
                }
            }
            .pointerInput(state) {
                detectTapGestures(onDoubleTap = { state.yZoom = 1f })
            }
    )
}
