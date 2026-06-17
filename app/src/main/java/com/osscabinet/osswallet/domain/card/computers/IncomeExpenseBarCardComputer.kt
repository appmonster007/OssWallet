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
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig
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
        val (currentStart, currentEnd) = TimelineService.resolveBoundRange(
            mergedFilter.timelineRange,
            input.transactions
        )

        val granularity = CardComputerUtils.resolveGranularity(currentStart, currentEnd)

        // No capping at today — show all buckets in the period (future buckets return zeros)
        val today = System.currentTimeMillis()
        val buckets = CardComputerUtils.generateBuckets(currentStart, currentEnd, granularity)
        val todayBucketIndex = buckets.indexOfLast { (bucketStart, _, _) -> bucketStart <= today }

        val series = computeSeriesFromBuckets(
            buckets = buckets,
            baseFilter = mergedFilter,
            profileId = input.profileId,
            baseCurrency = input.targetCurrency,
            exchangeRates = input.exchangeRates,
            granularity = granularity
        )

        return CardRenderData.IncomeExpenseBar(
            config = config,
            buckets = series.points,
            granularity = granularity,
            currency = input.targetCurrency,
            todayBucketIndex = todayBucketIndex
        )
    }

    private suspend fun computeSeriesFromBuckets(
        buckets: List<Triple<Long, Long, String>>,
        baseFilter: TransactionFilter,
        profileId: Int,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
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
                    CurrencyService.convertAmount(
                        summary.income,
                        currency,
                        baseCurrency,
                        exchangeRates,
                        10
                    )
                )
                // expense is already negated in FinancialSummary
                totalExpense = totalExpense.add(
                    CurrencyService.convertAmount(
                        summary.expense,
                        currency,
                        baseCurrency,
                        exchangeRates,
                        10
                    )
                )
            }

            CashFlowTimePoint(
                label = label,
                epochMillis = bucketStart,
                bucketEnd = bucketEnd,
                income = totalIncome.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                expense = totalExpense.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                net = totalIncome.add(totalExpense)
                    .setScale(CurrencyService.precision, RoundingMode.HALF_UP)
            )
        }

        return CashFlowComparisonSeries(points)
    }
}
