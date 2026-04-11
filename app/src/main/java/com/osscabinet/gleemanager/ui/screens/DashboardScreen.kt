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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel
import com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.gleemanager.ui.util.formatCurrency
import com.osscabinet.gleemanager.ui.util.isDark
import com.osscabinet.gleemanager.ui.util.toContainerColor
import com.osscabinet.gleemanager.ui.util.toOnContainerColor
import com.osscabinet.gleemanager.ui.components.AccountIconMap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val currentTheme by settingsViewModel.theme.collectAsState(initial = "System")

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
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    CashFlowSummarySection(activeTransactions, baseCurrency, exchangeRates)
                }
                
                item {
                    NetWorthSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
                }

                item {
                    val gridColumns = 2 // or from settings
                    AccountsSection(onManageAccountsClick, onAccountClick, nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates, currentTheme, gridColumns)
                }

                item {
                    RecentActivitySection(onRecordsClick, activeTransactions, nonArchivedAccounts)
                }

                item {
                    BudgetTrackingSection(baseCurrency)
                }

                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}

@Composable
fun CashFlowSummarySection(transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double>) {
    val now = Calendar.getInstance()
    val monthStart = now.clone() as Calendar
    monthStart.set(Calendar.DAY_OF_MONTH, 1)
    monthStart.set(Calendar.HOUR_OF_DAY, 0)
    monthStart.set(Calendar.MINUTE, 0)
    monthStart.set(Calendar.SECOND, 0)

    val monthTransactions = transactions.filter { it.timestamp >= monthStart.timeInMillis }
    
    val incomeInBase = monthTransactions.filter { it.isIncome }.sumOf { tx ->
        val rate = exchangeRates[tx.currency] ?: 1.0
        if (rate != 0.0) tx.amount / rate else tx.amount
    }
    
    val expenseInBase = monthTransactions.filter { !it.isIncome }.sumOf { tx ->
        val rate = exchangeRates[tx.currency] ?: 1.0
        if (rate != 0.0) tx.amount / rate else tx.amount
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Cash Flow", style = MaterialTheme.typography.titleMedium)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.ArrowUpward, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Text("Income", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(formatCurrency(incomeInBase, baseCurrency), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.ArrowDownward, null, tint = Color(0xFFF44336), modifier = Modifier.size(16.dp))
                        Text("Expense", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(formatCurrency(expenseInBase, baseCurrency), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun NetWorthSection(accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double>) {
    val totalNetWorth = accounts.sumOf { account ->
        val accountTransactions = transactions.filter { it.accountId == account.id }
        val supportedCurrencies = mutableSetOf<String>()
        supportedCurrencies.add(account.currencyCode)
        supportedCurrencies.addAll(account.allowedCurrencies)
        accountTransactions.forEach { supportedCurrencies.add(it.currency) }

        val balances = supportedCurrencies.associateWith { curr ->
            val txSum = accountTransactions.filter { it.currency == curr }.sumOf { if (it.isIncome) it.amount else -it.amount }
            if (curr == account.currencyCode) account.initialBalance + txSum else txSum
        }

        balances.entries.sumOf { (curr, amount) ->
            val rate = exchangeRates[curr] ?: 1.0
            if (rate != 0.0) amount / rate else amount
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Total Net Worth", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                formatCurrency(totalNetWorth, baseCurrency),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun CurrencyAggregationSection(accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double>) {
    val currencyBalances = mutableMapOf<String, Double>()
    
    accounts.forEach { account ->
        val accountTransactions = transactions.filter { it.accountId == account.id }
        val supportedCurrencies = mutableSetOf<String>()
        supportedCurrencies.add(account.currencyCode)
        supportedCurrencies.addAll(account.allowedCurrencies)
        accountTransactions.forEach { supportedCurrencies.add(it.currency) }

        supportedCurrencies.forEach { curr ->
            val txSum = accountTransactions.filter { it.currency == curr }.sumOf { if (it.isIncome) it.amount else -it.amount }
            val amount = if (curr == account.currencyCode) account.initialBalance + txSum else txSum
            currencyBalances[curr] = currencyBalances.getOrDefault(curr, 0.0) + amount
        }
    }

    if (currencyBalances.size > 1) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("By Currency", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(currencyBalances.toList().sortedBy { it.first }) { (curr, amount) ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(curr, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)
                            Text(com.osscabinet.gleemanager.ui.util.formatAmount(amount), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsSection(onManageClick: () -> Unit, onAccountClick: (String) -> Unit, accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double>, currentTheme: String, gridColumns: Int = 2) {
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
                currentTheme = currentTheme,
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
                        currentTheme = currentTheme,
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
    currentTheme: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gridColumns: Int = 2
) {
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val isDarkTheme = when (currentTheme) {
        "Light" -> false
        "Dark", "Amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val isAmoled = currentTheme == "Amoled"
    
    val accountColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    val containerColor = accountColor.toContainerColor(isDarkTheme, isAmoled)
    val contentColor = accountColor.toOnContainerColor(isDarkTheme, isAmoled)

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
            .clickable { onClick() }
            .onSizeChanged { cardSize = it },
        shape = RoundedCornerShape(if (gridColumns > 2) 16.dp else 24.dp),
        color = containerColor,
        contentColor = contentColor,
        border = if (isAmoled) BorderStroke(1.dp, contentColor.copy(alpha = 0.3f)) else null
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val icon = AccountIconMap[account.icon] ?: Icons.Default.AccountBalance
            val iconHeight = with(density) { (cardSize.height * 0.75f).toDp() }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset { IntOffset(x = (cardSize.width * 0.2f).toInt(), y = 0) }
                    .size(iconHeight)
                    .alpha(0.15f),
                tint = contentColor
            )

            Column(modifier = Modifier.padding(padding)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        icon,
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

                Text(
                    formatCurrency(balanceInBase, baseCurrency),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = secondaryBalanceFontSize),
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    formatCurrency(totalBalanceInAccountCurrency, account.currencyCode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = balanceFontSize),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val badges = @Composable {
                    val incomeBg = Color(0xFF2E7D32)
                    val expenseBg = Color(0xFFC62828)
                    val badgeTextColor = Color.White

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
                        badges()
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        badges()
                    }
                }
            }
        }
    }
}

@Composable
fun RecentActivitySection(onViewAllClick: () -> Unit, transactions: List<Transaction>, accounts: List<Account>) {
    val recentTransactions = transactions.sortedByDescending { it.timestamp }.take(5)
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onViewAllClick) {
                Text("View All", style = MaterialTheme.typography.labelLarge)
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                recentTransactions.forEachIndexed { index, tx ->
                    val account = accounts.find { it.id == tx.accountId }
                    val accountColor = try { Color(android.graphics.Color.parseColor(account?.colorHex ?: "#808080")) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                    
                    ActivityItem(
                        title = tx.category,
                        subtitle = account?.name ?: "Unknown",
                        amount = (if (tx.isIncome) "+" else "-") + com.osscabinet.gleemanager.ui.util.formatCurrency(tx.amount, tx.currency),
                        date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tx.timestamp)),
                        icon = Icons.Default.Category,
                        color = accountColor
                    )
                    
                    if (index < recentTransactions.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItem(title: String, subtitle: String, amount: String, date: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(amount, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = if (amount.startsWith("+")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface)
            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun BudgetTrackingSection(baseCurrency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Budget Tracking", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BudgetProgress("Monthly Budget", 0.75f, 1500f, Color(0xFF673AB7), baseCurrency)
                BudgetProgress("Savings Goal", 0.4f, 5000f, Color(0xFF009688), baseCurrency)
            }
        }
    }
}

@Composable
fun BudgetProgress(label: String, progress: Float, total: Float, color: Color, baseCurrency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(formatCurrency((progress * total).toDouble(), baseCurrency), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}
