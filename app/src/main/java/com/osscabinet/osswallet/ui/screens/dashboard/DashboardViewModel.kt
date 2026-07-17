package com.osscabinet.ossWallet.ui.screens.dashboard

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.core.util.DispatcherProvider
import com.osscabinet.ossWallet.domain.cache.CardRenderCache
import com.osscabinet.ossWallet.domain.card.CardComputeState
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.card.ScreenPersistenceEnvelope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.category.CategoryHierarchyCache
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.mapper.TransactionUiMapper
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import com.osscabinet.ossWallet.ui.screens.plannedpayments.UndoAction
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel.DashboardDisplayPrefs
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListItem
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@kotlinx.serialization.Serializable
enum class DashboardSection {
    NET_WORTH,
    CURRENCY_BALANCES,
    CASHFLOW,
    ACCOUNTS
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
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
    private val templateRepository: TemplateRepository,
    private val filterRepository: FilterRepository,
    private val profileRepository: ProfileRepository,
    private val cardRenderCache: CardRenderCache,
    private val dispatchers: DispatcherProvider,
) : BaseViewModel(settingsRepository), CardUpdateHandler {

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.DASHBOARD

    val cardsConfig: StateFlow<DashboardCardsConfig> =
        cardRepository.getCardsConfig(cardScreenContext)
            .stateIn(viewModelScope, SharingStarted.Eagerly, DashboardCardsConfig())

    private val _initialViewContext =
        MutableStateFlow<DashboardViewContext>(DashboardViewContext.None)
    val initialViewContext: StateFlow<DashboardViewContext> = _initialViewContext.asStateFlow()

    private val _lastUndoAction = MutableStateFlow<UndoAction?>(null)
    val lastUndoAction: StateFlow<UndoAction?> = _lastUndoAction.asStateFlow()

    private val _revealedCardIds = MutableStateFlow<Set<String>>(emptySet())
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _scrollToId = MutableStateFlow<String?>(null)
    private val _highlightedId = MutableStateFlow<String?>(null)
    private val _linkedPopupTxs = MutableStateFlow<List<TransactionListItem>?>(null)

    private val _cardDataMap = MutableStateFlow<PersistentMap<String, CardComputeState>>(persistentMapOf())

    private var highlightClearJob: Job? = null

    private data class AccountingSnapshot(
        val accounts: List<Account>,
        val version: Long,
        val categories: List<Category>,
        val links: Map<Long, List<Long>>,
        val currency: CurrencySnapshot,
    )

    private data class CurrencySnapshot(
        val baseCode: String,
        val rates: ExchangeRateSnapshot,
        val hasLoaded: Boolean
    )

    private data class StructuralState(
        val snapshot: AccountingSnapshot,
        val hierarchy: CategoryHierarchyCache,
        val visibleGroups: List<com.osscabinet.ossWallet.domain.model.TransactionGroup>,
        val history: List<com.osscabinet.ossWallet.domain.model.TransactionWithBalance>
    )

    private data class MetadataSnapshot(
        val plannedPayments: List<PlannedPayment>,
        val budgets: List<Budget>,
        val loans: List<Loan>,
        val loanPayments: Map<Long, List<LoanPayment>>,
        val tags: List<Tag>
    )

    private data class StableAppState(
        val initialViewContext: DashboardViewContext,
        val cardsConfig: DashboardCardsConfig,
        val displayPrefs: DashboardDisplayPrefs,
        val allTemplates: List<Template>,
        val savedFilters: List<Filter>
    )

    private data class IngredientSnapshot(
        val profileId: Int,
        val structural: StructuralState,
        val metadata: MetadataSnapshot,
        val appState: StableAppState
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val ingredientFlow: Flow<IngredientSnapshot> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            val structuralFlow = combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getLatestTransactionVersion(profileId),
                categoryRepository.getAllCategories(profileId),
                transactionManager.getAllTransactionLinks(),
                combine(
                    computationRepository.selectedCurrency,
                    computationRepository.exchangeRatesSnapshot,
                    computationRepository.hasLoadedExchangeRates
                ) { code, rates, loaded -> CurrencySnapshot(code, rates, loaded) }
            ) { acc, version, cat, links, curr -> AccountingSnapshot(acc, version ?: 0L, cat, links, curr) }
            .distinctUntilChanged()
            .flatMapLatest { snapshot ->
                if (!snapshot.currency.hasLoaded) {
                    flowOf(StructuralState(snapshot, CategoryHierarchyCache.build(snapshot.categories), emptyList(), emptyList()))
                } else {
                    transactionUseCases.observeHistoryWithBalances(profileId, TransactionFilter(), snapshot.currency.baseCode, snapshot.currency.rates, DASHBOARD_TRANSACTION_FEED_LIMIT)
                        .map { result ->
                            val txs = result.getOrNull() ?: emptyList()
                            val groups = TimelineService.getGroupedData(
                                transactions = txs.take(20),
                                absoluteHistory = txs,
                                groupingType = TransactionGroupingType.BY_DAY,
                                accounts = snapshot.accounts,
                                categories = snapshot.categories,
                                allTags = emptyList(),
                                baseCurrency = snapshot.currency.baseCode,
                                exchangeRates = snapshot.currency.rates,
                                plannedPayments = emptyList(),
                                linkedTransactionIds = snapshot.links,
                                initialBalances = emptyMap()
                            )
                            StructuralState(snapshot, CategoryHierarchyCache.build(snapshot.categories), groups, txs)
                        }
                }
            }

            val metadataFlow = combine(
                plannedPaymentRepository.getActivePlannedPayments(profileId),
                budgetRepository.getAllBudgets(profileId),
                loanRepository.getAllLoans(profileId),
                loanRepository.getAllPayments(profileId),
                tagRepository.getAllTags()
            ) { pp, b, l, lp, t -> MetadataSnapshot(pp, b, l, lp.groupBy { it.loanId }, t) }
            .distinctUntilChanged()

            val stableAppStateFlow = combine(
                combine(
                    _initialViewContext,
                    cardsConfig.filter { it.cards.isNotEmpty() },
                    combine(settingsRepository.cardAnimationEnabled, settingsRepository.expandableFabEnabled, settingsRepository.recentActivitiesBreakdown, profileRepository.getAllProfiles(), settingsRepository.recordsSettings)
                    { anim, fab, breakdown, profiles, rec -> 
                        DashboardDisplayPrefs(
                            currentProfileName = profiles.find { it.id == profileId }?.name,
                            cardAnimationEnabled = anim,
                            expandableFabEnabled = fab,
                            recentActivitiesBreakdown = breakdown,
                            fastScrollSide = rec.fastScrollSide
                        )
                    }
                ) { ctx, config, prefs -> Triple(ctx, config, prefs) },
                combine(templateRepository.getAllTemplates(profileId), filterRepository.getAllFilters())
                { t, f -> Pair(t, f) }
            ) { p1, p2 -> StableAppState(p1.first, p1.second, p1.third, p2.first, p2.second) }
            .distinctUntilChanged()

            combine(structuralFlow, metadataFlow, stableAppStateFlow) { s, m, a -> IngredientSnapshot(profileId, s, m, a) }
        }
        .conflate()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val baseSuccessStateFlow: Flow<DashboardUiState.Success> = ingredientFlow.map { ingredients ->
        val s = ingredients.structural
        val m = ingredients.metadata
        val a = ingredients.appState
        val snapshot = s.snapshot
        
        val cardInput = CardInput(
            profileId = ingredients.profileId,
            transactionsVersion = snapshot.version,
            accounts = snapshot.accounts,
            categories = snapshot.categories,
            tags = m.tags,
            exchangeRates = snapshot.currency.rates,
            targetCurrency = snapshot.currency.baseCode,
            filter = TransactionFilter(TimelineRange.AllTime(excludeFuture = true)),
            screenContext = ScreenContext.DASHBOARD,
            commonFilterEnabled = false,
            plannedPayments = m.plannedPayments,
            budgets = m.budgets,
            loans = m.loans,
            loanPayments = m.loanPayments,
            categoryHierarchy = s.hierarchy
        )
        
        val feed = TransactionUiMapper.mapToUiState(TransactionListState(s.visibleGroups, s.visibleGroups.isEmpty()), snapshot.currency.baseCode)

        DashboardUiState.Success(
            profileId = ingredients.profileId,
            accounts = snapshot.accounts.filter { !it.isArchived }.toImmutableList(),
            allAccounts = snapshot.accounts.toImmutableList(),
            allTransactions = s.history.map { it.transaction }.toImmutableList(),
            recentActivitiesState = feed,
            allCategories = snapshot.categories.toImmutableList(),
            allTags = m.tags.toImmutableList(),
            baseCurrency = snapshot.currency.baseCode,
            exchangeRates = snapshot.currency.rates,
            plannedPayments = m.plannedPayments.toImmutableList(),
            budgets = m.budgets.toImmutableList(),
            loans = m.loans.toImmutableList(),
            loanPayments = m.loanPayments.mapValues { v -> v.value.toImmutableList() }.toImmutableMap(),
            initialViewContext = a.initialViewContext,
            cardsConfig = a.cardsConfig,
            displayPrefs = a.displayPrefs,
            allTemplates = a.allTemplates.toImmutableList(),
            savedFilters = a.savedFilters.toImmutableList(),
            cardInput = cardInput
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val coreUiState: Flow<DashboardUiState> = baseSuccessStateFlow
        .combine(_cardDataMap) { base, cards -> base.copy(cardDataMap = cards) }
        .onStart<DashboardUiState> { emit(DashboardUiState.Loading) }
        .catch { e -> 
            Log.e("AeonBenchmark", "CRITICAL: Dashboard pipeline crashed", e)
            emit(DashboardUiState.Error(e.message ?: "Dashboard data error")) 
        }

    private val ephemeralFlow = combine(
        _revealedCardIds,
        _snackbarMessage,
        _scrollToId,
        _highlightedId,
        _linkedPopupTxs,
        _lastUndoAction
    ) { args: Array<Any?> ->
        EphemeralState(
            revealedCardIds = args[0] as Set<String>,
            transactionSnackbarMessage = args[1] as String?,
            scrollToId = args[2] as String?,
            highlightedId = args[3] as String?,
            linkedPopupTxs = args[4] as List<TransactionListItem>?,
            lastUndoAction = args[5] as UndoAction?
        )
    }

    private data class EphemeralState(
        val revealedCardIds: Set<String>,
        val transactionSnackbarMessage: String?,
        val scrollToId: String?,
        val highlightedId: String?,
        val linkedPopupTxs: List<TransactionListItem>?,
        val lastUndoAction: UndoAction?
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(coreUiState, ephemeralFlow) { core, eph ->
        if (core !is DashboardUiState.Success) return@combine core
        core.copy(
            revealedCardIds = eph.revealedCardIds.toImmutableSet(),
            transactionSnackbarMessage = eph.transactionSnackbarMessage,
            scrollToId = eph.scrollToId,
            highlightedId = eph.highlightedId,
            linkedPopupTxs = eph.linkedPopupTxs?.toImmutableList(),
            lastUndoAction = eph.lastUndoAction
        )
    }
    .catch { e -> emit(DashboardUiState.Error(e.message ?: "Dashboard data error")) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    private fun quickConfirmById(id: Long, scheduledDate: Long) {
        val state = (uiState.value as? DashboardUiState.Success) ?: return
        val payment = state.plannedPayments.find { it.id == id } ?: return
        
        // Idempotency: Check if already confirmed for this date to prevent double-entry
        val isAlreadyConfirmed = state.allTransactions.any { 
            (it.sourcePlannedPaymentId == id) && 
            (it.timestamp >= scheduledDate) && 
            (it.timestamp < scheduledDate + 86400000)
        }
        
        if (isAlreadyConfirmed) {
            _snackbarMessage.value = "Already confirmed for this date"
            return
        }
        
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val request = TransactionRequest(
                name = payment.name,
                amount = payment.amount,
                entryMode = payment.entryMode,
                accountId = payment.accountId ?: return@launch,
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
            val txId = transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = request,
                profileId = profileId,
                sourcePlannedPaymentId = payment.id,
                scheduledDate = scheduledDate
            )
            _lastUndoAction.value = UndoAction.ConfirmUndo(
                paymentId = payment.id,
                scheduledDate = scheduledDate,
                transactionId = txId,
                prevLastTriggeredDate = payment.schedule.lastTriggeredDate
            )
        }
    }

    private fun quickSkipById(id: Long, scheduledDate: Long) {
        val state = uiState.value as? DashboardUiState.Success ?: return
        val payment = state.plannedPayments.find { it.id == id } ?: return
        viewModelScope.launch {
            transactionManager.skipPlannedPayment(payment.id, scheduledDate)
            _lastUndoAction.value = UndoAction.SkipUndo(
                paymentId = payment.id,
                scheduledDate = scheduledDate,
                prevLastTriggeredDate = payment.schedule.lastTriggeredDate
            )
        }
    }

    private fun undoLastPlannedPaymentAction() {
        val action = _lastUndoAction.value ?: return
        _lastUndoAction.value = null
        viewModelScope.launch {
            when (action) {
                is UndoAction.SkipUndo -> {
                    val occ = plannedPaymentRepository.getOccurrenceForDate(action.paymentId, action.scheduledDate)
                    occ?.let { plannedPaymentRepository.updateOccurrenceStatus(it.id, OccurrenceStatus.PENDING) }
                    val payment = plannedPaymentRepository.getById(action.paymentId) ?: return@launch
                    plannedPaymentRepository.update(
                        payment.copy(
                            schedule = payment.schedule.copy(
                                nextDueDate = action.scheduledDate,
                                lastTriggeredDate = action.prevLastTriggeredDate
                            )
                        )
                    )
                }
                is UndoAction.ConfirmUndo -> {
                    val profileId = settingsRepository.currentProfileId.first()
                    transactionManager.deleteTransaction(action.transactionId, profileId)
                    val occ = plannedPaymentRepository.getOccurrenceForDate(action.paymentId, action.scheduledDate)
                    occ?.let { plannedPaymentRepository.updateOccurrenceStatus(it.id, OccurrenceStatus.PENDING) }
                    val payment = plannedPaymentRepository.getById(action.paymentId) ?: return@launch
                    plannedPaymentRepository.update(
                        payment.copy(
                            schedule = payment.schedule.copy(
                                nextDueDate = action.scheduledDate,
                                lastTriggeredDate = action.prevLastTriggeredDate
                            )
                        )
                    )
                }
            }
        }
    }

    private fun handleJumpRequest(id: Long, availableIds: Set<Long>? = null) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val linked = transactionUseCases.getLinkedTransactions(id).first()
            val scoped = if (availableIds != null) linked.filter { it.id in availableIds } else linked
            
            if (scoped.isEmpty()) {
                _snackbarMessage.value = "No linked transactions found"
                return@launch
            }
            
            if (scoped.size == 1) {
                performJump(scoped.first().id)
            } else {
                val base = computationRepository.selectedCurrency.first()
                val rates = computationRepository.exchangeRatesSnapshot.first()
                val enriched = TimelineService.enrichTransactions(
                    scoped,
                    accountRepository.getAllAccounts(profileId).first(),
                    categoryRepository.getAllCategories(profileId).first(),
                    tagRepository.getAllTags().first(),
                    base,
                    rates
                )
                _linkedPopupTxs.value = enriched.map { TransactionUiMapper.mapRecord(it, base) }
            }
        }
    }

    private fun performJump(id: Long) {
        highlightClearJob?.cancel()
        _scrollToId.value = "tx_$id"
        _highlightedId.value = "tx_$id"
        _linkedPopupTxs.value = null
        highlightClearJob = viewModelScope.launch {
            delay(1000)
            _highlightedId.value = null
        }
    }

    private fun changeTransactionStatus(id: Long, status: com.osscabinet.ossWallet.domain.model.TransactionStatus) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            transactionManager.updateStatus(id, status, profileId)
            _snackbarMessage.value = if (status == com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED) "Marked as cleared" else "Marked as uncleared"
        }
    }

    private fun addTag(name: String, color: Int) {
        viewModelScope.launch {
            tagRepository.insertTag(Tag(name = name, colorHex = color))
        }
    }

    private fun quickCloneTransaction(id: Long) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            transactionManager.cloneTransaction(id, profileId)
            _snackbarMessage.value = "Transaction cloned"
        }
    }

    private fun quickDeleteTransaction(id: Long) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            transactionManager.deleteTransaction(id, profileId)
            _snackbarMessage.value = "Transaction deleted"
        }
    }

    private fun toggleStatusClearedUncleared(id: Long) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val tx = transactionUseCases.getTransactionById(id, profileId) ?: return@launch
            val newStatus = if (tx.status == com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED)
                com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED
            else
                com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED
            transactionManager.updateStatus(id, newStatus, profileId)
            _snackbarMessage.value = if (newStatus == com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED) "Marked as cleared" else "Marked as uncleared"
        }
    }

    fun searchTransactions(query: String): Flow<List<Transaction>> = flow {
        if (query.trim().length < 2) {
            emit(emptyList())
            return@flow
        }
        val profileId = settingsRepository.currentProfileId.first()
        emitAll(transactionUseCases.searchTransactions(profileId, query))
    }

    fun onIntent(intent: com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent) {
        when (intent) {
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.InitializeView -> _initialViewContext.value = intent.viewContext
            com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.ResetToInitialView -> _initialViewContext.value = DashboardViewContext.None
            com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.ClearViewFocus -> {
                _initialViewContext.value = DashboardViewContext.None
                _scrollToId.value = null
                _highlightedId.value = null
            }
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.QuickConfirmPlannedPayment -> quickConfirmById(intent.plannedPaymentId, intent.scheduledDate)
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.QuickSkipPlannedPayment -> quickSkipById(intent.plannedPaymentId, intent.scheduledDate)
            com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.UndoLastPlannedPaymentAction -> undoLastPlannedPaymentAction()
            com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.ClearUndoAction -> _lastUndoAction.value = null
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.OnCardExpanded -> _revealedCardIds.update { it + intent.cardId }
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.OnCardCollapsed -> _revealedCardIds.update { it - intent.cardId }
            com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.ClearSnackbar -> _snackbarMessage.value = null
            com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.ClearScrollTo -> _scrollToId.value = null
            com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.CloseLinkedPopup -> _linkedPopupTxs.value = null
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.HandleJumpRequest -> handleJumpRequest(intent.transactionId, intent.availableIds)
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.PerformJump -> performJump(intent.transactionId)
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.ToggleStatusClearedUncleared -> toggleStatusClearedUncleared(intent.transactionId)
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.QuickDeleteTransaction -> quickDeleteTransaction(intent.transactionId)
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.QuickCloneTransaction -> quickCloneTransaction(intent.transactionId)
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.AddTag -> addTag(intent.name, intent.color)
            is com.osscabinet.ossWallet.ui.screens.dashboard.DashboardIntent.ChangeTransactionStatus -> changeTransactionStatus(intent.transactionId, intent.status)
        }
    }

    init {
        // Hydrate state from cache immediately to prevent flashing
        viewModelScope.launch(dispatchers.io) {
            val profileId = settingsRepository.currentProfileId.first()
            val baseCurrency = computationRepository.selectedCurrency.first()
            
            cardRenderCache.loadProfile(profileId, baseCurrency)
            val cachedCards = cardRenderCache.getProfileCardSnapshot(profileId, baseCurrency)
            
            _cardDataMap.update { current ->
                current.toMutableMap().apply {
                    cachedCards.forEach { (id, data) -> put(id, CardComputeState.Success(data)) }
                }.toPersistentMap()
            }
            Log.d("AeonBenchmark", "VM: Cache populated in init for profile $profileId")
        }

        // Trigger computations whenever ingredients change
        viewModelScope.launch {
            baseSuccessStateFlow.collectLatest { successState ->
                val cardInput = successState.cardInput ?: return@collectLatest
                val profileId = successState.profileId
                val baseCurrency = successState.baseCurrency
                val version = cardInput.transactionsVersion

                // Persist current ground truth for faster subsequent loads
                launch(dispatchers.io) {
                    val snapshot = ScreenPersistenceEnvelope.Snapshot(
                        accounts = successState.allAccounts,
                        categories = successState.allCategories,
                        tags = successState.allTags,
                        plannedPayments = successState.plannedPayments,
                        budgets = successState.budgets,
                        loans = successState.loans,
                        baseCurrency = baseCurrency,
                        exchangeRates = successState.exchangeRates,
                        cardsConfig = successState.cardsConfig
                    )
                    cardRenderCache.persistEnvelope(profileId, baseCurrency, "dashboard_snapshot", snapshot, version)
                    cardRenderCache.persistEnvelope(profileId, baseCurrency, "dashboard_feed", ScreenPersistenceEnvelope.Feed(successState.recentActivitiesState), version)
                }

                // Batch compute all cards
                successState.cardsConfig.cards.forEach { config ->
                    launch(dispatchers.default) {
                        val result = runCatching { cardComputerRegistry.compute(config.cardType, cardInput, config) }
                            .fold(
                                onSuccess = { data ->
                                    if (data is CardRenderData.Error) CardComputeState.Error(data.message ?: "Compute error")
                                    else CardComputeState.Success(data)
                                },
                                onFailure = { CardComputeState.Error(it.message ?: "Compute error") }
                            )

                        if (result is CardComputeState.Success) {
                            cardRenderCache.persist(profileId, baseCurrency, config.cardId, result.data, version)
                        }

                        _cardDataMap.update { it.putting(config.cardId, result) }
                    }
                }
            }
        }
    }

    private companion object { const val DASHBOARD_TRANSACTION_FEED_LIMIT = 100 }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()

    @Immutable
    data class Success(
        val profileId: Int = 1,
        val accounts: ImmutableList<Account> = persistentListOf(),
        val allAccounts: ImmutableList<Account> = persistentListOf(),
        val cardDataMap: ImmutableMap<String, CardComputeState> = persistentMapOf(),
        val allTransactions: ImmutableList<Transaction> = persistentListOf(),
        val recentActivitiesState: TransactionListUiState = TransactionListUiState(),
        val allCategories: ImmutableList<Category> = persistentListOf(),
        val allTags: ImmutableList<Tag> = persistentListOf(),
        val baseCurrency: String = "USD",
        val exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot.EMPTY,
        val plannedPayments: ImmutableList<PlannedPayment> = persistentListOf(),
        val budgets: ImmutableList<Budget> = persistentListOf(),
        val loans: ImmutableList<Loan> = persistentListOf(),
        val loanPayments: ImmutableMap<Long, ImmutableList<LoanPayment>> = persistentMapOf(),
        val initialViewContext: DashboardViewContext = DashboardViewContext.None,
        val cardsConfig: DashboardCardsConfig = DashboardCardsConfig(),
        val displayPrefs: DashboardDisplayPrefs? = null,
        val allTemplates: ImmutableList<Template> = persistentListOf(),
        val savedFilters: ImmutableList<Filter> = persistentListOf(),
        val lastUndoAction: UndoAction? = null,
        val revealedCardIds: ImmutableSet<String> = persistentSetOf(),
        val transactionSnackbarMessage: String? = null,
        val scrollToId: String? = null,
        val highlightedId: String? = null,
        val linkedPopupTxs: ImmutableList<TransactionListItem>? = null,
        val cardInput: CardInput? = null
    ) : DashboardUiState()

    @Immutable
    data class Error(val message: String) : DashboardUiState()
}
