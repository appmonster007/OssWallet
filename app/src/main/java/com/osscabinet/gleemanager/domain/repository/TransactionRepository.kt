package com.osscabinet.gleemanager.domain.repository

import com.osscabinet.gleemanager.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(profileId: Int): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction, profileId: Int)
    suspend fun deleteTransaction(id: Long)
    suspend fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
}
