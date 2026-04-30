package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService

class CategoryTreemapCardComputer : CardDataComputer<CategoryExpenseTreemapCardConfig, CardRenderData.CategoryTreemap> {
    override val supportedType = CardType.CATEGORY_EXPENSE_TREEMAP

    override fun compute(input: CardInput, config: CategoryExpenseTreemapCardConfig): CardRenderData.CategoryTreemap {
        val filteredTx = com.osscabinet.ossWallet.domain.service.TransactionFilterService.filter(input.transactions, config.filter)
        val tree = AggregationService.buildCategoryTree(
            transactions = filteredTx,
            allCategories = input.categories,
            exchangeRates = input.exchangeRates,
            baseCurrency = input.baseCurrency,
            onlyExpenses = true
        )
        return CardRenderData.CategoryTreemap(config, tree)
    }
}
