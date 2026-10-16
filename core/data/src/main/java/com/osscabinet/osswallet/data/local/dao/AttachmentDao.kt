package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.osscabinet.ossWallet.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

data class AttachmentCountRow(val transactionId: Long, val n: Int)

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId AND profileId = :profileId ORDER BY createdAt")
    fun observeForTransaction(transactionId: Long, profileId: Int): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId AND profileId = :profileId ORDER BY createdAt")
    suspend fun getForTransaction(transactionId: Long, profileId: Int): List<AttachmentEntity>

    @Query("SELECT transactionId, COUNT(*) AS n FROM attachments WHERE profileId = :profileId GROUP BY transactionId")
    fun observeCounts(profileId: Int): Flow<List<AttachmentCountRow>>

    @Query("SELECT COUNT(*) FROM attachments WHERE transactionId = :transactionId AND profileId = :profileId")
    suspend fun countForTransaction(transactionId: Long, profileId: Int): Int

    @Query("SELECT * FROM attachments WHERE id = :id AND profileId = :profileId")
    suspend fun getById(id: Long, profileId: Int): AttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity): Long

    @Query("DELETE FROM attachments WHERE id = :id AND profileId = :profileId")
    suspend fun deleteById(id: Long, profileId: Int)

    @Query("DELETE FROM attachments WHERE transactionId = :transactionId AND profileId = :profileId")
    suspend fun deleteForTransaction(transactionId: Long, profileId: Int)

    /** Cross-profile maintenance: rows whose transaction no longer exists. */
    @Query("DELETE FROM attachments WHERE transactionId NOT IN (SELECT id FROM transactions)")
    suspend fun deleteOrphanRows(): Int

    /** Cross-profile maintenance: every file name still referenced. */
    @Query("SELECT fileName FROM attachments")
    suspend fun getAllFileNames(): List<String>

    /** Cross-profile bulk read. ONLY for backup/export flows. */
    @Query("SELECT * FROM attachments")
    suspend fun getAllForBackup(): List<AttachmentEntity>

    @Query("DELETE FROM attachments")
    suspend fun clearAll()
}
