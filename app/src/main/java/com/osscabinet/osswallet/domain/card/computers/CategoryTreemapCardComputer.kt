package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService

class CategoryTreemapCardComputer :
    CardDataComputer<CategoryTreemapCardConfig, CardRenderData.CategoryTreemap> {
    override val supportedType = CardType.CATEGORY_TREEMAP

    override suspend fun compute(input: CardInput, config: CategoryTreemapCardConfig): CardRenderData.CategoryTreemap {
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
            transactionTypes = if (config.filter.transactionTypes.isEmpty()) setOf(TransactionType.EXPENSE) else config.filter.transactionTypes
        )
        val filteredTx = TransactionFilterService.filter(baseTx, expandedFilter)

        // Step 3: Compute all values from filteredTx (already filtered by transaction type)
        val tree = AggregationService.buildCategoryTree(
            transactions = filteredTx,
            allCategories = input.categories,
            exchangeRates = input.exchangeRates,
            baseCurrency = input.baseCurrency,
            precision = input.precision
        )

        return CardRenderData.CategoryTreemap(config, tree, input.precision)
    }
}
