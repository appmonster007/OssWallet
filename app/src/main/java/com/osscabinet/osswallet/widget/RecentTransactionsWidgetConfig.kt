package com.osscabinet.ossWallet.widget

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionDisplayPreference {
    CATEGORY_FIRST,
    NOTE_FIRST
}

@Serializable
data class RecentTransactionsWidgetConfig(
    val limit: Int = 10,
    val showCategoryIcon: Boolean = true,
    val primaryDisplay: TransactionDisplayPreference = TransactionDisplayPreference.CATEGORY_FIRST
)
