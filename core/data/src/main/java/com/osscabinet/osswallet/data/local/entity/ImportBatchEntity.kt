package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per completed CSV import, recording what it created so an undo can safely reverse it —
 * transactions are always deleted by [id]; accounts/categories in [createdAccountIds] /
 * [createdCategoryIds] are only deleted if they end up with zero remaining transactions.
 */
@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey val id: String,
    val sourceApp: String,
    val createdAt: Long,
    val transactionCount: Int,
    val createdAccountIds: List<String>,
    val createdCategoryIds: List<Long>
)
