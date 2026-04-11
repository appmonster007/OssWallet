package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import com.osscabinet.gleemanager.ui.util.formatCurrency
import com.osscabinet.gleemanager.ui.util.isDark
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAnalyticsClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onAddClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onLoansClick: () -> Unit,
    onSettingsClick: () -> Unit,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel,
    analyticsViewModel: com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
) {
    val scrollState = rememberScrollState()
    val accounts by accountViewModel.accounts.collectAsState()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val transactions by transactionViewModel.transactions.collectAsState()
    val nonArchivedAccountIds = remember(nonArchivedAccounts) { nonArchivedAccounts.map { it.id }.toSet() }
    val activeTransactions = remember(transactions, nonArchivedAccountIds) {
        transactions.filter { it.accountId in nonArchivedAccountIds }
    }
    val currentProfile by settingsViewModel.currentProfile.collectAsState()
    val baseCurrency by settingsViewModel.currency.collectAsState()
    
    // Using AnalyticsViewModel to get exchange rates
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()

    LaunchedEffect(baseCurrency) {
        analyticsViewModel.onCurrencySelected(baseCurrency)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(currentProfile?.name ?: "Intelligence", style = MaterialTheme.typography.titleLarge)
                            Text("Personal Ledger", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("DASHBOARD") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onRecordsClick,
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                    label = { Text("RECORDS") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onAnalyticsClick,
                    icon = { Icon(Icons.Default.AutoGraph, contentDescription = null) },
                    label = { Text("ANALYTICS") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onBudgetsClick,
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                    label = { Text("BUDGETS") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onLoansClick,
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("LOANS") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            NetWorthSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
            CurrencyAggregationSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
            CashFlowSummarySection(activeTransactions, baseCurrency, exchangeRates)
            AccountsSection(onManageClick = onManageAccountsClick, onAccountClick, nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
            RecentActivitySection(onRecordsClick, activeTransactions, nonArchivedAccounts)
            BudgetTrackingSection(baseCurrency)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CashFlowSummarySection(
    transactions: List<Transaction>,
    baseCurrency: String,
    exchangeRates: Map<String, Double>
) {
    val allSupportedCurrencies = remember(transactions) {
        transactions.map { it.currency }.distinct().sorted()
    }

    val flowData by remember(allSupportedCurrencies, transactions, exchangeRates, baseCurrency) {
        derivedStateOf {
            allSupportedCurrencies.mapNotNull { curr ->
                val txsForCurr = transactions.filter { it.currency == curr }
                val income = txsForCurr.filter { it.isIncome }.sumOf { it.amount }
                val expense = txsForCurr.filter { !it.isIncome }.sumOf { it.amount }

                if (income == 0.0 && expense == 0.0) null
                else {
                    val rate = if (curr == baseCurrency) 1.0 else (exchangeRates[curr] ?: 1.0)
                    val incomeInBase = if (rate != 0.0) income / rate else income
                    val expenseInBase = if (rate != 0.0) expense / rate else expense
                    Triple(curr, Pair(income, incomeInBase), Pair(expense, expenseInBase))
                }
            }
        }
    }

    val totalIncomeInBase = flowData.sumOf { it.second.second }
    val totalExpenseInBase = flowData.sumOf { it.third.second }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("CASH FLOW SUMMARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Income Section
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF2E7D32).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("INCOME", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        formatCurrency(totalIncomeInBase, baseCurrency),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        flowData.forEach { (curr, income, _) ->
                            FlowBreakdownItem(curr, income.first, income.second, baseCurrency)
                        }
                    }
                }
            }

            // Expense Section
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFC62828).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPENSE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        formatCurrency(totalExpenseInBase, baseCurrency),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        flowData.forEach { (curr, _, expense) ->
                            FlowBreakdownItem(curr, expense.first, expense.second, baseCurrency)
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun NetWorthSection(accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double> = emptyMap()) {
    val totalBalanceInBase = remember(accounts, transactions, exchangeRates, baseCurrency) {
        val accountInitialBalances = accounts.sumOf { account ->
            val rate = exchangeRates[account.currencyCode] ?: 1.0
            if (rate != 0.0) account.initialBalance / rate else account.initialBalance
        }
        val transactionSumInBase = transactions.sumOf { tx ->
            val amount = if (tx.isIncome) tx.amount else -tx.amount
            val rate = exchangeRates[tx.currency] ?: 1.0
            if (rate != 0.0) amount / rate else amount
        }
        accountInitialBalances + transactionSumInBase
    }
    
    Column {
        Text(
            "TOTAL NET WORTH",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            formatCurrency(totalBalanceInBase, baseCurrency),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Color(0xFF2E7D32).copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Text(
                        "+12.4%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "vs last month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun CurrencyAggregationSection(accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double>) {
    val allSupportedCurrencies = remember(accounts, transactions) {
        val list = mutableSetOf<String>()
        accounts.forEach { account ->
            list.add(account.currencyCode)
            list.addAll(account.allowedCurrencies)
        }
        transactions.forEach { list.add(it.currency) }
        list.toList().sorted()
    }

    val currencyTotals = remember(accounts, transactions, allSupportedCurrencies) {
        allSupportedCurrencies.associateWith { curr ->
            val initialBalanceSum = accounts.filter { it.currencyCode == curr }.sumOf { it.initialBalance }
            val txSum = transactions.filter { it.currency == curr }.sumOf { if (it.isIncome) it.amount else -it.amount }
            initialBalanceSum + txSum
        }
    }
    
    if (allSupportedCurrencies.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AGGREGATED CURRENCY BALANCES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(currencyTotals.toList()) { (currency, amount) ->
                    val rate = exchangeRates[currency] ?: 1.0
                    val inBase = if (rate != 0.0) amount / rate else amount
                    
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currency, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                if (currency != baseCurrency) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "≈ ${formatCurrency(inBase, baseCurrency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Text(
                                formatCurrency(amount, currency),
                                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountsSection(onManageClick: () -> Unit, onAccountClick: (String) -> Unit, accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Accounts", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onManageClick) {
                Text("Manage", color = MaterialTheme.colorScheme.primary)
            }
        }

        if (accounts.isEmpty()) {
            AccountCard(
                title = "GET STARTED",
                balance = "$0.00",
                icon = Icons.Default.Add,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                subAccounts = emptyList(),
                onClick = onManageClick
            )
        } else {
            accounts.forEach { account ->
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

                // Total balance in base currency for the main display
                val balanceInBase = accountCurrencyBalances.entries.sumOf { (curr, amount) ->
                    val rate = exchangeRates[curr] ?: 1.0
                    if (rate != 0.0) amount / rate else amount
                }

                val accountColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }

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
                    contentColor = if (accountColor.isDark()) Color.White else Color.Black,
                    subAccounts = listOf(account.structureType.uppercase()),
                    onClick = { onAccountClick(account.id) }
                )
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

@Composable
fun RecentActivitySection(onViewAll: () -> Unit, transactions: List<Transaction>, accounts: List<Account>) {
    val accountMap = accounts.associate { it.id to it.name }
    val recentTransactions = transactions.sortedByDescending { it.timestamp }.take(3)
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val today = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onViewAll) {
                Text("View all", color = MaterialTheme.colorScheme.primary)
            }
        }

        recentTransactions.forEach { transaction ->
            val dateStr = dateFormat.format(Date(transaction.timestamp))
            val displayDate = if (dateStr == today) "Today" else dateStr
            
            val icon = when(transaction.category.lowercase()) {
                "food", "dining" -> Icons.Default.Restaurant
                "shopping" -> Icons.Default.ShoppingBag
                "transport" -> Icons.Default.DirectionsCar
                "salary", "income" -> Icons.Default.Payments
                else -> Icons.Default.Receipt
            }

            ActivityItem(
                title = transaction.title,
                sub = "${transaction.category} • $displayDate",
                amount = formatCurrency(if (transaction.isIncome) transaction.amount else -transaction.amount, transaction.currency),
                account = (accountMap[transaction.accountId] ?: "Unknown").uppercase(),
                icon = icon,
                iconBg = if (transaction.isIncome) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ActivityItem(title: String, sub: String, amount: String, account: String, icon: ImageVector, iconBg: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = iconBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (amount.startsWith("+")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
                Text(account, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun BudgetTrackingSection(baseCurrency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Budget Tracking", style = MaterialTheme.typography.titleMedium)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                BudgetProgress("Lifestyle & Leisure", 1420f, 2000f, MaterialTheme.colorScheme.primary, baseCurrency)
                BudgetProgress("Housing & Utilities", 3800f, 4000f, Color(0xFFE57373), baseCurrency)
            }
        }
    }
}

@Composable
fun BudgetProgress(label: String, current: Float, total: Float, color: Color, baseCurrency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("${formatCurrency(current.toDouble(), baseCurrency)} / ${formatCurrency(total.toDouble(), baseCurrency)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        LinearProgressIndicator(
            progress = { current / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}
