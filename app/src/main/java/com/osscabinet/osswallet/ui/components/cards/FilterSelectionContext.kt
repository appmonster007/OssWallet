package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter

/**
 * Bundles the three filter-sheet selection lists that flow unchanged through the card system:
 * accounts for the account picker, categories for the category picker, and saved filter presets.
 */
data class FilterSelectionContext(
    val accounts: List<Account> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val savedFilters: List<Filter> = emptyList()
)
