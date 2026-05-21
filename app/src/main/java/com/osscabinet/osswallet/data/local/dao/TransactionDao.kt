package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.domain.model.query.RawAggregateResult
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

    @Transaction
    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY timestamp DESC")
    abstract fun getAllTransactionsWithTags(profileId: Int): Flow<List<com.osscabinet.ossWallet.data.local.entity.TransactionWithTags>>

    @RawQuery(observedEntities = [TransactionEntity::class])
    abstract fun getFilteredTransactions(query: SupportSQLiteQuery): Flow<List<TransactionEntity>>

    @RawQuery
    abstract suspend fun getFilteredTransactionsSync(query: SupportSQLiteQuery): List<TransactionEntity>

    @Upsert
    abstract suspend fun _insertTransaction(transaction: TransactionEntity): Long

    @Transaction
    open suspend fun insertTransactionWithBalanceUpdate(transaction: TransactionEntity, accountDao: AccountDao): Long {
        return _insertTransaction(transaction)
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
    }

    @Transaction
    open suspend fun updateTransactionWithBalanceUpdate(oldTransaction: TransactionEntity, newTransaction: TransactionEntity, accountDao: AccountDao) {
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

    @Query("""
        SELECT transactions.* FROM transactions 
        JOIN transactions_fts ON transactions.id = transactions_fts.rowid 
        WHERE transactions_fts MATCH :query AND profileId = :profileId
        ORDER BY timestamp DESC
    """)
    abstract fun searchTransactions(profileId: Int, query: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT transactions.* FROM transactions
        JOIN categories ON transactions.categoryId = categories.id
        WHERE categories.name LIKE '%' || :query || '%' AND transactions.profileId = :profileId
        ORDER BY transactions.timestamp DESC
    """)
    abstract fun searchTransactionsByCategoryName(profileId: Int, query: String): Flow<List<TransactionEntity>>

    @Query("SELECT DISTINCT payee FROM transactions WHERE profileId = :profileId AND payee IS NOT NULL AND payee != '' ORDER BY payee ASC")
    abstract suspend fun getUniquePayees(profileId: Int): List<String>

    @Query("SELECT DISTINCT note FROM transactions WHERE profileId = :profileId AND note IS NOT NULL AND note != '' ORDER BY note ASC")
    abstract suspend fun getUniqueNotes(profileId: Int): List<String>

    @Update
    abstract suspend fun updateTransactions(transactions: List<TransactionEntity>)

    @Upsert
    abstract suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransactionTagCrossRef(crossRef: com.osscabinet.ossWallet.data.local.entity.TransactionTagCrossRef)

    @Query("DELETE FROM transaction_tag_cross_ref WHERE transactionId = :transactionId")
    abstract suspend fun deleteTransactionTagCrossRefs(transactionId: Long)

    @Delete
    abstract suspend fun deleteTransaction(transaction: TransactionEntity)

    @RawQuery
    abstract suspend fun aggregateTransactionsSync(query: SupportSQLiteQuery): List<RawAggregateResult>

    @RawQuery(observedEntities = [TransactionEntity::class])
    abstract fun aggregateTransactions(query: SupportSQLiteQuery): Flow<List<RawAggregateResult>>
}
