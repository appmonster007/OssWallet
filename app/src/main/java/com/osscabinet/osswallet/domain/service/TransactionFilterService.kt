package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import java.math.BigDecimal

/**
 * Service for filtering transactions based on various criteria.
 *
 * This service is stateless and pure Kotlin (no Android dependencies).
 * All methods are independently testable.
 */
object TransactionFilterService {

    /**
     * Filters transactions based on the provided filter criteria.
     * Delegates to FilterService for the actual filtering logic.
     *
     * @param transactions List of transactions to filter
     * @param filter Filter criteria to apply
     * @return Filtered list of transactions
     */
    fun filter(transactions: List<Transaction>, filter: TransactionFilter): List<Transaction> =
        FilterService.filterTransactions(transactions, filter)

    /**
     * Expands selected category IDs to include all their descendants.
     */
    fun expandCategoryIds(
        categoryIds: List<Long>,
        allCategories: List<com.osscabinet.ossWallet.domain.model.Category>
    ): List<Long> {
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
     *
     * @param transactions List of transactions to filter
     * @return List of income transactions
     */
    fun filterIncome(transactions: List<Transaction>): List<Transaction> =
        transactions.filter { it.amount.signum() >= 0 }

    /**
     * Filters transactions to only include expense transactions (negative amounts).
     *
     * @param transactions List of transactions to filter
     * @return List of expense transactions
     */
    fun filterExpense(transactions: List<Transaction>): List<Transaction> =
        transactions.filter { it.amount.signum() < 0 }

    /**
     * Filters transactions by currency code(s).
     * Accepts either a single currency or multiple currencies.
     *
     * @param transactions List of transactions to filter
     * @param currencies Currency code(s) to filter by (String for single, Set<String> for multiple)
     * @return List of transactions with the specified currency/currencies
     */
    fun filterByCurrency(transactions: List<Transaction>, currencies: Any): List<Transaction> =
        when (currencies) {
            is String -> transactions.filter { it.currency == currencies }
            is Set<*> -> transactions.filter { it.currency in currencies }
            else -> throw IllegalArgumentException("currencies must be String or Set<String>")
        }

    /**
     * Filters transactions by category name(s).
     * Accepts either a single category or multiple categories.
     *
     * @param transactions List of transactions to filter
     * @param categories Category name(s) to filter by (String for single, Set<String> for multiple)
     * @return List of transactions with the specified category/categories
     */
    fun filterByCategory(transactions: List<Transaction>, categories: Any): List<Transaction> =
        when (categories) {
            is String -> transactions.filter { it.category == categories }
            is Set<*> -> transactions.filter { it.category in categories }
            else -> throw IllegalArgumentException("categories must be String or Set<String>")
        }

    /**
     * Filters transactions by account ID(s).
     * Accepts either a single account ID or multiple account IDs.
     *
     * @param transactions List of transactions to filter
     * @param accountIds Account ID(s) to filter by (String for single, Set<String> for multiple)
     * @return List of transactions for the specified account(s)
     */
    fun filterByAccount(transactions: List<Transaction>, accountIds: Any): List<Transaction> =
        when (accountIds) {
            is String -> transactions.filter { it.accountId == accountIds }
            is Set<*> -> transactions.filter { it.accountId in accountIds }
            else -> throw IllegalArgumentException("accountIds must be String or Set<String>")
        }

    /**
     * Filters transactions by transaction status(es).
     * Accepts either a single status or multiple statuses.
     *
     * @param transactions List of transactions to filter
     * @param statuses Status(es) to filter by (TransactionStatus for single, Set<TransactionStatus> for multiple)
     * @return List of transactions with the specified status(es)
     */
    fun filterByStatus(transactions: List<Transaction>, statuses: Any): List<Transaction> =
        when (statuses) {
            is TransactionStatus -> transactions.filter { it.status == statuses }
            is Set<*> -> transactions.filter { it.status in statuses }
            else -> throw IllegalArgumentException("statuses must be TransactionStatus or Set<TransactionStatus>")
        }

    /**
     * Filters transactions by transaction type.
     *
     * @param transactions List of transactions to filter
     * @param transactionType Transaction type to filter by
     * @return List of transactions with the specified transaction type
     */
    fun filterByTransactionType(transactions: List<Transaction>, transactionType: String): List<Transaction> =
        transactions.filter { it.transactionType == transactionType }

    /**
     * Filters transactions by amount greater than or equal to a threshold.
     *
     * @param transactions List of transactions to filter
     * @param amount Amount threshold
     * @return List of transactions with amount >= threshold
     */
    fun filterByAmountGreaterThan(transactions: List<Transaction>, amount: BigDecimal): List<Transaction> =
        transactions.filter { it.amount >= amount }

    /**
     * Filters transactions by amount less than or equal to a threshold.
     *
     * @param transactions List of transactions to filter
     * @param amount Amount threshold
     * @return List of transactions with amount <= threshold
     */
    fun filterByAmountLessThan(transactions: List<Transaction>, amount: BigDecimal): List<Transaction> =
        transactions.filter { it.amount <= amount }

    /**
     * Filters transactions by amount range (inclusive).
     *
     * @param transactions List of transactions to filter
     * @param minAmount Minimum amount (inclusive)
     * @param maxAmount Maximum amount (inclusive)
     * @return List of transactions with amount in the specified range
     */
    fun filterByAmountRange(
        transactions: List<Transaction>,
        minAmount: BigDecimal,
        maxAmount: BigDecimal
    ): List<Transaction> =
        transactions.filter { it.amount >= minAmount && it.amount <= maxAmount }

    /**
     * Filters transactions by date range (inclusive).
     *
     * @param transactions List of transactions to filter
     * @param startTime Start timestamp in milliseconds (inclusive)
     * @param endTime End timestamp in milliseconds (inclusive)
     * @return List of transactions within the specified date range
     */
    fun filterByDateRange(
        transactions: List<Transaction>,
        startTime: Long,
        endTime: Long
    ): List<Transaction> =
        transactions.filter { it.timestamp in startTime..endTime }

    /**
     * Filters transactions that contain any of the specified tags.
     *
     * @param transactions List of transactions to filter
     * @param tags Set of tags to filter by
     * @return List of transactions that contain any of the specified tags
     */
    fun filterByTags(transactions: List<Transaction>, tags: Set<String>): List<Transaction> =
        transactions.filter { tx -> tx.tags.any { it in tags } }

    /**
     * Filters transactions that contain all of the specified tags.
     *
     * @param transactions List of transactions to filter
     * @param tags Set of tags to filter by
     * @return List of transactions that contain all of the specified tags
     */
    fun filterByAllTags(transactions: List<Transaction>, tags: Set<String>): List<Transaction> =
        transactions.filter { tx -> tags.all { it in tx.tags } }
}
