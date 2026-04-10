package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import com.osscabinet.gleemanager.ui.util.getCurrencySymbol
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
    val transactions by transactionViewModel.transactions.collectAsState()
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
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
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
            NetWorthSection(accounts, transactions, baseCurrency, exchangeRates)
            CurrencyAggregationSection(transactions, baseCurrency, exchangeRates)
            IntelligenceInsightsSection(accounts, transactions, baseCurrency, exchangeRates)
            AccountsSection(onManageAccountsClick, onAccountClick, accounts, transactions, baseCurrency, exchangeRates)
            RecentActivitySection(onRecordsClick, baseCurrency)
            BudgetTrackingSection(baseCurrency)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun IntelligenceInsightsSection(accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double> = emptyMap()) {
    // Calculate current total net worth including all transactions and initial balances
    val totalBalanceInBase = transactions.groupBy { it.currency }.map { (currency, txs) ->
        val currencySum = txs.sumOf { if (it.isIncome) it.amount else -it.amount }
        val rate = exchangeRates[currency] ?: 1.0
        if (rate == 0.0) 0.0 else currencySum / rate
    }.sum() + accounts.sumOf { account ->
        val rate = exchangeRates[account.currencyCode] ?: 1.0
        if (rate == 0.0) 0.0 else account.balance / rate
    }
    
    val expectedBalance = totalBalanceInBase * 1.05
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("INTELLIGENCE INSIGHTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("PREDICTIVE BALANCE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                }
                
                Column {
                    Text(
                        "Expected by month-end:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${getCurrencySymbol(baseCurrency)}${String.format("%,.2f", expectedBalance)}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
                    )
                }
                
                LinearProgressIndicator(
                    progress = 0.85f,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                
                Text(
                    "You are on track to save 12% more than last month. Current spending trend is down by ${getCurrencySymbol(baseCurrency)}420.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NetWorthSection(accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double> = emptyMap()) {
    // Total net worth = (Initial balance of all accounts in base) + (Sum of all transactions in base)
    // Actually, account.balance should already be current. But the request says "balance for each currency separately"
    // and "aggregate balance in base".
    // If account.balance is just the initial balance plus transactions, we should calculate it per currency.
    
    val totalBalanceInBase = transactions.groupBy { it.currency }.map { (currency, txs) ->
        val currencySum = txs.sumOf { if (it.isIncome) it.amount else -it.amount }
        val rate = exchangeRates[currency] ?: 1.0
        if (rate == 0.0) 0.0 else currencySum / rate
    }.sum() + accounts.sumOf { account ->
        val rate = exchangeRates[account.currencyCode] ?: 1.0
        if (rate == 0.0) 0.0 else account.balance / rate
    }
    
    Column {
        Text(
            "TOTAL NET WORTH",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${getCurrencySymbol(baseCurrency)}${String.format("%,.0f", totalBalanceInBase)}",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                ".${String.format("%02d", ((totalBalanceInBase % 1) * 100).toInt())}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
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
fun CurrencyAggregationSection(transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double>) {
    val currencyTotals = transactions.groupBy { it.currency }.mapValues { (_, txs) ->
        txs.sumOf { if (it.isIncome) it.amount else -it.amount }
    }
    
    if (currencyTotals.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AGGREGATED CURRENCY BALANCES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(currencyTotals.toList()) { (currency, amount) ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(currency, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${getCurrencySymbol(currency)}${String.format("%,.2f", amount)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum")
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
                
                // Group by currency to show separate balances
                val currencyBalances = accountTransactions.groupBy { it.currency }.mapValues { (curr, txs) ->
                    val txSum = txs.sumOf { if (it.isIncome) it.amount else -it.amount }
                    if (curr == account.currencyCode) account.balance + txSum else txSum
                }.toMutableMap()
                
                if (!currencyBalances.containsKey(account.currencyCode)) {
                    currencyBalances[account.currencyCode] = account.balance
                }

                // Calculate total for this account in base currency
                val totalInBase = currencyBalances.map { (curr, bal) ->
                    val rate = exchangeRates[curr] ?: 1.0
                    if (rate == 0.0) 0.0 else bal / rate
                }.sum()

                AccountCard(
                    title = account.name.uppercase(),
                    balance = "${getCurrencySymbol(baseCurrency)}${String.format("%,.2f", totalInBase)}",
                    currencyBalances = currencyBalances,
                    icon = when(account.structureType) {
                        "Savings" -> Icons.Default.Savings
                        "Credit Card" -> Icons.Default.CreditCard
                        else -> Icons.Default.AccountBalance
                    },
                    containerColor = Color(android.graphics.Color.parseColor(account.colorHex)),
                    contentColor = if (Color(android.graphics.Color.parseColor(account.colorHex)).isDark()) Color.White else Color.Black,
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
                
                // Top Right Multi-currency breakdown
                if (currencyBalances.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        val entries = currencyBalances.toList()
                        // Chunk entries to display multiple per line or one per line as in screenshot
                        // The screenshot shows something like 2-3 lines
                        entries.chunked(2).forEach { chunk ->
                            Text(
                                chunk.joinToString(" | ") { (curr, bal) ->
                                    "${getCurrencySymbol(curr)}${String.format("%,.2f", bal)}"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = contentColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
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
                    
                    currencyBalances.keys.forEach { curr ->
                        Surface(
                            color = contentColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                curr.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = contentColor
                            )
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
fun RecentActivitySection(onViewAll: () -> Unit, baseCurrency: String) {
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

        ActivityItem("Apple Store", "Electronics • Today", "- ${getCurrencySymbol(baseCurrency)}1,299.00", "CHECKING", Icons.Default.ShoppingBag)
        ActivityItem("Salary Deposit", "Income • Oct 28", "+ ${getCurrencySymbol(baseCurrency)}8,500.00", "SAVINGS", Icons.Default.Payments, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ActivityItem("The Alchemist", "Dining • Oct 27", "- ${getCurrencySymbol(baseCurrency)}142.50", "CHECKING", Icons.Default.Restaurant)
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
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("INTELLIGENCE INSIGHT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Your savings rate is 15% higher than last quarter. Consider moving ${getCurrencySymbol(baseCurrency)}5,000 to your Portfolio for 4.2% APY.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetProgress(label: String, current: Float, total: Float, color: Color, baseCurrency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("${getCurrencySymbol(baseCurrency)}${current.toInt()} / ${getCurrencySymbol(baseCurrency)}${total.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        LinearProgressIndicator(
            progress = current / total,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}
