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
)

data class CurrencyAggregationItem(
    val currency: String,
    val amount: BigDecimal,
    val amountInBase: BigDecimal
)

class FinancialUseCases(
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository
) {
    fun calculateNetWorth(
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int,
        timelineRange: TimelineRange
    ): BigDecimal {
        val filteredTransactions = TimelineService.filterByTimelineRange(allTransactions, timelineRange)

        val transactionsByAccount = filteredTransactions.groupBy { it.accountId }
        val includeInitialBalance = timelineRange is TimelineRange.AllTime
        
        val globalCurrencyTotals = mutableMapOf<String, BigDecimal>()
        
        accounts.forEach { account ->
            val accountTransactions = transactionsByAccount[account.id] ?: emptyList()
            val breakdown = mutableMapOf<String, BigDecimal>()
            
            if (includeInitialBalance) {
                breakdown[account.currencyCode] = account.initialBalance
            }

            accountTransactions.forEach { tx ->
                val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
            }
            
            breakdown.forEach { (curr, amount) ->
                globalCurrencyTotals[curr] = (globalCurrencyTotals[curr] ?: BigDecimal.ZERO).add(amount)
            }
        }

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
        
        val breakdown = currencies.mapNotNull { curr ->
            val txsForCurr = filteredTransactions.filter { it.currency == curr }
            val income = txsForCurr.filter { it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
            val expense = txsForCurr.filter { !it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }

            if (income.signum() == 0 && expense.signum() == 0) null
            else {
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
        val filteredTransactions = TimelineService.filterByTimelineRange(allTransactions, timelineRange)

        val transactionsByAccount = filteredTransactions.groupBy { it.accountId }
        val includeInitial = timelineRange is TimelineRange.AllTime

        return accounts.associate { account ->
            val accountTransactions = transactionsByAccount[account.id] ?: emptyList()
            val breakdown = mutableMapOf<String, BigDecimal>()
            
            if (includeInitial) {
                breakdown[account.currencyCode] = account.initialBalance
            }

            val incomeBreakdown = mutableMapOf<String, BigDecimal>()
            val expenseBreakdown = mutableMapOf<String, BigDecimal>()
            
            accountTransactions.forEach { tx ->
                val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
                
                if (tx.isIncome) {
                    incomeBreakdown[tx.currency] = (incomeBreakdown[tx.currency] ?: BigDecimal.ZERO).add(tx.amount)
                } else {
                    expenseBreakdown[tx.currency] = (expenseBreakdown[tx.currency] ?: BigDecimal.ZERO).add(tx.amount)
                }
            }
            
            val incomeInAccountCurrency = CurrencyService.calculateTotalInCurrency(incomeBreakdown, account.currencyCode, exchangeRates, precision)
            val expenseInAccountCurrency = CurrencyService.calculateTotalInCurrency(expenseBreakdown, account.currencyCode, exchangeRates, precision)
            val balanceInBase = CurrencyService.calculateTotalInCurrency(breakdown, baseCurrency, exchangeRates, precision)
            
            account.id to AccountSummaryItem(
                account = account,
                currencyBalances = breakdown.toMap(),
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
        val filteredTransactions = TimelineService.filterByTimelineRange(allTransactions, timelineRange)

        val transactionsByAccount = filteredTransactions.groupBy { it.accountId }
        val includeInitial = timelineRange is TimelineRange.AllTime
        
        val totalsMap = mutableMapOf<String, BigDecimal>()
        
        accounts.forEach { account ->
            val accountTransactions = transactionsByAccount[account.id] ?: emptyList()
            val accountBreakdown = mutableMapOf<String, BigDecimal>()
            
            if (includeInitial) {
                accountBreakdown[account.currencyCode] = account.initialBalance
            }
            
            accountTransactions.forEach { tx ->
                val current = accountBreakdown[tx.currency] ?: BigDecimal.ZERO
                accountBreakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
            }
            accountBreakdown.forEach { (curr, amount) ->
                totalsMap[curr] = (totalsMap[curr] ?: BigDecimal.ZERO).add(amount)
            }
        }
        
        return totalsMap.map { (currency, amount) ->
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
        precision: Int
    ): List<Account> {
        val allTransactionsByAccount = allTransactions.groupBy { it.accountId }
        
        return accounts.map { account ->
            val accountTransactions = allTransactionsByAccount[account.id] ?: emptyList()
            val breakdown = mutableMapOf<String, BigDecimal>()
            breakdown[account.currencyCode] = account.initialBalance
            
            accountTransactions.forEach { tx ->
                val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
            }
            
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

            val currentBalance = transactions.fold(BigDecimal.ZERO) { acc, t ->
                if (t.isIncome) acc.add(t.amount) else acc.subtract(t.amount)
            }
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

    private fun calculateAvgDailyNetFlow(transactions: List<Transaction>, precision: Int = 2): BigDecimal {
        if (transactions.isEmpty()) return BigDecimal.ZERO

        val sortedTransactions = transactions.sortedBy { it.timestamp }
        val firstTimestamp = sortedTransactions.first().timestamp
        val lastTimestamp = System.currentTimeMillis()

        val totalDays = TimeUnit.MILLISECONDS.toDays(lastTimestamp - firstTimestamp).coerceAtLeast(1)
        val totalNetFlow = transactions.fold(BigDecimal.ZERO) { acc, t ->
            if (t.isIncome) acc.add(t.amount) else acc.subtract(t.amount)
        }

        return totalNetFlow.divide(BigDecimal(totalDays), precision, RoundingMode.HALF_UP)
    }

    suspend fun getExchangeRates(baseCurrency: String = "USD"): Map<String, Double> {
        return currencyRepository.getExchangeRates(baseCurrency)
    }
}
