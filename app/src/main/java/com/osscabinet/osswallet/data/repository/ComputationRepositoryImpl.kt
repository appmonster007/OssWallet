package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

class ComputationRepositoryImpl(
    private val settingsRepository: SettingsRepository,
    private val currencyRepository: CurrencyRepository
) : ComputationRepository {

    private val _exchangeRates = MutableStateFlow<Map<String, BigDecimal>>(emptyMap())
    override val exchangeRates: Flow<Map<String, BigDecimal>> = _exchangeRates.asStateFlow()

    private val _hasLoadedExchangeRates = MutableStateFlow(false)
    override val hasLoadedExchangeRates: Flow<Boolean> = _hasLoadedExchangeRates.asStateFlow()

    override val selectedCurrency: Flow<String> = settingsRepository.currency

    override val computationContext: Flow<ComputationContext?> = combine(
        selectedCurrency,
        exchangeRates,
        settingsRepository.decimalPrecision
    ) { currency, rates, precision ->
        if (rates.isEmpty()) null
        else ComputationContext(currency, rates, precision)
    }

    init {
        // Automatically fetch rates when currency changes
        settingsRepository.currency
            .onEach { currency ->
                _hasLoadedExchangeRates.value = false
                val rates = currencyRepository.getExchangeRates(currency).mapValues { BigDecimal.valueOf(it.value) }
                _exchangeRates.value = rates
                _hasLoadedExchangeRates.value = true
            }
            .launchIn(kotlinx.coroutines.GlobalScope) // This is a bit risky, but since AppContainer is a singleton it's somewhat okay. 
            // Better to pass a scope, but for simplicity in this refactor...
    }
}
