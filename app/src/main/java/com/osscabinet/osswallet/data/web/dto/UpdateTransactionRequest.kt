package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateTransactionRequest(
    val name: String? = null,
    val amount: Double? = null,
    val timestamp: Long? = null,
    val currency: String? = null,
    val transactionType: String? = null,
    val accountId: String? = null,
    val categoryId: Long? = null,
    val tags: List<String>? = null,
    val paymentMethod: String? = null,
    val payee: String? = null,
    val note: String? = null,
    val status: String? = null
)
