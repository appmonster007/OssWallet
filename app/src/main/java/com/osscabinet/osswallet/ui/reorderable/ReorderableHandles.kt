package com.osscabinet.ossWallet.ui.reorderable

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

/**
 * Fire [onLongPress] when this composable is long-pressed — a pure "trigger" gesture with no
 * associated drag. Built on the same [detectDragGesturesAfterLongPress] primitive
 * `sh.calvin.reorderable`'s own drag handles use, specifically because it tolerates a descendant
 * already claiming the touch (e.g. an icon's own `clickable`) — a plain tap detector's
 * `awaitFirstDown(requireUnconsumed = true)` never sees an unconsumed down once that happens, which
 * silently breaks a naively-built long-press detector nested inside a clickable. Drag deltas after
 * the long-press fires are just consumed (not acted on) so the gesture doesn't leak into a
 * scroll/pan once recognized.
 */
fun Modifier.longPressToTrigger(
    key: Any,
    enabled: Boolean = true,
    onLongPress: () -> Unit,
): Modifier = composed {
    val onLongPressState by rememberUpdatedState(onLongPress)

    this.pointerInput(key, enabled) {
        if (!enabled) return@pointerInput
        detectDragGesturesAfterLongPress(
            onDragStart = { onLongPressState() },
            onDrag = { change, _ -> change.consume() },
            onDragEnd = {},
            onDragCancel = {},
        )
    }
}

/**
 * Add TalkBack-accessible "Move up" / "Move down" custom actions for screen reader users.
 *
 * Operates directly on [lazyListState]'s layout info and calls [onMove] — entirely independent of
 * `sh.calvin.reorderable`'s internal drag state, which has no accessibility support of its own.
 * Unlike a drag gesture (which the caller may buffer and persist once on drop), each invocation
 * here is a complete, standalone edit with no natural "gesture end" to hang persistence off of, so
 * [onMove] is expected to persist immediately if that's what the caller wants — pass a lambda that
 * both reorders and writes through, e.g. `{ from, to -> editList = ...; onReorder(editList) }`.
 */
fun Modifier.reorderableSemantics(
    lazyListState: LazyListState,
    key: Any,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    moveUpLabel: String = "Move up",
    moveDownLabel: String = "Move down",
): Modifier = this.semantics(mergeDescendants = true) {
    customActions = listOf(
        CustomAccessibilityAction(moveUpLabel) { moveByAccessibility(lazyListState, key, -1, onMove) },
        CustomAccessibilityAction(moveDownLabel) { moveByAccessibility(lazyListState, key, +1, onMove) },
    )
}

private fun moveByAccessibility(
    lazyListState: LazyListState,
    key: Any,
    delta: Int,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): Boolean {
    val info = lazyListState.layoutInfo
    val currentIndex = info.visibleItemsInfo.firstOrNull { it.key == key }?.index ?: return false
    val targetIndex = currentIndex + delta
    if (targetIndex !in 0 until info.totalItemsCount) return false
    onMove(currentIndex, targetIndex)
    return true
}
