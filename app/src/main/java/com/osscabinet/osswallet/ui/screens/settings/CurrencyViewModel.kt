package com.osscabinet.ossWallet.ui.screens.settings

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.currency.CurrencyRate
import com.osscabinet.ossWallet.domain.model.currency.CurrencyRates
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

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
    }

    private fun loadRates(base: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, baseCurrency = base) }
            try {
                // CurrencyRepository doesn't return Flow of rates yet, using getExchangeRates
                val ratesMap = currencyRepository.getExchangeRates(base, false)
                val now = System.currentTimeMillis()
                val entities = ratesMap.map { (code, rate) ->
                    CurrencyRate(
                        baseCurrency = base,
                        currencyCode = code,
                        rate = rate,
                        timestamp = now,
                        nextUpdate = now + 24 * 60 * 60 * 1000L // 24h fallback
                    )
                }
                _uiState.update { it.copy(rates = entities, lastUpdated = now, isLoading = false) }
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
                val rates = currencyRepository.fetchFreshRates(base)
                if (autoSave) {
                    currencyRepository.saveRates(base, rates)
                    loadRates(base)
                } else {
                    _uiState.update { it.copy(pendingRates = rates, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(syncError = e.message, isLoading = false) }
            }
        }
    }

    fun confirmSaveRates() {
        viewModelScope.launch {
            val base = _uiState.value.baseCurrency
            val rates = _uiState.value.pendingRates ?: return@launch
            currencyRepository.saveRates(base, rates)
            _uiState.update { it.copy(pendingRates = null) }
            loadRates(base)
        }
    }

    fun cancelSaveRates() {
        _uiState.update { it.copy(pendingRates = null) }
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
    val rates: List<CurrencyRate> = emptyList(),
    val baseCurrency: String = "",
    val lastUpdated: Long = 0,
    val isLoading: Boolean = false,
    val syncError: String? = null,
    val pendingRates: CurrencyRates? = null
)
