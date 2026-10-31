package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Attachment
import com.osscabinet.ossWallet.domain.model.AttachmentLimits
import com.osscabinet.ossWallet.domain.model.PreparedAttachment
import kotlinx.coroutines.flow.Flow

interface AttachmentRepository {
    fun observeForTransaction(transactionId: Long, profileId: Int): Flow<List<Attachment>>

    /** transactionId -> attachment count, for list badges. */
    fun observeCounts(profileId: Int): Flow<Map<Long, Int>>

    suspend fun getForTransaction(transactionId: Long, profileId: Int): List<Attachment>

    /** Encrypts [bytes] into private storage and records it. Throws when the per-transaction cap is hit. */
    suspend fun add(
        transactionId: Long,
        profileId: Int,
        bytes: ByteArray,
        mimeType: String = AttachmentLimits.MIME_JPEG,
        displayName: String = ""
    ): Attachment

    /**
     * Stores one encrypted copy and records it on every id in [transactionIds]: both legs of a
     * transfer show the same attachments, yet the file exists once. Throws, storing nothing, if any
     * of them is already at the cap.
     */
    suspend fun addShared(
        transactionIds: List<Long>,
        profileId: Int,
        bytes: ByteArray,
        mimeType: String,
        displayName: String
    ): List<Attachment>

    /** Makes sure each of [attachments] also exists on every id in [transactionIds] (older one-sided ones). */
    suspend fun shareWith(attachments: List<Attachment>, transactionIds: List<Long>, profileId: Int)

    /** Decrypted file bytes, or null if the file is missing or can no longer be decrypted. */
    suspend fun readBytes(attachment: Attachment): ByteArray?

    /** Removes the attachment, including its copies on the other leg of a transfer. */
    suspend fun delete(id: Long, profileId: Int)

    /** Removes this transaction's attachments only; a file still shown on a transfer's other leg is kept. */
    suspend fun deleteForTransaction(transactionId: Long, profileId: Int)

    /** Drops rows whose transaction is gone and files with no row. Safe to call any time. */
    suspend fun removeOrphans()

    /** Removes every attachment row and file (Delete All Data). */
    suspend fun clearAll()
}

/** Turns a picked/captured file into something storable: images are downscaled and EXIF-stripped, other files pass through. */
interface AttachmentProcessor {
    /** [source] is a content URI string. Throws with a user-readable message when the file can't be used. */
    suspend fun prepare(source: String): PreparedAttachment

    /** Same as [prepare] for a file that arrived as raw bytes (web upload); [displayName] is its original name. */
    suspend fun prepareBytes(bytes: ByteArray, displayName: String): PreparedAttachment
}
