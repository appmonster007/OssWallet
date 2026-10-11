package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.util.NullableBigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * The text, amount, warranty and tag-matching criteria of a saved filter.
 *
 * Loans and budgets are stored as a row plus a linked filter, and read back into a flat domain
 * model. The structural criteria (categories, accounts, tags, ...) have their own fields on those
 * models; this carries the rest so that the filter built for calculations matches the one the user
 * saved. Without it a payee, note or amount criterion is accepted by the form but never applied.
 */
@Serializable
data class AdvancedFilterCriteria(
    val queryNote: String? = null,
    val isNoteRegex: Boolean = false,
    val queryPayee: String? = null,
    val isPayeeRegex: Boolean = false,
    @Serializable(with = NullableBigDecimalSerializer::class)
    val minAmount: BigDecimal? = null,
    @Serializable(with = NullableBigDecimalSerializer::class)
    val maxAmount: BigDecimal? = null,
    val hasWarranty: Boolean? = null,
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    val matchAllTags: Boolean = true
) {
    fun applyTo(filter: TransactionFilter): TransactionFilter = filter.copy(
        queryNote = queryNote,
        isNoteRegex = isNoteRegex,
        queryPayee = queryPayee,
        isPayeeRegex = isPayeeRegex,
        minAmount = minAmount,
        maxAmount = maxAmount,
        hasWarranty = hasWarranty,
        warrantyExpirationStart = warrantyExpirationStart,
        warrantyExpirationEnd = warrantyExpirationEnd,
        matchAllTags = matchAllTags
    )

    companion object {
        fun from(filter: TransactionFilter) = AdvancedFilterCriteria(
            queryNote = filter.queryNote,
            isNoteRegex = filter.isNoteRegex,
            queryPayee = filter.queryPayee,
            isPayeeRegex = filter.isPayeeRegex,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            hasWarranty = filter.hasWarranty,
            warrantyExpirationStart = filter.warrantyExpirationStart,
            warrantyExpirationEnd = filter.warrantyExpirationEnd,
            matchAllTags = filter.matchAllTags
        )
    }
}
