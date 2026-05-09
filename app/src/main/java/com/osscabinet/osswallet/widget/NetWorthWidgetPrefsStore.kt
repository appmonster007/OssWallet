package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.nwWidgetStore: DataStore<Preferences> by preferencesDataStore("nw_widget_prefs")

class NetWorthWidgetPrefsStore(private val context: Context) {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private fun key(widgetId: Int) = stringPreferencesKey("nw_config_$widgetId")

    suspend fun load(widgetId: Int): NetWorthWidgetConfig {
        val raw = context.nwWidgetStore.data.first()[key(widgetId)] ?: return NetWorthWidgetConfig()
        return try { json.decodeFromString(raw) } catch (e: Exception) { NetWorthWidgetConfig() }
    }

    suspend fun save(widgetId: Int, config: NetWorthWidgetConfig) {
        context.nwWidgetStore.edit { it[key(widgetId)] = json.encodeToString(config) }
    }

    suspend fun delete(widgetId: Int) {
        context.nwWidgetStore.edit { it.remove(key(widgetId)) }
    }
}
