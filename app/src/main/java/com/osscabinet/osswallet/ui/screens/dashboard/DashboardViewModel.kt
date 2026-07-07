package com.osscabinet.ossWallet.ui.screens.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.core.util.DispatcherProvider
import com.osscabinet.ossWallet.domain.cache.CardRenderCache
import com.osscabinet.ossWallet.domain.card.CardComputeState
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private val _revealedCardIds = MutableStateFlow<Set<String>>(emptySet())
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _scrollToId = MutableStateFlow<String?>(null)
    private val _highlightedId = MutableStateFlow<String?>(null)
    private val _linkedPopupTxs = MutableStateFlow<List<TransactionListItem>?>(null)

    // Job for clearing highlight after performJump — cancelled on rapid repeat calls
    private var highlightClearJob: Job? = null

    // Ephemeral state (snackbar, scroll, highlight, popup, undo) merged OUTSIDE flatMapLatest
    // to prevent any ephemeral change from restarting card computation.
    private data class EphemeralUiState(
        val revealedCardIds: Set<String> = emptySet(),
        val transactionSnackbarMessage: String? = null,
        val scrollToId: String? = null,
        val highlightedId: String? = null,
        val linkedPopupTxs: List<TransactionListItem>? = null,
        val lastUndoAction: UndoAction? = null
    )

    private val _ephemeralState = combine(
        _revealedCardIds,
        _snackbarMessage,
        _scrollToId,
        combine(_highlightedId, _linkedPopupTxs, _lastUndoAction) { h, p, u -> Triple(h, p, u) }
    ) { revealed, snackbar, scroll, rest ->
        EphemeralUiState(
            revealedCardIds = revealed,
            transactionSnackbarMessage = snackbar,
            scrollToId = scroll,
            highlightedId = rest.first,
            linkedPopupTxs = rest.second,
            lastUndoAction = rest.third
        )
    }

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.InitializeView -> initializeDashboardView(intent.viewContext)
            DashboardIntent.ResetToInitialView -> resetToInitialView()
            DashboardIntent.ClearViewFocus -> clearViewFocus()
            is DashboardIntent.QuickConfirmPlannedPayment -> quickConfirmById(
                intent.plannedPaymentId,
                intent.scheduledDate
            )

            is DashboardIntent.QuickSkipPlannedPayment -> quickSkipById(
                intent.plannedPaymentId,
                intent.scheduledDate
            )

            DashboardIntent.UndoLastPlannedPaymentAction -> undoLastPlannedPaymentAction()
            DashboardIntent.ClearUndoAction -> clearUndoAction()

            // New UI Intents
            is DashboardIntent.OnCardExpanded -> _revealedCardIds.update { it + intent.cardId }
            is DashboardIntent.OnCardCollapsed -> _revealedCardIds.update { it - intent.cardId }
            DashboardIntent.ClearSnackbar -> _snackbarMessage.value = null
            DashboardIntent.ClearScrollTo -> _scrollToId.value = null
            DashboardIntent.CloseLinkedPopup -> _linkedPopupTxs.value = null
            is DashboardIntent.HandleJumpRequest -> handleJumpRequest(
                intent.transactionId,
                intent.availableIds
            )

            is DashboardIntent.PerformJump -> performJump(intent.transactionId)
            is DashboardIntent.ToggleStatusClearedUncleared -> toggleStatusClearedUncleared(intent.transactionId)

            // Story 2.2 Patch: Consolidated UI Intents
            is DashboardIntent.QuickDeleteTransaction -> quickDeleteTransaction(intent.transactionId)
            is DashboardIntent.QuickCloneTransaction -> quickCloneTransaction(intent.transactionId)
            is DashboardIntent.AddTag -> addTag(intent.name, intent.color)
            is DashboardIntent.ChangeTransactionStatus -> changeTransactionStatus(
                intent.transactionId,
                intent.status
            )
        }
    }

    private fun changeTransactionStatus(
        id: Long,
        status: com.osscabinet.ossWallet.domain.model.TransactionStatus
    ) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            transactionManager.updateStatus(id, status, profileId)
            _snackbarMessage.value =
                if (status == com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED) "Marked as cleared" else "Marked as uncleared"
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
            val newStatus =
                if (tx.status == com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED)
                    com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED
                else
                    com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED
            transactionManager.updateStatus(id, newStatus, profileId)
            _snackbarMessage.value =
                if (newStatus == com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED) "Marked as cleared" else "Marked as uncleared"
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

    private fun initializeDashboardView(viewContext: DashboardViewContext) {
        _initialViewContext.value = viewContext
    }

    private fun resetToInitialView() {
        _initialViewContext.value = DashboardViewContext.None
    }

    private fun clearViewFocus() {
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
        val rates: ExchangeRateSnapshot,
        val hasLoaded: Boolean
    )

    /**
     * Pre-computed structural feed state (balances + grouping).
     */
    private data class StructuralState(
        val snapshot: AccountingSnapshot,
        val hierarchy: CategoryHierarchyCache,
        val visibleGroups: List<com.osscabinet.ossWallet.domain.model.TransactionGroup>
    )

    /**
     * Snapshot of metadata and settings used for enrichment and cards.
     */
    private data class MetadataSnapshot(
        val plannedPayments: List<PlannedPayment>,
        val budgets: List<Budget>,
        val loans: List<Loan>,
        val loanPayments: Map<Long, List<LoanPayment>>,
        val tags: List<Tag>
    )

    /**
     * Stable app-level settings that drive card computation restarts when they change.
     * Does NOT include ephemeral UI state (snackbar, scroll, highlight, popup, undo).
     */
    private data class StableAppState(
        val initialViewContext: DashboardViewContext,
        val cardsConfig: DashboardCardsConfig,
        val displayPrefs: DashboardDisplayPrefs,
        val allTemplates: List<Template>,
        val savedFilters: List<Filter>
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val coreUiState: Flow<DashboardUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId: Int ->
            // 1. Accounting Pipeline
            val structuralStateFlow = combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                categoryRepository.getAllCategories(profileId),
                transactionManager.getAllTransactionLinks(),
                combine(
                    computationRepository.selectedCurrency,
                    computationRepository.exchangeRatesSnapshot,
                    computationRepository.hasLoadedExchangeRates
                ) { code, rates, loaded -> CurrencySnapshot(code, rates, loaded) }
            ) { acc, tx, cat, links, curr ->
                AccountingSnapshot(acc, tx, cat, links, curr)
            }.distinctUntilChanged().flatMapLatest { snapshot ->
                if (!snapshot.currency.hasLoaded) {
                    kotlinx.coroutines.flow.flowOf<StructuralState?>(null)
                } else {
                    transactionUseCases.observeHistoryWithBalances(
                        profileId = profileId,
                        filter = TransactionFilter(),
                        baseCurrency = snapshot.currency.baseCode,
                        exchangeRates = snapshot.currency.rates,
                        limit = DASHBOARD_TRANSACTION_FEED_LIMIT
                    )
                        .map { result: Result<List<com.osscabinet.ossWallet.domain.model.TransactionWithBalance>> ->
                            val transactionsWithBalance = result.getOrNull() ?: emptyList()

                            val visibleGroups = TimelineService.getGroupedData(
                                transactions = transactionsWithBalance.take(20),
                                absoluteHistory = transactionsWithBalance,
                                groupingType = TransactionGroupingType.BY_DAY,
                                accounts = snapshot.accounts,
                                categories = snapshot.categories,
                                allTags = emptyList(),
                                baseCurrency = snapshot.currency.baseCode,
                                exchangeRates = snapshot.currency.rates,
                                linkedTransactionIds = snapshot.links,
                                initialBalances = emptyMap()
                            )
                            StructuralState(
                                snapshot,
                                CategoryHierarchyCache.build(snapshot.categories),
                                visibleGroups
                            )
                        }.flowOn(dispatchers.default)
                }
            }

            // 2. Metadata Pipeline
            val metadataFlow = combine(
                plannedPaymentRepository.getActivePlannedPayments(profileId),
                budgetRepository.getAllBudgets(profileId),
                loanRepository.getAllLoans(profileId),
                loanRepository.getAllPayments(profileId),
                tagRepository.getAllTags()
            ) { pp, b, l, lp, t ->
                MetadataSnapshot(pp, b, l, lp.groupBy { it.loanId }, t)
            }.distinctUntilChanged()

            // 3. Stable App State — excludes ephemeral UI state to prevent card computation restarts
            val stableAppStateFlow = combine(
                combine(
                    _initialViewContext,
                    cardsConfig,
                    combine(
                        settingsRepository.cardAnimationEnabled,
                        settingsRepository.expandableFabEnabled,
                        settingsRepository.recentActivitiesBreakdown,
                        profileRepository.getAllProfiles(),
                        settingsRepository.recordsSettings
                    ) { anim, fab, breakdown, profiles, records ->
                        DashboardDisplayPrefs(
                            currentProfileName = profiles.find { it.id == profileId }?.name,
                            cardAnimationEnabled = anim,
                            expandableFabEnabled = fab,
                            recentActivitiesBreakdown = breakdown,
                            fastScrollSide = records.fastScrollSide
                        )
                    }
                ) { initialCtx, config, prefs ->
                    Triple(initialCtx, config, prefs)
                },
                combine(
                    templateRepository.getAllTemplates(profileId),
                    filterRepository.getAllFilters()
                ) { templates, filters -> Pair(templates, filters) }
            ) { part1, part2 ->
                StableAppState(
                    initialViewContext = part1.first,
                    cardsConfig = part1.second,
                    displayPrefs = part1.third,
                    allTemplates = part2.first,
                    savedFilters = part2.second
                )
            }

            // 4. Build Success state and launch card computation
            combine(structuralStateFlow, metadataFlow, stableAppStateFlow) { s, m, a ->
                if (s == null) return@combine DashboardUiState.Loading

                val snapshot = s.snapshot
                val hierarchy = s.hierarchy

                val cardInput = CardInput(
                    profileId = profileId,
                    transactions = snapshot.transactions,
                    allProfileTransactions = snapshot.transactions,
                    accounts = snapshot.accounts,
                    categories = snapshot.categories,
                    tags = m.tags,
                    exchangeRates = snapshot.currency.rates,
                    targetCurrency = snapshot.currency.baseCode,
                    plannedPayments = m.plannedPayments,
                    budgets = m.budgets,
                    loans = m.loans,
                    loanPayments = m.loanPayments,
                    filter = TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = true)),
                    screenContext = ScreenContext.DASHBOARD,
                    commonFilterEnabled = false,
                    categoryHierarchy = hierarchy
                )

                DashboardUiState.Success(
                    profileId = profileId,
                    accounts = snapshot.accounts.filter { !it.isArchived }.toImmutableList(),
                    allAccounts = snapshot.accounts.toImmutableList(),
                    cardDataMap = persistentMapOf(),
                    allTransactions = snapshot.transactions.toImmutableList(),
                    recentActivitiesState = TransactionUiMapper.mapToUiState(
                        domainState = TransactionListState(
                            s.visibleGroups,
                            s.visibleGroups.isEmpty()
                        ),
                        baseCurrency = snapshot.currency.baseCode
                    ),
                    allCategories = snapshot.categories.toImmutableList(),
                    allTags = m.tags.toImmutableList(),
                    baseCurrency = snapshot.currency.baseCode,
                    exchangeRates = snapshot.currency.rates,
                    plannedPayments = m.plannedPayments.toImmutableList(),
                    budgets = m.budgets.toImmutableList(),
                    loans = m.loans.toImmutableList(),
                    loanPayments = m.loanPayments.mapValues { it.value.toImmutableList() }
                        .toImmutableMap(),
                    initialViewContext = a.initialViewContext,
                    cardsConfig = a.cardsConfig,
                    displayPrefs = a.displayPrefs,
                    allTemplates = a.allTemplates.toImmutableList(),
                    savedFilters = a.savedFilters.toImmutableList(),
                    cardInput = cardInput
                )
            }.flatMapLatest { state ->
                if (state is DashboardUiState.Loading) return@flatMapLatest flowOf(state)
                val successState = state as DashboardUiState.Success

                channelFlow<DashboardUiState> {
                    val configs = successState.cardsConfig.cards

                    // Seed from cache so recomputations (e.g. after saving a transaction) show
                    // stale-but-valid data immediately instead of flashing Loading cards.
                    val cachedSnapshot = cardRenderCache.getProfileSnapshot(
                        successState.profileId,
                        successState.baseCurrency
                    )
                    val localCardState = MutableStateFlow<PersistentMap<String, CardComputeState>>(
                        persistentMapOf<String, CardComputeState>().builder().apply {
                            configs.forEach { c ->
                                val cached = cachedSnapshot[c.cardId]
                                put(
                                    c.cardId,
                                    if (cached != null) CardComputeState.Success(cached) else CardComputeState.Loading
                                )
                            }
                        }.build()
                    )

                    launch {
                        localCardState.collect { cards ->
                            send(successState.copy(cardDataMap = cards))
                        }
                    }

                    configs.forEach { cardConfig ->
                        launch(dispatchers.default) {
                            val cardInput = successState.cardInput ?: run {
                                localCardState.update {
                                    it.putting(
                                        cardConfig.cardId,
                                        CardComputeState.Error("Card input unavailable")
                                    )
                                }
                                return@launch
                            }
                            val state = runCatching {
                                cardComputerRegistry.compute(
                                    cardConfig.cardType,
                                    cardInput,
                                    cardConfig
                                )
                            }.fold(
                                onSuccess = { data ->
                                    if (data is CardRenderData.Error) CardComputeState.Error(
                                        data.message ?: "Compute error"
                                    )
                                    else CardComputeState.Success(data)
                                },
                                onFailure = {
                                    CardComputeState.Error(
                                        it.message ?: "Compute error"
                                    )
                                }
                            )
                            if (state is CardComputeState.Success) {
                                cardRenderCache[successState.profileId, successState.baseCurrency, cardConfig.cardId] =
                                    state.data
                            }
                            localCardState.update { it.putting(cardConfig.cardId, state) }
                        }
                    }
                }
            }
        }
        .catch { e -> emit(DashboardUiState.Error(e.message ?: "Dashboard data error")) }

    // Merge ephemeral state (snackbar, scroll, revealed cards, popup, undo) OUTSIDE flatMapLatest
    // so none of these changes restart card computation.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(coreUiState, _ephemeralState) { core, eph ->
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

        // Idempotency: Check if already confirmed for this occurrence (Story 2.3 Patch)
        if (state.allTransactions.any { it.sourcePlannedPaymentId == id && it.timestamp >= scheduledDate && it.timestamp < scheduledDate + 24 * 60 * 60 * 1000 }) {
            _snackbarMessage.value = "Already confirmed for this date"
            return
        }

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
            _lastUndoAction.value = UndoAction.ConfirmUndo(
                paymentId = payment.id,
                scheduledDate = scheduledDate,
                transactionId = transactionId,
                prevLastTriggeredDate = payment.schedule.lastTriggeredDate
            )
        }
    }

    private fun quickSkipById(id: Long, scheduledDate: Long) {
        val state = uiState.value as? DashboardUiState.Success ?: return
        val payment = state.plannedPayments.find { it.id == id } ?: return
        viewModelScope.launch {
            transactionManager.skipPlannedPayment(
                paymentId = payment.id,
                scheduledDate = scheduledDate
            )
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
                    val occ = plannedPaymentRepository.getOccurrenceForDate(
                        action.paymentId,
                        action.scheduledDate
                    )
                    if (occ != null) plannedPaymentRepository.updateOccurrenceStatus(
                        occ.id,
                        OccurrenceStatus.PENDING
                    )
                    val payment =
                        plannedPaymentRepository.getById(action.paymentId) ?: return@launch
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
                    val occ = plannedPaymentRepository.getOccurrenceForDate(
                        action.paymentId,
                        action.scheduledDate
                    )
                    if (occ != null) plannedPaymentRepository.updateOccurrenceStatus(
                        occ.id,
                        OccurrenceStatus.PENDING
                    )
                    val payment =
                        plannedPaymentRepository.getById(action.paymentId) ?: return@launch
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

    private fun clearUndoAction() {
        _lastUndoAction.value = null
    }

    private fun handleJumpRequest(id: Long, availableIds: Set<Long>? = null) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            // Single call to avoid TOCTOU between size check and data fetch
            val linked = transactionUseCases.getLinkedTransactions(id).first()
            val scopedLinked =
                if (availableIds != null) linked.filter { it.id in availableIds } else linked
            if (scopedLinked.isEmpty()) {
                _snackbarMessage.value = "No linked transactions found"
                return@launch
            }

            if (scopedLinked.size == 1) {
                performJump(scopedLinked.first().id)
            } else {
                val baseCurr = computationRepository.selectedCurrency.first()
                val rates = computationRepository.exchangeRatesSnapshot.first()
                val enriched = TimelineService.enrichTransactions(
                    scopedLinked,
                    accountRepository.getAllAccounts(profileId).first(),
                    categoryRepository.getAllCategories(profileId).first(),
                    tagRepository.getAllTags().first(),
                    baseCurr,
                    rates
                )
                _linkedPopupTxs.value = enriched.map { TransactionUiMapper.mapRecord(it, baseCurr) }
            }
        }
    }

    private fun performJump(id: Long) {
        // Cancel any previous highlight-clear job to handle rapid repeat calls correctly
        highlightClearJob?.cancel()
        _scrollToId.value = "tx_$id"
        _highlightedId.value = "tx_$id"
        _linkedPopupTxs.value = null
        highlightClearJob = viewModelScope.launch {
            delay(1000)
            _highlightedId.value = null
        }
    }

    private companion object {
        const val DASHBOARD_TRANSACTION_FEED_LIMIT = 100
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()

    @Immutable
    data class Success(
        val profileId: Int,
        val accounts: ImmutableList<Account>,
        val allAccounts: ImmutableList<Account> = persistentListOf(),
        val cardDataMap: ImmutableMap<String, CardComputeState>,
        val allTransactions: ImmutableList<Transaction>,
        val recentActivitiesState: TransactionListUiState,
        val allCategories: ImmutableList<Category>,
        val allTags: ImmutableList<Tag> = persistentListOf(),
        val baseCurrency: String,
        val exchangeRates: ExchangeRateSnapshot,
        val plannedPayments: ImmutableList<PlannedPayment> = persistentListOf(),
        val budgets: ImmutableList<Budget> = persistentListOf(),
        val loans: ImmutableList<Loan> = persistentListOf(),
        val loanPayments: ImmutableMap<Long, ImmutableList<LoanPayment>> = persistentMapOf(),

        // Consolidated Dashboard state
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
