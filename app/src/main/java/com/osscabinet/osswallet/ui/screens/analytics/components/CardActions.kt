package com.osscabinet.ossWallet.ui.screens.analytics.components

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter

/**
 * Grouped dependencies and actions for analytics cards to reduce argument duplication.
 */
data class CardActions<T>(
    val onUpdate: (T) -> Unit = {},
    val onDelete: () -> Unit = {},
    val onRecordsClick: (TransactionFilter) -> Unit = {},
    val accounts: List<Account> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val savedFilters: List<Filter> = emptyList()
)
