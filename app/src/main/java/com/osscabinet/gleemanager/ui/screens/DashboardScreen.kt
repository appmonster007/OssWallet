package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.osscabinet.gleemanager.ui.components.AccountIconMap
import kotlinx.coroutines.launch
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
    onAccountsViewClick: () -> Unit,
    onLoansClick: () -> Unit,
    onSettingsClick: () -> Unit,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel,
    analyticsViewModel: com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
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
    val gridColumns by settingsViewModel.dashboardGridColumns.collectAsState()
    
    // Using AnalyticsViewModel to get exchange rates
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()

    LaunchedEffect(baseCurrency) {
        analyticsViewModel.onCurrencySelected(baseCurrency)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            com.osscabinet.gleemanager.ui.components.AppDrawerContent(
                currentProfile = currentProfile,
                nonArchivedAccounts = nonArchivedAccounts,
                selectedDestination = "dashboard",
                onDashboardClick = { scope.launch { drawerState.close() } },
                onAccountsViewClick = onAccountsViewClick,
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
                            Text(currentProfile?.name ?: "Intelligence", style = MaterialTheme.typography.titleLarge)
                            Text("Personal Ledger", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    NetWorthSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
                }
                item {
                    CurrencyAggregationSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
                }
                item {
                    CashFlowSummarySection(activeTransactions, baseCurrency, exchangeRates)
                }
                item {
                    AccountsSection(onManageClick = onManageAccountsClick, onAccountClick, nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates, gridColumns)
                }
                item {
                    RecentActivitySection(onRecordsClick, activeTransactions, nonArchivedAccounts)
                }
                item {
                    BudgetTrackingSection(baseCurrency)
                }
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsSection(onManageClick: () -> Unit, onAccountClick: (String) -> Unit, accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double>, gridColumns: Int = 2) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Accounts", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onManageClick) {
                Text("Manage", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (accounts.isEmpty()) {
            AccountGridItem(
                account = Account("empty", "GET STARTED", 0.0, 0.0, baseCurrency, "Default", "#808080", "Add"),
                balanceInBase = 0.0,
                totalBalanceInAccountCurrency = 0.0,
                incomeInAccountCurrency = 0.0,
                expenseInAccountCurrency = 0.0,
                baseCurrency = baseCurrency,
                onClick = onManageClick,
                modifier = Modifier.fillMaxWidth(),
                gridColumns = gridColumns
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = gridColumns
            ) {
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

                    // Total balance in base currency
                    val balanceInBase = accountCurrencyBalances.entries.sumOf { (curr, amount) ->
                        val rate = exchangeRates[curr] ?: 1.0
                        if (rate != 0.0) amount / rate else amount
                    }
                    
                    val accountRate = exchangeRates[account.currencyCode] ?: 1.0
                    val totalBalanceInAccountCurrency = if (accountRate != 0.0) balanceInBase * accountRate else balanceInBase

                    val incomeInAccountCurrency = accountTransactions.filter { it.isIncome }.sumOf { tx ->
                        val rate = exchangeRates[tx.currency] ?: 1.0
                        val inBase = if (rate != 0.0) tx.amount / rate else tx.amount
                        if (accountRate != 0.0) inBase * accountRate else inBase
                    }

                    val expenseInAccountCurrency = accountTransactions.filter { !it.isIncome }.sumOf { tx ->
                        val rate = exchangeRates[tx.currency] ?: 1.0
                        val inBase = if (rate != 0.0) tx.amount / rate else tx.amount
                        if (accountRate != 0.0) inBase * accountRate else inBase
                    }

                    AccountGridItem(
                        account = account,
                        balanceInBase = balanceInBase,
                        totalBalanceInAccountCurrency = totalBalanceInAccountCurrency,
                        incomeInAccountCurrency = incomeInAccountCurrency,
                        expenseInAccountCurrency = expenseInAccountCurrency,
                        baseCurrency = baseCurrency,
                        onClick = { onAccountClick(account.id) },
                        modifier = Modifier.weight(1f),
                        gridColumns = gridColumns
                    )
                }
                if (accounts.size % gridColumns != 0) {
                    repeat(gridColumns - (accounts.size % gridColumns)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun AccountGridItem(
    account: Account,
    balanceInBase: Double,
    totalBalanceInAccountCurrency: Double,
    incomeInAccountCurrency: Double,
    expenseInAccountCurrency: Double,
    baseCurrency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gridColumns: Int = 2
) {
    val accountColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
    val isDark = accountColor.isDark()
    val contentColor = if (isDark) Color.White else Color.Black

    // Scaling factors based on grid columns
    val titleFontSize = when(gridColumns) {
        1 -> 18.sp
        2 -> 14.sp
        3 -> 12.sp
        else -> 10.sp
    }
    val balanceFontSize = when(gridColumns) {
        1 -> 24.sp
        2 -> 16.sp
        3 -> 14.sp
        else -> 12.sp
    }
    val secondaryBalanceFontSize = when(gridColumns) {
        1 -> 14.sp
        2 -> 12.sp
        else -> 10.sp
    }
    val badgeFontSize = when(gridColumns) {
        1 -> 12.sp
        2 -> 9.sp
        3 -> 8.sp
        else -> 7.sp
    }
    val iconSize = if (gridColumns > 2) 14.dp else 18.dp
    val padding = if (gridColumns > 2) 12.dp else 16.dp

    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(if (gridColumns > 2) 16.dp else 24.dp),
        color = accountColor,
        contentColor = contentColor
    ) {
        Column(modifier = Modifier.padding(padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    AccountIconMap[account.icon] ?: Icons.Default.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    account.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = titleFontSize),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(if (gridColumns > 2) 8.dp else 12.dp))

            // Primary (Base) Currency Balance - Smaller
            Text(
                formatCurrency(balanceInBase, baseCurrency),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = secondaryBalanceFontSize),
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Account Default Currency Balance - Bigger
            Text(
                formatCurrency(totalBalanceInAccountCurrency, account.currencyCode),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = balanceFontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Income and Expense combined (converted to account currency)
            val badgeContent = @Composable {
                val incomeBg = Color(0xFF2E7D32)
                val expenseBg = Color(0xFFC62828)
                val badgeTextColor = Color.White

                // Income Badge
                Surface(
                    color = incomeBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, badgeTextColor.copy(alpha = 0.3f)),
                    modifier = if (gridColumns <= 2) Modifier.weight(1f, fill = false) else Modifier.wrapContentSize()
                ) {
                    Text(
                        "+${com.osscabinet.gleemanager.ui.util.formatAmount(incomeInAccountCurrency)}",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = badgeFontSize, fontWeight = FontWeight.ExtraBold),
                        color = badgeTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Expense Badge
                Surface(
                    color = expenseBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, badgeTextColor.copy(alpha = 0.3f)),
                    modifier = if (gridColumns <= 2) Modifier.weight(1f, fill = false) else Modifier.wrapContentSize()
                ) {
                    Text(
                        "-${com.osscabinet.gleemanager.ui.util.formatAmount(expenseInAccountCurrency)}",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = badgeFontSize, fontWeight = FontWeight.ExtraBold),
                        color = badgeTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (gridColumns <= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    badgeContent()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    badgeContent()
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
