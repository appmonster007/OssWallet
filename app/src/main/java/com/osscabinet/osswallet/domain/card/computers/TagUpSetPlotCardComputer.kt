package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.StandardMetrics
import com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig
import com.osscabinet.ossWallet.domain.model.settings.UpSetTagSortMode
import com.osscabinet.ossWallet.domain.model.settings.UpSetIntersectionSortMode
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
        val mergedFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val profileId = input.profileId
        val baseCurrency = input.displayCurrency
        val exchangeRates = input.exchangeRates

        // 1. Compute Individual Tag Totals
        val tagMetrics = listOf(
            StandardMetrics.INCOME_SUM,
            StandardMetrics.EXPENSE_SUM,
            StandardMetrics.COUNT
        )

        val individualTagData = queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.TAGS, AggregationDimension.CURRENCY),
            metrics = tagMetrics,
            filter = mergedFilter
        ).getOrDefault(emptyList())

        val tagTotals = individualTagData.groupBy { it.dimensions[AggregationDimension.TAGS] ?: "unspecified" }
            .mapValues { (_, rows) ->
                var incomeTotal = BigDecimal.ZERO
                var expenseTotal = BigDecimal.ZERO
                var countTotal = 0
                
                rows.forEach { row ->
                    val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val inc = row.metrics["income"] ?: BigDecimal.ZERO
                    val exp = row.metrics["expense"] ?: BigDecimal.ZERO
                    incomeTotal += CurrencyService.convertAmount(inc, currency, baseCurrency, exchangeRates, 10)
                    expenseTotal += CurrencyService.convertAmount(exp, currency, baseCurrency, exchangeRates, 10)
                    countTotal += row.count
                }
                
                val incomeByCurrency = rows.associate { 
                    (it.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency) to (it.metrics["income"] ?: BigDecimal.ZERO)
                }.filterValues { it.signum() != 0 }
                
                val expenseByCurrency = rows.associate { 
                    (it.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency) to (it.metrics["expense"] ?: BigDecimal.ZERO)
                }.filterValues { it.signum() != 0 }

                IndividualTagStats(incomeTotal, expenseTotal, countTotal, incomeByCurrency, expenseByCurrency)
            }
            .filter { it.key != "unspecified" }
            .toList()
            .let { list ->
                val net = { p: Pair<String, IndividualTagStats> -> p.second.income.subtract(p.second.expense) }
                when (config.tagSortMode) {
                    UpSetTagSortMode.NAME_ASC       -> list.sortedBy { it.first }
                    UpSetTagSortMode.NAME_DESC      -> list.sortedByDescending { it.first }
                    UpSetTagSortMode.DECREASING_ABS -> list.sortedByDescending { net(it).abs() }
                    UpSetTagSortMode.INCREASING_ABS -> list.sortedBy { net(it).abs() }
                    UpSetTagSortMode.DECREASING     -> list.sortedByDescending { net(it) }
                    UpSetTagSortMode.INCREASING     -> list.sortedBy { net(it) }
                }
            }

        val topTags = tagTotals.map { it.first }.toSet()

        // 2. Compute Tag Combinations (Intersections)
        val combinationData = queryService.aggregate(
            profileId = profileId,
            dimensions = listOf(AggregationDimension.TAG_COMBINATIONS, AggregationDimension.CURRENCY),
            metrics = tagMetrics,
            filter = mergedFilter
        ).getOrDefault(emptyList())

        val intersections = combinationData.groupBy { it.dimensions[AggregationDimension.TAG_COMBINATIONS] ?: "" }
            .map { (tagSetString, rows) ->
                val tags = tagSetString.split(",").filter { it.isNotEmpty() }.toSet()
                val filteredTags = tags.intersect(topTags)
                
                var incomeTotal = BigDecimal.ZERO
                var expenseTotal = BigDecimal.ZERO
                val incByCurr = mutableMapOf<String, BigDecimal>()
                val expByCurr = mutableMapOf<String, BigDecimal>()

                rows.forEach { row ->
                    val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val inc = row.metrics["income"] ?: BigDecimal.ZERO
                    val exp = row.metrics["expense"] ?: BigDecimal.ZERO
                    incomeTotal += CurrencyService.convertAmount(inc, currency, baseCurrency, exchangeRates, 10)
                    expenseTotal += CurrencyService.convertAmount(exp, currency, baseCurrency, exchangeRates, 10)
                    
                    incByCurr[currency] = (incByCurr[currency] ?: BigDecimal.ZERO) + inc
                    expByCurr[currency] = (expByCurr[currency] ?: BigDecimal.ZERO) + exp
                }
                
                val count = rows.sumOf { it.count }
                
                TagUpSetIntersectionResult(
                    tags = filteredTags,
                    income = incomeTotal,
                    expense = expenseTotal,
                    incomeByCurrency = incByCurr.filterValues { it.signum() != 0 },
                    expenseByCurrency = expByCurr.filterValues { it.signum() != 0 },
                    net = incomeTotal.subtract(expenseTotal),
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
                    r.incomeByCurrency.forEach { (c, v) -> incByCurr[c] = (incByCurr[c] ?: BigDecimal.ZERO) + v }
                    r.expenseByCurrency.forEach { (c, v) -> expByCurr[c] = (expByCurr[c] ?: BigDecimal.ZERO) + v }
                }

                TagUpSetIntersection(
                    tags = tags,
                    amount = inc.subtract(exp).setScale(CurrencyService.precision, RoundingMode.HALF_UP),
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
                    UpSetIntersectionSortMode.DECREASING     -> list.sortedByDescending { it.amount }
                    UpSetIntersectionSortMode.INCREASING     -> list.sortedBy { it.amount }
                }
            }

        val sets = tagTotals.map { (tag, stats) ->
            TagUpSetSet(
                tagName = tag,
                totalAmount = stats.income.subtract(stats.expense).setScale(CurrencyService.precision, RoundingMode.HALF_UP),
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
            baseCurrency = baseCurrency
        )
    }
}
