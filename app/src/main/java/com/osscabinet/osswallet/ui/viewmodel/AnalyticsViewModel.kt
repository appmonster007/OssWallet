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
import com.osscabinet.ossWallet.domain.service.FilterService
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.domain.service.TransactionsUtilityService
import com.osscabinet.ossWallet.domain.service.TimeBucket
import com.osscabinet.ossWallet.domain.service.TrajectoryPoint
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
    private val categoryDao: CategoryDao,
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

    // Direct computation implementations using TransactionsUtilityService
    // Note: Category tree building with colors requires CategoryDao and is implemented below

    suspend fun computeCategoryBreakdownForCard(
        cardConfig: CategoryBreakdownCardConfig,
        allTransactions: List<Transaction>
    ): List<CategoryBreakdownData> {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val allCategories = categoryDao.getAllCategoriesSync()
        val categoryMap = allCategories.associateBy { it.name }

        val categoryBreakdowns = TransactionsUtilityService.breakdownByCategory(
            transactions = filtered,
            exchangeRates = getComputationContext().exchangeRates,
            baseCurrency = getComputationContext().baseCurrency
        ).associateBy { it.key }

        val globalTotal = categoryBreakdowns.values.sumOf { it.totalAmount }

        fun buildTree(categoryName: String?, parentColor: Color? = null, index: Int = 0, totalSiblings: Int = 1): CategoryBreakdownData? {
            val entity = categoryMap[categoryName] ?: return null
            val breakdown = categoryBreakdowns[categoryName]
                ?: FinancialMetrics(categoryName ?: "", FinancialSummary.EMPTY, emptyMap())

            val baseColor = if (parentColor == null) Color(entity.colorHex) else {
                val factor = 0.7f + (index.toFloat() / totalSiblings.coerceAtLeast(1)) * 0.4f
                parentColor.copy(
                    red = (parentColor.red * factor).coerceIn(0f, 1f),
                    green = (parentColor.green * factor).coerceIn(0f, 1f),
                    blue = (parentColor.blue * factor).coerceIn(0f, 1f)
                )
            }

            val childrenEntities = allCategories.filter { it.parentId == entity.id }
            val childrenData = childrenEntities.mapIndexedNotNull { i, child ->
                buildTree(child.name, baseColor, i, childrenEntities.size)
            }.sortedByDescending { it.netBalance.abs() }

            val totalIncome = breakdown.income + childrenData.sumOf { it.income }
            val totalExpense = breakdown.expense + childrenData.sumOf { it.expense }
            val totalNet = totalIncome + totalExpense

            return CategoryBreakdownData(
                category = entity.name,
                categoryId = entity.id,
                netBalance = totalNet,
                income = totalIncome,
                expense = totalExpense,
                colorHex = baseColor.toArgb(),
                percentage = if (globalTotal.signum() != 0) (totalNet.abs().divide(globalTotal.abs(), 4, RoundingMode.HALF_UP)).toFloat() else 0f,
                hasSubcategories = childrenData.isNotEmpty(),
                subcategoryData = childrenData
            )
        }

        val rootCategories = allCategories.filter { it.parentId == null }
        return rootCategories.mapNotNull { root ->
            buildTree(root.name)
        }.sortedByDescending { it.netBalance.abs() }
    }

    suspend fun computeTimelineTrajectoryForCard(
        cardConfig: TimelineTrajectoryCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>
    ): List<TimelineTrajectoryData> {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val timeBuckets = TransactionsUtilityService.getTimeBuckets(cardConfig.filter.timelineRange)

        val bucketed = TransactionsUtilityService.createBreakdown<TimeBucket, FinancialMetrics<TimeBucket>>(
            transactions = filtered,
            selector = { tx -> timeBuckets.find { it.start <= tx.timestamp && tx.timestamp < it.end } ?: TimeBucket(0,0) },
            mapper = { bucket, summaries ->
                val baseSummary = TransactionsUtilityService.calculateTotalInBaseCurrency(summaries, getComputationContext().baseCurrency, getComputationContext().exchangeRates)
                FinancialMetrics(bucket, baseSummary, summaries)
            },
            exchangeRates = getComputationContext().exchangeRates,
            baseCurrency = getComputationContext().baseCurrency
        ).filter { it.key.start != 0L }.sortedBy { it.key.start }

        val (startTime, _) = com.osscabinet.ossWallet.domain.service.TimelineService.getDateRange(cardConfig.filter.timelineRange)
        val priorTransactions = allTransactions.filter { tx ->
            (cardConfig.filter.accountIds.isEmpty() || tx.accountId in cardConfig.filter.accountIds) &&
            tx.timestamp < (startTime ?: 0L)
        }

        val priorBalance = TransactionsUtilityService.breakdownByAccount(
            priorTransactions, accounts, getComputationContext().exchangeRates, getComputationContext().baseCurrency
        ).sumOf { it.totalAmount }

        val trajectory = TransactionsUtilityService.calculateCumulativeSum(bucketed, priorBalance)

        return trajectory.map { point ->
            TimelineTrajectoryData(
                timestamp = point.key.start,
                balance = point.cumulativeBalance,
                expense = point.expense,
                income = point.income
            )
        }
    }

    suspend fun computeCategoryExpenseTreemapForCard(
        cardConfig: CategoryExpenseTreemapCardConfig,
        allTransactions: List<Transaction>
    ): List<CategoryExpenseTreemapData> {
        // Similar to category breakdown but only for expenses
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val expenseTransactions = filtered.filter { it.amount.signum() < 0 }

        val allCategories = categoryDao.getAllCategoriesSync()
        val categoryMap = allCategories.associateBy { it.name }

        val categoryBreakdowns = TransactionsUtilityService.breakdownByCategory(
            transactions = expenseTransactions,
            exchangeRates = getComputationContext().exchangeRates,
            baseCurrency = getComputationContext().baseCurrency
        ).associateBy { it.key }

        val totalExpenseGlobal = categoryBreakdowns.values.sumOf { it.expense }.abs()

        fun buildTree(categoryName: String?, parentColor: Color? = null, index: Int = 0, totalSiblings: Int = 1): CategoryExpenseTreemapData? {
            val entity = categoryMap[categoryName] ?: return null
            val breakdown = categoryBreakdowns[categoryName]
                ?: FinancialMetrics(categoryName ?: "", FinancialSummary.EMPTY, emptyMap())

            val baseColor = if (parentColor == null) Color(entity.colorHex) else {
                val factor = 0.7f + (index.toFloat() / totalSiblings.coerceAtLeast(1)) * 0.4f
                parentColor.copy(
                    red = (parentColor.red * factor).coerceIn(0f, 1f),
                    green = (parentColor.green * factor).coerceIn(0f, 1f),
                    blue = (parentColor.blue * factor).coerceIn(0f, 1f)
                )
            }

            val childrenEntities = allCategories.filter { it.parentId == entity.id }
            val childrenData = childrenEntities.mapIndexedNotNull { i, child ->
                buildTree(child.name, baseColor, i, childrenEntities.size)
            }.sortedByDescending { it.expense.abs() }

            val totalExpense = breakdown.expense + childrenData.sumOf { it.expense }

            return CategoryExpenseTreemapData(
                category = entity.name,
                categoryId = entity.id,
                expense = totalExpense,
                colorHex = baseColor.toArgb(),
                percentage = if (totalExpenseGlobal.signum() != 0) (totalExpense.abs().divide(totalExpenseGlobal, 4, RoundingMode.HALF_UP)).toFloat() else 0f,
                hasSubcategories = childrenData.isNotEmpty(),
                subcategoryData = childrenData
            )
        }

        val rootCategories = allCategories.filter { it.parentId == null }
        return rootCategories.mapNotNull { root ->
            buildTree(root.name)
        }.sortedByDescending { it.expense.abs() }
    }

    suspend fun computeIncomeExpensesSummaryForCard(
        cardConfig: IncomeExpensesSummaryCardConfig,
        allTransactions: List<Transaction>
    ): IncomeExpensesSummaryData {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val breakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = filtered,
            exchangeRates = getComputationContext().exchangeRates,
            baseCurrency = getComputationContext().baseCurrency
        )
        
        val context = getComputationContext()
        val incomeTotal = breakdown.totalIncome
        val expenseTotal = breakdown.totalExpense
        val netFlow = breakdown.netFlow
        
        // Calculate savings rate
        val savingsRate = if (incomeTotal > BigDecimal.ZERO) {
            (netFlow / incomeTotal).toFloat()
        } else 0f
        
        // Calculate percentage change (simplified - would need historical data for real comparison)
        val percentageChange = if (netFlow >= BigDecimal.ZERO) "+${(savingsRate * 100).toInt()}%" else "${(savingsRate * 100).toInt()}%"
        
        // Get category breakdown for top categories
        val categoryBreakdown = TransactionsUtilityService.breakdownByCategory(
            transactions = filtered,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val categories = categoryBreakdown.take(5).map { metric ->
            CategoryItem(
                name = metric.key,
                amount = metric.totalAmount,
                change = percentageChange,
                color = androidx.compose.ui.graphics.Color(0xFF6750A4),
                icon = Icons.Default.Settings
            )
        }
        
        return IncomeExpensesSummaryData(
            totalAmount = netFlow,
            percentageChange = percentageChange,
            currency = context.baseCurrency,
            precision = context.precision,
            categories = categories,
            savingsRate = savingsRate,
            incomeTotal = incomeTotal,
            expenseTotal = expenseTotal
        )
    }

    suspend fun computePeriodComparisonForCard(
        cardConfig: PeriodComparisonCardConfig,
        allTransactions: List<Transaction>
    ): PeriodComparisonData {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val context = getComputationContext()
        
        val breakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = filtered,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val currentValue = breakdown.netFlow
        
        // Generate chart data from time buckets
        val timeBuckets = TransactionsUtilityService.getTimeBuckets(cardConfig.filter.timelineRange, 10)
        val chartData = timeBuckets.map { bucket ->
            val bucketTransactions = filtered.filter { it.timestamp in bucket.start until bucket.end }
            val bucketBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
                transactions = bucketTransactions,
                exchangeRates = context.exchangeRates,
                baseCurrency = context.baseCurrency
            )
            bucketBreakdown.netFlow.toDouble()
        }
        
        // Calculate delta percentage
        val deltaPercentage = if (chartData.isNotEmpty() && chartData.firstOrNull { it != 0.0 } != null) {
            val first = chartData.first { it != 0.0 }
            val last = chartData.lastOrNull() ?: first
            if (first != 0.0) ((last - first) / first * 100).toFloat() else 0f
        } else 0f
        
        return PeriodComparisonData(
            currentValue = currentValue,
            currency = context.baseCurrency,
            precision = context.precision,
            chartData = chartData,
            ghostData = null,
            runRate = currentValue,
            deltaPercentage = deltaPercentage
        )
    }

    suspend fun computeCashFlowTrendForCard(
        cardConfig: CashFlowTrendCardConfig,
        allTransactions: List<Transaction>
    ): CashFlowTrendData {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val context = getComputationContext()
        
        // Generate time buckets and calculate cash flow per bucket
        val timeBuckets = TransactionsUtilityService.getTimeBuckets(cardConfig.filter.timelineRange, 20)
        val barData = timeBuckets.map { bucket ->
            val bucketTransactions = filtered.filter { it.timestamp in bucket.start until bucket.end }
            val breakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
                transactions = bucketTransactions,
                exchangeRates = context.exchangeRates,
                baseCurrency = context.baseCurrency
            )
            breakdown.netFlow.toDouble()
        }
        
        // Calculate cumulative line data
        var cumulative = 0.0
        val lineData = barData.map { 
            cumulative += it
            cumulative
        }
        
        // Calculate simple moving average
        val smaData = barData.windowed(3, 1, partialWindows = true) { window ->
            window.average()
        }
        
        // Calculate volatility
        val mean = barData.average()
        val variance = barData.map { (it - mean) * (it - mean) }.average()
        val volatility = BigDecimal.valueOf(Math.sqrt(variance))
        
        // Calculate percentage change
        val percentageChange = if (barData.isNotEmpty() && barData.firstOrNull { it != 0.0 } != null) {
            val first = barData.first { it != 0.0 }
            val last = barData.lastOrNull() ?: first
            if (first != 0.0) "${((last - first) / first * 100).toInt()}%" else "0%"
        } else "0%"
        
        return CashFlowTrendData(
            barData = barData,
            lineData = lineData,
            smaData = smaData,
            volatility = volatility,
            percentageChange = percentageChange
        )
    }

    suspend fun computeCashFlowProgressForCard(
        cardConfig: CashFlowProgressCardConfig,
        allTransactions: List<Transaction>
    ): CashFlowProgressData {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val context = getComputationContext()
        
        val breakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = filtered,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        return CashFlowProgressData(
            income = breakdown.totalIncome,
            expenses = breakdown.totalExpense,
            currency = context.baseCurrency,
            precision = context.precision
        )
    }

    suspend fun computeBalanceForecastForCard(
        cardConfig: BalanceForecastCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>
    ): BalanceForecastData {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val context = getComputationContext()
        
        // Calculate current balance
        val accountBreakdown = TransactionsUtilityService.breakdownByAccount(
            transactions = allTransactions,
            accounts = accounts,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        val currentLiquidCash = accountBreakdown.sumOf { it.totalAmount }
        
        // Calculate average daily cash flow from historical data
        val breakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = filtered,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        val daysInPeriod = 30 // Default to 30 days for projection
        val avgDailyFlow = breakdown.netFlow / BigDecimal.valueOf(daysInPeriod.toLong())
        
        // Generate forecast data for next 6 months
        val chartData = (1..6).map { month ->
            val optimistic = currentLiquidCash.toDouble() + (avgDailyFlow.toDouble() * 30 * month * 1.2)
            val realistic = currentLiquidCash.toDouble() + (avgDailyFlow.toDouble() * 30 * month)
            val pessimistic = currentLiquidCash.toDouble() + (avgDailyFlow.toDouble() * 30 * month * 0.8)
            Triple(optimistic, realistic, pessimistic)
        }
        
        val forecastBalance = currentLiquidCash + (avgDailyFlow * BigDecimal.valueOf(90)) // 3 month forecast
        
        return BalanceForecastData(
            forecastBalance = forecastBalance.setScale(context.precision, RoundingMode.HALF_UP),
            currency = context.baseCurrency,
            precision = context.precision,
            chartData = chartData,
            projectedZeroDate = null,
            currentLiquidCash = currentLiquidCash.setScale(context.precision, RoundingMode.HALF_UP)
        )
    }

    suspend fun computePlannedPaymentsForCard(
        cardConfig: PlannedPaymentsCardConfig,
        allTransactions: List<Transaction>
    ): PlannedPaymentsData {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val context = getComputationContext()
        
        // For planned payments, we look at recurring expenses
        val expenseTransactions = filtered.filter { it.amount < BigDecimal.ZERO }
        
        // Group by category and calculate totals
        val categoryBreakdown = TransactionsUtilityService.breakdownByCategory(
            transactions = expenseTransactions,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val total = categoryBreakdown.sumOf { it.expense.abs() }
        
        // Create payment items from categories
        val payments = categoryBreakdown.take(5).map { metric ->
            PaymentItem(
                name = metric.key.ifEmpty { "Uncategorized" },
                amount = metric.expense.abs().setScale(context.precision, RoundingMode.HALF_UP),
                progress = 0.5f, // Default progress
                color = Color(0xFF6750A4),
                isAtRisk = false
            )
        }
        
        // Generate daily volume heatmap (simplified)
        val dailyVolumeHeatmap = (0..6).map { dayOffset ->
            val date = System.currentTimeMillis() + (dayOffset * 24 * 60 * 60 * 1000)
            date to (total.toDouble() / 30) // Average daily volume
        }
        
        return PlannedPaymentsData(
            total = total.setScale(context.precision, RoundingMode.HALF_UP),
            currency = context.baseCurrency,
            precision = context.precision,
            payments = payments,
            currentLiquidCash = BigDecimal.ZERO, // Would need actual account balances
            dailyVolumeHeatmap = dailyVolumeHeatmap
        )
    }

    suspend fun computeIncomeExpensesBookForCard(
        cardConfig: IncomeExpensesBookCardConfig,
        allTransactions: List<Transaction>
    ): IncomeExpensesBookData {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val context = getComputationContext()
        
        val incomeTransactions = filtered.filter { it.amount > BigDecimal.ZERO }
        val expenseTransactions = filtered.filter { it.amount < BigDecimal.ZERO }
        
        val incomeBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = incomeTransactions,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val expenseBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = expenseTransactions,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        return IncomeExpensesBookData(
            incomeTotal = incomeBreakdown.totalIncome,
            expenseTotal = expenseBreakdown.totalExpense,
            incomeCount = incomeTransactions.size,
            expenseCount = expenseTransactions.size,
            currency = context.baseCurrency,
            precision = context.precision
        )
    }

    suspend fun computeCashFlowTableForCard(
        cardConfig: CashFlowTableCardConfig,
        allTransactions: List<Transaction>
    ): CashFlowTableData {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val context = getComputationContext()
        
        val incomeTransactions = filtered.filter { it.amount > BigDecimal.ZERO }
        val expenseTransactions = filtered.filter { it.amount < BigDecimal.ZERO }
        
        val incomeBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = incomeTransactions,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val expenseBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = expenseTransactions,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val netBreakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = filtered,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        // Calculate days in period
        val timeBuckets = TransactionsUtilityService.getTimeBuckets(cardConfig.filter.timelineRange, 1)
        val daysInPeriod = if (timeBuckets.isNotEmpty()) {
            ((timeBuckets.first().end - timeBuckets.first().start) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
        } else 30
        
        val incomeAvgDay = if (daysInPeriod > 0) incomeBreakdown.totalIncome / BigDecimal.valueOf(daysInPeriod.toLong()) else BigDecimal.ZERO
        val expenseAvgDay = if (daysInPeriod > 0) expenseBreakdown.totalExpense.abs() / BigDecimal.valueOf(daysInPeriod.toLong()) else BigDecimal.ZERO
        
        val incomeAvgRecord = if (incomeTransactions.isNotEmpty()) incomeBreakdown.totalIncome / BigDecimal.valueOf(incomeTransactions.size.toLong()) else BigDecimal.ZERO
        val expenseAvgRecord = if (expenseTransactions.isNotEmpty()) expenseBreakdown.totalExpense.abs() / BigDecimal.valueOf(expenseTransactions.size.toLong()) else BigDecimal.ZERO
        
        return CashFlowTableData(
            incomeCount = incomeTransactions.size,
            expenseCount = expenseTransactions.size,
            incomeAvgDay = incomeAvgDay.setScale(context.precision, RoundingMode.HALF_UP),
            expenseAvgDay = expenseAvgDay.setScale(context.precision, RoundingMode.HALF_UP),
            incomeAvgRecord = incomeAvgRecord.setScale(context.precision, RoundingMode.HALF_UP),
            expenseAvgRecord = expenseAvgRecord.setScale(context.precision, RoundingMode.HALF_UP),
            incomeTotal = incomeBreakdown.totalIncome.setScale(context.precision, RoundingMode.HALF_UP),
            expenseTotal = expenseBreakdown.totalExpense.setScale(context.precision, RoundingMode.HALF_UP),
            netCashFlow = netBreakdown.netFlow.setScale(context.precision, RoundingMode.HALF_UP),
            currency = context.baseCurrency,
            precision = context.precision
        )
    }

    suspend fun computePlannedPaymentsTimelineForCard(
        cardConfig: PlannedPaymentsTimelineCardConfig,
        allTransactions: List<Transaction>
    ): PlannedPaymentsTimelineData {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val context = getComputationContext()
        
        // Group expenses by week
        val expenseTransactions = filtered.filter { it.amount < BigDecimal.ZERO }
        val timeBuckets = TransactionsUtilityService.getTimeBuckets(cardConfig.filter.timelineRange, 4)
        
        val weeklyData = timeBuckets.mapIndexed { index, bucket ->
            val weekTransactions = expenseTransactions.filter { it.timestamp in bucket.start until bucket.end }
            val weekTotal = weekTransactions.sumOf { tx ->
                TransactionsUtilityService.calculateTotalInBaseCurrency(
                    mapOf(tx.currency to FinancialSummary(tx.amount.abs(), tx.amount, BigDecimal.ZERO, 1)),
                    context.baseCurrency,
                    context.exchangeRates
                ).totalAmount
            }
            "Week ${index + 1}" to weekTotal.setScale(context.precision, RoundingMode.HALF_UP)
        }
        
        val total = weeklyData.sumOf { it.second }.setScale(context.precision, RoundingMode.HALF_UP)
        
        return PlannedPaymentsTimelineData(
            total = total.setScale(context.precision, RoundingMode.HALF_UP),
            currency = context.baseCurrency,
            precision = context.precision,
            weeklyData = weeklyData
        )
    }

    suspend fun computeBalanceByAccountsForCard(
        cardConfig: BalanceByAccountsCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>
    ): BalanceByAccountsData {
        val context = getComputationContext()
        
        val accountBreakdown = TransactionsUtilityService.breakdownByAccount(
            transactions = allTransactions,
            accounts = accounts,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        
        val totalBalance = accountBreakdown.sumOf { it.totalAmount }
        
        val accountBalances = accountBreakdown.map { metric ->
            AccountBalanceItem(
                accountName = metric.key,
                balance = metric.totalAmount.setScale(context.precision, RoundingMode.HALF_UP),
                currency = context.baseCurrency,
                isNegative = metric.totalAmount < BigDecimal.ZERO
            )
        }
        
        return BalanceByAccountsData(
            totalBalance = totalBalance.setScale(context.precision, RoundingMode.HALF_UP),
            currency = context.baseCurrency,
            precision = context.precision,
            accountBalances = accountBalances
        )
    }

    suspend fun computeBalanceTrendForCard(
        cardConfig: BalanceTrendCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>
    ): BalanceTrendData {
        val context = getComputationContext()
        
        // Calculate current balance
        val accountBreakdown = TransactionsUtilityService.breakdownByAccount(
            transactions = allTransactions,
            accounts = accounts,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        val currentBalance = accountBreakdown.sumOf { it.totalAmount }
        
        // Calculate weekly balances over time
        val timeBuckets = TransactionsUtilityService.getTimeBuckets(cardConfig.filter.timelineRange, 8)
        val weeklyBalances = timeBuckets.map { bucket ->
            val bucketTransactions = allTransactions.filter { it.timestamp < bucket.end }
            val bucketBreakdown = TransactionsUtilityService.breakdownByAccount(
                transactions = bucketTransactions,
                accounts = accounts,
                exchangeRates = context.exchangeRates,
                baseCurrency = context.baseCurrency
            )
            bucketBreakdown.sumOf { it.totalAmount }.toDouble()
        }
        
        // Calculate percentage change
        val percentageChange = if (weeklyBalances.size >= 2 && weeklyBalances.first() != 0.0) {
            val first = weeklyBalances.first()
            val last = weeklyBalances.last()
            val change = ((last - first) / first * 100).toInt()
            if (change >= 0) "+${change}%" else "${change}%"
        } else "0%"
        
        return BalanceTrendData(
            currentBalance = currentBalance.setScale(context.precision, RoundingMode.HALF_UP),
            percentageChange = percentageChange,
            currency = context.baseCurrency,
            precision = context.precision,
            weeklyBalances = weeklyBalances
        )
    }

    // Simplified ViewModel compute methods using cache
    suspend fun computeCategoryBreakdownCard(config: CategoryBreakdownCardConfig): List<CategoryBreakdownData> {
        val cacheKey = "category_breakdown_${config.cardId}_${config.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computeCategoryBreakdownForCard(config, allTransactions)
        }
    }

    suspend fun computeTimelineTrajectoryCard(config: TimelineTrajectoryCardConfig): List<TimelineTrajectoryData> {
        val cacheKey = "timeline_trajectory_${config.cardId}_${config.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computeTimelineTrajectoryForCard(config, allTransactions, allAccounts)
        }
    }

    suspend fun computeCategoryExpenseTreemapCard(config: CategoryExpenseTreemapCardConfig): List<CategoryExpenseTreemapData> {
        val cacheKey = "category_treemap_${config.cardId}_${config.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computeCategoryExpenseTreemapForCard(config, allTransactions)
        }
    }

    suspend fun computeIncomeExpensesSummaryCard(cardConfig: IncomeExpensesSummaryCardConfig): IncomeExpensesSummaryData {
        val cacheKey = "income_expenses_summary_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computeIncomeExpensesSummaryForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computePeriodComparisonCard(cardConfig: PeriodComparisonCardConfig): PeriodComparisonData {
        val cacheKey = "period_comparison_${cardConfig.cardId}_${cardConfig.filter.hashCode()}_${cardConfig.selectedTab}"
        return cardDataCache.getOrCompute(cacheKey) {
            computePeriodComparisonForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeCashFlowTrendCard(cardConfig: CashFlowTrendCardConfig): CashFlowTrendData {
        val cacheKey = "cash_flow_trend_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computeCashFlowTrendForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeCashFlowProgressCard(cardConfig: CashFlowProgressCardConfig): CashFlowProgressData {
        val cacheKey = "cash_flow_progress_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computeCashFlowProgressForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeBalanceForecastCard(cardConfig: BalanceForecastCardConfig): BalanceForecastData {
        val cacheKey = "balance_forecast_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computeBalanceForecastForCard(cardConfig, allTransactions, allAccounts)
        }
    }

    suspend fun computePlannedPaymentsCard(cardConfig: PlannedPaymentsCardConfig): PlannedPaymentsData {
        val cacheKey = "planned_payments_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computePlannedPaymentsForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeIncomeExpensesBookCard(cardConfig: IncomeExpensesBookCardConfig): IncomeExpensesBookData {
        val cacheKey = "income_expenses_book_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computeIncomeExpensesBookForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeCashFlowTableCard(cardConfig: CashFlowTableCardConfig): CashFlowTableData {
        val cacheKey = "cash_flow_table_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computeCashFlowTableForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computePlannedPaymentsTimelineCard(cardConfig: PlannedPaymentsTimelineCardConfig): PlannedPaymentsTimelineData {
        val cacheKey = "planned_payments_timeline_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computePlannedPaymentsTimelineForCard(cardConfig, allTransactions)
        }
    }

    suspend fun computeBalanceByAccountsCard(cardConfig: BalanceByAccountsCardConfig): BalanceByAccountsData {
        val cacheKey = "balance_by_accounts_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
            computeBalanceByAccountsForCard(cardConfig, allTransactions, allAccounts)
        }
    }

    suspend fun computeBalanceTrendCard(cardConfig: BalanceTrendCardConfig): BalanceTrendData {
        val cacheKey = "balance_trend_${cardConfig.cardId}_${cardConfig.filter.hashCode()}"
        return cardDataCache.getOrCompute(cacheKey) {
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
        _analyticsCardsConfig.value = AnalyticsCardsConfig(newOrder)
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
