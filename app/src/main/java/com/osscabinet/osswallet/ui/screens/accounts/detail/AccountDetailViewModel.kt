package com.osscabinet.ossWallet.ui.screens.accounts.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.FilterManager
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    settingsRepository: SettingsRepository,
    override val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val computationRepository: ComputationRepository,
    private val tagRepository: TagRepository
) : BaseViewModel(settingsRepository), CardUpdateHandler {

    private val accountId: String = checkNotNull(savedStateHandle["accountId"])

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.ACCOUNT_DETAIL

    // Lock the account ID for this screen
    private val filterManager = FilterManager().apply {
        setFilter("account_scope", TransactionFilter(accountIds = setOf(accountId)))
    }

    // Expose merged filter as StateFlow
    val filter: StateFlow<TransactionFilter> = filterManager.mergedFilter

    val cardsConfig: StateFlow<DashboardCardsConfig> =
        cardRepository.getCardsConfig(cardScreenContext)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCardsConfig())

    fun updateFilter(newFilter: TransactionFilter) {
        // Enforce the locked account ID
        val lockedFilter = newFilter.copy(accountIds = setOf(accountId))
        filterManager.setFilter("manual", lockedFilter)
    }

    fun addTag(name: String, color: Int) {
        viewModelScope.launch {
            tagRepository.insertTag(
                com.osscabinet.ossWallet.domain.model.Tag(
                    name = name,
                    colorHex = color
                )
            )
        }
    }

    fun updateShowPlannedPayments(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateRecordsSettings {
                it.copy(showPlannedPayments = show)
            }
        }
    }

    fun updateUseNoteAsHeader(use: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateRecordsSettings {
                it.copy(useNoteAsHeader = use)
            }
        }
    }

    private data class DomainSnapshot(
        val accounts: List<Account>,
        val allTransactions: List<Transaction>,
        val categories: List<Category>,
        val tags: List<Tag>,
        val links: Map<Long, List<Long>>
    )

    private data class ConfigSnapshot(
        val rates: Map<String, BigDecimal>,
        val baseCurrency: String,
        val hasLoadedRates: Boolean,
        val settings: TransactionSectionSettings,
        val filter: TransactionFilter
    )

    // cardsConfig is observed separately by the screen — removing it from this combine prevents
    // a full account data rebuild on every card filter change.
    val uiState: StateFlow<AccountDetailUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                combine(
                    accountRepository.getAllAccounts(profileId),
                    transactionUseCases.getTransactions(profileId),
                    categoryRepository.categories,
                    tagRepository.getAllTags(),
                    transactionUseCases.getAllTransactionLinks()
                ) { acc, tx, cat, tags, links ->
                    DomainSnapshot(acc, tx, cat, tags, links)
                }.distinctUntilChanged(),
                combine(
                    computationRepository.exchangeRates,
                    computationRepository.selectedCurrency,
                    computationRepository.hasLoadedExchangeRates,
                    settingsRepository.recordsSettings,
                    filter
                ) { rates, base, loaded, settings, currentFilter ->
                    ConfigSnapshot(rates, base, loaded, settings, currentFilter)
                }.distinctUntilChanged()
            ) { domain, config ->
                domain to config
            }.flatMapLatest { (domain, config) ->
                if (!config.hasLoadedRates) {
                    kotlinx.coroutines.flow.flowOf(AccountDetailUiState.Loading)
                } else {
                    val account = domain.accounts.find { it.id == accountId }
                    if (account == null) {
                        kotlinx.coroutines.flow.flowOf(AccountDetailUiState.Error("Account not found"))
                    } else {
                        transactionUseCases.observeHistoryWithBalances(
                            profileId = profileId,
                            filter = TransactionFilter(accountIds = setOf(accountId), timelineRange = TimelineRange.AllTime(excludeFuture = false)),
                            baseCurrency = config.baseCurrency,
                            exchangeRates = config.rates
                        ).map { result ->
                            val absoluteTransactionsWithBalance = result.getOrNull() ?: emptyList<com.osscabinet.ossWallet.domain.model.TransactionWithBalance>()

                            // Filtered transactions for the list tab
                            val filteredTransactionsWithBalance =
                                com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService.filterTransactionsWithBalance(
                                    absoluteTransactionsWithBalance,
                                    config.filter
                                )

                            val initialBalancesPerGroup = domain.accounts.associate { acc ->
                                val earliestTx = absoluteTransactionsWithBalance.findLast { it.transaction.accountId == acc.id }
                                acc.id to (earliestTx?.balanceBefore ?: BigDecimal.ZERO)
                            }

                            val listState = TimelineService.getGroupedData(
                                transactions = filteredTransactionsWithBalance,
                                absoluteHistory = absoluteTransactionsWithBalance,
                                groupingType = config.settings.groupBy,
                                accounts = domain.accounts,
                                categories = domain.categories,
                                allTags = domain.tags,
                                baseCurrency = config.baseCurrency,
                                exchangeRates = config.rates,
                                linkedTransactionIds = domain.links,
                                initialBalances = initialBalancesPerGroup
                            ).let { TransactionListState(groups = it, isEmpty = it.isEmpty()) }

                            AccountDetailUiState.Success(
                                profileId = profileId,
                                account = account,
                                allAccountTransactions = domain.allTransactions.filter { it.accountId == accountId },
                                allProfileTransactions = domain.allTransactions,
                                listState = listState,
                                allCategories = domain.categories,
                                allTags = domain.tags,
                                baseCurrency = config.baseCurrency,
                                exchangeRates = config.rates,
                                showPlannedPayments = config.settings.showPlannedPayments,
                                useNoteAsHeader = config.settings.useNoteAsHeader,
                                currentFilter = config.filter
                            )
                        }.flowOn(kotlinx.coroutines.Dispatchers.Default)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountDetailUiState.Loading
        )
}

sealed class AccountDetailUiState {
    object Loading : AccountDetailUiState()
    data class Success(
        val profileId: Int,
        val account: Account,
        val allAccountTransactions: List<Transaction>,
        val allProfileTransactions: List<Transaction> = emptyList(),
        val listState: TransactionListState,
        val allCategories: List<Category> = emptyList(),
        val allTags: List<com.osscabinet.ossWallet.domain.model.Tag> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val showPlannedPayments: Boolean = false,
        val useNoteAsHeader: Boolean = false,
        val currentFilter: TransactionFilter = TransactionFilter()
    ) : AccountDetailUiState()

    data class Error(val message: String) : AccountDetailUiState()
}
