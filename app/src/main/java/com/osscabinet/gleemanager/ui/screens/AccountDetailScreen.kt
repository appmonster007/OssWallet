package com.osscabinet.gleemanager.ui.screens

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
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import com.osscabinet.gleemanager.ui.components.*
import com.osscabinet.gleemanager.ui.util.formatCurrencyRight
import com.osscabinet.gleemanager.ui.util.isDark
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel,
    analyticsViewModel: com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel,
    onBackClick: () -> Unit,
    onEditAccountClick: (String) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onAddTransactionClick: (String) -> Unit
) {
    val currentTheme by settingsViewModel.theme.collectAsState(initial = "System")
    
    val accounts by accountViewModel.accounts.collectAsState()
    val account = accounts.find { it.id == accountId }
    
    val transactions by transactionViewModel.transactions.collectAsState()
    val accountTransactions = transactions.filter { it.accountId == accountId }
    
    val baseCurrency by settingsViewModel.currency.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()

    val totalBalanceInAccountCurrency by remember(account, accountTransactions, exchangeRates) {
        derivedStateOf {
            val currentAccount = account ?: return@derivedStateOf 0.0
            val initialBalance = currentAccount.initialBalance
            
            val convertedTxSum = accountTransactions.sumOf { tx ->
                val amount = if (tx.isIncome) tx.amount else -tx.amount
                val rate = exchangeRates[tx.currency] ?: 1.0
                if (rate != 0.0) amount / rate else amount
            }
            initialBalance + convertedTxSum
        }
    }

    val incomeInAccountCurrency by remember(account, accountTransactions, exchangeRates) {
        derivedStateOf {
            accountTransactions.filter { it.isIncome }.sumOf { tx ->
                val rate = exchangeRates[tx.currency] ?: 1.0
                if (rate != 0.0) tx.amount / rate else tx.amount
            }
        }
    }

    val expenseInAccountCurrency by remember(account, accountTransactions, exchangeRates) {
        derivedStateOf {
            accountTransactions.filter { !it.isIncome }.sumOf { tx ->
                val rate = exchangeRates[tx.currency] ?: 1.0
                if (rate != 0.0) tx.amount / rate else tx.amount
            }
        }
    }

    val netInAccountCurrency = incomeInAccountCurrency - expenseInAccountCurrency

    val supportedCurrencies = remember(account, accountTransactions) {
        val list = mutableSetOf<String>()
        account?.let {
            list.add(it.currencyCode)
            list.addAll(it.allowedCurrencies)
        }
        // Also add any currencies present in transactions even if not in "allowed"
        accountTransactions.forEach { list.add(it.currency) }
        list.toList().sorted()
    }


    val analyticsUiState by analyticsViewModel.uiState.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Transactions")

    LaunchedEffect(account?.id) {
        val newCurrency = account?.currencyCode
        if (newCurrency != null && newCurrency != analyticsUiState.selectedCurrency) {
            analyticsViewModel.onCurrencySelected(newCurrency)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(account?.name ?: "Account", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEditAccountClick(accountId) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { /* More actions like Delete */ }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More")
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
            ExtendedFloatingActionButton(
                onClick = { onAddTransactionClick(accountId) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Transaction") },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        if (account == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
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
                                balance = totalBalanceInAccountCurrency,
                                netBalance = netInAccountCurrency,
                                baseCurrency = baseCurrency,
                                exchangeRates = exchangeRates
                            )
                        }

                        item {
                            AeonSectionHeader(title = "Monthly Cash Flow")
                            Spacer(modifier = Modifier.height(4.dp))
                            AeonCashFlowSection(
                                transactions = accountTransactions,
                                baseCurrency = account.currencyCode,
                                exchangeRates = exchangeRates,
                                currentTheme = currentTheme,
                                showTitle = false
                            )
                        }

                        item {
                            AeonSectionHeader(title = "Currency Breakdown")
                            Spacer(modifier = Modifier.height(4.dp))
                            // Group by currency to show separate balances
                            val finalBalances = remember(account, accountTransactions, supportedCurrencies) {
                                val currentAccount = account
                                val initialBalance = currentAccount.initialBalance

                                val txsByCurrency = accountTransactions.groupBy { it.currency }
                                supportedCurrencies.associateWith { curr ->
                                    val txSum = txsByCurrency[curr]?.sumOf { if (it.isIncome) it.amount else -it.amount } ?: 0.0
                                    if (curr == currentAccount.currencyCode) initialBalance + txSum else txSum
                                }
                            }

                            AeonCurrencyBreakdown(
                                balances = finalBalances,
                                baseCurrency = baseCurrency,
                                accountCurrency = account.currencyCode,
                                exchangeRates = exchangeRates,
                                showTitle = false
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp) // Extra padding for FAB
                    ) {
                        item {
                            AeonSectionHeader(
                                title = "TRANSACTION HISTORY",
                                trailing = {
                                    Text(
                                        "${accountTransactions.size} total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            )
                        }

                        if (accountTransactions.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
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
                            }
                        } else {
                            items(accountTransactions.sortedByDescending { it.timestamp }, key = { it.id }) { transaction ->
                                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                AeonTransactionItem(
                                    title = transaction.title,
                                    sub = "${transaction.category} • ${dateFormat.format(Date(transaction.timestamp))}",
                                    amount = formatCurrencyRight(if (transaction.isIncome) transaction.amount else -transaction.amount, transaction.currency),
                                    isIncome = transaction.isIncome,
                                    icon = categoryIcons[transaction.category],
                                    onClick = { onEditTransactionClick(transaction.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
