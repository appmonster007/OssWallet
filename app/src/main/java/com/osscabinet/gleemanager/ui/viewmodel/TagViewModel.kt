package com.osscabinet.gleemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.gleemanager.data.local.entity.TagEntity
import com.osscabinet.gleemanager.domain.repository.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(private val repository: TagRepository) : ViewModel() {
    val tags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTag(name: String, colorHex: Int = -1) {
        viewModelScope.launch {
            repository.insertTag(TagEntity(name = name, colorHex = colorHex))
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.deleteTagAndRemoveFromTransactions(tag)
        }
    }
}
