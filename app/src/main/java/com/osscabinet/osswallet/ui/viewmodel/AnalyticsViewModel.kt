package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.usecase.GetExchangeRatesUseCase
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class AnalyticsViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getExchangeRatesUseCase: GetExchangeRatesUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    val exchangeRates: StateFlow<Map<String, Double>> = _uiState
        .map { it.exchangeRates }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private var allTransactions: List<Transaction> = emptyList()
    private var ratesJob: Job? = null

    init {
        viewModelScope.launch {
            // Observe currency from settings to load initial rates and react to changes
            settingsDataStore.currency.collect { currencyCode ->
                // This ensures we start with the correct currency and update if it changes in settings
                onCurrencySelected(currencyCode)
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
            _uiState.update { it.copy(isLoading = true) }
            val rates = getExchangeRatesUseCase(currencyCode)
            _uiState.update { it.copy(
                selectedCurrency = currencyCode,
                exchangeRates = rates,
                isLoading = false
            ) }
            calculateAnalytics()
        }
    }

    private fun calculateAnalytics() {
        val selectedCurrency = _uiState.value.selectedCurrency
        val currentRates = _uiState.value.exchangeRates
        
        // Convert all transactions to selected currency
        val convertedTransactions = allTransactions.map { tx ->
            val amountInBase = if (tx.currency == selectedCurrency) {
                tx.amount 
            } else {
                val rate = currentRates[tx.currency] ?: 1.0
                if (rate != 0.0) tx.amount / rate else tx.amount
            }
            tx.copy(amount = amountInBase)
        }

        val totalIncome = convertedTransactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = convertedTransactions.filter { !it.isIncome }.sumOf { it.amount }
        val currentBalance = totalIncome - totalExpense
        
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

        val thisMonthIncome = thisMonthTransactions.filter { it.isIncome }.sumOf { it.amount }
        val thisMonthExpense = thisMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }
        val thisMonthSavingsRate = if (thisMonthIncome > 0) (thisMonthIncome - thisMonthExpense) / thisMonthIncome else 0.0

        val lastMonthIncome = lastMonthTransactions.filter { it.isIncome }.sumOf { it.amount }
        val lastMonthExpense = lastMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }
        val lastMonthSavingsRate = if (lastMonthIncome > 0) (lastMonthIncome - lastMonthExpense) / lastMonthIncome else 0.0
        
        // Burn Rate = Monthly Expense Average
        val firstTx = convertedTransactions.minByOrNull { it.timestamp }
        val daysActive = if (firstTx != null) {
            val diff = System.currentTimeMillis() - firstTx.timestamp
            (diff / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
        } else 1
        val dailyExpenseAvg = totalExpense / daysActive
        val monthlyBurnRate = dailyExpenseAvg * 30

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
                entry.value.sumOf { if (it.isIncome) it.amount else -it.amount }
            }

        val avgDailyNetFlow = if (dailyNetFlow.isNotEmpty()) {
            dailyNetFlow.values.average()
        } else 0.0

        // Forecast for next 30 days
        val forecast = (1..30).map { day ->
            val timestamp = System.currentTimeMillis() + day * 24 * 60 * 60 * 1000L
            val projectedBalance = currentBalance + (avgDailyNetFlow * day)
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
                isLoading = false
            )
        }
    }
}

data class AnalyticsUiState(
    val currentBalance: Double = 0.0,
    val savingsRate: Double = 0.0,
    val lastMonthSavingsRate: Double = 0.0,
    val burnRate: Double = 0.0,
    val thisMonthSpending: Double = 0.0,
    val lastMonthSpending: Double = 0.0,
    val dailyNetFlow: Map<Long, Double> = emptyMap(),
    val forecast: Map<Long, Double> = emptyMap(),
    val selectedCurrency: String = "USD",
    val exchangeRates: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false
)
