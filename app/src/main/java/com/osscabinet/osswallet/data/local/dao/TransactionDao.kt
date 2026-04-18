package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
abstract class TransactionDao {
    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY timestamp DESC")
    abstract fun getAllTransactions(profileId: Int): Flow<List<TransactionEntity>>

    @RawQuery(observedEntities = [TransactionEntity::class])
    abstract fun getFilteredTransactions(query: SupportSQLiteQuery): Flow<List<TransactionEntity>>

    @RawQuery
    abstract suspend fun getFilteredTransactionsSync(query: SupportSQLiteQuery): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun _insertTransaction(transaction: TransactionEntity): Long

    @Transaction
    open suspend fun insertTransactionWithBalanceUpdate(transaction: TransactionEntity, accountDao: AccountDao): Long {
        val id = _insertTransaction(transaction)
        val updateAmount = if (transaction.isIncome) transaction.amount else transaction.amount.negate()
        accountDao.updateBalance(transaction.accountId, updateAmount)
        return id
    }

    @Query("SELECT * FROM transactions WHERE id = :id")
    abstract suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    abstract fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Delete
    abstract suspend fun _deleteTransaction(transaction: TransactionEntity)

    @Transaction
    open suspend fun deleteTransactionWithBalanceUpdate(transaction: TransactionEntity, accountDao: AccountDao) {
        _deleteTransaction(transaction)
        val updateAmount = if (transaction.isIncome) transaction.amount.negate() else transaction.amount
        accountDao.updateBalance(transaction.accountId, updateAmount)
    }

    @Transaction
    open suspend fun updateTransactionWithBalanceUpdate(oldTransaction: TransactionEntity, newTransaction: TransactionEntity, accountDao: AccountDao) {
        // Reverse old transaction impact
        val reverseAmount = if (oldTransaction.isIncome) oldTransaction.amount.negate() else oldTransaction.amount
        accountDao.updateBalance(oldTransaction.accountId, reverseAmount)

        // Apply new transaction impact
        insertTransaction(newTransaction)
        val applyAmount = if (newTransaction.isIncome) newTransaction.amount else newTransaction.amount.negate()
        accountDao.updateBalance(newTransaction.accountId, applyAmount)
    }

    @Query("SELECT * FROM transactions")
    abstract suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    abstract suspend fun clearAll()

    @Query("SELECT * FROM transactions WHERE tags LIKE '%' || :tagName || '%'")
    abstract suspend fun getTransactionsWithTag(tagName: String): List<TransactionEntity>

    @Update
    abstract suspend fun updateTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    abstract suspend fun deleteTransaction(transaction: TransactionEntity)
}
