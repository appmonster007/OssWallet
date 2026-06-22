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
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig
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
import java.time.format.DateTimeFormatter

private suspend fun resolveIncomeExpenseTimeBounds(
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

class IncomeExpenseBarCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<IncomeExpenseBarCardConfig, CardRenderData.IncomeExpenseBar> {

    override val supportedType = CardType.INCOME_EXPENSE_BAR

    override suspend fun compute(
        input: CardInput,
        config: IncomeExpenseBarCardConfig
    ): CardRenderData.IncomeExpenseBar {
        val rawFilter = resolveEffectiveFilter(
            input.filter,
            config.filter,
            config.filterRules,
            input.screenOwnsTimeline
        )
        val withCategories = if (rawFilter.categoryIds.isEmpty()) rawFilter
            else rawFilter.copy(categoryIds = input.categoryHierarchy.expand(rawFilter.categoryIds))
        val mergedFilter = withCategories.copy(
            accountIds = withCategories.accountIds.ifEmpty { input.effectiveAccountIds }
        )
        val excludeInternal = config.excludeInternalTransfers && config.perspective is AccountingPerspective.CashFlow
        val (currentStart, currentEnd) = resolveIncomeExpenseTimeBounds(mergedFilter.timelineRange, input.profileId, queryService)

        val granularity = CardComputerUtils.resolveGranularity(currentStart, currentEnd)
        val today = System.currentTimeMillis()
        val buckets = CardComputerUtils.generateBuckets(currentStart, currentEnd, granularity)

        // Binary search: last bucket whose start is ≤ today.
        val todayBucketIndex = run {
            val raw = buckets.binarySearch { (bucketStart, _, _) -> bucketStart.compareTo(today) }
            when {
                raw >= 0 -> raw
                raw == -1 -> -1
                else -> -raw - 2
            }
        }

        // Single DB call for the full range, partitioned into buckets in-memory.
        val dailyData = TransactionAggregationService.getSummaryByCurrencyByDay(
            queryService, input.profileId,
            mergedFilter.copy(timelineRange = TimelineRange.CustomRange(currentStart, currentEnd)),
            excludeInternal
        ).getOrDefault(emptyMap())

        val series = buildSeriesFromDailyData(
            buckets = buckets,
            dailyData = dailyData,
            baseCurrency = input.targetCurrency,
            exchangeRates = input.exchangeRates,
            granularity = granularity,
            perspective = config.perspective
        )

        return CardRenderData.IncomeExpenseBar(
            config = config,
            buckets = series.points,
            granularity = granularity,
            currency = input.targetCurrency,
            todayBucketIndex = todayBucketIndex
        )
    }

    private fun buildSeriesFromDailyData(
        buckets: List<Triple<Long, Long, String>>,
        dailyData: Map<String, Map<String, FinancialSummary>>,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        granularity: CashFlowComparisonGranularity,
        perspective: AccountingPerspective
    ): CashFlowComparisonSeries {
        val zone = TimelineService.systemZoneId
        val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val bucketStartMillis = buckets.map { it.first }

        val dayToBucketIdx: Map<String, Int> = dailyData.keys.mapNotNull { dayKey ->
            val dayStart = try {
                LocalDate.parse(dayKey, dayFormatter).atStartOfDay(zone).toInstant().toEpochMilli()
            } catch (_: Exception) { return@mapNotNull null }
            val idx = bucketStartMillis.indexOfLast { it <= dayStart }
            if (idx < 0) return@mapNotNull null
            val (_, bucketEnd, _) = buckets[idx]
            if (dayStart > bucketEnd) return@mapNotNull null
            dayKey to idx
        }.toMap()

        val byBucket: Map<Int, Map<String, FinancialSummary>> = buildMap {
            dayToBucketIdx.forEach { (dayKey, bucketIdx) ->
                val daySummaries = dailyData[dayKey] ?: return@forEach
                val existing = getOrDefault(bucketIdx, emptyMap()).toMutableMap()
                daySummaries.forEach { (currency, summary) ->
                    val prev = existing[currency]
                    existing[currency] = if (prev == null) summary else FinancialSummary(
                        totalAmount = prev.totalAmount.add(summary.totalAmount),
                        income = prev.income.add(summary.income),
                        expense = prev.expense.add(summary.expense),
                        transfer = prev.transfer.add(summary.transfer),
                        transferIn = prev.transferIn.add(summary.transferIn),
                        transferOut = prev.transferOut.add(summary.transferOut),
                        crossCcyTransferIn = prev.crossCcyTransferIn.add(summary.crossCcyTransferIn),
                        crossCcyTransferOut = prev.crossCcyTransferOut.add(summary.crossCcyTransferOut),
                        transactionCount = prev.transactionCount + summary.transactionCount
                    )
                }
                put(bucketIdx, existing)
            }
        }

        val points = buckets.mapIndexed { idx, (bucketStart, bucketEnd, label) ->
            val summaries = byBucket[idx] ?: emptyMap()
            var val1 = BigDecimal.ZERO
            var val2 = BigDecimal.ZERO
            var valTrf = BigDecimal.ZERO

            summaries.forEach { (currency, summary) ->
                val (inc, exp) = when (perspective) {
                    is AccountingPerspective.Accounting -> summary.income to summary.expense
                    is AccountingPerspective.CashFlow ->
                        summary.income.add(summary.transferIn) to summary.expense.add(summary.transferOut)
                }
                val1 = val1.add(CurrencyService.convertAmount(inc, currency, baseCurrency, exchangeRates, 10))
                val2 = val2.add(CurrencyService.convertAmount(exp, currency, baseCurrency, exchangeRates, 10))
                valTrf = valTrf.add(CurrencyService.convertAmount(summary.transfer, currency, baseCurrency, exchangeRates, 10))
            }

            CashFlowTimePoint(
                label = label,
                epochMillis = bucketStart,
                bucketEnd = bucketEnd,
                income = val1.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                expense = val2.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                transfer = valTrf.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                net = val1.add(val2).setScale(CurrencyService.precision, RoundingMode.HALF_UP)
            )
        }

        return CashFlowComparisonSeries(points)
    }
}
