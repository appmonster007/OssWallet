package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.osscabinet.ossWallet.ui.components.CardReorderableList
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.osscabinet.ossWallet.ui.components.GenericAddCardDialog
import com.osscabinet.ossWallet.ui.components.CardTypeOptionData
import java.math.BigDecimal
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.TimelineTrajectoryCardConfig
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesSummaryCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PeriodComparisonCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowTrendCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowProgressCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BalanceForecastCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesBookCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowTableCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsTimelineCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BalanceByAccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BalanceTrendCardConfig
import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownCard
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapCard
import com.osscabinet.ossWallet.ui.analytics.components.TimelineTrajectoryCard
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesSummaryCard
import com.osscabinet.ossWallet.ui.analytics.components.PeriodComparisonCard
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTrendCard
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowProgressCard
import com.osscabinet.ossWallet.ui.analytics.components.BalanceForecastCard
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsCard
import com.osscabinet.ossWallet.ui.components.cards.UnifiedCardRenderer
import com.osscabinet.ossWallet.ui.components.cards.CardRenderData
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val decimalPrecision by viewModel.decimalPrecision.collectAsStateWithLifecycle()
    val cardsConfig by viewModel.analyticsCardsConfig.collectAsStateWithLifecycle()

    var navTriggered by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intelligence", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { navigate { onBackClick() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnalyticsHeroSection(
                uiState.currentBalance,
                uiState.selectedCurrency,
                decimalPrecision,
                modifier = Modifier.padding(24.dp)
            )

            // Analytics Cards from config
            CardReorderableList(
                items = cardsConfig.cards.sortedBy { it.order },
                key = { card ->
                    when (card) {
                        is com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.TimelineTrajectoryCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesSummaryCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.PeriodComparisonCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.CashFlowTrendCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.CashFlowProgressCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.BalanceForecastCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesBookCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.CashFlowTableCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsTimelineCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.BalanceByAccountsCardConfig -> "${card.cardId}_${card.timelineRange}"
                        is com.osscabinet.ossWallet.domain.model.settings.BalanceTrendCardConfig -> "${card.cardId}_${card.timelineRange}"
                        else -> card.cardId
                    }
                },
                onReorder = { reorderedCards ->
                    coroutineScope.launch { viewModel.reorderCards(reorderedCards) }
                },
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) { cardConfig, isDragging, dragModifier ->
                val cardWithCallbacks = when (cardConfig) {
                    is CategoryBreakdownCardConfig -> {
                        val cardData by produceState<List<com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData>?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is TimelineTrajectoryCardConfig -> {
                        val cardData by produceState<List<com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData>?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is CategoryExpenseTreemapCardConfig -> {
                        val cardData by produceState<List<com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData>?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is IncomeExpensesSummaryCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesSummaryData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesSummaryData(
                                    totalAmount = BigDecimal.ZERO,
                                    percentageChange = "0%",
                                    currency = uiState.selectedCurrency,
                                    precision = decimalPrecision,
                                    categories = emptyList<com.osscabinet.ossWallet.ui.analytics.components.CategoryItem>()
                                )
                            ),
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is PeriodComparisonCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.PeriodComparisonData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.PeriodComparisonData(
                                    currentValue = BigDecimal.ZERO,
                                    currency = uiState.selectedCurrency,
                                    precision = decimalPrecision,
                                    chartData = emptyList<Double>()
                                )
                            ),
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is CashFlowTrendCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.CashFlowTrendData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.CashFlowTrendData(
                                    barData = emptyList<Double>(),
                                    lineData = emptyList<Double>()
                                )
                            ),
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is CashFlowProgressCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.CashFlowProgressData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.CashFlowProgressData(
                                    income = BigDecimal.ZERO,
                                    expenses = BigDecimal.ZERO,
                                    currency = uiState.selectedCurrency,
                                    precision = decimalPrecision
                                )
                            ),
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is BalanceForecastCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.BalanceForecastData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.BalanceForecastData(
                                    forecastBalance = BigDecimal.ZERO,
                                    currency = uiState.selectedCurrency,
                                    precision = decimalPrecision,
                                    chartData = emptyList<Triple<Double, Double, Double>>()
                                )
                            ),
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is PlannedPaymentsCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsData(
                                    total = BigDecimal.ZERO,
                                    currency = uiState.selectedCurrency,
                                    precision = decimalPrecision,
                                    payments = emptyList<com.osscabinet.ossWallet.ui.analytics.components.PaymentItem>()
                                )
                            ),
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is IncomeExpensesBookCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesBookData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesBookData(
                                    incomeTotal = BigDecimal.ZERO,
                                    expenseTotal = BigDecimal.ZERO,
                                    incomeCount = 0,
                                    expenseCount = 0,
                                    currency = uiState.selectedCurrency,
                                    precision = decimalPrecision
                                )
                            ),
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is CashFlowTableCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.CashFlowTableData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.CashFlowTableData(
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
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is PlannedPaymentsTimelineCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsTimelineData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsTimelineData(
                                    total = BigDecimal.ZERO,
                                    currency = uiState.selectedCurrency,
                                    precision = decimalPrecision,
                                    weeklyData = emptyList<Pair<String, java.math.BigDecimal>>()
                                )
                            ),
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is BalanceByAccountsCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.BalanceByAccountsData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.BalanceByAccountsData(
                                    totalBalance = BigDecimal.ZERO,
                                    currency = uiState.selectedCurrency,
                                    precision = decimalPrecision,
                                    accountBalances = emptyList<com.osscabinet.ossWallet.ui.analytics.components.AccountBalanceItem>()
                                )
                            ),
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    is BalanceTrendCardConfig -> {
                        val cardData by produceState<com.osscabinet.ossWallet.ui.analytics.components.BalanceTrendData?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = uiState) {
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
                                data = cardData ?: com.osscabinet.ossWallet.ui.analytics.components.BalanceTrendData(
                                    currentBalance = BigDecimal.ZERO,
                                    percentageChange = "0%",
                                    currency = uiState.selectedCurrency,
                                    precision = decimalPrecision,
                                    weeklyBalances = emptyList<Double>()
                                )
                            ),
                            callbacks = CardCallbacks.AnalyticsWithTimeline(
                                onTimelineRangeChange = { cardId, range -> coroutineScope.launch { viewModel.updateCardTimelineRange(cardId, range) } },
                                onUpdate = { coroutineScope.launch { viewModel.updateCardConfigById(cardConfig.cardId) { it } } },
                                onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                            )
                        )
                    }
                    else -> null
                }
                
                cardWithCallbacks?.let {
                    UnifiedCardRenderer(
                        renderData = it.renderData,
                        callbacks = it.callbacks
                    )
                }
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
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
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
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
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
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
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
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS),
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
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS),
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
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)
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
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)
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
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)
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
                            com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesBookCardConfig(
                                cardId = "analytics_income_expenses_book_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "INCOME & EXPENSES BOOK",
                                subtitle = "Want to see where your money goes?",
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS),
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
                            com.osscabinet.ossWallet.domain.model.settings.CashFlowTableCardConfig(
                                cardId = "analytics_cash_flow_table_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "CASH FLOW TABLE",
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)
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
                            com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsTimelineCardConfig(
                                cardId = "analytics_planned_payments_timeline_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "PLANNED PAYMENTS TIMELINE",
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)
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
                            com.osscabinet.ossWallet.domain.model.settings.BalanceByAccountsCardConfig(
                                cardId = "analytics_balance_by_accounts_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "BALANCE BY ACCOUNTS",
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime
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
                            com.osscabinet.ossWallet.domain.model.settings.BalanceTrendCardConfig(
                                cardId = "analytics_balance_trend_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "BALANCE TREND",
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)
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

@Composable
fun AnalyticsHeroSection(balance: BigDecimal, currency: String, precision: Int = 2, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        AeonFlowSectionHeader(title = "PROJECTED BALANCE")
        Text(
            formatCurrencyRight(balance, currency, precision),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", color = MaterialTheme.colorScheme.onSurface)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("On track for goal", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InsightSection(currency: String, precision: Int = 2, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("INTELLIGENCE INSIGHT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
            }
            Text(
                "Based on your current trajectory, your net worth will increase by 4.2% by the end of Q4. Consider increasing your 'Travel' budget by ${formatCurrencyRight(BigDecimal("400.0"), currency, precision)} for your upcoming trip.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
