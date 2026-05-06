package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.data.local.TransactionGroupingType
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

object TransactionGroupingService {

    data class GroupHeaderData(
        val title: String,
        val subtitle: String,
        val incomeByCurrency: Map<String, BigDecimal>,
        val expenseByCurrency: Map<String, BigDecimal>,
        val closingBalancesByCurrency: Map<String, BigDecimal>,
        val allCurrencies: List<String>,
        val incomeBase: BigDecimal,
        val expenseBase: BigDecimal,
        val netBase: BigDecimal,
        val totalClosingBalanceBase: BigDecimal
    )

    fun groupTransactions(
        transactions: List<TransactionWithBalance>,
        groupingType: TransactionGroupingType
    ): Map<String, List<TransactionWithBalance>> {
        val calendar = Calendar.getInstance()
        return when (groupingType) {
            TransactionGroupingType.BY_DAY -> {
                transactions.groupBy {
                    calendar.timeInMillis = it.transaction.timestamp
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    val month = calendar.get(Calendar.MONTH)
                    val year = calendar.get(Calendar.YEAR)
                    "$year-$month-$day"
                }
            }
            TransactionGroupingType.BY_WEEK -> {
                transactions.groupBy {
                    calendar.timeInMillis = it.transaction.timestamp
                    val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
                    val year = calendar.get(Calendar.YEAR)
                    "$year-W$weekOfYear"
                }
            }
        }
    }

    fun getHeaderData(
        groupKey: String,
        groupTransactions: List<TransactionWithBalance>,
        allTransactions: List<TransactionWithBalance>,
        groupingType: TransactionGroupingType,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): GroupHeaderData {
        val calendar = Calendar.getInstance()

        val title: String
        val subtitle: String
        val endTimestamp: Long

        when (groupingType) {
            TransactionGroupingType.BY_DAY -> {
                val firstTx = groupTransactions.first()
                val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(firstTx.transaction.timestamp))
                val isToday = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(System.currentTimeMillis())) == date
                
                title = if (isToday) "Today" else date
                subtitle = "${groupTransactions.size} transaction${if (groupTransactions.size > 1) "s" else ""}"
                
                // End of day
                calendar.timeInMillis = firstTx.transaction.timestamp
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endTimestamp = calendar.timeInMillis
            }
            TransactionGroupingType.BY_WEEK -> {
                val (year, week) = groupKey.split("-W").map { it.toInt() }
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.WEEK_OF_YEAR, week)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val weekStart = calendar.time
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val weekEnd = calendar.time
                
                title = "WEEK $week"
                subtitle = "${SimpleDateFormat("MMM d", Locale.getDefault()).format(weekStart)} - ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(weekEnd)}"
                endTimestamp = weekEnd.time
            }
        }

        val incomeByCurrency = groupTransactions.filter { it.transaction.amount.signum() >= 0 }
            .groupBy { it.transaction.currency }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) } }
        val incomeBase = CurrencyService.calculateTotalInCurrency(incomeByCurrency, baseCurrency, exchangeRates, precision)

        val expenseByCurrency = groupTransactions.filter { it.transaction.amount.signum() < 0 }
            .groupBy { it.transaction.currency }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) } }
        val expenseBase = CurrencyService.calculateTotalInCurrency(expenseByCurrency, baseCurrency, exchangeRates, precision)

        val netBase = incomeBase.add(expenseBase)

        val closingBalancesByCurrency = allTransactions
            .filter { it.transaction.timestamp <= endTimestamp }
            .groupBy { it.transaction.accountId }
            .mapValues { (_, accountTxs) -> accountTxs.maxBy { it.transaction.timestamp } }
            .values
            .groupBy { it.transaction.currency }
            .mapValues { (_, latestTxs) ->
                latestTxs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.balanceAfter) }
            }

        val totalClosingBalanceBase = CurrencyService.calculateTotalInCurrency(
            closingBalancesByCurrency,
            baseCurrency,
            exchangeRates,
            precision
        )

        val allCurrencies = (incomeByCurrency.keys + expenseByCurrency.keys)
            .distinct()
            .sorted()

        return GroupHeaderData(
            title = title,
            subtitle = subtitle,
            incomeByCurrency = incomeByCurrency,
            expenseByCurrency = expenseByCurrency,
            closingBalancesByCurrency = closingBalancesByCurrency,
            allCurrencies = allCurrencies,
            incomeBase = incomeBase,
            expenseBase = expenseBase,
            netBase = netBase,
            totalClosingBalanceBase = totalClosingBalanceBase
        )
    }
}
