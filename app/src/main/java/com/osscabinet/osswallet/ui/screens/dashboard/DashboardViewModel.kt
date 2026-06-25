package com.osscabinet.ossWallet.ui.screens.dashboard

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
import com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.category.CategoryHierarchyCache
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.cache.CardRenderCache
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import com.osscabinet.ossWallet.ui.screens.plannedpayments.UndoAction
import com.osscabinet.ossWallet.ui.mapper.TransactionUiMapper
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import java.math.BigDecimal
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

    /**
     * Snapshot of the accounting data used to drive the transaction feed.
     */
    private data class AccountingSnapshot(
        val accounts: List<Account>,
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val links: Map<Long, List<Long>>,
        val currency: CurrencySnapshot
    )

    private data class CurrencySnapshot(
        val baseCode: String,
        val rates: Map<String, BigDecimal>,
        val hasLoaded: Boolean
    )

    /**
     * Snapshot of metadata and settings used to enrichment and cards.
     */
    private data class MetadataSnapshot(
        val plannedPayments: List<PlannedPayment>,
        val budgets: List<Budget>,
        val loans: List<Loan>,
        val loanPayments: Map<Long, List<LoanPayment>>,
        val tags: List<Tag>
    )

    /**
     * Pre-computed structural feed state (balances + grouping).
     */
    private data class StructuralState(
        val snapshot: AccountingSnapshot,
        val hierarchy: CategoryHierarchyCache,
        val visibleGroups: List<com.osscabinet.ossWallet.domain.model.TransactionGroup>
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            // 1. Accounting Pipeline: Driven by financial changes.
            // Computes balances and groups only when accounting data actually changes.
            val structuralStateFlow = combine(
                accountRepository.getAllAccounts(profileId),
                databaseQueryService.observeFilteredTransactions(profileId, TransactionFilter(), limit = DASHBOARD_TRANSACTION_FEED_LIMIT),
                categoryRepository.categories,
                transactionManager.getAllTransactionLinks(),
                combine(
                    computationRepository.selectedCurrency,
                    computationRepository.exchangeRates,
                    computationRepository.hasLoadedExchangeRates
                ) { code, rates, loaded -> CurrencySnapshot(code, rates, loaded) }
            ) { acc, tx, cat, links, curr ->
                AccountingSnapshot(acc, tx, cat, links, curr)
            }.distinctUntilChanged().flatMapLatest { snapshot ->
                flow {
                    val activeAccountIds = snapshot.accounts.filter { !it.isArchived }.map { it.id }.toSet()
                    val activeTransactions = snapshot.transactions.filter { it.accountId in activeAccountIds }
                    val exchangeRateSnapshot = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(snapshot.currency.baseCode, snapshot.currency.rates)

                    // Fetch starting balances for the window for accurate running totals.
                    val startingBalances = if (activeTransactions.isNotEmpty()) {
                        val windowStart = activeTransactions.minOf { it.timestamp }
                        TransactionAggregationService.getSummaryByAccountAndCurrency(
                            databaseQueryService, profileId,
                            TransactionFilter(timelineRange = TimelineRange.CustomRange(end = windowStart - 1))
                        ).getOrDefault(emptyMap()).mapValues { (accountId, byCurrency) ->
                            val accountCurrency = snapshot.accounts.find { it.id == accountId }?.currencyCode ?: snapshot.currency.baseCode
                            CurrencyService.calculateTotalFinancialSummary(byCurrency, accountCurrency, exchangeRateSnapshot).totalAmount
                        }
                    } else emptyMap()

                    val transactionsWithBalance = CardAggregationService.computeRecentBalances(activeTransactions, snapshot.accounts, snapshot.currency.baseCode, exchangeRateSnapshot, startingBalances)

                    val visibleGroups = TimelineService.getGroupedData(
                        transactions = transactionsWithBalance.filter { it.transaction.timestamp <= System.currentTimeMillis() }.take(20),
                        absoluteHistory = transactionsWithBalance,
                        groupingType = com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType.BY_DAY,
                        accounts = snapshot.accounts,
                        categories = snapshot.categories,
                        allTags = emptyList(), // Enrichment handles tags from metadata
                        baseCurrency = snapshot.currency.baseCode,
                        exchangeRates = snapshot.currency.rates,
                        linkedTransactionIds = snapshot.links
                    )
                    emit(StructuralState(snapshot, CategoryHierarchyCache.build(snapshot.categories), visibleGroups))
                }.flowOn(Dispatchers.Default)
            }

            // 2. Metadata Pipeline: Enrichment data for cards and list items.
            val metadataFlow = combine(
                plannedPaymentRepository.getActivePlannedPayments(profileId),
                budgetRepository.getAllBudgets(profileId),
                loanRepository.getAllLoans(profileId),
                loanRepository.getAllPayments(profileId),
                tagRepository.getAllTags()
            ) { pp, b, l, lp, t ->
                MetadataSnapshot(pp, b, l, lp.groupBy { it.loanId }, t)
            }.distinctUntilChanged()

            // 3. UI State Pipeline: Final integration.
            combine(structuralStateFlow, metadataFlow, cardsConfig) { structuralState, metadata, config ->
                Triple(structuralState, metadata, config)
            }.flatMapLatest { (structuralState, metadata, config) ->
                channelFlow<DashboardUiState> {
                    val snapshot = structuralState.snapshot
                    val hierarchy = structuralState.hierarchy
                    val visibleGroups = structuralState.visibleGroups

                    val baseInput = CardInput(
                        profileId = profileId,
                        transactions = snapshot.transactions,
                        allProfileTransactions = snapshot.transactions,
                        accounts = snapshot.accounts,
                        categories = snapshot.categories,
                        tags = metadata.tags,
                        exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(snapshot.currency.baseCode, snapshot.currency.rates),
                        targetCurrency = snapshot.currency.baseCode,
                        plannedPayments = metadata.plannedPayments,
                        budgets = metadata.budgets,
                        loans = metadata.loans,
                        loanPayments = metadata.loanPayments,
                        filter = TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = true)),
                        screenContext = ScreenContext.DASHBOARD,
                        commonFilterEnabled = false,
                        categoryHierarchy = hierarchy
                    )

                    fun buildSuccess(cardMap: Map<String, CardRenderData>, computing: Boolean) = DashboardUiState.Success(
                        profileId = profileId,
                        accounts = snapshot.accounts.filter { !it.isArchived },
                        allAccounts = snapshot.accounts,
                        cardDataMap = cardMap,
                        isCardsComputing = computing,
                        allTransactions = snapshot.transactions,
                        recentActivitiesState = TransactionUiMapper.mapToUiState(
                            domainState = TransactionListState(visibleGroups, visibleGroups.isEmpty()),
                            baseCurrency = snapshot.currency.baseCode
                        ),
                        allCategories = snapshot.categories,
                        allTags = metadata.tags,
                        baseCurrency = snapshot.currency.baseCode,
                        exchangeRates = snapshot.currency.rates,
                        plannedPayments = metadata.plannedPayments,
                        budgets = metadata.budgets,
                        loans = metadata.loans,
                        loanPayments = metadata.loanPayments
                    )

                    val cachedCards = cardRenderCache[profileId, snapshot.currency.baseCode]
                    send(buildSuccess(cachedCards ?: emptyMap(), computing = true))

                    if (!snapshot.currency.hasLoaded) return@channelFlow

                    // Card Computations
                    val cardDataFlow = MutableStateFlow(cachedCards ?: emptyMap())
                    val completedCount = AtomicInteger(0)
                    val configs = config.cards

                    launch {
                        cardDataFlow.collect { send(buildSuccess(it, completedCount.get() < configs.size)) }
                    }

                    configs.forEach { cardConfig ->
                        launch(Dispatchers.Default) {
                            val result = runCatching { cardComputerRegistry.compute(cardConfig.cardType, baseInput, cardConfig) }
                            cardDataFlow.update { it + (cardConfig.cardId to result.getOrElse { e -> CardRenderData.Error(e.message) }) }
                            completedCount.incrementAndGet()
                        }
                    }
                }
            }
        }
        .catch { e -> emit(DashboardUiState.Error(e.message ?: "Dashboard data error")) }
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
                    val profileId = settingsRepository.currentProfileId.first()
                    transactionManager.deleteTransaction(action.transactionId, profileId)
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

    private companion object {
        const val DASHBOARD_TRANSACTION_FEED_LIMIT = 100
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val profileId: Int,
        val accounts: List<Account>,
        val allAccounts: List<Account> = emptyList(),
        val cardDataMap: Map<String, CardRenderData>,
        val isCardsComputing: Boolean = false,
        val allTransactions: List<Transaction>,
        val recentActivitiesState: TransactionListUiState,
        val allCategories: List<Category>,
        val allTags: List<Tag> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val plannedPayments: List<PlannedPayment> = emptyList(),
        val budgets: List<Budget> = emptyList(),
        val loans: List<Loan> = emptyList(),
        val loanPayments: Map<Long, List<LoanPayment>> = emptyMap()
    ) : DashboardUiState()

    data class Error(val message: String) : DashboardUiState()
}
