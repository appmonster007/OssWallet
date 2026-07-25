package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.osscabinet.ossWallet.data.local.entity.ImportBatchEntity

@Dao
interface ImportBatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: ImportBatchEntity)

    @Query("SELECT * FROM import_batches WHERE id = :id LIMIT 1")
    suspend fun getBatchById(id: String): ImportBatchEntity?

    @Query("DELETE FROM import_batches WHERE id = :id")
    suspend fun deleteBatch(id: String)

    @Query("DELETE FROM transactions WHERE importBatchId = :batchId")
    suspend fun deleteTransactionsForBatch(batchId: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun countTransactionsForAccount(accountId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: Long): Int

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: String)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)
}
