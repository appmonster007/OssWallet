package com.osscabinet.ossWallet.domain.repository

import android.graphics.Bitmap
import com.osscabinet.ossWallet.domain.model.web.WebServerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WebServerRepository {
    val serverState: StateFlow<WebServerState>
    val isEnabled: Flow<Boolean>
    suspend fun setEnabled(enabled: Boolean)
    suspend fun start()
    suspend fun stop()
    fun isRunning(): Boolean
    fun getAccessUrl(): String?
    fun getAccessCode(): String?
    fun generateQrCode(url: String): Bitmap?
    fun isConnectedToWifi(): Boolean
}
