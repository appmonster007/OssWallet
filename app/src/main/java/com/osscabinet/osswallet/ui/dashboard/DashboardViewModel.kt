package com.osscabinet.ossWallet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.settings.AccountsSectionSettings
import com.osscabinet.ossWallet.domain.model.settings.CashFlowSettings
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesSettings
import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.service.*
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.card.*
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
    private val categoryRepository: CategoryRepository,
    private val registry: CardComputerRegistry
) : ViewModel(), CardUpdateHandler {

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

    override fun updateCardNetWorthFontSize(cardId: String, fontSize: String) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is NetWorthCardConfig) config.copy(fontSize = fontSize) else config
            }
        }
    }

    override fun updateCardNetWorthFilter(cardId: String, filter: TransactionFilter) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is NetWorthCardConfig) config.copy(filter = filter) else config
            }
        }
    }

    override fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(layout = layout) else config
            }
        }
    }

    override fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(showBaseConversion = show) else config
            }
        }
    }

    override fun updateCardCurrencyBalancesFilter(cardId: String, filter: TransactionFilter) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is CurrencyBalancesCardConfig) config.copy(filter = filter) else config
            }
        }
    }

    override fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is CashFlowCardConfig) config.copy(showBaseConversion = show) else config
            }
        }
    }

    override fun updateCardCashFlowFilter(cardId: String, filter: TransactionFilter) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is CashFlowCardConfig) config.copy(filter = filter) else config
            }
        }
    }

    override fun updateCardAccountsLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is AccountsCardConfig) config.copy(layout = layout) else config
            }
        }
    }

    override fun updateCardAccountsGridColumns(cardId: String, columns: Int) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is AccountsCardConfig) config.copy(gridColumns = columns) else config
            }
        }
    }

    override fun updateCardAccountsShowAddAccountCard(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is AccountsCardConfig) config.copy(showAddAccountCard = show) else config
            }
        }
    }

    override fun updateCardAccountsFilter(cardId: String, filter: TransactionFilter) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config ->
                if (config is AccountsCardConfig) config.copy(filter = filter) else config
            }
        }
    }

    override fun updateCardCategoryBreakdownConfig(cardId: String, config: CategoryBreakdownCardConfig) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config }
        }
    }

    override fun updateCardCategoryExpenseTreemapConfig(cardId: String, config: CategoryExpenseTreemapCardConfig) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId) { config }
        }
    }

    // Registry-based engine handles all computations
    // Use the injected registry instead of a local reference

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                categoryRepository.categories,
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.decimalPrecision,
                settingsRepository.dashboardSettings
            ) { args: Array<Any> ->
                @Suppress("UNCHECKED_CAST")
                val accounts = args[0] as List<Account>
                @Suppress("UNCHECKED_CAST")
                val allTransactions = args[1] as List<Transaction>
                @Suppress("UNCHECKED_CAST")
                val categories = args[2] as List<Category>
                @Suppress("UNCHECKED_CAST")
                val rates = args[3] as Map<String, BigDecimal>
                val baseCurrency = args[4] as String
                val hasLoadedExchangeRates = args[5] as Boolean
                val precision = args[6] as Int
                val dashboardSettings = args[7] as com.osscabinet.ossWallet.data.local.DashboardSettings

                if (!hasLoadedExchangeRates) {
                    return@combine DashboardUiState.Loading
                }

                // Construct base CardInput
                val baseInput = CardInput(
                    transactions = allTransactions,
                    accounts = accounts,
                    categories = categories,
                    exchangeRates = rates,
                    baseCurrency = baseCurrency,
                    precision = precision
                )

                val currencyTotals = (registry.compute(
                    type = CardType.CURRENCY_BALANCES,
                    input = baseInput,
                    config = CurrencyBalancesCardConfig(
                        cardId = "currency_balances_main",
                        filter = TransactionFilter(timelineRange = dashboardSettings.currencyBalancesSection.timelineRange),
                        showBaseConversion = dashboardSettings.currencyBalancesSection.showBaseConversion
                    )
                ) as CardRenderData.CurrencyBalances).items

                val accountsData = registry.compute(
                    type = CardType.ACCOUNTS,
                    input = baseInput,
                    config = AccountsCardConfig(
                        cardId = "accounts_main",
                        filter = TransactionFilter(timelineRange = dashboardSettings.accountsSection.timelineRange),
                        layout = dashboardSettings.accountsSection.layout,
                        gridColumns = dashboardSettings.accountsSection.gridColumns,
                        showAddAccountCard = dashboardSettings.accountsSection.showAddAccountCard
                    )
                ) as CardRenderData.Accounts

                val netWorthData = registry.compute(
                    type = CardType.NET_WORTH,
                    input = baseInput,
                    config = NetWorthCardConfig(
                        cardId = "net_worth_main",
                        filter = TransactionFilter(timelineRange = dashboardSettings.netWorthSection.timelineRange),
                        fontSize = dashboardSettings.netWorthSection.fontSize
                    )
                ) as CardRenderData.NetWorth

                val cashFlowResult = (registry.compute(
                    type = CardType.CASH_FLOW,
                    input = baseInput,
                    config = CashFlowCardConfig(
                        cardId = "cash_flow_main",
                        filter = TransactionFilter(timelineRange = dashboardSettings.cashFlowSection.timelineRange),
                        showBaseConversion = dashboardSettings.cashFlowSection.showBaseConversion
                    )
                ) as CardRenderData.CashFlow).result

                val netWorthCashFlow = if (dashboardSettings.netWorthSection.timelineRange == dashboardSettings.cashFlowSection.timelineRange) {
                    cashFlowResult
                } else {
                    netWorthData.cashFlowResult
                }

                // Calculate all transactions with balance (without filtering)
                val updatedAccounts = accountsData.updatedAccounts
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
                    currencyTotals = currencyTotals,
                    decimalPrecision = precision,
                    accountStats = accountsData.accountStats,
                    gridColumns = dashboardSettings.accountsSection.gridColumns,
                    accountsLayout = dashboardSettings.accountsSection.layout,
                    showAddAccountCard = dashboardSettings.accountsSection.showAddAccountCard,
                    accountsTimelineRange = dashboardSettings.accountsSection.timelineRange,
                    currencyBalancesLayout = dashboardSettings.currencyBalancesSection.layout,
                    showBaseConversion = dashboardSettings.currencyBalancesSection.showBaseConversion,
                    currencyBalancesTimelineRange = dashboardSettings.currencyBalancesSection.timelineRange,
                    cashFlowShowBaseConversion = dashboardSettings.cashFlowSection.showBaseConversion,
                    cashFlowTimelineRange = dashboardSettings.cashFlowSection.timelineRange,
                    netWorth = netWorthData.value,
                    netIncome = netWorthCashFlow.totalIncomeInBase,
                    netExpense = netWorthCashFlow.totalExpensesInBase,
                    netWorthFontSize = dashboardSettings.netWorthSection.fontSize,
                    netWorthTimelineRange = dashboardSettings.netWorthSection.timelineRange,
                    cashFlowResult = cashFlowResult
                )
            }.flowOn(Dispatchers.Default)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )

    fun computeNetWorth(accounts: List<Account>, transactions: List<Transaction>, timelineRange: TimelineRange, context: ComputationContext): BigDecimal {
        val input = CardInput(
            transactions = transactions,
            accounts = accounts,
            categories = emptyList(),
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency,
            precision = context.precision
        )
        val data = registry.compute(
            type = CardType.NET_WORTH,
            input = input,
            config = NetWorthCardConfig(cardId = "temp", filter = TransactionFilter(timelineRange = timelineRange))
        ) as CardRenderData.NetWorth
        return data.value
    }

    fun computeCashFlow(transactions: List<Transaction>, timelineRange: TimelineRange, context: ComputationContext): CashFlowResult {
        val input = CardInput(
            transactions = transactions,
            accounts = emptyList(),
            categories = emptyList(),
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency,
            precision = context.precision
        )
        val data = registry.compute(
            type = CardType.CASH_FLOW,
            input = input,
            config = CashFlowCardConfig(cardId = "temp", filter = TransactionFilter(timelineRange = timelineRange))
        ) as CardRenderData.CashFlow
        return data.result
    }

    fun computeCurrencyAggregation(accounts: List<Account>, transactions: List<Transaction>, timelineRange: TimelineRange, context: ComputationContext): List<CurrencyAggregationItem> {
        val input = CardInput(
            transactions = transactions,
            accounts = accounts,
            categories = emptyList(),
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency,
            precision = context.precision
        )
        val data = registry.compute(
            type = CardType.CURRENCY_BALANCES,
            input = input,
            config = CurrencyBalancesCardConfig(cardId = "temp", filter = TransactionFilter(timelineRange = timelineRange))
        ) as CardRenderData.CurrencyBalances
        return data.items
    }

    fun computeAccountStats(accounts: List<Account>, transactions: List<Transaction>, timelineRange: TimelineRange, context: ComputationContext): Pair<List<Account>, Map<String, AccountSummaryItem>> {
        val input = CardInput(
            transactions = transactions,
            accounts = accounts,
            categories = emptyList(),
            exchangeRates = context.exchangeRates,
            baseCurrency = context.baseCurrency,
            precision = context.precision
        )
        val data = registry.compute(
            type = CardType.ACCOUNTS,
            input = input,
            config = AccountsCardConfig(cardId = "temp", filter = TransactionFilter(timelineRange = timelineRange))
        ) as CardRenderData.Accounts
        return Pair(data.updatedAccounts, data.accountStats)
    }

    suspend fun getTotalBalances(accounts: List<Account>, transactions: List<Transaction>, context: ComputationContext, timelineRange: TimelineRange): List<Account> {
        return computeAccountStats(accounts, transactions, timelineRange, context).first
    }

    suspend fun computeCategoryBreakdownForCard(cardConfig: CategoryBreakdownCardConfig, allTransactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, BigDecimal>, precision: Int): List<CategoryBreakdownData> {
        val categories = categoryRepository.getAllCategoriesSync()
        val input = CardInput(
            transactions = allTransactions,
            accounts = emptyList(),
            categories = categories,
            exchangeRates = exchangeRates,
            baseCurrency = baseCurrency,
            precision = precision
        )
        val data = registry.compute(
            type = CardType.CATEGORY_BREAKDOWN,
            input = input,
            config = cardConfig
        ) as CardRenderData.CategoryBreakdown
        
        fun mapToUi(metric: CategoryTreeMetrics): CategoryBreakdownData {
            return CategoryBreakdownData(
                category = metric.category,
                categoryId = metric.categoryId,
                netBalance = metric.netBalance.setScale(precision, RoundingMode.HALF_UP),
                income = metric.income.setScale(precision, RoundingMode.HALF_UP),
                expense = metric.expense.setScale(precision, RoundingMode.HALF_UP),
                colorHex = metric.colorHex,
                percentage = metric.percentage,
                hasSubcategories = metric.hasSubcategories,
                subcategoryData = metric.subcategoryData.map { mapToUi(it) }
            )
        }

        return data.items.map { mapToUi(it) }
    }

    suspend fun computeCategoryExpenseTreemapForCard(cardConfig: CategoryExpenseTreemapCardConfig, allTransactions: List<Transaction>, baseCurrency: String, exchangeRates: Map<String, BigDecimal>, precision: Int): List<CategoryExpenseTreemapData> {
        val categories = categoryRepository.getAllCategoriesSync()
        val input = CardInput(
            transactions = allTransactions,
            accounts = emptyList(),
            categories = categories,
            exchangeRates = exchangeRates,
            baseCurrency = baseCurrency,
            precision = precision
        )
        val data = registry.compute(
            type = CardType.CATEGORY_EXPENSE_TREEMAP,
            input = input,
            config = cardConfig
        ) as CardRenderData.CategoryTreemap
        
        fun mapToUi(metric: CategoryTreeMetrics): CategoryExpenseTreemapData {
            return CategoryExpenseTreemapData(
                category = if (metric.category.isEmpty()) "Uncategorized" else metric.category,
                categoryId = metric.categoryId,
                expense = metric.expense.abs().setScale(precision, RoundingMode.HALF_UP),
                colorHex = metric.colorHex,
                percentage = metric.percentage,
                hasSubcategories = metric.hasSubcategories,
                subcategoryData = metric.subcategoryData.map { mapToUi(it) }
            )
        }

        return data.items.map { mapToUi(it) }
    }
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
