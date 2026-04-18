package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterSheet
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsUiState
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    accountViewModel: com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel,
    analyticsViewModel: com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel,
    onAddTransactionClick: (Int?, String?) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onAccountDetailClick: (String) -> Unit,
    templateViewModel: TemplateViewModel
) {
    val transactionsWithBalance by viewModel.transactionsWithBalance.collectAsState()
    val accounts by accountViewModel.accounts.collectAsState()
    val accountMap = accounts.associate { it.id to it.name }
    val accountCurrencyMap = accounts.associate { it.id to it.currencyCode }
    val categories by viewModel.categories.collectAsState()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val decimalPrecision by viewModel.decimalPrecision.collectAsState()
    val analyticsState by analyticsViewModel.uiState.collectAsState()
    val filter by viewModel.recordsFilter.collectAsState()
    val recordsSettings by viewModel.recordsSettings.collectAsState()
    val savedFilters by viewModel.savedFilters.collectAsState()
    val allTemplates by templateViewModel.templates.collectAsState()

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

    // Reset filters when entering or leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearRecordsFilters()
        }
    }

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
        onBackClick = { navigate { onBackClick() } },
        onAccountDetailClick = { id -> navigate { onAccountDetailClick(id) } },
        onUpdateFilter = { viewModel.updateRecordsFilter(it) },
        onSetDateRange = { start, end -> viewModel.setRecordsDateRangeFilter(start, end) },
        onUpdateRecordsSettings = { settings -> viewModel.updateRecordsSettings { settings } }
    )
}

