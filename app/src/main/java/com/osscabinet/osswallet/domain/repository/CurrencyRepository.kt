package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.currency.CurrencyRates
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface CurrencyRepository {
    val ratesFlow: Flow<Map<String, BigDecimal>>
    suspend fun fetchFreshRates(baseCurrency: String): CurrencyRates
    suspend fun saveRates(baseCurrency: String, rates: CurrencyRates)
    suspend fun getExchangeRates(baseCurrency: String, forceRefresh: Boolean = false): Map<String, Double>
    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double
}
