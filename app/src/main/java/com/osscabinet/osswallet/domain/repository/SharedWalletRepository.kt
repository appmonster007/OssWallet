package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.SharedWallet
import com.osscabinet.ossWallet.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface SharedWalletRepository {
    fun getSharedWallets(): Flow<List<SharedWallet>>
    fun getTransactionsForWallet(walletId: String): Flow<List<Transaction>>
    suspend fun createWallet(name: String): String
    suspend fun joinWallet(inviteCode: String)
    suspend fun addTransactionToWallet(walletId: String, transaction: Transaction)
    suspend fun syncTransactions(walletId: String)
}
