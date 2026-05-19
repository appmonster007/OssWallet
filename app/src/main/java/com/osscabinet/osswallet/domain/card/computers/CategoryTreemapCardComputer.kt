package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService

class CategoryTreemapCardComputer :
    CardDataComputer<CategoryTreemapCardConfig, CardRenderData.CategoryTreemap> {
    override val supportedType = CardType.CATEGORY_TREEMAP

    override suspend fun compute(input: CardInput, config: CategoryTreemapCardConfig): CardRenderData.CategoryTreemap {
        // Step 1 & 2: Resolve effective filter using the canonical pipeline
        val mergedFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules)

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
            baseCurrency = input.baseCurrency
        )

        return CardRenderData.CategoryTreemap(config, tree)
    }
}
