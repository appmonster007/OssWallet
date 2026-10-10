package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.runtime.Stable
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Sealed class hierarchy for unified card callbacks.
 * Each subtype represents the callbacks available for a specific card type.
 *
 * Note: Callbacks match the actual signatures expected by card components,
 * which often include cardId as the first parameter.
 */
@Stable
sealed class CardCallbacks {

    /**
     * Common callbacks - just delete
     */
    data class Common(
        val onDelete: (() -> Unit)? = null,
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for Net Worth
     */
    data class NetWorth(
        val onFontSizeChange: (String, String) -> Unit = { _, _ -> },
        val onShowComparisonChange: (String, Boolean) -> Unit = { _, _ -> },
        val onShowTrendLineChange: (String, Boolean) -> Unit = { _, _ -> },
        val onPerspectiveChange: (String, com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective) -> Unit = { _, _ -> },
        val onExcludeInternalTransfersChange: (String, Boolean) -> Unit = { _, _ -> },
        val onSplitTransfersChange: (String, Boolean) -> Unit = { _, _ -> },
        val onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
        val onClickIncome: ((TransactionFilter) -> Unit)? = null,
        val onClickExpense: ((TransactionFilter) -> Unit)? = null,
        val onClickTransfer: ((TransactionFilter) -> Unit)? = null,
        val onClickInflow: ((TransactionFilter) -> Unit)? = null,
        val onClickOutflow: ((TransactionFilter) -> Unit)? = null,
        val onClickNet: ((TransactionFilter) -> Unit)? = null,
        val onClickNetFlow: ((TransactionFilter) -> Unit)? = null,
        val onSeeAll: () -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for currency balances
     */
    data class CurrencyBalances(
        val onGridColumnsChange: (String, Int) -> Unit = { _, _ -> },
        val onLayoutChange: (String, String) -> Unit = { _, _ -> },
        val onShowBaseConversionChange: (String, Boolean) -> Unit = { _, _ -> },
        val onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
        val onCurrencyClick: ((String, TimelineRange) -> Unit)? = null,
        val onSeeAll: () -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for cash flow card
     */
    data class CashFlow(
        val onShowBaseConversionChange: (String, Boolean) -> Unit = { _, _ -> },
        val onShowComparisonChange: (String, Boolean) -> Unit = { _, _ -> },
        val onPerspectiveChange: (String, com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective) -> Unit = { _, _ -> },
        val onExcludeInternalTransfersChange: (String, Boolean) -> Unit = { _, _ -> },
        val onSplitTransfersChange: (String, Boolean) -> Unit = { _, _ -> },
        val onShowNetColumnChange: (String, Boolean) -> Unit = { _, _ -> },
        val onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
        val onClickIncome: ((TransactionFilter) -> Unit)? = null,
        val onClickExpense: ((TransactionFilter) -> Unit)? = null,
        val onClickTransfer: ((TransactionFilter) -> Unit)? = null,
        val onClickInflow: ((TransactionFilter) -> Unit)? = null,
        val onClickOutflow: ((TransactionFilter) -> Unit)? = null,
        val onClickNet: ((TransactionFilter) -> Unit)? = null,
        val onClickNetFlow: ((TransactionFilter) -> Unit)? = null,
        val onSeeAll: () -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for analytics cards (update config + delete)
     */
    data class Analytics(
        val onUpdate: (Any) -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for account hero card
     */
    data class AccountHero(
        val onShowCurrencyBadgesChange: (String, Boolean) -> Unit = { _, _ -> },
        val onShowComparisonChange: (String, Boolean) -> Unit = { _, _ -> },
        val onPerspectiveChange: (String, com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective) -> Unit = { _, _ -> },
        val onExcludeInternalTransfersChange: (String, Boolean) -> Unit = { _, _ -> },
        val onSplitTransfersChange: (String, Boolean) -> Unit = { _, _ -> },
        val onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
        val onClickIncome: ((TransactionFilter) -> Unit)? = null,
        val onClickExpense: ((TransactionFilter) -> Unit)? = null,
        val onClickTransfer: ((TransactionFilter) -> Unit)? = null,
        val onClickInflow: ((TransactionFilter) -> Unit)? = null,
        val onClickOutflow: ((TransactionFilter) -> Unit)? = null,
        val onClickNet: ((TransactionFilter) -> Unit)? = null,
        val onClickNetFlow: ((TransactionFilter) -> Unit)? = null,
        val onClickCurrency: ((String) -> Unit)? = null,
        val onSeeAll: () -> Unit = {},
        val onClick: (() -> Unit)? = null,
        val onDelete: (() -> Unit)? = null,
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for accounts card
     */
    data class Accounts(
        val onGridColumnsChange: (String, Int) -> Unit = { _, _ -> },
        val onLayoutChange: (String, String) -> Unit = { _, _ -> },
        val onShowComparisonChange: (String, Boolean) -> Unit = { _, _ -> },
        val onPerspectiveChange: (String, com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective) -> Unit = { _, _ -> },
        val onExcludeInternalTransfersChange: (String, Boolean) -> Unit = { _, _ -> },
        val onSplitTransfersChange: (String, Boolean) -> Unit = { _, _ -> },
        val onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
        val onShowAddAccountCardChange: (String, Boolean) -> Unit = { _, _ -> },
        val onManageClick: () -> Unit = {},
        val onAccountClick: (String) -> Unit = {},
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        val onClickIncome: ((TransactionFilter) -> Unit)? = null,
        val onClickExpense: ((TransactionFilter) -> Unit)? = null,
        val onClickTransfer: ((TransactionFilter) -> Unit)? = null,
        val onClickInflow: ((TransactionFilter) -> Unit)? = null,
        val onClickOutflow: ((TransactionFilter) -> Unit)? = null,
        val onClickNet: ((TransactionFilter) -> Unit)? = null,
        val onClickNetFlow: ((TransactionFilter) -> Unit)? = null,
        val onSeeAll: () -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for planned payments card
     */
    data class PlannedPayments(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig) -> Unit = {},
        val onShowComparisonChange: (String, Boolean) -> Unit = { _, _ -> },
        val onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
        val onItemClick: (Long) -> Unit = {},
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        val onConfirm: ((Long, Long) -> Unit)? = null,
        val onSkip: ((Long, Long) -> Unit)? = null,
        val onSeeAll: () -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for budgets card
     */
    data class Budgets(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig) -> Unit = {},
        val onShowComparisonChange: (String, Boolean) -> Unit = { _, _ -> },
        val onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
        val onBudgetClick: (Long) -> Unit = {},
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        val onSeeAll: () -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for loans card
     */
    data class Loans(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig) -> Unit = {},
        val onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
        val onLoanClick: (Long) -> Unit = {},
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        val onSeeAll: () -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for recent activities card
     */
    data class RecentActivities(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig) -> Unit = {},
        val onTransactionClick: (Long) -> Unit = {},
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        val onSeeAll: () -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onItemExpanded: (String) -> Unit = {},
        val onItemCollapsed: (String) -> Unit = {},
        val onStatusChange: (Long, TransactionStatus) -> Unit = { _, _ -> },
        val onClone: (Long) -> Unit = {},
        val onDeleteTransaction: (Long) -> Unit = {},
        val onSplit: (Long) -> Unit = {},
        val onJump: (Long, Set<Long>?) -> Unit = { _, _ -> },
        val onFullSwipeRight: (Long) -> Unit = {},
        val onSaveFilterClick: (TransactionFilter) -> Unit = {},
        val onConfirmPlanned: (Long, Long) -> Unit = { _, _ -> },
        val onCancelPlanned: (Long, Long) -> Unit = { _, _ -> }
    ) : CardCallbacks()

    /**
     * Callbacks for cash flow comparison card
     */
    data class CashFlowComparison(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.CashFlowComparisonCardConfig) -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for income & expense bar chart card
     */
    data class IncomeExpenseBar(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig) -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for category spending trend card
     */
    data class CategorySpendingTrend(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig) -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for tag upset plot card
     */
    data class TagUpSetPlot(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig) -> Unit = {},
        val onDelete: (() -> Unit)? = null,
        val onRecordsClick: (TransactionFilter) -> Unit = {},
        val onSaveFilterClick: (TransactionFilter) -> Unit = {}
    ) : CardCallbacks()
}
