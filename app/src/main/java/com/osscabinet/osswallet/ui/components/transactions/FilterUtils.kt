package com.osscabinet.ossWallet.ui.components.transactions

import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Counts active filter criteria in a TransactionFilter relative to a base filter and rules.
 */
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
    
    // 6. Dates
    if (filter.timelineRange != TimelineRange.AllTime) count++
    
    // 7. Payment Methods
    if (filter.paymentMethods != baseFilter.paymentMethods) count++
    
    // 8. Queries
    if (filter.queryNote != baseFilter.queryNote) count++
    if (filter.queryPayee != baseFilter.queryPayee) count++
    
    // 9. Amount
    if (filter.minAmount != baseFilter.minAmount || filter.maxAmount != baseFilter.maxAmount) count++
    
    // 10. Statuses
    if (filter.statuses != baseFilter.statuses) count++
    
    // 11. Tags
    if (filter.tags != baseFilter.tags) count++

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
        "dateRange" -> filter.copy(timelineRange = TimelineRange.AllTime)
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
    val start = startDate?.let { java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "Start"
    val end = endDate?.let { java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "End"
    return "$start - $end"
}
