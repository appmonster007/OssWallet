package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import java.math.BigDecimal

@Composable
fun RecentActivitiesCard(
    listState: TransactionListState,
    config: RecentActivitiesCardConfig,
    callbacks: CardCallbacks.RecentActivities,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    baseCurrency: String = "USD",
    exchangeRates: com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot =
        com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(anchor = baseCurrency, rates = emptyMap()),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
) {
    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
            config.filter.copy(timelineRange = inheritedFilter.timelineRange)
        else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.History,
        modifier = modifier,
        headerModifier = headerModifier,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
        filterContext = filterContext,
        onClick = { callbacks.onRecordsClick(config.filter) },
        onFilterChange = { callbacks.onUpdate(config.copy(filter = it)) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            RecentActivitiesSettingsSheet(
                config = config,
                onUpdate = callbacks.onUpdate,
                onDelete = callbacks.onDelete,
                onDismiss = onDismiss
            )
        }
    ) {
        RecentActivitiesScreen(
            listState = listState,
            onRecordsClick = callbacks.onRecordsClick,
            baseCurrency = baseCurrency,
            modifier = Modifier.height(300.dp),
            showHeader = false,
            showCurrencyBreakdown = config.showCurrencyBreakdown,
            allTxnBreakdown = config.allTxnBreakdown,
            useNoteAsHeader = config.useNoteAsHeader,
            showHeaderOpeningBalance = config.showOpeningBalance,
            showHeaderClosingBalance = config.showClosingBalance,
            showItemOpeningBalance = config.showOpeningBalance,
            showItemClosingBalance = config.showClosingBalance,
            showAccount = config.showAccount,
            showCategory = config.showCategory,
            showNote = config.showNote,
            showTags = config.showTags,
            showTimestamp = config.showTimestamp,
            showIcon = config.showIcon,
            revealedCardIds = emptyList<String>(),
            onTransactionEvent = { event ->
                when (event) {
                    is TransactionItemEvent.Click -> {
                        if (event.id.startsWith("tx_")) {
                            callbacks.onTransactionClick(event.id.removePrefix("tx_").toLong())
                        }
                    }
                    is TransactionItemEvent.Expand -> callbacks.onItemExpanded(event.id)
                    is TransactionItemEvent.Collapse -> callbacks.onItemCollapsed(event.id)
                    is TransactionItemEvent.StatusChange -> callbacks.onStatusChange(
                        event.id,
                        event.status
                    )

                    is TransactionItemEvent.Clone -> callbacks.onClone(event.id)
                    is TransactionItemEvent.Delete -> callbacks.onDeleteTransaction(event.id)
                    is TransactionItemEvent.Split -> callbacks.onSplit(event.id)
                    is TransactionItemEvent.FullSwipeRight -> callbacks.onFullSwipeRight(event.id)
                    is TransactionItemEvent.ConfirmPlanned -> callbacks.onConfirmPlanned(event.paymentId, event.scheduledDate)
                    is TransactionItemEvent.CancelPlanned -> callbacks.onCancelPlanned(event.paymentId, event.scheduledDate)
                }
            }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun RecentActivitiesCardPreview() {
    val mockTransaction = Transaction(
        id = 1L,
        name = "Starbucks",
        amount = BigDecimal("5.50"),
        timestamp = System.currentTimeMillis(),
        currency = "USD",
        transactionType = TransactionType.EXPENSE,
        accountId = "acc1",
        categoryId = Category.SYSTEM_UNKNOWN
    )
    val mockUiModel = com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel(
        item = TransactionWithBalance(mockTransaction, BigDecimal("100.00"), BigDecimal("94.50")),
        accountName = "Cash",
        accountCurrency = "USD",
        categoryName = "Food",
        categoryIcon = "Restaurant",
        categoryColor = 0xFF00FF00.toInt()
    )
    val mockGroup = com.osscabinet.ossWallet.domain.model.TransactionGroup(
        key = "Today",
        headerData = com.osscabinet.ossWallet.domain.service.timeline.TimelineService.GroupHeaderData(
            title = "Today",
            subtitle = "July 15",
            incomeByCurrency = emptyMap(),
            expenseByCurrency = mapOf("USD" to BigDecimal("5.50")),
            openingBalancesByCurrency = emptyMap(),
            closingBalancesByCurrency = emptyMap(),
            allCurrencies = listOf("USD"),
            incomeBase = BigDecimal.ZERO,
            expenseBase = BigDecimal("5.50"),
            netBase = BigDecimal("-5.50"),
            totalOpeningBalanceBase = BigDecimal.ZERO,
            totalClosingBalanceBase = BigDecimal.ZERO
        ),
        transactions = listOf(mockUiModel)
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RecentActivitiesCard(
                listState = TransactionListState(
                    groups = listOf(mockGroup),
                    isEmpty = false
                ),
                config = RecentActivitiesCardConfig(cardId = "preview"),
                callbacks = CardCallbacks.RecentActivities(
                    onRecordsClick = {},
                    onTransactionClick = {})
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentActivitiesSettingsSheet(
    config: RecentActivitiesCardConfig,
    onUpdate: (RecentActivitiesCardConfig) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Recent Activities", onDismiss = onDismiss, onDelete = onDelete) {
        CardSettingsSection("Grouping") {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TransactionGroupingType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = config.groupBy == type,
                        onClick = { onUpdate(config.copy(groupBy = type)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TransactionGroupingType.entries.size
                        ),
                        label = {
                            Text(
                                when (type) {
                                    TransactionGroupingType.BY_DAY -> "Day"
                                    TransactionGroupingType.BY_WEEK -> "Week"
                                }
                            )
                        }
                    )
                }
            }
        }

        CardSettingsSection("Components") {
            AeonSettingsToggle(
                label = "Use Note as Header",
                checked = config.useNoteAsHeader,
                onCheckedChange = { onUpdate(config.copy(useNoteAsHeader = it)) }
            )
            AeonSettingsToggle(
                label = "Show Account",
                checked = config.showAccount,
                onCheckedChange = { onUpdate(config.copy(showAccount = it)) }
            )
            AeonSettingsToggle(
                label = "Show Category",
                checked = config.showCategory,
                onCheckedChange = { onUpdate(config.copy(showCategory = it)) }
            )
            AeonSettingsToggle(
                label = "Show Note",
                checked = config.showNote,
                onCheckedChange = { onUpdate(config.copy(showNote = it)) }
            )
            AeonSettingsToggle(
                label = "Show Tags",
                checked = config.showTags,
                onCheckedChange = { onUpdate(config.copy(showTags = it)) }
            )
            AeonSettingsToggle(
                label = "Show Timestamp",
                checked = config.showTimestamp,
                onCheckedChange = { onUpdate(config.copy(showTimestamp = it)) }
            )
            AeonSettingsToggle(
                label = "Show Icon",
                checked = config.showIcon,
                onCheckedChange = { onUpdate(config.copy(showIcon = it)) }
            )
        }

        CardSettingsSection("Balances") {
            AeonSettingsToggle(
                label = "Show Opening Balance",
                enabled = config.allTxnBreakdown,
                checked = config.allTxnBreakdown && config.showOpeningBalance,
                onCheckedChange = { onUpdate(config.copy(showOpeningBalance = it)) }
            )
            AeonSettingsToggle(
                label = "Show Closing Balance",
                enabled = config.allTxnBreakdown,
                checked = config.allTxnBreakdown && config.showClosingBalance,
                onCheckedChange = { onUpdate(config.copy(showClosingBalance = it)) }
            )
            AeonSettingsToggle(
                label = "Show Currency Breakdown",
                checked = config.showCurrencyBreakdown,
                onCheckedChange = { onUpdate(config.copy(showCurrencyBreakdown = it)) }
            )
            AeonSettingsToggle(
                label = "Period Overview",
                description = "Show full period totals regardless of active filter",
                checked = config.allTxnBreakdown,
                enabled = config.showCurrencyBreakdown,
                onCheckedChange = { onUpdate(config.copy(allTxnBreakdown = it)) }
            )
        }
    }
}

