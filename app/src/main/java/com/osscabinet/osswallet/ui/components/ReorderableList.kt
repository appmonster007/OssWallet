package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun <T> ReorderableList(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    onDragEnd: (List<T>) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    itemContent: @Composable (T, Boolean) -> Unit
) {
    val state = rememberLazyListState()
    var draggedKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    
    val scope = rememberCoroutineScope()
    var autoscrollJob by remember { mutableStateOf<Job?>(null) }

    val currentItems by rememberUpdatedState(items)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

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
                            }?.also { item ->
                                draggedKey = key(currentItems[item.index])
                                dragOffset = 0f
                            }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dKey = draggedKey ?: return@detectDragGesturesAfterLongPress
                        dragOffset += dragAmount.y
                        
                        val currentIndex = currentItems.indexOfFirst { key(it) == dKey }
                        if (currentIndex != -1) {
                            val layoutInfo = state.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val currentItemInfo = visibleItems.firstOrNull { it.index == currentIndex }
                            
                            if (currentItemInfo != null) {
                                // Find target for swap
                                val targetItem = visibleItems.firstOrNull { item ->
                                    val threshold = 0.5f
                                    if (dragOffset > 0 && item.index > currentIndex) {
                                        currentItemInfo.offset + currentItemInfo.size + dragOffset > item.offset + item.size * threshold
                                    } else if (dragOffset < 0 && item.index < currentIndex) {
                                        currentItemInfo.offset + dragOffset < item.offset + item.size * (1 - threshold)
                                    } else {
                                        false
                                    }
                                }

                                if (targetItem != null) {
                                    val newList = currentItems.toMutableList()
                                    val element = newList.removeAt(currentIndex)
                                    newList.add(targetItem.index, element)
                                    
                                    // Adjust dragOffset to keep item under finger
                                    dragOffset -= (targetItem.offset - currentItemInfo.offset)
                                    
                                    currentOnReorder(newList)
                                }
                            }
                            
                            // Autoscroll logic
                            val viewportHeight = layoutInfo.viewportSize.height
                            val scrollThreshold = 100f
                            val pointerY = (currentItemInfo?.offset ?: 0) + (currentItemInfo?.size ?: 0) / 2 + dragOffset
                            
                            if (pointerY < scrollThreshold) {
                                if (autoscrollJob == null) {
                                    autoscrollJob = scope.launch {
                                        while (draggedKey != null) {
                                            state.scrollBy(-15f)
                                            delay(10)
                                        }
                                    }
                                }
                            } else if (pointerY > viewportHeight - scrollThreshold) {
                                if (autoscrollJob == null) {
                                    autoscrollJob = scope.launch {
                                        while (draggedKey != null) {
                                            state.scrollBy(15f)
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
            itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
                val isDragging = draggedKey == key(item)
                val zIndex = if (isDragging) 1f else 0f
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "reorder_elevation")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(zIndex)
                        .graphicsLayer {
                            if (isDragging) {
                                translationY = dragOffset
                            }
                        }
                ) {
                    Surface(tonalElevation = elevation) {
                        itemContent(item, isDragging)
                    }
                }
            }
        }
    }
}
