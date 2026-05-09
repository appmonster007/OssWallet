package com.osscabinet.ossWallet.widget

import kotlinx.serialization.Serializable

@Serializable
data class RecentTransactionsWidgetConfig(
    val limit: Int = 10,
    val showCategoryIcon: Boolean = true
)
