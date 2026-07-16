package com.osscabinet.ossWallet.domain.service.currency

import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ExchangeRateSnapshot(
    val anchor: String,
    val rates: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
) {
    companion object {
        val EMPTY = ExchangeRateSnapshot(anchor = "USD", rates = emptyMap())
    }
}
