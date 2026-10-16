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

    /** Decrypted file bytes, or null if the file is missing or can no longer be decrypted. */
    suspend fun readBytes(attachment: Attachment): ByteArray?

    suspend fun delete(id: Long, profileId: Int)

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
}
