package com.osscabinet.ossWallet.domain.service.timeline

import com.osscabinet.ossWallet.data.local.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

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

    private val systemZoneId: ZoneId = ZoneId.systemDefault()
    private val weekFields = WeekFields.of(java.util.Locale.getDefault())

    fun groupTransactions(
        transactions: List<TransactionWithBalance>,
        groupingType: TransactionGroupingType
    ): Map<String, List<TransactionWithBalance>> = when (groupingType) {
        TransactionGroupingType.BY_DAY -> {
            transactions.groupBy {
                val date = Instant.ofEpochMilli(it.transaction.timestamp)
                    .atZone(systemZoneId)
                    .toLocalDate()
                "${date.year}-${date.monthValue}-${date.dayOfMonth}"
            }
        }

        TransactionGroupingType.BY_WEEK -> {
            transactions.groupBy {
                val date = Instant.ofEpochMilli(it.transaction.timestamp)
                    .atZone(systemZoneId)
                    .toLocalDate()
                val weekOfYear = date.get(weekFields.weekOfWeekBasedYear())
                "${date.year}-W$weekOfYear"
            }
        }
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")

    fun getHeaderData(
        groupKey: String,
        groupTransactions: List<TransactionWithBalance>,
        allTransactions: List<TransactionWithBalance>,
        groupingType: TransactionGroupingType,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): GroupHeaderData {
        val (title, subtitle, endTimestamp) = when (groupingType) {
            TransactionGroupingType.BY_DAY -> {
                val firstTx = groupTransactions.first()
                val date = Instant.ofEpochMilli(firstTx.transaction.timestamp)
                    .atZone(systemZoneId)
                    .toLocalDate()
                val formattedDate = date.format(dateFormatter)
                val today = LocalDate.now(systemZoneId).format(dateFormatter)
                val isToday = formattedDate == today

                val titleText = if (isToday) "Today" else formattedDate
                val subtitleText =
                    "${groupTransactions.size} transaction${if (groupTransactions.size > 1) "s" else ""}"

                // End of day timestamp
                val endOfDay = date.atTime(23, 59, 59, 999_999_999)
                    .atZone(systemZoneId)
                    .toInstant()
                    .toEpochMilli()

                Triple(titleText, subtitleText, endOfDay)
            }

            TransactionGroupingType.BY_WEEK -> {
                val (year, week) = groupKey.split("-W").map { it.toInt() }

                // Calculate week start (Monday of the given week)
                val weekStartDate = LocalDate.now(systemZoneId)
                    .withYear(year)
                    .with(weekFields.weekOfWeekBasedYear(), week.toLong())
                    .with(weekFields.dayOfWeek(), 1) // Monday

                val weekEndDate = weekStartDate.plusDays(6)

                val titleText = "WEEK $week"
                val subtitleText = "${weekStartDate.format(shortDateFormatter)} - ${
                    weekEndDate.format(dateFormatter)
                }"

                val endOfWeek = weekEndDate.atTime(23, 59, 59, 999_999_999)
                    .atZone(systemZoneId)
                    .toInstant()
                    .toEpochMilli()

                Triple(titleText, subtitleText, endOfWeek)
            }
        }

        val incomeByCurrency = groupTransactions.filter { it.transaction.amount.signum() >= 0 }
            .groupBy { it.transaction.currency }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) } }
        val incomeBase = CurrencyService.calculateTotalInCurrency(
            incomeByCurrency,
            baseCurrency,
            exchangeRates,
            precision
        )

        val expenseByCurrency = groupTransactions.filter { it.transaction.amount.signum() < 0 }
            .groupBy { it.transaction.currency }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) } }
        val expenseBase = CurrencyService.calculateTotalInCurrency(
            expenseByCurrency,
            baseCurrency,
            exchangeRates,
            precision
        )

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
