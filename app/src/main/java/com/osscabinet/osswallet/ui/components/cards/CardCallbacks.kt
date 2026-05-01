package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Sealed class hierarchy for unified card callbacks.
 * Each subtype represents the callbacks available for a specific card type.
 * 
 * Note: Callbacks match the actual signatures expected by card components,
 * which often include cardId as the first parameter.
 */
sealed class CardCallbacks {
    
    /**
     * Common callbacks - just delete
     */
    data class Common(
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks for Net Worth
     */
    data class NetWorth(
        val onFontSizeChange: (String, String) -> Unit,
        val onFilterChange: (String, TransactionFilter) -> Unit,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks for currency balances
     */
    data class CurrencyBalances(
        val onLayoutChange: (String, String) -> Unit,
        val onShowBaseConversionChange: (String, Boolean) -> Unit,
        val onFilterChange: (String, TransactionFilter) -> Unit,
        val onCurrencyClick: ((String, TimelineRange) -> Unit)? = null,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks for cash flow card
     */
    data class CashFlow(
        val onShowBaseConversionChange: (String, Boolean) -> Unit,
        val onFilterChange: (String, TransactionFilter) -> Unit,
        val onClickIncome: (TimelineRange) -> Unit = {},
        val onClickExpense: (TimelineRange) -> Unit = {},
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks for analytics cards (update config + delete)
     */
    data class Analytics(
        val onUpdate: (Any) -> Unit = {},
        val onDelete: () -> Unit = {},
        val onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {}
    ) : CardCallbacks()
    
    /**
     * Callbacks for account hero card
     */
    data class AccountHero(
        val onShowCurrencyBadgesChange: (String, Boolean) -> Unit,
        val onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
        val onClick: (() -> Unit)? = null,
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()
    
    /**
     * Callbacks for accounts card
     */
    data class Accounts(
        val onGridColumnsChange: (String, Int) -> Unit,
        val onLayoutChange: (String, String) -> Unit,
        val onFilterChange: (String, TransactionFilter) -> Unit,
        val onShowAddAccountCardChange: (String, Boolean) -> Unit,
        val onManageClick: () -> Unit = {},
        val onHeaderClick: () -> Unit = {},
        val onAccountClick: (String) -> Unit = {},
        val onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for planned payments card
     */
    data class PlannedPayments(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig) -> Unit = {},
        val onItemClick: (Long) -> Unit = {},
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for budgets card
     */
    data class Budgets(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig) -> Unit = {},
        val onBudgetClick: (Long) -> Unit = {},
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()

    /**
     * Callbacks for loans card
     */
    data class Loans(
        val onUpdate: (com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig) -> Unit = {},
        val onLoanClick: (Long) -> Unit = {},
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()

    /**
     * Legacy/Deprecated
     */
    data class WithFontSizeAndTimeline(
        val onFontSizeChange: (String, String) -> Unit,
        val onTimelineRangeChange: (String, TimelineRange) -> Unit,
        val onDelete: () -> Unit
    ) : CardCallbacks()
}
