package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.service.FinancialMetrics
import com.osscabinet.ossWallet.domain.service.FinancialSummary
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.service.*
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.domain.util.CardDataCache
import com.osscabinet.ossWallet.domain.util.CachedCardData
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.ui.analytics.components.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
    private val settingsRepository: SettingsRepository,
    private val computationRepository: ComputationRepository,
    private val categoryRepository: CategoryRepository,
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
    val decimalPrecision: StateFlow<Int> = settingsRepository.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)
    private var _currentPrecision: Int = 2

    private val cardDataCache = CardDataCache()
    private val engine = CardComputationEngine

    // Analytics cards config
    private val _analyticsCardsConfig = MutableStateFlow(defaultAnalyticsCardsConfig())
    val analyticsCardsConfig: StateFlow<AnalyticsCardsConfig> = _analyticsCardsConfig.asStateFlow()

    init {
        // Load analytics cards config from DataStore on initialization
        viewModelScope.launch {
            settingsRepository.analyticsCardsConfig.collect { configStr ->
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
            computationRepository.selectedCurrency.collect { currencyCode ->
                onCurrencySelected(currencyCode)
            }
        }

        viewModelScope.launch {
            computationRepository.exchangeRates.collect { rates ->
                _uiState.update { it.copy(exchangeRates = rates) }
                calculateAnalytics()
            }
        }

        viewModelScope.launch {
            computationRepository.hasLoadedExchangeRates.collect { hasLoaded ->
                _uiState.update { it.copy(hasLoadedExchangeRates = hasLoaded) }
                calculateAnalytics()
            }
        }

        viewModelScope.launch {
            settingsRepository.decimalPrecision.collect { precision ->
                _currentPrecision = precision
                _uiState.update { it.copy(decimalPrecision = precision) }
                calculateAnalytics()
            }
        }

        viewModelScope.launch {
            transactionUseCases.getTransactions().collect { transactions ->
                allTransactions = transactions
                _uiState.update { it.copy(allTransactions = transactions) }
                calculateAnalytics()
                cardDataCache.invalidateAll()
            }
        }

        viewModelScope.launch {
            settingsRepository.currentProfileId
                .flatMapLatest { profileId -> accountRepository.getAllAccounts(profileId) }
                .collect { accounts ->
                    allAccounts = accounts
                    _uiState.update { it.copy(accounts = accounts) }
                    calculateAnalytics()
                    cardDataCache.invalidateAll()
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

    // Simplified ViewModel compute methods using registry engine
    suspend fun computeCategoryBreakdownCard(config: CategoryBreakdownCardConfig): List<CategoryBreakdownData> {
        val cacheKey = "category_breakdown_${config.cardId}_${config.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            val categories = categoryRepository.getAllCategoriesSync()
            val data = engine.computeCard(
                config = config,
                accounts = emptyList(),
                transactions = allTransactions,
                categories = categories,
                context = getComputationContext()
            ) as CardRenderData.CategoryBreakdownResult
            
            fun mapToUi(metric: CategoryTreeMetrics): CategoryBreakdownData {
                return CategoryBreakdownData(
                    category = metric.category,
                    categoryId = metric.categoryId,
                    netBalance = metric.netBalance,
                    income = metric.income,
                    expense = metric.expense,
                    colorHex = metric.colorHex,
                    percentage = metric.percentage,
                    hasSubcategories = metric.hasSubcategories,
                    subcategoryData = metric.subcategoryData.map { mapToUi(it) }
                )
            }
            data.items.map { mapToUi(it) }
        }
    }

    suspend fun computeCategoryExpenseTreemapCard(config: CategoryExpenseTreemapCardConfig): List<CategoryExpenseTreemapData> {
        val cacheKey = "category_treemap_${config.cardId}_${config.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            val categories = categoryRepository.getAllCategoriesSync()
            val data = engine.computeCard(
                config = config,
                accounts = emptyList(),
                transactions = allTransactions,
                categories = categories,
                context = getComputationContext()
            ) as CardRenderData.CategoryTreemapResult
            
            fun mapToUi(metric: CategoryTreeMetrics): CategoryExpenseTreemapData {
                return CategoryExpenseTreemapData(
                    category = if (metric.category.isEmpty()) "Uncategorized" else metric.category,
                    categoryId = metric.categoryId,
                    expense = metric.expense,
                    colorHex = metric.colorHex,
                    percentage = metric.percentage,
                    hasSubcategories = metric.hasSubcategories,
                    subcategoryData = metric.subcategoryData.map { mapToUi(it) }
                )
            }
            data.items.map { mapToUi(it) }
        }
    }


    private fun calculateAnalytics() {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        // 1. Global Totals using AggregationService
        val globalBreakdown = AggregationService.calculateIncomeExpenseBreakdown(
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

        val thisMonthBreakdown = AggregationService.calculateIncomeExpenseBreakdown(
            thisMonthTransactions, currentRates, selectedCurrency
        )
        val lastMonthBreakdown = AggregationService.calculateIncomeExpenseBreakdown(
            lastMonthTransactions, currentRates, selectedCurrency
        )

        val thisMonthSavingsRate = AggregationService.calculateSavingsRate(
            thisMonthBreakdown.totalIncome, thisMonthBreakdown.totalExpense
        )
        val lastMonthSavingsRate = AggregationService.calculateSavingsRate(
            lastMonthBreakdown.totalIncome, lastMonthBreakdown.totalExpense
        )

        // 3. Burn Rate
        val firstTx = allTransactions.minByOrNull { it.timestamp }
        val daysActive = if (firstTx != null) {
            val diff = System.currentTimeMillis() - firstTx.timestamp
            (diff / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
        } else 1

        val monthlyBurnRate = AggregationService.calculateBurnRate(
            totalExpense = globalBreakdown.totalExpense,
            daysActive = daysActive
        )

        // 4. Daily Net Flow (Last 7 days)
        val dailyNetFlow = AggregationService.calculateDailyNetFlow(
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
        settingsRepository.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
    }

    suspend fun removeCard(cardId: String) {
        _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
            cards = _analyticsCardsConfig.value.cards.filter { it.cardId != cardId }
        )
        settingsRepository.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
    }
    
    suspend fun updateCardTimelineRange(cardId: String, newTimelineRange: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange) {
        _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
            cards = _analyticsCardsConfig.value.cards.map { card ->
                if (card.cardId == cardId) {
                    when (card) {
                        is CategoryBreakdownCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        is CategoryExpenseTreemapCardConfig -> card.copy(filter = card.filter.copy(timelineRange = newTimelineRange))
                        else -> card
                    }
                } else {
                    card
                }
            }
        )
        settingsRepository.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
    }

    suspend fun updateCardConfigById(cardId: String, update: (CardConfig) -> CardConfig) {
        _analyticsCardsConfig.value = _analyticsCardsConfig.value.copy(
            cards = _analyticsCardsConfig.value.cards.map { card ->
                if (card.cardId == cardId) update(card) else card
            }
        )
        settingsRepository.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
    }

    suspend fun reorderCards(newOrder: List<CardConfig>) {
        // Update order property to match new positions for each card type
        val reorderedWithUpdatedOrder = newOrder.mapIndexed { index, card ->
            when (card) {
                is NetWorthCardConfig -> card.copy(order = index)
                is CurrencyBalancesCardConfig -> card.copy(order = index)
                is CashFlowCardConfig -> card.copy(order = index)
                is AccountsCardConfig -> card.copy(order = index)
                is CategoryBreakdownCardConfig -> card.copy(order = index)
                is CategoryExpenseTreemapCardConfig -> card.copy(order = index)
                is AccountHeroCardConfig -> card.copy(order = index)
            }
        }
        _analyticsCardsConfig.value = AnalyticsCardsConfig(reorderedWithUpdatedOrder)
        settingsRepository.saveAnalyticsCardsConfig(Json.encodeToString(_analyticsCardsConfig.value))
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
            CategoryExpenseTreemapCardConfig(
                cardId = "analytics_category_expense_treemap",
                order = 1,
                title = "CATEGORY EXPENSES",
                filter = TransactionFilter(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS)),
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
    val hasLoadedExchangeRates: Boolean = false,
    val allTransactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList()
)
