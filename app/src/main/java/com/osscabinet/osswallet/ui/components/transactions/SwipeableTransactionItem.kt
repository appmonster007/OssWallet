package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.roundToInt
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.components.getIconByName

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
    onEvent: (TransactionItemEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var swipeDirection by remember { mutableStateOf(SwipeDirection.NONE) }
    
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    
    // Thresholds for action reveals
    val actionItemSizePx = with(density) { ACTION_ITEM_SIZE.dp.toPx() }
    val fullSwipeThresholdPx = actionItemSizePx * 1.5f
    val leftSwipeThresholdPx = actionItemSizePx * 3.2f // Reveal 3 actions (Edit, Copy, Delete)
    
    val animatedOffset by animateFloatAsState(
        targetValue = if (isRevealed) {
            if (swipeDirection == SwipeDirection.RIGHT) fullSwipeThresholdPx else -leftSwipeThresholdPx
        } else offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "swipeOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius))
            .background(AppColors.recessedSurface)
    ) {
        // Background Actions: Left side (revealed on Swipe Right)
        if (offsetX > 0 || (isRevealed && swipeDirection == SwipeDirection.RIGHT)) {
            StatusActionsRow(
                currentStatus = transaction.status,
                onStatusChange = { onEvent(TransactionItemEvent.StatusChange(transaction.id, it)) },
                revealFraction = (abs(animatedOffset) / fullSwipeThresholdPx).coerceIn(0f, 1f),
                onDismiss = { /* In this implementation, swipe complete handles dismissal */ },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        // Background Actions: Right side (revealed on Swipe Left)
        if (offsetX < 0 || (isRevealed && swipeDirection == SwipeDirection.LEFT)) {
            TransactionActionsRow(
                onEdit = { onEvent(TransactionItemEvent.Expand(transaction.id)) },
                onCopy = { onEvent(TransactionItemEvent.Clone(transaction.id)) },
                onDelete = { onEvent(TransactionItemEvent.Delete(transaction.id)) },
                revealFraction = (abs(animatedOffset) / leftSwipeThresholdPx).coerceIn(0f, 1f),
                onDismiss = { /* In this implementation, swipe complete handles dismissal */ },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Foreground Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .background(AppColors.cardSurface)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = if (swipeDirection == SwipeDirection.RIGHT) fullSwipeThresholdPx else leftSwipeThresholdPx
                            if (abs(offsetX) > threshold / 2) {
                                onEvent(TransactionItemEvent.FullSwipeRight(transaction.id)) // Fallback if no specific swipe event
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, delta ->
                            val newOffset = offsetX + delta
                            
                            // Determine direction if not already set
                            if (swipeDirection == SwipeDirection.NONE && abs(newOffset) > 10f) {
                                swipeDirection = if (newOffset > 0f) SwipeDirection.RIGHT else SwipeDirection.LEFT
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            
                            // Apply resistance when pulling beyond thresholds
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
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun StatusActionsRow(
    currentStatus: TransactionStatus,
    onStatusChange: (TransactionStatus) -> Unit,
    revealFraction: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(start = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransactionStatus.entries.forEach { status ->
            val isSelected = currentStatus == status
            val statusColor = try { Color(android.graphics.Color.parseColor(status.colorHex)) } catch (_: Exception) { AppColors.primary }
            
            ExpressiveActionButton(
                icon = getIconByName(status.iconName),
                label = status.displayName,
                containerColor = if (isSelected) statusColor else statusColor.copy(alpha = 0.15f),
                contentColor = if (isSelected) Color.White else statusColor,
                scale = revealFraction,
                onClick = {
                    onStatusChange(status)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun TransactionActionsRow(
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    revealFraction: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpressiveActionButton(
            icon = Icons.Rounded.Edit,
            label = "Edit",
            containerColor = AccentBlue.copy(alpha = 0.15f),
            contentColor = AccentBlue,
            scale = revealFraction,
            onClick = { onEdit(); onDismiss() }
        )
        ExpressiveActionButton(
            icon = Icons.Rounded.ContentCopy,
            label = "Copy",
            containerColor = PositiveAmountColor.copy(alpha = 0.15f),
            contentColor = PositiveAmountColor,
            scale = revealFraction,
            onClick = { onCopy(); onDismiss() }
        )
        ExpressiveActionButton(
            icon = Icons.Rounded.DeleteOutline,
            label = "Delete",
            containerColor = AppColors.error.copy(alpha = 0.15f),
            contentColor = AppColors.error,
            scale = revealFraction,
            onClick = { onDelete(); onDismiss() }
        )
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .scale(scale)
            .alpha(scale)
    ) {
        Surface(
            onClick = onClick,
            color = containerColor,
            shape = CircleShape,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = label.uppercase(),
            style = AppTextStyles.transactionSwipeAction,
            color = contentColor.copy(alpha = 0.8f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExpressiveActionButtonPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ExpressiveActionButton(
                icon = Icons.Rounded.Edit,
                label = "Edit",
                containerColor = AppColors.primaryContainer,
                contentColor = AppColors.primary,
                scale = 1f,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SwipeableTransactionItemPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SwipeableTransactionItem(
                transaction = Transaction(
                    id = 1L,
                    name = "MacDonalds",
                    amount = BigDecimal("100.00"),
                    timestamp = System.currentTimeMillis(),
                    currency = "USD",
                    transactionType = TransactionType.EXPENSE,
                    accountId = "acc1",
                    categoryId = 1002L,
                    note = "Lunch"
                ),
                isRevealed = false,
                onEvent = {}
            ) {
                Text("Transaction Content")
            }
        }
    }
}
