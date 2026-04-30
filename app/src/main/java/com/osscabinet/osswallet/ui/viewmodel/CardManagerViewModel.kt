package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    val availableDashboardCardTypes: StateFlow<List<UnifiedCardType>> = cardsConfig.map { config ->
        val usedTypes = config.cards.mapNotNull { card ->
            when (card) {
                is NetWorthCardConfig -> UnifiedCardType.DASHBOARD_NET_WORTH
                is CurrencyBalancesCardConfig -> UnifiedCardType.DASHBOARD_CURRENCY_BALANCES
                is CashFlowCardConfig -> UnifiedCardType.DASHBOARD_CASH_FLOW
                is AccountsCardConfig -> UnifiedCardType.DASHBOARD_ACCOUNTS
                is CategoryBreakdownCardConfig -> UnifiedCardType.DASHBOARD_CATEGORY_BREAKDOWN
                is CategoryExpenseTreemapCardConfig -> UnifiedCardType.DASHBOARD_CATEGORY_EXPENSE_TREEMAP
                else -> null
            }
        }.toSet()
        UnifiedCardType.getAvailableForScreen(ScreenContext.DASHBOARD).filter { it !in usedTypes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UnifiedCardType.getAvailableForScreen(ScreenContext.DASHBOARD))

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

    fun addDashboardCard(type: UnifiedCardType) {
        viewModelScope.launch {
            val card = when (type) {
                UnifiedCardType.DASHBOARD_NET_WORTH -> NetWorthCardConfig(cardId = UUID.randomUUID().toString())
                UnifiedCardType.DASHBOARD_CURRENCY_BALANCES -> CurrencyBalancesCardConfig(cardId = UUID.randomUUID().toString())
                UnifiedCardType.DASHBOARD_CASH_FLOW -> CashFlowCardConfig(cardId = UUID.randomUUID().toString())
                UnifiedCardType.DASHBOARD_ACCOUNTS -> AccountsCardConfig(cardId = UUID.randomUUID().toString())
                UnifiedCardType.DASHBOARD_CATEGORY_BREAKDOWN -> CategoryBreakdownCardConfig(cardId = UUID.randomUUID().toString())
                UnifiedCardType.DASHBOARD_CATEGORY_EXPENSE_TREEMAP -> CategoryExpenseTreemapCardConfig(cardId = UUID.randomUUID().toString())
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
    fun addAccountDetailCard(type: UnifiedCardType, accountId: String) {
        viewModelScope.launch {
            val card = when (type) {
                UnifiedCardType.ACCOUNT_DETAIL_HERO -> AccountHeroCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNT_DETAIL
                )
                UnifiedCardType.ACCOUNT_DETAIL_CASH_FLOW -> CashFlowCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNT_DETAIL,
                    filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(accountIds = listOf(accountId))
                )
                UnifiedCardType.ACCOUNT_DETAIL_CURRENCY_BALANCES -> CurrencyBalancesCardConfig(
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
    fun addAccountsViewCard(type: UnifiedCardType) {
        viewModelScope.launch {
            val card = when (type) {
                UnifiedCardType.ACCOUNTS_VIEW_NET_WORTH -> NetWorthCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
                UnifiedCardType.ACCOUNTS_VIEW_CURRENCY_BALANCES -> CurrencyBalancesCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
                UnifiedCardType.ACCOUNTS_VIEW_CASH_FLOW -> CashFlowCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
                UnifiedCardType.ACCOUNTS_VIEW_ACCOUNTS_LIST -> AccountsCardConfig(
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
