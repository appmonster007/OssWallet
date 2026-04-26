package com.osscabinet.ossWallet.ui.analytics.components

import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
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
    val allCategories: List<CategoryEntity> = emptyList(),
    val savedFilters: List<FilterEntity> = emptyList()
)
