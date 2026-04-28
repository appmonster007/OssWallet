package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    LOANS("Loans", "loans"),
    CATEGORY_BREAKDOWN("Category Breakdown", "category_breakdown"),
    TIMELINE_TRAJECTORY("Timeline Trajectory", "timeline_trajectory"),
    CATEGORY_EXPENSE_TREEMAP("Category Treemap", "category_expense_treemap");

    companion object {
        fun fromSerialName(name: String): DashboardCardType? = 
            entries.find { it.serialName == name }
    }
}

enum class AccountsViewCardType {
    NET_WORTH,
    CURRENCY_BALANCES,
    CASH_FLOW,
    ACCOUNTS_LIST
}

enum class AccountDetailCardType {
    ACCOUNT_HERO,
    CASH_FLOW,
    CURRENCY_BALANCES
}

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

    val availableDashboardCardTypes: StateFlow<List<DashboardCardType>> = cardsConfig.map { config ->
        val usedTypes = config.cards.mapNotNull { card ->
            when (card) {
                is NetWorthCardConfig -> DashboardCardType.NET_WORTH
                is CurrencyBalancesCardConfig -> DashboardCardType.CURRENCY_BALANCES
                is CashFlowCardConfig -> DashboardCardType.CASH_FLOW
                is AccountsCardConfig -> DashboardCardType.ACCOUNTS
                is BudgetsCardConfig -> DashboardCardType.BUDGETS
                is LoansCardConfig -> DashboardCardType.LOANS
                is CategoryBreakdownCardConfig -> DashboardCardType.CATEGORY_BREAKDOWN
                is TimelineTrajectoryCardConfig -> DashboardCardType.TIMELINE_TRAJECTORY
                is CategoryExpenseTreemapCardConfig -> DashboardCardType.CATEGORY_EXPENSE_TREEMAP
                else -> null
            }
        }.toSet()
        DashboardCardType.entries.filter { it !in usedTypes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCardType.entries)

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

    fun addDashboardCard(type: DashboardCardType) {
        viewModelScope.launch {
            val card = when (type) {
                DashboardCardType.NET_WORTH -> NetWorthCardConfig(cardId = UUID.randomUUID().toString())
                DashboardCardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(cardId = UUID.randomUUID().toString())
                DashboardCardType.CASH_FLOW -> CashFlowCardConfig(cardId = UUID.randomUUID().toString())
                DashboardCardType.ACCOUNTS -> AccountsCardConfig(cardId = UUID.randomUUID().toString())
                DashboardCardType.BUDGETS -> BudgetsCardConfig(cardId = UUID.randomUUID().toString())
                DashboardCardType.LOANS -> LoansCardConfig(cardId = UUID.randomUUID().toString())
                DashboardCardType.CATEGORY_BREAKDOWN -> CategoryBreakdownCardConfig(cardId = UUID.randomUUID().toString())
                DashboardCardType.TIMELINE_TRAJECTORY -> TimelineTrajectoryCardConfig(cardId = UUID.randomUUID().toString())
                DashboardCardType.CATEGORY_EXPENSE_TREEMAP -> CategoryExpenseTreemapCardConfig(cardId = UUID.randomUUID().toString())
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
    fun addAccountDetailCard(type: AccountDetailCardType, accountId: String) {
        viewModelScope.launch {
            val card = when (type) {
                AccountDetailCardType.ACCOUNT_HERO -> AccountHeroCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNT_DETAIL
                )
                AccountDetailCardType.CASH_FLOW -> CashFlowCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNT_DETAIL,
                    filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(accountIds = listOf(accountId))
                )
                AccountDetailCardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNT_DETAIL,
                    filter = com.osscabinet.ossWallet.domain.model.TransactionFilter(accountIds = listOf(accountId))
                )
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
    fun addAccountsViewCard(type: AccountsViewCardType) {
        viewModelScope.launch {
            val card = when (type) {
                AccountsViewCardType.NET_WORTH -> NetWorthCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
                AccountsViewCardType.CURRENCY_BALANCES -> CurrencyBalancesCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
                AccountsViewCardType.CASH_FLOW -> CashFlowCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
                AccountsViewCardType.ACCOUNTS_LIST -> AccountsCardConfig(
                    cardId = UUID.randomUUID().toString(),
                    screenContext = ScreenContext.ACCOUNTS_VIEW
                )
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
