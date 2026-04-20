package com.osscabinet.ossWallet.domain.model.settings

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Base sealed class for all accounts view card configurations.
 * Each card type extends this with its specific settings.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("cardType")
sealed class AccountsViewCardConfig : BaseCardConfig {
    abstract override val cardId: String
    abstract override val title: String
    abstract override val isEnabled: Boolean
    abstract override val order: Int
}

/**
 * Configuration for Net Worth card instances in Accounts View
 */
@Serializable
@SerialName("net_worth")
data class AccountsViewNetWorthCardConfig(
    override val cardId: String,
    override val title: String = "NET WORTH",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    val fontSize: String = "Medium",
    val timelineRange: TimelineRange = TimelineRange.AllTime
) : AccountsViewCardConfig()

/**
 * Configuration for Currency Balances card instances in Accounts View
 */
@Serializable
@SerialName("currency_balances")
data class AccountsViewCurrencyBalancesCardConfig(
    override val cardId: String,
    override val title: String = "CURRENCY BALANCES",
    override val isEnabled: Boolean = true,
    override val order: Int = 1,
    val layout: String = "scroll",
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
) : AccountsViewCardConfig()

/**
 * Configuration for Cash Flow card instances in Accounts View
 */
@Serializable
@SerialName("cash_flow")
data class AccountsViewCashFlowCardConfig(
    override val cardId: String,
    override val title: String = "CASH FLOW",
    override val isEnabled: Boolean = true,
    override val order: Int = 2,
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
) : AccountsViewCardConfig()

/**
 * Configuration for Accounts List card instances in Accounts View
 */
@Serializable
@SerialName("accounts_list")
data class AccountsViewAccountsListCardConfig(
    override val cardId: String,
    override val title: String = "ACCOUNTS LIST",
    override val isEnabled: Boolean = true,
    override val order: Int = 3,
    val layout: String = "grid",
    val gridColumns: Int = 2,
    val showAddAccountCard: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
) : AccountsViewCardConfig()

@Serializable
data class AccountsViewCardsConfig(
    val cards: List<AccountsViewCardConfig> = emptyList()
)
