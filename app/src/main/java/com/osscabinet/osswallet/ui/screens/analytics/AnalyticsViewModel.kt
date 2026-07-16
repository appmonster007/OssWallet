package com.osscabinet.ossWallet.ui.screens.analytics

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.core.util.DispatcherProvider
import com.osscabinet.ossWallet.domain.cache.CardRenderCache
import com.osscabinet.ossWallet.domain.card.AtelierTab
import com.osscabinet.ossWallet.domain.card.CardComputeState
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.card.getAtelierTab
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
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
import com.osscabinet.ossWallet.domain.service.category.CategoryHierarchyCache
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases,
    settingsRepository: SettingsRepository,
    private val computationRepository: ComputationRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    override val cardRepository: CardRepository,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val budgetRepository: BudgetRepository,
    private val loanRepository: LoanRepository,
    private val tagRepository: TagRepository,
    private val cardRenderCache: CardRenderCache,
    private val cardComputerRegistry: CardComputerRegistry,
    private val dispatchers: DispatcherProvider,
) : BaseViewModel(settingsRepository), CardUpdateHandler {

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.ANALYTICS

    private val _screenFilter = MutableStateFlow(TransactionFilter())
    val screenFilter: StateFlow<TransactionFilter> = _screenFilter.asStateFlow()

    val commonFilterEnabled: StateFlow<Boolean> = settingsRepository.analyticsCommonFilterEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = true,
        )

    fun updateScreenFilter(filter: TransactionFilter) {
        _screenFilter.value = filter
    }

    fun toggleCommonFilter(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveAnalyticsCommonFilterEnabled(enabled)
            if (enabled) {
                cardRepository.resetAllCardFilters(cardScreenContext)
            }
        }
    }

    /**
     * Reorders cards within a specific tab. This implementation isolates the reordering
     * logic to the specified tab, ensuring that changes do not affect the relative order
     * of cards in other tabs. It also handles edge cases like concurrent deletions.
     */
    fun reorderCardsInTab(tabName: String, reorderedSubset: List<CardConfig>) {
        viewModelScope.launch {
            // Use the latest repository state to avoid race conditions
            val currentCards = cardRepository.getCardsConfig(cardScreenContext).first().cards
            val groups = currentCards.groupBy { it.cardType.getAtelierTab().displayName }
            val updatedGroups = groups.toMutableMap()

            val existingTabCards = groups[tabName] ?: emptyList()
            val existingIds = existingTabCards.map { it.cardId }.toSet()

            // 1. Only keep cards that actually belong to this tab in the current DB state
            val validReordered = reorderedSubset.filter { it.cardId in existingIds }

            // 2. Identify cards that were in the tab but missing from the UI subset (safety)
            val reorderedIds = validReordered.map { it.cardId }.toSet()
            val missing = existingTabCards.filter { it.cardId !in reorderedIds }

            // 3. Update the specific tab group
            updatedGroups[tabName] = validReordered + missing

            // 4. Flatten back into a global list using deterministic Tab order.
            // This groups all cards by tab in the database, isolating their relative order.
            val newFullList = AtelierTab.entries.flatMap { tab ->
                updatedGroups[tab.displayName] ?: emptyList()
            }

            cardRepository.reorderCards(cardScreenContext, newFullList)
        }
    }

    val cardsConfig: StateFlow<DashboardCardsConfig> =
        cardRepository.getCardsConfig(cardScreenContext)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DashboardCardsConfig())

    private data class DomainSnapshot(
        val version: Long,
        val accounts: List<Account>,
        val categories: List<Category>,
        val tags: List<Tag>
    )

    private data class PlanningSnapshot(
        val plannedPayments: List<PlannedPayment>,
        val budgets: List<Budget>,
        val loans: List<Loan>
    )

    private data class ConfigSnapshot(
        val rates: Map<String, BigDecimal>,
        val baseCurrency: String,
        val hasLoadedRates: Boolean
    )

    val uiState: StateFlow<AnalyticsUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                combine(
                    transactionUseCases.getLatestTransactionVersion(profileId),
                    accountRepository.getAllAccounts(profileId),
                    categoryRepository.getAllCategories(profileId),
                    tagRepository.getAllTags()
                ) { version, acc, cat, tags ->
                    DomainSnapshot(
                        version ?: 0L,
                        acc,
                        cat,
                        tags
                    )
                }.distinctUntilChanged(),
                combine(
                    plannedPaymentRepository.getActivePlannedPayments(profileId),
                    budgetRepository.getActiveBudgets(profileId),
                    loanRepository.getActiveLoans(profileId)
                ) { pp, b, l -> PlanningSnapshot(pp, b, l) }.distinctUntilChanged(),
                combine(
                    computationRepository.exchangeRates,
                    computationRepository.selectedCurrency,
                    computationRepository.hasLoadedExchangeRates
                ) { rates, base, loaded ->
                    ConfigSnapshot(
                        rates,
                        base,
                        loaded
                    )
                }.distinctUntilChanged()
            ) { domain, planning, config ->
                AnalyticsUiState.Success(
                    profileId = profileId,
                    accounts = domain.accounts,
                    allCategories = domain.categories,
                    allTags = domain.tags,
                    exchangeRates = config.rates,
                    selectedCurrency = config.baseCurrency,
                    plannedPayments = planning.plannedPayments,
                    budgets = planning.budgets,
                    loans = planning.loans,
                    version = domain.version
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = AnalyticsUiState.Loading
        )

    private val _cardDataMap =
        MutableStateFlow<PersistentMap<String, CardComputeState>>(persistentMapOf())
    val cardDataMap: StateFlow<ImmutableMap<String, CardComputeState>> = _cardDataMap.asStateFlow()

    // Generation counter: guards against stale writes from CPU-bound children that run to
    // completion after coroutineScope is cancelled (cooperative cancellation, no suspension points).
    private val _computeGeneration = AtomicInteger(0)

    init {
        viewModelScope.launch {
            // D4: 4-arg combine replaces two nested combine operators, eliminating the intermediate flow.
            combine(
                cardsConfig,
                uiState,
                _screenFilter,
                commonFilterEnabled
            ) { cards, state, sf, ce ->
                cards to Triple(state, sf, ce)
            }.collectLatest { (cards, stateTriple) ->
                val (state, sf, commonEnabled) = stateTriple

                if (state !is AnalyticsUiState.Success) {
                    _cardDataMap.value = persistentMapOf()
                    return@collectLatest
                }

                val myGen = _computeGeneration.incrementAndGet()

                // D2: Seed from persistent cache to enable instant cold starts
                cardRenderCache.loadProfile(state.profileId, state.selectedCurrency)
                val cachedSnapshot = cardRenderCache.getProfileSnapshot(state.profileId, state.selectedCurrency)

                val prevMap = _cardDataMap.value
                _cardDataMap.value =
                    persistentMapOf<String, CardComputeState>().builder().apply {
                        cards.cards.forEach { c ->
                            val cached = cachedSnapshot[c.cardId]
                            put(
                                c.cardId,
                                prevMap[c.cardId] ?: if (cached != null) CardComputeState.Success(cached) else CardComputeState.Loading
                            )
                        }
                    }.build()

                // AC7: Build inputs on Default — build hierarchy once per round
                val input = kotlinx.coroutines.withContext(dispatchers.default) {
                    CardInput(
                        profileId = state.profileId,
                        transactionsVersion = state.version,
                        accounts = state.accounts,
                        categories = state.allCategories,
                        exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(
                            anchor = state.selectedCurrency,
                            rates = state.exchangeRates
                        ),
                        targetCurrency = state.selectedCurrency,
                        plannedPayments = state.plannedPayments,
                        budgets = state.budgets,
                        loans = state.loans,
                        filter = if (commonEnabled) sf else TransactionFilter(
                            timelineRange = sf.timelineRange.withExcludeFuture(
                                sf.timelineRange.excludeFuture
                            )
                        ),
                        screenContext = ScreenContext.ANALYTICS,
                        commonFilterEnabled = commonEnabled,
                        categoryHierarchy = CategoryHierarchyCache.build(state.allCategories)
                    )
                }

                // D1+P2: supervisorScope lets cards fail independently without cancelling siblings.
                // Children re-throw CancellationException so cooperative cancellation works correctly
                // when collectLatest cancels this block on a new emission.
                supervisorScope {
                    cards.cards.forEach { cardConfig ->
                        launch(dispatchers.default) {
                            val result = try {
                                val data = cardComputerRegistry.compute(
                                    cardConfig.cardType,
                                    input,
                                    cardConfig
                                )
                                if (data is CardRenderData.Error) CardComputeState.Error(
                                    data.message ?: "Compute error"
                                )
                                else CardComputeState.Success(data)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                CardComputeState.Error(e.message ?: "Compute error")
                            }

                            if (result is CardComputeState.Success) {
                                cardRenderCache.persist(
                                    state.profileId,
                                    state.selectedCurrency,
                                    cardConfig.cardId,
                                    result.data,
                                    state.version
                                )
                            }

                            // P1: check inside update{} to eliminate TOCTOU window.
                            _cardDataMap.update { map ->
                                if (_computeGeneration.get() == myGen) map.putting(
                                    cardConfig.cardId,
                                    result
                                ) else map
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(
        val profileId: Int = 0,
        val accounts: List<Account> = emptyList(),
        val allCategories: List<Category> = emptyList(),
        val allTags: List<Tag> = emptyList(),
        val selectedCurrency: String = "USD",
        val exchangeRates: Map<String, BigDecimal> = emptyMap(),
        val plannedPayments: List<PlannedPayment> = emptyList(),
        val budgets: List<Budget> = emptyList(),
        val loans: List<Loan> = emptyList(),
        val version: Long = 0L
    ) : AnalyticsUiState()

    data class Error(val message: String) : AnalyticsUiState()
}
