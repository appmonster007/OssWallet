package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import com.osscabinet.ossWallet.domain.service.financial.FinancialMetrics
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.financial.IncomeExpenseBreakdown
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Core service for aggregating and calculating transaction data.
 * Focused on base calculations and summary generation.
 */
object AggregationService {

    val EMPTY_SUMMARY = FinancialSummary.EMPTY

    /**
     * Calculates the total balance for an account in its target currency.
     */
    fun calculateAccountBalance(
        accountId: String,
        allTransactions: List<Transaction>,
        accountCurrency: String,
        exchangeRates: ExchangeRateSnapshot
    ): BigDecimal {
        val accountTransactions = allTransactions.filter { it.accountId == accountId }
        val totalsByCurrency = calculateTotalByCurrency(accountTransactions)
        return CurrencyService.calculateTotalInCurrency(
            totalsByCurrency,
            accountCurrency,
            exchangeRates
        )
    }

    /**
     * Calculates a comprehensive CashFlowResult for a set of transactions.
     */
    fun calculateCashFlowResult(
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        precision: Int = CurrencyService.precision
    ): CashFlowResult {
        if (transactions.isEmpty()) return CashFlowResult.empty()

        val summariesByCurrency = calculateSummaryByCurrency(transactions)
        
        val allCurrencies = summariesByCurrency.keys.distinct().sorted()

        val cashFlowBreakdown = allCurrencies.map { curr ->
            val summary = summariesByCurrency[curr] ?: FinancialSummary.EMPTY
            val metricsInBase = CurrencyService.convertFinancialSummaryToBase(
                summary, curr, baseCurrency, exchangeRates
            ).setScale(precision)

            CashFlowBreakdownItem(
                currency = curr,
                metrics = summary.setScale(precision),
                metricsInBase = metricsInBase
            )
        }

        val totalMetricsInBase = cashFlowBreakdown.fold(FinancialSummary.EMPTY) { acc, item ->
            acc + item.metricsInBase
        }

        return CashFlowResult(
            metricsInBase = totalMetricsInBase,
            breakdown = cashFlowBreakdown,
            filteredTransactions = transactions
        )
    }

    /**
     * Calculates currency balances with base currency conversion.
     */
    fun calculateCurrencyBalances(
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        precision: Int = CurrencyService.precision
    ): List<CurrencyAggregationItem> {
        val summaries = calculateSummaryByCurrency(transactions)
        return summaries.map { (curr, summary) ->
            CurrencyAggregationItem(
                currency = curr,
                amount = summary.netFlow.setScale(precision, RoundingMode.HALF_UP),
                amountInBase = CurrencyService.convertAmount(
                    summary.netFlow,
                    curr,
                    baseCurrency,
                    exchangeRates,
                    10
                ).setScale(precision, RoundingMode.HALF_UP)
            )
        }.sortedByDescending { it.amountInBase.abs() }
    }

    /**
     * Calculates summary metrics for a list of transactions using a single-pass loop.
     */
    fun calculateTransactionSummary(transactions: List<Transaction>): FinancialSummary {
        if (transactions.isEmpty()) return EMPTY_SUMMARY

        var income = BigDecimal.ZERO
        var expense = BigDecimal.ZERO
        var transferIn = BigDecimal.ZERO
        var transferOut = BigDecimal.ZERO

        for (tx in transactions) {
            when (tx.transactionType) {
                TransactionType.INCOME -> income = income.add(tx.amount)
                TransactionType.EXPENSE -> expense = expense.add(tx.amount)
                TransactionType.TRANSFER_INFLOW -> transferIn = transferIn.add(tx.amount)
                TransactionType.TRANSFER_OUTFLOW -> transferOut = transferOut.add(tx.amount)
            }
        }

        return FinancialSummary(
            income = income,
            expense = expense,
            transferIn = transferIn,
            transferOut = transferOut,
            transactionCount = transactions.size
        )
    }

    /**
     * Calculates transaction summary grouped by currency using a single-pass loop.
     */
    fun calculateSummaryByCurrency(transactions: List<Transaction>): Map<String, FinancialSummary> {
        val result = mutableMapOf<String, FinancialSummary>()
        
        for (tx in transactions) {
            val curr = tx.currency
            val current = result[curr] ?: FinancialSummary.EMPTY
            
            val updated = when (tx.transactionType) {
                TransactionType.INCOME -> current.copy(income = current.income.add(tx.amount), transactionCount = current.transactionCount + 1)
                TransactionType.EXPENSE -> current.copy(expense = current.expense.add(tx.amount), transactionCount = current.transactionCount + 1)
                TransactionType.TRANSFER_INFLOW -> current.copy(transferIn = current.transferIn.add(tx.amount), transactionCount = current.transactionCount + 1)
                TransactionType.TRANSFER_OUTFLOW -> current.copy(transferOut = current.transferOut.add(tx.amount), transactionCount = current.transactionCount + 1)
            }
            result[curr] = updated
        }
        
        return result
    }

    /**
     * Calculates total transaction amounts by currency.
     */
    fun calculateTotalByCurrency(transactions: List<Transaction>): Map<String, BigDecimal> =
        transactions.groupBy { it.currency }.mapValues { (_, txs) -> txs.sumOf { it.amount } }

    /**
     * Calculates income and expense breakdown with currency sub-grouping.
     */
    fun calculateIncomeExpenseBreakdown(
        transactions: List<Transaction>,
        exchangeRates: ExchangeRateSnapshot? = null,
        baseCurrency: String? = null
    ): IncomeExpenseBreakdown {
        if (transactions.isEmpty()) return IncomeExpenseBreakdown.empty()

        val summariesByCurrency = calculateSummaryByCurrency(transactions)
        
        val incomeCurrencyBreakdown = summariesByCurrency.mapValues { (_, s) -> 
            s.copy(expense = BigDecimal.ZERO, transferIn = BigDecimal.ZERO, transferOut = BigDecimal.ZERO)
        }
        val expenseCurrencyBreakdown = summariesByCurrency.mapValues { (_, s) -> 
            s.copy(income = BigDecimal.ZERO, transferIn = BigDecimal.ZERO, transferOut = BigDecimal.ZERO)
        }

        val totalMetrics = if (exchangeRates != null && baseCurrency != null) {
            CurrencyService.calculateTotalInBaseCurrency(summariesByCurrency, baseCurrency, exchangeRates)
        } else {
            summariesByCurrency.values.fold(FinancialSummary.EMPTY) { acc, s -> acc + s }
        }

        return IncomeExpenseBreakdown(
            metrics = totalMetrics,
            incomeTransactions = transactions.filter { it.transactionType == TransactionType.INCOME },
            expenseTransactions = transactions.filter { it.transactionType == TransactionType.EXPENSE },
            incomeCurrencyBreakdown = incomeCurrencyBreakdown,
            expenseCurrencyBreakdown = expenseCurrencyBreakdown
        )
    }

    /**
     * Generic helper for creating breakdowns.
     */
    fun <K, R> createBreakdown(
        transactions: List<Transaction>,
        selector: (Transaction) -> K,
        mapper: (K, Map<String, FinancialSummary>) -> R,
        exchangeRates: ExchangeRateSnapshot? = null,
        baseCurrency: String? = null
    ): List<R> where K : Any {
        val groupedSummaries = mutableMapOf<K, MutableMap<String, FinancialSummary>>()
        for (tx in transactions) {
            val key = selector(tx)
            val currencyMap = groupedSummaries.getOrPut(key) { mutableMapOf() }
            val s = currencyMap[tx.currency] ?: FinancialSummary.EMPTY
            currencyMap[tx.currency] = when (tx.transactionType) {
                TransactionType.INCOME -> s.copy(income = s.income.add(tx.amount), transactionCount = s.transactionCount + 1)
                TransactionType.EXPENSE -> s.copy(expense = s.expense.add(tx.amount), transactionCount = s.transactionCount + 1)
                TransactionType.TRANSFER_INFLOW -> s.copy(transferIn = s.transferIn.add(tx.amount), transactionCount = s.transactionCount + 1)
                TransactionType.TRANSFER_OUTFLOW -> s.copy(transferOut = s.transferOut.add(tx.amount), transactionCount = s.transactionCount + 1)
            }
        }

        return groupedSummaries.map { (key, summaries) ->
            val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                summaries.mapValues { (currency, summary) ->
                    CurrencyService.convertFinancialSummaryToBase(
                        summary,
                        currency,
                        baseCurrency,
                        exchangeRates
                    )
                }
            } else {
                summaries
            }
            mapper(key, convertedSummaries)
        }
    }

    /**
     * Creates a list of FinancialMetrics by grouping transactions by a selector.
     * Centralizes the common breakdown pattern.
     */
    fun <K> createMetricsBreakdown(
        transactions: List<Transaction>,
        selector: (Transaction) -> K,
        exchangeRates: ExchangeRateSnapshot? = null,
        baseCurrency: String? = null
    ): List<FinancialMetrics<K>> where K : Any {
        return createBreakdown(
            transactions = transactions,
            selector = selector,
            mapper = { key, summaries ->
                FinancialMetrics(
                    key = key,
                    metrics = FinancialSummary.aggregate(summaries.values),
                    currencyBreakdown = summaries
                )
            },
            exchangeRates = exchangeRates,
            baseCurrency = baseCurrency
        )
    }

    /**
     * Builds a hierarchical category tree with financial metrics.
     */
    fun buildCategoryTree(
        transactions: List<Transaction>,
        allCategories: List<Category>,
        exchangeRates: ExchangeRateSnapshot? = null,
        baseCurrency: String? = null,
        precision: Int = CurrencyService.precision,
        previousTransactions: List<Transaction>? = null
    ): List<CategoryTreeMetrics> {
        val categoryBreakdowns = createBreakdown(
            transactions = transactions,
            selector = { it.categoryId },
            mapper = { categoryId, summaries ->
                FinancialMetrics(
                    key = categoryId,
                    metrics = FinancialSummary.aggregate(summaries.values),
                    currencyBreakdown = summaries
                )
            },
            exchangeRates = exchangeRates,
            baseCurrency = baseCurrency
        ).associateBy { it.key }

        val previousBreakdowns = previousTransactions?.let {
            createBreakdown(
                transactions = it,
                selector = { it.categoryId },
                mapper = { categoryId, summaries ->
                    FinancialMetrics(
                        key = categoryId,
                        metrics = FinancialSummary.aggregate(summaries.values),
                        currencyBreakdown = summaries
                    )
                },
                exchangeRates = exchangeRates,
                baseCurrency = baseCurrency
            ).associateBy { it.key }
        }

        return buildCategoryTreeFromBreakdowns(
            categoryBreakdowns,
            previousBreakdowns,
            allCategories,
            precision
        )
    }

    fun buildCategoryTreeFromBreakdowns(
        categoryBreakdowns: Map<Long, FinancialMetrics<Long>>,
        previousBreakdowns: Map<Long, FinancialMetrics<Long>>?,
        allCategories: List<Category>,
        precision: Int = CurrencyService.precision
    ): List<CategoryTreeMetrics> {
        val categoryMap = allCategories.associateBy { it.id }
        val globalTotal =
            categoryBreakdowns.values.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.totalAmount.abs()) }

        fun buildSubTree(categoryId: Long): CategoryTreeMetrics? {
            val entity = categoryMap[categoryId] ?: return null
            val breakdown = categoryBreakdowns[categoryId] ?: FinancialMetrics(
                categoryId,
                FinancialSummary.EMPTY,
                emptyMap()
            )
            val previousBreakdown = previousBreakdowns?.get(categoryId)

            val childrenEntities = allCategories.filter { it.parentId == entity.id }
            val actualChildrenData = childrenEntities.mapNotNull { buildSubTree(it.id) }
            val combinedChildren = mutableListOf<CategoryTreeMetrics>()

            if (actualChildrenData.isNotEmpty()) {
                if (breakdown.totalAmount.signum() != 0 || (previousBreakdown != null && previousBreakdown.totalAmount.signum() != 0)) {
                    combinedChildren.add(
                        CategoryTreeMetrics(
                            category = entity.name,
                            categoryId = entity.id,
                            metrics = breakdown.metrics.setScale(precision),
                            colorHex = entity.colorHex,
                            percentage = if (globalTotal.signum() != 0) (breakdown.totalAmount.abs()
                                .divide(globalTotal, 4, RoundingMode.HALF_UP)).toFloat() else 0f,
                            hasSubcategories = false,
                            subcategoryData = emptyList(),
                            previousMetrics = previousBreakdown?.metrics?.setScale(precision)
                        )
                    )
                }
                combinedChildren.addAll(actualChildrenData)
            }

            combinedChildren.sortByDescending { it.netBalance.abs() }

            val totalMetrics = if (actualChildrenData.isEmpty()) {
                breakdown.metrics
            } else {
                combinedChildren.fold(FinancialSummary.EMPTY) { acc, child -> acc + child.metrics }
            }

            val totalPrevMetrics = if (previousBreakdowns != null) {
                if (actualChildrenData.isEmpty()) {
                    previousBreakdown?.metrics ?: FinancialSummary.EMPTY
                } else {
                    combinedChildren.fold(FinancialSummary.EMPTY) { acc, child -> acc + (child.previousMetrics ?: FinancialSummary.EMPTY) }
                }
            } else null

            return CategoryTreeMetrics(
                category = entity.name,
                categoryId = entity.id,
                metrics = totalMetrics.setScale(precision),
                colorHex = entity.colorHex,
                percentage = if (globalTotal.signum() != 0) (totalMetrics.netFlow.abs()
                    .divide(globalTotal, 4, RoundingMode.HALF_UP)).toFloat() else 0f,
                hasSubcategories = combinedChildren.isNotEmpty(),
                subcategoryData = combinedChildren,
                previousMetrics = totalPrevMetrics?.setScale(precision)
            )
        }

        return allCategories.filter { it.parentId == null }
            .mapNotNull { buildSubTree(it.id) }
            .filter { it.netBalance.signum() != 0 || it.subcategoryData.isNotEmpty() || (it.previousNetBalance?.signum() ?: 0 != 0) }
            .sortedByDescending { it.netBalance.abs() }
    }
}
