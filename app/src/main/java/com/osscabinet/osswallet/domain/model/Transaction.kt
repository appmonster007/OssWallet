package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

interface Timestamped {
    val timestamp: Long
}

data class Transaction(
    val id: Long = 0,
    val name: String,
    val amount: BigDecimal,
    override val timestamp: Long,
    val currency: String,
    val transactionType: TransactionType,
    val accountId: String?,
    val categoryId: Long,
    val tags: Set<String> = emptySet(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val payee: String = "",
    val note: String = "",
    val sourcePlannedPaymentId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val warrantyExpiration: Long? = null,
    val status: TransactionStatus = TransactionStatus.CLEARED
) : Timestamped

data class TransactionWithBalance(
    val transaction: Transaction,
    val balanceBefore: BigDecimal,
    val balanceAfter: BigDecimal
)

/**
 * UI representation of a tag with its associated color.
 */
data class TagUiModel(
    val name: String,
    val color: Int
)

/**
 * Maps a set of tag names to their UI models using a provided tag library.
 * Memoizes results for performance during bulk operations.
 */
fun Set<String>.toUiModels(
    allTags: List<Tag>,
    cache: MutableMap<String, TagUiModel>? = null
): List<TagUiModel> {
    if (this.isEmpty()) return emptyList()
    val tagMap = allTags.associateBy { it.name }
    return this.map { name ->
        cache?.getOrPut(name) {
            TagUiModel(name, tagMap[name]?.colorHex ?: -1)
        } ?: TagUiModel(name, tagMap[name]?.colorHex ?: -1)
    }
}

/**
 * Enriched UI model for a transaction, containing all metadata needed for display.
 */
data class TransactionUiModel(
    val item: TransactionWithBalance,
    val accountName: String,
    val accountCurrency: String,
    val categoryName: String,
    val categoryIcon: String?,
    val categoryColor: Int,
    val tags: List<TagUiModel> = emptyList(),
    val amountInBase: BigDecimal? = null
)

/**
 * Pre-computed group of transactions with its period-specific header data.
 */
data class TransactionGroup(
    val key: String,
    val headerData: com.osscabinet.ossWallet.domain.service.timeline.TimelineService.GroupHeaderData,
    val transactions: List<TransactionUiModel>
)

/**
 * Unified state for transaction lists across the app.
 */
data class TransactionListState(
    val groups: List<TransactionGroup> = emptyList(),
    val isEmpty: Boolean = true
)
