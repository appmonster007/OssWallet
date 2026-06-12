package com.osscabinet.ossWallet.ui.reorderable

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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

    // Visual feedback: Scale up slightly when dragged to give "lift"
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reorder_item_scale"
    )

    // Lift effect: increase elevation when dragging
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 8f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reorder_item_elevation"
    )

    Box(
        modifier = modifier
            // animateItem handles smooth slide transitions for non-dragged items when the data
            // re-orders. We skip it for the dragged item — graphicsLayer.translationY handles its
            // pinning, and animateItem would fight that.
            .then(if (!isDragging && enabled) Modifier.animateItem() else Modifier)
            .onGloballyPositioned { coords ->
                if (enabled) state.registerItem(key, coords)
            },
    ) {
        if (isDragging) {
            // THE GHOST (Placeholder in the list)
            // Stays in the list at the original layout position while the "real" card floats.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.15f }
            ) {
                content(false)
            }
        }

        // THE DRAGGED ITEM (Floating under finger)
        Box(
            modifier = Modifier
                .zIndex(if (isDragging) 10f else 0f) // High Z-index to float above all
                .graphicsLayer {
                    translationY = if (isDragging) state.draggedItemTranslation(key) else 0f
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = elevation
                    clip = elevation > 0f
                }
        ) {
            content(isDragging)
        }
    }
}
