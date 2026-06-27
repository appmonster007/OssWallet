package com.osscabinet.ossWallet.domain.service.currency.provider

/**
 * Metadata for a currency supported by an exchange rate provider.
 */
data class CurrencyMetadata(
    val code: String,
    val name: String,
    val country: String,
    val note: String? = null,
    val isVolatile: Boolean = false
)
