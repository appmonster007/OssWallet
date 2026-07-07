package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.CurrencySummary
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(profileId: Int): Flow<List<Transaction>>
    suspend fun getAllTransactionsSync(profileId: Int): List<Transaction>
    fun getFilteredTransactions(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>>
    suspend fun getFilteredTransactionsSync(
        profileId: Int,
        filter: TransactionFilter
    ): List<Transaction>

    suspend fun insertTransaction(transaction: Transaction, profileId: Int): Long
    suspend fun saveTransfer(
        sourceTransaction: Transaction,
        targetTransaction: Transaction,
        profileId: Int
    ): Pair<Long, Long>

    suspend fun deleteTransaction(id: Long, profileId: Int)
    suspend fun updateTransaction(
        oldTransaction: Transaction,
        newTransaction: Transaction,
        profileId: Int
    )

    fun getTransactionsByAccount(accountId: String, profileId: Int): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long, profileId: Int): Transaction?

    // Link management methods
    suspend fun addTransactionLink(transactionId: Long, linkedTransactionId: Long)
    suspend fun removeTransactionLink(transactionId: Long, linkedTransactionId: Long)
    suspend fun clearTransactionLinks(transactionId: Long)
    fun getAllTransactionLinks(): Flow<Map<Long, List<Long>>>
    fun getLinkedTransactionIds(transactionId: Long): Flow<List<Long>>
    fun getLinkedTransactions(transactionId: Long): Flow<List<Transaction>>

    fun searchTransactions(profileId: Int, query: String): Flow<List<Transaction>>

    /**
     * Observes filtered transactions and calculates anchored running balances.
     * Use this for history feeds that require financial accuracy.
     */
    fun observeFilteredTransactionsWithBalance(
        profileId: Int,
        filter: TransactionFilter,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        limit: Int? = null
    ): Flow<Result<List<com.osscabinet.ossWallet.domain.model.TransactionWithBalance>>>

    suspend fun getUniquePayees(profileId: Int): List<String>
    fun observeUniquePayees(profileId: Int): Flow<List<String>>
    suspend fun getUniqueNotes(profileId: Int): List<String>
    fun observeUniqueNotes(profileId: Int): Flow<List<String>>

    suspend fun getCurrencySummaries(profileId: Int): List<CurrencySummary>
    suspend fun getCurrencySummariesInRange(
        profileId: Int,
        start: Long,
        end: Long
    ): List<CurrencySummary>

    suspend fun getInitialBalanceTransaction(
        accountId: String,
        currency: String,
        profileId: Int
    ): Transaction?

    /** Executes [block] inside a single database transaction. */
    suspend fun <T> runAtomically(block: suspend () -> T): T
}
