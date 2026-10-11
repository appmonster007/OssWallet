package com.osscabinet.ossWallet.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Shared persistence for a single widget type's per-instance config, keyed by `widgetId`.
 * Each widget type supplies its own [DataStore] (a distinct top-level `preferencesDataStore`
 * delegate — androidx requires one delegate instance per file name) and its config's
 * serializer/default, and gets load/save/delete for free.
 */
abstract class WidgetPrefsStore<T>(
    private val store: DataStore<Preferences>,
    private val keyPrefix: String,
    private val serializer: KSerializer<T>,
    private val defaultValue: T
) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private fun key(widgetId: Int) = stringPreferencesKey("${keyPrefix}_$widgetId")

    suspend fun load(widgetId: Int): T {
        val raw = store.data.first()[key(widgetId)] ?: return defaultValue
        return try { json.decodeFromString(serializer, raw) } catch (e: Exception) { defaultValue }
    }

    /** Same as [load], but also swallows failures from the store read itself (e.g. a corrupt file). */
    suspend fun loadSafe(widgetId: Int): T = try { load(widgetId) } catch (e: Exception) { defaultValue }

    suspend fun save(widgetId: Int, config: T) {
        store.edit { it[key(widgetId)] = json.encodeToString(serializer, config) }
    }

    suspend fun delete(widgetId: Int) {
        store.edit { it.remove(key(widgetId)) }
    }
}
