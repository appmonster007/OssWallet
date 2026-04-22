package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.domain.model.SharedWallet
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.SharedWalletRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.util.*

/**
 * Mock implementation of SharedWalletRepository since actual Firebase configuration
 * and Firestore dependencies are not provided.
 * This simulates real-time cloud synchronization and collaborative tracking.
 */
class SharedWalletRepositoryImpl(
    private val transactionDao: TransactionDao
) : SharedWalletRepository {

    private val mockedWallets = mutableListOf<SharedWallet>(
        SharedWallet(id = "wallet_1", name = "Family Expenses", ownerId = "user_me", memberIds = listOf("user_me", "user_wife")),
        SharedWallet(id = "wallet_2", name = "Trip to Japan", ownerId = "user_me", memberIds = listOf("user_me", "user_friend1", "user_friend2"))
    )

    override fun getSharedWallets(): Flow<List<SharedWallet>> = flow {
        // In a real implementation, this would be a SnapshotListener from Firestore
        while (true) {
            emit(mockedWallets.toList())
            delay(5000) // Poll every 5 seconds to simulate sync
        }
    }

    override fun getTransactionsForWallet(walletId: String): Flow<List<Transaction>> = flow {
        // Simulation of fetching transactions from Firestore for a specific wallet
        while (true) {
            // Emitting mocked collaborative transactions
            emit(generateMockSharedTransactions(walletId))
            delay(10000)
        }
    }

    override suspend fun createWallet(name: String): String {
        val newId = UUID.randomUUID().toString()
        mockedWallets.add(SharedWallet(id = newId, name = name, ownerId = "user_me", memberIds = listOf("user_me")))
        return newId
    }

    override suspend fun joinWallet(inviteCode: String) {
        // Simulating joining a wallet by invite code
        delay(1000)
        if (inviteCode == "OSSWALLET123") {
            mockedWallets.add(SharedWallet(id = "shared_3", name = "Roommate Pool", ownerId = "user_other", memberIds = listOf("user_other", "user_me")))
        }
    }

    override suspend fun addTransactionToWallet(walletId: String, transaction: Transaction) {
        // Mock sync to cloud
        delay(500)
        println("Synced transaction ${transaction.title} to cloud wallet $walletId")
    }

    override suspend fun syncTransactions(walletId: String) {
        // Force a manual sync simulation
        delay(2000)
    }

    private fun generateMockSharedTransactions(walletId: String): List<Transaction> {
        return listOf(
            Transaction(id = 100, title = "Groceries (Synced)", amount = BigDecimal("45.0"), category = "Food", timestamp = System.currentTimeMillis() - 100000, currency = "USD", transactionType = "expense", accountId = "mock_acc"),
            Transaction(id = 101, title = "Rent Shared", amount = BigDecimal("1200.0"), category = "Housing", timestamp = System.currentTimeMillis() - 500000, currency = "USD", transactionType = "expense", accountId = "mock_acc")
        )
    }
}
