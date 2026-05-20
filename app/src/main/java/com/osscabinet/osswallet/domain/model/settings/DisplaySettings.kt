package com.osscabinet.ossWallet.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class LayoutSettings(
    val sectionOrder: String? = null
)

@Serializable
data class BudgetSettings(
    val showProgressLabels: Boolean = true
)

@Serializable
data class DashboardSettings(
    val accountsSection: AccountsSectionSettings = AccountsSectionSettings(gridColumns = 2),
    val currencyBalancesSection: CurrencyBalancesSettings = CurrencyBalancesSettings(),
    val netWorthSection: NetWorthSettings = NetWorthSettings(),
    val cashFlowSection: CashFlowSettings = CashFlowSettings(),
    val budgetSection: BudgetSettings = BudgetSettings(),
    val layout: LayoutSettings = LayoutSettings()
)

@Serializable
data class AccountsViewSettings(
    val accountsSection: AccountsSectionSettings = AccountsSectionSettings(gridColumns = 1),
    val currencyBalancesSection: CurrencyBalancesSettings = CurrencyBalancesSettings(),
    val netWorthSection: NetWorthSettings = NetWorthSettings(),
    val cashFlowSection: CashFlowSettings = CashFlowSettings(),
    val layout: LayoutSettings = LayoutSettings()
)

@Serializable
enum class TransactionGroupingType {
    BY_DAY, BY_WEEK
}

@Serializable
data class TransactionSectionSettings(
    val showPlannedPayments: Boolean = false,
    val useNoteAsHeader: Boolean = false,
    val showOpeningBalance: Boolean = false,
    val showClosingBalance: Boolean = true,
    val groupBy: TransactionGroupingType = TransactionGroupingType.BY_WEEK,
    val showOnlyActiveCurrencies: Boolean = false,
    val excludeFutureTransactions: Boolean = true,
    val showCurrencyBreakdown: Boolean = true
)

@Serializable
data class AccountDetailSettings(
    val currencyBalancesSection: CurrencyBalancesSettings = CurrencyBalancesSettings(),
    val cashFlowSection: CashFlowSettings = CashFlowSettings(),
    val transactionsSection: TransactionSectionSettings = TransactionSectionSettings()
)
