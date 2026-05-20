package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Bundles the three filter-sheet selection lists that flow unchanged through the card system:
 * accounts for the account picker, categories for the category picker, and saved filter presets.
 */
data class FilterSelectionContext(
    val accounts: List<Account> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val savedFilters: List<Filter> = emptyList(),
    val onSearchTransactions: (String) -> Flow<List<Transaction>> = { flowOf(emptyList()) }
)
