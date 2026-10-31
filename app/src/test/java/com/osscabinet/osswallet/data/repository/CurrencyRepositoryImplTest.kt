package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.core.util.TestDispatcherProvider
import com.osscabinet.ossWallet.data.local.dao.CurrencyDao
import com.osscabinet.ossWallet.data.local.entity.CurrencyRateEntity
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.currency.provider.CurrencyMetadata
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProvider
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProviderRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies [CurrencyRepositoryImpl] runs its IO work on the *injected*
 * [com.osscabinet.ossWallet.core.util.DispatcherProvider] (AD-9). Each test drives a
 * [TestDispatcherProvider] backed by `runTest`'s own scheduler — if the repository still
 * used a hardcoded `Dispatchers.IO`, this work would not be controlled by the scheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyRepositoryImplTest {

    private fun repository(
        dao: CurrencyDao,
        provider: ExchangeRateProvider = FakeExchangeRateProvider(),
        currencyProvider: String = "fake",
        dispatcherProvider: TestDispatcherProvider,
    ) = CurrencyRepositoryImpl(
        providerRegistry = ExchangeRateProviderRegistry(listOf(provider)),
        dao = dao,
        settingsRepository = FakeSettingsRepository(currencyProvider),
        dispatcherProvider = dispatcherProvider,
    )

    @Test
    fun `getExchangeRates returns cached rates when cache is valid - runs on injected dispatcher`() = runTest {
        val dao = FakeCurrencyDao(
            rates = listOf(
                rateRow("EUR", base = "USD", rate = "0.90", nextUpdate = Long.MAX_VALUE),
                rateRow("GBP", base = "USD", rate = "0.80", nextUpdate = Long.MAX_VALUE),
            )
        )
        val repo = repository(
            dao = dao,
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )

        val result = repo.getExchangeRates(baseCurrency = "USD", forceRefresh = false)

        assertEquals(mapOf("EUR" to 0.90, "GBP" to 0.80), result)
        // Cache was valid, so no network fetch happened.
        assertEquals(0, dao.replaceCallCount)
    }

    @Test
    fun `fetchFreshRates fetches from the configured provider - runs on injected dispatcher`() = runTest {
        val provider = FakeExchangeRateProvider(
            providerId = "fake",
            rates = mapOf("EUR" to 0.91, "JPY" to 150.0),
        )
        val repo = repository(
            dao = FakeCurrencyDao(),
            provider = provider,
            currencyProvider = "fake",
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )

        val result = repo.fetchFreshRates(baseCurrency = "USD")

        assertEquals("USD", result.baseCurrency)
        assertEquals(mapOf("EUR" to 0.91, "JPY" to 150.0), result.rates)
    }

    private fun rateRow(code: String, base: String, rate: String, nextUpdate: Long) =
        CurrencyRateEntity(
            currencyCode = code,
            baseCurrency = base,
            rate = rate,
            timestamp = 0L,
            nextUpdate = nextUpdate,
        )
}

/** In-memory [CurrencyDao] fake (no Room). */
private class FakeCurrencyDao(
    private val rates: List<CurrencyRateEntity> = emptyList(),
) : CurrencyDao {
    var replaceCallCount = 0
        private set

    override suspend fun getAllRates(): List<CurrencyRateEntity> = rates
    override suspend fun getRatesForBase(baseCurrency: String): List<CurrencyRateEntity> =
        rates.filter { it.baseCurrency == baseCurrency }
    override fun observeRatesForBase(baseCurrency: String): Flow<List<CurrencyRateEntity>> =
        flowOf(rates.filter { it.baseCurrency == baseCurrency })
    override suspend fun getCachedBaseCurrencies(): List<String> = rates.map { it.baseCurrency }.distinct()
    override suspend fun insertRates(rates: List<CurrencyRateEntity>) { replaceCallCount++ }
    override suspend fun clearAll() {}
    override suspend fun deleteRatesForBase(baseCurrency: String) {}
}

/** Minimal [ExchangeRateProvider] fake. */
private class FakeExchangeRateProvider(
    private val providerId: String = "fake",
    private val rates: Map<String, Double> = emptyMap(),
) : ExchangeRateProvider {
    override val id: String = providerId
    override val name: String = "Fake"
    override val attribution: String = "Fake"
    override suspend fun fetchSupportedCurrencies(): List<CurrencyMetadata> = emptyList()
    override suspend fun fetchRate(base: String, target: String): Result<Double> =
        Result.success(rates[target] ?: 1.0)
    override suspend fun fetchAllRates(base: String): Result<Map<String, Double>> = Result.success(rates)
    override suspend fun convertCurrencyAmount(amount: Double, from: String, to: String): Result<Double> =
        Result.success(amount * (rates[to] ?: 1.0))
}

/**
 * Fake [SettingsRepository] exposing only [currencyProvider] (the single field the
 * currency repository reads). All other members are unused by these tests and throw.
 */
private class FakeSettingsRepository(currencyProviderId: String) : SettingsRepository {
    override val currencyProvider: Flow<String> = MutableStateFlow(currencyProviderId)

    private val unused: Nothing get() = throw NotImplementedError("Not needed for CurrencyRepositoryImplTest")

    override val currency: Flow<String> get() = unused
    override val theme: Flow<String> get() = unused
    override val currentProfileId: Flow<Int> get() = unused
    override val decimalPrecision: Flow<Int> get() = unused
    override val autoSaveExchangeRates: Flow<Boolean> get() = unused
    override val isOnboardingCompleted: Flow<Boolean> get() = unused
    override val cardAnimationEnabled: Flow<Boolean> get() = unused
    override val cardOpenAnimation: Flow<String> get() = unused
    override val expandableFabEnabled: Flow<Boolean> get() = unused
    override val isDatabaseSeeded: Flow<Boolean> get() = unused
    override val use24HourFormat: Flow<Boolean> get() = unused
    override val mapsAccessEnabled: Flow<Boolean> get() = unused
    override val recentActivitiesBreakdown: Flow<Boolean> get() = unused
    override val userName: Flow<String> get() = unused
    override val useSystemFont: Flow<Boolean> get() = unused
    override val selectedFont: Flow<String> get() = unused
    override val uiIdentity: Flow<String> get() = unused
    override val dynamicAccent: Flow<Boolean> get() = unused
    override val numberingSystem: Flow<String> get() = unused
    override val recordsSettings: Flow<TransactionSectionSettings> get() = unused
    override val dashboardCardsConfig: Flow<String> get() = unused
    override val accountDetailCardsConfig: Flow<String> get() = unused
    override val accountsViewCardsConfig: Flow<String> get() = unused
    override val analyticsCardsConfig: Flow<String> get() = unused
    override val analyticsCommonFilterEnabled: Flow<Boolean> get() = unused
    override val shortlistedCurrencies: Flow<Set<String>?> get() = unused
    override val glassmorphismEnabled: Flow<Boolean> get() = unused
    override val contourBgVersion: Flow<Int> get() = unused
    override val contourLinesEnabled: Flow<Boolean> get() = unused
    override val randomizeContourEachLoad: Flow<Boolean> get() = unused
    override val glassSettings: Flow<GlassSettings> get() = unused
    override val autoBackupConfig: Flow<AutoBackupConfig> get() = unused
    override val uiDensity: Flow<String> get() = unused
    override val cardListEdgeSpacing: Flow<Int> get() = unused
    override val cardListGap: Flow<Int> get() = unused
    override val buyMeACoffeePromptLastShownWindow: Flow<Long> get() = unused

    override suspend fun saveCurrency(currency: String) = unused
    override suspend fun saveTheme(theme: String) = unused
    override suspend fun saveCurrentProfileId(profileId: Int) = unused
    override suspend fun saveDecimalPrecision(precision: Int) = unused
    override suspend fun saveAutoSaveExchangeRates(enabled: Boolean) = unused
    override suspend fun saveOnboardingCompleted(completed: Boolean) = unused
    override suspend fun saveCardAnimationEnabled(enabled: Boolean) = unused
    override suspend fun saveCardOpenAnimation(name: String) = unused
    override suspend fun saveExpandableFabEnabled(enabled: Boolean) = unused
    override suspend fun saveDatabaseSeeded(seeded: Boolean) = unused
    override suspend fun saveUse24HourFormat(use24Hour: Boolean) = unused
    override suspend fun saveMapsAccessEnabled(enabled: Boolean) = unused
    override suspend fun saveRecentActivitiesBreakdown(enabled: Boolean) = unused
    override suspend fun saveCurrencyProvider(providerId: String) = unused
    override suspend fun saveUserName(name: String) = unused
    override suspend fun saveUseSystemFont(enabled: Boolean) = unused
    override suspend fun saveSelectedFont(fontName: String) = unused
    override suspend fun saveUiIdentity(identity: String) = unused
    override suspend fun saveDynamicAccent(enabled: Boolean) = unused
    override suspend fun saveNumberingSystem(system: String) = unused
    override suspend fun updateRecordsSettings(update: (TransactionSectionSettings) -> TransactionSectionSettings) = unused
    override suspend fun saveDashboardCardsConfig(config: String) = unused
    override suspend fun saveAccountDetailCardsConfig(config: String) = unused
    override suspend fun saveAccountsViewCardsConfig(config: String) = unused
    override suspend fun saveAnalyticsCardsConfig(config: String) = unused
    override suspend fun saveAnalyticsCommonFilterEnabled(enabled: Boolean) = unused
    override suspend fun saveShortlistedCurrencies(currencies: Set<String>) = unused
    override suspend fun saveGlassmorphismEnabled(enabled: Boolean) = unused
    override suspend fun saveContourBgVersion(version: Int) = unused
    override suspend fun saveContourLinesEnabled(enabled: Boolean) = unused
    override suspend fun saveRandomizeContourEachLoad(enabled: Boolean) = unused
    override suspend fun updateGlassSettings(update: (GlassSettings) -> GlassSettings) = unused
    override suspend fun saveAutoBackupConfig(config: AutoBackupConfig) = unused
    override suspend fun saveUiDensity(name: String) = unused
    override suspend fun saveCardListEdgeSpacing(dp: Int) = unused
    override suspend fun saveCardListGap(dp: Int) = unused
    override suspend fun saveBuyMeACoffeePromptLastShownWindow(epochDay: Long) = unused
}
