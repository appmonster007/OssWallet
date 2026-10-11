package com.osscabinet.ossWallet.widget

import kotlinx.serialization.Serializable

@Serializable
data class PlannedPaymentsWidgetConfig(
    val lookaheadDays: Int = 14,
    val maxItems: Int = 4
)
