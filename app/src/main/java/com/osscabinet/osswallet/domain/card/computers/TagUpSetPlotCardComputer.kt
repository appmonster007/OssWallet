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

// All fields are perspective-resolved (toPerspective already applied).
private data class IndividualTagStats(
    val income: BigDecimal,
    val expense: BigDecimal,
    val transfer: BigDecimal,
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
                var rawIncome  = BigDecimal.ZERO
                var rawExpense = BigDecimal.ZERO
                var rawTrfIn   = BigDecimal.ZERO
                var rawTrfOut  = BigDecimal.ZERO
                var countTotal = 0
                val incByCurr  = mutableMapOf<String, BigDecimal>()
                val expByCurr  = mutableMapOf<String, BigDecimal>()

                rows.forEach { row ->
                    val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val inc  = row.metrics["income"]      ?: BigDecimal.ZERO
                    val exp  = row.metrics["expense"]     ?: BigDecimal.ZERO
                    val trIn = row.metrics["transferIn"]  ?: BigDecimal.ZERO
                    val trOut= row.metrics["transferOut"] ?: BigDecimal.ZERO

                    // Perspective-resolved native breakdown (no currency conversion)
                    val rowPs = FinancialSummary(
                        totalAmount = BigDecimal.ZERO, income = inc, expense = exp,
                        transfer = trIn.add(trOut), transferIn = trIn, transferOut = trOut,
                        transactionCount = 0
                    ).toPerspective(config.perspective)
                    incByCurr[currency] = (incByCurr[currency] ?: BigDecimal.ZERO).add(rowPs.income)
                    expByCurr[currency] = (expByCurr[currency] ?: BigDecimal.ZERO).add(rowPs.expense)

                    // Accumulate raw primitives in base currency
                    rawIncome  = rawIncome.add(CurrencyService.convertAmount(inc,  currency, baseCurrency, exchangeRates, 10))
                    rawExpense = rawExpense.add(CurrencyService.convertAmount(exp,  currency, baseCurrency, exchangeRates, 10))
                    rawTrfIn   = rawTrfIn.add(CurrencyService.convertAmount(trIn,  currency, baseCurrency, exchangeRates, 10))
                    rawTrfOut  = rawTrfOut.add(CurrencyService.convertAmount(trOut, currency, baseCurrency, exchangeRates, 10))
                    countTotal += row.count
                }

                val ps = FinancialSummary(
                    totalAmount = BigDecimal.ZERO,
                    income = rawIncome, expense = rawExpense,
                    transfer = rawTrfIn.add(rawTrfOut), transferIn = rawTrfIn, transferOut = rawTrfOut,
                    transactionCount = 0
                ).toPerspective(config.perspective)

                IndividualTagStats(
                    income = ps.income, expense = ps.expense, transfer = ps.transfer,
                    count = countTotal,
                    incomeByCurrency = incByCurr.filterValues { it.signum() != 0 },
                    expenseByCurrency = expByCurr.filterValues { it.signum() != 0 }
                )
            }
            .toList()
            .let { list ->
                val metricValue = { p: Pair<String, IndividualTagStats> ->
                    // income/expense/transfer are already perspective-resolved
                    config.selectedMetric.extractFrom(FinancialSummary(
                        totalAmount = BigDecimal.ZERO,
                        income = p.second.income, expense = p.second.expense,
                        transfer = p.second.transfer, transactionCount = 0
                    ))
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
                val incByCurr  = mutableMapOf<String, BigDecimal>()
                val expByCurr  = mutableMapOf<String, BigDecimal>()
                var count = 0

                rows.forEach { row ->
                    val currency = row.dimensions[AggregationDimension.CURRENCY] ?: baseCurrency
                    val inc  = row.metrics["income"]      ?: BigDecimal.ZERO
                    val exp  = row.metrics["expense"]     ?: BigDecimal.ZERO
                    val trIn = row.metrics["transferIn"]  ?: BigDecimal.ZERO
                    val trOut= row.metrics["transferOut"] ?: BigDecimal.ZERO

                    // Perspective-resolved native breakdown (no currency conversion)
                    val rowPs = FinancialSummary(
                        totalAmount = BigDecimal.ZERO, income = inc, expense = exp,
                        transfer = trIn.add(trOut), transferIn = trIn, transferOut = trOut,
                        transactionCount = 0
                    ).toPerspective(config.perspective)
                    incByCurr[currency] = (incByCurr[currency] ?: BigDecimal.ZERO).add(rowPs.income)
                    expByCurr[currency] = (expByCurr[currency] ?: BigDecimal.ZERO).add(rowPs.expense)

                    rawIncome  = rawIncome.add(CurrencyService.convertAmount(inc,  currency, baseCurrency, exchangeRates, 10))
                    rawExpense = rawExpense.add(CurrencyService.convertAmount(exp,  currency, baseCurrency, exchangeRates, 10))
                    rawTrfIn   = rawTrfIn.add(CurrencyService.convertAmount(trIn,  currency, baseCurrency, exchangeRates, 10))
                    rawTrfOut  = rawTrfOut.add(CurrencyService.convertAmount(trOut, currency, baseCurrency, exchangeRates, 10))
                    count += row.count
                }

                val ps = FinancialSummary(
                    totalAmount = BigDecimal.ZERO,
                    income = rawIncome, expense = rawExpense,
                    transfer = rawTrfIn.add(rawTrfOut), transferIn = rawTrfIn, transferOut = rawTrfOut,
                    transactionCount = 0
                ).toPerspective(config.perspective)

                val amount = config.selectedMetric.extractFrom(ps)

                TagUpSetIntersection(
                    tags = filteredTags,
                    amount = amount.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    income = ps.income.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    expense = ps.expense.setScale(CurrencyService.precision, RoundingMode.HALF_UP),
                    incomeByCurrency = incByCurr.filterValues { it.signum() != 0 },
                    expenseByCurrency = expByCurr.filterValues { it.signum() != 0 },
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
            // income/expense/transfer are already perspective-resolved
            val total = config.selectedMetric.extractFrom(FinancialSummary(
                totalAmount = BigDecimal.ZERO,
                income = stats.income, expense = stats.expense,
                transfer = stats.transfer, transactionCount = 0
            ))
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
