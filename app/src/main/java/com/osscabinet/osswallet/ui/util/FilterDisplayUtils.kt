package com.osscabinet.ossWallet.ui.util

import com.osscabinet.ossWallet.domain.model.filter.FilterOperator
import com.osscabinet.ossWallet.domain.model.filter.FilterStack
import com.osscabinet.ossWallet.domain.model.filter.LogicalOperator
import com.osscabinet.ossWallet.domain.model.filter.UnifiedFilter
import java.math.BigDecimal

/**
 * Utility functions for displaying filters in the UI.
 * Converts filter objects to human-readable strings.
 */
object FilterDisplayUtils {

    /**
     * Converts a FilterStack to a list of display strings.
     * Useful for showing active filters to the user.
     */
    fun getFilterStackDisplayNames(stack: FilterStack): List<String> {
        return stack.activeFilters.map { getFilterDisplayName(it) }
    }

    /**
     * Converts a single UnifiedFilter to a display string.
     */
    fun getFilterDisplayName(filter: UnifiedFilter): String {
        return when (filter) {
            is UnifiedFilter.TextFilter -> {
                if (filter.isRegex) {
                    "Text (regex): ${filter.query}"
                } else {
                    "Text: ${filter.query}"
                }
            }
            is UnifiedFilter.CategoryFilter -> {
                "${filter.categoryIds.size} categories"
            }
            is UnifiedFilter.AccountFilter -> {
                "${filter.accountIds.size} accounts"
            }
            is UnifiedFilter.DateRangeFilter -> {
                "Date range"
            }
            is UnifiedFilter.AmountFilter -> {
                val parts = mutableListOf<String>()
                if (filter.min != null) parts.add("≥ ${filter.min}")
                if (filter.max != null) parts.add("≤ ${filter.max}")
                if (parts.isEmpty()) "Any amount" else parts.joinToString(" ")
            }
            is UnifiedFilter.PaymentMethodFilter -> {
                "${filter.methods.size} payment methods"
            }
            is UnifiedFilter.CurrencyFilter -> {
                "${filter.currencies.size} currencies"
            }
            is UnifiedFilter.IncomeTypeFilter -> {
                when (filter.isIncome) {
                    true -> "Income only"
                    false -> "Expense only"
                    null -> "All types"
                }
            }
            is UnifiedFilter.WarrantyFilter -> {
                when (filter.hasWarranty) {
                    true -> "Has warranty"
                    false -> "No warranty"
                    null -> "Any warranty status"
                }
            }
            is UnifiedFilter.CompositeFilter -> {
                "${filter.filters.size} filters (${filter.operator.name})"
            }
            is UnifiedFilter.SavedFilterReference -> {
                "Saved filter #${filter.filterId}"
            }
            is UnifiedFilter.InstantFilter -> {
                "⚡ ${getFilterDisplayName(filter.filter)}"
            }
        }
    }

    /**
     * Gets a detailed description of a filter for tooltips or detailed views.
     */
    fun getFilterDescription(filter: UnifiedFilter): String {
        return when (filter) {
            is UnifiedFilter.TextFilter -> {
                val modifiers = mutableListOf<String>()
                if (filter.isRegex) modifiers.add("regex")
                if (filter.isCaseInsensitive) modifiers.add("case-insensitive")
                val modStr = if (modifiers.isNotEmpty()) " (${modifiers.joinToString(", ")})" else ""
                "Search for \"${
                    filter.query.take(
                        50
                    )
                }${if (filter.query.length > 50) "..." else ""}\" in note and payee$modStr"
            }
            is UnifiedFilter.CategoryFilter -> {
                "Filter by ${filter.categoryIds.size} categor${if (filter.categoryIds.size == 1) "y" else "ies"}"
            }
            is UnifiedFilter.AccountFilter -> {
                "Filter by ${filter.accountIds.size} account${if (filter.accountIds.size == 1) "" else "s"}"
            }
            is UnifiedFilter.DateRangeFilter -> {
                "Filter by date range"
            }
            is UnifiedFilter.AmountFilter -> {
                val range = when {
                    filter.min != null && filter.max != null -> "${filter.min} to ${filter.max}"
                    filter.min != null -> "${filter.min} or more"
                    filter.max != null -> "${filter.max} or less"
                    else -> "Any amount"
                }
                "Filter by amount: $range"
            }
            is UnifiedFilter.PaymentMethodFilter -> {
                "Filter by ${filter.methods.size} payment method${if (filter.methods.size == 1) "" else "s"}"
            }
            is UnifiedFilter.CurrencyFilter -> {
                "Filter by ${filter.currencies.size} currenc${if (filter.currencies.size == 1) "y" else "ies"}"
            }
            is UnifiedFilter.IncomeTypeFilter -> {
                when (filter.isIncome) {
                    true -> "Show only income transactions"
                    false -> "Show only expense transactions"
                    null -> "Show all transaction types"
                }
            }
            is UnifiedFilter.WarrantyFilter -> {
                when (filter.hasWarranty) {
                    true -> "Show only transactions with warranty"
                    false -> "Show only transactions without warranty"
                    null -> "Show all transactions regardless of warranty"
                }
            }
            is UnifiedFilter.CompositeFilter -> {
                "Combines ${filter.filters.size} filters with ${filter.operator.name} logic"
            }
            is UnifiedFilter.SavedFilterReference -> {
                "Load saved filter #${filter.filterId}"
            }
            is UnifiedFilter.InstantFilter -> {
                "Instant (temporary) filter: ${getFilterDescription(filter.filter)}"
            }
        }
    }

    /**
     * Checks if a filter stack has any active filters.
     */
    fun hasActiveFilters(stack: FilterStack): Boolean {
        return !stack.isEmpty()
    }

    /**
     * Gets the count of active filters in a stack.
     */
    fun getFilterCount(stack: FilterStack): Int {
        return stack.activeFilters.size
    }

    /**
     * Gets the count of instant/transient filters in a stack.
     */
    fun getInstantFilterCount(stack: FilterStack): Int {
        return stack.getInstantFilters().size
    }

    /**
     * Gets the count of permanent filters in a stack.
     */
    fun getPermanentFilterCount(stack: FilterStack): Int {
        return stack.getPermanentFilters().size
    }
}
