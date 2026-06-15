package com.osscabinet.ossWallet.domain.model.dto

import com.osscabinet.ossWallet.domain.model.toEntryMode
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * DTO for prefilling transaction entry screen with existing transaction data.
 * Used for clone functionality and reusing the entry screen.
 */
@Serializable
data class TransactionPrefillDTO(
    val name: String = "",
    val amount: String,
    val categoryId: Long,
    val entryMode: EntryMode,
    val accountId: String? = null,
    val currency: String,
    val tags: Set<String> = emptySet(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val payee: String = "",
    val note: String = "",
    val gpsLocation: String? = null,
    val location: String? = null,
    val warrantyExpiration: Long? = null,
    val status: TransactionStatus = TransactionStatus.CLEARED,
    // For transfers and linking
    val linkedTransactionIds: List<Long> = emptyList(),
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val exchangeRate: String = "1.0",
    // For clone functionality
    val clonedFromTransactionId: Long? = null,
    // Source identifier
    val sourceTemplateId: Long? = null,
    val sourcePlannedPaymentId: Long? = null,
    val timestamp: Long? = null,
    // Planned payment occurrence date — separate from timestamp so schedule advances correctly
    val plannedScheduledDate: Long? = null,
    val isSplitMode: Boolean = false
) {
    companion object {
        /**
         * Create a TransactionPrefillDTO from a Transaction domain model.
         * Used for cloning or splitting existing transactions.
         */
        fun fromTransaction(
            transaction: Transaction,
            clonedFromId: Long? = null,
            isSplitMode: Boolean = false
        ): TransactionPrefillDTO {
            return TransactionPrefillDTO(
                name = "", // Transactions don't have names
                amount = transaction.amount.abs().toPlainString(),
                categoryId = transaction.categoryId,
                entryMode = transaction.transactionType.toEntryMode(),
                accountId = transaction.accountId,
                currency = transaction.currency,
                tags = transaction.tags,
                paymentMethod = transaction.paymentMethod,
                payee = transaction.payee,
                note = transaction.note,
                gpsLocation = transaction.gpsLocation,
                location = transaction.location,
                warrantyExpiration = transaction.warrantyExpiration,
                status = transaction.status,
                targetAccountId = transaction.targetAccountId,
                targetCurrency = transaction.targetCurrency,
                exchangeRate = transaction.transferRate.toPlainString(),
                clonedFromTransactionId = clonedFromId ?: transaction.id,
                timestamp = transaction.timestamp, // Keep original timestamp for splits
                isSplitMode = isSplitMode
            )
        }

        /**
         * Create a TransactionPrefillDTO from a Template domain model.
         */
        fun fromTemplate(
            template: Template
        ): TransactionPrefillDTO {
            return TransactionPrefillDTO(
                name = template.name,
                amount = if (template.amount.signum() != 0) template.amount.abs().toPlainString() else "",
                categoryId = template.categoryId,
                entryMode = template.entryMode,
                accountId = template.accountId,
                currency = template.currency,
                tags = template.tags,
                paymentMethod = template.paymentMethod ?: PaymentMethod.CASH,
                payee = template.payee ?: "",
                note = template.note ?: "",
                targetAccountId = template.targetAccountId,
                targetCurrency = template.targetCurrency,
                exchangeRate = template.transferRate.toPlainString(),
                sourceTemplateId = template.id
            )
        }

        /**
         * Create a TransactionPrefillDTO from a PlannedPayment domain model.
         * This is used when manually confirming a planned payment.
         */
        fun fromPlannedPayment(
            payment: PlannedPayment,
            scheduledDate: Long? = null
        ): TransactionPrefillDTO {
            return TransactionPrefillDTO(
                name = payment.name,
                amount = payment.amount.abs().toPlainString(),
                categoryId = payment.categoryId,
                entryMode = payment.entryMode,
                accountId = payment.accountId,
                currency = payment.currency,
                tags = payment.tags,
                paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                payee = payment.payee ?: "",
                note = payment.note ?: "",
                targetAccountId = payment.targetAccountId,
                targetCurrency = payment.targetCurrency,
                exchangeRate = payment.transferRate.toPlainString(),
                sourcePlannedPaymentId = payment.id,
                timestamp = System.currentTimeMillis(),
                plannedScheduledDate = scheduledDate ?: payment.schedule.nextDueDate
            )
        }
    }
}
