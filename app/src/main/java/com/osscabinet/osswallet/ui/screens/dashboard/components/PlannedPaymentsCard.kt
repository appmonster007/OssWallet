package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsViewMode
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentsCard(
    upcoming: List<UpcomingPaymentItem>,
    overdue: List<PlannedPayment>,
    config: PlannedPaymentsCardConfig,
    callbacks: CardCallbacks.PlannedPayments,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier
) {
    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.EventRepeat,
        modifier = modifier,
        headerModifier = headerModifier,
        filterEnabled = false,
        displaySettings = { onDismiss ->
            PlannedPaymentsSettingsSheet(config = config, onUpdate = callbacks.onUpdate, onDelete = callbacks.onDelete, onDismiss = onDismiss)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (overdue.isEmpty() && upcoming.isEmpty()) {
                Text(
                    text = when (config.viewMode) {
                        PlannedPaymentsViewMode.OVERDUE_ONLY -> "No overdue payments"
                        PlannedPaymentsViewMode.FUTURE_ONLY -> "No upcoming payments"
                        PlannedPaymentsViewMode.BOTH -> "No upcoming payments"
                    },
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            overdue.forEach { payment ->
                PlannedPaymentRow(
                    title = payment.name,
                    amount = payment.amount,
                    currency = payment.currency,
                    dateLabel = "OVERDUE",
                    isOverdue = true,
                    onClick = { callbacks.onItemClick(payment.id) },
                    onConfirm = callbacks.onConfirm?.let { { it(payment.id) } },
                    onSkip = callbacks.onSkip?.let { { it(payment.id) } }
                )
            }

            upcoming.forEach { item ->
                PlannedPaymentRow(
                    title = item.plannedPayment.name,
                    amount = item.plannedPayment.amount,
                    currency = item.plannedPayment.currency,
                    dateLabel = formatDate(item.scheduledDate),
                    isOverdue = false,
                    onClick = { callbacks.onItemClick(item.plannedPayment.id) },
                    onConfirm = callbacks.onConfirm?.let { { it(item.plannedPayment.id) } },
                    onSkip = callbacks.onSkip?.let { { it(item.plannedPayment.id) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannedPaymentsSettingsSheet(
    config: PlannedPaymentsCardConfig,
    onUpdate: (PlannedPaymentsCardConfig) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Planned Payments", onDismiss = onDismiss, onDelete = onDelete) {
        CardSettingsSection("Show") {
            val viewModes = listOf(
                PlannedPaymentsViewMode.OVERDUE_ONLY to "Overdue only",
                PlannedPaymentsViewMode.BOTH to "Overdue + Upcoming",
                PlannedPaymentsViewMode.FUTURE_ONLY to "Future only (1 per schedule)"
            )
            viewModes.forEach { (mode, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = AppTextStyles.transactionTitle)
                    RadioButton(
                        selected = config.viewMode == mode,
                        onClick = { onUpdate(config.copy(viewMode = mode)) }
                    )
                }
            }
        }
        CardSettingsSection("Display") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Look-ahead window", style = AppTextStyles.transactionTitle)
                    Text(
                        "${config.daysAhead} days",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 14, 30, 60).forEach { days ->
                        FilterChip(
                            selected = config.daysAhead == days,
                            onClick = { onUpdate(config.copy(daysAhead = days)) },
                            label = { Text("${days}d", style = AppTextStyles.transactionSwipeAction) }
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.3f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Max items", style = AppTextStyles.transactionTitle)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 5, 7, 10).forEach { n ->
                        FilterChip(
                            selected = config.maxItems == n,
                            onClick = { onUpdate(config.copy(maxItems = n)) },
                            label = { Text("$n", style = AppTextStyles.transactionSwipeAction) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannedPaymentRow(
    title: String,
    amount: BigDecimal,
    currency: String,
    dateLabel: String,
    isOverdue: Boolean,
    onClick: () -> Unit,
    onConfirm: (() -> Unit)?,
    onSkip: (() -> Unit)?
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "rowScale"
    )
    var showActions by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        color = if (isOverdue) AppColors.errorContainer.copy(alpha = 0.2f)
        else AppColors.cardSurfaceProminent.copy(alpha = 0.3f),
        border = BorderStroke(
            0.5.dp,
            if (isOverdue) AppColors.error.copy(alpha = 0.2f)
            else AppColors.divider.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextStyles.managementItemTitle.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = dateLabel.uppercase(),
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isOverdue) AppColors.error
                        else AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                )
            }
            Text(
                text = "${formatAmount(amount)} $currency",
                style = AppTextStyles.screenSubTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isOverdue) AppColors.error
                    else AppColors.contentPrimary
                )
            )
            if (onConfirm != null || onSkip != null) {
                Box {
                    IconButton(
                        onClick = { showActions = true },
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Actions",
                            modifier = Modifier.size(16.dp),
                            tint = AppColors.contentSecondary.copy(alpha = 0.5f)
                        )
                    }
                    DropdownMenu(
                        expanded = showActions,
                        onDismissRequest = { showActions = false }
                    ) {
                        if (onConfirm != null) {
                            DropdownMenuItem(
                                text = { Text("Confirm") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = AppColors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showActions = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onConfirm()
                                }
                            )
                        }
                        if (onSkip != null) {
                            DropdownMenuItem(
                                text = { Text("Skip") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.SkipNext,
                                        contentDescription = null,
                                        tint = AppColors.contentSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showActions = false
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSkip()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PlannedPaymentsCardPreview() {
    val mockSchedule = com.osscabinet.ossWallet.domain.model.PaymentSchedule(
        recurrenceRule = com.osscabinet.ossWallet.domain.model.RecurrenceRule(
            type = com.osscabinet.ossWallet.domain.model.RecurrenceType.MONTHLY,
            interval = 1
        ),
        startDate = System.currentTimeMillis(),
        nextDueDate = System.currentTimeMillis() + 86400000L
    )

    val overduePayment = com.osscabinet.ossWallet.domain.model.PlannedPayment(
        id = 1L,
        name = "Internet Bill",
        categoryId = 1,
        amount = BigDecimal("50.00"),
        transactionType = com.osscabinet.ossWallet.domain.model.TransactionType.EXPENSE,
        currency = "USD",
        schedule = mockSchedule
    )

    val upcomingPayment = com.osscabinet.ossWallet.domain.model.PlannedPayment(
        id = 2L,
        name = "Netflix",
        categoryId = 2,
        amount = BigDecimal("15.99"),
        transactionType = com.osscabinet.ossWallet.domain.model.TransactionType.EXPENSE,
        currency = "USD",
        schedule = mockSchedule
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlannedPaymentsCard(
                upcoming = listOf(
                    com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem(
                        plannedPayment = upcomingPayment,
                        scheduledDate = System.currentTimeMillis() + 86400000L,
                        isOverdue = false
                    )
                ),
                overdue = listOf(overduePayment),
                config = PlannedPaymentsCardConfig(cardId = "preview"),
                callbacks = CardCallbacks.PlannedPayments(onUpdate = {}, onDelete = {}, onItemClick = {})
            )
        }
    }
}

private fun formatDate(epochMilli: Long): String {
    val date = Instant.ofEpochMilli(epochMilli).atOffset(ZoneOffset.UTC).toLocalDate()
    val today = LocalDate.now(ZoneOffset.UTC)
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}
//
///**
// * Confirms a planned payment by ID.
// * This is a standalone helper that can be used from any ViewModel.
// *
// * @param paymentId The ID of the planned payment to confirm
// * @param plannedPayments List of all planned payments to find the target payment
// * @param plannedPaymentRepository Repository for planned payment operations
// * @param transactionManager Manager for creating transactions
// * @param profileId The current profile ID
// */
//suspend fun quickConfirmPlannedPayment(
//    paymentId: Long,
//    plannedPayments: List<PlannedPayment>,
//    plannedPaymentRepository: PlannedPaymentRepository,
//    transactionManager: TransactionManager,
//    profileId: Int
//) {
//    val payment = plannedPayments.find { it.id == paymentId } ?: return
//    val occurrence = PlannedPaymentOccurrence(
//        id = 0,
//        plannedPaymentId = payment.id,
//        scheduledDate = payment.nextDueDate
//    )
//    val request = TransactionRequest(
//        title = payment.title,
//        amount = payment.amount,
//        category = "",
//        transactionType = payment.transactionType,
//        accountId = payment.accountId,
//        currency = payment.currency,
//        categoryId = payment.categoryId,
//        tags = payment.tags,
//        paymentMethod = payment.paymentMethod,
//        timestamp = occurrence.scheduledDate,
//        payee = payment.payee,
//        note = payment.note
//    )
//    val transactionId = transactionManager.handleSaveTransaction(null, request, profileId)
//    plannedPaymentRepository.upsertOccurrence(
//        occurrence.copy(status = OccurrenceStatus.CONFIRMED, generatedTransactionId = transactionId)
//    )
//    val nextDue = PlannedPaymentService.calculateNextDueDate(payment, occurrence.scheduledDate)
//    if (nextDue == null) {
//        plannedPaymentRepository.update(payment.copy(status = PlannedPaymentStatus.COMPLETED))
//    } else {
//        plannedPaymentRepository.update(
//            payment.copy(nextDueDate = nextDue, lastTriggeredDate = occurrence.scheduledDate)
//        )
//    }
//}
//
///**
// * Skips a planned payment by ID.
// * This is a standalone helper that can be used from any ViewModel.
// *
// * @param paymentId The ID of the planned payment to skip
// * @param plannedPayments List of all planned payments to find the target payment
// * @param plannedPaymentRepository Repository for planned payment operations
// */
//suspend fun quickSkipPlannedPayment(
//    paymentId: Long,
//    plannedPayments: List<PlannedPayment>,
//    plannedPaymentRepository: PlannedPaymentRepository
//) {
//    val payment = plannedPayments.find { it.id == paymentId } ?: return
//    val occurrence = PlannedPaymentOccurrence(
//        id = 0,
//        plannedPaymentId = payment.id,
//        scheduledDate = payment.nextDueDate
//    )
//    plannedPaymentRepository.upsertOccurrence(occurrence.copy(status = OccurrenceStatus.SKIPPED))
//    val nextDue = PlannedPaymentService.calculateNextDueDate(payment, occurrence.scheduledDate)
//    if (nextDue == null) {
//        plannedPaymentRepository.update(payment.copy(status = PlannedPaymentStatus.COMPLETED))
//    } else {
//        plannedPaymentRepository.update(
//            payment.copy(nextDueDate = nextDue, lastTriggeredDate = occurrence.scheduledDate)
//        )
//    }
//}
