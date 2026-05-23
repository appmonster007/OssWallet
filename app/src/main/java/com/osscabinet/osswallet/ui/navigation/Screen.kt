package com.osscabinet.ossWallet.ui.navigation

import com.osscabinet.ossWallet.domain.model.TransactionType

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
    object SettingsDisplay : Screen("settings_display")
    object SettingsCurrency : Screen("settings_currency")
    object SettingsData : Screen("settings_data")
    object SettingsSecurity : Screen("settings_security")
    object TransactionList : Screen("transaction_list?filter={filter}&topLevel={topLevel}") {
        fun createRoute(filter: String? = null, topLevel: Boolean = true): String {
            val params = mutableListOf<String>()
            filter?.let { params.add("filter=$it") }
            params.add("topLevel=$topLevel")
            return "transaction_list?${params.joinToString("&")}"
        }
    }

    object TransactionEntry :
        Screen("transaction_entry?transactionId={transactionId}&accountId={accountId}&type={type}&templateId={templateId}") {
        fun createRoute(
            transactionId: Long? = null,
            accountId: String? = null,
            type: TransactionType? = null,
            templateId: Long? = null
        ): String {
            val params = mutableListOf<String>()
            transactionId?.let { params.add("transactionId=$it") }
            accountId?.let { params.add("accountId=$it") }
            type?.let { params.add("type=${it.toInt()}") }
            templateId?.let { params.add("templateId=$it") }
            return if (params.isEmpty()) "transaction_entry" else "transaction_entry?${
                params.joinToString(
                    "&"
                )
            }"
        }
    }

    object Analytics : Screen("analytics?topLevel={topLevel}") {
        fun createRoute(topLevel: Boolean = true) = "analytics?topLevel=$topLevel"
    }

    object SharedWallet : Screen("shared_wallet")
    object AccountEntry : Screen("account_entry?accountId={accountId}") {
        fun createRoute(accountId: String? = null) =
            if (accountId != null) "account_entry?accountId=$accountId" else "account_entry"
    }

    object AccountList : Screen("account_list")
    object ExchangeRates : Screen("exchange_rates?topLevel={topLevel}") {
        fun createRoute(topLevel: Boolean = true) = "exchange_rates?topLevel=$topLevel"
    }

    object TemplateList : Screen("template_list")
    object TemplateEntry : Screen("template_entry?templateId={templateId}") {
        fun createRoute(templateId: Long? = null) =
            if (templateId != null) "template_entry?templateId=$templateId" else "template_entry"
    }

    object AccountDetail : Screen("account_detail/{accountId}?filter={filter}") {
        fun createRoute(accountId: String, filter: String? = null): String {
            val base = "account_detail/$accountId"
            return if (filter != null) "$base?filter=$filter" else base
        }
    }


    object TagList : Screen("tag_list")
    object CategoryList : Screen("category_list")
    object FilterList : Screen("filter_list")
    object FilterEntry : Screen("filter_entry?filterId={filterId}&initialFilter={initialFilter}") {
        fun createRoute(filterId: Long? = null, initialFilter: String? = null): String {
            val params = mutableListOf<String>()
            filterId?.let { params.add("filterId=$it") }
            initialFilter?.let { params.add("initialFilter=$it") }
            return if (params.isEmpty()) "filter_entry" else "filter_entry?${params.joinToString("&")}"
        }
    }

    object PlannedPaymentList : Screen("planned_payment_list")
    object BudgetList : Screen("budget_list")
    object BudgetEntry : Screen("budget_entry?budgetId={budgetId}") {
        fun createRoute(budgetId: Long? = null) =
            if (budgetId != null) "budget_entry?budgetId=$budgetId" else "budget_entry"
    }

    object PlannedPaymentEntry :
        Screen("planned_payment_entry?plannedPaymentId={plannedPaymentId}") {
        fun createRoute(plannedPaymentId: Long? = null) =
            if (plannedPaymentId != null) "planned_payment_entry?plannedPaymentId=$plannedPaymentId" else "planned_payment_entry"
    }

    object LoanList : Screen("loan_list")
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
    object Onboarding : Screen("onboarding")
    object ShortlistedCurrencies : Screen("shortlisted_currencies")
    object OpenInWeb : Screen("open_in_web")
    object Main : Screen("main?tab={tab}") {
        fun createRoute(tab: String? = null) = if (tab != null) "main?tab=$tab" else "main"
    }
}
