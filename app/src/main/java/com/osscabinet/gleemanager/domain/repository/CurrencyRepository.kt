package com.osscabinet.gleemanager.domain.repository

interface CurrencyRepository {
    suspend fun getExchangeRates(baseCurrency: String): Map<String, Double>
}
