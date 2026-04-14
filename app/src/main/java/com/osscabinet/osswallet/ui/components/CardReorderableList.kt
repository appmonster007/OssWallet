package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

@Composable
fun <T> CardReorderableList(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    itemContent: @Composable LazyItemScope.(item: T, isDragging: Boolean, dragModifier: Modifier) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    var draggedKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    
    val currentItems by rememberUpdatedState(items)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentKeySelector by rememberUpdatedState(key)

    val spacingPx = with(density) { verticalArrangement.spacing.toPx() }

    LazyColumn(
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        modifier = modifier
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> key(item) }
        ) { index, item ->
            val isDragging = draggedKey == key(item)
            val zIndex = if (isDragging) 10f else 0f
            
            val scale by animateFloatAsState(
                targetValue = if (isDragging) 1.05f else 1.0f,
                label = "drag_scale"
            )
            
            val elevation by animateDpAsState(
                targetValue = if (isDragging) 12.dp else 0.dp,
                label = "drag_elevation"
            )

            // Fluid swap offset for non-dragged items
            val itemOffset by animateFloatAsState(
                targetValue = if (!isDragging && draggedKey != null) {
                    val draggedIndex = currentItems.indexOfFirst { currentKeySelector(it) == draggedKey }
                    if (draggedIndex != -1) {
                        val layoutInfo = state.layoutInfo
                        val draggedInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggedKey }
                        val currentInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key(item) }
                        
                        if (draggedInfo != null && currentInfo != null) {
                            val draggedCenter = draggedInfo.offset + (draggedInfo.size / 2) + dragOffset
                            val currentCenter = currentInfo.offset + (currentInfo.size / 2)
                            
                            if (draggedIndex < index && draggedCenter > currentCenter) {
                                -draggedInfo.size.toFloat() - spacingPx // Glide up
                            } else if (draggedIndex > index && draggedCenter < currentCenter) {
                                draggedInfo.size.toFloat() + spacingPx // Glide down
                            } else 0f
                        } else 0f
                    } else 0f
                } else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "fluid_swap_offset"
            )

            val dragModifier = Modifier.pointerInput(key(item)) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        draggedKey = key(item)
                        dragOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                        
                        // Check for swap
                        val currentIndex = currentItems.indexOfFirst { currentKeySelector(it) == draggedKey }
                        if (currentIndex != -1) {
                            val layoutInfo = state.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val currentItemInfo = visibleItems.firstOrNull { it.key == draggedKey }
                            
                            if (currentItemInfo != null) {
                                val draggedItemCenter = currentItemInfo.offset + (currentItemInfo.size / 2) + dragOffset
                                
                                val targetItem = visibleItems.firstOrNull { other ->
                                    if (other.key == draggedKey) return@firstOrNull false
                                    val otherCenter = other.offset + (other.size / 2)
                                    
                                    if (dragOffset > 0 && other.index > currentIndex) {
                                        draggedItemCenter > otherCenter
                                    } else if (dragOffset < 0 && other.index < currentIndex) {
                                        draggedItemCenter < otherCenter
                                    } else false
                                }
                                
                                if (targetItem != null) {
                                    val newList = currentItems.toMutableList()
                                    val element = newList.removeAt(currentIndex)
                                    newList.add(targetItem.index, element)
                                    
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    
                                    // Root Cause Fix: Correctly calculate the offset jump when swapping items of different sizes.
                                    // When swapping, the dragOffset must be adjusted by the relative distance between items.
                                    val offsetAdjustment = if (targetItem.index > currentIndex) {
                                        targetItem.size + spacingPx
                                    } else {
                                        -(targetItem.size + spacingPx)
                                    }
                                    
                                    dragOffset -= offsetAdjustment
                                    currentOnReorder(newList)
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        draggedKey = null
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        draggedKey = null
                        dragOffset = 0f
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(zIndex)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else itemOffset
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                Surface(
                    tonalElevation = elevation,
                    shadowElevation = elevation,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent
                ) {
                    itemContent(item, isDragging, dragModifier)
                }
            }
        }
    }
}
