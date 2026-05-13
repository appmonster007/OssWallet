package com.osscabinet.ossWallet.ui.screens.transactions.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterSheet
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.screens.accounts.*
import com.osscabinet.ossWallet.ui.screens.transactions.*
import com.osscabinet.ossWallet.ui.screens.templates.*
import com.osscabinet.ossWallet.ui.screens.settings.*
import com.osscabinet.ossWallet.ui.screens.analytics.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
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
    onBackClick: () -> Unit,
    templateViewModel: TemplateViewModel,
    // Context-aware parameters
    filterJson: String? = null
) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val decimalPrecision by viewModel.decimalPrecision.collectAsStateWithLifecycle()
    val analyticsState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.recordsFilter.collectAsStateWithLifecycle()
    val recordsSettings by viewModel.recordsSettings.collectAsStateWithLifecycle()
    val savedFilters by viewModel.savedFilters.collectAsStateWithLifecycle()
    val revealedCardIds by viewModel.revealedCardIdsList.collectAsStateWithLifecycle()
    val use24HourFormat by viewModel.use24HourFormat.collectAsStateWithLifecycle()

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
        accounts = accounts,
        categories = categories,
        decimalPrecision = decimalPrecision,
        analyticsState = analyticsState,
        filter = filter,
        recordsSettings = recordsSettings,
        savedFilters = savedFilters,
        onEditTransactionClick = { id -> navigate { onEditTransactionClick(id) } },
        onBackClick = {
            navigate {
                viewModel.clearRecordsFilters()
                onBackClick()
            }
        },
        onMenuClick = onMenuClick,
        isTopLevel = isTopLevel,
        onUpdateFilter = { viewModel.updateRecordsFilter(it) },
        onUpdateTimelineRange = { viewModel.updateRecordsTimelineRange(it) },
        onUpdateRecordsSettings = { settings -> viewModel.updateRecordsSettings { settings } },
        onResetToInitial = { viewModel.resetToInitialFilter() },
        revealedCardIds = revealedCardIds,
        onItemExpanded = { viewModel.onItemExpanded(it) },
        onItemCollapsed = { viewModel.onItemCollapsed(it) },
        onStatusChange = { transactionId, status -> viewModel.quickUpdateStatus(transactionId, status) },
        onClone = { onCloneTransactionClick(it) },
        onDelete = { viewModel.quickDeleteTransaction(it) },
        onSplit = { onSplitTransactionClick(it) },
        onFullSwipeRight = { viewModel.toggleStatusClearedUncleared(it) },
        is24Hour = use24HourFormat
    )
}

