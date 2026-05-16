package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import java.math.BigDecimal

class TransactionManager(
    private val transactionUseCases: TransactionUseCases,
    private val widgetRefreshTrigger: WidgetRefreshTrigger
) {

    suspend fun handleSaveTransaction(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int
    ): Long = andRefresh {
        if (request.isTransfer) handleTransferSave(existingTransaction, request, profileId)
        else handleNormalSave(existingTransaction, request, profileId)
    }

    private suspend fun handleNormalSave(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int
    ): Long {
        val updated = request.toNormalTransaction(existingTransaction)
        return if (existingTransaction != null) {
            if (existingTransaction.categoryId == 1003L) {
                transactionUseCases.clearTransactionLinks(existingTransaction.id)
            }
            transactionUseCases.updateTransaction(existingTransaction, updated, profileId)
            existingTransaction.id
        } else {
            transactionUseCases.insertTransaction(updated, profileId)
        }
    }

    private suspend fun handleTransferSave(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int
    ): Long {
        val targetAccountId = request.targetAccountId
        val sourceTransaction = (existingTransaction ?: Transaction(
            id = 0,
            name = request.name,
            amount = BigDecimal.ZERO,
            timestamp = request.timestamp,
            currency = request.currency,
            transactionType = TransactionType.TRANSFER,
            accountId = request.accountId,
            categoryId = 1003L
        )).copy(
            amount = request.amount.negate(),
            transactionType = TransactionType.TRANSFER,
            accountId = request.accountId,
            currency = request.currency,
            categoryId = 1003L, // Transfer category ID
            tags = request.tags,
            timestamp = request.timestamp,
            payee = request.payee,
            note = request.note,
            latitude = request.latitude,
            longitude = request.longitude,
            warrantyExpiration = request.warrantyExpiration,
            status = request.status,
            sourcePlannedPaymentId = request.sourcePlannedPaymentId
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

        // Target inherits all shared metadata from source; only the amount, direction,
        // account, and currency differ — and id must be 0 since it is always a new row.
        val targetTransaction = sourceTransaction.copy(
            id = 0,
            amount = request.amount.multiply(request.transferRate),
            transactionType = TransactionType.TRANSFER,
            accountId = targetAccountId,
            currency = request.targetCurrency ?: request.currency
        )

        if (existingTransaction != null && existingTransaction.categoryId == 1003L) {
            transactionUseCases.updateTransaction(existingTransaction, sourceTransaction, profileId)
        }

        return transactionUseCases.saveTransfer(sourceTransaction, targetTransaction, profileId).first
    }

    suspend fun cloneTransaction(transactionId: Long, profileId: Int): Long? {
        val transaction = transactionUseCases.getTransactionById(transactionId) ?: return null
        return transactionUseCases.insertTransaction(
            transaction.copy(id = 0, timestamp = System.currentTimeMillis()), profileId
        ).also { widgetRefreshTrigger.trigger() }
    }

    suspend fun toggleStatus(transactionId: Long, profileId: Int): TransactionStatus? {
        val transaction = transactionUseCases.getTransactionById(transactionId) ?: return null
        val newStatus = when (transaction.status) {
            TransactionStatus.CLEARED -> TransactionStatus.UNCLEARED
            TransactionStatus.UNCLEARED -> TransactionStatus.CLEARED
            TransactionStatus.RECONCILE -> TransactionStatus.CLEARED
        }
        transactionUseCases.updateTransaction(transaction, transaction.copy(status = newStatus), profileId)
        return newStatus.also { widgetRefreshTrigger.trigger() }
    }

    suspend fun updateStatus(transactionId: Long, newStatus: TransactionStatus, profileId: Int): Boolean {
        val transaction = transactionUseCases.getTransactionById(transactionId) ?: return false
        transactionUseCases.updateTransaction(transaction, transaction.copy(status = newStatus), profileId)
        widgetRefreshTrigger.trigger()
        return true
    }

    // Maps a TransactionRequest onto a Transaction, reusing an existing row's identity
    // if provided so that updates preserve the original id and any unrelated fields.
    private fun TransactionRequest.toNormalTransaction(base: Transaction?): Transaction =
        (base ?: Transaction(
            id = 0,
            name = name,
            amount = BigDecimal.ZERO,
            timestamp = timestamp,
            currency = currency,
            transactionType = transactionType,
            accountId = accountId,
            categoryId = categoryId
        )).copy(
            amount = amount,
            transactionType = transactionType,
            accountId = accountId,
            currency = currency,
            categoryId = categoryId,
            tags = tags,
            paymentMethod = paymentMethod,
            timestamp = timestamp,
            payee = payee,
            note = note,
            latitude = latitude,
            longitude = longitude,
            warrantyExpiration = warrantyExpiration,
            status = status,
            sourcePlannedPaymentId = sourcePlannedPaymentId
        )

    private suspend fun <T> andRefresh(block: suspend () -> T): T =
        block().also { widgetRefreshTrigger.trigger() }
}
