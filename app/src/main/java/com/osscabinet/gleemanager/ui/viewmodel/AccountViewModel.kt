package com.osscabinet.gleemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.osscabinet.gleemanager.data.local.SettingsDataStore
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.domain.repository.AccountRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModel(
    private val repository: AccountRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            repository.getAllAccounts(profileId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addAccount(name: String, balance: Double, currency: String, type: String, color: Int) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            val account = Account(
                id = UUID.randomUUID().toString(),
                name = name,
                balance = balance,
                currencyCode = currency,
                accountType = type,
                colorHex = color
            )
            repository.insertAccount(account, profileId)
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
        }
    }
}

class AccountViewModelFactory(
    private val repository: AccountRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(repository, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
