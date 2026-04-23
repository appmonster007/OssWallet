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
const val ACTION_ITEM_SIZE = 56

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

    val density = LocalDensity.current
    val actionItemSizePx = with(density) { ACTION_ITEM_SIZE.dp.toPx() }
    val startActionSizePx = with(density) { (ACTION_ITEM_SIZE.dp * 3).toPx() }
    val endActionSizePx = with(density) { (ACTION_ITEM_SIZE.dp * 2).toPx() }

    // Sync external isRevealed state with local offset
    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            if (swipeDirection == SwipeDirection.RIGHT) {
                offsetX = endActionSizePx
            } else if (swipeDirection == SwipeDirection.LEFT) {
                offsetX = -startActionSizePx
            }
        } else {
            offsetX = 0f
            swipeDirection = SwipeDirection.NONE
        }
    }

    // Animate offset changes smoothly
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = FastOutSlowInEasing),
        label = "offsetAnimation"
    )

    // Background color based on drag progress
    val progress = when {
        animatedOffset > 0f -> animatedOffset / endActionSizePx
        animatedOffset < 0f -> animatedOffset / -startActionSizePx
        else -> 0f
    }.coerceIn(0f, 1f)

    val cardBgColor = when {
        swipeDirection == SwipeDirection.LEFT && isRevealed -> MaterialTheme.colorScheme.errorContainer
        swipeDirection == SwipeDirection.RIGHT && isRevealed -> MaterialTheme.colorScheme.primaryContainer
        animatedOffset > 0f -> androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer,
            progress
        )
        animatedOffset < 0f -> androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.errorContainer,
            progress
        )
        else -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Left swipe actions (Split, Clone, Delete) - visible when dragged right
        if (animatedOffset < -actionItemSizePx || (isRevealed && swipeDirection == SwipeDirection.LEFT)) {
            TransactionActionsRow(
                onSplit = onSplit,
                onClone = onClone,
                onDelete = onDelete,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Right swipe actions (Status change) - visible when dragged left
        if (animatedOffset > actionItemSizePx || (isRevealed && swipeDirection == SwipeDirection.RIGHT)) {
            StatusActionsRow(
                currentStatus = transaction.status,
                onStatusChange = onStatusChange,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Check if full swipe was performed
                            val fullSwipeThreshold = with(density) { 200.dp.toPx() }
                            if (abs(offsetX) >= fullSwipeThreshold) {
                                if (offsetX > 0) {
                                    onFullSwipeRight()
                                }
                            }

                            // Snap to nearest anchor
                            val halfThreshold = with(density) { 80.dp.toPx() }
                            when {
                                offsetX > halfThreshold -> {
                                    swipeDirection = SwipeDirection.RIGHT
                                    offsetX = endActionSizePx
                                    onExpandRight()
                                }
                                offsetX < -halfThreshold -> {
                                    swipeDirection = SwipeDirection.LEFT
                                    offsetX = -startActionSizePx
                                    onExpandLeft()
                                }
                                else -> {
                                    swipeDirection = SwipeDirection.NONE
                                    offsetX = 0f
                                    onCollapse()
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        val original = Offset(offsetX, 0f)
                        val summed = original + Offset(x = dragAmount, y = 0f)

                        // Determine swipe direction based on drag
                        val newDirection = when {
                            dragAmount > 0 && swipeDirection == SwipeDirection.NONE -> SwipeDirection.RIGHT
                            dragAmount < 0 && swipeDirection == SwipeDirection.NONE -> SwipeDirection.LEFT
                            else -> swipeDirection
                        }

                        // Coerce offset within bounds
                        val maxOffset = startActionSizePx
                        val newValue = Offset(
                            x = summed.x.coerceIn(-maxOffset, endActionSizePx),
                            y = 0f
                        )

                        if (change.positionChange() != Offset.Zero) change.consume()
                        offsetX = newValue.x
                        swipeDirection = newDirection
                    }
                },
            color = cardBgColor,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = if (isRevealed) 4.dp else 1.dp,
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
            val (icon, bgColor, iconColor) = when (status) {
                TransactionStatus.CLEARED -> Triple(
                    Icons.Rounded.Check,
                    Color(0xFF4CAF50),
                    Color.White
                )
                TransactionStatus.RECONCILE -> Triple(
                    Icons.Rounded.Sync,
                    Color(0xFF2196F3),
                    Color.White
                )
                TransactionStatus.UNCLEARED -> Triple(
                    Icons.Rounded.Schedule,
                    Color(0xFFFF9800),
                    Color.White
                )
            }

            Box(
                modifier = Modifier
                    .size(ACTION_ITEM_SIZE.dp)
                    .background(
                        color = bgColor,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    modifier = Modifier.size(ACTION_ITEM_SIZE.dp),
                    onClick = { onStatusChange(status) }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = status.displayName,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
        // Split action
        Box(
            modifier = Modifier
                .size(ACTION_ITEM_SIZE.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                modifier = Modifier.size(ACTION_ITEM_SIZE.dp),
                onClick = onSplit
            ) {
                Icon(
                    imageVector = Icons.Rounded.CallSplit,
                    contentDescription = "Split transaction",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Clone action
        Box(
            modifier = Modifier
                .size(ACTION_ITEM_SIZE.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                modifier = Modifier.size(ACTION_ITEM_SIZE.dp),
                onClick = onClone
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Clone transaction",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Delete action
        Box(
            modifier = Modifier
                .size(ACTION_ITEM_SIZE.dp)
                .background(
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                modifier = Modifier.size(ACTION_ITEM_SIZE.dp),
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete transaction",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
