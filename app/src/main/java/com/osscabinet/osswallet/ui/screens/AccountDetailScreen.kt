package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.dashboard.*
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.components.AeonExtendedFloatingActionButton
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.isDark
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

import com.osscabinet.ossWallet.ui.viewmodel.AccountDetailViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountDetailUiState

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

    val transactionsWithBalance by transactionViewModel.transactionsWithBalance.collectAsState()
    val accountTransactionsWithBalance = remember(transactionsWithBalance, accountId, uiState) {
        val filtered = transactionsWithBalance.filter { it.transaction.accountId == accountId }
        val state = uiState as? AccountDetailUiState.Success
        if (state != null && !state.showPlannedPayments) {
            val now = System.currentTimeMillis()
            filtered.filter { it.transaction.timestamp <= now }
        } else {
            filtered
        }
    }

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
    ) { innerPadding ->
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
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (selectedTabIndex == 0) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp) // Extra padding for FAB
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
                        if (accountTransactionsWithBalance.isEmpty()) {
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
                            AeonTransactionList(
                                transactions = accountTransactionsWithBalance,
                                accountMap = mapOf(account.id to account.name),
                                categoryMap = categoryMap,
                                onTransactionClick = { onEditTransactionClick(it.id) },
                                contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp),
                                precision = state.decimalPrecision,
                                useNoteAsHeader = state.useNoteAsHeader
                            )
                        }
                    }
                }
            }
        }
    }
}
