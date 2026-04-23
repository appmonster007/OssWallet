package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import kotlin.math.abs
import kotlin.math.roundToInt

const val ANIMATION_DURATION = 300
const val MIN_DRAG_AMOUNT = 10
const val ACTION_ITEM_SIZE = 56
const val HALF_SWIPE_THRESHOLD = 100f
const val FULL_SWIPE_THRESHOLD = 200f

enum class SwipeDirection {
    NONE,
    LEFT,
    RIGHT
}

@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    isRevealed: Boolean,
    onExpandLeft: () -> Unit,
    onExpandRight: () -> Unit,
    onCollapse: () -> Unit,
    onFullSwipeRight: () -> Unit,
    onStatusChange: (TransactionStatus) -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit,
    onSplit: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var swipeDirection by remember { mutableStateOf(SwipeDirection.NONE) }
    
    val transitionState = remember {
        MutableTransitionState(isRevealed).apply {
            targetState = !isRevealed
        }
    }
    
    val transition = updateTransition(transitionState, "cardTransition")
    
    val cardBgColor by transition.animateColor(
        label = "cardBgColorTransition",
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION) },
        targetValueByState = {
            when {
                isRevealed && swipeDirection == SwipeDirection.LEFT -> MaterialTheme.colorScheme.errorContainer
                isRevealed && swipeDirection == SwipeDirection.RIGHT -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        }
    )
    
    val density = LocalDensity.current
    val halfSwipeThresholdPx = with(density) { HALF_SWIPE_THRESHOLD.dp.toPx() }
    val fullSwipeThresholdPx = with(density) { FULL_SWIPE_THRESHOLD.dp.toPx() }
    val actionItemSizePx = with(density) { ACTION_ITEM_SIZE.dp.toPx() }
    
    val offsetTransition by transition.animateFloat(
        label = "cardOffsetTransition",
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION) },
        targetValueByState = {
            when {
                isRevealed && swipeDirection == SwipeDirection.LEFT -> -actionItemSizePx * 3 - offsetX
                isRevealed && swipeDirection == SwipeDirection.RIGHT -> actionItemSizePx * 2 + offsetX
                else -> -offsetX
            }
        }
    )
    
    val cardElevation by transition.animateDp(
        label = "cardElevation",
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION) },
        targetValueByState = { if (isRevealed) 8.dp else 1.dp }
    )
    
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Left swipe actions (Split, Clone, Delete)
        if (isRevealed && swipeDirection == SwipeDirection.LEFT) {
            TransactionActionsRow(
                onSplit = onSplit,
                onClone = onClone,
                onDelete = onDelete,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        
        // Right swipe actions (Status change)
        if (isRevealed && swipeDirection == SwipeDirection.RIGHT) {
            StatusActionsRow(
                currentStatus = transaction.status,
                onStatusChange = onStatusChange,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset((offsetX + offsetTransition).roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Check if full swipe was performed
                            if (abs(offsetX) >= fullSwipeThresholdPx) {
                                if (offsetX > 0) {
                                    onFullSwipeRight()
                                }
                            }
                            // Reset to revealed or collapsed state
                            offsetX = 0f
                        }
                    ) { change, dragAmount ->
                        val original = Offset(offsetX, 0f)
                        val summed = original + Offset(x = dragAmount, y = 0f)
                        
                        // Determine swipe direction
                        val newDirection = when {
                            dragAmount > 0 -> SwipeDirection.RIGHT
                            dragAmount < 0 -> SwipeDirection.LEFT
                            else -> swipeDirection
                        }
                        
                        // Coerce offset within bounds
                        val maxOffset = actionItemSizePx * 3
                        val newValue = Offset(
                            x = summed.x.coerceIn(-maxOffset, maxOffset),
                            y = 0f
                        )
                        
                        // Check thresholds
                        when {
                            newValue.x >= halfSwipeThresholdPx && swipeDirection != SwipeDirection.RIGHT -> {
                                swipeDirection = SwipeDirection.RIGHT
                                onExpandRight()
                            }
                            newValue.x <= -halfSwipeThresholdPx && swipeDirection != SwipeDirection.LEFT -> {
                                swipeDirection = SwipeDirection.LEFT
                                onExpandLeft()
                            }
                            abs(newValue.x) < halfSwipeThresholdPx && isRevealed -> {
                                swipeDirection = SwipeDirection.NONE
                                onCollapse()
                            }
                        }
                        
                        if (change.positionChange() != Offset.Zero) change.consume()
                        offsetX = newValue.x
                    }
                },
            color = cardBgColor,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = cardElevation,
            content = { content() }
        )
    }
}

@Composable
fun StatusActionsRow(
    currentStatus: TransactionStatus,
    onStatusChange: (TransactionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show statuses that are NOT currently set
        val availableStatuses = TransactionStatus.values().filter { it != currentStatus }
        
        availableStatuses.forEach { status ->
            val icon = when (status) {
                TransactionStatus.CLEARED -> Icons.Rounded.Check
                TransactionStatus.RECONCILE -> Icons.Rounded.Sync
                TransactionStatus.UNCLEARED -> Icons.Rounded.Schedule
            }
            val color = when (status) {
                TransactionStatus.CLEARED -> Color(0xFF4CAF50)
                TransactionStatus.RECONCILE -> Color(0xFF2196F3)
                TransactionStatus.UNCLEARED -> Color(0xFFFF9800)
            }
            
            IconButton(
                modifier = Modifier.size(ACTION_ITEM_SIZE.dp),
                onClick = { onStatusChange(status) }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = status.displayName,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionActionsRow(
    onSplit: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
    ) {
        IconButton(
            modifier = Modifier.size(ACTION_ITEM_SIZE.dp),
            onClick = onSplit
        ) {
            Icon(
                imageVector = Icons.Rounded.CallSplit,
                contentDescription = "Split transaction",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        IconButton(
            modifier = Modifier.size(ACTION_ITEM_SIZE.dp),
            onClick = onClone
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Clone transaction",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        
        IconButton(
            modifier = Modifier.size(ACTION_ITEM_SIZE.dp),
            onClick = onDelete
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete transaction",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
