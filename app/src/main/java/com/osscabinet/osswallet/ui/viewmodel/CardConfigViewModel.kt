package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.domain.repository.CardRepository
import kotlinx.coroutines.launch

/**
 * ViewModel focused on managing per-card configuration updates.
 */
class CardConfigViewModel(
    private val cardRepository: CardRepository
) : ViewModel() {

    fun updateDashboardCard(cardId: String, update: (CardConfig) -> CardConfig) {
        viewModelScope.launch {
            cardRepository.updateDashboardCard(cardId, update)
        }
    }

    fun updateAccountDetailCard(cardId: String, update: (CardConfig) -> CardConfig) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId, update)
        }
    }

    fun updateAccountsViewCard(cardId: String, update: (CardConfig) -> CardConfig) {
        viewModelScope.launch {
            cardRepository.updateAccountsViewCard(cardId, update)
        }
    }
}
