package com.osscabinet.ossWallet.domain.model.analytics

import java.math.BigDecimal

/**
 * Context for financial computations.
 * Groups recurring parameters to simplify function signatures.
 */
data class ComputationContext(
    val baseCurrency: String,
    val exchangeRates: Map<String, BigDecimal>
)
