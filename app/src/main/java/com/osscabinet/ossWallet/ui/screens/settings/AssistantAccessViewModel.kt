package com.osscabinet.ossWallet.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.appfunctions.AssistantAccessController
import com.osscabinet.ossWallet.domain.repository.AssistantAccessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AssistantAccessViewModel @Inject constructor(
    private val controller: AssistantAccessController,
    repository: AssistantAccessRepository
) : ViewModel() {
    /** False before Android 16: the switch isn't shown at all. */
    val isSupported: Boolean get() = controller.isSupported

    val enabled: StateFlow<Boolean> = repository.enabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    init {
        // The system may not have been ready when the app started; try again whenever the screen opens.
        if (controller.isSupported) viewModelScope.launch { runCatching { controller.sync() } }
    }

    fun setEnabled(on: Boolean) {
        viewModelScope.launch {
            val applied = controller.setEnabled(on)
            _notice.update { if (applied) null else "Saved, but the system hasn't registered OssWallet's functions yet. Try again in a moment." }
        }
    }
}
