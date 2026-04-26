package com.osscabinet.ossWallet.ui.dashboard

import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.dashboard.components.*
import com.osscabinet.ossWallet.ui.dashboard.model.*
import com.osscabinet.ossWallet.domain.formatter.TimelineFormatter
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma
import com.osscabinet.ossWallet.domain.util.toEnum
import com.osscabinet.ossWallet.domain.formatter.ThemeHelper
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.domain.service.CardComputationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
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
    private val cardManagerViewModel: com.osscabinet.ossWallet.ui.viewmodel.CardManagerViewModel,
    private val settingsViewModel: com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel,
    private val cardComputationService: CardComputationService
) : ViewModel() {

    private val _sectionOrder = MutableStateFlow(listOf(DashboardSection.NET_WORTH, DashboardSection.CURRENCY_BALANCES, DashboardSection.CASHFLOW, DashboardSection.ACCOUNTS, DashboardSection.BUDGETS))
    val sectionOrder: StateFlow<List<DashboardSection>> = _sectionOrder.asStateFlow()

    // View context for dashboard navigation
    private val _initialViewContext = MutableStateFlow<DashboardViewContext>(DashboardViewContext.None)
    val initialViewContext: StateFlow<DashboardViewContext> = _initialViewContext.asStateFlow()

    data class ViewFocus(
        val focusedCardId: String? = null,
        val focusedSection: DashboardSection? = null
    )

    private val _viewFocus = MutableStateFlow(ViewFocus())
    val viewFocus: StateFlow<ViewFocus> = _viewFocus.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.dashboardSettings.collect { settings ->
                val savedOrder = settings.layout.sectionOrder
                if (savedOrder != null) {
                    try {
                        val sections = savedOrder.splitByComma().map { it.toEnum<DashboardSection>() }
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
            settingsDataStore.updateDashboardSettings { it.copy(layout = it.layout.copy(sectionOrder = newOrder.joinByComma { it.name })) }
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

    /**
     * Initialize the dashboard view from a navigation context
     */
    fun initializeDashboardView(context: DashboardViewContext) {
        _initialViewContext.value = context
        val focus = when (context) {
            is DashboardViewContext.None -> ViewFocus()
            is DashboardViewContext.FocusCard -> ViewFocus(focusedCardId = context.cardId)
            is DashboardViewContext.FocusSection -> ViewFocus(focusedSection = context.section)
        }
        _viewFocus.value = focus
    }

    /**
     * Update the view focus
     */
    fun updateViewFocus(focus: ViewFocus) {
        _viewFocus.value = focus
    }

    /**
     * Reset the view focus to the initial context
     */
    fun resetToInitialView() {
        initializeDashboardView(_initialViewContext.value)
    }

    /**
     * Check if the current view focus has been modified from the initial context
     */
    fun hasModifiedView(): Boolean {
        val initialFocus = when (_initialViewContext.value) {
            is DashboardViewContext.None -> ViewFocus()
            is DashboardViewContext.FocusCard -> ViewFocus(focusedCardId = (_initialViewContext.value as DashboardViewContext.FocusCard).cardId)
            is DashboardViewContext.FocusSection -> ViewFocus(focusedSection = (_initialViewContext.value as DashboardViewContext.FocusSection).section)
        }
        return _viewFocus.value != initialFocus
    }

    /**
     * Clear the view focus (reset to default)
     */
    fun clearViewFocus() {
        _viewFocus.value = ViewFocus()
        _initialViewContext.value = DashboardViewContext.None
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
            if (cardConfig is NetWorthCardConfig) {
                cardConfig.copy(fontSize = fontSize)
            } else cardConfig
        }
    }

    fun updateCardNetWorthTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is NetWorthCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is CurrencyBalancesCardConfig) {
                cardConfig.copy(layout = layout)
            } else cardConfig
        }
    }

    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is CurrencyBalancesCardConfig) {
                cardConfig.copy(showBaseConversion = show)
            } else cardConfig
        }
    }

    fun updateCardCurrencyBalancesTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is CurrencyBalancesCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is CashFlowCardConfig) {
                cardConfig.copy(showBaseConversion = show)
            } else cardConfig
        }
    }

    fun updateCardCashFlowTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is CashFlowCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardAccountsLayout(cardId: String, layout: String) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is AccountsCardConfig) {
                cardConfig.copy(layout = layout)
            } else cardConfig
        }
    }

    fun updateCardAccountsGridColumns(cardId: String, columns: Int) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is AccountsCardConfig) {
                cardConfig.copy(gridColumns = columns)
            } else cardConfig
        }
    }

    fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is AccountsCardConfig) {
                cardConfig.copy(showAddAccountCard = show)
            } else cardConfig
        }
    }

    fun updateCardAccountsTimelineRange(cardId: String, range: TimelineRange) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is AccountsCardConfig) {
                cardConfig.copy(timelineRange = range)
            } else cardConfig
        }
    }

    fun updateCardBudgetsShowProgressLabels(cardId: String, show: Boolean) {
        cardManagerViewModel.updateCard(cardId) { cardConfig ->
            if (cardConfig is BudgetsCardConfig) {
                cardConfig.copy(showProgressLabels = show)
            } else cardConfig
        }
    }

    // ============================================================
    // Helper Functions for Card-Level Filtering and Computation
    // ============================================================

    fun computeNetWorth(
        accounts: List<Account>,
        transactions: List<Transaction>,
        timelineRange: TimelineRange,
        context: ComputationContext
    ): BigDecimal {
        return financialUseCases.calculateNetWorth(
            accounts = accounts,
            allTransactions = transactions,
            context = context,
            timelineRange = timelineRange
        )
    }

    fun computeCashFlow(
        transactions: List<Transaction>,
        timelineRange: TimelineRange,
        context: ComputationContext
    ): CashFlowResult {
        return financialUseCases.calculateCashFlow(
            allTransactions = transactions,
            timelineRange = timelineRange,
            context = context
        )
    }

    fun computeCurrencyAggregation(
        accounts: List<Account>,
        transactions: List<Transaction>,
        timelineRange: TimelineRange,
        context: ComputationContext
    ): List<CurrencyAggregationItem> {
        return financialUseCases.getCurrencyAggregation(
            accounts = accounts,
            allTransactions = transactions,
            timelineRange = timelineRange,
            context = context
        )
    }

    fun computeAccountStats(
        accounts: List<Account>,
        transactions: List<Transaction>,
        timelineRange: TimelineRange,
        context: ComputationContext
    ): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        val accountStats = financialUseCases.getAccountSummaries(
            accounts = accounts,
            allTransactions = transactions,
            timelineRange = timelineRange,
            context = context
        )
        // Update accounts with timeline-filtered balances
        val updatedAccounts = financialUseCases.getTotalBalances(
            accounts = accounts,
            allTransactions = transactions,
            context = context,
            timelineRange = timelineRange
        )
        return Pair(updatedAccounts, accountStats)
    }

    fun getTotalBalances(
        accounts: List<Account>,
        transactions: List<Transaction>,
        context: ComputationContext,
        timelineRange: TimelineRange
    ): List<Account> {
        return financialUseCases.getTotalBalances(
            accounts = accounts,
            allTransactions = transactions,
            context = context,
            timelineRange = timelineRange
        )
    }

    // Analytics card compute methods delegating to CardComputationService
    suspend fun computeCategoryBreakdownForCard(
        cardConfig: CategoryBreakdownCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ) = cardComputationService.computeCategoryBreakdown(
        config = cardConfig,
        allTransactions = allTransactions,
        context = ComputationContext(baseCurrency, exchangeRates, precision)
    )

    suspend fun computeTimelineTrajectoryForCard(
        cardConfig: TimelineTrajectoryCardConfig,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ) = cardComputationService.computeTimelineTrajectory(
        config = cardConfig,
        allTransactions = allTransactions,
        accounts = accounts,
        context = ComputationContext(baseCurrency, exchangeRates, precision)
    )

    suspend fun computeCategoryExpenseTreemapForCard(
        cardConfig: CategoryExpenseTreemapCardConfig,
        allTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ) = cardComputationService.computeCategoryExpenseTreemap(
        config = cardConfig,
        allTransactions = allTransactions,
        context = ComputationContext(baseCurrency, exchangeRates, precision)
    )

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
                val computationContext = ComputationContext(baseCurrency, rates, precision)

                val globalCurrencyTotals = financialUseCases.getCurrencyAggregation(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    timelineRange = dashboardSettings.currencyBalancesSection.timelineRange,
                    context = computationContext
                )

                val accountStats = financialUseCases.getAccountSummaries(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    timelineRange = dashboardSettings.accountsSection.timelineRange,
                    context = computationContext
                )

                val updatedAccounts = financialUseCases.getTotalBalances(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    context = computationContext,
                    timelineRange = dashboardSettings.accountsSection.timelineRange
                )

                val netWorth = financialUseCases.calculateNetWorth(
                    accounts = accounts,
                    allTransactions = allTransactions,
                    context = computationContext,
                    timelineRange = dashboardSettings.netWorthSection.timelineRange
                )

                // Calculate cash flow for Net Worth section (used for income/expense badges)
                val netWorthTimelineRange = dashboardSettings.netWorthSection.timelineRange
                val cashFlowTimelineRange = dashboardSettings.cashFlowSection.timelineRange

                // Calculate cash flow for both sections - reuse result if timeline ranges match
                val cashFlowResult = financialUseCases.calculateCashFlow(
                    allTransactions = allTransactions,
                    timelineRange = cashFlowTimelineRange,
                    context = computationContext
                )

                // Use cashFlowResult for Net Worth card if timeline ranges match, otherwise calculate separately
                val netWorthCashFlow = if (netWorthTimelineRange == cashFlowTimelineRange) {
                    cashFlowResult
                } else {
                    financialUseCases.calculateCashFlow(
                        allTransactions = allTransactions,
                        timelineRange = netWorthTimelineRange,
                        context = computationContext
                    )
                }

                // Calculate all transactions with balance (without filtering)
                val transactionsWithBalance = allTransactions.sortedBy { it.timestamp }
                    .groupBy { it.accountId }
                    .flatMap { (accId, accountTransactions) ->
                        val account = updatedAccounts.find { it.id == accId }
                        val accountCurrency = account?.currencyCode ?: "USD"

                        var runningBalance = BigDecimal.ZERO
                        accountTransactions.map { transaction ->
                            val txAmountInAccountCurrency = CurrencyService.convertAmount(
                                amount = transaction.amount,
                                fromCurrency = transaction.currency,
                                toCurrency = accountCurrency,
                                exchangeRates = rates,
                                precision = precision + 4
                            )

                            val balanceBefore = runningBalance
                            runningBalance = runningBalance.add(txAmountInAccountCurrency)
                            TransactionWithBalance(
                                transaction = transaction,
                                balanceBefore = balanceBefore.setScale(precision, RoundingMode.HALF_UP),
                                balanceAfter = runningBalance.setScale(precision, RoundingMode.HALF_UP)
                            )
                        }
                    }
                    .filter { it.transaction.accountId in updatedAccounts.filter { !it.isArchived }.map { it.id }.toSet() }
                    .sortedByDescending { it.transaction.timestamp }

                DashboardUiState.Success(
                    accounts = updatedAccounts.filter { !it.isArchived },
                    allTransactions = allTransactions,
                    allTransactionsWithBalance = transactionsWithBalance,
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
        val allTransactionsWithBalance: List<TransactionWithBalance>,
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
