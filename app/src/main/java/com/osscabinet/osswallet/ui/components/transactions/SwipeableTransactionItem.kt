package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.roundToInt

const val ANIMATION_DURATION = 400
const val ACTION_ITEM_SIZE = 64

enum class SwipeDirection {
    NONE,
    LEFT, // Dragged towards left (reveals right actions)
    RIGHT // Dragged towards right (reveals left actions)
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
    
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    // Expressive motion configuration
    val leftSwipeThresholdPx = with(density) { (ACTION_ITEM_SIZE.dp * 3).toPx() } // Revealing right actions
    val rightSwipeThresholdPx = with(density) { (ACTION_ITEM_SIZE.dp * 2).toPx() } // Revealing left actions
    val fullSwipeThresholdPx = with(density) { 220.dp.toPx() }

    // External state synchronization
    LaunchedEffect(isRevealed) {
        if (!isRevealed) {
            offsetX = 0f
            swipeDirection = SwipeDirection.NONE
        }
    }

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipeOffset"
    )

    // Calculate progress for expressive scaling (0 to 1)
    val progress = when {
        animatedOffset > 0f -> (animatedOffset / rightSwipeThresholdPx).coerceIn(0f, 1f)
        animatedOffset < 0f -> (abs(animatedOffset) / leftSwipeThresholdPx).coerceIn(0f, 1f)
        else -> 0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Actions Layer (Background)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Right Swipe Content (Reveals Status Actions on the Left)
            if (animatedOffset > 0) {
                StatusActionsRow(
                    currentStatus = transaction.status,
                    onStatusChange = onStatusChange,
                    progress = progress,
                    onCollapse = onCollapse,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            // Left Swipe Content (Reveals Item Actions on the Right)
            if (animatedOffset < 0) {
                TransactionActionsRow(
                    onSplit = onSplit,
                    onClone = onClone,
                    onDelete = onDelete,
                    progress = progress,
                    onCollapse = onCollapse,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        // Main Item Layer (Foreground)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            val newOffset = offsetX + dragAmount
                            
                            // Initialize direction
                            if (swipeDirection == SwipeDirection.NONE && abs(dragAmount) > 0.5f) {
                                swipeDirection = if (dragAmount > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                            }

                            // Constrain and apply offset with resistance
                            offsetX = when (swipeDirection) {
                                SwipeDirection.RIGHT -> {
                                    if (newOffset > fullSwipeThresholdPx) {
                                        fullSwipeThresholdPx + (newOffset - fullSwipeThresholdPx) * 0.3f
                                    } else newOffset.coerceAtLeast(0f)
                                }
                                SwipeDirection.LEFT -> {
                                    if (newOffset < -leftSwipeThresholdPx) {
                                        -leftSwipeThresholdPx + (newOffset + leftSwipeThresholdPx) * 0.3f
                                    } else newOffset.coerceAtMost(0f)
                                }
                                else -> newOffset
                            }
                        },
                        onDragEnd = {
                            when (swipeDirection) {
                                SwipeDirection.RIGHT -> {
                                    if (offsetX >= fullSwipeThresholdPx) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onFullSwipeRight()
                                        offsetX = 0f
                                        swipeDirection = SwipeDirection.NONE
                                    } else if (offsetX > rightSwipeThresholdPx / 2) {
                                        offsetX = rightSwipeThresholdPx
                                        onExpandRight()
                                    } else {
                                        offsetX = 0f
                                        swipeDirection = SwipeDirection.NONE
                                        onCollapse()
                                    }
                                }
                                SwipeDirection.LEFT -> {
                                    if (abs(offsetX) > leftSwipeThresholdPx / 2) {
                                        offsetX = -leftSwipeThresholdPx
                                        onExpandLeft()
                                    } else {
                                        offsetX = 0f
                                        swipeDirection = SwipeDirection.NONE
                                        onCollapse()
                                    }
                                }
                                else -> {
                                    offsetX = 0f
                                    onCollapse()
                                }
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                            swipeDirection = SwipeDirection.NONE
                            onCollapse()
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (animatedOffset != 0f) 8.dp else 0.dp,
            shadowElevation = if (animatedOffset != 0f) 4.dp else 0.dp
        ) {
            content()
        }
    }
}

@Composable
fun StatusActionsRow(
    currentStatus: TransactionStatus,
    onStatusChange: (TransactionStatus) -> Unit,
    progress: Float,
    onCollapse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val statuses = TransactionStatus.values().filter { it != currentStatus }
        statuses.forEachIndexed { index, status ->
            val (icon, color) = when (status) {
                TransactionStatus.CLEARED -> Icons.Rounded.CheckCircle to Color(0xFF4CAF50)
                TransactionStatus.RECONCILE -> Icons.Rounded.AccountBalance to Color(0xFF2196F3)
                TransactionStatus.UNCLEARED -> Icons.Rounded.Pending to Color(0xFFFF9800)
            }

            val itemProgress = (progress * 1.5f - (index * 0.2f)).coerceIn(0f, 1f)

            ExpressiveActionButton(
                icon = icon,
                label = status.displayName,
                containerColor = color.copy(alpha = 0.12f),
                contentColor = color,
                scale = itemProgress,
                onClick = { onStatusChange(status) }
            )
        }
    }
}

@Composable
fun TransactionActionsRow(
    onSplit: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit,
    progress: Float,
    onCollapse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val actions = listOf(
            Triple(Icons.Rounded.CallSplit, "Split", MaterialTheme.colorScheme.primary),
            Triple(Icons.Rounded.ContentCopy, "Clone", MaterialTheme.colorScheme.tertiary),
            Triple(Icons.Rounded.DeleteForever, "Delete", MaterialTheme.colorScheme.error)
        )

        actions.forEachIndexed { index, (icon, label, color) ->
            val itemProgress = (progress * 1.5f - (index * 0.15f)).coerceIn(0f, 1f)

            ExpressiveActionButton(
                icon = icon,
                label = label,
                containerColor = color.copy(alpha = 0.12f),
                contentColor = color,
                scale = itemProgress,
                onClick = {
                    when (label) {
                        "Split" -> onSplit()
                        "Clone" -> onClone()
                        else -> onDelete()
                    }
                    onCollapse()
                }
            )
        }
    }
}

@Composable
fun ExpressiveActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    scale: Float,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable { onClick() },
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            ),
            color = contentColor.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// --- Previews ---

@Preview(name = "No Swipe", showBackground = true)
@Composable
fun PreviewSwipeableTransactionItem_None() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            SwipeableTransactionItem(
                transaction = mockTransactionPreview(),
                isRevealed = false,
                onExpandLeft = {}, onExpandRight = {}, onCollapse = {}, onFullSwipeRight = {},
                onStatusChange = {}, onClone = {}, onDelete = {}, onSplit = {}
            ) {
                MockContentPreview()
            }
        }
    }
}

@Preview(name = "Left Swipe (Actions)", showBackground = true)
@Composable
fun PreviewSwipeableTransactionItem_LeftSwipe() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            val density = LocalDensity.current
            val offsetPx = with(density) { -192.dp.toPx() }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                TransactionActionsRow(
                    onSplit = {}, onClone = {}, onDelete = {},
                    progress = 1f,
                    onCollapse = {},
                    modifier = Modifier.align(Alignment.CenterEnd).padding(horizontal = 12.dp)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(offsetPx.roundToInt(), 0) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp
                ) {
                    MockContentPreview()
                }
            }
        }
    }
}

@Preview(name = "Right Swipe (Status)", showBackground = true)
@Composable
fun PreviewSwipeableTransactionItem_RightSwipe() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            val density = LocalDensity.current
            val offsetPx = with(density) { 128.dp.toPx() }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                StatusActionsRow(
                    currentStatus = TransactionStatus.CLEARED,
                    onStatusChange = {},
                    progress = 1f,
                    onCollapse = {},
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 12.dp)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(offsetPx.roundToInt(), 0) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp
                ) {
                    MockContentPreview()
                }
            }
        }
    }
}

@Composable
private fun MockContentPreview() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ShoppingBag, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.size(140.dp, 16.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp)))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(90.dp, 12.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
        }
        Text("$ 45.00", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
    }
}

private fun mockTransactionPreview() = Transaction(
    id = 1,
    title = "Mock Transaction",
    amount = BigDecimal("45.00"),
    category = "Shopping",
    timestamp = System.currentTimeMillis(),
    currency = "USD",
    transactionType = "expense",
    accountId = "acc_1"
)
