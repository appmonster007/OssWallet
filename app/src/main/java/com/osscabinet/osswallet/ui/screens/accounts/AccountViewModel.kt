package com.osscabinet.ossWallet.ui.screens.accounts

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
) : BaseViewModel(settingsRepository) {

    val accounts: StateFlow<List<Account>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> accountRepository.getAllAccounts(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                accountRepository.insertAccount(account.copy(isArchived = !account.isArchived), profileId)
            }
        }
    }
}
