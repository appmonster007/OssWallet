package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
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

    val uiState: StateFlow<NetWorthUiState> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                getTransactionsUseCase(profileId),
                analyticsViewModel.uiState,
                settingsDataStore.decimalPrecision,
                settingsDataStore.dashboardSettings.map { it.netWorthSection.fontSize }
            ) { accounts, transactions, analyticsState, precision, fontSize ->
                val rates = analyticsState.exchangeRates
                val baseCurrency = analyticsState.selectedCurrency
                
                val netWorth = calculateNetWorth(
                    accounts = accounts,
                    allTransactions = transactions,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    precision = precision
                )

                NetWorthUiState.Success(
                    netWorth = netWorth,
                    baseCurrency = baseCurrency,
                    precision = precision,
                    fontSize = fontSize
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NetWorthUiState.Loading
        )

    fun updateNetWorthFontSize(fontSize: String) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(netWorthSection = it.netWorthSection.copy(fontSize = fontSize)) }
        }
    }
}

sealed class NetWorthUiState {
    object Loading : NetWorthUiState()
    data class Success(
        val netWorth: BigDecimal,
        val baseCurrency: String,
        val precision: Int,
        val fontSize: String
    ) : NetWorthUiState()
    data class Error(val message: String) : NetWorthUiState()
}
