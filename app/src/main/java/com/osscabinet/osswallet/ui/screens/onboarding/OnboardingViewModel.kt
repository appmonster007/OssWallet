package com.osscabinet.ossWallet.ui.screens.onboarding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.seed.SeedingConfig
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.currency.provider.CurrencyMetadata
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProvider
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProviderRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OnboardingEntryChoice { CREATE_NEW, IMPORT_BACKUP }

data class OnboardingState(
    val entryChoice: OnboardingEntryChoice? = null,
    val userName: String = "",
    val selectedCurrency: String = "USD",
    val selectedProviderId: String = "exchangerate-api",
    val selectedTheme: String = "System",
    val decimalPrecision: Int = 2,
    val availableProviders: List<ExchangeRateProvider> = emptyList(),
    val availableCurrencies: List<CurrencyMetadata> = emptyList(),
    val currenciesPerProvider: Map<String, List<CurrencyMetadata>> = emptyMap(),
    val shortlistedCurrencies: Set<String> = emptySet(),
    val isCompleting: Boolean = false,
    val showDebugSeedStep: Boolean = false,
    val isSeeding: Boolean = false,
    val isImporting: Boolean = false,
    val importError: String? = null
)

sealed class OnboardingEvent {
    data class EntryChoiceSelected(val choice: OnboardingEntryChoice) : OnboardingEvent()
    object EntryChoiceCleared : OnboardingEvent()
    data class UserNameChanged(val name: String) : OnboardingEvent()
    data class CurrencySelected(val currency: String) : OnboardingEvent()
    data class ProviderSelected(val providerId: String) : OnboardingEvent()
    data class ThemeSelected(val theme: String) : OnboardingEvent()
    data class PrecisionSelected(val precision: Int) : OnboardingEvent()
    data class ShortlistedCurrenciesChanged(val currencies: Set<String>) : OnboardingEvent()
    object CompleteOnboarding : OnboardingEvent()
    object SeedDataRequested : OnboardingEvent()
    data class ImportBackupRequested(
        val uri: Uri,
        val type: BackupType,
        val passphrase: CharArray?
    ) : OnboardingEvent()
    object ImportErrorDismissed : OnboardingEvent()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val providerRegistry: ExchangeRateProviderRegistry,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val backupRepository: BackupRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingState(
            availableProviders = providerRegistry.getAllProviders(),
            showDebugSeedStep = SeedingConfig.isEnabled
        )
    )
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    private val _onboardingCompleted = MutableSharedFlow<Unit>()
    val onboardingCompleted: SharedFlow<Unit> = _onboardingCompleted.asSharedFlow()

    init {
        viewModelScope.launch {
            val results = coroutineScope {
                providerRegistry.getAllProviders().map { provider ->
                    async { provider.id to provider.fetchSupportedCurrencies() }
                }.awaitAll().toMap()
            }
            _uiState.update { state ->
                val currencies = results[state.selectedProviderId]
                    ?: results.values.firstOrNull()
                    ?: emptyList()
                state.copy(currenciesPerProvider = results, availableCurrencies = currencies)
            }
        }
    }

    suspend fun isEncryptedBackup(uri: Uri): Boolean = backupRepository.isEncryptedBackup(uri.toString())

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.EntryChoiceSelected -> {
                _uiState.update { it.copy(entryChoice = event.choice, importError = null) }
            }

            OnboardingEvent.EntryChoiceCleared -> {
                _uiState.update { it.copy(entryChoice = null, importError = null) }
            }

            is OnboardingEvent.ImportBackupRequested -> {
                importBackup(event.uri, event.type, event.passphrase)
            }

            OnboardingEvent.ImportErrorDismissed -> {
                _uiState.update { it.copy(importError = null) }
            }

            is OnboardingEvent.UserNameChanged -> {
                _uiState.update { it.copy(userName = event.name) }
            }

            is OnboardingEvent.CurrencySelected -> {
                _uiState.update {
                    it.copy(
                        selectedCurrency = event.currency,
                        shortlistedCurrencies = it.shortlistedCurrencies + event.currency
                    )
                }
            }

            is OnboardingEvent.ProviderSelected -> {
                _uiState.update { state ->
                    val currencies = state.currenciesPerProvider[event.providerId]
                        ?: emptyList()
                    val newBase =
                        if (currencies.any { c -> c.code == state.selectedCurrency }) state.selectedCurrency else (currencies.firstOrNull()?.code
                            ?: "USD")
                    state.copy(
                        selectedProviderId = event.providerId,
                        availableCurrencies = currencies,
                        selectedCurrency = newBase,
                        shortlistedCurrencies = setOf(newBase)
                    )
                }
            }

            is OnboardingEvent.ShortlistedCurrenciesChanged -> {
                _uiState.update { it.copy(shortlistedCurrencies = event.currencies + it.selectedCurrency) }
            }

            is OnboardingEvent.ThemeSelected -> {
                _uiState.update { it.copy(selectedTheme = event.theme) }
                viewModelScope.launch {
                    settingsRepository.saveTheme(event.theme)
                }
            }

            is OnboardingEvent.PrecisionSelected -> {
                _uiState.update { it.copy(decimalPrecision = event.precision) }
            }

            OnboardingEvent.CompleteOnboarding -> {
                completeOnboarding()
            }

            OnboardingEvent.SeedDataRequested -> {
                seedData()
            }
        }
    }

    private fun importBackup(uri: Uri, type: BackupType, passphrase: CharArray?) {
        if (_uiState.value.isImporting) return
        _uiState.update { it.copy(isImporting = true, importError = null) }
        viewModelScope.launch {
            try {
                val check = backupRepository.verifyBackup(uri.toString(), type, passphrase)
                if (check.isFailure) {
                    passphrase?.fill('0')
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importError = check.exceptionOrNull()?.message
                        )
                    }
                    return@launch
                }
                val result = backupRepository.importData(uri.toString(), type, passphrase)
                passphrase?.fill('0')
                if (result.isSuccess) {
                    if (type == BackupType.SQLITE) {
                        // A full SQLite restore replaces the database file underneath the already-open
                        // connection, so the process is killed and relaunched to pick up the restored
                        // profiles/settings instead of continuing the onboarding wizard.
                        withContext(NonCancellable) {
                            delay(1500)
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                    } else {
                        // A CSV/ZIP restore writes straight into the live database without touching
                        // DataStore settings, so onboarding-completion and the active profile have to
                        // be set explicitly here from whatever the backup just restored.
                        val restoredProfileId = profileRepository.getAllProfiles().first()
                            .firstOrNull()?.id ?: 1
                        settingsRepository.saveCurrentProfileId(restoredProfileId)
                        settingsRepository.saveOnboardingCompleted(true)
                        _uiState.update { it.copy(isImporting = false) }
                        _onboardingCompleted.emit(Unit)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importError = result.exceptionOrNull()?.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, importError = e.message) }
            }
        }
    }

    private fun seedData() {
        if (_uiState.value.isSeeding) return
        _uiState.update { it.copy(isSeeding = true) }
        viewModelScope.launch {
            try {
                SeedingConfig.triggerManualSeeding(database, settingsDataStore, context)
                settingsRepository.saveOnboardingCompleted(true)
                _onboardingCompleted.emit(Unit)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSeeding = false) }
            }
        }
    }

    private fun completeOnboarding() {
        if (_uiState.value.isCompleting) return

        _uiState.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            try {
                val state = _uiState.value
                settingsRepository.saveUserName(state.userName)
                settingsRepository.saveCurrency(state.selectedCurrency)
                settingsRepository.saveCurrencyProvider(state.selectedProviderId)
                settingsRepository.saveDecimalPrecision(state.decimalPrecision)
                val shortlist = state.shortlistedCurrencies.ifEmpty {
                    state.availableCurrencies.map { it.code }.toSet()
                }
                settingsRepository.saveShortlistedCurrencies(shortlist)

                // Create initial profile
                profileRepository.insertProfile(
                    Profile(
                        id = 1,
                        name = if (state.userName.isBlank()) "Default Profile" else "${state.userName}'s Profile",
                        baseCurrency = state.selectedCurrency
                    )
                )
                settingsRepository.saveCurrentProfileId(1)
                settingsRepository.saveOnboardingCompleted(true)

                _onboardingCompleted.emit(Unit)
            } catch (e: Exception) {
                _uiState.update { it.copy(isCompleting = false) }
            }
        }
    }
}
