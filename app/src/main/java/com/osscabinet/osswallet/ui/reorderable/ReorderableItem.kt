package com.osscabinet.ossWallet.ui.reorderable

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.ReorderableItem as CalvinReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState as rememberCalvinReorderableLazyListState

/**
 * Thin convenience wrapper matching this app's old `(fromIndex, toIndex) -> Unit` callback shape,
 * so existing call sites don't need to adapt to Calvin's `LazyListItemInfo`-based `onMove`.
 */
@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): ReorderableLazyListState =
    rememberCalvinReorderableLazyListState(lazyListState) { from, to -> onMove(from.index, to.index) }

/**
 * Thin wrapper around `sh.calvin.reorderable`'s `ReorderableItem` that adds the lift/scale/shadow
 * treatment and cross-item dim-alpha this app wants on top.
 *
 * `sh.calvin.reorderable` renders each item exactly once — a dragged item visually offsets in
 * place via `graphicsLayer.translationY`, and its old slot just closes up through the sibling's own
 * `animateItem()). There is no separate ghost/placeholder concept to preserve here; this app's
 * previous hand-rolled engine had one, but it's an intentional simplification to drop it as part of
 * switching to the maintained library rather than reintroducing custom position tracking just to
 * keep it.
 *
 * @param shape The shape of the content being wrapped (e.g. a card's rounded corners, or a pill-
 * shaped list row). `shadowElevation` casts its shadow using the graphicsLayer's `shape` — left at
 * the default `RectangleShape`, the lift shadow renders as a hard rectangular box around content
 * that isn't actually rectangular. Pass the content's real shape so the shadow's outline matches
 * what's visually inside it. `clip` stays `false` regardless (the content already clips itself to
 * this same shape via its own Surface/Card/GlassSurface) so the shadow's soft bleed past the edge
 * isn't cut off — only the shadow's own outline needs `shape` to match, not the content clip.
 */
@Composable
fun LazyItemScope.ReorderableItem(
    state: ReorderableLazyListState,
    key: Any,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    content: @Composable ReorderableCollectionItemScope.(isDragging: Boolean) -> Unit,
) {
    CalvinReorderableItem(
        state = state,
        key = key,
        modifier = modifier,
        enabled = enabled,
    ) { isDragging ->
        val scope = this
        val isOtherItemDragging = state.isAnyItemDragging && !isDragging

        // Lift effect: scale + elevate while dragging, for tactile separation.
        val scale by animateFloatAsState(
            targetValue = if (isDragging) 1.05f else 1.0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "reorder_item_scale",
        )
        val elevationPx by animateFloatAsState(
            targetValue = if (isDragging) with(LocalDensity.current) { 12.dp.toPx() } else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "reorder_item_elevation",
        )
        // Dim non-dragged peers to focus the interaction.
        val dimAlpha by animateFloatAsState(
            targetValue = if (isOtherItemDragging) 0.6f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "reorder_dim_alpha",
        )

        Box(
            modifier = Modifier.graphicsLayer {
                alpha = dimAlpha
                scaleX = scale
                scaleY = scale
                shadowElevation = elevationPx
                this.shape = shape
                // Content (e.g. AeonExpressiveCard) handles its own clipping to rounded corners;
                // we don't clip here so the lift shadow isn't cut off.
                clip = false
            }
        ) {
            with(scope) { content(isDragging) }
        }
    }
}
