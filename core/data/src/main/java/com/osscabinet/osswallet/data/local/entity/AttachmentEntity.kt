package com.osscabinet.ossWallet.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Deliberately has no foreign key to `transactions`: transaction saves use REPLACE, which would
 * cascade-delete receipts on every edit. Orphans are swept by [AttachmentDao.deleteOrphanRows].
 */
@Entity(
    tableName = "attachments",
    indices = [Index(value = ["transactionId"]), Index(value = ["profileId"])]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val profileId: Int,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "") val displayName: String = ""
)
