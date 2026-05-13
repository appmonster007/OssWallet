package com.osscabinet.ossWallet.ui.util

import androidx.lifecycle.ViewModel
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Base ViewModel providing common utilities.
 */
abstract class BaseViewModel(
    protected val settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * Executes a block of code with the current profile ID.
     */
    protected suspend fun withProfile(block: suspend (Int) -> Unit) {
        val profileId = settingsRepository.currentProfileId.first()
        block(profileId)
    }
}
