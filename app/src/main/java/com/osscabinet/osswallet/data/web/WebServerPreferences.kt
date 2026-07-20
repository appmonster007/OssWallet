package com.osscabinet.ossWallet.data.web

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.webServerDataStore by preferencesDataStore(name = "web_server_prefs")

class WebServerPreferences(private val context: Context) {

    private val KEY_ENABLED = booleanPreferencesKey("server_enabled")
    private val KEY_STAY_ALIVE = booleanPreferencesKey("stay_alive")

    val isEnabled: Flow<Boolean> = context.webServerDataStore.data
        .map { prefs -> prefs[KEY_ENABLED] ?: false }

    val stayAlive: Flow<Boolean> = context.webServerDataStore.data
        .map { prefs -> prefs[KEY_STAY_ALIVE] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        context.webServerDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
        }
    }

    suspend fun setStayAlive(stayAlive: Boolean) {
        context.webServerDataStore.edit { prefs ->
            prefs[KEY_STAY_ALIVE] = stayAlive
        }
    }
}
