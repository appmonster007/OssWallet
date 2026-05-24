package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: Long,
    val title: String,
    val amount: Double,
    val currency: String,
    val type: String,
    val categoryId: Long?,
    val categoryName: String?,
    val accountId: String?,
    val accountName: String,
    val targetAccountId: String? = null,
    val targetAccountName: String? = null,
    val date: Long,
    val note: String? = null,
    val payee: String? = null,
    val tags: List<String> = emptyList(),
    val status: String = "CLEARED",
    val paymentMethod: String? = null,
    val warrantyExpiration: Long? = null,
    val location: String? = null
)
