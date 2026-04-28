package com.osscabinet.ossWallet.ui.analytics.components

import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext

/**
 * Grouped dependencies and actions for analytics cards to reduce argument duplication.
 */
data class CardActions<T>(
    val onUpdate: (T) -> Unit = {},
    val onDelete: () -> Unit = {},
    val onRecordsClick: (RecordsFilterContext) -> Unit = {},
    val accounts: List<Account> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val savedFilters: List<Filter> = emptyList()
)
