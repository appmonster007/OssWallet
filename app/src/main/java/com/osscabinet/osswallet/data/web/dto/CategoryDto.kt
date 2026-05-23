package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val colorHex: Int,
    val nature: String,   // "MUST", "NEED", "WANT"
    val iconName: String,
    val isDefault: Boolean
)
