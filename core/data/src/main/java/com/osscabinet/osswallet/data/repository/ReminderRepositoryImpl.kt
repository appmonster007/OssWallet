package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.ReminderDataStore
import com.osscabinet.ossWallet.domain.model.settings.ReminderConfig
import com.osscabinet.ossWallet.domain.repository.ReminderRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val store: ReminderDataStore
) : ReminderRepository {
    override val config: Flow<ReminderConfig> = store.config
    override suspend fun saveConfig(config: ReminderConfig) = store.saveConfig(config)
    override suspend fun sentKeys(): Set<String> = store.sentKeys()
    override suspend fun markSent(keys: Collection<String>, now: Long) = store.markSent(keys, now)
}
