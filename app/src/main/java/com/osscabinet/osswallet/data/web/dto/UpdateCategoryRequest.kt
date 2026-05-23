package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    val iconName: String? = null,
    val colorHex: Int? = null,
    val nature: String? = null,
    val parentId: Long? = null
)
