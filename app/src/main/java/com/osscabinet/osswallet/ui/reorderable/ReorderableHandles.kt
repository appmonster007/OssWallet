package com.osscabinet.ossWallet.ui.reorderable

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

/**
 * Long-press anywhere within this composable to begin a drag of the item identified by [key].
 * Apply to the entire row when the whole row is the drag affordance (Accounts, Templates).
 *
 * The pointer offset is converted from this composable's local space into the container's
 * coordinate space via [LayoutCoordinates.localPositionOf], so the handle can be any descendant
 * of the [ReorderableItem] — not just the row root.
 */
fun Modifier.longPressDraggableHandle(
    state: ReorderableLazyListState,
    key: Any,
    enabled: Boolean = true,
    onDragStarted: () -> Unit = {},
    onDragStopped: () -> Unit = {},
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    var handleCoords: LayoutCoordinates? by remember { mutableStateOf(null) }

    this
        .onGloballyPositioned { coords -> handleCoords = coords }
        .pointerInput(key, enabled) {
            if (!enabled) return@pointerInput
            detectDragGesturesAfterLongPress(
                onDragStart = { localOffset ->
                    val handle = handleCoords ?: return@detectDragGesturesAfterLongPress
                    val container = state.containerCoords ?: return@detectDragGesturesAfterLongPress
                    if (!handle.isAttached || !container.isAttached) return@detectDragGesturesAfterLongPress
                    val pointerInContainer = container.localPositionOf(handle, localOffset)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.onDragStart(key, pointerInContainer)
                    onDragStarted()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.onDrag(dragAmount.y)
                },
                onDragEnd = {
                    state.onDragStop()
                    onDragStopped()
                },
                onDragCancel = {
                    state.onDragStop()
                    onDragStopped()
                },
            )
        }
}

/**
 * Press-and-drag (no long-press) variant for explicit drag handles. Use on a child of the item
 * when only a specific UI element (e.g. a grip icon or the card header) should initiate drag.
 */
fun Modifier.draggableHandle(
    state: ReorderableLazyListState,
    key: Any,
    enabled: Boolean = true,
    onDragStarted: () -> Unit = {},
    onDragStopped: () -> Unit = {},
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    var handleCoords: LayoutCoordinates? by remember { mutableStateOf(null) }

    this
        .onGloballyPositioned { coords -> handleCoords = coords }
        .pointerInput(key, enabled) {
            if (!enabled) return@pointerInput
            detectDragGestures(
                onDragStart = { localOffset ->
                    val handle = handleCoords ?: return@detectDragGestures
                    val container = state.containerCoords ?: return@detectDragGestures
                    if (!handle.isAttached || !container.isAttached) return@detectDragGestures
                    val pointerInContainer: Offset = container.localPositionOf(handle, localOffset)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.onDragStart(key, pointerInContainer)
                    onDragStarted()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.onDrag(dragAmount.y)
                },
                onDragEnd = {
                    state.onDragStop()
                    onDragStopped()
                },
                onDragCancel = {
                    state.onDragStop()
                    onDragStopped()
                },
            )
        }
}

/**
 * Add TalkBack-accessible "Move up" / "Move down" custom actions for screen reader users.
 * Apply to the row (or any focusable child) so the actions are exposed at that semantics node.
 */
fun Modifier.reorderableSemantics(
    state: ReorderableLazyListState,
    key: Any,
    moveUpLabel: String = "Move up",
    moveDownLabel: String = "Move down",
): Modifier = this.semantics(mergeDescendants = true) {
    customActions = listOf(
        CustomAccessibilityAction(moveUpLabel) { state.moveByAccessibility(key, -1) },
        CustomAccessibilityAction(moveDownLabel) { state.moveByAccessibility(key, +1) },
    )
}
