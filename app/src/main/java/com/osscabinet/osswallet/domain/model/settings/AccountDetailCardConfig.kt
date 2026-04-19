package com.osscabinet.ossWallet.domain.model.settings

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Base sealed class for all account detail card configurations.
 * Each card type extends this with its specific settings.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("cardType")
sealed class AccountDetailCardConfig {
    abstract val cardId: String
    abstract val title: String
    abstract val isEnabled: Boolean
    abstract val order: Int
}

/**
 * Configuration for Account Hero card instances
 */
@Serializable
@SerialName("account_hero")
data class AccountHeroCardConfig(
    override val cardId: String,
    override val title: String = "ACCOUNT SUMMARY",
    override val isEnabled: Boolean = true,
    override val order: Int = 0
) : AccountDetailCardConfig()

/**
 * Configuration for Account Cash Flow card instances
 */
@Serializable
@SerialName("account_cash_flow")
data class AccountCashFlowCardConfig(
    override val cardId: String,
    override val title: String = "CASH FLOW",
    override val isEnabled: Boolean = true,
    override val order: Int = 1,
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
) : AccountDetailCardConfig()

/**
 * Configuration for Account Currency Balances card instances
 */
@Serializable
@SerialName("account_currency_balances")
data class AccountCurrencyBalancesCardConfig(
    override val cardId: String,
    override val title: String = "CURRENCY BALANCES",
    override val isEnabled: Boolean = true,
    override val order: Int = 2,
    val layout: String = "scroll",
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
) : AccountDetailCardConfig()

@Serializable
data class AccountDetailCardsConfig(
    val cards: List<AccountDetailCardConfig> = emptyList()
)
