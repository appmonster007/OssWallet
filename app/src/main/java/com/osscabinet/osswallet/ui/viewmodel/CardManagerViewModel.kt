package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class CardManagerViewModel(
    private val settingsRepository: SettingsRepository,
    private val cardRepository: CardRepository
) : ViewModel() {

    // Dashboard Cards Configuration
    private val _cardsConfig = MutableStateFlow(DashboardCardsConfig())
    val cardsConfig: StateFlow<DashboardCardsConfig> = _cardsConfig.asStateFlow()

    // Account Detail Cards Configuration
    private val _accountDetailCardsConfig = MutableStateFlow(DashboardCardsConfig())
    val accountDetailCardsConfig: StateFlow<DashboardCardsConfig> = _accountDetailCardsConfig.asStateFlow()

    // Accounts View Cards Configuration
    private val _accountsViewCardsConfig = MutableStateFlow(DashboardCardsConfig())
    val accountsViewCardsConfig: StateFlow<DashboardCardsConfig> = _accountsViewCardsConfig.asStateFlow()

    val availableDashboardCardTypes: StateFlow<List<CardType>> = cardsConfig.map { config ->
        val usedTypes = config.cards.mapNotNull { card ->
            when (card) {
                is NetWorthCardConfig -> CardType.NET_WORTH
                is CurrencyBalancesCardConfig -> CardType.CURRENCY_BALANCES
                is CashFlowCardConfig -> CardType.CASH_FLOW
                is AccountsCardConfig -> CardType.ACCOUNTS
                is CategoryBreakdownCardConfig -> CardType.CATEGORY_BREAKDOWN
                is CategoryExpenseTreemapCardConfig -> CardType.CATEGORY_EXPENSE_TREEMAP
                else -> null
            }
        }.toSet()
        // For now, allow all on dashboard if not used. 
        // In a real app, we might want screen-specific filtering logic in CardType or a separate service.
        CardType.entries.filter { it !in usedTypes && it != CardType.ACCOUNT_HERO && it != CardType.BUDGETS }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CardType.entries.filter { it != CardType.ACCOUNT_HERO })

    init {
        viewModelScope.launch {
            cardRepository.dashboardCardsConfig.collect { config ->
                _cardsConfig.value = config
            }
        }
        
        viewModelScope.launch {
            cardRepository.accountDetailCardsConfig.collect { config ->
                _accountDetailCardsConfig.value = config
            }
        }
        
        viewModelScope.launch {
            cardRepository.accountsViewCardsConfig.collect { config ->
                _accountsViewCardsConfig.value = config
            }
        }
    }

    fun addDashboardCard(type: CardType) {
        viewModelScope.launch {
            val card = when (type) {
                CardType.NET_WORTH -> NetWorthCardConfig(cardId = UUID.randomUUID().toString())
                CardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(cardId = UUID.randomUUID().toString())
                CardType.CASH_FLOW -> CashFlowCardConfig(cardId = UUID.randomUUID().toString())
                CardType.ACCOUNTS -> AccountsCardConfig(cardId = UUID.randomUUID().toString())
                CardType.CATEGORY_BREAKDOWN -> CategoryBreakdownCardConfig(cardId = UUID.randomUUID().toString())
                CardType.CATEGORY_EXPENSE_TREEMAP -> CategoryExpenseTreemapCardConfig(cardId = UUID.randomUUID().toString())
                else -> throw IllegalArgumentException("Unsupported dashboard card type: $type")
            }
            cardRepository.addDashboardCard(card)
        }
    }

    fun removeCard(cardId: String) {
        viewModelScope.launch {
            cardRepository.removeDashboardCard(cardId)
        }
    }

    fun updateCard(cardId: String, update: (CardConfig) -> CardConfig) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId, update)
        }
    }

    fun reorderDashboardCards(cards: List<CardConfig>) {
        viewModelScope.launch {
            cardRepository.reorderDashboardCards(cards)
        }
    }

    // Account Detail Cards Management
    fun addAccountDetailCard(type: CardType, accountId: String) {
        viewModelScope.launch {
            val card = when (type) {
                CardType.ACCOUNT_HERO -> AccountHeroCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNT_DETAIL
                )
                CardType.CASH_FLOW -> CashFlowCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNT_DETAIL,
                    filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(accountIds = listOf(accountId))
                )
                CardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNT_DETAIL,
                    filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(accountIds = listOf(accountId))
                )
                else -> throw IllegalArgumentException("Unsupported account detail card type: $type")
            }
            cardRepository.addAccountDetailCard(card)
        }
    }

    fun removeAccountDetailCard(cardId: String) {
        viewModelScope.launch {
            cardRepository.removeAccountDetailCard(cardId)
        }
    }

    fun updateAccountDetailCard(cardId: String, update: (CardConfig) -> CardConfig) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId, update)
        }
    }

    fun reorderAccountDetailCards(cards: List<CardConfig>) {
        viewModelScope.launch {
            cardRepository.reorderAccountDetailCards(cards)
        }
    }

    // Accounts View Cards Management
    fun addAccountsViewCard(type: CardType) {
        viewModelScope.launch {
            val card = when (type) {
                CardType.NET_WORTH -> NetWorthCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
                CardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
                CardType.CASH_FLOW -> CashFlowCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
                CardType.ACCOUNTS -> AccountsCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
                else -> throw IllegalArgumentException("Unsupported accounts view card type: $type")
            }
            cardRepository.addAccountsViewCard(card)
        }
    }

    fun removeAccountsViewCard(cardId: String) {
        viewModelScope.launch {
            cardRepository.removeAccountsViewCard(cardId)
        }
    }

    fun updateAccountsViewCard(cardId: String, update: (CardConfig) -> CardConfig) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId, update)
        }
    }

    fun reorderAccountsViewCards(cards: List<CardConfig>) {
        viewModelScope.launch {
            cardRepository.reorderAccountsViewCards(cards)
        }
    }
}
