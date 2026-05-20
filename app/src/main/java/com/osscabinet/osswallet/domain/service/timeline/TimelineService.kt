package com.osscabinet.ossWallet.domain.service.timeline

import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.TransactionGroup
import com.osscabinet.ossWallet.domain.model.TransactionUiModel
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
     * Global decimal precision used for timeline computations when not specified.
     */
    var precision: Int = 2

    /**
     * Gets the date range for a "this period" type.
     */
    fun getRangeForThis(
        period: ThisPeriodType,
        offset: Int = 0,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        referenceTime: Long? = null
    ): Pair<Long, Long> {
        val now = if (referenceTime != null) {
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(referenceTime), systemZoneId)
        } else {
            ZonedDateTime.now(systemZoneId)
        }
        val base: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> now.plusDays(offset.toLong())
            ThisPeriodType.WEEK -> now.plusWeeks(offset.toLong())
            ThisPeriodType.MONTH -> now.plusMonths(offset.toLong())
            ThisPeriodType.QUARTER -> now.plusMonths(offset.toLong() * 3)
            ThisPeriodType.HALF_YEAR -> now.plusMonths(offset.toLong() * 6)
            ThisPeriodType.YEAR -> now.plusYears(offset.toLong())
        }

        val start: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> base.toLocalDate().atStartOfDay(base.zone)
            ThisPeriodType.WEEK -> base.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).atStartOfDay(base.zone)
            ThisPeriodType.MONTH -> base.toLocalDate().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(base.zone)
            ThisPeriodType.QUARTER -> {
                val month = base.monthValue
                val firstMonthOfQuarter = ((month - 1) / 3) * 3 + 1
                base.toLocalDate().withMonth(firstMonthOfQuarter).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(base.zone)
            }
            ThisPeriodType.HALF_YEAR -> {
                val firstMonthOfHalf = if (base.monthValue <= 6) 1 else 7
                base.toLocalDate().withMonth(firstMonthOfHalf).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(base.zone)
            }
            ThisPeriodType.YEAR -> base.toLocalDate().with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(base.zone)
        }
        val end: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> base.toLocalDate().atTime(LocalTime.MAX).atZone(base.zone)
            ThisPeriodType.WEEK -> start.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(firstDayOfWeek.plus(6))).atTime(LocalTime.MAX).atZone(base.zone)
            ThisPeriodType.MONTH -> base.toLocalDate().with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(base.zone)
            ThisPeriodType.QUARTER -> {
                val month = base.monthValue
                val lastMonthOfQuarter = ((month - 1) / 3) * 3 + 3
                base.toLocalDate().withMonth(lastMonthOfQuarter).with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(base.zone)
            }
            ThisPeriodType.HALF_YEAR -> {
                val lastMonthOfHalf = if (base.monthValue <= 6) 6 else 12
                base.toLocalDate().withMonth(lastMonthOfHalf).with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(base.zone)
            }
            ThisPeriodType.YEAR -> base.toLocalDate().with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX).atZone(base.zone)
        }
        return Pair(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
    }

    /**
     * Gets the date range for a "last period" type.
     */
    fun getRangeForLast(
        period: LastPeriodType,
        offset: Int = 0,
        referenceTime: Long? = null
    ): Pair<Long, Long> {
        val now = if (referenceTime != null) {
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(referenceTime), systemZoneId)
        } else {
            ZonedDateTime.now(systemZoneId)
        }
        
        // Duration of one period
        val (startDelta, endDelta) = when (period) {
            LastPeriodType.LAST_24H -> Pair(now.minusHours(24), now)
            LastPeriodType.LAST_7_DAYS -> Pair(now.minusDays(7), now)
            LastPeriodType.LAST_30_DAYS -> Pair(now.minusDays(30), now)
            LastPeriodType.LAST_12_WEEKS -> Pair(now.minusWeeks(12), now)
            LastPeriodType.LAST_6_MONTHS -> Pair(now.minusMonths(6), now)
            LastPeriodType.LAST_1_YEAR -> Pair(now.minusYears(1), now)
            LastPeriodType.LAST_5_YEARS -> Pair(now.minusYears(5), now)
        }
        val durationMs = endDelta.toInstant().toEpochMilli() - startDelta.toInstant().toEpochMilli()
        val totalOffsetMs = durationMs * offset

        val finalStart = startDelta.toInstant().toEpochMilli() + totalOffsetMs
        val finalEnd = endDelta.toInstant().toEpochMilli() + totalOffsetMs
        
        return Pair(finalStart, finalEnd)
    }

    /**
     * Extracts the date range from a timeline range.
     */
    fun getDateRange(timelineRange: TimelineRange, referenceTime: Long? = null): Pair<Long?, Long?> {
        return when (timelineRange) {
            is TimelineRange.AllTime -> Pair(null, null)
            is TimelineRange.ThisPeriod -> getRangeForThis(timelineRange.period, timelineRange.offset, referenceTime = referenceTime)
            is TimelineRange.LastPeriod -> getRangeForLast(timelineRange.period, timelineRange.offset, referenceTime = referenceTime)
            is TimelineRange.CustomRange -> Pair(timelineRange.start, timelineRange.end)
        }
    }

    /**
     * Resolves a timeline range into concrete start/end timestamps.
     * If the range is AllTime, it derives boundaries from the transaction history.
     */
    fun resolveBoundRange(
        timelineRange: TimelineRange,
        transactions: List<com.osscabinet.ossWallet.domain.model.Transaction>,
        referenceTime: Long? = null
    ): Pair<Long, Long> {
        val (start, end) = getDateRange(timelineRange, referenceTime)
        if (start != null && end != null) return Pair(start, end)

        val now = referenceTime ?: System.currentTimeMillis()
        if (transactions.isEmpty()) {
            // Default to today's full range if no data
            return getRangeForThis(ThisPeriodType.TODAY, referenceTime = now)
        }

        val minTs = transactions.minOf { it.timestamp }
        val maxTs = maxOf(transactions.maxOf { it.timestamp }, now)

        val zone = systemZoneId
        val resolvedStart = Instant.ofEpochMilli(minTs).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val resolvedEnd = Instant.ofEpochMilli(maxTs).atZone(zone).toLocalDate().atTime(23, 59, 59, 999).atZone(zone).toInstant().toEpochMilli()

        return Pair(resolvedStart, resolvedEnd)
    }

    /**
     * Gets the previous date range for a given timeline range.
     * Legacy method used for comparison metrics.
     */
    fun getPreviousRange(timelineRange: TimelineRange, referenceTime: Long? = null): Pair<Long?, Long?> {
        val prevRange = shiftRange(timelineRange, -1)
        return getDateRange(prevRange, referenceTime = referenceTime)
    }

    /**
     * Gets the shifted timeline range (previous or next).
     */
    fun shiftRange(range: TimelineRange, delta: Int): TimelineRange {
        return when (range) {
            is TimelineRange.ThisPeriod -> range.copy(offset = range.offset + delta)
            is TimelineRange.LastPeriod -> range.copy(offset = range.offset + delta)
            is TimelineRange.CustomRange -> {
                val s = range.start ?: return range
                val e = range.end ?: return range
                val duration = e - s
                TimelineRange.CustomRange(s + duration * delta, e + duration * delta)
            }
            else -> range
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
                date.toString()
            }
        }
        TransactionGroupingType.BY_WEEK -> {
            transactions.groupBy {
                val date = Instant.ofEpochMilli(it.transaction.timestamp).atZone(systemZoneId).toLocalDate()
                val weekOfYear = date.get(weekFields.weekOfWeekBasedYear())
                "${date.year}-W${weekOfYear.toString().padStart(2, '0')}"
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
        absoluteHistory: List<TransactionWithBalance>,
        groupingType: TransactionGroupingType,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = this.precision
    ): GroupHeaderData {
        data class RangeInfo(val title: String, val subtitle: String, val start: Long, val end: Long)
        val info = when (groupingType) {
            TransactionGroupingType.BY_DAY -> {
                val firstTx = groupTransactions.first()
                val date =
                    Instant.ofEpochMilli(firstTx.transaction.timestamp).atZone(systemZoneId)
                        .toLocalDate()
                val today = LocalDate.now(systemZoneId)
                val formattedDate = date.format(dateFormatter)
                val dayLabel = when {
                    date == today -> "Today"
                    date == today.minusDays(1) -> "Yesterday"
                    else -> date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                }
                val startOfDay = date.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
                val endOfDay = date.atTime(23, 59, 59, 999_999_999).atZone(systemZoneId)
                    .toInstant().toEpochMilli()
                RangeInfo(
                    dayLabel,
                    formattedDate,
                    startOfDay,
                    endOfDay
                )
            }

            TransactionGroupingType.BY_WEEK -> {
                val (year, week) = groupKey.split("-W").map { it.toInt() }
                val weekStartDate = LocalDate.now(systemZoneId).withYear(year)
                    .with(weekFields.weekOfWeekBasedYear(), week.toLong())
                    .with(weekFields.dayOfWeek(), 1)
                val weekEndDate = weekStartDate.plusDays(6)
                val startOfWeek = weekStartDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
                val endOfWeek = weekEndDate.atTime(23, 59, 59, 999_999_999).atZone(systemZoneId)
                    .toInstant().toEpochMilli()
                RangeInfo(
                    "WEEK $week",
                    "${weekStartDate.format(shortDateFormatter)} - ${weekEndDate.format(dateFormatter)}",
                    startOfWeek,
                    endOfWeek
                )
            }
        }
        val (title, subtitle, startTimestamp, endTimestamp) = info


        val summaryByCurrency =
            AggregationService.calculateSummaryByCurrency(groupTransactions.map { it.transaction })
        val incomeByCurrency = summaryByCurrency.mapValues { it.value.income }
        val incomeBase = CurrencyService.calculateTotalInCurrency(
            incomeByCurrency,
            baseCurrency,
            exchangeRates,
            precision
        )

        val expenseByCurrency = summaryByCurrency.mapValues { it.value.expense }
        val expenseBase = CurrencyService.calculateTotalInCurrency(
            expenseByCurrency,
            baseCurrency,
            exchangeRates,
            precision
        )

        // Optimized opening balance calculation
        val openingBalancesByCurrency = absoluteHistory
            .asSequence()
            .filter { it.transaction.timestamp < startTimestamp }
            .groupBy { it.transaction.currency }
            .mapValues { (_, txs) ->
                txs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) }
            }

        val totalOpeningBalanceBase = CurrencyService.calculateTotalInCurrency(
            openingBalancesByCurrency,
            baseCurrency,
            exchangeRates,
            precision
        )

        // Optimized closing balance calculation: group by transaction currency and sum amounts
        // This provides a breakdown of all currencies held/used in the current scope
        val closingBalancesByCurrency = absoluteHistory
            .asSequence()
            .filter { it.transaction.timestamp <= endTimestamp }
            .groupBy { it.transaction.currency }
            .mapValues { (_, txs) ->
                txs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.transaction.amount) }
            }

        val totalClosingBalanceBase = CurrencyService.calculateTotalInCurrency(
            closingBalancesByCurrency,
            baseCurrency,
            exchangeRates,
            precision
        )

        return GroupHeaderData(
            title = title,
            subtitle = subtitle,
            incomeByCurrency = incomeByCurrency,
            expenseByCurrency = expenseByCurrency,
            openingBalancesByCurrency = openingBalancesByCurrency,
            closingBalancesByCurrency = closingBalancesByCurrency,
            allCurrencies = (incomeByCurrency.keys + expenseByCurrency.keys + closingBalancesByCurrency.keys + openingBalancesByCurrency.keys).distinct().sorted(),
            incomeBase = incomeBase,
            expenseBase = expenseBase,
            netBase = incomeBase.add(expenseBase),
            totalOpeningBalanceBase = totalOpeningBalanceBase,
            totalClosingBalanceBase = totalClosingBalanceBase
        )
    }

    /**
     * Pre-computes grouped transactions with their corresponding header data and enriched UI models.
     */
    fun getGroupedData(
        transactions: List<TransactionWithBalance>,
        absoluteHistory: List<TransactionWithBalance>,
        groupingType: TransactionGroupingType,
        accounts: List<Account>,
        categories: List<Category>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = this.precision
    ): List<TransactionGroup> {
        val groups = groupTransactions(transactions, groupingType)
        val sortedKeys = groups.keys.sortedDescending()

        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }

        return sortedKeys.map { key ->
            val groupItems = groups[key] ?: emptyList()
            
            val enrichedTransactions = groupItems.map { txWithBalance ->
                val tx = txWithBalance.transaction
                val account = accountMap[tx.accountId]
                val category = categoryMap[tx.categoryId]
                
                val amountInBase = if (tx.currency != baseCurrency) {
                    CurrencyService.convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
                } else tx.amount

                TransactionUiModel(
                    item = txWithBalance,
                    accountName = account?.name ?: "Unknown",
                    accountCurrency = account?.currencyCode ?: tx.currency,
                    categoryName = category?.name ?: "Uncategorized",
                    categoryIcon = category?.iconName,
                    categoryColor = category?.colorHex ?: 0,
                    amountInBase = amountInBase
                )
            }

            TransactionGroup(
                key = key,
                headerData = getHeaderData(
                    groupKey = key,
                    groupTransactions = groupItems,
                    absoluteHistory = absoluteHistory,
                    groupingType = groupingType,
                    baseCurrency = baseCurrency,
                    exchangeRates = exchangeRates,
                    precision = precision
                ),
                transactions = enrichedTransactions
            )
        }
    }

    data class GroupHeaderData(
        val title: String,
        val subtitle: String,
        val incomeByCurrency: Map<String, BigDecimal>,
        val expenseByCurrency: Map<String, BigDecimal>,
        val openingBalancesByCurrency: Map<String, BigDecimal>,
        val closingBalancesByCurrency: Map<String, BigDecimal>,
        val allCurrencies: List<String>,
        val incomeBase: BigDecimal,
        val expenseBase: BigDecimal,
        val netBase: BigDecimal,
        val totalOpeningBalanceBase: BigDecimal,
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
