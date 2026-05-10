package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal

/**
 * Convenience extension functions for creating common TransactionFilter patterns.
 * These functions make it easier to create filters for common use cases.
 */

/**
 * Merges two TransactionFilters. The `other` filter's values take precedence where non-default.
 * For list fields, intersects both lists to honor the narrowest scope.
 */
fun TransactionFilter.merge(other: TransactionFilter): TransactionFilter {
    return this.copy(
        timelineRange = if (other.timelineRange != TimelineRange.AllTime) other.timelineRange else this.timelineRange,
        accountIds = mergeLists(this.accountIds, other.accountIds),
        categoryIds = mergeLists(this.categoryIds, other.categoryIds),
        currencies = mergeLists(this.currencies, other.currencies),
        tags = mergeLists(this.tags, other.tags),
        paymentMethods = mergeLists(this.paymentMethods, other.paymentMethods),
        statuses = mergeLists(this.statuses, other.statuses),
        transactionType = other.transactionType ?: this.transactionType,
        queryNote = other.queryNote ?: this.queryNote,
        isNoteRegex = if (other.queryNote != null) other.isNoteRegex else this.isNoteRegex,
        queryPayee = other.queryPayee ?: this.queryPayee,
        isPayeeRegex = if (other.queryPayee != null) other.isPayeeRegex else this.isPayeeRegex,
        minAmount = other.minAmount ?: this.minAmount,
        maxAmount = other.maxAmount ?: this.maxAmount,
        hasWarranty = other.hasWarranty ?: this.hasWarranty,
        warrantyExpirationStart = other.warrantyExpirationStart ?: this.warrantyExpirationStart,
        warrantyExpirationEnd = other.warrantyExpirationEnd ?: this.warrantyExpirationEnd,
        excludeFuture = this.excludeFuture && other.excludeFuture
    )
}

private fun <T> mergeLists(base: List<T>, other: List<T>): List<T> {
    return if (other.isNotEmpty()) {
        if (base.isEmpty()) other else base.intersect(other.toSet()).toList()
    } else base
}

/**
 * Creates a filter for a specific account.
 */
fun TransactionFilter.forAccount(accountId: String): TransactionFilter =
    copy(accountIds = listOf(accountId))

/**
 * Creates a filter for specific accounts.
 */
fun TransactionFilter.forAccounts(accountIds: List<String>): TransactionFilter =
    copy(accountIds = accountIds)

/**
 * Creates a filter for a specific category.
 */
fun TransactionFilter.forCategory(categoryId: Long): TransactionFilter =
    copy(categoryIds = listOf(categoryId))

/**
 * Creates a filter for specific categories.
 */
fun TransactionFilter.forCategories(categoryIds: List<Long>): TransactionFilter =
    copy(categoryIds = categoryIds)

/**
 * Creates a filter for income transactions only.
 */
fun TransactionFilter.forIncome(): TransactionFilter =
    copy(transactionType = "income")

/**
 * Creates a filter for expense transactions only.
 */
fun TransactionFilter.forExpense(): TransactionFilter =
    copy(transactionType = "expense")

/**
 * Creates a filter with a specific timeline range.
 * Uses TimelineService.getDateRange() to convert TimelineRange to start/end timestamps.
 */
fun TransactionFilter.withTimeline(timelineRange: TimelineRange): TransactionFilter {
    return copy(timelineRange = timelineRange)
}

/**
 * Creates a filter for a specific currency.
 */
fun TransactionFilter.withCurrency(currency: String): TransactionFilter =
    copy(currencies = listOf(currency))

/**
 * Creates a filter for specific currencies.
 */
fun TransactionFilter.withCurrencies(currencies: List<String>): TransactionFilter =
    copy(currencies = currencies)

/**
 * Creates a filter with a specific date range.
 */
fun TransactionFilter.withDateRange(start: Long?, end: Long?): TransactionFilter =
    copy(timelineRange = TimelineRange.CustomRange(start, end))

/**
 * Creates a filter with a specific amount range.
 */
fun TransactionFilter.withAmountRange(min: BigDecimal?, max: BigDecimal?): TransactionFilter =
    copy(minAmount = min, maxAmount = max)

/**
 * Creates a filter with a specific payment method.
 */
fun TransactionFilter.withPaymentMethod(method: String): TransactionFilter =
    copy(paymentMethods = listOf(method))

/**
 * Creates a filter with specific payment methods.
 */
fun TransactionFilter.withPaymentMethods(methods: List<String>): TransactionFilter =
    copy(paymentMethods = methods)

/**
 * Creates a filter with a note query.
 */
fun TransactionFilter.withNoteQuery(query: String, isRegex: Boolean = false): TransactionFilter =
    copy(queryNote = query, isNoteRegex = isRegex)

/**
 * Creates a filter with a payee query.
 */
fun TransactionFilter.withPayeeQuery(query: String, isRegex: Boolean = false): TransactionFilter =
    copy(queryPayee = query, isPayeeRegex = isRegex)

/**
 * Creates a filter for transactions with warranty.
 */
fun TransactionFilter.withWarranty(): TransactionFilter =
    copy(hasWarranty = true)

/**
 * Creates a filter for transactions without warranty.
 */
fun TransactionFilter.withoutWarranty(): TransactionFilter =
    copy(hasWarranty = false)

/**
 * Creates a filter with a warranty expiration range.
 */
fun TransactionFilter.withWarrantyExpirationRange(start: Long?, end: Long?): TransactionFilter =
    copy(warrantyExpirationStart = start, warrantyExpirationEnd = end)

/**
 * Creates a filter for specific transaction statuses.
 */
fun TransactionFilter.withStatuses(statuses: List<TransactionStatus>): TransactionFilter =
    copy(statuses = statuses)
