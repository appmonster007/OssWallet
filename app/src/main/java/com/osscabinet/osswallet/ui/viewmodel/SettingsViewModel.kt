package com.osscabinet.ossWallet.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.local.*
import com.osscabinet.ossWallet.domain.model.settings.AccountsSectionSettings
import com.osscabinet.ossWallet.domain.model.settings.CashFlowSettings
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesSettings
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.domain.model.ProfileType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Error(val message: String) : SyncState()
    object Success : SyncState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository,
    private val currencyRepository: CurrencyRepository,
    private val filterRepository: FilterRepository
) : ViewModel() {

    val allFilters: Flow<List<Filter>> = filterRepository.getAllFilters()

    fun saveFilter(filter: Filter) {
        viewModelScope.launch {
            filterRepository.insertFilter(filter)
        }
    }

    fun deleteFilter(filter: Filter) {
        viewModelScope.launch {
            filterRepository.deleteFilter(filter)
        }
    }

    val currency: StateFlow<String> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val theme: StateFlow<String> = settingsRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    val dashboardGridColumns: StateFlow<Int> = settingsRepository.dashboardSettings
        .map { it.accountsSection.gridColumns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val accountsViewGridColumns: StateFlow<Int> = settingsRepository.accountsViewSettings
        .map { it.accountsSection.gridColumns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val decimalPrecision: StateFlow<Int> = settingsRepository.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val dashboardShowAddAccountCard: StateFlow<Boolean> = settingsRepository.dashboardSettings
        .map { it.accountsSection.showAddAccountCard }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val accountsViewShowAddAccountCard: StateFlow<Boolean> = settingsRepository.accountsViewSettings
        .map { it.accountsSection.showAddAccountCard }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val accountsViewSettings: Flow<AccountsViewSettings> = settingsRepository.accountsViewSettings

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

    val isOnboardingCompleted: StateFlow<Boolean?> = settingsRepository.isOnboardingCompleted
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    fun setDashboardGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(gridColumns = columns)) }
        }
    }

    fun setAccountsViewGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(gridColumns = columns)) }
        }
    }

    fun setDecimalPrecision(precision: Int) {
        viewModelScope.launch {
            settingsRepository.saveDecimalPrecision(precision)
        }
    }

    fun setDashboardShowAddAccountCard(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDashboardSettings { it.copy(accountsSection = it.accountsSection.copy(showAddAccountCard = show)) }
        }
    }

    fun setAccountsViewShowAddAccountCard(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(accountsSection = it.accountsSection.copy(showAddAccountCard = show)) }
        }
    }

    fun updateAccountsViewCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateAccountsViewCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateAccountsViewCurrencyBalancesShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateAccountsViewCurrencyBalancesTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountsViewSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
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
            profileRepository.insertProfile(Profile(name = name, baseCurrency = currency, type = ProfileType.PERSONAL))
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportData(uri.toString())
                .onSuccess { _backupMessage.send("Backup exported successfully") }
                .onFailure { _backupMessage.send("Export failed: ${it.message}") }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            backupRepository.importData(uri.toString())
                .onSuccess { _backupMessage.send("Backup imported successfully") }
                .onFailure { _backupMessage.send("Import failed: ${it.message}") }
        }
    }
}

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository,
    private val currencyRepository: CurrencyRepository,
    private val filterRepository: FilterRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
            settingsRepository,
            profileRepository,
            backupRepository,
            currencyRepository,
            filterRepository
        ) as T
    }
}
