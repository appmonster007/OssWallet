package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
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
        // Step 1 & 2: Resolve effective filter using the canonical pipeline
        val mergedFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)

        // Expand category IDs to include children
        val expandedCategoryIds = TransactionFilterService.expandCategoryIds(
            mergedFilter.categoryIds,
            input.categories
        )

        // Resolve transaction type: use whatever is in mergedFilter.transactionTypes.
        // If it's a single value, use it. If empty or multiple, default to EXPENSE.
        val resolvedTypes = if (mergedFilter.transactionTypes.size == 1) {
            mergedFilter.transactionTypes
        } else {
            setOf(TransactionType.EXPENSE)
        }

        val finalFilter = mergedFilter.copy(
            categoryIds = expandedCategoryIds,
            transactionTypes = resolvedTypes
        )

        // Single-pass filter against all transactions
        val filteredTx = TransactionFilterService.filter(input.transactions, finalFilter)

        // Step 3: Compute all values from filteredTx (already filtered by transaction type)
        val tree = AggregationService.buildCategoryTree(
            transactions = filteredTx,
            allCategories = input.categories,
            exchangeRates = input.exchangeRates,
            baseCurrency = input.displayCurrency
        )

        // Step 4: Compute previous period total if comparison is enabled
        val previousTotal: BigDecimal? = if (config.showComparison && finalFilter.timelineRange !is TimelineRange.AllTime) {
            val (prevStart, prevEnd) = TimelineService.getPreviousRange(finalFilter.timelineRange)
            if (prevStart != null && prevEnd != null) {
                val prevFilter = finalFilter.copy(
                    timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                )
                val prevTx = TransactionFilterService.filter(input.transactions, prevFilter)
                val prevTree = AggregationService.buildCategoryTree(
                    transactions = prevTx,
                    allCategories = input.categories,
                    exchangeRates = input.exchangeRates,
                    baseCurrency = input.displayCurrency
                )
                prevTree.sumOf { it.netBalance.abs() }
            } else null
        } else null

        return CardRenderData.CategoryPieChart(config, tree, input.displayCurrency, previousTotal)
    }
}
