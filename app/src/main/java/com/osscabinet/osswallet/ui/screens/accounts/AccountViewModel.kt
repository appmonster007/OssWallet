package com.osscabinet.ossWallet.ui.screens.accounts

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val computationRepository: ComputationRepository,
    settingsRepository: SettingsRepository,
) : BaseViewModel(settingsRepository) {

    val accounts: StateFlow<List<Account>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> accountRepository.getAllAccounts(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountBalances: StateFlow<Map<String, BigDecimal>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionRepository.getAllTransactions(profileId),
                computationRepository.exchangeRates
            ) { accounts, transactions, rates ->
                accounts.associate { account ->
                    account.id to AggregationService.calculateAccountBalance(
                        accountId = account.id,
                        allTransactions = transactions,
                        accountCurrency = account.currencyCode,
                        exchangeRates = rates
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            accountRepository.deleteAccount(accountId)
        }
    }

    fun updateAccountsOrder(updatedAccounts: List<Account>) {
        viewModelScope.launch {
            withProfile { profileId ->
                val accountsWithOrder = updatedAccounts.mapIndexed { index, account ->
                    account.copy(displayOrder = index)
                }
                accountRepository.updateAccounts(accountsWithOrder, profileId)
            }
        }
    }

    fun toggleArchiveAccount(account: Account) {
        viewModelScope.launch {
            withProfile { profileId ->
                accountRepository.insertAccount(
                    account.copy(isArchived = !account.isArchived),
                    profileId
                )
            }
        }
    }
}
