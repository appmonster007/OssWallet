package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface ComputationRepository {
    val computationContext: Flow<ComputationContext?>
    val exchangeRates: Flow<Map<String, BigDecimal>>

    /** Pre-packaged snapshot with the correct anchor — use this for any currency conversion. */
    val exchangeRatesSnapshot: Flow<ExchangeRateSnapshot>
    val selectedCurrency: Flow<String>
    val hasLoadedExchangeRates: Flow<Boolean>

    /** ARC-04: emits when a rate refresh fails so UI can surface a snackbar/banner. */
    val rateRefreshErrors: Flow<RateRefreshError>
}

data class RateRefreshError(
    val baseCurrency: String,
    val cause: Throwable,
    val timestamp: Long = System.currentTimeMillis()
)
