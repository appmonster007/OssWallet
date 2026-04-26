package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.domain.model.Transaction

/**
 * Service for centralizing transaction lifecycle logic.
 * Reduces logic duplication across ViewModels.
 */
class TransactionManager(
    private val transactionUseCases: TransactionUseCases
) {
    /**
     * Clones an existing transaction with a new timestamp.
     * @return The ID of the newly created transaction.
     */
    suspend fun cloneTransaction(transactionId: Long, profileId: Int): Long? {
        val transaction = transactionUseCases.getTransactionById(transactionId) ?: return null
        val clonedTransaction = transaction.copy(
            id = 0, // Reset ID for new entry
            timestamp = System.currentTimeMillis()
        )
        val newId = transactionUseCases.insertTransaction(clonedTransaction, profileId)
        
        // Optionally link cloned transactions in the future
        // transactionUseCases.addTransactionLink(newId, transactionId)
        
        return newId
    }

    /**
     * Toggles the status of a transaction (Cleared <-> Uncleared).
     * @return The new status.
     */
    suspend fun toggleStatus(transactionId: Long, profileId: Int): TransactionStatus? {
        val transaction = transactionUseCases.getTransactionById(transactionId) ?: return null
        val newStatus = when (transaction.status) {
            TransactionStatus.CLEARED -> TransactionStatus.UNCLEARED
            TransactionStatus.UNCLEARED -> TransactionStatus.CLEARED
            TransactionStatus.RECONCILE -> TransactionStatus.CLEARED
        }
        
        val updatedTransaction = transaction.copy(status = newStatus)
        transactionUseCases.updateTransaction(transaction, updatedTransaction, profileId)
        
        return newStatus
    }

    /**
     * Updates the status of a transaction to a specific value.
     */
    suspend fun updateStatus(transactionId: Long, newStatus: TransactionStatus, profileId: Int): Boolean {
        val transaction = transactionUseCases.getTransactionById(transactionId) ?: return false
        val updatedTransaction = transaction.copy(status = newStatus)
        transactionUseCases.updateTransaction(transaction, updatedTransaction, profileId)
        return true
    }
}
