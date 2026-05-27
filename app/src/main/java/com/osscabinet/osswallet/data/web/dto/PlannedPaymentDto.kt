package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlannedPaymentDto(
    val id: Long,
    val name: String,
    val amount: Double,
    val currency: String,
    val transactionType: String,
    val categoryId: Long,
    val categoryName: String?,
    val accountId: String?,
    val accountName: String?,
    val nextDueDate: Long,
    val recurrenceDescription: String,
    val status: String,
    val isOverdue: Boolean,
    val autoConfirm: Boolean,
    val targetAccountId: String? = null,
    val targetAccountName: String? = null,
    val targetCurrency: String? = null,
    val transferRate: Double = 1.0,
    val location: String? = null,
    val gpsLocation: String? = null,
    val warrantyExpiration: Long? = null,
    val payee: String? = null,
    val tags: List<String> = emptyList()
)
