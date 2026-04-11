package com.osscabinet.gleemanager.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AccountsView : Screen("accounts_view")
    object Settings : Screen("settings")
    object Loans : Screen("loans")
    object Budgets : Screen("budgets")
    object TransactionList : Screen("list")
    object AddTransaction : Screen("add?accountId={accountId}") {
        fun createRoute(accountId: String? = null) = if (accountId != null) "add?accountId=$accountId" else "add"
    }
    object Analytics : Screen("analytics")
    object SharedWallet : Screen("shared")
    object AccountSetup : Screen("account_setup")
    object AccountManagement : Screen("manage_accounts")
    object ExchangeRates : Screen("exchange_rates")
    
    object EditBudget : Screen("edit_budget/{category}") {
        fun createRoute(category: String) = "edit_budget/$category"
    }
    
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    
    object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: String) = "account_detail/$accountId"
    }
    
    object EditAccount : Screen("edit_account/{accountId}") {
        fun createRoute(accountId: String) = "edit_account/$accountId"
    }
}
