package com.osscabinet.ossWallet.ui.components.transactions

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus

/**
 * Counts active filter criteria in a TransactionFilter.
 * Note: timelineRange is usually not counted as an "adjustable filter chip" 
 * in the same way categories or accounts are.
 */
fun countActiveFilters(filter: TransactionFilter): Int {
    var count = 0
    count += filter.accountIds.size
    if (filter.transactionType != null) count++
    count += filter.categoryIds.size
    count += filter.currencies.size
    if (filter.startDate != null || filter.endDate != null) count++
    count += filter.paymentMethods.size
    if (!filter.queryNote.isNullOrBlank()) count++
    if (!filter.queryPayee.isNullOrBlank()) count++
    if (filter.minAmount != null || filter.maxAmount != null) count++
    count += filter.statuses.size
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
        "transactionType" -> filter.copy(transactionType = null)
        "dateRange" -> filter.copy(startDate = null, endDate = null)
        "categoryIds" -> filter.copy(categoryIds = filter.categoryIds - (value as Long))
        "currencies" -> filter.copy(currencies = filter.currencies - (value as String))
        "paymentMethods" -> filter.copy(paymentMethods = filter.paymentMethods - (value as String))
        "queryNote" -> filter.copy(queryNote = null)
        "queryPayee" -> filter.copy(queryPayee = null)
        "amountRange" -> filter.copy(minAmount = null, maxAmount = null)
        "statuses" -> filter.copy(statuses = filter.statuses - (value as TransactionStatus))
        else -> filter
    }
    onUpdateFilter(updatedFilter)
}

fun formatDateRange(startDate: Long?, endDate: Long?): String {
    val start = startDate?.let { java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "Start"
    val end = endDate?.let { java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "End"
    return "$start - $end"
}
