package com.osscabinet.ossWallet.ui.screens.analytics

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
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
import com.osscabinet.ossWallet.domain.usecase.ComputeCardUseCase
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

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
    private val computeCardUseCase: ComputeCardUseCase,
) : BaseViewModel(settingsRepository), CardUpdateHandler {

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.ANALYTICS

    private val _screenFilter = MutableStateFlow(TransactionFilter())
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

    // Analytics cards config - now using cardRepository
    val cardsConfig: StateFlow<DashboardCardsConfig> =
        cardRepository.getCardsConfig(cardScreenContext)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCardsConfig())

    val uiState: StateFlow<AnalyticsUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                transactionUseCases.getTransactions(profileId),
                accountRepository.getAllAccounts(profileId),
                categoryRepository.categories,
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                plannedPaymentRepository.getActivePlannedPayments(profileId),
                budgetRepository.getActiveBudgets(profileId),
                loanRepository.getActiveLoans(profileId),
                tagRepository.getAllTags()
            ) { args: Array<Any> ->
                @Suppress("UNCHECKED_CAST")
                val transactions = args[0] as List<Transaction>

                @Suppress("UNCHECKED_CAST")
                val accounts = args[1] as List<Account>

                @Suppress("UNCHECKED_CAST")
                val categories = args[2] as List<Category>

                @Suppress("UNCHECKED_CAST")
                val rates = args[3] as Map<String, BigDecimal>
                val baseCurrency = args[4] as String
                val hasLoadedRates = args[5] as Boolean

                if (!hasLoadedRates) {
                    return@combine AnalyticsUiState.Loading
                }

                @Suppress("UNCHECKED_CAST")
                val plannedPayments = args[6] as List<PlannedPayment>

                @Suppress("UNCHECKED_CAST")
                val budgets = args[7] as List<Budget>

                @Suppress("UNCHECKED_CAST")
                val loans = args[8] as List<Loan>

                @Suppress("UNCHECKED_CAST")
                val tags = args[9] as List<Tag>

                AnalyticsUiState.Success(
                    profileId = profileId,
                    allTransactions = transactions,
                    accounts = accounts,
                    allCategories = categories,
                    allTags = tags,
                    exchangeRates = rates,
                    selectedCurrency = baseCurrency,
                    plannedPayments = plannedPayments,
                    budgets = budgets,
                    loans = loans
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsUiState.Loading
        )

    val cardDataMap: StateFlow<Map<String, CardRenderData>> =
        combine(
            cardsConfig,
            uiState,
            _screenFilter,
            commonFilterEnabled
        ) { cards, state, sf, commonEnabled ->
            if (state !is AnalyticsUiState.Success) {
                return@combine emptyMap()
            }
            val input = CardInput(
                profileId = state.profileId,
                transactions = state.allTransactions,
                accounts = state.accounts,
                categories = state.allCategories,
                exchangeRates = state.exchangeRates,
                baseCurrency = state.selectedCurrency,
                plannedPayments = state.plannedPayments,
                budgets = state.budgets,
                loans = state.loans,
                filter = if (commonEnabled) sf else TransactionFilter(timelineRange = sf.timelineRange.withExcludeFuture(sf.timelineRange.excludeFuture)),
                screenContext = ScreenContext.ANALYTICS,
                commonFilterEnabled = commonEnabled
            )
            computeCardUseCase.computeAll(cards.cards, input)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(
        val profileId: Int = 0,
        val allTransactions: List<Transaction> = emptyList(),
        val accounts: List<Account> = emptyList(),
        val allCategories: List<Category> = emptyList(),
        val allTags: List<Tag> = emptyList(),
        val selectedCurrency: String = "USD",
        val exchangeRates: Map<String, BigDecimal> = emptyMap(),
        val plannedPayments: List<PlannedPayment> = emptyList(),
        val budgets: List<Budget> = emptyList(),
        val loans: List<Loan> = emptyList()
    ) : AnalyticsUiState()

    data class Error(val message: String) : AnalyticsUiState()
}
