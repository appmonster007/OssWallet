package com.osscabinet.ossWallet.domain.model.currency

/**
 * Domain model for currency rates, agnostic of the provider.
 */
data class CurrencyRates(
    val baseCurrency: String,
    val rates: Map<String, Double>,
    val lastUpdated: Long = System.currentTimeMillis(),
    val nextUpdate: Long? = null
)
