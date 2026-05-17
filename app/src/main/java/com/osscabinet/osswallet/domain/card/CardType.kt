package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.settings.ScreenContext

private val ALL_SCREENS = setOf(ScreenContext.DASHBOARD, ScreenContext.ACCOUNT_DETAIL, ScreenContext.ANALYTICS)

/**
 * Single source of truth for all card types.
 *
 * [availableOnScreens] declares which screens this card type can appear on.
 * [alwaysPresent]      if true, the card is always rendered at a fixed position
 *                      and must not appear in user-managed add/remove/reorder lists.
 */
enum class CardType(
    val serialName: String,
    val displayName: String,
    val description: String = "",
    val availableOnScreens: Set<ScreenContext> = ALL_SCREENS,
    val alwaysPresent: Boolean = false
) {
    NET_WORTH(
        "net_worth",
        "Net Worth",
        "Overall balance across all accounts converted to base currency."
    ),
    CASH_FLOW(
        "cash_flow",
        "Cash Flow",
        "Income and expenses summary for the selected period."
    ),
    CURRENCY_BALANCES(
        "currency_balances",
        "Currency Balances",
        "Total balances grouped by currency."
    ),
    ACCOUNTS(
        "accounts",
        "Accounts",
        "Detailed list of your financial accounts and their stats."
    ),
    BUDGETS(
        "budgets",
        "Budgets",
        "Track your spending against set limits."
    ),
    CATEGORY_PIE_CHART(
        "category_breakdown",
        "Category Pie Chart",
        "Spending distribution across categories."
    ),
    CATEGORY_TREEMAP(
        "category_expense_treemap",
        "Category Treemap",
        "Hierarchical view of category expenses."
    ),
    ACCOUNT_HERO(
        "account_hero",
        "Account Hero",
        "Prominent display of account balance and details.",
        availableOnScreens = setOf(ScreenContext.ACCOUNT_DETAIL),
        alwaysPresent = true
    ),
    PLANNED_PAYMENTS(
        "planned_payments",
        "Planned Payments",
        "Upcoming and overdue scheduled payments."
    ),
    LOANS(
        "loans",
        "Loans",
        "Track money you lent or borrowed."
    ),
    RECENT_ACTIVITIES(
        "recent_activities",
        "Recent Activities",
        "List of your latest transactions."
    ),
    CASHFLOW_COMPARISON(
        "cashflow_comparison",
        "Cash Flow Comparison",
        "Compare cash flow across current, previous, and same period last year."
    ),
    INCOME_EXPENSE_BAR(
        "income_expense_bar",
        "Income & Expense Chart",
        "Bar chart showing income and expenses per time bucket with a net trend line."
    );

    /** Returns true when this card type can be added by the user on [screen]. */
    fun isUserAddableOn(screen: ScreenContext): Boolean =
        screen in availableOnScreens && !alwaysPresent
}
