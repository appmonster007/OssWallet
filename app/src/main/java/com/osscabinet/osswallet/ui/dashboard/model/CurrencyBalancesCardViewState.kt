package com.osscabinet.ossWallet.ui.dashboard.model

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Lean ViewState for CurrencyBalancesCard
 * Pre-computed display items with formatted strings
 */
data class CurrencyBalancesCardViewState(
    override val cardId: String,
    override val isLoading: Boolean = false,
    val currencyItems: List<CurrencyBalanceDisplayItem>,
    val layout: String, // "scroll" or "grid"
    val timelineLabel: String,
    val timelineRange: TimelineRange,
    val error: String? = null
) : CardViewState {
    
    companion object {
        fun loading(cardId: String) = CurrencyBalancesCardViewState(
            cardId = cardId,
            isLoading = true,
            currencyItems = emptyList(),
            layout = "scroll",
            timelineLabel = "",
            timelineRange = TimelineRange.AllTime
        )
        
        fun error(cardId: String, message: String) = CurrencyBalancesCardViewState(
            cardId = cardId,
            isLoading = false,
            currencyItems = emptyList(),
            layout = "scroll",
            timelineLabel = "",
            timelineRange = TimelineRange.AllTime,
            error = message
        )
    }
}

/**
 * Pre-computed display item for a single currency balance
 * All formatting done in ViewModel/Domain layer
 */
data class CurrencyBalanceDisplayItem(
    val currency: String,
    val formattedAmount: String,
    val formattedBaseConversion: String?, // null if not needed or same currency
    val showBaseConversion: Boolean
)

/**
 * Intents for CurrencyBalancesCard
 */
sealed class CurrencyBalancesCardIntent : CardIntent {
    data class UpdateLayout(
        override val cardId: String,
        val layout: String
    ) : CurrencyBalancesCardIntent()
    
    data class UpdateShowBaseConversion(
        override val cardId: String,
        val show: Boolean
    ) : CurrencyBalancesCardIntent()
    
    data class UpdateTimelineRange(
        override val cardId: String,
        val range: TimelineRange
    ) : CurrencyBalancesCardIntent()
}
