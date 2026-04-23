package com.osscabinet.ossWallet.domain.model.settings

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator

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
 * Common settings shared across different card types.
 */
@Serializable
data class CardSettings(
    val timelineRange: TimelineRange = TimelineRange.AllTime,
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
    val fontSize: String = "Medium",
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val profileId: Int? = null,
    val selectedAccountIds: List<String>? = null
) : CardConfig()

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
    val layout: String = "scroll",
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val accountId: String? = null // For account scope (null = global)
) : CardConfig()

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
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val accountId: String? = null // For account scope (null = global)
) : CardConfig()

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
    val layout: String = "grid",
    val gridColumns: Int = 2,
    val showAddAccountCard: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
) : CardConfig()

/**
 * Configuration for Budgets card instances
 */
@Serializable
@SerialName("budgets")
data class BudgetsCardConfig(
    override val cardId: String,
    override val title: String = "BUDGETS",
    override val isEnabled: Boolean = true,
    override val order: Int = 4,
    val showProgressLabels: Boolean = true
) : CardConfig()

/**
 * Configuration for Category Breakdown card (Pie Wheel Chart)
 */
@Serializable
@SerialName("category_breakdown")
data class CategoryBreakdownCardConfig(
    override val cardId: String,
    override val title: String = "CATEGORY BREAKDOWN",
    override val isEnabled: Boolean = true,
    override val order: Int = 5,
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val showLabels: Boolean = true,
    val showLegend: Boolean = true,
    val accountId: String? = null // For account scope (null = global)
) : CardConfig()

/**
 * Configuration for Timeline Trajectory card (Multi-Line Chart)
 */
@Serializable
@SerialName("timeline_trajectory")
data class TimelineTrajectoryCardConfig(
    override val cardId: String,
    override val title: String = "TIMELINE TRAJECTORY",
    override val isEnabled: Boolean = true,
    override val order: Int = 6,
    val timelineRange: TimelineRange = TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
    val showBalance: Boolean = true,
    val showExpense: Boolean = true,
    val showIncome: Boolean = true,
    val accountId: String? = null // For account scope (null = global)
) : CardConfig()

/**
 * Configuration for Category Expense Treemap card
 */
@Serializable
@SerialName("category_expense_treemap")
data class CategoryExpenseTreemapCardConfig(
    override val cardId: String,
    override val title: String = "CATEGORY EXPENSES",
    override val isEnabled: Boolean = true,
    override val order: Int = 7,
    val timelineRange: TimelineRange = TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
    val showLabels: Boolean = true,
    val accountId: String? = null // For account scope (null = global)
) : CardConfig()

@Serializable
data class DashboardCardsConfig(
    val cards: List<CardConfig> = emptyList()
)

@Serializable
data class AnalyticsCardsConfig(
    val cards: List<CardConfig> = emptyList()
)
