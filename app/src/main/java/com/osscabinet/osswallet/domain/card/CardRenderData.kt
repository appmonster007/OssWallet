package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.PlannedPaymentSummary
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.service.*
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import java.math.BigDecimal

enum class CashFlowComparisonGranularity { DAY, WEEK, MONTH }

data class CashFlowTimePoint(
    val label: String,
    val epochMillis: Long,
    val income: BigDecimal,
    val expense: BigDecimal,
    val transfer: BigDecimal,
    val net: BigDecimal,
    val bucketEnd: Long = 0L
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
    abstract val cardType: CardType?

    data class NetWorth(
        val config: NetWorthCardConfig,
        val currentPeriodMetrics: NetWorthMetrics,
        val currency: String,
        val previousPeriodMetrics: NetWorthMetrics? = null
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.NET_WORTH
    }

    data class CurrencyBalances(
        val config: CurrencyBalancesCardConfig,
        val items: List<CurrencyAggregationItem>,
        val currency: String,
        val exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot(anchor = currency, rates = emptyMap())
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CURRENCY_BALANCES
    }

    data class CashFlow(
        val config: CashFlowCardConfig,
        val result: CashFlowResult,
        val currency: String
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CASH_FLOW
    }

    data class Accounts(
        val config: AccountsCardConfig,
        val updatedAccounts: List<Account>,
        val accountSummaries: Map<String, AccountSummaryItem>,
        val currency: String,
        val currentTheme: String,
        val exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot(anchor = currency, rates = emptyMap())
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.ACCOUNTS
    }

    data class CategoryPieChart(
        val config: CategoryPieChartCardConfig,
        val items: List<CategoryTreeMetrics>,
        val currency: String,
        val previousTotal: BigDecimal? = null,
        val incomeTotal: BigDecimal = BigDecimal.ZERO,
        val expenseTotal: BigDecimal = BigDecimal.ZERO,
        val transferTotal: BigDecimal = BigDecimal.ZERO,
        val previousIncomeTotal: BigDecimal? = null,
        val previousExpenseTotal: BigDecimal? = null,
        val previousTransferTotal: BigDecimal? = null
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CATEGORY_PIE_CHART
    }

    data class CategoryTreemap(
        val config: CategoryTreemapCardConfig,
        val items: List<CategoryTreeMetrics>,
        val currency: String,
        val previousTotal: BigDecimal? = null,
        val incomeTotal: BigDecimal = BigDecimal.ZERO,
        val expenseTotal: BigDecimal = BigDecimal.ZERO,
        val transferTotal: BigDecimal = BigDecimal.ZERO,
        val previousIncomeTotal: BigDecimal? = null,
        val previousExpenseTotal: BigDecimal? = null,
        val previousTransferTotal: BigDecimal? = null
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CATEGORY_TREEMAP
    }

    data class PlannedPayments(
        val config: PlannedPaymentsCardConfig,
        val items: List<PlannedPaymentSummary>,
        val currency: String,
        val exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot(anchor = currency, rates = emptyMap())
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.PLANNED_PAYMENTS
    }

    data class Budgets(
        val config: BudgetsCardConfig,
        val items: List<BudgetProgress>,
        val currency: String
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.BUDGETS
    }

    data class Loans(
        val config: LoansCardConfig,
        val items: List<LoanSummary>,
        val currency: String,
        val exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot(anchor = currency, rates = emptyMap())
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.LOANS
    }

    data class AccountHero(
        val config: AccountHeroCardConfig,
        val summary: AccountSummaryItem,
        val currency: String,
        val exchangeRates: ExchangeRateSnapshot
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.ACCOUNT_HERO
    }

    data class RecentActivities(
        val config: RecentActivitiesCardConfig,
        val listState: TransactionListState,
        val currency: String,
        val exchangeRates: ExchangeRateSnapshot
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.RECENT_ACTIVITIES
    }

    data class CashFlowComparison(
        val config: CashFlowComparisonCardConfig,
        val currentSeries: CashFlowComparisonSeries,
        val previousSeries: CashFlowComparisonSeries,
        val lastYearSeries: CashFlowComparisonSeries,
        val currency: String,
        val granularity: CashFlowComparisonGranularity,
        // Labels for every bucket in the full current period (no zero-origin sentinel).
        // Used to drive the x-axis when currentSeries is truncated at today.
        val periodLabels: List<String> = emptyList(),
        // Expected total buckets for each period if they were completed.
        // Used to draw "End of Period" markers when lengths differ.
        val currentTotalBuckets: Int = 0,
        val previousTotalBuckets: Int = 0,
        val lastYearTotalBuckets: Int = 0
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CASHFLOW_COMPARISON
    }

    data class IncomeExpenseBar(
        val config: IncomeExpenseBarCardConfig,
        val buckets: List<CashFlowTimePoint>,
        val granularity: CashFlowComparisonGranularity,
        val currency: String,
        // Index of the last bucket whose start time is ≤ today.
        // -1 means all buckets are in the future. Used to terminate the net line at today.
        val todayBucketIndex: Int = -1
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.INCOME_EXPENSE_BAR
    }

    data class CategorySpendingTrend(
        val config: CategorySpendingTrendCardConfig,
        val buckets: List<CategorySpendingBucket>,
        val granularity: CashFlowComparisonGranularity,
        val activeCategories: List<CategorySummary>,
        val currency: String
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CATEGORY_SPENDING_TREND
    }

    data class TagUpSetPlot(
        val config: TagUpSetPlotCardConfig,
        val sets: List<TagUpSetSet>,
        val intersections: List<TagUpSetIntersection>,
        val currency: String
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.TAG_UPSET_PLOT
    }

    data class Error(val message: String?) : CardRenderData() {
        override val cardType: CardType? get() = null
    }
}

data class TagUpSetSet(
    val tagName: String,
    val totalAmount: BigDecimal,
    val income: BigDecimal = BigDecimal.ZERO,
    val expense: BigDecimal = BigDecimal.ZERO,
    val incomeByCurrency: Map<String, BigDecimal> = emptyMap(),
    val expenseByCurrency: Map<String, BigDecimal> = emptyMap(),
    val transactionCount: Int = 0,
    val colorHex: Int = -1
)

data class TagUpSetIntersection(
    val tags: Set<String>,
    val amount: BigDecimal,
    val income: BigDecimal = BigDecimal.ZERO,
    val expense: BigDecimal = BigDecimal.ZERO,
    val incomeByCurrency: Map<String, BigDecimal> = emptyMap(),
    val expenseByCurrency: Map<String, BigDecimal> = emptyMap(),
    val transactionCount: Int
)

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
