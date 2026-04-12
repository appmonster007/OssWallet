package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.SharedWallet
import com.osscabinet.ossWallet.domain.repository.SharedWalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SharedWalletViewModel(
    private val repository: SharedWalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedWalletUiState())
    val uiState: StateFlow<SharedWalletUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSharedWallets().collect { wallets ->
                _uiState.update { it.copy(wallets = wallets, isLoading = false) }
            }
        }
    }

    fun onCreateWallet(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.createWallet(name)
        }
    }

    fun onJoinWallet(inviteCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.joinWallet(inviteCode)
        }
    }
}

data class SharedWalletUiState(
    val wallets: List<SharedWallet> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
