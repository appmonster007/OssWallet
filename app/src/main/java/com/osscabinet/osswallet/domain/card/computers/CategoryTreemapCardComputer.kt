package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.CategoryTreemapCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.TransactionFilterService
import com.osscabinet.ossWallet.domain.model.settings.TransactionType

class CategoryTreemapCardComputer : CardDataComputer<CategoryTreemapCardConfig, CardRenderData.CategoryTreemap> {
    override val supportedType = CardType.CATEGORY_TREEMAP

    override fun compute(input: CardInput, config: CategoryTreemapCardConfig): CardRenderData.CategoryTreemap {
        // Step 1: Apply base scope filter (screen-level: profile, account group, etc.)
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)

        // Step 2: Apply card config filter (user-selected: timeline, accounts, categories)
        // EXPAND categories to include children so they aren't filtered out
        // Also set transactionType in filter to handle expense/income filtering
        val expandedFilter = config.filter.copy(
            categoryIds = TransactionFilterService.expandCategoryIds(config.filter.categoryIds, input.categories),
            transactionType = when (config.transactionType) {
                TransactionType.EXPENSE_ONLY -> "expense"
                TransactionType.INCOME_ONLY -> "income"
            }
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
