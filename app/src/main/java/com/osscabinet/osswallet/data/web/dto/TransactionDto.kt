package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: Long,
    val title: String,
    val amount: Double,
    val currency: String,
    val type: String,
    val categoryName: String?,
    val accountName: String,
    val date: Long,
    val note: String?
)
