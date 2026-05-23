package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTagRequest(
    val name: String,
    val colorHex: Int = -1
)

@Serializable
data class RenameTagRequest(
    val newName: String,
    val colorHex: Int? = null
)
