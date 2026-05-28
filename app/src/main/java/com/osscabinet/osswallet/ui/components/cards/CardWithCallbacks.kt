package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.TransactionFilter

/**
 * Sealed class hierarchy that pairs specific card render data with its corresponding callbacks.
 * This ensures type safety at the compiler level, eliminating the need for unsafe casts
 * in the renderer.
 */
sealed class CardWithCallbacks {
    abstract val filterRules: CardFilterRules
    abstract val baseFilter: TransactionFilter
    open val filterSheetEnabled: Boolean = true

    data class NetWorth(
        val renderData: CardRenderData.NetWorth,
        val callbacks: CardCallbacks.NetWorth,
        override val filterRules: CardFilterRules = CardFilterRules.DEFAULT,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class CurrencyBalances(
        val renderData: CardRenderData.CurrencyBalances,
        val callbacks: CardCallbacks.CurrencyBalances,
        override val filterRules: CardFilterRules = CardFilterRules.DEFAULT,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class CashFlow(
        val renderData: CardRenderData.CashFlow,
        val callbacks: CardCallbacks.CashFlow,
        override val filterRules: CardFilterRules = CardFilterRules.DEFAULT,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class Accounts(
        val renderData: CardRenderData.Accounts,
        val callbacks: CardCallbacks.Accounts,
        override val filterRules: CardFilterRules = CardFilterRules.DEFAULT,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class CategoryPieChart(
        val renderData: CardRenderData.CategoryPieChart,
        val callbacks: CardCallbacks.Analytics,
        override val filterRules: CardFilterRules = CardFilterRules.INCOME_OR_EXPENSE_ONLY,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class CategoryTreemap(
        val renderData: CardRenderData.CategoryTreemap,
        val callbacks: CardCallbacks.Analytics,
        override val filterRules: CardFilterRules = CardFilterRules.INCOME_OR_EXPENSE_ONLY,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class AccountHero(
        val renderData: CardRenderData.AccountHero,
        val callbacks: CardCallbacks.AccountHero,
        override val filterRules: CardFilterRules = CardFilterRules.DEFAULT,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class PlannedPayments(
        val renderData: CardRenderData.PlannedPayments,
        val callbacks: CardCallbacks.PlannedPayments,
        override val filterRules: CardFilterRules = CardFilterRules.DEFAULT,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class Budgets(
        val renderData: CardRenderData.Budgets,
        val callbacks: CardCallbacks.Budgets,
        override val filterRules: CardFilterRules = CardFilterRules.DEFAULT,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class Loans(
        val renderData: CardRenderData.Loans,
        val callbacks: CardCallbacks.Loans,
        override val filterRules: CardFilterRules = CardFilterRules.DEFAULT,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class RecentActivities(
        val renderData: CardRenderData.RecentActivities,
        val callbacks: CardCallbacks.RecentActivities,
        override val filterRules: CardFilterRules = CardFilterRules.DEFAULT,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class CashFlowComparison(
        val renderData: CardRenderData.CashFlowComparison,
        val callbacks: CardCallbacks.CashFlowComparison,
        override val filterRules: CardFilterRules = CardFilterRules.INCOME_OR_EXPENSE_ONLY,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class IncomeExpenseBar(
        val renderData: CardRenderData.IncomeExpenseBar,
        val callbacks: CardCallbacks.IncomeExpenseBar,
        override val filterRules: CardFilterRules = CardFilterRules.INCOME_OR_EXPENSE_ONLY,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class CategorySpendingTrend(
        val renderData: CardRenderData.CategorySpendingTrend,
        val callbacks: CardCallbacks.CategorySpendingTrend,
        override val filterRules: CardFilterRules = CardFilterRules.SPENDING_LOCKED_CATEGORIES,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class TagUpSetPlot(
        val renderData: CardRenderData.TagUpSetPlot,
        val callbacks: CardCallbacks.TagUpSetPlot,
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        override val filterRules: CardFilterRules = CardFilterRules.INCOME_OR_EXPENSE_ONLY,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()

    data class ComputeError(
        val cardId: String,
        val message: String?,
        override val filterRules: CardFilterRules = CardFilterRules.DEFAULT,
        override val baseFilter: TransactionFilter = TransactionFilter(),
        override val filterSheetEnabled: Boolean = true
    ) : CardWithCallbacks()
}
