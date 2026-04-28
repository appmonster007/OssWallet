package com.osscabinet.ossWallet.domain.model

data class Tag(
    val id: Long = 0,
    val name: String,
    val colorHex: Int = -1
)
