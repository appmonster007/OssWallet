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
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
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
import com.osscabinet.ossWallet.ui.analytics.components.AccountBalanceItem
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
import com.osscabinet.ossWallet.domain.service.FilterService
import com.osscabinet.ossWallet.domain.service.TimelineService
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
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
                _uiState.update { it.copy(allTransactions = transactions) }
                calculateAnalytics()
                // Invalidate cache when transactions change to force recomputation
                invalidateAllCache()
            }
        }

        viewModelScope.launch {
            settingsDataStore.currentProfileId.collect { profileId ->
                accountRepository.getAllAccounts(profileId).collect { accounts ->
                    allAccounts = accounts
                    _uiState.update { it.copy(accounts = accounts) }
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
    // These follow the DashboardScreen pattern - screens use produceState to call them with explicit parameters

    // NEW: Card compute functions that accept explicit parameters (like DashboardScreen)
    suspend fun computeCategoryBreakdownForCard(
        cardConfig: CategoryBreakdownCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ) = cardComputationService.computeCategoryBreakdown(
        config = cardConfig,
        allTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter),
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )

    suspend fun computeTimelineTrajectoryForCard(
        cardConfig: TimelineTrajectoryCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ) = cardComputationService.computeTimelineTrajectory(
        config = cardConfig,
        allTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter),
        accounts = accounts,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )

    suspend fun computeCategoryExpenseTreemapForCard(
        cardConfig: CategoryExpenseTreemapCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ) = cardComputationService.computeCategoryExpenseTreemap(
        config = cardConfig,
        allTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter),
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )

    suspend fun computeIncomeExpensesSummaryForCard(
        cardConfig: IncomeExpensesSummaryCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): IncomeExpensesSummaryData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        // Unified Filtering for Current Period
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
            )
            tx.copy(amount = convertedAmount)
        }

        // Smarter Previous Period Resolution
        val prevFilter = getPreviousPeriodFilter(cardConfig.filter)
        val prevTransactions = filterTransactionsByFilter(allTransactions, prevFilter)
            .map { tx ->
                val convertedAmount = CurrencyService.convertAmount(
                    amount = tx.amount,
                    fromCurrency = tx.currency,
                    toCurrency = selectedCurrency,
                    exchangeRates = currentRates,
                    precision = precision + 4
                )
                tx.copy(amount = convertedAmount)
            }
        
        // Totals by category
        val expenseCategoryTotals = convertedTransactions
            .filter { it.amount.signum() < 0 }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
        
        val incomeCategoryTotals = convertedTransactions
            .filter { it.amount.signum() >= 0 }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
        
        val totalIncome = incomeCategoryTotals.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        val totalExpenses = expenseCategoryTotals.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        val totalAmount = totalIncome.add(totalExpenses)

        // Previous Totals for Delta
        val prevTotalIncome = prevTransactions.filter { it.amount.signum() >= 0 }.sumOf { it.amount }
        val prevTotalExpenses = prevTransactions.filter { it.amount.signum() < 0 }.sumOf { it.amount }
        val prevNetFlow = prevTotalIncome.add(prevTotalExpenses)

        val deltaPercentage = if (prevNetFlow.signum() != 0) {
            val change = ((totalAmount - prevNetFlow).toDouble() / prevNetFlow.abs().toDouble()) * 100
            String.format("%+1.1f%%", change)
        } else "0.0%"
        
        // Efficiency Ratio: Savings Rate %
        val savingsRate = if (totalIncome.signum() > 0) {
            (totalIncome.add(totalExpenses)).divide(totalIncome, 4, RoundingMode.HALF_UP).toFloat()
        } else 0f

        // Category items with relative change
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
            Color(0xFFFF5722), AccentBlue, AccentOrange, Color(0xFF607D8B),
            AccentPurple, Color(0xFF4CAF50), Color(0xFFE91E63), Color(0xFF9C27B0),
            Color(0xFF00BCD4), Color(0xFFFFC107)
        )
        
        val categories = expenseCategoryTotals.entries
            .sortedByDescending { it.value.abs() }
            .take(4)
            .mapIndexed { index, entry ->
                val prevCatTotal = prevTransactions.filter { it.category == entry.key }.sumOf { it.amount }
                val catChange = if (prevCatTotal.signum() != 0) {
                    val change = ((entry.value - prevCatTotal).toDouble() / prevCatTotal.abs().toDouble()) * 100
                    String.format("%+1.1f%%", change)
                } else "0.0%"

                CategoryItem(
                    name = entry.key,
                    amount = entry.value,
                    change = catChange,
                    color = categoryColors.getOrElse(index) { AccentPurple },
                    icon = categoryIcons[entry.key] ?: Icons.Default.Label
                )
            }
        
        return IncomeExpensesSummaryData(
            totalAmount = totalAmount,
            percentageChange = deltaPercentage,
            currency = selectedCurrency,
            precision = precision,
            categories = categories,
            savingsRate = savingsRate,
            incomeTotal = totalIncome,
            expenseTotal = totalExpenses
        )
    }

    private fun getPreviousPeriodFilter(currentFilter: TransactionFilter): TransactionFilter {
        val (prevStart, prevEnd) = TimelineService.getPreviousRange(currentFilter.timelineRange)
        return currentFilter.copy(
            timelineRange = if (prevStart != null && prevEnd != null) 
                TimelineRange.CustomRange(prevStart, prevEnd) 
                else TimelineRange.AllTime,
            startDate = prevStart,
            endDate = prevEnd
        )
    }

    suspend fun computePeriodComparisonForCard(
        cardConfig: PeriodComparisonCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): PeriodComparisonData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        // Smarter Previous Period Resolution
        val prevFilter = getPreviousPeriodFilter(cardConfig.filter)
        val prevTransactions = filterTransactionsByFilter(allTransactions, prevFilter)
            .map { tx ->
                val convertedAmount = CurrencyService.convertAmount(
                    amount = tx.amount,
                    fromCurrency = tx.currency,
                    toCurrency = selectedCurrency,
                    exchangeRates = currentRates,
                    precision = precision + 4
                )
                tx.copy(amount = convertedAmount)
            }

        // Calculate current value
        val currentValue = when (cardConfig.selectedTab) {
            "Income" -> convertedTransactions.filter { it.amount.signum() >= 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            "Expenses" -> convertedTransactions.filter { it.amount.signum() < 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount).abs() }
            else -> convertedTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        }

        // Alignment Logic for Ghosting
        val (currentStart, currentEnd) = TimelineService.getDateRange(cardConfig.filter.timelineRange)
        val actualStart = currentStart ?: allTransactions.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()
        val actualEnd = currentEnd ?: System.currentTimeMillis()
        val duration = actualEnd - actualStart

        val numPeriods = when (val range = cardConfig.filter.timelineRange) {
            is TimelineRange.LastPeriod -> {
                when (range.period) {
                    LastPeriodType.LAST_24H -> 24
                    LastPeriodType.LAST_7_DAYS -> 7
                    else -> 12
                }
            }
            is TimelineRange.ThisPeriod -> {
                when (range.period) {
                    ThisPeriodType.WEEK -> 7
                    ThisPeriodType.MONTH -> 30
                    else -> 12
                }
            }
            else -> 12
        }

        val periodLength = duration / numPeriods.coerceAtLeast(1)
        
        val chartData = (0 until numPeriods).map { i ->
            val end = actualEnd - (i * periodLength)
            val start = end - periodLength
            val txs = convertedTransactions.filter { it.timestamp in start until end }
            when (cardConfig.selectedTab) {
                "Income" -> txs.filter { it.amount.signum() >= 0 }.sumOf { it.amount }.toDouble()
                "Expenses" -> txs.filter { it.amount.signum() < 0 }.sumOf { it.amount }.abs().toDouble()
                else -> txs.sumOf { it.amount }.toDouble()
            }
        }.reversed()

        val ghostData = if (cardConfig.showGhosting) {
            val (pStart, pEnd) = TimelineService.getPreviousRange(cardConfig.filter.timelineRange)
            val actualPEnd = pEnd ?: actualStart
            
            (0 until numPeriods).map { i ->
                val end = actualPEnd - (i * periodLength)
                val start = end - periodLength
                val txs = prevTransactions.filter { it.timestamp in start until end }
                when (cardConfig.selectedTab) {
                    "Income" -> txs.filter { it.amount.signum() >= 0 }.sumOf { it.amount }.toDouble()
                    "Expenses" -> txs.filter { it.amount.signum() < 0 }.sumOf { it.amount }.abs().toDouble()
                    else -> txs.sumOf { it.amount }.toDouble()
                }
            }.reversed()
        } else null

        // Delta Calculation vs Previous Total
        val prevTotal = when (cardConfig.selectedTab) {
            "Income" -> prevTransactions.filter { it.amount.signum() >= 0 }.sumOf { it.amount }
            "Expenses" -> prevTransactions.filter { it.amount.signum() < 0 }.sumOf { it.amount }.abs()
            else -> prevTransactions.sumOf { it.amount }
        }

        val deltaPercentage = if (prevTotal.signum() != 0) {
            ((currentValue - prevTotal).toDouble() / prevTotal.abs().toDouble()).toFloat() * 100f
        } else 0f

        return PeriodComparisonData(
            currentValue = currentValue,
            currency = selectedCurrency,
            precision = precision,
            chartData = chartData,
            ghostData = ghostData,
            runRate = prevTotal, // Run rate is renamed/reused to represent previous total for simplicity in this context
            deltaPercentage = deltaPercentage
        )
    }

    suspend fun computeCashFlowTrendForCard(
        cardConfig: CashFlowTrendCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowTrendData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
            )
            tx.copy(amount = convertedAmount)
        }

        // Previous Period for Delta
        val prevFilter = getPreviousPeriodFilter(cardConfig.filter)
        val prevTransactions = filterTransactionsByFilter(allTransactions, prevFilter)
            .map { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, selectedCurrency, currentRates, precision + 4)
            }
        val prevTotal = prevTransactions.sumOf { it }
        val currentTotal = convertedTransactions.sumOf { it.amount }
        
        val deltaPercentage = if (prevTotal.signum() != 0) {
            val change = ((currentTotal - prevTotal).toDouble() / prevTotal.abs().toDouble()) * 100
            String.format("%+1.1f%%", change)
        } else "0.0%"
        
        val now = System.currentTimeMillis()
        val numPeriods = 30 // Use 30 days for trend analysis
        val periodLength = 24 * 60 * 60 * 1000L
        
        val dailyData = (0 until numPeriods).map { i ->
            val end = now - (i * periodLength)
            val start = end - periodLength
            val txs = convertedTransactions.filter { it.timestamp in start until end }
            txs.sumOf { it.amount }.toDouble()
        }.reversed()

        // SMA Smoothing
        val smaData = if (cardConfig.showSMA) {
            val period = cardConfig.smaPeriod.coerceAtLeast(1)
            dailyData.indices.map { i ->
                val start = (i - period + 1).coerceAtLeast(0)
                dailyData.subList(start, i + 1).average()
            }
        } else null

        // Volatility (Standard Deviation)
        val mean = dailyData.average()
        val variance = dailyData.map { (it - mean) * (it - mean) }.average()
        val volatility = Math.sqrt(variance).toBigDecimal().setScale(precision, RoundingMode.HALF_UP)

        val lineData = dailyData.runningReduce { acc, d -> acc + d }

        return CashFlowTrendData(
            barData = dailyData,
            lineData = lineData,
            smaData = smaData,
            volatility = volatility,
            percentageChange = deltaPercentage
        )
    }

    suspend fun computeCashFlowProgressForCard(
        cardConfig: CashFlowProgressCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowProgressData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val income = convertedTransactions.filter { it.amount.signum() >= 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val expenses = convertedTransactions.filter { it.amount.signum() < 0 }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        
        return CashFlowProgressData(
            income = income,
            expenses = expenses,
            currency = selectedCurrency,
            precision = precision
        )
    }

    suspend fun computeBalanceForecastForCard(
        cardConfig: BalanceForecastCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): BalanceForecastData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        // Unified Filtering for historical trend calculation
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        // Calculate average daily net flow from historical data
        val now = System.currentTimeMillis()
        val daysInPeriod = when (val range = cardConfig.filter.timelineRange) {
            is TimelineRange.LastPeriod -> {
                when (range.period) {
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
            totalNetFlow.divide(BigDecimal.valueOf(daysInPeriod), precision, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        // Calculate volatility for confidence intervals
        val dailyNetFlows = convertedTransactions.groupBy { (it.timestamp / (24 * 60 * 60 * 1000L)) }
            .map { entry -> entry.value.sumOf { it.amount }.toDouble() }
        
        val stdDev = if (dailyNetFlows.size > 1) {
            val mean = dailyNetFlows.average()
            val variance = dailyNetFlows.map { (it - mean) * (it - mean) }.average()
            Math.sqrt(variance)
        } else 0.0
        
        // Get current balance
        val currentBalance = allTransactions.fold(BigDecimal.ZERO) { acc, tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
            )
            acc.add(convertedAmount)
        }
        
        // Include Planned Payments if enabled
        var forecastDailyFlow = avgDailyNetFlow
        if (cardConfig.includePlannedPayments) {
            val plannedPayments = allTransactions.filter { 
                it.status == com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED && 
                it.timestamp > now && it.timestamp < now + (cardConfig.forecastDays * 24 * 60 * 60 * 1000L)
            }
            val totalPlanned = plannedPayments.sumOf { tx ->
                CurrencyService.convertAmount(tx.amount, tx.currency, selectedCurrency, currentRates, precision)
            }
            // Distribute planned payments over the forecast period for the daily flow estimate
            val dailyPlannedFlow = totalPlanned.divide(BigDecimal.valueOf(cardConfig.forecastDays.toLong()), precision, RoundingMode.HALF_UP)
            forecastDailyFlow = forecastDailyFlow.add(dailyPlannedFlow)
        }

        // Calculate forecast balance
        val forecastBalance = currentBalance.add(forecastDailyFlow.multiply(BigDecimal.valueOf(cardConfig.forecastDays.toLong())))
        
        // Projected Zero Date
        val projectedZeroDate = if (forecastDailyFlow.signum() < 0 && currentBalance.signum() > 0) {
            val daysToZero = currentBalance.divide(forecastDailyFlow.abs(), 0, RoundingMode.DOWN).toLong()
            now + (daysToZero * 24 * 60 * 60 * 1000L)
        } else null

        // Generate chart data with confidence intervals
        val numPoints = 10
        val chartData = (0 until numPoints).map { i ->
            val daysFromNow = i * (cardConfig.forecastDays / (numPoints - 1))
            val realistic = currentBalance.add(forecastDailyFlow.multiply(BigDecimal.valueOf(daysFromNow.toLong())))
            
            // Confidence intervals grow with time (square root of time)
            val margin = (stdDev * Math.sqrt(daysFromNow.toDouble())).toBigDecimal()
            val optimistic = realistic.add(margin)
            val pessimistic = realistic.subtract(margin)
            
            Triple(optimistic.toDouble(), realistic.toDouble(), pessimistic.toDouble())
        }
        
        return BalanceForecastData(
            forecastBalance = forecastBalance,
            currency = selectedCurrency,
            precision = precision,
            chartData = chartData,
            projectedZeroDate = projectedZeroDate,
            currentLiquidCash = currentBalance
        )
    }

    suspend fun computePlannedPaymentsForCard(
        cardConfig: PlannedPaymentsCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): PlannedPaymentsData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        // Group transactions by category for planned payments
        val categoryTotals = convertedTransactions
            .filter { it.amount.signum() < 0 } // Only expenses
            .groupBy { it.category ?: "Uncategorized" }
            .mapValues { entry -> 
                entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.abs() 
            }
        
        // Calculate total planned amount
        val total = categoryTotals.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        
        // Create payment items from category data
        val categoryColors = listOf(
            AccentPurple,
            PositiveAmountColor,
            AccentOrange,
            Color(0xFF4CAF50),
            Color(0xFFE91E63),
            Color(0xFF9C27B0),
            Color(0xFF00BCD4)
        )
        
        val payments = categoryTotals.entries.mapIndexed { index, entry ->
            val amount = entry.value
            val progress = if (total > BigDecimal.ZERO) {
                (amount.divide(total, 2, RoundingMode.HALF_UP).toFloat()).coerceAtMost(1.0f)
            } else {
                0f
            }
            val color = categoryColors.getOrElse(index % categoryColors.size) { AccentPurple }
            
            PaymentItem(
                name = entry.key,
                amount = amount,
                progress = progress,
                color = color
            )
        }.sortedByDescending { it.amount }
        
        return PlannedPaymentsData(
            total = total,
            currency = selectedCurrency,
            precision = precision,
            payments = payments
        )
    }

    suspend fun computeIncomeExpensesBookForCard(
        cardConfig: IncomeExpensesBookCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): IncomeExpensesBookData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
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
            precision = precision
        )
    }

    suspend fun computeCashFlowTableForCard(
        cardConfig: CashFlowTableCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowTableData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        val incomeTransactions = convertedTransactions.filter { it.amount.signum() >= 0 }
        val expenseTransactions = convertedTransactions.filter { it.amount.signum() < 0 }
        
        val incomeCount = incomeTransactions.size
        val expenseCount = expenseTransactions.size
        
        val incomeTotal = incomeTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val expenseTotal = expenseTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        
        val incomeAvgDay = if (incomeCount > 0) incomeTotal.divide(BigDecimal.valueOf(84), precision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val expenseAvgDay = if (expenseCount > 0) expenseTotal.divide(BigDecimal.valueOf(84), precision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val incomeAvgRecord = if (incomeCount > 0) incomeTotal.divide(BigDecimal.valueOf(incomeCount.toLong()), precision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val expenseAvgRecord = if (expenseCount > 0) expenseTotal.divide(BigDecimal.valueOf(expenseCount.toLong()), precision, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
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
            precision = precision
        )
    }

    suspend fun computePlannedPaymentsTimelineForCard(
        cardConfig: PlannedPaymentsTimelineCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): PlannedPaymentsTimelineData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        // Filter for expense transactions (planned payments are typically expenses)
        val expenseTransactions = convertedTransactions.filter { it.amount.signum() < 0 }
        
        // Group transactions by week based on timestamp
        val calendar = java.util.Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        // Determine number of weeks based on timeline range
        val numWeeks = when (val range = cardConfig.filter.timelineRange) {
            is TimelineRange.LastPeriod -> {
                when (range.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 1
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 4
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 12
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_6_MONTHS -> 24
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_1_YEAR -> 52
                    else -> 12
                }
            }
            else -> 12
        }
        
        val weekMillis = 7 * 24 * 60 * 60 * 1000L
        
        val weeklyData = (0 until numWeeks).map { weekIndex ->
            val weekEnd = now - (weekIndex * weekMillis)
            val weekStart = weekEnd - weekMillis
            
            val weekTransactions = expenseTransactions.filter { 
                it.timestamp >= weekStart && it.timestamp < weekEnd 
            }
            
            val weekTotal = weekTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            
            // Calculate week number in year for label
            calendar.timeInMillis = weekStart
            val weekOfYear = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
            val year = calendar.get(java.util.Calendar.YEAR)
            
            "Week $weekOfYear ($year)" to weekTotal.abs()
        }.reversed()
        
        val total = weeklyData.fold(BigDecimal.ZERO) { acc, (_, amount) -> acc.add(amount) }
        
        return PlannedPaymentsTimelineData(
            total = total,
            currency = selectedCurrency,
            precision = precision,
            weeklyData = weeklyData
        )
    }

    suspend fun computeBalanceByAccountsForCard(
        cardConfig: BalanceByAccountsCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): BalanceByAccountsData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        // Filter transactions based on timeline range
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        
        // Calculate balance by account using filtered transactions
        val accountBalances = accounts.map { account ->
            val accountTransactions = filteredTransactions.filter { it.accountId == account.id }
            
            // Convert account transactions to selected currency
            val convertedTransactions = accountTransactions.map { tx ->
                val convertedAmount = CurrencyService.convertAmount(
                    amount = tx.amount,
                    fromCurrency = tx.currency,
                    toCurrency = selectedCurrency,
                    exchangeRates = currentRates,
                    precision = precision + 4
                )
                convertedAmount
            }
            
            val balance = convertedTransactions.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
            
            AccountBalanceItem(
                accountName = account.name,
                balance = balance,
                currency = selectedCurrency,
                isNegative = balance < BigDecimal.ZERO
            )
        }
        
        val totalBalance = accountBalances.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.balance) }
        
        return BalanceByAccountsData(
            totalBalance = totalBalance,
            currency = selectedCurrency,
            precision = precision,
            accountBalances = accountBalances
        )
    }

    suspend fun computeBalanceTrendForCard(
        cardConfig: BalanceTrendCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): BalanceTrendData {
        val selectedCurrency = baseCurrency
        val currentRates = exchangeRates
        
        val filteredTransactions = filterTransactionsByFilter(allTransactions, cardConfig.filter)
        val convertedTransactions = filteredTransactions.map { tx ->
            val convertedAmount = CurrencyService.convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = precision + 4
            )
            tx.copy(amount = convertedAmount)
        }
        
        // Calculate balance trend over time based on timeline
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        val numPeriods = when (val range = cardConfig.filter.timelineRange) {
            is TimelineRange.LastPeriod -> {
                when (range.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 7
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 4
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 12
                    else -> 12
                }
            }
            else -> 12
        }
        
        val periodLength = when (val range = cardConfig.filter.timelineRange) {
            is TimelineRange.LastPeriod -> {
                when (range.period) {
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
            precision = precision,
            weeklyBalances = weeklyBalances
        )
    }

    // NEW: Refactored Card compute functions using consolidated implementation
    suspend fun computeCategoryBreakdownCard(config: CategoryBreakdownCardConfig): List<CategoryBreakdownData> {
        val cacheKey = "category_breakdown_${config.cardId}_${config.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeCategoryBreakdownForCard(config, allTransactions, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computeTimelineTrajectoryCard(config: TimelineTrajectoryCardConfig): List<TimelineTrajectoryData> {
        val cacheKey = "timeline_trajectory_${config.cardId}_${config.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeTimelineTrajectoryForCard(config, allTransactions, allAccounts, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computeCategoryExpenseTreemapCard(config: CategoryExpenseTreemapCardConfig): List<CategoryExpenseTreemapData> {
        val cacheKey = "category_treemap_${config.cardId}_${config.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeCategoryExpenseTreemapForCard(config, allTransactions, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computeIncomeExpensesSummaryCard(cardConfig: IncomeExpensesSummaryCardConfig): IncomeExpensesSummaryData {
        val cacheKey = "income_expenses_summary_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeIncomeExpensesSummaryForCard(cardConfig, allTransactions, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computePeriodComparisonCard(cardConfig: PeriodComparisonCardConfig): PeriodComparisonData {
        val cacheKey = "period_comparison_${cardConfig.cardId}_${cardConfig.filter.hashCode()}_${cardConfig.selectedTab}"
        return getCachedOrCompute(cacheKey) {
            computePeriodComparisonForCard(cardConfig, allTransactions, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computeCashFlowTrendCard(cardConfig: CashFlowTrendCardConfig): CashFlowTrendData {
        val cacheKey = "cash_flow_trend_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeCashFlowTrendForCard(cardConfig, allTransactions, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computeCashFlowProgressCard(cardConfig: CashFlowProgressCardConfig): CashFlowProgressData {
        val cacheKey = "cash_flow_progress_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeCashFlowProgressForCard(cardConfig, allTransactions, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computeBalanceForecastCard(cardConfig: BalanceForecastCardConfig): BalanceForecastData {
        val cacheKey = "balance_forecast_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeBalanceForecastForCard(cardConfig, allTransactions, allAccounts, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computePlannedPaymentsCard(cardConfig: PlannedPaymentsCardConfig): PlannedPaymentsData {
        val cacheKey = "planned_payments_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computePlannedPaymentsForCard(cardConfig, allTransactions, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computeIncomeExpensesBookCard(cardConfig: IncomeExpensesBookCardConfig): IncomeExpensesBookData {
        val cacheKey = "income_expenses_book_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeIncomeExpensesBookForCard(cardConfig, allTransactions, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computeCashFlowTableCard(cardConfig: CashFlowTableCardConfig): CashFlowTableData {
        val cacheKey = "cash_flow_table_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeCashFlowTableForCard(cardConfig, allTransactions, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computePlannedPaymentsTimelineCard(cardConfig: PlannedPaymentsTimelineCardConfig): PlannedPaymentsTimelineData {
        val cacheKey = "planned_payments_timeline_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computePlannedPaymentsTimelineForCard(cardConfig, allTransactions, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computeBalanceByAccountsCard(cardConfig: BalanceByAccountsCardConfig): BalanceByAccountsData {
        val cacheKey = "balance_by_accounts_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeBalanceByAccountsForCard(cardConfig, allTransactions, allAccounts, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
    }

    suspend fun computeBalanceTrendCard(cardConfig: BalanceTrendCardConfig): BalanceTrendData {
        val cacheKey = "balance_trend_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return getCachedOrCompute(cacheKey) {
            computeBalanceTrendForCard(cardConfig, allTransactions, allAccounts, _uiState.value.selectedCurrency, _uiState.value.exchangeRates, _currentPrecision)
        }
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

    // Unified filtering logic for all analytics cards
    private fun filterTransactionsByFilter(
        transactions: List<Transaction>,
        filter: TransactionFilter
    ): List<Transaction> {
        return FilterService.filterTransactions(transactions, filter)
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
    // Data needed for card computation (like DashboardScreen)
    val allTransactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList()
)

