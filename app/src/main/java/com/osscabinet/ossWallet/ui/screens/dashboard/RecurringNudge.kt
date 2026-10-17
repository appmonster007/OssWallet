package com.osscabinet.ossWallet.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.service.discovery.DiscoveredPattern
import com.osscabinet.ossWallet.domain.service.discovery.RecurringSuggestions
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RecurringNudgeViewModel @Inject constructor(
    private val suggestions: RecurringSuggestions
) : ViewModel() {
    /** Repeated payments the user hasn't looked at yet. Drives the dashboard's badge dot. */
    val unseen: StateFlow<List<DiscoveredPattern>> = suggestions.observeUnseen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
