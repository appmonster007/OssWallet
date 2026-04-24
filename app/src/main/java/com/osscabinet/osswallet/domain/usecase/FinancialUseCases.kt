package com.osscabinet.ossWallet.domain.usecase

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.domain.service.TimelineService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit

data class AccountSummaryItem(
    val account: Account,
    val currencyBalances: Map<String, BigDecimal>,
    val balanceInBase: BigDecimal,
    val incomeInAccountCurrency: BigDecimal,
    val expenseInAccountCurrency: BigDecimal
)

data class CashFlowBreakdownItem(
    val currency: String,
    val income: BigDecimal,
    val incomeInBase: BigDecimal,
    val expense: BigDecimal,
    val expenseInBase: BigDecimal
)

data class CashFlowResult(
    val totalIncomeInBase: BigDecimal,
    val totalExpensesInBase: BigDecimal,
    val netInBase: BigDecimal,
    val breakdown: List<CashFlowBreakdownItem>,
    val filteredTransactions: List<Transaction>
) {
    companion object {
        fun empty() = CashFlowResult(
            totalIncomeInBase = BigDecimal.ZERO,
            totalExpensesInBase = BigDecimal.ZERO,
            netInBase = BigDecimal.ZERO,
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    }
}

data class CurrencyAggregationItem(
    val currency: String,
    val amount: BigDecimal,
    val amountInBase: BigDecimal
) {
    companion object {
        fun empty(currency: String = "") = CurrencyAggregationItem(
            currency = currency,
            amount = BigDecimal.ZERO,
            amountInBase = BigDecimal.ZERO
        )
    }
}

/**
 * Shared data structure for filtered and grouped transactions
 */
private data class FilteredTransactionData(
    val filteredTransactions: List<Transaction>,
    val transactionsByAccount: Map<String, List<Transaction>>
)

class FinancialUseCases(
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository
) {
    /**
     * Shared helper to filter transactions by timeline range and group by account
     */
    private fun filterAndGroupTransactions(
        allTransactions: List<Transaction>,
        timelineRange: TimelineRange
    ): FilteredTransactionData {
        val filteredTransactions = TimelineService.filterByTimelineRange(allTransactions, timelineRange)
        val transactionsByAccount = filteredTransactions.groupBy { it.accountId }
        return FilteredTransactionData(filteredTransactions, transactionsByAccount)
    }

    /**
     * Calculates the currency breakdown for a single account based on its transactions
     * Starts with zero balance and applies filtered transactions for proper timeline filtering
     * @param account The account to calculate breakdown for
     * @param accountTransactions List of transactions for this account (already filtered by timeline)
     * @return Map of currency code to balance amount
     */
    private fun calculateAccountCurrencyBreakdown(
        account: Account,
        accountTransactions: List<Transaction>
    ): Map<String, BigDecimal> {
        val breakdown = mutableMapOf<String, BigDecimal>()

        // Start with zero balance
        // Apply filtered transactions to calculate the balance for the selected timeline
        // Amount already carries the sign (positive for income, negative for expense)
        accountTransactions.forEach { tx ->
            val current = breakdown[tx.currency] ?: BigDecimal.ZERO
            breakdown[tx.currency] = current.add(tx.amount)
        }

        return breakdown.toMap()
    }

    /**
     * Aggregates multiple account currency breakdowns into global currency totals
     * @param accountBreakdowns List of account breakdown maps
     * @return Map of currency code to total amount across all accounts
     */
    private fun aggregateCurrencyBreakdowns(
        accountBreakdowns: List<Map<String, BigDecimal>>
    ): Map<String, BigDecimal> {
        val globalTotals = mutableMapOf<String, BigDecimal>()

        accountBreakdowns.forEach { breakdown ->
            breakdown.forEach { (currency, amount) ->
                globalTotals[currency] = (globalTotals[currency] ?: BigDecimal.ZERO).add(amount)
            }
        }

        return globalTotals.toMap()
    }

    /**
     * Calculates income and expense breakdowns for a single account
     * @param accountTransactions List of transactions for this account
     * @return Pair of (incomeBreakdown, expenseBreakdown) maps
     */
    private fun calculateIncomeExpenseBreakdown(
        accountTransactions: List<Transaction>
    ): Pair<Map<String, BigDecimal>, Map<String, BigDecimal>> {
        val incomeBreakdown = mutableMapOf<String, BigDecimal>()
        val expenseBreakdown = mutableMapOf<String, BigDecimal>()

        accountTransactions.forEach { tx ->
            if (tx.amount.signum() >= 0) {
                incomeBreakdown[tx.currency] = (incomeBreakdown[tx.currency] ?: BigDecimal.ZERO).add(tx.amount)
            } else {
                expenseBreakdown[tx.currency] = (expenseBreakdown[tx.currency] ?: BigDecimal.ZERO).add(tx.amount.abs())
            }
        }

        return Pair(incomeBreakdown.toMap(), expenseBreakdown.toMap())
    }

    /**
     * Sums transactions by currency and type (income/expense)
     * @param transactions List of transactions to sum
     * @param currency The currency to filter by
     * @param transactionType Whether to sum income or expense transactions
     * @return Total amount for the specified currency and type
     * @deprecated Use sign-based filtering instead
     */
    private fun sumTransactionsByCurrencyAndType(
        transactions: List<Transaction>,
        currency: String,
        transactionType: String
    ): BigDecimal {
        // Use sign-based filtering for compatibility
        val isIncome = transactionType == "income"
        return transactions.filter { 
            it.currency == currency && 
            (if (isIncome) it.amount.signum() >= 0 else it.amount.signum() < 0)
        }.fold(BigDecimal.ZERO) { acc, t -> 
            // Amount already carries the sign (positive for income, negative for expense)
            acc.add(t.amount)
        }
    }

    fun calculateNetWorth(
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int,
        timelineRange: TimelineRange
    ): BigDecimal {
        val transactionData = filterAndGroupTransactions(allTransactions, timelineRange)

        val accountBreakdowns = accounts.map { account ->
            val accountTransactions = transactionData.transactionsByAccount[account.id] ?: emptyList()
            calculateAccountCurrencyBreakdown(account, accountTransactions)
        }

        val globalCurrencyTotals = aggregateCurrencyBreakdowns(accountBreakdowns)

        return CurrencyService.calculateTotalInCurrency(globalCurrencyTotals, baseCurrency, exchangeRates, precision)
    }

    fun calculateCashFlow(
        allTransactions: List<Transaction>,
        timelineRange: TimelineRange,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        val filteredTransactions = TimelineService.filterByTimelineRange(allTransactions, timelineRange)

        val currencies = filteredTransactions.map { it.currency }.distinct().sorted()

        val breakdown = currencies.map { curr ->
            val income = sumTransactionsByCurrencyAndType(filteredTransactions, curr, transactionType = "income")
            val expense = sumTransactionsByCurrencyAndType(filteredTransactions, curr, transactionType = "expense")

            val incomeInBase = CurrencyService.convertAmount(income, curr, baseCurrency, exchangeRates, precision + 4)
            val expenseInBase = CurrencyService.convertAmount(expense, curr, baseCurrency, exchangeRates, precision + 4)
            CashFlowBreakdownItem(
                currency = curr,
                income = income,
                incomeInBase = incomeInBase,
                expense = expense,
                expenseInBase = expenseInBase
            )
        }

        val totalIncomeInBase = breakdown.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.incomeInBase)
        }.setScale(precision, RoundingMode.HALF_UP)

        val totalExpensesInBase = breakdown.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.expenseInBase)
        }.setScale(precision, RoundingMode.HALF_UP)

        return CashFlowResult(
            totalIncomeInBase = totalIncomeInBase,
            totalExpensesInBase = totalExpensesInBase,
            netInBase = totalIncomeInBase.subtract(totalExpensesInBase),
            breakdown = breakdown,
            filteredTransactions = filteredTransactions
        )
    }

    fun getAccountSummaries(
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        timelineRange: TimelineRange,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Map<String, AccountSummaryItem> {
        val transactionData = filterAndGroupTransactions(allTransactions, timelineRange)

        return accounts.associate { account ->
            val accountTransactions = transactionData.transactionsByAccount[account.id] ?: emptyList()
            val breakdown = calculateAccountCurrencyBreakdown(account, accountTransactions)
            val (incomeBreakdown, expenseBreakdown) = calculateIncomeExpenseBreakdown(accountTransactions)

            val incomeInAccountCurrency = CurrencyService.calculateTotalInCurrency(incomeBreakdown, account.currencyCode, exchangeRates, precision)
            val expenseInAccountCurrency = CurrencyService.calculateTotalInCurrency(expenseBreakdown, account.currencyCode, exchangeRates, precision)
            val balanceInBase = CurrencyService.calculateTotalInCurrency(breakdown, baseCurrency, exchangeRates, precision)

            account.id to AccountSummaryItem(
                account = account,
                currencyBalances = breakdown,
                balanceInBase = balanceInBase,
                incomeInAccountCurrency = incomeInAccountCurrency,
                expenseInAccountCurrency = expenseInAccountCurrency
            )
        }
    }

    fun getCurrencyAggregation(
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        timelineRange: TimelineRange,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem> {
        val transactionData = filterAndGroupTransactions(allTransactions, timelineRange)

        val accountBreakdowns = accounts.map { account ->
            val accountTransactions = transactionData.transactionsByAccount[account.id] ?: emptyList()
            calculateAccountCurrencyBreakdown(account, accountTransactions)
        }

        val globalCurrencyTotals = aggregateCurrencyBreakdowns(accountBreakdowns)

        return globalCurrencyTotals.map { (currency, amount) ->
            CurrencyAggregationItem(
                currency = currency,
                amount = amount,
                amountInBase = CurrencyService.convertAmount(amount, currency, baseCurrency, exchangeRates, precision)
            )
        }.sortedBy { it.currency }
    }

    fun getTotalBalances(
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int,
        timelineRange: TimelineRange = TimelineRange.AllTime
    ): List<Account> {
        val transactionData = filterAndGroupTransactions(allTransactions, timelineRange)

        return accounts.map { account ->
            val accountTransactions = transactionData.transactionsByAccount[account.id] ?: emptyList()
            val breakdown = calculateAccountCurrencyBreakdown(account, accountTransactions)

            val totalInAccountCurrency = CurrencyService.calculateTotalInCurrency(
                currencyTotals = breakdown,
                targetCurrency = account.currencyCode,
                exchangeRates = exchangeRates,
                precision = precision
            )
            account.copy(balance = totalInAccountCurrency)
        }
    }

    fun getPredictiveBalance(days: Int = 30, profileId: Int = 1, precision: Int = 2): Flow<List<Pair<Long, BigDecimal>>> {
        return transactionRepository.getAllTransactions(profileId).map { transactions ->
            if (transactions.isEmpty()) return@map emptyList()

            val currentBalance = calculateNetFlow(transactions)
            val avgDailyNetFlow = calculateAvgDailyNetFlow(transactions, precision)

            val predictions = mutableListOf<Pair<Long, BigDecimal>>()
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            for (i in 1..days) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val projectedBalance = currentBalance.add(avgDailyNetFlow.multiply(BigDecimal(i)))
                predictions.add(calendar.timeInMillis to projectedBalance)
            }
            predictions
        }
    }

    /**
     * Calculates the net flow (income minus expenses) from a list of transactions
     * @param transactions List of transactions to calculate net flow from
     * @return Net flow amount
     */
    private fun calculateNetFlow(transactions: List<Transaction>): BigDecimal {
        // Amount already carries the sign (positive for income, negative for expense)
        return transactions.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
    }

    private fun calculateAvgDailyNetFlow(transactions: List<Transaction>, precision: Int = 2): BigDecimal {
        if (transactions.isEmpty()) return BigDecimal.ZERO

        val sortedTransactions = transactions.sortedBy { it.timestamp }
        val firstTimestamp = sortedTransactions.first().timestamp
        val lastTimestamp = System.currentTimeMillis()

        val totalDays = TimeUnit.MILLISECONDS.toDays(lastTimestamp - firstTimestamp).coerceAtLeast(1)
        val totalNetFlow = calculateNetFlow(transactions)

        return totalNetFlow.divide(BigDecimal(totalDays), precision, RoundingMode.HALF_UP)
    }

    suspend fun getExchangeRates(baseCurrency: String = "USD"): Map<String, Double> {
        return currencyRepository.getExchangeRates(baseCurrency)
    }
}
