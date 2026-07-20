package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.timeline.GroupHeaderData
import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

interface Timestamped {
    val timestamp: Long
}

@Serializable
data class Transaction(
    val id: Long = 0,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
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
    val gpsLocation: String? = null,
    val location: String? = null,
    val warrantyExpiration: Long? = null,
    val status: TransactionStatus = TransactionStatus.CLEARED,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val transferRate: BigDecimal = BigDecimal.ONE
) : Timestamped

@Serializable
data class TransactionWithBalance(
    val transaction: Transaction,
    @Serializable(with = BigDecimalSerializer::class)
    val balanceBefore: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val balanceAfter: BigDecimal
)

/**
 * UI representation of a tag with its associated color.
 */
@Serializable
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
 * Pre-computed group of transactions with its period-specific header data.
 */
@Serializable
data class TransactionGroup(
    val key: String,
    val headerData: GroupHeaderData,
    val transactions: List<RecordUiModel>
)

/**
 * Unified state for transaction lists across the app.
 */
@Serializable
data class TransactionListState(
    val groups: List<TransactionGroup> = emptyList(),
    val isEmpty: Boolean = true
)
