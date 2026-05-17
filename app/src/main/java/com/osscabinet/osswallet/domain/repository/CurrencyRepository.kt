package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.currency.CurrencyRates

interface CurrencyRepository {
    suspend fun fetchFreshRates(baseCurrency: String): CurrencyRates
    suspend fun saveRates(baseCurrency: String, rates: CurrencyRates)
    suspend fun getExchangeRates(baseCurrency: String, forceRefresh: Boolean = false): Map<String, Double>
    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double
}
