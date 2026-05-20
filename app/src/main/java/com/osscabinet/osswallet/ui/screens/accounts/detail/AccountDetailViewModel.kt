package com.osscabinet.ossWallet.ui.screens.accounts.detail

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.FilterManager
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.data.local.AccountDetailSettings
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val tagRepository: TagRepository,
    @Suppress("UnusedPrivateMember") private val registry: CardComputerRegistry
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

    fun updateTimelineRange(range: TimelineRange) {
        filterManager.setFilter("date", TransactionFilter(timelineRange = range))
    }

    fun clearFilters() {
        filterManager.clear()
        // Re-apply scope lock after clear
        filterManager.setFilter("account_scope", TransactionFilter(accountIds = setOf(accountId)))
    }

    fun addTag(name: String, color: Int) {
        viewModelScope.launch {
            tagRepository.insertTag(com.osscabinet.ossWallet.domain.model.Tag(name = name, colorHex = color))
        }
    }

    fun updateShowPlannedPayments(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings {
                it.copy(
                    transactionsSection = it.transactionsSection.copy(
                        showPlannedPayments = show
                    )
                )
            }
        }
    }

    fun updateUseNoteAsHeader(use: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings {
                it.copy(
                    transactionsSection = it.transactionsSection.copy(
                        useNoteAsHeader = use
                    )
                )
            }
        }
    }

    // cardsConfig is observed separately by the screen — removing it from this combine prevents
    // a full account data rebuild on every card filter change.
    val uiState: StateFlow<AccountDetailUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                categoryRepository.categories,
                tagRepository.getAllTags(),
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.accountDetailSettings,
                filter
            ) { args: Array<*> ->
                @Suppress("UNCHECKED_CAST")
                val accounts = args[0] as List<Account>

                @Suppress("UNCHECKED_CAST")
                val allTransactions = args[1] as List<Transaction>

                @Suppress("UNCHECKED_CAST")
                val categories = args[2] as List<Category>

                @Suppress("UNCHECKED_CAST")
                val tags = args[3] as List<com.osscabinet.ossWallet.domain.model.Tag>

                @Suppress("UNCHECKED_CAST")
                val rates = args[4] as Map<String, BigDecimal>
                val baseCurrency = args[5] as String
                val hasLoadedExchangeRates = args[6] as Boolean
                val settings = args[7] as AccountDetailSettings
                val currentFilter = args[8] as TransactionFilter

                if (!hasLoadedExchangeRates) {
                    return@combine AccountDetailUiState.Loading
                }

                val account = accounts.find { it.id == accountId }
                    ?: return@combine AccountDetailUiState.Error("Account not found")

                // All transactions for this account (Card Input)
                val accountTransactions = allTransactions.filter { it.accountId == accountId }

                // Absolute balances for this account
                val absoluteTransactionsWithBalance = transactionUseCases.getRecentTransactions(
                    allTransactions = accountTransactions,
                    accounts = listOf(account),
                    exchangeRates = rates,
                    limit = Int.MAX_VALUE,
                    excludeFuture = !settings.transactionsSection.showPlannedPayments || currentFilter.excludeFuture
                )

                // Filtered transactions for the list tab
                val filteredTransactionsWithBalance = TransactionFilterService.filterTransactionsWithBalance(
                    absoluteTransactionsWithBalance,
                    if (!settings.transactionsSection.showPlannedPayments) currentFilter.copy(excludeFuture = true) else currentFilter
                )

                val listState = TimelineService.getGroupedData(
                    transactions = filteredTransactionsWithBalance,
                    absoluteHistory = absoluteTransactionsWithBalance,
                    groupingType = settings.transactionsSection.groupBy,
                    accounts = accounts,
                    categories = categories,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates
                ).let { TransactionListState(groups = it, isEmpty = it.isEmpty()) }

                AccountDetailUiState.Success(
                    profileId = profileId,
                    account = account,
                    allAccountTransactions = accountTransactions,
                    listState = listState,
                    allCategories = categories,
                    allTags = tags,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    showPlannedPayments = settings.transactionsSection.showPlannedPayments,
                    useNoteAsHeader = settings.transactionsSection.useNoteAsHeader,
                    currentFilter = currentFilter
                )
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
