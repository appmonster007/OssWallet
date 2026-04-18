package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.usecase.GetExchangeRatesUseCase
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.ui.util.convertAmount
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class AnalyticsViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getExchangeRatesUseCase: GetExchangeRatesUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    val exchangeRates: StateFlow<Map<String, BigDecimal>> = _uiState
        .map { it.exchangeRates }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private var allTransactions: List<Transaction> = emptyList()
    private var ratesJob: Job? = null
    val decimalPrecision: StateFlow<Int> = settingsDataStore.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)
    private var _currentPrecision: Int = 2

    init {
        viewModelScope.launch {
            // Observe currency from settings to load initial rates and react to changes
            settingsDataStore.currency.collect { currencyCode ->
                // This ensures we start with the correct currency and update if it changes in settings
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
            getTransactionsUseCase().collect { transactions ->
                allTransactions = transactions
                calculateAnalytics()
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
            val rates = getExchangeRatesUseCase(currencyCode).mapValues { BigDecimal.valueOf(it.value) }
            _uiState.update { it.copy(
                selectedCurrency = currencyCode,
                exchangeRates = rates,
                isLoading = false,
                hasLoadedExchangeRates = true
            ) }
            calculateAnalytics()
        }
    }

    private fun calculateAnalytics() {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        // Convert all transactions to selected currency
        val convertedTransactions = allTransactions.map { tx ->
            val convertedAmount = convertAmount(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = selectedCurrency,
                exchangeRates = currentRates,
                precision = _currentPrecision + 4 // Use higher internal precision
            )
            tx.copy(amount = convertedAmount)
        }

        val totalIncome = convertedTransactions.filter { it.isIncome }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.setScale(_currentPrecision, RoundingMode.HALF_UP)
        val totalExpense = convertedTransactions.filter { !it.isIncome }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }.setScale(_currentPrecision, RoundingMode.HALF_UP)
        val currentBalance = totalIncome.subtract(totalExpense).setScale(_currentPrecision, RoundingMode.HALF_UP)
        
        // Month-over-month analysis
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

        val thisMonthIncome = thisMonthTransactions.filter { it.isIncome }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val thisMonthExpense = thisMonthTransactions.filter { !it.isIncome }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val thisMonthSavingsRate = if (thisMonthIncome.signum() > 0) {
            thisMonthIncome.subtract(thisMonthExpense).divide(thisMonthIncome, 4, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        val lastMonthIncome = lastMonthTransactions.filter { it.isIncome }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val lastMonthExpense = lastMonthTransactions.filter { !it.isIncome }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val lastMonthSavingsRate = if (lastMonthIncome.signum() > 0) {
            lastMonthIncome.subtract(lastMonthExpense).divide(lastMonthIncome, 4, RoundingMode.HALF_UP).toDouble()
        } else 0.0
        
        // Burn Rate = Monthly Expense Average
        val firstTx = convertedTransactions.minByOrNull { it.timestamp }
        val daysActive = if (firstTx != null) {
            val diff = System.currentTimeMillis() - firstTx.timestamp
            (diff / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
        } else 1
        val dailyExpenseAvg = totalExpense.divide(BigDecimal.valueOf(daysActive), _currentPrecision, RoundingMode.HALF_UP)
        val monthlyBurnRate = dailyExpenseAvg.multiply(BigDecimal.valueOf(30))

        // Group by day for cash flow (last 7 days)
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
                    if (tx.isIncome) acc.add(tx.amount) else acc.subtract(tx.amount)
                }
            }

        val avgDailyNetFlow = if (dailyNetFlow.isNotEmpty()) {
            dailyNetFlow.values.fold(BigDecimal.ZERO) { acc, bd -> acc.add(bd) }
                .divide(BigDecimal.valueOf(dailyNetFlow.size.toLong()), _currentPrecision, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        // Forecast for next 30 days
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
