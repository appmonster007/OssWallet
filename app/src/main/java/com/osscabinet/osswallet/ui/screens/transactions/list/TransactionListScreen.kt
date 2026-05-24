package com.osscabinet.ossWallet.ui.screens.transactions.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.analytics.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import kotlinx.serialization.json.Json

@Composable
fun TransactionListScreen(
    onMenuClick: () -> Unit,
    isTopLevelParam: Boolean = false,
    viewModel: TransactionViewModel,
    accountViewModel: AccountViewModel,
    analyticsViewModel: AnalyticsViewModel,
    settingsViewModel: SettingsViewModel,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onSplitTransactionClick: (Long) -> Unit = {},
    onAddClick: (com.osscabinet.ossWallet.domain.model.TransactionType?, Long?) -> Unit = { _, _ -> },
    onBackClick: () -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit = {},
    templateViewModel: TemplateViewModel,
    // Context-aware parameters
    filterJson: String? = null
) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val analyticsState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.recordsFilter.collectAsStateWithLifecycle()
    val recordsSettings by viewModel.recordsSettings.collectAsStateWithLifecycle()
    val savedFilters by viewModel.savedFilters.collectAsStateWithLifecycle()
    val revealedCardIds by viewModel.revealedCardIdsList.collectAsStateWithLifecycle()
    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()
    val expandableFabEnabled by settingsViewModel.expandableFabEnabled.collectAsStateWithLifecycle(initialValue = true)

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

    // Initialize filter from navigation parameter when screen opens
    LaunchedEffect(filterJson) {
        if (filterJson != null) {
            try {
                val filter = Json.decodeFromString<TransactionFilter>(
                    java.net.URLDecoder.decode(filterJson, "UTF-8")
                )
                viewModel.initializeRecordsFilter(filter)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    // Only clear filters when explicitly navigating back (not when navigating within records context)
    // Removed automatic filter clearing on screen disposal

    val isTopLevel = isTopLevelParam

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
                is TransactionItemEvent.Click -> navigate { onEditTransactionClick(event.id) }
                is TransactionItemEvent.Expand -> viewModel.onItemExpanded(event.id)
                is TransactionItemEvent.Collapse -> viewModel.onItemCollapsed(event.id)
                is TransactionItemEvent.StatusChange -> viewModel.quickUpdateStatus(
                    event.id,
                    event.status
                )

                is TransactionItemEvent.Clone -> onCloneTransactionClick(event.id)
                is TransactionItemEvent.Delete -> viewModel.quickDeleteTransaction(event.id)
                is TransactionItemEvent.Split -> onSplitTransactionClick(event.id)
                is TransactionItemEvent.FullSwipeRight -> viewModel.toggleStatusClearedUncleared(
                    event.id
                )
            }
        },
        onBackClick = {
            navigate {
                viewModel.clearRecordsFilters()
                onBackClick()
            }
        },
        onMenuClick = onMenuClick,
        isTopLevel = isTopLevel,
        onSaveFilterClick = onSaveFilterClick,
        onFilterEvent = { event ->
            when (event) {
                is FilterEvent.Apply -> viewModel.updateRecordsFilter(event.filter)
                is FilterEvent.UpdateTimeline -> viewModel.updateRecordsTimelineRange(event.range)
                is FilterEvent.UpdateSettings -> viewModel.updateRecordsSettings { event.settings }
                is FilterEvent.Reset -> viewModel.resetToInitialFilter()
            }
        },
        onAddTag = viewModel::addTag,
        revealedCardIds = revealedCardIds,
        allTemplates = allTemplates,
        allCategories = categories,
        onAddClick = onAddClick,
        expandableFabEnabled = expandableFabEnabled
    )
}

