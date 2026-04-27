package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

data class TransactionSummary(
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal
)

data class CurrencySummary(
    val currency: String,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal
)

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
        accountDao.updateBalance(transaction.accountId, transaction.amount)
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
        accountDao.updateBalance(transaction.accountId, transaction.amount.negate())
    }

    @Transaction
    open suspend fun updateTransactionWithBalanceUpdate(oldTransaction: TransactionEntity, newTransaction: TransactionEntity, accountDao: AccountDao) {
        // Reverse old transaction impact
        accountDao.updateBalance(oldTransaction.accountId, oldTransaction.amount.negate())

        // Apply new transaction impact
        accountDao.updateBalance(newTransaction.accountId, newTransaction.amount)

        // Perform the database update
        insertTransaction(newTransaction)
    }

    @Query("SELECT * FROM transactions WHERE profileId = :profileId")
    abstract suspend fun getAllTransactionsSync(profileId: Int): List<TransactionEntity>

    @Query("SELECT currency, SUM(CASE WHEN transactionType = 'income' THEN amount ELSE 0 END) as totalIncome, SUM(CASE WHEN transactionType = 'expense' THEN amount ELSE 0 END) as totalExpense FROM transactions WHERE profileId = :profileId GROUP BY currency")
    abstract suspend fun getCurrencySummaries(profileId: Int): List<CurrencySummary>

    @Query("SELECT currency, SUM(CASE WHEN transactionType = 'income' THEN amount ELSE 0 END) as totalIncome, SUM(CASE WHEN transactionType = 'expense' THEN amount ELSE 0 END) as totalExpense FROM transactions WHERE profileId = :profileId AND timestamp >= :start AND timestamp <= :end GROUP BY currency")
    abstract suspend fun getCurrencySummariesInRange(profileId: Int, start: Long, end: Long): List<CurrencySummary>

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
