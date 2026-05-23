package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.web.WebServerState
import kotlinx.coroutines.flow.StateFlow

interface WebServerRepository {
    val serverState: StateFlow<WebServerState>
    suspend fun start()
    suspend fun stop()
    fun isRunning(): Boolean
    fun getAccessUrl(): String?
    fun getAccessCode(): String?
}
