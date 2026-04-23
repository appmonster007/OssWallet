package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData
import com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
import com.osscabinet.ossWallet.domain.model.settings.AnalyticsCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.TimelineTrajectoryCardConfig
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

        val totalIncome = convertedTransactions.filter { it.transactionType == "income" }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.setScale(_currentPrecision, RoundingMode.HALF_UP)
        val totalExpense = convertedTransactions.filter { it.transactionType == "expense" }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.setScale(_currentPrecision, RoundingMode.HALF_UP)
        val currentBalance = totalIncome.subtract(totalExpense).setScale(_currentPrecision, RoundingMode.HALF_UP)
        
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

        val thisMonthIncome = thisMonthTransactions.filter { it.transactionType == "income" }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val thisMonthExpense = thisMonthTransactions.filter { it.transactionType == "expense" }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val thisMonthSavingsRate = if (thisMonthIncome.signum() > 0) {
            thisMonthIncome.subtract(thisMonthExpense).divide(thisMonthIncome, 4, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        val lastMonthIncome = lastMonthTransactions.filter { it.transactionType == "income" }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val lastMonthExpense = lastMonthTransactions.filter { it.transactionType == "expense" }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val lastMonthSavingsRate = if (lastMonthIncome.signum() > 0) {
            lastMonthIncome.subtract(lastMonthExpense).divide(lastMonthIncome, 4, RoundingMode.HALF_UP).toDouble()
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
                    if (tx.transactionType == "income") acc.add(tx.amount) else acc.subtract(tx.amount)
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
                title = "CATEGORY EXPENSE TREEMAP",
                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
                showLabels = true
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

