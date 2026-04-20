package com.osscabinet.ossWallet.ui.dashboard.model

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Lean ViewState for CashFlowCard
 * Pre-computed income/expense breakdown with formatted strings
 */
data class CashFlowCardViewState(
    override val cardId: String,
    override val isLoading: Boolean = false,
    val formattedTotalIncome: String,
    val formattedTotalExpense: String,
    val incomeBreakdown: List<CashFlowBreakdownDisplayItem>,
    val expenseBreakdown: List<CashFlowBreakdownDisplayItem>,
    val showBaseConversion: Boolean,
    val timelineLabel: String,
    val timelineRange: TimelineRange,
    val error: String? = null
) : CardViewState {
    
    companion object {
        fun loading(cardId: String) = CashFlowCardViewState(
            cardId = cardId,
            isLoading = true,
            formattedTotalIncome = "",
            formattedTotalExpense = "",
            incomeBreakdown = emptyList(),
            expenseBreakdown = emptyList(),
            showBaseConversion = true,
            timelineLabel = "",
            timelineRange = TimelineRange.AllTime
        )
        
        fun error(cardId: String, message: String) = CashFlowCardViewState(
            cardId = cardId,
            isLoading = false,
            formattedTotalIncome = "",
            formattedTotalExpense = "",
            incomeBreakdown = emptyList(),
            expenseBreakdown = emptyList(),
            showBaseConversion = true,
            timelineLabel = "",
            timelineRange = TimelineRange.AllTime,
            error = message
        )
    }
}

/**
 * Pre-computed display item for cash flow breakdown
 * All formatting done in ViewModel/Domain layer
 */
data class CashFlowBreakdownDisplayItem(
    val currency: String,
    val formattedAmount: String,
    val formattedBaseConversion: String?, // null if not needed
    val showBaseConversion: Boolean
)

/**
 * Intents for CashFlowCard
 */
sealed class CashFlowCardIntent : CardIntent {
    data class UpdateShowBaseConversion(
        override val cardId: String,
        val show: Boolean
    ) : CashFlowCardIntent()
    
    data class UpdateTimelineRange(
        override val cardId: String,
        val range: TimelineRange
    ) : CashFlowCardIntent()
}
