package com.osscabinet.ossWallet.ui.screens.categories

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : BaseViewModel(settingsRepository) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<Category>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> categoryRepository.getAllCategories(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun upsertCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.upsertCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}
