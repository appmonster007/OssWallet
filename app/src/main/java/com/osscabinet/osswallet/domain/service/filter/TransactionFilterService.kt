package com.osscabinet.ossWallet.domain.service.filter

import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.util.toSmartRegex
import java.math.BigDecimal

/**
 * Domain service for unified transaction filtering.
 * Consolidates complex filtering logic used across ViewModels and Repositories.
 */
object TransactionFilterService {

    /**
     * Filters a list of transactions based on the provided filter criteria.
     */
    fun filter(
        transactions: List<Transaction>,
        filter: TransactionFilter
    ): List<Transaction> = filterTransactions(transactions, filter)

    fun filterTransactions(
        transactions: List<Transaction>,
        filter: TransactionFilter
    ): List<Transaction> {
        val (startTime, endTime) = TimelineService.getDateRange(filter.timelineRange)
        val now = System.currentTimeMillis()

        return transactions.filter { tx ->
            matches(tx, filter, startTime, endTime, now)
        }
    }

    fun matches(
        tx: Transaction,
        filter: TransactionFilter,
        startTime: Long? = null,
        endTime: Long? = null,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        // 0. Future exclusion
        if (filter.excludeFuture && tx.timestamp > now) return false

        // 1. Timeline check
        val effectiveStartTime: Long?
        val effectiveEndTime: Long?
        if (startTime == null && endTime == null) {
            val (s, e) = TimelineService.getDateRange(filter.timelineRange)
            effectiveStartTime = s
            effectiveEndTime = e
        } else {
            effectiveStartTime = startTime
            effectiveEndTime = endTime
        }

        val inTimeRange = (effectiveStartTime == null || tx.timestamp >= effectiveStartTime) &&
                (effectiveEndTime == null || tx.timestamp <= effectiveEndTime)
        if (!inTimeRange) return false

        // 2. Attribute checks
        val matchesAccounts = filter.accountIds.isEmpty() || tx.accountId in filter.accountIds
        val matchesCategories = filter.categoryIds.isEmpty() || tx.categoryId in filter.categoryIds
        
        val matchesType = filter.transactionTypes.isEmpty() || tx.transactionType in filter.transactionTypes

        // Amount check (absolute value for UI consistency)
        val matchesAmount = (filter.minAmount == null || tx.amount.abs() >= filter.minAmount) &&
                (filter.maxAmount == null || tx.amount.abs() <= filter.maxAmount)

        val matchesStatuses = filter.statuses.isEmpty() || tx.status in filter.statuses
        val matchesCurrencies = filter.currencies.isEmpty() || tx.currency in filter.currencies
        val matchesTags = tx.tags.containsAll(filter.tags)

        // Payee match
        val matchesPayee = if (filter.isPayeeRegex && !filter.queryPayee.isNullOrBlank()) {
            try { filter.queryPayee.toSmartRegex().containsMatchIn(tx.payee) } catch (e: Exception) { false }
        } else if (!filter.queryPayee.isNullOrBlank()) {
            tx.payee.contains(filter.queryPayee, ignoreCase = true)
        } else true

        // Note match
        val matchesNote = if (filter.isNoteRegex && !filter.queryNote.isNullOrBlank()) {
            try { filter.queryNote.toSmartRegex().containsMatchIn(tx.note) } catch (e: Exception) { false }
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
        val matchesLinkedTransactions = filter.linkedTransactionIds.isEmpty() || tx.id in filter.linkedTransactionIds

        return matchesAccounts && matchesCategories && matchesType && matchesAmount &&
                matchesStatuses && matchesPayee && matchesNote && matchesCurrencies &&
                matchesWarranty && inWarrantyRange && matchesPaymentMethod && matchesTags &&
                matchesLinkedTransactions
    }

    fun filterTransactionsWithBalance(
        items: List<TransactionWithBalance>,
        filter: TransactionFilter
    ): List<TransactionWithBalance> {
        val (startTime, endTime) = TimelineService.getDateRange(filter.timelineRange)
        val now = System.currentTimeMillis()

        return items.filter { matches(it.transaction, filter, startTime, endTime, now) }
    }

    /**
     * Creates a filter for the period immediately preceding the current filter's timeline range.
     */
    fun getPreviousPeriodFilter(currentFilter: TransactionFilter): TransactionFilter {
        val (prevStart, prevEnd) = TimelineService.getPreviousRange(currentFilter.timelineRange)
        return currentFilter.copy(
            timelineRange = TimelineRange.CustomRange(
                start = prevStart ?: 0L,
                end = prevEnd ?: System.currentTimeMillis()
            )
        )
    }

    /**
     * Expands selected category IDs to include all their descendants.
     */
    fun expandCategoryIds(categoryIds: Collection<Long>, allCategories: List<Category>): Set<Long> {
        if (categoryIds.isEmpty()) return emptySet()
        val expanded = mutableSetOf<Long>()
        val queue = categoryIds.toMutableList()

        while (queue.isNotEmpty()) {
            val id = queue.removeAt(0)
            if (expanded.add(id)) {
                allCategories.filter { it.parentId == id }.forEach { queue.add(it.id) }
            }
        }
        return expanded
    }

    /**
     * Common filters for semantic clarity.
     */
    fun filterIncome(transactions: List<Transaction>): List<Transaction> = transactions.filter { it.transactionType == TransactionType.INCOME }
    fun filterExpense(transactions: List<Transaction>): List<Transaction> = transactions.filter { it.transactionType == TransactionType.EXPENSE }

    // ==================== Additional Convenience Filter Methods ====================

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
     * Filters transactions by a single category ID.
     */
    fun filterByCategoryId(transactions: List<Transaction>, categoryId: Long): List<Transaction> =
        transactions.filter { it.categoryId == categoryId }

    /**
     * Filters transactions by multiple category IDs.
     */
    fun filterByCategoryIds(
        transactions: List<Transaction>,
        categoryIds: Set<Long>
    ): List<Transaction> =
        transactions.filter { it.categoryId in categoryIds }

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
        transactionType: TransactionType
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

    /**
     * Filters items to recent ones (last N or last X days, whichever is more)
     */
    fun <T> filterToRecent(
        items: List<T>,
        getTimestamp: (T) -> Long,
        limit: Int = 20,
        days: Int = 7,
        excludeFuture: Boolean = true
    ): List<T> {
        val now = System.currentTimeMillis()
        val base = if (excludeFuture) items.filter { getTimestamp(it) <= now } else items

        // Ensure sorted by descending timestamp
        val sortedBase = base.sortedByDescending { getTimestamp(it) }

        val cutoffTime = now - (days * 24 * 60 * 60 * 1000L)
        val transactionsFromLastDays = sortedBase.filter { getTimestamp(it) >= cutoffTime }
        val lastLimitTransactions = sortedBase.take(limit)

        return if (transactionsFromLastDays.size > lastLimitTransactions.size) {
            transactionsFromLastDays
        } else {
            lastLimitTransactions
        }
    }
}
