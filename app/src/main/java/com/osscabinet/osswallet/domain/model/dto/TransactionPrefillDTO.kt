package com.osscabinet.ossWallet.domain.model.dto

import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
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
    val transactionType: TransactionType,
    val accountId: String? = null,
    val currency: String,
    val tags: Set<String> = emptySet(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val payee: String = "",
    val note: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
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
    val isSplitMode: Boolean = false
) {
    companion object {
        /**
         * Create a TransactionPrefillDTO from a Transaction domain model.
         * Used for cloning or splitting existing transactions.
         */
        fun fromTransaction(
            transaction: com.osscabinet.ossWallet.domain.model.Transaction,
            clonedFromId: Long? = null,
            isSplitMode: Boolean = false
        ): TransactionPrefillDTO {
            return TransactionPrefillDTO(
                name = "", // Transactions don't have names
                amount = transaction.amount.abs().toPlainString(),
                categoryId = transaction.categoryId,
                transactionType = if (transaction.categoryId == 1003L) TransactionType.TRANSFER else transaction.transactionType,
                accountId = transaction.accountId,
                currency = transaction.currency,
                tags = transaction.tags,
                paymentMethod = transaction.paymentMethod,
                payee = transaction.payee,
                note = transaction.note,
                latitude = transaction.latitude,
                longitude = transaction.longitude,
                warrantyExpiration = transaction.warrantyExpiration,
                status = transaction.status,
                clonedFromTransactionId = clonedFromId ?: transaction.id,
                timestamp = transaction.timestamp, // Keep original timestamp for splits
                isSplitMode = isSplitMode
            )
        }

        /**
         * Create a TransactionPrefillDTO from a Template domain model.
         */
        fun fromTemplate(
            template: com.osscabinet.ossWallet.domain.model.Template
        ): TransactionPrefillDTO {
            return TransactionPrefillDTO(
                name = template.name,
                amount = if (template.amount.signum() != 0) template.amount.abs().toPlainString() else "",
                categoryId = template.categoryId,
                transactionType = template.transactionType,
                accountId = template.accountId ?: "",
                currency = template.currency ?: "",
                tags = template.tags,
                paymentMethod = template.paymentMethod ?: PaymentMethod.CASH,
                payee = template.payee ?: "",
                note = template.note ?: "",
                sourceTemplateId = template.id
            )
        }

        /**
         * Create a TransactionPrefillDTO from a PlannedPayment domain model.
         * This is used when manually confirming a planned payment.
         */
        fun fromPlannedPayment(
            payment: com.osscabinet.ossWallet.domain.model.PlannedPayment,
            scheduledDate: Long? = null
        ): TransactionPrefillDTO {
            return TransactionPrefillDTO(
                name = payment.name,
                amount = payment.amount.abs().toPlainString(),
                categoryId = payment.categoryId,
                transactionType = payment.transactionType,
                accountId = payment.accountId ?: "",
                currency = payment.currency,
                tags = payment.tags,
                paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                payee = payment.payee ?: "",
                note = payment.note ?: "",
                targetAccountId = payment.targetAccountId,
                targetCurrency = payment.targetCurrency,
                exchangeRate = payment.transferRate.toPlainString(),
                sourcePlannedPaymentId = payment.id,
                timestamp = scheduledDate ?: payment.schedule.nextDueDate
            )
        }
    }
}
