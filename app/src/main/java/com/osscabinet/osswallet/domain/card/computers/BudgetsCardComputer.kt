package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.card.resolveEffectiveFilter
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.BudgetService

class BudgetsCardComputer : CardDataComputer<BudgetsCardConfig, CardRenderData.Budgets> {
    override val supportedType = CardType.BUDGETS

    override suspend fun compute(input: CardInput, config: BudgetsCardConfig): CardRenderData.Budgets {
        val now = System.currentTimeMillis()

        // Resolve effective filter using canonical pipeline, then single-pass filter
        val mergedFilter = resolveEffectiveFilter(input.filter, config.filter, config.filterRules, input.screenOwnsTimeline)
        val filteredTx = TransactionFilterService.filter(input.transactions, mergedFilter)

        // BudgetService applies its own period/category/account rules on top of filteredTx.
        val budgets = if (config.showArchived) input.budgets
        else input.budgets.filter { !it.isArchived }

        val items = budgets.take(config.maxItems).map { budget ->
            val rollover = BudgetService.calculateRollover(
                budget = budget,
                transactions = filteredTx,
                exchangeRates = input.exchangeRates,
                baseCurrency = input.baseCurrency,
                now = now
            )
            val spent = BudgetService.calculateSpent(
                budget = budget,
                transactions = filteredTx,
                exchangeRates = input.exchangeRates,
                baseCurrency = input.baseCurrency,
                now = now
            )
            BudgetService.buildProgress(budget, spent, rollover)
        }

        return CardRenderData.Budgets(config = config, items = items)
    }
}
