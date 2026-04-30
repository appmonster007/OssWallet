package com.osscabinet.ossWallet.domain.model.settings

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

/**
 * Settings for the Accounts section.
 */
@Serializable
data class AccountsSectionSettings(
    val layout: String = "grid",
    val gridColumns: Int = 2,
    val showAddAccountCard: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
)

/**
 * Settings for the Currency Balances section.
 */
@Serializable
data class CurrencyBalancesSettings(
    val layout: String = "scroll",
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
)

/**
 * Settings for the Cash Flow section.
 */
@Serializable
data class CashFlowSettings(
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
)
