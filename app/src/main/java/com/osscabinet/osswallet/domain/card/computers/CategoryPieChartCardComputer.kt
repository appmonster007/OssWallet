package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.CategoryPieChartCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal

class CategoryPieChartCardComputer :
    CardDataComputer<CategoryPieChartCardConfig, CardRenderData.CategoryPieChart> {
    override val supportedType = CardType.CATEGORY_PIE_CHART

    override suspend fun compute(input: CardInput, config: CategoryPieChartCardConfig): CardRenderData.CategoryPieChart {
        // Step 1: Apply base scope filter (screen-level: profile, account group, etc.)
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)

        // Step 2: Apply card config filter (user-selected: timeline, accounts, categories)
        // EXPAND categories to include children so they aren't filtered out
        // Also ensure transactionType is set (default to expense if not specified)
        val expandedFilter = config.filter.copy(
            categoryIds = TransactionFilterService.expandCategoryIds(
                config.filter.categoryIds,
                input.categories
            ),
            transactionTypes = run {
                val allowed = setOf(TransactionType.EXPENSE, TransactionType.INCOME)
                val valid = config.filter.transactionTypes.intersect(allowed)
                if (valid.size == 1) valid else setOf(TransactionType.EXPENSE)
            }
        )
        val filteredTx = TransactionFilterService.filter(baseTx, expandedFilter)

        // Step 3: Compute all values from filteredTx (already filtered by transaction type)
        val tree = AggregationService.buildCategoryTree(
            transactions = filteredTx,
            allCategories = input.categories,
            exchangeRates = input.exchangeRates,
            baseCurrency = input.baseCurrency
        )

        // Step 4: Compute previous period total if comparison is enabled
        val previousTotal: BigDecimal? = if (config.showComparison && expandedFilter.timelineRange !is TimelineRange.AllTime) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(expandedFilter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = expandedFilter.copy(
                    timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                )
                val prevTx = TransactionFilterService.filter(baseTx, prevFilter)
                val prevTree = AggregationService.buildCategoryTree(
                    transactions = prevTx,
                    allCategories = input.categories,
                    exchangeRates = input.exchangeRates,
                    baseCurrency = input.baseCurrency
                )
                prevTree.sumOf { it.netBalance.abs() }
            } else null
        } else null

        return CardRenderData.CategoryPieChart(config, tree, previousTotal)
    }
}
