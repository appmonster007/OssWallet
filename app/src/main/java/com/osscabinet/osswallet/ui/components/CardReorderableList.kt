package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min

/**
 * A robust, Material 3-compliant reorderable list component designed for dashboard sections
 * and complex card layouts.
 *
 * ### Key Constraints & Requirements:
 * 1. **Unique Keys**: Each item [T] must have a stable, unique key provided by [key].
 * 2. **Overlay Approach**: Uses a separate overlay for the dragged item to prevent disposal
 *    glitches during long scrolls. The dragged item is removed from the [LazyColumn] and
 *    rendered in a floating [Box].
 * 3. **Centralized Gestures**: Gesture detection is handled by the root [Box] container. This
 *    ensures that if the [LazyColumn] recycles an item during a scroll, the pointer tracker
 *    isn't cancelled.
 * 4. **Long Item Handling**: Specialized logic in [performReorder] and the auto-scroll loop
 *    allows users to drag items over/through cards that are taller than the viewport.
 * 5. **Material 3 Expressive Motion**:
 *    - Uses [animateItem] for smooth background item swaps.
 *    - Applies 1.05x scale and 16dp elevation to the dragged overlay.
 *    - Dims non-dragged items to 0.6 alpha to focus attention.
 *
 * @param items The list of items to display.
 * @param key Function to extract a unique key from an item.
 * @param onReorder Callback triggered when a swap occurs.
 * @param modifier Modifier for the root container.
 * @param state State for the [LazyColumn].
 * @param contentPadding Padding for the list content.
 * @param verticalArrangement Spacing between items.
 * @param itemContent The composable used to render each item.
 */
@Composable
fun <T> CardReorderableList(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    animateCards: Boolean = true,
    header: @Composable (LazyItemScope.() -> Unit)? = null,
    itemContent: @Composable LazyItemScope.(item: T, isDragging: Boolean, dragModifier: Modifier) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    // Core Drag State
    var draggedKey by remember { mutableStateOf<Any?>(null) }
    var currentPointerY by remember { mutableFloatStateOf(0f) }
    var touchOffsetOnItem by remember { mutableFloatStateOf(0f) }
    var draggedItemHeight by remember { mutableIntStateOf(0) }
    var lastSwapTime by remember { mutableLongStateOf(0L) }

    // Hit test tracking for handles
    var listCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val handleBounds = remember { mutableStateMapOf<Any, Rect>() }

    val currentItems by rememberUpdatedState(items)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentKeySelector by rememberUpdatedState(key)

    // Find the item data for the overlay
    val draggedItem = remember(draggedKey, items) {
        if (draggedKey != null) items.find { key(it) == draggedKey } else null
    }

    val verticalSpacingPx = with(density) { 12.dp.toPx() }

    /**
     * Identifies the immediate adjacent swap target based on a strict hysteresis threshold.
     * To eliminate oscillation jitter and handle direction changes gracefully:
     * 1. Swapping Down: Leading edge (bottom) must travel through 95% of the next item's total height (including spacing).
     * 2. Swapping Up: Leading edge (top) must travel through 95% of the previous item's total height (from bottom).
     * 
     * Formula: threshold = (95% adjacent item height) - (50% current item height).
     * This ensures the item feels "pushed" into its new position before swapping.
     * 
     * This adjacent-only approach ensures structural integrity during fast drags by cascading
     * swaps one-by-one, synchronized with the LazyColumn's layout state via the Sync Guard.
     */
    fun performReorder() {
        val now = System.currentTimeMillis()
        // With 95% hysteresis, cooldown can be very short to allow fast successive swaps
        if (now - lastSwapTime < 40) return 

        val draggedKeyVal = draggedKey ?: return
        val itemsList = currentItems
        
        val currentIndex = itemsList.indexOfFirst { currentKeySelector(it) == draggedKeyVal }
        if (currentIndex == -1) return

        val layoutInfo = state.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        
        // Sync Guard: Ensure layout and data are in sync before making structural changes.
        // This prevents multiple swaps before the list has finished its previous transition.
        val draggedItemInfo = visibleItems.find { it.key == draggedKeyVal }
        if (draggedItemInfo != null && draggedItemInfo.index != (if (header != null) currentIndex + 1 else currentIndex)) return
        
        // Use the center point of the dragged item for directional decisions
        val draggedCenter = currentPointerY - touchOffsetOnItem + (draggedItemHeight / 2f)

        // Find the adjacent target based on the strict 95% travel threshold.
        val targetItem = visibleItems.find { other ->
            val headerOffset = if (header != null) 1 else 0
            val isNext = other.index == (currentIndex + headerOffset + 1)
            val isPrev = other.index == (currentIndex + headerOffset - 1)
            if (!isNext && !isPrev) return@find false
            
            val otherTop = other.offset.toFloat()
            val otherBottom = (other.offset + other.size).toFloat()
            // In M3 Expressive, we consider the spacing part of the item's logical boundary for reordering
            val otherHeight = other.size.toFloat() + verticalSpacingPx
            
            if (isNext) {
                // Moving DOWN: Leading edge (bottom) must pass 95% of the next item's height
                draggedCenter > (otherTop + otherHeight * 0.95f - draggedItemHeight * 0.5f)
            } else {
                // Moving UP: Leading edge (top) must pass 95% of the previous item's height (from the bottom)
                // PREVENT DRAGGING ABOVE HEADER
                if (header != null && other.index == 0) return@find false
                draggedCenter < (otherBottom - (otherHeight * 0.95f - draggedItemHeight * 0.5f))
            }
        }

        if (targetItem != null) {
            val newList = itemsList.toMutableList()
            val element = newList.removeAt(currentIndex)
            val targetListIndex = if (header != null) targetItem.index - 1 else targetItem.index
            newList.add(targetListIndex, element)
            
            lastSwapTime = now
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            currentOnReorder(newList)
        }
    }

    // Auto-Scroll & Continuous Reorder Loop
    LaunchedEffect(draggedKey) {
        if (draggedKey != null) {
            val maxScrollSpeed = with(density) { 24.dp.toPx() }
            val bufferPx = with(density) { 50.dp.toPx() }

            while (isActive) {
                val layoutInfo = state.layoutInfo
                val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
                
                if (viewportHeight > 0) {
                    val speed = when {
                        currentPointerY < bufferPx -> {
                            val distance = (bufferPx - currentPointerY).coerceIn(0f, bufferPx)
                            -(distance / bufferPx) * maxScrollSpeed
                        }
                        currentPointerY > viewportHeight - bufferPx -> {
                            val distance = (currentPointerY - (viewportHeight - bufferPx)).coerceIn(0f, bufferPx)
                            (distance / bufferPx) * maxScrollSpeed
                        }
                        else -> 0f
                    }

                    if (speed != 0f) {
                        state.scrollBy(speed)
                        // Ensure reorder is recalculated during scroll animation
                        performReorder()
                    }
                }
                withFrameNanos { it }
            }
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { listCoordinates = it }
            .pointerInput(Unit) {
                // Centralized gesture detection on the container to prevent disposal-induced cancellations
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val layoutInfo = state.layoutInfo
                        val item = layoutInfo.visibleItemsInfo.firstOrNull { 
                            offset.y.toInt() in it.offset..(it.offset + it.size)
                        }
                        if (item != null) {
                            // If header is present and we clicked on it (index 0), don't start drag
                            if (header != null && item.index == 0) return@detectDragGesturesAfterLongPress

                            // Hit test: only start drag if the touch is within the reported handle bounds
                            val itemKey = item.key
                            val bounds = handleBounds[itemKey]
                            
                            // If a handle is defined, restrict drag to it. 
                            // If no handle is defined (empty map entry), we allow full-card drag as fallback?
                            // No, user specifically asked for "only drag if pressed on respective cards header".
                            // So we strictly require handle match.
                            if (bounds != null && bounds.contains(offset)) {
                                draggedKey = itemKey
                                draggedItemHeight = item.size
                                touchOffsetOnItem = offset.y - item.offset
                                currentPointerY = offset.y
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentPointerY += dragAmount.y
                        performReorder()
                    },
                    onDragEnd = { draggedKey = null },
                    onDragCancel = { draggedKey = null }
                )
            }
    ) {
        LazyColumn(
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            // Disable native scrolling while dragging to avoid gesture conflicts
            userScrollEnabled = draggedKey == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (header != null) {
                item(key = "header_item") {
                    header()
                }
            }

            itemsIndexed(
                items = items,
                key = { _, item -> key(item) }
            ) { _, item ->
                val itemKey = key(item)
                val isDragging = draggedKey == itemKey
                
                // Construct the drag handle modifier
                val dragModifier = Modifier.onGloballyPositioned { coords ->
                    val listCoords = listCoordinates ?: return@onGloballyPositioned
                    handleBounds[itemKey] = listCoords.localBoundingBoxOf(coords)
                }

                // M3 Background Dimming
                val alpha by animateFloatAsState(
                    targetValue = if (draggedKey != null && !isDragging) 0.6f else 1.0f,
                    label = "drag_alpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (animateCards || draggedKey != null) Modifier.animateItem() else Modifier) // Animate if setting enabled or during reordering
                        .graphicsLayer { this.alpha = alpha }
                ) {
                    if (isDragging) {
                        // Ghost Placeholder with dashed outline
                        val outlineColor = MaterialTheme.colorScheme.outlineVariant
                        val cornerRadius = with(density) { 16.dp.toPx() }
                        val strokeWidth = with(density) { 2.dp.toPx() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(with(density) { draggedItemHeight.toDp() })
                                .padding(vertical = 4.dp)
                                .drawBehind {
                                    val stroke = Stroke(
                                        width = strokeWidth,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                    drawRoundRect(
                                        color = outlineColor,
                                        style = stroke,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                                    )
                                }
                        )
                    }
                    else {
                        // Normal Item
                        itemContent(item, false, dragModifier)
                    }
                }
            }
        }

        // The Overlay: This is what makes the drag "glitch-free". 
        // It stays in composition regardless of the list's scroll state.
        val currentDraggedItem = draggedItem
        if (draggedKey != null && currentDraggedItem != null) {
            val scale by animateFloatAsState(targetValue = 1.05f, label = "overlay_scale")
            val elevation by animateDpAsState(targetValue = 16.dp, label = "overlay_elevation")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = contentPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        end = contentPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                    )
                    .graphicsLayer {
                        translationY = currentPointerY - touchOffsetOnItem
                        scaleX = scale
                        scaleY = scale
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(16.dp)
                        clip = true
                    }
                    .zIndex(1.0f) // Task 3: Force zIndex to 1.0f
            ) {
                Surface(
                    tonalElevation = elevation,
                    shadowElevation = 0.dp, // Elevation handled by graphicsLayer for better performance during animation
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    // Provide LazyItemScope via a single-item LazyColumn
                    LazyColumn(
                        modifier = Modifier.wrapContentHeight(),
                        userScrollEnabled = false
                    ) {
                        item {
                            this@item.itemContent(currentDraggedItem, true, Modifier)
                        }
                    }
                }
            }
        }
    }
}
