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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val currency: StateFlow<String> = settingsDataStore.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val theme: StateFlow<String> = settingsDataStore.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    val dashboardGridColumns: StateFlow<Int> = settingsDataStore.dashboardGridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val decimalPrecision: StateFlow<Int> = settingsDataStore.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

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

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            settingsDataStore.saveCurrency(currency)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsDataStore.saveTheme(theme)
        }
    }

    fun setDashboardGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsDataStore.saveDashboardGridColumns(columns)
        }
    }

    fun setDecimalPrecision(precision: Int) {
        viewModelScope.launch {
            settingsDataStore.saveDecimalPrecision(precision)
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
    private val backupRepository: BackupRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(settingsDataStore, profileRepository, backupRepository) as T
    }
}
