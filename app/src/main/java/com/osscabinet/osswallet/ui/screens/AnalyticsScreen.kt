package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesSummaryData
import com.osscabinet.ossWallet.ui.analytics.components.PeriodComparisonData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTrendData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowProgressData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceForecastData
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsData
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesBookData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTableData
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsTimelineData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceByAccountsData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceTrendData
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardRenderData
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.viewmodel.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    templateViewModel: TemplateViewModel,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onAccountsViewClick: () -> Unit,
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit,
    onBudgetsClick: () -> Unit,
    onLoansClick: () -> Unit,
    onExchangeRatesClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onAddClick: (Int?, String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val decimalPrecision by viewModel.decimalPrecision.collectAsStateWithLifecycle()
    val cardsConfig by viewModel.analyticsCardsConfig.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val allCategories by transactionViewModel.categories.collectAsStateWithLifecycle()
    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
    val nonArchivedAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    val starredTemplates by templateViewModel.starredTemplates.collectAsStateWithLifecycle()
    val currentProfile by settingsViewModel.currentProfile.collectAsStateWithLifecycle()
    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showAddCardDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentProfile = currentProfile,
                nonArchivedAccounts = nonArchivedAccounts,
                starredTemplates = starredTemplates,
                selectedDestination = "analytics",
                onDashboardClick = { onBackClick() },
                onAccountsViewClick = { onAccountsViewClick() },
                onRecordsClick = { onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.None) },
                onAnalyticsClick = { coroutineScope.launch { drawerState.close() } },
                onBudgetsClick = { onBudgetsClick() },
                onLoansClick = { onLoansClick() },
                onExchangeRatesClick = { onExchangeRatesClick() },
                onTemplatesClick = { onTemplatesClick() },
                onManageAccountsClick = { onManageAccountsClick() },
                onSettingsClick = { onSettingsClick() },
                onAccountClick = { id -> onAccountClick(id) },
                onTemplateClick = { template -> onAddClick(template.transactionType, template.name) },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Intelligence", style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showAddCardDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add card",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Analytics Cards from config
                    CardReorderableList(
                        items = cardsConfig.cards.sortedBy { it.order },
                        key = { it.cardId },
                        onReorder = { reorderedCards ->
                            coroutineScope.launch { viewModel.reorderCards(reorderedCards) }
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) { cardConfig, isDragging, dragModifier ->
                        val cardWithCallbacks = when (cardConfig) {
                            is CategoryBreakdownCardConfig -> {
                                val cardData by produceState<List<com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData>?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeCategoryBreakdownForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.CategoryBreakdown(
                                        config = cardConfig,
                                        data = cardData ?: emptyList()
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is TimelineTrajectoryCardConfig -> {
                                val cardData by produceState<List<com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData>?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeTimelineTrajectoryForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        accounts = uiState.accounts,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.TimelineTrajectory(
                                        config = cardConfig,
                                        data = cardData ?: emptyList()
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is CategoryExpenseTreemapCardConfig -> {
                                val cardData by produceState<List<com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData>?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeCategoryExpenseTreemapForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.CategoryExpenseTreemap(
                                        config = cardConfig,
                                        data = cardData ?: emptyList()
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is IncomeExpensesSummaryCardConfig -> {
                                val cardData by produceState<IncomeExpensesSummaryData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeIncomeExpensesSummaryForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.IncomeExpensesSummary(
                                        config = cardConfig,
                                        data = cardData ?: IncomeExpensesSummaryData(
                                            totalAmount = BigDecimal.ZERO,
                                            percentageChange = "0%",
                                            currency = uiState.selectedCurrency,
                                            precision = decimalPrecision,
                                            categories = emptyList()
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is PeriodComparisonCardConfig -> {
                                val cardData by produceState<PeriodComparisonData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computePeriodComparisonForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.PeriodComparison(
                                        config = cardConfig,
                                        data = cardData ?: PeriodComparisonData(
                                            currentValue = BigDecimal.ZERO,
                                            currency = uiState.selectedCurrency,
                                            precision = decimalPrecision,
                                            chartData = emptyList()
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is CashFlowTrendCardConfig -> {
                                val cardData by produceState<CashFlowTrendData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeCashFlowTrendForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.CashFlowTrend(
                                        config = cardConfig,
                                        data = cardData ?: CashFlowTrendData(
                                            barData = emptyList(),
                                            lineData = emptyList()
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is CashFlowProgressCardConfig -> {
                                val cardData by produceState<CashFlowProgressData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeCashFlowProgressForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.CashFlowProgress(
                                        config = cardConfig,
                                        data = cardData ?: CashFlowProgressData(
                                            income = BigDecimal.ZERO,
                                            expenses = BigDecimal.ZERO,
                                            currency = uiState.selectedCurrency,
                                            precision = decimalPrecision
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is BalanceForecastCardConfig -> {
                                val cardData by produceState<BalanceForecastData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeBalanceForecastForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        accounts = uiState.accounts,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.BalanceForecast(
                                        config = cardConfig,
                                        data = cardData ?: BalanceForecastData(
                                            forecastBalance = BigDecimal.ZERO,
                                            currency = uiState.selectedCurrency,
                                            precision = decimalPrecision,
                                            chartData = emptyList()
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is PlannedPaymentsCardConfig -> {
                                val cardData by produceState<PlannedPaymentsData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computePlannedPaymentsForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.PlannedPayments(
                                        config = cardConfig,
                                        data = cardData ?: PlannedPaymentsData(
                                            total = BigDecimal.ZERO,
                                            currency = uiState.selectedCurrency,
                                            precision = decimalPrecision,
                                            payments = emptyList()
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is IncomeExpensesBookCardConfig -> {
                                val cardData by produceState<IncomeExpensesBookData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeIncomeExpensesBookForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.IncomeExpensesBook(
                                        config = cardConfig,
                                        data = cardData ?: IncomeExpensesBookData(
                                            incomeTotal = BigDecimal.ZERO,
                                            expenseTotal = BigDecimal.ZERO,
                                            incomeCount = 0,
                                            expenseCount = 0,
                                            currency = uiState.selectedCurrency,
                                            precision = decimalPrecision
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is CashFlowTableCardConfig -> {
                                val cardData by produceState<CashFlowTableData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeCashFlowTableForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.CashFlowTable(
                                        config = cardConfig,
                                        data = cardData ?: CashFlowTableData(
                                            incomeCount = 0,
                                            expenseCount = 0,
                                            incomeAvgDay = BigDecimal.ZERO,
                                            expenseAvgDay = BigDecimal.ZERO,
                                            incomeAvgRecord = BigDecimal.ZERO,
                                            expenseAvgRecord = BigDecimal.ZERO,
                                            incomeTotal = BigDecimal.ZERO,
                                            expenseTotal = BigDecimal.ZERO,
                                            netCashFlow = BigDecimal.ZERO,
                                            currency = uiState.selectedCurrency,
                                            precision = decimalPrecision
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is PlannedPaymentsTimelineCardConfig -> {
                                val cardData by produceState<PlannedPaymentsTimelineData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computePlannedPaymentsTimelineForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.PlannedPaymentsTimeline(
                                        config = cardConfig,
                                        data = cardData ?: PlannedPaymentsTimelineData(
                                            total = BigDecimal.ZERO,
                                            currency = uiState.selectedCurrency,
                                            precision = decimalPrecision,
                                            weeklyData = emptyList()
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is BalanceByAccountsCardConfig -> {
                                val cardData by produceState<BalanceByAccountsData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeBalanceByAccountsForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        accounts = uiState.accounts,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.BalanceByAccounts(
                                        config = cardConfig,
                                        data = cardData ?: BalanceByAccountsData(
                                            totalBalance = BigDecimal.ZERO,
                                            currency = uiState.selectedCurrency,
                                            precision = decimalPrecision,
                                            accountBalances = emptyList()
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            is BalanceTrendCardConfig -> {
                                val cardData by produceState<BalanceTrendData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.filter, key3 = uiState) {
                                    value = viewModel.computeBalanceTrendForCard(
                                        cardConfig = cardConfig,
                                        allTransactions = uiState.allTransactions,
                                        accounts = uiState.accounts,
                                        baseCurrency = uiState.selectedCurrency,
                                        exchangeRates = uiState.exchangeRates,
                                        precision = decimalPrecision
                                    )
                                }
                                CardWithCallbacks(
                                    renderData = CardRenderData.BalanceTrend(
                                        config = cardConfig,
                                        data = cardData ?: BalanceTrendData(
                                            currentBalance = BigDecimal.ZERO,
                                            percentageChange = "0%",
                                            currency = uiState.selectedCurrency,
                                            precision = decimalPrecision,
                                            weeklyBalances = emptyList()
                                        )
                                    ),
                                    callbacks = CardCallbacks.Analytics(
                                        onUpdate = { updatedConfig ->
                                            coroutineScope.launch {
                                                viewModel.updateCardConfigById(cardConfig.cardId) { updatedConfig as CardConfig }
                                            }
                                        },
                                        onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } },
                                        onRecordsClick = onRecordsClick
                                    )
                                )
                            }
                            else -> null
                        }

                        cardWithCallbacks?.let {
                            UnifiedCardRenderer(
                                renderData = it.renderData,
                                callbacks = it.callbacks,
                                headerModifier = dragModifier,
                                accounts = nonArchivedAccounts,
                                allCategories = allCategories,
                                savedFilters = savedFilters
                            )
                        }
                    }
                }

                AeonExpandableFab(
                    onAddClick = { type, name -> onAddClick(type, name) },
                    templates = allTemplates,
                    fabPaddingBottom = 16.dp
                )
            }
        }
    }

    if (showAddCardDialog) {
        val analyticsCardOptions = listOf(
            CardTypeOptionData(
                title = "Category Breakdown",
                description = "View spending by category",
                icon = Icons.Default.PieChart,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            CategoryBreakdownCardConfig(
                                cardId = "analytics_category_breakdown_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "CATEGORY BREAKDOWN",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                                showLabels = true,
                                showLegend = true
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Timeline Trajectory",
                description = "Track balance over time",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            TimelineTrajectoryCardConfig(
                                cardId = "analytics_timeline_trajectory_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "TIMELINE TRAJECTORY",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                                showBalance = true,
                                showExpense = true,
                                showIncome = true
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Category Expenses",
                description = "Expense distribution treemap",
                icon = Icons.Default.BarChart,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            CategoryExpenseTreemapCardConfig(
                                cardId = "analytics_category_expense_treemap_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "CATEGORY EXPENSES",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                                showLabels = true
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Income & Expenses",
                description = "Summary by category",
                icon = Icons.Default.Assessment,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            IncomeExpensesSummaryCardConfig(
                                cardId = "analytics_income_expenses_summary_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "INCOME & EXPENSES",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)),
                                showPercentage = true
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Period Comparison",
                description = "Compare periods over time",
                icon = Icons.Default.ShowChart,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            PeriodComparisonCardConfig(
                                cardId = "analytics_period_comparison_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "PERIOD COMPARISON",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)),
                                selectedTab = "Income"
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Cash Flow Trend",
                description = "Bar and line trend chart",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            CashFlowTrendCardConfig(
                                cardId = "analytics_cash_flow_trend_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "CASH FLOW TREND",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS))
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Cash Flow Progress",
                description = "Income vs expenses progress",
                icon = Icons.Default.PieChart,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            CashFlowProgressCardConfig(
                                cardId = "analytics_cash_flow_progress_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "CASH FLOW",
                                subtitle = "Am I spending less than I make?",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS))
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Balance Forecast",
                description = "Predict future balance",
                icon = Icons.Default.AccountBalanceWallet,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            BalanceForecastCardConfig(
                                cardId = "analytics_balance_forecast_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "BALANCE FORECAST",
                                subtitle = "Will I have enough money?",
                                forecastDays = 30
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Planned Payments",
                description = "Upcoming planned payments",
                icon = Icons.Default.EventNote,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            PlannedPaymentsCardConfig(
                                cardId = "analytics_planned_payments_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "PLANNED PAYMENTS",
                                subtitle = "Planned Payments by Category",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS))
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Income & Expenses Book",
                description = "General summary with share",
                icon = Icons.Default.Book,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            IncomeExpensesBookCardConfig(
                                cardId = "analytics_income_expenses_book_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "INCOME & EXPENSES BOOK",
                                subtitle = "Want to see where your money goes?",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)),
                                showShareButton = true
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Cash Flow Table",
                description = "Statistical analysis table",
                icon = Icons.Default.TableChart,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            CashFlowTableCardConfig(
                                cardId = "analytics_cash_flow_table_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "CASH FLOW TABLE",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS))
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Planned Payments Timeline",
                description = "Weekly bar chart",
                icon = Icons.Default.CalendarMonth,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            PlannedPaymentsTimelineCardConfig(
                                cardId = "analytics_planned_payments_timeline_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "PLANNED PAYMENTS TIMELINE",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS))
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Balance by Accounts",
                description = "Liquidity distribution",
                icon = Icons.Default.AccountBalance,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            BalanceByAccountsCardConfig(
                                cardId = "analytics_balance_by_accounts_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "BALANCE BY ACCOUNTS",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Balance Trend",
                description = "Area chart trend",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            BalanceTrendCardConfig(
                                cardId = "analytics_balance_trend_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "BALANCE TREND",
                                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS))
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            )
        )
        
        GenericAddCardDialog(
            onDismiss = { showAddCardDialog = false },
            cardOptions = analyticsCardOptions
        )
    }
}
