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
            accountIds = withCategories.accountIds.ifEmpty { input.effectiveAccountIds },
            excludeInternalTransfers = config.excludeInternalTransfers
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

    private suspend fun computeSeriesFromBuckets(
        buckets: List<Triple<Long, Long, String>>,
        baseFilter: TransactionFilter,
        profileId: Int,
        baseCurrency: String,
        exchangeRates: ExchangeRateSnapshot,
        granularity: CashFlowComparisonGranularity,
        perspective: AccountingPerspective
    ): CashFlowComparisonSeries {
        val points = buckets.map { (bucketStart, bucketEnd, label) ->
            val bucketFilter = baseFilter.copy(
                timelineRange = TimelineRange.CustomRange(bucketStart, bucketEnd)
            )
            val summaries = TransactionAggregationService.getSummaryByCurrency(
                queryService, profileId, bucketFilter
            ).getOrDefault(emptyMap())

            var val1 = BigDecimal.ZERO // Income or Inflow
            var val2 = BigDecimal.ZERO // Expense or Outflow
            var valTrf = BigDecimal.ZERO // Transfer volume

            summaries.forEach { (currency, summary) ->
                val (inc, exp) = when (perspective) {
                    AccountingPerspective.ACCOUNTING -> summary.income to summary.expense
                    AccountingPerspective.CASH_FLOW -> {
                        // Inflow = Income + TransferIn; Outflow = Expense + TransferOut
                        val inflow = summary.income.add(summary.transferIn)
                        val outflow = summary.expense.add(summary.transferOut)
                        inflow to outflow
                    }
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
