package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val CURRENCY_KEY = stringPreferencesKey("currency")
        val THEME_KEY = stringPreferencesKey("theme")
        val CURRENT_PROFILE_ID_KEY = intPreferencesKey("current_profile_id")
        val DASHBOARD_GRID_COLUMNS_KEY = intPreferencesKey("dashboard_grid_columns")
        val DECIMAL_PRECISION_KEY = intPreferencesKey("decimal_precision")
        val AUTO_SAVE_EXCHANGE_RATES_KEY = booleanPreferencesKey("auto_save_exchange_rates")
        val IS_ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("is_onboarding_completed")
        val DASHBOARD_SECTION_ORDER_KEY = stringPreferencesKey("dashboard_section_order")
    }

    val currency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY_KEY] ?: "USD"
    }

    val theme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "System"
    }

    val currentProfileId: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_PROFILE_ID_KEY] ?: 1
    }

    val dashboardGridColumns: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DASHBOARD_GRID_COLUMNS_KEY] ?: 2
    }

    val decimalPrecision: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DECIMAL_PRECISION_KEY] ?: 2
    }

    val autoSaveExchangeRates: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SAVE_EXCHANGE_RATES_KEY] ?: true
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ONBOARDING_COMPLETED_KEY] ?: false
    }

    val dashboardSectionOrder: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DASHBOARD_SECTION_ORDER_KEY]
    }

    suspend fun saveCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = currency
        }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun saveCurrentProfileId(profileId: Int) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_PROFILE_ID_KEY] = profileId
        }
    }

    suspend fun saveDashboardGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[DASHBOARD_GRID_COLUMNS_KEY] = columns
        }
    }

    suspend fun saveDecimalPrecision(precision: Int) {
        context.dataStore.edit { preferences ->
            preferences[DECIMAL_PRECISION_KEY] = precision
        }
    }

    suspend fun saveAutoSaveExchangeRates(autoSave: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SAVE_EXCHANGE_RATES_KEY] = autoSave
        }
    }

    suspend fun saveOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun saveDashboardSectionOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[DASHBOARD_SECTION_ORDER_KEY] = order
        }
    }
}
