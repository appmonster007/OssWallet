package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Pending
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel
import com.osscabinet.ossWallet.domain.model.PlannedPaymentUiModel
import com.osscabinet.ossWallet.domain.model.RecordUiModel
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.WarningColor
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.roundToInt

const val ACTION_ITEM_SIZE = 56

enum class SwipeDirection {
    NONE,
    LEFT, // Dragged towards left (reveals right actions)
    RIGHT // Dragged towards right (reveals left actions)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionableTransactionItem(
    record: RecordUiModel,
    isRevealed: Boolean,
    onEvent: (TransactionItemEvent) -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
    isSwipeActionEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val isPlanned = record is PlannedPaymentUiModel
    var offsetX by remember { mutableFloatStateOf(0f) }
    var swipeDirection by remember { mutableStateOf(SwipeDirection.NONE) }
    var showDropdown by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Expressive motion configuration
    val leftSwipeThresholdPx =
        with(density) { (ACTION_ITEM_SIZE.dp * (if (isPlanned) 2 else if (record is ConfirmedTransactionUiModel && record.linkedTransactionIds.isNotEmpty()) 4 else 3)).toPx() } // Revealing right actions
    val rightSwipeThresholdPx =
        with(density) { (ACTION_ITEM_SIZE.dp * 2).toPx() } // Revealing left actions
    val fullSwipeThresholdPx = with(density) { 220.dp.toPx() }

    // External state synchronization
    LaunchedEffect(isRevealed) {
        if (!isRevealed) {
            offsetX = 0f
            swipeDirection = SwipeDirection.NONE
        }
    }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isRevealed) {
            if (swipeDirection == SwipeDirection.RIGHT) rightSwipeThresholdPx else -leftSwipeThresholdPx
        } else offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipeOffset"
    )

    // While swiped open, a tap collapses the row instead of opening the record.
    val onRowClick: () -> Unit = {
        if (isRevealed || animatedOffset != 0f) {
            offsetX = 0f
            swipeDirection = SwipeDirection.NONE
            onEvent(TransactionItemEvent.Collapse(record.id))
        } else {
            onEvent(TransactionItemEvent.Click(record.id))
        }
    }

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
        // Actions Layer (Background) - Only shown if swipe is enabled
        if (actionEnabled && isSwipeActionEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = AeonTheme.dimensions.listPadding),
                contentAlignment = Alignment.Center
            ) {
                // Right Swipe Content (Reveals Status Actions on the Left)
                if (animatedOffset > 0 && record is ConfirmedTransactionUiModel) {
                    StatusActionsRow(
                        currentStatus = record.status,
                        onStatusChange = { status ->
                            onEvent(
                                TransactionItemEvent.StatusChange(
                                    record.item.transaction.id,
                                    status
                                )
                            )
                        },
                        progress = progress,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }

                // Left Swipe Content (Reveals Item Actions on the Right)
                if (animatedOffset < 0) {
                    if (record is ConfirmedTransactionUiModel) {
                        TransactionActionsRow(
                            onSplit = { onEvent(TransactionItemEvent.Split(record.item.transaction.id)) },
                            onClone = { onEvent(TransactionItemEvent.Clone(record.item.transaction.id)) },
                            onDelete = { onEvent(TransactionItemEvent.Delete(record.item.transaction.id)) },
                            onJump = { onEvent(TransactionItemEvent.Jump(record.item.transaction.id)) },
                            hasLinks = record.linkedTransactionIds.isNotEmpty(),
                            progress = progress,
                            onCollapse = { onEvent(TransactionItemEvent.Collapse(record.id)) },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    } else if (record is PlannedPaymentUiModel) {
                        PlannedActionsRow(
                            onConfirm = {
                                onEvent(
                                    TransactionItemEvent.ConfirmPlanned(
                                        record.paymentId,
                                        record.timestamp
                                    )
                                )
                            },
                            onCancel = {
                                onEvent(
                                    TransactionItemEvent.CancelPlanned(
                                        record.paymentId,
                                        record.timestamp
                                    )
                                )
                            },
                            progress = progress,
                            onCollapse = { onEvent(TransactionItemEvent.Collapse(record.id)) },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        // Main Item Layer (Foreground)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .then(
                    if (actionEnabled) {
                        Modifier.combinedClickable(
                            onClick = onRowClick,
                            onLongClick = {
                                if (!isSwipeActionEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showDropdown = true
                                }
                            }
                        )
                    } else {
                        Modifier.clickable(onClick = onRowClick)
                    }
                )
                .then(
                    if (actionEnabled && isSwipeActionEnabled) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    val newOffset = offsetX + dragAmount

                                    // Initialize direction
                                    if (swipeDirection == SwipeDirection.NONE && abs(dragAmount) > 0.5f) {
                                        val direction =
                                            if (dragAmount > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT

                                        // Disable right swipe (revealing left actions) for planned payments
                                        if (isPlanned && direction == SwipeDirection.RIGHT) return@detectHorizontalDragGestures

                                        swipeDirection = direction
                                    }

                                    if (swipeDirection == SwipeDirection.NONE) return@detectHorizontalDragGestures

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

                                        else -> offsetX
                                    }
                                },
                                onDragEnd = {
                                    when (swipeDirection) {
                                        SwipeDirection.RIGHT -> {
                                            if (record is ConfirmedTransactionUiModel && offsetX >= fullSwipeThresholdPx) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onEvent(TransactionItemEvent.FullSwipeRight(record.item.transaction.id))
                                                offsetX = 0f
                                                swipeDirection = SwipeDirection.NONE
                                            } else if (offsetX > rightSwipeThresholdPx / 2) {
                                                offsetX = rightSwipeThresholdPx
                                                onEvent(TransactionItemEvent.Expand(record.id))
                                            } else {
                                                offsetX = 0f
                                                swipeDirection = SwipeDirection.NONE
                                                onEvent(TransactionItemEvent.Collapse(record.id))
                                            }
                                        }

                                        SwipeDirection.LEFT -> {
                                            if (abs(offsetX) > leftSwipeThresholdPx / 2) {
                                                offsetX = -leftSwipeThresholdPx
                                                onEvent(TransactionItemEvent.Expand(record.id))
                                            } else {
                                                offsetX = 0f
                                                swipeDirection = SwipeDirection.NONE
                                                onEvent(TransactionItemEvent.Collapse(record.id))
                                            }
                                        }

                                        else -> {
                                            offsetX = 0f
                                            onEvent(TransactionItemEvent.Collapse(record.id))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    offsetX = 0f
                                    swipeDirection = SwipeDirection.NONE
                                    onEvent(TransactionItemEvent.Collapse(record.id))
                                }
                            )
                        }
                    } else Modifier
                ),
            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
            color = AppColors.panelSurface,
            tonalElevation = if (animatedOffset != 0f) 8.dp else 0.dp,
            shadowElevation = if (animatedOffset != 0f) 4.dp else 0.dp
        ) {
            content()

            if (showDropdown) {
                ActionDropdown(
                    record = record,
                    onDismiss = { showDropdown = false },
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
fun ActionDropdown(
    record: RecordUiModel,
    onDismiss: () -> Unit,
    onEvent: (TransactionItemEvent) -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(min = 180.dp),
        containerColor = AppColors.popupSurface,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
    ) {
        if (record is ConfirmedTransactionUiModel) {
            // Header for Context
            Text(
                text = "TRANSACTION ACTIONS",
                style = AppTextStyles.sectionOverline,
                color = AppColors.accent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.5f)
            )

            // Status Actions Section
            TransactionStatus.entries.filter { it != record.status }.forEach { status ->
                val (icon, color) = when (status) {
                    TransactionStatus.CLEARED -> Icons.Rounded.CheckCircle to PositiveAmountColor
                    TransactionStatus.RECONCILE -> Icons.Rounded.AccountBalance to AccentBlue
                    TransactionStatus.UNCLEARED -> Icons.Rounded.Pending to WarningColor
                }

                DropdownMenuItem(
                    text = { Text("Mark as ${status.displayName}", style = AppTextStyles.listItem) },
                    leadingIcon = {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                        )
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = AppColors.contentPrimary,
                        leadingIconColor = color
                    ),
                    onClick = {
                        onEvent(TransactionItemEvent.StatusChange(record.item.transaction.id, status))
                        onDismiss()
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.5f)
            )

            // Management Actions Section
            val managementActions = mutableListOf<Triple<ImageVector, String, Color>>()
            
            if (record.linkedTransactionIds.isNotEmpty()) {
                managementActions.add(Triple(Icons.AutoMirrored.Rounded.Launch, "Jump to Linked", AppColors.secondary))
            }
            managementActions.add(Triple(Icons.AutoMirrored.Rounded.CallSplit, "Split Transaction", AppColors.primary))
            managementActions.add(Triple(Icons.Rounded.ContentCopy, "Clone Transaction", AppColors.tertiary))

            managementActions.forEach { (icon, label, color) ->
                DropdownMenuItem(
                    text = { Text(label, style = AppTextStyles.listItem) },
                    leadingIcon = {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                        )
                    },
                    onClick = {
                        when (label) {
                            "Jump to Linked" -> onEvent(TransactionItemEvent.Jump(record.item.transaction.id))
                            "Split Transaction" -> onEvent(TransactionItemEvent.Split(record.item.transaction.id))
                            "Clone Transaction" -> onEvent(TransactionItemEvent.Clone(record.item.transaction.id))
                        }
                        onDismiss()
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.5f)
            )

            // Danger Zone
            DropdownMenuItem(
                text = {
                    Text(
                        "Delete Transaction",
                        style = AppTextStyles.listItem.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.error
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = AppColors.error,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                    )
                },
                colors = MenuDefaults.itemColors(
                    textColor = AppColors.error,
                    leadingIconColor = AppColors.error
                ),
                onClick = {
                    onEvent(TransactionItemEvent.Delete(record.item.transaction.id))
                    onDismiss()
                }
            )
        } else if (record is PlannedPaymentUiModel) {
            Text(
                text = "PLANNED PAYMENT",
                style = AppTextStyles.sectionOverline,
                color = AppColors.accent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.5f)
            )

            // Confirm Action
            DropdownMenuItem(
                text = { Text("Confirm Payment", style = AppTextStyles.listItem) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = PositiveAmountColor,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                    )
                },
                onClick = {
                    onEvent(TransactionItemEvent.ConfirmPlanned(record.paymentId, record.timestamp))
                    onDismiss()
                }
            )

            // Cancel Action
            DropdownMenuItem(
                text = {
                    Text(
                        "Cancel Payment",
                        style = AppTextStyles.listItem.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.error
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        tint = AppColors.error,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                    )
                },
                colors = MenuDefaults.itemColors(
                    textColor = AppColors.error,
                    leadingIconColor = AppColors.error
                ),
                onClick = {
                    onEvent(TransactionItemEvent.CancelPlanned(record.paymentId, record.timestamp))
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun StatusActionsRow(
    currentStatus: TransactionStatus,
    onStatusChange: (TransactionStatus) -> Unit,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val statuses = TransactionStatus.entries.filter { it != currentStatus }
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
    onJump: () -> Unit,
    hasLinks: Boolean,
    progress: Float,
    onCollapse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val actions = mutableListOf(
            Triple(Icons.AutoMirrored.Rounded.CallSplit, "Split", AppColors.primary),
            Triple(Icons.Rounded.ContentCopy, "Clone", AppColors.tertiary),
            Triple(Icons.Rounded.DeleteForever, "Delete", AppColors.error)
        )

        if (hasLinks) {
            actions.add(0, Triple(Icons.AutoMirrored.Rounded.Launch, "Jump", AppColors.secondary))
        }

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
                        "Jump" -> onJump()
                        else -> onDelete()
                    }
                    onCollapse()
                }
            )
        }
    }
}

@Composable
fun PlannedActionsRow(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
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
            Triple(Icons.Rounded.CheckCircle, "Confirm", PositiveAmountColor),
            Triple(Icons.Rounded.Close, "Cancel", AppColors.error)
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
                        "Confirm" -> onConfirm()
                        "Cancel" -> onCancel()
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.scale(scale)
    ) {
        Surface(
            modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg),
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                )
            }
        }
        Text(
            text = label,
            style = AppTextStyles.transactionSwipeAction.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 8.sp
            ),
            color = contentColor.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = AeonTheme.dimensions.spacingXs)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ActionableTransactionItemPreview() {
    val mockTransaction = Transaction(
        id = 1L,
        name = "Dinner",
        amount = BigDecimal("45.00"),
        timestamp = System.currentTimeMillis(),
        currency = "USD",
        transactionType = TransactionType.EXPENSE,
        accountId = "acc1",
        categoryId = 1L
    )

    val mockRecord = ConfirmedTransactionUiModel(
        item = TransactionWithBalance(mockTransaction, BigDecimal("100"), BigDecimal("55")),
        accountName = "Main Wallet",
        accountCurrency = "USD",
        categoryName = "Food",
        categoryIcon = "Category",
        categoryColor = 0xFF4CAF50.toInt()
    )

    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionableTransactionItem(
                record = mockRecord,
                isRevealed = false,
                onEvent = {}
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = Color.Transparent
                ) {
                    Text("Transaction Content (Swipe me!)")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpressiveActionButtonPreview() {
    OssWalletTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExpressiveActionButton(
                icon = Icons.Rounded.CheckCircle,
                label = "Cleared",
                containerColor = PositiveAmountColor.copy(alpha = 0.12f),
                contentColor = PositiveAmountColor,
                scale = 1f,
                onClick = {}
            )
            ExpressiveActionButton(
                icon = Icons.Rounded.DeleteForever,
                label = "Delete",
                containerColor = AppColors.error.copy(alpha = 0.12f),
                contentColor = AppColors.error,
                scale = 1f,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActionDropdownPreview() {
    val mockTransaction = Transaction(
        id = 1L,
        name = "Starbucks Coffee",
        amount = BigDecimal("5.50"),
        timestamp = System.currentTimeMillis(),
        currency = "USD",
        transactionType = TransactionType.EXPENSE,
        accountId = "acc1",
        categoryId = 1L
    )

    val mockRecord = ConfirmedTransactionUiModel(
        item = TransactionWithBalance(mockTransaction, BigDecimal("100"), BigDecimal("94.50")),
        accountName = "Personal Account",
        accountCurrency = "USD",
        categoryName = "Coffee & Drinks",
        categoryIcon = "LocalCoffee",
        categoryColor = 0xFF795548.toInt()
    )

    OssWalletTheme {
        Box(modifier = Modifier.size(300.dp, 500.dp)) {
            ActionDropdown(
                record = mockRecord,
                onDismiss = {},
                onEvent = {}
            )
        }
    }
}
