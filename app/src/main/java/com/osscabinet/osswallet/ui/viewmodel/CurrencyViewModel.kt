package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.CurrencyDao
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CurrencyViewModel(
    private val repository: CurrencyRepository,
    private val dao: CurrencyDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    private var autoSave: Boolean = true

    init {
        viewModelScope.launch {
            settingsDataStore.autoSaveExchangeRates.collect {
                autoSave = it
            }
        }
        viewModelScope.launch {
            settingsDataStore.currency.collect { base ->
                _uiState.update { it.copy(baseCurrency = base) }
                loadRates(base)
            }
        }
    }

    fun loadRates(baseCurrency: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, syncError = null) }
            try {
                repository.getExchangeRates(baseCurrency)
                val rates = dao.getAllRates()
                _uiState.update {
                    it.copy(
                        rates = rates,
                        lastUpdated = rates.firstOrNull()?.timestamp ?: 0L,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, syncError = "Sync Failed: ${e.localizedMessage}") }
            }
        }
    }

    fun refreshRates() {
        val baseCurrency = _uiState.value.baseCurrency
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, syncError = null) }
            try {
                val response = repository.fetchFreshRates(baseCurrency)
                if (autoSave) {
                    repository.saveRates(baseCurrency, response)
                    val rates = dao.getAllRates()
                    _uiState.update {
                        it.copy(
                            rates = rates,
                            lastUpdated = rates.firstOrNull()?.timestamp ?: 0L,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(pendingResponse = response, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, syncError = "Refresh Failed: ${e.localizedMessage}") }
            }
        }
    }

    fun confirmSaveRates() {
        val response = _uiState.value.pendingResponse ?: return
        val baseCurrency = _uiState.value.baseCurrency
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.saveRates(baseCurrency, response)
                val rates = dao.getAllRates()
                _uiState.update {
                    it.copy(
                        rates = rates,
                        lastUpdated = rates.firstOrNull()?.timestamp ?: 0L,
                        pendingResponse = null,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, syncError = "Save Failed") }
            }
        }
    }

    fun cancelSaveRates() {
        _uiState.update { it.copy(pendingResponse = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(syncError = null) }
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
    val isLoading: Boolean = false,
    val syncError: String? = null,
    val pendingResponse: com.osscabinet.ossWallet.data.remote.CurrencyResponse? = null
)
