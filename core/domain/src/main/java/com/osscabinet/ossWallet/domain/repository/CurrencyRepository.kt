package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.currency.CurrencyRates
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface CurrencyRepository {
    /**
     * Emits an anchored snapshot for the given base currency (ARC-01).
     * The flow re-emits whenever the cached rows for [baseCurrency] change.
     */
    fun observeRatesSnapshot(baseCurrency: String): Flow<ExchangeRateSnapshot>

    @Deprecated(
        "Use observeRatesSnapshot(baseCurrency) — the bare Map carries no anchor (ARC-01).",
        replaceWith = ReplaceWith("observeRatesSnapshot(baseCurrency)")
    )
    val ratesFlow: Flow<Map<String, BigDecimal>>

    suspend fun fetchFreshRates(baseCurrency: String): CurrencyRates
    suspend fun saveRates(baseCurrency: String, rates: CurrencyRates)
    suspend fun getExchangeRates(
        baseCurrency: String,
        forceRefresh: Boolean = false
    ): Map<String, Double>

    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double
}
