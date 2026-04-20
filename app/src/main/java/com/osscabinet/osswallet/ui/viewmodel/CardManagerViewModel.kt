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

/**
 * Enum representing available accounts view card types.
 */
enum class AccountsViewCardType(val displayName: String, val serialName: String) {
    NET_WORTH("Net Worth", "net_worth"),
    CURRENCY_BALANCES("Currency Balances", "currency_balances"),
    CASH_FLOW("Cash Flow", "cash_flow"),
    ACCOUNTS_LIST("Accounts List", "accounts_list");

    companion object {
        fun fromSerialName(serialName: String): AccountsViewCardType? {
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

    // Accounts View Cards Configuration
    private val _accountsViewCardsConfig = MutableStateFlow(AccountsViewCardsConfig())
    val accountsViewCardsConfig: StateFlow<AccountsViewCardsConfig> = _accountsViewCardsConfig.asStateFlow()

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

    val availableAccountsViewCardTypes: StateFlow<List<AccountsViewCardType>> = accountsViewCardsConfig.map { config ->
        val usedTypes = config.cards.mapNotNull { card ->
            when (card) {
                is AccountsViewNetWorthCardConfig -> AccountsViewCardType.NET_WORTH
                is AccountsViewCurrencyBalancesCardConfig -> AccountsViewCardType.CURRENCY_BALANCES
                is AccountsViewCashFlowCardConfig -> AccountsViewCardType.CASH_FLOW
                is AccountsViewAccountsListCardConfig -> AccountsViewCardType.ACCOUNTS_LIST
            }
        }
        AccountsViewCardType.entries.filter { it !in usedTypes }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsViewCardType.entries
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

        viewModelScope.launch {
            // Load accounts view cards
            settingsDataStore.accountsViewCardsConfig
                .distinctUntilChanged()
                .collect { configJson ->
                    if (configJson.isNotEmpty()) {
                        try {
                            val decoded = json.decodeFromString<AccountsViewCardsConfig>(configJson)
                            if (decoded.cards.isNotEmpty()) {
                                _accountsViewCardsConfig.value = decoded
                            } else {
                                initializeDefaultAccountsViewCards()
                            }
                        } catch (e: Exception) {
                            initializeDefaultAccountsViewCards()
                        }
                    } else {
                        initializeDefaultAccountsViewCards()
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

    private fun initializeDefaultAccountsViewCards() {
        val defaultAccountsViewCards = listOf(
            AccountsViewNetWorthCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 0
            ),
            AccountsViewCurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 1
            ),
            AccountsViewCashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 2
            ),
            AccountsViewAccountsListCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 3
            )
        )
        _accountsViewCardsConfig.value = AccountsViewCardsConfig(cards = defaultAccountsViewCards)
        saveAccountsViewCards()
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

    private fun saveAccountsViewCards() {
        viewModelScope.launch {
            settingsDataStore.saveAccountsViewCardsConfig(json.encodeToString(_accountsViewCardsConfig.value))
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

    // Accounts View Card Management
    /**
     * Adds a new card of the specified type to the accounts view screen.
     * Uses type-safe enum for card type selection.
     */
    fun addAccountsViewCard(cardType: AccountsViewCardType) {
        val newCard = when (cardType) {
            AccountsViewCardType.NET_WORTH -> AccountsViewNetWorthCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountsViewCardsConfig.value.cards.size
            )
            AccountsViewCardType.CURRENCY_BALANCES -> AccountsViewCurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountsViewCardsConfig.value.cards.size
            )
            AccountsViewCardType.CASH_FLOW -> AccountsViewCashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountsViewCardsConfig.value.cards.size
            )
            AccountsViewCardType.ACCOUNTS_LIST -> AccountsViewAccountsListCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountsViewCardsConfig.value.cards.size
            )
        }

        _accountsViewCardsConfig.value = _accountsViewCardsConfig.value.copy(
            cards = _accountsViewCardsConfig.value.cards + newCard
        )
        saveAccountsViewCards()
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use addAccountsViewCard with AccountsViewCardType enum instead.
     */
    @Deprecated("Use addAccountsViewCard with AccountsViewCardType enum instead")
    fun addAccountsViewCard(cardType: String) {
        AccountsViewCardType.fromSerialName(cardType)?.let { addAccountsViewCard(it) }
    }

    fun removeAccountsViewCard(cardId: String) {
        _accountsViewCardsConfig.value = _accountsViewCardsConfig.value.copy(
            cards = _accountsViewCardsConfig.value.cards.filter { it.cardId != cardId }
        )
        saveAccountsViewCards()
    }

    fun updateAccountsViewCard(cardId: String, updateFn: (AccountsViewCardConfig) -> AccountsViewCardConfig) {
        _accountsViewCardsConfig.value = _accountsViewCardsConfig.value.copy(
            cards = _accountsViewCardsConfig.value.cards.map { card ->
                if (card.cardId == cardId) updateFn(card) else card
            }
        )
        saveAccountsViewCards()
    }

    fun reorderAccountsViewCards(reorderedCards: List<AccountsViewCardConfig>) {
        _accountsViewCardsConfig.value = _accountsViewCardsConfig.value.copy(
            cards = reorderedCards.mapIndexed { index, card ->
                when (card) {
                    is AccountsViewNetWorthCardConfig -> card.copy(order = index)
                    is AccountsViewCurrencyBalancesCardConfig -> card.copy(order = index)
                    is AccountsViewCashFlowCardConfig -> card.copy(order = index)
                    is AccountsViewAccountsListCardConfig -> card.copy(order = index)
                }
            }
        )
        saveAccountsViewCards()
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

    // Generic Card Management Helpers
    /**
     * Generic function to remove a card from any card list.
     * This can be used by screens to provide a unified delete experience.
     */
    fun <T : com.osscabinet.ossWallet.domain.model.settings.BaseCardConfig> removeCardGeneric(
        cards: List<T>,
        cardId: String
    ): List<T> {
        return cards.filter { it.cardId != cardId }
    }

    /**
     * Generic function to reorder cards.
     * Updates the order field based on the new position.
     */
    fun <T : com.osscabinet.ossWallet.domain.model.settings.BaseCardConfig> reorderCardsGeneric(
        cards: List<T>,
        onOrderUpdate: (T, Int) -> T
    ): List<T> {
        return cards.mapIndexed { index, card -> onOrderUpdate(card, index) }
    }

    /**
     * Generic function to update a card in a list.
     */
    fun <T : com.osscabinet.ossWallet.domain.model.settings.BaseCardConfig> updateCardGeneric(
        cards: List<T>,
        cardId: String,
        updateFn: (T) -> T
    ): List<T> {
        return cards.map { card ->
            if (card.cardId == cardId) updateFn(card) else card
        }
    }
}
