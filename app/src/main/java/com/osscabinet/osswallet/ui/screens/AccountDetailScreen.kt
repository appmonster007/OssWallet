package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.dashboard.*
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterSheet
import com.osscabinet.ossWallet.ui.components.AeonExtendedFloatingActionButton
import com.osscabinet.ossWallet.ui.viewmodel.AccountDetailViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountDetailUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel,
    analyticsViewModel: com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel,
    accountDetailViewModel: AccountDetailViewModel,
    onBackClick: () -> Unit,
    onEditAccountClick: (String) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onAddTransactionClick: (String) -> Unit
) {
    val currentTheme by settingsViewModel.theme.collectAsState(initial = "System")
    
    val uiState by accountDetailViewModel.uiState.collectAsState()
    
    val categories by transactionViewModel.categories.collectAsState()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Transactions")

    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog && uiState is AccountDetailUiState.Success) {
        val state = uiState as AccountDetailUiState.Success
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Transaction Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Planned Payments", style = MaterialTheme.typography.bodyLarge)
                            Text("Include future transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = state.showPlannedPayments,
                            onCheckedChange = { accountDetailViewModel.updateShowPlannedPayments(it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Note as Header", style = MaterialTheme.typography.bodyLarge)
                            Text("Primary header will show transaction note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = state.useNoteAsHeader,
                            onCheckedChange = { accountDetailViewModel.updateUseNoteAsHeader(it) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        val title = when (val state = uiState) {
                            is AccountDetailUiState.Success -> state.account.name
                            else -> "Account"
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEditAccountClick(accountId) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AeonExtendedFloatingActionButton(
                onClick = { onAddTransactionClick(accountId) },
                icon = Icons.Rounded.Add,
                text = "Transaction"
            )
        }
    ) { screenPadding ->
        when (val state = uiState) {
            is AccountDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AccountDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message)
                }
            }
            is AccountDetailUiState.Success -> {
                val account = state.account
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (selectedTabIndex == 0) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            contentPadding = PaddingValues(
                                top = screenPadding.calculateTopPadding() + 24.dp,
                                bottom = screenPadding.calculateBottomPadding() + 96.dp
                            )
                        ) {
                            item {
                                AeonSectionHeader(title = "Account Summary")
                                Spacer(modifier = Modifier.height(4.dp))
                                AeonAccountHeroCard(
                                    account = account,
                                    balance = account.balance,
                                    netBalance = account.balance.subtract(account.initialBalance), // Correct net in account currency
                                    baseCurrency = state.baseCurrency,
                                    exchangeRates = state.exchangeRates,
                                    precision = state.decimalPrecision
                                )
                            }

                            item {
                                AeonCashFlowSummary(
                                    transactions = state.cashFlowTransactions,
                                    baseCurrency = account.currencyCode,
                                    exchangeRates = state.exchangeRates,
                                    currentTheme = currentTheme,
                                    modifier = Modifier.fillMaxWidth(),
                                    showTitle = true,
                                    precision = state.decimalPrecision,
                                    showBaseConversion = state.cashFlowShowBaseConversion,
                                    timelineRange = state.cashFlowTimelineRange,
                                    onShowBaseConversionChange = { accountDetailViewModel.updateCashFlowShowBaseConversion(it) },
                                    onTimelineRangeChange = { accountDetailViewModel.updateCashFlowTimelineRange(it) }
                                )
                            }

                            item {
                                CurrencyAggregationSection(
                                    currencyTotals = state.currencyTotals,
                                    baseCurrency = state.baseCurrency,
                                    exchangeRates = state.exchangeRates,
                                    modifier = Modifier.fillMaxWidth(),
                                    precision = state.decimalPrecision,
                                    layout = state.currencyBalancesLayout,
                                    showBaseConversion = state.showBaseConversion,
                                    timelineRange = state.currencyBalancesTimelineRange,
                                    onLayoutChange = { accountDetailViewModel.updateCurrencyBalancesLayout(it) },
                                    onShowBaseConversionChange = { accountDetailViewModel.updateShowBaseConversion(it) },
                                    onTimelineRangeChange = { accountDetailViewModel.updateCurrencyBalancesTimelineRange(it) }
                                )
                            }
                        }
                    } else {
                        val transactionsWithBalanceList = remember(state.cashFlowTransactions, state.account.initialBalance) {
                            var currentBalance = state.account.initialBalance
                            state.cashFlowTransactions.sortedBy { it.timestamp }.map { tx ->
                                val balanceBefore = currentBalance
                                val balanceAfter = if (tx.isIncome) currentBalance.add(tx.amount) else currentBalance.subtract(tx.amount)
                                currentBalance = balanceAfter
                                TransactionWithBalance(tx, balanceBefore, balanceAfter)
                            }.sortedByDescending { it.transaction.timestamp }
                        }

                        if (transactionsWithBalanceList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Rounded.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No transactions yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            val filter by transactionViewModel.filter.collectAsState()
                            val savedFilters by transactionViewModel.savedFilters.collectAsState()
                            val accounts by accountViewModel.accounts.collectAsState()
                            val categoriesList by transactionViewModel.categories.collectAsState()
                            val scope = rememberCoroutineScope()
                            val scaffoldState = rememberBottomSheetScaffoldState(
                                bottomSheetState = rememberStandardBottomSheetState(
                                    initialValue = SheetValue.PartiallyExpanded,
                                    skipHiddenState = true
                                )
                            )

                            // Apply account filter automatically if not already set specifically for this account
                            LaunchedEffect(accountId) {
                                if (!filter.accountIds.contains(accountId)) {
                                    transactionViewModel.updateFilter(filter.copy(accountIds = listOf(accountId)))
                                }
                            }

                            BottomSheetScaffold(
                                scaffoldState = scaffoldState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = screenPadding.calculateTopPadding())
                                    .background(Color.Transparent),
                                sheetPeekHeight = 90.dp,
                                sheetContainerColor = Color.Transparent,
                                sheetContentColor = MaterialTheme.colorScheme.onSurface,
                                sheetTonalElevation = 0.dp,
                                sheetShadowElevation = 0.dp,
                                sheetShape = RectangleShape,
                                sheetDragHandle = null,
                                sheetSwipeEnabled = true,
                                sheetContent = {
                                    TransactionFilterSheet(
                                        filter = filter,
                                        accounts = accounts,
                                        categories = categoriesList,
                                        savedFilters = savedFilters,
                                        onSetDateRange = { start, end -> transactionViewModel.setDateRangeFilter(start, end) },
                                        onUpdateFilter = { updatedFilter ->
                                            // Ensure we always stay filtered by this account
                                            transactionViewModel.updateFilter(updatedFilter.copy(accountIds = listOf(accountId)))
                                        },
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
                                        isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded,
                                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                        showAccountFilter = false
                                    )
                                }
                            ) { sheetPadding ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = sheetPadding.calculateTopPadding())
                                ) {
                                    val filteredTransactions = remember(transactionsWithBalanceList, filter) {
                                        transactionsWithBalanceList.filter { item ->
                                            val tx = item.transaction
                                            
                                            // Since we are in account detail, we only care about filters OTHER than accountId 
                                            // (though the list is already scoped to this account in ViewModel)
                                            val matchesNote = filter.queryNote == null || 
                                                (if (filter.isNoteRegex) tx.note.contains(filter.queryNote!!.toRegex()) else tx.note.contains(filter.queryNote!!, ignoreCase = true))
                                            val matchesPayee = filter.queryPayee == null || 
                                                (if (filter.isPayeeRegex) tx.title.contains(filter.queryPayee!!.toRegex()) else tx.title.contains(filter.queryPayee!!, ignoreCase = true))
                                            val matchesCategory = filter.categoryIds.isEmpty() || tx.categoryId in filter.categoryIds
                                            val matchesDate = (filter.startDate == null || tx.timestamp >= filter.startDate!!) &&
                                                              (filter.endDate == null || tx.timestamp <= filter.endDate!!)
                                            val matchesCurrency = filter.currencies.isEmpty() || tx.currency in filter.currencies
                                            val matchesIncome = filter.isIncome == null || tx.isIncome == filter.isIncome
                                            
                                            matchesNote && matchesPayee && matchesCategory && matchesDate && matchesCurrency && matchesIncome
                                        }
                                    }

                                    AeonTransactionList(
                                        transactions = filteredTransactions,
                                        accountMap = mapOf(account.id to account.name),
                                        categoryMap = categoryMap,
                                        onTransactionClick = { onEditTransactionClick(it.id) },
                                        contentPadding = PaddingValues(
                                            bottom = sheetPadding.calculateBottomPadding() + screenPadding.calculateBottomPadding() + 16.dp
                                        ),
                                        precision = state.decimalPrecision,
                                        useNoteAsHeader = state.useNoteAsHeader,
                                        baseCurrency = account.currencyCode,
                                        exchangeRates = state.exchangeRates,
                                        accountCurrencyMap = mapOf(account.id to account.currencyCode),
                                        filterAccountId = accountId
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
