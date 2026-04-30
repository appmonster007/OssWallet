package com.osscabinet.ossWallet.domain.calculator

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.domain.service.AggregationService

class CategoryTreemapCalculator : CardCalculator<CategoryExpenseTreemapCardConfig, CardRenderData.CategoryTreemapResult> {
    override fun compute(
        config: CategoryExpenseTreemapCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        context: ComputationContext
    ): CardRenderData.CategoryTreemapResult {
        val tree = AggregationService.buildCategoryTree(
            transactions = transactions,
            allCategories = categories,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency,
            onlyExpenses = true
        )
        return CardRenderData.CategoryTreemapResult(tree)
    }
}
