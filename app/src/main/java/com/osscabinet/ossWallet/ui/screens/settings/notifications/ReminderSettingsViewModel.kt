package com.osscabinet.ossWallet.ui.screens.settings.notifications

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.settings.ReminderConfig
import com.osscabinet.ossWallet.domain.repository.ReminderRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    val config: StateFlow<ReminderConfig> = reminderRepository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderConfig())

    fun update(transform: (ReminderConfig) -> ReminderConfig) {
        viewModelScope.launch {
            reminderRepository.saveConfig(transform(reminderRepository.config.first()))
        }
    }
}
