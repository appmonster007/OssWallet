package com.osscabinet.ossWallet.ui.components.cards.types

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
import com.osscabinet.ossWallet.domain.model.PlannedPaymentSummary
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
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
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentsCard(
    items: List<PlannedPaymentSummary>,
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
            if (items.isEmpty()) {
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

            items(items, key = { "${if (it.isOverdue) "overdue" else "upcoming"}_${it.id}_${it.scheduledDate}" }) { summary ->
                PlannedPaymentRow(
                    title = summary.name,
                    amount = summary.amount,
                    currency = summary.currency,
                    targetCurrency = targetCurrency,
                    exchangeRates = exchangeRates,
                    entryMode = summary.entryMode,
                    dateLabel = if (summary.isOverdue) "OVERDUE" else formatRelativeDate(summary.scheduledDate, System.currentTimeMillis()),
                    isOverdue = summary.isOverdue,
                    onClick = { callbacks.onItemClick(summary.id) },
                    onConfirm = callbacks.onConfirm?.let { cb -> { cb(summary.id, summary.scheduledDate) } },
                    onSkip = callbacks.onSkip?.let { cb -> { cb(summary.id, summary.scheduledDate) } }
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
                        .padding(vertical = dims.spacingXs),
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
                    .padding(vertical = dims.spacingSm),
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

/**
 * Maps [PlannedPaymentsCardConfig] + [CardRenderData.PlannedPayments] to [CardWithCallbacks.PlannedPayments].
 */
fun mapPlannedPayments(
    config: PlannedPaymentsCardConfig,
    renderData: CardRenderData.PlannedPayments,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: (String) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.PlannedPayments(
    renderData = renderData,
    callbacks = CardCallbacks.PlannedPayments(
        onUpdate = { newConfig -> updateHandler.updateCardPlannedPaymentsConfig(config.cardId, newConfig) },
        onFilterChange = updateHandler::updateCardPlannedPaymentsFilter,
        onItemClick = callbacks.paymentActions.onClick,
        onConfirm = callbacks.paymentActions.onConfirm,
        onSkip = callbacks.paymentActions.onSkip,
        onSeeAll = callbacks.paymentActions.onSeeAll,
        onDelete = { onDelete(config.cardId) },
        onSaveFilterClick = callbacks.filterActions.onSaveClick
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = filterSheetEnabled
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PlannedPaymentsCardPreview() {
    val now = System.currentTimeMillis()
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlannedPaymentsCard(
                items = listOf(
                    PlannedPaymentSummary(
                        id = 1L, name = "Internet Bill",
                        amount = BigDecimal("50.00"), currency = "USD",
                        entryMode = EntryMode.EXPENSE,
                        scheduledDate = now - 86400000L, isOverdue = true
                    ),
                    PlannedPaymentSummary(
                        id = 2L, name = "Netflix",
                        amount = BigDecimal("15.99"), currency = "USD",
                        entryMode = EntryMode.EXPENSE,
                        scheduledDate = now + 86400000L, isOverdue = false
                    )
                ),
                config = PlannedPaymentsCardConfig(cardId = "preview"),
                callbacks = CardCallbacks.PlannedPayments(
                    onUpdate = {},
                    onFilterChange = { _, _ -> },
                    onItemClick = {})
            )
        }
    }
}
