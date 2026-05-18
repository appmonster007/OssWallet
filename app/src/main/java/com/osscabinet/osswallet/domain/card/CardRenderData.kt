package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.service.*
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import java.math.BigDecimal

enum class CashFlowComparisonGranularity { DAY, WEEK, MONTH }

data class CashFlowTimePoint(
    val label: String,
    val epochMillis: Long,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal
)

data class CashFlowComparisonSeries(val points: List<CashFlowTimePoint>)

/**
 * Metrics specifically for Net Worth card rendering.
 */
data class NetWorthMetrics(
    val netWorth: BigDecimal,
    val cashFlow: CashFlowResult
) {
    companion object {
        fun empty() = NetWorthMetrics(
            netWorth = BigDecimal.ZERO,
            cashFlow = CashFlowResult.empty()
        )
    }
}

/**
 * Sealed class hierarchy for unified card rendering data.
 * Every card computation returns a subtype of this class.
 */
sealed class CardRenderData {
    
    data class NetWorth(
        val config: NetWorthCardConfig,
        val currentMetrics: NetWorthMetrics,
        val baseCurrency: String,
        val previousMetrics: NetWorthMetrics? = null
    ) : CardRenderData()

    data class CurrencyBalances(
        val config: CurrencyBalancesCardConfig,
        val items: List<CurrencyAggregationItem>,
        val baseCurrency: String
    ) : CardRenderData()

    data class CashFlow(
        val config: CashFlowCardConfig,
        val result: CashFlowResult,
        val baseCurrency: String
    ) : CardRenderData()

    data class Accounts(
        val config: AccountsCardConfig,
        val updatedAccounts: List<Account>,
        val accountStats: Map<String, AccountSummaryItem>,
        val baseCurrency: String,
        val currentTheme: String
    ) : CardRenderData()

    data class CategoryPieChart(
        val config: CategoryPieChartCardConfig,
        val items: List<CategoryTreeMetrics>,
        val previousTotal: BigDecimal? = null
    ) : CardRenderData()

    data class CategoryTreemap(
        val config: CategoryTreemapCardConfig,
        val items: List<CategoryTreeMetrics>
    ) : CardRenderData()

    data class PlannedPayments(
        val config: PlannedPaymentsCardConfig,
        val upcoming: List<UpcomingPaymentItem>,
        val overdue: List<PlannedPayment>
    ) : CardRenderData()

    data class Budgets(
        val config: BudgetsCardConfig,
        val items: List<BudgetProgress>
    ) : CardRenderData()

    data class Loans(
        val config: LoansCardConfig,
        val items: List<LoanSummary>
    ) : CardRenderData()

    data class AccountHero(
        val config: AccountHeroCardConfig,
        val account: Account,
        val balance: BigDecimal,
        val income: BigDecimal,
        val expense: BigDecimal,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val currencyBalances: Map<String, BigDecimal> = emptyMap()
    ) : CardRenderData()

    data class RecentActivities(
        val config: RecentActivitiesCardConfig,
        val listState: TransactionListState,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>
    ) : CardRenderData()

    data class CashFlowComparison(
        val config: CashFlowComparisonCardConfig,
        val currentSeries: CashFlowComparisonSeries,
        val previousSeries: CashFlowComparisonSeries,
        val lastYearSeries: CashFlowComparisonSeries,
        val baseCurrency: String,
        val granularity: CashFlowComparisonGranularity,
        // Labels for every bucket in the full current period (no zero-origin sentinel).
        // Used to drive the x-axis when currentSeries is truncated at today.
        val periodLabels: List<String> = emptyList()
    ) : CardRenderData()

    data class IncomeExpenseBar(
        val config: com.osscabinet.ossWallet.domain.model.settings.IncomeExpenseBarCardConfig,
        val buckets: List<CashFlowTimePoint>,
        val granularity: CashFlowComparisonGranularity,
        val baseCurrency: String,
        // Index of the last bucket whose start time is ≤ today.
        // -1 means all buckets are in the future. Used to terminate the net line at today.
        val todayBucketIndex: Int = -1
    ) : CardRenderData()

    data class CategorySpendingTrend(
        val config: com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig,
        val buckets: List<CategorySpendingBucket>,
        val granularity: CashFlowComparisonGranularity,
        val activeCategories: List<CategorySummary>,
        val baseCurrency: String
    ) : CardRenderData()
}

data class CategorySpendingBucket(
    val label: String,
    val epochMillis: Long,
    val categoryAmounts: List<CategoryAmount>
)

data class CategoryAmount(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: Int,
    val nature: CategoryNature,
    val amount: BigDecimal
)

data class CategorySummary(
    val categoryId: Long,
    val name: String,
    val colorHex: Int,
    val nature: CategoryNature,
    val totalAmount: BigDecimal
)
