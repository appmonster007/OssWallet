package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.service.BudgetService
import com.osscabinet.ossWallet.domain.service.TransactionFilterService

class BudgetsCardComputer : CardDataComputer<BudgetsCardConfig, CardRenderData.Budgets> {
    override val supportedType = CardType.BUDGETS

    override fun compute(input: CardInput, config: BudgetsCardConfig): CardRenderData.Budgets {
        val now = System.currentTimeMillis()

        // Step 1: Apply base scope filter (screen-level: profile, account group, etc.)
        val baseTx = TransactionFilterService.filter(input.transactions, input.filter)

        // Step 2: Apply card config filter (user-selected: timeline, accounts, categories)
        val filteredTx = TransactionFilterService.filter(baseTx, config.filter)

        // Step 3: Compute all values from filteredTx
        // BudgetService applies its own period/category/account rules on top of filteredTx.
        val budgets = if (config.showArchived) input.budgets
                      else input.budgets.filter { !it.isArchived }

        val items = budgets.take(config.maxItems).map { budget ->
            val rollover = BudgetService.calculateRollover(
                budget = budget,
                transactions = filteredTx,
                exchangeRates = input.exchangeRates,
                baseCurrency = input.baseCurrency,
                precision = input.precision,
                now = now
            )
            val spent = BudgetService.calculateSpent(
                budget = budget,
                transactions = filteredTx,
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
