package com.osscabinet.ossWallet.domain.calculator

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService

class CategoryBreakdownCalculator : CardCalculator<CategoryBreakdownCardConfig, CardRenderData.CategoryBreakdownResult> {
    override fun compute(
        config: CategoryBreakdownCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        context: ComputationContext
    ): CardRenderData.CategoryBreakdownResult {
        val tree = AggregationService.buildCategoryTree(
            transactions = transactions,
            allCategories = categories,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency,
            onlyExpenses = false
        )
        return CardRenderData.CategoryBreakdownResult(tree)
    }
}
