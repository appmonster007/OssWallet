package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.query.AggregationDimension
import com.osscabinet.ossWallet.domain.model.query.StandardMetrics
import com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import java.math.BigDecimal
import java.math.RoundingMode

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
        val baseCurrency = input.baseCurrency
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
                rows.fold(BigDecimal.ZERO) { acc, row ->
                    val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val income = row.metrics["income"] ?: BigDecimal.ZERO
                    val expense = row.metrics["expense"] ?: BigDecimal.ZERO
                    val net = income.subtract(expense)
                    acc + CurrencyService.convertAmount(net, currency, baseCurrency, exchangeRates, 10)
                }
            }
            .filter { it.key != "unspecified" }
            .toList()
            .sortedByDescending { it.second.abs() }
            .take(config.maxTags)

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
                // Only keep intersections involving top tags
                val filteredTags = tags.intersect(topTags)
                
                val amount = rows.fold(BigDecimal.ZERO) { acc, row ->
                    val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val income = row.metrics["income"] ?: BigDecimal.ZERO
                    val expense = row.metrics["expense"] ?: BigDecimal.ZERO
                    val net = income.subtract(expense)
                    acc + CurrencyService.convertAmount(net, currency, baseCurrency, exchangeRates, 10)
                }
                
                val count = rows.sumOf { it.count }
                
                TagUpSetIntersection(
                    tags = filteredTags,
                    amount = amount.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    transactionCount = count
                )
            }
            .filter { it.tags.isNotEmpty() }
            // Re-group because after filtering tags, multiple original combinations might map to same top-tag intersection
            .groupBy { it.tags }
            .map { (tags, results) ->
                TagUpSetIntersection(
                    tags = tags,
                    amount = results.fold(BigDecimal.ZERO) { acc, r -> acc + r.amount },
                    transactionCount = results.sumOf { it.transactionCount }
                )
            }
            .sortedByDescending { it.amount.abs() }
            .take(config.maxIntersections)

        val sets = tagTotals.map { (tag, amount) ->
            TagUpSetSet(
                tagName = tag,
                totalAmount = amount.setScale(CurrencyService.precision, RoundingMode.HALF_UP)
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
