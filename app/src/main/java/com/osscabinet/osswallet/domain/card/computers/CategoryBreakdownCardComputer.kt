package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.TransactionFilterService

class CategoryBreakdownCardComputer : CardDataComputer<CategoryBreakdownCardConfig, CardRenderData.CategoryBreakdown> {
    override val supportedType = CardType.CATEGORY_BREAKDOWN

    override fun compute(input: CardInput, config: CategoryBreakdownCardConfig): CardRenderData.CategoryBreakdown {
        // Step 1: Filter
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)

        // Step 2: Compute
        val tree = AggregationService.buildCategoryTree(
            transactions = filteredTx,
            allCategories = input.categories,
            exchangeRates = input.exchangeRates,
            baseCurrency = input.baseCurrency,
            onlyExpenses = false
        )

        // Step 3: Build render data
        return CardRenderData.CategoryBreakdown(config, tree)
    }
}
