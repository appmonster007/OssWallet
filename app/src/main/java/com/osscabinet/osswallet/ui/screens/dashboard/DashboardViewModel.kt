package com.osscabinet.ossWallet.ui.screens.dashboard

import com.osscabinet.ossWallet.domain.model.EntryMode
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
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
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.category.CategoryHierarchyCache
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.cache.CardRenderCache
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import com.osscabinet.ossWallet.ui.screens.plannedpayments.UndoAction
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

enum class DashboardSection {
    NET_WORTH,
    CURRENCY_BALANCES,
    CASHFLOW,
    ACCOUNTS
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
    private val computationRepository: ComputationRepository,
    override val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    internal val cardComputerRegistry: CardComputerRegistry,
    internal val transactionManager: TransactionManager,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val budgetRepository: BudgetRepository,
    private val loanRepository: LoanRepository,
    private val tagRepository: TagRepository,
    private val databaseQueryService: DatabaseQueryService,
    private val cardRenderCache: CardRenderCache
) : BaseViewModel(settingsRepository), CardUpdateHandler {

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.DASHBOARD

    val cardsConfig: StateFlow<DashboardCardsConfig> =
        cardRepository.getCardsConfig(cardScreenContext)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCardsConfig())

    private val _initialViewContext =
        MutableStateFlow<DashboardViewContext>(DashboardViewContext.None)
    val initialViewContext: StateFlow<DashboardViewContext> = _initialViewContext.asStateFlow()

    data class ViewFocus(
        val focusedCardId: String? = null,
        val focusedSection: DashboardSection? = null
    )

    private val _viewFocus = MutableStateFlow<ViewFocus>(ViewFocus())

    private val _lastUndoAction = MutableStateFlow<UndoAction?>(null)
    val lastUndoAction: StateFlow<UndoAction?> = _lastUndoAction.asStateFlow()

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
                plannedPaymentRepository.getActivePlannedPayments(profileId)
            val budgetsFlow: Flow<List<Budget>> =
                budgetRepository.getAllBudgets(profileId)
            val loansFlow: Flow<List<Loan>> =
                loanRepository.getAllLoans(profileId)
            val loanPaymentsFlow: Flow<List<com.osscabinet.ossWallet.domain.model.LoanPayment>> =
                loanRepository.getAllPayments()
            val tagsFlow: Flow<List<com.osscabinet.ossWallet.domain.model.Tag>> =
                tagRepository.getAllTags()
            combine(
                accountRepository.getAllAccounts(profileId),
                databaseQueryService.observeFilteredTransactions(profileId, TransactionFilter(), limit = 100),
                categoryRepository.categories,
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                cardsConfig,
                plannedPaymentsFlow,
                budgetsFlow,
                loansFlow,
                tagsFlow,
                loanPaymentsFlow
            ) { args: Array<Any> -> args }
                .flatMapLatest { args ->
                    @Suppress("UNCHECKED_CAST")
                    val accounts = args[0] as List<Account>

                    @Suppress("UNCHECKED_CAST")
                    val recentTransactions = args[1] as List<Transaction>

                    @Suppress("UNCHECKED_CAST")
                    val categories = args[2] as List<Category>

                    @Suppress("UNCHECKED_CAST")
                    val rates = args[3] as Map<String, BigDecimal>
                    val baseCurrency = args[4] as String
                    val hasLoadedExchangeRates = args[5] as Boolean
                    val cardsCfg = args[6] as DashboardCardsConfig

                    @Suppress("UNCHECKED_CAST")
                    val plannedPayments = args[7] as List<PlannedPayment>

                    @Suppress("UNCHECKED_CAST")
                    val budgets = args[8] as List<Budget>

                    @Suppress("UNCHECKED_CAST")
                    val loans = args[9] as List<Loan>

                    @Suppress("UNCHECKED_CAST")
                    val tags = args[10] as List<com.osscabinet.ossWallet.domain.model.Tag>

                    @Suppress("UNCHECKED_CAST")
                    val loanPayments =
                        args[11] as List<com.osscabinet.ossWallet.domain.model.LoanPayment>

                    val activeAccountIds = accounts.filter { !it.isArchived }.map { it.id }.toSet()
                    val groupedLoanPayments = loanPayments.groupBy { it.loanId }

                    val baseInput = CardInput(
                        profileId = profileId,
                        transactions = recentTransactions,
                        allProfileTransactions = recentTransactions,
                        accounts = accounts,
                        categories = categories,
                        tags = tags,
                        exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(
                            anchor = baseCurrency,
                            rates = rates
                        ),
                        targetCurrency = baseCurrency,
                        plannedPayments = plannedPayments,
                        budgets = budgets,
                        loans = loans,
                        loanPayments = groupedLoanPayments,
                        filter = TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = true)),
                        screenContext = ScreenContext.DASHBOARD,
                        commonFilterEnabled = false
                    )

                    channelFlow<DashboardUiState> {
                        // --- Phase 1: Structural data — renders before any card computes ---
                        val transactionsWithBalance = CardAggregationService.computeRecentBalances(
                            recentTransactions.filter { it.accountId in activeAccountIds },
                            accounts,
                            baseCurrency,
                            rates
                        )
                        val now = System.currentTimeMillis()
                        val visibleTransactions = transactionsWithBalance
                            .filter { it.transaction.timestamp <= now }
                            .take(20)
                            .toList()
                        val groups = TimelineService.getGroupedData(
                            transactions = visibleTransactions,
                            absoluteHistory = transactionsWithBalance,
                            groupingType = com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType.BY_DAY,
                            accounts = accounts,
                            categories = categories,
                            allTags = tags,
                            baseCurrency = baseCurrency,
                            exchangeRates = rates
                        )

                        fun buildSuccess(
                            cardDataMap: Map<String, CardRenderData>,
                            isCardsComputing: Boolean
                        ) = DashboardUiState.Success(
                            profileId = profileId,
                            accounts = accounts.filter { !it.isArchived },
                            allAccounts = accounts,
                            cardDataMap = cardDataMap,
                            isCardsComputing = isCardsComputing,
                            allTransactions = recentTransactions,
                            recentActivitiesState = TransactionListState(
                                groups = groups,
                                isEmpty = groups.isEmpty()
                            ),
                            allCategories = categories,
                            allTags = tags,
                            baseCurrency = baseCurrency,
                            exchangeRates = rates,
                            plannedPayments = plannedPayments,
                            budgets = budgets,
                            loans = loans,
                            loanPayments = groupedLoanPayments
                        )

                        val configs = cardsCfg.cards
                        if (configs.isEmpty()) {
                            send(buildSuccess(emptyMap(), isCardsComputing = false))
                            return@channelFlow
                        }

                        val cachedCards = cardRenderCache[profileId]

                        if (cachedCards != null) {
                            // Cache hit: show last-known cards immediately — no shimmers, no blanks.
                            send(buildSuccess(cachedCards, isCardsComputing = false))
                        } else {
                            // First launch for this profile: no cache yet, show card shimmers.
                            send(buildSuccess(emptyMap(), isCardsComputing = true))
                        }

                        // First launch only: rates not in DB yet. Phase 1 is shown with shimmers.
                        // Combine re-fires when rates arrive → new channelFlow starts → Phase 2 runs.
                        if (!hasLoadedExchangeRates) return@channelFlow

                        // Phase 2: Recompute all cards in parallel.
                        // Cache hit → seed partialMap with stale data so cards update silently (no shimmer flash).
                        // No cache → partialMap starts empty, cards pop in as they finish.
                        val enrichedInput = baseInput.copy(
                            categoryHierarchy = CategoryHierarchyCache.build(baseInput.categories)
                        )
                        val mutex = Mutex()
                        val partialMap = cachedCards?.toMutableMap() ?: mutableMapOf()
                        var completed = 0

                        configs.forEach { config ->
                            launch {
                                val result = runCatching {
                                    cardComputerRegistry.compute(config.cardType, enrichedInput, config)
                                }
                                val snapshot: Pair<Map<String, CardRenderData>, Boolean>
                                mutex.withLock {
                                    result.onSuccess { data -> partialMap[config.cardId] = data }
                                    result.onFailure { e -> partialMap[config.cardId] = CardRenderData.Error(e.message) }
                                    completed++
                                    if (completed == configs.size) {
                                        cardRenderCache[profileId] = partialMap
                                            .filterValues { it !is CardRenderData.Error }
                                    }
                                    snapshot = partialMap.toMap() to (completed < configs.size)
                                }
                                // Cache hit: silent background refresh (isCardsComputing always false).
                                // No cache: show progress so shimmers resolve as cards finish.
                                send(buildSuccess(snapshot.first, if (cachedCards != null) false else snapshot.second))
                            }
                        }
                    }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )

    fun quickConfirmById(id: Long, scheduledDate: Long) {
        val state = uiState.value as? DashboardUiState.Success ?: return
        val payment = state.plannedPayments.find { it.id == id } ?: return
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val request = TransactionRequest(
                name = payment.name,
                amount = payment.amount,
                entryMode = payment.entryMode,
                accountId = payment.accountId.takeIf { !it.isNullOrBlank() } ?: return@launch,
                currency = payment.currency,
                categoryId = payment.categoryId,
                tags = payment.tags,
                paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                timestamp = System.currentTimeMillis(),
                payee = payment.payee ?: "",
                note = payment.note ?: "",
                targetAccountId = payment.targetAccountId,
                targetCurrency = payment.targetCurrency,
                transferRate = payment.transferRate,
                sourcePlannedPaymentId = payment.id,
                warrantyExpiration = payment.warrantyExpiration,
                gpsLocation = payment.gpsLocation
            )
            val transactionId = transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = request,
                profileId = profileId,
                sourcePlannedPaymentId = payment.id,
                scheduledDate = scheduledDate
            )
            _lastUndoAction.value = UndoAction.ConfirmUndo(payment, scheduledDate, transactionId)
        }
    }

    fun quickSkipById(id: Long, scheduledDate: Long) {
        val state = uiState.value as? DashboardUiState.Success ?: return
        val payment = state.plannedPayments.find { it.id == id } ?: return
        viewModelScope.launch {
            transactionManager.skipPlannedPayment(
                paymentId = payment.id,
                scheduledDate = scheduledDate
            )
            _lastUndoAction.value = UndoAction.SkipUndo(payment, scheduledDate)
        }
    }

    fun undoLastPlannedPaymentAction() {
        val action = _lastUndoAction.value ?: return
        _lastUndoAction.value = null
        viewModelScope.launch {
            when (action) {
                is UndoAction.SkipUndo -> {
                    val occ = plannedPaymentRepository.getOccurrenceForDate(action.payment.id, action.scheduledDate)
                    if (occ != null) plannedPaymentRepository.updateOccurrenceStatus(occ.id, OccurrenceStatus.PENDING)
                    plannedPaymentRepository.update(
                        action.payment.copy(schedule = action.payment.schedule.copy(nextDueDate = action.scheduledDate))
                    )
                }
                is UndoAction.ConfirmUndo -> {
                    transactionManager.deleteTransaction(action.transactionId)
                    val occ = plannedPaymentRepository.getOccurrenceForDate(action.payment.id, action.scheduledDate)
                    if (occ != null) plannedPaymentRepository.updateOccurrenceStatus(occ.id, OccurrenceStatus.PENDING)
                    plannedPaymentRepository.update(
                        action.payment.copy(schedule = action.payment.schedule.copy(nextDueDate = action.scheduledDate))
                    )
                }
            }
        }
    }

    fun clearUndoAction() { _lastUndoAction.value = null }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val profileId: Int,
        val accounts: List<Account>,          // active (non-archived) — for dashboard account cards
        val allAccounts: List<Account> = emptyList(), // all accounts including archived — for filter pickers
        val cardDataMap: Map<String, CardRenderData>,
        val isCardsComputing: Boolean = false,
        val allTransactions: List<Transaction>,
        val recentActivitiesState: TransactionListState,
        val allCategories: List<Category>,
        val allTags: List<com.osscabinet.ossWallet.domain.model.Tag> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val plannedPayments: List<PlannedPayment> = emptyList(),
        val budgets: List<Budget> = emptyList(),
        val loans: List<Loan> = emptyList(),
        val loanPayments: Map<Long, List<com.osscabinet.ossWallet.domain.model.LoanPayment>> = emptyMap()
    ) : DashboardUiState()

    data class Error(val message: String) : DashboardUiState()
}
