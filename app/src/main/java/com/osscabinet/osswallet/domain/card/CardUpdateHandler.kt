package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.repository.CardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Robust mix-in interface for handling card configuration updates.
 * ViewModels implement this to provide card update functionality with zero boilerplate
 * by leveraging default interface methods.
 */
interface CardUpdateHandler {
    val cardUpdateScope: CoroutineScope
    val cardRepository: CardRepository
    val cardScreenContext: ScreenContext

    // Net Worth
    fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is NetWorthCardConfig) config.copy(fontSize = fontSize) else config
            }
        }
    }
    
    fun updateCardNetWorthFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is NetWorthCardConfig) config.copy(filter = filter) else config
            }
        }
    }
    
    // Currency Balances
    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(layout = layout) else config
            }
        }
    }
    
    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(showBaseConversion = show) else config
            }
        }
    }
    
    fun updateCardCurrencyBalancesFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(filter = filter) else config
            }
        }
    }
    
    // Cash Flow
    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CashFlowCardConfig) config.copy(showBaseConversion = show) else config
            }
        }
    }
    
    fun updateCardCashFlowFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is CashFlowCardConfig) config.copy(filter = filter) else config
            }
        }
    }
    
    // Accounts
    fun updateCardAccountsLayout(cardId: String, layout: String) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountsCardConfig) config.copy(layout = layout) else config
            }
        }
    }
    
    fun updateCardAccountsGridColumns(cardId: String, columns: Int) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountsCardConfig) config.copy(gridColumns = columns) else config
            }
        }
    }
    
    fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountsCardConfig) config.copy(showAddAccountCard = show) else config
            }
        }
    }
    
    fun updateCardAccountsFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountsCardConfig) config.copy(filter = filter) else config
            }
        }
    }
    
    // Account Hero
    fun updateCardHeroShowCurrencyBadges(cardId: String, show: Boolean) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountHeroCardConfig) config.copy(showCurrencyBadges = show) else config
            }
        }
    }
    
    fun updateCardHeroFilter(cardId: String, filter: TransactionFilter) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config ->
                if (config is AccountHeroCardConfig) config.copy(filter = filter) else config
            }
        }
    }

    // Analytics
    fun updateCardCategoryPieChartConfig(cardId: String, config: CategoryPieChartCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }
    
    fun updateCardCategoryTreemapConfig(cardId: String, config: CategoryTreemapCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    // New Cards
    fun updateCardPlannedPaymentsConfig(cardId: String, config: PlannedPaymentsCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    fun updateCardBudgetsConfig(cardId: String, config: BudgetsCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    fun updateCardLoansConfig(cardId: String, config: LoansCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    fun updateCardRecentActivitiesConfig(cardId: String, config: RecentActivitiesCardConfig) {
        cardUpdateScope.launch {
            cardRepository.updateCard(cardScreenContext, cardId) { config }
        }
    }

    // Structural Updates
    fun addCard(type: CardType) {
        cardUpdateScope.launch {
            val config = CardConfig.createDefault(type, cardScreenContext)
            cardRepository.addCard(cardScreenContext, config)
        }
    }

    fun addCard(config: CardConfig) {
        cardUpdateScope.launch {
            cardRepository.addCard(cardScreenContext, config)
        }
    }

    fun removeCard(cardId: String) {
        cardUpdateScope.launch {
            cardRepository.removeCard(cardScreenContext, cardId)
        }
    }

    fun reorderCards(newOrder: List<CardConfig>) {
        cardUpdateScope.launch {
            cardRepository.reorderCards(cardScreenContext, newOrder)
        }
    }
}
