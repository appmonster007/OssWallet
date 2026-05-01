package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.service.BudgetService

class BudgetsCardComputer : CardDataComputer<BudgetsCardConfig, CardRenderData.Budgets> {
    override val supportedType = CardType.BUDGETS

    override fun compute(input: CardInput, config: BudgetsCardConfig): CardRenderData.Budgets {
        val now = System.currentTimeMillis()
        val budgets = if (config.showArchived) input.budgets
                      else input.budgets.filter { !it.isArchived }

        val items = budgets.take(config.maxItems).map { budget ->
            val rollover = BudgetService.calculateRollover(
                budget = budget,
                transactions = input.transactions,
                exchangeRates = input.exchangeRates,
                baseCurrency = input.baseCurrency,
                precision = input.precision,
                now = now
            )
            val spent = BudgetService.calculateSpent(
                budget = budget,
                transactions = input.transactions,
                exchangeRates = input.exchangeRates,
                baseCurrency = input.baseCurrency,
                precision = input.precision,
                now = now
            )
            BudgetService.buildProgress(budget, spent, rollover)
        }

        return CardRenderData.Budgets(config = config, items = items)
    }
}
