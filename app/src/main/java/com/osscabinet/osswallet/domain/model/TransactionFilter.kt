package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.data.local.BigDecimalSerializer
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
    val startDate: Long? = null,
    val endDate: Long? = null,
    val paymentMethods: List<String> = emptyList(),
    val hasWarranty: Boolean? = null, // null = any, true = has warranty, false = no warranty
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val minAmount: java.math.BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val maxAmount: java.math.BigDecimal? = null,
    val transactionType: String? = null, // null = all, "income" = income only, "expense" = expense only
    val currencies: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val statuses: List<TransactionStatus> = emptyList()
)
