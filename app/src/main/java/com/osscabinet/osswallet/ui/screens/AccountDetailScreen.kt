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
import com.osscabinet.ossWallet.ui.components.AeonExtendedFloatingActionButton
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.isDark
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel,
    analyticsViewModel: com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel,
    onBackClick: () -> Unit,
    onEditAccountClick: (String) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onAddTransactionClick: (String) -> Unit
) {
    val currentTheme by settingsViewModel.theme.collectAsState(initial = "System")
    
    val accounts by accountViewModel.accounts.collectAsState()
    val account = accounts.find { it.id == accountId }
    
    val transactionsWithBalance by transactionViewModel.transactionsWithBalance.collectAsState()
    val accountTransactionsWithBalance = remember(transactionsWithBalance, accountId) {
        transactionsWithBalance.filter { it.transaction.accountId == accountId }
    }
    val accountTransactions = remember(accountTransactionsWithBalance) {
        accountTransactionsWithBalance.map { it.transaction }
    }
    
    val categories by transactionViewModel.categories.collectAsState()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val baseCurrency by settingsViewModel.currency.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()
    val decimalPrecision by settingsViewModel.decimalPrecision.collectAsState()

    val totalBalanceInAccountCurrency by remember(account, accountTransactions, exchangeRates, decimalPrecision) {
        derivedStateOf {
            val currentAccount = account ?: return@derivedStateOf BigDecimal.ZERO
            val initialBalance = currentAccount.initialBalance

            val currencyTotals = accountTransactions.groupBy { it.currency }
                .mapValues { (_, txs) ->
                    txs.fold(BigDecimal.ZERO) { acc, tx ->
                        if (tx.isIncome) acc.add(tx.amount) else acc.subtract(tx.amount)
                    }
                }
            
            val transactionsSumInAccountCurrency = calculateTotalInCurrency(currencyTotals, currentAccount.currencyCode, exchangeRates, decimalPrecision)
            initialBalance.add(transactionsSumInAccountCurrency)
        }
    }

    val incomeInAccountCurrency by remember(account, accountTransactions, exchangeRates, decimalPrecision) {
        derivedStateOf {
            val currentAccount = account ?: return@derivedStateOf BigDecimal.ZERO
            val currencyTotals = accountTransactions.filter { it.isIncome }.groupBy { it.currency }
                .mapValues { (_, txs) -> txs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
            calculateTotalInCurrency(currencyTotals, currentAccount.currencyCode, exchangeRates, decimalPrecision)
        }
    }

    val expenseInAccountCurrency by remember(account, accountTransactions, exchangeRates, decimalPrecision) {
        derivedStateOf {
            val currentAccount = account ?: return@derivedStateOf BigDecimal.ZERO
            val currencyTotals = accountTransactions.filter { !it.isIncome }.groupBy { it.currency }
                .mapValues { (_, txs) -> txs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
            calculateTotalInCurrency(currencyTotals, currentAccount.currencyCode, exchangeRates, decimalPrecision)
        }
    }

    val netInAccountCurrency = incomeInAccountCurrency.subtract(expenseInAccountCurrency)

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
    val accountsViewSettings by settingsViewModel.accountsViewSettings.collectAsState(initial = com.osscabinet.ossWallet.data.local.AccountsViewSettings())

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
                            Icon(Icons.Rounded.MoreHoriz, contentDescription = "More")
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
                                exchangeRates = exchangeRates,
                                precision = decimalPrecision
                            )
                        }

                        item {
                            AeonCashFlowSummary(
                                transactions = accountTransactions,
                                baseCurrency = account.currencyCode,
                                exchangeRates = exchangeRates,
                                currentTheme = currentTheme,
                                modifier = Modifier.fillMaxWidth(),
                                showTitle = true,
                                precision = decimalPrecision,
                                showBaseConversion = accountsViewSettings.cashFlowSection.showBaseConversion,
                                onShowBaseConversionChange = { settingsViewModel.updateAccountsViewCashFlowShowBaseConversion(it) }
                            )
                        }

                        item {
                            // Group by currency to show separate balances
                            val finalBalances = remember(account, accountTransactions, supportedCurrencies) {
                                val currentAccount = account
                                val initialBalance = currentAccount?.initialBalance ?: BigDecimal.ZERO

                                val txsByCurrency = accountTransactions.groupBy { it.currency }
                                supportedCurrencies.associateWith { curr ->
                                    val txSum = txsByCurrency[curr]?.fold(BigDecimal.ZERO) { acc, tx ->
                                        if (tx.isIncome) acc.add(tx.amount) else acc.subtract(tx.amount)
                                    } ?: BigDecimal.ZERO
                                    if (curr == currentAccount?.currencyCode) initialBalance.add(txSum) else txSum
                                }
                            }

                            CurrencyAggregationSection(
                                currencyTotals = finalBalances,
                                baseCurrency = baseCurrency,
                                exchangeRates = exchangeRates,
                                modifier = Modifier.fillMaxWidth(),
                                precision = decimalPrecision,
                                layout = accountsViewSettings.currencyBalancesSection.layout,
                                showBaseConversion = accountsViewSettings.currencyBalancesSection.showBaseConversion,
                                onLayoutChange = { settingsViewModel.updateAccountsViewCurrencyBalancesLayout(it) },
                                onShowBaseConversionChange = { settingsViewModel.updateAccountsViewCurrencyBalancesShowBaseConversion(it) }
                            )
                        }
                    }
                } else {
                    val categories by transactionViewModel.categories.collectAsState()
                    val categoryMap = remember(categories) { categories.associateBy { it.id } }

                    if (accountTransactions.isEmpty()) {
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
                            precision = decimalPrecision
                        )
                    }
                }
            }
        }
    }
}
