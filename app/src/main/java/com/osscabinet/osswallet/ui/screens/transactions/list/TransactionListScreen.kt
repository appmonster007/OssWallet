package com.osscabinet.ossWallet.ui.screens.transactions.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import kotlinx.serialization.json.Json

/**
 * A Value Object representing a valid configuration for the Transaction List screen.
 */
data class TransactionListConfig(
    val isTopLevel: Boolean = false,
    val filterJson: String? = null
)

/**
 * Grouped actions for the Transaction List screen.
 */
data class TransactionListActions(
    val onMenuClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onEditTransactionClick: (Long) -> Unit,
    val onCloneTransactionClick: (Long) -> Unit = {},
    val onSplitTransactionClick: (Long) -> Unit = {},
    val onSaveFilterClick: (TransactionFilter) -> Unit = {}
)

@Composable
fun TransactionListScreen(
    config: TransactionListConfig,
    actions: TransactionListActions,
    vms: SharedNavViewModels
) {
    val viewModel = vms.transactionViewModel
    val accountViewModel = vms.accountViewModel
    val analyticsViewModel = vms.analyticsViewModel
    
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val analyticsState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.recordsFilter.collectAsStateWithLifecycle()
    val recordsSettings by viewModel.recordsSettings.collectAsStateWithLifecycle()
    val savedFilters by viewModel.savedFilters.collectAsStateWithLifecycle()
    val revealedCardIds by viewModel.revealedCardIdsList.collectAsStateWithLifecycle()

    var navTriggered by remember { mutableStateOf(false) }

    // Reset navTriggered when returning to this screen
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        navTriggered = false
    }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    // Initialize mode and filter from navigation parameters
    LaunchedEffect(config.isTopLevel, config.filterJson) {
        val filter = if (!config.filterJson.isNullOrEmpty()) {
            try {
                Json.decodeFromString<TransactionFilter>(
                    java.net.URLDecoder.decode(config.filterJson, "UTF-8")
                )
            } catch (e: Exception) { null }
        } else null
        
        viewModel.setRecordsMode(topLevel = config.isTopLevel, filter = filter)
    }

    // Only clear filters when explicitly navigating back (not when navigating within records context)
    // Removed automatic filter clearing on screen disposal

    val isTopLevel = config.isTopLevel

    TransactionListContent(
        listState = listState,
        onSearchTransactions = { viewModel.searchTransactions(it) },
        accounts = accounts,
        categories = categories,
        allTags = allTags,
        analyticsState = analyticsState,
        filter = filter,
        recordsSettings = recordsSettings,
        savedFilters = savedFilters,
        onTransactionEvent = { event ->
            when (event) {
                is TransactionItemEvent.Click -> navigate { 
                    if (event.id.startsWith("tx_")) {
                        actions.onEditTransactionClick(event.id.removePrefix("tx_").toLong())
                    } else {
                        // Handle planned payment click if needed, maybe open detail?
                    }
                }
                is TransactionItemEvent.Expand -> viewModel.onItemExpanded(event.id)
                is TransactionItemEvent.Collapse -> viewModel.onItemCollapsed(event.id)
                is TransactionItemEvent.StatusChange -> viewModel.quickUpdateStatus(
                    event.id,
                    event.status
                )

                is TransactionItemEvent.Clone -> actions.onCloneTransactionClick(event.id)
                is TransactionItemEvent.Delete -> viewModel.quickDeleteTransaction(event.id)
                is TransactionItemEvent.Split -> actions.onSplitTransactionClick(event.id)
                is TransactionItemEvent.FullSwipeRight -> viewModel.toggleStatusClearedUncleared(
                    event.id
                )
                is TransactionItemEvent.ConfirmPlanned -> viewModel.confirmPlannedPayment(event.paymentId, event.scheduledDate)
                is TransactionItemEvent.CancelPlanned -> viewModel.cancelPlannedPayment(event.paymentId, event.scheduledDate)
            }
        },
        onBackClick = {
            navigate {
                actions.onBackClick()
            }
        },
        onMenuClick = actions.onMenuClick,
        isTopLevel = isTopLevel,
        onSaveFilterClick = actions.onSaveFilterClick,
        onFilterEvent = { event ->
            when (event) {
                is FilterEvent.Apply -> viewModel.updateRecordsFilter(event.filter)
                is FilterEvent.UpdateTimeline -> viewModel.updateRecordsTimelineRange(event.range)
                is FilterEvent.UpdateSettings -> viewModel.updateRecordsSettings { event.settings }
                is FilterEvent.Reset -> viewModel.resetToInitialFilter()
            }
        },
        onAddTag = viewModel::addTag,
        revealedCardIds = revealedCardIds
    )
}

