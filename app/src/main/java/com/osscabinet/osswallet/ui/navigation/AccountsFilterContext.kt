package com.osscabinet.ossWallet.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Sealed class representing different filter contexts for AccountsViewScreen navigation.
 * Used to pass pre-applied filter criteria when navigating to the accounts view.
 */
@Serializable
sealed class AccountsFilterContext {
    /**
     * No pre-applied filter - default state
     */
    @Serializable
    data object None : AccountsFilterContext()

    /**
     * Filter by account type (e.g., Bank, Cash, Credit Card, Investment)
     */
    @Serializable
    data class ByType(val accountType: String) : AccountsFilterContext()

    /**
     * Filter by currency
     */
    @Serializable
    data class ByCurrency(val currency: String) : AccountsFilterContext()

    /**
     * Custom filter with arbitrary account criteria
     */
    @Serializable
    data class Custom(
        val accountTypes: List<String> = emptyList(),
        val currencies: List<String> = emptyList(),
        val minBalance: Double? = null,
        val maxBalance: Double? = null,
        val includeHidden: Boolean = true
    ) : AccountsFilterContext()
}
