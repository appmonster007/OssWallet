package com.osscabinet.ossWallet.ui.reorderable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex

/**
 * Mark a composable as the reorderable container. Apply to the LazyColumn (or its parent Box)
 * so the state holder can capture container coordinates for hit testing.
 */
fun Modifier.reorderable(state: ReorderableLazyListState): Modifier =
    this.onGloballyPositioned { coords -> state.containerCoords = coords }

/**
 * Wrap each LazyColumn row that should be reorderable. The wrapper:
 *  - registers/unregisters the item's bounds (in container coords) in [state],
 *  - applies `Modifier.animateItem()` to non-dragged items for smooth swap transitions,
 *  - applies a `graphicsLayer { translationY = ... }` to the dragged item so it stays pinned
 *    under the finger across mid-drag swaps,
 *  - elevates the dragged item via `zIndex` so it floats above siblings.
 *
 * The content slot receives `isDragging` so callers can drive their own visual treatments
 * (scale, elevation, dim non-dragged peers, etc.).
 */
@Composable
fun LazyItemScope.ReorderableItem(
    state: ReorderableLazyListState,
    key: Any,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.(isDragging: Boolean) -> Unit,
) {
    val isDragging = state.isDragging(key)

    DisposableEffect(key) {
        onDispose { state.unregisterItem(key) }
    }

    Box(
        modifier = modifier
            // animateItem handles smooth slide transitions for non-dragged items when the data
            // re-orders. We skip it for the dragged item — graphicsLayer.translationY handles its
            // pinning, and animateItem would fight that.
            .then(if (!isDragging && enabled) Modifier.animateItem() else Modifier)
            .onGloballyPositioned { coords ->
                if (enabled) state.registerItem(key, coords)
            }
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) state.draggedItemTranslation(key) else 0f
            },
        content = { content(isDragging) },
    )
}
