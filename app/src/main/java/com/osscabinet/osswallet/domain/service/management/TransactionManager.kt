package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSplit
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
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
        transactionUseCases.runAtomically {
            val newId =
                if (request.isTransfer) handleTransferSave(existingTransaction, request, profileId)
                else handleNormalSave(existingTransaction, request, profileId)

            // Manage links
            if (existingTransaction != null) {
                updateLinks(newId, linkedTransactionIds)
            } else {
                linkedTransactionIds.forEach { transactionUseCases.addTransactionLink(newId, it) }
                clonedFromId?.let { transactionUseCases.addTransactionLink(newId, it) }
            }

            // Planned Payment advancement — atomic with the transaction save
            sourcePlannedPaymentId?.let { paymentId ->
                confirmPlannedPaymentTransaction(paymentId, newId, scheduledDate ?: request.timestamp)
            }

            newId
        }
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
                transactionUseCases.deleteTransaction(existingTransactionId, profileId)
            }

            val splitIds = splits.map { split ->
                val amount = split.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val request = baseRequest.copy(
                    name = categoryNameLookup(split.categoryId ?: baseRequest.categoryId),
                    amount = if (baseRequest.entryMode == EntryMode.INCOME) amount else amount.negate(),
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
            if (Category.isTransfer(existingTransaction.categoryId) && !Category.isTransfer(request.categoryId)) {
                // Conversion Logic: If it was a linked transfer and is now becoming Normal
                // 1. Delink bidirectionally. Counterparts automatically become standalone transfers.
                transactionUseCases.clearTransactionLinks(existingTransaction.id)
            } else if (Category.isTransfer(existingTransaction.categoryId)) {
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

        val baseCategory = if (Category.isInitialBalance(request.categoryId))
            Category.SYSTEM_INITIAL_BALANCE else Category.SYSTEM_TRANSFER

        return when {
            // Outflow: Wallet -> Outside
            sourceAccountId != null && targetAccountId == null -> {
                val tx = request.toNormalTransaction(existingTransaction).copy(
                    amount = amount.negate(),
                    transactionType = TransactionType.TRANSFER_OUTFLOW,
                    categoryId = baseCategory,
                    targetCurrency = request.targetCurrency
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
                    transactionType = TransactionType.TRANSFER_INFLOW,
                    currency = request.targetCurrency ?: request.currency,
                    categoryId = baseCategory,
                    targetAccountId = null,
                    targetCurrency = request.currency
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
                // If it's an internal transfer, we force the signs: Source is -ve, Target is +ve
                val sourceTransaction = request.toNormalTransaction(existingTransaction).copy(
                    amount = amount.negate(),
                    transactionType = TransactionType.TRANSFER_OUTFLOW,
                    categoryId = baseCategory,
                    targetAccountId = targetAccountId,
                    targetCurrency = request.targetCurrency
                )

                // Target inherits all shared metadata from source
                val targetTransaction = sourceTransaction.copy(
                    id = 0,
                    amount = amount.multiply(request.transferRate)
                        .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros(),
                    transactionType = TransactionType.TRANSFER_INFLOW,
                    accountId = targetAccountId,
                    currency = request.targetCurrency ?: request.currency,
                    categoryId = baseCategory,
                    targetAccountId = sourceAccountId,
                    targetCurrency = request.currency,
                    transferRate = if (request.transferRate.signum() != 0) BigDecimal.ONE.divide(
                        request.transferRate,
                        8,
                        java.math.RoundingMode.HALF_UP
                    ).stripTrailingZeros() else BigDecimal.ONE
                )

                if (existingTransaction != null && Category.isTransfer(existingTransaction.categoryId)) {
                    // When updating a linked transfer, find the other side and update it too
                    val linkedIds =
                        transactionUseCases.getLinkedTransactionIds(existingTransaction.id).first()
                    
                    if (linkedIds.isEmpty()) {
                        // Conversion from Standalone (Outside Wallet) to Internal: Create the missing leg
                        val targetId = transactionUseCases.insertTransaction(targetTransaction, profileId)
                        transactionUseCases.addTransactionLink(existingTransaction.id, targetId)
                        transactionUseCases.addTransactionLink(targetId, existingTransaction.id)
                    } else {
                        linkedIds.forEach { linkedId ->
                            val linkedTx = transactionUseCases.getTransactionById(linkedId, profileId)
                            if (linkedTx != null && Category.isTransfer(linkedTx.categoryId)) {
                                // Update the target side to match the new metadata from source
                                val updatedTarget = targetTransaction.copy(id = linkedId)
                                transactionUseCases.updateTransaction(
                                    linkedTx,
                                    updatedTarget,
                                    profileId
                                )
                            }
                        }
                    }
                    transactionUseCases.updateTransaction(
                        existingTransaction,
                        sourceTransaction,
                        profileId
                    )
                    existingTransaction.id
                } else {
                    transactionUseCases.saveTransfer(
                        sourceTransaction,
                        targetTransaction,
                        profileId
                    ).first
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

        // Try to find existing occurrence first to update it instead of creating duplicate
        val existing = plannedPaymentRepository.getOccurrenceForDate(paymentId, scheduledDate)

        // Mark occurrence as confirmed
        val occurrence = PlannedPaymentOccurrence(
            id = existing?.id ?: 0,
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

        // Try to find existing occurrence first to update it instead of creating duplicate
        val existing = plannedPaymentRepository.getOccurrenceForDate(paymentId, scheduledDate)

        val occurrence = PlannedPaymentOccurrence(
            id = existing?.id ?: 0,
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

    suspend fun deleteTransaction(id: Long, profileId: Int) = andRefresh {
        val transaction = transactionUseCases.getTransactionById(id, profileId)
        if (transaction != null && Category.isTransfer(transaction.categoryId)) {
            // Decouple from linked transfers instead of deleting them.
            // This converts internal transfers into standalone "Outside Wallet" transfers.
            val linkedIds = transactionUseCases.getLinkedTransactionIds(id).first()
            linkedIds.forEach { linkedId ->
                // Remove bidirectional links
                transactionUseCases.removeTransactionLink(id, linkedId)
                transactionUseCases.removeTransactionLink(linkedId, id)
            }
        }
        transactionUseCases.deleteTransaction(id, profileId)
    }

    suspend fun cloneTransaction(transactionId: Long, profileId: Int): Long? {
        val transaction = transactionUseCases.getTransactionById(transactionId, profileId) ?: return null
        return transactionUseCases.insertTransaction(
            transaction.copy(id = 0, timestamp = System.currentTimeMillis()), profileId
        ).also { widgetRefreshTrigger.trigger() }
    }

    suspend fun toggleStatus(transactionId: Long, profileId: Int): TransactionStatus? {
        val transaction = transactionUseCases.getTransactionById(transactionId, profileId) ?: return null
        val newStatus = when (transaction.status) {
            TransactionStatus.CLEARED -> TransactionStatus.UNCLEARED
            TransactionStatus.UNCLEARED -> TransactionStatus.CLEARED
            TransactionStatus.RECONCILE -> TransactionStatus.CLEARED
        }
        transactionUseCases.updateTransaction(
            transaction,
            transaction.copy(status = newStatus),
            profileId
        )
        return newStatus.also { widgetRefreshTrigger.trigger() }
    }

    suspend fun updateStatus(
        transactionId: Long,
        newStatus: TransactionStatus,
        profileId: Int
    ): Boolean {
        val transaction = transactionUseCases.getTransactionById(transactionId, profileId) ?: return false
        transactionUseCases.updateTransaction(
            transaction,
            transaction.copy(status = newStatus),
            profileId
        )
        widgetRefreshTrigger.trigger()
        return true
    }

    // Maps a TransactionRequest onto a Transaction, reusing an existing row's identity
    // if provided so that updates preserve the original id and any unrelated fields.
    // Transfer rows default to TRANSFER_OUTFLOW here; handleTransferSave overrides per leg.
    private fun TransactionRequest.toNormalTransaction(base: Transaction?): Transaction {
        val defaultType = when (entryMode) {
            EntryMode.EXPENSE -> TransactionType.EXPENSE
            EntryMode.INCOME -> TransactionType.INCOME
            EntryMode.TRANSFER -> TransactionType.TRANSFER_OUTFLOW
        }
        return (base ?: Transaction(
            id = 0,
            name = name,
            amount = BigDecimal.ZERO,
            timestamp = timestamp,
            currency = currency,
            transactionType = defaultType,
            accountId = accountId,
            categoryId = categoryId
        )).copy(
            amount = amount,
            transactionType = defaultType,
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
            sourcePlannedPaymentId = sourcePlannedPaymentId,
            targetAccountId = targetAccountId,
            targetCurrency = targetCurrency,
            transferRate = transferRate
        )
    }

    private suspend fun <T> andRefresh(block: suspend () -> T): T =
        block().also { widgetRefreshTrigger.trigger() }
}
