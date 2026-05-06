package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.service.*
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import java.math.BigDecimal

/**
 * Sealed class hierarchy for unified card rendering data.
 * Every card computation returns a subtype of this class.
 */
sealed class CardRenderData {
    
    data class NetWorth(
        val config: NetWorthCardConfig,
        val value: BigDecimal,
        val cashFlowResult: CashFlowResult,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()

    data class CurrencyBalances(
        val config: CurrencyBalancesCardConfig,
        val items: List<CurrencyAggregationItem>,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()

    data class CashFlow(
        val config: CashFlowCardConfig,
        val result: CashFlowResult,
        val baseCurrency: String,
        val precision: Int
    ) : CardRenderData()

    data class Accounts(
        val config: AccountsCardConfig,
        val updatedAccounts: List<Account>,
        val accountStats: Map<String, AccountSummaryItem>,
        val baseCurrency: String,
        val currentTheme: String,
        val precision: Int
    ) : CardRenderData()

    data class CategoryPieChart(
        val config: CategoryPieChartCardConfig,
        val items: List<CategoryTreeMetrics>,
        val precision: Int
    ) : CardRenderData()

    data class CategoryTreemap(
        val config: CategoryTreemapCardConfig,
        val items: List<CategoryTreeMetrics>,
        val precision: Int
    ) : CardRenderData()

    data class PlannedPayments(
        val config: PlannedPaymentsCardConfig,
        val upcoming: List<UpcomingPaymentItem>,
        val overdue: List<PlannedPayment>,
        val precision: Int
    ) : CardRenderData()

    data class Budgets(
        val config: com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig,
        val items: List<BudgetProgress>,
        val precision: Int
    ) : CardRenderData()

    data class Loans(
        val config: com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig,
        val items: List<LoanSummary>,
        val precision: Int
    ) : CardRenderData()

    data class AccountHero(
        val config: AccountHeroCardConfig,
        val account: Account,
        val balance: BigDecimal,
        val income: BigDecimal,
        val expense: BigDecimal,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val currencyBalances: Map<String, BigDecimal> = emptyMap(),
        val precision: Int
    ) : CardRenderData()
}
