package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.core.util.DispatcherProvider
import com.osscabinet.ossWallet.data.local.dao.CurrencyDao
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity
import com.osscabinet.ossWallet.domain.model.currency.CurrencyRates
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProviderRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val providerRegistry: ExchangeRateProviderRegistry,
    private val dao: CurrencyDao,
    private val settingsRepository: SettingsRepository,
    private val dispatcherProvider: DispatcherProvider
) : CurrencyRepository {

    override fun observeRatesSnapshot(baseCurrency: String): Flow<ExchangeRateSnapshot> =
        dao.observeRatesForBase(baseCurrency).map { rows ->
            ExchangeRateSnapshot(
                anchor = baseCurrency,
                rates = rows.associate { it.currencyCode to it.rate.toBigDecimalOrNull().orZero() }
            )
        }

    @Suppress("OVERRIDE_DEPRECATION")
    override val ratesFlow: Flow<Map<String, BigDecimal>> =
        dao.observeRatesForBase("").map { it.associate { row -> row.currencyCode to row.rate.toBigDecimalOrNull().orZero() } }

    private fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO

    override suspend fun fetchFreshRates(baseCurrency: String): CurrencyRates {
        return withContext(dispatcherProvider.io) {
            val providerId = settingsRepository.currencyProvider.first()
            val provider = providerRegistry.getProvider(providerId)

            val result = provider.fetchAllRates(baseCurrency)
            val rates = result.getOrThrow()

            CurrencyRates(
                baseCurrency = baseCurrency,
                rates = rates
            )
        }
    }

    override suspend fun saveRates(baseCurrency: String, rates: CurrencyRates) {
        val nextUpdateMillis = rates.nextUpdate ?: 0L
        val entities = rates.rates.map {
            CurrencyRateEntity(
                currencyCode = it.key,
                baseCurrency = baseCurrency,
                rate = it.value.toBigDecimal().toPlainString(),
                timestamp = rates.lastUpdated,
                nextUpdate = nextUpdateMillis
            )
        }
        // ARC-09: scoped delete-then-insert via the transactional helper.
        dao.replaceRatesForBase(baseCurrency, entities)
    }

    override suspend fun getExchangeRates(
        baseCurrency: String,
        forceRefresh: Boolean
    ): Map<String, Double> {
        return withContext(dispatcherProvider.io) {
            val localRates = dao.getRatesForBase(baseCurrency)

            if (!forceRefresh && localRates.isNotEmpty() && isCacheValid(localRates.first())) {
                return@withContext localRates.associate { it.currencyCode to it.rate.toDouble() }
            }

            val providerId = settingsRepository.currencyProvider.first()
            val provider = providerRegistry.getProvider(providerId)

            val result = provider.fetchAllRates(baseCurrency)
            if (result.isSuccess) {
                val rates = result.getOrThrow()
                val entities = rates.map {
                    CurrencyRateEntity(
                        currencyCode = it.key,
                        baseCurrency = baseCurrency,
                        rate = it.value.toBigDecimal().toPlainString(),
                        timestamp = System.currentTimeMillis(),
                        nextUpdate = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                    )
                }
                dao.replaceRatesForBase(baseCurrency, entities)
                return@withContext rates
            }

            // Network failed — return whatever's cached so the UI degrades gracefully.
            // The caller (ComputationRepositoryImpl) is responsible for surfacing the
            // failure via its error flow (ARC-04).
            localRates.associate { it.currencyCode to it.rate.toDouble() }
        }
    }

    override suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val providerId = settingsRepository.currencyProvider.first()
        val provider = providerRegistry.getProvider(providerId)

        val result = provider.convertCurrencyAmount(amount, fromCurrency, toCurrency)
        return result.getOrElse {
            val rates = getExchangeRates(fromCurrency)
            val toRate = rates[toCurrency] ?: 1.0
            amount * toRate
        }
    }

    private fun isCacheValid(entity: CurrencyRateEntity): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (entity.nextUpdate > 0) {
            currentTime < entity.nextUpdate
        } else {
            val oneDayMillis = 24 * 60 * 60 * 1000L
            currentTime - entity.timestamp < oneDayMillis
        }
    }
}
