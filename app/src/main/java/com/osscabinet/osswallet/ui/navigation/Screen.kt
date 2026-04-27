package com.osscabinet.ossWallet.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard?viewContext={viewContext}") {
        fun createRoute(viewContext: String? = null): String {
            return if (viewContext != null) "dashboard?viewContext=$viewContext" else "dashboard"
        }
    }
    object AccountsView : Screen("accounts_view?filterContext={filterContext}") {
        fun createRoute(filterContext: String? = null): String {
            return if (filterContext != null) "accounts_view?filterContext=$filterContext" else "accounts_view"
        }
    }
    object Settings : Screen("settings")
    object Loans : Screen("loans")
    object Budgets : Screen("budgets")
    object TransactionList : Screen("list?filterContext={filterContext}") {
        fun createRoute(filterContext: String? = null): String {
            return if (filterContext != null) "list?filterContext=$filterContext" else "list"
        }
    }
    object AddTransaction : Screen("add") {
        fun createRoute(accountId: String? = null, type: Int? = null, template: String? = null): String {
            val params = mutableListOf<String>()
            accountId?.let { params.add("accountId=$it") }
            type?.let { params.add("type=$it") }
            template?.let { params.add("template=$it") }
            return if (params.isEmpty()) "add" else "add?${params.joinToString("&")}"
        }
    }
    object Analytics : Screen("analytics")
    object SharedWallet : Screen("shared")
    object AccountSetup : Screen("account_setup")
    object AccountManagement : Screen("manage_accounts")
    object ExchangeRates : Screen("exchange_rates")
    object Templates : Screen("templates")
    object AddTemplate : Screen("add_template?templateId={templateId}") {
        fun createRoute(templateId: Long? = null) = if (templateId != null) "add_template?templateId=$templateId" else "add_template"
    }
    
    object BudgetDetail : Screen("budget_detail/{budgetId}") {
        fun createRoute(budgetId: String) = "budget_detail/$budgetId"
    }
    
    object LoanDetail : Screen("loan_detail/{accountId}") {
        fun createRoute(accountId: String) = "loan_detail/$accountId"
    }

    object AddEditBudget : Screen("add_edit_budget?budgetId={budgetId}") {
        fun createRoute(budgetId: String? = null) = if (budgetId != null) "add_edit_budget?budgetId=$budgetId" else "add_edit_budget"
    }

    object AddEditLoan : Screen("add_edit_loan?accountId={accountId}") {
        fun createRoute(accountId: String? = null) = if (accountId != null) "add_edit_loan?accountId=$accountId" else "add_edit_loan"
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

    object ManageLabels : Screen("manage_labels")
    object ManageCategories : Screen("manage_categories")
    object ManageFilters : Screen("manage_filters")
    object EditFilter : Screen("edit_filter?filterId={filterId}") {
        fun createRoute(filterId: Long? = null) = if (filterId != null) "edit_filter?filterId=$filterId" else "edit_filter"
    }
}
