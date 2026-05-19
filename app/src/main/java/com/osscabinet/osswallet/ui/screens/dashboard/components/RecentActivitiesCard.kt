package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.data.local.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import java.math.BigDecimal

@Composable
fun RecentActivitiesCard(
    listState: TransactionListState,
    config: RecentActivitiesCardConfig,
    callbacks: CardCallbacks.RecentActivities,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    baseCurrency: String = "USD",
    exchangeRates: Map<String, BigDecimal> = emptyMap(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
) {
    val displayFilter = if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
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
        filterRules = config.filterRules,
        filterContext = filterContext,
        onClick = { callbacks.onRecordsClick(config.filter) },
        onFilterChange = { callbacks.onUpdate(config.copy(filter = it)) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            RecentActivitiesSettingsSheet(config = config, onUpdate = callbacks.onUpdate, onDelete = callbacks.onDelete, onDismiss = onDismiss)
        }
    ) {
        RecentActivitiesScreen(
            listState = listState,
            onRecordsClick = callbacks.onRecordsClick,
            baseCurrency = baseCurrency,
            modifier = Modifier.height(300.dp),
            showHeader = false,
            showCurrencyBreakdown = config.showCurrencyBreakdown,
            revealedCardIds = emptyList(),
            onTransactionEvent = { event ->
                when (event) {
                    is TransactionItemEvent.Click -> callbacks.onTransactionClick(event.id)
                    is TransactionItemEvent.Expand -> callbacks.onItemExpanded(event.id)
                    is TransactionItemEvent.Collapse -> callbacks.onItemCollapsed(event.id)
                    is TransactionItemEvent.StatusChange -> callbacks.onStatusChange(event.id, event.status)
                    is TransactionItemEvent.Clone -> callbacks.onClone(event.id)
                    is TransactionItemEvent.Delete -> callbacks.onDeleteTransaction(event.id)
                    is TransactionItemEvent.Split -> callbacks.onSplit(event.id)
                    is TransactionItemEvent.FullSwipeRight -> callbacks.onFullSwipeRight(event.id)
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
        CardSettingsSection("Group By") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
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
    }
}
