package com.osscabinet.ossWallet.ui.dashboard.model

/**
 * Generic Loading/Success/Error wrapper for card UI states.
 * Replaces specialized card view states to standardize data flow.
 */
sealed interface CardUiState<out T> {
    val cardId: String
    
    data class Loading(
        override val cardId: String
    ) : CardUiState<Nothing>
    
    data class Success<T>(
        override val cardId: String,
        val data: T
    ) : CardUiState<T>
    
    data class Error(
        override val cardId: String,
        val message: String
    ) : CardUiState<Nothing>
}
