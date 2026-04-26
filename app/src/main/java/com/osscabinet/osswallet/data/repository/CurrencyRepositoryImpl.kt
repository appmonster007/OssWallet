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

    override suspend fun fetchFreshRates(baseCurrency: String): com.osscabinet.ossWallet.data.remote.CurrencyResponse {
        return withContext(Dispatchers.IO) {
            api.getLatestRates(baseCurrency)
        }
    }

    override suspend fun saveRates(baseCurrency: String, response: com.osscabinet.ossWallet.data.remote.CurrencyResponse) {
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
        dao.replaceRates(entities)
    }

    override suspend fun getExchangeRates(baseCurrency: String, forceRefresh: Boolean): Map<String, Double> {
        return withContext(Dispatchers.IO) {
            val localRates = dao.getAllRates()
            
            // If cache is valid and matches our base currency, use it
            if (!forceRefresh && localRates.isNotEmpty() && isCacheValid(localRates.first())) {
                val rates = localRates.associate { it.currencyCode to it.rate }
                
                // If it's already in the correct base, return it
                if (localRates.first().baseCurrency == baseCurrency) {
                    return@withContext rates
                }
                
                // If it's in a different base, we can derive the cross-rates
                // Rate(X to B) = Rate(X to A) / Rate(B to A)
                val newBaseRate = rates[baseCurrency]
                if (newBaseRate != null && newBaseRate != 0.0) {
                    return@withContext rates.mapValues { it.value / newBaseRate }
                }
            }
            
            // Otherwise try to fetch fresh rates
            val freshRates = updateRates(baseCurrency)
            if (freshRates.isNotEmpty()) {
                return@withContext freshRates
            }
            
            // If fetch fails but we have old rates, return what we have
            localRates.associate { it.currencyCode to it.rate }
        }
    }

    override suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        // Get rates relative to any stable base (the cache will handle derivation or fetching)
        val rates = getExchangeRates(fromCurrency)
        val toRate = rates[toCurrency] ?: 1.0
        return amount * toRate
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
                
                // Transactional update: delete old and insert new
                // This ensures we don't have partial data if one step fails
                dao.deleteAllRates()
                dao.insertRates(entities)
                
                response.rates
            } else {
                // Log provider-specific error result using proper logging
                dao.getAllRates().associate { it.currencyCode to it.rate }
            }
        } catch (e: java.net.UnknownHostException) {
                // Use proper logging for sync failures
            dao.getAllRates().associate { it.currencyCode to it.rate }
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val message = when(code) {
                429 -> "Rate limit exceeded (HTTP 429)"
                403 -> "Invalid API Key or forbidden (HTTP 403)"
                404 -> "Base currency not supported (HTTP 404)"
                else -> "HTTP Error $code"
            }
            // Use proper logging for sync failures
            dao.getAllRates().associate { it.currencyCode to it.rate }
        } catch (e: Exception) {
            // Use proper logging for sync failures
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
