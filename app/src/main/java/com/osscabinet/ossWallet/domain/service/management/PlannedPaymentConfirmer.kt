package com.osscabinet.ossWallet.domain.service.management

import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton

sealed interface ConfirmResult {
    data class Confirmed(val transactionId: Long, val paymentName: String) : ConfirmResult

    /** The occurrence was already confirmed or skipped (e.g. from the app) before this ran. */
    data class AlreadyHandled(val paymentName: String) : ConfirmResult
    data object NotFound : ConfirmResult

    /** The payment has no account to post to, so it needs the full form. */
    data class NeedsReview(val paymentName: String) : ConfirmResult
}

/** Turns a planned payment's occurrence into a real transaction: the one place that does it. */
@Singleton
class PlannedPaymentConfirmer @Inject constructor(
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val templateRepository: TemplateRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionManager: TransactionManager
) {
    suspend fun confirm(paymentId: Long, scheduledDate: Long, profileId: Int): ConfirmResult {
        val payment = plannedPaymentRepository.getById(paymentId) ?: return ConfirmResult.NotFound
        return confirm(payment, scheduledDate, profileId)
    }

    suspend fun confirm(payment: PlannedPayment, scheduledDate: Long, profileId: Int): ConfirmResult {
        // Only a confirmed occurrence blocks: a skipped one can still be confirmed on purpose.
        val occurrence = plannedPaymentRepository.getOccurrenceForDate(payment.id, scheduledDate)
        if (occurrence?.status == OccurrenceStatus.CONFIRMED) return ConfirmResult.AlreadyHandled(payment.name)
        val request = buildTransactionRequest(payment) ?: return ConfirmResult.NeedsReview(payment.name)
        val transactionId = transactionManager.handleSaveTransaction(
            existingTransaction = null,
            request = request,
            profileId = profileId,
            sourcePlannedPaymentId = payment.id,
            scheduledDate = scheduledDate
        )
        return ConfirmResult.Confirmed(transactionId, payment.name)
    }

    /** The payment's own details, overridden by its template where it has one. Null when there's no account. */
    private suspend fun buildTransactionRequest(payment: PlannedPayment): TransactionRequest? {
        val template = payment.templateId?.let { templateRepository.getTemplateById(it) }

        val amount = template?.amount ?: payment.amount
        val entryMode = template?.entryMode ?: payment.entryMode
        val accountId =
            (template?.accountId ?: payment.accountId).takeIf { !it.isNullOrBlank() } ?: return null
        val currency = template?.currency ?: payment.currency
        val categoryId = template?.categoryId ?: payment.categoryId
        val tags = template?.tags ?: payment.tags
        val paymentMethod = template?.paymentMethod ?: payment.paymentMethod ?: PaymentMethod.CASH
        val payee = template?.payee ?: payment.payee ?: ""
        val note = template?.note ?: payment.note ?: ""
        val gpsLocation = template?.gpsLocation ?: payment.gpsLocation
        val warrantyExp = template?.warrantyExpiration ?: payment.warrantyExpiration
        val status = template?.transactionStatus ?: payment.transactionStatus
        val targetAccountId = template?.targetAccountId ?: payment.targetAccountId
        val targetCurrency = template?.targetCurrency ?: payment.targetCurrency
        val transferRate = template?.transferRate ?: payment.transferRate

        val categoryName = categoryRepository.getAllCategoriesSync()
            .find { it.id == categoryId }?.name ?: "Planned Payment"

        return TransactionRequest(
            name = categoryName,
            amount = TransactionEntryService.getMagnitude(amount),
            entryMode = entryMode,
            accountId = accountId,
            currency = currency,
            categoryId = categoryId,
            tags = tags,
            paymentMethod = paymentMethod,
            timestamp = System.currentTimeMillis(),
            payee = payee,
            note = note,
            gpsLocation = gpsLocation,
            warrantyExpiration = warrantyExp,
            status = status,
            targetAccountId = targetAccountId,
            targetCurrency = targetCurrency,
            transferRate = transferRate
        )
    }
}
