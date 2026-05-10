package com.osscabinet.ossWallet.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard?viewContext={viewContext}&topLevel={topLevel}") {
        fun createRoute(viewContext: String? = null, topLevel: Boolean = true): String {
            val params = mutableListOf<String>()
            viewContext?.let { params.add("viewContext=$it") }
            params.add("topLevel=$topLevel")
            return "dashboard?${params.joinToString("&")}"
        }
    }
    object Settings : Screen("settings")
    object TransactionList : Screen("list?filter={filter}&topLevel={topLevel}") {
        fun createRoute(filter: String? = null, topLevel: Boolean = true): String {
            val params = mutableListOf<String>()
            filter?.let { params.add("filter=$it") }
            params.add("topLevel=$topLevel")
            return "list?${params.joinToString("&")}"
        }
    }
    object AddTransaction : Screen("add") {
        fun createRoute(accountId: String? = null, type: Int? = null, templateId: Long? = null): String {
            val params = mutableListOf<String>()
            accountId?.let { params.add("accountId=$it") }
            type?.let { params.add("type=$it") }
            templateId?.let { params.add("templateId=$it") }
            return if (params.isEmpty()) "add" else "add?${params.joinToString("&")}"
        }
    }
    object Analytics : Screen("analytics?topLevel={topLevel}") {
        fun createRoute(topLevel: Boolean = true) = "analytics?topLevel=$topLevel"
    }
    object SharedWallet : Screen("shared")
    object AccountEntry : Screen("account_entry?accountId={accountId}") {
        fun createRoute(accountId: String? = null) =
            if (accountId != null) "account_entry?accountId=$accountId" else "account_entry"
    }
    object AccountManagement : Screen("manage_accounts")
    object ExchangeRates : Screen("exchange_rates?topLevel={topLevel}") {
        fun createRoute(topLevel: Boolean = true) = "exchange_rates?topLevel=$topLevel"
    }
    object Templates : Screen("templates")
    object AddTemplate : Screen("add_template?templateId={templateId}") {
        fun createRoute(templateId: Long? = null) = if (templateId != null) "add_template?templateId=$templateId" else "add_template"
    }
    
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    
    object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: String) = "account_detail/$accountId"
    }
    

    object ManageTags : Screen("manage_tags")
    object ManageCategories : Screen("manage_categories")
    object ManageFilters : Screen("manage_filters")
    object EditFilter : Screen("edit_filter?filterId={filterId}") {
        fun createRoute(filterId: Long? = null) = if (filterId != null) "edit_filter?filterId=$filterId" else "edit_filter"
    }

    object PlannedPayments : Screen("planned_payments")
    object BudgetList : Screen("budgets")
    object BudgetEntry : Screen("budget_entry?budgetId={budgetId}") {
        fun createRoute(budgetId: Long? = null) =
            if (budgetId != null) "budget_entry?budgetId=$budgetId" else "budget_entry"
    }
    object PlannedPaymentEntry : Screen("planned_payment_entry?plannedPaymentId={plannedPaymentId}") {
        fun createRoute(plannedPaymentId: Long? = null) =
            if (plannedPaymentId != null) "planned_payment_entry?plannedPaymentId=$plannedPaymentId" else "planned_payment_entry"
    }
    object LoanList : Screen("loans")
    object LoanEntry : Screen("loan_entry?loanId={loanId}") {
        fun createRoute(loanId: Long? = null) =
            if (loanId != null) "loan_entry?loanId=$loanId" else "loan_entry"
    }
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }
    object PlannedPaymentDetail : Screen("planned_payment_detail/{plannedPaymentId}") {
        fun createRoute(plannedPaymentId: Long) = "planned_payment_detail/$plannedPaymentId"
    }
    object BudgetDetail : Screen("budget_detail/{budgetId}") {
        fun createRoute(budgetId: Long) = "budget_detail/$budgetId"
    }
    object SmartSuggestions : Screen("smart_suggestions")
}
