package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A reusable service for aggregating, filtering, and breaking down transaction data.
 * Optimized for performance and code reusability using functional patterns.
 * All breakdowns include currency sub-grouping since transactions can have different currencies.
 *
 * This service is stateless and pure Kotlin (no Android dependencies).
 *
 * ## Currency Conversion
 *
 * Many functions in this service support optional currency conversion parameters to handle
 * multi-currency transactions correctly:
 *
 * - **exchangeRates**: Map<String, Double> where the key is the currency code and the value
 *   is the exchange rate to convert from that currency to the base currency.
 *   Example: For USD as base, {"EUR": 0.85, "GBP": 0.75} means 1 USD = 0.85 EUR, 1 USD = 0.75 GBP
 *
 * - **baseCurrency**: String representing the target currency code for conversion.
 *   Required when exchangeRates is provided.
 *
 * When conversion parameters are provided:
 * - All monetary values are converted to the base currency before aggregation
 * - The currencyBreakdown maps still contain the original per-currency values
 * - Top-level totals (totalAmount, income, expense) are accurate cross-currency sums
 *
 * When conversion parameters are NOT provided:
 * - Functions maintain backward compatibility with original behavior
 * - Top-level totals may be incorrect for multi-currency scenarios (summing without conversion)
 * - Use currencyBreakdown maps for accurate per-currency analysis
 *
 * ## Exchange Rate Structure
 *
 * Based on CurrencyRateEntity:
 * - currencyCode: String (e.g., "USD", "EUR")
 * - baseCurrency: String (e.g., "USD")
 * - rate: Double (conversion rate from currency to base)
 *
 * Example: If baseCurrency is "USD" and you have EUR transactions with rate 0.85:
 * - 100 EUR * 0.85 = 85 USD
 */
object TransactionsUtilityService {

    val systemZoneId: ZoneId = ZoneId.systemDefault()
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    // ==================== Constants ====================

    /**
     * Empty financial summary for use when no transactions are available.
     */
    val EMPTY_SUMMARY = FinancialSummary.EMPTY

    // ==================== Currency Conversion Helpers ====================

    /**
     * Converts an amount from source currency to base currency using exchange rates.
     *
     * @param amount Amount to convert
     * @param fromCurrency Source currency code
     * @param toCurrency Target/base currency code
     * @param exchangeRates Map of currency codes to exchange rates (rate is from currency to base currency)
     * @return Converted amount in base currency, or original amount if conversion not possible
     */
    private fun convertToBaseCurrency(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        exchangeRates: Map<String, Double>
    ): BigDecimal {
        if (fromCurrency == toCurrency) {
            return amount
        }
        val rate = exchangeRates[fromCurrency] ?: return amount
        return amount.multiply(BigDecimal.valueOf(rate))
    }

    /**
     * Converts a FinancialSummary from multiple currencies to a single base currency.
     *
     * @param summary Summary to convert (may have mixed currencies)
     * @param currency Currency code of the summary
     * @param baseCurrency Target/base currency code
     * @param exchangeRates Map of currency codes to exchange rates
     * @return Converted FinancialSummary in base currency
     */
    private fun convertFinancialSummary(
        summary: FinancialSummary,
        currency: String,
        baseCurrency: String,
        exchangeRates: Map<String, Double>
    ): FinancialSummary {
        return FinancialSummary(
            totalAmount = convertToBaseCurrency(summary.totalAmount, currency, baseCurrency, exchangeRates),
            income = convertToBaseCurrency(summary.income, currency, baseCurrency, exchangeRates),
            expense = convertToBaseCurrency(summary.expense, currency, baseCurrency, exchangeRates),
            transactionCount = summary.transactionCount
        )
    }

    // ==================== Filtering ====================

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
     * Filters transactions by currency code.
     *
     * @param transactions List of transactions to filter
     * @param currency Currency code to filter by
     * @return List of transactions with the specified currency
     */
    fun filterByCurrency(transactions: List<Transaction>, currency: String): List<Transaction> =
        transactions.filter { it.currency == currency }

    /**
     * Filters transactions by multiple currency codes.
     *
     * @param transactions List of transactions to filter
     * @param currencies Set of currency codes to filter by
     * @return List of transactions with any of the specified currencies
     */
    fun filterByCurrencies(transactions: List<Transaction>, currencies: Set<String>): List<Transaction> =
        transactions.filter { it.currency in currencies }

    /**
     * Filters transactions by category name.
     *
     * @param transactions List of transactions to filter
     * @param category Category name to filter by
     * @return List of transactions with the specified category
     */
    fun filterByCategory(transactions: List<Transaction>, category: String): List<Transaction> =
        transactions.filter { it.category == category }

    /**
     * Filters transactions by multiple category names.
     *
     * @param transactions List of transactions to filter
     * @param categories Set of category names to filter by
     * @return List of transactions with any of the specified categories
     */
    fun filterByCategories(transactions: List<Transaction>, categories: Set<String>): List<Transaction> =
        transactions.filter { it.category in categories }

    /**
     * Filters transactions by account ID.
     *
     * @param transactions List of transactions to filter
     * @param accountId Account ID to filter by
     * @return List of transactions for the specified account
     */
    fun filterByAccount(transactions: List<Transaction>, accountId: String): List<Transaction> =
        transactions.filter { it.accountId == accountId }

    /**
     * Filters transactions by multiple account IDs.
     *
     * @param transactions List of transactions to filter
     * @param accountIds Set of account IDs to filter by
     * @return List of transactions for any of the specified accounts
     */
    fun filterByAccounts(transactions: List<Transaction>, accountIds: Set<String>): List<Transaction> =
        transactions.filter { it.accountId in accountIds }

    /**
     * Filters transactions by transaction status.
     *
     * @param transactions List of transactions to filter
     * @param status Status to filter by
     * @return List of transactions with the specified status
     */
    fun filterByStatus(transactions: List<Transaction>, status: TransactionStatus): List<Transaction> =
        transactions.filter { it.status == status }

    /**
     * Filters transactions by multiple transaction statuses.
     *
     * @param transactions List of transactions to filter
     * @param statuses Set of statuses to filter by
     * @return List of transactions with any of the specified statuses
     */
    fun filterByStatuses(transactions: List<Transaction>, statuses: Set<TransactionStatus>): List<Transaction> =
        transactions.filter { it.status in statuses }

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

    // ==================== Grouping Logic (Higher-Order Functions) ====================

    /**
     * Groups transactions by their currency code.
     *
     * @param transactions List of transactions to group
     * @return Map where key is currency code and value is list of transactions
     */
    fun groupByCurrency(transactions: List<Transaction>) = transactions.groupBy { it.currency }

    /**
     * Groups transactions by their category name.
     *
     * @param transactions List of transactions to group
     * @return Map where key is category name and value is list of transactions
     */
    fun groupByCategory(transactions: List<Transaction>) = transactions.groupBy { it.category }

    /**
     * Groups transactions by their account ID.
     *
     * @param transactions List of transactions to group
     * @return Map where key is account ID and value is list of transactions
     */
    fun groupByAccount(transactions: List<Transaction>) = transactions.groupBy { it.accountId }

    /**
     * Groups transactions by their transaction type.
     *
     * @param transactions List of transactions to group
     * @return Map where key is transaction type and value is list of transactions
     */
    fun groupByTransactionType(transactions: List<Transaction>) = transactions.groupBy { it.transactionType }

    /**
     * Groups transactions by their payment method.
     *
     * @param transactions List of transactions to group
     * @return Map where key is payment method and value is list of transactions
     */
    fun groupByPaymentMethod(transactions: List<Transaction>) = transactions.groupBy { it.paymentMethod }

    /**
     * Groups transactions by their status name.
     *
     * @param transactions List of transactions to group
     * @return Map where key is status name and value is list of transactions
     */
    fun groupByStatus(transactions: List<Transaction>) = transactions.groupBy { it.status.name }

    /**
     * Groups transactions by their payee.
     *
     * @param transactions List of transactions to group
     * @return Map where key is payee and value is list of transactions
     */
    fun groupByPayee(transactions: List<Transaction>) = transactions.groupBy { it.payee }

    /**
     * Groups transactions by their category ID.
     *
     * @param transactions List of transactions to group
     * @return Map where key is category ID and value is list of transactions
     */
    fun groupByCategoryId(transactions: List<Transaction>) = transactions.groupBy { it.categoryId }

    /**
     * Groups transactions by their title.
     *
     * @param transactions List of transactions to group
     * @return Map where key is title and value is list of transactions
     */
    fun groupByTitle(transactions: List<Transaction>) = transactions.groupBy { it.title }

    /**
     * Groups transactions by their date (local date string format).
     * Uses the system timezone for conversion.
     *
     * @param transactions List of transactions to group
     * @return Map where key is date string (yyyy-MM-dd) and value is list of transactions
     */
    fun groupByDate(transactions: List<Transaction>) = transactions.groupBy { tx ->
        Instant.ofEpochMilli(tx.timestamp)
            .atZone(systemZoneId)
            .toLocalDate()
            .toString()
    }

    /**
     * Groups transactions by their month (yyyy-MM format).
     * Uses the system timezone for conversion.
     *
     * @param transactions List of transactions to group
     * @return Map where key is month string (yyyy-MM) and value is list of transactions
     */
    fun groupByMonth(transactions: List<Transaction>) = transactions.groupBy { tx ->
        Instant.ofEpochMilli(tx.timestamp)
            .atZone(systemZoneId)
            .toLocalDate()
            .format(monthFormatter)
    }

    /**
     * Groups transactions by their tags.
     * A single transaction can appear in multiple tag groups if it has multiple tags.
     *
     * @param transactions List of transactions to group
     * @return Map where key is tag name and value is list of transactions with that tag
     */
    fun groupByTags(transactions: List<Transaction>): Map<String, List<Transaction>> =
        transactions.flatMap { tx -> tx.tags.map { tag -> tag to tx } }
            .groupBy { it.first }
            .mapValues { (_, pairs) -> pairs.map { it.second } }

    // ==================== Calculation Methods ====================

    /**
     * Calculates summary metrics for a list of transactions.
     * All transactions must have the same currency, otherwise throws IllegalArgumentException.
     *
     * @param transactions List of transactions to calculate summary for
     * @return TransactionSummary with total, income, expense, and count
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
     * @return Map where key is currency code and value is TransactionSummary for that currency
     */
    fun calculateSummaryByCurrency(
        transactions: List<Transaction>
    ): Map<String, FinancialSummary> {
        val currencyGroups = groupByCurrency(transactions)

        val summaries = currencyGroups.mapValues { (_, txs) -> calculateTransactionSummary(txs) }

        return summaries
    }

    /**
     * Calculates total transaction amounts by currency.
     * Returns a map with currency codes as keys and total amounts as values.
     *
     * @param transactions List of transactions to calculate totals for
     * @return Map where key is currency code and value is total amount for that currency
     */
    fun calculateTotalByCurrency(transactions: List<Transaction>): Map<String, BigDecimal> =
        groupByCurrency(transactions).mapValues { (_, txs) -> txs.sumOf { it.amount } }

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
     * Depends on separateIncomeExpense and calculateTransactionSummary.
     *
     * @param transactions List of transactions to calculate breakdown for
     * @param exchangeRates Optional map of currency codes to exchange rates (rate is from currency to base currency)
     * @param baseCurrency Optional base currency code for conversion. Required if exchangeRates is provided.
     * @return IncomeExpenseBreakdown with totals, transactions, and currency breakdowns
     * @throws IllegalArgumentException if exchangeRates is provided but baseCurrency is null or empty
     */
    fun calculateIncomeExpenseBreakdown(
        transactions: List<Transaction>,
        exchangeRates: Map<String, Double>? = null,
        baseCurrency: String? = null
    ): IncomeExpenseBreakdown {
        if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
            throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
        }

        val (income, expenses) = separateIncomeExpense(transactions)

        val incomeCurrencyBreakdown = calculateSummaryByCurrency(income)

        val expenseCurrencyBreakdown = calculateSummaryByCurrency(expenses)

        val (totalIncome, totalExpense, netFlow) = if (exchangeRates != null && baseCurrency != null) {
            // Convert all amounts to base currency
            val convertedIncome = incomeCurrencyBreakdown.map { (currency, summary) ->
                convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
            }
            val convertedExpense = expenseCurrencyBreakdown.map { (currency, summary) ->
                convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
            }

            val totalInc = convertedIncome.sumOf { it.totalAmount }
            val totalExp = convertedExpense.sumOf { it.expense }
            val net = totalInc + totalExp

            Triple(totalInc, totalExp, net)
        } else {
            // Original behavior (may be incorrect for multi-currency)
            val totalInc = income.sumOf { it.amount }
            val totalExp = expenses.sumOf { it.amount }
            val net = transactions.sumOf { it.amount }

            Triple(totalInc, totalExp, net)
        }

        return IncomeExpenseBreakdown(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netFlow = netFlow,
            incomeTransactions = income,
            expenseTransactions = expenses,
            incomeCurrencyBreakdown = incomeCurrencyBreakdown,
            expenseCurrencyBreakdown = expenseCurrencyBreakdown
        )
    }

    // ==================== Generic Breakdown Helper ====================

    /**
     * Generic helper that reduces boilerplate for breakdown methods.
     * Groups by key selector and creates breakdown with currency sub-grouping for each group.
     * Depends on calculateSummaryByCurrency.
     *
     * @param transactions List of transactions to break down
     * @param selector Function to extract grouping key from transaction
     * @param mapper Function to create result from key and currency summaries
     * @param exchangeRates Optional map of currency codes to exchange rates
     * @param baseCurrency Optional base currency code for conversion
     * @return List of breakdown results
     */
    private fun <K, R> createBreakdown(
        transactions: List<Transaction>,
        selector: (Transaction) -> K,
        mapper: (K, Map<String, FinancialSummary>) -> R,
        exchangeRates: Map<String, Double>? = null,
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
                mapper(
                    key,
                    convertedSummaries
                )
            }
    }

    // ==================== Breakdown Methods ====================
    // All breakdown methods depend on the breakdownBy helper and grouping methods above

    /**
     * Breaks down transactions by currency with income, expense, and transaction count.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @return List of CurrencyBreakdown sorted by absolute total amount
     */
    fun breakdownByCurrency(transactions: List<Transaction>): List<CurrencyBreakdown> =
        groupByCurrency(transactions)
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
     * Breaks down transactions by account with currency sub-grouping.
     * Requires account information to include account names and currencies in the result.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param exchangeRates Optional map of currency codes to exchange rates (rate is from currency to base currency)
     * @param baseCurrency Optional base currency code for conversion. Required if exchangeRates is provided.
     * @return List of AccountBreakdown sorted by absolute total amount
     */
    fun breakdownByAccount(
        transactions: List<Transaction>,
        accounts: List<Account>,
        exchangeRates: Map<String, Double>? = null,
        baseCurrency: String? = null
    ): List<AccountBreakdown> {
        val accountMap = accounts.associateBy { it.id }
        return createBreakdown(transactions, { it.accountId }, { accountId, summaries ->
            val account = accountMap[accountId]
            Breakdown(
                key = AccountInfo(
                    accountId = accountId,
                    accountName = account?.name ?: "Unknown",
                    currency = account?.currencyCode ?: "Unknown"
                ),
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
     * Filters transactions to only include those matching the specified currency,
     * then breaks them down by account. Requires account information to include
     * account names and currencies in the result.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param currency Currency code to filter transactions by
     * @return List of AccountBreakdown sorted by absolute total amount
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
     * Filters transactions to only include those matching the specified account ID,
     * then breaks them down by currency. Requires account information to include
     * account names and currencies in the result.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param accountId Account ID to filter transactions by
     * @return List of CurrencyBreakdown sorted by absolute total amount
     */
    fun breakdownByCurrencyForAccount(
        transactions: List<Transaction>,
        accounts: List<Account>,
        accountId: String
    ): List<CurrencyBreakdown> {
        val filteredTransactions = transactions.filter { it.accountId == accountId }
        return breakdownByCurrency(filteredTransactions)
    }

    /**
     * Breaks down transactions by category for a specific account.
     * Filters transactions to only include those matching the specified account ID,
     * then breaks them down by category.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accountId Account ID to filter transactions by
     * @return List of CategoryBreakdown sorted by absolute total amount
     */
    fun breakdownByCategoryForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<CategoryBreakdown> {
        val filteredTransactions = transactions.filter { it.accountId == accountId }
        return breakdownByCategory(filteredTransactions)
    }

    /**
     * Breaks down transactions by category, then by account within each category.
     * Returns a nested structure where each category contains a breakdown by account.
     * Requires account information to include account names and currencies in the result.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @return Map where key is category and value is list of AccountBreakdown for that category
     */
    fun breakdownByCategoryAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        val accountMap = accounts.associateBy { it.id }
        return transactions.groupBy { it.category }
            .mapValues { (category, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by account, then by category within each account.
     * Returns a nested structure where each account contains a breakdown by category.
     * Requires account information to include account names and currencies in the result.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @return Map where key is accountId and value is list of CategoryBreakdown for that account
     */
    fun breakdownByAccountAndCategory(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<CategoryBreakdown>> {
        val accountMap = accounts.associateBy { it.id }
        return transactions.groupBy { it.accountId }
            .mapValues { (accountId, txs) ->
                breakdownByCategory(txs)
            }
    }

    /**
     * Breaks down transactions by account for a specific category.
     * Filters transactions to only include those matching the specified category,
     * then breaks them down by account.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param category Category to filter transactions by
     * @return List of AccountBreakdown sorted by absolute total amount
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
     * Filters transactions to only include those matching the specified date,
     * then breaks them down by account.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param date Date to filter transactions by (format: yyyy-MM-dd)
     * @return List of AccountBreakdown sorted by absolute total amount
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
     * Filters transactions to only include those matching the specified month,
     * then breaks them down by account.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param month Month to filter transactions by (format: yyyy-MM)
     * @return List of AccountBreakdown sorted by absolute total amount
     */
    fun breakdownByAccountForMonth(
        transactions: List<Transaction>,
        accounts: List<Account>,
        month: String
    ): List<AccountBreakdown> {
        val filteredTransactions = transactions.filter {
            Instant.ofEpochMilli(it.timestamp).atZone(systemZoneId).format(monthFormatter) == month
        }
        return breakdownByAccount(filteredTransactions, accounts)
    }

    /**
     * Breaks down transactions by account for a specific transaction type.
     * Filters transactions to only include those matching the specified transaction type,
     * then breaks them down by account.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param transactionType Transaction type to filter transactions by
     * @return List of AccountBreakdown sorted by absolute total amount
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
     * Filters transactions to only include those matching the specified payment method,
     * then breaks them down by account.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param paymentMethod Payment method to filter transactions by
     * @return List of AccountBreakdown sorted by absolute total amount
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
     * Filters transactions to only include those matching the specified status,
     * then breaks them down by account.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param status Status to filter transactions by
     * @return List of AccountBreakdown sorted by absolute total amount
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
     * Filters transactions to only include those containing the specified tag,
     * then breaks them down by account.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param tag Tag to filter transactions by
     * @return List of AccountBreakdown sorted by absolute total amount
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
     * Filters transactions to only include those matching the specified label selector,
     * then breaks them down by account.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @param labelSelector Function to extract a label from each transaction
     * @param labelValue Label value to filter transactions by
     * @return List of AccountBreakdown sorted by absolute total amount
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
     * Filters transactions to only include those matching the specified account ID,
     * then breaks them down by date.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accountId Account ID to filter transactions by
     * @return List of DateBreakdown sorted by absolute total amount
     */
    fun breakdownByDateForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<DateBreakdown> {
        val filteredTransactions = transactions.filter { it.accountId == accountId }
        return breakdownByDate(filteredTransactions)
    }

    /**
     * Breaks down transactions by month for a specific account.
     * Filters transactions to only include those matching the specified account ID,
     * then breaks them down by month.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accountId Account ID to filter transactions by
     * @return List of MonthBreakdown sorted by absolute total amount
     */
    fun breakdownByMonthForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<MonthBreakdown> {
        val filteredTransactions = transactions.filter { it.accountId == accountId }
        return breakdownByMonth(filteredTransactions)
    }

    /**
     * Breaks down transactions by transaction type for a specific account.
     * Filters transactions to only include those matching the specified account ID,
     * then breaks them down by transaction type.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accountId Account ID to filter transactions by
     * @return List of TransactionTypeBreakdown sorted by absolute total amount
     */
    fun breakdownByTransactionTypeForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<TransactionTypeBreakdown> {
        val filteredTransactions = transactions.filter { it.accountId == accountId }
        return breakdownByTransactionType(filteredTransactions)
    }

    /**
     * Breaks down transactions by payment method for a specific account.
     * Filters transactions to only include those matching the specified account ID,
     * then breaks them down by payment method.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accountId Account ID to filter transactions by
     * @return List of PaymentMethodBreakdown sorted by absolute total amount
     */
    fun breakdownByPaymentMethodForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<PaymentMethodBreakdown> {
        val filteredTransactions = transactions.filter { it.accountId == accountId }
        return breakdownByPaymentMethod(filteredTransactions)
    }

    /**
     * Breaks down transactions by status for a specific account.
     * Filters transactions to only include those matching the specified account ID,
     * then breaks them down by status.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accountId Account ID to filter transactions by
     * @return List of StatusBreakdown sorted by absolute total amount
     */
    fun breakdownByStatusForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<StatusBreakdown> {
        val filteredTransactions = transactions.filter { it.accountId == accountId }
        return breakdownByStatus(filteredTransactions)
    }

    /**
     * Breaks down transactions by tags for a specific account.
     * Filters transactions to only include those matching the specified account ID,
     * then breaks them down by tags.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param accountId Account ID to filter transactions by
     * @return List of TagBreakdown sorted by absolute total amount
     */
    fun breakdownByTagsForAccount(
        transactions: List<Transaction>,
        accountId: String
    ): List<TagBreakdown> {
        val filteredTransactions = transactions.filter { it.accountId == accountId }
        return breakdownByTags(filteredTransactions)
    }

    /**
     * Breaks down transactions by account, then by currency within each account.
     * Returns a nested structure where each account contains a breakdown by currency.
     * Requires account information to include account names and currencies in the result.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @return Map where key is accountId and value is list of CurrencyBreakdown for that account
     */
    fun breakdownByAccountAndCurrency(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<CurrencyBreakdown>> {
        val accountMap = accounts.associateBy { it.id }
        return transactions.groupBy { it.accountId }
            .mapValues { (accountId, txs) ->
                breakdownByCurrency(txs)
            }
    }

    /**
     * Breaks down transactions by currency, then by account within each currency.
     * Returns a nested structure where each currency contains a breakdown by account.
     * Requires account information to include account names and currencies in the result.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @return Map where key is currency code and value is list of AccountBreakdown for that currency
     */
    fun breakdownByCurrencyAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        val accountMap = accounts.associateBy { it.id }
        return transactions.groupBy { it.currency }
            .mapValues { (currency, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by account, then by date within each account.
     * Returns a nested structure where each account contains a breakdown by date.
     *
     * @param transactions List of transactions to break down
     * @return Map where key is accountId and value is list of DateBreakdown for that account
     */
    fun breakdownByAccountAndDate(
        transactions: List<Transaction>
    ): Map<String, List<DateBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (accountId, txs) ->
                breakdownByDate(txs)
            }
    }

    /**
     * Breaks down transactions by account, then by month within each account.
     * Returns a nested structure where each account contains a breakdown by month.
     *
     * @param transactions List of transactions to break down
     * @return Map where key is accountId and value is list of MonthBreakdown for that account
     */
    fun breakdownByAccountAndMonth(
        transactions: List<Transaction>
    ): Map<String, List<MonthBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (accountId, txs) ->
                breakdownByMonth(txs)
            }
    }

    /**
     * Breaks down transactions by account, then by transaction type within each account.
     * Returns a nested structure where each account contains a breakdown by transaction type.
     *
     * @param transactions List of transactions to break down
     * @return Map where key is accountId and value is list of TransactionTypeBreakdown for that account
     */
    fun breakdownByAccountAndTransactionType(
        transactions: List<Transaction>
    ): Map<String, List<TransactionTypeBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (accountId, txs) ->
                breakdownByTransactionType(txs)
            }
    }

    /**
     * Breaks down transactions by account, then by payment method within each account.
     * Returns a nested structure where each account contains a breakdown by payment method.
     *
     * @param transactions List of transactions to break down
     * @return Map where key is accountId and value is list of PaymentMethodBreakdown for that account
     */
    fun breakdownByAccountAndPaymentMethod(
        transactions: List<Transaction>
    ): Map<String, List<PaymentMethodBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (accountId, txs) ->
                breakdownByPaymentMethod(txs)
            }
    }

    /**
     * Breaks down transactions by account, then by status within each account.
     * Returns a nested structure where each account contains a breakdown by status.
     *
     * @param transactions List of transactions to break down
     * @return Map where key is accountId and value is list of StatusBreakdown for that account
     */
    fun breakdownByAccountAndStatus(
        transactions: List<Transaction>
    ): Map<String, List<StatusBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (accountId, txs) ->
                breakdownByStatus(txs)
            }
    }

    /**
     * Breaks down transactions by account, then by tags within each account.
     * Returns a nested structure where each account contains a breakdown by tags.
     *
     * @param transactions List of transactions to break down
     * @return Map where key is accountId and value is list of TagBreakdown for that account
     */
    fun breakdownByAccountAndTags(
        transactions: List<Transaction>
    ): Map<String, List<TagBreakdown>> {
        return transactions.groupBy { it.accountId }
            .mapValues { (accountId, txs) ->
                breakdownByTags(txs)
            }
    }

    /**
     * Breaks down transactions by date, then by account within each date.
     * Returns a nested structure where each date contains a breakdown by account.
     * Requires account information to include account names and currencies in the result.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @return Map where key is date and value is list of AccountBreakdown for that date
     */
    fun breakdownByDateAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        val accountMap = accounts.associateBy { it.id }
        return transactions.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(systemZoneId).toLocalDate().toString()
        }
            .mapValues { (date, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by month, then by account within each month.
     * Returns a nested structure where each month contains a breakdown by account.
     * Requires account information to include account names and currencies in the result.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @return Map where key is month and value is list of AccountBreakdown for that month
     */
    fun breakdownByMonthAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        val accountMap = accounts.associateBy { it.id }
        return transactions.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(systemZoneId).format(monthFormatter)
        }
            .mapValues { (month, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by transaction type, then by account within each type.
     * Returns a nested structure where each transaction type contains a breakdown by account.
     * Requires account information to include account names and currencies in the result.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @return Map where key is transaction type and value is list of AccountBreakdown for that type
     */
    fun breakdownByTransactionTypeAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        val accountMap = accounts.associateBy { it.id }
        return transactions.groupBy { it.transactionType }
            .mapValues { (type, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by payment method, then by account within each method.
     * Returns a nested structure where each payment method contains a breakdown by account.
     * Requires account information to include account names and currencies in the result.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @return Map where key is payment method and value is list of AccountBreakdown for that method
     */
    fun breakdownByPaymentMethodAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        val accountMap = accounts.associateBy { it.id }
        return transactions.groupBy { it.paymentMethod }
            .mapValues { (method, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by status, then by account within each status.
     * Returns a nested structure where each status contains a breakdown by account.
     * Requires account information to include account names and currencies in the result.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @return Map where key is status name and value is list of AccountBreakdown for that status
     */
    fun breakdownByStatusAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        val accountMap = accounts.associateBy { it.id }
        return transactions.groupBy { it.status.name }
            .mapValues { (status, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by tags, then by account within each tag.
     * Returns a nested structure where each tag contains a breakdown by account.
     * Requires account information to include account names and currencies in the result.
     *
     * @param transactions List of transactions to break down
     * @param accounts List of accounts for account information lookup
     * @return Map where key is tag and value is list of AccountBreakdown for that tag
     */
    fun breakdownByTagsAndAccount(
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Map<String, List<AccountBreakdown>> {
        val accountMap = accounts.associateBy { it.id }
        return groupByTags(transactions)
            .mapValues { (tag, txs) ->
                breakdownByAccount(txs, accounts)
            }
    }

    /**
     * Breaks down transactions by category with currency sub-grouping.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param exchangeRates Optional map of currency codes to exchange rates (rate is from currency to base currency)
     * @param baseCurrency Optional base currency code for conversion. Required if exchangeRates is provided.
     * @return List of CategoryBreakdown sorted by absolute total amount
     */
    fun breakdownByCategory(
        transactions: List<Transaction>,
        exchangeRates: Map<String, Double>? = null,
        baseCurrency: String? = null
    ): List<CategoryBreakdown> =
        createBreakdown(transactions, { it.category }, { category, summaries ->
            Breakdown(
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
     * Breaks down transactions by date with currency sub-grouping.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param exchangeRates Optional map of currency codes to exchange rates (rate is from currency to base currency)
     * @param baseCurrency Optional base currency code for conversion. Required if exchangeRates is provided.
     * @return List of DateBreakdown sorted by absolute total amount
     */
    fun breakdownByDate(
        transactions: List<Transaction>,
        exchangeRates: Map<String, Double>? = null,
        baseCurrency: String? = null
    ): List<DateBreakdown> {
        if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
            throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
        }

        return groupByDate(transactions)
            .map { (date, txs) ->
                val summaries = calculateSummaryByCurrency(txs)
                val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                    summaries.mapValues { (currency, summary) ->
                        convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
                    }
                } else {
                    summaries
                }
                Breakdown(
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
     * Breaks down transactions by month with currency sub-grouping.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param exchangeRates Optional map of currency codes to exchange rates (rate is from currency to base currency)
     * @param baseCurrency Optional base currency code for conversion. Required if exchangeRates is provided.
     * @return List of MonthBreakdown sorted by absolute total amount
     */
    fun breakdownByMonth(
        transactions: List<Transaction>,
        exchangeRates: Map<String, Double>? = null,
        baseCurrency: String? = null
    ): List<MonthBreakdown> {
        if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
            throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
        }

        return groupByMonth(transactions)
            .map { (month, txs) ->
                val summaries = calculateSummaryByCurrency(txs)
                val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                    summaries.mapValues { (currency, summary) ->
                        convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
                    }
                } else {
                    summaries
                }
                Breakdown(
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
     * Breaks down transactions by transaction type with currency sub-grouping.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param exchangeRates Optional map of currency codes to exchange rates (rate is from currency to base currency)
     * @param baseCurrency Optional base currency code for conversion. Required if exchangeRates is provided.
     * @return List of TransactionTypeBreakdown sorted by absolute total amount
     */
    fun breakdownByTransactionType(
        transactions: List<Transaction>,
        exchangeRates: Map<String, Double>? = null,
        baseCurrency: String? = null
    ): List<TransactionTypeBreakdown> =
        createBreakdown(transactions, { it.transactionType }, { type, summaries ->
            Breakdown(
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
     * Breaks down transactions by payment method with currency sub-grouping.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param exchangeRates Optional map of currency codes to exchange rates (rate is from currency to base currency)
     * @param baseCurrency Optional base currency code for conversion. Required if exchangeRates is provided.
     * @return List of PaymentMethodBreakdown sorted by absolute total amount
     */
    fun breakdownByPaymentMethod(
        transactions: List<Transaction>,
        exchangeRates: Map<String, Double>? = null,
        baseCurrency: String? = null
    ): List<PaymentMethodBreakdown> =
        createBreakdown(transactions, { it.paymentMethod }, { method, summaries ->
            Breakdown(
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
     * Breaks down transactions by status with currency sub-grouping.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param exchangeRates Optional map of currency codes to exchange rates (rate is from currency to base currency)
     * @param baseCurrency Optional base currency code for conversion. Required if exchangeRates is provided.
     * @return List of StatusBreakdown sorted by absolute total amount
     */
    fun breakdownByStatus(
        transactions: List<Transaction>,
        exchangeRates: Map<String, Double>? = null,
        baseCurrency: String? = null
    ): List<StatusBreakdown> =
        createBreakdown(transactions, { it.status.name }, { status, summaries ->
            Breakdown(
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
     * Breaks down transactions by tags with currency sub-grouping.
     * A single transaction can appear in multiple tag breakdowns if it has multiple tags.
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param exchangeRates Optional map of currency codes to exchange rates (rate is from currency to base currency)
     * @param baseCurrency Optional base currency code for conversion. Required if exchangeRates is provided.
     * @return List of TagBreakdown sorted by absolute total amount
     */
    fun breakdownByTags(
        transactions: List<Transaction>,
        exchangeRates: Map<String, Double>? = null,
        baseCurrency: String? = null
    ): List<TagBreakdown> {
        if (exchangeRates != null && baseCurrency.isNullOrEmpty()) {
            throw IllegalArgumentException("baseCurrency must be provided when exchangeRates is specified")
        }

        return groupByTags(transactions)
            .map { (tag, txs) ->
                val summaries = calculateSummaryByCurrency(txs)
                val convertedSummaries = if (exchangeRates != null && baseCurrency != null) {
                    summaries.mapValues { (currency, summary) ->
                        convertFinancialSummary(summary, currency, baseCurrency, exchangeRates)
                    }
                } else {
                    summaries
                }
                Breakdown(
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
     * Includes the actual transactions in each group (unlike other breakdown methods).
     * Results are sorted by absolute total amount in descending order.
     *
     * @param transactions List of transactions to break down
     * @param labelSelector Function to extract a label from each transaction
     * @param exchangeRates Optional map of currency codes to exchange rates (rate is from currency to base currency)
     * @param baseCurrency Optional base currency code for conversion. Required if exchangeRates is provided.
     * @return List of LabelGroup sorted by absolute total amount
     */
    fun <L> breakdownByLabel(
        transactions: List<Transaction>,
        labelSelector: (Transaction) -> L,
        exchangeRates: Map<String, Double>? = null,
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

// ==================== Data Classes for Return Types ====================

/**
 * Base financial summary with common metrics for transaction calculations.
 * Used as the foundation for other financial data structures.
 */
data class FinancialSummary(
    val totalAmount: BigDecimal,
    val income: BigDecimal,
    val expense: BigDecimal,
    val transactionCount: Int
) {
    companion object {
        /**
         * Empty financial summary for use when no transactions are available.
         */
        val EMPTY = FinancialSummary(
            totalAmount = BigDecimal.ZERO,
            income = BigDecimal.ZERO,
            expense = BigDecimal.ZERO,
            transactionCount = 0
        )
    }
}

/**
 * Type alias for backward compatibility. TransactionSummary is now FinancialSummary.
 */
typealias TransactionSummary = FinancialSummary

/**
 * Generic breakdown class with a key field and currency sub-grouping.
 * Composes FinancialSummary for the financial metrics.
 */
data class Breakdown<K>(
    val key: K,
    val financialSummary: FinancialSummary,
    val currencyBreakdown: Map<String, FinancialSummary>
) {
    val totalAmount: BigDecimal get() = financialSummary.totalAmount
    val income: BigDecimal get() = financialSummary.income
    val expense: BigDecimal get() = financialSummary.expense
    val transactionCount: Int get() = financialSummary.transactionCount
}

// Type aliases for specific breakdown types
typealias AccountBreakdown = Breakdown<AccountInfo>
typealias CategoryBreakdown = Breakdown<String>
typealias DateBreakdown = Breakdown<String>
typealias MonthBreakdown = Breakdown<String>
typealias TransactionTypeBreakdown = Breakdown<String>
typealias PaymentMethodBreakdown = Breakdown<String>
typealias TagBreakdown = Breakdown<String>
typealias StatusBreakdown = Breakdown<String>

/**
 * Account info for AccountBreakdown.
 */
data class AccountInfo(
    val accountId: String,
    val accountName: String,
    val currency: String
)

/**
 * Currency breakdown with detailed financial metrics.
 * Used by breakdownByCurrency to provide per-currency breakdowns.
 */
data class CurrencyBreakdown(
    val currency: String,
    val financialSummary: FinancialSummary
) {
    val totalAmount: BigDecimal get() = financialSummary.totalAmount
    val income: BigDecimal get() = financialSummary.income
    val expense: BigDecimal get() = financialSummary.expense
    val transactionCount: Int get() = financialSummary.transactionCount
}

/**
 * Complete breakdown of income and expense transactions with currency sub-grouping.
 * Used by calculateIncomeExpenseBreakdown to provide comprehensive income/expense analysis.
 */
data class IncomeExpenseBreakdown(
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val netFlow: BigDecimal,
    val incomeTransactions: List<Transaction>,
    val expenseTransactions: List<Transaction>,
    val incomeCurrencyBreakdown: Map<String, FinancialSummary>,
    val expenseCurrencyBreakdown: Map<String, FinancialSummary>
)

/**
 * Generic label group with transactions and currency breakdown.
 * Unlike other breakdown types, this includes the actual transactions in each group.
 * Used by breakdownByLabel for custom label-based breakdowns.
 */
data class LabelGroup<L>(
    val label: L,
    val financialSummary: FinancialSummary,
    val transactions: List<Transaction>,
    val currencyBreakdown: Map<String, FinancialSummary>
) {
    val totalAmount: BigDecimal get() = financialSummary.totalAmount
    val income: BigDecimal get() = financialSummary.income
    val expense: BigDecimal get() = financialSummary.expense
    val transactionCount: Int get() = financialSummary.transactionCount
}
