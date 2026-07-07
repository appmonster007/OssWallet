package com.osscabinet.ossWallet.ui.screens.tags

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TagViewModel @Inject constructor(
    private val repository: TagRepository,
    settingsRepository: SettingsRepository,
) : BaseViewModel(settingsRepository) {
    val tags: StateFlow<List<Tag>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTag(name: String, colorHex: Int = -1) {
        viewModelScope.launch {
            repository.insertTag(Tag(name = name, colorHex = colorHex))
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            repository.deleteTagAndRemoveFromTransactions(tag)
        }
    }

    suspend fun getTransactionCountForTag(tagName: String): Int {
        val profileId = settingsRepository.currentProfileId.first()
        return repository.getTransactionCountForTag(tagName, profileId)
    }

    fun updateTag(oldName: String, newTag: Tag) {
        viewModelScope.launch {
            repository.updateTag(oldName, newTag)
        }
    }
}
