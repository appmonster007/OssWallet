package com.osscabinet.ossWallet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.FinancialSummary
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.service.FilterService
import com.osscabinet.ossWallet.domain.service.TransactionsUtilityService
import com.osscabinet.ossWallet.domain.service.TimelineService
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.domain.service.TimeBucket
import com.osscabinet.ossWallet.domain.service.FinancialMetrics
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class DashboardSection {
    NET_WORTH,
    CURRENCY_BALANCES,
    CASHFLOW,
    ACCOUNTS
}

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    private val settingsRepository: SettingsRepository,
    private val computationRepository: ComputationRepository,
    private val cardRepository: CardRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _sectionOrder = MutableStateFlow<List<DashboardSection>>(DashboardSection.entries)
    val sectionOrder: StateFlow<List<DashboardSection>> = _sectionOrder.asStateFlow()

    private val _initialViewContext = MutableStateFlow<DashboardViewContext>(DashboardViewContext.None)
    val initialViewContext: StateFlow<DashboardViewContext> = _initialViewContext.asStateFlow()

    data class ViewFocus(
        val focusedCardId: String? = null,
        val focusedSection: DashboardSection? = null
    )

    private val _viewFocus = MutableStateFlow<ViewFocus>(ViewFocus())
    val viewFocus: StateFlow<ViewFocus> = _viewFocus.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.dashboardSettings.collect { settings ->
                _sectionOrder.value = settings.layout.sectionOrder?.split(",")?.mapNotNull { 
                    try { DashboardSection.valueOf(it) } catch (e: Exception) { null }
                } ?: DashboardSection.entries
            }
        }
    }

    fun updateSectionOrder(sections: List<DashboardSection>) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(layout = it.layout.copy(sectionOrder = sections.joinToString(","))) }
        }
    }

    fun updateGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(gridColumns = columns)) }
        }
    }

    fun updateCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCurrencyBalancesTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
        }
    }

    fun initializeDashboardView(viewContext: DashboardViewContext) {
        _initialViewContext.value = viewContext
        when (viewContext) {
            is DashboardViewContext.FocusCard -> {
                _viewFocus.value = ViewFocus(focusedCardId = viewContext.cardId)
            }
            is DashboardViewContext.FocusSection -> {
                _viewFocus.value = ViewFocus(focusedSection = viewContext.section)
            }
            else -> {
                _viewFocus.value = ViewFocus()
            }
        }
    }

    fun updateViewFocus(focus: ViewFocus) {
        _viewFocus.value = focus
    }

    fun resetToInitialView() {
        initializeDashboardView(_initialViewContext.value)
    }

    fun hasModifiedView(): Boolean {
        val currentFocus = _viewFocus.value
        return when (val initial = _initialViewContext.value) {
            is DashboardViewContext.FocusCard -> currentFocus.focusedCardId != initial.cardId
            is DashboardViewContext.FocusSection -> currentFocus.focusedSection != initial.section
            else -> currentFocus.focusedCardId != null || currentFocus.focusedSection != null
        }
    }

    fun clearViewFocus() {
        _viewFocus.value = ViewFocus()
    }

    fun updateNetWorthFontSize(fontSize: String) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(netWorthSection = it.netWorthSection.copy(fontSize = fontSize)) }
        }
    }

    fun updateNetWorthTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(netWorthSection = it.netWorthSection.copy(timelineRange = range)) }
        }
    }

    fun updateCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCashFlowTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(timelineRange = range)) }
        }
    }

    fun updateAccountsLayout(layout: String) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(layout = layout)) }
        }
    }

    fun updateShowAddAccountCard(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(showAddAccountCard = show)) }
        }
    }

    fun updateAccountsTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(timelineRange = range)) }
        }
    }

    fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is NetWorthCardConfig) config.copy(fontSize = fontSize) else config
            }
        }
    }

    fun updateCardNetWorthTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is NetWorthCardConfig) config.copy(timelineRange = range) else config
            }
        }
    }

    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(layout = layout) else config
            }
        }
    }

    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(showBaseConversion = show) else config
            }
        }
    }

    fun updateCardCurrencyBalancesTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(timelineRange = range) else config
            }
        }
    }

    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is CashFlowCardConfig) config.copy(showBaseConversion = show) else config
            }
        }
    }

    fun updateCardCashFlowTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is CashFlowCardConfig) config.copy(timelineRange = range) else config
            }
        }
    }

    fun updateCardAccountsLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is AccountsCardConfig) config.copy(layout = layout) else config
            }
        }
    }

    fun updateCardAccountsGridColumns(cardId: String, columns: Int) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is AccountsCardConfig) config.copy(gridColumns = columns) else config
            }
        }
    }

    fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is AccountsCardConfig) config.copy(showAddAccountCard = show) else config
            }
        }
    }

    fun updateCardAccountsTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is AccountsCardConfig) config.copy(timelineRange = range) else config
            }
        }
    }

    // Direct implementations using TransactionsUtilityService
    suspend fun computeNetWorth(accounts: List<Account>, transactions: List<Transaction>, timelineRange: TimelineRange, context: ComputationContext): BigDecimal {
        val filter = TransactionFilter(timelineRange = timelineRange, accountIds = emptyList())
        val filteredTx = TransactionsUtilityService.filter(transactions, filter)
        val accountBreakdowns = TransactionsUtilityService.breakdownByAccount(
            transactions = filteredTx,
            accounts = accounts,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        return accountBreakdowns.sumOf { it.totalAmount }
    }

    suspend fun computeCashFlow(transactions: List<Transaction>, timelineRange: TimelineRange, context: ComputationContext): CashFlowResult {
        val filtered = TransactionsUtilityService.filter(transactions, TransactionFilter(timelineRange = timelineRange))
        val breakdown = TransactionsUtilityService.calculateIncomeExpenseBreakdown(
            transactions = filtered,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )

        val cashFlowBreakdown = (breakdown.incomeCurrencyBreakdown.keys + breakdown.expenseCurrencyBreakdown.keys)
            .distinct().sorted().map { curr ->
                val inc = breakdown.incomeCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                val exp = breakdown.expenseCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                CashFlowBreakdownItem(
                    currency = curr,
                    income = inc.totalAmount,
                    incomeInBase = TransactionsUtilityService.convertToBaseCurrency(inc.totalAmount, curr, context.baseCurrency, context.exchangeRates),
                    expense = exp.totalAmount,
                    expenseInBase = TransactionsUtilityService.convertToBaseCurrency(exp.totalAmount, curr, context.baseCurrency, context.exchangeRates)
                )
            }

        return CashFlowResult(
            totalIncomeInBase = breakdown.totalIncome.setScale(context.precision, RoundingMode.HALF_UP),
            totalExpensesInBase = breakdown.totalExpense.setScale(context.precision, RoundingMode.HALF_UP),
            netInBase = breakdown.netFlow.setScale(context.precision, RoundingMode.HALF_UP),
            breakdown = cashFlowBreakdown,
            filteredTransactions = filtered
        )
    }

    suspend fun computeCurrencyAggregation(accounts: List<Account>, transactions: List<Transaction>, timelineRange: TimelineRange, context: ComputationContext): List<CurrencyAggregationItem> {
        val filteredTx = TransactionsUtilityService.filter(transactions, TransactionFilter(timelineRange = timelineRange))
        return TransactionsUtilityService.breakdownByCurrency(transactions = filteredTx).map { b ->
            val amountInBase = TransactionsUtilityService.convertToBaseCurrency(
                b.totalAmount, b.currency, context.baseCurrency, context.exchangeRates
            )
            CurrencyAggregationItem(
                currency = b.currency,
                amount = b.totalAmount,
                amountInBase = amountInBase
            )
        }.sortedByDescending { it.amountInBase.abs() }
    }

    suspend fun computeAccountStats(accounts: List<Account>, transactions: List<Transaction>, timelineRange: TimelineRange, context: ComputationContext): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        val filter = TransactionFilter(timelineRange = timelineRange)
        val filteredTx = TransactionsUtilityService.filter(transactions, filter)
        val accountBreakdowns = TransactionsUtilityService.breakdownByAccount(
            transactions = filteredTx,
            accounts = accounts,
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency
        )
        val accountStats = accountBreakdowns.associate { b ->
            val acc = accounts.find { it.id == b.key }!!
            val accCurrSummary = b.currencyBreakdown[acc.currencyCode] ?: FinancialSummary.EMPTY
            b.key to AccountSummaryItem(
                account = acc,
                currencyBalances = b.currencyBreakdown.mapValues { it.value.totalAmount },
                balanceInBase = b.totalAmount,
                incomeInAccountCurrency = accCurrSummary.income,
                expenseInAccountCurrency = accCurrSummary.expense
            )
        }
        val updatedAccounts = accounts.map { account ->
            val b = accountBreakdowns.find { it.key == account.id }
            if (b != null) {
                val totalInAccountCurrency = b.currencyBreakdown.map { (curr, summary) ->
                    TransactionsUtilityService.convertToBaseCurrency(summary.totalAmount, curr, account.currencyCode, context.exchangeRates)
                }.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                account.copy(balance = totalInAccountCurrency)
            } else account
        }
        return Pair(updatedAccounts, accountStats)
    }

    suspend fun getTotalBalances(accounts: List<Account>, transactions: List<Transaction>, context: ComputationContext, timelineRange: TimelineRange): List<Account> {
        val (updatedAccounts, _) = computeAccountStats(accounts, transactions, timelineRange, context)
        return updatedAccounts
    }

    suspend fun computeCategoryBreakdownForCard(cardConfig: CategoryBreakdownCardConfig, allTransactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, BigDecimal>, precision: Int): List<CategoryBreakdownData> {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val categoryBreakdown = TransactionsUtilityService.breakdownByCategory(
            transactions = filtered,
            exchangeRates = exchangeRates,
            baseCurrency = baseCurrency
        )
        
        val totalBalance = categoryBreakdown.sumOf { it.totalAmount.abs() }
        
        return categoryBreakdown.mapIndexed { index, metric ->
            CategoryBreakdownData(
                category = metric.key,
                categoryId = null,
                netBalance = metric.totalAmount.setScale(precision, RoundingMode.HALF_UP),
                income = metric.income.setScale(precision, RoundingMode.HALF_UP),
                expense = metric.expense.setScale(precision, RoundingMode.HALF_UP),
                colorHex = 0xFF6750A4.toInt(), // Default purple color
                percentage = if (totalBalance > BigDecimal.ZERO) {
                    (metric.totalAmount.abs().toDouble() / totalBalance.toDouble() * 100).toFloat()
                } else 0f
            )
        }.sortedByDescending { it.netBalance.abs() }
    }

    suspend fun computeCategoryExpenseTreemapForCard(cardConfig: CategoryExpenseTreemapCardConfig, allTransactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, BigDecimal>, precision: Int): List<CategoryExpenseTreemapData> {
        val filtered = TransactionsUtilityService.filter(allTransactions, cardConfig.filter)
        val expenseTransactions = filtered.filter { it.amount < BigDecimal.ZERO }
        
        val categoryBreakdown = TransactionsUtilityService.breakdownByCategory(
            transactions = expenseTransactions,
            exchangeRates = exchangeRates,
            baseCurrency = baseCurrency
        )
        
        val totalExpense = categoryBreakdown.sumOf { it.expense.abs() }
        
        return categoryBreakdown.mapIndexed { index, metric ->
            CategoryExpenseTreemapData(
                category = metric.key.ifEmpty { "Uncategorized" },
                categoryId = null,
                expense = metric.expense.abs().setScale(precision, RoundingMode.HALF_UP),
                colorHex = 0xFF6750A4.toInt(), // Default purple color as Int
                percentage = if (totalExpense > BigDecimal.ZERO) {
                    ((metric.expense.abs().toDouble() / totalExpense.toDouble() * 100).toFloat())
                } else 0f,
                hasSubcategories = false,
                subcategoryData = null
            )
        }.sortedByDescending { it.expense }
    }

    val uiState: StateFlow<DashboardUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.decimalPrecision,
                settingsRepository.dashboardSettings
            ) { args: Array<Any> ->
                val accounts = args[0] as List<Account>
                val allTransactions = args[1] as List<Transaction>
                val rates = args[2] as Map<String, BigDecimal>
                val baseCurrency = args[3] as String
                val hasLoadedExchangeRates = args[4] as Boolean
                val precision = args[5] as Int
                val dashboardSettings = args[6] as com.osscabinet.ossWallet.data.local.DashboardSettings

                if (!hasLoadedExchangeRates) {
                    return@combine DashboardUiState.Loading
                }

                val computationContext = ComputationContext(baseCurrency, rates, precision)

                val globalCurrencyTotals = computeCurrencyAggregation(
                    accounts = accounts,
                    transactions = allTransactions,
                    timelineRange = dashboardSettings.currencyBalancesSection.timelineRange,
                    context = computationContext
                )

                val (updatedAccounts, accountStats) = computeAccountStats(
                    accounts = accounts,
                    transactions = allTransactions,
                    timelineRange = dashboardSettings.accountsSection.timelineRange,
                    context = computationContext
                )

                val netWorth = computeNetWorth(
                    accounts = accounts,
                    transactions = allTransactions,
                    timelineRange = dashboardSettings.netWorthSection.timelineRange,
                    context = computationContext
                )

                // Calculate cash flow for Net Worth section (used for income/expense badges)
                val netWorthTimelineRange = dashboardSettings.netWorthSection.timelineRange
                val cashFlowTimelineRange = dashboardSettings.cashFlowSection.timelineRange

                val cashFlowResult = computeCashFlow(
                    transactions = allTransactions,
                    timelineRange = cashFlowTimelineRange,
                    context = computationContext
                )

                val netWorthCashFlow = if (netWorthTimelineRange == cashFlowTimelineRange) {
                    cashFlowResult
                } else {
                    computeCashFlow(
                        transactions = allTransactions,
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
        val allTransactions: List<Transaction>,
        val allTransactionsWithBalance: List<TransactionWithBalance>,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val currencyTotals: List<CurrencyAggregationItem>,
        val decimalPrecision: Int,
        val accountStats: Map<String, AccountSummaryItem>,
        val gridColumns: Int,
        val accountsLayout: String,
        val showAddAccountCard: Boolean,
        val accountsTimelineRange: TimelineRange,
        val currencyBalancesLayout: String,
        val showBaseConversion: Boolean,
        val currencyBalancesTimelineRange: TimelineRange,
        val cashFlowShowBaseConversion: Boolean,
        val cashFlowTimelineRange: TimelineRange,
        val netWorth: BigDecimal,
        val netIncome: BigDecimal,
        val netExpense: BigDecimal,
        val netWorthFontSize: String,
        val netWorthTimelineRange: TimelineRange,
        val cashFlowResult: CashFlowResult?
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
