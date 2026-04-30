package com.osscabinet.ossWallet.domain.model.settings

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
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
    abstract override val screenContext: ScreenContext
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
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val layout: String = "scroll",
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val filter: TransactionFilter = TransactionFilter()
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
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val filter: TransactionFilter = TransactionFilter()
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
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val layout: String = "grid",
    val gridColumns: Int = 2,
    val showAddAccountCard: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
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
    override val screenContext: ScreenContext = ScreenContext.ANALYTICS,
    val showLabels: Boolean = true,
    val showLegend: Boolean = true,
    val filter: TransactionFilter = TransactionFilter()
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
    override val screenContext: ScreenContext = ScreenContext.ANALYTICS,
    val showLabels: Boolean = true,
    val showLegend: Boolean = true,
    val filter: TransactionFilter = TransactionFilter()
) : CardConfig()

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
    val showCurrencyBadges: Boolean = true
) : CardConfig()

@Serializable
data class DashboardCardsConfig(
    val cards: List<CardConfig> = emptyList()
)

@Serializable
data class AnalyticsCardsConfig(
    val cards: List<CardConfig> = emptyList()
)
