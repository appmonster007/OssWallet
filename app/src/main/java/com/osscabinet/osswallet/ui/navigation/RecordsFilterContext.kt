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
        val isIncome: Boolean // Kept for backward compatibility, will be converted to transactionType
    ) : RecordsFilterContext()

    /**
     * Filter by account and currency (Note: TransactionFilter doesn't support currency filtering directly)
     * This context is reserved for future enhancement or can be handled via custom filtering
     */
    @Serializable
    data class ByAccountAndCurrency(
        val accountId: String,
        val currency: String
    ) : RecordsFilterContext()

    /**
     * Custom filter with arbitrary TransactionFilter criteria
     * Note: categoryIds are stored as Strings for serialization but converted to Long when applied
     * Note: minAmount/maxAmount are stored as Double for serialization but converted to BigDecimal when applied
     */
    @Serializable
    data class Custom(
        val accountIds: List<String> = emptyList(),
        val transactionType: String? = null,
        val startDate: Long? = null,
        val endDate: Long? = null,
        val categoryIds: List<String> = emptyList(),
        val currencies: List<String> = emptyList(),
        val paymentMethods: List<String> = emptyList(),
        val queryNote: String? = null,
        val minAmount: Double? = null,
        val maxAmount: Double? = null,
        val queryPayee: String? = null,
        val tags: List<String> = emptyList()
    ) : RecordsFilterContext()
}
