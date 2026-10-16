package com.osscabinet.ossWallet.domain.model

/** A file attached to a transaction (receipt photo, invoice PDF, ...). Bytes live in encrypted private storage. */
data class Attachment(
    val id: Long = 0,
    val transactionId: Long,
    val profileId: Int,
    /** Opaque on-disk name; never derived from user input. */
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long,
    /** The name the user sees (original file name); display only, never used as a path. */
    val displayName: String = ""
) {
    val isImage: Boolean get() = AttachmentLimits.isImage(mimeType)
}

/** A picked file after processing, ready to store. Images are downscaled JPEGs; everything else is kept as-is. */
class PreparedAttachment(val bytes: ByteArray, val mimeType: String, val displayName: String)

object AttachmentLimits {
    const val MAX_PER_TRANSACTION = 10
    const val MAX_IMAGE_EDGE_PX = 1600
    const val JPEG_QUALITY = 82
    const val MIME_JPEG = "image/jpeg"
    const val MIME_PDF = "application/pdf"
    const val MIME_UNKNOWN = "application/octet-stream"

    /** Per file; stored encrypted in memory-sized chunks, so keep it modest. */
    const val MAX_FILE_BYTES = 20L * 1024 * 1024

    fun isImage(mimeType: String) = mimeType.startsWith("image/")
}
