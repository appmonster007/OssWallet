package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.TagUpSetIntersection
import com.osscabinet.ossWallet.domain.card.TagUpSetSet
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.StandardMetrics
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig
import com.osscabinet.ossWallet.domain.model.settings.UpSetIntersectionSortMode
import com.osscabinet.ossWallet.domain.model.settings.UpSetTagSortMode
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import java.math.BigDecimal
import java.math.RoundingMode

private data class IndividualTagStats(
    val income: BigDecimal,
    val expense: BigDecimal,
    val count: Int,
    val incomeByCurrency: Map<String, BigDecimal>,
    val expenseByCurrency: Map<String, BigDecimal>
)

class TagUpSetPlotCardComputer(
    private val queryService: DatabaseQueryService
) : CardDataComputer<TagUpSetPlotCardConfig, CardRenderData.TagUpSetPlot> {

    override val supportedType = CardType.TAG_UPSET_PLOT

    override suspend fun compute(
        input: CardInput,
        config: TagUpSetPlotCardConfig
    ): CardRenderData.TagUpSetPlot {
        val rawFilter = resolveEffectiveFilter(
            input.filter,
            config.filter,
            config.filterRules,
            input.screenOwnsTimeline
        )
        val mergedFilter = rawFilter.copy(
            accountIds = rawFilter.accountIds.ifEmpty { input.effectiveAccountIds }
        )
        val excludeInternal = config.excludeInternalTransfers && config.perspective is AccountingPerspective.CashFlow
        val profileId = input.profileId
        val baseCurrency = input.targetCurrency
        val exchangeRates = input.exchangeRates

        val tagMetrics = listOf(
            StandardMetrics.INCOME_SUM,
            StandardMetrics.EXPENSE_SUM,
            StandardMetrics.TRANSFER_IN_SUM,
            StandardMetrics.TRANSFER_OUT_SUM,
            StandardMetrics.COUNT
        )

        // 1. Individual tag totals — pre-filter "unspecified" rows before groupBy to skip
        // processing data that will be discarded.
        val individualTagData = queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.TAGS, AggregationDimension.CURRENCY),
            metrics = tagMetrics,
            filter = mergedFilter,
            excludeInternalTransfers = excludeInternal
        ).getOrDefault(emptyList())

        val tagTotals = individualTagData
            .filter { row ->
                val tag = row.dimensions[AggregationDimension.TAGS] ?: ""
                tag.isNotEmpty() && tag != "unspecified"
            }
            .groupBy { it.dimensions[AggregationDimension.TAGS]!! }
            .mapValues { (_, rows) ->
                var val1Total = BigDecimal.ZERO
                var val2Total = BigDecimal.ZERO
                var countTotal = 0

                rows.forEach { row ->
                    val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val inc = row.metrics["income"] ?: BigDecimal.ZERO
                    val exp = row.metrics["expense"] ?: BigDecimal.ZERO
                    val trIn = row.metrics["transferIn"] ?: BigDecimal.ZERO
                    val trOut = row.metrics["transferOut"] ?: BigDecimal.ZERO

                    val (v1, v2) = when (config.perspective) {
                        is AccountingPerspective.Accounting -> {
                            if (config.selectedMetric == ChartMetric.TRANSFER) trIn to trOut else inc to exp
                        }
                        is AccountingPerspective.CashFlow -> inc.add(trIn) to exp.add(trOut)
                    }

                    val1Total += CurrencyService.convertAmount(v1, currency, baseCurrency, exchangeRates, 10)
                    val2Total += CurrencyService.convertAmount(v2, currency, baseCurrency, exchangeRates, 10)
                    countTotal += row.count
                }

                val (incByCurr, expByCurr) = rows.associate {
                    val currency = it.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val inc = it.metrics["income"] ?: BigDecimal.ZERO
                    val exp = it.metrics["expense"] ?: BigDecimal.ZERO
                    val trIn = it.metrics["transferIn"] ?: BigDecimal.ZERO
                    val trOut = it.metrics["transferOut"] ?: BigDecimal.ZERO

                    val (v1, v2) = when (config.perspective) {
                        is AccountingPerspective.Accounting -> {
                            if (config.selectedMetric == ChartMetric.TRANSFER) trIn to trOut else inc to exp
                        }
                        is AccountingPerspective.CashFlow -> inc.add(trIn) to exp.add(trOut)
                    }
                    currency to (v1 to v2)
                }.let { map ->
                    map.mapValues { it.value.first }.filterValues { it.signum() != 0 } to
                        map.mapValues { it.value.second }.filterValues { it.signum() != 0 }
                }

                IndividualTagStats(val1Total, val2Total, countTotal, incByCurr, expByCurr)
            }
            .toList()
            .let { list ->
                val metricValue = { p: Pair<String, IndividualTagStats> ->
                    when (config.selectedMetric) {
                        ChartMetric.INCOME -> p.second.income
                        ChartMetric.EXPENSE -> p.second.expense
                        ChartMetric.NET -> p.second.income.add(p.second.expense)
                        ChartMetric.TRANSFER -> p.second.income.add(p.second.expense)
                    }
                }
                when (config.tagSortMode) {
                    UpSetTagSortMode.NAME_ASC -> list.sortedBy { it.first }
                    UpSetTagSortMode.NAME_DESC -> list.sortedByDescending { it.first }
                    UpSetTagSortMode.DECREASING_ABS -> list.sortedByDescending { metricValue(it).abs() }
                    UpSetTagSortMode.INCREASING_ABS -> list.sortedBy { metricValue(it).abs() }
                    UpSetTagSortMode.DECREASING -> list.sortedByDescending { metricValue(it) }
                    UpSetTagSortMode.INCREASING -> list.sortedBy { metricValue(it) }
                }
            }

        val topTags = tagTotals.map { it.first }.toSet()

        // 2. Tag combinations — use canonical key (sorted raw tags ∩ topTags) in the first groupBy
        // so that e.g. "a,b,c" and "a,b,d" (both reduce to {"a","b"}) are merged in one pass,
        // eliminating the original second groupBy after mapping.
        val combinationData = queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.TAG_COMBINATIONS, AggregationDimension.CURRENCY),
            metrics = tagMetrics,
            filter = mergedFilter,
            excludeInternalTransfers = excludeInternal
        ).getOrDefault(emptyList())

        val intersections = combinationData
            .groupBy { row ->
                val tagSetStr = row.dimensions[AggregationDimension.TAG_COMBINATIONS] ?: ""
                tagSetStr.split(",").filter { it.isNotEmpty() }.toSortedSet().intersect(topTags)
            }
            .filter { (key, _) -> key.isNotEmpty() }
            .map { (filteredTags, rows) ->
                var val1Total = BigDecimal.ZERO
                var val2Total = BigDecimal.ZERO
                val val1ByCurr = mutableMapOf<String, BigDecimal>()
                val val2ByCurr = mutableMapOf<String, BigDecimal>()
                var count = 0

                rows.forEach { row ->
                    val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val inc = row.metrics["income"] ?: BigDecimal.ZERO
                    val exp = row.metrics["expense"] ?: BigDecimal.ZERO
                    val trIn = row.metrics["transferIn"] ?: BigDecimal.ZERO
                    val trOut = row.metrics["transferOut"] ?: BigDecimal.ZERO

                    val (v1, v2) = when (config.perspective) {
                        is AccountingPerspective.Accounting -> {
                            if (config.selectedMetric == ChartMetric.TRANSFER) trIn to trOut else inc to exp
                        }
                        is AccountingPerspective.CashFlow -> inc.add(trIn) to exp.add(trOut)
                    }

                    val1Total += CurrencyService.convertAmount(v1, currency, baseCurrency, exchangeRates, 10)
                    val2Total += CurrencyService.convertAmount(v2, currency, baseCurrency, exchangeRates, 10)

                    val1ByCurr[currency] = (val1ByCurr[currency] ?: BigDecimal.ZERO) + v1
                    val2ByCurr[currency] = (val2ByCurr[currency] ?: BigDecimal.ZERO) + v2
                    count += row.count
                }

                val amount = when (config.selectedMetric) {
                    ChartMetric.INCOME -> val1Total
                    ChartMetric.EXPENSE -> val2Total
                    ChartMetric.NET -> val1Total.add(val2Total)
                    ChartMetric.TRANSFER -> val1Total.add(val2Total)
                }

                TagUpSetIntersection(
                    tags = filteredTags,
                    amount = amount.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    income = val1Total.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    expense = val2Total.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    incomeByCurrency = val1ByCurr.filterValues { it.signum() != 0 },
                    expenseByCurrency = val2ByCurr.filterValues { it.signum() != 0 },
                    transactionCount = count
                )
            }
            .let { list ->
                when (config.intersectionSortMode) {
                    UpSetIntersectionSortMode.DECREASING_ABS -> list.sortedByDescending { it.amount.abs() }
                    UpSetIntersectionSortMode.INCREASING_ABS -> list.sortedBy { it.amount.abs() }
                    UpSetIntersectionSortMode.DECREASING -> list.sortedByDescending { it.amount }
                    UpSetIntersectionSortMode.INCREASING -> list.sortedBy { it.amount }
                }
            }

        val sets = tagTotals.map { (tag, stats) ->
            val total = when (config.selectedMetric) {
                ChartMetric.INCOME -> stats.income
                ChartMetric.EXPENSE -> stats.expense
                ChartMetric.NET -> stats.income.add(stats.expense)
                ChartMetric.TRANSFER -> stats.income.add(stats.expense)
            }
            TagUpSetSet(
                tagName = tag,
                totalAmount = total.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                income = stats.income.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                expense = stats.expense.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                incomeByCurrency = stats.incomeByCurrency,
                expenseByCurrency = stats.expenseByCurrency,
                transactionCount = stats.count
            )
        }

        return CardRenderData.TagUpSetPlot(
            config = config,
            sets = sets,
            intersections = intersections,
            currency = baseCurrency
        )
    }
}
