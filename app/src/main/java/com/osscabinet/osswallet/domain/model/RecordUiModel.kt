package com.osscabinet.ossWallet.domain.model

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * Unified interface for items that can appear in the transaction list.
 * This is a domain-level representation of a timeline record.
 */
@Immutable
sealed interface RecordUiModel {
    val id: String
    val timestamp: Long
    val amount: BigDecimal
    val currency: String
    val categoryName: String
    val categoryIcon: String?
    val categoryColor: Int
    val accountName: String
    val accountCurrency: String
    val note: String?
    val tags: List<TagUiModel>
    val amountInBase: BigDecimal?
}

@Immutable
data class ConfirmedTransactionUiModel(
    val item: TransactionWithBalance,
    override val accountName: String,
    override val accountCurrency: String,
    override val categoryName: String,
    override val categoryIcon: String?,
    override val categoryColor: Int,
    override val tags: List<TagUiModel> = emptyList(),
    override val amountInBase: BigDecimal? = null,
    val linkedTransactionIds: List<Long> = emptyList()
) : RecordUiModel {
    override val id: String get() = "tx_${item.transaction.id}"
    override val timestamp: Long get() = item.transaction.timestamp
    override val amount: BigDecimal get() = item.transaction.amount
    override val currency: String get() = item.transaction.currency
    override val note: String? get() = item.transaction.note
    val status: TransactionStatus get() = item.transaction.status
}

@Immutable
data class PlannedPaymentUiModel(
    val paymentId: Long,
    val occurrenceId: Long?,
    override val timestamp: Long,
    override val amount: BigDecimal,
    override val currency: String,
    override val categoryName: String,
    override val categoryIcon: String?,
    override val categoryColor: Int,
    override val accountName: String,
    override val accountCurrency: String,
    override val note: String?,
    override val tags: List<TagUiModel> = emptyList(),
    override val amountInBase: BigDecimal? = null,
    val isOverdue: Boolean
) : RecordUiModel {
    override val id: String get() = "planned_${paymentId}_$timestamp"
}
