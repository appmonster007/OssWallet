package com.osscabinet.gleemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.gleemanager.data.local.SettingsDataStore
import com.osscabinet.gleemanager.data.local.dao.CurrencyDao
import com.osscabinet.gleemanager.data.local.entity.CurrencyRateEntity
import com.osscabinet.gleemanager.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CurrencyViewModel(
    private val repository: CurrencyRepository,
    private val dao: CurrencyDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.currency.collect { base ->
                _uiState.update { it.copy(baseCurrency = base) }
                loadRates(base)
            }
        }
    }

    fun loadRates(baseCurrency: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getExchangeRates(baseCurrency)
            val rates = dao.getAllRates()
            _uiState.update {
                it.copy(
                    rates = rates,
                    lastUpdated = rates.firstOrNull()?.timestamp ?: 0L,
                    isLoading = false
                )
            }
        }
    }

    fun refreshRates() {
        val baseCurrency = _uiState.value.baseCurrency
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getExchangeRates(baseCurrency, forceRefresh = true)
            val rates = dao.getAllRates()
            _uiState.update {
                it.copy(
                    rates = rates,
                    lastUpdated = rates.firstOrNull()?.timestamp ?: 0L,
                    isLoading = false
                )
            }
        }
    }

    fun convert(amount: Double, from: String, to: String): Double {
        val rates = _uiState.value.rates
        val fromRate = rates.find { it.currencyCode == from }?.rate ?: 1.0
        val toRate = rates.find { it.currencyCode == to }?.rate ?: 1.0
        
        return if (fromRate != 0.0) (amount / fromRate) * toRate else 0.0
    }
}

data class CurrencyUiState(
    val rates: List<CurrencyRateEntity> = emptyList(),
    val baseCurrency: String = "USD",
    val lastUpdated: Long = 0L,
    val isLoading: Boolean = false
)
