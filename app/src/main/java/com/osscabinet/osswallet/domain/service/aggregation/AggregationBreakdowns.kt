package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.service.AccountBreakdown
import com.osscabinet.ossWallet.domain.service.CategoryBreakdown
import com.osscabinet.ossWallet.domain.service.CurrencyBreakdown
import com.osscabinet.ossWallet.domain.service.DateBreakdown
import com.osscabinet.ossWallet.domain.service.FinancialMetrics
import com.osscabinet.ossWallet.domain.service.FinancialSummary
import com.osscabinet.ossWallet.domain.service.MonthBreakdown
import com.osscabinet.ossWallet.domain.service.PaymentMethodBreakdown
import com.osscabinet.ossWallet.domain.service.StatusBreakdown
import com.osscabinet.ossWallet.domain.service.TagBreakdown
import com.osscabinet.ossWallet.domain.service.TransactionTypeBreakdown
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.filter.FilterService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineGroupingService
import java.math.BigDecimal
import java.time.Instant

/**
 * Extension functions for breaking down transactions by various dimensions.
 *
 * These functions provide a fluent API for transaction aggregation:
 * ```kotlin
 * val byCurrency = transactions.breakdownByCurrency()
 * val byAccount = transactions.breakdownByAccount(accounts, exchangeRates, baseCurrency)
 * ```
 */

// ==================== Core Breakdown Methods ====================

/**
 * Breaks down transactions by currency.
 */
fun List<Transaction>.breakdownByCurrency(): List<CurrencyBreakdown> =
    groupBy { it.currency }
        .map { (currency, txs) ->
            val incomeTx = FilterService.filterIncome(txs)
            val expenseTx = FilterService.filterExpense(txs)
            CurrencyBreakdown(
                currency = currency,
                financialSummary = FinancialSummary(
                    totalAmount = txs.sumOf { it.amount },
                    income = incomeTx.sumOf { it.amount },
                    expense = expenseTx.sumOf { it.amount },
                    transactionCount = txs.size
                )
            )
        }
        .sortedByDescending { it.totalAmount }

/**
 * Breaks down transactions by account.
 */
fun List<Transaction>.breakdownByAccount(
    accounts: List<Account>,
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<AccountBreakdown> =
    AggregationService.createBreakdown(this, { it.accountId }, { accountId, summaries ->
        FinancialMetrics(
            key = accountId,
            financialSummary = FinancialSummary(
                totalAmount = summaries.values.sumOf { it.totalAmount },
                income = summaries.values.sumOf { it.income },
                expense = summaries.values.sumOf { it.expense },
                transactionCount = summaries.values.sumOf { it.transactionCount }
            ),
            currencyBreakdown = summaries
        )
    }, exchangeRates, baseCurrency).sortedByDescending { it.totalAmount }

/**
 * Breaks down transactions by category.
 */
fun List<Transaction>.breakdownByCategory(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<CategoryBreakdown> =
    AggregationService.createBreakdown(this, { it.category }, { category, summaries ->
        FinancialMetrics(
            key = category,
            financialSummary = FinancialSummary(
                totalAmount = summaries.values.sumOf { it.totalAmount },
                income = summaries.values.sumOf { it.income },
                expense = summaries.values.sumOf { it.expense },
                transactionCount = summaries.values.sumOf { it.transactionCount }
            ),
            currencyBreakdown = summaries
        )
    }, exchangeRates, baseCurrency).sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by date.
 */
fun List<Transaction>.breakdownByDate(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<DateBreakdown> {
    if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
        throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
    }

    return groupBy {
        Instant.ofEpochMilli(it.timestamp).atZone(TimelineGroupingService.systemZoneId)
            .toLocalDate().toString()
    }
        .map { (date, txs) ->
            val summaries = AggregationService.calculateSummaryByCurrency(txs)
            val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                summaries.mapValues { (currency, summary) ->
                    CurrencyService.convertFinancialSummary(
                        summary,
                        currency,
                        baseCurrency,
                        exchangeRates
                    )
                }
            } else {
                summaries
            }
            FinancialMetrics(
                key = date,
                financialSummary = FinancialSummary(
                    totalAmount = convertedSummaries.values.sumOf { it.totalAmount },
                    income = convertedSummaries.values.sumOf { it.income },
                    expense = convertedSummaries.values.sumOf { it.expense },
                    transactionCount = convertedSummaries.values.sumOf { it.transactionCount }
                ),
                currencyBreakdown = summaries
            )
        }
        .sortedByDescending { it.totalAmount.abs() }
}

/**
 * Breaks down transactions by month.
 */
fun List<Transaction>.breakdownByMonth(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<MonthBreakdown> {
    if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
        throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
    }

    return groupBy { TimelineGroupingService.formatMonth(it.timestamp) }
        .map { (month, txs) ->
            val summaries = AggregationService.calculateSummaryByCurrency(txs)
            val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                summaries.mapValues { (currency, summary) ->
                    CurrencyService.convertFinancialSummary(
                        summary,
                        currency,
                        baseCurrency,
                        exchangeRates
                    )
                }
            } else {
                summaries
            }
            FinancialMetrics(
                key = month,
                financialSummary = FinancialSummary(
                    totalAmount = convertedSummaries.values.sumOf { it.totalAmount },
                    income = convertedSummaries.values.sumOf { it.income },
                    expense = convertedSummaries.values.sumOf { it.expense },
                    transactionCount = convertedSummaries.values.sumOf { it.transactionCount }
                ),
                currencyBreakdown = summaries
            )
        }
        .sortedByDescending { it.totalAmount.abs() }
}

/**
 * Breaks down transactions by transaction type.
 */
fun List<Transaction>.breakdownByTransactionType(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<TransactionTypeBreakdown> =
    AggregationService.createBreakdown(this, { it.transactionType }, { type, summaries ->
        FinancialMetrics(
            key = type,
            financialSummary = FinancialSummary(
                totalAmount = summaries.values.sumOf { it.totalAmount },
                income = summaries.values.sumOf { it.income },
                expense = summaries.values.sumOf { it.expense },
                transactionCount = summaries.values.sumOf { it.transactionCount }
            ),
            currencyBreakdown = summaries
        )
    }, exchangeRates, baseCurrency).sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by payment method.
 */
fun List<Transaction>.breakdownByPaymentMethod(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<PaymentMethodBreakdown> =
    AggregationService.createBreakdown(this, { it.paymentMethod }, { method, summaries ->
        FinancialMetrics(
            key = method,
            financialSummary = FinancialSummary(
                totalAmount = summaries.values.sumOf { it.totalAmount },
                income = summaries.values.sumOf { it.income },
                expense = summaries.values.sumOf { it.expense },
                transactionCount = summaries.values.sumOf { it.transactionCount }
            ),
            currencyBreakdown = summaries
        )
    }, exchangeRates, baseCurrency).sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by status.
 */
fun List<Transaction>.breakdownByStatus(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<StatusBreakdown> =
    AggregationService.createBreakdown(this, { it.status.name }, { status, summaries ->
        FinancialMetrics(
            key = status,
            financialSummary = FinancialSummary(
                totalAmount = summaries.values.sumOf { it.totalAmount },
                income = summaries.values.sumOf { it.income },
                expense = summaries.values.sumOf { it.expense },
                transactionCount = summaries.values.sumOf { it.transactionCount }
            ),
            currencyBreakdown = summaries
        )
    }, exchangeRates, baseCurrency).sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by tags.
 */
fun List<Transaction>.breakdownByTags(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<TagBreakdown> {
    if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
        throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
    }

    return flatMap { tx -> tx.tags.map { tag -> tag to tx } }
        .groupBy { it.first }
        .mapValues { (_, pairs) -> pairs.map { it.second } }
        .map { (tag, txs) ->
            val summaries = AggregationService.calculateSummaryByCurrency(txs)
            val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                summaries.mapValues { (currency, summary) ->
                    CurrencyService.convertFinancialSummary(
                        summary,
                        currency,
                        baseCurrency,
                        exchangeRates
                    )
                }
            } else {
                summaries
            }
            FinancialMetrics(
                key = tag,
                financialSummary = FinancialSummary(
                    totalAmount = convertedSummaries.values.sumOf { it.totalAmount },
                    income = convertedSummaries.values.sumOf { it.income },
                    expense = convertedSummaries.values.sumOf { it.expense },
                    transactionCount = convertedSummaries.values.sumOf { it.transactionCount }
                ),
                currencyBreakdown = summaries
            )
        }
        .sortedByDescending { it.totalAmount.abs() }
}

// ==================== Composite Breakdown Methods ====================

/**
 * Breaks down transactions by account for a specific currency.
 */
fun List<Transaction>.breakdownByAccountForCurrency(
    accounts: List<Account>,
    currency: String
): List<AccountBreakdown> =
    filter { it.currency == currency }.breakdownByAccount(accounts)

/**
 * Breaks down transactions by currency for a specific account.
 */
fun List<Transaction>.breakdownByCurrencyForAccount(
    accounts: List<Account>,
    accountId: String
): List<CurrencyBreakdown> =
    FilterService.filterByAccount(this, accountId).breakdownByCurrency()

/**
 * Breaks down transactions by category for a specific account.
 */
fun List<Transaction>.breakdownByCategoryForAccount(
    accountId: String,
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<CategoryBreakdown> =
    FilterService.filterByAccount(this, accountId).breakdownByCategory(exchangeRates, baseCurrency)

/**
 * Breaks down transactions by category, then by account within each category.
 */
fun List<Transaction>.breakdownByCategoryAndAccount(
    accounts: List<Account>
): Map<String, List<AccountBreakdown>> =
    groupBy { it.category }
        .mapValues { (_, txs) -> txs.breakdownByAccount(accounts) }

/**
 * Breaks down transactions by account, then by category within each account.
 */
fun List<Transaction>.breakdownByAccountAndCategory(): Map<String, List<CategoryBreakdown>> =
    groupBy { it.accountId }
        .mapValues { (_, txs) -> txs.breakdownByCategory() }

/**
 * Breaks down transactions by account for a specific category.
 */
fun List<Transaction>.breakdownByAccountForCategory(
    accounts: List<Account>,
    category: String
): List<AccountBreakdown> =
    filter { it.category == category }.breakdownByAccount(accounts)

/**
 * Breaks down transactions by account for a specific date.
 */
fun List<Transaction>.breakdownByAccountForDate(
    accounts: List<Account>,
    date: String
): List<AccountBreakdown> =
    filter {
        Instant.ofEpochMilli(it.timestamp).atZone(TimelineGroupingService.systemZoneId)
            .toLocalDate().toString() == date
    }.breakdownByAccount(accounts)

/**
 * Breaks down transactions by account for a specific month.
 */
fun List<Transaction>.breakdownByAccountForMonth(
    accounts: List<Account>,
    month: String
): List<AccountBreakdown> =
    filter { TimelineGroupingService.formatMonth(it.timestamp) == month }
        .breakdownByAccount(accounts)

/**
 * Breaks down transactions by account for a specific transaction type.
 */
fun List<Transaction>.breakdownByAccountForTransactionType(
    accounts: List<Account>,
    transactionType: String
): List<AccountBreakdown> =
    filter { it.transactionType == transactionType }.breakdownByAccount(accounts)

/**
 * Breaks down transactions by account for a specific payment method.
 */
fun List<Transaction>.breakdownByAccountForPaymentMethod(
    accounts: List<Account>,
    paymentMethod: String
): List<AccountBreakdown> =
    filter { it.paymentMethod == paymentMethod }.breakdownByAccount(accounts)

/**
 * Breaks down transactions by account for a specific status.
 */
fun List<Transaction>.breakdownByAccountForStatus(
    accounts: List<Account>,
    status: TransactionStatus
): List<AccountBreakdown> =
    filter { it.status == status }.breakdownByAccount(accounts)

/**
 * Breaks down transactions by account for a specific tag.
 */
fun List<Transaction>.breakdownByAccountForTag(
    accounts: List<Account>,
    tag: String
): List<AccountBreakdown> =
    filter { it.tags.contains(tag) }.breakdownByAccount(accounts)

/**
 * Breaks down transactions by date for a specific account.
 */
fun List<Transaction>.breakdownByDateForAccount(
    accountId: String,
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<DateBreakdown> =
    FilterService.filterByAccount(this, accountId).breakdownByDate(exchangeRates, baseCurrency)

/**
 * Breaks down transactions by month for a specific account.
 */
fun List<Transaction>.breakdownByMonthForAccount(
    accountId: String,
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<MonthBreakdown> =
    FilterService.filterByAccount(this, accountId).breakdownByMonth(exchangeRates, baseCurrency)

/**
 * Breaks down transactions by transaction type for a specific account.
 */
fun List<Transaction>.breakdownByTransactionTypeForAccount(
    accountId: String,
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<TransactionTypeBreakdown> =
    FilterService.filterByAccount(this, accountId)
        .breakdownByTransactionType(exchangeRates, baseCurrency)

/**
 * Breaks down transactions by payment method for a specific account.
 */
fun List<Transaction>.breakdownByPaymentMethodForAccount(
    accountId: String,
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<PaymentMethodBreakdown> =
    FilterService.filterByAccount(this, accountId)
        .breakdownByPaymentMethod(exchangeRates, baseCurrency)

/**
 * Breaks down transactions by status for a specific account.
 */
fun List<Transaction>.breakdownByStatusForAccount(
    accountId: String,
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<StatusBreakdown> =
    FilterService.filterByAccount(this, accountId).breakdownByStatus(exchangeRates, baseCurrency)

/**
 * Breaks down transactions by tags for a specific account.
 */
fun List<Transaction>.breakdownByTagsForAccount(
    accountId: String,
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<TagBreakdown> =
    FilterService.filterByAccount(this, accountId).breakdownByTags(exchangeRates, baseCurrency)

// ==================== Hierarchical Breakdown Methods ====================

/**
 * Breaks down transactions by account, then by currency within each account.
 */
fun List<Transaction>.breakdownByAccountAndCurrency(): Map<String, List<CurrencyBreakdown>> =
    groupBy { it.accountId }
        .mapValues { (_, txs) -> txs.breakdownByCurrency() }

/**
 * Breaks down transactions by currency, then by account within each currency.
 */
fun List<Transaction>.breakdownByCurrencyAndAccount(
    accounts: List<Account>
): Map<String, List<AccountBreakdown>> =
    groupBy { it.currency }
        .mapValues { (_, txs) -> txs.breakdownByAccount(accounts) }

/**
 * Breaks down transactions by account, then by date within each account.
 */
fun List<Transaction>.breakdownByAccountAndDate(): Map<String, List<DateBreakdown>> =
    groupBy { it.accountId }
        .mapValues { (_, txs) -> txs.breakdownByDate() }

/**
 * Breaks down transactions by account, then by month within each account.
 */
fun List<Transaction>.breakdownByAccountAndMonth(): Map<String, List<MonthBreakdown>> =
    groupBy { it.accountId }
        .mapValues { (_, txs) -> txs.breakdownByMonth() }

/**
 * Breaks down transactions by account, then by transaction type within each account.
 */
fun List<Transaction>.breakdownByAccountAndTransactionType(): Map<String, List<TransactionTypeBreakdown>> =
    groupBy { it.accountId }
        .mapValues { (_, txs) -> txs.breakdownByTransactionType() }

/**
 * Breaks down transactions by account, then by payment method within each account.
 */
fun List<Transaction>.breakdownByAccountAndPaymentMethod(): Map<String, List<PaymentMethodBreakdown>> =
    groupBy { it.accountId }
        .mapValues { (_, txs) -> txs.breakdownByPaymentMethod() }

/**
 * Breaks down transactions by account, then by status within each account.
 */
fun List<Transaction>.breakdownByAccountAndStatus(): Map<String, List<StatusBreakdown>> =
    groupBy { it.accountId }
        .mapValues { (_, txs) -> txs.breakdownByStatus() }

/**
 * Breaks down transactions by account, then by tags within each account.
 */
fun List<Transaction>.breakdownByAccountAndTags(): Map<String, List<TagBreakdown>> =
    groupBy { it.accountId }
        .mapValues { (_, txs) -> txs.breakdownByTags() }

/**
 * Breaks down transactions by date, then by account within each date.
 */
fun List<Transaction>.breakdownByDateAndAccount(
    accounts: List<Account>
): Map<String, List<AccountBreakdown>> =
    groupBy {
        Instant.ofEpochMilli(it.timestamp).atZone(TimelineGroupingService.systemZoneId)
            .toLocalDate().toString()
    }
        .mapValues { (_, txs) -> txs.breakdownByAccount(accounts) }

/**
 * Breaks down transactions by month, then by account within each month.
 */
fun List<Transaction>.breakdownByMonthAndAccount(
    accounts: List<Account>
): Map<String, List<AccountBreakdown>> =
    groupBy { TimelineGroupingService.formatMonth(it.timestamp) }
        .mapValues { (_, txs) -> txs.breakdownByAccount(accounts) }

/**
 * Breaks down transactions by transaction type, then by account within each type.
 */
fun List<Transaction>.breakdownByTransactionTypeAndAccount(
    accounts: List<Account>
): Map<String, List<AccountBreakdown>> =
    groupBy { it.transactionType }
        .mapValues { (_, txs) -> txs.breakdownByAccount(accounts) }

/**
 * Breaks down transactions by payment method, then by account within each method.
 */
fun List<Transaction>.breakdownByPaymentMethodAndAccount(
    accounts: List<Account>
): Map<String, List<AccountBreakdown>> =
    groupBy { it.paymentMethod }
        .mapValues { (_, txs) -> txs.breakdownByAccount(accounts) }

/**
 * Breaks down transactions by status, then by account within each status.
 */
fun List<Transaction>.breakdownByStatusAndAccount(
    accounts: List<Account>
): Map<String, List<AccountBreakdown>> =
    groupBy { it.status.name }
        .mapValues { (_, txs) -> txs.breakdownByAccount(accounts) }

/**
 * Breaks down transactions by tags, then by account within each tag.
 */
fun List<Transaction>.breakdownByTagsAndAccount(
    accounts: List<Account>
): Map<String, List<AccountBreakdown>> =
    flatMap { tx -> tx.tags.map { tag -> tag to tx } }
        .groupBy { it.first }
        .mapValues { (_, pairs) -> pairs.map { it.second } }
        .mapValues { (_, txs) -> txs.breakdownByAccount(accounts) }
