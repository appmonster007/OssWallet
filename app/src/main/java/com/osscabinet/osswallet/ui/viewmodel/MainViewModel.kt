package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val templateRepository: TemplateRepository
) : ViewModel() {

    val currentProfile: StateFlow<Profile?> = combine(
        settingsRepository.currentProfileId,
        profileRepository.getAllProfiles()
    ) { id, profiles ->
        profiles.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val nonArchivedAccounts: StateFlow<List<Account>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> accountRepository.getAllAccounts(profileId) }
        .map { accounts -> accounts.filter { !it.isArchived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val starredTemplates: StateFlow<List<Template>> = templateRepository.getStarredTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
