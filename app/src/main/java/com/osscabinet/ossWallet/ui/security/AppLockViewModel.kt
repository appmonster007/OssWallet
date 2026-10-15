package com.osscabinet.ossWallet.ui.security

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.settings.AppLockConfig
import com.osscabinet.ossWallet.domain.repository.AppLockRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val repository: AppLockRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    val config: StateFlow<AppLockConfig> = repository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLockConfig())

    fun enable(pin: CharArray, biometric: Boolean) = viewModelScope.launch {
        repository.enable(pin, biometric)
        pin.fill('0')
    }

    fun disable() = viewModelScope.launch { repository.disable() }

    fun changePin(pin: CharArray) = viewModelScope.launch {
        repository.changePin(pin)
        pin.fill('0')
    }

    suspend fun verifyPin(pin: CharArray): Boolean = repository.verifyPin(pin)
    fun setBiometric(enabled: Boolean) = viewModelScope.launch { repository.setBiometricEnabled(enabled) }
    fun setTimeoutSeconds(seconds: Int) = viewModelScope.launch { repository.setTimeoutSeconds(seconds) }
    fun setPrivacyScreen(enabled: Boolean) = viewModelScope.launch { repository.setPrivacyScreenEnabled(enabled) }
}
