package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.osscabinet.ossWallet.domain.model.settings.AccountsSectionSettings
import com.osscabinet.ossWallet.domain.model.settings.CashFlowSettings
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesSettings
import com.osscabinet.ossWallet.domain.model.settings.NetWorthSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Serializable
data class LayoutSettings(
    val sectionOrder: String? = null
)

@Serializable
data class BudgetSettings(
    val showProgressLabels: Boolean = true
)

@Serializable
data class DashboardSettings(
    val accountsSection: AccountsSectionSettings = AccountsSectionSettings(gridColumns = 2),
    val currencyBalancesSection: CurrencyBalancesSettings = CurrencyBalancesSettings(),
    val netWorthSection: NetWorthSettings = NetWorthSettings(),
    val cashFlowSection: CashFlowSettings = CashFlowSettings(),
    val budgetSection: BudgetSettings = BudgetSettings(),
    val layout: LayoutSettings = LayoutSettings()
)

@Serializable
data class AccountsViewSettings(
    val accountsSection: AccountsSectionSettings = AccountsSectionSettings(gridColumns = 1),
    val currencyBalancesSection: CurrencyBalancesSettings = CurrencyBalancesSettings(),
    val netWorthSection: NetWorthSettings = NetWorthSettings(),
    val cashFlowSection: CashFlowSettings = CashFlowSettings(),
    val layout: LayoutSettings = LayoutSettings()
)

@Serializable
enum class TransactionGroupingType {
    BY_DAY, BY_WEEK
}

@Serializable
data class TransactionSectionSettings(
    val showPlannedPayments: Boolean = false,
    val useNoteAsHeader: Boolean = false,
    val showOpeningBalance: Boolean = false,
    val showClosingBalance: Boolean = true,
    val groupBy: TransactionGroupingType = TransactionGroupingType.BY_WEEK,
    val showOnlyActiveCurrencies: Boolean = false,
    val excludeFutureTransactions: Boolean = true
)

@Serializable
data class AccountDetailSettings(
    val currencyBalancesSection: CurrencyBalancesSettings = CurrencyBalancesSettings(),
    val cashFlowSection: CashFlowSettings = CashFlowSettings(),
    val transactionsSection: TransactionSectionSettings = TransactionSectionSettings()
)

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
        val CURRENCY_PROVIDER_KEY = stringPreferencesKey("currency_provider")
        val USER_NAME_KEY = stringPreferencesKey("user_name")

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
        preferences[USE_24_HOUR_FORMAT_KEY] ?: android.text.format.DateFormat.is24HourFormat(context)
    }

    val mapsAccessEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MAPS_ACCESS_ENABLED_KEY] ?: false
    }

    val currencyProvider: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY_PROVIDER_KEY] ?: "exchangerate-api"
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY] ?: ""
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

    val accountsViewSettings: Flow<AccountsViewSettings> = context.dataStore.data.map { preferences ->
        preferences[ACCOUNTS_VIEW_SETTINGS_KEY]?.let {
            try {
                Json.decodeFromString<AccountsViewSettings>(it)
            } catch (e: Exception) {
                AccountsViewSettings()
            }
        } ?: AccountsViewSettings()
    }

    val accountDetailSettings: Flow<AccountDetailSettings> = context.dataStore.data.map { preferences ->
        preferences[ACCOUNT_DETAIL_SETTINGS_KEY]?.let {
            try {
                Json.decodeFromString<AccountDetailSettings>(it)
            } catch (e: Exception) {
                AccountDetailSettings()
            }
        } ?: AccountDetailSettings()
    }

    val recordsSettings: Flow<TransactionSectionSettings> = context.dataStore.data.map { preferences ->
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
}
