package com.osscabinet.ossWallet.domain.model.filter

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import java.math.BigDecimal

/**
 * Generic builder for creating FilterStack instances.
 * Reusable across all screens and navigation patterns.
 *
 * Usage:
 * ```kotlin
 * val filterStack = FilterStackBuilder()
 *     .accountIds(listOf("acc1", "acc2"))
 *     .categoryIds(listOf(1L, 2L))
 *     .isIncome(true)
 *     .build()
 * ```
 */
class FilterStackBuilder {
    private val filters = mutableListOf<UnifiedFilter>()
    private var operator: LogicalOperator = LogicalOperator.AND

    fun accountIds(ids: List<String>) = apply {
        filters.add(UnifiedFilter.AccountFilter(ids))
    }

    fun categoryIds(ids: List<Long>) = apply {
        filters.add(UnifiedFilter.CategoryFilter(ids))
    }

    fun isIncome(isIncome: Boolean) = apply {
        filters.add(UnifiedFilter.IncomeTypeFilter(isIncome = isIncome))
    }

    fun currencies(currencies: List<String>) = apply {
        filters.add(UnifiedFilter.CurrencyFilter(currencies))
    }

    fun dateRange(timelineRange: TimelineRange) = apply {
        filters.add(UnifiedFilter.DateRangeFilter(timelineRange))
    }

    fun amountRange(min: BigDecimal?, max: BigDecimal?) = apply {
        filters.add(UnifiedFilter.AmountFilter(min = min, max = max))
    }

    fun search(query: String) = apply {
        filters.add(UnifiedFilter.TextFilter(query))
    }

    fun paymentMethods(methods: List<String>) = apply {
        filters.add(UnifiedFilter.PaymentMethodFilter(methods))
    }

    fun withOperator(op: LogicalOperator) = apply {
        this.operator = op
    }

    fun build(): FilterStack = FilterStack(filters.toList(), operator)
}

// Extension functions for common navigation patterns
// These provide a fluent API for creating FilterStack instances for common use cases

/**
 * Creates a FilterStack for filtering by a specific currency.
 */
fun FilterStack.Companion.forCurrency(currency: String): FilterStack =
    FilterStackBuilder().currencies(listOf(currency)).build()

/**
 * Creates a FilterStack for filtering by income transactions only.
 */
fun FilterStack.Companion.forIncome(): FilterStack =
    FilterStackBuilder().isIncome(true).build()

/**
 * Creates a FilterStack for filtering by expense transactions only.
 */
fun FilterStack.Companion.forExpense(): FilterStack =
    FilterStackBuilder().isIncome(false).build()

/**
 * Creates a FilterStack for filtering by a specific account.
 */
fun FilterStack.Companion.forAccount(accountId: String): FilterStack =
    FilterStackBuilder().accountIds(listOf(accountId)).build()

/**
 * Creates a FilterStack for filtering by account and income/expense type.
 */
fun FilterStack.Companion.forAccountAndType(accountId: String, isIncome: Boolean): FilterStack =
    FilterStackBuilder()
        .accountIds(listOf(accountId))
        .isIncome(isIncome)
        .build()

/**
 * Creates a FilterStack for filtering by account and currency.
 */
fun FilterStack.Companion.forAccountAndCurrency(accountId: String, currency: String): FilterStack =
    FilterStackBuilder()
        .accountIds(listOf(accountId))
        .currencies(listOf(currency))
        .build()

/**
 * Creates a FilterStack for filtering by a specific category.
 */
fun FilterStack.Companion.forCategory(categoryId: Long): FilterStack =
    FilterStackBuilder().categoryIds(listOf(categoryId)).build()

/**
 * Creates a FilterStack for filtering by multiple categories.
 */
fun FilterStack.Companion.forCategories(categoryIds: List<Long>): FilterStack =
    FilterStackBuilder().categoryIds(categoryIds).build()

/**
 * Creates a FilterStack for filtering by search query.
 */
fun FilterStack.Companion.forSearch(query: String): FilterStack =
    FilterStackBuilder().search(query).build()

/**
 * Creates a FilterStack for filtering by date range.
 */
fun FilterStack.Companion.forDateRange(timelineRange: TimelineRange): FilterStack =
    FilterStackBuilder().dateRange(timelineRange).build()

/**
 * Creates a FilterStack for filtering by amount range.
 */
fun FilterStack.Companion.forAmountRange(min: BigDecimal?, max: BigDecimal?): FilterStack =
    FilterStackBuilder().amountRange(min, max).build()
