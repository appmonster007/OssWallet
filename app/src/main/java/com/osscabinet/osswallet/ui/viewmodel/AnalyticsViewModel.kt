package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Label
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData
import com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesSummaryData
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesBookData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryItem
import com.osscabinet.ossWallet.ui.analytics.components.PeriodComparisonData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTrendData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowProgressData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceForecastData
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsData
import com.osscabinet.ossWallet.ui.analytics.components.PaymentItem
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTableData
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsTimelineData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceByAccountsData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceTrendData
import com.osscabinet.ossWallet.domain.model.settings.AnalyticsCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
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
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.CardComputationService
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.domain.usecase.FinancialUseCases
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.service.CurrencyService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.AccentOrange
import com.osscabinet.ossWallet.ui.theme.AccentPurple
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor

class AnalyticsViewModel(
    private val transactionUseCases: TransactionUseCases,
    private val financialUseCases: FinancialUseCases,
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

    // Generic card data computation functions (pure functions, no StateFlows)
    // These follow the DashboardScreen pattern - screens use remember(cardConfig.cardId, ...) to cache per-card

    // Analytics cards config - using MutableStateFlow for immediate updates (like DashboardScreen)
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
                calculateAnalytics()
                // Invalidate cache when transactions change to force recomputation
                invalidateAllCache()
            }
        }

        viewModelScope.launch {
            settingsDataStore.currentProfileId.collect { profileId ->
                accountRepository.getAllAccounts(profileId).collect { accounts ->
                    allAccounts = accounts
                    calculateAnalytics()
                    // Invalidate cache when accounts change to force recomputation
                    invalidateAllCache()
                }
            }
        }

        // Initialize default analytics cards config if empty
        viewModelScope.launch {
            val currentConfig = settingsDataStore.analyticsCardsConfig.first()
            if (currentConfig.isEmpty()) {
                settingsDataStore.saveAnalyticsCardsConfig(Json.encodeToString(defaultAnalyticsCardsConfig()))
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
            val rates = financialUseCases.getExchangeRates(currencyCode).mapValues { BigDecimal.valueOf(it.value) }
            _uiState.update { it.copy(
                selectedCurrency = currencyCode,
                exchangeRates = rates,
                isLoading = false,
                hasLoadedExchangeRates = true
            ) }
            calculateAnalytics()
        }
    }

    // Generic card data computation functions (pure functions, no StateFlows)
    // These follow the DashboardScreen pattern - screens use LaunchedEffect to call them and remember to cache per-card

    suspend fun computeCategoryBreakdownCard(
        cardConfig: CategoryBreakdownCardConfig
    ): List<CategoryBreakdownData> {
        val cacheKey = "category_breakdown_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) {
            cardComputationService.computeCategoryBreakdown(
                config = cardConfig,
                transactions = allTransactions,
                baseCurrency = _uiState.value.selectedCurrency,
                exchangeRates = _uiState.value.exchangeRates,
                precision = _currentPrecision
            )
        }
    }

    suspend fun computeTimelineTrajectoryCard(
        cardConfig: TimelineTrajectoryCardConfig
    ): List<TimelineTrajectoryData> {
        val cacheKey = "timeline_trajectory_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) {
            cardComputationService.computeTimelineTrajectory(
                config = cardConfig,
                transactions = allTransactions,
                accounts = allAccounts,
                baseCurrency = _uiState.value.selectedCurrency,
                exchangeRates = _uiState.value.exchangeRates,
                precision = _currentPrecision
            )
        }
    }

    suspend fun computeCategoryExpenseTreemapCard(
        cardConfig: CategoryExpenseTreemapCardConfig
    ): List<CategoryExpenseTreemapData> {
        val cacheKey = "category_treemap_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) {
            cardComputationService.computeCategoryExpenseTreemap(
                config = cardConfig,
                transactions = allTransactions,
                baseCurrency = _uiState.value.selectedCurrency,
                exchangeRates = _uiState.value.exchangeRates,
                precision = _currentPrecision
            )
        }
    }

    suspend fun computeIncomeExpensesSummaryCard(
        cardConfig: IncomeExpensesSummaryCardConfig
    ): IncomeExpensesSummaryData {
        val cacheKey = "income_expenses_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) { computeIncomeExpensesSummary(cardConfig) }
    }

    suspend fun computePeriodComparisonCard(
        cardConfig: PeriodComparisonCardConfig
    ): PeriodComparisonData {
        val cacheKey = "period_comparison_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) { computePeriodComparison(cardConfig) }
    }

    suspend fun computeCashFlowTrendCard(
        cardConfig: CashFlowTrendCardConfig
    ): CashFlowTrendData {
        val cacheKey = "cashflow_trend_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) { computeCashFlowTrend(cardConfig) }
    }

    suspend fun computeCashFlowProgressCard(
        cardConfig: CashFlowProgressCardConfig
    ): CashFlowProgressData {
        val cacheKey = "cashflow_progress_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) { computeCashFlowProgress(cardConfig) }
    }

    suspend fun computeBalanceForecastCard(
        cardConfig: BalanceForecastCardConfig
    ): BalanceForecastData {
        val cacheKey = "balance_forecast_${cardConfig.cardId}_${cardConfig.forecastDays}"
        return getCachedOrCompute(cacheKey) { computeBalanceForecast(cardConfig) }
    }

    suspend fun computePlannedPaymentsCard(
        cardConfig: PlannedPaymentsCardConfig
    ): PlannedPaymentsData {
        val cacheKey = "planned_payments_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) { computePlannedPayments(cardConfig) }
    }

    suspend fun computeIncomeExpensesBookCard(
        cardConfig: IncomeExpensesBookCardConfig
    ): IncomeExpensesBookData {
        val cacheKey = "income_expenses_book_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) { computeIncomeExpensesBook(cardConfig) }
    }

    suspend fun computeCashFlowTableCard(
        cardConfig: CashFlowTableCardConfig
    ): CashFlowTableData {
        val cacheKey = "cash_flow_table_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) { computeCashFlowTable(cardConfig) }
    }

    suspend fun computePlannedPaymentsTimelineCard(
        cardConfig: PlannedPaymentsTimelineCardConfig
    ): PlannedPaymentsTimelineData {
        val cacheKey = "planned_payments_timeline_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) { computePlannedPaymentsTimeline(cardConfig) }
    }

    suspend fun computeBalanceByAccountsCard(
        cardConfig: BalanceByAccountsCardConfig
    ): BalanceByAccountsData {
        val cacheKey = "balance_by_accounts_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) { computeBalanceByAccounts(cardConfig) }
    }

    suspend fun computeBalanceTrendCard(
        cardConfig: BalanceTrendCardConfig
    ): BalanceTrendData {
        val cacheKey = "balance_trend_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return getCachedOrCompute(cacheKey) { computeBalanceTrend(cardConfig) }
    }

    private fun calculateAnalytics() {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        val convertedTransactions = allTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            tx.copy(amount = convertedAmount)
        }

        val totalIncome = convertedTransactions.filter { it.amount.signum() >= 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.setScale(_currentPrecision, RoundingMode.HALF_UP)
        val totalExpense = convertedTransactions.filter { it.amount.signum() < 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.setScale(_currentPrecision, RoundingMode.HALF_UP)
        val currentBalance = totalIncome.add(totalExpense).setScale(_currentPrecision, RoundingMode.HALF_UP)
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.add(Calendar.MONTH, -1)
        val lastMonth = calendar.get(Calendar.MONTH)
        val lastYear = calendar.get(Calendar.YEAR)
        
        val thisMonthTransactions = convertedTransactions.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
        
        val lastMonthTransactions = convertedTransactions.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == lastMonth && cal.get(Calendar.YEAR) == lastYear
        }

        val thisMonthIncome = thisMonthTransactions.filter { it.amount.signum() >= 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val thisMonthExpense = thisMonthTransactions.filter { it.amount.signum() < 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val thisMonthSavingsRate = if (thisMonthIncome.signum() > 0) {
            thisMonthIncome.add(thisMonthExpense).divide(thisMonthIncome, 4, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        val lastMonthIncome = lastMonthTransactions.filter { it.amount.signum() >= 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val lastMonthExpense = lastMonthTransactions.filter { it.amount.signum() < 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val lastMonthSavingsRate = if (lastMonthIncome.signum() > 0) {
            lastMonthIncome.add(lastMonthExpense).divide(lastMonthIncome, 4, RoundingMode.HALF_UP).toDouble()
        } else 0.0
        
        val firstTx = convertedTransactions.minByOrNull { it.timestamp }
        val daysActive = if (firstTx != null) {
            val diff = System.currentTimeMillis() - firstTx.timestamp
            (diff / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
        } else 1
        val dailyExpenseAvg = totalExpense.divide(BigDecimal.valueOf(daysActive), _currentPrecision, RoundingMode.HALF_UP)
        val monthlyBurnRate = dailyExpenseAvg.multiply(BigDecimal.valueOf(30))

        val dailyNetFlow = convertedTransactions
            .filter { it.timestamp > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L }
            .groupBy { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }.mapValues { entry ->
                entry.value.fold(BigDecimal.ZERO) { acc, tx ->
                    acc.add(tx.amount)
                }
            }

        val avgDailyNetFlow = if (dailyNetFlow.isNotEmpty()) {
            dailyNetFlow.values.fold(BigDecimal.ZERO) { acc, bd -> acc.add(bd) }
                .divide(BigDecimal.valueOf(dailyNetFlow.size.toLong()), _currentPrecision, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val forecast = (1..30).map { day ->
            val timestamp = System.currentTimeMillis() + day * 24 * 60 * 60 * 1000L
            val projectedBalance = currentBalance.add(avgDailyNetFlow.multiply(BigDecimal.valueOf(day.toLong())))
            timestamp to projectedBalance
        }

        _uiState.update { 
            it.copy(
                currentBalance = currentBalance,
                savingsRate = thisMonthSavingsRate,
                lastMonthSavingsRate = lastMonthSavingsRate,
                burnRate = monthlyBurnRate,
                thisMonthSpending = thisMonthExpense,
                lastMonthSpending = lastMonthExpense,
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
    
    suspend fun updateCardTimelineRange(cardId: String, newTimelineRange: TimelineRange) {
        // Update MutableStateFlow directly (like DashboardScreen)
        _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
            cards = _analyticsCardsConfig.value.cards.map { card ->
                if (card.cardId == cardId) {
                    when (card) {
                        is CategoryBreakdownCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is TimelineTrajectoryCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is CategoryExpenseTreemapCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is IncomeExpensesSummaryCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is PeriodComparisonCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is CashFlowTrendCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is CashFlowProgressCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is BalanceForecastCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is PlannedPaymentsCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is IncomeExpensesBookCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is CashFlowTableCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is PlannedPaymentsTimelineCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is BalanceByAccountsCardConfig -> card.copy(timelineRange = newTimelineRange)
                        is BalanceTrendCardConfig -> card.copy(timelineRange = newTimelineRange)
                        else -> card
                    }
                } else {
                    card
                }
            }
        )
        // Save to DataStore for persistence
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

    // Computation methods for new card types
    private fun computeIncomeExpensesSummary(config: IncomeExpensesSummaryCardConfig): IncomeExpensesSummaryData {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        // Filter transactions based on timeline range
        val filteredTransactions = filterTransactionsByTimeline(allTransactions, config.timelineRange)
        
        // Convert to base currency
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        // Calculate totals by category for expenses
        val expenseCategoryTotals = convertedTransactions
            .filter { it.amount.signum() < 0 } // Only expenses
            .groupBy { it.category ?: "Uncategorized" }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
        
        // Calculate totals by category for income
        val incomeCategoryTotals = convertedTransactions
            .filter { it.amount.signum() >= 0 } // Only income
            .groupBy { it.category ?: "Uncategorized" }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
        
        // Calculate total amount (net cash flow)
        val totalIncome = incomeCategoryTotals.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        val totalExpenses = expenseCategoryTotals.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        val totalAmount = totalIncome.add(totalExpenses)
        
        // Calculate percentage change (compare with previous period)
        val now = System.currentTimeMillis()
        val daysInPeriod = when (config.timelineRange) {
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                when (config.timelineRange.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 7L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 30L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 84L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_6_MONTHS -> 180L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_1_YEAR -> 365L
                    else -> 30L
                }
            }
            else -> 30L
        }
        
        val currentPeriodEnd = now
        val currentPeriodStart = now - daysInPeriod * 24 * 60 * 60 * 1000L
        val previousPeriodStart = currentPeriodStart - daysInPeriod * 24 * 60 * 60 * 1000L
        
        val previousPeriodTransactions = allTransactions.filter { 
            it.timestamp >= previousPeriodStart && it.timestamp < currentPeriodStart 
        }
        
        val previousPeriodNetFlow = previousPeriodTransactions.fold(BigDecimal.ZERO) { acc, tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            acc.add(convertedAmount)
        }
        
        val percentageChange = if (previousPeriodNetFlow != BigDecimal.ZERO) {
            val change = ((totalAmount - previousPeriodNetFlow).toDouble() / Math.abs(previousPeriodNetFlow.toDouble())) * 100
            String.format("%.1f%%", change)
        } else {
            "0%"
        }
        
        // Create category items with icons and colors based on actual data
        val categoryIcons = mapOf(
            "Food & Drinks" to Icons.Default.Restaurant,
            "Shopping" to Icons.Default.ShoppingCart,
            "Housing" to Icons.Default.Home,
            "Transportation" to Icons.Default.DirectionsCar,
            "Salary" to Icons.Default.AttachMoney,
            "Investments" to Icons.Default.TrendingUp,
            "Utilities" to Icons.Default.ElectricBolt,
            "Entertainment" to Icons.Default.Movie,
            "Health" to Icons.Default.LocalHospital,
            "Education" to Icons.Default.School
        )
        
        val categoryColors = listOf(
            Color(0xFFFF5722),
            AccentBlue,
            AccentOrange,
            Color(0xFF607D8B),
            AccentPurple,
            Color(0xFF4CAF50),
            Color(0xFFE91E63),
            Color(0xFF9C27B0),
            Color(0xFF00BCD4),
            Color(0xFFFFC107)
        )
        
        val sortedCategories = expenseCategoryTotals.entries
            .sortedByDescending { it.value.abs() }
            .take(4)
        
        val categories = sortedCategories.mapIndexed { index, entry ->
            val name = entry.key
            val amount = entry.value
            val icon = categoryIcons[name] ?: Icons.Default.Label
            val color = categoryColors.getOrElse(index) { AccentPurple }
            
            // Calculate category change (simplified - would need historical data per category)
            val change = "+0%"
            
            CategoryItem(
                name = name,
                amount = amount,
                change = change,
                color = color,
                icon = icon
            )
        }
        
        return IncomeExpensesSummaryData(
            totalAmount = totalAmount,
            percentageChange = percentageChange,
            currency = selectedCurrency,
            precision = _currentPrecision,
            categories = categories
        )
    }
    
    private fun computePeriodComparison(config: PeriodComparisonCardConfig): PeriodComparisonData {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        val filteredTransactions = filterTransactionsByTimeline(allTransactions, config.timelineRange)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        // Calculate current value based on selected tab
        val currentValue = when (config.selectedTab) {
            "Income" -> convertedTransactions.filter { it.amount.signum() >= 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            "Expenses" -> convertedTransactions.filter { it.amount.signum() < 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            else -> convertedTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        }
        
        // Generate chart data (weekly breakdown based on timeline)
        val calendar = java.util.Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        val numPeriods = when (config.timelineRange) {
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                when (config.timelineRange.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 7
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 4
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 12
                    else -> 12
                }
            }
            else -> 12
        }
        
        val periodLength = when (config.timelineRange) {
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                when (config.timelineRange.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 24 * 60 * 60 * 1000L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 7 * 24 * 60 * 60 * 1000L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 7 * 24 * 60 * 60 * 1000L
                    else -> 7 * 24 * 60 * 60 * 1000L
                }
            }
            else -> 7 * 24 * 60 * 60 * 1000L
        }
        
        val chartData = (0 until numPeriods).map { periodIndex ->
            val periodEnd = now - (periodIndex * periodLength)
            val periodStart = periodEnd - periodLength
            
            val periodTransactions = convertedTransactions.filter { 
                it.timestamp >= periodStart && it.timestamp < periodEnd 
            }
            
            when (config.selectedTab) {
                "Income" -> periodTransactions.filter { it.amount.signum() >= 0 }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.toDouble()
                "Expenses" -> periodTransactions.filter { it.amount.signum() < 0 }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.toDouble()
                else -> periodTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.toDouble()
            }
        }.reversed()
        
        return PeriodComparisonData(
            currentValue = currentValue,
            currency = selectedCurrency,
            precision = _currentPrecision,
            chartData = chartData
        )
    }
    
    private fun computeCashFlowTrend(config: CashFlowTrendCardConfig): CashFlowTrendData {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        val filteredTransactions = filterTransactionsByTimeline(allTransactions, config.timelineRange)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        // Group transactions by time period (weekly for better visualization)
        val calendar = java.util.Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        // Determine number of periods based on timeline range
        val numPeriods = when (config.timelineRange) {
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                when (config.timelineRange.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 7
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 4
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 12
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_6_MONTHS -> 6
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_1_YEAR -> 12
                    else -> 12
                }
            }
            else -> 12
        }
        
        // Group by period and calculate net cash flow
        val periodData = (0 until numPeriods).map { periodIndex ->
            // Calculate time range for this period
            val periodLength = when (config.timelineRange) {
                is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                    when (config.timelineRange.period) {
                        com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 24 * 60 * 60 * 1000L // 1 day
                        com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 7 * 24 * 60 * 60 * 1000L // 1 week
                        com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 7 * 24 * 60 * 60 * 1000L // 1 week
                        com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_6_MONTHS -> 30 * 24 * 60 * 60 * 1000L // 1 month
                        com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_1_YEAR -> 30 * 24 * 60 * 60 * 1000L // 1 month
                        else -> 7 * 24 * 60 * 60 * 1000L
                    }
                }
                else -> 7 * 24 * 60 * 60 * 1000L
            }
            
            val periodEnd = now - (periodIndex * periodLength)
            val periodStart = periodEnd - periodLength
            
            val periodTransactions = convertedTransactions.filter { 
                it.timestamp >= periodStart && it.timestamp < periodEnd 
            }
            
            val income = periodTransactions.filter { it.amount.signum() >= 0 }
                .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            val expenses = periodTransactions.filter { it.amount.signum() < 0 }
                .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            val netFlow = income.add(expenses)
            
            netFlow.toDouble()
        }
        
        // Bar data = net cash flow per period
        val barData = periodData
        
        // Line data = cumulative balance trend
        val lineData = periodData.runningReduce { acc, value -> acc + value }
        
        return CashFlowTrendData(
            barData = barData,
            lineData = lineData
        )
    }
    
    private fun computeCashFlowProgress(config: CashFlowProgressCardConfig): CashFlowProgressData {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        val filteredTransactions = filterTransactionsByTimeline(allTransactions, config.timelineRange)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val income = convertedTransactions.filter { it.amount.signum() >= 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val expenses = convertedTransactions.filter { it.amount.signum() < 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        
        return CashFlowProgressData(
            income = income,
            expenses = expenses,
            currency = selectedCurrency,
            precision = _currentPrecision
        )
    }
    
    private fun computeBalanceForecast(config: BalanceForecastCardConfig): BalanceForecastData {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        // Filter transactions based on timeline range to calculate historical trends
        val filteredTransactions = filterTransactionsByTimeline(allTransactions, config.timelineRange)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        // Calculate average daily net flow from historical data
        val calendar = java.util.Calendar.getInstance()
        val now = System.currentTimeMillis()
        val daysInPeriod = when (config.timelineRange) {
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                when (config.timelineRange.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 7L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 30L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 84L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_6_MONTHS -> 180L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_1_YEAR -> 365L
                    else -> 30L
                }
            }
            else -> 30L
        }
        
        val totalNetFlow = convertedTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val avgDailyNetFlow = if (daysInPeriod > 0) {
            totalNetFlow.divide(BigDecimal.valueOf(daysInPeriod), _currentPrecision, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        
        // Get current balance from all transactions (not just filtered)
        val currentBalance = allTransactions.fold(BigDecimal.ZERO) { acc, tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            acc.add(convertedAmount)
        }
        
        // Calculate forecast
        val forecastBalance = currentBalance.add(avgDailyNetFlow.multiply(BigDecimal.valueOf(config.forecastDays.toLong())))
        
        // Generate chart data with historical and forecast data
        val chartData = (0 until 5).map { periodIndex ->
            val daysFromNow = periodIndex * (config.forecastDays / 4)
            val projectedBalance = currentBalance.add(avgDailyNetFlow.multiply(BigDecimal.valueOf(daysFromNow.toLong())))
            
            // Create optimistic, realistic, and pessimistic projections
            val optimistic = projectedBalance.multiply(BigDecimal.valueOf(1.1))
            val realistic = projectedBalance
            val pessimistic = projectedBalance.multiply(BigDecimal.valueOf(0.9))
            
            Triple(optimistic.toDouble(), realistic.toDouble(), pessimistic.toDouble())
        }
        
        return BalanceForecastData(
            forecastBalance = forecastBalance,
            currency = selectedCurrency,
            precision = _currentPrecision,
            chartData = chartData
        )
    }
    
    private fun computePlannedPayments(config: PlannedPaymentsCardConfig): PlannedPaymentsData {
        val selectedCurrency = _uiState.value.selectedCurrency
        
        // Note: This uses placeholder data. To implement properly, you would need:
        // 1. A data model for planned/recurring transactions
        // 2. Query the database for planned payments within the timeline range
        // 3. Calculate progress based on actual vs planned amounts
        val payments = listOf(
            PaymentItem(
                name = "Financial investments",
                amount = BigDecimal("210000.00"),
                progress = 0.6f,
                color = AccentPurple
            ),
            PaymentItem(
                name = "Transfer, withdraw",
                amount = BigDecimal("105000.00"),
                progress = 0.4f,
                color = PositiveAmountColor
            ),
            PaymentItem(
                name = "Rent",
                amount = BigDecimal("45675.00"),
                progress = 0.8f,
                color = AccentOrange
            )
        )
        
        val total = payments.fold(BigDecimal.ZERO) { acc, payment -> acc.add(payment.amount) }
        
        return PlannedPaymentsData(
            total = total,
            currency = selectedCurrency,
            precision = _currentPrecision,
            payments = payments
        )
    }

    private fun computeIncomeExpensesBook(config: IncomeExpensesBookCardConfig): IncomeExpensesBookData {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        val filteredTransactions = filterTransactionsByTimeline(allTransactions, config.timelineRange)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val incomeTransactions = convertedTransactions.filter { it.amount.signum() >= 0 }
        val expenseTransactions = convertedTransactions.filter { it.amount.signum() < 0 }
        
        val totalIncome = incomeTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val totalExpense = expenseTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        
        return IncomeExpensesBookData(
            incomeTotal = totalIncome,
            expenseTotal = totalExpense,
            incomeCount = incomeTransactions.size,
            expenseCount = expenseTransactions.size,
            currency = selectedCurrency,
            precision = _currentPrecision
        )
    }
    
    private fun filterTransactionsByTimeline(transactions: List<Transaction>, timelineRange: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange): List<Transaction> {
        val now = System.currentTimeMillis()
        return when (timelineRange) {
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                val days = when (timelineRange.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 7L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 30L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 84L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_6_MONTHS -> 180L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_1_YEAR -> 365L
                    else -> 30L
                }
                val startTime = now - days * 24 * 60 * 60 * 1000L
                transactions.filter { it.timestamp >= startTime }
            }
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.CustomRange -> {
                transactions.filter { it.timestamp >= timelineRange.start && it.timestamp <= timelineRange.end }
            }
            else -> transactions
        }
    }

    private fun computeCashFlowTable(config: CashFlowTableCardConfig): CashFlowTableData {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        val filteredTransactions = filterTransactionsByTimeline(allTransactions, config.timelineRange)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val incomeTransactions = convertedTransactions.filter { it.amount.signum() >= 0 }
        val expenseTransactions = convertedTransactions.filter { it.amount.signum() < 0 }
        
        val incomeCount = incomeTransactions.size
        val expenseCount = expenseTransactions.size
        
        val incomeTotal = incomeTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val expenseTotal = expenseTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        
        val incomeAvgDay = if (incomeCount > 0) incomeTotal.divide(BigDecimal.valueOf(84), _currentPrecision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val expenseAvgDay = if (expenseCount > 0) expenseTotal.divide(BigDecimal.valueOf(84), _currentPrecision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val incomeAvgRecord = if (incomeCount > 0) incomeTotal.divide(BigDecimal.valueOf(incomeCount.toLong()), _currentPrecision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val expenseAvgRecord = if (expenseCount > 0) expenseTotal.divide(BigDecimal.valueOf(expenseCount.toLong()), _currentPrecision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        return CashFlowTableData(
            incomeCount = incomeCount,
            expenseCount = expenseCount,
            incomeAvgDay = incomeAvgDay,
            expenseAvgDay = expenseAvgDay,
            incomeAvgRecord = incomeAvgRecord,
            expenseAvgRecord = expenseAvgRecord,
            incomeTotal = incomeTotal,
            expenseTotal = expenseTotal,
            netCashFlow = incomeTotal.add(expenseTotal),
            currency = selectedCurrency,
            precision = _currentPrecision
        )
    }

    private fun computePlannedPaymentsTimeline(config: PlannedPaymentsTimelineCardConfig): PlannedPaymentsTimelineData {
        val selectedCurrency = _uiState.value.selectedCurrency
        
        // Note: This uses placeholder data. To implement properly, you would need:
        // 1. A data model for planned/recurring transactions
        // 2. Query the database for planned payments within the timeline range
        // 3. Group payments by week based on due dates
        val weeklyData = listOf(
            "Week 17" to BigDecimal("50000.00"),
            "Week 18" to BigDecimal("75000.00"),
            "Week 19" to BigDecimal("40000.00"),
            "Week 20" to BigDecimal("60000.00"),
            "Week 21" to BigDecimal("85000.00"),
            "Week 22" to BigDecimal("30000.00"),
            "Week 23" to BigDecimal("20000.00")
        )
        
        val total = weeklyData.fold(BigDecimal.ZERO) { acc, (_, amount) -> acc.add(amount) }
        
        return PlannedPaymentsTimelineData(
            total = total,
            currency = selectedCurrency,
            precision = _currentPrecision,
            weeklyData = weeklyData
        )
    }

    private fun computeBalanceByAccounts(config: BalanceByAccountsCardConfig): BalanceByAccountsData {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        // Calculate current balance by account
        val accountBalances = allAccounts.map { account ->
            val accountTransactions = allTransactions.filter { it.accountId == account.id }
            val balance = accountTransactions.fold(BigDecimal.ZERO) { acc, tx ->
                acc.add(tx.amount)
            }
            com.osscabinet.ossWallet.ui.analytics.components.AccountBalanceItem(
                accountName = account.name,
                balance = balance,
                currency = account.currencyCode,
                isNegative = balance < BigDecimal.ZERO
            )
        }
        
        val totalBalance = accountBalances.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.balance) }
        
        return BalanceByAccountsData(
            totalBalance = totalBalance,
            currency = selectedCurrency,
            precision = _currentPrecision,
            accountBalances = accountBalances
        )
    }

    private fun computeBalanceTrend(config: BalanceTrendCardConfig): BalanceTrendData {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        val filteredTransactions = filterTransactionsByTimeline(allTransactions, config.timelineRange)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        // Calculate balance trend over time based on timeline
        val calendar = java.util.Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        val numPeriods = when (config.timelineRange) {
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                when (config.timelineRange.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 7
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 4
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 12
                    else -> 12
                }
            }
            else -> 12
        }
        
        val periodLength = when (config.timelineRange) {
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                when (config.timelineRange.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 24 * 60 * 60 * 1000L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 7 * 24 * 60 * 60 * 1000L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 7 * 24 * 60 * 60 * 1000L
                    else -> 7 * 24 * 60 * 60 * 1000L
                }
            }
            else -> 7 * 24 * 60 * 60 * 1000L
        }
        
        val weeklyBalances = (0 until numPeriods).map { periodIndex ->
            val periodEnd = now - (periodIndex * periodLength)
            val periodStart = periodEnd - periodLength
            
            val periodTransactions = convertedTransactions.filter { 
                it.timestamp >= periodStart && it.timestamp < periodEnd 
            }
            
            periodTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.toDouble()
        }.reversed()
        
        val currentBalance = weeklyBalances.lastOrNull()?.toBigDecimal() ?: BigDecimal.ZERO
        
        // Calculate percentage change from first to last period
        val firstBalance = weeklyBalances.firstOrNull() ?: 0.0
        val percentageChange = if (firstBalance != 0.0) {
            val change = ((currentBalance.toDouble() - firstBalance) / Math.abs(firstBalance)) * 100
            String.format("%.1f%%", change)
        } else {
            "0%"
        }
        
        return BalanceTrendData(
            currentBalance = currentBalance,
            percentageChange = percentageChange,
            currency = selectedCurrency,
            precision = _currentPrecision,
            weeklyBalances = weeklyBalances
        )
    }
}

fun defaultAnalyticsCardsConfig(): AnalyticsCardsConfig {
    return AnalyticsCardsConfig(
        cards = listOf(
            CategoryBreakdownCardConfig(
                cardId = "analytics_category_breakdown",
                order = 0,
                title = "CATEGORY BREAKDOWN",
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
                showLabels = true,
                showLegend = true
            ),
            TimelineTrajectoryCardConfig(
                cardId = "analytics_timeline_trajectory",
                order = 1,
                title = "TIMELINE TRAJECTORY",
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
                showBalance = true,
                showExpense = true,
                showIncome = true
            ),
            CategoryExpenseTreemapCardConfig(
                cardId = "analytics_category_expense_treemap",
                order = 2,
                title = "CATEGORY EXPENSES",
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
                showLabels = true
            ),
            IncomeExpensesSummaryCardConfig(
                cardId = "analytics_income_expenses_summary",
                order = 3,
                title = "INCOME & EXPENSES",
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS),
                showPercentage = true
            ),
            PeriodComparisonCardConfig(
                cardId = "analytics_period_comparison",
                order = 4,
                title = "PERIOD COMPARISON",
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS),
                selectedTab = "Income"
            ),
            CashFlowTrendCardConfig(
                cardId = "analytics_cash_flow_trend",
                order = 5,
                title = "CASH FLOW TREND",
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)
            ),
            CashFlowProgressCardConfig(
                cardId = "analytics_cash_flow_progress",
                order = 6,
                title = "CASH FLOW",
                subtitle = "Am I spending less than I make?",
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)
            ),
            BalanceForecastCardConfig(
                cardId = "analytics_balance_forecast",
                order = 7,
                title = "BALANCE FORECAST",
                subtitle = "Will I have enough money?",
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
                forecastDays = 30
            ),
            PlannedPaymentsCardConfig(
                cardId = "analytics_planned_payments",
                order = 8,
                title = "PLANNED PAYMENTS",
                subtitle = "Planned Payments by Category",
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS)
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
    val hasLoadedExchangeRates: Boolean = false
)

