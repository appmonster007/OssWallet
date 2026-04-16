package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModel(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    val settingsDataStore: SettingsDataStore,
    private val analyticsViewModel: AnalyticsViewModel
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
                getTransactionsUseCase(profileId),
                analyticsViewModel.uiState,
                settingsDataStore.decimalPrecision,
                settingsDataStore.accountDetailSettings
            ) { accounts, allTransactions, analyticsState, precision, settings ->
                val account = accounts.find { it.id == accountId } ?: return@combine AccountDetailUiState.Error("Account not found")

                val rates = analyticsState.exchangeRates
                val baseCurrency = analyticsState.selectedCurrency
                val currencyBalancesLayout = settings.currencyBalancesSection.layout
                val showBaseConversion = settings.currencyBalancesSection.showBaseConversion
                val currencyBalancesTimelineRange = settings.currencyBalancesSection.timelineRange
                val cashFlowShowBaseConversion = settings.cashFlowSection.showBaseConversion
                val cashFlowTimelineRange = settings.cashFlowSection.timelineRange
                val showPlannedPayments = settings.transactionsSection.showPlannedPayments
                val useNoteAsHeader = settings.transactionsSection.useNoteAsHeader

                var accountTransactions = allTransactions.filter { it.accountId == accountId }
                
                if (!showPlannedPayments) {
                    val now = System.currentTimeMillis()
                    accountTransactions = accountTransactions.filter { it.timestamp <= now }
                }

                val filteredTransactionsForBalances = when (val range = currencyBalancesTimelineRange) {
                    is TimelineRange.AllTime -> accountTransactions
                    is TimelineRange.ThisPeriod -> {
                        val (start, end) = FilterUtils.getRangeForThis(range.period)
                        accountTransactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.LastPeriod -> {
                        val (start, end) = FilterUtils.getRangeForLast(range.period)
                        accountTransactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.CustomRange -> {
                        accountTransactions.filter { it.timestamp in range.start..range.end }
                    }
                }

                val filteredTransactionsForCashFlow = when (val range = cashFlowTimelineRange) {
                    is TimelineRange.AllTime -> accountTransactions
                    is TimelineRange.ThisPeriod -> {
                        val (start, end) = FilterUtils.getRangeForThis(range.period)
                        accountTransactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.LastPeriod -> {
                        val (start, end) = FilterUtils.getRangeForLast(range.period)
                        accountTransactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.CustomRange -> {
                        accountTransactions.filter { it.timestamp in range.start..range.end }
                    }
                }

                // Currency aggregation for this account
                val supportedCurrencies = (setOf(account.currencyCode) + account.allowedCurrencies.toSet() + accountTransactions.map { it.currency }.toSet()).toList().sorted()
                
                val currencyTotals = supportedCurrencies.associateWith { curr ->
                    val txSum = filteredTransactionsForBalances.filter { it.currency == curr }.fold(BigDecimal.ZERO) { acc, tx ->
                        if (tx.isIncome) acc.add(tx.amount) else acc.subtract(tx.amount)
                    }
                    if (curr == account.currencyCode && currencyBalancesTimelineRange is TimelineRange.AllTime) {
                         account.initialBalance.add(txSum)
                    } else {
                        txSum
                    }
                }

                // Overall balance (always all time for accurate current balance)
                val totalTxSumPerCurrency = accountTransactions.groupBy { it.currency }.mapValues { (_, txs) ->
                    txs.fold(BigDecimal.ZERO) { acc, tx -> if (tx.isIncome) acc.add(tx.amount) else acc.subtract(tx.amount) }
                }
                val totalBalanceInAccountCurrency = account.initialBalance.add(calculateTotalInCurrency(totalTxSumPerCurrency, account.currencyCode, rates, precision))

                AccountDetailUiState.Success(
                    account = account.copy(balance = totalBalanceInAccountCurrency),
                    cashFlowTransactions = filteredTransactionsForCashFlow,
                    currencyTotals = currencyTotals,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    decimalPrecision = precision,
                    currencyBalancesLayout = currencyBalancesLayout,
                    showBaseConversion = showBaseConversion,
                    currencyBalancesTimelineRange = currencyBalancesTimelineRange,
                    cashFlowShowBaseConversion = cashFlowShowBaseConversion,
                    cashFlowTimelineRange = cashFlowTimelineRange,
                    showPlannedPayments = showPlannedPayments,
                    useNoteAsHeader = useNoteAsHeader
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
        val currencyTotals: Map<String, BigDecimal>,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val decimalPrecision: Int,
        val currencyBalancesLayout: String,
        val showBaseConversion: Boolean,
        val currencyBalancesTimelineRange: TimelineRange,
        val cashFlowShowBaseConversion: Boolean,
        val cashFlowTimelineRange: TimelineRange,
        val showPlannedPayments: Boolean = false,
        val useNoteAsHeader: Boolean = false
    ) : AccountDetailUiState()
    data class Error(val message: String) : AccountDetailUiState()
}
