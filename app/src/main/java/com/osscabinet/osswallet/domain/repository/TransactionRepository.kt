package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface TransactionRepository {
    fun getAllTransactions(profileId: Int): Flow<List<Transaction>>
    suspend fun getAllTransactionsSync(profileId: Int): List<Transaction>
    fun getFilteredTransactions(profileId: Int, filter: TransactionFilter): Flow<List<Transaction>>
    suspend fun getFilteredTransactionsSync(profileId: Int, filter: TransactionFilter): List<Transaction>
    suspend fun insertTransaction(transaction: Transaction, profileId: Int): Long
    suspend fun saveTransfer(sourceTransaction: Transaction, targetTransaction: Transaction, profileId: Int): Pair<Long, Long>
    suspend fun deleteTransaction(id: Long)
    suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int)
    suspend fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun triggerWidgetUpdate()
    
    suspend fun getCurrencySummaries(profileId: Int): List<com.osscabinet.ossWallet.data.local.dao.CurrencySummary>
    suspend fun getCurrencySummariesInRange(profileId: Int, start: Long, end: Long): List<com.osscabinet.ossWallet.data.local.dao.CurrencySummary>
}
