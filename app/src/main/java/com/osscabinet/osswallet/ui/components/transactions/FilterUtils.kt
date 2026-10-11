package com.osscabinet.ossWallet.ui.components.transactions

import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Counts active filter criteria in a TransactionFilter relative to a base filter and rules.
 */
/**
 * True when [baseFilter] locks any scope the sheet lists under "Base scope" (accounts, categories,
 * types, ...). The timeline is deliberately ignored: a screen's default period is not a scope.
 */
fun hasBaseScope(baseFilter: TransactionFilter): Boolean =
    countActiveFilters(baseFilter.copy(timelineRange = TransactionFilter().timelineRange)) > 0

/**
 * A card's own filter leaves accounts, categories and types empty to mean "whatever the screen
 * scopes it to" (e.g. the locked account on Account Detail). Fills those from [base] so the filter
 * can be compared with the base to tell whether the user has actually changed anything.
 */
fun TransactionFilter.inheritingScopeFrom(base: TransactionFilter): TransactionFilter = copy(
    accountIds = accountIds.ifEmpty { base.accountIds },
    categoryIds = categoryIds.ifEmpty { base.categoryIds },
    transactionTypes = transactionTypes.ifEmpty { base.transactionTypes }
)

fun countActiveFilters(
    filter: TransactionFilter,
    baseFilter: TransactionFilter = TransactionFilter(),
    rules: CardFilterRules? = null
): Int {
    var count = 0

    // 1. Timeline
    if (filter.timelineRange != baseFilter.timelineRange) count++

    // 2. Accounts
    if (filter.accountIds != baseFilter.accountIds) count++

    // 3. Transaction Types
    if (filter.transactionTypes != baseFilter.transactionTypes) count++

    // 4. Categories
    if (filter.categoryIds != baseFilter.categoryIds) count++

    // 5. Currencies
    if (filter.currencies != baseFilter.currencies) count++

    // 6. Payment Methods
    if (filter.paymentMethods != baseFilter.paymentMethods) count++

    // 7. Queries
    if (filter.queryNote != baseFilter.queryNote) count++
    if (filter.queryPayee != baseFilter.queryPayee) count++

    // 8. Amount
    if (filter.minAmount != baseFilter.minAmount || filter.maxAmount != baseFilter.maxAmount) count++

    // 9. Statuses
    if (filter.statuses != baseFilter.statuses) count++

    // 10. Tags
    if (filter.tags != baseFilter.tags || filter.matchAllTags != baseFilter.matchAllTags) count++

    return count
}

fun removeFilter(
    filter: TransactionFilter,
    field: String,
    value: Any?,
    onUpdateFilter: (TransactionFilter) -> Unit
) {
    val updatedFilter = when (field) {
        "accountIds" -> filter.copy(accountIds = filter.accountIds - (value as String))
        "transactionTypes" -> filter.copy(transactionTypes = filter.transactionTypes - (value as TransactionType))
        "dateRange" -> filter.copy(timelineRange = TimelineRange.AllTime())
        "categoryIds" -> filter.copy(categoryIds = filter.categoryIds - (value as Long))
        "currencies" -> filter.copy(currencies = filter.currencies - (value as String))
        "paymentMethods" -> filter.copy(paymentMethods = filter.paymentMethods - (value as PaymentMethod))
        "queryNote" -> filter.copy(queryNote = null)
        "queryPayee" -> filter.copy(queryPayee = null)
        "amountRange" -> filter.copy(minAmount = null, maxAmount = null)
        "statuses" -> filter.copy(statuses = filter.statuses - (value as TransactionStatus))
        "tags" -> filter.copy(tags = filter.tags - (value as String))
        else -> filter
    }
    onUpdateFilter(updatedFilter)
}

fun formatDateRange(startDate: Long?, endDate: Long?): String {
    val start = startDate?.let {
        java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            .format(java.util.Date(it))
    } ?: "Start"
    val end = endDate?.let {
        java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            .format(java.util.Date(it))
    } ?: "End"
    return "$start - $end"
}
