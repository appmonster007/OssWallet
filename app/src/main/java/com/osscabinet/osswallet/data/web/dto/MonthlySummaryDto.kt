package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class MonthlySummaryDto(
    val netWorth: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val month: String
)
