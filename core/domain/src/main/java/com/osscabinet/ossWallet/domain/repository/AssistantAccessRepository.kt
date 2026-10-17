package com.osscabinet.ossWallet.domain.repository

import kotlinx.coroutines.flow.Flow

/** Whether the user has let AI assistants call into the app. Off until they switch it on. */
interface AssistantAccessRepository {
    val enabled: Flow<Boolean>
    suspend fun isEnabled(): Boolean
    suspend fun setEnabled(enabled: Boolean)
}
