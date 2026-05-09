package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.service.financial.AccountBreakdown
import com.osscabinet.ossWallet.domain.service.financial.CategoryBreakdown
import com.osscabinet.ossWallet.domain.service.financial.CurrencyBreakdown
import com.osscabinet.ossWallet.domain.service.financial.DateBreakdown
import com.osscabinet.ossWallet.domain.service.financial.FinancialMetrics
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.financial.MonthBreakdown
import com.osscabinet.ossWallet.domain.service.financial.PaymentMethodBreakdown
import com.osscabinet.ossWallet.domain.service.financial.StatusBreakdown
import com.osscabinet.ossWallet.domain.service.financial.TagBreakdown
import com.osscabinet.ossWallet.domain.service.financial.TransactionTypeBreakdown
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal
import java.time.Instant

/**
 * Extension functions for breaking down transactions by various dimensions.
 * Consolidates repetitive grouping logic using AggregationService helpers.
 */

// ==================== Core Breakdown Methods ====================

/**
 * Breaks down transactions by currency.
 */
fun List<Transaction>.breakdownByCurrency(): List<CurrencyBreakdown> =
    AggregationService.calculateSummaryByCurrency(this)
        .map { (currency, summary) -> CurrencyBreakdown(currency, summary) }
        .sortedByDescending { it.totalAmount }

/**
 * Breaks down transactions by account.
 */
fun List<Transaction>.breakdownByAccount(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<AccountBreakdown> =
    AggregationService.createMetricsBreakdown(this, { it.accountId }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount }

/**
 * Breaks down transactions by category.
 */
fun List<Transaction>.breakdownByCategory(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<CategoryBreakdown> =
    AggregationService.createMetricsBreakdown(this, { it.categoryId.toString() }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by date.
 */
fun List<Transaction>.breakdownByDate(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<DateBreakdown> =
    AggregationService.createMetricsBreakdown(this, {
        Instant.ofEpochMilli(it.timestamp).atZone(TimelineService.systemZoneId)
            .toLocalDate().toString()
    }, exchangeRates, baseCurrency).sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by month.
 */
fun List<Transaction>.breakdownByMonth(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<MonthBreakdown> =
    AggregationService.createMetricsBreakdown(this, { TimelineService.formatMonth(it.timestamp) }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by transaction type.
 */
fun List<Transaction>.breakdownByTransactionType(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<TransactionTypeBreakdown> =
    AggregationService.createMetricsBreakdown(this, { it.transactionType }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by payment method.
 */
fun List<Transaction>.breakdownByPaymentMethod(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<PaymentMethodBreakdown> =
    AggregationService.createMetricsBreakdown(this, { it.paymentMethod }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by status.
 */
fun List<Transaction>.breakdownByStatus(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<StatusBreakdown> =
    AggregationService.createMetricsBreakdown(this, { it.status.name }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by tags.
 */
fun List<Transaction>.breakdownByTags(
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<TagBreakdown> {
    // Tags are multi-value, so we need a custom flatMap before grouping
    return flatMap { tx -> tx.tags.map { tag -> tag to tx } }
        .groupBy { it.first }
        .mapValues { (_, pairs) -> pairs.map { it.second } }
        .map { (tag, txs) ->
            val summaries = AggregationService.calculateSummaryByCurrency(txs)
            val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                summaries.mapValues { (currency, summary) ->
                    CurrencyService.convertFinancialSummaryToBase(summary, currency, baseCurrency, exchangeRates)
                }
            } else {
                summaries
            }
            FinancialMetrics(
                key = tag,
                financialSummary = FinancialSummary.aggregate(convertedSummaries.values),
                currencyBreakdown = summaries
            )
        }.sortedByDescending { it.totalAmount.abs() }
}

// ==================== Composite Breakdown Methods ====================

fun List<Transaction>.breakdownByAccountForCurrency(currency: String): List<AccountBreakdown> =
    filter { it.currency == currency }.breakdownByAccount()

fun List<Transaction>.breakdownByCurrencyForAccount(accountId: String): List<CurrencyBreakdown> =
    filter { it.accountId == accountId }.breakdownByCurrency()

fun List<Transaction>.breakdownByCategoryForAccount(
    accountId: String,
    exchangeRates: Map<String, BigDecimal>? = null,
    baseCurrency: String? = null
): List<CategoryBreakdown> =
    filter { it.accountId == accountId }.breakdownByCategory(exchangeRates, baseCurrency)

fun List<Transaction>.breakdownByCategoryAndAccount(accounts: List<Account>): Map<String, List<AccountBreakdown>> =
    groupBy { it.categoryId.toString() }.mapValues { (_, txs) -> txs.breakdownByAccount() }

fun List<Transaction>.breakdownByAccountAndCategory(): Map<String, List<CategoryBreakdown>> =
    groupBy { it.accountId }.mapValues { (_, txs) -> txs.breakdownByCategory() }

fun List<Transaction>.breakdownByAccountAndCurrency(): Map<String, List<CurrencyBreakdown>> =
    groupBy { it.accountId }.mapValues { (_, txs) -> txs.breakdownByCurrency() }

fun List<Transaction>.breakdownByCurrencyAndAccount(): Map<String, List<AccountBreakdown>> =
    groupBy { it.currency }.mapValues { (_, txs) -> txs.breakdownByAccount() }

fun List<Transaction>.breakdownByAccountAndDate(): Map<String, List<DateBreakdown>> =
    groupBy { it.accountId }.mapValues { (_, txs) -> txs.breakdownByDate() }

fun List<Transaction>.breakdownByAccountAndMonth(): Map<String, List<MonthBreakdown>> =
    groupBy { it.accountId }.mapValues { (_, txs) -> txs.breakdownByMonth() }

fun List<Transaction>.breakdownByAccountAndTransactionType(): Map<String, List<TransactionTypeBreakdown>> =
    groupBy { it.accountId }.mapValues { (_, txs) -> txs.breakdownByTransactionType() }

fun List<Transaction>.breakdownByAccountAndPaymentMethod(): Map<String, List<PaymentMethodBreakdown>> =
    groupBy { it.accountId }.mapValues { (_, txs) -> txs.breakdownByPaymentMethod() }

fun List<Transaction>.breakdownByAccountAndStatus(): Map<String, List<StatusBreakdown>> =
    groupBy { it.accountId }.mapValues { (_, txs) -> txs.breakdownByStatus() }

fun List<Transaction>.breakdownByAccountAndTags(): Map<String, List<TagBreakdown>> =
    groupBy { it.accountId }.mapValues { (_, txs) -> txs.breakdownByTags() }
