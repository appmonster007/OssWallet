package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_configs")

class WidgetConfigStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun key(glanceIdHash: Int) = stringPreferencesKey("widget_$glanceIdHash")

    suspend fun getConfig(glanceIdHash: Int): WidgetConfig? {
        val raw = context.widgetDataStore.data.first()[key(glanceIdHash)] ?: return null
        return try { json.decodeFromString<WidgetConfig>(raw) } catch (e: Exception) { null }
    }

    suspend fun saveConfig(glanceIdHash: Int, config: WidgetConfig) {
        context.widgetDataStore.edit { prefs ->
            prefs[key(glanceIdHash)] = json.encodeToString(WidgetConfig.serializer(), config)
        }
    }

    suspend fun deleteConfig(glanceIdHash: Int) {
        context.widgetDataStore.edit { prefs -> prefs.remove(key(glanceIdHash)) }
    }
}
