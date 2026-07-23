package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
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
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
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
    exchangeRates: ExchangeRateSnapshot? = null,
    baseCurrency: String? = null
): List<AccountBreakdown> =
    AggregationService.createMetricsBreakdown(filter { it.accountId != null }, { it.accountId!! }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount }

/**
 * Breaks down transactions by category.
 */
fun List<Transaction>.breakdownByCategory(
    exchangeRates: ExchangeRateSnapshot? = null,
    baseCurrency: String? = null
): List<CategoryBreakdown> =
    AggregationService.createMetricsBreakdown(this, { it.categoryId.toString() }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by date.
 */
fun List<Transaction>.breakdownByDate(
    exchangeRates: ExchangeRateSnapshot? = null,
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
    exchangeRates: ExchangeRateSnapshot? = null,
    baseCurrency: String? = null
): List<MonthBreakdown> =
    AggregationService.createMetricsBreakdown<String>(this, { TimelineService.formatMonth(it.timestamp) }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by transaction type.
 */
fun List<Transaction>.breakdownByTransactionType(
    exchangeRates: ExchangeRateSnapshot? = null,
    baseCurrency: String? = null
): List<TransactionTypeBreakdown> =
    AggregationService.createMetricsBreakdown(this, { it.transactionType }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by payment method.
 */
fun List<Transaction>.breakdownByPaymentMethod(
    exchangeRates: ExchangeRateSnapshot? = null,
    baseCurrency: String? = null
): List<PaymentMethodBreakdown> =
    AggregationService.createMetricsBreakdown(this, { it.paymentMethod }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by status.
 */
fun List<Transaction>.breakdownByStatus(
    exchangeRates: ExchangeRateSnapshot? = null,
    baseCurrency: String? = null
): List<StatusBreakdown> =
    AggregationService.createMetricsBreakdown(this, { it.status.name }, exchangeRates, baseCurrency)
        .sortedByDescending { it.totalAmount.abs() }

/**
 * Breaks down transactions by tags using a single-pass optimized loop.
 */
fun List<Transaction>.breakdownByTags(
    exchangeRates: ExchangeRateSnapshot? = null,
    baseCurrency: String? = null
): List<TagBreakdown> {
    val tagMap = mutableMapOf<String, MutableMap<String, FinancialSummary>>()
    for (tx in this) {
        for (tag in tx.tags) {
            val currencyMap = tagMap.getOrPut(tag) { mutableMapOf() }
            val s = currencyMap[tx.currency] ?: FinancialSummary.EMPTY
            currencyMap[tx.currency] = when (tx.transactionType) {
                TransactionType.INCOME -> s.copy(income = s.income.add(tx.amount), transactionCount = s.transactionCount + 1)
                TransactionType.EXPENSE -> s.copy(expense = s.expense.add(tx.amount), transactionCount = s.transactionCount + 1)
                TransactionType.TRANSFER_INFLOW -> s.copy(transferIn = s.transferIn.add(tx.amount), transactionCount = s.transactionCount + 1)
                TransactionType.TRANSFER_OUTFLOW -> s.copy(transferOut = s.transferOut.add(tx.amount), transactionCount = s.transactionCount + 1)
            }
        }
    }

    return tagMap.map { (tag, summaries) ->
        val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
            summaries.mapValues { (currency, summary) ->
                CurrencyService.convertFinancialSummaryToBase(summary, currency, baseCurrency, exchangeRates)
            }
        } else {
            summaries
        }
        FinancialMetrics(
            key = tag,
            metrics = FinancialSummary.aggregate(convertedSummaries.values),
            currencyBreakdown = summaries
        )
    }.sortedByDescending { it.totalAmount.abs() }
}
