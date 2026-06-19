package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal

class CategoryTreemapCardComputer :
    CardDataComputer<CategoryTreemapCardConfig, CardRenderData.CategoryTreemap> {
    override val supportedType = CardType.CATEGORY_TREEMAP

    override suspend fun compute(
        input: CardInput,
        config: CategoryTreemapCardConfig
    ): CardRenderData.CategoryTreemap {
        // Step 1 & 2: Resolve effective filter using the canonical pipeline
        val mergedFilter = resolveEffectiveFilter(
            input.filter,
            config.filter,
            config.filterRules,
            input.screenOwnsTimeline
        )

        // Expand category IDs to include children using the pre-computed cache
        val expandedCategoryIds = input.categoryHierarchy.expand(mergedFilter.categoryIds)

        // Resolve transaction types: include all types to support Transfers as categories
        val resolvedTypes = TransactionType.entries.toSet()

        val excludeInternal = (config.perspective as? AccountingPerspective.CashFlow)?.excludeInternalTransfers ?: false
        val finalFilter = mergedFilter.copy(
            accountIds = mergedFilter.accountIds.ifEmpty { input.effectiveAccountIds },
            categoryIds = expandedCategoryIds,
            transactionTypes = resolvedTypes
        )

        // Single-pass filter against the correct transaction source (includes archived if explicitly selected)
        val txSource = input.transactionsFor(finalFilter)
        val filteredTx = TransactionFilterService.filter(txSource, finalFilter, excludeInternal)

        // Step 4: Compute previous period transactions if comparison is enabled
        val previousTx: List<Transaction>? =
            if (config.showComparison && finalFilter.timelineRange !is TimelineRange.AllTime) {
                val (prevStart, prevEnd) = TimelineService.getPreviousRange(finalFilter.timelineRange)
                if (prevStart != null && prevEnd != null) {
                    val prevFilter = finalFilter.copy(
                        timelineRange = TimelineRange.CustomRange(prevStart, prevEnd)
                    )
                    TransactionFilterService.filter(txSource, prevFilter, excludeInternal)
                } else null
            } else null

        // Step 3: Compute all values from filteredTx
        val tree = AggregationService.buildCategoryTree(
            transactions = filteredTx,
            allCategories = input.categories,
            exchangeRates = input.exchangeRates.rates,
            baseCurrency = input.targetCurrency,
            previousTransactions = previousTx,
            perspective = config.perspective
        )

        val incomeTotal = tree.sumOf { it.income }
        val expenseTotal = tree.sumOf { it.expense }
        val transferTotal = tree.sumOf { it.transfer }

        val previousIncomeTotal = previousTx?.let { tree.sumOf { it.previousIncome ?: BigDecimal.ZERO } }
        val previousExpenseTotal = previousTx?.let { tree.sumOf { it.previousExpense ?: BigDecimal.ZERO } }
        val previousTransferTotal = previousTx?.let { tree.sumOf { it.previousTransfer ?: BigDecimal.ZERO } }
        val previousTotal = previousTx?.let { tree.sumOf { it.previousNetBalance ?: BigDecimal.ZERO } }

        return CardRenderData.CategoryTreemap(
            config = config,
            items = tree,
            currency = input.targetCurrency,
            previousTotal = previousTotal,
            incomeTotal = incomeTotal,
            expenseTotal = expenseTotal,
            transferTotal = transferTotal,
            previousIncomeTotal = previousIncomeTotal,
            previousExpenseTotal = previousExpenseTotal,
            previousTransferTotal = previousTransferTotal
        )
    }
}
