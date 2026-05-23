package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePlannedPaymentRequest(
    val name: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val transactionType: String? = null,
    val categoryId: Long? = null,
    val accountId: String? = null,
    val note: String? = null,
    val recurrenceType: String? = null,
    val recurrenceInterval: Int? = null,
    val startDate: Long? = null,
    val autoConfirm: Boolean? = null
)
