package com.osscabinet.ossWallet.ui.screens.transactions.model

import androidx.compose.runtime.Immutable
import com.osscabinet.ossWallet.domain.model.RecordUiModel
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService

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
    val headerData: TimelineService.GroupHeaderData,
    val items: List<TransactionListItem>
)

/**
 * State for the transaction list.
 */
@Immutable
data class TransactionListUiState(
    val groups: List<TransactionGroupUi> = emptyList(),
    val isEmpty: Boolean = true
)
