package com.osscabinet.ossWallet.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionGroupingType {
    BY_DAY, BY_WEEK
}

@Serializable
data class TransactionSectionSettings(
    val showPlannedPayments: Boolean = false,
    val useNoteAsHeader: Boolean = false,
    val showHeaderOpeningBalance: Boolean = false,
    val showHeaderClosingBalance: Boolean = true,
    val showItemOpeningBalance: Boolean = false,
    val showItemClosingBalance: Boolean = true,
    val groupBy: TransactionGroupingType = TransactionGroupingType.BY_WEEK,
    val showOnlyActiveCurrencies: Boolean = false,
    val excludeFutureTransactions: Boolean = true,
    val showCurrencyBreakdown: Boolean = true,
    val showAccount: Boolean = true,
    val showCategory: Boolean = true,
    val showNote: Boolean = true,
    val showTags: Boolean = true,
    val showTimestamp: Boolean = true,
    val showIcon: Boolean = true
)
