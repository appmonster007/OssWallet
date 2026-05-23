package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class FilterDto(
    val id: Long,
    val name: String,
    val timelineType: String,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val queryNote: String? = null,
    val isNoteRegex: Boolean = false,
    val queryPayee: String? = null,
    val isPayeeRegex: Boolean = false,
    val categoryIds: List<Long> = emptyList(),
    val accountIds: List<String> = emptyList(),
    val paymentMethods: List<String> = emptyList(),
    val transactionTypes: List<String> = emptyList(),
    val currencies: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val hasWarranty: Boolean? = null,
    val excludeFuture: Boolean = true,
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    val linkedTransactionIds: List<Long> = emptyList()
)

@Serializable
data class CreateFilterRequest(
    val name: String,
    val timelineType: String = "ALL_TIME",
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val queryNote: String? = null,
    val isNoteRegex: Boolean = false,
    val queryPayee: String? = null,
    val isPayeeRegex: Boolean = false,
    val categoryIds: List<Long> = emptyList(),
    val accountIds: List<String> = emptyList(),
    val paymentMethods: List<String> = emptyList(),
    val transactionTypes: List<String> = emptyList(),
    val currencies: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val hasWarranty: Boolean? = null,
    val excludeFuture: Boolean = true,
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    val linkedTransactionIds: List<Long> = emptyList()
)
