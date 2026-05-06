package com.osscabinet.ossWallet.domain.service.timeline

import com.osscabinet.ossWallet.data.local.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.financial.FinancialMetrics
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Domain service for timeline, grouping, and filtering business logic.
 */
object TimelineService {

    val systemZoneId: ZoneId = ZoneId.systemDefault()
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val weekFields = WeekFields.of(Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")

    /**
     * Gets the date range for a "this period" type.
     */
    fun getRangeForThis(
        period: ThisPeriodType,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
    ): Pair<Long, Long> {
        val now = ZonedDateTime.now(systemZoneId)
        val start: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> now.toLocalDate().atStartOfDay(now.zone)
            ThisPeriodType.WEEK -> now.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).atStartOfDay(now.zone)
            ThisPeriodType.MONTH -> now.toLocalDate().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(now.zone)
            ThisPeriodType.QUARTER -> {
                val month = now.monthValue
                val firstMonthOfQuarter = ((month - 1) / 3) * 3 + 1
                now.toLocalDate().withMonth(firstMonthOfQuarter).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(now.zone)
            }
            ThisPeriodType.HALF_YEAR -> {
                val firstMonthOfHalf = if (now.monthValue <= 6) 1 else 7
                now.toLocalDate().withMonth(firstMonthOfHalf).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(now.zone)
            }
            ThisPeriodType.YEAR -> now.toLocalDate().with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(now.zone)
        }
        val end: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> now.toLocalDate().atTime(LocalTime.MAX).atZone(now.zone)
            ThisPeriodType.WEEK -> now.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(firstDayOfWeek.plus(6))).atTime(LocalTime.MAX).atZone(now.zone)
            ThisPeriodType.MONTH -> now.toLocalDate().with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(now.zone)
            ThisPeriodType.QUARTER -> {
                val month = now.monthValue
                val lastMonthOfQuarter = ((month - 1) / 3) * 3 + 3
                now.toLocalDate().withMonth(lastMonthOfQuarter).with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(now.zone)
            }
            ThisPeriodType.HALF_YEAR -> {
                val lastMonthOfHalf = if (now.monthValue <= 6) 6 else 12
                now.toLocalDate().withMonth(lastMonthOfHalf).with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(now.zone)
            }
            ThisPeriodType.YEAR -> now.toLocalDate().with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX).atZone(now.zone)
        }
        return Pair(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
    }

    /**
     * Gets the date range for a "last period" type.
     */
    fun getRangeForLast(period: LastPeriodType): Pair<Long, Long> {
        val now = ZonedDateTime.now(systemZoneId)
        val start: ZonedDateTime = when (period) {
            LastPeriodType.LAST_24H -> now.minusHours(24)
            LastPeriodType.LAST_7_DAYS -> now.minusDays(7)
            LastPeriodType.LAST_30_DAYS -> now.minusDays(30)
            LastPeriodType.LAST_12_WEEKS -> now.minusWeeks(12)
            LastPeriodType.LAST_6_MONTHS -> now.minusMonths(6)
            LastPeriodType.LAST_1_YEAR -> now.minusYears(1)
            LastPeriodType.LAST_5_YEARS -> now.minusYears(5)
        }
        return Pair(start.toInstant().toEpochMilli(), now.toInstant().toEpochMilli())
    }

    /**
     * Extracts the date range from a timeline range.
     */
    fun getDateRange(timelineRange: TimelineRange): Pair<Long?, Long?> {
        return when (timelineRange) {
            is TimelineRange.AllTime -> Pair(null, null)
            is TimelineRange.ThisPeriod -> getRangeForThis(timelineRange.period)
            is TimelineRange.LastPeriod -> getRangeForLast(timelineRange.period)
            is TimelineRange.CustomRange -> Pair(timelineRange.start, timelineRange.end)
        }
    }

    /**
     * Gets the previous date range for a given timeline range.
     */
    fun getPreviousRange(timelineRange: TimelineRange): Pair<Long?, Long?> {
        val (currentStart, currentEnd) = getDateRange(timelineRange)
        if (currentStart == null || currentEnd == null) return Pair(null, null)

        return when (timelineRange) {
            is TimelineRange.ThisPeriod -> {
                val start = Instant.ofEpochMilli(currentStart).atZone(systemZoneId)
                val prevStart: ZonedDateTime = when (timelineRange.period) {
                    ThisPeriodType.TODAY -> start.minusDays(1)
                    ThisPeriodType.WEEK -> start.minusWeeks(1)
                    ThisPeriodType.MONTH -> start.minusMonths(1)
                    ThisPeriodType.QUARTER -> start.minusMonths(3)
                    ThisPeriodType.HALF_YEAR -> start.minusMonths(6)
                    ThisPeriodType.YEAR -> start.minusYears(1)
                }
                val prevEnd: ZonedDateTime = when (timelineRange.period) {
                    ThisPeriodType.TODAY -> prevStart.with(LocalTime.MAX)
                    ThisPeriodType.WEEK -> prevStart.plusDays(6).with(LocalTime.MAX)
                    ThisPeriodType.MONTH -> prevStart.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
                    ThisPeriodType.QUARTER -> prevStart.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
                    ThisPeriodType.HALF_YEAR -> prevStart.plusMonths(5).with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)
                    ThisPeriodType.YEAR -> prevStart.with(TemporalAdjusters.lastDayOfYear()).with(LocalTime.MAX)
                }
                Pair(prevStart.toInstant().toEpochMilli(), prevEnd.toInstant().toEpochMilli())
            }
            else -> {
                val duration = currentEnd - currentStart
                Pair(currentStart - duration, currentStart)
            }
        }
    }

    /**
     * Filters a list of timestamped items by a timeline range.
     */
    fun <T : com.osscabinet.ossWallet.domain.model.Timestamped> filterByTimelineRange(
        items: List<T>,
        timelineRange: TimelineRange
    ): List<T> {
        val (start, end) = getDateRange(timelineRange)
        return if (start == null || end == null) items else items.filter { it.timestamp in start..end }
    }

    // ==================== Grouping Methods ====================

    fun groupTransactions(
        transactions: List<TransactionWithBalance>,
        groupingType: TransactionGroupingType
    ): Map<String, List<TransactionWithBalance>> = when (groupingType) {
        TransactionGroupingType.BY_DAY -> {
            transactions.groupBy {
                val date = Instant.ofEpochMilli(it.transaction.timestamp).atZone(systemZoneId).toLocalDate()
                "${date.year}-${date.monthValue}-${date.dayOfMonth}"
            }
        }
        TransactionGroupingType.BY_WEEK -> {
            transactions.groupBy {
                val date = Instant.ofEpochMilli(it.transaction.timestamp).atZone(systemZoneId).toLocalDate()
                val weekOfYear = date.get(weekFields.weekOfWeekBasedYear())
                "${date.year}-W$weekOfYear"
            }
        }
    }

    /**
     * Formats a timestamp as a month string (yyyy-MM).
     */
    fun formatMonth(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp).atZone(systemZoneId).format(monthFormatter)
    }

    /**
     * Converts a timestamp to a date string (yyyy-MM-dd).
     */
    fun formatDate(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp).atZone(systemZoneId).toLocalDate().toString()
    }

    /**
     * Generates a list of time buckets (start/end pairs) for a given range.
     */
    fun getTimeBuckets(timelineRange: TimelineRange, numPoints: Int = 30): List<TimeBucket> {
        val (start, end) = getDateRange(timelineRange)
        val actualStart = start ?: 0L
        val actualEnd = end ?: System.currentTimeMillis()
        val duration = actualEnd - actualStart
        val step = if (numPoints > 0) duration / numPoints else 0L

        return if (step > 0) {
            (0 until numPoints).map { i ->
                TimeBucket(actualStart + (i * step), actualStart + ((i + 1) * step))
            }
        } else listOf(TimeBucket(actualStart, actualEnd))
    }

    /**
     * Calculates a cumulative sum (running total) across a list of financial metrics.
     */
    fun <K> calculateCumulativeSum(
        items: List<FinancialMetrics<K>>,
        initialBalance: BigDecimal = BigDecimal.ZERO
    ): List<TrajectoryPoint<K>> {
        var currentBalance = initialBalance
        return items.map { item ->
            currentBalance += item.totalAmount
            TrajectoryPoint(
                key = item.key,
                income = item.income,
                expense = item.expense,
                totalAmount = item.totalAmount,
                cumulativeBalance = currentBalance,
                transactionCount = item.transactionCount
            )
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
        val (title, subtitle, endTimestamp) = when (groupingType) {
            TransactionGroupingType.BY_DAY -> {
                val firstTx = groupTransactions.first()
                val date = Instant.ofEpochMilli(firstTx.transaction.timestamp).atZone(systemZoneId).toLocalDate()
                val formattedDate = date.format(dateFormatter)
                val isToday = formattedDate == LocalDate.now(systemZoneId).format(dateFormatter)
                val titleText = if (isToday) "Today" else formattedDate
                val endOfDay = date.atTime(23, 59, 59, 999_999_999).atZone(systemZoneId).toInstant().toEpochMilli()
                Triple(titleText, "${groupTransactions.size} transaction${if (groupTransactions.size > 1) "s" else ""}", endOfDay)
            }
            TransactionGroupingType.BY_WEEK -> {
                val (year, week) = groupKey.split("-W").map { it.toInt() }
                val weekStartDate = LocalDate.now(systemZoneId).withYear(year).with(weekFields.weekOfWeekBasedYear(), week.toLong()).with(weekFields.dayOfWeek(), 1)
                val weekEndDate = weekStartDate.plusDays(6)
                val endOfWeek = weekEndDate.atTime(23, 59, 59, 999_999_999).atZone(systemZoneId).toInstant().toEpochMilli()
                Triple("WEEK $week", "${weekStartDate.format(shortDateFormatter)} - ${weekEndDate.format(dateFormatter)}", endOfWeek)
            }
        }

        val summaryByCurrency = AggregationService.calculateSummaryByCurrency(groupTransactions.map { it.transaction })
        val incomeByCurrency = summaryByCurrency.mapValues { it.value.income }
        val incomeBase = CurrencyService.calculateTotalInCurrency(incomeByCurrency, baseCurrency, exchangeRates, precision)

        val expenseByCurrency = summaryByCurrency.mapValues { it.value.expense }
        val expenseBase = CurrencyService.calculateTotalInCurrency(expenseByCurrency, baseCurrency, exchangeRates, precision)

        val closingBalancesByCurrency = allTransactions
            .filter { it.transaction.timestamp <= endTimestamp }
            .groupBy { it.transaction.accountId }
            .mapValues { (_, txs) -> txs.maxBy { it.transaction.timestamp } }
            .values
            .groupBy { it.transaction.currency }
            .mapValues { it.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.balanceAfter) } }

        val totalClosingBalanceBase = CurrencyService.calculateTotalInCurrency(closingBalancesByCurrency, baseCurrency, exchangeRates, precision)

        return GroupHeaderData(
            title = title,
            subtitle = subtitle,
            incomeByCurrency = incomeByCurrency,
            expenseByCurrency = expenseByCurrency,
            closingBalancesByCurrency = closingBalancesByCurrency,
            allCurrencies = (incomeByCurrency.keys + expenseByCurrency.keys).distinct().sorted(),
            incomeBase = incomeBase,
            expenseBase = expenseBase,
            netBase = incomeBase.add(expenseBase),
            totalClosingBalanceBase = totalClosingBalanceBase
        )
    }

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
}

data class TimeBucket(val start: Long, val end: Long)

data class TrajectoryPoint<K>(
    val key: K,
    val income: BigDecimal,
    val expense: BigDecimal,
    val totalAmount: BigDecimal,
    val cumulativeBalance: BigDecimal,
    val transactionCount: Int
)
