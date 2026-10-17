package com.osscabinet.ossWallet.ui.screens.filters

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FilterViewModel @Inject constructor(
    private val filterRepository: FilterRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    val allFilters: StateFlow<List<Filter>> = filterRepository.getAllFilters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** A copy to tweak without touching the original. */
    fun duplicateFilter(filter: Filter) {
        viewModelScope.launch {
            filterRepository.insertFilter(filter.copy(id = 0, name = "${filter.name} copy"))
        }
    }

    fun deleteFilter(filter: Filter) {
        viewModelScope.launch {
            filterRepository.deleteFilter(filter)
        }
    }
}
