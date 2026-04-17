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
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListTopAppBar
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.TransactionSectionSettings
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListContent(
    transactionsWithBalance: List<TransactionWithBalance>,
    accounts: List<Account>,
    accountMap: Map<String, String>,
    accountCurrencyMap: Map<String, String>,
    categories: List<CategoryEntity>,
    categoryMap: Map<Long, CategoryEntity>,
    decimalPrecision: Int,
    analyticsState: AnalyticsUiState,
    filter: TransactionFilter,
    recordsSettings: TransactionSectionSettings,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onAccountDetailClick: (String) -> Unit,
    onUpdateFilter: (TransactionFilter) -> Unit,
    onSetDateRange: (Long?, Long?) -> Unit,
    onUpdateRecordsSettings: (TransactionSectionSettings) -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 90.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetDragHandle = null,
        topBar = {
            TransactionListTopAppBar(
                filter = filter,
                recordsSettings = recordsSettings,
                onBackClick = onBackClick,
                onUpdateRecordsSettings = onUpdateRecordsSettings
            )
        },
        sheetContent = {
            TransactionFilterSheet(
                filter = filter,
                accounts = accounts,
                categories = categories,
                onSetDateRange = onSetDateRange,
                onUpdateFilter = onUpdateFilter,
                onToggleExpand = {
                    scope.launch {
                        if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                            scaffoldState.bottomSheetState.expand()
                        } else {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    }
                },
                onClose = {
                    scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                },
                isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (transactionsWithBalance.isEmpty()) {
                TransactionListEmptyState()
            } else {
                AeonTransactionList(
                    transactions = transactionsWithBalance,
                    accountMap = accountMap,
                    categoryMap = categoryMap,
                    onTransactionClick = { onEditTransactionClick(it.id) },
                    contentPadding = PaddingValues(bottom = 16.dp),
                    precision = decimalPrecision,
                    useNoteAsHeader = recordsSettings.useNoteAsHeader,
                    showOpeningBalance = recordsSettings.showOpeningBalance,
                    showClosingBalance = recordsSettings.showClosingBalance,
                    baseCurrency = analyticsState.selectedCurrency,
                    exchangeRates = analyticsState.exchangeRates,
                    accountCurrencyMap = accountCurrencyMap,
                    filterAccountId = if (filter.accountIds.size == 1) filter.accountIds.first() else null
                )
            }
        }
    }
}
