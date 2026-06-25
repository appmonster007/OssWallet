package com.osscabinet.ossWallet.domain.service.timeline

import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel
import com.osscabinet.ossWallet.domain.model.PlannedPaymentUiModel
import com.osscabinet.ossWallet.domain.model.RecordUiModel
import com.osscabinet.ossWallet.domain.model.TransactionGroup
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
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
 * strictly follows DDD: No UI dependencies, focuses on business calculations.
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
        val baseRange = when (timelineRange) {
            is TimelineRange.AllTime -> Pair(null, null)
            is TimelineRange.ThisPeriod -> getRangeForThis(
                timelineRange.period,
                timelineRange.offset,
                referenceTime = referenceTime
            )

            is TimelineRange.LastPeriod -> getRangeForLast(
                timelineRange.period,
                timelineRange.offset,
                referenceTime = referenceTime
            )

            is TimelineRange.CustomRange -> Pair(timelineRange.start, timelineRange.end)
        }

        if (!timelineRange.excludeFuture) return baseRange

        val now = referenceTime ?: System.currentTimeMillis()
        val start = baseRange.first
        val end = baseRange.second

        val constrainedEnd = if (end != null) minOf(end, now) else now

        return Pair(start, constrainedEnd)
    }

    /**
     * Resolves a timeline range into concrete start/end timestamps.
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
            return getRangeForThis(ThisPeriodType.TODAY, referenceTime = now)
        }

        val minTs = transactions.minOf { it.timestamp }
        val maxTs = maxOf(transactions.maxOf { it.timestamp }, now)

        val zone = systemZoneId
        val resolvedStart = Instant.ofEpochMilli(minTs).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val resolvedEnd = Instant.ofEpochMilli(maxTs).atZone(zone).toLocalDate().atTime(23, 59, 59, 999).atZone(zone).toInstant().toEpochMilli()

        return Pair(resolvedStart, resolvedEnd)
    }

    fun getPreviousRange(timelineRange: TimelineRange, referenceTime: Long? = null): Pair<Long?, Long?> {
        val prevRange = shiftRange(timelineRange, -1)
        return getDateRange(prevRange, referenceTime = referenceTime)
    }

    fun shiftRange(range: TimelineRange, delta: Int): TimelineRange {
        return when (range) {
            is TimelineRange.ThisPeriod -> range.copy(offset = range.offset + delta)
            is TimelineRange.LastPeriod -> range.copy(offset = range.offset + delta)
            is TimelineRange.CustomRange -> {
                val s = range.start ?: return range
                val e = range.end ?: return range
                val duration = e - s
                TimelineRange.CustomRange(
                    start = s + duration * delta,
                    end = e + duration * delta,
                    excludeFuture = range.excludeFuture
                )
            }

            is TimelineRange.AllTime -> range
        }
    }

    fun <T : com.osscabinet.ossWallet.domain.model.Timestamped> filterByTimelineRange(
        items: List<T>,
        timelineRange: TimelineRange
    ): List<T> {
        val (start, end) = getDateRange(timelineRange)
        return if (start == null || end == null) items else items.filter { it.timestamp in start..end }
    }

    // ==================== Grouping Methods ====================

    private fun getGroupKey(timestamp: Long, groupingType: TransactionGroupingType): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(systemZoneId).toLocalDate()
        return when (groupingType) {
            TransactionGroupingType.BY_DAY -> date.toString()
            TransactionGroupingType.BY_WEEK -> {
                val weekOfYear = date.get(weekFields.weekOfWeekBasedYear())
                "${date.year}-W${weekOfYear.toString().padStart(2, '0')}"
            }
        }
    }

    /**
     * Enriches a flat list of transactions into domain UI models.
     * Pure DDD: No formatting strings.
     */
    fun enrichTransactions(
        transactions: List<com.osscabinet.ossWallet.domain.model.Transaction>,
        accounts: List<Account>,
        categories: List<Category>,
        allTags: List<com.osscabinet.ossWallet.domain.model.Tag>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int = this.precision
    ): List<ConfirmedTransactionUiModel> {
        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }
        val tagMap = allTags.associateBy { it.name }
        val tagUiModelCache = mutableMapOf<String, com.osscabinet.ossWallet.domain.model.TagUiModel>()

        return transactions.map { tx ->
            val amountInBase = if (tx.currency != baseCurrency) {
                CurrencyService.convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
            } else tx.amount

            ConfirmedTransactionUiModel(
                item = TransactionWithBalance(tx, BigDecimal.ZERO, BigDecimal.ZERO),
                accountName = accountMap[tx.accountId]?.name ?: "Unknown",
                accountCurrency = accountMap[tx.accountId]?.currencyCode ?: tx.currency,
                categoryName = categoryMap[tx.categoryId]?.name ?: "Uncategorized",
                categoryIcon = categoryMap[tx.categoryId]?.iconName,
                categoryColor = categoryMap[tx.categoryId]?.colorHex ?: 0,
                tags = tx.tags.map { name -> tagUiModelCache.getOrPut(name) { com.osscabinet.ossWallet.domain.model.TagUiModel(name, tagMap[name]?.colorHex ?: -1) } },
                amountInBase = amountInBase,
                linkedTransactionIds = emptyList()
            )
        }
    }

    fun formatMonth(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp).atZone(systemZoneId).format(monthFormatter)
    }

    /**
     * Optimized O(N) grouping and header calculation.
     * Processes history in a single pass to derive running balances.
     */
    fun getGroupedData(
        transactions: List<TransactionWithBalance>,
        absoluteHistory: List<TransactionWithBalance>,
        groupingType: TransactionGroupingType,
        accounts: List<Account>,
        categories: List<Category>,
        allTags: List<com.osscabinet.ossWallet.domain.model.Tag> = emptyList(),
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        plannedPayments: List<UpcomingPaymentItem> = emptyList(),
        linkedTransactionIds: Map<Long, List<Long>> = emptyMap(),
        initialBalances: Map<String, BigDecimal> = emptyMap(),
        precision: Int = this.precision,
        allTxnBreakdown: Boolean = true,
    ): List<TransactionGroup> {
        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }
        val tagMap = allTags.associateBy { it.name }
        val tagUiModelCache = mutableMapOf<String, com.osscabinet.ossWallet.domain.model.TagUiModel>()

        // 1. Enrich Records (O(N))
        val enrichedTransactions = transactions.map { txWithBalance ->
            val tx = txWithBalance.transaction
            val amountInBase = if (tx.currency != baseCurrency) {
                CurrencyService.convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
            } else tx.amount

            ConfirmedTransactionUiModel(
                item = txWithBalance,
                accountName = accountMap[tx.accountId]?.name ?: "Unknown",
                accountCurrency = accountMap[tx.accountId]?.currencyCode ?: tx.currency,
                categoryName = categoryMap[tx.categoryId]?.name ?: "Uncategorized",
                categoryIcon = categoryMap[tx.categoryId]?.iconName,
                categoryColor = categoryMap[tx.categoryId]?.colorHex ?: 0,
                tags = tx.tags.map { name -> tagUiModelCache.getOrPut(name) { com.osscabinet.ossWallet.domain.model.TagUiModel(name, tagMap[name]?.colorHex ?: -1) } },
                amountInBase = amountInBase,
                linkedTransactionIds = linkedTransactionIds[tx.id] ?: emptyList()
            )
        }

        val enrichedPlanned = plannedPayments.map { item ->
            val pp = item.plannedPayment
            val amountInBase = if (pp.currency != baseCurrency) {
                CurrencyService.convertAmount(pp.amount, pp.currency, baseCurrency, exchangeRates, precision)
            } else pp.amount

            PlannedPaymentUiModel(
                paymentId = pp.id, occurrenceId = item.occurrenceId, timestamp = item.scheduledDate,
                amount = pp.amount, currency = pp.currency,
                categoryName = categoryMap[pp.categoryId]?.name ?: "Uncategorized",
                categoryIcon = categoryMap[pp.categoryId]?.iconName,
                categoryColor = categoryMap[pp.categoryId]?.colorHex ?: 0,
                accountName = accountMap[pp.accountId]?.name ?: "Unknown",
                accountCurrency = accountMap[pp.accountId]?.currencyCode ?: pp.currency,
                note = pp.note, tags = pp.tags.map { name -> tagUiModelCache.getOrPut(name) { com.osscabinet.ossWallet.domain.model.TagUiModel(name, tagMap[name]?.colorHex ?: -1) } },
                amountInBase = amountInBase, isOverdue = item.isOverdue
            )
        }

        val allItems: List<RecordUiModel> = (enrichedTransactions + enrichedPlanned).sortedByDescending { it.timestamp }
        val groupsMap = allItems.groupBy { getGroupKey(it.timestamp, groupingType) }

        // 2. Optimized O(N) Balance Calculation for Headers using Anchor & Delta
        val sortedHistory = absoluteHistory.sortedBy { it.transaction.timestamp }
        val sortedGroupKeys = groupsMap.keys.sorted()
        
        val openingBalancesMap = mutableMapOf<String, Map<String, BigDecimal>>()
        val closingBalancesMap = mutableMapOf<String, Map<String, BigDecimal>>()
        val currentAccountBalances = accounts.associate { it.id to (initialBalances[it.id] ?: BigDecimal.ZERO) }.toMutableMap()

        fun captureBalances(): Map<String, BigDecimal> {
            val balancesByCurrency = mutableMapOf<String, BigDecimal>()
            currentAccountBalances.forEach { (accId, balance) ->
                val account = accountMap[accId]
                val currency = account?.currencyCode ?: baseCurrency
                balancesByCurrency[currency] = (balancesByCurrency[currency] ?: BigDecimal.ZERO).add(balance)
            }
            return balancesByCurrency
        }

        var historyIdx = 0
        sortedGroupKeys.forEach { key ->
            openingBalancesMap[key] = captureBalances()
            
            while (historyIdx < sortedHistory.size) {
                val txWithBalance = sortedHistory[historyIdx]
                val txKey = getGroupKey(txWithBalance.transaction.timestamp, groupingType)
                if (txKey > key) break
                
                txWithBalance.transaction.accountId?.let { accId ->
                    currentAccountBalances[accId] = txWithBalance.balanceAfter
                }
                historyIdx++
            }
            
            closingBalancesMap[key] = captureBalances()
        }

        // 3. Final Group Construction
        return groupsMap.keys.sortedDescending().map { key ->
            val groupItems = groupsMap[key] ?: emptyList()
            val opening = openingBalancesMap[key] ?: emptyMap()
            val closing = closingBalancesMap[key] ?: emptyMap()
            
            val summaryByCurrency = AggregationService.calculateSummaryByCurrency(groupItems.filterIsInstance<ConfirmedTransactionUiModel>().map { it.item.transaction })
            val incomeByCurrency = summaryByCurrency.mapValues { it.value.income }
            val expenseByCurrency = summaryByCurrency.mapValues { it.value.expense }
            
            val incomeBase = CurrencyService.calculateTotalInCurrency(incomeByCurrency, baseCurrency, exchangeRates, precision)
            val expenseBase = CurrencyService.calculateTotalInCurrency(expenseByCurrency, baseCurrency, exchangeRates, precision)
            val totalOpeningBase = CurrencyService.calculateTotalInCurrency(opening, baseCurrency, exchangeRates, precision)
            val totalClosingBase = CurrencyService.calculateTotalInCurrency(closing, baseCurrency, exchangeRates, precision)

            TransactionGroup(
                key = key,
                headerData = GroupHeaderData(
                    title = formatGroupTitle(key, groupingType),
                    subtitle = formatGroupSubtitle(key, groupingType),
                    incomeByCurrency = incomeByCurrency,
                    expenseByCurrency = expenseByCurrency,
                    openingBalancesByCurrency = opening,
                    closingBalancesByCurrency = closing,
                    allCurrencies = (incomeByCurrency.keys + expenseByCurrency.keys + opening.keys + closing.keys).distinct().sorted(),
                    incomeBase = incomeBase,
                    expenseBase = expenseBase,
                    netBase = incomeBase.add(expenseBase),
                    totalOpeningBalanceBase = totalOpeningBase,
                    totalClosingBalanceBase = totalClosingBase
                ),
                transactions = groupItems
            )
        }
    }

    private fun formatGroupTitle(key: String, groupingType: TransactionGroupingType): String {
        return when (groupingType) {
            TransactionGroupingType.BY_DAY -> {
                val date = LocalDate.parse(key)
                val today = LocalDate.now(systemZoneId)
                when {
                    date == today -> "Today"
                    date == today.minusDays(1) -> "Yesterday"
                    else -> date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                }
            }
            TransactionGroupingType.BY_WEEK -> "WEEK ${key.split("-W")[1]}"
        }
    }

    private fun formatGroupSubtitle(key: String, groupingType: TransactionGroupingType): String {
        return when (groupingType) {
            TransactionGroupingType.BY_DAY -> LocalDate.parse(key).format(dateFormatter)
            TransactionGroupingType.BY_WEEK -> {
                val parts = key.split("-W")
                val year = parts[0].toInt()
                val week = parts[1].toInt()
                val start = LocalDate.now(systemZoneId).withYear(year).with(weekFields.weekOfWeekBasedYear(), week.toLong()).with(weekFields.dayOfWeek(), 1)
                "${start.format(shortDateFormatter)} - ${start.plusDays(6).format(dateFormatter)}"
            }
        }
    }

    @androidx.compose.runtime.Immutable
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
        val totalClosingBalanceBase: BigDecimal,
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
