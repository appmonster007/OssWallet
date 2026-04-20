package com.osscabinet.ossWallet.ui.dashboard.model

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Lean ViewState for NetWorthCard
 * All formatting and computations done in ViewModel/Domain layer
 */
data class NetWorthCardViewState(
    override val cardId: String,
    override val isLoading: Boolean = false,
    val formattedNetWorth: String,
    val formattedNetIncome: String,
    val formattedNetExpense: String,
    val baseCurrency: String,
    val fontSize: String,
    val timelineLabel: String,
    val timelineRange: TimelineRange,
    val error: String? = null
) : CardViewState {
    
    companion object {
        fun loading(cardId: String) = NetWorthCardViewState(
            cardId = cardId,
            isLoading = true,
            formattedNetWorth = "",
            formattedNetIncome = "",
            formattedNetExpense = "",
            baseCurrency = "",
            fontSize = "Medium",
            timelineLabel = "",
            timelineRange = TimelineRange.AllTime
        )
        
        fun error(cardId: String, message: String) = NetWorthCardViewState(
            cardId = cardId,
            isLoading = false,
            formattedNetWorth = "",
            formattedNetIncome = "",
            formattedNetExpense = "",
            baseCurrency = "",
            fontSize = "Medium",
            timelineLabel = "",
            timelineRange = TimelineRange.AllTime,
            error = message
        )
    }
}

/**
 * Intents for NetWorthCard
 */
sealed class NetWorthCardIntent : CardIntent {
    data class UpdateFontSize(
        override val cardId: String,
        val fontSize: String
    ) : NetWorthCardIntent()
    
    data class UpdateTimelineRange(
        override val cardId: String,
        val range: TimelineRange
    ) : NetWorthCardIntent()
}
