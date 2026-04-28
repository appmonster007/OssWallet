package com.osscabinet.ossWallet.domain.model

data class Category(
    val id: Long,
    val name: String,
    val iconName: String,
    val colorHex: Int,
    val isDefault: Boolean,
    val parentId: Long?
)
