package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import java.math.BigDecimal

/**
 * Service for centralizing transaction lifecycle logic.
 * Reduces logic duplication across ViewModels.
 */
class TransactionManager(
    private val transactionUseCases: TransactionUseCases
) {

    /**
     * Handles saving (creating or updating) a transaction.
     * Centralizes logic for transfers, status updates, and type changes.
     */
    suspend fun handleSaveTransaction(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int
    ): Long {
        if (request.isTransfer) {
            return handleTransferSave(existingTransaction, request, profileId)
        } else {
            return handleNormalSave(existingTransaction, request, profileId)
        }
    }

    private suspend fun handleNormalSave(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int
    ): Long {
        if (existingTransaction != null) {
            val wasTransfer = existingTransaction.category == "Transfer"
            if (wasTransfer) {
                // Changing from Transfer to Normal: Clear links
                transactionUseCases.clearTransactionLinks(existingTransaction.id)
            }

            val updatedTransaction = existingTransaction.copy(
                title = request.title.ifBlank { request.category },
                amount = request.amount,
                category = request.category,
                transactionType = request.transactionType,
                accountId = request.accountId,
                currency = request.currency,
                categoryId = request.categoryId,
                tags = request.tags,
                paymentMethod = request.paymentMethod,
                timestamp = request.timestamp,
                payee = request.payee,
                note = request.note,
                latitude = request.latitude,
                longitude = request.longitude,
                warrantyExpiration = request.warrantyExpiration,
                status = request.status
            )
            transactionUseCases.updateTransaction(
                existingTransaction,
                updatedTransaction,
                profileId
            )
            return existingTransaction.id
        } else {
            val newTransaction = Transaction(
                title = request.title.ifBlank { request.category },
                amount = request.amount,
                category = request.category,
                transactionType = request.transactionType,
                accountId = request.accountId,
                currency = request.currency,
                categoryId = request.categoryId,
                tags = request.tags,
                paymentMethod = request.paymentMethod,
                timestamp = request.timestamp,
                payee = request.payee,
                note = request.note,
                latitude = request.latitude,
                longitude = request.longitude,
                warrantyExpiration = request.warrantyExpiration,
                status = request.status
            )
            return transactionUseCases.insertTransaction(newTransaction, profileId)
        }
    }

    private suspend fun handleTransferSave(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int
    ): Long {
        val targetAccountId = request.targetAccountId

        // Build source transaction (outgoing)
        val sourceTransaction = (existingTransaction ?: Transaction(
            title = "",
            amount = BigDecimal.ZERO,
            category = "Transfer",
            transactionType = "expense",
            accountId = request.accountId,
            currency = request.currency,
            timestamp = request.timestamp
        )).copy(
            title = request.title.ifBlank { "Transfer" },
            amount = request.amount.negate(),
            category = "Transfer",
            transactionType = "expense",
            accountId = request.accountId,
            currency = request.currency,
            categoryId = null,
            tags = request.tags,
            timestamp = request.timestamp,
            payee = request.payee,
            note = request.note,
            latitude = request.latitude,
            longitude = request.longitude,
            warrantyExpiration = request.warrantyExpiration,
            status = request.status
        )

        // If no target account, this is an "Outside of wallet" transfer
        // Only create the source transaction, no counterpart
        if (targetAccountId == null) {
            return if (existingTransaction != null) {
                // Clear any existing links since this is now outside-wallet
                transactionUseCases.clearTransactionLinks(existingTransaction.id)
                transactionUseCases.updateTransaction(
                    existingTransaction,
                    sourceTransaction,
                    profileId
                )
                existingTransaction.id
            } else {
                transactionUseCases.insertTransaction(sourceTransaction, profileId)
            }
        }

        // Internal transfer: create counterpart transaction
        val targetTransaction = Transaction(
            title = request.title.ifBlank { "Transfer" },
            amount = request.amount.multiply(request.transferRate),
            category = "Transfer",
            transactionType = "income",
            accountId = targetAccountId,
            currency = request.targetCurrency ?: request.currency,
            categoryId = null,
            tags = request.tags,
            timestamp = request.timestamp,
            payee = request.payee,
            note = request.note,
            latitude = request.latitude,
            longitude = request.longitude,
            warrantyExpiration = request.warrantyExpiration,
            status = request.status
        )

        if (existingTransaction != null && existingTransaction.category == "Transfer") {
            // Already a transfer, update both
            transactionUseCases.updateTransaction(existingTransaction, sourceTransaction, profileId)
        }

        val pair = transactionUseCases.saveTransfer(sourceTransaction, targetTransaction, profileId)
        return pair.first // Source ID
    }

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
    suspend fun updateStatus(
        transactionId: Long,
        newStatus: TransactionStatus,
        profileId: Int
    ): Boolean {
        val transaction = transactionUseCases.getTransactionById(transactionId) ?: return false
        val updatedTransaction = transaction.copy(status = newStatus)
        transactionUseCases.updateTransaction(transaction, updatedTransaction, profileId)
        return true
    }
}
