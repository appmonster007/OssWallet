package com.osscabinet.ossWallet.ui.dashboard

import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.NetWorthSettings
import com.osscabinet.ossWallet.ui.dashboard.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

enum class DashboardSection {
    NET_WORTH,
    CURRENCY_BALANCES,
    CASHFLOW,
    ACCOUNTS,
    BUDGETS
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    val settingsDataStore: SettingsDataStore,
    private val analyticsViewModel: AnalyticsViewModel,
    private val financialUseCases: FinancialUseCases,
    private val cardManagerViewModel: com.osscabinet.ossWallet.ui.viewmodel.CardManagerViewModel
) : ViewModel() {

    private val _sectionOrder = MutableStateFlow(listOf(DashboardSection.NET_WORTH, DashboardSection.CURRENCY_BALANCES, DashboardSection.CASHFLOW, DashboardSection.ACCOUNTS, DashboardSection.BUDGETS))
    val sectionOrder: StateFlow<List<DashboardSection>> = _sectionOrder.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.dashboardSettings.collect { settings ->
                val savedOrder = settings.layout.sectionOrder
                if (savedOrder != null) {
                    try {
                        val sections = savedOrder.split(",").map { DashboardSection.valueOf(it) }
                        _sectionOrder.value = sections
                    } catch (e: Exception) {
                        // Keep default order on error
                    }
                }
            }
        }
    }

    fun updateSectionOrder(newOrder: List<DashboardSection>) {
        _sectionOrder.value = newOrder
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(layout = it.layout.copy(sectionOrder = newOrder.joinToString(",") { it.name })) }
        }
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(gridColumns = columns)) }
        }
    }

    fun updateCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCurrencyBalancesTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
        }
    }

    fun updateNetWorthFontSize(fontSize: String) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(netWorthSection = it.netWorthSection.copy(fontSize = fontSize)) }
        }
    }

    fun updateNetWorthTimelineRange(range: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(netWorthSection = it.netWorthSection.copy(timelineRange = range)) }
        }
    }

    fun updateCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCashFlowTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(timelineRange = range)) }
        }
    }

    fun updateBudgetShowProgressLabels(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(budgetSection = it.budgetSection.copy(showProgressLabels = show)) }
        }
    }

    fun updateAccountsLayout(layout: String) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(layout = layout)) }
        }
    }

    fun updateShowAddAccountCard(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(showAddAccountCard = show)) }
        }
    }

    fun updateAccountsTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(timelineRange = range)) }
        }
    }

    // Card-specific update functions - delegate to CardManagerViewModel only
    // These do NOT update global settings to allow per-card independent data computation
    fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig) {
                cardConfig.copy(fontSize = fontSize)
            } else cardConfig
        }
    }

    fun updateCardNetWorthTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                cardConfig.copy(layout = layout)
            } else cardConfig
        }
    }

    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                cardConfig.copy(showBaseConversion = show)
            } else cardConfig
        }
    }

    fun updateCardCurrencyBalancesTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig) {
                cardConfig.copy(showBaseConversion = show)
            } else cardConfig
        }
    }

    fun updateCardCashFlowTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardAccountsLayout(cardId: String, layout: String) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig) {
                cardConfig.copy(layout = layout)
            } else cardConfig
        }
    }

    fun updateCardAccountsGridColumns(cardId: String, columns: Int) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig) {
                cardConfig.copy(gridColumns = columns)
            } else cardConfig
        }
    }

    fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig) {
                cardConfig.copy(showAddAccountCard = show)
            } else cardConfig
        }
    }

    fun updateCardAccountsTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardBudgetsShowProgressLabels(cardId: String, show: Boolean) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig) {
                cardConfig.copy(showProgressLabels = show)
            } else cardConfig
        }
    }

    // Helper functions to compute data for a specific card based on its config
    suspend fun computeNetWorthForCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig,
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
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig,
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
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig,
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
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig,
        accounts: List<Account>,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        val accountStats = financialUseCases.getAccountSummaries(
            accounts = accounts,
            allTransactions = allTransactions,
            timelineRange = cardConfig.timelineRange,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
        val updatedAccounts = financialUseCases.getTotalBalances(
            accounts = accounts,
            allTransactions = allTransactions,
            exchangeRates = exchangeRates,
            precision = precision,
            timelineRange = cardConfig.timelineRange
        )
        return Pair(updatedAccounts, accountStats)
    }

    val uiState: StateFlow<DashboardUiState> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                analyticsViewModel.uiState,
                settingsDataStore.decimalPrecision,
                settingsDataStore.dashboardSettings
            ) { accounts, allTransactions, analyticsState, precision, dashboardSettings ->
                if (!analyticsState.hasLoadedExchangeRates) {
                    return@combine DashboardUiState.Loading
                }

                val rates = analyticsState.exchangeRates
                val baseCurrency = analyticsState.selectedCurrency

                val globalCurrencyTotals = financialUseCases.getCurrencyAggregation(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    timelineRange = dashboardSettings.currencyBalancesSection.timelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val accountStats = financialUseCases.getAccountSummaries(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    timelineRange = dashboardSettings.accountsSection.timelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val updatedAccounts = financialUseCases.getTotalBalances(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    exchangeRates = rates,
                    precision = precision,
                    timelineRange = dashboardSettings.accountsSection.timelineRange
                )

                val netWorth = financialUseCases.calculateNetWorth(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision,
                    timelineRange = dashboardSettings.netWorthSection.timelineRange
                )

                // Calculate cash flow for Net Worth section (used for income/expense badges)
                val netWorthTimelineRange = dashboardSettings.netWorthSection.timelineRange
                val cashFlowTimelineRange = dashboardSettings.cashFlowSection.timelineRange

                // Calculate cash flow for both sections - reuse result if timeline ranges match
                val cashFlowResult = financialUseCases.calculateCashFlow(
                    allTransactions = allTransactions,
                    timelineRange = cashFlowTimelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                // Use cashFlowResult for Net Worth card if timeline ranges match, otherwise calculate separately
                val netWorthCashFlow = if (netWorthTimelineRange == cashFlowTimelineRange) {
                    cashFlowResult
                } else {
                    financialUseCases.calculateCashFlow(
                        allTransactions = allTransactions,
                        timelineRange = netWorthTimelineRange,
                        baseCurrency = baseCurrency,
                        exchangeRates = rates,
                        precision = precision
                    )
                }

                val transactionsWithBalance = transactionUseCases.getRecentTransactions(
                    allTransactions = allTransactions,
                    accounts = updatedAccounts,
                    exchangeRates = rates,
                    precision = precision
                )

                DashboardUiState.Success(
                    accounts = updatedAccounts.filter { !it.isArchived },
                    allTransactions = allTransactions,
                    recentTransactions = transactionsWithBalance,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    currencyTotals = globalCurrencyTotals,
                    decimalPrecision = precision,
                    accountStats = accountStats,
                    gridColumns = dashboardSettings.accountsSection.gridColumns,
                    accountsLayout = dashboardSettings.accountsSection.layout,
                    showAddAccountCard = dashboardSettings.accountsSection.showAddAccountCard,
                    accountsTimelineRange = dashboardSettings.accountsSection.timelineRange,
                    currencyBalancesLayout = dashboardSettings.currencyBalancesSection.layout,
                    showBaseConversion = dashboardSettings.currencyBalancesSection.showBaseConversion,
                    currencyBalancesTimelineRange = dashboardSettings.currencyBalancesSection.timelineRange,
                    cashFlowShowBaseConversion = dashboardSettings.cashFlowSection.showBaseConversion,
                    cashFlowTimelineRange = cashFlowTimelineRange,
                    budgetShowProgressLabels = dashboardSettings.budgetSection.showProgressLabels,
                    netWorth = netWorth,
                    netIncome = netWorthCashFlow.totalIncomeInBase,
                    netExpense = netWorthCashFlow.totalExpensesInBase,
                    netWorthFontSize = dashboardSettings.netWorthSection.fontSize,
                    netWorthTimelineRange = netWorthTimelineRange,
                    cashFlowResult = cashFlowResult
                )
            }.flowOn(Dispatchers.Default)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val accounts: List<Account>,
        val allTransactions: List<Transaction> = emptyList(),
        val recentTransactions: List<TransactionWithBalance>,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val currencyTotals: List<CurrencyAggregationItem> = emptyList(),
        val decimalPrecision: Int = 2,
        val accountStats: Map<String, AccountSummaryItem> = emptyMap(),
        val gridColumns: Int = 2,
        val accountsLayout: String = "grid",
        val showAddAccountCard: Boolean = true,
        val accountsTimelineRange: TimelineRange = TimelineRange.AllTime,
        val currencyBalancesLayout: String = "scroll",
        val showBaseConversion: Boolean = true,
        val currencyBalancesTimelineRange: TimelineRange = TimelineRange.AllTime,
        val cashFlowShowBaseConversion: Boolean = true,
        val cashFlowTimelineRange: TimelineRange = TimelineRange.AllTime,
        val budgetShowProgressLabels: Boolean = true,
        val netWorth: BigDecimal = BigDecimal.ZERO,
        val netIncome: BigDecimal = BigDecimal.ZERO,
        val netExpense: BigDecimal = BigDecimal.ZERO,
        val netWorthFontSize: String = "Medium",
        val netWorthTimelineRange: TimelineRange = TimelineRange.AllTime,
        val cashFlowResult: CashFlowResult? = null
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
