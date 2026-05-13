package com.osscabinet.ossWallet.ui.screens.filters.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FilterEntryViewModel(
    private val filterRepository: FilterRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(FilterEntryState())
    val state: StateFlow<FilterEntryState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { profileId ->
                combine(
                    accountRepository.getAllAccounts(profileId),
                    categoryRepository.categories
                ) { accounts, categories ->
                    _state.update { 
                        it.copy(
                            accounts = accounts,
                            categories = categories
                        )
                    }
                }
            }.collect()
        }
    }

    fun initialize(filterId: Long? = null) {
        if (filterId == null || filterId == -1L) {
            _state.update { it.copy(isEditMode = false, filterId = null) }
            return
        }

        viewModelScope.launch {
            filterRepository.getAllFilters().first().find { it.id == filterId }?.let { filter ->
                _state.update {
                    it.copy(
                        filterId = filterId,
                        isEditMode = true,
                        existingFilter = filter,
                        name = filter.name,
                        transactionFilter = filter.filter
                    )
                }
            }
        }
    }

    fun onEvent(event: FilterEntryEvent) {
        when (event) {
            is FilterEntryEvent.SetName -> _state.update { it.copy(name = event.name) }
            is FilterEntryEvent.SetTransactionFilter -> _state.update { it.copy(transactionFilter = event.filter) }
            is FilterEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    fun saveFilter(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val filter = Filter(
                id = s.filterId ?: 0L,
                name = s.name,
                filter = s.transactionFilter
            )
            filterRepository.insertFilter(filter)
            onSuccess()
        }
    }

    fun deleteFilter(onSuccess: () -> Unit) {
        val filter = _state.value.existingFilter ?: return
        viewModelScope.launch {
            filterRepository.deleteFilter(filter)
            onSuccess()
        }
    }
}
