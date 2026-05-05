package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.service.CurrencyService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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

    val cardsConfig: StateFlow<DashboardCardsConfig> = cardRepository.getCardsConfig(cardScreenContext)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCardsConfig())

    fun updateShowPlannedPayments(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(transactionsSection = it.transactionsSection.copy(showPlannedPayments = show)) }
        }
    }

    fun updateUseNoteAsHeader(use: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(transactionsSection = it.transactionsSection.copy(useNoteAsHeader = use)) }
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
                settingsRepository.accountDetailSettings
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

                if (!hasLoadedExchangeRates) {
                    return@combine AccountDetailUiState.Loading
                }

                val account = accounts.find { it.id == accountId }
                    ?: return@combine AccountDetailUiState.Error("Account not found")

                var accountTransactions = allTransactions.filter { it.accountId == accountId }

                if (!settings.transactionsSection.showPlannedPayments) {
                    val now = System.currentTimeMillis()
                    accountTransactions = accountTransactions.filter { it.timestamp <= now }
                }

                val transactionsWithBalance = computeRunningBalance(
                    transactions = accountTransactions,
                    accountCurrency = account.currencyCode,
                    exchangeRates = rates
                )

                AccountDetailUiState.Success(
                    account = account,
                    allAccountTransactions = accountTransactions,
                    transactionsWithBalance = transactionsWithBalance,
                    allCategories = categories,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    decimalPrecision = precision,
                    use24HourFormat = use24Hour,
                    showPlannedPayments = settings.transactionsSection.showPlannedPayments,
                    useNoteAsHeader = settings.transactionsSection.useNoteAsHeader
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
            acc + CurrencyService.convertAmount(tx.amount, tx.currency, accountCurrency, exchangeRates, 10)
        }
        var currentBalance = totalBalance
        return sorted.map { tx ->
            val converted = CurrencyService.convertAmount(tx.amount, tx.currency, accountCurrency, exchangeRates, 10)
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
        val account: Account,
        val allAccountTransactions: List<Transaction>,
        val transactionsWithBalance: List<TransactionWithBalance> = emptyList(),
        val allCategories: List<Category> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val decimalPrecision: Int,
        val use24HourFormat: Boolean = false,
        val showPlannedPayments: Boolean = false,
        val useNoteAsHeader: Boolean = false
    ) : AccountDetailUiState()
    data class Error(val message: String) : AccountDetailUiState()
}
