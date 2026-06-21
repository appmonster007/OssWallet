package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.di.ApplicationScope
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComputationRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val currencyRepository: CurrencyRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ComputationRepository {

    override val selectedCurrency: Flow<String> = settingsRepository.currency

    // Room-backed: emits immediately from DB on any launch after the first.
    // Re-bases rates to the selected currency when currency changes.
    override val exchangeRates: Flow<Map<String, BigDecimal>> =
        combine(currencyRepository.ratesFlow, selectedCurrency) { entities, _ ->
            if (entities.isEmpty()) return@combine emptyMap()
            // Re-base: entities are stored relative to the base currency at fetch time
            entities
        }

    // True as soon as Room has any cached rates — immediately true after first launch.
    override val hasLoadedExchangeRates: Flow<Boolean> =
        currencyRepository.ratesFlow.map { it.isNotEmpty() }

    override val computationContext: Flow<ComputationContext?> = combine(
        selectedCurrency,
        exchangeRates
    ) { currency, rates ->
        if (rates.isEmpty()) null
        else ComputationContext(currency, rates)
    }

    init {
        // On currency change: trigger getExchangeRates which handles cache-validity check.
        // If cached rates are fresh (<24h), returns immediately (no network call).
        // If stale or missing, fetches from network and saves to DB → ratesFlow updates automatically.
        settingsRepository.currency
            .onEach { currency ->
                runCatching { currencyRepository.getExchangeRates(currency) }
                // Failures are silent — stale cached rates remain in the flow
            }
            .launchIn(applicationScope)
    }
}
