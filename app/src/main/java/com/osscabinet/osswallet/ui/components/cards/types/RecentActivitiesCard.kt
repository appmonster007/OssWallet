package com.osscabinet.ossWallet.ui.components.cards.types

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.cards.resolveDrillDown
import com.osscabinet.ossWallet.ui.components.cards.resolveSeeAll
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState

@Composable
fun RecentActivitiesCard(
    listState: TransactionListUiState,
    config: RecentActivitiesCardConfig,
    callbacks: CardCallbacks.RecentActivities,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    baseCurrency: String = "USD",
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    scrollToId: String? = null,
    highlightedId: String? = null,
    onScrollComplete: () -> Unit = {}
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
            scrollToId = scrollToId,
            highlightedId = highlightedId,
            onScrollComplete = onScrollComplete,
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
                    is TransactionItemEvent.Jump -> callbacks.onJump(
                        event.id,
                        listState.getAllTransactionIds()
                    )

                    is TransactionItemEvent.FullSwipeRight -> callbacks.onFullSwipeRight(event.id)
                    is TransactionItemEvent.ConfirmPlanned -> callbacks.onConfirmPlanned(
                        event.paymentId,
                        event.scheduledDate
                    )

                    is TransactionItemEvent.CancelPlanned -> callbacks.onCancelPlanned(
                        event.paymentId,
                        event.scheduledDate
                    )
                }
            }
        )
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
                    .padding(vertical = 8.dp)
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
                onCheckedChange = { onUpdate(config.copy(useNoteAsHeader = it)) },
                showBackground = false
            )
            AeonSettingsToggle(
                label = "Show Account",
                checked = config.showAccount,
                onCheckedChange = { onUpdate(config.copy(showAccount = it)) },
                showBackground = false
            )
            AeonSettingsToggle(
                label = "Show Category",
                checked = config.showCategory,
                onCheckedChange = { onUpdate(config.copy(showCategory = it)) },
                showBackground = false
            )
            AeonSettingsToggle(
                label = "Show Note",
                checked = config.showNote,
                onCheckedChange = { onUpdate(config.copy(showNote = it)) },
                showBackground = false
            )
            AeonSettingsToggle(
                label = "Show Tags",
                checked = config.showTags,
                onCheckedChange = { onUpdate(config.copy(showTags = it)) },
                showBackground = false
            )
            AeonSettingsToggle(
                label = "Show Timestamp",
                checked = config.showTimestamp,
                onCheckedChange = { onUpdate(config.copy(showTimestamp = it)) },
                showBackground = false
            )
            AeonSettingsToggle(
                label = "Show Icon",
                checked = config.showIcon,
                onCheckedChange = { onUpdate(config.copy(showIcon = it)) },
                showBackground = false
            )
        }

        CardSettingsSection("Balances") {
            AeonSettingsToggle(
                label = "Show Opening Balance",
                enabled = config.allTxnBreakdown,
                checked = config.allTxnBreakdown && config.showOpeningBalance,
                onCheckedChange = { onUpdate(config.copy(showOpeningBalance = it)) },
                showBackground = false
            )
            AeonSettingsToggle(
                label = "Show Closing Balance",
                enabled = config.allTxnBreakdown,
                checked = config.allTxnBreakdown && config.showClosingBalance,
                onCheckedChange = { onUpdate(config.copy(showClosingBalance = it)) },
                showBackground = false
            )
            AeonSettingsToggle(
                label = "Show Currency Breakdown",
                checked = config.showCurrencyBreakdown,
                onCheckedChange = { onUpdate(config.copy(showCurrencyBreakdown = it)) },
                showBackground = false
            )
            AeonSettingsToggle(
                label = "Period Overview",
                description = "Show full period totals regardless of active filter",
                checked = config.allTxnBreakdown,
                enabled = config.showCurrencyBreakdown,
                onCheckedChange = { onUpdate(config.copy(allTxnBreakdown = it)) },
                showBackground = false
            )
        }
    }
}

/**
 * Maps [RecentActivitiesCardConfig] + [CardRenderData.RecentActivities] to [CardWithCallbacks.RecentActivities].
 */
fun mapRecentActivities(
    config: RecentActivitiesCardConfig,
    renderData: CardRenderData.RecentActivities,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: (String) -> Unit,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.RecentActivities(
    renderData = renderData,
    callbacks = CardCallbacks.RecentActivities(
        onUpdate = { newConfig ->
            updateHandler.updateCardRecentActivitiesConfig(
                config.cardId,
                newConfig
            )
        },
        onTransactionClick = callbacks.transactionActions.onClick,
        onRecordsClick = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onSeeAll = { onRecordsClick(resolveSeeAll(input, config)) },
        onDelete = { onDelete(config.cardId) },
        onItemExpanded = callbacks.transactionActions.onItemExpanded,
        onItemCollapsed = callbacks.transactionActions.onItemCollapsed,
        onStatusChange = callbacks.transactionActions.onStatusChange,
        onClone = callbacks.transactionActions.onClone,
        onDeleteTransaction = callbacks.transactionActions.onDelete,
        onSplit = callbacks.transactionActions.onSplit,
        onJump = callbacks.transactionActions.onJump,
        onFullSwipeRight = callbacks.transactionActions.onFullSwipeRight,
        onSaveFilterClick = callbacks.filterActions.onSaveClick,
        onConfirmPlanned = callbacks.paymentActions.onConfirm,
        onCancelPlanned = callbacks.paymentActions.onSkip
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = filterSheetEnabled
)
