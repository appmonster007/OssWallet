package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData
import com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesSummaryData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryItem
import com.osscabinet.ossWallet.ui.analytics.components.PeriodComparisonData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTrendData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowProgressData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceForecastData
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsData
import com.osscabinet.ossWallet.ui.analytics.components.PaymentItem
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
import androidx.compose.material.icons.Icons
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

    // Card data states
    private val _categoryBreakdownData = MutableStateFlow<List<CategoryBreakdownData>>(emptyList())
    val categoryBreakdownData: StateFlow<List<CategoryBreakdownData>> = _categoryBreakdownData.asStateFlow()

    private val _timelineTrajectoryData = MutableStateFlow<List<TimelineTrajectoryData>>(emptyList())
    val timelineTrajectoryData: StateFlow<List<TimelineTrajectoryData>> = _timelineTrajectoryData.asStateFlow()

    private val _categoryExpenseTreemapData = MutableStateFlow<List<CategoryExpenseTreemapData>>(emptyList())
    val categoryExpenseTreemapData: StateFlow<List<CategoryExpenseTreemapData>> = _categoryExpenseTreemapData.asStateFlow()

    // New card data states
    private val _incomeExpensesSummaryData = MutableStateFlow<IncomeExpensesSummaryData?>(null)
    val incomeExpensesSummaryData: StateFlow<IncomeExpensesSummaryData?> = _incomeExpensesSummaryData.asStateFlow()

    private val _periodComparisonData = MutableStateFlow<PeriodComparisonData?>(null)
    val periodComparisonData: StateFlow<PeriodComparisonData?> = _periodComparisonData.asStateFlow()

    private val _cashFlowTrendData = MutableStateFlow<CashFlowTrendData?>(null)
    val cashFlowTrendData: StateFlow<CashFlowTrendData?> = _cashFlowTrendData.asStateFlow()

    private val _cashFlowProgressData = MutableStateFlow<CashFlowProgressData?>(null)
    val cashFlowProgressData: StateFlow<CashFlowProgressData?> = _cashFlowProgressData.asStateFlow()

    private val _balanceForecastData = MutableStateFlow<BalanceForecastData?>(null)
    val balanceForecastData: StateFlow<BalanceForecastData?> = _balanceForecastData.asStateFlow()

    private val _plannedPaymentsData = MutableStateFlow<PlannedPaymentsData?>(null)
    val plannedPaymentsData: StateFlow<PlannedPaymentsData?> = _plannedPaymentsData.asStateFlow()

    // Analytics cards config
    val analyticsCardsConfig: StateFlow<AnalyticsCardsConfig> = settingsDataStore.analyticsCardsConfig
        .map { configStr ->
            if (configStr.isEmpty()) {
                defaultAnalyticsCardsConfig()
            } else {
                try {
                    Json.decodeFromString<AnalyticsCardsConfig>(configStr)
                } catch (e: Exception) {
                    defaultAnalyticsCardsConfig()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultAnalyticsCardsConfig())

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
                // Recompute card data when transactions change
                analyticsCardsConfig.value.cards.forEach { cardConfig ->
                    updateCardConfig(cardConfig)
                }
            }
        }

        viewModelScope.launch {
            settingsDataStore.currentProfileId.collect { profileId ->
                accountRepository.getAllAccounts(profileId).collect { accounts ->
                    allAccounts = accounts
                    calculateAnalytics()
                    // Recompute card data when accounts change
                    analyticsCardsConfig.value.cards.forEach { cardConfig ->
                        updateCardConfig(cardConfig)
                    }
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

        // Compute card data when config changes
        viewModelScope.launch {
            analyticsCardsConfig.collect { config ->
                config.cards.forEach { cardConfig ->
                    updateCardConfig(cardConfig)
                }
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

    fun updateCardConfig(cardConfig: CardConfig) {
        viewModelScope.launch {
            when (cardConfig) {
                is CategoryBreakdownCardConfig -> {
                    _categoryBreakdownData.value = cardComputationService.computeCategoryBreakdown(
                        config = cardConfig,
                        transactions = allTransactions,
                        baseCurrency = _uiState.value.selectedCurrency,
                        exchangeRates = _uiState.value.exchangeRates,
                        precision = _currentPrecision
                    )
                }
                is TimelineTrajectoryCardConfig -> {
                    _timelineTrajectoryData.value = cardComputationService.computeTimelineTrajectory(
                        config = cardConfig,
                        transactions = allTransactions,
                        accounts = allAccounts,
                        baseCurrency = _uiState.value.selectedCurrency,
                        exchangeRates = _uiState.value.exchangeRates,
                        precision = _currentPrecision
                    )
                }
                is CategoryExpenseTreemapCardConfig -> {
                    _categoryExpenseTreemapData.value = cardComputationService.computeCategoryExpenseTreemap(
                        config = cardConfig,
                        transactions = allTransactions,
                        baseCurrency = _uiState.value.selectedCurrency,
                        exchangeRates = _uiState.value.exchangeRates,
                        precision = _currentPrecision
                    )
                }
                is IncomeExpensesSummaryCardConfig -> {
                    _incomeExpensesSummaryData.value = computeIncomeExpensesSummary(cardConfig)
                }
                is PeriodComparisonCardConfig -> {
                    _periodComparisonData.value = computePeriodComparison(cardConfig)
                }
                is CashFlowTrendCardConfig -> {
                    _cashFlowTrendData.value = computeCashFlowTrend(cardConfig)
                }
                is CashFlowProgressCardConfig -> {
                    _cashFlowProgressData.value = computeCashFlowProgress(cardConfig)
                }
                is BalanceForecastCardConfig -> {
                    _balanceForecastData.value = computeBalanceForecast(cardConfig)
                }
                is PlannedPaymentsCardConfig -> {
                    _plannedPaymentsData.value = computePlannedPayments(cardConfig)
                }
                else -> {}
            }
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
        val currentConfig = analyticsCardsConfig.value
        val newCards = currentConfig.cards + cardConfig
        val updatedConfig = AnalyticsCardsConfig(newCards)
        settingsDataStore.saveAnalyticsCardsConfig(Json.encodeToString(updatedConfig))
    }

    suspend fun removeCard(cardId: String) {
        val currentConfig = analyticsCardsConfig.value
        val newCards = currentConfig.cards.filter { it.cardId != cardId }
        val updatedConfig = AnalyticsCardsConfig(newCards)
        settingsDataStore.saveAnalyticsCardsConfig(Json.encodeToString(updatedConfig))
    }

    suspend fun updateCardConfigById(cardId: String, update: (CardConfig) -> CardConfig) {
        val currentConfig = analyticsCardsConfig.value
        val newCards = currentConfig.cards.map { card ->
            if (card.cardId == cardId) update(card) else card
        }
        val updatedConfig = AnalyticsCardsConfig(newCards)
        settingsDataStore.saveAnalyticsCardsConfig(Json.encodeToString(updatedConfig))
    }

    suspend fun reorderCards(newOrder: List<CardConfig>) {
        val updatedConfig = AnalyticsCardsConfig(newOrder)
        settingsDataStore.saveAnalyticsCardsConfig(Json.encodeToString(updatedConfig))
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
        
        // Calculate totals by category
        val categoryTotals = convertedTransactions
            .filter { it.amount.signum() < 0 } // Only expenses
            .groupBy { it.category ?: "Uncategorized" }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
        
        // Calculate total amount
        val totalAmount = categoryTotals.values.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
        
        // Calculate percentage change (simplified - compare with previous period)
        val percentageChange = "+154%" // Placeholder - would need historical data
        
        // Create category items with icons and colors
        val categories = listOf(
            "Food & Drinks" to Icons.Default.Restaurant,
            "Shopping" to Icons.Default.ShoppingCart,
            "Housing" to Icons.Default.Home,
            "Transportation" to Icons.Default.DirectionsCar
        ).mapNotNull { (name, icon) ->
            val amount = categoryTotals[name]
            if (amount != null) {
                CategoryItem(
                    name = name,
                    amount = amount,
                    change = "+5%", // Placeholder
                    color = when (name) {
                        "Food & Drinks" -> Color(0xFFFF5722)
                        "Shopping" -> AccentBlue
                        "Housing" -> AccentOrange
                        "Transportation" -> Color(0xFF607D8B)
                        else -> AccentPurple
                    },
                    icon = icon
                )
            } else null
        }.take(4)
        
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
        
        // Generate chart data (weekly breakdown)
        val chartData = listOf(4.0, 12.0, 8.0, 16.0, 20.0, 15.0) // Placeholder - would need actual weekly data
        
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
        
        // Generate bar and line data
        val barData = listOf(5.0, -10.0, 15.0, -5.0, 10.0) // Placeholder
        val lineData = listOf(2.0, -4.0, 8.0, -2.0, 5.0) // Placeholder
        
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
        val currentBalance = _uiState.value.currentBalance
        val avgDailyNetFlow = if (_uiState.value.dailyNetFlow.values.isNotEmpty()) {
            _uiState.value.dailyNetFlow.values.map { it.toDouble() }.average().toBigDecimal()
        } else BigDecimal.ZERO
        
        // Calculate forecast
        val forecastBalance = currentBalance.add(avgDailyNetFlow.multiply(BigDecimal.valueOf(config.forecastDays.toLong())))
        
        // Generate chart data
        val chartData = (1..5).map { Triple(it.toDouble(), (it * 1.5).toDouble(), (it * 0.8).toDouble()) }
        
        return BalanceForecastData(
            forecastBalance = forecastBalance,
            currency = selectedCurrency,
            precision = _currentPrecision,
            chartData = chartData
        )
    }
    
    private fun computePlannedPayments(config: PlannedPaymentsCardConfig): PlannedPaymentsData {
        val selectedCurrency = _uiState.value.selectedCurrency
        
        // Placeholder data - would need actual planned/recurring transactions
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
    
    private fun filterTransactionsByTimeline(transactions: List<Transaction>, timelineRange: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange): List<Transaction> {
        val now = System.currentTimeMillis()
        return when (timelineRange) {
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> {
                val days = when (timelineRange.period) {
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_24H -> 1L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_7_DAYS -> 7L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS -> 30L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_12_WEEKS -> 84L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_6_MONTHS -> 180L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_1_YEAR -> 365L
                    com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_5_YEARS -> 1825L
                }
                val cutoff = now - days * 24 * 60 * 60 * 1000L
                transactions.filter { it.timestamp >= cutoff }
            }
            is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.CustomRange -> {
                transactions.filter { it.timestamp >= timelineRange.start && it.timestamp <= timelineRange.end }
            }
            else -> transactions
        }
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

