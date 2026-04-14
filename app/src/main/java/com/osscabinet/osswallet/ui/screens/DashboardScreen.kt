package com.osscabinet.ossWallet.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.ui.viewmodel.DashboardViewModel
import com.osscabinet.ossWallet.ui.viewmodel.DashboardUiState
import com.osscabinet.ossWallet.ui.viewmodel.AccountStats
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.toContainerColor
import com.osscabinet.ossWallet.ui.util.toOnContainerColor
import com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel
import com.osscabinet.ossWallet.ui.components.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    templateViewModel: TemplateViewModel,
    dashboardViewModel: DashboardViewModel,
    budgetViewModel: BudgetViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val dashboardUiState by dashboardViewModel.uiState.collectAsState()
    val accounts by accountViewModel.accounts.collectAsState()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    
    val currentProfile by settingsViewModel.currentProfile.collectAsState()
    val analyticsUiState by analyticsViewModel.uiState.collectAsState()
    val budgets by budgetViewModel.budgets.collectAsState()
    val currentTheme by settingsViewModel.theme.collectAsState(initial = "System")
    val gridColumns by settingsViewModel.dashboardGridColumns.collectAsState()
    val decimalPrecision by settingsViewModel.decimalPrecision.collectAsState()

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

    // We no longer need this LaunchedEffect because AnalyticsViewModel 
    // now observes settingsDataStore.currency directly in its init block.
    // However, we might still want to ensure it's synced if it was overridden by other screens.
    val settingsCurrency by settingsViewModel.currency.collectAsState()
    LaunchedEffect(settingsCurrency) {
        analyticsViewModel.onCurrencySelected(settingsCurrency)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            modifier = Modifier.fillMaxSize(),
            drawerContent = {
                AppDrawerContent(
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
                when (val state = dashboardUiState) {
                    is DashboardUiState.Loading -> {
                        DashboardShimmer(innerPadding)
                    }
                    is DashboardUiState.Success -> {
                            DashboardContent(
                                innerPadding = innerPadding,
                                pagerState = pagerState,
                                tabs = tabs,
                                state = state,
                                currentTheme = currentTheme,
                                gridColumns = gridColumns,
                                decimalPrecision = state.decimalPrecision,
                                onManageAccountsClick = onManageAccountsClick,
                                onAccountsViewClick = onAccountsViewClick,
                                onAccountClick = onAccountClick,
                                onRecordsClick = onRecordsClick,
                                onBudgetsClick = onBudgetsClick,
                                transactionViewModel = transactionViewModel,
                                budgets = budgets,
                                dashboardViewModel = dashboardViewModel
                            )
                    }
                    is DashboardUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message)
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
                            AeonFloatingActionButton(
                                onClick = { 
                                    isFabExpanded = false
                                    onAddClick(type, name)
                                },
                                modifier = Modifier.size(40.dp),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
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
                        AeonFloatingActionButton(
                            onClick = { 
                                isFabExpanded = false
                                onAddClick(2, null)
                            },
                            modifier = Modifier.size(40.dp),
                            icon = Icons.Default.SyncAlt,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
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
                        AeonFloatingActionButton(
                            onClick = { 
                                isFabExpanded = false
                                onAddClick(0, null)
                            },
                            modifier = Modifier.size(40.dp),
                            icon = Icons.Default.Add,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            val rotation by animateFloatAsState(
                targetValue = if (isFabExpanded) 45f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )

            AeonFloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                modifier = Modifier
                    .zIndex(3f)
                    .graphicsLayer { rotationZ = rotation },
                icon = Icons.Default.Add,
                contentDescription = if (isFabExpanded) "Close" else "Add"
            )
        }
    }
}


@Composable
fun DashboardShimmer(innerPadding: PaddingValues) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(150.dp, 20.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                Box(modifier = Modifier.size(250.dp, 48.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(8.dp)))
                Box(modifier = Modifier.size(100.dp, 24.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(16.dp)))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Box(modifier = Modifier.size(120.dp, 60.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(20.dp)))
                }
            }
        }
        item {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(32.dp)))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(100.dp, 20.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                    Box(modifier = Modifier.size(60.dp, 20.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f).height(120.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(24.dp)))
                    Box(modifier = Modifier.weight(1f).height(120.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(24.dp)))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardContent(
    innerPadding: PaddingValues,
    pagerState: androidx.compose.foundation.pager.PagerState,
    tabs: List<String>,
    state: DashboardUiState.Success,
    currentTheme: String,
    gridColumns: Int,
    decimalPrecision: Int,
    onManageAccountsClick: () -> Unit,
    onAccountsViewClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onRecordsClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    transactionViewModel: TransactionViewModel,
    budgets: List<Budget>,
    dashboardViewModel: DashboardViewModel
) {
    val activeTransactions = remember(state.recentTransactions) {
        state.recentTransactions.map { it.transaction }
    }

    val sectionOrder by dashboardViewModel.sectionOrder.collectAsState()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        verticalAlignment = Alignment.Top
    ) { page ->
        when (page) {
            0 -> {
                CardReorderableList(
                    items = sectionOrder,
                    key = { it.ordinal },
                    onReorder = { dashboardViewModel.updateSectionOrder(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) { section, isDragging, dragModifier ->
                    when (section) {
                        com.osscabinet.ossWallet.ui.viewmodel.DashboardSection.NET_WORTH -> {
                            NetWorthSection(
                                accounts = state.accounts,
                                baseCurrency = state.baseCurrency,
                                modifier = Modifier.fillMaxWidth(),
                                headerModifier = dragModifier,
                                exchangeRates = state.exchangeRates,
                                precision = decimalPrecision
                            )
                        }
                        com.osscabinet.ossWallet.ui.viewmodel.DashboardSection.CURRENCY_BALANCES -> {
                            CurrencyAggregationSection(
                                currencyTotals = state.currencyTotals,
                                baseCurrency = state.baseCurrency,
                                exchangeRates = state.exchangeRates,
                                modifier = Modifier.fillMaxWidth(),
                                headerModifier = dragModifier,
                                precision = decimalPrecision
                            )
                        }
                        com.osscabinet.ossWallet.ui.viewmodel.DashboardSection.CASHFLOW -> {
                            AeonCashFlowSummary(
                                transactions = activeTransactions,
                                baseCurrency = state.baseCurrency,
                                exchangeRates = state.exchangeRates,
                                currentTheme = currentTheme,
                                modifier = Modifier.fillMaxWidth(),
                                headerModifier = dragModifier,
                                showTitle = true,
                                precision = decimalPrecision
                            )
                        }
                        com.osscabinet.ossWallet.ui.viewmodel.DashboardSection.ACCOUNTS -> {
                            AccountsSection(
                                onManageClick = onManageAccountsClick,
                                onAccountsViewClick = onAccountsViewClick,
                                onAccountClick = onAccountClick,
                                onGridColumnsChange = { dashboardViewModel.updateGridColumns(it) },
                                accounts = state.accounts,
                                accountStats = state.accountStats,
                                baseCurrency = state.baseCurrency,
                                exchangeRates = state.exchangeRates,
                                currentTheme = currentTheme,
                                modifier = Modifier.fillMaxWidth(),
                                gridColumns = gridColumns,
                                precision = decimalPrecision,
                                headerModifier = dragModifier.fillMaxWidth()
                            )
                        }
                        com.osscabinet.ossWallet.ui.viewmodel.DashboardSection.BUDGETS -> {
                            if (budgets.isNotEmpty()) {
                                BudgetTrackingSection(
                                    onManageClick = onBudgetsClick,
                                    budgets = budgets,
                                    baseCurrency = state.baseCurrency,
                                    modifier = Modifier.fillMaxWidth(),
                                    precision = decimalPrecision,
                                    headerModifier = dragModifier.fillMaxWidth()
                                )
                            } else {
                                Box(modifier = Modifier.size(0.dp))
                            }
                        }
                    }
                }
            }
            1 -> {

                val accountMap = remember(state.accounts) { state.accounts.associate { it.id to it.name } }
                val categories by transactionViewModel.categories.collectAsState()
                val categoryMap = remember(categories) { categories.associateBy { it.name.lowercase() } }

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

                    items(state.recentTransactions) { item ->
                        val transaction = item.transaction
                        val categoryEntity = categoryMap[transaction.category.lowercase()]
                        val balanceBeforeValue = item.balanceAfter - (if (transaction.isIncome) transaction.amount else -transaction.amount)

                        AeonTransactionItem(
                            category = transaction.category,
                            account = accountMap[transaction.accountId] ?: "Unknown",
                            amount = formatCurrencyRight(if (transaction.isIncome) transaction.amount else -transaction.amount, transaction.currency, decimalPrecision),
                            balanceBefore = formatCurrencyRight(balanceBeforeValue, transaction.currency, decimalPrecision),
                            balanceAfter = formatCurrencyRight(item.balanceAfter, transaction.currency, decimalPrecision),
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountsSection(
    onManageClick: () -> Unit,
    onAccountsViewClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    accounts: List<Account>,
    accountStats: Map<String, AccountStats>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    currentTheme: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    gridColumns: Int = 2,
    precision: Int = 2
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AeonFlowSectionHeader(
                title = "Accounts",
                modifier = headerModifier.clickable(onClick = onAccountsViewClick),
                trailing = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "Grid Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).width(200.dp)) {
                                Text(
                                    "Grid Columns: $gridColumns",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Slider(
                                        value = gridColumns.toFloat(),
                                        onValueChange = { onGridColumnsChange(it.toInt()) },
                                        valueRange = 1f..3f,
                                        steps = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            onManageClick()
                                            showMenu = false
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Manage Accounts",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )

            if (accounts.isEmpty()) {
                AeonAccountCardLarge(
                    account = Account("empty", "GET STARTED", BigDecimal.ZERO, BigDecimal.ZERO, baseCurrency, "Default", "#808080", "Add"),
                    balanceInBase = BigDecimal.ZERO,
                    baseCurrency = baseCurrency,
                    exchangeRates = exchangeRates,
                    isDarkTheme = when (currentTheme) {
                        "Light" -> false
                        "Dark", "Amoled" -> true
                        else -> isSystemInDarkTheme()
                    },
                    isAmoled = currentTheme == "Amoled",
                    onClick = onManageClick,
                    modifier = Modifier.fillMaxWidth(),
                    precision = precision
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = gridColumns
                ) {
                    accounts.forEach { account ->
                        val balanceInBase = convertAmount(account.balance, account.currencyCode, baseCurrency, exchangeRates, precision)
                        val isDarkTheme = when (currentTheme) {
                            "Light" -> false
                            "Dark", "Amoled" -> true
                            else -> isSystemInDarkTheme()
                        }
                        val isAmoled = currentTheme == "Amoled"
                        val stats = accountStats[account.id]

                        when (gridColumns) {
                            1 -> AeonAccountCardLarge(
                                account = account,
                                balanceInBase = balanceInBase,
                                baseCurrency = baseCurrency,
                                exchangeRates = exchangeRates,
                                currencyBalances = stats?.currencyBalances ?: emptyMap(),
                                incomeInAccountCurrency = stats?.incomeInAccountCurrency ?: BigDecimal.ZERO,
                                expenseInAccountCurrency = stats?.expenseInAccountCurrency ?: BigDecimal.ZERO,
                                isDarkTheme = isDarkTheme,
                                isAmoled = isAmoled,
                                onClick = { onAccountClick(account.id) },
                                modifier = Modifier.fillMaxWidth(),
                                precision = precision
                            )
                            2 -> AeonAccountCardMedium(
                                account = account,
                                balanceInBase = balanceInBase,
                                baseCurrency = baseCurrency,
                                isDarkTheme = isDarkTheme,
                                isAmoled = isAmoled,
                                onClick = { onAccountClick(account.id) },
                                modifier = Modifier.weight(1f),
                                precision = precision
                            )
                            3 -> AeonAccountCardSmall(
                                account = account,
                                balanceInBase = balanceInBase,
                                baseCurrency = baseCurrency,
                                isDarkTheme = isDarkTheme,
                                isAmoled = isAmoled,
                                onClick = { onAccountClick(account.id) },
                                modifier = Modifier.weight(1f),
                                precision = precision
                            )
                            else -> AeonAccountCardSmall(
                                account = account,
                                balanceInBase = balanceInBase,
                                baseCurrency = baseCurrency,
                                isDarkTheme = isDarkTheme,
                                isAmoled = isAmoled,
                                onClick = { onAccountClick(account.id) },
                                modifier = Modifier.weight(1f),
                                precision = precision
                            )
                        }
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
}


@Composable
fun BudgetTrackingSection(
    onManageClick: () -> Unit,
    budgets: List<Budget>,
    baseCurrency: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    precision: Int = 2
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AeonFlowSectionHeader(
                title = "Budget Tracking",
                modifier = headerModifier,
                trailing = {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable(onClick = onManageClick)
                    )
                }
            )

            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                budgets.forEach { budget ->
                    AeonBudgetProgress(
                        label = budget.category,
                        current = budget.spent,
                        total = budget.limit,
                        color = if (budget.spent > budget.limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        baseCurrency = baseCurrency,
                        precision = precision
                    )
                }
            }
        }
    }
}
