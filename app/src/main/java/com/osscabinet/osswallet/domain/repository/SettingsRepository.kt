package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.data.local.*
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
    val dashboardSettings: Flow<DashboardSettings>
    val accountsViewSettings: Flow<AccountsViewSettings>
    val accountDetailSettings: Flow<AccountDetailSettings>
    val recordsSettings: Flow<TransactionSectionSettings>
    val dashboardCardsConfig: Flow<String>
    val accountDetailCardsConfig: Flow<String>
    val accountsViewCardsConfig: Flow<String>
    val analyticsCardsConfig: Flow<String>

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
    suspend fun updateDashboardSettings(update: (DashboardSettings) -> DashboardSettings)
    suspend fun updateAccountsViewSettings(update: (AccountsViewSettings) -> AccountsViewSettings)
    suspend fun updateAccountDetailSettings(update: (AccountDetailSettings) -> AccountDetailSettings)
    suspend fun updateRecordsSettings(update: (TransactionSectionSettings) -> TransactionSectionSettings)
    suspend fun saveDashboardCardsConfig(config: String)
    suspend fun saveAccountDetailCardsConfig(config: String)
    suspend fun saveAccountsViewCardsConfig(config: String)
    suspend fun saveAnalyticsCardsConfig(config: String)
}
