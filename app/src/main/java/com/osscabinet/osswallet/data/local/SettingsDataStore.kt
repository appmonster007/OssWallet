package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.osscabinet.ossWallet.ui.components.dashboard.AccountsSectionSettings
import com.osscabinet.ossWallet.ui.components.dashboard.BudgetSettings
import com.osscabinet.ossWallet.ui.components.dashboard.CashFlowSettings
import com.osscabinet.ossWallet.ui.components.dashboard.CurrencyBalancesSettings
import com.osscabinet.ossWallet.ui.components.dashboard.NetWorthSettings
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

class SettingsDataStore(private val context: Context) {

    companion object {
        val CURRENCY_KEY = stringPreferencesKey("currency")
        val THEME_KEY = stringPreferencesKey("theme")
        val CURRENT_PROFILE_ID_KEY = intPreferencesKey("current_profile_id")
        val DECIMAL_PRECISION_KEY = intPreferencesKey("decimal_precision")
        val AUTO_SAVE_EXCHANGE_RATES_KEY = booleanPreferencesKey("auto_save_exchange_rates")
        val IS_ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("is_onboarding_completed")
        
        val DASHBOARD_SETTINGS_KEY = stringPreferencesKey("dashboard_settings")
        val ACCOUNTS_VIEW_SETTINGS_KEY = stringPreferencesKey("accounts_view_settings")
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
}
