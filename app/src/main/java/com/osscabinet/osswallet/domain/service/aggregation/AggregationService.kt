package com.osscabinet.ossWallet.domain.service.aggregation

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import com.osscabinet.ossWallet.domain.service.financial.FinancialMetrics
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.financial.IncomeExpenseBreakdown
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
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
        exchangeRates: Map<String, BigDecimal>
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
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        val breakdown = calculateIncomeExpenseBreakdown(transactions, exchangeRates, baseCurrency)

        val cashFlowBreakdown =
            (breakdown.incomeCurrencyBreakdown.keys + breakdown.expenseCurrencyBreakdown.keys)
                .distinct().sorted().map { curr ->
                    val inc = breakdown.incomeCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                    val exp = breakdown.expenseCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                    CashFlowBreakdownItem(
                        currency = curr,
                        income = inc.totalAmount.setScale(precision, RoundingMode.HALF_UP),
                        incomeInBase = CurrencyService.convertAmount(
                            inc.totalAmount,
                            curr,
                            baseCurrency,
                            exchangeRates,
                            10
                        ).setScale(precision, RoundingMode.HALF_UP),
                        expense = exp.totalAmount.setScale(precision, RoundingMode.HALF_UP),
                        expenseInBase = CurrencyService.convertAmount(
                            exp.totalAmount,
                            curr,
                            baseCurrency,
                            exchangeRates,
                            10
                        ).setScale(precision, RoundingMode.HALF_UP)
                    )
                }

        return CashFlowResult(
            totalIncomeInBase = breakdown.totalIncome.setScale(precision, RoundingMode.HALF_UP),
            totalExpensesInBase = breakdown.totalExpense.setScale(precision, RoundingMode.HALF_UP),
            netInBase = breakdown.netFlow.setScale(precision, RoundingMode.HALF_UP),
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
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = 2
    ): List<CurrencyAggregationItem> {
        val summaries = calculateSummaryByCurrency(transactions)
        return summaries.map { (curr, summary) ->
            CurrencyAggregationItem(
                currency = curr,
                amount = summary.totalAmount.setScale(precision, RoundingMode.HALF_UP),
                amountInBase = CurrencyService.convertAmount(
                    summary.totalAmount,
                    curr,
                    baseCurrency,
                    exchangeRates,
                    10
                ).setScale(precision, RoundingMode.HALF_UP)
            )
        }.sortedByDescending { it.amountInBase.abs() }
    }

    /**
     * Calculates summary metrics for a list of transactions.
     */
    fun calculateTransactionSummary(transactions: List<Transaction>): FinancialSummary {
        if (transactions.isEmpty()) return EMPTY_SUMMARY
        
        val total = transactions.sumOf { it.amount }
        val (incomeTransactions, expenseTransactions) = transactions.partition { it.amount.signum() >= 0 }
        val income = incomeTransactions.sumOf { it.amount }
        val expense = expenseTransactions.sumOf { it.amount }

        return FinancialSummary(total, income, expense, transactions.size)
    }

    /**
     * Calculates transaction summary grouped by currency.
     */
    fun calculateSummaryByCurrency(transactions: List<Transaction>): Map<String, FinancialSummary> {
        return transactions.groupBy { it.currency }
            .mapValues { (_, txs) ->
                val total = txs.sumOf { it.amount }
                val (incomeTransactions, expenseTransactions) = txs.partition { it.amount.signum() >= 0 }
                val income = incomeTransactions.sumOf { it.amount }
                val expense = expenseTransactions.sumOf { it.amount }
                FinancialSummary(total, income, expense, txs.size)
            }
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
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): IncomeExpenseBreakdown {
        val (income, expenses) = transactions.partition { it.amount.signum() >= 0 }
        val incomeCurrencyBreakdown = calculateSummaryByCurrency(income)
        val expenseCurrencyBreakdown = calculateSummaryByCurrency(expenses)

        val (totalIncome, totalExpense, netFlow) = if (exchangeRates != null && baseCurrency != null) {
            val totalInc = incomeCurrencyBreakdown.map { (curr, summary) ->
                CurrencyService.convertToBaseCurrency(summary.totalAmount, curr, baseCurrency, exchangeRates)
            }.fold(BigDecimal.ZERO, BigDecimal::add)
            
            val totalExp = expenseCurrencyBreakdown.map { (curr, summary) ->
                CurrencyService.convertToBaseCurrency(summary.totalAmount, curr, baseCurrency, exchangeRates)
            }.fold(BigDecimal.ZERO, BigDecimal::add)

            Triple(totalInc, totalExp, totalInc + totalExp)
        } else {
            val totalInc = income.sumOf { it.amount }
            val totalExp = expenses.sumOf { it.amount }
            Triple(totalInc, totalExp, totalInc + totalExp)
        }

        return IncomeExpenseBreakdown(
            financialSummary = FinancialSummary(netFlow, totalIncome, totalExpense, transactions.size),
            incomeTransactions = income,
            expenseTransactions = expenses,
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
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<R> where K : Any {
        return transactions.groupBy(selector)
            .map { (key, txs) ->
                val summaries = calculateSummaryByCurrency(txs)
                val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                    summaries.mapValues { (currency, summary) ->
                        CurrencyService.convertFinancialSummaryToBase(summary, currency, baseCurrency, exchangeRates)
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
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<FinancialMetrics<K>> where K : Any {
        return createBreakdown(
            transactions = transactions,
            selector = selector,
            mapper = { key, summaries ->
                FinancialMetrics(
                    key = key,
                    financialSummary = FinancialSummary.aggregate(summaries.values),
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
        allCategories: List<com.osscabinet.ossWallet.domain.model.Category>,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null,
        precision: Int = 2
    ): List<CategoryTreeMetrics> {
        val categoryMap = allCategories.associateBy { it.id }

        val categoryBreakdowns = createBreakdown(
            transactions = transactions,
            selector = { it.categoryId ?: 0L },
            mapper = { categoryId, summaries ->
                FinancialMetrics(
                    key = categoryId,
                    financialSummary = FinancialSummary(
                        totalAmount = summaries.values.sumOf { it.totalAmount },
                        income = summaries.values.sumOf { it.income },
                        expense = summaries.values.sumOf { it.expense },
                        transactionCount = summaries.values.sumOf { it.transactionCount }
                    ),
                    currencyBreakdown = summaries
                )
            },
            exchangeRates = exchangeRates,
            baseCurrency = baseCurrency
        ).associateBy { it.key }

        val globalTotal = categoryBreakdowns.values.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.totalAmount.abs()) }

        fun buildSubTree(categoryId: Long): CategoryTreeMetrics? {
            val entity = categoryMap[categoryId] ?: return null
            val breakdown = categoryBreakdowns[categoryId] ?: FinancialMetrics(categoryId, FinancialSummary.EMPTY, emptyMap())
            val childrenEntities = allCategories.filter { it.parentId == entity.id }
            val actualChildrenData = childrenEntities.mapNotNull { buildSubTree(it.id) }
            val combinedChildren = mutableListOf<CategoryTreeMetrics>()

            if (actualChildrenData.isNotEmpty()) {
                if (breakdown.totalAmount.signum() != 0) {
                    combinedChildren.add(CategoryTreeMetrics(
                        category = entity.name,
                        categoryId = entity.id,
                        netBalance = breakdown.totalAmount.setScale(precision, RoundingMode.HALF_UP),
                        income = breakdown.income.setScale(precision, RoundingMode.HALF_UP),
                        expense = breakdown.expense.setScale(precision, RoundingMode.HALF_UP),
                        colorHex = entity.colorHex,
                        percentage = if (globalTotal.signum() != 0) (breakdown.totalAmount.abs().divide(globalTotal, 4, RoundingMode.HALF_UP)).toFloat() else 0f,
                        hasSubcategories = false,
                        subcategoryData = emptyList()
                    ))
                }
                combinedChildren.addAll(actualChildrenData)
            }

            combinedChildren.sortByDescending { it.netBalance.abs() }

            val totalIncome = if (actualChildrenData.isEmpty()) breakdown.income else combinedChildren.sumOf { it.income }
            val totalExpense = if (actualChildrenData.isEmpty()) breakdown.expense else combinedChildren.sumOf { it.expense }
            val totalNet = totalIncome + totalExpense

            return CategoryTreeMetrics(
                category = entity.name,
                categoryId = entity.id,
                netBalance = totalNet.setScale(precision, RoundingMode.HALF_UP),
                income = totalIncome.setScale(precision, RoundingMode.HALF_UP),
                expense = totalExpense.setScale(precision, RoundingMode.HALF_UP),
                colorHex = entity.colorHex,
                percentage = if (globalTotal.signum() != 0) (totalNet.abs().divide(globalTotal, 4, RoundingMode.HALF_UP)).toFloat() else 0f,
                hasSubcategories = combinedChildren.isNotEmpty(),
                subcategoryData = combinedChildren
            )
        }

        return allCategories.filter { it.parentId == null }
            .mapNotNull { buildSubTree(it.id) }
            .filter { it.netBalance.signum() != 0 || it.subcategoryData.isNotEmpty() }
            .sortedByDescending { it.netBalance.abs() }
    }
}
