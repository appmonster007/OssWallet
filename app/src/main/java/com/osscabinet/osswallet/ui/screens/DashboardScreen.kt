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
import kotlinx.coroutines.flow.map
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
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.components.dashboard.*
import com.osscabinet.ossWallet.ui.viewmodel.NetWorthViewModel
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
    budgetViewModel: BudgetViewModel,
    netWorthViewModel: NetWorthViewModel
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
                        AeonShimmer(innerPadding = innerPadding)
                    }
                    is DashboardUiState.Success -> {
                            DashboardContent(
                                innerPadding = innerPadding,
                                pagerState = pagerState,
                                tabs = tabs,
                                state = state,
                                currentTheme = currentTheme,
                                gridColumns = state.gridColumns,
                                decimalPrecision = state.decimalPrecision,
                                onManageAccountsClick = onManageAccountsClick,
                                onAccountsViewClick = onAccountsViewClick,
                                onAccountClick = onAccountClick,
                                onRecordsClick = onRecordsClick,
                                onBudgetsClick = onBudgetsClick,
                                transactionViewModel = transactionViewModel,
                                budgets = budgets,
                                dashboardViewModel = dashboardViewModel,
                                netWorthViewModel = netWorthViewModel
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
    dashboardViewModel: DashboardViewModel,
    netWorthViewModel: NetWorthViewModel
) {
    val activeTransactions = remember(state.recentTransactions) {
        state.recentTransactions.map { it.transaction }
    }

    val sectionOrder by dashboardViewModel.sectionOrder.collectAsState()
    
    val netWorthUiState by remember(netWorthViewModel, dashboardViewModel) {
        netWorthViewModel.getNetWorthUiState(dashboardViewModel.settingsDataStore.dashboardSettings.map { it.netWorthSection })
    }.collectAsState()

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
                                uiState = netWorthUiState,
                                onFontSizeChange = { dashboardViewModel.updateNetWorthFontSize(it) },
                                onTimelineRangeChange = { dashboardViewModel.updateNetWorthTimelineRange(it) },
                                modifier = Modifier.fillMaxWidth(),
                                headerModifier = dragModifier
                            )
                        }
                        com.osscabinet.ossWallet.ui.viewmodel.DashboardSection.CURRENCY_BALANCES -> {
                            CurrencyAggregationSection(
                                currencyTotals = state.currencyTotals,
                                baseCurrency = state.baseCurrency,
                                exchangeRates = state.exchangeRates,
                                modifier = Modifier.fillMaxWidth(),
                                headerModifier = dragModifier,
                                precision = decimalPrecision,
                                layout = state.currencyBalancesLayout,
                                showBaseConversion = state.showBaseConversion,
                                timelineRange = state.currencyBalancesTimelineRange,
                                onLayoutChange = { dashboardViewModel.updateCurrencyBalancesLayout(it) },
                                onShowBaseConversionChange = { dashboardViewModel.updateShowBaseConversion(it) },
                                onTimelineRangeChange = { dashboardViewModel.updateCurrencyBalancesTimelineRange(it) }
                            )
                        }
                        com.osscabinet.ossWallet.ui.viewmodel.DashboardSection.CASHFLOW -> {
                            val cashFlowTransactions = remember(state.allTransactions, state.cashFlowTimelineRange) {
                                when (val range = state.cashFlowTimelineRange) {
                                    is TimelineRange.AllTime -> state.allTransactions
                                    is TimelineRange.ThisPeriod -> {
                                        val (start, end) = FilterUtils.getRangeForThis(range.period)
                                        state.allTransactions.filter { it.timestamp in start..end }
                                    }
                                    is TimelineRange.LastPeriod -> {
                                        val (start, end) = FilterUtils.getRangeForLast(range.period)
                                        state.allTransactions.filter { it.timestamp in start..end }
                                    }
                                    is TimelineRange.CustomRange -> {
                                        state.allTransactions.filter { it.timestamp in range.start..range.end }
                                    }
                                }
                            }
                            AeonCashFlowSummary(
                                transactions = cashFlowTransactions,
                                baseCurrency = state.baseCurrency,
                                exchangeRates = state.exchangeRates,
                                currentTheme = currentTheme,
                                modifier = Modifier.fillMaxWidth(),
                                headerModifier = dragModifier,
                                showTitle = true,
                                precision = decimalPrecision,
                                showBaseConversion = state.cashFlowShowBaseConversion,
                                timelineRange = state.cashFlowTimelineRange,
                                onShowBaseConversionChange = { dashboardViewModel.updateCashFlowShowBaseConversion(it) },
                                onTimelineRangeChange = { dashboardViewModel.updateCashFlowTimelineRange(it) }
                            )
                        }
                        com.osscabinet.ossWallet.ui.viewmodel.DashboardSection.ACCOUNTS -> {
                            AeonAccountsSection(
                                onManageClick = onManageAccountsClick,
                                onHeaderClick = onAccountsViewClick,
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
                                layout = state.accountsLayout,
                                onLayoutChange = { dashboardViewModel.updateAccountsLayout(it) },
                                timelineRange = state.accountsTimelineRange,
                                onTimelineRangeChange = { dashboardViewModel.updateAccountsTimelineRange(it) },
                                showAddAccountCard = state.showAddAccountCard,
                                onShowAddAccountCardChange = { dashboardViewModel.updateShowAddAccountCard(it) },
                                headerModifier = dragModifier.fillMaxWidth(),
                                settingsTitle = "Dashboard Settings"
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
                                    headerModifier = dragModifier.fillMaxWidth(),
                                    showProgressLabels = state.budgetShowProgressLabels,
                                    onShowProgressLabelsChange = { dashboardViewModel.updateBudgetShowProgressLabels(it) }
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
                val categoryMap = remember(categories) { categories.associateBy { it.id } }

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
                        val categoryEntity = categoryMap[transaction.categoryId]
                        val balanceBeforeValue = item.balanceAfter.subtract(transaction.amount)

                        AeonTransactionItem(
                            category = transaction.category,
                            account = accountMap[transaction.accountId] ?: "Unknown",
                            amount = formatCurrencyRight(transaction.amount.abs(), "", decimalPrecision),
                            balanceBefore = formatCurrencyRight(balanceBeforeValue, transaction.currency, decimalPrecision),
                            balanceAfter = formatCurrencyRight(item.balanceAfter, transaction.currency, decimalPrecision),
                            type = if (transaction.isIncome) CashFlowType.INCOME else CashFlowType.EXPENSE,
                            timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                            note = transaction.note,
                            tags = transaction.tags,
                            icon = categoryEntity?.let { categoryIcons[it.iconName] } ?: categoryIcons[categoryEntity?.name] ?: Icons.Rounded.Category,
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




