package com.osscabinet.gleemanager.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.osscabinet.gleemanager.data.local.SettingsDataStore
import com.osscabinet.gleemanager.data.local.entity.ProfileEntity
import com.osscabinet.gleemanager.domain.repository.BackupRepository
import com.osscabinet.gleemanager.domain.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _currency = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _theme = MutableStateFlow("System")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _currentProfile = MutableStateFlow<ProfileEntity?>(null)
    val currentProfile: StateFlow<ProfileEntity?> = _currentProfile.asStateFlow()

    private val _profiles = MutableStateFlow<List<ProfileEntity>>(emptyList())
    val profiles: StateFlow<List<ProfileEntity>> = _profiles.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.currency.collect { _currency.value = it }
        }
        viewModelScope.launch {
            settingsDataStore.theme.collect { _theme.value = it }
        }
        viewModelScope.launch {
            profileRepository.getAllProfiles().collect { _profiles.value = it }
        }
        viewModelScope.launch {
            settingsDataStore.currentProfileId.flatMapLatest { id ->
                flow { emit(profileRepository.getProfileById(id)) }
            }.collect { _currentProfile.value = it }
        }
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
