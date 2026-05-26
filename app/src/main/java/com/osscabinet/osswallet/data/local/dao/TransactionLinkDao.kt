package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionLinkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing transaction links (many-to-many relationship).
 */
@Dao
abstract class TransactionLinkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertLink(link: TransactionLinkEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertLinks(links: List<TransactionLinkEntity>)

    @Query("DELETE FROM transaction_links WHERE transactionId = :transactionId AND linkedTransactionId = :linkedTransactionId")
    abstract suspend fun deleteLink(transactionId: Long, linkedTransactionId: Long)

    @Query("DELETE FROM transaction_links WHERE transactionId = :transactionId")
    abstract suspend fun deleteAllLinksForTransaction(transactionId: Long)

    @Query("DELETE FROM transaction_links WHERE transactionId = :transactionId OR linkedTransactionId = :transactionId")
    abstract suspend fun deleteAnyLink(transactionId: Long)

    @Query("SELECT linkedTransactionId FROM transaction_links WHERE transactionId = :transactionId")
    abstract fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>>

    @Query("SELECT transactionId FROM transaction_links WHERE linkedTransactionId = :linkedTransactionId")
    abstract fun getReverseLinkedTransactionIds(linkedTransactionId: Long): Flow<List<Long>>

    @Query("SELECT * FROM transactions WHERE id IN (SELECT linkedTransactionId FROM transaction_links WHERE transactionId = :transactionId)")
    abstract fun getLinkedTransactions(transactionId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id IN (SELECT transactionId FROM transaction_links WHERE linkedTransactionId = :linkedTransactionId)")
    abstract fun getReverseLinkedTransactions(linkedTransactionId: Long): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transaction_links")
    abstract suspend fun clearAll()

    @Query("SELECT * FROM transaction_links")
    abstract suspend fun getAllLinksSync(): List<TransactionLinkEntity>
}
