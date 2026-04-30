package com.osscabinet.ossWallet.ui.viewmodel

import com.osscabinet.ossWallet.domain.model.settings.ScreenContext

/**
 * Unified card type enum that can be used across all screens.
 * Each card type has a display name, serial name, and description.
 */
enum class UnifiedCardType(
    val displayName: String,
    val serialName: String,
    val description: String,
    val screenContext: ScreenContext
) {
    // Dashboard cards
    DASHBOARD_NET_WORTH("Net Worth", "net_worth", "Track your total net worth over time", ScreenContext.DASHBOARD),
    DASHBOARD_CURRENCY_BALANCES("Currency Balances", "currency_balances", "View balances across different currencies", ScreenContext.DASHBOARD),
    DASHBOARD_CASH_FLOW("Cash Flow", "cash_flow", "Track income and expenses", ScreenContext.DASHBOARD),
    DASHBOARD_ACCOUNTS("Accounts", "accounts", "View your account summaries", ScreenContext.DASHBOARD),
    DASHBOARD_CATEGORY_BREAKDOWN("Category Breakdown", "category_breakdown", "See spending by category", ScreenContext.DASHBOARD),
    DASHBOARD_CATEGORY_EXPENSE_TREEMAP("Category Treemap", "category_expense_treemap", "Visual treemap of expenses", ScreenContext.DASHBOARD),

    // Accounts View cards
    ACCOUNTS_VIEW_NET_WORTH("Net Worth", "net_worth", "Track your total net worth over time", ScreenContext.ACCOUNTS_VIEW),
    ACCOUNTS_VIEW_CURRENCY_BALANCES("Currency Balances", "currency_balances", "View balances across different currencies", ScreenContext.ACCOUNTS_VIEW),
    ACCOUNTS_VIEW_CASH_FLOW("Cash Flow", "cash_flow", "Track income and expenses", ScreenContext.ACCOUNTS_VIEW),
    ACCOUNTS_VIEW_ACCOUNTS_LIST("Accounts List", "accounts_list", "Display your accounts", ScreenContext.ACCOUNTS_VIEW),

    // Account Detail cards
    ACCOUNT_DETAIL_HERO("Account Summary", "account_hero", "Display account balance and details", ScreenContext.ACCOUNT_DETAIL),
    ACCOUNT_DETAIL_CASH_FLOW("Cash Flow", "account_cash_flow", "Track income and expenses for this account", ScreenContext.ACCOUNT_DETAIL),
    ACCOUNT_DETAIL_CURRENCY_BALANCES("Currency Balances", "account_currency_balances", "Show currency breakdown for this account", ScreenContext.ACCOUNT_DETAIL),

    // Analytics cards
    ANALYTICS_CATEGORY_BREAKDOWN("Category Breakdown", "category_breakdown", "Detailed spending breakdown", ScreenContext.ANALYTICS),
    ANALYTICS_CATEGORY_EXPENSE_TREEMAP("Category Expenses", "category_expense_treemap", "Treemap analysis of expenses", ScreenContext.ANALYTICS);

    companion object {
        fun fromSerialName(serialName: String, screenContext: ScreenContext): UnifiedCardType? {
            return entries.find { it.serialName == serialName && it.screenContext == screenContext }
        }

        fun getAvailableForScreen(screenContext: ScreenContext): List<UnifiedCardType> {
            return entries.filter { it.screenContext == screenContext }
        }
    }
}
