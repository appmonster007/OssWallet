package com.osscabinet.ossWallet.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionGroupingType {
    BY_DAY, BY_WEEK
}

@Serializable
enum class FastScrollSide {
    LEFT, RIGHT
}

@Serializable
data class TransactionSectionSettings(
    val stickyHeaders: Boolean = false,
    val showPlannedPayments: Boolean = false,
    val useNoteAsHeader: Boolean = false,
    val showHeaderOpeningBalance: Boolean = false,
    val showHeaderClosingBalance: Boolean = true,
    val showItemOpeningBalance: Boolean = false,
    val showItemClosingBalance: Boolean = true,
    val groupBy: TransactionGroupingType = TransactionGroupingType.BY_WEEK,
    val showOnlyActiveCurrencies: Boolean = false,
    val showCurrencyBreakdown: Boolean = true,
    val allTxnBreakdown: Boolean = true,
    val showAccount: Boolean = true,
    val showCategory: Boolean = true,
    val showNote: Boolean = true,
    val showTags: Boolean = true,
    val showTimestamp: Boolean = true,
    val showIcon: Boolean = true,
    val fastScrollSide: FastScrollSide = FastScrollSide.RIGHT
)
