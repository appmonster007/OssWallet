package com.osscabinet.ossWallet.ui.screens.main

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.BuildConfig
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.BuyMeACoffeePromptScheduler
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val templateRepository: TemplateRepository
) : BaseViewModel(settingsRepository) {

    val isOnboardingCompleted: StateFlow<Boolean> = settingsRepository.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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

    val starredTemplates: StateFlow<List<Template>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> templateRepository.getStarredTemplates(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showBuyMeACoffeePrompt: StateFlow<Boolean> = if (!BuildConfig.ENABLE_BUY_ME_A_COFFEE) {
        MutableStateFlow(false)
    } else {
        combine(
            settingsRepository.isOnboardingCompleted,
            settingsRepository.buyMeACoffeePromptLastShownWindow
        ) { onboardingCompleted, lastShownWindow ->
            onboardingCompleted && BuyMeACoffeePromptScheduler.shouldShowPrompt(LocalDate.now(), lastShownWindow)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    }

    fun onBuyMeACoffeePromptDismissed() {
        viewModelScope.launch {
            settingsRepository.saveBuyMeACoffeePromptLastShownWindow(
                BuyMeACoffeePromptScheduler.currentWindowEpochDay(LocalDate.now())
            )
        }
    }
}
