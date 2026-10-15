package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.settings.ReminderConfig
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    val config: Flow<ReminderConfig>
    suspend fun saveConfig(config: ReminderConfig)

    /** Keys of reminders already posted, so each event notifies once. */
    suspend fun sentKeys(): Set<String>
    suspend fun markSent(keys: Collection<String>, now: Long)
}
