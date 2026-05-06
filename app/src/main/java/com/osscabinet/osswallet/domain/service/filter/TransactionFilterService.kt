package com.osscabinet.ossWallet.domain.service.filter

import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import java.math.BigDecimal

/**
 * **DEPRECATED**: All functionality has been merged into [FilterService].
 * Use [FilterService] directly for all transaction filtering operations.
 *
 * This service now acts as a thin wrapper for backward compatibility.
 * It will be removed in a future release.
 */
@Deprecated(
    message = "Use FilterService directly. All filter methods are available there with better type safety.",
    replaceWith = ReplaceWith("FilterService"),
    level = DeprecationLevel.WARNING
)
object TransactionFilterService {

    @Deprecated(
        "Use FilterService.filterTransactions instead",
        ReplaceWith("FilterService.filterTransactions(transactions, filter)")
    )
    fun filter(transactions: List<Transaction>, filter: TransactionFilter): List<Transaction> =
        FilterService.filterTransactions(transactions, filter)

    @Deprecated(
        "Use FilterService.expandCategoryIds instead",
        ReplaceWith("FilterService.expandCategoryIds(categoryIds, allCategories)")
    )
    fun expandCategoryIds(categoryIds: List<Long>, allCategories: List<Category>): List<Long> =
        FilterService.expandCategoryIds(categoryIds, allCategories)

    @Deprecated(
        "Use FilterService.filterIncome instead",
        ReplaceWith("FilterService.filterIncome(transactions)")
    )
    fun filterIncome(transactions: List<Transaction>): List<Transaction> =
        FilterService.filterIncome(transactions)

    @Deprecated(
        "Use FilterService.filterExpense instead",
        ReplaceWith("FilterService.filterExpense(transactions)")
    )
    fun filterExpense(transactions: List<Transaction>): List<Transaction> =
        FilterService.filterExpense(transactions)

    @Deprecated(
        "Use FilterService.filterByCurrency or FilterService.filterByCurrencies instead",
        ReplaceWith("FilterService.filterByCurrency(transactions, currency) or FilterService.filterByCurrencies(transactions, currencies)")
    )
    fun filterByCurrency(transactions: List<Transaction>, currencies: Any): List<Transaction> =
        when (currencies) {
            is String -> FilterService.filterByCurrency(transactions, currencies)
            is Set<*> -> FilterService.filterByCurrencies(transactions, currencies as Set<String>)
            else -> throw IllegalArgumentException("currencies must be String or Set<String>")
        }

    @Deprecated(
        "Use FilterService.filterByCategory or FilterService.filterByCategories instead",
        ReplaceWith("FilterService.filterByCategory(transactions, category) or FilterService.filterByCategories(transactions, categories)")
    )
    fun filterByCategory(transactions: List<Transaction>, categories: Any): List<Transaction> =
        when (categories) {
            is String -> FilterService.filterByCategory(transactions, categories)
            is Set<*> -> FilterService.filterByCategories(transactions, categories as Set<String>)
            else -> throw IllegalArgumentException("categories must be String or Set<String>")
        }

    @Deprecated(
        "Use FilterService.filterByAccount or FilterService.filterByAccounts instead",
        ReplaceWith("FilterService.filterByAccount(transactions, accountId) or FilterService.filterByAccounts(transactions, accountIds)")
    )
    fun filterByAccount(transactions: List<Transaction>, accountIds: Any): List<Transaction> =
        when (accountIds) {
            is String -> FilterService.filterByAccount(transactions, accountIds)
            is Set<*> -> FilterService.filterByAccounts(transactions, accountIds as Set<String>)
            else -> throw IllegalArgumentException("accountIds must be String or Set<String>")
        }

    @Deprecated(
        "Use FilterService.filterByStatus or FilterService.filterByStatuses instead",
        ReplaceWith("FilterService.filterByStatus(transactions, status) or FilterService.filterByStatuses(transactions, statuses)")
    )
    fun filterByStatus(transactions: List<Transaction>, statuses: Any): List<Transaction> =
        when (statuses) {
            is TransactionStatus -> FilterService.filterByStatus(transactions, statuses)
            is Set<*> -> FilterService.filterByStatuses(
                transactions,
                statuses as Set<TransactionStatus>
            )

            else -> throw IllegalArgumentException("statuses must be TransactionStatus or Set<TransactionStatus>")
        }

    @Deprecated(
        "Use FilterService.filterByTransactionType instead",
        ReplaceWith("FilterService.filterByTransactionType(transactions, transactionType)")
    )
    fun filterByTransactionType(
        transactions: List<Transaction>,
        transactionType: String
    ): List<Transaction> =
        FilterService.filterByTransactionType(transactions, transactionType)

    @Deprecated(
        "Use FilterService.filterByAmountGreaterThan instead",
        ReplaceWith("FilterService.filterByAmountGreaterThan(transactions, amount)")
    )
    fun filterByAmountGreaterThan(
        transactions: List<Transaction>,
        amount: BigDecimal
    ): List<Transaction> =
        FilterService.filterByAmountGreaterThan(transactions, amount)

    @Deprecated(
        "Use FilterService.filterByAmountLessThan instead",
        ReplaceWith("FilterService.filterByAmountLessThan(transactions, amount)")
    )
    fun filterByAmountLessThan(
        transactions: List<Transaction>,
        amount: BigDecimal
    ): List<Transaction> =
        FilterService.filterByAmountLessThan(transactions, amount)

    @Deprecated(
        "Use FilterService.filterByAmountRange instead",
        ReplaceWith("FilterService.filterByAmountRange(transactions, minAmount, maxAmount)")
    )
    fun filterByAmountRange(
        transactions: List<Transaction>,
        minAmount: BigDecimal,
        maxAmount: BigDecimal
    ): List<Transaction> =
        FilterService.filterByAmountRange(transactions, minAmount, maxAmount)

    @Deprecated(
        "Use FilterService.filterByDateRange instead",
        ReplaceWith("FilterService.filterByDateRange(transactions, startTime, endTime)")
    )
    fun filterByDateRange(
        transactions: List<Transaction>,
        startTime: Long,
        endTime: Long
    ): List<Transaction> =
        FilterService.filterByDateRange(transactions, startTime, endTime)

    @Deprecated(
        "Use FilterService.filterByTags instead",
        ReplaceWith("FilterService.filterByTags(transactions, tags)")
    )
    fun filterByTags(transactions: List<Transaction>, tags: Set<String>): List<Transaction> =
        FilterService.filterByTags(transactions, tags)

    @Deprecated(
        "Use FilterService.filterByAllTags instead",
        ReplaceWith("FilterService.filterByAllTags(transactions, tags)")
    )
    fun filterByAllTags(transactions: List<Transaction>, tags: Set<String>): List<Transaction> =
        FilterService.filterByAllTags(transactions, tags)
}
