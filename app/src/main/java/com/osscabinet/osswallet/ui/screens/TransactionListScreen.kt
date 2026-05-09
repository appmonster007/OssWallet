package com.osscabinet.ossWallet.ui.screens

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
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsUiState
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onMenuClick: () -> Unit,
    isTopLevelParam: Boolean = false,
    viewModel: TransactionViewModel,
    accountViewModel: com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel,
    analyticsViewModel: com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel,
    settingsViewModel: com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel,
    onAddTransactionClick: (Int?, String?) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onBackClick: () -> Unit,
    onAccountDetailClick: (String) -> Unit,
    templateViewModel: TemplateViewModel,
    // Context-aware parameters
    filterContext: String? = null
) {
    val transactionsWithBalance by viewModel.transactionsWithBalance.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val accountMap = accounts.associate { it.id to it.name }
    val accountCurrencyMap = accounts.associate { it.id to it.currencyCode }
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val decimalPrecision by viewModel.decimalPrecision.collectAsStateWithLifecycle()
    val analyticsState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.recordsFilter.collectAsStateWithLifecycle()
    val recordsSettings by viewModel.recordsSettings.collectAsStateWithLifecycle()
    val savedFilters by viewModel.savedFilters.collectAsStateWithLifecycle()
    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()
    val revealedCardIds by viewModel.revealedCardIdsList.collectAsStateWithLifecycle()
    val use24HourFormat by viewModel.use24HourFormat.collectAsStateWithLifecycle()
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

    // Initialize filter from context when screen opens
    LaunchedEffect(filterContext) {
        if (filterContext != null) {
            try {
                val context = Json.decodeFromString<RecordsFilterContext>(
                    java.net.URLDecoder.decode(filterContext, "UTF-8")
                )
                viewModel.initializeRecordsFilter(context)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    // Only clear filters when explicitly navigating back (not when navigating within records context)
    // Removed automatic filter clearing on screen disposal

    val isTopLevel = isTopLevelParam

    TransactionListContent(
        transactionsWithBalance = transactionsWithBalance,
        accounts = accounts,
        accountMap = accountMap,
        accountCurrencyMap = accountCurrencyMap,
        categories = categories,
        categoryMap = categoryMap,
        decimalPrecision = decimalPrecision,
        analyticsState = analyticsState,
        filter = filter,
        recordsSettings = recordsSettings,
        savedFilters = savedFilters,
        templates = allTemplates,
        onAddTransactionClick = { type, template -> navigate { onAddTransactionClick(type, template) } },
        onEditTransactionClick = { id -> navigate { onEditTransactionClick(id) } },
        onBackClick = {
            navigate {
                viewModel.clearRecordsFilters()
                onBackClick()
            }
        },
        onMenuClick = onMenuClick,
        isTopLevel = isTopLevel,
        onAccountDetailClick = { id -> navigate { onAccountDetailClick(id) } },
        onUpdateFilter = { viewModel.updateRecordsFilter(it) },
        onSetDateRange = { start, end -> viewModel.setRecordsDateRangeFilter(start, end) },
        onUpdateRecordsSettings = { settings -> viewModel.updateRecordsSettings { settings } },
        accountNames = accountMap,
        onResetToInitial = { viewModel.resetToInitialFilter() },
        revealedCardIds = revealedCardIds,
        onItemExpanded = { viewModel.onItemExpanded(it) },
        onItemCollapsed = { viewModel.onItemCollapsed(it) },
        onStatusChange = { transactionId, status -> viewModel.quickUpdateStatus(transactionId, status) },
        onClone = { onCloneTransactionClick(it) },
        onDelete = { viewModel.quickDeleteTransaction(it) },
        onSplit = { viewModel.quickSplitTransaction(it) },
        onFullSwipeRight = { viewModel.toggleStatusClearedUncleared(it) },
        expandableFabEnabled = expandableFabEnabled,
        is24Hour = use24HourFormat
    )
}

