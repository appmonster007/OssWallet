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

    override suspend fun compute(
        input: CardInput,
        config: BudgetsCardConfig
    ): CardRenderData.Budgets {
        val now = System.currentTimeMillis()

        // Budgets should NOT follow the global timeline filter (e.g. Dashboard "Today")
        // but should respect global Account/Category filters if explicitly merged.
        // We use a clean filter for budgets that ignores the timeline of input.filter.
        val baseBudgetFilter =
            input.filter.copy(timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
        val mergedFilter = resolveEffectiveFilter(
            baseBudgetFilter,
            config.filter,
            config.filterRules,
            input.screenOwnsTimeline
        )
        val allTransactions = input.allProfileTransactions.ifEmpty { input.transactions }
        val filteredTx = TransactionFilterService.filter(allTransactions, mergedFilter)

        val budgets = if (config.showArchived) input.budgets
        else input.budgets.filter { !it.isArchived }

        val items = budgets.take(config.maxItems).map { budget ->
            val rollover = BudgetService.calculateRollover(
                budget = budget,
                transactions = filteredTx,
                exchangeRates = input.exchangeRates,
                baseCurrency = input.displayCurrency,
                now = now,
                allCategories = input.categories
            )
            val spent = BudgetService.calculateSpent(
                budget = budget,
                transactions = filteredTx,
                exchangeRates = input.exchangeRates,
                baseCurrency = input.displayCurrency,
                now = now,
                allCategories = input.categories
            )
            BudgetService.buildProgress(budget, spent, rollover)
        }

        return CardRenderData.Budgets(
            config = config,
            items = items,
            displayCurrency = input.displayCurrency
        )
    }
}
