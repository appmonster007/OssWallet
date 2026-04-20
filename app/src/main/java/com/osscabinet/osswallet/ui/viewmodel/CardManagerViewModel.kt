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

/**
 * Enum representing available dashboard card types.
 * Provides type-safe card type selection and metadata.
 */
enum class DashboardCardType(val displayName: String, val serialName: String) {
    NET_WORTH("Net Worth", "net_worth"),
    CURRENCY_BALANCES("Currency Balances", "currency_balances"),
    CASH_FLOW("Cash Flow", "cash_flow"),
    ACCOUNTS("Accounts", "accounts"),
    BUDGETS("Budgets", "budgets");

    companion object {
        fun fromSerialName(serialName: String): DashboardCardType? {
            return entries.find { it.serialName == serialName }
        }
    }
}

/**
 * Enum representing available account detail card types.
 */
enum class AccountDetailCardType(val displayName: String, val serialName: String) {
    ACCOUNT_HERO("Account Summary", "account_hero"),
    ACCOUNT_CASH_FLOW("Cash Flow", "account_cash_flow"),
    ACCOUNT_CURRENCY_BALANCES("Currency Balances", "account_currency_balances");

    companion object {
        fun fromSerialName(serialName: String): AccountDetailCardType? {
            return entries.find { it.serialName == serialName }
        }
    }
}

class CardManagerViewModel(
    private val settingsDataStore: com.osscabinet.ossWallet.data.local.SettingsDataStore
) : ViewModel() {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Dashboard Cards Configuration
    private val _cardsConfig = MutableStateFlow(DashboardCardsConfig())
    val cardsConfig: StateFlow<DashboardCardsConfig> = _cardsConfig.asStateFlow()

    // Account Detail Cards Configuration
    private val _accountDetailCardsConfig = MutableStateFlow(AccountDetailCardsConfig())
    val accountDetailCardsConfig: StateFlow<AccountDetailCardsConfig> = _accountDetailCardsConfig.asStateFlow()

    // Track which card types are currently in use for the add card dialog
    val availableDashboardCardTypes: StateFlow<List<DashboardCardType>> = cardsConfig.map { config ->
        val usedTypes = config.cards.mapNotNull { card ->
            when (card) {
                is NetWorthCardConfig -> DashboardCardType.NET_WORTH
                is CurrencyBalancesCardConfig -> DashboardCardType.CURRENCY_BALANCES
                is CashFlowCardConfig -> DashboardCardType.CASH_FLOW
                is AccountsCardConfig -> DashboardCardType.ACCOUNTS
                is BudgetsCardConfig -> DashboardCardType.BUDGETS
            }
        }
        DashboardCardType.entries.filter { it !in usedTypes }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardCardType.entries
    )

    val availableAccountDetailCardTypes: StateFlow<List<AccountDetailCardType>> = accountDetailCardsConfig.map { config ->
        val usedTypes = config.cards.mapNotNull { card ->
            when (card) {
                is AccountHeroCardConfig -> AccountDetailCardType.ACCOUNT_HERO
                is AccountCashFlowCardConfig -> AccountDetailCardType.ACCOUNT_CASH_FLOW
                is AccountCurrencyBalancesCardConfig -> AccountDetailCardType.ACCOUNT_CURRENCY_BALANCES
            }
        }
        AccountDetailCardType.entries.filter { it !in usedTypes }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountDetailCardType.entries
    )

    init {
        loadCardsFromStorage()
    }

    private fun loadCardsFromStorage() {
        viewModelScope.launch {
            // Load dashboard cards
            settingsDataStore.dashboardCardsConfig
                .distinctUntilChanged()
                .collect { configJson ->
                    if (configJson.isNotEmpty()) {
                        try {
                            val decoded = json.decodeFromString<DashboardCardsConfig>(configJson)
                            if (decoded.cards.isNotEmpty()) {
                                _cardsConfig.value = decoded
                            } else {
                                initializeDefaultDashboardCards()
                            }
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
            settingsDataStore.accountDetailCardsConfig
                .distinctUntilChanged()
                .collect { configJson ->
                    if (configJson.isNotEmpty()) {
                        try {
                            val decoded = json.decodeFromString<AccountDetailCardsConfig>(configJson)
                            if (decoded.cards.isNotEmpty()) {
                                _accountDetailCardsConfig.value = decoded
                            } else {
                                initializeDefaultAccountDetailCards()
                            }
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
    /**
     * Adds a new card of the specified type to the dashboard.
     * Uses type-safe enum for card type selection.
     */
    fun addDashboardCard(cardType: DashboardCardType) {
        val newCard = when (cardType) {
            DashboardCardType.NET_WORTH -> NetWorthCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size
            )
            DashboardCardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size
            )
            DashboardCardType.CASH_FLOW -> CashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size
            )
            DashboardCardType.ACCOUNTS -> AccountsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size
            )
            DashboardCardType.BUDGETS -> BudgetsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size
            )
        }
        
        _cardsConfig.value = _cardsConfig.value.copy(
            cards = _cardsConfig.value.cards + newCard
        )
        saveDashboardCards()
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use addDashboardCard with DashboardCardType enum instead.
     */
    @Deprecated("Use addDashboardCard with DashboardCardType enum instead")
    fun addCard(cardType: String) {
        DashboardCardType.fromSerialName(cardType)?.let { addDashboardCard(it) }
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
    /**
     * Adds a new card of the specified type to the account detail screen.
     * Uses type-safe enum for card type selection.
     */
    fun addAccountDetailCard(cardType: AccountDetailCardType) {
        val newCard = when (cardType) {
            AccountDetailCardType.ACCOUNT_HERO -> AccountHeroCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountDetailCardsConfig.value.cards.size
            )
            AccountDetailCardType.ACCOUNT_CASH_FLOW -> AccountCashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountDetailCardsConfig.value.cards.size
            )
            AccountDetailCardType.ACCOUNT_CURRENCY_BALANCES -> AccountCurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountDetailCardsConfig.value.cards.size
            )
        }
        
        _accountDetailCardsConfig.value = _accountDetailCardsConfig.value.copy(
            cards = _accountDetailCardsConfig.value.cards + newCard
        )
        saveAccountDetailCards()
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use addAccountDetailCard with AccountDetailCardType enum instead.
     */
    @Deprecated("Use addAccountDetailCard with AccountDetailCardType enum instead")
    fun addAccountDetailCard(cardType: String) {
        AccountDetailCardType.fromSerialName(cardType)?.let { addAccountDetailCard(it) }
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
