package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransactionSplitDto(
    val categoryId: Long? = null,
    val amount: Double = 0.0,
    val note: String = ""
)
