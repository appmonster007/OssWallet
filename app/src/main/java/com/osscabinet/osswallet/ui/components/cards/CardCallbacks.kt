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
        val onShowCurrencyBadgesChange: (Boolean) -> Unit,
        val onClick: (() -> Unit)? = null,
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()
    
    /**
     * Callbacks for account cash flow card
     */
    data class AccountCashFlow(
        val onShowBaseConversionChange: (Boolean) -> Unit,
        val onTimelineRangeChange: (TimelineRange) -> Unit,
        val onClickIncome: (() -> Unit)? = null,
        val onClickExpense: (() -> Unit)? = null,
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()
    
    /**
     * Callbacks for account currency balances card
     */
    data class AccountCurrencyBalances(
        val onLayoutChange: (String) -> Unit,
        val onShowBaseConversionChange: (Boolean) -> Unit,
        val onTimelineRangeChange: (TimelineRange) -> Unit,
        val onCurrencyClick: (String, TimelineRange) -> Unit,
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()
    
    /**
     * Callbacks for accounts card
     */
    data class Accounts(
        val onGridColumnsChange: (Int) -> Unit,
        val onLayoutChange: (String) -> Unit,
        val onTimelineRangeChange: (TimelineRange) -> Unit,
        val onShowAddAccountCardChange: (Boolean) -> Unit,
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
        val onShowProgressLabelsChange: (Boolean) -> Unit,
        val onManageClick: () -> Unit = {},
        val onBudgetClick: (String) -> Unit = {},
        val onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
        val onDelete: () -> Unit = {}
    ) : CardCallbacks()
    
    /**
     * Callbacks for AccountsViewNetWorth card
     */
    data class AccountsViewNetWorth(
        val onFontSizeChange: (String, String) -> Unit,
        val onTimelineRangeChange: (String, TimelineRange) -> Unit,
        val onClickIncome: ((TimelineRange) -> Unit)? = null,
        val onClickExpense: ((TimelineRange) -> Unit)? = null,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks for AccountsViewCurrencyBalances card
     */
    data class AccountsViewCurrencyBalances(
        val onLayoutChange: (String, String) -> Unit,
        val onShowBaseConversionChange: (String, Boolean) -> Unit,
        val onTimelineRangeChange: (String, TimelineRange) -> Unit,
        val onCurrencyClick: ((String, TimelineRange) -> Unit)? = null,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks for AccountsViewCashFlow card
     */
    data class AccountsViewCashFlow(
        val onShowBaseConversionChange: (String, Boolean) -> Unit,
        val onTimelineRangeChange: (String, TimelineRange) -> Unit,
        val onClickIncome: ((TimelineRange) -> Unit)? = null,
        val onClickExpense: ((TimelineRange) -> Unit)? = null,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    /**
     * Callbacks for AccountsViewAccountsList card
     */
    data class AccountsViewAccountsList(
        val onGridColumnsChange: (String, Int) -> Unit,
        val onLayoutChange: (String, String) -> Unit,
        val onTimelineRangeChange: (String, TimelineRange) -> Unit,
        val onManageClick: (() -> Unit)? = null,
        val onHeaderClick: (() -> Unit)? = null,
        val onAccountClick: ((String) -> Unit)? = null,
        val onRecordsClick: ((com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit)? = null,
        val onDelete: () -> Unit
    ) : CardCallbacks()
}
