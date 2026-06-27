package com.osscabinet.ossWallet.ui.screens.transactions.model

import androidx.compose.runtime.Immutable
import com.osscabinet.ossWallet.domain.model.RecordUiModel
import com.osscabinet.ossWallet.domain.model.timeline.GroupHeaderData
import com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel

/**
 * UI-specific representation of a transaction record.
 * Contains pre-formatted strings to ensure 60fps scrolling.
 */
@Immutable
data class TransactionListItem(
    val record: RecordUiModel,
    val formattedAmount: String,
    val formattedConvertedAmount: String?,
    val formattedDate: String,
    val formattedBalanceBefore: String,
    val formattedBalanceAfter: String
)

/**
 * UI-specific representation of a transaction group.
 */
@Immutable
data class TransactionGroupUi(
    val key: String,
    val headerData: GroupHeaderData,
    val items: List<TransactionListItem>
)

/**
 * State for the transaction list.
 */
@Immutable
data class TransactionListUiState(
    val groups: List<TransactionGroupUi> = emptyList(),
    val isEmpty: Boolean = true
) {
    /**
     * Returns a set of all confirmed transaction IDs currently in the UI state.
     */
    fun getAllTransactionIds(): Set<Long> {
        return groups.flatMap { group ->
            group.items.mapNotNull { item ->
                (item.record as? ConfirmedTransactionUiModel)?.item?.transaction?.id
            }
        }.toSet()
    }
}
