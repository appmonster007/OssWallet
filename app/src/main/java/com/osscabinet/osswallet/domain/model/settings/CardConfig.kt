package com.osscabinet.ossWallet.domain.model.settings

import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator

import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.CardType

/**
 * Card scope determines the context in which a card operates.
 * GLOBAL: Dashboard - all accounts
 * ACCOUNT: Account Detail - single account
 * PROFILE: Future - profile-level cards
 */
@Serializable
enum class CardScope {
    GLOBAL,
    ACCOUNT,
    PROFILE
}

/**
 * Screen context identifies which screen a card belongs to.
 * This ensures card configurations are decoupled across screens.
 */
@Serializable
enum class ScreenContext {
    DASHBOARD,
    ACCOUNT_DETAIL,
    ACCOUNTS_VIEW,
    ANALYTICS
}

/**
 * Common settings shared across different card types.
 */
@Serializable
data class CardSettings(
    val showBaseConversion: Boolean = true,
    val layout: String = "grid"
)

/**
 * Base sealed class for all dashboard card configurations.
 * Each card type extends this with its specific settings.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("cardType")
sealed class CardConfig : BaseCardConfig {
    abstract override val cardId: String
    abstract override val title: String
    abstract override val isEnabled: Boolean
    abstract override val order: Int
    abstract override val screenContext: ScreenContext
    
    abstract val cardType: CardType

    /**
     * Transaction filter for this card instance.
     * Each card type has its own default filter.
     */
    abstract val filter: TransactionFilter

    /**
     * Rules that define constraints for this card's filter.
     * These are hardcoded per card type and used to restrict the filter UI.
     */
    abstract val filterRules: CardFilterRules

    companion object {
        fun createDefault(
            type: CardType,
            context: ScreenContext,
            cardId: String = java.util.UUID.randomUUID().toString()
        ): CardConfig = when (type) {
            CardType.NET_WORTH -> NetWorthCardConfig(cardId = cardId, screenContext = context)
            CardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(cardId = cardId, screenContext = context)
            CardType.CASH_FLOW -> CashFlowCardConfig(cardId = cardId, screenContext = context)
            CardType.ACCOUNTS -> AccountsCardConfig(cardId = cardId, screenContext = context)
            CardType.CATEGORY_PIE_CHART -> CategoryPieChartCardConfig(cardId = cardId, screenContext = context)
            CardType.CATEGORY_TREEMAP -> CategoryTreemapCardConfig(cardId = cardId, screenContext = context)
            CardType.PLANNED_PAYMENTS -> PlannedPaymentsCardConfig(cardId = cardId, screenContext = context)
            CardType.BUDGETS -> BudgetsCardConfig(cardId = cardId, screenContext = context)
            CardType.LOANS -> LoansCardConfig(cardId = cardId, screenContext = context)
            CardType.ACCOUNT_HERO -> AccountHeroCardConfig(cardId = cardId, screenContext = context)
            CardType.RECENT_ACTIVITIES -> RecentActivitiesCardConfig(cardId = cardId, screenContext = context)
            CardType.CASHFLOW_COMPARISON -> CashFlowComparisonCardConfig(cardId = cardId, screenContext = context)
            CardType.INCOME_EXPENSE_BAR -> IncomeExpenseBarCardConfig(cardId = cardId, screenContext = context)
            CardType.CATEGORY_SPENDING_TREND -> CategorySpendingTrendCardConfig(cardId = cardId, screenContext = context)
            CardType.TAG_UPSET_PLOT -> TagUpSetPlotCardConfig(cardId = cardId, screenContext = context)
        }
    }
}

/**
 * Configuration for Net Worth card instances
 */
@Serializable
@SerialName("net_worth")
data class NetWorthCardConfig(
    override val cardId: String,
    override val title: String = "NET WORTH",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val fontSize: String = "Medium",
    val showComparison: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter(),
    val profileId: Int? = null
) : CardConfig() {
    override val cardType: CardType get() = CardType.NET_WORTH
    override val filterRules: CardFilterRules get() = CardFilterRules.DEFAULT
}

/**
 * Configuration for Currency Balances card instances
 */
@Serializable
@SerialName("currency_balances")
data class CurrencyBalancesCardConfig(
    override val cardId: String,
    override val title: String = "CURRENCY BALANCES",
    override val isEnabled: Boolean = true,
    override val order: Int = 1,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val layout: String = "scroll",
    val showBaseConversion: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.CURRENCY_BALANCES
    override val filterRules: CardFilterRules get() = CardFilterRules.DEFAULT
}

/**
 * Configuration for Cash Flow card instances
 */
@Serializable
@SerialName("cash_flow")
data class CashFlowCardConfig(
    override val cardId: String,
    override val title: String = "CASH FLOW",
    override val isEnabled: Boolean = true,
    override val order: Int = 2,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val showBaseConversion: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.CASH_FLOW
    override val filterRules: CardFilterRules get() = CardFilterRules.DEFAULT
}

/**
 * Configuration for Accounts card instances
 */
@Serializable
@SerialName("accounts")
data class AccountsCardConfig(
    override val cardId: String,
    override val title: String = "ACCOUNTS",
    override val isEnabled: Boolean = true,
    override val order: Int = 3,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val layout: String = "grid",
    val gridColumns: Int = 2,
    val showAddAccountCard: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.ACCOUNTS
    override val filterRules: CardFilterRules get() = CardFilterRules.DEFAULT
}

/**
 * Configuration for Category Breakdown card (Pie Wheel Chart)
 */
@Serializable
@SerialName("category_breakdown")
data class CategoryPieChartCardConfig(
    override val cardId: String,
    override val title: String = "CATEGORY PIE CHART",
    override val isEnabled: Boolean = true,
    override val order: Int = 5,
    override val screenContext: ScreenContext = ScreenContext.ANALYTICS,
    val showLabels: Boolean = true,
    val showLegend: Boolean = true,
    val rounded: Boolean = true,
    val showFullBreakdown: Boolean = false,
    val showOnlyActiveCategories: Boolean = true,
    val showComparison: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.CATEGORY_PIE_CHART
    override val filterRules: CardFilterRules get() = CardFilterRules.INCOME_OR_EXPENSE_ONLY
}

/**
 * Configuration for Category Expense Treemap card
 */
@Serializable
@SerialName("category_expense_treemap")
data class CategoryTreemapCardConfig(
    override val cardId: String,
    override val title: String = "CATEGORY TREEMAP",
    override val isEnabled: Boolean = true,
    override val order: Int = 7,
    override val screenContext: ScreenContext = ScreenContext.ANALYTICS,
    val showLabels: Boolean = true,
    val showLegend: Boolean = true,
    val rounded: Boolean = true,
    val showFullBreakdown: Boolean = false,
    val showOnlyActiveCategories: Boolean = true,
    val showComparison: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.CATEGORY_TREEMAP
    override val filterRules: CardFilterRules get() = CardFilterRules.INCOME_OR_EXPENSE_ONLY
}

/**
 * Configuration for Account Hero card (account detail screen)
 */
@Serializable
@SerialName("account_hero")
data class AccountHeroCardConfig(
    override val cardId: String,
    override val title: String = "ACCOUNT HERO",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.ACCOUNT_DETAIL,
    val showCurrencyBadges: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.ACCOUNT_HERO
    override val filterRules: CardFilterRules get() = CardFilterRules(
        allowAccountSelection = false,
        allowCategorySelection = false,
        allowStatusSelection = false,
        allowSearch = false,
        allowAmountRange = false,
        allowCurrencySelection = false,
        allowPaymentMethodSelection = false,
        allowTimelineSelection = false,
        allowFutureTransactionsToggle = false
    )
}

/**
 * Configuration for Planned Payments card instances
 */
/** Three-stage visibility mode for the planned-payments dashboard card. */
@Serializable
enum class PlannedPaymentsViewMode { OVERDUE_ONLY, BOTH, FUTURE_ONLY }

@Serializable
enum class UpSetDynamicFilteringMode { OFF, HORIZONTAL, VERTICAL }

@Serializable
enum class UpSetTagSortMode { NAME_ASC, NAME_DESC, DECREASING_ABS, INCREASING_ABS, DECREASING, INCREASING }

@Serializable
enum class UpSetIntersectionSortMode { DECREASING_ABS, INCREASING_ABS, DECREASING, INCREASING }

@Serializable
@SerialName("planned_payments")
data class PlannedPaymentsCardConfig(
    override val cardId: String,
    override val title: String = "PLANNED PAYMENTS",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val daysAhead: Int = 30,
    val maxItems: Int = 5,
    /** Legacy field kept for JSON backwards-compat; prefer viewMode. */
    val showOverdue: Boolean = true,
    val viewMode: PlannedPaymentsViewMode = PlannedPaymentsViewMode.BOTH,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.PLANNED_PAYMENTS
    override val filterRules: CardFilterRules get() = CardFilterRules.DEFAULT
}

/**
 * Configuration for Budgets card instances
 */
@Serializable
@SerialName("budgets")
data class BudgetsCardConfig(
    override val cardId: String,
    override val title: String = "BUDGETS",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val showArchived: Boolean = false,
    val maxItems: Int = 5,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.BUDGETS
    override val filterRules: CardFilterRules get() = CardFilterRules.DEFAULT
}

/**
 * Configuration for Loans card instances
 */
@Serializable
@SerialName("loans")
data class LoansCardConfig(
    override val cardId: String,
    override val title: String = "LOANS",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val showType: String = "ALL",
    val maxItems: Int = 5,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.LOANS
    override val filterRules: CardFilterRules get() = CardFilterRules.DEFAULT
}

/**
 * Configuration for Recent Activities card instances
 */
@Serializable
@SerialName("recent_activities")
data class RecentActivitiesCardConfig(
    override val cardId: String,
    override val title: String = "RECENT ACTIVITIES",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val groupBy: TransactionGroupingType = TransactionGroupingType.BY_DAY,
    val showCurrencyBreakdown: Boolean = false,
    val useNoteAsHeader: Boolean = false,
    val showOpeningBalance: Boolean = false,
    val showClosingBalance: Boolean = true,
    val showAccount: Boolean = true,
    val showCategory: Boolean = true,
    val showNote: Boolean = true,
    val showTags: Boolean = true,
    val showTimestamp: Boolean = true,
    val showIcon: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.RECENT_ACTIVITIES
    override val filterRules: CardFilterRules get() = CardFilterRules.DEFAULT
}

/**
 * Metric selector for the Cash Flow Comparison card.
 */
@Serializable
enum class CashFlowComparisonMetric { NET, INCOME, EXPENSE }

/**
 * Configuration for Cash Flow Comparison card instances.
 */
@Serializable
@SerialName("cashflow_comparison")
data class CashFlowComparisonCardConfig(
    override val cardId: String,
    override val title: String = "CASH FLOW COMPARISON",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val selectedMetric: CashFlowComparisonMetric = CashFlowComparisonMetric.NET,
    val showCurrentPeriod: Boolean = true,
    val showPreviousPeriod: Boolean = true,
    val showSamePeriodLastYear: Boolean = true,
    val isCumulative: Boolean = true,
    val showGridLines: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.CASHFLOW_COMPARISON
    override val filterRules: CardFilterRules get() = CardFilterRules.INCOME_AND_EXPENSE
}

/**
 * Configuration for Income & Expense Bar chart card instances.
 */
@Serializable
enum class CategoryNatureFilter { ALL, MUST, NEED, WANT }

@Serializable
@SerialName("category_spending_trend")
data class CategorySpendingTrendCardConfig(
    override val cardId: String,
    override val title: String = "CATEGORY SPENDING",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val selectedNature: CategoryNatureFilter = CategoryNatureFilter.ALL,
    val showGridLines: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.CATEGORY_SPENDING_TREND
    override val filterRules: CardFilterRules get() = CardFilterRules.INCOME_OR_EXPENSE_ONLY
}

@Serializable
@SerialName("income_expense_bar")
data class IncomeExpenseBarCardConfig(
    override val cardId: String,
    override val title: String = "INCOME & EXPENSE",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val isCumulative: Boolean = false,
    val showGridLines: Boolean = true,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.INCOME_EXPENSE_BAR
    override val filterRules: CardFilterRules get() = CardFilterRules.INCOME_AND_EXPENSE
}

@Serializable
@SerialName("tag_upset_plot")
data class TagUpSetPlotCardConfig(
    override val cardId: String,
    override val title: String = "TAG UPSET PLOT",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val maxTags: Int = 7,
    val maxIntersections: Int = 15,
    val dynamicFilteringMode: UpSetDynamicFilteringMode = UpSetDynamicFilteringMode.OFF,
    val tagSortMode: UpSetTagSortMode = UpSetTagSortMode.DECREASING_ABS,
    val intersectionSortMode: UpSetIntersectionSortMode = UpSetIntersectionSortMode.DECREASING_ABS,
    override val filter: TransactionFilter = TransactionFilter()
) : CardConfig() {
    override val cardType: CardType get() = CardType.TAG_UPSET_PLOT
    override val filterRules: CardFilterRules get() = CardFilterRules.INCOME_OR_EXPENSE_ONLY
}

@Serializable
data class DashboardCardsConfig(
    val cards: List<CardConfig> = emptyList()
)

@Serializable
data class AnalyticsCardsConfig(
    val cards: List<CardConfig> = emptyList()
)

/**
 * Returns a copy of this config with its filter reset to the empty default.
 * Used when saving ACCOUNT_DETAIL configs so per-visit filter changes never persist.
 */
fun CardConfig.withClearedFilter(): CardConfig = when (this) {
    is NetWorthCardConfig -> copy(filter = TransactionFilter())
    is CurrencyBalancesCardConfig -> copy(filter = TransactionFilter())
    is CashFlowCardConfig -> copy(filter = TransactionFilter())
    is AccountsCardConfig -> copy(filter = TransactionFilter())
    is CategoryPieChartCardConfig -> copy(filter = TransactionFilter())
    is CategoryTreemapCardConfig -> copy(filter = TransactionFilter())
    is AccountHeroCardConfig -> copy(filter = TransactionFilter())
    is PlannedPaymentsCardConfig -> copy(filter = TransactionFilter())
    is BudgetsCardConfig -> copy(filter = TransactionFilter())
    is LoansCardConfig -> copy(filter = TransactionFilter())
    is RecentActivitiesCardConfig -> copy(filter = TransactionFilter())
    is CashFlowComparisonCardConfig -> copy(filter = TransactionFilter())
    is IncomeExpenseBarCardConfig -> copy(filter = TransactionFilter())
    is CategorySpendingTrendCardConfig -> copy(filter = TransactionFilter())
    is TagUpSetPlotCardConfig -> copy(filter = TransactionFilter())
}

/**
 * Returns a copy of this config with a new order value.
 */
fun CardConfig.withOrder(newOrder: Int): CardConfig = when (this) {
    is NetWorthCardConfig -> copy(order = newOrder)
    is CurrencyBalancesCardConfig -> copy(order = newOrder)
    is CashFlowCardConfig -> copy(order = newOrder)
    is AccountsCardConfig -> copy(order = newOrder)
    is CategoryPieChartCardConfig -> copy(order = newOrder)
    is CategoryTreemapCardConfig -> copy(order = newOrder)
    is AccountHeroCardConfig -> copy(order = newOrder)
    is PlannedPaymentsCardConfig -> copy(order = newOrder)
    is BudgetsCardConfig -> copy(order = newOrder)
    is LoansCardConfig -> copy(order = newOrder)
    is RecentActivitiesCardConfig -> copy(order = newOrder)
    is CashFlowComparisonCardConfig -> copy(order = newOrder)
    is IncomeExpenseBarCardConfig -> copy(order = newOrder)
    is CategorySpendingTrendCardConfig -> copy(order = newOrder)
    is TagUpSetPlotCardConfig -> copy(order = newOrder)
}
