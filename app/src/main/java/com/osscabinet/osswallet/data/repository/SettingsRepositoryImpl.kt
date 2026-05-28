package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.AccountDetailSettings
import com.osscabinet.ossWallet.data.local.AccountsViewSettings
import com.osscabinet.ossWallet.data.local.DashboardSettings
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {
    override val currency: Flow<String> = settingsDataStore.currency
    override val theme: Flow<String> = settingsDataStore.theme
    override val currentProfileId: Flow<Int> = settingsDataStore.currentProfileId
    override val decimalPrecision: Flow<Int> = settingsDataStore.decimalPrecision
    override val autoSaveExchangeRates: Flow<Boolean> = settingsDataStore.autoSaveExchangeRates
    override val isOnboardingCompleted: Flow<Boolean> = settingsDataStore.isOnboardingCompleted
    override val cardAnimationEnabled: Flow<Boolean> = settingsDataStore.cardAnimationEnabled
    override val expandableFabEnabled: Flow<Boolean> = settingsDataStore.expandableFabEnabled
    override val isDatabaseSeeded: Flow<Boolean> = settingsDataStore.isDatabaseSeeded
    override val use24HourFormat: Flow<Boolean> = settingsDataStore.use24HourFormat
    override val mapsAccessEnabled: Flow<Boolean> = settingsDataStore.mapsAccessEnabled
    override val recentActivitiesBreakdown: Flow<Boolean> =
        settingsDataStore.recentActivitiesBreakdown
    override val currencyProvider: Flow<String> = settingsDataStore.currencyProvider
    override val userName: Flow<String> = settingsDataStore.userName
    override val useSystemFont: Flow<Boolean> = settingsDataStore.useSystemFont
    override val selectedFont: Flow<String> = settingsDataStore.selectedFont
    override val numberingSystem: Flow<String> = settingsDataStore.numberingSystem
    override val dashboardSettings: Flow<DashboardSettings> = settingsDataStore.dashboardSettings
    override val accountsViewSettings: Flow<AccountsViewSettings> =
        settingsDataStore.accountsViewSettings
    override val accountDetailSettings: Flow<AccountDetailSettings> =
        settingsDataStore.accountDetailSettings
    override val recordsSettings: Flow<TransactionSectionSettings> =
        settingsDataStore.recordsSettings
    override val dashboardCardsConfig: Flow<String> = settingsDataStore.dashboardCardsConfig
    override val accountDetailCardsConfig: Flow<String> = settingsDataStore.accountDetailCardsConfig
    override val accountsViewCardsConfig: Flow<String> = settingsDataStore.accountsViewCardsConfig
    override val analyticsCardsConfig: Flow<String> = settingsDataStore.analyticsCardsConfig
    override val shortlistedCurrencies: Flow<Set<String>?> = settingsDataStore.shortlistedCurrencies

    override suspend fun saveCurrency(currency: String) = settingsDataStore.saveCurrency(currency)
    override suspend fun saveTheme(theme: String) = settingsDataStore.saveTheme(theme)
    override suspend fun saveCurrentProfileId(profileId: Int) =
        settingsDataStore.saveCurrentProfileId(profileId)

    override suspend fun saveDecimalPrecision(precision: Int) =
        settingsDataStore.saveDecimalPrecision(precision)

    override suspend fun saveAutoSaveExchangeRates(enabled: Boolean) =
        settingsDataStore.saveAutoSaveExchangeRates(enabled)

    override suspend fun saveOnboardingCompleted(completed: Boolean) =
        settingsDataStore.saveOnboardingCompleted(completed)

    override suspend fun saveCardAnimationEnabled(enabled: Boolean) =
        settingsDataStore.saveCardAnimationEnabled(enabled)

    override suspend fun saveExpandableFabEnabled(enabled: Boolean) =
        settingsDataStore.saveExpandableFabEnabled(enabled)

    override suspend fun saveDatabaseSeeded(seeded: Boolean) =
        settingsDataStore.saveDatabaseSeeded(seeded)

    override suspend fun saveUse24HourFormat(use24Hour: Boolean) =
        settingsDataStore.saveUse24HourFormat(use24Hour)

    override suspend fun saveMapsAccessEnabled(enabled: Boolean) =
        settingsDataStore.saveMapsAccessEnabled(enabled)

    override suspend fun saveRecentActivitiesBreakdown(enabled: Boolean) =
        settingsDataStore.saveRecentActivitiesBreakdown(enabled)

    override suspend fun saveCurrencyProvider(providerId: String) =
        settingsDataStore.saveCurrencyProvider(providerId)

    override suspend fun saveUserName(name: String) = settingsDataStore.saveUserName(name)
    override suspend fun saveUseSystemFont(enabled: Boolean) =
        settingsDataStore.saveUseSystemFont(enabled)

    override suspend fun saveSelectedFont(fontName: String) =
        settingsDataStore.saveSelectedFont(fontName)

    override suspend fun saveNumberingSystem(system: String) =
        settingsDataStore.saveNumberingSystem(system)

    override suspend fun updateDashboardSettings(update: (DashboardSettings) -> DashboardSettings) =
        settingsDataStore.updateDashboardSettings(update)

    override suspend fun updateAccountsViewSettings(update: (AccountsViewSettings) -> AccountsViewSettings) =
        settingsDataStore.updateAccountsViewSettings(update)

    override suspend fun updateAccountDetailSettings(update: (AccountDetailSettings) -> AccountDetailSettings) =
        settingsDataStore.updateAccountDetailSettings(update)

    override suspend fun updateRecordsSettings(update: (TransactionSectionSettings) -> TransactionSectionSettings) =
        settingsDataStore.updateRecordsSettings(update)

    override suspend fun saveDashboardCardsConfig(config: String) =
        settingsDataStore.saveDashboardCardsConfig(config)

    override suspend fun saveAccountDetailCardsConfig(config: String) =
        settingsDataStore.saveAccountDetailCardsConfig(config)

    override suspend fun saveAccountsViewCardsConfig(config: String) =
        settingsDataStore.saveAccountsViewCardsConfig(config)

    override suspend fun saveAnalyticsCardsConfig(config: String) =
        settingsDataStore.saveAnalyticsCardsConfig(config)

    override suspend fun saveShortlistedCurrencies(currencies: Set<String>) =
        settingsDataStore.saveShortlistedCurrencies(currencies)
}
