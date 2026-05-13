package com.osscabinet.ossWallet.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.service.*
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val transactionUseCases: TransactionUseCases,
    settingsRepository: SettingsRepository,
    private val computationRepository: ComputationRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val registry: CardComputerRegistry,
    override val cardRepository: CardRepository,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val budgetRepository: BudgetRepository,
    private val loanRepository: LoanRepository
) : BaseViewModel(settingsRepository), CardUpdateHandler {

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.ANALYTICS

    // Analytics cards config - now using cardRepository
    val cardsConfig: StateFlow<DashboardCardsConfig> = cardRepository.getCardsConfig(cardScreenContext)
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
                settingsRepository.decimalPrecision,
                plannedPaymentRepository.getActivePlannedPayments(profileId),
                budgetRepository.getActiveBudgets(profileId),
                loanRepository.getActiveLoans(profileId)
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
                val precision = args[6] as Int
                @Suppress("UNCHECKED_CAST")
                val plannedPayments = args[7] as List<PlannedPayment>
                @Suppress("UNCHECKED_CAST")
                val budgets = args[8] as List<Budget>
                @Suppress("UNCHECKED_CAST")
                val loans = args[9] as List<Loan>

                if (!hasLoadedRates) {
                    return@combine AnalyticsUiState.Loading
                }

                AnalyticsUiState.Success(
                    allTransactions = transactions,
                    accounts = accounts,
                    allCategories = categories,
                    exchangeRates = rates,
                    selectedCurrency = baseCurrency,
                    decimalPrecision = precision,
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
}

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(
        val allTransactions: List<Transaction> = emptyList(),
        val accounts: List<Account> = emptyList(),
        val allCategories: List<Category> = emptyList(),
        val selectedCurrency: String = "USD",
        val exchangeRates: Map<String, BigDecimal> = emptyMap(),
        val decimalPrecision: Int = 2,
        val plannedPayments: List<PlannedPayment> = emptyList(),
        val budgets: List<Budget> = emptyList(),
        val loans: List<Loan> = emptyList()
    ) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}
