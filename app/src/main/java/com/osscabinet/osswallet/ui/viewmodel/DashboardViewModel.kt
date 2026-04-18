package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.util.calculateNetWorth
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import com.osscabinet.ossWallet.ui.util.convertAmount
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
    private val analyticsViewModel: AnalyticsViewModel
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
                val gridColumns = dashboardSettings.accountsSection.gridColumns
                val accountsLayout = dashboardSettings.accountsSection.layout
                val showAddAccountCard = dashboardSettings.accountsSection.showAddAccountCard
                val accountsTimelineRange = dashboardSettings.accountsSection.timelineRange
                val currencyBalancesLayout = dashboardSettings.currencyBalancesSection.layout
                val showBaseConversion = dashboardSettings.currencyBalancesSection.showBaseConversion
                val currencyBalancesTimelineRange = dashboardSettings.currencyBalancesSection.timelineRange
                val cashFlowShowBaseConversion = dashboardSettings.cashFlowSection.showBaseConversion
                val cashFlowTimelineRange = dashboardSettings.cashFlowSection.timelineRange
                val budgetShowProgressLabels = dashboardSettings.budgetSection.showProgressLabels
                val netWorthFontSize = dashboardSettings.netWorthSection.fontSize
                val netWorthTimelineRange = dashboardSettings.netWorthSection.timelineRange

                val filteredTransactionsForNetWorth = when (val range = netWorthTimelineRange) {
                    is TimelineRange.AllTime -> allTransactions
                    is TimelineRange.ThisPeriod -> {
                        val (start, end) = FilterUtils.getRangeForThis(range.period)
                        allTransactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.LastPeriod -> {
                        val (start, end) = FilterUtils.getRangeForLast(range.period)
                        allTransactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.CustomRange -> {
                        allTransactions.filter { it.timestamp in range.start..range.end }
                    }
                }
                
                val filteredTransactionsForBalances = when (val range = currencyBalancesTimelineRange) {
                    is TimelineRange.AllTime -> allTransactions
                    is TimelineRange.ThisPeriod -> {
                        val (start, end) = FilterUtils.getRangeForThis(range.period)
                        allTransactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.LastPeriod -> {
                        val (start, end) = FilterUtils.getRangeForLast(range.period)
                        allTransactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.CustomRange -> {
                        allTransactions.filter { it.timestamp in range.start..range.end }
                    }
                }

                val filteredTransactionsForAccounts = when (val range = accountsTimelineRange) {
                    is TimelineRange.AllTime -> allTransactions
                    is TimelineRange.ThisPeriod -> {
                        val (start, end) = FilterUtils.getRangeForThis(range.period)
                        allTransactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.LastPeriod -> {
                        val (start, end) = FilterUtils.getRangeForLast(range.period)
                        allTransactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.CustomRange -> {
                        allTransactions.filter { it.timestamp in range.start..range.end }
                    }
                }

                val transactionsByAccountForBalances = filteredTransactionsForBalances.groupBy { it.accountId }
                val transactionsByAccountForAccounts = filteredTransactionsForAccounts.groupBy { it.accountId }

                // Global currency totals across all accounts (using balances timeline)
                val globalCurrencyTotals = mutableMapOf<String, BigDecimal>()
                val includeInitialInBalances = currencyBalancesTimelineRange is TimelineRange.AllTime
                
                accounts.forEach { account ->
                    val accountTransactions = transactionsByAccountForBalances[account.id] ?: emptyList()
                    val breakdown = mutableMapOf<String, BigDecimal>()
                    
                    if (includeInitialInBalances) {
                        breakdown[account.currencyCode] = account.initialBalance
                    }
                    
                    accountTransactions.forEach { tx ->
                        val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                        breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
                    }
                    breakdown.forEach { (curr, amount) ->
                        globalCurrencyTotals[curr] = (globalCurrencyTotals[curr] ?: BigDecimal.ZERO).add(amount)
                    }
                }
                
                // Calculate breakdown and totals per account (using accounts timeline)
                val includeInitialInAccounts = accountsTimelineRange is TimelineRange.AllTime
                val accountStats = accounts.associate { account ->
                    val accountTransactions = transactionsByAccountForAccounts[account.id] ?: emptyList()
                    val breakdown = mutableMapOf<String, BigDecimal>()
                    
                    if (includeInitialInAccounts) {
                        breakdown[account.currencyCode] = account.initialBalance
                    }

                    val incomeBreakdown = mutableMapOf<String, BigDecimal>()
                    val expenseBreakdown = mutableMapOf<String, BigDecimal>()
                    
                    accountTransactions.forEach { tx ->
                        val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                        breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
                        
                        if (tx.isIncome) {
                            incomeBreakdown[tx.currency] = (incomeBreakdown[tx.currency] ?: BigDecimal.ZERO).add(tx.amount)
                        } else {
                            expenseBreakdown[tx.currency] = (expenseBreakdown[tx.currency] ?: BigDecimal.ZERO).add(tx.amount)
                        }
                    }
                    
                    val incomeInAccountCurrency = calculateTotalInCurrency(incomeBreakdown, account.currencyCode, rates, precision)
                    val expenseInAccountCurrency = calculateTotalInCurrency(expenseBreakdown, account.currencyCode, rates, precision)
                    
                    account.id to AccountStats(breakdown.toMap(), incomeInAccountCurrency, expenseInAccountCurrency)
                }

                // For account total balances shown in account cards, we still use ALL transactions to show actual current balance
                val allTransactionsByAccount = allTransactions.groupBy { it.accountId }
                val updatedAccounts = accounts.map { account ->
                    val accountTransactions = allTransactionsByAccount[account.id] ?: emptyList()
                    val breakdown = mutableMapOf<String, BigDecimal>()
                    breakdown[account.currencyCode] = account.initialBalance
                    accountTransactions.forEach { tx ->
                        val current = breakdown[tx.currency] ?: BigDecimal.ZERO
                        breakdown[tx.currency] = if (tx.isIncome) current.add(tx.amount) else current.subtract(tx.amount)
                    }
                    
                    val totalInAccountCurrency = calculateTotalInCurrency(
                        currencyTotals = breakdown,
                        targetCurrency = account.currencyCode,
                        exchangeRates = rates,
                        precision = precision
                    )
                    account.copy(balance = totalInAccountCurrency)
                }

                val nonArchivedAccounts = updatedAccounts.filter { !it.isArchived }
                val nonArchivedAccountIds = nonArchivedAccounts.map { it.id }.toSet()
                val netWorth = calculateNetWorth(
                    accounts = accounts,
                    allTransactions = filteredTransactionsForNetWorth,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision,
                    includeInitialBalance = netWorthTimelineRange is TimelineRange.AllTime
                )
                
                val accountMapForTransactions = updatedAccounts.associateBy { it.id }

                // Calculate TransactionsWithBalance (recent activities) with correct balance progression in account currency
                val transactionsWithBalance = allTransactions.sortedBy { it.timestamp }
                    .groupBy { it.accountId }
                    .flatMap { (accId, accountTransactions) ->
                        val account = accountMapForTransactions[accId]
                        val accountCurrency = account?.currencyCode ?: "USD"

                        var runningBalance = account?.initialBalance ?: BigDecimal.ZERO
                        accountTransactions.map { transaction ->
                            val txAmountInAccountCurrency = convertAmount(
                                amount = transaction.amount,
                                fromCurrency = transaction.currency,
                                toCurrency = accountCurrency,
                                exchangeRates = rates,
                                precision = precision + 4
                            )
                            
                            val balanceBefore = runningBalance
                            runningBalance = if (transaction.isIncome) {
                                runningBalance.add(txAmountInAccountCurrency)
                            } else {
                                runningBalance.subtract(txAmountInAccountCurrency)
                            }
                            TransactionWithBalance(
                                transaction = transaction,
                                balanceBefore = balanceBefore.setScale(precision, RoundingMode.HALF_UP),
                                balanceAfter = runningBalance.setScale(precision, RoundingMode.HALF_UP)
                            )
                        }
                    }
                    .filter { it.transaction.accountId in nonArchivedAccountIds }
                    .sortedByDescending { it.transaction.timestamp }
                    .take(20)

                DashboardUiState.Success(
                    accounts = nonArchivedAccounts,
                    allTransactions = allTransactions,
                    recentTransactions = transactionsWithBalance,
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
                    budgetShowProgressLabels = budgetShowProgressLabels,
                    netWorth = netWorth,
                    netWorthFontSize = netWorthFontSize,
                    netWorthTimelineRange = netWorthTimelineRange
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
        val currencyTotals: Map<String, BigDecimal> = emptyMap(),
        val decimalPrecision: Int = 2,
        val accountStats: Map<String, AccountStats> = emptyMap(),
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
        val netWorthTimelineRange: TimelineRange = TimelineRange.AllTime
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
