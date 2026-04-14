package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.CurrencyDao
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity
import com.osscabinet.ossWallet.data.remote.CurrencyApi
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CurrencyRepositoryImpl(
    private val api: CurrencyApi,
    private val dao: CurrencyDao
) : CurrencyRepository {

    override suspend fun getExchangeRates(baseCurrency: String, forceRefresh: Boolean): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val localRates = dao.getAllRates()
            
            // If cache is valid and matches our base currency, use it
            if (!forceRefresh && localRates.isNotEmpty() && isCacheValid(localRates.first()) && localRates.first().baseCurrency == baseCurrency) {
                return@withContext localRates.associate { it.currencyCode to it.rate }
            }
            
            // Otherwise try to fetch fresh rates
            val freshRates = updateRates(baseCurrency)
            if (freshRates.isNotEmpty()) {
                return@withContext freshRates
            }
            
            // If fetch fails but we have old rates, we can try to "re-base" them if needed
            // But for now, returning what we have or empty
            localRates.associate { it.currencyCode to it.rate }
        }
    }

    override suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        return withContext(Dispatchers.IO) {
            // Get rates relative to any stable base (the cache will handle derivation or fetching)
            val rates = getExchangeRates(fromCurrency)
            val toRate = rates[toCurrency] ?: 1.0
            amount * toRate
        }
    }

    private suspend fun updateRates(baseCurrency: String): Map<String, Double> {
        return try {
            val response = api.getLatestRates(baseCurrency)
            if (response.result == "success") {
                val nextUpdateMillis = (response.timeNextUpdateUnix ?: 0L) * 1000L
                val entities = response.rates.map {
                    CurrencyRateEntity(
                        currencyCode = it.key,
                        baseCurrency = baseCurrency,
                        rate = it.value,
                        timestamp = System.currentTimeMillis(),
                        nextUpdate = nextUpdateMillis
                    )
                }
                dao.deleteAllRates()
                dao.insertRates(entities)
                response.rates
            } else {
                dao.getAllRates().associate { it.currencyCode to it.rate }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            dao.getAllRates().associate { it.currencyCode to it.rate }
        }
    }

    private fun isCacheValid(entity: CurrencyRateEntity): Boolean {
        val currentTime = System.currentTimeMillis()
        // Use the provider's next update time if available, otherwise 24h fallback
        return if (entity.nextUpdate > 0) {
            currentTime < entity.nextUpdate
        } else {
            val oneDayMillis = 24 * 60 * 60 * 1000L
            currentTime - entity.timestamp < oneDayMillis
        }
    }
}
