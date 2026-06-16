package com.osscabinet.ossWallet.ui.screens.dashboard.components

import com.osscabinet.ossWallet.domain.model.EntryMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsViewMode
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.ui.components.AeonStepperField
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.aeonDottedUnderline
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.util.TimeUtils.formatRelativeDate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentsCard(
    upcoming: List<UpcomingPaymentItem>,
    overdue: List<PlannedPayment>,
    targetCurrency: String = "USD",
    exchangeRates: com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot =
        com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(anchor = targetCurrency, rates = emptyMap()),
    config: PlannedPaymentsCardConfig,
    callbacks: CardCallbacks.PlannedPayments,
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    filterRules: CardFilterRules? = null,
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier
) {
    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is TimelineRange.AllTime)
            config.filter.copy(timelineRange = inheritedFilter.timelineRange)
        else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.EventRepeat,
        modifier = modifier,
        headerModifier = headerModifier,
        onClick = callbacks.onSeeAll,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            PlannedPaymentsSettingsSheet(
                config = config,
                onUpdate = callbacks.onUpdate,
                onDelete = callbacks.onDelete,
                onDismiss = onDismiss
            )
        }
    ) {
        val dims = AeonTheme.dimensions
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentPadding = PaddingValues(bottom = dims.cardPadding)
        ) {
            if (overdue.isEmpty() && upcoming.isEmpty()) {
                item(key = "empty_state") {
                    Text(
                        text = when (config.viewMode) {
                            PlannedPaymentsViewMode.OVERDUE_ONLY -> "No overdue payments"
                            PlannedPaymentsViewMode.FUTURE_ONLY -> "No upcoming payments"
                            PlannedPaymentsViewMode.BOTH -> "No upcoming payments"
                        },
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary,
                        modifier = Modifier.padding(vertical = dims.spacingSm)
                    )
                }
            }

            items(overdue, key = { "overdue_${it.id}" }) { payment ->
                PlannedPaymentRow(
                    title = payment.name,
                    amount = payment.amount,
                    currency = payment.currency,
                    targetCurrency = targetCurrency,
                    exchangeRates = exchangeRates,
                    entryMode = payment.entryMode,
                    dateLabel = "OVERDUE",
                    isOverdue = true,
                    onClick = { callbacks.onItemClick(payment.id) },
                    onConfirm = callbacks.onConfirm?.let { cb -> { cb(payment.id, payment.schedule.nextDueDate) } },
                    onSkip = callbacks.onSkip?.let { cb -> { cb(payment.id, payment.schedule.nextDueDate) } }
                )
            }

            items(upcoming, key = { "upcoming_${it.plannedPayment.id}_${it.scheduledDate}" }) { item ->
                PlannedPaymentRow(
                    title = item.plannedPayment.name,
                    amount = item.plannedPayment.amount,
                    currency = item.plannedPayment.currency,
                    targetCurrency = targetCurrency,
                    exchangeRates = exchangeRates,
                    entryMode = item.plannedPayment.entryMode,
                    dateLabel = formatRelativeDate(item.scheduledDate, System.currentTimeMillis()),
                    isOverdue = false,
                    onClick = { callbacks.onItemClick(item.plannedPayment.id) },
                    onConfirm = callbacks.onConfirm?.let { cb -> { cb(item.plannedPayment.id, item.scheduledDate) } },
                    onSkip = callbacks.onSkip?.let { cb -> { cb(item.plannedPayment.id, item.scheduledDate) } }
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
        val dims = AeonTheme.dimensions
        CardSettingsSection("Show") {
            val viewModes = listOf(
                PlannedPaymentsViewMode.OVERDUE_ONLY to "Overdue only",
                PlannedPaymentsViewMode.BOTH to "Overdue + Upcoming",
                PlannedPaymentsViewMode.FUTURE_ONLY to "Future only (1 per schedule)"
            )
            viewModes.forEach { (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dims.cardPadding, vertical = dims.spacingXs),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.cardPadding, vertical = dims.spacingSm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Max items", style = AppTextStyles.transactionTitle)
                    Text(
                        "Showing up to ${config.maxItems} payments",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
                AeonStepperField(
                    value = config.maxItems,
                    onDecrement = { onUpdate(config.copy(maxItems = (config.maxItems - 1).coerceAtLeast(1))) },
                    onIncrement = { onUpdate(config.copy(maxItems = config.maxItems + 1)) },
                    min = 1
                )
            }
        }
    }
}

@Composable
private fun PlannedPaymentRow(
    title: String,
    amount: BigDecimal,
    currency: String,
    targetCurrency: String,
    exchangeRates: com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot,
    entryMode: EntryMode,
    dateLabel: String,
    isOverdue: Boolean,
    onClick: () -> Unit,
    onConfirm: (() -> Unit)?,
    onSkip: (() -> Unit)?
) {
    val haptic = LocalHapticFeedback.current
    val dims = AeonTheme.dimensions
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "rowScale"
    )
    var showActions by remember { mutableStateOf(false) }

    val typeColor = when {
        isOverdue -> AppColors.negative
        entryMode == EntryMode.INCOME -> AppColors.positive
        entryMode == EntryMode.TRANSFER -> AccentBlue
        else -> AppColors.negative
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .aeonDottedUnderline(AppColors.outlineVariant)
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
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Medium),
                    color = AppColors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateLabel.uppercase(),
                    style = AppTextStyles.metaLabel,
                    color = if (isOverdue) AppColors.negative
                    else AppColors.contentSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            com.osscabinet.ossWallet.ui.components.ConvertedWithNativeSubtext(
                amount = amount,
                nativeCurrency = currency,
                targetCurrency = targetCurrency,
                rates = exchangeRates,
                primaryStyle = AppTextStyles.transactionAmount.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = typeColor
                ),
                primaryColor = typeColor,
                subtextStyle = AppTextStyles.metaLabel,
                subtextColor = AppColors.contentSecondary.copy(alpha = 0.7f)
            )
            if (onConfirm != null || onSkip != null) {
                Box {
                    IconButton(
                        onClick = { showActions = true },
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(dims.iconSizeMd)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Actions",
                            modifier = Modifier.size(14.dp),
                            tint = AppColors.contentSecondary.copy(alpha = 0.4f)
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
                                        modifier = Modifier.size(dims.iconSizeSm)
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
                                        modifier = Modifier.size(dims.iconSizeSm)
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

    val overduePayment = PlannedPayment(
        id = 1L,
        name = "Internet Bill",
        categoryId = 1,
        amount = BigDecimal("50.00"),
        entryMode = EntryMode.EXPENSE,
        currency = "USD",
        schedule = mockSchedule
    )

    val upcomingPayment = PlannedPayment(
        id = 2L,
        name = "Netflix",
        categoryId = 2,
        amount = BigDecimal("15.99"),
        entryMode = EntryMode.EXPENSE,
        currency = "USD",
        schedule = mockSchedule
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlannedPaymentsCard(
                upcoming = listOf(
                    UpcomingPaymentItem(
                        plannedPayment = upcomingPayment,
                        scheduledDate = System.currentTimeMillis() + 86400000L,
                        isOverdue = false
                    )
                ),
                overdue = listOf(overduePayment),
                config = PlannedPaymentsCardConfig(cardId = "preview"),
                callbacks = CardCallbacks.PlannedPayments(
                    onUpdate = {},
                    onDelete = {},
                    onItemClick = {})
            )
        }
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
//        entryMode = payment.entryMode,
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
