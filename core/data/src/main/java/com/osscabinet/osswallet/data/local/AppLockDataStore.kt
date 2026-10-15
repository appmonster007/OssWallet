package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.osscabinet.ossWallet.domain.model.settings.AppLockConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appLockStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock")

/** Kept apart from [SettingsDataStore] so credentials are never swept up by settings export/seeding. */
@Singleton
class AppLockDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val PRIVACY_SCREEN = booleanPreferencesKey("privacy_screen")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
    }

    val config: Flow<AppLockConfig> = context.appLockStore.data.map { p ->
        AppLockConfig(
            enabled = p[ENABLED] ?: false,
            biometricEnabled = p[BIOMETRIC] ?: false,
            timeoutSeconds = p[TIMEOUT_SECONDS] ?: AppLockConfig.DEFAULT_TIMEOUT_SECONDS,
            privacyScreenEnabled = p[PRIVACY_SCREEN] ?: true,
            hasPin = p[PIN_HASH] != null && p[PIN_SALT] != null
        )
    }

    suspend fun credentials(): Pair<String, String>? {
        val p = context.appLockStore.data.first()
        val hash = p[PIN_HASH] ?: return null
        val salt = p[PIN_SALT] ?: return null
        return hash to salt
    }

    suspend fun savePin(hash: String, salt: String) {
        context.appLockStore.edit { it[PIN_HASH] = hash; it[PIN_SALT] = salt }
    }

    suspend fun enable(hash: String, salt: String, biometric: Boolean) {
        context.appLockStore.edit {
            it[PIN_HASH] = hash
            it[PIN_SALT] = salt
            it[BIOMETRIC] = biometric
            it[ENABLED] = true
        }
    }

    suspend fun disable() {
        context.appLockStore.edit {
            it[ENABLED] = false
            it[BIOMETRIC] = false
            it.remove(PIN_HASH)
            it.remove(PIN_SALT)
        }
    }

    suspend fun setBiometric(enabled: Boolean) { context.appLockStore.edit { it[BIOMETRIC] = enabled } }
    suspend fun setTimeoutSeconds(seconds: Int) { context.appLockStore.edit { it[TIMEOUT_SECONDS] = seconds } }
    suspend fun setPrivacyScreen(enabled: Boolean) { context.appLockStore.edit { it[PRIVACY_SCREEN] = enabled } }
}
