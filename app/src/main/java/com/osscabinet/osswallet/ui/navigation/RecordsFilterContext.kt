package com.osscabinet.ossWallet.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Sealed class representing different filter contexts for TransactionListScreen navigation.
 * Used to pass pre-applied filter criteria when navigating to the records screen.
 */
@Serializable
sealed class RecordsFilterContext {
    /**
     * No pre-applied filter - default state
     */
    @Serializable
    data object None : RecordsFilterContext()

    /**
     * Filter by specific account
     */
    @Serializable
    data class ByAccount(val accountId: String) : RecordsFilterContext()

    /**
     * Filter by account and transaction type (income/expense)
     */
    @Serializable
    data class ByAccountAndType(
        val accountId: String,
        val isIncome: Boolean
    ) : RecordsFilterContext()

    /**
     * Filter by account and currency
     */
    @Serializable
    data class ByAccountAndCurrency(
        val accountId: String,
        val currency: String
    ) : RecordsFilterContext()

    /**
     * Custom filter with arbitrary TransactionFilter criteria
     */
    @Serializable
    data class Custom(
        val accountIds: List<String> = emptyList(),
        val isIncome: Boolean? = null,
        val currencies: List<String> = emptyList(),
        val startDate: Long? = null,
        val endDate: Long? = null,
        val categoryIds: List<String> = emptyList(),
        val paymentMethodIds: List<String> = emptyList(),
        val note: String? = null,
        val minAmount: Double? = null,
        val maxAmount: Double? = null
    ) : RecordsFilterContext()
}
