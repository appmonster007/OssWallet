package com.osscabinet.ossWallet.domain.repository

interface CurrencyRepository {
    suspend fun getExchangeRates(baseCurrency: String, forceRefresh: Boolean = false): Map<String, Double>
    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double
}
