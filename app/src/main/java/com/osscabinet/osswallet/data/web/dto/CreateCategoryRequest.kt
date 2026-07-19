package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val parentId: Long? = null,
    val colorHex: Int = 0xFF9E9E9E.toInt(),
    val nature: String = "NEED",
    val iconName: String = "category"
)
