package com.osscabinet.ossWallet.ui.mapper

import com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel
import com.osscabinet.ossWallet.domain.model.PlannedPaymentUiModel
import com.osscabinet.ossWallet.domain.model.RecordUiModel
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.ui.components.nativeWithConvertedStrings
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionGroupUi
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListItem
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState
import com.osscabinet.ossWallet.ui.util.TimeUtils
import com.osscabinet.ossWallet.ui.util.formatCurrency

/**
 * Mapper that transforms raw Domain Transaction models into UI-Ready Models.
 * Contains all formatting logic to keep Domain services pure.
 */
object TransactionUiMapper {

    fun mapRecord(
        record: RecordUiModel,
        baseCurrency: String
    ): TransactionListItem {
        val (amountText, convertedText) = nativeWithConvertedStrings(
            amount = record.amount,
            nativeCurrency = record.currency,
            targetCurrency = baseCurrency,
            convertedAmount = record.amountInBase
        )

        return TransactionListItem(
            record = record,
            formattedAmount = amountText,
            formattedConvertedAmount = convertedText,
            formattedDate = when (record) {
                is ConfirmedTransactionUiModel -> TimeUtils.formatDateTime(record.timestamp)
                is PlannedPaymentUiModel -> TimeUtils.formatDate(record.timestamp)
            },
            formattedBalanceBefore = if (record is ConfirmedTransactionUiModel) {
                formatCurrency(record.item.balanceBefore, record.accountCurrency)
            } else "",
            formattedBalanceAfter = if (record is ConfirmedTransactionUiModel) {
                formatCurrency(record.item.balanceAfter, record.accountCurrency)
            } else ""
        )
    }

    fun mapToUiState(
        domainState: TransactionListState,
        baseCurrency: String
    ): TransactionListUiState {
        if (domainState.isEmpty) return TransactionListUiState()

        val groups = domainState.groups.map { group ->
            TransactionGroupUi(
                key = group.key,
                headerData = group.headerData,
                items = group.transactions.map { mapRecord(it, baseCurrency) }
            )
        }

        return TransactionListUiState(groups = groups, isEmpty = false)
    }
}
