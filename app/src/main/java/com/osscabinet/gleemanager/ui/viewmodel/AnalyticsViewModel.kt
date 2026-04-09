package com.osscabinet.gleemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.domain.usecase.GetExchangeRatesUseCase
import com.osscabinet.gleemanager.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class AnalyticsViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getExchangeRatesUseCase: GetExchangeRatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()
    private var exchangeRates: Map<String, Double> = emptyMap()

    init {
        viewModelScope.launch {
            // Load initial exchange rates (default USD)
            exchangeRates = getExchangeRatesUseCase("USD")
            
            getTransactionsUseCase().collect { transactions ->
                allTransactions = transactions
                calculateAnalytics()
            }
        }
    }

    fun onCurrencySelected(currencyCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            exchangeRates = getExchangeRatesUseCase(currencyCode)
            _uiState.update { it.copy(selectedCurrency = currencyCode, isLoading = false) }
            calculateAnalytics()
        }
    }

    private fun calculateAnalytics() {
        val selectedCurrency = _uiState.value.selectedCurrency
        
        // Convert all transactions to selected currency
        // Note: For simplicity, we use the current rate for all historical data
        // In a real app, you'd want historical rates, but for this task, current is fine.
        val convertedTransactions = allTransactions.map { tx ->
            val rate = if (tx.currency == selectedCurrency) 1.0 
                      else (exchangeRates[selectedCurrency] ?: 1.0) / (exchangeRates[tx.currency] ?: 1.0)
            tx.copy(amount = tx.amount * rate)
        }

        val currentBalance = convertedTransactions.sumOf { if (it.isIncome) it.amount else -it.amount }
        
        // Group by day for cash flow (last 7 days)
        val calendar = Calendar.getInstance()
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
                dailyNetFlow = dailyNetFlow,
                forecast = forecast.toMap(),
                isLoading = false
            )
        }
    }
}

data class AnalyticsUiState(
    val currentBalance: Double = 0.0,
    val dailyNetFlow: Map<Long, Double> = emptyMap(),
    val forecast: Map<Long, Double> = emptyMap(),
    val selectedCurrency: String = "USD",
    val isLoading: Boolean = false
)
