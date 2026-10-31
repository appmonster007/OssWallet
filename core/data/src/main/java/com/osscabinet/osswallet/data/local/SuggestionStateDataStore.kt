package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.suggestionStore: DataStore<Preferences> by preferencesDataStore(name = "recurring_suggestions")

/** Entries are stored as "<profileId>|<key>" so each profile keeps its own dismissals. */
@Singleton
class SuggestionStateDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        val DISMISSED = stringSetPreferencesKey("dismissed")
        val SEEN = stringSetPreferencesKey("seen")
    }

    fun dismissed(profileId: Int): Flow<Set<String>> = read(DISMISSED, profileId)
    fun seen(profileId: Int): Flow<Set<String>> = read(SEEN, profileId)

    private fun read(key: Preferences.Key<Set<String>>, profileId: Int): Flow<Set<String>> {
        val prefix = "$profileId|"
        return context.suggestionStore.data.map { p ->
            (p[key] ?: emptySet()).filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }.toSet()
        }
    }

    suspend fun add(which: Preferences.Key<Set<String>>, profileId: Int, keys: Collection<String>) {
        if (keys.isEmpty()) return
        context.suggestionStore.edit { p -> p[which] = (p[which] ?: emptySet()) + keys.map { "$profileId|$it" } }
    }

    suspend fun dismiss(profileId: Int, key: String) = add(DISMISSED, profileId, listOf(key))
    suspend fun markSeen(profileId: Int, keys: Collection<String>) = add(SEEN, profileId, keys)

    suspend fun restoreDismissed(profileId: Int) {
        val prefix = "$profileId|"
        context.suggestionStore.edit { p -> p[DISMISSED] = (p[DISMISSED] ?: emptySet()).filterNot { it.startsWith(prefix) }.toSet() }
    }
}
