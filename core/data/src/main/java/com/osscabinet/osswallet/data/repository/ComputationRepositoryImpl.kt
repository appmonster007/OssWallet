package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.di.qualifiers.ApplicationScope
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.RateRefreshError
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.math.BigDecimal
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ComputationRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val currencyRepository: CurrencyRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ComputationRepository {

    override val selectedCurrency: Flow<String> = settingsRepository.currency

    @OptIn(ExperimentalCoroutinesApi::class)
    override val exchangeRatesSnapshot: Flow<ExchangeRateSnapshot> =
        selectedCurrency
            .distinctUntilChanged()
            .flatMapLatest { base ->
                // ARC-01/02: rates are stored per-base (composite PK) and the flow is
                // anchored to that base. No client-side rebasing, no anchor mismatch.
                currencyRepository.observeRatesSnapshot(base)
            }

    override val exchangeRates: Flow<Map<String, BigDecimal>> =
        exchangeRatesSnapshot.map { it.rates }

    override val hasLoadedExchangeRates: Flow<Boolean> =
        exchangeRatesSnapshot.map { it.rates.isNotEmpty() }

    override val computationContext: Flow<ComputationContext?> =
        exchangeRatesSnapshot.map { snapshot ->
            if (snapshot.rates.isEmpty()) null
            else ComputationContext(snapshot.anchor, snapshot.rates)
        }

    private val _rateRefreshErrors = MutableSharedFlow<RateRefreshError>(
        replay = 0,
        extraBufferCapacity = 4
    )
    override val rateRefreshErrors: Flow<RateRefreshError> = _rateRefreshErrors.asSharedFlow()

    init {
        // ARC-04: on currency change, fetch rates and surface the failure if it happens.
        // distinctUntilChanged prevents re-fetching the same base on unrelated emissions.
        settingsRepository.currency
            .distinctUntilChanged()
            .onEach { currency ->
                runCatching { currencyRepository.getExchangeRates(currency) }
                    .onFailure { _rateRefreshErrors.tryEmit(RateRefreshError(currency, it)) }
            }
            .launchIn(applicationScope)
    }
}
