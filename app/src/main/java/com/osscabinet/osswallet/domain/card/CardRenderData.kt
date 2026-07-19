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
import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import com.osscabinet.ossWallet.domain.service.*
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.financial.CategoryTreeMetrics
import kotlinx.serialization.Serializable
import java.math.BigDecimal

enum class CashFlowComparisonGranularity { DAY, WEEK, MONTH }

@Serializable
data class CashFlowTimePoint(
    val label: String,
    val epochMillis: Long,
    @Serializable(with = BigDecimalSerializer::class)
    val income: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val expense: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val transfer: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val net: BigDecimal,
    val bucketEnd: Long = 0L
)

@Serializable
data class CashFlowComparisonSeries(val points: List<CashFlowTimePoint>)

/**
 * Metrics specifically for Net Worth card rendering.
 */
@Serializable
data class NetWorthMetrics(
    @Serializable(with = BigDecimalSerializer::class)
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
@Serializable
sealed class CardRenderData {
    abstract val cardType: CardType?

    @Serializable
    data class NetWorth(
        val config: NetWorthCardConfig,
        val currentPeriodMetrics: NetWorthMetrics,
        val currency: String,
        val previousPeriodMetrics: NetWorthMetrics? = null
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.NET_WORTH
    }

    @Serializable
    data class CurrencyBalances(
        val config: CurrencyBalancesCardConfig,
        val items: List<CurrencyAggregationItem>,
        val currency: String,
        val exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot(anchor = currency, rates = emptyMap())
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CURRENCY_BALANCES
    }

    @Serializable
    data class CashFlow(
        val config: CashFlowCardConfig,
        val result: CashFlowResult,
        val currency: String,
        val previousResult: CashFlowResult? = null
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CASH_FLOW
    }

    @Serializable
    data class Accounts(
        val config: AccountsCardConfig,
        val updatedAccounts: List<Account>,
        val accountSummaries: Map<String, AccountSummaryItem>,
        val currency: String,
        val currentTheme: String,
        val previousSummaries: Map<String, AccountSummaryItem>? = null,
        val exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot(anchor = currency, rates = emptyMap())
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.ACCOUNTS
    }

    @Serializable
    data class CategoryPieChart(
        val config: CategoryPieChartCardConfig,
        val items: List<CategoryTreeMetrics>,
        val currency: String,
        @Serializable(with = BigDecimalSerializer::class)
        val previousTotal: BigDecimal? = null,
        @Serializable(with = BigDecimalSerializer::class)
        val incomeTotal: BigDecimal = BigDecimal.ZERO,
        @Serializable(with = BigDecimalSerializer::class)
        val expenseTotal: BigDecimal = BigDecimal.ZERO,
        @Serializable(with = BigDecimalSerializer::class)
        val transferTotal: BigDecimal = BigDecimal.ZERO,
        @Serializable(with = BigDecimalSerializer::class)
        val previousIncomeTotal: BigDecimal? = null,
        @Serializable(with = BigDecimalSerializer::class)
        val previousExpenseTotal: BigDecimal? = null,
        @Serializable(with = BigDecimalSerializer::class)
        val previousTransferTotal: BigDecimal? = null
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CATEGORY_PIE_CHART
    }

    @Serializable
    data class CategoryTreemap(
        val config: CategoryTreemapCardConfig,
        val items: List<CategoryTreeMetrics>,
        val currency: String,
        @Serializable(with = BigDecimalSerializer::class)
        val previousTotal: BigDecimal? = null,
        @Serializable(with = BigDecimalSerializer::class)
        val incomeTotal: BigDecimal = BigDecimal.ZERO,
        @Serializable(with = BigDecimalSerializer::class)
        val expenseTotal: BigDecimal = BigDecimal.ZERO,
        @Serializable(with = BigDecimalSerializer::class)
        val transferTotal: BigDecimal = BigDecimal.ZERO,
        @Serializable(with = BigDecimalSerializer::class)
        val previousIncomeTotal: BigDecimal? = null,
        @Serializable(with = BigDecimalSerializer::class)
        val previousExpenseTotal: BigDecimal? = null,
        @Serializable(with = BigDecimalSerializer::class)
        val previousTransferTotal: BigDecimal? = null
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CATEGORY_TREEMAP
    }

    @Serializable
    data class PlannedPayments(
        val config: PlannedPaymentsCardConfig,
        val items: List<PlannedPaymentSummary>,
        val currency: String,
        val previousItems: List<PlannedPaymentSummary>? = null,
        val exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot(anchor = currency, rates = emptyMap())
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.PLANNED_PAYMENTS
    }

    @Serializable
    data class Budgets(
        val config: BudgetsCardConfig,
        val items: List<BudgetProgress>,
        val currency: String,
        val previousItems: List<BudgetProgress>? = null
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.BUDGETS
    }

    @Serializable
    data class Loans(
        val config: LoansCardConfig,
        val items: List<LoanSummary>,
        val currency: String,
        val exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot(anchor = currency, rates = emptyMap())
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.LOANS
    }

    @Serializable
    data class AccountHero(
        val config: AccountHeroCardConfig,
        val summary: AccountSummaryItem,
        val currency: String,
        val exchangeRates: ExchangeRateSnapshot
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.ACCOUNT_HERO
    }

    @Serializable
    data class RecentActivities(
        val config: RecentActivitiesCardConfig,
        val listState: TransactionListState,
        val currency: String,
        val exchangeRates: ExchangeRateSnapshot
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.RECENT_ACTIVITIES
    }

    @Serializable
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

    @Serializable
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

    @Serializable
    data class CategorySpendingTrend(
        val config: CategorySpendingTrendCardConfig,
        val buckets: List<CategorySpendingBucket>,
        val granularity: CashFlowComparisonGranularity,
        val activeCategories: List<CategorySummary>,
        val currency: String
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.CATEGORY_SPENDING_TREND
    }

    @Serializable
    data class TagUpSetPlot(
        val config: TagUpSetPlotCardConfig,
        val sets: List<TagUpSetSet>,
        val intersections: List<TagUpSetIntersection>,
        val currency: String
    ) : CardRenderData() {
        override val cardType: CardType get() = CardType.TAG_UPSET_PLOT
    }

    @Serializable
    data class Error(val message: String?) : CardRenderData() {
        override val cardType: CardType? get() = null
    }
}

@Serializable
data class TagUpSetSet(
    val tagName: String,
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val income: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val expense: BigDecimal = BigDecimal.ZERO,
    val incomeByCurrency: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal> = emptyMap(),
    val expenseByCurrency: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal> = emptyMap(),
    val transactionCount: Int = 0,
    val colorHex: Int = -1
)

@Serializable
data class TagUpSetIntersection(
    val tags: Set<String>,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val income: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val expense: BigDecimal = BigDecimal.ZERO,
    val incomeByCurrency: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal> = emptyMap(),
    val expenseByCurrency: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal> = emptyMap(),
    val transactionCount: Int
)

@Serializable
data class CategorySpendingBucket(
    val label: String,
    val epochMillis: Long,
    val categoryAmounts: List<CategoryAmount>
)

@Serializable
data class CategoryAmount(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: Int,
    val nature: CategoryNature,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal
)

@Serializable
data class CategorySummary(
    val categoryId: Long,
    val name: String,
    val colorHex: Int,
    val nature: CategoryNature,
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal
)
