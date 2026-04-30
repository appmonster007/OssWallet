package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.TransactionFilter

/**
 * Interface for handling card configuration updates.
 * Implemented by ViewModels to provide a unified way for UI to trigger updates.
 */
interface CardUpdateHandler {
    // Net Worth
    fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {}
    fun updateCardNetWorthFilter(cardId: String, filter: TransactionFilter) {}
    
    // Currency Balances
    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {}
    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {}
    fun updateCardCurrencyBalancesFilter(cardId: String, filter: TransactionFilter) {}
    
    // Cash Flow
    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {}
    fun updateCardCashFlowFilter(cardId: String, filter: TransactionFilter) {}
    
    // Accounts
    fun updateCardAccountsLayout(cardId: String, layout: String) {}
    fun updateCardAccountsGridColumns(cardId: String, columns: Int) {}
    fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {}
    fun updateCardAccountsFilter(cardId: String, filter: TransactionFilter) {}
    
    // Account Hero
    fun updateCardHeroShowCurrencyBadges(cardId: String, show: Boolean) {}
    fun updateCardHeroFilter(cardId: String, filter: TransactionFilter) {}

    // Analytics
    fun updateCardCategoryBreakdownConfig(cardId: String, config: com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig) {}
    fun updateCardCategoryExpenseTreemapConfig(cardId: String, config: com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig) {}
}
