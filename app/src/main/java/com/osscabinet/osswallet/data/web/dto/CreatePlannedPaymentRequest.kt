package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreatePlannedPaymentRequest(
    val name: String,
    val amount: Double,
    val currency: String,
    val transactionType: String,
    val categoryId: Long,
    val accountId: String? = null,
    val recurrenceType: String,   // "MONTHLY", "WEEKLY", etc.
    val recurrenceInterval: Int = 1,
    val startDate: Long,
    val autoConfirm: Boolean = false,
    val note: String = ""
)
