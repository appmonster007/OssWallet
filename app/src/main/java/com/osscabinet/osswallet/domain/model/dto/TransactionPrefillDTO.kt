package com.osscabinet.ossWallet.domain.model.dto

import com.osscabinet.ossWallet.domain.model.TransactionStatus
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * DTO for prefilling transaction entry screen with existing transaction data.
 * Used for clone functionality and reusing the entry screen.
 */
@Serializable
data class TransactionPrefillDTO(
    val title: String = "",
    val amount: String,
    val categoryId: Long,
    val transactionType: String, // "income", "expense", or "transfer"
    val accountId: String,
    val currency: String,
    val tags: List<String> = emptyList(),
    val paymentMethod: String = "Cash",
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
                title = transaction.title,
                amount = transaction.amount.abs().toPlainString(),
                categoryId = transaction.categoryId,
                transactionType = if (transaction.categoryId == 1003L) "transfer" else transaction.transactionType,
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
            val typeStr = when (template.transactionType) {
                1 -> "income"
                2 -> "transfer"
                else -> "expense"
            }
            return TransactionPrefillDTO(
                title = template.name,
                amount = if (template.amount.signum() != 0) template.amount.abs().toPlainString() else "",
                categoryId = template.categoryId,
                transactionType = typeStr,
                accountId = template.accountId ?: "",
                currency = template.currency ?: "",
                tags = template.tags,
                paymentMethod = template.paymentMethod ?: "Cash",
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
            val template = payment.template
            val typeStr = when (template.transactionType) {
                1 -> "income"
                2 -> "transfer"
                else -> "expense"
            }
            return TransactionPrefillDTO(
                title = template.name,
                amount = template.amount.abs().toPlainString(),
                categoryId = template.categoryId,
                transactionType = typeStr,
                accountId = template.accountId ?: "",
                currency = template.currency ?: "USD",
                tags = template.tags,
                paymentMethod = template.paymentMethod ?: "Cash",
                payee = template.payee ?: "",
                note = template.note ?: "",
                targetAccountId = template.targetAccountId,
                targetCurrency = template.targetCurrency,
                exchangeRate = template.transferRate.toPlainString(),
                sourcePlannedPaymentId = payment.id,
                timestamp = scheduledDate ?: payment.schedule.nextDueDate
            )
        }
    }
}
