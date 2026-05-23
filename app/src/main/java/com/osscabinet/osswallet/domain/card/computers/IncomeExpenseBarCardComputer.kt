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
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig
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

class IncomeExpenseBarCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<IncomeExpenseBarCardConfig, CardRenderData.IncomeExpenseBar> {

    override val supportedType = CardType.INCOME_EXPENSE_BAR

    private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM")
    private val monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy")

    override suspend fun compute(
        input: CardInput,
        config: IncomeExpenseBarCardConfig
    ): CardRenderData.IncomeExpenseBar {
        val mergedFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val (currentStart, currentEnd) = TimelineService.resolveBoundRange(mergedFilter.timelineRange, input.transactions)

        val spanDays = (currentEnd - currentStart) / 86_400_000L
        val granularity = when {
            spanDays <= 90L  -> CashFlowComparisonGranularity.DAY
            spanDays <= 365L -> CashFlowComparisonGranularity.WEEK
            else             -> CashFlowComparisonGranularity.MONTH
        }

        // No capping at today — show all buckets in the period (future buckets return zeros)
        val today = System.currentTimeMillis()
        val buckets = generateBuckets(currentStart, currentEnd, granularity)
        val todayBucketIndex = buckets.indexOfLast { (bucketStart, _, _) -> bucketStart <= today }

        val series = computeSeriesFromBuckets(
            buckets = buckets,
            baseFilter = mergedFilter,
            profileId = input.profileId,
            baseCurrency = input.displayCurrency,
            exchangeRates = input.exchangeRates,
            granularity = granularity
        )

        return CardRenderData.IncomeExpenseBar(
            config = config,
            buckets = series.points,
            granularity = granularity,
            baseCurrency = input.displayCurrency,
            todayBucketIndex = todayBucketIndex
        )
    }

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

    private suspend fun computeSeriesFromBuckets(
        buckets: List<Triple<Long, Long, String>>,
        baseFilter: TransactionFilter,
        profileId: Int,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        granularity: CashFlowComparisonGranularity
    ): CashFlowComparisonSeries {
        val points = buckets.map { (bucketStart, bucketEnd, label) ->
            val bucketFilter = baseFilter.copy(
                timelineRange = TimelineRange.CustomRange(bucketStart, bucketEnd)
            )
            val summaries = TransactionAggregationService.getSummaryByCurrency(
                queryService, profileId, bucketFilter
            ).getOrDefault(emptyMap())

            var totalIncome = BigDecimal.ZERO
            var totalExpense = BigDecimal.ZERO

            summaries.forEach { (currency, summary) ->
                totalIncome = totalIncome.add(
                    CurrencyService.convertAmount(summary.income, currency, baseCurrency, exchangeRates, 10)
                )
                // expense is already negated in FinancialSummary; take abs for display
                totalExpense = totalExpense.add(
                    CurrencyService.convertAmount(summary.expense.abs(), currency, baseCurrency, exchangeRates, 10)
                )
            }

            CashFlowTimePoint(
                label = label,
                epochMillis = bucketStart,
                income = totalIncome.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                expense = totalExpense.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                net = totalIncome.subtract(totalExpense).setScale(CurrencyService.precision, RoundingMode.HALF_UP)
            )
        }

        return CashFlowComparisonSeries(points)
    }
}
