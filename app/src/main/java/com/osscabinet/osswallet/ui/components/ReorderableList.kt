package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A robust, Material 3-compliant reorderable list component.
 *
 * FEATURES:
 * - High-performance drag-and-drop using [graphicsLayer] and [zIndex].
 * - Smooth, velocity-aware autoscrolling when dragging near screen edges.
 * - Robust state management with [rememberUpdatedState] to prevent gesture interruptions.
 * - Adaptive thresholds calculated using [LocalDensity] for consistent behavior across devices.
 * - Separation of concerns between UI reordering ([onReorder]) and persistence ([onDragEnd]).
 */
@Composable
fun <T> ReorderableList(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    onDragEnd: (List<T>) -> Unit = {},
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    itemContent: @Composable LazyItemScope.(item: T, isDragging: Boolean) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Internal state for active drag tracking
    var draggedKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var autoscrollJob by remember { mutableStateOf<Job?>(null) }

    // Screen-adaptive constants
    val scrollThreshold = with(density) { 56.dp.toPx() }
    val maxScrollSpeed = with(density) { 10.dp.toPx() }

    // Use rememberUpdatedState to ensure pointerInput logic always uses latest data
    // without triggering a restart of the gesture detection block.
    val currentItems by rememberUpdatedState(items)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentKeySelector by rememberUpdatedState(key)

    Box(modifier = modifier) {
        LazyColumn(
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            modifier = Modifier.pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        state.layoutInfo.visibleItemsInfo
                            .firstOrNull { item ->
                                offset.y.toInt() in item.offset..(item.offset + item.size)
                            }?.also { itemInfo ->
                                if (itemInfo.index in currentItems.indices) {
                                    draggedKey = currentKeySelector(currentItems[itemInfo.index])
                                    dragOffset = 0f
                                }
                            }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dKey = draggedKey ?: return@detectDragGesturesAfterLongPress
                        dragOffset += dragAmount.y

                        val currentIndex = currentItems.indexOfFirst { currentKeySelector(it) == dKey }
                        if (currentIndex != -1) {
                            val layoutInfo = state.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val currentItemInfo = visibleItems.firstOrNull { it.index == currentIndex }

                            if (currentItemInfo != null) {
                                val draggedItemCenter = currentItemInfo.offset + (currentItemInfo.size / 2) + dragOffset

                                // Swap logic: triggered when item center passes 25% threshold of target item
                                val targetItem = visibleItems.firstOrNull { item ->
                                    if (item.index == currentIndex) return@firstOrNull false

                                    val itemStart = item.offset.toFloat()
                                    val itemEnd = (item.offset + item.size).toFloat()

                                    if (dragOffset > 0 && item.index > currentIndex) {
                                        draggedItemCenter > itemStart + (item.size * 0.25f)
                                    } else if (dragOffset < 0 && item.index < currentIndex) {
                                        draggedItemCenter < itemEnd - (item.size * 0.25f)
                                    } else {
                                        false
                                    }
                                }

                                if (targetItem != null) {
                                    val newList = currentItems.toMutableList()
                                    val element = newList.removeAt(currentIndex)
                                    newList.add(targetItem.index, element)

                                    // Visual compensation: adjust offset to keep the item pinned under touch
                                    dragOffset -= (targetItem.offset - currentItemInfo.offset)
                                    currentOnReorder(newList)
                                }
                            }

                            // Dynamic autoscroll based on pointer position relative to viewport edges
                            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                            val pointerY = (currentItemInfo?.offset ?: 0).toFloat() +
                                    (currentItemInfo?.size ?: 0).toFloat() / 2f + dragOffset

                            val scrollSpeed = when {
                                pointerY < scrollThreshold ->
                                    -maxScrollSpeed * (1f - (pointerY / scrollThreshold).coerceIn(0f, 1f))
                                pointerY > viewportHeight - scrollThreshold ->
                                    maxScrollSpeed * (1f - ((viewportHeight - pointerY) / scrollThreshold).coerceIn(0f, 1f))
                                else -> 0f
                            }

                            if (scrollSpeed != 0f) {
                                if (autoscrollJob == null) {
                                    autoscrollJob = scope.launch {
                                        while (draggedKey != null) {
                                            state.scrollBy(scrollSpeed)
                                            delay(10)
                                        }
                                    }
                                }
                            } else {
                                autoscrollJob?.cancel()
                                autoscrollJob = null
                            }
                        }
                    },
                    onDragEnd = {
                        autoscrollJob?.cancel()
                        autoscrollJob = null
                        currentOnDragEnd(currentItems)
                        draggedKey = null
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        autoscrollJob?.cancel()
                        autoscrollJob = null
                        currentOnDragEnd(currentItems)
                        draggedKey = null
                        dragOffset = 0f
                    }
                )
            }
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> key(item) }
            ) { _, item ->
                val isDragging = draggedKey == key(item)
                val zIndex = if (isDragging) 10f else 0f
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 0.dp,
                    label = "reorder_elevation"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(zIndex)
                        .graphicsLayer {
                            if (isDragging) {
                                translationY = dragOffset
                                scaleX = 1.02f
                                scaleY = 1.02f
                            }
                        }
                ) {
                    // Surface provides elevation shadows and tonal coloring
                    Surface(
                        tonalElevation = elevation,
                        shadowElevation = elevation,
                        shape = RoundedCornerShape(if (isDragging) 16.dp else 0.dp),
                        color = Color.Transparent
                    ) {
                        itemContent(item, isDragging)
                    }
                }
            }
        }
    }
}
