package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import java.math.BigDecimal

/**
 * Service for centralizing transaction lifecycle logic.
 * Reduces logic duplication across ViewModels.
 * Widget refresh is handled entirely by WidgetInvalidationObserver watching the
 * transactions table, so no explicit trigger calls are needed here.
 */
class TransactionManager(
    private val transactionUseCases: TransactionUseCases
) {

    suspend fun handleSaveTransaction(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int
    ): Long {
        return if (request.isTransfer) {
            handleTransferSave(existingTransaction, request, profileId)
        } else {
            handleNormalSave(existingTransaction, request, profileId)
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
            transactionUseCases.updateTransaction(existingTransaction, updatedTransaction, profileId)
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

        if (targetAccountId == null) {
            return if (existingTransaction != null) {
                transactionUseCases.clearTransactionLinks(existingTransaction.id)
                transactionUseCases.updateTransaction(existingTransaction, sourceTransaction, profileId)
                existingTransaction.id
            } else {
                transactionUseCases.insertTransaction(sourceTransaction, profileId)
            }
        }

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
            transactionUseCases.updateTransaction(existingTransaction, sourceTransaction, profileId)
        }

        val pair = transactionUseCases.saveTransfer(sourceTransaction, targetTransaction, profileId)
        return pair.first
    }

    suspend fun cloneTransaction(transactionId: Long, profileId: Int): Long? {
        val transaction = transactionUseCases.getTransactionById(transactionId) ?: return null
        val clonedTransaction = transaction.copy(id = 0, timestamp = System.currentTimeMillis())
        return transactionUseCases.insertTransaction(clonedTransaction, profileId)
    }

    suspend fun toggleStatus(transactionId: Long, profileId: Int): TransactionStatus? {
        val transaction = transactionUseCases.getTransactionById(transactionId) ?: return null
        val newStatus = when (transaction.status) {
            TransactionStatus.CLEARED -> TransactionStatus.UNCLEARED
            TransactionStatus.UNCLEARED -> TransactionStatus.CLEARED
            TransactionStatus.RECONCILE -> TransactionStatus.CLEARED
        }
        transactionUseCases.updateTransaction(transaction, transaction.copy(status = newStatus), profileId)
        return newStatus
    }

    suspend fun updateStatus(transactionId: Long, newStatus: TransactionStatus, profileId: Int): Boolean {
        val transaction = transactionUseCases.getTransactionById(transactionId) ?: return false
        transactionUseCases.updateTransaction(transaction, transaction.copy(status = newStatus), profileId)
        return true
    }
}
