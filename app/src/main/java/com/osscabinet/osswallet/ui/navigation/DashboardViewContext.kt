package com.osscabinet.ossWallet.ui.navigation

import com.osscabinet.ossWallet.ui.dashboard.DashboardSection
import kotlinx.serialization.Serializable

/**
 * Sealed class representing different view contexts for Dashboard navigation.
 * Used to pass pre-applied view focus when navigating to the dashboard.
 */
@Serializable
sealed class DashboardViewContext {
    /**
     * No pre-applied view focus - default state
     */
    @Serializable
    data object None : DashboardViewContext()

    /**
     * Focus on a specific dashboard card by its ID
     */
    @Serializable
    data class FocusCard(val cardId: String) : DashboardViewContext()

    /**
     * Focus on a specific dashboard section
     */
    @Serializable
    data class FocusSection(val section: DashboardSection) : DashboardViewContext()
}
