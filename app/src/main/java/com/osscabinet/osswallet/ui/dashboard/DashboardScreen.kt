package com.osscabinet.ossWallet.ui.dashboard

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.osscabinet.ossWallet.ui.dashboard.components.NetWorthCard
import com.osscabinet.ossWallet.ui.dashboard.components.RecentActivitiesCard
import com.osscabinet.ossWallet.ui.dashboard.components.AccountsCard
import com.osscabinet.ossWallet.ui.dashboard.components.CashFlowCard
import com.osscabinet.ossWallet.ui.dashboard.components.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.dashboard.components.BudgetsCard
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

    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }

    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val analyticsUiState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
    val budgets by budgetViewModel.budgets.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    val gridColumns by settingsViewModel.dashboardGridColumns.collectAsStateWithLifecycle()
    val decimalPrecision by settingsViewModel.decimalPrecision.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val tabs = listOf("Glance", "Recent Activities")

    var isFabExpanded by remember { mutableStateOf(false) }

    val dbTemplates by templateViewModel.templates.collectAsStateWithLifecycle()
    val starredTemplates by templateViewModel.starredTemplates.collectAsStateWithLifecycle()

    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()

    // We no longer need this LaunchedEffect because AnalyticsViewModel
    // now observes settingsDataStore.currency directly in its init block.
    // However, we might still want to ensure it's synced if it was overridden by other screens.
    val settingsCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()
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

    val sectionOrder by dashboardViewModel.sectionOrder.collectAsStateWithLifecycle()
    val netWorthUiState = remember(
        state.netWorth,
        state.netIncome,
        state.netExpense,
        state.baseCurrency,
        state.decimalPrecision,
        state.netWorthFontSize,
        state.netWorthTimelineRange
    ) {
        NetWorthUiState.Success(
            netWorth = state.netWorth,
            netIncome = state.netIncome,
            netExpense = state.netExpense,
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
                        DashboardSection.NET_WORTH -> {
                            NetWorthCard(
                                uiState = netWorthUiState,
                                onFontSizeChange = { dashboardViewModel.updateNetWorthFontSize(it) },
                                onTimelineRangeChange = { dashboardViewModel.updateNetWorthTimelineRange(it) },
                                modifier = Modifier.fillMaxWidth(),
                                headerModifier = dragModifier
                            )
                        }
                        DashboardSection.CURRENCY_BALANCES -> {
                            CurrencyBalancesCard(
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
                        DashboardSection.CASHFLOW -> {
                            CashFlowCard(
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
                        DashboardSection.ACCOUNTS -> {
                            AccountsCard(
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
                        DashboardSection.BUDGETS -> {
                            if (budgets.isNotEmpty()) {
                                BudgetsCard(
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
                val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
                val categoryMap = remember(categories) { categories.associateBy { it.id } }

                RecentActivitiesCard(
                    recentTransactions = state.recentTransactions,
                    accountNamesMap = accountMap,
                    categoryMap = categoryMap,
                    decimalPrecision = state.decimalPrecision,
                    onRecordsClick = onRecordsClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}