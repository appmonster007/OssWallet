package com.osscabinet.ossWallet.ui.accountdetail

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
    private val financialUseCases: FinancialUseCases
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

                val updatedAccount = financialUseCases.getTotalBalances(
                    accounts = listOf(account),
                    allTransactions = allTransactions.filter { it.accountId == accountId },
                    exchangeRates = rates,
                    precision = precision
                ).first()

                AccountDetailUiState.Success(
                    account = updatedAccount,
                    cashFlowTransactions = cashFlowResult.filteredTransactions,
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
        val cashFlowTransactions: List<Transaction>,
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
