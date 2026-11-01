package com.osscabinet.ossWallet.ui.navigation

import kotlinx.serialization.Serializable

// ── Primary tabs ──────────────────────────────────────────────────────────────

/**
 * Marks a route as a drawer-reachable sibling tab switch (Dashboard/Records/Analytics) rather
 * than a hierarchical drill-down. Single source of truth for route category — [AppNavigator]
 * derives its default [androidx.navigation.NavOptions] from this, and the nav graph files derive
 * their transition-spec choice (fade vs slide) from it too, replacing what used to be three
 * independently hand-maintained lists that had to be kept in sync by convention.
 */
interface TopLevelRoute

/** Start destination. viewContextJson = URL-encoded JSON of DashboardViewContext (nullable). */
@Serializable
data class DashboardRoute(
    val viewContextJson: String? = null,
    val topLevel: Boolean = true
) : TopLevelRoute

/** filterJson = URL-encoded JSON of TransactionFilter (empty string = no filter). */
@Serializable
data class TransactionListRoute(
    val filterJson: String = "",
    val topLevel: Boolean = true
) : TopLevelRoute

@Serializable
data object AnalyticsRoute : TopLevelRoute

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
data class AccountEntryRoute(
    val accountId: String? = null,
    val prefillName: String? = null,
    val prefillCurrency: String? = null,
    val isDraftMode: Boolean = false
)

/**
 * SavedStateHandle key used to relay an Account Entry result (a persisted account id, or in draft
 * mode a JSON-encoded [com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryDraft]) back
 * to the caller that navigated to [AccountEntryRoute] (e.g. the CSV import wizard). Shared between
 * the producer (AccountsNavGraph's onDone) and consumer (the caller's composable) to avoid a
 * stringly-typed key mismatch.
 */
const val ACCOUNT_ENTRY_RESULT_KEY = "createdAccountId"

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
data object GoalListRoute

@Serializable
data object SearchRoute

@Serializable
data object ExchangeRatesRoute

@Serializable
data object ReportsRoute

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
data object SettingsNotificationsRoute

@Serializable
data object SettingsDataManagementRoute

@Serializable
data object SettingsSupportUsRoute

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
