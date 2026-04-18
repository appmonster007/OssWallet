package com.osscabinet.ossWallet.ui.viewmodel

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import java.math.BigDecimal

sealed class NetWorthUiState {
    data object Loading : NetWorthUiState()
    data class Success(
        val netWorth: BigDecimal,
        val baseCurrency: String,
        val precision: Int,
        val fontSize: String,
        val timelineRange: TimelineRange
    ) : NetWorthUiState()
    data class Error(val message: String) : NetWorthUiState()
}
