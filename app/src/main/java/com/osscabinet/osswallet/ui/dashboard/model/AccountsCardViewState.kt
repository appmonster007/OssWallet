package com.osscabinet.ossWallet.ui.dashboard.model

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Lean ViewState for AccountsCard
 * Pre-computed account display items with stats
 */
data class AccountsCardViewState(
    override val cardId: String,
    override val isLoading: Boolean = false,
    val accountItems: List<AccountDisplayItem>,
    val gridColumns: Int,
    val layout: String, // "scroll" or "grid"
    val showAddAccountCard: Boolean,
    val timelineLabel: String,
    val timelineRange: TimelineRange,
    val error: String? = null
) : CardViewState {
    
    companion object {
        fun loading(cardId: String) = AccountsCardViewState(
            cardId = cardId,
            isLoading = true,
            accountItems = emptyList(),
            gridColumns = 2,
            layout = "grid",
            showAddAccountCard = true,
            timelineLabel = "",
            timelineRange = TimelineRange.AllTime
        )
        
        fun error(cardId: String, message: String) = AccountsCardViewState(
            cardId = cardId,
            isLoading = false,
            accountItems = emptyList(),
            gridColumns = 2,
            layout = "grid",
            showAddAccountCard = true,
            timelineLabel = "",
            timelineRange = TimelineRange.AllTime,
            error = message
        )
    }
}

/**
 * Pre-computed display item for a single account
 * All formatting and stats lookup done in ViewModel/Domain layer
 */
data class AccountDisplayItem(
    val accountId: String,
    val accountName: String,
    val accountCurrency: String,
    val accountColorHex: String,
    val accountIcon: String,
    val formattedBalanceInBase: String,
    val currencyBalances: Map<String, String>, // currency -> formatted balance
    val formattedIncomeInAccountCurrency: String,
    val formattedExpenseInAccountCurrency: String,
    val isDarkTheme: Boolean,
    val isAmoled: Boolean
)

/**
 * Intents for AccountsCard
 */
sealed class AccountsCardIntent : CardIntent {
    data class UpdateLayout(
        override val cardId: String,
        val layout: String
    ) : AccountsCardIntent()
    
    data class UpdateGridColumns(
        override val cardId: String,
        val columns: Int
    ) : AccountsCardIntent()
    
    data class UpdateShowAddAccountCard(
        override val cardId: String,
        val show: Boolean
    ) : AccountsCardIntent()
    
    data class UpdateTimelineRange(
        override val cardId: String,
        val range: TimelineRange
    ) : AccountsCardIntent()
}
