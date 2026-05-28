package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val currency: Flow<String>
    val theme: Flow<String>
    val currentProfileId: Flow<Int>
    val decimalPrecision: Flow<Int>
    val autoSaveExchangeRates: Flow<Boolean>
    val isOnboardingCompleted: Flow<Boolean>
    val cardAnimationEnabled: Flow<Boolean>
    val expandableFabEnabled: Flow<Boolean>
    val isDatabaseSeeded: Flow<Boolean>
    val use24HourFormat: Flow<Boolean>
    val mapsAccessEnabled: Flow<Boolean>
    val recentActivitiesBreakdown: Flow<Boolean>
    val currencyProvider: Flow<String>
    val userName: Flow<String>
    val useSystemFont: Flow<Boolean>
    val selectedFont: Flow<String>
    val numberingSystem: Flow<String>
    val recordsSettings: Flow<TransactionSectionSettings>
    val dashboardCardsConfig: Flow<String>
    val accountDetailCardsConfig: Flow<String>
    val accountsViewCardsConfig: Flow<String>
    val analyticsCardsConfig: Flow<String>
    val analyticsCommonFilterEnabled: Flow<Boolean>
    val shortlistedCurrencies: Flow<Set<String>?>

    suspend fun saveCurrency(currency: String)
    suspend fun saveTheme(theme: String)
    suspend fun saveCurrentProfileId(profileId: Int)
    suspend fun saveDecimalPrecision(precision: Int)
    suspend fun saveAutoSaveExchangeRates(enabled: Boolean)
    suspend fun saveOnboardingCompleted(completed: Boolean)
    suspend fun saveCardAnimationEnabled(enabled: Boolean)
    suspend fun saveExpandableFabEnabled(enabled: Boolean)
    suspend fun saveDatabaseSeeded(seeded: Boolean)
    suspend fun saveUse24HourFormat(use24Hour: Boolean)
    suspend fun saveMapsAccessEnabled(enabled: Boolean)
    suspend fun saveRecentActivitiesBreakdown(enabled: Boolean)
    suspend fun saveCurrencyProvider(providerId: String)
    suspend fun saveUserName(name: String)
    suspend fun saveUseSystemFont(enabled: Boolean)
    suspend fun saveSelectedFont(fontName: String)
    suspend fun saveNumberingSystem(system: String)
    suspend fun updateRecordsSettings(update: (TransactionSectionSettings) -> TransactionSectionSettings)
    suspend fun saveDashboardCardsConfig(config: String)
    suspend fun saveAccountDetailCardsConfig(config: String)
    suspend fun saveAccountsViewCardsConfig(config: String)
    suspend fun saveAnalyticsCardsConfig(config: String)
    suspend fun saveAnalyticsCommonFilterEnabled(enabled: Boolean)
    suspend fun saveShortlistedCurrencies(currencies: Set<String>)
}
