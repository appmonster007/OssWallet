package com.osscabinet.ossWallet.ui.screens.dashboard

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.usecase.ComputeCardUseCase
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

enum class DashboardSection {
    NET_WORTH,
    CURRENCY_BALANCES,
    CASHFLOW,
    ACCOUNTS
}

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    settingsRepository: SettingsRepository,
    private val computationRepository: ComputationRepository,
    override val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val computeCardUseCase: ComputeCardUseCase,
    private val plannedPaymentRepository: PlannedPaymentRepository? = null,
    private val budgetRepository: BudgetRepository? = null,
    private val loanRepository: LoanRepository? = null,
    private val transactionManager: TransactionManager? = null,
) : BaseViewModel(settingsRepository), CardUpdateHandler {

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.DASHBOARD

    val cardsConfig: StateFlow<DashboardCardsConfig> =
        cardRepository.getCardsConfig(cardScreenContext)
            .stateIn(viewModelScope, SharingStarted.Eagerly, DashboardCardsConfig())

    private val _sectionOrder = MutableStateFlow<List<DashboardSection>>(DashboardSection.entries)

    private val _initialViewContext =
        MutableStateFlow<DashboardViewContext>(DashboardViewContext.None)
    val initialViewContext: StateFlow<DashboardViewContext> = _initialViewContext.asStateFlow()

    data class ViewFocus(
        val focusedCardId: String? = null,
        val focusedSection: DashboardSection? = null
    )

    private val _viewFocus = MutableStateFlow<ViewFocus>(ViewFocus())

    init {
        viewModelScope.launch {
            settingsRepository.dashboardSettings.collect { settings ->
                _sectionOrder.value = settings.layout.sectionOrder?.split(",")?.mapNotNull {
                    try {
                        DashboardSection.valueOf(it)
                    } catch (_: Exception) {
                        null
                    }
                } ?: DashboardSection.entries
            }
        }
    }

    fun initializeDashboardView(viewContext: DashboardViewContext) {
        _initialViewContext.value = viewContext
    }

    fun resetToInitialView() {
        initializeDashboardView(_initialViewContext.value)
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
                settingsRepository.use24HourFormat,
                cardsConfig,
                plannedPaymentsFlow,
                budgetsFlow,
                loansFlow
            ) { args: Array<Any> -> args }
            .flatMapLatest { args ->
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
                val use24Hour = args[6] as Boolean
                val cardsCfg = args[7] as DashboardCardsConfig
                @Suppress("UNCHECKED_CAST")
                val plannedPayments = args[8] as List<PlannedPayment>
                @Suppress("UNCHECKED_CAST")
                val budgets = args[9] as List<Budget>
                @Suppress("UNCHECKED_CAST")
                val loans = args[10] as List<Loan>

                if (!hasLoadedExchangeRates) {
                    return@flatMapLatest flowOf(DashboardUiState.Loading)
                }

                val baseInput = CardInput(
                    profileId = profileId,
                    transactions = allTransactions,
                    accounts = accounts,
                    categories = categories,
                    exchangeRates = rates,
                    baseCurrency = baseCurrency,
                    plannedPayments = plannedPayments,
                    budgets = budgets,
                    loans = loans
                )

                flow<DashboardUiState> {
                    val cardDataMap = computeCardUseCase.computeAll(
                        configs = cardsCfg.cards,
                        input = baseInput
                    )

                    // Compute transactionsWithBalance
                    val activeAccountIds = accounts.asSequence().filter { !it.isArchived }.map { it.id }.toSet()
                    val now = System.currentTimeMillis()
                    val transactionsWithBalance = allTransactions
                        .asSequence()
                        .filter { it.timestamp <= now }
                        .sortedBy { it.timestamp }
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
                                    exchangeRates = rates
                                )

                                val balanceBefore = runningBalance
                                runningBalance = runningBalance.add(txAmountInAccountCurrency)
                                TransactionWithBalance(
                                    transaction = transaction,
                                    balanceBefore = balanceBefore.setScale(
                                        CurrencyService.precision,
                                        RoundingMode.HALF_UP
                                    ),
                                    balanceAfter = runningBalance.setScale(
                                        CurrencyService.precision,
                                        RoundingMode.HALF_UP
                                    )
                                )
                            }
                        }
                        .filter { it.transaction.accountId in activeAccountIds }
                        .sortedByDescending { it.transaction.timestamp }
                        .toList()

                    val groups = TimelineService.getGroupedData(
                        transactions = transactionsWithBalance.take(20),
                        absoluteHistory = transactionsWithBalance,
                        groupingType = com.osscabinet.ossWallet.data.local.TransactionGroupingType.BY_DAY,
                        accounts = accounts,
                        categories = categories,
                        baseCurrency = baseCurrency,
                        exchangeRates = rates
                    )

                    emit(DashboardUiState.Success(
                        accounts = accounts.filter { !it.isArchived },
                        cardDataMap = cardDataMap,
                        allTransactions = allTransactions,
                        recentActivitiesState = TransactionListState(groups = groups, isEmpty = groups.isEmpty()),
                        allCategories = categories,
                        baseCurrency = baseCurrency,
                        exchangeRates = rates,
                        use24HourFormat = use24Hour,
                        plannedPayments = plannedPayments,
                        budgets = budgets,
                        loans = loans
                    ))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )

    fun quickConfirmById(id: Long) {
        val state = uiState.value as? DashboardUiState.Success ?: return
        val payment = state.plannedPayments.find { it.id == id } ?: return
        val repo = plannedPaymentRepository ?: return
        val manager = transactionManager ?: return
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val occurrence = PlannedPaymentOccurrence(
                id = 0,
                plannedPaymentId = payment.id,
                scheduledDate = payment.schedule.nextDueDate
            )
            val request = TransactionRequest(
                name = payment.name,
                amount = payment.amount,
                transactionType = payment.transactionType,
                accountId = payment.accountId ?: "",
                currency = payment.currency,
                categoryId = payment.categoryId,
                tags = payment.tags,
                paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                timestamp = occurrence.scheduledDate,
                payee = payment.payee ?: "",
                note = payment.note ?: "",
                isTransfer = payment.transactionType == TransactionType.TRANSFER,
                targetAccountId = payment.targetAccountId,
                targetCurrency = payment.targetCurrency,
                transferRate = payment.transferRate
            )
            val transactionId = manager.handleSaveTransaction(null, request, profileId)
            repo.upsertOccurrence(
                occurrence.copy(
                    status = OccurrenceStatus.CONFIRMED,
                    generatedTransactionId = transactionId
                )
            )
            val nextDue =
                PlannedPaymentService.calculateNextDueDate(payment, occurrence.scheduledDate)
            if (nextDue == null) {
                repo.update(payment.copy(status = PlannedPaymentStatus.COMPLETED))
            } else {
                repo.update(
                    payment.copy(
                        schedule = payment.schedule.copy(
                            nextDueDate = nextDue,
                            lastTriggeredDate = occurrence.scheduledDate
                        )
                    )
                )
            }
        }
    }

    fun quickSkipById(id: Long) {
        val state = uiState.value as? DashboardUiState.Success ?: return
        val payment = state.plannedPayments.find { it.id == id } ?: return
        val repo = plannedPaymentRepository ?: return
        viewModelScope.launch {
            val occurrence = PlannedPaymentOccurrence(
                id = 0,
                plannedPaymentId = payment.id,
                scheduledDate = payment.schedule.nextDueDate
            )
            repo.upsertOccurrence(occurrence.copy(status = OccurrenceStatus.SKIPPED))
            val nextDue =
                PlannedPaymentService.calculateNextDueDate(payment, occurrence.scheduledDate)
            if (nextDue == null) {
                repo.update(payment.copy(status = PlannedPaymentStatus.COMPLETED))
            } else {
                repo.update(
                    payment.copy(
                        schedule = payment.schedule.copy(
                            nextDueDate = nextDue,
                            lastTriggeredDate = occurrence.scheduledDate
                        )
                    )
                )
            }
        }
    }

    // All card-specific computation functions now use CardComputerRegistry as the single source of truth
    // The standardized card computers handle all computation logic:
    // - NetWorthCardComputer handles net worth computations
    // - CashFlowCardComputer handles cash flow computations  
    // - CurrencyBalancesCardComputer handles currency aggregation
    // - AccountsCardComputer handles account statistics
    // - CategoryPieChartCardComputer handles pie chart data
    // - CategoryTreemapCardComputer handles treemap data
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val accounts: List<Account>,
        val cardDataMap: Map<String, CardRenderData>,
        val allTransactions: List<Transaction>,
        val recentActivitiesState: TransactionListState,
        val allCategories: List<Category>,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val use24HourFormat: Boolean = false,
        val plannedPayments: List<PlannedPayment> = emptyList(),
        val budgets: List<Budget> = emptyList(),
        val loans: List<Loan> = emptyList()
    ) : DashboardUiState()

    data class Error(val message: String) : DashboardUiState()
}
