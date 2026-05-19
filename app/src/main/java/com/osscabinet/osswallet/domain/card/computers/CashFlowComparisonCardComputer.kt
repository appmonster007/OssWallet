package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.CashFlowComparisonGranularity
import com.osscabinet.ossWallet.domain.card.CashFlowComparisonSeries
import com.osscabinet.ossWallet.domain.card.CashFlowTimePoint
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CashFlowComparisonCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<CashFlowComparisonCardConfig, CardRenderData.CashFlowComparison> {

    override val supportedType = CardType.CASHFLOW_COMPARISON

    private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM")
    private val monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy")

    override suspend fun compute(
        input: CardInput,
        config: CashFlowComparisonCardConfig
    ): CardRenderData.CashFlowComparison {
        val mergedFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val (currentStart, currentEnd) = TimelineService.resolveBoundRange(mergedFilter.timelineRange, input.transactions)

        val spanDays = (currentEnd - currentStart) / 86_400_000L
        val granularity = when {
            spanDays <= 182L -> CashFlowComparisonGranularity.DAY   // ≤ ~6 months → daily
            spanDays <= 730L -> CashFlowComparisonGranularity.WEEK  // ≤ ~2 years  → weekly
            else             -> CashFlowComparisonGranularity.MONTH // > 2 years   → monthly
        }

        val zone = TimelineService.systemZoneId
        val today = System.currentTimeMillis()
        // Use full-period buckets so the current line spans the same visual width as prev/last-year.
        // Future buckets return zeros; past/current buckets are capped at today.
        val fullBuckets = generateBuckets(currentStart, currentEnd, granularity)
        val currentSeries = computeSeriesFromBucketsWithFutureCap(
            buckets = fullBuckets,
            baseFilter = mergedFilter,
            profileId = input.profileId,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            today = today
        )

        // Previous period — cap at today so we don't show future zero-lines
        val (prevStart, prevEnd) = TimelineService.getPreviousRange(mergedFilter.timelineRange)
        var prevTotalBuckets = 0
        val previousSeries = if (prevStart != null && prevEnd != null) {
            val prevBuckets = generateBuckets(prevStart, prevEnd, granularity)
            prevTotalBuckets = prevBuckets.size
            computeSeriesFromBucketsWithFutureCap(
                buckets = prevBuckets,
                baseFilter = mergedFilter,
                profileId = input.profileId,
                baseCurrency = input.baseCurrency,
                exchangeRates = input.exchangeRates,
                today = today
            )
        } else CashFlowComparisonSeries(emptyList())

        // Same period last year — cap at today (unlikely to trigger unless current range is far in future)
        val lastYearStart = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentStart), zone)
            .minusYears(1).toInstant().toEpochMilli()
        val lastYearEnd = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentEnd), zone)
            .minusYears(1).toInstant().toEpochMilli()
        val lastYearBuckets = generateBuckets(lastYearStart, lastYearEnd, granularity)
        val lastYearSeries = computeSeriesFromBucketsWithFutureCap(
            buckets = lastYearBuckets,
            baseFilter = mergedFilter,
            profileId = input.profileId,
            baseCurrency = input.baseCurrency,
            exchangeRates = input.exchangeRates,
            today = today
        )

        return CardRenderData.CashFlowComparison(
            config = config,
            currentSeries = currentSeries,
            previousSeries = previousSeries,
            lastYearSeries = lastYearSeries,
            baseCurrency = input.baseCurrency,
            granularity = granularity,
            periodLabels = fullBuckets.map { it.third },
            currentTotalBuckets = fullBuckets.size,
            previousTotalBuckets = prevTotalBuckets,
            lastYearTotalBuckets = lastYearBuckets.size
        )
    }

    // Generates a list of (bucketStart, bucketEnd, label) for all buckets covering [rangeStart, rangeEnd].
    private fun generateBuckets(
        rangeStart: Long,
        rangeEnd: Long,
        granularity: CashFlowComparisonGranularity
    ): List<Triple<Long, Long, String>> {
        val zone = TimelineService.systemZoneId
        val buckets = mutableListOf<Triple<Long, Long, String>>()

        when (granularity) {
            CashFlowComparisonGranularity.DAY -> {
                var day = LocalDate.ofInstant(Instant.ofEpochMilli(rangeStart), zone)
                val lastDay = LocalDate.ofInstant(Instant.ofEpochMilli(rangeEnd), zone)
                while (!day.isAfter(lastDay)) {
                    val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = day.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    buckets.add(Triple(start, end, day.format(dayFormatter)))
                    day = day.plusDays(1)
                }
            }
            CashFlowComparisonGranularity.WEEK -> {
                var weekStart = LocalDate.ofInstant(Instant.ofEpochMilli(rangeStart), zone)
                    .with(DayOfWeek.MONDAY)
                    .let { if (it.atStartOfDay(zone).toInstant().toEpochMilli() > rangeStart) it.minusWeeks(1) else it }
                val rangeEndDate = LocalDate.ofInstant(Instant.ofEpochMilli(rangeEnd), zone)
                while (!weekStart.isAfter(rangeEndDate)) {
                    val weekEnd = weekStart.plusDays(6)
                    val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = weekEnd.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    buckets.add(Triple(start, end, weekStart.format(dayFormatter)))
                    weekStart = weekStart.plusWeeks(1)
                }
            }
            CashFlowComparisonGranularity.MONTH -> {
                var month = LocalDate.ofInstant(Instant.ofEpochMilli(rangeStart), zone).withDayOfMonth(1)
                val lastMonth = LocalDate.ofInstant(Instant.ofEpochMilli(rangeEnd), zone).withDayOfMonth(1)
                while (!month.isAfter(lastMonth)) {
                    val start = month.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = month.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    buckets.add(Triple(start, end, month.format(monthFormatter)))
                    month = month.plusMonths(1)
                }
            }
        }

        return buckets
    }

    // Current / Previous / Last Year series: drop future buckets entirely so the line terminates naturally at today.
    // The bucket straddling today is capped at today; all fully-future buckets are skipped.
    private suspend fun computeSeriesFromBucketsWithFutureCap(
        buckets: List<Triple<Long, Long, String>>,
        baseFilter: TransactionFilter,
        profileId: Int,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        today: Long
    ): CashFlowComparisonSeries {
        val points = mutableListOf<CashFlowTimePoint>()
        for ((bucketStart, bucketEnd, label) in buckets) {
            if (bucketStart > today) break
            val effectiveEnd = minOf(bucketEnd, today)
            points.add(queryBucket(baseFilter, profileId, bucketStart, effectiveEnd, label, baseCurrency, exchangeRates))
        }
        return CashFlowComparisonSeries(points)
    }

    private suspend fun queryBucket(
        baseFilter: TransactionFilter,
        profileId: Int,
        bucketStart: Long,
        bucketEnd: Long,
        label: String,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): CashFlowTimePoint {
        val summaries = TransactionAggregationService.getSummaryByCurrency(
            queryService, profileId,
            baseFilter.copy(timelineRange = TimelineRange.CustomRange(bucketStart, bucketEnd))
        ).getOrDefault(emptyMap())

        var totalIncome = BigDecimal.ZERO
        var totalExpense = BigDecimal.ZERO
        summaries.forEach { (currency, summary) ->
            totalIncome += CurrencyService.convertAmount(summary.income, currency, baseCurrency, exchangeRates, 10)
            totalExpense += CurrencyService.convertAmount(summary.expense.abs(), currency, baseCurrency, exchangeRates, 10)
        }
        return CashFlowTimePoint(
            label = label,
            epochMillis = bucketStart,
            income = totalIncome.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            expense = totalExpense.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            net = totalIncome.subtract(totalExpense).setScale(CurrencyService.precision, RoundingMode.HALF_UP)
        )
    }
}
