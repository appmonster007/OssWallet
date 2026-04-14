package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.data.remote.CurrencyResponse

interface CurrencyRepository {
    suspend fun fetchFreshRates(baseCurrency: String): CurrencyResponse
    suspend fun saveRates(baseCurrency: String, response: CurrencyResponse)
    suspend fun getExchangeRates(baseCurrency: String, forceRefresh: Boolean = false): Map<String, Double>
    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double
}
