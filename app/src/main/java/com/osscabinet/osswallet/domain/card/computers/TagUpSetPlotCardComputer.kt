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
import com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig
import com.osscabinet.ossWallet.domain.model.settings.UpSetIntersectionSortMode
import com.osscabinet.ossWallet.domain.model.settings.UpSetTagSortMode
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.service.financial.extractFrom
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import java.math.BigDecimal
import java.math.RoundingMode

// All fields are raw. inflow/outflow properties are used for perspective-specific logic if needed.
private data class IndividualTagStats(
    val metrics: FinancialSummary,
    val metricsByCurrency: Map<String, FinancialSummary>
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
                var rawIncome  = BigDecimal.ZERO
                var rawExpense = BigDecimal.ZERO
                var rawTrfIn   = BigDecimal.ZERO
                var rawTrfOut  = BigDecimal.ZERO
                var countTotal = 0
                val metricsByCurr = mutableMapOf<String, FinancialSummary>()

                rows.forEach { row ->
                    val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val inc  = row.metrics["income"]      ?: BigDecimal.ZERO
                    val exp  = row.metrics["expense"]     ?: BigDecimal.ZERO
                    val trIn = row.metrics["transferIn"]  ?: BigDecimal.ZERO
                    val trOut= row.metrics["transferOut"] ?: BigDecimal.ZERO

                    val rowPs = FinancialSummary(
                        income = inc,
                        expense = exp,
                        transferIn = trIn,
                        transferOut = trOut,
                        transactionCount = row.count
                    )
                    metricsByCurr[currency] = (metricsByCurr[currency] ?: FinancialSummary.EMPTY) + rowPs

                    // Accumulate raw primitives in base currency
                    rawIncome  = rawIncome.add(CurrencyService.convertAmount(inc,  currency, baseCurrency, exchangeRates, 10))
                    rawExpense = rawExpense.add(CurrencyService.convertAmount(exp,  currency, baseCurrency, exchangeRates, 10))
                    rawTrfIn   = rawTrfIn.add(CurrencyService.convertAmount(trIn,  currency, baseCurrency, exchangeRates, 10))
                    rawTrfOut  = rawTrfOut.add(CurrencyService.convertAmount(trOut, currency, baseCurrency, exchangeRates, 10))
                    countTotal += row.count
                }

                val ps = FinancialSummary(
                    income = rawIncome,
                    expense = rawExpense,
                    transferIn = rawTrfIn,
                    transferOut = rawTrfOut,
                    transactionCount = countTotal
                )

                IndividualTagStats(
                    metrics = ps.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    metricsByCurrency = metricsByCurr.mapValues { it.value.setScale(CurrencyService.precision, RoundingMode.HALF_UP) }
                )
            }
            .toList()
            .let { list ->
                val metricValue = { p: Pair<String, IndividualTagStats> ->
                    config.selectedMetric.extractFrom(p.second.metrics)
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
                var rawIncome  = BigDecimal.ZERO
                var rawExpense = BigDecimal.ZERO
                var rawTrfIn   = BigDecimal.ZERO
                var rawTrfOut  = BigDecimal.ZERO
                val metricsByCurr = mutableMapOf<String, FinancialSummary>()
                var count = 0

                rows.forEach { row ->
                    val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val inc  = row.metrics["income"]      ?: BigDecimal.ZERO
                    val exp  = row.metrics["expense"]     ?: BigDecimal.ZERO
                    val trIn = row.metrics["transferIn"]  ?: BigDecimal.ZERO
                    val trOut= row.metrics["transferOut"] ?: BigDecimal.ZERO

                    val rowPs = FinancialSummary(
                        income = inc,
                        expense = exp,
                        transferIn = trIn,
                        transferOut = trOut,
                        transactionCount = row.count
                    )
                    metricsByCurr[currency] = (metricsByCurr[currency] ?: FinancialSummary.EMPTY) + rowPs

                    rawIncome  = rawIncome.add(CurrencyService.convertAmount(inc,  currency, baseCurrency, exchangeRates, 10))
                    rawExpense = rawExpense.add(CurrencyService.convertAmount(exp,  currency, baseCurrency, exchangeRates, 10))
                    rawTrfIn   = rawTrfIn.add(CurrencyService.convertAmount(trIn,  currency, baseCurrency, exchangeRates, 10))
                    rawTrfOut  = rawTrfOut.add(CurrencyService.convertAmount(trOut, currency, baseCurrency, exchangeRates, 10))
                    count += row.count
                }

                val ps = FinancialSummary(
                    income = rawIncome,
                    expense = rawExpense,
                    transferIn = rawTrfIn,
                    transferOut = rawTrfOut,
                    transactionCount = count
                )

                TagUpSetIntersection(
                    tags = filteredTags,
                    metrics = ps.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    metricsByCurrency = metricsByCurr.mapValues { it.value.setScale(CurrencyService.precision, RoundingMode.HALF_UP) }
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
            TagUpSetSet(
                tagName = tag,
                metrics = stats.metrics,
                metricsByCurrency = stats.metricsByCurrency
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
