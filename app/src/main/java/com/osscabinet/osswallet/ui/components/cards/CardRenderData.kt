package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.analytics.CategoryBreakdownData
import com.osscabinet.ossWallet.domain.model.analytics.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.domain.model.settings.TimelineTrajectoryCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesSummaryCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PeriodComparisonCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowTrendCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowProgressCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BalanceForecastCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesBookCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowTableCardConfig
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsTimelineCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BalanceByAccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BalanceTrendCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowProgressData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTrendData
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesSummaryData
import com.osscabinet.ossWallet.ui.analytics.components.IncomeExpensesBookData
import com.osscabinet.ossWallet.ui.analytics.components.PeriodComparisonData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceForecastData
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsData
import com.osscabinet.ossWallet.ui.analytics.components.CashFlowTableData
import com.osscabinet.ossWallet.ui.analytics.components.PlannedPaymentsTimelineData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceByAccountsData
import com.osscabinet.ossWallet.ui.analytics.components.BalanceTrendData
import com.osscabinet.ossWallet.ui.viewmodel.NetWorthUiState
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.Account
import java.math.BigDecimal

/**
 * Sealed class hierarchy for unified card rendering data.
 * Each subtype represents the data required for a specific card type.
 */
sealed class CardRenderData {
    
    /**
     * Data for NetWorth card
     * Uses shared NetWorthCardConfig across all screens
     * Screens set accountId or selectedAccountIds to filter data for their context
     */
    data class NetWorth(
        val config: NetWorthCardConfig,
        val uiState: NetWorthUiState
    ) : CardRenderData()
    
    /**
     * Data for CurrencyBalances card
     * Uses shared CurrencyBalancesCardConfig across all screens
     * Screens set accountId to filter data for their context
     */
    data class CurrencyBalances(
        val config: CurrencyBalancesCardConfig,
        val currencyTotals: List<CurrencyAggregationItem>,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()
    
    /**
     * Data for CashFlow card
     * Uses shared CashFlowCardConfig across all screens
     * Screens set accountId to filter data for their context
     */
    data class CashFlow(
        val config: CashFlowCardConfig,
        val cashFlowResult: CashFlowResult,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()
    
    /**
     * Data for CategoryBreakdown card
     */
    data class CategoryBreakdown(
        val config: CategoryBreakdownCardConfig,
        val data: List<CategoryBreakdownData>
    ) : CardRenderData()
    
    /**
     * Data for TimelineTrajectory card
     */
    data class TimelineTrajectory(
        val config: TimelineTrajectoryCardConfig,
        val data: List<TimelineTrajectoryData>
    ) : CardRenderData()
    
    /**
     * Data for CategoryExpenseTreemap card
     */
    data class CategoryExpenseTreemap(
        val config: CategoryExpenseTreemapCardConfig,
        val data: List<CategoryExpenseTreemapData>
    ) : CardRenderData()
    
    /**
     * Data for IncomeExpensesSummary card
     */
    data class IncomeExpensesSummary(
        val config: IncomeExpensesSummaryCardConfig,
        val data: IncomeExpensesSummaryData
    ) : CardRenderData()
    
    /**
     * Data for PeriodComparison card
     */
    data class PeriodComparison(
        val config: PeriodComparisonCardConfig,
        val data: PeriodComparisonData
    ) : CardRenderData()
    
    /**
     * Data for CashFlowTrend card
     */
    data class CashFlowTrend(
        val config: CashFlowTrendCardConfig,
        val data: CashFlowTrendData
    ) : CardRenderData()
    
    /**
     * Data for CashFlowProgress card
     */
    data class CashFlowProgress(
        val config: CashFlowProgressCardConfig,
        val data: CashFlowProgressData
    ) : CardRenderData()
    
    /**
     * Data for BalanceForecast card
     */
    data class BalanceForecast(
        val config: BalanceForecastCardConfig,
        val data: BalanceForecastData
    ) : CardRenderData()
    
    /**
     * Data for PlannedPayments card
     */
    data class PlannedPayments(
        val config: PlannedPaymentsCardConfig,
        val data: PlannedPaymentsData
    ) : CardRenderData()
    
    /**
     * Data for AccountHero card
     */
    data class AccountHero(
        val config: AccountHeroCardConfig,
        val account: Account,
        val balance: BigDecimal,
        val netBalance: BigDecimal,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val precision: Int
    ) : CardRenderData()
    
    /**
     * Data for Accounts card
     * Uses shared AccountsCardConfig across all screens
     */
    data class Accounts(
        val config: AccountsCardConfig,
        val accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
        val accountStats: Map<String, AccountSummaryItem>,
        val baseCurrency: String,
        val currentTheme: String,
        val precision: Int
    ) : CardRenderData()
    
    /**
     * Data for Budgets card
     */
    data class Budgets(
        val config: BudgetsCardConfig,
        val budgets: List<com.osscabinet.ossWallet.domain.usecase.BudgetAnalysis>,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()
    
    /**
     * Data for Loans card
     */
    data class Loans(
        val config: com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig,
        val loans: List<com.osscabinet.ossWallet.domain.usecase.LoanAnalysis>,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()

    /**
     * Data for IncomeExpensesBook card
     */
    data class IncomeExpensesBook(
        val config: IncomeExpensesBookCardConfig,
        val data: IncomeExpensesBookData
    ) : CardRenderData()
    
    /**
     * Data for CashFlowTable card
     */
    data class CashFlowTable(
        val config: CashFlowTableCardConfig,
        val data: CashFlowTableData
    ) : CardRenderData()
    
    /**
     * Data for PlannedPaymentsTimeline card
     */
    data class PlannedPaymentsTimeline(
        val config: PlannedPaymentsTimelineCardConfig,
        val data: PlannedPaymentsTimelineData
    ) : CardRenderData()
    
    /**
     * Data for BalanceByAccounts card
     */
    data class BalanceByAccounts(
        val config: BalanceByAccountsCardConfig,
        val data: BalanceByAccountsData
    ) : CardRenderData()
    
    /**
     * Data for BalanceTrend card
     */
    data class BalanceTrend(
        val config: BalanceTrendCardConfig,
        val data: BalanceTrendData
    ) : CardRenderData()
}
