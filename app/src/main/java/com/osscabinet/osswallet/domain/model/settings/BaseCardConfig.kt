package com.osscabinet.ossWallet.domain.model.settings

/**
 * Base interface for all card configurations across different screens.
 * This provides a common contract for card management operations.
 */
interface BaseCardConfig {
    val cardId: String
    val title: String
    val isEnabled: Boolean
    val order: Int
    val screenContext: ScreenContext
}

/**
 * Generic card configuration wrapper that can be used across all screens.
 * This provides a unified way to manage card lists.
 */
data class GenericCardsConfig<T : BaseCardConfig>(
    val cards: List<T> = emptyList()
)
