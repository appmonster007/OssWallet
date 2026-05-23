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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

// These types live in domain — typealiases preserve backward compatibility for data-layer references.
typealias LayoutSettings = com.osscabinet.ossWallet.domain.model.settings.LayoutSettings
typealias BudgetSettings = com.osscabinet.ossWallet.domain.model.settings.BudgetSettings
typealias DashboardSettings = com.osscabinet.ossWallet.domain.model.settings.DashboardSettings
typealias AccountsViewSettings = com.osscabinet.ossWallet.domain.model.settings.AccountsViewSettings
typealias TransactionGroupingType = com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
typealias TransactionSectionSettings = com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
typealias AccountDetailSettings = com.osscabinet.ossWallet.domain.model.settings.AccountDetailSettings

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
        val EXPANDABLE_FAB_ENABLED_KEY = booleanPreferencesKey("expandable_fab_enabled")
        val IS_DATABASE_SEEDED_KEY = booleanPreferencesKey("is_database_seeded")
        val USE_24_HOUR_FORMAT_KEY = booleanPreferencesKey("use_24_hour_format")
        val MAPS_ACCESS_ENABLED_KEY = booleanPreferencesKey("maps_access_enabled")
        val RECENT_ACTIVITIES_BREAKDOWN_KEY = booleanPreferencesKey("recent_activities_breakdown")
        val CURRENCY_PROVIDER_KEY = stringPreferencesKey("currency_provider")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USE_SYSTEM_FONT_KEY = booleanPreferencesKey("use_system_font")
        val SELECTED_FONT_KEY = stringPreferencesKey("selected_font")
        val SHORTLISTED_CURRENCIES_KEY = stringSetPreferencesKey("shortlisted_currencies")

        val DASHBOARD_SETTINGS_KEY = stringPreferencesKey("dashboard_settings")
        val ACCOUNTS_VIEW_SETTINGS_KEY = stringPreferencesKey("accounts_view_settings")
        val ACCOUNT_DETAIL_SETTINGS_KEY = stringPreferencesKey("account_detail_settings")
        val RECORDS_SETTINGS_KEY = stringPreferencesKey("records_settings")
        val DASHBOARD_CARDS_CONFIG_KEY = stringPreferencesKey("dashboard_cards_config")
        val ACCOUNT_DETAIL_CARDS_CONFIG_KEY = stringPreferencesKey("account_detail_cards_config")
        val ACCOUNTS_VIEW_CARDS_CONFIG_KEY = stringPreferencesKey("accounts_view_cards_config")
        val ANALYTICS_CARDS_CONFIG_KEY = stringPreferencesKey("analytics_cards_config")
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
        preferences[CARD_ANIMATION_ENABLED_KEY] ?: true
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

    val dashboardSettings: Flow<DashboardSettings> = context.dataStore.data.map { preferences ->
        preferences[DASHBOARD_SETTINGS_KEY]?.let {
            try {
                Json.decodeFromString<DashboardSettings>(it)
            } catch (e: Exception) {
                DashboardSettings()
            }
        } ?: DashboardSettings()
    }

    val accountsViewSettings: Flow<AccountsViewSettings> =
        context.dataStore.data.map { preferences ->
            preferences[ACCOUNTS_VIEW_SETTINGS_KEY]?.let {
                try {
                    Json.decodeFromString<AccountsViewSettings>(it)
                } catch (e: Exception) {
                    AccountsViewSettings()
                }
            } ?: AccountsViewSettings()
        }

    val accountDetailSettings: Flow<AccountDetailSettings> =
        context.dataStore.data.map { preferences ->
            preferences[ACCOUNT_DETAIL_SETTINGS_KEY]?.let {
                try {
                    Json.decodeFromString<AccountDetailSettings>(it)
                } catch (e: Exception) {
                    AccountDetailSettings()
                }
            } ?: AccountDetailSettings()
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

    val shortlistedCurrencies: Flow<Set<String>?> = context.dataStore.data.map { preferences ->
        preferences[SHORTLISTED_CURRENCIES_KEY]
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

    suspend fun updateDashboardSettings(update: (DashboardSettings) -> DashboardSettings) {
        context.dataStore.edit { preferences ->
            val current = preferences[DASHBOARD_SETTINGS_KEY]?.let {
                try {
                    Json.decodeFromString<DashboardSettings>(it)
                } catch (e: Exception) {
                    DashboardSettings()
                }
            } ?: DashboardSettings()
            preferences[DASHBOARD_SETTINGS_KEY] = Json.encodeToString(update(current))
        }
    }

    suspend fun updateAccountsViewSettings(update: (AccountsViewSettings) -> AccountsViewSettings) {
        context.dataStore.edit { preferences ->
            val current = preferences[ACCOUNTS_VIEW_SETTINGS_KEY]?.let {
                try {
                    Json.decodeFromString<AccountsViewSettings>(it)
                } catch (e: Exception) {
                    AccountsViewSettings()
                }
            } ?: AccountsViewSettings()
            preferences[ACCOUNTS_VIEW_SETTINGS_KEY] = Json.encodeToString(update(current))
        }
    }

    suspend fun updateAccountDetailSettings(update: (AccountDetailSettings) -> AccountDetailSettings) {
        context.dataStore.edit { preferences ->
            val current = preferences[ACCOUNT_DETAIL_SETTINGS_KEY]?.let {
                try {
                    Json.decodeFromString<AccountDetailSettings>(it)
                } catch (e: Exception) {
                    AccountDetailSettings()
                }
            } ?: AccountDetailSettings()
            preferences[ACCOUNT_DETAIL_SETTINGS_KEY] = Json.encodeToString(update(current))
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

    suspend fun saveShortlistedCurrencies(currencies: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SHORTLISTED_CURRENCIES_KEY] = currencies
        }
    }
}
