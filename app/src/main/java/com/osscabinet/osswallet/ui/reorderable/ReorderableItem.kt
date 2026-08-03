package com.osscabinet.ossWallet.ui.reorderable

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
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
 *
 * [placeholder] renders what's left behind in the vacated slot while dragging. Defaults to a
 * dimmed copy of the row's own `content` (the original ghost behavior); pass a lighter drop-target
 * indicator instead when re-rendering the full content there is wasteful or visually noisy.
 */
@Composable
fun LazyItemScope.ReorderableItem(
    state: ReorderableLazyListState,
    key: Any,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.(isDragging: Boolean) -> Unit,
) {
    val isDragging = state.isDragging(key)
    val isAnyItemDragging = state.draggedKey != null
    val isOtherItemDragging = isAnyItemDragging && !isDragging
    val isSettling = state.isSettling(key)

    // 1. Visual Feedback: Scale and Elevation (Lifting)
    // Scale up slightly when dragged to give "lift" and tactile separation.
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reorder_item_scale",
    )

    // Lift effect: increase elevation when dragging for a prominent shadow.
    // We convert Dp to Px inline to keep the Composable body lean.
    val elevationPx by animateFloatAsState(
        targetValue = if (isDragging) with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() } else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reorder_item_elevation",
    )

    // 2. Focus: Dim non-dragged peers to focus the interaction
    val dimAlpha by animateFloatAsState(
        targetValue = if (isOtherItemDragging) 0.6f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reorder_dim_alpha",
    )

    Box(
        modifier = modifier
            // animateItem handles smooth slide transitions for non-dragged items when the data
            // re-orders. We skip it for the dragged item — graphicsLayer.translationY handles its
            // pinning, and animateItem would fight that. Also skipped while settling: the drop
            // spring already animates translationY back to zero, so animateItem would fight that too.
            .then(if (!isDragging && !isSettling && enabled) Modifier.animateItem() else Modifier)
            .graphicsLayer {
                alpha = dimAlpha
            },
    ) {
        if (isDragging) {
            // 3. Placeholder: Stays in the list at the original layout position while the "real"
            // row floats, providing a reference for the drop point.
            Box(modifier = Modifier.matchParentSize()) {
                if (placeholder != null) {
                    placeholder()
                } else {
                    Box(modifier = Modifier.graphicsLayer { alpha = 0.2f }) {
                        content(false)
                    }
                }
            }
        }

        // 4. Floating Item: The actual card being moved, elevated and scaled. Stays elevated
        // briefly after drop while it eases back to rest (see ReorderableLazyListState.onDragStop).
        Box(
            modifier = Modifier
                .zIndex(if (isDragging || isSettling) 10f else 0f) // High Z-index to float above all
                .graphicsLayer {
                    translationY = when {
                        isDragging -> state.draggedItemTranslation(key)
                        isSettling -> state.settlingTranslation(key)
                        else -> 0f
                    }
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = elevationPx
                    // We don't clip here so the shadow isn't cut off. The content (e.g. AeonListItem)
                    // usually handles its own clipping to rounded corners.
                    clip = false
                }
        ) {
            content(isDragging)
        }
    }
}
