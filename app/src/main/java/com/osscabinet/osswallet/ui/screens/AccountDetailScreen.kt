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
import com.osscabinet.ossWallet.ui.components.transactions.TransactionListEmptyState
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
                            val filter by transactionViewModel.accountDetailFilter.collectAsState()
                            val savedFilters by transactionViewModel.savedFilters.collectAsState()
                            val accounts by accountViewModel.accounts.collectAsState()
                            val accountNamesMap = remember(accounts) { accounts.associate { it.id to it.name } }
                            val accountCurrencyMap = remember(accounts) { accounts.associate { it.id to it.currencyCode } }
                            val categoriesList by transactionViewModel.categories.collectAsState()
                            val analyticsState by analyticsViewModel.uiState.collectAsState()
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
                                    transactionViewModel.updateAccountDetailFilter(filter.copy(accountIds = listOf(accountId)))
                                }
                            }

                            // Reset filters when leaving the transactions tab or the screen
                            DisposableEffect(Unit) {
                                onDispose {
                                    transactionViewModel.clearAccountDetailFilters()
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
                                        onSetDateRange = { start, end -> transactionViewModel.setAccountDetailDateRangeFilter(start, end) },
                                        onUpdateFilter = { updatedFilter ->
                                            // Ensure we always stay filtered by this account
                                            transactionViewModel.updateAccountDetailFilter(updatedFilter.copy(accountIds = listOf(accountId)))
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
                                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                            
                                            // Manual in-memory filtering for account detail screen
                                            val queryNote = filter.queryNote
                                            val matchesNote = queryNote == null || 
                                                (if (filter.isNoteRegex) {
                                                    try { Regex(queryNote, RegexOption.IGNORE_CASE).containsMatchIn(tx.note) } catch(e: Exception) { false }
                                                } else tx.note.contains(queryNote, ignoreCase = true))
                                            
                                            val queryPayee = filter.queryPayee
                                            val matchesPayee = queryPayee == null || 
                                                (if (filter.isPayeeRegex) {
                                                    try { Regex(queryPayee, RegexOption.IGNORE_CASE).containsMatchIn(tx.payee) } catch(e: Exception) { false }
                                                } else tx.payee.contains(queryPayee, ignoreCase = true))
                                            
                                            val categoryIds = filter.categoryIds
                                            val matchesCategory = categoryIds.isEmpty() || tx.categoryId in categoryIds
                                            
                                            val startDate = filter.startDate
                                            val endDate = filter.endDate
                                            val matchesDate = (startDate == null || tx.timestamp >= startDate) &&
                                                              (endDate == null || tx.timestamp <= endDate)
                                            
                                            val currencies = filter.currencies
                                            val matchesCurrency = currencies.isEmpty() || tx.currency in currencies
                                            
                                            val isIncome = filter.isIncome
                                            val matchesIncome = isIncome == null || tx.isIncome == isIncome
                                            
                                            val paymentMethods = filter.paymentMethods
                                            val matchesPaymentMethod = paymentMethods.isEmpty() || tx.paymentMethod in paymentMethods
                                            
                                            matchesNote && matchesPayee && matchesCategory && matchesDate && matchesCurrency && matchesIncome && matchesPaymentMethod
                                        }
                                    }

                                if (filteredTransactions.isEmpty()) {
                                    TransactionListEmptyState()
                                } else {
                                    AeonTransactionList(
                                        transactions = filteredTransactions,
                                        accountMap = accountNamesMap,
                                        categoryMap = categoryMap,
                                        onTransactionClick = { tx -> onEditTransactionClick(tx.id) },
                                        contentPadding = PaddingValues(
                                            bottom = sheetPadding.calculateBottomPadding() + screenPadding.calculateBottomPadding() + 16.dp
                                        ),
                                        precision = state.decimalPrecision,
                                        useNoteAsHeader = state.useNoteAsHeader,
                                        showOpeningBalance = true, // Force show balances for account details
                                        showClosingBalance = true,
                                        baseCurrency = analyticsState.selectedCurrency,
                                        exchangeRates = state.exchangeRates,
                                        accountCurrencyMap = accountCurrencyMap,
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
}
