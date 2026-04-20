package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class AccountsViewSection {
    NET_WORTH,
    CURRENCY_BALANCES,
    CASHFLOW,
    ACCOUNTS_LIST
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewViewModel(
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    val settingsDataStore: SettingsDataStore,
    private val analyticsViewModel: AnalyticsViewModel,
    private val financialUseCases: FinancialUseCases,
    private val cardManagerViewModel: CardManagerViewModel,
    private val cardComputationService: com.osscabinet.ossWallet.domain.service.CardComputationService
) : ViewModel() {

    private val _sectionOrder = MutableStateFlow(listOf(AccountsViewSection.NET_WORTH, AccountsViewSection.CURRENCY_BALANCES, AccountsViewSection.CASHFLOW, AccountsViewSection.ACCOUNTS_LIST))
    val sectionOrder: StateFlow<List<AccountsViewSection>> = _sectionOrder.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.accountsViewSettings.collect { settings ->
                val savedOrder = settings.layout.sectionOrder
                if (savedOrder != null) {
                    try {
                        val sections = savedOrder.split(",").map { AccountsViewSection.valueOf(it) }
                        _sectionOrder.value = sections
                    } catch (e: Exception) {
                        // Keep default order on error
                    }
                }
            }
        }
    }

    fun updateSectionOrder(newOrder: List<AccountsViewSection>) {
        _sectionOrder.value = newOrder
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(layout = it.layout.copy(sectionOrder = newOrder.joinToString(",") { it.name })) }
        }
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(gridColumns = columns)) }
        }
    }

    fun updateCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCurrencyBalancesTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
        }
    }

    fun updateNetWorthFontSize(fontSize: String) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(netWorthSection = it.netWorthSection.copy(fontSize = fontSize)) }
        }
    }

    fun updateNetWorthTimelineRange(range: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(netWorthSection = it.netWorthSection.copy(timelineRange = range)) }
        }
    }

    fun updateCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCashFlowTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(timelineRange = range)) }
        }
    }

    fun updateAccountsLayout(layout: String) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(layout = layout)) }
        }
    }

    fun updateShowAddAccountCard(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(showAddAccountCard = show)) }
        }
    }

    fun updateAccountsTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(timelineRange = range)) }
        }
    }

    val uiState: StateFlow<AccountsViewUiState> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                analyticsViewModel.uiState,
                settingsDataStore.decimalPrecision,
                settingsDataStore.accountsViewSettings
            ) { accounts, allTransactions, analyticsState, precision, accountsViewSettings ->
                if (!analyticsState.hasLoadedExchangeRates) {
                    return@combine AccountsViewUiState.Loading
                }

                val rates = analyticsState.exchangeRates
                val baseCurrency = analyticsState.selectedCurrency
                val gridColumns = accountsViewSettings.accountsSection.gridColumns
                val accountsLayout = accountsViewSettings.accountsSection.layout
                val showAddAccountCard = accountsViewSettings.accountsSection.showAddAccountCard
                val accountsTimelineRange = accountsViewSettings.accountsSection.timelineRange
                val currencyBalancesLayout = accountsViewSettings.currencyBalancesSection.layout
                val showBaseConversion = accountsViewSettings.currencyBalancesSection.showBaseConversion
                val currencyBalancesTimelineRange = accountsViewSettings.currencyBalancesSection.timelineRange
                val cashFlowShowBaseConversion = accountsViewSettings.cashFlowSection.showBaseConversion
                val cashFlowTimelineRange = accountsViewSettings.cashFlowSection.timelineRange
                val netWorthFontSize = accountsViewSettings.netWorthSection.fontSize
                val netWorthTimelineRange = accountsViewSettings.netWorthSection.timelineRange

                val globalCurrencyTotals = financialUseCases.getCurrencyAggregation(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    timelineRange = currencyBalancesTimelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val accountStats = financialUseCases.getAccountSummaries(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    timelineRange = accountsTimelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val updatedAccounts = financialUseCases.getTotalBalances(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    exchangeRates = rates,
                    precision = precision,
                    timelineRange = accountsTimelineRange
                )

                val netWorth = financialUseCases.calculateNetWorth(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision,
                    timelineRange = netWorthTimelineRange
                )

                val netWorthFlow = financialUseCases.calculateCashFlow(
                    allTransactions = allTransactions,
                    timelineRange = netWorthTimelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val cashFlowResult = financialUseCases.calculateCashFlow(
                    allTransactions = allTransactions,
                    timelineRange = cashFlowTimelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val nonArchivedAccounts = updatedAccounts.filter { !it.isArchived }

                AccountsViewUiState.Success(
                    accounts = nonArchivedAccounts,
                    allTransactions = allTransactions,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    currencyTotals = globalCurrencyTotals,
                    decimalPrecision = precision,
                    accountStats = accountStats,
                    gridColumns = gridColumns,
                    accountsLayout = accountsLayout,
                    showAddAccountCard = showAddAccountCard,
                    accountsTimelineRange = accountsTimelineRange,
                    currencyBalancesLayout = currencyBalancesLayout,
                    showBaseConversion = showBaseConversion,
                    currencyBalancesTimelineRange = currencyBalancesTimelineRange,
                    cashFlowShowBaseConversion = cashFlowShowBaseConversion,
                    cashFlowTimelineRange = cashFlowTimelineRange,
                    netWorth = netWorth,
                    netIncome = netWorthFlow.totalIncomeInBase,
                    netExpense = netWorthFlow.totalExpensesInBase,
                    netWorthFontSize = netWorthFontSize,
                    netWorthTimelineRange = netWorthTimelineRange,
                    cashFlowResult = cashFlowResult
                )
            }.flowOn(Dispatchers.Default)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountsViewUiState.Loading
        )

    // Card-specific update functions - delegate to CardManagerViewModel only
    // These do NOT update global settings to allow per-card independent data computation
    fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewNetWorthCardConfig) {
                cardConfig.copy(fontSize = fontSize)
            } else cardConfig
        }
    }

    fun updateCardNetWorthTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewNetWorthCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCurrencyBalancesCardConfig) {
                cardConfig.copy(layout = layout)
            } else cardConfig
        }
    }

    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCurrencyBalancesCardConfig) {
                cardConfig.copy(showBaseConversion = show)
            } else cardConfig
        }
    }

    fun updateCardCurrencyBalancesTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCurrencyBalancesCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCashFlowCardConfig) {
                cardConfig.copy(showBaseConversion = show)
            } else cardConfig
        }
    }

    fun updateCardCashFlowTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewCashFlowCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardAccountsLayout(cardId: String, layout: String) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewAccountsListCardConfig) {
                cardConfig.copy(layout = layout)
            } else cardConfig
        }
    }

    fun updateCardAccountsGridColumns(cardId: String, columns: Int) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewAccountsListCardConfig) {
                cardConfig.copy(gridColumns = columns)
            } else cardConfig
        }
    }

    fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewAccountsListCardConfig) {
                cardConfig.copy(showAddAccountCard = show)
            } else cardConfig
        }
    }

    fun updateCardAccountsTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateAccountsViewCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsViewAccountsListCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    // Helper functions to compute data for a specific card based on its config
    suspend fun computeNetWorthForCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.AccountsViewNetWorthCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<BigDecimal, CashFlowResult> {
        val netWorth = financialUseCases.calculateNetWorth(
            accounts = accounts,
            allTransactions = allTransactions,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision,
            timelineRange = cardConfig.timelineRange
        )
        val cashFlow = financialUseCases.calculateCashFlow(
            allTransactions = allTransactions,
            timelineRange = cardConfig.timelineRange,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
        return Pair(netWorth, cashFlow)
    }

    suspend fun computeCurrencyTotalsForCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.AccountsViewCurrencyBalancesCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem> {
        return financialUseCases.getCurrencyAggregation(
            accounts = accounts,
            allTransactions = allTransactions,
            timelineRange = cardConfig.timelineRange,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
    }

    suspend fun computeCashFlowForCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.AccountsViewCashFlowCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        return financialUseCases.calculateCashFlow(
            allTransactions = allTransactions,
            timelineRange = cardConfig.timelineRange,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
    }

    suspend fun computeAccountStatsForCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.AccountsViewAccountsListCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        // Convert AccountsViewAccountsListCardConfig to AccountsCardConfig
        val unifiedConfig = com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig(
            cardId = cardConfig.cardId,
            title = cardConfig.title,
            isEnabled = cardConfig.isEnabled,
            order = cardConfig.order,
            layout = cardConfig.layout,
            gridColumns = cardConfig.gridColumns,
            showAddAccountCard = cardConfig.showAddAccountCard,
            timelineRange = cardConfig.timelineRange
        )
        
        return cardComputationService.computeAccountStats(
            config = unifiedConfig,
            accounts = accounts,
            transactions = allTransactions,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
    }
}

sealed class AccountsViewUiState {
    object Loading : AccountsViewUiState()
    data class Success(
        val accounts: List<Account>,
        val allTransactions: List<Transaction> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val currencyTotals: List<CurrencyAggregationItem> = emptyList(),
        val decimalPrecision: Int = 2,
        val accountStats: Map<String, AccountSummaryItem> = emptyMap(),
        val gridColumns: Int = 1,
        val accountsLayout: String = "grid",
        val showAddAccountCard: Boolean = true,
        val accountsTimelineRange: TimelineRange = TimelineRange.AllTime,
        val currencyBalancesLayout: String = "scroll",
        val showBaseConversion: Boolean = true,
        val currencyBalancesTimelineRange: TimelineRange = TimelineRange.AllTime,
        val cashFlowShowBaseConversion: Boolean = true,
        val cashFlowTimelineRange: TimelineRange = TimelineRange.AllTime,
        val netWorth: BigDecimal = BigDecimal.ZERO,
        val netIncome: BigDecimal = BigDecimal.ZERO,
        val netExpense: BigDecimal = BigDecimal.ZERO,
        val netWorthFontSize: String = "Medium",
        val netWorthTimelineRange: TimelineRange = TimelineRange.AllTime,
        val cashFlowResult: CashFlowResult? = null
    ) : AccountsViewUiState()
    data class Error(val message: String) : AccountsViewUiState()
}
