package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.Transaction
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

    override suspend fun compute(
        input: CardInput,
        config: CategoryPieChartCardConfig
    ): CardRenderData.CategoryPieChart {
        // Step 1 & 2: Resolve effective filter using the canonical pipeline
        val mergedFilter = resolveEffectiveFilter(
            input.filter,
            config.filter,
            config.filterRules,
            input.screenOwnsTimeline
        )

        // Expand category IDs to include children using the pre-computed cache
        val expandedCategoryIds = input.categoryHierarchy.expand(mergedFilter.categoryIds)

        // Resolve transaction types: we now always fetch both to support the in-card toggle.
        val resolvedTypes = setOf(TransactionType.EXPENSE, TransactionType.INCOME)

        val finalFilter = mergedFilter.copy(
            categoryIds = expandedCategoryIds,
            transactionTypes = resolvedTypes
        )

        // Single-pass filter against all transactions
        val filteredTx = TransactionFilterService.filter(input.transactions, finalFilter)

        // Step 4: Compute previous period transactions if comparison is enabled
        val previousTx: List<Transaction>? =
            if (config.showComparison && finalFilter.timelineRange !is TimelineRange.AllTime) {
                val (prevStart, prevEnd) = TimelineService.getPreviousRange(finalFilter.timelineRange)
                if (prevStart != null && prevEnd != null) {
                    val prevFilter = finalFilter.copy(
                        timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                    )
                    TransactionFilterService.filter(input.transactions, prevFilter)
                } else null
            } else null

        // Step 3: Compute all values from filteredTx
        val tree = AggregationService.buildCategoryTree(
            transactions = filteredTx,
            allCategories = input.categories,
            exchangeRates = input.exchangeRates,
            baseCurrency = input.displayCurrency,
            previousTransactions = previousTx
        )

        val incomeTotal = tree.sumOf { it.income.abs() }
        val expenseTotal = tree.sumOf { it.expense.abs() }

        val previousIncomeTotal = previousTx?.let { tree.sumOf { it.previousIncome?.abs() ?: BigDecimal.ZERO } }
        val previousExpenseTotal = previousTx?.let { tree.sumOf { it.previousExpense?.abs() ?: BigDecimal.ZERO } }
        val previousTotal = previousTx?.let { tree.sumOf { it.previousNetBalance?.abs() ?: BigDecimal.ZERO } }

        return CardRenderData.CategoryPieChart(
            config = config,
            items = tree,
            displayCurrency = input.displayCurrency,
            previousTotal = previousTotal,
            incomeTotal = incomeTotal,
            expenseTotal = expenseTotal,
            previousIncomeTotal = previousIncomeTotal,
            previousExpenseTotal = previousExpenseTotal
        )
    }
}
