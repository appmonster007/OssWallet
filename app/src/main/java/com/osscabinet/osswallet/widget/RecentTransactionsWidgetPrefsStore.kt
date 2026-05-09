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

private val Context.rtWidgetStore: DataStore<Preferences> by preferencesDataStore("rt_widget_prefs")

class RecentTransactionsWidgetPrefsStore(private val context: Context) {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private fun key(widgetId: Int) = stringPreferencesKey("rt_config_$widgetId")

    suspend fun load(widgetId: Int): RecentTransactionsWidgetConfig {
        val raw = context.rtWidgetStore.data.first()[key(widgetId)] ?: return RecentTransactionsWidgetConfig()
        return try { json.decodeFromString(raw) } catch (e: Exception) { RecentTransactionsWidgetConfig() }
    }

    suspend fun save(widgetId: Int, config: RecentTransactionsWidgetConfig) {
        context.rtWidgetStore.edit { it[key(widgetId)] = json.encodeToString(config) }
    }

    suspend fun delete(widgetId: Int) {
        context.rtWidgetStore.edit { it.remove(key(widgetId)) }
    }
}
