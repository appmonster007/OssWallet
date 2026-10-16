package com.osscabinet.ossWallet.domain.model

/** A receipt photo attached to a transaction. The image bytes live in encrypted private storage. */
data class Attachment(
    val id: Long = 0,
    val transactionId: Long,
    val profileId: Int,
    /** Opaque on-disk name; never derived from user input. */
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long
)

object AttachmentLimits {
    const val MAX_PER_TRANSACTION = 10
    const val MAX_IMAGE_EDGE_PX = 1600
    const val JPEG_QUALITY = 82
    const val MIME_JPEG = "image/jpeg"
}
