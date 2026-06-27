package com.osscabinet.ossWallet.domain.model.currency

/**
 * Domain model representing a single currency exchange rate entry.
 */
data class CurrencyRate(
    val currencyCode: String,
    val baseCurrency: String,
    val rate: Double,
    val timestamp: Long,
    val nextUpdate: Long
)
