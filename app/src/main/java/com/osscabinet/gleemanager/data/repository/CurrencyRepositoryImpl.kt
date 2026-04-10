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

    override suspend fun getExchangeRates(baseCurrency: String): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val localRates = dao.getAllRates()
            val firstRate = localRates.firstOrNull()
            
            if (firstRate != null && isCacheValid(firstRate)) {
                if (firstRate.baseCurrency == baseCurrency) {
                    localRates.associate { it.currencyCode to it.rate }
                } else {
                    // Derive rates relative to the requested baseCurrency using math
                    // 1 StoredBase = R_new RequestedBase
                    // 1 StoredBase = R_any AnyCurrency
                    // => 1 RequestedBase = R_any / R_new AnyCurrency
                    val ratesInStoredBase = localRates.associate { it.currencyCode to it.rate }
                    val storedBaseToNewBaseRate = ratesInStoredBase[baseCurrency]
                    
                    if (storedBaseToNewBaseRate != null && storedBaseToNewBaseRate != 0.0) {
                        ratesInStoredBase.mapValues { it.value / storedBaseToNewBaseRate }
                    } else {
                        // If we can't derive (e.g. requested base not in cache), fetch new
                        updateRates(baseCurrency)
                    }
                }
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
