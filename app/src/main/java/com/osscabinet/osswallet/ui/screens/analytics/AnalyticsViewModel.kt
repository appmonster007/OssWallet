package com.osscabinet.ossWallet.ui.screens.analytics

import com.osscabinet.ossWallet.domain.model.timeline.DefaultTimeline
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

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

    init {
        viewModelScope.launch(dispatchers.io) {
            val profileId = settingsRepository.currentProfileId.first()
            val currency = computationRepository.selectedCurrency.first()
            cardRenderCache.kickOffProfileLoad(profileId, currency)
        }
    }

    private val _screenFilter = MutableStateFlow(TransactionFilter(timelineRange = DefaultTimeline.INITIAL))
    val screenFilter: StateFlow<TransactionFilter> = _screenFilter.asStateFlow()

    val commonFilterEnabled: StateFlow<Boolean> = settingsRepository.analyticsCommonFilterEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
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
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCardsConfig())

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
                    tagRepository.getAllTags(profileId)
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
                    accounts = domain.accounts.toImmutableList(),
                    allCategories = domain.categories.toImmutableList(),
                    allTags = domain.tags.toImmutableList(),
                    exchangeRates = config.rates.toImmutableMap(),
                    selectedCurrency = config.baseCurrency,
                    plannedPayments = planning.plannedPayments.toImmutableList(),
                    budgets = planning.budgets.toImmutableList(),
                    loans = planning.loans.toImmutableList(),
                    version = domain.version
                ) as AnalyticsUiState
            }
        }
        .catch { e -> emit(AnalyticsUiState.Error(e.message ?: "Failed to load analytics data")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsUiState.Loading
        )

    val cardDataMap: StateFlow<ImmutableMap<String, CardComputeState>> =
        combine(
            cardsConfig,
            uiState,
            _screenFilter,
            commonFilterEnabled
        ) { cards, state, sf, ce ->
            cards to Triple(state, sf, ce)
        }.flatMapLatest { (cards, stateTriple) ->
            val (state, sf, commonEnabled) = stateTriple
            if (state !is AnalyticsUiState.Success) return@flatMapLatest flowOf(persistentMapOf())

            channelFlow {
                // Seed from persistent cache — short-circuits if already loaded by DashboardViewModel.
                try {
                    cardRenderCache.awaitProfileLoaded(state.profileId, state.selectedCurrency)
                } catch (_: CancellationException) {
                    // If this coroutine is being cancelled by flatMapLatest, ensureActive() re-throws.
                    // If the Deferred was cancelled externally (e.g. profile switch), proceed with
                    // empty snapshot.
                    coroutineContext.ensureActive()
                }
                val cachedSnapshot =
                    cardRenderCache.getProfileSnapshot(state.profileId, state.selectedCurrency)

                val localCardState = MutableStateFlow(
                    persistentMapOf<String, CardComputeState>().builder().apply {
                        cards.cards.forEach { c ->
                            val cached = cachedSnapshot[c.cardId]
                            put(
                                c.cardId,
                                if (cached != null) CardComputeState.Success(cached) else CardComputeState.Loading
                            )
                        }
                    }.build()
                )

                launch { localCardState.collect { send(it) } }

                val input = withContext(dispatchers.default) {
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

                // channelFlow scope cancels all children when flatMapLatest fires a new emission,
                // so no generation counter is needed — structured concurrency handles it.
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
                            try {
                                cardRenderCache.persist(
                                    state.profileId,
                                    state.selectedCurrency,
                                    cardConfig.cardId,
                                    result.data,
                                    state.version
                                )
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                // Cache write failure — result still applied to local state below.
                            }
                        }

                        localCardState.update { it.putting(cardConfig.cardId, result) }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentMapOf())
}

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(
        val profileId: Int = 0,
        val accounts: ImmutableList<Account> = persistentListOf(),
        val allCategories: ImmutableList<Category> = persistentListOf(),
        val allTags: ImmutableList<Tag> = persistentListOf(),
        val selectedCurrency: String = "USD",
        val exchangeRates: ImmutableMap<String, BigDecimal> = persistentMapOf(),
        val plannedPayments: ImmutableList<PlannedPayment> = persistentListOf(),
        val budgets: ImmutableList<Budget> = persistentListOf(),
        val loans: ImmutableList<Loan> = persistentListOf(),
        val version: Long = 0L
    ) : AnalyticsUiState()

    data class Error(val message: String) : AnalyticsUiState()
}
