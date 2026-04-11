package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.ui.components.AppDrawerContent
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel
import com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.gleemanager.ui.util.formatCurrency
import com.osscabinet.gleemanager.ui.util.isDark
import com.osscabinet.gleemanager.domain.model.Account
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsViewScreen(
    onDashboardClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onLoansClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onSettingsClick: () -> Unit,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    analyticsViewModel: AnalyticsViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val accounts by accountViewModel.accounts.collectAsState()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val transactions by transactionViewModel.transactions.collectAsState()
    val nonArchivedAccountIds = remember(nonArchivedAccounts) { nonArchivedAccounts.map { it.id }.toSet() }
    val activeTransactions = remember(transactions, nonArchivedAccountIds) {
        transactions.filter { it.accountId in nonArchivedAccountIds }
    }
    val currentProfile by settingsViewModel.currentProfile.collectAsState()
    val baseCurrency by settingsViewModel.currency.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentProfile = currentProfile,
                nonArchivedAccounts = nonArchivedAccounts,
                selectedDestination = "accounts_view",
                onDashboardClick = onDashboardClick,
                onAccountsViewClick = { scope.launch { drawerState.close() } },
                onRecordsClick = onRecordsClick,
                onAnalyticsClick = onAnalyticsClick,
                onBudgetsClick = onBudgetsClick,
                onLoansClick = onLoansClick,
                onManageAccountsClick = onManageAccountsClick,
                onSettingsClick = onSettingsClick,
                onAccountClick = onAccountClick,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    title = {
                        Column {
                            Text("Accounts", style = MaterialTheme.typography.titleLarge)
                            Text("Manage your ledgers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddAccountClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                NetWorthSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
                CurrencyAggregationSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
                
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ledgers", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onManageAccountsClick) {
                            Text("Manage", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    nonArchivedAccounts.forEach { account ->
                        val accountTransactions = transactions.filter { it.accountId == account.id }
                        
                        val supportedCurrencies = remember(account, accountTransactions) {
                            val list = mutableSetOf<String>()
                            list.add(account.currencyCode)
                            list.addAll(account.allowedCurrencies)
                            accountTransactions.forEach { list.add(it.currency) }
                            list.toList().sorted()
                        }

                        val accountCurrencyBalances = supportedCurrencies.associateWith { curr ->
                            val txSum = accountTransactions.filter { it.currency == curr }.sumOf { if (it.isIncome) it.amount else -it.amount }
                            if (curr == account.currencyCode) account.initialBalance + txSum else txSum
                        }

                        val balanceInBase = accountCurrencyBalances.entries.sumOf { (curr, amount) ->
                            val rate = exchangeRates[curr] ?: 1.0
                            if (rate != 0.0) amount / rate else amount
                        }

                        val accountColor = try { Color(AndroidColor.parseColor(account.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        val contentColor = if (accountColor.isDark()) Color.White else Color.Black

                        AccountCard(
                            title = account.name.uppercase(),
                            balance = formatCurrency(balanceInBase, baseCurrency),
                            currencyBalances = accountCurrencyBalances,
                            icon = when(account.structureType) {
                                "Savings" -> Icons.Default.Savings
                                "Credit Card" -> Icons.Default.CreditCard
                                else -> Icons.Default.AccountBalance
                            },
                            containerColor = accountColor,
                            contentColor = contentColor,
                            subAccounts = listOf(account.structureType),
                            onClick = { onAccountClick(account.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountCard(
    title: String,
    balance: String,
    currencyBalances: Map<String, Double> = emptyMap(),
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    subAccounts: List<String>,
    cardNumber: String? = null,
    extraLabel: String? = null,
    border: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(32.dp),
        border = if (border) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    balance,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
                Icon(icon, contentDescription = null, tint = contentColor.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Horizontal scrollable list for subAccounts and currency codes
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subAccounts.forEach { sub ->
                        Surface(
                            color = contentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                sub.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = contentColor
                            )
                        }
                    }
                    
                    currencyBalances.forEach { (curr, bal) ->
                        Surface(
                            color = contentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    curr,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                                    color = contentColor.copy(alpha = 0.6f)
                                )
                                Text(
                                    com.osscabinet.gleemanager.ui.util.formatAmount(bal),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))

                if (cardNumber != null) {
                    Text(cardNumber, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.5f))
                }
                if (extraLabel != null) {
                    Text(extraLabel, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.5f))
                }
            }
        }
    }
}
