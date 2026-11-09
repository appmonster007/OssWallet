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
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.service.WidgetRefreshTrigger
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode

class TransactionManager(
    internal val transactionUseCases: TransactionUseCases,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val widgetRefreshTrigger: WidgetRefreshTrigger
) {

    fun getAllTransactionLinks(): Flow<Map<Long, List<Long>>> = transactionUseCases.getAllTransactionLinks()

    suspend fun handleSaveTransaction(
        existingTransaction: Transaction?,
        request: TransactionRequest,
        profileId: Int,
        linkedTransactionIds: List<Long> = emptyList(),
        clonedFromId: Long? = null,
        sourcePlannedPaymentId: Long? = null,
        scheduledDate: Long? = null,
        replaceExistingInitialBalance: Boolean = false
    ): Long = andRefresh {
        val request = request.normalizedForSave()
        transactionUseCases.runAtomically {
            val newId =
                if (request.isTransfer) handleTransferSave(
                    existingTransaction, request, profileId, replaceExistingInitialBalance
                )
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

            alignOpeningBalances(
                setOfNotNull(existingTransaction?.accountId, request.accountId, request.targetAccountId),
                profileId
            )

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
                    amount = TransactionEntryService.getMagnitude(amount),
                    categoryId = split.categoryId ?: baseRequest.categoryId,
                    note = split.note
                )
                handleNormalSave(null, request, profileId)
            }

            splitIds.forEachIndexed { i, first ->
                splitIds.drop(i + 1).forEach { second -> transactionUseCases.addTransactionLink(first, second) }
            }

            alignOpeningBalances(setOfNotNull(baseRequest.accountId), profileId)

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
            // Only a transfer-type leg has links. Opening balances and adjustments never do, so
            // a plain save of them leaves any links alone.
            if (existingTransaction.transactionType.isTransfer) {
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
        profileId: Int,
        replaceExistingInitialBalance: Boolean
    ): Long {
        val sourceAccountId = request.accountId?.takeIf { it.isNotBlank() }
        val targetAccountId = request.targetAccountId?.takeIf { it.isNotBlank() }
        val amount = request.amount.abs()

        // Transfer mode has no category picker, so the category is derived from the shape: only a
        // single-leg (Outside Wallet) row can be an opening balance/adjustment; anything that is not
        // one of the three transfer categories is stored as plain Transfer.
        val category = Category.transferCategoryFor(
            request.categoryId,
            isTwoSided = sourceAccountId != null && targetAccountId != null
        )
        val outflow = request.toNormalTransaction(existingTransaction).copy(
            amount = amount.negate(),
            transactionType = TransactionType.TRANSFER_OUTFLOW,
            categoryId = category,
            targetAccountId = targetAccountId
        )

        return when {
            // Wallet -> Outside
            sourceAccountId != null && targetAccountId == null -> saveSingleLeg(
                outflow, existingTransaction, profileId, replaceExistingInitialBalance
            )
            // Outside -> Wallet
            sourceAccountId == null && targetAccountId != null -> saveSingleLeg(
                outflow.copy(
                    accountId = targetAccountId,
                    amount = amount,
                    transactionType = TransactionType.TRANSFER_INFLOW,
                    currency = request.targetCurrency ?: request.currency,
                    targetAccountId = null,
                    targetCurrency = request.currency
                ),
                existingTransaction, profileId, replaceExistingInitialBalance
            )
            // Wallet -> Wallet
            sourceAccountId != null && targetAccountId != null ->
                saveInternalTransfer(outflow, request, existingTransaction, profileId)

            else -> -1L // Invalid state
        }
    }

    // A single-leg row never has a counterpart, so any link from a former two-sided shape is dropped.
    private suspend fun saveSingleLeg(
        tx: Transaction,
        existingTransaction: Transaction?,
        profileId: Int,
        replaceExistingInitialBalance: Boolean
    ): Long {
        resolveInitialBalanceConflict(tx, existingTransaction, profileId, replaceExistingInitialBalance)
        if (existingTransaction == null) return transactionUseCases.insertTransaction(tx, profileId)
        transactionUseCases.clearTransactionLinks(existingTransaction.id)
        transactionUseCases.updateTransaction(existingTransaction, tx, profileId)
        return existingTransaction.id
    }

    // Source leg is negative, target leg positive; the target inherits the source's metadata.
    private suspend fun saveInternalTransfer(
        sourceTransaction: Transaction,
        request: TransactionRequest,
        existingTransaction: Transaction?,
        profileId: Int
    ): Long {
        val targetTransaction = sourceTransaction.copy(
            id = 0,
            amount = request.amount.abs().multiply(request.transferRate)
                .setScale(8, RoundingMode.HALF_UP).stripTrailingZeros(),
            transactionType = TransactionType.TRANSFER_INFLOW,
            accountId = sourceTransaction.targetAccountId,
            currency = request.targetCurrency ?: request.currency,
            targetAccountId = sourceTransaction.accountId,
            targetCurrency = request.currency,
            transferRate = if (request.transferRate.signum() != 0) {
                BigDecimal.ONE.divide(request.transferRate, 8, RoundingMode.HALF_UP).stripTrailingZeros()
            } else {
                BigDecimal.ONE
            }
        )

        if (existingTransaction == null || !existingTransaction.transactionType.isTransfer) {
            return transactionUseCases.saveTransfer(sourceTransaction, targetTransaction, profileId).first
        }

        val linkedIds = transactionUseCases.getLinkedTransactionIds(existingTransaction.id).first()
        if (linkedIds.isEmpty()) {
            // Converting a single-leg row to an internal transfer: create the missing leg.
            val targetId = transactionUseCases.insertTransaction(targetTransaction, profileId)
            transactionUseCases.addTransactionLink(existingTransaction.id, targetId)
            transactionUseCases.addTransactionLink(targetId, existingTransaction.id)
        } else {
            linkedIds.forEach { linkedId ->
                val linkedTx = transactionUseCases.getTransactionById(linkedId, profileId)
                if (linkedTx != null && linkedTx.transactionType.isTransfer) {
                    transactionUseCases.updateTransaction(linkedTx, targetTransaction.copy(id = linkedId), profileId)
                }
            }
        }
        transactionUseCases.updateTransaction(existingTransaction, sourceTransaction, profileId)
        return existingTransaction.id
    }

    suspend fun confirmPlannedPaymentTransaction(
        paymentId: Long,
        transactionId: Long,
        scheduledDate: Long
    ) = resolveOccurrence(paymentId, scheduledDate, OccurrenceStatus.CONFIRMED, transactionId)

    suspend fun skipPlannedPayment(paymentId: Long, scheduledDate: Long) =
        resolveOccurrence(paymentId, scheduledDate, OccurrenceStatus.SKIPPED, generatedTransactionId = null)

    // Records the occurrence (updating one that already exists rather than duplicating it), then
    // moves the planned payment on to its next due date or completes it.
    private suspend fun resolveOccurrence(
        paymentId: Long,
        scheduledDate: Long,
        status: OccurrenceStatus,
        generatedTransactionId: Long?
    ) {
        val payment = plannedPaymentRepository.getById(paymentId) ?: return
        val existing = plannedPaymentRepository.getOccurrenceForDate(paymentId, scheduledDate)
        plannedPaymentRepository.upsertOccurrence(
            PlannedPaymentOccurrence(
                id = existing?.id ?: 0,
                plannedPaymentId = paymentId,
                scheduledDate = scheduledDate,
                status = status,
                generatedTransactionId = generatedTransactionId
            )
        )

        val nextDue = PlannedPaymentService.calculateNextDueDate(payment, scheduledDate)
        plannedPaymentRepository.update(
            if (nextDue == null) {
                payment.copy(status = PlannedPaymentStatus.COMPLETED)
            } else {
                payment.copy(
                    schedule = payment.schedule.copy(nextDueDate = nextDue, lastTriggeredDate = scheduledDate)
                )
            }
        )
    }

    suspend fun deleteTransaction(id: Long, profileId: Int) = andRefresh {
        transactionUseCases.runAtomically {
            val transaction = transactionUseCases.getTransactionById(id, profileId)
            if (transaction != null && transaction.transactionType.isTransfer) {
                detachCounterparts(transaction, profileId)
            }
            transactionUseCases.deleteTransaction(id, profileId)
            transaction?.accountId?.let { alignOpeningBalances(setOf(it), profileId) }
        }
    }

    // Deleting one leg of a transfer keeps the other: it is decoupled and becomes a standalone leg
    // against Outside Wallet. It must also forget the deleted leg's account, or editing it later would
    // see a two-sided transfer with no counterpart and quietly re-create the leg that was deleted.
    private suspend fun detachCounterparts(transaction: Transaction, profileId: Int) {
        transactionUseCases.getLinkedTransactionIds(transaction.id).first().forEach { linkedId ->
            transactionUseCases.removeTransactionLink(transaction.id, linkedId)
            transactionUseCases.removeTransactionLink(linkedId, transaction.id)

            val counterpart = transactionUseCases.getTransactionById(linkedId, profileId)
            if (counterpart?.targetAccountId != null) {
                transactionUseCases.updateTransaction(counterpart, counterpart.copy(targetAccountId = null), profileId)
            }
        }
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
            TransactionStatus.UNCLEARED, TransactionStatus.RECONCILE -> TransactionStatus.CLEARED
        }
        applyStatus(transaction, newStatus, profileId)
        return newStatus
    }

    suspend fun updateStatus(
        transactionId: Long,
        newStatus: TransactionStatus,
        profileId: Int
    ): Boolean {
        val transaction = transactionUseCases.getTransactionById(transactionId, profileId) ?: return false
        applyStatus(transaction, newStatus, profileId)
        return true
    }

    private suspend fun applyStatus(transaction: Transaction, status: TransactionStatus, profileId: Int) {
        transactionUseCases.updateTransaction(transaction, transaction.copy(status = status), profileId)
        widgetRefreshTrigger.trigger()
    }

    // The transfer categories are transfer-typed by definition. An Income/Expense request that
    // carries one (an import, the API, or a picker choice) is turned into the single-leg transfer
    // the entry form would have produced: income is an inflow into the account, expense an outflow.
    private fun TransactionRequest.normalizedForSave(): TransactionRequest {
        if (isTransfer || !Category.isTransfer(categoryId)) return this
        return when (entryMode) {
            EntryMode.INCOME -> copy(
                entryMode = EntryMode.TRANSFER,
                accountId = null,
                targetAccountId = accountId,
                targetCurrency = currency
            )
            else -> copy(entryMode = EntryMode.TRANSFER, targetAccountId = null)
        }
    }

    // One opening balance per account+currency. A save that would create a second one either
    // replaces the first (the caller confirmed) or fails with the row it collides with.
    private suspend fun resolveInitialBalanceConflict(
        tx: Transaction,
        existingTransaction: Transaction?,
        profileId: Int,
        replace: Boolean
    ) {
        if (!Category.isInitialBalance(tx.categoryId)) return
        val accountId = tx.accountId ?: return
        val other = transactionUseCases.getInitialBalanceTransaction(accountId, tx.currency, profileId)
            ?.takeIf { it.id != existingTransaction?.id } ?: return
        if (!replace) throw DuplicateInitialBalanceException(other)
        transactionUseCases.deleteTransaction(other.id, profileId)
    }

    /**
     * An opening balance sits one day before the earliest real row of its account, wherever that
     * row moves to: it follows an earlier backdated entry and falls back when the earliest row is
     * deleted. Adjustments are not "real" rows here, so they never drag it forward.
     */
    internal suspend fun alignOpeningBalances(accountIds: Set<String>, profileId: Int) {
        accountIds.filter { it.isNotBlank() }.forEach { accountId ->
            val earliest = transactionUseCases.getEarliestNonInitialBalanceTimestamp(accountId, profileId)
                ?: return@forEach
            val target = earliest - ONE_DAY_MILLIS
            transactionUseCases.getInitialBalanceCurrencies(accountId, profileId).forEach { currency ->
                val opening = transactionUseCases.getInitialBalanceTransaction(accountId, currency, profileId)
                if (opening != null && opening.timestamp != target) {
                    transactionUseCases.updateTransaction(opening, opening.copy(timestamp = target), profileId)
                }
            }
        }
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
            amount = when (entryMode) {
                EntryMode.INCOME -> amount.abs()
                EntryMode.EXPENSE, EntryMode.TRANSFER -> amount.abs().negate()
            },
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

private const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000

/** Raised when a save would give an account a second opening balance in the same currency. */
class DuplicateInitialBalanceException(val existing: Transaction) :
    IllegalStateException("Account already has an opening balance in ${existing.currency}")
