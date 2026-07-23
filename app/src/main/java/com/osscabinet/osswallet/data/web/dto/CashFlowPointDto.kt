package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class CashFlowPointDto(
    val label: String,
    val epochMillis: Long,
    val metrics: FinancialSummaryDto
)
