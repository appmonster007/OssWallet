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

private val Context.cfWidgetStore: DataStore<Preferences> by preferencesDataStore("cf_widget_prefs")

class CashFlowWidgetPrefsStore(private val context: Context) {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private fun key(widgetId: Int) = stringPreferencesKey("cf_config_$widgetId")

    suspend fun load(widgetId: Int): CashFlowWidgetConfig {
        val raw = context.cfWidgetStore.data.first()[key(widgetId)] ?: return CashFlowWidgetConfig()
        return try { json.decodeFromString(raw) } catch (e: Exception) { CashFlowWidgetConfig() }
    }

    suspend fun save(widgetId: Int, config: CashFlowWidgetConfig) {
        context.cfWidgetStore.edit { it[key(widgetId)] = json.encodeToString(config) }
    }

    suspend fun delete(widgetId: Int) {
        context.cfWidgetStore.edit { it.remove(key(widgetId)) }
    }
}
