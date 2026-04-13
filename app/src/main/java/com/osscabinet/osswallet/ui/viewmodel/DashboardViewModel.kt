package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val analyticsViewModel: AnalyticsViewModel
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                getTransactionsUseCase(profileId),
                analyticsViewModel.uiState
            ) { accounts, allTransactions, analyticsState ->
                val nonArchivedAccounts = accounts.filter { !it.isArchived }
                val nonArchivedAccountIds = nonArchivedAccounts.map { it.id }.toSet()
                
                // Calculate TransactionsWithBalance (recent activities)
                val accountMap = accounts.associateBy { it.id }
                val transactionsWithBalance = allTransactions.sortedBy { it.timestamp }
                    .groupBy { it.accountId }
                    .flatMap { (accId, accountTransactions) ->
                        val initialBalance = accountMap[accId]?.initialBalance ?: BigDecimal.ZERO
                        var currentBalance = initialBalance
                        accountTransactions.map { transaction ->
                            currentBalance += if (transaction.isIncome) transaction.amount else transaction.amount.negate()
                            TransactionWithBalance(transaction, currentBalance)
                        }
                    }
                    .filter { it.transaction.accountId in nonArchivedAccountIds }
                    .sortedByDescending { it.transaction.timestamp }
                    .take(20)

                DashboardUiState.Success(
                    accounts = nonArchivedAccounts,
                    recentTransactions = transactionsWithBalance,
                    baseCurrency = analyticsState.selectedCurrency,
                    exchangeRates = analyticsState.exchangeRates
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val accounts: List<Account>,
        val recentTransactions: List<TransactionWithBalance>,
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
