package com.osscabinet.ossWallet.ui.dashboard.components

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsViewMode
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.util.formatAmount
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentsCard(
    upcoming: List<UpcomingPaymentItem>,
    overdue: List<PlannedPayment>,
    config: PlannedPaymentsCardConfig,
    onUpdate: (PlannedPaymentsCardConfig) -> Unit,
    onItemClick: (Long) -> Unit,
    onDelete: () -> Unit,
    onConfirmPayment: ((PlannedPayment) -> Unit)? = null,
    onSkipPayment: ((PlannedPayment) -> Unit)? = null,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    precision: Int = 2
) {
    var showSettings by remember { mutableStateOf(false) }

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.EventRepeat,
        modifier = modifier,
        headerModifier = headerModifier,
        headerTrailing = { _ ->
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = "Card settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            overdue.forEach { payment ->
                PlannedPaymentRow(
                    title = payment.title,
                    amount = payment.amount,
                    currency = payment.currency,
                    dateLabel = "OVERDUE",
                    isOverdue = true,
                    onClick = { onItemClick(payment.id) },
                    onConfirm = onConfirmPayment?.let { { it(payment) } },
                    onSkip = onSkipPayment?.let { { it(payment) } },
                    precision = precision
                )
            }

            upcoming.forEach { item ->
                PlannedPaymentRow(
                    title = item.plannedPayment.title,
                    amount = item.plannedPayment.amount,
                    currency = item.plannedPayment.currency,
                    dateLabel = formatDate(item.scheduledDate),
                    isOverdue = false,
                    onClick = { onItemClick(item.plannedPayment.id) },
                    onConfirm = onConfirmPayment?.let { { it(item.plannedPayment) } },
                    onSkip = onSkipPayment?.let { { it(item.plannedPayment) } },
                    precision = precision
                )
            }
        }
    }

    if (showSettings) {
        PlannedPaymentsSettingsSheet(
            config = config,
            onUpdate = onUpdate,
            onDelete = onDelete,
            onDismiss = { showSettings = false }
        )
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Planned Payments",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(
                    onClick = { onDelete(); onDismiss() },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete card")
                }
            }

            // View mode selector
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "SHOW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
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
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            RadioButton(
                                selected = config.viewMode == mode,
                                onClick = { onUpdate(config.copy(viewMode = mode)) }
                            )
                        }
                    }
                }
            }

            // Days-ahead and max-items controls
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "DISPLAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Look-ahead window", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${config.daysAhead} days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(7, 14, 30, 60).forEach { days ->
                                FilterChip(
                                    selected = config.daysAhead == days,
                                    onClick = { onUpdate(config.copy(daysAhead = days)) },
                                    label = {
                                        Text(
                                            "${days}d",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Max items", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 5, 7, 10).forEach { n ->
                                FilterChip(
                                    selected = config.maxItems == n,
                                    onClick = { onUpdate(config.copy(maxItems = n)) },
                                    label = {
                                        Text(
                                            "$n",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Done") }
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
    onSkip: (() -> Unit)?,
    precision: Int = 2
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
        color = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
        border = BorderStroke(
            0.5.dp,
            if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
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
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = dateLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
            Text(
                text = "${formatAmount(amount, precision)} $currency",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                                        tint = MaterialTheme.colorScheme.primary,
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
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
