package com.osscabinet.ossWallet.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.entity.ProfileEntity
import com.osscabinet.ossWallet.domain.repository.BackupRepository
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SyncState {
    data object Idle : SyncState
    data object Loading : SyncState
    data class Error(val message: String) : SyncState
    data object Success : SyncState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository,
    private val currencyRepository: com.osscabinet.ossWallet.domain.repository.CurrencyRepository
) : ViewModel() {

    val currency: StateFlow<String> = settingsDataStore.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val theme: StateFlow<String> = settingsDataStore.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    val dashboardGridColumns: StateFlow<Int> = settingsDataStore.dashboardSettings
        .map { it.accountsSection.gridColumns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val accountsViewGridColumns: StateFlow<Int> = settingsDataStore.accountsViewSettings
        .map { it.accountsSection.gridColumns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val decimalPrecision: StateFlow<Int> = settingsDataStore.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val dashboardShowAddAccountCard: StateFlow<Boolean> = settingsDataStore.dashboardSettings
        .map { it.accountsSection.showAddAccountCard }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val accountsViewShowAddAccountCard: StateFlow<Boolean> = settingsDataStore.accountsViewSettings
        .map { it.accountsSection.showAddAccountCard }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val accountsViewSettings: Flow<com.osscabinet.ossWallet.data.local.AccountsViewSettings> = settingsDataStore.accountsViewSettings

    val autoSaveExchangeRates: StateFlow<Boolean> = settingsDataStore.autoSaveExchangeRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isOnboardingCompleted: StateFlow<Boolean?> = settingsDataStore.isOnboardingCompleted
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<ProfileEntity>> = profileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentProfile: StateFlow<ProfileEntity?> = settingsDataStore.currentProfileId
        .flatMapLatest { id ->
            flow { emit(profileRepository.getProfileById(id)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Initialization logic if needed
    }

    private val _onboardingSyncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val onboardingSyncState: StateFlow<SyncState> = _onboardingSyncState.asStateFlow()

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            settingsDataStore.saveCurrency(currency)
            fetchAndSaveRates(currency)
        }
    }

    private fun fetchAndSaveRates(baseCurrency: String) {
        viewModelScope.launch {
            _onboardingSyncState.value = SyncState.Loading
            try {
                val response = currencyRepository.fetchFreshRates(baseCurrency)
                currencyRepository.saveRates(baseCurrency, response)
                _onboardingSyncState.value = SyncState.Success
            } catch (e: Exception) {
                _onboardingSyncState.value = SyncState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsDataStore.saveTheme(theme)
        }
    }

    fun setDashboardGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(gridColumns = columns)) }
        }
    }

    fun setAccountsViewGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(gridColumns = columns)) }
        }
    }

    fun setDecimalPrecision(precision: Int) {
        viewModelScope.launch {
            settingsDataStore.saveDecimalPrecision(precision)
        }
    }

    fun setDashboardShowAddAccountCard(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(showAddAccountCard = show)) }
        }
    }

    fun setAccountsViewShowAddAccountCard(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(showAddAccountCard = show)) }
        }
    }

    fun updateAccountsViewCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateAccountsViewCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateAccountsViewCurrencyBalancesShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateAccountsViewCurrencyBalancesTimelineRange(range: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange) {
        viewModelScope.launch {
            settingsDataStore.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
        }
    }

    fun setAutoSaveExchangeRates(autoSave: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveAutoSaveExchangeRates(autoSave)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveOnboardingCompleted(completed)
        }
    }

    fun switchProfile(profileId: Int) {
        viewModelScope.launch {
            settingsDataStore.saveCurrentProfileId(profileId)
        }
    }

    fun createProfile(name: String, type: String) {
        viewModelScope.launch {
            profileRepository.insertProfile(ProfileEntity(name = name, type = type))
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportData(uri)
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            backupRepository.importData(uri)
        }
    }
}

class SettingsViewModelFactory(
    private val settingsDataStore: SettingsDataStore,
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository,
    private val currencyRepository: com.osscabinet.ossWallet.domain.repository.CurrencyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(settingsDataStore, profileRepository, backupRepository, currencyRepository) as T
    }
}
