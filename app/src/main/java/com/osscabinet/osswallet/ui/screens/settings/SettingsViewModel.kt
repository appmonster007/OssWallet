package com.osscabinet.ossWallet.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.domain.model.ProfileType
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.currency.provider.CurrencyMetadata
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProvider
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProviderRegistry
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Error(val message: String) : SyncState()
    object Success : SyncState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository,
    private val currencyRepository: CurrencyRepository,
    private val providerRegistry: ExchangeRateProviderRegistry
) : BaseViewModel(settingsRepository) {

    val currency: StateFlow<String> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

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

    val expandableFabEnabled: StateFlow<Boolean> = settingsRepository.expandableFabEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val use24HourFormat: StateFlow<Boolean> = settingsRepository.use24HourFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val mapsAccessEnabled: StateFlow<Boolean> = settingsRepository.mapsAccessEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currencyProvider: StateFlow<String> = settingsRepository.currencyProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "exchangerate-api")

    val useSystemFont: StateFlow<Boolean> = settingsRepository.useSystemFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedFont: StateFlow<String> = settingsRepository.selectedFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MANROPE")

    val glassmorphismEnabled: StateFlow<Boolean> = settingsRepository.glassmorphismEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val contourBgVersion: StateFlow<Int> = settingsRepository.contourBgVersion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val contourLinesEnabled: StateFlow<Boolean> = settingsRepository.contourLinesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val randomizeContourEachLoad: StateFlow<Boolean> = settingsRepository.randomizeContourEachLoad
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val availableCurrencies: StateFlow<List<CurrencyMetadata>> = currencyProvider
        .flatMapLatest { providerId ->
            flow {
                emit(providerRegistry.getProvider(providerId).fetchSupportedCurrencies())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableProviders: List<ExchangeRateProvider> = providerRegistry.getAllProviders()

    val isOnboardingCompleted: StateFlow<Boolean?> = settingsRepository.isOnboardingCompleted
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profiles: StateFlow<List<Profile>> = profileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentProfile: StateFlow<Profile?> = settingsRepository.currentProfileId
        .map { id -> profileRepository.getProfileById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _onboardingSyncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val onboardingSyncState: StateFlow<SyncState> = _onboardingSyncState.asStateFlow()

    private val _backupMessage = Channel<String>(Channel.BUFFERED)
    val backupMessage: Flow<String> = _backupMessage.receiveAsFlow()

    fun setCurrency(currencyCode: String) {
        viewModelScope.launch {
            settingsRepository.saveCurrency(currencyCode)
            fetchAndSaveRates(currencyCode)
        }
    }

    fun setShortlistedCurrencies(currencies: Set<String>) {
        viewModelScope.launch {
            settingsRepository.saveShortlistedCurrencies(currencies)
        }
    }

    fun fetchAndSaveRates(baseCurrency: String) {
        viewModelScope.launch {
            try {
                val rates = currencyRepository.fetchFreshRates(baseCurrency)
                currencyRepository.saveRates(baseCurrency, rates)
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.saveTheme(theme)
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

    fun setExpandableFabEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveExpandableFabEnabled(enabled)
        }
    }

    fun setUse24HourFormat(use24Hour: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveUse24HourFormat(use24Hour)
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

    fun setCurrencyProvider(providerId: String) {
        viewModelScope.launch {
            settingsRepository.saveCurrencyProvider(providerId)
            // Trigger a refresh with the new provider
            fetchAndSaveRates(currency.value)
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

    fun createProfile(name: String, currency: String) {
        viewModelScope.launch {
            profileRepository.insertProfile(
                Profile(
                    name = name,
                    baseCurrency = currency,
                    type = ProfileType.PERSONAL
                )
            )
        }
    }

    fun exportBackup(uri: Uri, type: BackupType) {
        viewModelScope.launch {
            backupRepository.exportData(uri.toString(), type)
                .onSuccess { _backupMessage.send("Backup exported successfully") }
                .onFailure { _backupMessage.send("Export failed: ${it.message}") }
        }
    }

    fun importBackup(uri: Uri, type: BackupType) {
        viewModelScope.launch {
            backupRepository.importData(uri.toString(), type)
                .onSuccess { _backupMessage.send("Backup imported successfully") }
                .onFailure { _backupMessage.send("Import failed: ${it.message}") }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            backupRepository.deleteAllData()
                .onSuccess { _backupMessage.send("All data cleared") }
                .onFailure { _backupMessage.send("Failed to clear data: ${it.message}") }
        }
    }
}
