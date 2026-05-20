package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComputationRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val currencyRepository: CurrencyRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ComputationRepository {

    private val _exchangeRates = MutableStateFlow<Map<String, BigDecimal>>(emptyMap())
    override val exchangeRates: Flow<Map<String, BigDecimal>> = _exchangeRates.asStateFlow()

    private val _hasLoadedExchangeRates = MutableStateFlow(false)
    override val hasLoadedExchangeRates: Flow<Boolean> = _hasLoadedExchangeRates.asStateFlow()

    override val selectedCurrency: Flow<String> = settingsRepository.currency

    override val computationContext: Flow<ComputationContext?> = combine(
        selectedCurrency,
        exchangeRates
    ) { currency, rates ->
        if (rates.isEmpty()) null
        else ComputationContext(currency, rates)
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
            .launchIn(applicationScope)
    }
}
