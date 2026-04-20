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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
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
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.settings.*
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
import com.osscabinet.ossWallet.ui.viewmodel.CardManagerViewModel
import com.osscabinet.ossWallet.ui.viewmodel.DashboardCardType
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.AeonExpandableFab
import com.osscabinet.ossWallet.ui.components.CardTypeOptionData
import com.osscabinet.ossWallet.ui.components.GenericAddCardDialog
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.TimelineService
import com.osscabinet.ossWallet.ui.dashboard.components.NetWorthCard
import com.osscabinet.ossWallet.ui.dashboard.components.RecentActivitiesCard
import com.osscabinet.ossWallet.ui.dashboard.components.AccountsCard
import com.osscabinet.ossWallet.ui.dashboard.components.CashFlowCard
import com.osscabinet.ossWallet.ui.dashboard.components.CurrencyBalancesCard
import com.osscabinet.ossWallet.ui.dashboard.components.BudgetsCard
import com.osscabinet.ossWallet.ui.components.NavigationPill
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
    onEditTransactionClick: (Long) -> Unit,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    analyticsViewModel: AnalyticsViewModel,
    templateViewModel: TemplateViewModel,
    dashboardViewModel: DashboardViewModel,
    budgetViewModel: BudgetViewModel,
    cardManagerViewModel: CardManagerViewModel
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
    val cardsConfig by cardManagerViewModel.cardsConfig.collectAsStateWithLifecycle()

    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val budgets by budgetViewModel.budgets.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.theme.collectAsStateWithLifecycle(initialValue = "System")
    
    var showAddCardDialog by remember { mutableStateOf(false) }

    val pageCount = 2
    val pagerState = remember {
        object : PagerState() {
            override val pageCount: Int get() = pageCount
        }
    }
    val tabs = listOf(
        Icons.Rounded.Dashboard to "Glance",
        Icons.Rounded.History to "Recent Activities"
    )

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
                                onEditTransactionClick = { id -> navigate { onEditTransactionClick(id) } },
                                transactionViewModel = transactionViewModel,
                                budgets = budgets,
                                dashboardViewModel = dashboardViewModel,
                                cardManagerViewModel = cardManagerViewModel,
                                cardsConfig = cardsConfig,
                                showAddCardDialog = showAddCardDialog,
                                onShowAddCardDialogChange = { showAddCardDialog = it }
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

        NavigationPill(
            pagerState = pagerState,
            tabs = tabs,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        AeonExpandableFab(
            onAddClick = { type, name -> navigate { onAddClick(type, name) } },
            templates = allTemplates,
            fabPaddingBottom = 24.dp
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardContent(
    innerPadding: PaddingValues,
    pagerState: androidx.compose.foundation.pager.PagerState,
    tabs: List<Pair<ImageVector, String>>,
    state: DashboardUiState.Success,
    currentTheme: String,
    gridColumns: Int,
    decimalPrecision: Int,
    onManageAccountsClick: () -> Unit,
    onAccountsViewClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onRecordsClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    transactionViewModel: TransactionViewModel,
    budgets: List<Budget>,
    dashboardViewModel: DashboardViewModel,
    cardManagerViewModel: CardManagerViewModel,
    cardsConfig: DashboardCardsConfig,
    showAddCardDialog: Boolean,
    onShowAddCardDialogChange: (Boolean) -> Unit
) {
    remember(state.recentTransactions) {
        state.recentTransactions.map { it.transaction }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Header row with Add Card button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Glance",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onShowAddCardDialogChange(true) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add card",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    CardReorderableList(
                        items = cardsConfig.cards.sortedBy { it.order },
                        key = { it.cardId },
                        onReorder = { reorderedCards ->
                            cardManagerViewModel.reorderDashboardCards(reorderedCards)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) { cardConfig, isDragging, dragModifier ->
                        when (cardConfig) {
                            is NetWorthCardConfig -> {
                                val cardData by produceState<Pair<BigDecimal, CashFlowResult>?>(initialValue = null, key1 = cardConfig, key2 = state) {
                                    value = dashboardViewModel.computeNetWorthForCard(
                                        cardConfig = cardConfig,
                                        accounts = state.accounts,
                                        allTransactions = state.allTransactions,
                                        baseCurrency = state.baseCurrency,
                                        exchangeRates = state.exchangeRates,
                                        precision = state.decimalPrecision
                                    )
                                }
                                
                                cardData?.let { (netWorth, cashFlow) ->
                                    val cardUiState = remember(
                                        netWorth,
                                        cashFlow.totalIncomeInBase,
                                        cashFlow.totalExpensesInBase,
                                        state.baseCurrency,
                                        state.decimalPrecision,
                                        cardConfig.fontSize,
                                        cardConfig.timelineRange
                                    ) {
                                        NetWorthUiState.Success(
                                            netWorth = netWorth,
                                            netIncome = cashFlow.totalIncomeInBase,
                                            netExpense = cashFlow.totalExpensesInBase,
                                            baseCurrency = state.baseCurrency,
                                            precision = state.decimalPrecision,
                                            fontSize = cardConfig.fontSize,
                                            timelineRange = cardConfig.timelineRange
                                        )
                                    }
                                    NetWorthCard(
                                        uiState = cardUiState,
                                        cardId = cardConfig.cardId,
                                        fontSize = cardConfig.fontSize,
                                        timelineRange = cardConfig.timelineRange,
                                        onFontSizeChange = { cardId, fontSize -> dashboardViewModel.updateCardNetWorthFontSize(cardId, fontSize) },
                                        onTimelineRangeChange = { cardId, range -> dashboardViewModel.updateCardNetWorthTimelineRange(cardId, range) },
                                        onDelete = { cardManagerViewModel.removeCard(cardConfig.cardId) },
                                        modifier = Modifier.fillMaxWidth(),
                                        headerModifier = dragModifier
                                    )
                                } ?: Box(modifier = Modifier.size(0.dp))
                            }
                            is CurrencyBalancesCardConfig -> {
                                val currencyTotals by produceState<List<CurrencyAggregationItem>?>(initialValue = null, key1 = cardConfig, key2 = state) {
                                    value = dashboardViewModel.computeCurrencyTotalsForCard(
                                        cardConfig = cardConfig,
                                        accounts = state.accounts,
                                        allTransactions = state.allTransactions,
                                        baseCurrency = state.baseCurrency,
                                        exchangeRates = state.exchangeRates,
                                        precision = state.decimalPrecision
                                    )
                                }
                                
                                currencyTotals?.let { totals ->
                                    CurrencyBalancesCard(
                                        currencyTotals = totals,
                                        baseCurrency = state.baseCurrency,
                                        modifier = Modifier.fillMaxWidth(),
                                        headerModifier = dragModifier,
                                        precision = decimalPrecision,
                                        layout = cardConfig.layout,
                                        showBaseConversion = cardConfig.showBaseConversion,
                                        timelineRange = cardConfig.timelineRange,
                                        cardId = cardConfig.cardId,
                                        onLayoutChange = { cardId, layout -> dashboardViewModel.updateCardCurrencyBalancesLayout(cardId, layout) },
                                        onShowBaseConversionChange = { cardId, show -> dashboardViewModel.updateCardCurrencyBalancesShowBaseConversion(cardId, show) },
                                        onTimelineRangeChange = { cardId, range -> dashboardViewModel.updateCardCurrencyBalancesTimelineRange(cardId, range) },
                                        onDelete = { cardManagerViewModel.removeCard(cardConfig.cardId) }
                                    )
                                } ?: Box(modifier = Modifier.size(0.dp))
                            }
                            is CashFlowCardConfig -> {
                                val cashFlowResult by produceState<CashFlowResult?>(initialValue = null, key1 = cardConfig, key2 = state) {
                                    value = dashboardViewModel.computeCashFlowForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = state.allTransactions,
                                        baseCurrency = state.baseCurrency,
                                        exchangeRates = state.exchangeRates,
                                        precision = state.decimalPrecision
                                    )
                                }
                                
                                cashFlowResult?.let { result ->
                                    CashFlowCard(
                                        cashFlowResult = result,
                                        baseCurrency = state.baseCurrency,
                                        currentTheme = currentTheme,
                                        modifier = Modifier.fillMaxWidth(),
                                        headerModifier = dragModifier,
                                        showTitle = true,
                                        precision = decimalPrecision,
                                        showBaseConversion = cardConfig.showBaseConversion,
                                        timelineRange = cardConfig.timelineRange,
                                        cardId = cardConfig.cardId,
                                        onShowBaseConversionChange = { cardId, show -> dashboardViewModel.updateCardCashFlowShowBaseConversion(cardId, show) },
                                        onTimelineRangeChange = { cardId, range -> dashboardViewModel.updateCardCashFlowTimelineRange(cardId, range) },
                                        onDelete = { cardManagerViewModel.removeCard(cardConfig.cardId) }
                                    )
                                } ?: Box(modifier = Modifier.size(0.dp))
                            }
                            is AccountsCardConfig -> {
                                val accountData by produceState<Pair<List<Account>, Map<String, AccountSummaryItem>>?>(initialValue = null, key1 = cardConfig, key2 = state) {
                                    value = dashboardViewModel.computeAccountStatsForCard(
                                        cardConfig = cardConfig,
                                        accounts = state.accounts,
                                        allTransactions = state.allTransactions,
                                        baseCurrency = state.baseCurrency,
                                        exchangeRates = state.exchangeRates,
                                        precision = state.decimalPrecision
                                    )
                                }
                                
                                accountData?.let { (updatedAccounts, accountStats) ->
                                    AccountsCard(
                                        onManageClick = onManageAccountsClick,
                                        onHeaderClick = onAccountsViewClick,
                                        onAccountClick = onAccountClick,
                                        onGridColumnsChange = { cardId, columns -> dashboardViewModel.updateCardAccountsGridColumns(cardId, columns) },
                                        accounts = updatedAccounts,
                                        accountStats = accountStats,
                                        baseCurrency = state.baseCurrency,
                                        currentTheme = currentTheme,
                                        modifier = Modifier.fillMaxWidth(),
                                        gridColumns = cardConfig.gridColumns,
                                        precision = decimalPrecision,
                                        layout = cardConfig.layout,
                                        onLayoutChange = { cardId, layout -> dashboardViewModel.updateCardAccountsLayout(cardId, layout) },
                                        timelineRange = cardConfig.timelineRange,
                                        onTimelineRangeChange = { cardId, range -> dashboardViewModel.updateCardAccountsTimelineRange(cardId, range) },
                                        showAddAccountCard = cardConfig.showAddAccountCard,
                                        onShowAddAccountCardChange = { cardId, show -> dashboardViewModel.updateCardAccountsShowAddAccountCard(cardId, show) },
                                        cardId = cardConfig.cardId,
                                        headerModifier = dragModifier.fillMaxWidth(),
                                        settingsTitle = "Dashboard Settings",
                                        onDelete = { cardManagerViewModel.removeCard(cardConfig.cardId) }
                                    )
                                } ?: Box(modifier = Modifier.size(0.dp))
                            }
                            is BudgetsCardConfig -> {
                                if (budgets.isNotEmpty()) {
                                    BudgetsCard(
                                        onManageClick = onBudgetsClick,
                                        budgets = budgets,
                                        baseCurrency = state.baseCurrency,
                                        modifier = Modifier.fillMaxWidth(),
                                        precision = decimalPrecision,
                                        headerModifier = dragModifier.fillMaxWidth(),
                                        showProgressLabels = cardConfig.showProgressLabels,
                                        cardId = cardConfig.cardId,
                                        onShowProgressLabelsChange = { cardId, show -> dashboardViewModel.updateCardBudgetsShowProgressLabels(cardId, show) },
                                        onDelete = { cardManagerViewModel.removeCard(cardConfig.cardId) }
                                    )
                                }
//                                else {
//                                    Box(modifier = Modifier.size(0.dp))
//                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                val accountMap = remember(state.accounts) { state.accounts.associate { it.id to it.name } }
                val accountCurrencyMap = remember(state.accounts) { state.accounts.associate { it.id to it.currencyCode } }
                val categories by transactionViewModel.categories.collectAsStateWithLifecycle()
                val categoryMap = remember(categories) { categories.associateBy { it.id } }

                RecentActivitiesCard(
                    recentTransactions = state.recentTransactions,
                    accountNamesMap = accountMap,
                    categoryMap = categoryMap,
                    decimalPrecision = state.decimalPrecision,
                    onRecordsClick = onRecordsClick,
                    onTransactionClick = onEditTransactionClick,
                    baseCurrency = state.baseCurrency,
                    exchangeRates = state.exchangeRates,
                    accountCurrencyMap = accountCurrencyMap,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    
    // Add Card Dialog
    if (showAddCardDialog) {
        AddCardDialog(
            onDismiss = { onShowAddCardDialogChange(false) },
            onAddCard = { cardType ->
                cardManagerViewModel.addDashboardCard(DashboardCardType.fromSerialName(cardType) ?: return@AddCardDialog)
                onShowAddCardDialogChange(false)
            }
        )
    }
}

@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onAddCard: (String) -> Unit
) {
    GenericAddCardDialog(
        onDismiss = onDismiss,
        cardOptions = listOf(
            CardTypeOptionData(
                title = "Net Worth",
                description = "Track your total net worth over time",
                icon = Icons.Default.AccountBalance,
                onClick = { onAddCard("net_worth") }
            ),
            CardTypeOptionData(
                title = "Currency Balances",
                description = "View balances across different currencies",
                icon = Icons.Default.AttachMoney,
                onClick = { onAddCard("currency_balances") }
            ),
            CardTypeOptionData(
                title = "Cash Flow",
                description = "Track income and expenses",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                onClick = { onAddCard("cash_flow") }
            ),
            CardTypeOptionData(
                title = "Accounts",
                description = "View your account summaries",
                icon = Icons.Default.AccountBox,
                onClick = { onAddCard("accounts") }
            ),
            CardTypeOptionData(
                title = "Budgets",
                description = "Monitor your budget progress",
                icon = Icons.Default.PieChart,
                onClick = { onAddCard("budgets") }
            )
        )
    )
}