package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountDetailCardConfig
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class CardManagerViewModel(
    private val settingsDataStore: com.osscabinet.ossWallet.data.local.SettingsDataStore
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }

    // Dashboard Cards Configuration
    private val _cardsConfig = MutableStateFlow(DashboardCardsConfig())
    val cardsConfig: StateFlow<DashboardCardsConfig> = _cardsConfig.asStateFlow()

    // Account Detail Cards Configuration
    private val _accountDetailCardsConfig = MutableStateFlow(AccountDetailCardsConfig())
    val accountDetailCardsConfig: StateFlow<AccountDetailCardsConfig> = _accountDetailCardsConfig.asStateFlow()

    init {
        loadCardsFromStorage()
    }

    private fun loadCardsFromStorage() {
        viewModelScope.launch {
            // Load dashboard cards
            settingsDataStore.dashboardCardsConfig.collect { configJson ->
                if (configJson.isNotEmpty()) {
                    try {
                        _cardsConfig.value = json.decodeFromString(configJson)
                    } catch (e: Exception) {
                        initializeDefaultDashboardCards()
                    }
                } else {
                    initializeDefaultDashboardCards()
                }
            }
        }

        viewModelScope.launch {
            // Load account detail cards
            settingsDataStore.accountDetailCardsConfig.collect { configJson ->
                if (configJson.isNotEmpty()) {
                    try {
                        _accountDetailCardsConfig.value = json.decodeFromString(configJson)
                    } catch (e: Exception) {
                        initializeDefaultAccountDetailCards()
                    }
                } else {
                    initializeDefaultAccountDetailCards()
                }
            }
        }
    }

    private fun initializeDefaultDashboardCards() {
        val defaultDashboardCards = listOf(
            NetWorthCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 0
            ),
            CurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 1
            ),
            CashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 2
            ),
            AccountsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 3
            ),
            BudgetsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 4
            )
        )
        _cardsConfig.value = DashboardCardsConfig(cards = defaultDashboardCards)
        saveDashboardCards()
    }

    private fun initializeDefaultAccountDetailCards() {
        val defaultAccountDetailCards = listOf(
            AccountHeroCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 0
            ),
            AccountCashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 1
            ),
            AccountCurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 2
            )
        )
        _accountDetailCardsConfig.value = AccountDetailCardsConfig(cards = defaultAccountDetailCards)
        saveAccountDetailCards()
    }

    private fun saveDashboardCards() {
        viewModelScope.launch {
            settingsDataStore.saveDashboardCardsConfig(json.encodeToString(_cardsConfig.value))
        }
    }

    private fun saveAccountDetailCards() {
        viewModelScope.launch {
            settingsDataStore.saveAccountDetailCardsConfig(json.encodeToString(_accountDetailCardsConfig.value))
        }
    }

    // Dashboard Card Management
    fun addCard(cardType: String) {
        val newCard = when (cardType) {
            "net_worth" -> NetWorthCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size
            )
            "currency_balances" -> CurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size
            )
            "cash_flow" -> CashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size
            )
            "accounts" -> AccountsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size
            )
            "budgets" -> BudgetsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size
            )
            else -> return
        }
        
        _cardsConfig.value = _cardsConfig.value.copy(
            cards = _cardsConfig.value.cards + newCard
        )
        saveDashboardCards()
    }

    fun removeCard(cardId: String) {
        _cardsConfig.value = _cardsConfig.value.copy(
            cards = _cardsConfig.value.cards.filter { it.cardId != cardId }
        )
        saveDashboardCards()
    }

    fun updateCard(cardId: String, updateFn: (CardConfig) -> CardConfig) {
        _cardsConfig.value = _cardsConfig.value.copy(
            cards = _cardsConfig.value.cards.map { card ->
                if (card.cardId == cardId) updateFn(card) else card
            }
        )
        saveDashboardCards()
    }

    fun reorderDashboardCards(reorderedCards: List<CardConfig>) {
        _cardsConfig.value = _cardsConfig.value.copy(
            cards = reorderedCards.mapIndexed { index, card ->
                when (card) {
                    is NetWorthCardConfig -> card.copy(order = index)
                    is CurrencyBalancesCardConfig -> card.copy(order = index)
                    is CashFlowCardConfig -> card.copy(order = index)
                    is AccountsCardConfig -> card.copy(order = index)
                    is BudgetsCardConfig -> card.copy(order = index)
                }
            }
        )
        saveDashboardCards()
    }

    // Account Detail Card Management
    fun addAccountDetailCard(cardType: String) {
        val newCard = when (cardType) {
            "account_hero" -> AccountHeroCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountDetailCardsConfig.value.cards.size
            )
            "account_cash_flow" -> AccountCashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountDetailCardsConfig.value.cards.size
            )
            "account_currency_balances" -> AccountCurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountDetailCardsConfig.value.cards.size
            )
            else -> return
        }
        
        _accountDetailCardsConfig.value = _accountDetailCardsConfig.value.copy(
            cards = _accountDetailCardsConfig.value.cards + newCard
        )
        saveAccountDetailCards()
    }

    fun removeAccountDetailCard(cardId: String) {
        _accountDetailCardsConfig.value = _accountDetailCardsConfig.value.copy(
            cards = _accountDetailCardsConfig.value.cards.filter { it.cardId != cardId }
        )
        saveAccountDetailCards()
    }

    fun updateAccountDetailCard(cardId: String, updateFn: (AccountDetailCardConfig) -> AccountDetailCardConfig) {
        _accountDetailCardsConfig.value = _accountDetailCardsConfig.value.copy(
            cards = _accountDetailCardsConfig.value.cards.map { card ->
                if (card.cardId == cardId) updateFn(card) else card
            }
        )
        saveAccountDetailCards()
    }

    fun reorderAccountDetailCards(reorderedCards: List<AccountDetailCardConfig>) {
        _accountDetailCardsConfig.value = _accountDetailCardsConfig.value.copy(
            cards = reorderedCards.mapIndexed { index, card ->
                when (card) {
                    is AccountHeroCardConfig -> card.copy(order = index)
                    is AccountCashFlowCardConfig -> card.copy(order = index)
                    is AccountCurrencyBalancesCardConfig -> card.copy(order = index)
                }
            }
        )
        saveAccountDetailCards()
    }

    // Serialization for persistence
    fun serializeDashboardCards(): String {
        return json.encodeToString(_cardsConfig.value)
    }

    fun deserializeDashboardCards(jsonString: String) {
        try {
            _cardsConfig.value = json.decodeFromString(jsonString)
        } catch (e: Exception) {
            // Keep default configuration if deserialization fails
        }
    }

    fun serializeAccountDetailCards(): String {
        return json.encodeToString(_accountDetailCardsConfig.value)
    }

    fun deserializeAccountDetailCards(jsonString: String) {
        try {
            _accountDetailCardsConfig.value = json.decodeFromString(jsonString)
        } catch (e: Exception) {
            // Keep default configuration if deserialization fails
        }
    }
}
