package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.data.local.util.BigDecimalSerializer
import com.osscabinet.ossWallet.data.local.util.NullableBigDecimalSerializer
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

@Serializable
data class TransactionFilter(
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val queryNote: String? = null,
    val isNoteRegex: Boolean = false,
    val queryPayee: String? = null,
    val isPayeeRegex: Boolean = false,
    val categoryIds: List<Long> = emptyList(),
    val accountIds: List<String> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val hasWarranty: Boolean? = null, // null = any, true = has warranty, false = no warranty
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    @Serializable(with = NullableBigDecimalSerializer::class)
    val minAmount: java.math.BigDecimal? = null,
    @Serializable(with = NullableBigDecimalSerializer::class)
    val maxAmount: java.math.BigDecimal? = null,
    val transactionType: TransactionType? = null, // null = all
    val currencies: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val statuses: List<TransactionStatus> = emptyList(),
    val excludeFuture: Boolean = true
)
