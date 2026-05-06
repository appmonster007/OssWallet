package com.osscabinet.ossWallet.domain.service.filter

import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
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
            val matchesCategories =
                filter.categoryIds.isEmpty() || tx.categoryId in filter.categoryIds
            val matchesType =
                filter.transactionType == null || tx.transactionType == filter.transactionType

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

            val inWarrantyRange = (filter.warrantyExpirationStart == null || (tx.warrantyExpiration
                ?: 0) >= filter.warrantyExpirationStart) &&
                    (filter.warrantyExpirationEnd == null || (tx.warrantyExpiration
                        ?: Long.MAX_VALUE) <= filter.warrantyExpirationEnd)

            val matchesPaymentMethod =
                filter.paymentMethods.isEmpty() || tx.paymentMethod in filter.paymentMethods

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

    // ==================== Convenience Filter Methods ====================

    /**
     * Expands selected category IDs to include all their descendants.
     */
    fun expandCategoryIds(categoryIds: List<Long>, allCategories: List<Category>): List<Long> {
        if (categoryIds.isEmpty()) return emptyList()
        val expanded = mutableSetOf<Long>()
        val queue = categoryIds.toMutableList()

        while (queue.isNotEmpty()) {
            val id = queue.removeAt(0)
            if (expanded.add(id)) {
                allCategories.filter { it.parentId == id }.forEach {
                    queue.add(it.id)
                }
            }
        }
        return expanded.toList()
    }

    /**
     * Filters transactions to only include income transactions (positive amounts).
     */
    fun filterIncome(transactions: List<Transaction>): List<Transaction> =
        transactions.filter { it.amount.signum() >= 0 }

    /**
     * Filters transactions to only include expense transactions (negative amounts).
     */
    fun filterExpense(transactions: List<Transaction>): List<Transaction> =
        transactions.filter { it.amount.signum() < 0 }

    /**
     * Filters transactions by a single currency code.
     */
    fun filterByCurrency(transactions: List<Transaction>, currency: String): List<Transaction> =
        transactions.filter { it.currency == currency }

    /**
     * Filters transactions by multiple currency codes.
     */
    fun filterByCurrencies(
        transactions: List<Transaction>,
        currencies: Set<String>
    ): List<Transaction> =
        transactions.filter { it.currency in currencies }

    /**
     * Filters transactions by a single category name.
     */
    fun filterByCategory(transactions: List<Transaction>, category: String): List<Transaction> =
        transactions.filter { it.category == category }

    /**
     * Filters transactions by multiple category names.
     */
    fun filterByCategories(
        transactions: List<Transaction>,
        categories: Set<String>
    ): List<Transaction> =
        transactions.filter { it.category in categories }

    /**
     * Filters transactions by a single account ID.
     */
    fun filterByAccount(transactions: List<Transaction>, accountId: String): List<Transaction> =
        transactions.filter { it.accountId == accountId }

    /**
     * Filters transactions by multiple account IDs.
     */
    fun filterByAccounts(
        transactions: List<Transaction>,
        accountIds: Set<String>
    ): List<Transaction> =
        transactions.filter { it.accountId in accountIds }

    /**
     * Filters transactions by a single status.
     */
    fun filterByStatus(
        transactions: List<Transaction>,
        status: TransactionStatus
    ): List<Transaction> =
        transactions.filter { it.status == status }

    /**
     * Filters transactions by multiple statuses.
     */
    fun filterByStatuses(
        transactions: List<Transaction>,
        statuses: Set<TransactionStatus>
    ): List<Transaction> =
        transactions.filter { it.status in statuses }

    /**
     * Filters transactions by transaction type.
     */
    fun filterByTransactionType(
        transactions: List<Transaction>,
        transactionType: String
    ): List<Transaction> =
        transactions.filter { it.transactionType == transactionType }

    /**
     * Filters transactions by amount greater than or equal to a threshold.
     */
    fun filterByAmountGreaterThan(
        transactions: List<Transaction>,
        amount: BigDecimal
    ): List<Transaction> =
        transactions.filter { it.amount >= amount }

    /**
     * Filters transactions by amount less than or equal to a threshold.
     */
    fun filterByAmountLessThan(
        transactions: List<Transaction>,
        amount: BigDecimal
    ): List<Transaction> =
        transactions.filter { it.amount <= amount }

    /**
     * Filters transactions by amount range (inclusive).
     */
    fun filterByAmountRange(
        transactions: List<Transaction>,
        minAmount: BigDecimal,
        maxAmount: BigDecimal
    ): List<Transaction> =
        transactions.filter { it.amount >= minAmount && it.amount <= maxAmount }

    /**
     * Filters transactions by date range (inclusive).
     */
    fun filterByDateRange(
        transactions: List<Transaction>,
        startTime: Long,
        endTime: Long
    ): List<Transaction> =
        transactions.filter { it.timestamp in startTime..endTime }

    /**
     * Filters transactions that contain any of the specified tags.
     */
    fun filterByTags(transactions: List<Transaction>, tags: Set<String>): List<Transaction> =
        transactions.filter { tx -> tx.tags.any { it in tags } }

    /**
     * Filters transactions that contain all of the specified tags.
     */
    fun filterByAllTags(transactions: List<Transaction>, tags: Set<String>): List<Transaction> =
        transactions.filter { tx -> tags.all { it in tx.tags } }
}
