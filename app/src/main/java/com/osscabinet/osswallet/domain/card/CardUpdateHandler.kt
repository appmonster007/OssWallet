package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

/**
 * Interface for handling card configuration updates.
 * Implemented by ViewModels to provide a unified way for UI to trigger updates.
 */
interface CardUpdateHandler {
    fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {}
    fun updateCardNetWorthTimelineRange(cardId: String, range: TimelineRange) {}
    
    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {}
    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {}
    fun updateCardCurrencyBalancesTimelineRange(cardId: String, range: TimelineRange) {}
    
    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {}
    fun updateCardCashFlowTimelineRange(cardId: String, range: TimelineRange) {}
    
    fun updateCardAccountsLayout(cardId: String, layout: String) {}
    fun updateCardAccountsGridColumns(cardId: String, columns: Int) {}
    fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {}
    fun updateCardAccountsTimelineRange(cardId: String, range: TimelineRange) {}
    
    fun updateCardHeroShowCurrencyBadges(cardId: String, show: Boolean) {}
}
