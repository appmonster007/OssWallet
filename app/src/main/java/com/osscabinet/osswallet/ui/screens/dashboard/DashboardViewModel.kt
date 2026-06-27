package com.osscabinet.ossWallet.ui.screens.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.core.util.DispatcherProvider
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
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import com.osscabinet.ossWallet.ui.screens.plannedpayments.UndoAction
import com.osscabinet.ossWallet.ui.mapper.TransactionUiMapper
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListItem
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel.DashboardDisplayPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emitAll
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.flowOf
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
import kotlinx.coroutines.flow.map
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
    internal val tagRepository: TagRepository,
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

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.InitializeView -> initializeDashboardView(intent.viewContext)
            DashboardIntent.ResetToInitialView -> resetToInitialView()
            DashboardIntent.ClearViewFocus -> clearViewFocus()
            is DashboardIntent.QuickConfirmPlannedPayment -> quickConfirmById(intent.plannedPaymentId, intent.scheduledDate)
            is DashboardIntent.QuickSkipPlannedPayment -> quickSkipById(intent.plannedPaymentId, intent.scheduledDate)
            DashboardIntent.UndoLastPlannedPaymentAction -> undoLastPlannedPaymentAction()
            DashboardIntent.ClearUndoAction -> clearUndoAction()
            is DashboardIntent.CardBecameVisible -> computeCardIfNeeded(intent.cardType)
            
            // New UI Intents
            is DashboardIntent.OnCardExpanded -> _revealedCardIds.update { it + intent.cardId }
            is DashboardIntent.OnCardCollapsed -> _revealedCardIds.update { it - intent.cardId }
            DashboardIntent.ClearSnackbar -> _snackbarMessage.value = null
            DashboardIntent.ClearScrollTo -> _scrollToId.value = null
            DashboardIntent.CloseLinkedPopup -> _linkedPopupTxs.value = null
            is DashboardIntent.HandleJumpRequest -> handleJumpRequest(intent.transactionId)
            is DashboardIntent.PerformJump -> performJump(intent.transactionId)
            is DashboardIntent.ToggleStatusClearedUncleared -> toggleStatusClearedUncleared(intent.transactionId)
            
            // Story 2.2 Patch: Consolidated UI Intents
            is DashboardIntent.QuickDeleteTransaction -> quickDeleteTransaction(intent.transactionId)
            is DashboardIntent.QuickCloneTransaction -> quickCloneTransaction(intent.transactionId)
            is DashboardIntent.AddTag -> addTag(intent.name, intent.color)
            is DashboardIntent.ChangeTransactionStatus -> changeTransactionStatus(intent.transactionId, intent.status)
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
            tagRepository.insertTag(com.osscabinet.ossWallet.domain.model.Tag(name = name, colorHex = color))
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

    private fun initializeDashboardView(viewContext: DashboardViewContext) {
        _initialViewContext.value = viewContext
    }

    private fun resetToInitialView() {
        initializeDashboardView(_initialViewContext.value)
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
        .flatMapLatest { profileId: Int ->
            // 1. Accounting Pipeline
            val structuralStateFlow = combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
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
                if (!snapshot.currency.hasLoaded) {
                    kotlinx.coroutines.flow.flowOf<StructuralState?>(null)
                } else {
                    transactionUseCases.observeHistoryWithBalances(
                        profileId = profileId,
                        filter = TransactionFilter(),
                        baseCurrency = snapshot.currency.baseCode,
                        exchangeRates = snapshot.currency.rates,
                        limit = DASHBOARD_TRANSACTION_FEED_LIMIT
                    ).map { result: Result<List<com.osscabinet.ossWallet.domain.model.TransactionWithBalance>> ->
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
                        StructuralState(snapshot, CategoryHierarchyCache.build(snapshot.categories), visibleGroups)
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

            // 3. App State Pipeline (Consolidation)
            val appStateFlow = combine(
                combine(
                    _initialViewContext,
                    cardsConfig,
                    combine(
                        settingsRepository.cardAnimationEnabled,
                        settingsRepository.expandableFabEnabled,
                        settingsRepository.recentActivitiesBreakdown,
                        profileRepository.getAllProfiles()
                    ) { anim, fab, breakdown, profiles ->
                        DashboardDisplayPrefs(
                            currentProfileName = profiles.find { it.id == profileId }?.name,
                            cardAnimationEnabled = anim,
                            expandableFabEnabled = fab,
                            recentActivitiesBreakdown = breakdown
                        )
                    }
                ) { initialCtx, config, prefs ->
                    Triple(initialCtx, config, prefs)
                },
                combine(
                    templateRepository.getAllTemplates(profileId),
                    filterRepository.getAllFilters(),
                    _lastUndoAction
                ) { templates, filters, undo ->
                    Triple(templates, filters, undo)
                },
                combine(
                    _revealedCardIds,
                    _snackbarMessage,
                    _scrollToId,
                    _highlightedId,
                    _linkedPopupTxs
                ) { revealed, snackbar, scroll, highlighted, popup ->
                    Pair(revealed, Pair(snackbar, Triple(scroll, highlighted, popup)))
                }
            ) { part1, part2, part3 ->
                @Suppress("UNCHECKED_CAST")
                DashboardAppState(
                    initialViewContext = part1.first,
                    cardsConfig = part1.second,
                    displayPrefs = part1.third,
                    allTemplates = part2.first,
                    savedFilters = part2.second,
                    lastUndoAction = part2.third,
                    revealedCardIds = part3.first,
                    transactionSnackbarMessage = part3.second.first,
                    scrollToId = part3.second.second.first,
                    highlightedId = part3.second.second.second,
                    linkedPopupTxs = part3.second.second.third
                )
            }

            // 4. UI State Pipeline
            combine(structuralStateFlow, metadataFlow, appStateFlow) { s, m, a ->
                if (s == null) return@combine DashboardUiState.Loading

                val snapshot = s.snapshot
                val hierarchy = s.hierarchy
                
                // Moved CardInput construction into ViewModel (Story 2.3)
                val cardInput = CardInput(
                    profileId = profileId,
                    transactions = snapshot.transactions,
                    allProfileTransactions = snapshot.transactions,
                    accounts = snapshot.accounts,
                    categories = snapshot.categories,
                    tags = m.tags,
                    exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(snapshot.currency.baseCode, snapshot.currency.rates),
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

                val successState = DashboardUiState.Success(
                    profileId = profileId,
                    accounts = snapshot.accounts.filter { !it.isArchived },
                    allAccounts = snapshot.accounts,
                    cardDataMap = emptyMap<String, CardRenderData>().toImmutableMap(),
                    isCardsComputing = true,
                    allTransactions = snapshot.transactions,
                    recentActivitiesState = TransactionUiMapper.mapToUiState(
                        domainState = TransactionListState(s.visibleGroups, s.visibleGroups.isEmpty()),
                        baseCurrency = snapshot.currency.baseCode
                    ),
                    allCategories = snapshot.categories,
                    allTags = m.tags,
                    baseCurrency = snapshot.currency.baseCode,
                    exchangeRates = snapshot.currency.rates,
                    plannedPayments = m.plannedPayments,
                    budgets = m.budgets,
                    loans = m.loans,
                    loanPayments = m.loanPayments,
                    
                    // Consolidated fields
                    initialViewContext = a.initialViewContext,
                    cardsConfig = a.cardsConfig,
                    displayPrefs = a.displayPrefs,
                    allTemplates = a.allTemplates,
                    savedFilters = a.savedFilters,
                    lastUndoAction = a.lastUndoAction,
                    revealedCardIds = a.revealedCardIds,
                    transactionSnackbarMessage = a.transactionSnackbarMessage,
                    scrollToId = a.scrollToId,
                    highlightedId = a.highlightedId,
                    linkedPopupTxs = a.linkedPopupTxs,
                    cardInput = cardInput
                )
                successState
            }.flatMapLatest { state ->
                if (state is DashboardUiState.Loading) return@flatMapLatest flowOf(state)
                val successState = state as DashboardUiState.Success
                
                channelFlow<DashboardUiState> {
                    val configs = successState.cardsConfig.cards
                    val cachedSnapshot = cardRenderCache.getProfileSnapshot(profileId, successState.baseCurrency)
                    val cardDataFlow = MutableStateFlow(cachedSnapshot.toImmutableMap())
                    val completedCount = AtomicInteger(0)

                    launch {
                        cardDataFlow.collect { currentCards ->
                            send(
                                successState.copy(
                                    cardDataMap = currentCards,
                                    isCardsComputing = completedCount.get() < minOf(configs.size, 4) // Lazy loading threshold
                                )
                            )
                        }
                    }

                    if (configs.isEmpty()) {
                        completedCount.set(0)
                        cardDataFlow.update { it } 
                    } else {
                        // Task 7.3: Compute only the first 4 cards immediately (Story 2.3)
                        val initialCards = configs.take(4)
                        initialCards.forEach { cardConfig ->
                            launch(dispatchers.default) {
                                val result = runCatching { cardComputerRegistry.compute(cardConfig.cardType, successState.cardInput!!, cardConfig) }
                                val data = result.getOrElse { e -> CardRenderData.Error(e.message) }
                                
                                cardRenderCache[profileId, successState.baseCurrency, cardConfig.cardId] = data
                                cardDataFlow.update { (it + (cardConfig.cardId to data)).toImmutableMap() }
                                
                                completedCount.incrementAndGet()
                            }
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

    private fun computeCardIfNeeded(cardType: com.osscabinet.ossWallet.domain.card.CardType) {
        val state = uiState.value as? DashboardUiState.Success ?: return
        val config = state.cardsConfig.cards.find { it.cardType == cardType } ?: return
        val profileId = state.profileId
        val currency = state.baseCurrency
        val cardInput = state.cardInput ?: return

        if (cardRenderCache[profileId, currency, config.cardId] != null) return

        viewModelScope.launch(dispatchers.default) {
            val result = runCatching { cardComputerRegistry.compute(cardType, cardInput, config) }
            val data = result.getOrElse { e -> CardRenderData.Error(e.message) }
            cardRenderCache[profileId, currency, config.cardId] = data
            // uiState will automatically emit updated cache snapshot in its next pass or via current active flows
        }
    }

    private fun quickConfirmById(id: Long, scheduledDate: Long) {
        val state = (uiState.value as? DashboardUiState.Success) ?: return
        val payment = state.plannedPayments.find { it.id == id } ?: return
        
        // Idempotency: Check if already confirmed for this occurrence (Story 2.3 Patch)
        if (state.allTransactions.any { it.sourcePlannedPaymentId == id && it.timestamp >= scheduledDate && it.timestamp < scheduledDate + 24*60*60*1000 }) {
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
            _lastUndoAction.value = UndoAction.ConfirmUndo(payment, scheduledDate, transactionId)
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
            _lastUndoAction.value = UndoAction.SkipUndo(payment, scheduledDate)
        }
    }

    private fun undoLastPlannedPaymentAction() {
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

    private fun clearUndoAction() { _lastUndoAction.value = null }

    private fun handleJumpRequest(id: Long) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val links = transactionUseCases.getLinkedTransactionIds(id).first()
            if (links.isEmpty()) {
                _snackbarMessage.value = "No linked transactions found"
                return@launch
            }
            
            if (links.size == 1) {
                performJump(links[0])
            } else {
                val baseCurr = computationRepository.selectedCurrency.first()
                val rates = computationRepository.exchangeRates.first()
                val enriched = transactionUseCases.getLinkedTransactions(id).first()
                    .let { TimelineService.enrichTransactions(it, accountRepository.getAllAccounts(profileId).first(), categoryRepository.categories.first(), tagRepository.getAllTags().first(), baseCurr, rates) }
                
                _linkedPopupTxs.value = enriched.map { TransactionUiMapper.mapRecord(it, baseCurr) }
            }
        }
    }

    private fun performJump(id: Long) {
        viewModelScope.launch {
            _scrollToId.value = "tx_$id"
            _highlightedId.value = "tx_$id"
            _linkedPopupTxs.value = null
            delay(1000)
            if (_highlightedId.value == "tx_$id") _highlightedId.value = null
        }
    }

    private data class DashboardAppState(
        val initialViewContext: DashboardViewContext,
        val cardsConfig: DashboardCardsConfig,
        val displayPrefs: DashboardDisplayPrefs,
        val allTemplates: List<Template>,
        val savedFilters: List<Filter>,
        val lastUndoAction: UndoAction?,
        val revealedCardIds: Set<String>,
        val transactionSnackbarMessage: String?,
        val scrollToId: String?,
        val highlightedId: String?,
        val linkedPopupTxs: List<TransactionListItem>?
    )

    private companion object {
        const val DASHBOARD_TRANSACTION_FEED_LIMIT = 100
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()

    @Immutable
    data class Success(
        val profileId: Int,
        val accounts: List<Account>,
        val allAccounts: List<Account> = emptyList(),
        val cardDataMap: ImmutableMap<String, CardRenderData>,
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
        val loanPayments: Map<Long, List<LoanPayment>> = emptyMap(),
        
        // Consolidated Dashboard state
        val initialViewContext: DashboardViewContext = DashboardViewContext.None,
        val cardsConfig: DashboardCardsConfig = DashboardCardsConfig(),
        val displayPrefs: DashboardDisplayPrefs? = null,
        val allTemplates: List<Template> = emptyList(),
        val savedFilters: List<Filter> = emptyList(),
        val lastUndoAction: UndoAction? = null,
        val revealedCardIds: Set<String> = emptySet(),
        val transactionSnackbarMessage: String? = null,
        val scrollToId: String? = null,
        val highlightedId: String? = null,
        val linkedPopupTxs: List<TransactionListItem>? = null,
        val cardInput: CardInput? = null
    ) : DashboardUiState()

    @Immutable
    data class Error(val message: String) : DashboardUiState()
}
