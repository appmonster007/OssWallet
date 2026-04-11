package com.osscabinet.gleemanager.data.repository

import com.osscabinet.gleemanager.data.local.dao.CurrencyDao
import com.osscabinet.gleemanager.data.local.entity.CurrencyRateEntity
import com.osscabinet.gleemanager.data.remote.CurrencyApi
import com.osscabinet.gleemanager.domain.repository.CurrencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CurrencyRepositoryImpl(
    private val api: CurrencyApi,
    private val dao: CurrencyDao
) : CurrencyRepository {

    override suspend fun getExchangeRates(baseCurrency: String, forceRefresh: Boolean): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val localRates = dao.getAllRates()
            val firstRate = localRates.firstOrNull()
            
            if (!forceRefresh && firstRate != null && isCacheValid(firstRate) && firstRate.baseCurrency == baseCurrency) {
                localRates.associate { it.currencyCode to it.rate }
            } else {
                updateRates(baseCurrency)
            }
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
                val nextUpdateMillis = response.timeNextUpdateUnix * 1000L
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
