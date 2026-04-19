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
import com.osscabinet.ossWallet.ui.dashboard.components.*
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
    private val getTransactionsUseCase: GetTransactionsUseCase,
    val settingsDataStore: SettingsDataStore,
    private val analyticsViewModel: AnalyticsViewModel,
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
    private val getCurrencyAggregationUseCase: GetCurrencyAggregationUseCase,
    private val getAccountSummariesUseCase: GetAccountSummariesUseCase,
    private val getTotalBalancesUseCase: GetTotalBalancesUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
    private val calculateCashFlowUseCase: CalculateCashFlowUseCase
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

    val uiState: StateFlow<DashboardUiState> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                getTransactionsUseCase(profileId),
                analyticsViewModel.uiState,
                settingsDataStore.decimalPrecision,
                settingsDataStore.dashboardSettings
            ) { accounts, allTransactions, analyticsState, precision, dashboardSettings ->
                if (!analyticsState.hasLoadedExchangeRates) {
                    return@combine DashboardUiState.Loading
                }

                val rates = analyticsState.exchangeRates
                val baseCurrency = analyticsState.selectedCurrency

                val globalCurrencyTotals = getCurrencyAggregationUseCase(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    timelineRange = dashboardSettings.currencyBalancesSection.timelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val accountStats = getAccountSummariesUseCase(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    timelineRange = dashboardSettings.accountsSection.timelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val updatedAccounts = getTotalBalancesUseCase(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    exchangeRates = rates,
                    precision = precision
                )

                val netWorth = calculateNetWorthUseCase(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision,
                    timelineRange = dashboardSettings.netWorthSection.timelineRange
                )

                val transactionsWithBalance = getRecentTransactionsUseCase(
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
                    cashFlowTimelineRange = dashboardSettings.cashFlowSection.timelineRange,
                    budgetShowProgressLabels = dashboardSettings.budgetSection.showProgressLabels,
                    netWorth = netWorth,
                    netWorthFontSize = dashboardSettings.netWorthSection.fontSize,
                    netWorthTimelineRange = dashboardSettings.netWorthSection.timelineRange,
                    cashFlowResult = calculateCashFlowUseCase(
                        allTransactions = allTransactions,
                        timelineRange = dashboardSettings.cashFlowSection.timelineRange,
                        baseCurrency = baseCurrency,
                        exchangeRates = rates,
                        precision = precision
                    )
                )
            }
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
        val netWorthFontSize: String = "Medium",
        val netWorthTimelineRange: TimelineRange = TimelineRange.AllTime,
        val cashFlowResult: CashFlowResult? = null
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
