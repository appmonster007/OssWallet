package com.osscabinet.ossWallet.ui.components.cards

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
     * Callbacks with timeline range change capability
     */
    data class WithTimeline(
        val onTimelineRangeChange: (String, TimelineRange) -> Unit,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks with font size change capability
     */
    data class WithFontSize(
        val onFontSizeChange: (String, String) -> Unit,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks with both font size and timeline range change
     */
    data class WithFontSizeAndTimeline(
        val onFontSizeChange: (String, String) -> Unit,
        val onTimelineRangeChange: (String, TimelineRange) -> Unit,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks with click actions (income/expense/currency)
     */
    data class WithClickActions(
        val onClickIncome: ((TimelineRange) -> Unit)? = null,
        val onClickExpense: ((TimelineRange) -> Unit)? = null,
        val onCurrencyClick: ((String, TimelineRange) -> Unit)? = null,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks for currency balances
     */
    data class CurrencyBalances(
        val onLayoutChange: (String, String) -> Unit,
        val onShowBaseConversionChange: (String, Boolean) -> Unit,
        val onTimelineRangeChange: (String, TimelineRange) -> Unit,
        val onCurrencyClick: ((String, TimelineRange) -> Unit)? = null,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks for cash flow card
     */
    data class CashFlow(
        val onShowBaseConversionChange: (String, Boolean) -> Unit,
        val onTimelineRangeChange: (String, TimelineRange) -> Unit,
        val onClickIncome: (TimelineRange) -> Unit = {},
        val onClickExpense: (TimelineRange) -> Unit = {},
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks for analytics cards (update config + delete)
     */
    data class Analytics(
        val onUpdate: (Any) -> Unit = {},
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()
    
    /**
     * Callbacks for account hero card
     */
    data class AccountHero(
        val onShowCurrencyBadgesChange: (String, Boolean) -> Unit,
        val onClick: (() -> Unit)? = null,
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()
    
    /**
     * Callbacks for accounts card
     */
    data class Accounts(
        val onGridColumnsChange: (String, Int) -> Unit,
        val onLayoutChange: (String, String) -> Unit,
        val onTimelineRangeChange: (String, TimelineRange) -> Unit,
        val onShowAddAccountCardChange: (String, Boolean) -> Unit,
        val onManageClick: () -> Unit = {},
        val onHeaderClick: () -> Unit = {},
        val onAccountClick: (String) -> Unit = {},
        val onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()
    
    /**
     * Callbacks for budgets card
     */
    data class Budgets(
        val onShowProgressLabelsChange: (String, Boolean) -> Unit,
        val onManageClick: () -> Unit = {},
        val onBudgetClick: (String) -> Unit = {},
        val onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()
}
