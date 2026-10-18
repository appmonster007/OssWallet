package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import com.osscabinet.ossWallet.domain.model.util.NullableBigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class CreditCardDetails(
    val accountId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val creditLimit: BigDecimal,
    val statementClosingDay: Int? = null,
    val paymentDueDay: Int? = null,
    val interestRateApr: Float? = null,
    @Serializable(with = NullableBigDecimalSerializer::class)
    val minimumPaymentAmount: BigDecimal? = null
)
