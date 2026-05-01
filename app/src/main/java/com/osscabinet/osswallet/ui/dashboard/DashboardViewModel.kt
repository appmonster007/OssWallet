package com.osscabinet.ossWallet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownData
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapData
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.service.*
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
    override val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val registry: CardComputerRegistry,
    private val plannedPaymentRepository: PlannedPaymentRepository? = null,
    private val budgetRepository: BudgetRepository? = null,
    private val loanRepository: LoanRepository? = null
) : ViewModel(), CardUpdateHandler {

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.DASHBOARD

    val cardsConfig: StateFlow<DashboardCardsConfig> = cardRepository.getCardsConfig(cardScreenContext)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCardsConfig())

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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            val plannedPaymentsFlow: Flow<List<PlannedPayment>> =
                plannedPaymentRepository?.getActivePlannedPayments(profileId) ?: flowOf(emptyList())
            val budgetsFlow: Flow<List<Budget>> =
                budgetRepository?.getActiveBudgets(profileId) ?: flowOf(emptyList())
            val loansFlow: Flow<List<Loan>> =
                loanRepository?.getActiveLoans(profileId) ?: flowOf(emptyList())
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                categoryRepository.categories,
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.decimalPrecision,
                settingsRepository.dashboardSettings,
                plannedPaymentsFlow,
                budgetsFlow,
                loansFlow,
                cardsConfig
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
                @Suppress("UNUSED_VARIABLE")
                val dashboardSettings = args[7] as com.osscabinet.ossWallet.data.local.DashboardSettings
                @Suppress("UNCHECKED_CAST")
                val plannedPayments = args[8] as List<PlannedPayment>
                @Suppress("UNCHECKED_CAST")
                val budgets = args[9] as List<Budget>
                @Suppress("UNCHECKED_CAST")
                val loans = args[10] as List<Loan>
                @Suppress("UNUSED_VARIABLE")
                val cardConfig = args[11] as DashboardCardsConfig

                if (!hasLoadedExchangeRates) {
                    return@combine DashboardUiState.Loading
                }

                // Invalidate registry cache when data changes to ensure cards refresh
                registry.clearCache()

                // Construct base CardInput
                @Suppress("UNUSED_VARIABLE")
                val baseInput = CardInput(
                    transactions = allTransactions,
                    accounts = accounts,
                    categories = categories,
                    exchangeRates = rates,
                    baseCurrency = baseCurrency,
                    precision = precision,
                    plannedPayments = plannedPayments,
                    budgets = budgets,
                    loans = loans
                )

                // Calculate all transactions with balance (without filtering)
                // We need this for the recent activities list
                val transactionsWithBalance = allTransactions.sortedBy { it.timestamp }
                    .groupBy { it.accountId }
                    .flatMap { (accId, accountTransactions) ->
                        val account = accounts.find { it.id == accId }
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
                    .filter { it.transaction.accountId in accounts.filter { !it.isArchived }.map { it.id }.toSet() }
                    .sortedByDescending { it.transaction.timestamp }

                DashboardUiState.Success(
                    accounts = accounts.filter { !it.isArchived },
                    allTransactions = allTransactions,
                    allTransactionsWithBalance = transactionsWithBalance,
                    allCategories = categories,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    decimalPrecision = precision,
                    plannedPayments = plannedPayments,
                    budgets = budgets,
                    loans = loans
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

    suspend fun computeCategoryBreakdownForCard(cardConfig: CategoryBreakdownCardConfig, allTransactions: List<Transaction>, accounts: List<Account>, baseCurrency: String, exchangeRates: Map<String, BigDecimal>, precision: Int): List<CategoryBreakdownData> {
        val categories = categoryRepository.getAllCategoriesSync()
        val input = CardInput(
            transactions = allTransactions,
            accounts = accounts,
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

    suspend fun computeCategoryExpenseTreemapForCard(cardConfig: CategoryExpenseTreemapCardConfig, allTransactions: List<Transaction>, accounts: List<Account>, baseCurrency: String, exchangeRates: Map<String, BigDecimal>, precision: Int): List<CategoryExpenseTreemapData> {
        val categories = categoryRepository.getAllCategoriesSync()
        val input = CardInput(
            transactions = allTransactions,
            accounts = accounts,
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
        val allCategories: List<Category>,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val decimalPrecision: Int,
        val plannedPayments: List<PlannedPayment> = emptyList(),
        val budgets: List<Budget> = emptyList(),
        val loans: List<Loan> = emptyList()
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
