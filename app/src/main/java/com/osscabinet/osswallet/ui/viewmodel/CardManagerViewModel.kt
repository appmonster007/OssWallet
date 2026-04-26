package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
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
    BUDGETS("Budgets", "budgets"),
    CATEGORY_BREAKDOWN("Category Breakdown", "category_breakdown"),
    TIMELINE_TRAJECTORY("Timeline Trajectory", "timeline_trajectory"),
    CATEGORY_EXPENSE_TREEMAP("Category Expenses", "category_expense_treemap");

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
    CASH_FLOW("Cash Flow", "cash_flow"),
    CURRENCY_BALANCES("Currency Balances", "currency_balances");

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
    ACCOUNTS_LIST("Accounts List", "accounts");

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

    // Account Detail Cards Configuration - using shared CardConfig
    private val _accountDetailCardsConfig = MutableStateFlow(DashboardCardsConfig())
    val accountDetailCardsConfig: StateFlow<DashboardCardsConfig> = _accountDetailCardsConfig.asStateFlow()

    // Accounts View Cards Configuration - using shared CardConfig
    private val _accountsViewCardsConfig = MutableStateFlow(DashboardCardsConfig())
    val accountsViewCardsConfig: StateFlow<DashboardCardsConfig> = _accountsViewCardsConfig.asStateFlow()

    // Track which card types are currently in use for the add card dialog
    val availableDashboardCardTypes: StateFlow<List<DashboardCardType>> = cardsConfig.map { config ->
        val usedTypes = config.cards.mapNotNull { card ->
            when (card) {
                is NetWorthCardConfig -> DashboardCardType.NET_WORTH
                is CurrencyBalancesCardConfig -> DashboardCardType.CURRENCY_BALANCES
                is CashFlowCardConfig -> DashboardCardType.CASH_FLOW
                is AccountsCardConfig -> DashboardCardType.ACCOUNTS
                is BudgetsCardConfig -> DashboardCardType.BUDGETS
                is CategoryBreakdownCardConfig -> DashboardCardType.CATEGORY_BREAKDOWN
                is TimelineTrajectoryCardConfig -> DashboardCardType.TIMELINE_TRAJECTORY
                is CategoryExpenseTreemapCardConfig -> DashboardCardType.CATEGORY_EXPENSE_TREEMAP
                is IncomeExpensesSummaryCardConfig -> null // Analytics-only card
                is PeriodComparisonCardConfig -> null // Analytics-only card
                is CashFlowTrendCardConfig -> null // Analytics-only card
                is CashFlowProgressCardConfig -> null // Analytics-only card
                is BalanceForecastCardConfig -> null // Analytics-only card
                is PlannedPaymentsCardConfig -> null // Analytics-only card
                is AccountHeroCardConfig -> null // Account detail-only card
                is IncomeExpensesBookCardConfig -> null // Analytics-only card
                is CashFlowTableCardConfig -> null // Analytics-only card
                is PlannedPaymentsTimelineCardConfig -> null // Analytics-only card
                is BalanceByAccountsCardConfig -> null // Analytics-only card
                is BalanceTrendCardConfig -> null // Analytics-only card
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
                is CashFlowCardConfig -> AccountDetailCardType.CASH_FLOW
                is CurrencyBalancesCardConfig -> AccountDetailCardType.CURRENCY_BALANCES
                else -> null
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
                is NetWorthCardConfig -> AccountsViewCardType.NET_WORTH
                is CurrencyBalancesCardConfig -> AccountsViewCardType.CURRENCY_BALANCES
                is CashFlowCardConfig -> AccountsViewCardType.CASH_FLOW
                is AccountsCardConfig -> AccountsViewCardType.ACCOUNTS_LIST
                else -> null
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
                                // Migration: Add screenContext to cards that don't have it
                                val migratedCards = decoded.cards.map { card ->
                                    migrateScreenContext(card, ScreenContext.DASHBOARD)
                                }
                                _cardsConfig.value = DashboardCardsConfig(cards = migratedCards)
                                // Only save if migration actually changed something
                                if (needsMigration(decoded.cards, migratedCards)) {
                                    saveDashboardCards()
                                }
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
                            val decoded = json.decodeFromString<DashboardCardsConfig>(configJson)
                            if (decoded.cards.isNotEmpty()) {
                                // Migration: Add screenContext to cards that don't have it
                                val migratedCards = decoded.cards.map { card ->
                                    migrateScreenContext(card, ScreenContext.ACCOUNT_DETAIL)
                                }
                                _accountDetailCardsConfig.value = DashboardCardsConfig(cards = migratedCards)
                                // Only save if migration actually changed something
                                if (needsMigration(decoded.cards, migratedCards)) {
                                    saveAccountDetailCards()
                                }
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
                            val decoded = json.decodeFromString<DashboardCardsConfig>(configJson)
                            if (decoded.cards.isNotEmpty()) {
                                // Migration: Add screenContext to cards that don't have it
                                val migratedCards = decoded.cards.map { card ->
                                    migrateScreenContext(card, ScreenContext.ACCOUNTS_VIEW)
                                }
                                _accountsViewCardsConfig.value = DashboardCardsConfig(cards = migratedCards)
                                // Only save if migration actually changed something
                                if (needsMigration(decoded.cards, migratedCards)) {
                                    saveAccountsViewCards()
                                }
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

    /**
     * Checks if any card needs migration (missing screenContext).
     * Returns true if migration actually changed any card.
     */
    private fun needsMigration(original: List<CardConfig>, migrated: List<CardConfig>): Boolean {
        return original.zip(migrated).any { (orig, migr) ->
            // Check if screenContext was added during migration
            // We compare the original card's screenContext with the migrated one
            // If they differ, migration occurred
            when (orig) {
                is NetWorthCardConfig -> orig.screenContext != (migr as NetWorthCardConfig).screenContext
                is CurrencyBalancesCardConfig -> orig.screenContext != (migr as CurrencyBalancesCardConfig).screenContext
                is CashFlowCardConfig -> orig.screenContext != (migr as CashFlowCardConfig).screenContext
                is AccountsCardConfig -> orig.screenContext != (migr as AccountsCardConfig).screenContext
                is BudgetsCardConfig -> orig.screenContext != (migr as BudgetsCardConfig).screenContext
                is CategoryBreakdownCardConfig -> orig.screenContext != (migr as CategoryBreakdownCardConfig).screenContext
                is TimelineTrajectoryCardConfig -> orig.screenContext != (migr as TimelineTrajectoryCardConfig).screenContext
                is CategoryExpenseTreemapCardConfig -> orig.screenContext != (migr as CategoryExpenseTreemapCardConfig).screenContext
                is IncomeExpensesSummaryCardConfig -> orig.screenContext != (migr as IncomeExpensesSummaryCardConfig).screenContext
                is PeriodComparisonCardConfig -> orig.screenContext != (migr as PeriodComparisonCardConfig).screenContext
                is CashFlowTrendCardConfig -> orig.screenContext != (migr as CashFlowTrendCardConfig).screenContext
                is CashFlowProgressCardConfig -> orig.screenContext != (migr as CashFlowProgressCardConfig).screenContext
                is BalanceForecastCardConfig -> orig.screenContext != (migr as BalanceForecastCardConfig).screenContext
                is PlannedPaymentsCardConfig -> orig.screenContext != (migr as PlannedPaymentsCardConfig).screenContext
                is AccountHeroCardConfig -> orig.screenContext != (migr as AccountHeroCardConfig).screenContext
                is IncomeExpensesBookCardConfig -> orig.screenContext != (migr as IncomeExpensesBookCardConfig).screenContext
                is CashFlowTableCardConfig -> orig.screenContext != (migr as CashFlowTableCardConfig).screenContext
                is PlannedPaymentsTimelineCardConfig -> orig.screenContext != (migr as PlannedPaymentsTimelineCardConfig).screenContext
                is BalanceByAccountsCardConfig -> orig.screenContext != (migr as BalanceByAccountsCardConfig).screenContext
                is BalanceTrendCardConfig -> orig.screenContext != (migr as BalanceTrendCardConfig).screenContext
            }
        }
    }

    /**
     * Migrates card configs to include screenContext if missing.
     * This handles backward compatibility for existing saved configs.
     * Each card is treated independently, so multiple instances of the same card type
     * on the same screen will each get their own screenContext.
     */
    private fun migrateScreenContext(card: CardConfig, defaultContext: ScreenContext): CardConfig {
        return when (card) {
            is NetWorthCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is CurrencyBalancesCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is CashFlowCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is AccountsCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is BudgetsCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is CategoryBreakdownCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is TimelineTrajectoryCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is CategoryExpenseTreemapCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is IncomeExpensesSummaryCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is PeriodComparisonCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is CashFlowTrendCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is CashFlowProgressCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is BalanceForecastCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is PlannedPaymentsCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is AccountHeroCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is IncomeExpensesBookCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is CashFlowTableCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is PlannedPaymentsTimelineCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is BalanceByAccountsCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
            is BalanceTrendCardConfig -> card.copy(screenContext = card.screenContext ?: defaultContext)
        }
    }

    private fun initializeDefaultDashboardCards() {
        val defaultDashboardCards = listOf(
            NetWorthCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 0,
                screenContext = ScreenContext.DASHBOARD
            ),
            CurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 1,
                screenContext = ScreenContext.DASHBOARD
            ),
            CashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 2,
                screenContext = ScreenContext.DASHBOARD
            ),
            AccountsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 3,
                screenContext = ScreenContext.DASHBOARD
            ),
            BudgetsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 4,
                screenContext = ScreenContext.DASHBOARD
            )
        )
        _cardsConfig.value = DashboardCardsConfig(cards = defaultDashboardCards)
        saveDashboardCards()
    }

    private fun initializeDefaultAccountDetailCards() {
        val defaultAccountDetailCards = listOf(
            AccountHeroCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 0,
                screenContext = ScreenContext.ACCOUNT_DETAIL
            ),
            CashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 1,
                screenContext = ScreenContext.ACCOUNT_DETAIL,
                filter = com.osscabinet.ossWallet.domain.model.TransactionFilter()
            ),
            CurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 2,
                screenContext = ScreenContext.ACCOUNT_DETAIL,
                filter = com.osscabinet.ossWallet.domain.model.TransactionFilter()
            )
        )
        _accountDetailCardsConfig.value = DashboardCardsConfig(cards = defaultAccountDetailCards)
        saveAccountDetailCards()
    }

    private fun initializeDefaultAccountsViewCards() {
        val defaultAccountsViewCards = listOf(
            NetWorthCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 0,
                screenContext = ScreenContext.ACCOUNTS_VIEW,
                selectedAccountIds = emptyList() // Will be set by screen
            ),
            CurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 1,
                screenContext = ScreenContext.ACCOUNTS_VIEW
            ),
            CashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 2,
                screenContext = ScreenContext.ACCOUNTS_VIEW
            ),
            AccountsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = 3,
                screenContext = ScreenContext.ACCOUNTS_VIEW
            )
        )
        _accountsViewCardsConfig.value = DashboardCardsConfig(cards = defaultAccountsViewCards)
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
                order = _cardsConfig.value.cards.size,
                screenContext = ScreenContext.DASHBOARD
            )
            DashboardCardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size,
                screenContext = ScreenContext.DASHBOARD
            )
            DashboardCardType.CASH_FLOW -> CashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size,
                screenContext = ScreenContext.DASHBOARD
            )
            DashboardCardType.ACCOUNTS -> AccountsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size,
                screenContext = ScreenContext.DASHBOARD
            )
            DashboardCardType.BUDGETS -> BudgetsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size,
                screenContext = ScreenContext.DASHBOARD
            )
            DashboardCardType.CATEGORY_BREAKDOWN -> CategoryBreakdownCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size,
                screenContext = ScreenContext.ANALYTICS
            )
            DashboardCardType.TIMELINE_TRAJECTORY -> TimelineTrajectoryCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size,
                screenContext = ScreenContext.ANALYTICS
            )
            DashboardCardType.CATEGORY_EXPENSE_TREEMAP -> CategoryExpenseTreemapCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _cardsConfig.value.cards.size,
                screenContext = ScreenContext.ANALYTICS
            )
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
                    is NetWorthCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CurrencyBalancesCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CashFlowCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is AccountsCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is BudgetsCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CategoryBreakdownCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is TimelineTrajectoryCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CategoryExpenseTreemapCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is IncomeExpensesSummaryCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is PeriodComparisonCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CashFlowTrendCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CashFlowProgressCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is BalanceForecastCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is PlannedPaymentsCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is AccountHeroCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is IncomeExpensesBookCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CashFlowTableCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is PlannedPaymentsTimelineCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is BalanceByAccountsCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is BalanceTrendCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                }
            }
        )
        saveDashboardCards()
    }

    // Account Detail Card Management
    /**
     * Adds a new card of the specified type to the account detail screen.
     * Uses type-safe enum for card type selection.
     * Uses shared CardConfig classes instead of screen-specific ones.
     * Sets accountId for cards that support account filtering.
     */
    fun addAccountDetailCard(cardType: AccountDetailCardType, accountId: String?) {
        val newCard = when (cardType) {
            AccountDetailCardType.ACCOUNT_HERO -> AccountHeroCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountDetailCardsConfig.value.cards.size,
                screenContext = ScreenContext.ACCOUNT_DETAIL
            )
            AccountDetailCardType.CASH_FLOW -> CashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountDetailCardsConfig.value.cards.size,
                screenContext = ScreenContext.ACCOUNT_DETAIL,
                filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(accountIds = listOfNotNull(accountId))
            )
            AccountDetailCardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountDetailCardsConfig.value.cards.size,
                screenContext = ScreenContext.ACCOUNT_DETAIL,
                filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(accountIds = listOfNotNull(accountId))
            )
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

    fun updateAccountDetailCard(cardId: String, updateFn: (CardConfig) -> CardConfig) {
        _accountDetailCardsConfig.value = _accountDetailCardsConfig.value.copy(
            cards = _accountDetailCardsConfig.value.cards.map { card ->
                if (card.cardId == cardId) updateFn(card) else card
            }
        )
        saveAccountDetailCards()
    }

    fun reorderAccountDetailCards(reorderedCards: List<CardConfig>) {
        _accountDetailCardsConfig.value = _accountDetailCardsConfig.value.copy(
            cards = reorderedCards.mapIndexed { index, card ->
                when (card) {
                    is AccountHeroCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CashFlowCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CurrencyBalancesCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    else -> card
                }
            }
        )
        saveAccountDetailCards()
    }

    // Accounts View Card Management
    /**
     * Adds a new card of the specified type to the accounts view screen.
     * Uses type-safe enum for card type selection.
     * Uses shared CardConfig classes instead of screen-specific ones.
     */
    fun addAccountsViewCard(cardType: AccountsViewCardType) {
        val newCard = when (cardType) {
            AccountsViewCardType.NET_WORTH -> NetWorthCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountsViewCardsConfig.value.cards.size,
                screenContext = ScreenContext.ACCOUNTS_VIEW,
                selectedAccountIds = emptyList() // Will be set by screen
            )
            AccountsViewCardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountsViewCardsConfig.value.cards.size,
                screenContext = ScreenContext.ACCOUNTS_VIEW
            )
            AccountsViewCardType.CASH_FLOW -> CashFlowCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountsViewCardsConfig.value.cards.size,
                screenContext = ScreenContext.ACCOUNTS_VIEW
            )
            AccountsViewCardType.ACCOUNTS_LIST -> AccountsCardConfig(
                cardId = UUID.randomUUID().toString(),
                order = _accountsViewCardsConfig.value.cards.size,
                screenContext = ScreenContext.ACCOUNTS_VIEW
            )
        }

        _accountsViewCardsConfig.value = _accountsViewCardsConfig.value.copy(
            cards = _accountsViewCardsConfig.value.cards + newCard
        )
        saveAccountsViewCards()
    }

    fun removeAccountsViewCard(cardId: String) {
        _accountsViewCardsConfig.value = _accountsViewCardsConfig.value.copy(
            cards = _accountsViewCardsConfig.value.cards.filter { it.cardId != cardId }
        )
        saveAccountsViewCards()
    }

    fun updateAccountsViewCard(cardId: String, updateFn: (CardConfig) -> CardConfig) {
        _accountsViewCardsConfig.value = _accountsViewCardsConfig.value.copy(
            cards = _accountsViewCardsConfig.value.cards.map { card ->
                if (card.cardId == cardId) updateFn(card) else card
            }
        )
        saveAccountsViewCards()
    }

    fun reorderAccountsViewCards(reorderedCards: List<CardConfig>) {
        _accountsViewCardsConfig.value = _accountsViewCardsConfig.value.copy(
            cards = reorderedCards.mapIndexed { index, card ->
                when (card) {
                    is NetWorthCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CurrencyBalancesCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is CashFlowCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    is AccountsCardConfig -> card.copy(order = index, screenContext = card.screenContext)
                    else -> card
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
