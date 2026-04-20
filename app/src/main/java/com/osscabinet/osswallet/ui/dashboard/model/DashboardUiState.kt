package com.osscabinet.ossWallet.ui.dashboard.model

import com.osscabinet.ossWallet.ui.dashboard.model.CardViewState
import java.math.BigDecimal

/**
 * Lean DashboardUiState that provides card-specific ViewStates
 * No heavy domain objects passed to UI layer
 */
sealed class DashboardUiState {
    object Loading : DashboardUiState()
    
    data class Success(
        val baseCurrency: String,
        val decimalPrecision: Int,
        val cardViewStates: Map<String, CardViewState>, // cardId -> ViewState
        val sharedData: SharedDashboardData
    ) : DashboardUiState()
    
    data class Error(val message: String) : DashboardUiState()
}

/**
 * Shared data needed by multiple cards
 * Computed once and reused
 */
data class SharedDashboardData(
    val exchangeRates: Map<String, BigDecimal>,
    val currentProfileId: String
)
