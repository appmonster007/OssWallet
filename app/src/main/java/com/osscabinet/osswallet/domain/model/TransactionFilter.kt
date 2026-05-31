package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.util.NullableBigDecimalSerializer
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class TransactionFilter(
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val queryNote: String? = null,
    val isNoteRegex: Boolean = false,
    val queryPayee: String? = null,
    val isPayeeRegex: Boolean = false,
    val categoryIds: Set<Long> = emptySet(),
    val accountIds: Set<String> = emptySet(),
    val paymentMethods: Set<PaymentMethod> = emptySet(),
    val hasWarranty: Boolean? = null, // null = any, true = has warranty, false = no warranty
    val warrantyExpirationStart: Long? = null,
    val warrantyExpirationEnd: Long? = null,
    @Serializable(with = NullableBigDecimalSerializer::class)
    val minAmount: BigDecimal? = null,
    @Serializable(with = NullableBigDecimalSerializer::class)
    val maxAmount: BigDecimal? = null,
    val transactionTypes: Set<TransactionType> = emptySet(), // if empty, matches all
    val currencies: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val statuses: Set<TransactionStatus> = emptySet(),
    val linkedTransactionIds: Set<Long> = emptySet(),
    val excludeFuture: Boolean = false
)

/**
 * Convenience extension functions for creating common TransactionFilter patterns.
 * These functions make it easier to create filters for common use cases.
 */

/**
 * Merges two TransactionFilters. The `other` filter's values take precedence where non-default.
 * For set fields, intersects both sets to honor the narrowest scope.
 */
fun TransactionFilter.merge(other: TransactionFilter): TransactionFilter {
    return this.copy(
        timelineRange = other.timelineRange,
        accountIds = mergeSets(this.accountIds, other.accountIds),
        categoryIds = mergeSets(this.categoryIds, other.categoryIds),
        currencies = mergeSets(this.currencies, other.currencies),
        tags = mergeSets(this.tags, other.tags),
        paymentMethods = mergeSets(this.paymentMethods, other.paymentMethods),
        statuses = mergeSets(this.statuses, other.statuses),
        linkedTransactionIds = mergeSets(this.linkedTransactionIds, other.linkedTransactionIds),
        transactionTypes = mergeSets(this.transactionTypes, other.transactionTypes),
        queryNote = other.queryNote ?: this.queryNote,
        isNoteRegex = if (other.queryNote != null) other.isNoteRegex else this.isNoteRegex,
        queryPayee = other.queryPayee ?: this.queryPayee,
        isPayeeRegex = if (other.queryPayee != null) other.isPayeeRegex else this.isPayeeRegex,
        minAmount = other.minAmount ?: this.minAmount,
        maxAmount = other.maxAmount ?: this.maxAmount,
        hasWarranty = other.hasWarranty ?: this.hasWarranty,
        warrantyExpirationStart = other.warrantyExpirationStart ?: this.warrantyExpirationStart,
        warrantyExpirationEnd = other.warrantyExpirationEnd ?: this.warrantyExpirationEnd,
        excludeFuture = other.excludeFuture
    )
}

private fun <T> mergeSets(base: Set<T>, other: Set<T>): Set<T> {
    return if (other.isNotEmpty()) {
        if (base.isEmpty()) other else base.intersect(other)
    } else base
}

/**
 * Creates a filter for a specific account.
 */
fun TransactionFilter.forAccount(accountId: String): TransactionFilter =
    copy(accountIds = setOf(accountId))

/**
 * Creates a filter for specific accounts.
 */
fun TransactionFilter.forAccounts(accountIds: Collection<String>): TransactionFilter =
    copy(accountIds = accountIds.toSet())

/**
 * Creates a filter for a specific category.
 */
fun TransactionFilter.forCategory(categoryId: Long): TransactionFilter =
    copy(categoryIds = setOf(categoryId))

/**
 * Creates a filter for specific categories.
 */
fun TransactionFilter.forCategories(categoryIds: Collection<Long>): TransactionFilter =
    copy(categoryIds = categoryIds.toSet())

/**
 * Creates a filter for income transactions only.
 */
fun TransactionFilter.forIncome(): TransactionFilter =
    copy(transactionTypes = setOf(TransactionType.INCOME))

/**
 * Creates a filter for expense transactions only.
 */
fun TransactionFilter.forExpense(): TransactionFilter =
    copy(transactionTypes = setOf(TransactionType.EXPENSE))

/**
 * Creates a filter with a specific timeline range.
 */
fun TransactionFilter.withTimeline(timelineRange: TimelineRange): TransactionFilter {
    return copy(timelineRange = timelineRange)
}

/**
 * Creates a filter for a specific currency.
 */
fun TransactionFilter.withCurrency(currency: String): TransactionFilter =
    copy(currencies = setOf(currency))

/**
 * Creates a filter for specific currencies.
 */
fun TransactionFilter.withCurrencies(currencies: Collection<String>): TransactionFilter =
    copy(currencies = currencies.toSet())

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
fun TransactionFilter.withPaymentMethod(method: PaymentMethod): TransactionFilter =
    copy(paymentMethods = setOf(method))

/**
 * Creates a filter with specific payment methods.
 */
fun TransactionFilter.withPaymentMethods(methods: Collection<PaymentMethod>): TransactionFilter =
    copy(paymentMethods = methods.toSet())

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
fun TransactionFilter.withStatuses(statuses: Collection<TransactionStatus>): TransactionFilter =
    copy(statuses = statuses.toSet())
