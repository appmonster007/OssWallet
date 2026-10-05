package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

// These types live in domain — typealiases preserve backward compatibility for data-layer references.
typealias TransactionGroupingType = com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
typealias TransactionSectionSettings = com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
typealias GlassSettings = com.osscabinet.ossWallet.domain.model.settings.GlassSettings

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val CURRENCY_KEY = stringPreferencesKey("currency")
        val THEME_KEY = stringPreferencesKey("theme")
        val CURRENT_PROFILE_ID_KEY = intPreferencesKey("current_profile_id")
        val DECIMAL_PRECISION_KEY = intPreferencesKey("decimal_precision")
        val AUTO_SAVE_EXCHANGE_RATES_KEY = booleanPreferencesKey("auto_save_exchange_rates")
        val IS_ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("is_onboarding_completed")
        val CARD_ANIMATION_ENABLED_KEY = booleanPreferencesKey("card_animation_enabled")
        val CARD_OPEN_ANIMATION_KEY = stringPreferencesKey("card_open_animation")
        val EXPANDABLE_FAB_ENABLED_KEY = booleanPreferencesKey("expandable_fab_enabled")
        val IS_DATABASE_SEEDED_KEY = booleanPreferencesKey("is_database_seeded")
        val USE_24_HOUR_FORMAT_KEY = booleanPreferencesKey("use_24_hour_format")
        val MAPS_ACCESS_ENABLED_KEY = booleanPreferencesKey("maps_access_enabled")
        val RECENT_ACTIVITIES_BREAKDOWN_KEY = booleanPreferencesKey("recent_activities_breakdown")
        val CURRENCY_PROVIDER_KEY = stringPreferencesKey("currency_provider")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USE_SYSTEM_FONT_KEY = booleanPreferencesKey("use_system_font")
        val SELECTED_FONT_KEY = stringPreferencesKey("selected_font")
        val UI_IDENTITY_KEY = stringPreferencesKey("ui_identity")
        val DYNAMIC_ACCENT_KEY = booleanPreferencesKey("dynamic_accent")
        val NUMBERING_SYSTEM_KEY = stringPreferencesKey("numbering_system")
        val SHORTLISTED_CURRENCIES_KEY = stringSetPreferencesKey("shortlisted_currencies")

        val RECORDS_SETTINGS_KEY = stringPreferencesKey("records_settings")
        val DASHBOARD_CARDS_CONFIG_KEY = stringPreferencesKey("dashboard_cards_config")
        val ACCOUNT_DETAIL_CARDS_CONFIG_KEY = stringPreferencesKey("account_detail_cards_config")
        val ACCOUNTS_VIEW_CARDS_CONFIG_KEY = stringPreferencesKey("accounts_view_cards_config")
        val ANALYTICS_CARDS_CONFIG_KEY = stringPreferencesKey("analytics_cards_config")
        val ANALYTICS_COMMON_FILTER_ENABLED_KEY =
            booleanPreferencesKey("analytics_common_filter_enabled")
        val GLASSMORPHISM_ENABLED_KEY = booleanPreferencesKey("glassmorphism_enabled")
        val CONTOUR_BG_VERSION_KEY = intPreferencesKey("contour_bg_version")
        val CONTOUR_LINES_ENABLED_KEY = booleanPreferencesKey("contour_lines_enabled")
        val RANDOMIZE_CONTOUR_EACH_LOAD_KEY = booleanPreferencesKey("randomize_contour_each_load")
        val GLASS_SETTINGS_KEY = stringPreferencesKey("glass_settings")
        val AUTO_BACKUP_CONFIG_KEY = stringPreferencesKey("auto_backup_config")
        val UI_DENSITY_KEY = stringPreferencesKey("ui_density")
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

    val decimalPrecision: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DECIMAL_PRECISION_KEY] ?: 2
    }

    val autoSaveExchangeRates: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SAVE_EXCHANGE_RATES_KEY] ?: true
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ONBOARDING_COMPLETED_KEY] ?: false
    }

    val cardAnimationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CARD_ANIMATION_ENABLED_KEY] ?: false
    }

    val expandableFabEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[EXPANDABLE_FAB_ENABLED_KEY] ?: true
    }

    val isDatabaseSeeded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_DATABASE_SEEDED_KEY] ?: false
    }

    val use24HourFormat: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_24_HOUR_FORMAT_KEY]
            ?: android.text.format.DateFormat.is24HourFormat(context)
    }

    val mapsAccessEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MAPS_ACCESS_ENABLED_KEY] ?: false
    }

    val recentActivitiesBreakdown: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[RECENT_ACTIVITIES_BREAKDOWN_KEY] ?: false
    }

    val currencyProvider: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY_PROVIDER_KEY] ?: "exchangerate-api"
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY] ?: ""
    }

    val useSystemFont: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_SYSTEM_FONT_KEY] ?: false
    }

    // Migrates the legacy useSystemFont boolean: if no explicit font choice has been saved yet,
    // users who had useSystemFont=true keep "SYSTEM"; everyone else gets the app default "MANROPE".
    val selectedFont: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_FONT_KEY]
            ?: if (preferences[USE_SYSTEM_FONT_KEY] == true) "SYSTEM" else "MANROPE"
    }

    val uiIdentity: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[UI_IDENTITY_KEY]
            ?: com.osscabinet.ossWallet.domain.model.settings.UiIdentity.DEFAULT.name
    }

    val dynamicAccent: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_ACCENT_KEY] ?: false
    }

    val numberingSystem: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[NUMBERING_SYSTEM_KEY] ?: "WESTERN"
    }

    val recordsSettings: Flow<TransactionSectionSettings> =
        context.dataStore.data.map { preferences ->
            preferences[RECORDS_SETTINGS_KEY]?.let {
                try {
                    Json.decodeFromString<TransactionSectionSettings>(it)
                } catch (e: Exception) {
                    TransactionSectionSettings()
                }
            } ?: TransactionSectionSettings()
        }

    val dashboardCardsConfig: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DASHBOARD_CARDS_CONFIG_KEY] ?: ""
    }

    val accountDetailCardsConfig: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ACCOUNT_DETAIL_CARDS_CONFIG_KEY] ?: ""
    }

    val accountsViewCardsConfig: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ACCOUNTS_VIEW_CARDS_CONFIG_KEY] ?: ""
    }

    val analyticsCardsConfig: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ANALYTICS_CARDS_CONFIG_KEY] ?: ""
    }

    val analyticsCommonFilterEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ANALYTICS_COMMON_FILTER_ENABLED_KEY] ?: true
    }

    val shortlistedCurrencies: Flow<Set<String>?> = context.dataStore.data.map { preferences ->
        preferences[SHORTLISTED_CURRENCIES_KEY]
    }

    val glassmorphismEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSMORPHISM_ENABLED_KEY] ?: false
    }

    val contourBgVersion: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CONTOUR_BG_VERSION_KEY] ?: 4
    }

    val contourLinesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CONTOUR_LINES_ENABLED_KEY] ?: true
    }

    val randomizeContourEachLoad: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[RANDOMIZE_CONTOUR_EACH_LOAD_KEY] ?: true
    }

    val autoBackupConfig: Flow<AutoBackupConfig> = context.dataStore.data.map { prefs ->
        prefs[AUTO_BACKUP_CONFIG_KEY]?.let {
            try {
                Json.decodeFromString<AutoBackupConfig>(it)
            } catch (_: Exception) {
                AutoBackupConfig()
            }
        } ?: AutoBackupConfig()
    }

    val glassSettings: Flow<GlassSettings> = context.dataStore.data.map { preferences ->
        preferences[GLASS_SETTINGS_KEY]?.let {
            try {
                Json.decodeFromString<GlassSettings>(it)
            } catch (e: Exception) {
                GlassSettings()
            }
        } ?: GlassSettings()
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

    suspend fun saveCardAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CARD_ANIMATION_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveExpandableFabEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EXPANDABLE_FAB_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveDatabaseSeeded(seeded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DATABASE_SEEDED_KEY] = seeded
        }
    }

    suspend fun saveUse24HourFormat(use24Hour: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_24_HOUR_FORMAT_KEY] = use24Hour
        }
    }

    suspend fun saveMapsAccessEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MAPS_ACCESS_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveRecentActivitiesBreakdown(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RECENT_ACTIVITIES_BREAKDOWN_KEY] = enabled
        }
    }

    suspend fun saveCurrencyProvider(providerId: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_PROVIDER_KEY] = providerId
        }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    suspend fun saveUseSystemFont(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_SYSTEM_FONT_KEY] = enabled
        }
    }

    suspend fun saveSelectedFont(fontName: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_FONT_KEY] = fontName
        }
    }

    suspend fun saveUiIdentity(identity: String) {
        context.dataStore.edit { preferences ->
            preferences[UI_IDENTITY_KEY] = identity
        }
    }

    suspend fun saveDynamicAccent(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_ACCENT_KEY] = enabled
        }
    }

    suspend fun saveNumberingSystem(system: String) {
        context.dataStore.edit { preferences ->
            preferences[NUMBERING_SYSTEM_KEY] = system
        }
    }

    suspend fun updateRecordsSettings(update: (TransactionSectionSettings) -> TransactionSectionSettings) {
        context.dataStore.edit { preferences ->
            val current = preferences[RECORDS_SETTINGS_KEY]?.let {
                try {
                    Json.decodeFromString<TransactionSectionSettings>(it)
                } catch (e: Exception) {
                    TransactionSectionSettings()
                }
            } ?: TransactionSectionSettings()
            preferences[RECORDS_SETTINGS_KEY] = Json.encodeToString(update(current))
        }
    }

    suspend fun saveDashboardCardsConfig(config: String) {
        context.dataStore.edit { preferences ->
            preferences[DASHBOARD_CARDS_CONFIG_KEY] = config
        }
    }

    suspend fun saveAccountDetailCardsConfig(config: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCOUNT_DETAIL_CARDS_CONFIG_KEY] = config
        }
    }

    suspend fun saveAccountsViewCardsConfig(config: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCOUNTS_VIEW_CARDS_CONFIG_KEY] = config
        }
    }

    suspend fun saveAnalyticsCardsConfig(config: String) {
        context.dataStore.edit { preferences ->
            preferences[ANALYTICS_CARDS_CONFIG_KEY] = config
        }
    }

    suspend fun saveAnalyticsCommonFilterEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ANALYTICS_COMMON_FILTER_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveShortlistedCurrencies(currencies: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SHORTLISTED_CURRENCIES_KEY] = currencies
        }
    }

    suspend fun saveGlassmorphismEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSMORPHISM_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveContourBgVersion(version: Int) {
        context.dataStore.edit { preferences ->
            preferences[CONTOUR_BG_VERSION_KEY] = version
        }
    }

    suspend fun saveContourLinesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CONTOUR_LINES_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveRandomizeContourEachLoad(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RANDOMIZE_CONTOUR_EACH_LOAD_KEY] = enabled
        }
    }

    suspend fun updateGlassSettings(update: (GlassSettings) -> GlassSettings) {
        context.dataStore.edit { preferences ->
            val current = preferences[GLASS_SETTINGS_KEY]?.let {
                try {
                    Json.decodeFromString<GlassSettings>(it)
                } catch (e: Exception) {
                    GlassSettings()
                }
            } ?: GlassSettings()
            preferences[GLASS_SETTINGS_KEY] = Json.encodeToString(update(current))
        }
    }

    suspend fun saveAutoBackupConfig(config: AutoBackupConfig) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_BACKUP_CONFIG_KEY] = Json.encodeToString(config)
        }
    }

    val uiDensity: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[UI_DENSITY_KEY]
            ?: com.osscabinet.ossWallet.domain.model.settings.UiDensity.DEFAULT.name
    }

    suspend fun saveUiDensity(name: String) {
        context.dataStore.edit { it[UI_DENSITY_KEY] = name }
    }

    val cardOpenAnimation: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CARD_OPEN_ANIMATION_KEY]
            ?: com.osscabinet.ossWallet.domain.model.settings.CardOpenAnimation.DEFAULT.name
    }

    suspend fun saveCardOpenAnimation(name: String) {
        context.dataStore.edit { it[CARD_OPEN_ANIMATION_KEY] = name }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
