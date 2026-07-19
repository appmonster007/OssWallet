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
    val note: String = "",
    val tags: List<String> = emptyList(),
    val paymentMethod: String? = null,
    val transactionStatus: String = "CLEARED",
    val payee: String? = null,
    val endDate: Long? = null,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: Double = 1.0,
    val gpsLocation: String? = null,
    val location: String? = null,
    val warrantyExpiration: Long? = null
)
