package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardComputerUtils
import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.CashFlowComparisonGranularity
import com.osscabinet.ossWallet.domain.card.CashFlowComparisonSeries
import com.osscabinet.ossWallet.domain.card.CashFlowTimePoint
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.BarChartGranularity
import com.osscabinet.ossWallet.domain.model.settings.BarChartViewMode
import com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private suspend fun resolveTimeBoundsFromDb(
    timelineRange: TimelineRange,
    profileId: Int,
    queryService: DatabaseQueryService
): Pair<Long, Long> {
    val (start, end) = TimelineService.getDateRange(timelineRange)
    if (start != null && end != null) return start to end
    val now = System.currentTimeMillis()
    val minTs = queryService.getMinTransactionTimestamp(profileId) ?: return now to now
    val zone = TimelineService.systemZoneId
    val resolvedStart = Instant.ofEpochMilli(minTs).atZone(zone).toLocalDate()
        .atStartOfDay(zone).toInstant().toEpochMilli()
    return resolvedStart to now
}

class CashFlowComparisonCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<CashFlowComparisonCardConfig, CardRenderData.CashFlowComparison> {

    override val supportedType = CardType.CASHFLOW_COMPARISON

    override suspend fun compute(
        input: CardInput,
        config: CashFlowComparisonCardConfig
    ): CardRenderData.CashFlowComparison {
        val rawFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val withCategories = if (rawFilter.categoryIds.isEmpty()) rawFilter
            else rawFilter.copy(categoryIds = input.categoryHierarchy.expand(rawFilter.categoryIds))
        val mergedFilter = withCategories.copy(
            accountIds = withCategories.accountIds.ifEmpty { input.effectiveAccountIds }
        )
        val excludeInternal = config.excludeInternalTransfers && config.perspective is AccountingPerspective.CashFlow
        val (currentStart, currentEnd) = resolveTimeBoundsFromDb(mergedFilter.timelineRange, input.profileId, queryService)

        val excludeFuture = mergedFilter.timelineRange.excludeFuture
        val today = System.currentTimeMillis()
        val zone = TimelineService.systemZoneId
        val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val masterEnd = if (excludeFuture) minOf(currentEnd, today) else currentEnd
        val scrollable = config.viewMode == BarChartViewMode.SCROLLABLE
        val chartGranularity = CardComputerUtils.resolveBarGranularity(
            if (scrollable) config.scrollGranularity else BarChartGranularity.AUTO,
            currentStart, currentEnd
        )
        val granularity = with(CardComputerUtils) { chartGranularity.toLegacyGranularity() }
        // Scrollable mode uses calendar-aligned buckets; fixed mode keeps the original range-start buckets.
        fun bucketsFor(start: Long, end: Long) =
            if (scrollable) CardComputerUtils.generateAlignedBuckets(start, end, chartGranularity)
            else CardComputerUtils.generateBuckets(start, end, granularity)
        val masterBuckets = bucketsFor(currentStart, masterEnd)

        // 1. Current series — single DB call for full range, partitioned in-memory
        val currentDailyData = TransactionAggregationService.getSummaryByCurrencyByDay(
            queryService, input.profileId,
            mergedFilter.copy(timelineRange = TimelineRange.CustomRange(currentStart, masterEnd)),
            excludeInternal
        ).getOrDefault(emptyMap())

        val currentSeries = buildSeriesFromDailyData(
            buckets = masterBuckets,
            dailyData = currentDailyData,
            effectiveToday = today,
            excludeFuture = true,
            baseCurrency = input.targetCurrency,
            exchangeRates = input.exchangeRates,
            zone = zone,
            dayFormatter = dayFormatter,
            perspective = config.perspective
        )

        // 2. Previous period series — shift master buckets back, single DB call
        val (prevStart, prevEnd) = TimelineService.getPreviousRange(mergedFilter.timelineRange)
        val previousSeries = if (prevStart != null && prevEnd != null) {
            val shift = currentStart - prevStart
            val prevBuckets = masterBuckets.map { (s, e, l) -> Triple(s - shift, e - shift, l) }
            val prevEffectiveToday = today - shift
            val prevRangeEnd = if (excludeFuture) minOf(prevBuckets.last().second, prevEffectiveToday) else prevBuckets.last().second

            val prevDailyData = TransactionAggregationService.getSummaryByCurrencyByDay(
                queryService, input.profileId,
                mergedFilter.copy(timelineRange = TimelineRange.CustomRange(prevBuckets.first().first, prevRangeEnd)),
                excludeInternal
            ).getOrDefault(emptyMap())

            buildSeriesFromDailyData(
                buckets = prevBuckets,
                dailyData = prevDailyData,
                effectiveToday = prevEffectiveToday,
                excludeFuture = excludeFuture,
                baseCurrency = input.targetCurrency,
                exchangeRates = input.exchangeRates,
                zone = zone,
                dayFormatter = dayFormatter,
                perspective = config.perspective
            )
        } else CashFlowComparisonSeries(emptyList())

        // 3. Last year series — shift by 1 calendar year, single DB call
        val lastYearToday = ZonedDateTime.ofInstant(Instant.ofEpochMilli(today), zone)
            .minusYears(1).toInstant().toEpochMilli()
        val lastYearBuckets = masterBuckets.map { (s, e, l) ->
            Triple(
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(s), zone).minusYears(1).toInstant().toEpochMilli(),
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(e), zone).minusYears(1).toInstant().toEpochMilli(),
                l
            )
        }
        val lyRangeEnd = if (excludeFuture) minOf(lastYearBuckets.last().second, lastYearToday) else lastYearBuckets.last().second

        val lastYearDailyData = TransactionAggregationService.getSummaryByCurrencyByDay(
            queryService, input.profileId,
            mergedFilter.copy(timelineRange = TimelineRange.CustomRange(lastYearBuckets.first().first, lyRangeEnd)),
            excludeInternal
        ).getOrDefault(emptyMap())

        val lastYearSeries = buildSeriesFromDailyData(
            buckets = lastYearBuckets,
            dailyData = lastYearDailyData,
            effectiveToday = lastYearToday,
            excludeFuture = excludeFuture,
            baseCurrency = input.targetCurrency,
            exchangeRates = input.exchangeRates,
            zone = zone,
            dayFormatter = dayFormatter,
            perspective = config.perspective
        )

        val fullCurrentBuckets = if (excludeFuture) masterBuckets.size else bucketsFor(currentStart, currentEnd).size

        val fullPrevBucketsCount = if (excludeFuture) {
            masterBuckets.size
        } else if (prevStart != null && prevEnd != null) {
            bucketsFor(prevStart, prevEnd).size
        } else 0

        val lastYearBucketsCount = if (excludeFuture) {
            masterBuckets.size
        } else {
            val lyStart = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentStart), zone)
                .minusYears(1).toInstant().toEpochMilli()
            val lyEnd = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentEnd), zone)
                .minusYears(1).toInstant().toEpochMilli()
            bucketsFor(lyStart, lyEnd).size
        }

        return CardRenderData.CashFlowComparison(
            config = config,
            currentSeries = currentSeries,
            previousSeries = previousSeries,
            lastYearSeries = lastYearSeries,
            currency = input.targetCurrency,
            granularity = granularity,
            periodLabels = masterBuckets.map { it.third },
            currentTotalBuckets = fullCurrentBuckets,
            previousTotalBuckets = fullPrevBucketsCount,
            lastYearTotalBuckets = lastYearBucketsCount,
            chartGranularity = chartGranularity
        )
    }

    private fun buildSeriesFromDailyData(
        buckets: List<Triple<Long, Long, String>>,
        dailyData: Map<String, Map<String, FinancialSummary>>,
        effectiveToday: Long,
        excludeFuture: Boolean,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        zone: java.time.ZoneId,
        dayFormatter: DateTimeFormatter,
        perspective: AccountingPerspective
    ): CashFlowComparisonSeries {
        val bucketStartMillis = buckets.map { it.first }

        // Map each day key to its bucket index, filtering out future days if needed.
        val dayToBucketIdx: Map<String, Int> = dailyData.keys.mapNotNull { dayKey ->
            val dayStart = try {
                LocalDate.parse(dayKey, dayFormatter).atStartOfDay(zone).toInstant().toEpochMilli()
            } catch (_: Exception) { return@mapNotNull null }
            if (excludeFuture && dayStart > effectiveToday) return@mapNotNull null
            val idx = bucketStartMillis.indexOfLast { it <= dayStart }
            if (idx < 0) return@mapNotNull null
            val (_, bucketEnd, _) = buckets[idx]
            val effectiveEnd = if (excludeFuture) minOf(bucketEnd, effectiveToday) else bucketEnd
            if (dayStart > effectiveEnd) return@mapNotNull null
            dayKey to idx
        }.toMap()

        // Aggregate daily summaries into buckets.
        val byBucket: Map<Int, Map<String, FinancialSummary>> = buildMap {
            dayToBucketIdx.forEach { (dayKey, bucketIdx) ->
                val daySummaries = dailyData[dayKey] ?: return@forEach
                val existing = getOrDefault(bucketIdx, emptyMap()).toMutableMap()
                daySummaries.forEach { (currency, summary) ->
                    val prev = existing[currency]
                    existing[currency] = if (prev == null) summary else prev + summary
                }
                put(bucketIdx, existing)
            }
        }

        // Pre-compute one conversion factor per source currency to avoid repeated
        // map lookups and BigDecimal divides inside the bucket loop.
        val anchor = exchangeRates.anchor
        val toRate = if (baseCurrency == anchor) BigDecimal.ONE else exchangeRates.rates[baseCurrency]
        val conversionFactors: Map<String, BigDecimal> = byBucket.values
            .flatMap { it.keys }
            .toSet()
            .mapNotNull { currency ->
                if (currency == baseCurrency) return@mapNotNull currency to BigDecimal.ONE
                val fromRate = if (currency == anchor) BigDecimal.ONE else exchangeRates.rates[currency]
                if (fromRate == null || toRate == null || fromRate.signum() == 0) return@mapNotNull null
                currency to toRate.divide(fromRate, 14, RoundingMode.HALF_UP)
            }
            .toMap()

        val points = mutableListOf<CashFlowTimePoint>()
        for ((idx, triple) in buckets.withIndex()) {
            val (bucketStart, _, label) = triple
            if (excludeFuture && bucketStart > effectiveToday) break
            val summaries = byBucket[idx] ?: emptyMap()

            var rawIncome  = BigDecimal.ZERO
            var rawExpense = BigDecimal.ZERO
            var rawTrfIn   = BigDecimal.ZERO
            var rawTrfOut  = BigDecimal.ZERO
            summaries.forEach { (currency, summary) ->
                val factor = conversionFactors[currency] ?: return@forEach
                rawIncome  = rawIncome.add(summary.income.multiply(factor))
                rawExpense = rawExpense.add(summary.expense.multiply(factor))
                rawTrfIn   = rawTrfIn.add(summary.transferIn.multiply(factor))
                rawTrfOut  = rawTrfOut.add(summary.transferOut.multiply(factor))
            }

            val ps = FinancialSummary(
                income = rawIncome,
                expense = rawExpense,
                transferIn = rawTrfIn,
                transferOut = rawTrfOut
            )

            points.add(
                CashFlowTimePoint(
                    label = label,
                    epochMillis = bucketStart,
                    metrics = ps.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
                )
            )
        }
        return CashFlowComparisonSeries(points)
    }
}
