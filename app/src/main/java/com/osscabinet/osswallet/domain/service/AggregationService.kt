package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.service.CurrencyConversionService.calculateTotalInBaseCurrency
import com.osscabinet.ossWallet.domain.service.CurrencyConversionService.convertFinancialSummary
import com.osscabinet.ossWallet.domain.service.TransactionFilterService.filterByAccount
import com.osscabinet.ossWallet.domain.service.TransactionFilterService.filterExpense
import com.osscabinet.ossWallet.domain.service.TransactionFilterService.filterIncome
import com.osscabinet.ossWallet.domain.service.TimelineGroupingService.formatDate
import com.osscabinet.ossWallet.domain.service.TimelineGroupingService.formatMonth
import com.osscabinet.ossWallet.domain.service.TimelineGroupingService.systemZoneId
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * Service for aggregating, calculating, and breaking down transaction data.
 *
 * This service is stateless and pure Kotlin (no Android dependencies).
 * All methods are independently testable.
 */
object AggregationService {

    // ==================== Empty Summary Constant ====================

    /**
     * Empty financial summary for use when no transactions are available.
     */
    val EMPTY_SUMMARY = FinancialSummary.EMPTY

    // ==================== Calculation Methods ====================

    /**
     * Calculates summary metrics for a list of transactions.
     * All transactions must have the same currency, otherwise throws IllegalArgumentException.
     *
     * @param transactions List of transactions to calculate summary for
     * @return FinancialSummary with total, income, expense, and count
     */
    fun calculateTransactionSummary(transactions: List<Transaction>): FinancialSummary {
        if (transactions.isEmpty()) {
            return EMPTY_SUMMARY
        }

        val currencies = transactions.map { it.currency }.distinct()
        if (currencies.size > 1) {
            throw IllegalArgumentException(
                "calculateTransactionSummary can only process transactions with the same currency. " +
                "Found currencies: ${currencies.joinToString()}"
            )
        }

        val total = transactions.sumOf { it.amount }
        val (incomeTransactions, expenseTransactions) = transactions.partition { it.amount.signum() >= 0 }
        val income = incomeTransactions.sumOf { it.amount }
        val expense = expenseTransactions.sumOf { it.amount }

        return FinancialSummary(total, income, expense, transactions.size)
    }

    /**
     * Calculates transaction summary grouped by currency.
     * Groups transactions by currency and calculates summary for each currency group.
     *
     * @param transactions List of transactions to calculate summary for
     * @return Map where key is currency code and value is FinancialSummary for that currency
     */
    fun calculateSummaryByCurrency(
        transactions: List<Transaction>
    ): Map<String, FinancialSummary> {
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
     * Returns a map with currency codes as keys and total amounts as values.
     *
     * @param transactions List of transactions to calculate totals for
     * @return Map where key is currency code and value is total amount for that currency
     */
    fun calculateTotalByCurrency(transactions: List<Transaction>): Map<String, BigDecimal> =
        transactions.groupBy { it.currency }.mapValues { (_, txs) -> txs.sumOf { it.amount } }

    /**
     * Separates transactions into income (positive amounts) and expense (negative amounts).
     * Uses partition for single-pass efficiency.
     *
     * @param transactions List of transactions to separate
     * @return Pair where first element is income transactions and second is expense transactions
     */
    fun separateIncomeExpense(transactions: List<Transaction>): Pair<List<Transaction>, List<Transaction>> =
        transactions.partition { it.amount.signum() >= 0 }

    /**
     * Calculates comprehensive income and expense breakdown with currency sub-grouping.
     *
     * @param transactions List of transactions to calculate breakdown for
     * @param exchangeRates Optional map of currency codes to exchange rates
     * @param baseCurrency Optional base currency code for conversion
     * @return IncomeExpenseBreakdown with totals, transactions, and currency breakdowns
     */
    fun calculateIncomeExpenseBreakdown(
        transactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): IncomeExpenseBreakdown {
        if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
            throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
        }

        val (income, expenses) = separateIncomeExpense(transactions)

        val incomeCurrencyBreakdown = calculateSummaryByCurrency(income)
        val expenseCurrencyBreakdown = calculateSummaryByCurrency(expenses)

        val (totalIncome, totalExpense, netFlow) = if (exchangeRates != null && baseCurrency != null) {
            val convertedIncome = incomeCurrencyBreakdown.map { (currency, summary) ->
                convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
            }
            val convertedExpense = expenseCurrencyBreakdown.map { (currency, summary) ->
                convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
            }

            val totalInc = convertedIncome.sumOf { it.income }
            val totalExp = convertedExpense.sumOf { it.expense }
            val net = totalInc + totalExp

            Triple(totalInc, totalExp, net)
        } else {
            val totalInc = income.sumOf { it.amount }
            val totalExp = expenses.sumOf { it.amount }
            val net = transactions.sumOf { it.amount }

            Triple(totalInc, totalExp, net)
        }

        val summary = FinancialSummary(
            totalAmount = netFlow,
            income = totalIncome,
            expense = totalExpense,
            transactionCount = income.size + expenses.size
        )

        return IncomeExpenseBreakdown(
            financialSummary = summary,
            incomeTransactions = income,
            expenseTransactions = expenses,
            incomeCurrencyBreakdown = incomeCurrencyBreakdown,
            expenseCurrencyBreakdown = expenseCurrencyBreakdown
        )
    }

    /**
     * Calculates the delta (absolute and percentage change) between two financial summaries.
     *
     * @param current Current period summary
     * @param previous Previous period summary
     * @return FinancialDelta containing changes
     */
    fun calculateSummaryDelta(
        current: FinancialSummary,
        previous: FinancialSummary
    ): FinancialDelta {
        val amountDelta = current.totalAmount - previous.totalAmount
        val percentageDelta = if (previous.totalAmount.signum() != 0) {
            amountDelta.divide(previous.totalAmount.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
        } else if (current.totalAmount.signum() != 0) {
            100.0
        } else 0.0

        return FinancialDelta(
            absoluteChange = amountDelta,
            percentageChange = percentageDelta,
            incomeChange = current.income - previous.income,
            expenseChange = current.expense - previous.expense
        )
    }

    /**
     * Calculates the savings rate based on income and expense.
     * Savings Rate = (Income + Expense) / Income
     *
     * @param income Total income (positive)
     * @param expense Total expense (negative)
     * @return Savings rate as a fraction (0.0 to 1.0)
     */
    fun calculateSavingsRate(income: BigDecimal, expense: BigDecimal): Double {
        if (income.signum() <= 0) return 0.0
        val net = income + expense
        return if (net.signum() <= 0) 0.0 else net.divide(income, 4, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Calculates the burn rate (monthly average expense).
     *
     * @param totalExpense Total absolute expense
     * @param daysActive Number of days the account has been active
     * @return Monthly burn rate
     */
    fun calculateBurnRate(totalExpense: BigDecimal, daysActive: Long): BigDecimal {
        if (daysActive <= 0) return BigDecimal.ZERO
        val dailyAvg = totalExpense.abs().divide(BigDecimal.valueOf(daysActive), 10, RoundingMode.HALF_UP)
        return dailyAvg.multiply(BigDecimal.valueOf(30)).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates the standard deviation (volatility) of a list of numeric values.
     *
     * @param values List of numeric values (e.g., daily cash flows)
     * @return Standard deviation as BigDecimal
     */
    fun calculateVolatility(values: List<Double>): BigDecimal {
        if (values.isEmpty()) return BigDecimal.ZERO

        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance).toBigDecimal()
    }

    /**
     * Calculates daily net flow for a given number of days.
     *
     * @param transactions List of transactions
     * @param days Number of days to look back
     * @param exchangeRates Optional map of currency codes to exchange rates
     * @param baseCurrency Optional base currency code
     * @return Map where key is start of day timestamp and value is net flow in base currency
     */
    fun calculateDailyNetFlow(
        transactions: List<Transaction>,
        days: Int,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): Map<Long, BigDecimal> {
        val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        val filtered = transactions.filter { it.timestamp >= cutoff }

        return createBreakdown(
            transactions = filtered,
            selector = { tx ->
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            },
            mapper = { timestamp, summaries ->
                val baseSummary = if (exchangeRates != null && baseCurrency != null) {
                    calculateTotalInBaseCurrency(summaries, baseCurrency, exchangeRates)
                } else {
                    FinancialSummary(
                        totalAmount = summaries.values.sumOf { it.totalAmount },
                        income = summaries.values.sumOf { it.income },
                        expense = summaries.values.sumOf { it.expense },
                        transactionCount = summaries.values.sumOf { it.transactionCount }
                    )
                }
                timestamp to baseSummary.totalAmount
            },
            exchangeRates = exchangeRates,
            baseCurrency = baseCurrency
        ).toMap()
    }

    // ==================== Generic Breakdown Helper ====================

    /**
     * Generic helper that reduces boilerplate for breakdown methods.
     *
     * @param transactions List of transactions to break down
     * @param selector Function to extract grouping key from transaction
     * @param mapper Function to create result from key and currency summaries
     * @param exchangeRates Optional map of currency codes to exchange rates
     * @param baseCurrency Optional base currency code for conversion
     * @return List of breakdown results
     */
    fun <K, R> createBreakdown(
        transactions: List<Transaction>,
        selector: (Transaction) -> K,
        mapper: (K, Map<String, FinancialSummary>) -> R,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<R> where K : Any {
        if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
            throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
        }

        return transactions.groupBy(selector)
            .map { (key, txs) ->
                val summaries = calculateSummaryByCurrency(txs)
                val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                    summaries.mapValues { (currency, summary) ->
                        convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
                    }
                } else {
                    summaries
                }
                mapper(key, convertedSummaries)
            }
    }

    // ==================== Breakdown Methods ====================

    /**
     * Breaks down transactions by currency.
     */
    fun breakdownByCurrency(transactions: List<Transaction>): List<CurrencyBreakdown> =
        transactions.groupBy { it.currency }
            .map { (currency, txs) ->
                val incomeTx = filterIncome(txs)
                val expenseTx = filterExpense(txs)
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
    fun breakdownByAccount(
        transactions: List<Transaction>,
        accounts: List<Account>,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<AccountBreakdown> {
        return createBreakdown(transactions, { it.accountId }, { accountId, summaries ->
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
    }

    /**
     * Breaks down transactions by account for a specific currency.
     */
    fun breakdownByAccountForCurrency(
        transactions: List<Transaction>,
        accounts: List<Account>,
        currency: String
    ): List<AccountBreakdown> {
        val filteredTransactions = transactions.filter { it.currency == currency }
        return breakdownByAccount(filteredTransactions, accounts)
    }

    /**
     * Breaks down transactions by currency for a specific account.
     */
    fun breakdownByCurrencyForAccount(
        transactions: List<Transaction>,
        accounts: List<Account>,
        accountId: String
    ): List<CurrencyBreakdown> {
        val filteredTransactions = filterByAccount(transactions, accountId)
        return breakdownByCurrency(filteredTransactions)
    }

    /**
     * Breaks down transactions by category for a specific account.
     */
    fun breakdownByCategoryForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<CategoryBreakdown> {
        val filteredTransactions = filterByAccount(transactions, accountId)
        return breakdownByCategory(filteredTransactions)
    }

    /**
     * Breaks down transactions by category, then by account within each category.
     */
    fun breakdownByCategoryAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        return transactions.groupBy { it.category }
            .mapValues { (_, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by account, then by category within each account.
     */
    fun breakdownByAccountAndCategory(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<CategoryBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (_, txs) ->
                breakdownByCategory(txs)
            }
    }

    /**
     * Breaks down transactions by account for a specific category.
     */
    fun breakdownByAccountForCategory(
        transactions: List<Transaction>,
        accounts: List<Account>,
        category: String
    ): List<AccountBreakdown> {
        val filteredTransactions = transactions.filter { it.category == category }
        return breakdownByAccount(filteredTransactions, accounts)
    }

    /**
     * Breaks down transactions by account for a specific date.
     */
    fun breakdownByAccountForDate(
        transactions: List<Transaction>,
        accounts: List<Account>,
        date: String
    ): List<AccountBreakdown> {
        val filteredTransactions = transactions.filter {
            Instant.ofEpochMilli(it.timestamp).atZone(systemZoneId).toLocalDate().toString() == date
        }
        return breakdownByAccount(filteredTransactions, accounts)
    }

    /**
     * Breaks down transactions by account for a specific month.
     */
    fun breakdownByAccountForMonth(
        transactions: List<Transaction>,
        accounts: List<Account>,
        month: String
    ): List<AccountBreakdown> {
        val filteredTransactions = transactions.filter {
            formatMonth(it.timestamp) == month
        }
        return breakdownByAccount(filteredTransactions, accounts)
    }

    /**
     * Breaks down transactions by account for a specific transaction type.
     */
    fun breakdownByAccountForTransactionType(
        transactions: List<Transaction>,
        accounts: List<Account>,
        transactionType: String
    ): List<AccountBreakdown> {
        val filteredTransactions = transactions.filter { it.transactionType == transactionType }
        return breakdownByAccount(filteredTransactions, accounts)
    }

    /**
     * Breaks down transactions by account for a specific payment method.
     */
    fun breakdownByAccountForPaymentMethod(
        transactions: List<Transaction>,
        accounts: List<Account>,
        paymentMethod: String
    ): List<AccountBreakdown> {
        val filteredTransactions = transactions.filter { it.paymentMethod == paymentMethod }
        return breakdownByAccount(filteredTransactions, accounts)
    }

    /**
     * Breaks down transactions by account for a specific status.
     */
    fun breakdownByAccountForStatus(
        transactions: List<Transaction>,
        accounts: List<Account>,
        status: TransactionStatus
    ): List<AccountBreakdown> {
        val filteredTransactions = transactions.filter { it.status == status }
        return breakdownByAccount(filteredTransactions, accounts)
    }

    /**
     * Breaks down transactions by account for a specific tag.
     */
    fun breakdownByAccountForTag(
        transactions: List<Transaction>,
        accounts: List<Account>,
        tag: String
    ): List<AccountBreakdown> {
        val filteredTransactions = transactions.filter { it.tags.contains(tag) }
        return breakdownByAccount(filteredTransactions, accounts)
    }

    /**
     * Breaks down transactions by account for a specific custom label.
     */
    fun <L> breakdownByAccountForLabel(
        transactions: List<Transaction>,
        accounts: List<Account>,
        labelSelector: (Transaction) -> L,
        labelValue: L
    ): List<AccountBreakdown> where L : Any {
        val filteredTransactions = transactions.filter { labelSelector(it) == labelValue }
        return breakdownByAccount(filteredTransactions, accounts)
    }

    /**
     * Breaks down transactions by date for a specific account.
     */
    fun breakdownByDateForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<DateBreakdown> {
        val filteredTransactions = filterByAccount(transactions, accountId)
        return breakdownByDate(filteredTransactions)
    }

    /**
     * Breaks down transactions by month for a specific account.
     */
    fun breakdownByMonthForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<MonthBreakdown> {
        val filteredTransactions = filterByAccount(transactions, accountId)
        return breakdownByMonth(filteredTransactions)
    }

    /**
     * Breaks down transactions by transaction type for a specific account.
     */
    fun breakdownByTransactionTypeForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<TransactionTypeBreakdown> {
        val filteredTransactions = filterByAccount(transactions, accountId)
        return breakdownByTransactionType(filteredTransactions)
    }

    /**
     * Breaks down transactions by payment method for a specific account.
     */
    fun breakdownByPaymentMethodForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<PaymentMethodBreakdown> {
        val filteredTransactions = filterByAccount(transactions, accountId)
        return breakdownByPaymentMethod(filteredTransactions)
    }

    /**
     * Breaks down transactions by status for a specific account.
     */
    fun breakdownByStatusForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<StatusBreakdown> {
        val filteredTransactions = filterByAccount(transactions, accountId)
        return breakdownByStatus(filteredTransactions)
    }

    /**
     * Breaks down transactions by tags for a specific account.
     */
    fun breakdownByTagsForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<TagBreakdown> {
        val filteredTransactions = filterByAccount(transactions, accountId)
        return breakdownByTags(filteredTransactions)
    }

    /**
     * Breaks down transactions by account, then by currency within each account.
     */
    fun breakdownByAccountAndCurrency(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<CurrencyBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (_, txs) ->
                breakdownByCurrency(txs)
            }
    }

    /**
     * Breaks down transactions by currency, then by account within each currency.
     */
    fun breakdownByCurrencyAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        return transactions.groupBy { it.currency }
            .mapValues { (_, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by account, then by date within each account.
     */
    fun breakdownByAccountAndDate(
        transactions: List<Transaction>
    ): Map<String, List<DateBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (_, txs) ->
                breakdownByDate(txs)
            }
    }

    /**
     * Breaks down transactions by account, then by month within each account.
     */
    fun breakdownByAccountAndMonth(
        transactions: List<Transaction>
    ): Map<String, List<MonthBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (_, txs) ->
                breakdownByMonth(txs)
            }
    }

    /**
     * Breaks down transactions by account, then by transaction type within each account.
     */
    fun breakdownByAccountAndTransactionType(
        transactions: List<Transaction>
    ): Map<String, List<TransactionTypeBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (_, txs) ->
                breakdownByTransactionType(txs)
            }
    }

    /**
     * Breaks down transactions by account, then by payment method within each account.
     */
    fun breakdownByAccountAndPaymentMethod(
        transactions: List<Transaction>
    ): Map<String, List<PaymentMethodBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (_, txs) ->
                breakdownByPaymentMethod(txs)
            }
    }

    /**
     * Breaks down transactions by account, then by status within each account.
     */
    fun breakdownByAccountAndStatus(
        transactions: List<Transaction>
    ): Map<String, List<StatusBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (_, txs) ->
                breakdownByStatus(txs)
            }
    }

    /**
     * Breaks down transactions by account, then by tags within each account.
     */
    fun breakdownByAccountAndTags(
        transactions: List<Transaction>
    ): Map<String, List<TagBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (_, txs) ->
                breakdownByTags(txs)
            }
    }

    /**
     * Breaks down transactions by date, then by account within each date.
     */
    fun breakdownByDateAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        return transactions.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(systemZoneId).toLocalDate().toString()
        }
            .mapValues { (_, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by month, then by account within each month.
     */
    fun breakdownByMonthAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        return transactions.groupBy {
            formatMonth(it.timestamp)
        }
            .mapValues { (_, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by transaction type, then by account within each type.
     */
    fun breakdownByTransactionTypeAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        return transactions.groupBy { it.transactionType }
            .mapValues { (_, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by payment method, then by account within each method.
     */
    fun breakdownByPaymentMethodAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        return transactions.groupBy { it.paymentMethod }
            .mapValues { (_, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by status, then by account within each status.
     */
    fun breakdownByStatusAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        return transactions.groupBy { it.status.name }
            .mapValues { (_, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by tags, then by account within each tag.
     */
    fun breakdownByTagsAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        return transactions.flatMap { tx -> tx.tags.map { tag -> tag to tx } }
            .groupBy { it.first }
            .mapValues { (_, pairs) -> pairs.map { it.second } }
            .mapValues { (_, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by category.
     */
    fun breakdownByCategory(
        transactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<CategoryBreakdown> =
        createBreakdown(transactions, { it.category }, { category, summaries ->
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
    fun breakdownByDate(
        transactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<DateBreakdown> {
        if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
            throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
        }

        return transactions.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(systemZoneId).toLocalDate().toString()
        }
            .map { (date, txs) ->
                val summaries = calculateSummaryByCurrency(txs)
                val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                    summaries.mapValues { (currency, summary) ->
                        convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
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
    fun breakdownByMonth(
        transactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<MonthBreakdown> {
        if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
            throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
        }

        return transactions.groupBy {
            formatMonth(it.timestamp)
        }
            .map { (month, txs) ->
                val summaries = calculateSummaryByCurrency(txs)
                val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                    summaries.mapValues { (currency, summary) ->
                        convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
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
    fun breakdownByTransactionType(
        transactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<TransactionTypeBreakdown> =
        createBreakdown(transactions, { it.transactionType }, { type, summaries ->
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
    fun breakdownByPaymentMethod(
        transactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<PaymentMethodBreakdown> =
        createBreakdown(transactions, { it.paymentMethod }, { method, summaries ->
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
    fun breakdownByStatus(
        transactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<StatusBreakdown> =
        createBreakdown(transactions, { it.status.name }, { status, summaries ->
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
    fun breakdownByTags(
        transactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<TagBreakdown> {
        if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
            throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
        }

        return transactions.flatMap { tx -> tx.tags.map { tag -> tag to tx } }
            .groupBy { it.first }
            .mapValues { (_, pairs) -> pairs.map { it.second } }
            .map { (tag, txs) ->
                val summaries = calculateSummaryByCurrency(txs)
                val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                    summaries.mapValues { (currency, summary) ->
                        convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
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

    /**
     * Generic breakdown method that groups transactions by a custom label selector.
     */
    fun <L> breakdownByLabel(
        transactions: List<Transaction>,
        labelSelector: (Transaction) -> L,
        exchangeRates: Map<String, BigDecimal>? = null,
        baseCurrency: String? = null
    ): List<LabelGroup<L>> where L : Any {
        if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
            throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
        }

        return transactions.groupBy(labelSelector)
            .map { (label, txs) ->
                val summaries = calculateSummaryByCurrency(txs)
                val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                    summaries.mapValues { (currency, summary) ->
                        convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
                    }
                } else {
                    summaries
                }
                LabelGroup(
                    label = label,
                    financialSummary = FinancialSummary(
                        totalAmount = convertedSummaries.values.sumOf { it.totalAmount },
                        income = convertedSummaries.values.sumOf { it.income },
                        expense = convertedSummaries.values.sumOf { it.expense },
                        transactionCount = convertedSummaries.values.sumOf { it.transactionCount }
                    ),
                    transactions = txs,
                    currencyBreakdown = summaries
                )
            }
            .sortedByDescending { it.totalAmount.abs() }
    }
}
