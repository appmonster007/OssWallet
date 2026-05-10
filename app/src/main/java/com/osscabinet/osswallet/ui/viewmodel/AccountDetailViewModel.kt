package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.FilterManager
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModel(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    private val settingsRepository: SettingsRepository,
    override val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val computationRepository: ComputationRepository,
    @Suppress("UnusedPrivateMember") private val registry: CardComputerRegistry
) : ViewModel(), CardUpdateHandler {

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.ACCOUNT_DETAIL

    // Lock the account ID for this screen
    private val filterManager = FilterManager().apply {
        setFilter("account_scope", TransactionFilter(accountIds = listOf(accountId)))
    }

    // Expose merged filter as StateFlow
    val filter: StateFlow<TransactionFilter> = filterManager.mergedFilter

    val cardsConfig: StateFlow<DashboardCardsConfig> =
        cardRepository.getCardsConfig(cardScreenContext)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCardsConfig())

    fun updateFilter(newFilter: TransactionFilter) {
        // Enforce the locked account ID
        val lockedFilter = newFilter.copy(accountIds = listOf(accountId))
        filterManager.setFilter("manual", lockedFilter)
    }

    fun updateTimelineRange(range: TimelineRange) {
        filterManager.setFilter("date", TransactionFilter(timelineRange = range))
    }

    fun clearFilters() {
        filterManager.clear()
        // Re-apply scope lock after clear
        filterManager.setFilter("account_scope", TransactionFilter(accountIds = listOf(accountId)))
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
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.decimalPrecision,
                settingsRepository.use24HourFormat,
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
                val rates = args[3] as Map<String, BigDecimal>
                val baseCurrency = args[4] as String
                val hasLoadedExchangeRates = args[5] as Boolean
                val precision = args[6] as Int
                val use24Hour = args[7] as Boolean
                val settings = args[8] as com.osscabinet.ossWallet.data.local.AccountDetailSettings
                val currentFilter = args[9] as TransactionFilter

                if (!hasLoadedExchangeRates) {
                    return@combine AccountDetailUiState.Loading
                }

                val account = accounts.find { it.id == accountId }
                    ?: return@combine AccountDetailUiState.Error("Account not found")

                // All transactions for this account (Card Input)
                val accountTransactions = allTransactions.filter { it.accountId == accountId }

                // Filtered transactions for the list tab
                var filteredTransactions = TransactionFilterService.filter(accountTransactions, currentFilter)

                if (!settings.transactionsSection.showPlannedPayments) {
                    val now = System.currentTimeMillis()
                    filteredTransactions = filteredTransactions.filter { it.timestamp <= now }
                }

                val transactionsWithBalance = computeRunningBalance(
                    transactions = filteredTransactions,
                    accountCurrency = account.currencyCode,
                    exchangeRates = rates
                )

                AccountDetailUiState.Success(
                    profileId = profileId,
                    account = account,
                    allAccountTransactions = accountTransactions,
                    transactionsWithBalance = transactionsWithBalance,
                    allCategories = categories,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    decimalPrecision = precision,
                    use24HourFormat = use24Hour,
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

    private fun computeRunningBalance(
        transactions: List<Transaction>,
        accountCurrency: String,
        exchangeRates: Map<String, BigDecimal>
    ): List<TransactionWithBalance> {
        val sorted = transactions.sortedByDescending { it.timestamp }
        val totalBalance = sorted.fold(BigDecimal.ZERO) { acc, tx ->
            acc + CurrencyService.convertAmount(
                tx.amount,
                tx.currency,
                accountCurrency,
                exchangeRates,
                10
            )
        }
        var currentBalance = totalBalance
        return sorted.map { tx ->
            val converted = CurrencyService.convertAmount(
                tx.amount,
                tx.currency,
                accountCurrency,
                exchangeRates,
                10
            )
            val balanceAfter = currentBalance
            val balanceBefore = currentBalance - converted
            currentBalance = balanceBefore
            TransactionWithBalance(tx, balanceBefore, balanceAfter)
        }
    }
}

sealed class AccountDetailUiState {
    object Loading : AccountDetailUiState()
    data class Success(
        val profileId: Int,
        val account: Account,
        val allAccountTransactions: List<Transaction>,
        val transactionsWithBalance: List<TransactionWithBalance> = emptyList(),
        val allCategories: List<Category> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val decimalPrecision: Int,
        val use24HourFormat: Boolean = false,
        val showPlannedPayments: Boolean = false,
        val useNoteAsHeader: Boolean = false,
        val currentFilter: TransactionFilter = TransactionFilter()
    ) : AccountDetailUiState()

    data class Error(val message: String) : AccountDetailUiState()
}
