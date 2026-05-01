package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import java.math.RoundingMode
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
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val computationRepository: ComputationRepository,
    private val registry: CardComputerRegistry
) : ViewModel(), CardUpdateHandler {

    fun updateCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCurrencyBalancesTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
        }
    }

    fun updateCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCashFlowTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(timelineRange = range)) }
        }
    }

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

    // Card-specific update methods
    override fun updateCardHeroShowCurrencyBadges(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is AccountHeroCardConfig) {
                    config.copy(showCurrencyBadges = show)
                } else config
            }
        }
    }

    override fun updateCardHeroFilter(cardId: String, filter: TransactionFilter) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is AccountHeroCardConfig) {
                    config.copy(filter = filter)
                } else config
            }
        }
    }

    override fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is CashFlowCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
        }
    }


    override fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is CurrencyBalancesCardConfig) {
                    config.copy(layout = layout)
                } else config
            }
        }
    }

    override fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is CurrencyBalancesCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
        }
    }


    fun removeAccountDetailCard(cardId: String) {
        viewModelScope.launch {
            cardRepository.removeAccountDetailCard(cardId)
        }
    }

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
                val settings = args[7] as com.osscabinet.ossWallet.data.local.AccountDetailSettings

                if (!hasLoadedExchangeRates) {
                    return@combine AccountDetailUiState.Loading
                }

                // Invalidate registry cache when data changes
                registry.clearCache()
                
                val account = accounts.find { it.id == accountId } ?: return@combine AccountDetailUiState.Error("Account not found")

                var accountTransactions = allTransactions.filter { it.accountId == accountId }

                if (!settings.transactionsSection.showPlannedPayments) {
                    val now = System.currentTimeMillis()
                    accountTransactions = accountTransactions.filter { it.timestamp <= now }
                }

                AccountDetailUiState.Success(
                    account = account,
                    allAccountTransactions = accountTransactions,
                    allCategories = categories,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    decimalPrecision = precision,
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
}

sealed class AccountDetailUiState {
    object Loading : AccountDetailUiState()
    data class Success(
        val account: Account,
        val allAccountTransactions: List<Transaction>,
        val allCategories: List<Category> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val decimalPrecision: Int,
        val showPlannedPayments: Boolean = false,
        val useNoteAsHeader: Boolean = false
    ) : AccountDetailUiState()
    data class Error(val message: String) : AccountDetailUiState()
}
