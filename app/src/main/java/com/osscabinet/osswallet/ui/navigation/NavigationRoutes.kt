package com.osscabinet.ossWallet.ui.navigation

import kotlinx.serialization.Serializable

// ── Primary tabs ──────────────────────────────────────────────────────────────

/** Start destination. viewContextJson = URL-encoded JSON of DashboardViewContext (nullable). */
@Serializable
data class DashboardRoute(
    val viewContextJson: String? = null,
    val topLevel: Boolean = true
)

/** filterJson = URL-encoded JSON of TransactionFilter (empty string = no filter). */
@Serializable
data class TransactionListRoute(
    val filterJson: String = "",
    val topLevel: Boolean = true
)

@Serializable
data object AnalyticsRoute

@Serializable
data object ExchangeRatesRoute

// ── Transactions ──────────────────────────────────────────────────────────────

@Serializable
data class TransactionEntryRoute(
    val transactionId: Long = -1L,
    val accountId: String? = null,
    val transactionType: Int = -1,
    val templateId: Long = -1L
)

@Serializable
data object SharedWalletRoute

// ── Accounts ──────────────────────────────────────────────────────────────────

@Serializable
data object AccountListRoute

/** filterJson = URL-encoded JSON of TransactionFilter (nullable). */
@Serializable
data class AccountDetailRoute(
    val accountId: String,
    val filterJson: String? = null
)

@Serializable
data class AccountEntryRoute(val accountId: String? = null)

// ── Templates ─────────────────────────────────────────────────────────────────

@Serializable
data object TemplateListRoute

@Serializable
data class TemplateEntryRoute(val templateId: Long = -1L)

// ── Filters ───────────────────────────────────────────────────────────────────

@Serializable
data object FilterListRoute

@Serializable
data class FilterEntryRoute(
    val filterId: Long = -1L,
    val initialFilterJson: String? = null
)

// ── Planning ──────────────────────────────────────────────────────────────────

@Serializable
data object PlannedPaymentListRoute

@Serializable
data class PlannedPaymentDetailRoute(val plannedPaymentId: Long)

@Serializable
data class PlannedPaymentEntryRoute(val plannedPaymentId: Long = -1L)

@Serializable
data object BudgetListRoute

@Serializable
data class BudgetDetailRoute(val budgetId: Long)

@Serializable
data class BudgetEntryRoute(val budgetId: Long = -1L)

@Serializable
data object LoanListRoute

@Serializable
data class LoanDetailRoute(val loanId: Long)

@Serializable
data class LoanEntryRoute(val loanId: Long = -1L)

// ── Management ────────────────────────────────────────────────────────────────

@Serializable
data object TagListRoute

@Serializable
data object CategoryListRoute

// ── Settings ──────────────────────────────────────────────────────────────────

@Serializable
data object SettingsRoute

@Serializable
data object SettingsDisplayRoute

@Serializable
data object SettingsCurrencyRoute

@Serializable
data object SettingsSecurityRoute

@Serializable
data object SettingsDataManagementRoute

@Serializable
data object ShortlistedCurrenciesRoute

@Serializable
data object SmartSuggestionsRoute

@Serializable
data object OpenInWebRoute

// ── Import ────────────────────────────────────────────────────────────────────

@Serializable
data object CsvImportRoute

// ── Misc ──────────────────────────────────────────────────────────────────────

@Serializable
data object OnboardingRoute
