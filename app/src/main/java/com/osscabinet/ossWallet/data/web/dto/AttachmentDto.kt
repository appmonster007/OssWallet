package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class AttachmentDto(
    val id: Long,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long,
    /** True only for formats the browser can safely draw in an <img>; everything else is download-only. */
    val previewable: Boolean
)
