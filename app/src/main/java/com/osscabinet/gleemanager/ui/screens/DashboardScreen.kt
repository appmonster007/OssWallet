package com.osscabinet.gleemanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.domain.model.CashFlowType
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel
import com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.gleemanager.ui.util.formatCurrencyRight
import com.osscabinet.gleemanager.ui.util.toContainerColor
import com.osscabinet.gleemanager.ui.util.toOnContainerColor
import com.osscabinet.gleemanager.ui.viewmodel.TemplateViewModel
import com.osscabinet.gleemanager.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    onAnalyticsClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onAddClick: (Int?, String?) -> Unit,
    onManageAccountsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onAccountsViewClick: () -> Unit,
    onLoansClick: () -> Unit,
    onExchangeRatesClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    analyticsViewModel: AnalyticsViewModel,
    templateViewModel: TemplateViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val accounts by accountViewModel.accounts.collectAsState()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val transactionsWithBalance by transactionViewModel.transactionsWithBalance.collectAsState()
    val nonArchivedAccountIds = remember(nonArchivedAccounts) { nonArchivedAccounts.map { it.id }.toSet() }
    val activeTransactionsWithBalance = remember(transactionsWithBalance, nonArchivedAccountIds) {
        transactionsWithBalance.filter { it.transaction.accountId in nonArchivedAccountIds }
    }
    val activeTransactions = remember(activeTransactionsWithBalance) {
        activeTransactionsWithBalance.map { it.transaction }
    }

    val currentProfile by settingsViewModel.currentProfile.collectAsState()
    val baseCurrency by settingsViewModel.currency.collectAsState()
    val exchangeRates by analyticsViewModel.exchangeRates.collectAsState()
    val currentTheme by settingsViewModel.theme.collectAsState(initial = "System")
    val gridColumns by settingsViewModel.dashboardGridColumns.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val tabs = listOf("Glance", "Recent Activities")

    var isFabExpanded by remember { mutableStateOf(false) }

    val dbTemplates by templateViewModel.templates.collectAsState()
    val starredTemplates by templateViewModel.starredTemplates.collectAsState()
    
    val templates = remember(dbTemplates, starredTemplates) {
        val displayTemplates = if (starredTemplates.isNotEmpty()) {
            starredTemplates
        } else if (dbTemplates.isNotEmpty()) {
            dbTemplates.take(5)
        } else {
            emptyList()
        }

        if (displayTemplates.isEmpty()) {
            listOf(
                Triple("Grocery Run", Icons.Rounded.ShoppingCart, 0),
                Triple("Salary Deposit", Icons.Rounded.Payments, 1),
                Triple("Rent Payment", Icons.Rounded.Home, 0),
                Triple("Coffee", Icons.Rounded.CoffeeMaker, 0)
            )
        } else {
            displayTemplates.map {
                Triple(it.name, when(it.transactionType) {
                    1 -> Icons.Rounded.Payments
                    2 -> Icons.Rounded.SwapHoriz
                    else -> Icons.Rounded.ShoppingCart
                }, it.transactionType)
            }
        }
    }

    LaunchedEffect(baseCurrency) {
        analyticsViewModel.onCurrencySelected(baseCurrency)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            modifier = Modifier.fillMaxSize(),
            drawerContent = {
                com.osscabinet.gleemanager.ui.components.AppDrawerContent(
                    currentProfile = currentProfile,
                    nonArchivedAccounts = nonArchivedAccounts,
                    starredTemplates = starredTemplates,
                    selectedDestination = "dashboard",
                    onDashboardClick = { scope.launch { drawerState.close() } },
                    onAccountsViewClick = onAccountsViewClick,
                    onRecordsClick = onRecordsClick,
                    onAnalyticsClick = onAnalyticsClick,
                    onBudgetsClick = onBudgetsClick,
                    onLoansClick = onLoansClick,
                    onExchangeRatesClick = onExchangeRatesClick,
                    onTemplatesClick = onTemplatesClick,
                    onManageAccountsClick = onManageAccountsClick,
                    onSettingsClick = onSettingsClick,
                    onAccountClick = onAccountClick,
                    onTemplateClick = { template -> onAddClick(template.transactionType, template.name) },
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            },
                            title = {
                                Column {
                                    Text(currentProfile?.name ?: "Intelligence", style = MaterialTheme.typography.titleLarge)
                                    Text("Personal Account", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        )
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            divider = {},
                            indicator = { tabPositions ->
                                if (pagerState.currentPage < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                    text = {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (page) {
                        0 -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                            ) {
                                item {
                                    NetWorthSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
                                }
                                item {
                                    CurrencyAggregationSection(nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates)
                                }
                                item {
                                    AeonCashFlowSummary(activeTransactions, baseCurrency, exchangeRates, currentTheme)
                                }
                                item {
                                    AccountsSection(onManageAccountsClick, onAccountClick, nonArchivedAccounts, activeTransactions, baseCurrency, exchangeRates, currentTheme, gridColumns)
                                }
                                item {
                                    BudgetTrackingSection(baseCurrency)
                                }
                            }
                        }
                        1 -> {
                            val accountMap = remember(nonArchivedAccounts) { nonArchivedAccounts.associate { it.id to it.name } }
                            val categories by transactionViewModel.categories.collectAsState()
                            val categoryMap = remember(categories) { categories.associateBy { it.name.lowercase() } }
                            val recentTransactions = remember(activeTransactionsWithBalance) { 
                                activeTransactionsWithBalance.sortedByDescending { it.transaction.timestamp }.take(20) 
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                            ) {
                                item {
                                    AeonSectionHeader(
                                        title = "Recent Activities",
                                        trailing = {
                                            TextButton(onClick = onRecordsClick) {
                                                Text("View all", color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    )
                                }

                                items(recentTransactions) { item ->
                                    val transaction = item.transaction
                                    val categoryEntity = categoryMap[transaction.category.lowercase()]
                                    val balanceBeforeValue = item.balanceAfter - (if (transaction.isIncome) transaction.amount else -transaction.amount)

                                    AeonTransactionItem(
                                        category = transaction.category,
                                        account = accountMap[transaction.accountId] ?: "Unknown",
                                        amount = formatCurrencyRight(if (transaction.isIncome) transaction.amount else -transaction.amount, transaction.currency),
                                        balanceBefore = formatCurrencyRight(balanceBeforeValue, transaction.currency),
                                        balanceAfter = formatCurrencyRight(item.balanceAfter, transaction.currency),
                                        type = if (transaction.isIncome) CashFlowType.INCOME else CashFlowType.EXPENSE,
                                        timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                                        note = transaction.note,
                                        tags = transaction.tags,
                                        icon = categoryEntity?.let { categoryIcons[it.name] },
                                        iconBg = categoryEntity?.let { Color(it.colorHex) },
                                        onClick = { /* Navigate to edit? */ },
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Background dimming - outside the drawer/scaffold to cover everything
        AnimatedVisibility(
            visible = isFabExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize().zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isFabExpanded = false }
            )
        }

        // Expandable FAB Menu - Moved to root Box for better z-index control
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .zIndex(2f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 72.dp, end = 4.dp)
            ) {
                templates.forEach { (name, icon, type) ->
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 4.dp,
                                onClick = { 
                                    isFabExpanded = false
                                    onAddClick(type, name)
                                }
                            ) {
                                Text(
                                    "Template: $name",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            FloatingActionButton(
                                onClick = { 
                                    isFabExpanded = false
                                    onAddClick(type, name)
                                },
                                modifier = Modifier.size(40.dp),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = CircleShape
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Transfer
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp,
                            onClick = { 
                                isFabExpanded = false
                                onAddClick(2, null)
                            }
                        ) {
                            Text(
                                "Transfer",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        FloatingActionButton(
                            onClick = { 
                                isFabExpanded = false
                                onAddClick(2, null)
                            },
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // New Transaction
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp,
                            onClick = { 
                                isFabExpanded = false
                                onAddClick(0, null)
                            }
                        ) {
                            Text(
                                "New Transaction",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        FloatingActionButton(
                            onClick = { 
                                isFabExpanded = false
                                onAddClick(0, null)
                            },
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            val rotation by animateFloatAsState(
                targetValue = if (isFabExpanded) 45f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )

            FloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.zIndex(3f)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (isFabExpanded) "Close" else "Add",
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
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
        AeonSectionHeader(title = "TOTAL NET WORTH")
        Text(
            formatCurrencyRight(totalBalanceInBase, baseCurrency),
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
            AeonSectionHeader(title = "AGGREGATED CURRENCY BALANCES")

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(currencyTotals.toList().sortedBy { it.first }) { (currency, amount) ->
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
                                        "≈ ${formatCurrencyRight(inBase, baseCurrency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Text(
                                formatCurrencyRight(amount, currency),
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
fun AccountsSection(onManageClick: () -> Unit, onAccountClick: (String) -> Unit, accounts: List<Account>, transactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, Double>, currentTheme: String, gridColumns: Int = 2) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AeonSectionHeader(
            title = "Accounts",
            trailing = {
                TextButton(onClick = onManageClick) {
                    Text("Manage", style = MaterialTheme.typography.labelLarge)
                }
            }
        )

        if (accounts.isEmpty()) {
            AccountGridItem(
                account = Account("empty", "GET STARTED", 0.0, 0.0, baseCurrency, "Default", "#808080", "Add"),
                balanceInBase = 0.0,
                totalBalanceInAccountCurrency = 0.0,
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
//                    .offset { IntOffset(x = (cardSize.width * 0.1f).toInt(), y = 0) }
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
                    formatCurrencyRight(balanceInBase, baseCurrency),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = secondaryBalanceFontSize),
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    formatCurrencyRight(totalBalanceInAccountCurrency, account.currencyCode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = balanceFontSize),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                /*
                Spacer(modifier = Modifier.height(8.dp))

                val badges = @Composable {
                    val incomeColor = Color(0xFF2E7D32)
                    val expenseColor = Color(0xFFC62828)

                    Surface(
                        color = incomeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, incomeColor.copy(alpha = 0.2f)),
                        modifier = if (gridColumns <= 2) Modifier.weight(1f, fill = false) else Modifier.wrapContentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "+${com.osscabinet.gleemanager.ui.util.formatAmount(incomeInAccountCurrency)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = badgeFontSize, fontWeight = FontWeight.ExtraBold),
                                color = incomeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = incomeColor, modifier = Modifier.size(14.dp))
                        }
                    }

                    Surface(
                        color = expenseColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, expenseColor.copy(alpha = 0.2f)),
                        modifier = if (gridColumns <= 2) Modifier.weight(1f, fill = false) else Modifier.wrapContentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "-${com.osscabinet.gleemanager.ui.util.formatAmount(expenseInAccountCurrency)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = badgeFontSize, fontWeight = FontWeight.ExtraBold),
                                color = expenseColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = expenseColor, modifier = Modifier.size(14.dp))
                        }
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
                */
            }
        }
    }
}

@Composable
fun BudgetTrackingSection(baseCurrency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AeonSectionHeader(title = "Budget Tracking")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                AeonBudgetProgress("Lifestyle & Leisure", 1420f, 2000f, MaterialTheme.colorScheme.primary, baseCurrency)
                AeonBudgetProgress("Housing & Utilities", 3800f, 4000f, Color(0xFFE57373), baseCurrency)
            }
        }
    }
}
