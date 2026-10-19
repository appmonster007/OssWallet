package com.osscabinet.ossWallet.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.osscabinet.ossWallet.domain.repository.AssistantAccessRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.assistantAccessStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_access")

@Singleton
class AssistantAccessRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AssistantAccessRepository {
    private val key = booleanPreferencesKey("enabled")

    override val enabled: Flow<Boolean> = context.assistantAccessStore.data.map { it[key] ?: true }

    override suspend fun isEnabled(): Boolean = enabled.first()

    override suspend fun setEnabled(enabled: Boolean) {
        context.assistantAccessStore.edit { it[key] = enabled }
    }
}
