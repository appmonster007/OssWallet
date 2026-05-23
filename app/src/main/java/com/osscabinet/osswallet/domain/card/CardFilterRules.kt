package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
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
    val allowFutureTransactionsToggle: Boolean = true,

    /**
     * Whether warranty protection status and dates can be filtered.
     */
    val allowWarrantySelection: Boolean = true,

    /**
     * Whether linked records (e.g. attachments, linked payments) can be filtered.
     */
    val allowLinkedRecords: Boolean = true
) {
    /**
     * Creates a new rule set by layering this set with external [constraints].
     * The "Most restrictive rule wins" principle is applied.
     */
    fun layeredWith(constraints: CardFilterRules): CardFilterRules {
        return this.copy(
            // Permissions: logical AND (both must allow for it to be enabled)
            allowAccountSelection = this.allowAccountSelection && constraints.allowAccountSelection,
            allowCategorySelection = this.allowCategorySelection && constraints.allowCategorySelection,
            allowStatusSelection = this.allowStatusSelection && constraints.allowStatusSelection,
            allowSearch = this.allowSearch && constraints.allowSearch,
            allowAmountRange = this.allowAmountRange && constraints.allowAmountRange,
            allowCurrencySelection = this.allowCurrencySelection && constraints.allowCurrencySelection,
            allowPaymentMethodSelection = this.allowPaymentMethodSelection && constraints.allowPaymentMethodSelection,
            allowTimelineSelection = this.allowTimelineSelection && constraints.allowTimelineSelection,
            allowTimelineCustomRange = this.allowTimelineCustomRange && constraints.allowTimelineCustomRange,
            allowTagSelection = this.allowTagSelection && constraints.allowTagSelection,
            allowTagCreation = this.allowTagCreation && constraints.allowTagCreation,
            allowFutureTransactionsToggle = this.allowFutureTransactionsToggle && constraints.allowFutureTransactionsToggle,
            allowWarrantySelection = this.allowWarrantySelection && constraints.allowWarrantySelection,
            allowLinkedRecords = this.allowLinkedRecords && constraints.allowLinkedRecords,

            // Forced values: External constraints take precedence if set
            forcedTransactionType = constraints.forcedTransactionType ?: this.forcedTransactionType,
            forcedTransactionTypes = constraints.forcedTransactionTypes ?: this.forcedTransactionTypes,

            // Allowed types: Intersection of both sets (most restrictive)
            allowedTransactionTypes = when {
                this.allowedTransactionTypes != null && constraints.allowedTransactionTypes != null ->
                    this.allowedTransactionTypes.intersect(constraints.allowedTransactionTypes.toSet()).toList()
                else -> constraints.allowedTransactionTypes ?: this.allowedTransactionTypes
            }
        )
    }

    /**
     * Normalizes a candidate filter against these rules.
     * - If [forcedTransactionTypes] is set, replaces candidate's transactionTypes unconditionally.
     * - Else if [allowedTransactionTypes] is set, intersects with allowed set (or falls back to allowed set on conflict).
     */
    fun normalize(candidate: TransactionFilter): TransactionFilter {
        if (forcedTransactionTypes != null) {
            val forced = TransactionType.entries.filter { it.value in forcedTransactionTypes }.toSet()
            return candidate.copy(transactionTypes = forced)
        }
        if (allowedTransactionTypes != null) {
            val allowedSet = TransactionType.entries.filter { it.value in allowedTransactionTypes }.toSet()
            if (candidate.transactionTypes.isNotEmpty()) {
                val intersection = candidate.transactionTypes.intersect(allowedSet)
                return candidate.copy(
                    transactionTypes = if (intersection.isNotEmpty()) intersection else allowedSet
                )
            }
        }
        return candidate
    }

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

        /**
         * Rules for spending-by-category cards where the category selection is controlled
         * by the in-card segmented button, not the filter sheet.
         */
        val SPENDING_LOCKED_CATEGORIES = CardFilterRules(
            allowedTransactionTypes = listOf("income", "expense"),
            allowCategorySelection = false
        )

        /**
         * Constraint profile for Account-scoped screens.
         * Disables account switching but keeps everything else per-card.
         */
        val ACCOUNT_LOCKED = CardFilterRules(
            allowAccountSelection = false
        )
    }
}
