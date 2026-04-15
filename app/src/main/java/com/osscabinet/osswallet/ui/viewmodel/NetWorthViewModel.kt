package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.dashboard.NetWorthSettings
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.util.calculateNetWorth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class NetWorthViewModel(
    private val accountRepository: AccountRepository,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val analyticsViewModel: AnalyticsViewModel
) : ViewModel() {

    fun getNetWorthUiState(settingsFlow: Flow<NetWorthSettings>): StateFlow<NetWorthUiState> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                getTransactionsUseCase(profileId),
                analyticsViewModel.uiState,
                settingsDataStore.decimalPrecision,
                settingsFlow
            ) { accounts, transactions, analyticsState, precision, sectionSettings ->
                val rates = analyticsState.exchangeRates
                val baseCurrency = analyticsState.selectedCurrency
                
                val filteredTransactions = when (val range = sectionSettings.timelineRange) {
                    is TimelineRange.AllTime -> transactions
                    is TimelineRange.ThisPeriod -> {
                        val (start, end) = FilterUtils.getRangeForThis(range.period)
                        transactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.LastPeriod -> {
                        val (start, end) = FilterUtils.getRangeForLast(range.period)
                        transactions.filter { it.timestamp in start..end }
                    }
                    is TimelineRange.CustomRange -> {
                        transactions.filter { it.timestamp in range.start..range.end }
                    }
                }

                val netWorth = calculateNetWorth(
                    accounts = accounts,
                    allTransactions = filteredTransactions,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                NetWorthUiState.Success(
                    netWorth = netWorth,
                    baseCurrency = baseCurrency,
                    precision = precision,
                    fontSize = sectionSettings.fontSize,
                    timelineRange = sectionSettings.timelineRange
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NetWorthUiState.Loading
        )

    // These can be deprecated or removed since they are now screen-specific
    fun updateNetWorthFontSize(fontSize: String) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(netWorthSection = it.netWorthSection.copy(fontSize = fontSize)) }
        }
    }

    fun updateTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(netWorthSection = it.netWorthSection.copy(timelineRange = range)) }
        }
    }
}

sealed class NetWorthUiState {
    object Loading : NetWorthUiState()
    data class Success(
        val netWorth: BigDecimal,
        val baseCurrency: String,
        val precision: Int,
        val fontSize: String,
        val timelineRange: TimelineRange
    ) : NetWorthUiState()
    data class Error(val message: String) : NetWorthUiState()
}
