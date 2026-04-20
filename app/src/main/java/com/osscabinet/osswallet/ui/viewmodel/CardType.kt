package com.osscabinet.ossWallet.ui.viewmodel

/**
 * Enum representing the screen context for card types.
 * This helps distinguish between cards used in different screens.
 */
enum class CardScreenContext {
    DASHBOARD,
    ACCOUNTS_VIEW,
    ACCOUNT_DETAIL
}

/**
 * Unified card type enum that can be used across all screens.
 * Each card type has a display name and serial name for serialization.
 */
enum class UnifiedCardType(
    val displayName: String,
    val serialName: String,
    val screenContext: CardScreenContext
) {
    // Dashboard cards
    DASHBOARD_NET_WORTH("Net Worth", "net_worth", CardScreenContext.DASHBOARD),
    DASHBOARD_CURRENCY_BALANCES("Currency Balances", "currency_balances", CardScreenContext.DASHBOARD),
    DASHBOARD_CASH_FLOW("Cash Flow", "cash_flow", CardScreenContext.DASHBOARD),
    DASHBOARD_ACCOUNTS("Accounts", "accounts", CardScreenContext.DASHBOARD),
    DASHBOARD_BUDGETS("Budgets", "budgets", CardScreenContext.DASHBOARD),

    // Accounts View cards
    ACCOUNTS_VIEW_NET_WORTH("Net Worth", "net_worth", CardScreenContext.ACCOUNTS_VIEW),
    ACCOUNTS_VIEW_CURRENCY_BALANCES("Currency Balances", "currency_balances", CardScreenContext.ACCOUNTS_VIEW),
    ACCOUNTS_VIEW_CASH_FLOW("Cash Flow", "cash_flow", CardScreenContext.ACCOUNTS_VIEW),
    ACCOUNTS_VIEW_ACCOUNTS_LIST("Accounts List", "accounts_list", CardScreenContext.ACCOUNTS_VIEW),

    // Account Detail cards
    ACCOUNT_DETAIL_HERO("Account Summary", "account_hero", CardScreenContext.ACCOUNT_DETAIL),
    ACCOUNT_DETAIL_CASH_FLOW("Cash Flow", "account_cash_flow", CardScreenContext.ACCOUNT_DETAIL),
    ACCOUNT_DETAIL_CURRENCY_BALANCES("Currency Balances", "account_currency_balances", CardScreenContext.ACCOUNT_DETAIL);

    companion object {
        fun fromSerialName(serialName: String, screenContext: CardScreenContext): UnifiedCardType? {
            return entries.find { it.serialName == serialName && it.screenContext == screenContext }
        }

        fun getAvailableForScreen(screenContext: CardScreenContext): List<UnifiedCardType> {
            return entries.filter { it.screenContext == screenContext }
        }
    }
}
