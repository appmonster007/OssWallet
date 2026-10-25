package com.osscabinet.ossWallet.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.domain.model.settings.CardOpenAnimation
import com.osscabinet.ossWallet.domain.model.settings.FastScrollSide
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.GlassStyleConfig
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.repository.BackupMeta
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.BackupVerification
import com.osscabinet.ossWallet.domain.repository.BackupScope
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.RestorableEntity
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.BackupScheduler
import com.osscabinet.ossWallet.domain.service.currency.provider.CurrencyMetadata
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProvider
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProviderRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Error(val message: String) : SyncState()
    object Success : SyncState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository,
    private val currencyRepository: CurrencyRepository,
    private val providerRegistry: ExchangeRateProviderRegistry,
    private val backupScheduler: BackupScheduler
) : ViewModel() {

    val currency: StateFlow<String> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val theme: StateFlow<String> = settingsRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    val decimalPrecision: StateFlow<Int> = settingsRepository.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val numberingSystem: StateFlow<String> = settingsRepository.numberingSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "WESTERN")

    val autoSaveExchangeRates: StateFlow<Boolean> = settingsRepository.autoSaveExchangeRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val cardAnimationEnabled: StateFlow<Boolean> = settingsRepository.cardAnimationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val cardOpenAnimation: StateFlow<String> = settingsRepository.cardOpenAnimation
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CardOpenAnimation.DEFAULT.name
        )

    val expandableFabEnabled: StateFlow<Boolean> = settingsRepository.expandableFabEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val use24HourFormat: StateFlow<Boolean> = settingsRepository.use24HourFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val mapsAccessEnabled: StateFlow<Boolean> = settingsRepository.mapsAccessEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currencyProvider: StateFlow<String> = settingsRepository.currencyProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "exchangerate-api")

    val useSystemFont: StateFlow<Boolean> = settingsRepository.useSystemFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val selectedFont: StateFlow<String> = settingsRepository.selectedFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Default")

    val dynamicAccent: StateFlow<Boolean> = settingsRepository.dynamicAccent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val glassmorphismEnabled: StateFlow<Boolean> = settingsRepository.glassmorphismEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val contourBgVersion: StateFlow<Int> = settingsRepository.contourBgVersion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val contourLinesEnabled: StateFlow<Boolean> = settingsRepository.contourLinesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val randomizeContourEachLoad: StateFlow<Boolean> = settingsRepository.randomizeContourEachLoad
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val glassSettings: StateFlow<GlassSettings> = settingsRepository.glassSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlassSettings())

    val uiDensity: StateFlow<String> = settingsRepository.uiDensity
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            com.osscabinet.ossWallet.domain.model.settings.UiDensity.DEFAULT.name
        )

    val cardListEdgeSpacing: StateFlow<Int> = settingsRepository.cardListEdgeSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14)

    val cardListGap: StateFlow<Int> = settingsRepository.cardListGap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 12)

    val recentActivitiesBreakdown: StateFlow<Boolean> = settingsRepository.recentActivitiesBreakdown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val recordsSettings: StateFlow<TransactionSectionSettings> = settingsRepository.recordsSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionSectionSettings())

    val shortlistedCurrencies: StateFlow<Set<String>?> = settingsRepository.shortlistedCurrencies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _availableCurrencies = MutableStateFlow<List<CurrencyMetadata>>(emptyList())
    val availableCurrencies: StateFlow<List<CurrencyMetadata>> = _availableCurrencies

    val availableProviders: List<ExchangeRateProvider> = providerRegistry.getAllProviders()

    val isOnboardingCompleted: StateFlow<Boolean?> = settingsRepository.isOnboardingCompleted
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<Profile>> = profileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentProfile: StateFlow<Profile?> = combine(
        profileRepository.getAllProfiles(),
        settingsRepository.currentProfileId
    ) { profiles, id ->
        profiles.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    data class DashboardDisplayPrefs(
        val currentProfileName: String?,
        val cardAnimationEnabled: Boolean,
        val expandableFabEnabled: Boolean,
        val recentActivitiesBreakdown: Boolean,
        val fastScrollSide: FastScrollSide = FastScrollSide.RIGHT
    )

    val dashboardDisplayPrefs: StateFlow<DashboardDisplayPrefs> = combine(
        currentProfile,
        cardAnimationEnabled,
        expandableFabEnabled,
        recentActivitiesBreakdown,
        settingsRepository.recordsSettings
    ) { profile, anim, fab, breakdown, records ->
        DashboardDisplayPrefs(
            currentProfileName = profile?.name,
            cardAnimationEnabled = anim,
            expandableFabEnabled = fab,
            recentActivitiesBreakdown = breakdown,
            fastScrollSide = records.fastScrollSide
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardDisplayPrefs(
            currentProfileName = null,
            cardAnimationEnabled = true,
            expandableFabEnabled = true,
            recentActivitiesBreakdown = true,
            fastScrollSide = FastScrollSide.RIGHT
        )
    )

    private val _onboardingSyncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val onboardingSyncState: StateFlow<SyncState> = _onboardingSyncState

    private val _backupMessage = Channel<String>(Channel.BUFFERED)
    val backupMessage: Flow<String> = _backupMessage.receiveAsFlow()

    val autoBackupConfig: StateFlow<AutoBackupConfig> = settingsRepository.autoBackupConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoBackupConfig())

    private val _recentBackups = MutableStateFlow<List<BackupMeta>>(emptyList())
    val recentBackups: StateFlow<List<BackupMeta>> = _recentBackups

    init {
        viewModelScope.launch {
            val providerId = settingsRepository.currencyProvider.first()
            val provider = providerRegistry.getProvider(providerId)
            _availableCurrencies.value = provider.fetchSupportedCurrencies()
        }
        viewModelScope.launch { refreshRecentBackups() }
    }

    private suspend fun refreshRecentBackups() {
        _recentBackups.value = backupRepository.listPrivateBackups()
    }

    fun updateAutoBackupConfig(config: AutoBackupConfig) {
        viewModelScope.launch {
            settingsRepository.saveAutoBackupConfig(config)
            backupScheduler.schedule(config)
        }
    }

    fun deleteBackup(id: String) {
        viewModelScope.launch {
            backupRepository.deleteBackup(id)
            refreshRecentBackups()
        }
    }

    fun setCurrency(currencyCode: String) {
        viewModelScope.launch {
            settingsRepository.saveCurrency(currencyCode)
            // ComputationRepository reacts to the currency change and fetches fresh rates
            // automatically. fetchAndSaveRates is NOT called here to avoid a concurrent
            // duplicate fetch race condition.
        }
    }

    fun setShortlistedCurrencies(currencies: Set<String>) {
        viewModelScope.launch {
            settingsRepository.saveShortlistedCurrencies(currencies)
        }
    }

    fun fetchAndSaveRates(baseCurrency: String) {
        viewModelScope.launch {
            _onboardingSyncState.value = SyncState.Loading
            try {
                val rates = currencyRepository.fetchFreshRates(baseCurrency)
                currencyRepository.saveRates(baseCurrency, rates)
                _onboardingSyncState.value = SyncState.Success
            } catch (e: Exception) {
                _onboardingSyncState.value = SyncState.Error(e.message ?: "Failed to fetch rates")
            }
        }
    }

    fun setTheme(themeName: String) {
        viewModelScope.launch {
            settingsRepository.saveTheme(themeName)
        }
    }

    fun setDecimalPrecision(precision: Int) {
        viewModelScope.launch {
            settingsRepository.saveDecimalPrecision(precision)
        }
    }

    fun setNumberingSystem(system: String) {
        viewModelScope.launch {
            settingsRepository.saveNumberingSystem(system)
        }
    }

    fun setAutoSaveExchangeRates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveAutoSaveExchangeRates(enabled)
        }
    }

    fun setCardAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveCardAnimationEnabled(enabled)
        }
    }

    fun setCardOpenAnimation(animation: CardOpenAnimation) {
        viewModelScope.launch {
            settingsRepository.saveCardOpenAnimation(animation.name)
        }
    }

    fun setExpandableFabEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveExpandableFabEnabled(enabled)
        }
    }

    fun setUse24HourFormat(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveUse24HourFormat(enabled)
        }
    }

    fun setMapsAccessEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveMapsAccessEnabled(enabled)
        }
    }

    fun setRecentActivitiesBreakdown(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveRecentActivitiesBreakdown(enabled)
        }
    }

    fun updateRecordsSettings(update: (TransactionSectionSettings) -> TransactionSectionSettings) {
        viewModelScope.launch {
            settingsRepository.updateRecordsSettings(update)
        }
    }

    fun setCurrencyProvider(providerId: String) {
        viewModelScope.launch {
            settingsRepository.saveCurrencyProvider(providerId)
            val provider = providerRegistry.getProvider(providerId)
            _availableCurrencies.value = provider.fetchSupportedCurrencies()
        }
    }

    fun setUseSystemFont(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveUseSystemFont(enabled)
        }
    }

    fun setSelectedFont(fontName: String) {
        viewModelScope.launch {
            settingsRepository.saveSelectedFont(fontName)
        }
    }

    fun setUiDensity(density: com.osscabinet.ossWallet.domain.model.settings.UiDensity) {
        viewModelScope.launch {
            settingsRepository.saveUiDensity(density.name)
        }
    }

    fun setCardListEdgeSpacing(dp: Int) {
        viewModelScope.launch {
            settingsRepository.saveCardListEdgeSpacing(dp)
        }
    }

    fun setCardListGap(dp: Int) {
        viewModelScope.launch {
            settingsRepository.saveCardListGap(dp)
        }
    }

    fun setDynamicAccent(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveDynamicAccent(enabled)
        }
    }

    fun setGlassmorphismEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveGlassmorphismEnabled(enabled)
        }
    }

    fun setContourBgVersion(version: Int) {
        viewModelScope.launch {
            settingsRepository.saveContourBgVersion(version)
        }
    }

    fun setContourLinesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveContourLinesEnabled(enabled)
        }
    }

    fun setRandomizeContourEachLoad(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveRandomizeContourEachLoad(enabled)
        }
    }

    fun updateGlassSettings(update: (GlassSettings) -> GlassSettings) {
        viewModelScope.launch {
            settingsRepository.updateGlassSettings(update)
        }
    }

    fun updateGlassStyle(style: String, update: (GlassStyleConfig) -> GlassStyleConfig) {
        viewModelScope.launch {
            settingsRepository.updateGlassSettings { current ->
                when (style) {
                    "Standard" -> current.copy(standard = update(current.standard))
                    "Drawer" -> current.copy(drawer = update(current.drawer))
                    "Pill" -> current.copy(pill = update(current.pill))
                    "Card" -> current.copy(card = update(current.card))
                    "Sheet" -> current.copy(sheet = update(current.sheet))
                    "Popup" -> current.copy(popup = update(current.popup))
                    "Item" -> current.copy(item = update(current.item))
                    else -> current
                }
            }
        }
    }

    fun resetGlassSettings() {
        viewModelScope.launch {
            settingsRepository.updateGlassSettings { GlassSettings() }
        }
    }

    fun resetGlassStyle(style: String) {
        viewModelScope.launch {
            val defaults = GlassSettings()
            settingsRepository.updateGlassSettings { current ->
                when (style) {
                    "Standard" -> current.copy(standard = defaults.standard)
                    "Drawer" -> current.copy(drawer = defaults.drawer)
                    "Pill" -> current.copy(pill = defaults.pill)
                    "Card" -> current.copy(card = defaults.card)
                    "Sheet" -> current.copy(sheet = defaults.sheet)
                    "Popup" -> current.copy(popup = defaults.popup)
                    "Item" -> current.copy(item = defaults.item)
                    else -> current
                }
            }
        }
    }

    fun resetGlassParameter(style: String, parameter: String) {
        viewModelScope.launch {
            val defaults = GlassSettings()
            val defaultStyle = when (style) {
                "Standard" -> defaults.standard
                "Drawer" -> defaults.drawer
                "Pill" -> defaults.pill
                "Card" -> defaults.card
                "Sheet" -> defaults.sheet
                "Popup" -> defaults.popup
                "Item" -> defaults.item
                else -> return@launch
            }

            settingsRepository.updateGlassSettings { current ->
                val currentStyle = when (style) {
                    "Standard" -> current.standard
                    "Drawer" -> current.drawer
                    "Pill" -> current.pill
                    "Card" -> current.card
                    "Sheet" -> current.sheet
                    "Popup" -> current.popup
                    "Item" -> current.item
                    else -> return@updateGlassSettings current
                }

                val updatedStyle = when (parameter) {
                    "blurRadius" -> currentStyle.copy(glass = currentStyle.glass.copy(blurRadius = defaultStyle.glass.blurRadius))
                    "lightAlpha" -> currentStyle.copy(glass = currentStyle.glass.copy(lightAlpha = defaultStyle.glass.lightAlpha))
                    "darkAlpha" -> currentStyle.copy(glass = currentStyle.glass.copy(darkAlpha = defaultStyle.glass.darkAlpha))
                    "noiseFactor" -> currentStyle.copy(glass = currentStyle.glass.copy(noiseFactor = defaultStyle.glass.noiseFactor))
                    "shadowElevation" -> currentStyle.copy(
                        surface = currentStyle.surface.copy(
                            shadowElevation = defaultStyle.surface.shadowElevation
                        )
                    )

                    "borderAlpha" -> currentStyle.copy(
                        surface = currentStyle.surface.copy(
                            borderAlpha = defaultStyle.surface.borderAlpha
                        )
                    )

                    "ambientShadowAlpha" -> currentStyle.copy(
                        surface = currentStyle.surface.copy(
                            ambientShadowAlpha = defaultStyle.surface.ambientShadowAlpha
                        )
                    )

                    "spotShadowAlpha" -> currentStyle.copy(
                        surface = currentStyle.surface.copy(
                            spotShadowAlpha = defaultStyle.surface.spotShadowAlpha
                        )
                    )

                    "borderWidth" -> currentStyle.copy(
                        surface = currentStyle.surface.copy(
                            borderWidth = defaultStyle.surface.borderWidth
                        )
                    )

                    else -> currentStyle
                }

                when (style) {
                    "Standard" -> current.copy(standard = updatedStyle)
                    "Drawer" -> current.copy(drawer = updatedStyle)
                    "Pill" -> current.copy(pill = updatedStyle)
                    "Card" -> current.copy(card = updatedStyle)
                    "Sheet" -> current.copy(sheet = updatedStyle)
                    "Popup" -> current.copy(popup = updatedStyle)
                    "Item" -> current.copy(item = updatedStyle)
                    else -> current
                }
            }
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveOnboardingCompleted(completed)
        }
    }

    fun switchProfile(profileId: Int) {
        viewModelScope.launch {
            settingsRepository.saveCurrentProfileId(profileId)
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            profileRepository.insertProfile(Profile(name = name, baseCurrency = currency.value))
        }
    }

    fun exportBackup(
        uri: Uri,
        type: BackupType,
        scope: BackupScope = BackupScope.ALL_PROFILES,
        passphrase: CharArray? = null
    ) {
        viewModelScope.launch {
            val result = backupRepository.exportData(uri.toString(), type, scope, passphrase)
            passphrase?.fill('0')
            _backupMessage.send(
                if (result.isSuccess) {
                    if (passphrase != null) "Encrypted backup exported successfully" else "Backup exported successfully"
                } else "Export failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    suspend fun isEncryptedBackup(uri: Uri): Boolean = backupRepository.isEncryptedBackup(uri.toString())

    suspend fun detectBackupEntity(uri: Uri, passphrase: CharArray? = null): RestorableEntity? =
        backupRepository.detectBackupEntity(uri.toString(), passphrase)

    /** Checks a backup (decrypting it if needed) without touching live data. */
    suspend fun verifyBackup(uri: Uri, type: BackupType, passphrase: CharArray?): Result<BackupVerification> =
        backupRepository.verifyBackup(uri.toString(), type, passphrase)

    fun reportBackupMessage(message: String) {
        viewModelScope.launch { _backupMessage.send(message) }
    }

    private val _backupKeyAvailable = MutableStateFlow(true)
    val backupKeyAvailable: StateFlow<Boolean> = _backupKeyAvailable

    init {
        viewModelScope.launch { _backupKeyAvailable.value = backupRepository.hasBackupEncryptionKey() }
    }

    fun enableBackupEncryption(passphrase: CharArray) {
        viewModelScope.launch {
            val result = backupRepository.enableBackupEncryption(passphrase)
            passphrase.fill('0')
            if (result.isSuccess) {
                _backupKeyAvailable.value = true
                updateAutoBackupConfig(autoBackupConfig.value.copy(encrypt = true))
                _backupMessage.send("Scheduled backups are now encrypted")
            } else {
                _backupMessage.send("Could not enable encryption: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun disableBackupEncryption() {
        viewModelScope.launch {
            backupRepository.disableBackupEncryption()
            _backupKeyAvailable.value = false
            updateAutoBackupConfig(autoBackupConfig.value.copy(encrypt = false))
            _backupMessage.send("Scheduled backups are no longer encrypted")
        }
    }

    /**
     * Verifies first, so a wrong passphrase or damaged file is rejected before any live data
     * is cleared, then restores.
     */
    fun importBackup(
        uri: Uri,
        type: BackupType,
        entities: Set<RestorableEntity>? = null,
        passphrase: CharArray? = null
    ) {
        viewModelScope.launch {
            val check = backupRepository.verifyBackup(uri.toString(), type, passphrase)
            if (check.isFailure) {
                passphrase?.fill('0')
                _backupMessage.send("Import failed: ${check.exceptionOrNull()?.message}. Nothing was changed.")
                return@launch
            }
            val result = if (entities != null && entities.size < RestorableEntity.entries.size) {
                backupRepository.importDataSelective(uri.toString(), type, entities, passphrase)
            } else {
                backupRepository.importData(uri.toString(), type, passphrase)
            }
            passphrase?.fill('0')

            if (result.isSuccess) {
                if (type == BackupType.SQLITE) {
                    _backupMessage.send("Database restored. Restarting app...")
                    withContext(NonCancellable) {
                        delay(1500)
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                } else {
                    _backupMessage.send("Backup imported successfully")
                }
            } else {
                _backupMessage.send("Import failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            _backupMessage.send("Clearing all data and restarting...")
            backupRepository.deleteAllData()
            withContext(NonCancellable) {
                delay(1500)
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
}
