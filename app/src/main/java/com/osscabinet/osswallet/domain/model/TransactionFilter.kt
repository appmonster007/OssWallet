package com.osscabinet.ossWallet.domain.model

data class TransactionFilter(
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
    val minAmount: java.math.BigDecimal? = null,
    val maxAmount: java.math.BigDecimal? = null,
    val isIncome: Boolean? = null // null = both, true = income only, false = expense only
)
