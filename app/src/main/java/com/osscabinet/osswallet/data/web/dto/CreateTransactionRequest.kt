package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTransactionRequest(
    val name: String,
    val amount: Double,
    val currency: String,
    val transactionType: String,
    val accountId: String? = null,
    val categoryId: Long,
    val timestamp: Long,
    val note: String = "",
    val payee: String = "",
    val tags: List<String> = emptyList(),
    val paymentMethod: String = "CASH",
    val status: String = "CLEARED",
    val gpsLocation: String? = null,
    val location: String? = null,
    val warrantyExpiration: Long? = null
)
