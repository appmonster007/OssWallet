package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.CurrencyDao
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity
import com.osscabinet.ossWallet.domain.model.currency.CurrencyRates
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val providerRegistry: ExchangeRateProviderRegistry,
    private val dao: CurrencyDao,
    private val settingsRepository: SettingsRepository
) : CurrencyRepository {

    override val ratesFlow: Flow<Map<String, BigDecimal>> =
        dao.observeAllRates().map { entities ->
            entities.associate { it.currencyCode to it.rate.toBigDecimal() }
        }

    override suspend fun fetchFreshRates(baseCurrency: String): CurrencyRates {
        return withContext(Dispatchers.IO) {
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
        dao.replaceRates(entities)
    }

    override suspend fun getExchangeRates(
        baseCurrency: String,
        forceRefresh: Boolean
    ): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val localRates = dao.getAllRates()

            // If cache is valid and matches our base currency, use it
            if (!forceRefresh && localRates.isNotEmpty() && isCacheValid(localRates.first())) {
                val rates = localRates.associate { it.currencyCode to it.rate.toDouble() }

                if (localRates.first().baseCurrency == baseCurrency) {
                    return@withContext rates
                }

                val newBaseRate = rates[baseCurrency]
                if (newBaseRate != null && newBaseRate != 0.0) {
                    return@withContext rates.mapValues { it.value / newBaseRate }
                }
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
                        nextUpdate = System.currentTimeMillis() + 24 * 60 * 60 * 1000L // 24h fallback
                    )
                }
                dao.replaceRates(entities)
                return@withContext rates
            }

            localRates.associate { it.currencyCode to it.rate.toDouble() }
        }
    }

    override suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val providerId = settingsRepository.currencyProvider.first()
        val provider = providerRegistry.getProvider(providerId)

        val result = provider.convertCurrencyAmount(amount, fromCurrency, toCurrency)
        return result.getOrElse {
            // Fallback to local conversion if provider fails
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
