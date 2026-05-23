package com.osscabinet.ossWallet.ui.screens.openinweb

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.web.WebServerState
import com.osscabinet.ossWallet.domain.repository.WebServerRepository
import com.osscabinet.ossWallet.service.WebServerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpenInWebUiState(
    val serverState: WebServerState = WebServerState.Stopped,
    val qrBitmap: Bitmap? = null,
    val isWifiConnected: Boolean = false
)

@HiltViewModel
class OpenInWebViewModel @Inject constructor(
    private val webServerRepository: WebServerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpenInWebUiState())
    val uiState: StateFlow<OpenInWebUiState> = _uiState.asStateFlow()

    val isEnabled: StateFlow<Boolean> = webServerRepository.isEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            webServerRepository.serverState.collect { state ->
                val qr = if (state is WebServerState.Running) {
                    webServerRepository.generateQrCode(state.accessUrl)
                } else null
                _uiState.value = _uiState.value.copy(serverState = state, qrBitmap = qr)
            }
        }
        refreshWifiStatus()
    }

    fun toggleServer(enabled: Boolean) {
        viewModelScope.launch {
            webServerRepository.setEnabled(enabled)
            val intent = Intent(context, WebServerService::class.java).apply {
                action =
                    if (enabled) WebServerService.ACTION_START else WebServerService.ACTION_STOP
            }
            context.startForegroundService(intent)
        }
    }

    fun refreshWifiStatus() {
        _uiState.value = _uiState.value.copy(
            isWifiConnected = webServerRepository.isConnectedToWifi()
        )
    }
}
