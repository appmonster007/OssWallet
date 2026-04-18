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
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.AeonExpandableFab
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.components.dashboard.*
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import com.osscabinet.ossWallet.ui.viewmodel.NetWorthUiState
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

    var navTriggered by remember { mutableStateOf(false) }
    
    // Reset navTriggered when returning to this screen
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        navTriggered = false
    }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

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
    
    val allTemplates by templateViewModel.templates.collectAsState()

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
                    onAccountsViewClick = { navigate(onAccountsViewClick) },
                    onRecordsClick = { navigate(onRecordsClick) },
                    onAnalyticsClick = { navigate(onAnalyticsClick) },
                    onBudgetsClick = { navigate(onBudgetsClick) },
                    onLoansClick = { navigate(onLoansClick) },
                    onExchangeRatesClick = { navigate(onExchangeRatesClick) },
                    onTemplatesClick = { navigate(onTemplatesClick) },
                    onManageAccountsClick = { navigate(onManageAccountsClick) },
                    onSettingsClick = { navigate(onSettingsClick) },
                    onAccountClick = { id -> navigate { onAccountClick(id) } },
                    onTemplateClick = { template -> navigate { onAddClick(template.transactionType, template.name) } },
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
                                Column(modifier = Modifier.clickable { navigate(onSettingsClick) }) {
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
                                onManageAccountsClick = { navigate(onManageAccountsClick) },
                                onAccountsViewClick = { navigate(onAccountsViewClick) },
                                onAccountClick = { id -> navigate { onAccountClick(id) } },
                                onRecordsClick = { navigate(onRecordsClick) },
                                onBudgetsClick = { navigate(onBudgetsClick) },
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

        AeonExpandableFab(
            onAddClick = { type, name -> navigate { onAddClick(type, name) } },
            templates = allTemplates
        )
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
    val netWorthUiState = remember(
        state.netWorth,
        state.baseCurrency,
        state.decimalPrecision,
        state.netWorthFontSize,
        state.netWorthTimelineRange
    ) {
        NetWorthUiState.Success(
            netWorth = state.netWorth,
            baseCurrency = state.baseCurrency,
            precision = state.decimalPrecision,
            fontSize = state.netWorthFontSize,
            timelineRange = state.netWorthTimelineRange
        )
    }

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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            AeonCashFlowSummary(
                                cashFlowResult = state.cashFlowResult,
                                baseCurrency = state.baseCurrency,
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
                        .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        AeonSectionHeader(
                            title = "Recent Activities",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                            amount = formatCurrencyRight(transaction.amount.abs(), transaction.currency, decimalPrecision),
                            balanceBefore = formatCurrencyRight(balanceBeforeValue, transaction.currency, decimalPrecision),
                            balanceAfter = formatCurrencyRight(item.balanceAfter, transaction.currency, decimalPrecision),
                            type = if (transaction.isIncome) CashFlowType.INCOME else CashFlowType.EXPENSE,
                            timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                            note = transaction.note,
                            tags = transaction.tags,
                            icon = categoryEntity?.let { categoryIcons[it.iconName] } ?: categoryIcons[categoryEntity?.name] ?: Icons.Rounded.Category,
                            iconBg = categoryEntity?.let { Color(it.colorHex) },
                            precision = decimalPrecision,
                            onClick = { /* Navigate to edit? */ }
                        )
                    }
                    
                    item {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}



