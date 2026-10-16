package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Attachment
import kotlinx.coroutines.flow.Flow

interface AttachmentRepository {
    fun observeForTransaction(transactionId: Long, profileId: Int): Flow<List<Attachment>>

    /** transactionId -> attachment count, for list badges. */
    fun observeCounts(profileId: Int): Flow<Map<Long, Int>>

    suspend fun getForTransaction(transactionId: Long, profileId: Int): List<Attachment>

    /** Encrypts [jpeg] into private storage and records it. Throws when the per-transaction cap is hit. */
    suspend fun add(transactionId: Long, profileId: Int, jpeg: ByteArray): Attachment

    /** Decrypted image bytes, or null if the file is missing or can no longer be decrypted. */
    suspend fun readBytes(attachment: Attachment): ByteArray?

    suspend fun delete(id: Long, profileId: Int)

    suspend fun deleteForTransaction(transactionId: Long, profileId: Int)

    /** Drops rows whose transaction is gone and files with no row. Safe to call any time. */
    suspend fun removeOrphans()

    /** Removes every attachment row and file (Delete All Data). */
    suspend fun clearAll()
}

/** Turns a picked/captured image into a downscaled, EXIF-rotated JPEG ready to store. */
interface ReceiptImageProcessor {
    /** [source] is a content URI string. */
    suspend fun prepare(source: String): ByteArray
}
