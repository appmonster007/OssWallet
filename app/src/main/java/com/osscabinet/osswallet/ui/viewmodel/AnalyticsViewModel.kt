package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.service.FilterService
import com.osscabinet.ossWallet.domain.service.CardComputationService
import com.osscabinet.ossWallet.domain.service.TransactionsUtilityService
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.domain.util.CachedCardData
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.ui.analytics.components.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val transactionUseCases: TransactionUseCases,
    private val currencyRepository: CurrencyRepository,
    private val settingsDataStore: SettingsDataStore,
    private val cardComputationService: CardComputationService,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    val exchangeRates: StateFlow<Map<String, BigDecimal>> = _uiState
        .map { it.exchangeRates }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private var allTransactions: List<Transaction> = emptyList()
    private var allAccounts: List<Account> = emptyList()
    private var ratesJob: Job? = null
    val decimalPrecision: StateFlow<Int> = settingsDataStore.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)
    private var _currentPrecision: Int = 2

    // Cache for computed card data to avoid recomputation
    private val cardDataCache = mutableMapOf<String, CachedCardData<*>>()
    private val cacheExpirationMinutes = 5L

    /**
     * Get cached data if available and not expired, otherwise compute and cache
     */
    private inline fun <T> getCachedOrCompute(
        cacheKey: String,
        compute: () -> T
    ): T {
        val cached = cardDataCache[cacheKey]
        @Suppress("UNCHECKED_CAST")
        if (cached != null && !cached.isExpired(cacheExpirationMinutes)) {
            return cached.data as T
        }

        val result = compute()
        cardDataCache[cacheKey] = CachedCardData(System.currentTimeMillis(), result)
        return result
    }

    /**
     * Invalidate cache for a specific card
     */
    fun invalidateCardCache(cardId: String) {
        cardDataCache.keys.removeIf { it.startsWith(cardId) }
    }

    /**
     * Invalidate all cache (called when data changes significantly)
     */
    fun invalidateAllCache() {
        cardDataCache.clear()
    }

    // Analytics cards config
    private val _analyticsCardsConfig = MutableStateFlow(defaultAnalyticsCardsConfig())
    val analyticsCardsConfig: StateFlow<AnalyticsCardsConfig> = _analyticsCardsConfig.asStateFlow()

    init {
        // Load analytics cards config from DataStore on initialization
        viewModelScope.launch {
            settingsDataStore.analyticsCardsConfig.collect { configStr ->
                if (configStr.isEmpty()) {
                    _analyticsCardsConfig.value = defaultAnalyticsCardsConfig()
                } else {
                    try {
                        _analyticsCardsConfig.value = Json.decodeFromString<AnalyticsCardsConfig>(configStr)
                    } catch (e: Exception) {
                        _analyticsCardsConfig.value = defaultAnalyticsCardsConfig()
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            settingsDataStore.currency.collect { currencyCode ->
                onCurrencySelected(currencyCode)
            }
        }

        viewModelScope.launch {
            decimalPrecision.collect { precision ->
                _currentPrecision = precision
                calculateAnalytics()
            }
        }

        viewModelScope.launch {
            transactionUseCases.getTransactions().collect { transactions ->
                allTransactions = transactions
                _uiState.update { it.copy(allTransactions = transactions) }
                calculateAnalytics()
                invalidateAllCache()
            }
        }

        viewModelScope.launch {
            settingsDataStore.currentProfileId
                .flatMapLatest { profileId -> accountRepository.getAllAccounts(profileId) }
                .collect { accounts ->
                    allAccounts = accounts
                    _uiState.update { it.copy(accounts = accounts) }
                    calculateAnalytics()
                    invalidateAllCache()
                }
        }
    }

    fun onCurrencySelected(currencyCode: String) {
        if (_uiState.value.selectedCurrency == currencyCode && _uiState.value.exchangeRates.isNotEmpty()) {
            return
        }
        ratesJob?.cancel()
        ratesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasLoadedExchangeRates = false) }
            val rates = currencyRepository.getExchangeRates(currencyCode).mapValues { BigDecimal.valueOf(it.value) }
            _uiState.update { it.copy(
                selectedCurrency = currencyCode,
                exchangeRates = rates,
                isLoading = false,
                hasLoadedExchangeRates = true
            ) }
            calculateAnalytics()
        }
    }

    private fun getComputationContext() = ComputationContext(
        baseCurrency = _uiState.value.selectedCurrency,
        exchangeRates = _uiState.value.exchangeRates,
        precision = _currentPrecision
    )

    // Delegation to CardComputationService with ComputationContext
    suspend fun computeCategoryBreakdownForCard(
        cardConfig: CategoryBreakdownCardConfig,
        allTransactions: List<Transaction>
    ) = cardComputationService.computeCategoryBreakdown(
        config = cardConfig,
        allTransactions = allTransactions,
        context = getComputationContext()
    )

    suspend fun computeTimelineTrajectoryForCard(
        cardConfig: TimelineTrajectoryCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>
    ) = cardComputationService.computeTimelineTrajectory(
        config = cardConfig,
        allTransactions = allTransactions,
        accounts = accounts,
        context = getComputationContext()
    )

    suspend fun computeCategoryExpenseTreemapForCard(
        cardConfig: CategoryExpenseTreemapCardConfig,
        allTransactions: List<Transaction>
    ) = cardComputationService.computeCategoryExpenseTreemap(
        config = cardConfig,
        allTransactions = allTransactions,
        context = getComputationContext()
    )

    suspend fun computeIncomeExpensesSummaryForCard(
        cardConfig: IncomeExpensesSummaryCardConfig,
        allTransactions: List<Transaction>
    ) = cardComputationService.computeIncomeExpensesSummary(
        config = cardConfig,
        allTransactions = allTransactions,
        context = getComputationContext()
    )

    suspend fun computePeriodComparisonForCard(
        cardConfig: PeriodComparisonCardConfig,
        allTransactions: List<Transaction>
    ) = cardComputationService.computePeriodComparison(
        config = cardConfig,
        allTransactions = allTransactions,
        context = getComputationContext()
    )

    suspend fun computeCashFlowTrendForCard(
        cardConfig: CashFlowTrendCardConfig,
        allTransactions: List<Transaction>
    ) = cardComputationService.computeCashFlowTrend(
        config = cardConfig,
        allTransactions = allTransactions,
        context = getComputationContext()
    )

    suspend fun computeCashFlowProgressForCard(
        cardConfig: CashFlowProgressCardConfig,
        allTransactions: List<Transaction>
    ) = cardComputationService.computeCashFlowProgress(
        config = cardConfig,
        allTransactions = allTransactions,
        context = getComputationContext()
    )

    suspend fun computeBalanceForecastForCard(
        cardConfig: BalanceForecastCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>
    ) = cardComputationService.computeBalanceForecast(
        config = cardConfig,
        allTransactions = allTransactions,
        accounts = accounts,
        context = getComputationContext()
    )

    suspend fun computePlannedPaymentsForCard(
        cardConfig: PlannedPaymentsCardConfig,
        allTransactions: List<Transaction>
    ) = cardComputationService.computePlannedPayments(
        config = cardConfig,
        allTransactions = allTransactions,
        context = getComputationContext()
    )

    suspend fun computeIncomeExpensesBookForCard(
        cardConfig: IncomeExpensesBookCardConfig,
        allTransactions: List<Transaction>
    ) = cardComputationService.computeIncomeExpensesBook(
        config = cardConfig,
        allTransactions = allTransactions,
        context = getComputationContext()
    )

    suspend fun computeCashFlowTableForCard(
        cardConfig: CashFlowTableCardConfig,
        allTransactions: List<Transaction>
    ) = cardComputationService.computeCashFlowTable(
        config = cardConfig,
        allTransactions = allTransactions,
        context = getComputationContext()
    )

    suspend fun computePlannedPaymentsTimelineForCard(
        cardConfig: PlannedPaymentsTimelineCardConfig,
        allTransactions: List<Transaction>
    ) = cardComputationService.computePlannedPaymentsTimeline(
        config = cardConfig,
        allTransactions = allTransactions,
        context = getComputationContext()
    )

    suspend fun computeBalanceByAccountsForCard(
        cardConfig: BalanceByAccountsCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>
    ) = cardComputationService.computeBalanceByAccounts(
        config = cardConfig,
        allTransactions = allTransactions,
        accounts = accounts,
        context = getComputationContext()
    )

    suspend fun computeBalanceTrendForCard(
        cardConfig: BalanceTrendCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>
    ) = cardComputationService.computeBalanceTrend(
        config = cardConfig,
        allTransactions = allTransactions,
        accounts = accounts,
        context = getComputationContext()
    )

    // Simplified ViewModel compute methods using cache
    suspend fun computeCategoryBreakdownCard(config: CategoryBreakdownCardConfig): List<CategoryBreakdownData> {
        val cacheKey = "category_breakdown_${config.cardId}_${config.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeCategoryBreakdownForCard(config, allTransactions)
        }
    }

    suspend fun computeTimelineTrajectoryCard(config: TimelineTrajectoryCardConfig): List<TimelineTrajectoryData> {
        val cacheKey = "timeline_trajectory_${config.cardId}_${config.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeTimelineTrajectoryForCard(config, allTransactions, allAccounts)
        }
    }

    suspend fun computeCategoryExpenseTreemapCard(config: CategoryExpenseTreemapCardConfig): List<CategoryExpenseTreemapData> {
        val cacheKey = "category_treemap_${config.cardId}_${config.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeCategoryExpenseTreemapForCard(config, allTransactions)
        }
    }

    suspend fun computeIncomeExpensesSummaryCard(cardConfig: IncomeExpensesSummaryCardConfig): IncomeExpensesSummaryData {
        val cacheKey = "income_expenses_summary_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeIncomeExpensesSummaryForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computePeriodComparisonCard(cardConfig: PeriodComparisonCardConfig): PeriodComparisonData {
        val cacheKey = "period_comparison_${cardConfig.cardId}_${cardConfig.filter.hashCode()}_${cardConfig.selectedTab}"
        return getCachedOrCompute(cacheKey) {
            computePeriodComparisonForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeCashFlowTrendCard(cardConfig: CashFlowTrendCardConfig): CashFlowTrendData {
        val cacheKey = "cash_flow_trend_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeCashFlowTrendForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeCashFlowProgressCard(cardConfig: CashFlowProgressCardConfig): CashFlowProgressData {
        val cacheKey = "cash_flow_progress_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeCashFlowProgressForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeBalanceForecastCard(cardConfig: BalanceForecastCardConfig): BalanceForecastData {
        val cacheKey = "balance_forecast_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeBalanceForecastForCard(cardConfig, allTransactions, allAccounts)
        }
    }

    suspend fun computePlannedPaymentsCard(cardConfig: PlannedPaymentsCardConfig): PlannedPaymentsData {
        val cacheKey = "planned_payments_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computePlannedPaymentsForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeIncomeExpensesBookCard(cardConfig: IncomeExpensesBookCardConfig): IncomeExpensesBookData {
        val cacheKey = "income_expenses_book_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeIncomeExpensesBookForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeCashFlowTableCard(cardConfig: CashFlowTableCardConfig): CashFlowTableData {
        val cacheKey = "cash_flow_table_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeCashFlowTableForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computePlannedPaymentsTimelineCard(cardConfig: PlannedPaymentsTimelineCardConfig): PlannedPaymentsTimelineData {
        val cacheKey = "planned_payments_timeline_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computePlannedPaymentsTimelineForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeBalanceByAccountsCard(cardConfig: BalanceByAccountsCardConfig): BalanceByAccountsData {
        val cacheKey = "balance_by_accounts_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeBalanceByAccountsForCard(cardConfig, allTransactions, allAccounts)
        }
    }

    suspend fun computeBalanceTrendCard(cardConfig: BalanceTrendCardConfig): BalanceTrendData {
        val cacheKey = "balance_trend_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeBalanceTrendForCard(cardConfig, allTransactions, allAccounts)
        }
    }


    private fun calculateAnalytics() {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        // 1. Global Totals using TransactionsUtilityService
        val globalBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = allTransactions,
            exchangeRates = currentRates,
            baseCurrency = selectedCurrency
        )

        val totalIncome = globalBreakdown.totalIncome.setScale(_currentPrecision, RoundingMode.HALF_UP)
        val totalExpense = globalBreakdown.totalExpense.setScale(_currentPrecision, RoundingMode.HALF_UP)
        val currentBalance = globalBreakdown.netFlow.setScale(_currentPrecision, RoundingMode.HALF_UP)
        
        // 2. Monthly Data
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.add(Calendar.MONTH, -1)
        val lastMonth = calendar.get(Calendar.MONTH)
        val lastYear = calendar.get(Calendar.YEAR)
        
        val thisMonthTransactions = allTransactions.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
        
        val lastMonthTransactions = allTransactions.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == lastMonth && cal.get(Calendar.YEAR) == lastYear
        }

        val thisMonthBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            thisMonthTransactions, currentRates, selectedCurrency
        )
        val lastMonthBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            lastMonthTransactions, currentRates, selectedCurrency
        )

        val thisMonthSavingsRate = TransactionsUtilityService.calculateSavingsRate(
            thisMonthBreakdown.totalIncome, thisMonthBreakdown.totalExpense
        )
        val lastMonthSavingsRate = TransactionsUtilityService.calculateSavingsRate(
            lastMonthBreakdown.totalIncome, lastMonthBreakdown.totalExpense
        )
        
        // 3. Burn Rate
        val firstTx = allTransactions.minByOrNull { it.timestamp }
        val daysActive = if (firstTx != null) {
            val diff = System.currentTimeMillis() - firstTx.timestamp
            (diff / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
        } else 1
        
        val monthlyBurnRate = TransactionsUtilityService.calculateBurnRate(
            totalExpense = globalBreakdown.totalExpense,
            daysActive = daysActive
        )

        // 4. Daily Net Flow (Last 7 days)
        val dailyNetFlow = TransactionsUtilityService.calculateDailyNetFlow(
            transactions = allTransactions,
            days = 7,
            exchangeRates = currentRates,
            baseCurrency = selectedCurrency
        )

        // 5. Forecast (Simple Projection)
        val avgDailyNetFlow = if (dailyNetFlow.isNotEmpty()) {
            dailyNetFlow.values.reduce { acc, bd -> acc.add(bd) }
                .divide(BigDecimal.valueOf(dailyNetFlow.size.toLong()), 10, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val forecast = (1..30).map { day ->
            val timestamp = System.currentTimeMillis() + day * 24 * 60 * 60 * 1000L
            val projectedBalance = currentBalance.add(avgDailyNetFlow.multiply(BigDecimal.valueOf(day.toLong())))
            timestamp to projectedBalance.setScale(_currentPrecision, RoundingMode.HALF_UP)
        }.toMap()

        _uiState.update { 
            it.copy(
                currentBalance = currentBalance,
                savingsRate = thisMonthSavingsRate,
                lastMonthSavingsRate = lastMonthSavingsRate,
                burnRate = monthlyBurnRate,
                thisMonthSpending = thisMonthBreakdown.totalExpense,
                lastMonthSpending = lastMonthBreakdown.totalExpense,
                dailyNetFlow = dailyNetFlow,
                forecast = forecast.toMap(),
                decimalPrecision = _currentPrecision,
                isLoading = false
            )
        }
    }

    // Card management methods
    suspend fun addCard(cardConfig: CardConfig) {
        _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
            cards = _analyticsCardsConfig.value.cards + cardConfig
        )
        settingsDataStore.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
    }

    suspend fun removeCard(cardId: String) {
        _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
            cards = _analyticsCardsConfig.value.cards.filter { it.cardId != cardId }
        )
        settingsDataStore.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
    }
    
    suspend fun updateCardTimelineRange(cardId: String, newTimelineRange: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange) {
        _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
            cards = _analyticsCardsConfig.value.cards.map { card ->
                if (card.cardId == cardId) {
                    when (card) {
                        is CategoryBreakdownCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is TimelineTrajectoryCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is CategoryExpenseTreemapCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is IncomeExpensesSummaryCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is PeriodComparisonCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is CashFlowTrendCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is CashFlowProgressCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is BalanceForecastCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is PlannedPaymentsCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is IncomeExpensesBookCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is CashFlowTableCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is PlannedPaymentsTimelineCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is BalanceByAccountsCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is BalanceTrendCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        else -> card
                    }
                } else {
                    card
                }
            }
        )
        settingsDataStore.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
    }

    suspend fun updateCardConfigById(cardId: String, update: (CardConfig) -> CardConfig) {
        _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
            cards = _analyticsCardsConfig.value.cards.map { card ->
                if (card.cardId == cardId) update(card) else card
            }
        )
        settingsDataStore.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
    }

    suspend fun reorderCards(newOrder: List<CardConfig>) {
        _analyticsCardsConfig.value = AnalyticsCardsConfig(newOrder)
        settingsDataStore.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
    }
}

fun defaultAnalyticsCardsConfig(): AnalyticsCardsConfig {
    return AnalyticsCardsConfig(
        cards = listOf(
            CategoryBreakdownCardConfig(
                cardId = "analytics_category_breakdown",
                order = 0,
                title = "CATEGORY BREAKDOWN",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                showLabels = true,
                showLegend = true
            ),
            TimelineTrajectoryCardConfig(
                cardId = "analytics_timeline_trajectory",
                order = 1,
                title = "TIMELINE TRAJECTORY",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                showBalance = true,
                showExpense = true,
                showIncome = true
            ),
            CategoryExpenseTreemapCardConfig(
                cardId = "analytics_category_expense_treemap",
                order = 2,
                title = "CATEGORY EXPENSES",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                showLabels = true
            ),
            IncomeExpensesSummaryCardConfig(
                cardId = "analytics_income_expenses_summary",
                order = 3,
                title = "INCOME & EXPENSES",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)),
                showPercentage = true
            ),
            PeriodComparisonCardConfig(
                cardId = "analytics_period_comparison",
                order = 4,
                title = "PERIOD COMPARISON",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)),
                selectedTab = "Income"
            ),
            CashFlowTrendCardConfig(
                cardId = "analytics_cash_flow_trend",
                order = 5,
                title = "CASH FLOW TREND",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS))
            ),
            CashFlowProgressCardConfig(
                cardId = "analytics_cash_flow_progress",
                order = 6,
                title = "CASH FLOW",
                subtitle = "Am I spending less than I make?",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS))
            ),
            BalanceForecastCardConfig(
                cardId = "analytics_balance_forecast",
                order = 7,
                title = "BALANCE FORECAST",
                subtitle = "Will I have enough money?",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
                forecastDays = 30
            ),
            PlannedPaymentsCardConfig(
                cardId = "analytics_planned_payments",
                order = 8,
                title = "PLANNED PAYMENTS",
                subtitle = "Planned Payments by Category",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS))
            )
        )
    )
}

data class AnalyticsUiState(
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    val savingsRate: Double = 0.0,
    val lastMonthSavingsRate: Double = 0.0,
    val burnRate: BigDecimal = BigDecimal.ZERO,
    val thisMonthSpending: BigDecimal = BigDecimal.ZERO,
    val lastMonthSpending: BigDecimal = BigDecimal.ZERO,
    val dailyNetFlow: Map<Long, BigDecimal> = emptyMap(),
    val forecast: Map<Long, BigDecimal> = emptyMap(),
    val selectedCurrency: String = "USD",
    val exchangeRates: Map<String, BigDecimal> = emptyMap(),
    val decimalPrecision: Int = 2,
    val isLoading: Boolean = false,
    val hasLoadedExchangeRates: Boolean = false,
    val allTransactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList()
)
