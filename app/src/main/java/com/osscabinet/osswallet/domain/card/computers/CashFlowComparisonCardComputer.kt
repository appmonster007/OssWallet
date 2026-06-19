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
import com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
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

    override suspend fun compute(
        input: CardInput,
        config: CashFlowComparisonCardConfig
    ): CardRenderData.CashFlowComparison {
        val rawFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val withCategories = if (rawFilter.categoryIds.isEmpty()) rawFilter
            else rawFilter.copy(categoryIds = input.categoryHierarchy.expand(rawFilter.categoryIds))
        val mergedFilter = withCategories.copy(
            accountIds = withCategories.accountIds.ifEmpty { input.effectiveAccountIds },
            excludeInternalTransfers = config.excludeInternalTransfers
        )
        val (currentStart, currentEnd) = TimelineService.resolveBoundRange(mergedFilter.timelineRange, input.transactions)

        val excludeFuture = mergedFilter.timelineRange.excludeFuture
        val today = System.currentTimeMillis()

        // 1. Determine the visual period for the chart (X-axis labels and markers).
        // If excludeFuture is true, the entire chart truncates at 'today'.
        val masterEnd = if (excludeFuture) minOf(currentEnd, today) else currentEnd

        val granularity = CardComputerUtils.resolveGranularity(currentStart, currentEnd)

        val zone = TimelineService.systemZoneId
        val masterBuckets = CardComputerUtils.generateBuckets(currentStart, masterEnd, granularity)

        // 2. Compute Current Series (always stop at 'today' for the line)
        val currentSeries = computeSeriesFromBucketsWithFutureCap(
            buckets = masterBuckets,
            baseFilter = mergedFilter,
            profileId = input.profileId,
            baseCurrency = input.targetCurrency,
            exchangeRates = input.exchangeRates,
            today = today,
            excludeFuture = true, // Always cap current line at today
            perspective = config.perspective
        )

        // 3. Compute Previous Period Series:
        // If excludeFuture is true, we shift previous period data to align with masterBuckets relative to 'today'.
        val (prevStart, prevEnd) = TimelineService.getPreviousRange(mergedFilter.timelineRange)
        val previousSeries = if (prevStart != null && prevEnd != null) {
            val shift = currentStart - prevStart
            val prevBuckets = masterBuckets.map { (start, end, label) ->
                Triple(start - shift, end - shift, label)
            }
            computeSeriesFromBucketsWithFutureCap(
                buckets = prevBuckets,
                baseFilter = mergedFilter,
                profileId = input.profileId,
                baseCurrency = input.targetCurrency,
                exchangeRates = input.exchangeRates,
                today = today - shift, // Relative 'today' for the previous period
                excludeFuture = excludeFuture,
                perspective = config.perspective
            )
        } else CashFlowComparisonSeries(emptyList())

        // 4. Compute Last Year Series (Shifted by exactly 1 year)
        val lastYearSeries = run {
            val lastYearBuckets = masterBuckets.map { (start, end, label) ->
                val shiftedStart = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), zone)
                    .minusYears(1).toInstant().toEpochMilli()
                val shiftedEnd = ZonedDateTime.ofInstant(Instant.ofEpochMilli(end), zone)
                    .minusYears(1).toInstant().toEpochMilli()
                Triple(shiftedStart, shiftedEnd, label)
            }
            val lastYearToday = ZonedDateTime.ofInstant(Instant.ofEpochMilli(today), zone)
                .minusYears(1).toInstant().toEpochMilli()

            computeSeriesFromBucketsWithFutureCap(
                buckets = lastYearBuckets,
                baseFilter = mergedFilter,
                profileId = input.profileId,
                baseCurrency = input.targetCurrency,
                exchangeRates = input.exchangeRates,
                today = lastYearToday,
                excludeFuture = excludeFuture,
                perspective = config.perspective
            )
        }

        // 5. Total bucket counts for markers.
        // We calculate what the full period length would be if it wasn't truncated by 'today'.
        val fullCurrentBuckets = if (excludeFuture) masterBuckets.size else CardComputerUtils.generateBuckets(currentStart, currentEnd, granularity).size
        
        val fullPrevBucketsCount = if (excludeFuture) {
            masterBuckets.size 
        } else if (prevStart != null && prevEnd != null) {
            CardComputerUtils.generateBuckets(prevStart, prevEnd, granularity).size
        } else 0

        val lastYearBucketsCount = if (excludeFuture) {
            masterBuckets.size
        } else {
            val lyStart = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentStart), zone)
                .minusYears(1).toInstant().toEpochMilli()
            val lyEnd = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentEnd), zone)
                .minusYears(1).toInstant().toEpochMilli()
            CardComputerUtils.generateBuckets(lyStart, lyEnd, granularity).size
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
            lastYearTotalBuckets = lastYearBucketsCount
        )
    }

    // Current / Previous / Last Year series: drop future buckets entirely if excludeFuture is true.
    // If excludeFuture is false, we show all buckets in the requested range.
    private suspend fun computeSeriesFromBucketsWithFutureCap(
        buckets: List<Triple<Long, Long, String>>,
        baseFilter: TransactionFilter,
        profileId: Int,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        today: Long,
        excludeFuture: Boolean,
        perspective: AccountingPerspective
    ): CashFlowComparisonSeries {
        val points = mutableListOf<CashFlowTimePoint>()
        for ((bucketStart, bucketEnd, label) in buckets) {
            if (excludeFuture && bucketStart > today) break
            val effectiveEnd = if (excludeFuture) minOf(bucketEnd, today) else bucketEnd
            points.add(queryBucket(baseFilter, profileId, bucketStart, effectiveEnd, label, baseCurrency, exchangeRates, perspective))
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
        exchangeRates: ExchangeRateSnapshot,
        perspective: AccountingPerspective
    ): CashFlowTimePoint {
        val summaries = TransactionAggregationService.getSummaryByCurrency(
            queryService, profileId,
            baseFilter.copy(timelineRange = TimelineRange.CustomRange(bucketStart, bucketEnd))
        ).getOrDefault(emptyMap())

        var totalVal1 = BigDecimal.ZERO
        var totalVal2 = BigDecimal.ZERO
        var totalTrf = BigDecimal.ZERO
        summaries.forEach { (currency, summary) ->
            val (v1, v2) = when (perspective) {
                AccountingPerspective.ACCOUNTING -> summary.income to summary.expense
                AccountingPerspective.CASH_FLOW -> {
                    summary.income.add(summary.transferIn) to summary.expense.add(summary.transferOut)
                }
            }
            totalVal1 += CurrencyService.convertAmount(v1, currency, baseCurrency, exchangeRates, 10)
            totalVal2 += CurrencyService.convertAmount(v2, currency, baseCurrency, exchangeRates, 10)
            totalTrf += CurrencyService.convertAmount(summary.transfer, currency, baseCurrency, exchangeRates, 10)
        }
        return CashFlowTimePoint(
            label = label,
            epochMillis = bucketStart,
            income = totalVal1.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            expense = totalVal2.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            transfer = totalTrf.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
            net = totalVal1.add(totalVal2).setScale(CurrencyService.precision, RoundingMode.HALF_UP)
        )
    }
}
