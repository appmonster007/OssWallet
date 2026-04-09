package com.osscabinet.gleemanager.domain.repository

import com.osscabinet.gleemanager.domain.model.SharedWallet
import com.osscabinet.gleemanager.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface SharedWalletRepository {
    fun getSharedWallets(): Flow<List<SharedWallet>>
    fun getTransactionsForWallet(walletId: String): Flow<List<Transaction>>
    suspend fun createWallet(name: String): String
    suspend fun joinWallet(inviteCode: String)
    suspend fun addTransactionToWallet(walletId: String, transaction: Transaction)
    suspend fun syncTransactions(walletId: String)
}
