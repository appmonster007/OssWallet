package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.settings.ScreenContext

private val ALL_SCREENS =
    setOf(ScreenContext.DASHBOARD, ScreenContext.ACCOUNT_DETAIL, ScreenContext.ANALYTICS)

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
    val alwaysPresent: Boolean = false,
    val category: CardCategory = CardCategory.OTHER
) {
    NET_WORTH(
        "net_worth",
        "Net Worth",
        "Overall balance across all accounts converted to base currency.",
        category = CardCategory.SUMMARY
    ),
    CASH_FLOW(
        "cash_flow",
        "Cash Flow",
        "Income and expenses summary for the selected period.",
        category = CardCategory.SUMMARY
    ),
    CURRENCY_BALANCES(
        "currency_balances",
        "Currency Balances",
        "Total balances grouped by currency.",
        category = CardCategory.SUMMARY
    ),
    ACCOUNTS(
        "accounts",
        "Accounts",
        "Detailed list of your financial accounts and their stats.",
        category = CardCategory.SUMMARY
    ),
    BUDGETS(
        "budgets",
        "Budgets",
        "Track your spending against set limits.",
        category = CardCategory.TRACKING
    ),
    CATEGORY_PIE_CHART(
        "category_breakdown",
        "Category Pie Chart",
        "Spending distribution across categories.",
        category = CardCategory.ANALYTICS
    ),
    CATEGORY_TREEMAP(
        "category_expense_treemap",
        "Category Treemap",
        "Hierarchical view of category expenses.",
        category = CardCategory.ANALYTICS
    ),
    ACCOUNT_HERO(
        "account_hero",
        "Account Hero",
        "Prominent display of account balance and details.",
        availableOnScreens = setOf(ScreenContext.ACCOUNT_DETAIL),
        alwaysPresent = true,
        category = CardCategory.SUMMARY
    ),
    PLANNED_PAYMENTS(
        "planned_payments",
        "Planned Payments",
        "Upcoming and overdue scheduled payments.",
        category = CardCategory.TRACKING
    ),
    LOANS(
        "loans",
        "Loans",
        "Track money you lent or borrowed.",
        availableOnScreens = setOf(ScreenContext.DASHBOARD, ScreenContext.ANALYTICS),
        category = CardCategory.TRACKING
    ),
    RECENT_ACTIVITIES(
        "recent_activities",
        "Recent Activities",
        "List of your latest transactions.",
        category = CardCategory.TRACKING
    ),
    CASHFLOW_COMPARISON(
        "cashflow_comparison",
        "Cash Flow Comparison",
        "Compare cash flow across current, previous, and same period last year.",
        category = CardCategory.ANALYTICS
    ),
    INCOME_EXPENSE_BAR(
        "income_expense_bar",
        "Income & Expense Chart",
        "Bar chart showing income and expenses per time bucket with a net trend line.",
        category = CardCategory.ANALYTICS
    ),
    CATEGORY_SPENDING_TREND(
        "category_spending_trend",
        "Category Spending Trend",
        "Period-wise stacked spending by category, filtered by nature (Must / Need / Want).",
        category = CardCategory.ANALYTICS
    ),
    TAG_UPSET_PLOT(
        "tag_upset_plot",
        "Tag UpSet Plot",
        "Analyze intersections of tags and their total amounts.",
        category = CardCategory.ANALYTICS
    );

    /** Returns true when this card type can be added by the user on [screen]. */
    fun isUserAddableOn(screen: ScreenContext): Boolean =
        screen in availableOnScreens && !alwaysPresent
}

enum class CardCategory(val displayName: String, val order: Int) {
    SUMMARY("Summary & Balances", 0),
    TRACKING("Planning & Tracking", 1),
    ANALYTICS("Analytics & Charts", 2),
    OTHER("Other", 3)
}

