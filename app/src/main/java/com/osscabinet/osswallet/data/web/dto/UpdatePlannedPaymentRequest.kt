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
    val autoConfirm: Boolean? = null,
    val tags: List<String>? = null,
    val paymentMethod: String? = null,
    val transactionStatus: String? = null,
    val payee: String? = null,
    val endDate: Long? = null,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: Double? = null,
    val gpsLocation: String? = null,
    val location: String? = null,
    val warrantyExpiration: Long? = null
)
