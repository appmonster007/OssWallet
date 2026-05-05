package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity
import com.osscabinet.ossWallet.data.remote.CurrencyResponse
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CurrencyViewModel(
    private val currencyRepository: CurrencyRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    private var autoSave: Boolean = true

    init {
        viewModelScope.launch {
            settingsRepository.currency.collect { base ->
                loadRates(base)
            }
        }
        viewModelScope.launch {
            settingsRepository.autoSaveExchangeRates.collect { auto ->
                autoSave = auto
            }
        }
        viewModelScope.launch {
            settingsRepository.use24HourFormat.collect { use24Hour ->
                _uiState.update { it.copy(use24HourFormat = use24Hour) }
            }
        }
    }

    private fun loadRates(base: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, baseCurrency = base) }
            try {
                // CurrencyRepository doesn't return Flow of rates yet, using getExchangeRates
                val ratesMap = currencyRepository.getExchangeRates(base, false)
                val entities = ratesMap.map { (code, rate) ->
                    CurrencyRateEntity(
                        baseCurrency = base,
                        currencyCode = code,
                        rate = rate,
                        timestamp = System.currentTimeMillis(),
                        nextUpdate = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // Placeholder
                    )
                }
                _uiState.update { it.copy(rates = entities, lastUpdated = System.currentTimeMillis(), isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(syncError = e.message, isLoading = false) }
            }
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            val base = _uiState.value.baseCurrency
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = currencyRepository.fetchFreshRates(base)
                if (autoSave) {
                    currencyRepository.saveRates(base, response)
                    loadRates(base)
                } else {
                    _uiState.update { it.copy(pendingResponse = response, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(syncError = e.message, isLoading = false) }
            }
        }
    }

    fun confirmSaveRates() {
        viewModelScope.launch {
            val base = _uiState.value.baseCurrency
            val response = _uiState.value.pendingResponse ?: return@launch
            currencyRepository.saveRates(base, response)
            _uiState.update { it.copy(pendingResponse = null) }
            loadRates(base)
        }
    }

    fun cancelSaveRates() {
        _uiState.update { it.copy(pendingResponse = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(syncError = null) }
    }

    fun convert(amount: Double, from: String, to: String, callback: (Double) -> Unit) {
        viewModelScope.launch {
            callback(currencyRepository.convert(amount, from, to))
        }
    }
}

data class CurrencyUiState(
    val rates: List<CurrencyRateEntity> = emptyList(),
    val baseCurrency: String = "",
    val lastUpdated: Long = 0,
    val isLoading: Boolean = false,
    val syncError: String? = null,
    val pendingResponse: CurrencyResponse? = null,
    val use24HourFormat: Boolean = false
)
