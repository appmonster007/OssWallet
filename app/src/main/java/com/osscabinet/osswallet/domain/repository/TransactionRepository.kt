package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface TransactionRepository {
    fun getAllTransactions(profileId: Int): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction, profileId: Int)
    suspend fun deleteTransaction(id: Long)
    suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, profileId: Int)
    suspend fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
}
