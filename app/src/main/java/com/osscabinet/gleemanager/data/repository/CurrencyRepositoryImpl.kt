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
            if (localRates.isNotEmpty() && isCacheValid(localRates.first().timestamp)) {
                localRates.associate { it.currencyCode to it.rate }
            } else {
                try {
                    val response = api.getLatestRates(baseCurrency)
                    if (response.result == "success") {
                        val entities = response.rates.map { 
                            CurrencyRateEntity(it.key, it.value, System.currentTimeMillis())
                        }
                        dao.deleteAllRates()
                        dao.insertRates(entities)
                        response.rates
                    } else {
                        localRates.associate { it.currencyCode to it.rate }
                    }
                } catch (e: Exception) {
                    localRates.associate { it.currencyCode to it.rate }
                }
            }
        }
    }

    private fun isCacheValid(timestamp: Long): Boolean {
        // Reduced cache to 5 minutes for "live" rates
        val fiveMinutesMillis = 5 * 60 * 1000L
        return System.currentTimeMillis() - timestamp < fiveMinutesMillis
    }
}
