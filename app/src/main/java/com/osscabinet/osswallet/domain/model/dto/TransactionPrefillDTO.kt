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
    val clonedFromTransactionId: Long? = null
) {
    companion object {
        /**
         * Create a TransactionPrefillDTO from a Transaction domain model.
         * This is used when cloning a transaction.
         */
        fun fromTransaction(transaction: com.osscabinet.ossWallet.domain.model.Transaction, clonedFromTransactionId: Long? = null): TransactionPrefillDTO {
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
                clonedFromTransactionId = clonedFromTransactionId
            )
        }
    }
}
