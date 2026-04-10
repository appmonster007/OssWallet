package com.osscabinet.gleemanager.domain.repository

interface CurrencyRepository {
    suspend fun getExchangeRates(baseCurrency: String): Map<String, Double>
    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double
}
