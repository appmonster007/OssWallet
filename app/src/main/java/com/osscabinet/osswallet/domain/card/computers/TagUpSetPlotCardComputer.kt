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

data class TagUpSetIntersectionResult(
    val tags: Set<String>,
    val income: BigDecimal,
    val expense: BigDecimal,
    val incomeByCurrency: Map<String, BigDecimal>,
    val expenseByCurrency: Map<String, BigDecimal>,
    val net: BigDecimal,
    val transactionCount: Int
)

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
            accountIds = rawFilter.accountIds.ifEmpty { input.effectiveAccountIds },
            excludeInternalTransfers = config.excludeInternalTransfers
        )
        val profileId = input.profileId
        val baseCurrency = input.targetCurrency
        val exchangeRates = input.exchangeRates

        // 1. Compute Individual Tag Totals
        val tagMetrics = listOf(
            StandardMetrics.INCOME_SUM,
            StandardMetrics.EXPENSE_SUM,
            StandardMetrics.TRANSFER_IN_SUM,
            StandardMetrics.TRANSFER_OUT_SUM,
            StandardMetrics.COUNT
        )

        val individualTagData = queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.TAGS, AggregationDimension.CURRENCY),
            metrics = tagMetrics,
            filter = mergedFilter
        ).getOrDefault(emptyList())

        val tagTotals =
            individualTagData.groupBy { it.dimensions[AggregationDimension.TAGS] ?: "unspecified" }
                .mapValues { (_, rows) ->
                    var val1Total = BigDecimal.ZERO // Income or Inflow
                    var val2Total = BigDecimal.ZERO // Expense or Outflow
                    var countTotal = 0

                    rows.forEach { row ->
                        val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                        val inc = row.metrics["income"] ?: BigDecimal.ZERO
                        val exp = row.metrics["expense"] ?: BigDecimal.ZERO
                        val trIn = row.metrics["transferIn"] ?: BigDecimal.ZERO
                        val trOut = row.metrics["transferOut"] ?: BigDecimal.ZERO

                        val (v1, v2) = when (config.perspective) {
                            AccountingPerspective.ACCOUNTING -> {
                                if (config.selectedMetric == ChartMetric.TRANSFER) {
                                    trIn to trOut
                                } else {
                                    inc to exp
                                }
                            }
                            AccountingPerspective.CASH_FLOW -> {
                                inc.add(trIn) to exp.add(trOut)
                            }
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
                            AccountingPerspective.ACCOUNTING -> {
                                if (config.selectedMetric == ChartMetric.TRANSFER) trIn to trOut else inc to exp
                            }
                            AccountingPerspective.CASH_FLOW -> inc.add(trIn) to exp.add(trOut)
                        }
                        currency to (v1 to v2)
                    }.let { map ->
                        map.mapValues { it.value.first }.filterValues { it.signum() != 0 } to
                        map.mapValues { it.value.second }.filterValues { it.signum() != 0 }
                    }

                    IndividualTagStats(
                        val1Total,
                        val2Total,
                        countTotal,
                        incByCurr,
                        expByCurr
                    )
                }
                .filter { it.key != "unspecified" }
                .toList()
                .let { list ->
                    val metricValue = { p: Pair<String, IndividualTagStats> ->
                        when (config.selectedMetric) {
                            ChartMetric.INCOME -> p.second.income
                            ChartMetric.EXPENSE -> p.second.expense
                            ChartMetric.NET -> p.second.income.add(p.second.expense)
                            ChartMetric.TRANSFER -> p.second.income.add(p.second.expense) // In Accounting perspective, transfer metric uses income/expense fields for legs
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

        // 2. Compute Tag Combinations (Intersections)
        val combinationData = queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(
                AggregationDimension.TAG_COMBINATIONS,
                AggregationDimension.CURRENCY
            ),
            metrics = tagMetrics,
            filter = mergedFilter
        ).getOrDefault(emptyList())

        val intersections =
            combinationData.groupBy { it.dimensions[AggregationDimension.TAG_COMBINATIONS] ?: "" }
                .map { (tagSetString, rows) ->
                    val tags = tagSetString.split(",").filter { it.isNotEmpty() }.toSet()
                    val filteredTags = tags.intersect(topTags)

                    var val1Total = BigDecimal.ZERO
                    var val2Total = BigDecimal.ZERO
                    val val1ByCurr = mutableMapOf<String, BigDecimal>()
                    val val2ByCurr = mutableMapOf<String, BigDecimal>()

                    rows.forEach { row ->
                        val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                        val inc = row.metrics["income"] ?: BigDecimal.ZERO
                        val exp = row.metrics["expense"] ?: BigDecimal.ZERO
                        val trIn = row.metrics["transferIn"] ?: BigDecimal.ZERO
                        val trOut = row.metrics["transferOut"] ?: BigDecimal.ZERO

                        val (v1, v2) = when (config.perspective) {
                            AccountingPerspective.ACCOUNTING -> {
                                if (config.selectedMetric == ChartMetric.TRANSFER) trIn to trOut else inc to exp
                            }
                            AccountingPerspective.CASH_FLOW -> inc.add(trIn) to exp.add(trOut)
                        }

                        val1Total += CurrencyService.convertAmount(v1, currency, baseCurrency, exchangeRates, 10)
                        val2Total += CurrencyService.convertAmount(v2, currency, baseCurrency, exchangeRates, 10)

                        val1ByCurr[currency] = (val1ByCurr[currency] ?: BigDecimal.ZERO) + v1
                        val2ByCurr[currency] = (val2ByCurr[currency] ?: BigDecimal.ZERO) + v2
                    }

                    val count = rows.sumOf { it.count }

                    TagUpSetIntersectionResult(
                        tags = filteredTags,
                        income = val1Total,
                        expense = val2Total,
                        incomeByCurrency = val1ByCurr.filterValues { it.signum() != 0 },
                        expenseByCurrency = val2ByCurr.filterValues { it.signum() != 0 },
                        net = val1Total.add(val2Total),
                        transactionCount = count
                    )
                }
                .filter { it.tags.isNotEmpty() }
                .groupBy { it.tags }
                .map { (tags, results) ->
                    val inc = results.fold(BigDecimal.ZERO) { acc, r -> acc + r.income }
                    val exp = results.fold(BigDecimal.ZERO) { acc, r -> acc + r.expense }

                    val incByCurr = mutableMapOf<String, BigDecimal>()
                    val expByCurr = mutableMapOf<String, BigDecimal>()
                    results.forEach { r ->
                        r.incomeByCurrency.forEach { (c, v) ->
                            incByCurr[c] = (incByCurr[c] ?: BigDecimal.ZERO) + v
                        }
                        r.expenseByCurrency.forEach { (c, v) ->
                            expByCurr[c] = (expByCurr[c] ?: BigDecimal.ZERO) + v
                        }
                    }

                    val amount = when (config.selectedMetric) {
                        ChartMetric.INCOME -> inc
                        ChartMetric.EXPENSE -> exp
                        ChartMetric.NET -> inc.add(exp)
                        ChartMetric.TRANSFER -> inc.add(exp)
                    }

                    TagUpSetIntersection(
                        tags = tags,
                        amount = amount.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                        income = inc.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                        expense = exp.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                        incomeByCurrency = incByCurr,
                        expenseByCurrency = expByCurr,
                        transactionCount = results.sumOf { it.transactionCount }
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
