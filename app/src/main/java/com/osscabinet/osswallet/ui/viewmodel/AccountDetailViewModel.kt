package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModel(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    val settingsDataStore: SettingsDataStore,
    private val analyticsViewModel: com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel,
    private val financialUseCases: FinancialUseCases,
    private val cardManagerViewModel: CardManagerViewModel,
    private val cardComputationService: com.osscabinet.ossWallet.domain.service.CardComputationService
) : ViewModel() {

    fun updateCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCurrencyBalancesTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
        }
    }

    fun updateCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCashFlowTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(timelineRange = range)) }
        }
    }

    fun updateShowPlannedPayments(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(transactionsSection = it.transactionsSection.copy(showPlannedPayments = show)) }
        }
    }

    fun updateUseNoteAsHeader(use: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountDetailSettings { it.copy(transactionsSection = it.transactionsSection.copy(useNoteAsHeader = use)) }
        }
    }

    // Card-specific update methods
    fun updateCardHeroShowCurrencyBadges(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig) {
                    config.copy(showCurrencyBadges = show)
                } else config
            }
        }
    }

    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountCashFlowCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
        }
    }

    fun updateCardCashFlowTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountCashFlowCardConfig) {
                    config.copy(timelineRange = range)
                } else config
            }
        }
    }

    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountCurrencyBalancesCardConfig) {
                    config.copy(layout = layout)
                } else config
            }
        }
    }

    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountCurrencyBalancesCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
        }
    }

    fun updateCardCurrencyBalancesTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardManagerViewModel.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountCurrencyBalancesCardConfig) {
                    config.copy(timelineRange = range)
                } else config
            }
        }
    }

    fun removeAccountDetailCard(cardId: String) {
        viewModelScope.launch {
            cardManagerViewModel.removeAccountDetailCard(cardId)
        }
    }

    // Card-specific compute methods
    suspend fun computeCashFlowForCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.AccountCashFlowCardConfig,
        accountTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        // Convert AccountCashFlowCardConfig to CashFlowCardConfig with accountId
        val unifiedConfig = com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig(
            cardId = cardConfig.cardId,
            title = cardConfig.title,
            isEnabled = cardConfig.isEnabled,
            order = cardConfig.order,
            showBaseConversion = cardConfig.showBaseConversion,
            timelineRange = cardConfig.timelineRange,
            accountId = accountId // Set accountId for account scope
        )
        
        return cardComputationService.computeCashFlow(
            config = unifiedConfig,
            transactions = accountTransactions,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
    }

    suspend fun computeCurrencyTotalsForCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.AccountCurrencyBalancesCardConfig,
        account: Account,
        accountTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem> {
        // Convert AccountCurrencyBalancesCardConfig to CurrencyBalancesCardConfig with accountId
        val unifiedConfig = com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig(
            cardId = cardConfig.cardId,
            title = cardConfig.title,
            isEnabled = cardConfig.isEnabled,
            order = cardConfig.order,
            layout = cardConfig.layout,
            showBaseConversion = cardConfig.showBaseConversion,
            timelineRange = cardConfig.timelineRange,
            accountId = accountId // Set accountId for account scope
        )
        
        return cardComputationService.computeCurrencyTotals(
            config = unifiedConfig,
            accounts = listOf(account),
            transactions = accountTransactions,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
    }

    val uiState: StateFlow<AccountDetailUiState> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                analyticsViewModel.uiState,
                settingsDataStore.decimalPrecision,
                settingsDataStore.accountDetailSettings
            ) { accounts, allTransactions, analyticsState, precision, settings ->
                val account = accounts.find { it.id == accountId } ?: return@combine AccountDetailUiState.Error("Account not found")

                val rates = analyticsState.exchangeRates
                val baseCurrency = analyticsState.selectedCurrency

                var accountTransactions = allTransactions.filter { it.accountId == accountId }
                
                if (!analyticsState.hasLoadedExchangeRates) {
                    return@combine AccountDetailUiState.Loading
                }

                if (!settings.transactionsSection.showPlannedPayments) {
                    val now = System.currentTimeMillis()
                    accountTransactions = accountTransactions.filter { it.timestamp <= now }
                }

                val currencyTotals = financialUseCases.getCurrencyAggregation(
                    accounts = listOf(account),
                    allTransactions = accountTransactions,
                    timelineRange = settings.currencyBalancesSection.timelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                val cashFlowResult = financialUseCases.calculateCashFlow(
                    allTransactions = accountTransactions,
                    timelineRange = settings.cashFlowSection.timelineRange,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                // Get recent transactions using the same logic as dashboard (last 20 or last 7 days, whichever has more)
                val recentTransactions = transactionUseCases.getRecentTransactions(
                    allTransactions = accountTransactions,
                    accounts = listOf(account),
                    exchangeRates = rates,
                    precision = precision
                ).map { it.transaction }

                val updatedAccount = financialUseCases.getTotalBalances(
                    accounts = listOf(account),
                    allTransactions = accountTransactions,
                    exchangeRates = rates,
                    precision = precision,
                    timelineRange = settings.currencyBalancesSection.timelineRange
                ).first()

                AccountDetailUiState.Success(
                    account = updatedAccount,
                    recentTransactions = recentTransactions,
                    currencyTotals = currencyTotals,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    decimalPrecision = precision,
                    currencyBalancesLayout = settings.currencyBalancesSection.layout,
                    showBaseConversion = settings.currencyBalancesSection.showBaseConversion,
                    currencyBalancesTimelineRange = settings.currencyBalancesSection.timelineRange,
                    cashFlowShowBaseConversion = settings.cashFlowSection.showBaseConversion,
                    cashFlowTimelineRange = settings.cashFlowSection.timelineRange,
                    showPlannedPayments = settings.transactionsSection.showPlannedPayments,
                    useNoteAsHeader = settings.transactionsSection.useNoteAsHeader,
                    cashFlowResult = cashFlowResult
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
        val recentTransactions: List<Transaction>,
        val currencyTotals: List<CurrencyAggregationItem> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val decimalPrecision: Int,
        val currencyBalancesLayout: String,
        val showBaseConversion: Boolean,
        val currencyBalancesTimelineRange: TimelineRange,
        val cashFlowShowBaseConversion: Boolean,
        val cashFlowTimelineRange: TimelineRange,
        val showPlannedPayments: Boolean = false,
        val useNoteAsHeader: Boolean = false,
        val cashFlowResult: CashFlowResult? = null
    ) : AccountDetailUiState()
    data class Error(val message: String) : AccountDetailUiState()
}
