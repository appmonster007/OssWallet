package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class TagDto(
    val name: String,
    val colorHex: Int,
    val transactionCount: Int
)
