package com.osscabinet.ossWallet.domain.service.currency

import java.math.BigDecimal

data class ExchangeRateSnapshot(
    val anchor: String,
    val rates: Map<String, BigDecimal>,
) {
    companion object {
        val EMPTY = ExchangeRateSnapshot(anchor = "USD", rates = emptyMap())
    }
}
