package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Hardcoded rules for what a specific card type allows in its filter.
 * These rules are used to restrict the UI in the filter settings sheet.
 */
data class CardFilterRules(
    /**
     * List of allowed transaction types. 
     * If not null, the UI should only show these options.
     * If empty, the transaction type selection should be hidden.
     */
    val allowedTransactionTypes: List<String>? = null,
    
    /**
     * If set, the card MUST use this transaction type.
     * The UI should disable the selection and show it as fixed.
     */
    val forcedTransactionType: String? = null,

    /**
     * If set, the card MUST use exactly these transaction types and the user cannot change them.
     * The UI should disable the type selector and display the forced types as locked.
     */
    val forcedTransactionTypes: List<String>? = null,
    
    /**
     * Whether account selection is allowed.
     */
    val allowAccountSelection: Boolean = true,
    
    /**
     * Whether category selection is allowed.
     */
    val allowCategorySelection: Boolean = true,
    
    /**
     * Whether status selection is allowed.
     */
    val allowStatusSelection: Boolean = true,
    
    /**
     * Whether search (note/payee) is allowed.
     */
    val allowSearch: Boolean = true,
    
    /**
     * Whether amount range selection is allowed.
     */
    val allowAmountRange: Boolean = true,
    
    /**
     * Whether currency selection is allowed.
     */
    val allowCurrencySelection: Boolean = true,
    
    /**
     * Whether payment method selection is allowed.
     */
    val allowPaymentMethodSelection: Boolean = true,
    
    /**
     * Whether timeline selection is allowed.
     */
    val allowTimelineSelection: Boolean = true,
    
    /**
     * Whether custom range in timeline is allowed.
     */
    val allowTimelineCustomRange: Boolean = true,

    /**
     * Whether tag selection is allowed.
     */
    val allowTagSelection: Boolean = true,

    /**
     * Whether tag creation is allowed from the selection dialog.
     */
    val allowTagCreation: Boolean = true,

    /**
     * Whether the "Include future transactions" toggle is shown.
     * Set to false for cards whose computer enforces excludeFuture=true regardless of config.
     */
    val allowFutureTransactionsToggle: Boolean = true
) {
    companion object {
        /**
         * Default rules that allow everything.
         */
        val DEFAULT = CardFilterRules()

        /**
         * Rules for cards that can be either Income or Expense but NOT both/all (e.g., Category Breakdown).
         */
        val INCOME_OR_EXPENSE_ONLY = CardFilterRules(
            allowedTransactionTypes = listOf("income", "expense")
        )

        /**
         * Rules for cards that always show both Income and Expense and must not allow type changes
         * (e.g., Income & Expense Bar, Cash Flow Comparison).
         */
        val INCOME_AND_EXPENSE = CardFilterRules(
            forcedTransactionTypes = listOf("income", "expense")
        )
    }
}
