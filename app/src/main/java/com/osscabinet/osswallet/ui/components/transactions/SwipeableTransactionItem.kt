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
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.WarningColor
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
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.roundToInt
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
            .clip(RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius))
            .background(AppColors.cardSurface)
    ) {
        // Actions Layer (Background)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = AeonTheme.dimensions.listPadding),
            contentAlignment = Alignment.Center
        ) {
            // Right Swipe Content (Reveals Status Actions on the Left)
            if (animatedOffset > 0) {
                StatusActionsRow(
                    currentStatus = transaction.status,
                    onStatusChange = { status -> onEvent(TransactionItemEvent.StatusChange(transaction.id, status)) },
                    progress = progress,
                    onCollapse = { onEvent(TransactionItemEvent.Collapse(transaction.id)) },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            // Left Swipe Content (Reveals Item Actions on the Right)
            if (animatedOffset < 0) {
                TransactionActionsRow(
                    onSplit = { onEvent(TransactionItemEvent.Split(transaction.id)) },
                    onClone = { onEvent(TransactionItemEvent.Clone(transaction.id)) },
                    onDelete = { onEvent(TransactionItemEvent.Delete(transaction.id)) },
                    progress = progress,
                    onCollapse = { onEvent(TransactionItemEvent.Collapse(transaction.id)) },
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
                                        onEvent(TransactionItemEvent.FullSwipeRight(transaction.id))
                                        offsetX = 0f
                                        swipeDirection = SwipeDirection.NONE
                                    } else if (offsetX > rightSwipeThresholdPx / 2) {
                                        offsetX = rightSwipeThresholdPx
                                        onEvent(TransactionItemEvent.Expand(transaction.id))
                                    } else {
                                        offsetX = 0f
                                        swipeDirection = SwipeDirection.NONE
                                        onEvent(TransactionItemEvent.Collapse(transaction.id))
                                    }
                                }
                                SwipeDirection.LEFT -> {
                                    if (abs(offsetX) > leftSwipeThresholdPx / 2) {
                                        offsetX = -leftSwipeThresholdPx
                                        onEvent(TransactionItemEvent.Expand(transaction.id))
                                    } else {
                                        offsetX = 0f
                                        swipeDirection = SwipeDirection.NONE
                                        onEvent(TransactionItemEvent.Collapse(transaction.id))
                                    }
                                }
                                else -> {
                                    offsetX = 0f
                                    onEvent(TransactionItemEvent.Collapse(transaction.id))
                                }
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                            swipeDirection = SwipeDirection.NONE
                            onEvent(TransactionItemEvent.Collapse(transaction.id))
                        }
                    )
                },
            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
            color = AppColors.panelSurface,
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
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val statuses = TransactionStatus.values().filter { it != currentStatus }
        statuses.forEachIndexed { index, status ->
            val (icon, color) = when (status) {
                TransactionStatus.CLEARED -> Icons.Rounded.CheckCircle to PositiveAmountColor
                TransactionStatus.RECONCILE -> Icons.Rounded.AccountBalance to AccentBlue
                TransactionStatus.UNCLEARED -> Icons.Rounded.Pending to WarningColor
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
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val actions = listOf(
            Triple(Icons.Rounded.CallSplit, "Split", AppColors.primary),
            Triple(Icons.Rounded.ContentCopy, "Clone", AppColors.tertiary),
            Triple(Icons.Rounded.DeleteForever, "Delete", AppColors.error)
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
                Icon(icon, contentDescription = label, modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm))
            }
        }
        Text(
            text = label,
            style = AppTextStyles.transactionSwipeAction.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            ),
            color = contentColor.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = AeonTheme.dimensions.spacingXs)
        )
    }
}

// --- Previews ---

@Preview(name = "No Swipe", showBackground = true)
@Composable
fun PreviewSwipeableTransactionItem_None() {
    MaterialTheme {
        Box(Modifier.padding(AeonTheme.dimensions.screenPadding)) {
            SwipeableTransactionItem(
                transaction = mockTransactionPreview(),
                isRevealed = false,
                onEvent = {}
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
        Box(Modifier.padding(AeonTheme.dimensions.screenPadding)) {
            val density = LocalDensity.current
            val offsetPx = with(density) { -192.dp.toPx() }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius))
                    .background(AppColors.cardSurface)
            ) {
                TransactionActionsRow(
                    onSplit = {}, onClone = {}, onDelete = {},
                    progress = 1f,
                    onCollapse = {},
                    modifier = Modifier.align(Alignment.CenterEnd).padding(horizontal = AeonTheme.dimensions.listPadding)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(offsetPx.roundToInt(), 0) },
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    color = AppColors.panelSurface,
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
        Box(Modifier.padding(AeonTheme.dimensions.screenPadding)) {
            val density = LocalDensity.current
            val offsetPx = with(density) { 128.dp.toPx() }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius))
                    .background(AppColors.cardSurface)
            ) {
                StatusActionsRow(
                    currentStatus = TransactionStatus.CLEARED,
                    onStatusChange = {},
                    progress = 1f,
                    onCollapse = {},
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = AeonTheme.dimensions.listPadding)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(offsetPx.roundToInt(), 0) },
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    color = AppColors.panelSurface,
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
        modifier = Modifier.fillMaxWidth().padding(AeonTheme.dimensions.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(AeonTheme.dimensions.iconSizeXl).background(AppColors.accentContainer.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ShoppingBag, null, tint = AppColors.primary, modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm))
        }
        Spacer(Modifier.width(AeonTheme.dimensions.spacingMd))
        Column(Modifier.weight(1f)) {
            Box(Modifier.size(140.dp, 16.dp).background(AppColors.divider.copy(alpha = 0.5f), RoundedCornerShape(4.dp)))
            Spacer(Modifier.height(AeonTheme.dimensions.spacingXs))
            Box(Modifier.size(90.dp, 12.dp).background(AppColors.divider.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
        }
        Text("$ 45.00", style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Black, color = AppColors.error))
    }
}

private fun mockTransactionPreview() = Transaction(
    id = 1,
    name = "Mock Transaction",
    amount = BigDecimal("45.00"),
    timestamp = System.currentTimeMillis(),
    currency = "USD",
    transactionType = TransactionType.EXPENSE,
    accountId = "acc_1",
    categoryId = 1002L
)
