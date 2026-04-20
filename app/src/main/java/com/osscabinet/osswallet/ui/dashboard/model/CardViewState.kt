package com.osscabinet.ossWallet.ui.dashboard.model

/**
 * Base contract for all dashboard cards
 * Follows MVI pattern with unidirectional data flow
 */
sealed interface CardViewState {
    val cardId: String
    val isLoading: Boolean
    
    data class Loading(
        override val cardId: String
    ) : CardViewState {
        override val isLoading: Boolean = true
    }
    
    data class Error(
        override val cardId: String,
        val message: String
    ) : CardViewState {
        override val isLoading: Boolean = false
    }
}

/**
 * Base contract for card intents (events flowing upward)
 */
sealed interface CardIntent {
    val cardId: String
}

/**
 * Base contract for card events (one-time effects like navigation, snackbar)
 */
sealed interface CardEvent {
    val cardId: String
}
