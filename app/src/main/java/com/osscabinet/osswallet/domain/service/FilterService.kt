package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import java.math.BigDecimal

/**
 * Domain service for unified transaction filtering.
 * Consolidates in-memory filtering logic used across ViewModels and Repositories.
 */
object FilterService {

    /**
     * Filters a list of transactions based on the provided filter criteria.
     * This implementation is the "source of truth" for in-memory filtering.
     * 
     * @param transactions The list of transactions to filter
     * @param filter The filter criteria
     * @return The filtered list of transactions
     */
    fun filterTransactions(
        transactions: List<Transaction>,
        filter: TransactionFilter
    ): List<Transaction> {
        val (startTime, endTime) = TimelineService.getDateRange(filter.timelineRange)

        val now = System.currentTimeMillis()

        return transactions.filter { tx ->
            // 0. Future exclusion (checked first for performance)
            if (filter.excludeFuture && tx.timestamp > now) return@filter false

            // 1. Timeline check (TimelineRange OR explicit dates)
            val effectiveStart = filter.startDate ?: startTime
            val effectiveEnd = filter.endDate ?: endTime
            
            val inTimeRange = (effectiveStart == null || tx.timestamp >= effectiveStart) &&
                             (effectiveEnd == null || tx.timestamp <= effectiveEnd)
            
            if (!inTimeRange) return@filter false

            // 2. Attribute checks
            val matchesAccounts = filter.accountIds.isEmpty() || tx.accountId in filter.accountIds
            val matchesCategories = filter.categoryIds.isEmpty() || tx.categoryId in filter.categoryIds
            val matchesType = filter.transactionType == null || tx.transactionType == filter.transactionType
            
            // Amount check (using absolute value for consistency with UI filters)
            val matchesAmount = (filter.minAmount == null || tx.amount.abs() >= filter.minAmount) &&
                               (filter.maxAmount == null || tx.amount.abs() <= filter.maxAmount)
            
            val matchesStatuses = filter.statuses.isEmpty() || tx.status in filter.statuses
            val matchesCurrencies = filter.currencies.isEmpty() || tx.currency in filter.currencies
            val matchesTags = filter.tags.isEmpty() || tx.tags.any { it in filter.tags }
            
            // Payee match (Regex or string contains)
            val matchesPayee = if (filter.isPayeeRegex && !filter.queryPayee.isNullOrBlank()) {
                try {
                    Regex(filter.queryPayee, RegexOption.IGNORE_CASE).containsMatchIn(tx.payee)
                } catch (e: Exception) {
                    false
                }
            } else if (!filter.queryPayee.isNullOrBlank()) {
                tx.payee.contains(filter.queryPayee, ignoreCase = true)
            } else true

            // Note match (Regex or string contains)
            val matchesNote = if (filter.isNoteRegex && !filter.queryNote.isNullOrBlank()) {
                try {
                    Regex(filter.queryNote, RegexOption.IGNORE_CASE).containsMatchIn(tx.note)
                } catch (e: Exception) {
                    false
                }
            } else if (!filter.queryNote.isNullOrBlank()) {
                tx.note.contains(filter.queryNote, ignoreCase = true)
            } else true
            
            // Warranty check
            val matchesWarranty = when (filter.hasWarranty) {
                true -> tx.warrantyExpiration != null
                false -> tx.warrantyExpiration == null
                null -> true
            }
            
            val inWarrantyRange = (filter.warrantyExpirationStart == null || (tx.warrantyExpiration ?: 0) >= filter.warrantyExpirationStart) &&
                                 (filter.warrantyExpirationEnd == null || (tx.warrantyExpiration ?: Long.MAX_VALUE) <= filter.warrantyExpirationEnd)

            val matchesPaymentMethod = filter.paymentMethods.isEmpty() || tx.paymentMethod in filter.paymentMethods

            matchesAccounts && matchesCategories && matchesType && matchesAmount && 
            matchesStatuses && matchesPayee && matchesNote && matchesCurrencies &&
            matchesWarranty && inWarrantyRange && matchesPaymentMethod && matchesTags
        }
    }

    /**
     * Creates a filter for the period immediately preceding the current filter's timeline range.
     * Useful for period-over-period comparison.
     */
    fun getPreviousPeriodFilter(currentFilter: TransactionFilter): TransactionFilter {
        val (prevStart, prevEnd) = TimelineService.getPreviousRange(currentFilter.timelineRange)
        return currentFilter.copy(
            timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.CustomRange(
                start = prevStart ?: 0L,
                end = prevEnd ?: System.currentTimeMillis()
            ),
            startDate = null,
            endDate = null
        )
    }
}
