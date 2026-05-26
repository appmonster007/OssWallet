package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

class TransactionManager(
    private val transactionUseCases: TransactionUseCases,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val widgetRefreshTrigger: WidgetRefreshTrigger
) {

    suspend fun handleSaveTransaction(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int,
        linkedTransactionIds: List<Long> = emptyList(),
        clonedFromId: Long? = null,
        sourcePlannedPaymentId: Long? = null,
        scheduledDate: Long? = null
    ): Long = andRefresh {
        val newId = if (request.isTransfer) handleTransferSave(existingTransaction, request, profileId)
        else handleNormalSave(existingTransaction, request, profileId)

        // Manage links
        if (existingTransaction != null) {
            updateLinks(newId, linkedTransactionIds)
        } else {
            linkedTransactionIds.forEach { transactionUseCases.addTransactionLink(newId, it) }
            clonedFromId?.let { transactionUseCases.addTransactionLink(newId, it) }
        }

        // Planned Payment side effects
        sourcePlannedPaymentId?.let { paymentId ->
            confirmPlannedPaymentTransaction(paymentId, newId, scheduledDate ?: request.timestamp)
        }

        newId
    }

    suspend fun handleSaveSplitTransactions(
        existingTransactionId: Long?,
        splits: List<TransactionSplit>,
        baseRequest: TransactionRequest,
        profileId: Int,
        categoryNameLookup: (Long) -> String
    ): Long = andRefresh {
        transactionUseCases.runAtomically {
            if (existingTransactionId != null) {
                transactionUseCases.deleteTransaction(existingTransactionId)
            }

            val splitIds = splits.map { split ->
                val amount = split.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val request = baseRequest.copy(
                    name = categoryNameLookup(split.categoryId ?: baseRequest.categoryId),
                    amount = if (baseRequest.transactionType == TransactionType.INCOME) amount else amount.negate(),
                    categoryId = split.categoryId ?: baseRequest.categoryId,
                    note = split.note
                )
                // Recursive call for each split record
                handleNormalSave(null, request, profileId)
            }

            // Link them
            if (splitIds.size > 1) {
                for (i in splitIds.indices) {
                    for (j in i + 1 until splitIds.size) {
                        transactionUseCases.addTransactionLink(splitIds[i], splitIds[j])
                    }
                }
            }

            splitIds.firstOrNull() ?: -1L
        }
    }

    private suspend fun updateLinks(transactionId: Long, newLinkedIds: List<Long>) {
        val currentIds = transactionUseCases.getLinkedTransactionIds(transactionId).first()
        val toRemove = currentIds.toSet() - newLinkedIds.toSet()
        val toAdd = newLinkedIds.toSet() - currentIds.toSet()

        for (id in toRemove) {
            transactionUseCases.removeTransactionLink(transactionId, id)
        }
        for (id in toAdd) {
            transactionUseCases.addTransactionLink(transactionId, id)
        }
    }

    private suspend fun handleNormalSave(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int
    ): Long {
        val updated = request.toNormalTransaction(existingTransaction)
        return if (existingTransaction != null) {
            if (existingTransaction.categoryId == Category.SYSTEM_TRANSFER && request.categoryId != Category.SYSTEM_TRANSFER) {
                // Conversion Logic: If it was a linked transfer and is now becoming Normal
                // 1. Delink bidirectionally. Counterparts automatically become standalone transfers.
                transactionUseCases.clearTransactionLinks(existingTransaction.id)
            } else if (existingTransaction.categoryId == Category.SYSTEM_TRANSFER) {
                // If it's still a transfer (maybe just account change), clear links to be rebuilt if needed
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
        val sourceAccountId = request.accountId
        val targetAccountId = request.targetAccountId
        val amount = request.amount.abs()

        return when {
            // Outflow: Wallet -> Outside
            sourceAccountId != null && targetAccountId == null -> {
                val tx = request.toNormalTransaction(existingTransaction).copy(
                    amount = amount.negate(),
                    categoryId = Category.SYSTEM_TRANSFER
                )
                if (existingTransaction != null) {
                    transactionUseCases.clearTransactionLinks(existingTransaction.id)
                    transactionUseCases.updateTransaction(existingTransaction, tx, profileId)
                    existingTransaction.id
                } else {
                    transactionUseCases.insertTransaction(tx, profileId)
                }
            }
            // Inflow: Outside -> Wallet
            sourceAccountId == null && targetAccountId != null -> {
                val tx = request.toNormalTransaction(existingTransaction).copy(
                    accountId = targetAccountId,
                    amount = amount,
                    currency = request.targetCurrency ?: request.currency,
                    categoryId = Category.SYSTEM_TRANSFER
                )
                if (existingTransaction != null) {
                    transactionUseCases.clearTransactionLinks(existingTransaction.id)
                    transactionUseCases.updateTransaction(existingTransaction, tx, profileId)
                    existingTransaction.id
                } else {
                    transactionUseCases.insertTransaction(tx, profileId)
                }
            }
            // Internal: Wallet -> Wallet
            sourceAccountId != null && targetAccountId != null -> {
                val sourceTransaction = request.toNormalTransaction(existingTransaction).copy(
                    amount = amount.negate(),
                    categoryId = Category.SYSTEM_TRANSFER
                )

                // Target inherits all shared metadata from source; only the amount, direction,
                // account, and currency differ — and id must be 0 since it is always a new row.
                val targetTransaction = sourceTransaction.copy(
                    id = 0,
                    amount = amount.multiply(request.transferRate),
                    accountId = targetAccountId,
                    currency = request.targetCurrency ?: request.currency
                )

                if (existingTransaction != null && existingTransaction.categoryId == Category.SYSTEM_TRANSFER) {
                    // When updating a linked transfer, find the other side and update it too
                    val linkedIds = transactionUseCases.getLinkedTransactionIds(existingTransaction.id).first()
                    linkedIds.forEach { linkedId ->
                        val linkedTx = transactionUseCases.getTransactionById(linkedId)
                        if (linkedTx != null && linkedTx.categoryId == Category.SYSTEM_TRANSFER) {
                            // Update the target side to match the new metadata from source
                            val updatedTarget = sourceTransaction.copy(
                                id = linkedId,
                                amount = amount.multiply(request.transferRate),
                                accountId = targetAccountId,
                                currency = request.targetCurrency ?: request.currency
                            )
                            transactionUseCases.updateTransaction(linkedTx, updatedTarget, profileId)
                        }
                    }
                    transactionUseCases.updateTransaction(existingTransaction, sourceTransaction, profileId)
                    existingTransaction.id
                } else {
                    transactionUseCases.saveTransfer(sourceTransaction, targetTransaction, profileId).first
                }
            }
            else -> -1L // Invalid state
        }
    }

    suspend fun confirmPlannedPaymentTransaction(
        paymentId: Long,
        transactionId: Long,
        scheduledDate: Long
    ) {
        val payment = plannedPaymentRepository.getById(paymentId) ?: return
        
        // Mark occurrence as confirmed
        val occurrence = PlannedPaymentOccurrence(
            id = 0,
            plannedPaymentId = paymentId,
            scheduledDate = scheduledDate,
            status = OccurrenceStatus.CONFIRMED,
            generatedTransactionId = transactionId
        )
        plannedPaymentRepository.upsertOccurrence(occurrence)

        // Update planned payment next due date
        val nextDue = PlannedPaymentService.calculateNextDueDate(payment, scheduledDate)
        if (nextDue == null) {
            plannedPaymentRepository.update(payment.copy(status = PlannedPaymentStatus.COMPLETED))
        } else {
            plannedPaymentRepository.update(
                payment.copy(
                    schedule = payment.schedule.copy(
                        nextDueDate = nextDue,
                        lastTriggeredDate = scheduledDate
                    )
                )
            )
        }
    }

    suspend fun skipPlannedPayment(
        paymentId: Long,
        scheduledDate: Long
    ) {
        val payment = plannedPaymentRepository.getById(paymentId) ?: return
        
        val occurrence = PlannedPaymentOccurrence(
            id = 0,
            plannedPaymentId = paymentId,
            scheduledDate = scheduledDate,
            status = OccurrenceStatus.SKIPPED
        )
        plannedPaymentRepository.upsertOccurrence(occurrence)

        val nextDue = PlannedPaymentService.calculateNextDueDate(payment, scheduledDate)
        if (nextDue == null) {
            plannedPaymentRepository.update(payment.copy(status = PlannedPaymentStatus.COMPLETED))
        } else {
            plannedPaymentRepository.update(
                payment.copy(
                    schedule = payment.schedule.copy(
                        nextDueDate = nextDue,
                        lastTriggeredDate = scheduledDate
                    )
                )
            )
        }
    }

    suspend fun deleteTransaction(id: Long) = andRefresh {
        val transaction = transactionUseCases.getTransactionById(id)
        if (transaction != null && transaction.categoryId == Category.SYSTEM_TRANSFER) {
            // Decouple from linked transfers instead of deleting them.
            // This converts internal transfers into standalone "Outside Wallet" transfers.
            val linkedIds = transactionUseCases.getLinkedTransactionIds(id).first()
            linkedIds.forEach { linkedId ->
                // Remove bidirectional links
                transactionUseCases.removeTransactionLink(id, linkedId)
                transactionUseCases.removeTransactionLink(linkedId, id)
                
                // The linked transaction remains as a standalone "Outside" transfer
                // because it is now unlinked but still has category SYSTEM_TRANSFER.
            }
        }
        transactionUseCases.deleteTransaction(id)
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
            gpsLocation = gpsLocation,
            location = location,
            warrantyExpiration = warrantyExpiration,
            status = status,
            sourcePlannedPaymentId = sourcePlannedPaymentId
        )

    private suspend fun <T> andRefresh(block: suspend () -> T): T =
        block().also { widgetRefreshTrigger.trigger() }
}
