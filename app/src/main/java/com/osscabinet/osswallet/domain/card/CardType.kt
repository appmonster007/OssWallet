package com.osscabinet.ossWallet.domain.card

/**
 * Single source of truth for all card types.
 */
enum class CardType(
    val serialName: String, 
    val displayName: String,
    val description: String = ""
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
    CATEGORY_BREAKDOWN(
        "category_breakdown", 
        "Category Breakdown", 
        "Spending distribution across categories."
    ),
    CATEGORY_EXPENSE_TREEMAP(
        "category_expense_treemap", 
        "Category Expenses", 
        "Hierarchical view of category expenses."
    ),
    ACCOUNT_HERO(
        "account_hero",
        "Account Hero",
        "Prominent display of account balance and details."
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
    )
}
