package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService

class CategoryBreakdownCardComputer : CardDataComputer<CategoryBreakdownCardConfig, CardRenderData.CategoryBreakdown> {
    override val supportedType = CardType.CATEGORY_BREAKDOWN

    override fun compute(input: CardInput, config: CategoryBreakdownCardConfig): CardRenderData.CategoryBreakdown {
        val tree = AggregationService.buildCategoryTree(
            transactions = input.transactions,
            allCategories = input.categories,
            exchangeRates = input.exchangeRates,
            baseCurrency = input.baseCurrency,
            onlyExpenses = false
        )
        return CardRenderData.CategoryBreakdown(config, tree)
    }
}
