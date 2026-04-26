package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import kotlinx.coroutines.flow.first

/**
 * Base ViewModel providing common utilities.
 */
abstract class BaseViewModel(
    protected val settingsDataStore: SettingsDataStore
) : ViewModel() {

    /**
     * Executes a block of code with the current profile ID.
     */
    protected suspend fun withProfile(block: suspend (Int) -> Unit) {
        val profileId = settingsDataStore.currentProfileId.first()
        block(profileId)
    }
}
