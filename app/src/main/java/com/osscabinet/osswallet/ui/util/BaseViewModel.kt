package com.osscabinet.ossWallet.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

/**
 * Base ViewModel providing common utilities.
 */
abstract class BaseViewModel(
    protected val settingsRepository: SettingsRepository
) : ViewModel() {

    val recentActivitiesBreakdown: StateFlow<Boolean> = settingsRepository.recentActivitiesBreakdown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Executes a block of code with the current profile ID.
     */
    protected suspend fun withProfile(block: suspend (Int) -> Unit) {
        val profileId = settingsRepository.currentProfileId.first()
        block(profileId)
    }
}
