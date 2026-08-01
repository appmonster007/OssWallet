package com.osscabinet.ossWallet.ui.reorderable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.pow

/**
 * Drop-in drag-to-reorder primitive for [androidx.compose.foundation.lazy.LazyColumn].
 *
 * Built around [LayoutCoordinates] — every hit-test compares finger position and item bounds in
 * the SAME container coordinate space (captured via [LayoutCoordinates.localBoundingBoxOf]).
 * No pixel math, no reasoning about `contentPadding` or `viewportStartOffset`.
 *
 * Use the entry points:
 *  - [rememberReorderableLazyListState] — create state + autoscroll loop tied to a [LazyListState].
 *  - [androidx.compose.foundation.lazy.LazyItemScope] `ReorderableItem` — wraps your row, registers bounds, animates.
 *  - [Modifier.reorderable] — apply to the LazyColumn's parent to register the container coords.
 *  - [Modifier.longPressDraggableHandle] / [Modifier.draggableHandle] — start a drag from anywhere on a row.
 *  - [Modifier.reorderableSemantics] — TalkBack "Move up"/"Move down" custom actions.
 *
 * This module is intentionally self-contained so it can be lifted into a standalone library later.
 */
@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState = rememberLazyListState(),
    scrollThreshold: Dp = 64.dp,
    maxScrollSpeedPxPerFrame: Float = 24f,
    swapCooldownMs: Long = 50L,
    swapTravelRatio: Float = 0.5f,
    onAccessibilityMove: () -> Unit = {},
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): ReorderableLazyListState {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val onMoveState = rememberUpdatedState(onMove)
    val onAccessibilityMoveState = rememberUpdatedState(onAccessibilityMove)
    val scrollThresholdPx = with(density) { scrollThreshold.toPx() }

    return remember(lazyListState) {
        ReorderableLazyListState(
            lazyListState = lazyListState,
            haptic = haptic,
            scope = scope,
            onMoveState = onMoveState,
            onAccessibilityMoveState = onAccessibilityMoveState,
            scrollThresholdPx = scrollThresholdPx,
            maxScrollSpeedPx = maxScrollSpeedPxPerFrame,
            swapCooldownMs = swapCooldownMs,
            swapTravelRatio = swapTravelRatio.coerceIn(0.1f, 0.95f),
        )
    }
}

class ReorderableLazyListState internal constructor(
    val lazyListState: LazyListState,
    private val haptic: HapticFeedback,
    private val scope: CoroutineScope,
    private val onMoveState: State<(Int, Int) -> Unit>,
    private val onAccessibilityMoveState: State<() -> Unit>,
    private val scrollThresholdPx: Float,
    private val maxScrollSpeedPx: Float,
    private val swapCooldownMs: Long,
    private val swapTravelRatio: Float,
) {
    // ---- Container ----
    internal var containerCoords: LayoutCoordinates? = null

    // ---- Per-item bounds in container space ----
    // mutableStateMap writes from onGloballyPositioned cause targeted invalidations only on
    // readers of changed keys — touched per-frame during drag, idle otherwise.
    internal val itemBounds = mutableStateMapOf<Any, Rect>()

    // ---- Drag state ----
    var draggedKey: Any? by mutableStateOf(null)
        private set

    private var pointerYInContainer by mutableFloatStateOf(0f)
    private var touchOffsetWithinItem by mutableFloatStateOf(0f)
    private var draggedItemHeightSnapshot by mutableFloatStateOf(0f)
    private var lastSwapTimeMs: Long = 0L
    private var autoScrollJob: Job? = null

    // ---- Swap re-entrancy guard ----
    // Prevents onDrag() and the autoscroll frame loop from both triggering onMove() for the same
    // stale index before the LazyColumn's layoutInfo (and this state's itemBounds) reflect the
    // previous swap. Without this, fast drags combined with autoscroll can double-swap or swap
    // against indices that are already out of date.
    private var swapInFlight by mutableStateOf(false)

    // ---- Drop-settle animation ----
    // The key of the item that just finished dragging and is easing back to rest, plus the
    // translationY it should keep applying while settling. Avoids a hard visual "snap" the
    // instant a drag ends.
    private var settlingKey: Any? by mutableStateOf(null)
    private val settlingOffset = Animatable(0f)

    fun isDragging(key: Any): Boolean = draggedKey == key

    /**
     * The translationY (in the LazyColumn's local layout space) that the dragged item must apply
     * to remain pinned under the finger. Returns 0 for non-dragged items.
     *
     * Works correctly across mid-drag swaps: when [onMove] re-orders the list, the LazyColumn
     * places the dragged item at a new layout position, [itemBounds] updates via
     * [LayoutCoordinates.localBoundingBoxOf], and the returned translation adjusts so the item
     * stays under the finger.
     */
    fun draggedItemTranslation(key: Any): Float {
        if (draggedKey != key) return 0f
        return currentDraggedTranslation()
    }

    private fun currentDraggedTranslation(): Float {
        val key = draggedKey ?: return 0f
        val layoutTop = itemBounds[key]?.top ?: return 0f
        val desiredTop = pointerYInContainer - touchOffsetWithinItem
        return desiredTop - layoutTop
    }

    /** Whether [key] is currently easing back to rest after being dropped. */
    fun isSettling(key: Any): Boolean = settlingKey == key

    /** The translationY to apply while [key] is settling. Returns 0 once settled or if not settling. */
    fun settlingTranslation(key: Any): Float = if (settlingKey == key) settlingOffset.value else 0f

    // ---- Internal API for ReorderableItem ----

    internal fun registerItem(key: Any, coords: LayoutCoordinates) {
        if (!coords.isAttached) return
        val container = containerCoords ?: return
        if (!container.isAttached) return
        itemBounds[key] = container.localBoundingBoxOf(coords)
    }

    internal fun unregisterItem(key: Any) {
        itemBounds.remove(key)
    }

    internal fun itemTopInContainer(key: Any): Float? = itemBounds[key]?.top

    // ---- Internal API for drag-handle modifiers ----

    /**
     * Begin a drag of [key]. [pointerInContainer] is the finger position in container coordinates.
     */
    internal fun onDragStart(key: Any, pointerInContainer: Offset) {
        val item = itemBounds[key] ?: return
        if (settlingKey == key) {
            // Re-grabbed before its settle animation finished — cancel the settle so the two
            // translations don't fight each other.
            settlingKey = null
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        draggedKey = key
        pointerYInContainer = pointerInContainer.y
        touchOffsetWithinItem = pointerInContainer.y - item.top
        draggedItemHeightSnapshot = item.height
        lastSwapTimeMs = System.currentTimeMillis()
        startAutoScrollLoop()
    }

    internal fun onDrag(dragDeltaY: Float) {
        if (draggedKey == null) return
        pointerYInContainer += dragDeltaY
        attemptSwap()
    }

    internal fun onDragStop() {
        autoScrollJob?.cancel()
        autoScrollJob = null

        val key = draggedKey
        val lastTranslation = currentDraggedTranslation()

        draggedKey = null
        pointerYInContainer = 0f
        touchOffsetWithinItem = 0f
        draggedItemHeightSnapshot = 0f

        if (key != null && lastTranslation != 0f) {
            settlingKey = key
            scope.launch {
                settlingOffset.snapTo(lastTranslation)
                settlingOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = 0.5f,
                    ),
                )
                if (settlingKey == key) settlingKey = null
            }
        }
    }

    // ---- Accessibility ----

    /**
     * Move the item identified by [key] by [delta] positions in the underlying data list.
     * Returns true if the move was accepted (bounds-checked against the LazyColumn's total item count).
     *
     * Unlike a drag gesture (which buffers swaps and persists once on drop), a11y moves have no
     * "gesture end" signal to hang persistence off of — each invocation is treated as a complete,
     * standalone edit and [onAccessibilityMoveState] is invoked immediately after so the caller can
     * write it straight through to storage.
     */
    fun moveByAccessibility(key: Any, delta: Int): Boolean {
        val info = lazyListState.layoutInfo
        val currentIndex = info.visibleItemsInfo.firstOrNull { it.key == key }?.index ?: return false
        val targetIndex = currentIndex + delta
        if (targetIndex !in 0 until info.totalItemsCount) return false
        onMoveState.value(currentIndex, targetIndex)
        onAccessibilityMoveState.value()
        return true
    }

    // ---- Autoscroll ----

    private fun startAutoScrollLoop() {
        autoScrollJob?.cancel()
        autoScrollJob = scope.launch {
            while (isActive && draggedKey != null) {
                val container = containerCoords
                if (container == null || !container.isAttached) {
                    withFrameNanos { it }
                    continue
                }
                val viewportHeight = container.size.height.toFloat()
                val distFromTop = pointerYInContainer
                val distFromBottom = viewportHeight - pointerYInContainer

                val speed = when {
                    distFromTop < scrollThresholdPx -> {
                        // Quadratic speed-up near edges for natural feel
                        val factor = (1f - (distFromTop / scrollThresholdPx).coerceIn(0f, 1f)).pow(2f)
                        -maxScrollSpeedPx * factor
                    }
                    distFromBottom < scrollThresholdPx -> {
                        val factor = (1f - (distFromBottom / scrollThresholdPx).coerceIn(0f, 1f)).pow(2f)
                        maxScrollSpeedPx * factor
                    }
                    else -> 0f
                }

                if (speed != 0f) {
                    lazyListState.scrollBy(speed)
                    attemptSwap()
                }
                withFrameNanos { it }
            }
        }
    }

    // ---- Swap engine ----

    /**
     * Finds the nearest visible item on either side of the dragged item whose swap threshold has
     * been crossed, and swaps with it.
     *
     * Unlike a strict `currentIndex ± 1` lookup, this scans outward through *all* visible items in
     * each direction so a neighbor that happens to be scrolled just outside the ±1 slot (e.g. right
     * at the viewport edge during autoscroll) is still found. Candidates are checked nearest-first
     * and the scan stops at the first miss in that direction — since farther items require crossing
     * an even larger threshold, a miss on the nearest candidate guarantees a miss on farther ones.
     *
     * The direction-specific threshold check ([Rect.top] for items below, [Rect.bottom] for items
     * above) is what gives the swap hysteresis: reversing a swap requires crossing back through the
     * same fraction of the target's bounds, so a jittery finger sitting near the boundary can't
     * flip-flop the same pair back and forth.
     */
    private fun attemptSwap() {
        val key = draggedKey ?: return
        if (swapInFlight) return
        val now = System.currentTimeMillis()
        if (now - lastSwapTimeMs < swapCooldownMs) return

        val visible = lazyListState.layoutInfo.visibleItemsInfo
        val currentInfo = visible.firstOrNull { it.key == key } ?: return
        val currentIndex = currentInfo.index

        // Visual position of the dragged item: pinned under the finger.
        val draggedTop = pointerYInContainer - touchOffsetWithinItem
        val draggedBottom = draggedTop + draggedItemHeightSnapshot
        val draggedCenterY = (draggedTop + draggedBottom) / 2f

        val below = visible.filter { it.index > currentIndex }.sortedBy { it.index }
        for (candidate in below) {
            val bounds = itemBounds[candidate.key] ?: continue
            if (draggedCenterY > bounds.top + bounds.height * swapTravelRatio) {
                performSwap(currentIndex, candidate.index, now)
                return
            }
            break
        }

        val above = visible.filter { it.index < currentIndex }.sortedByDescending { it.index }
        for (candidate in above) {
            val bounds = itemBounds[candidate.key] ?: continue
            if (draggedCenterY < bounds.bottom - bounds.height * swapTravelRatio) {
                performSwap(currentIndex, candidate.index, now)
                return
            }
            break
        }
    }

    private fun performSwap(fromIndex: Int, toIndex: Int, now: Long) {
        lastSwapTimeMs = now
        swapInFlight = true
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onMoveState.value(fromIndex, toIndex)

        // Hold the gate until the LazyColumn's layoutInfo actually reflects the new order (or a
        // timeout elapses) so a fast drag/autoscroll can't queue a second swap against indices
        // that are already stale.
        scope.launch {
            try {
                withTimeout(500L) {
                    snapshotFlow { lazyListState.layoutInfo }.drop(1).first()
                }
            } catch (e: TimeoutCancellationException) {
                // Unblock anyway — better to risk a rare stale swap than to wedge dragging.
            }
            swapInFlight = false
        }
    }
}
