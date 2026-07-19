package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class BudgetDto(
    val id: Long,
    val name: String,
    val currency: String,
    val limitAmount: Double,
    val spent: Double,
    val remaining: Double,
    val percentage: Double,
    val alertLevel: String,      // "NONE", "WARNING", "EXCEEDED"
    val period: String,          // e.g. "2025-06"
    val isArchived: Boolean,
    val alertThreshold: Double = 0.8,
    val timelineType: String = "MONTHLY",
    val categoryIds: List<Long> = emptyList(),
    val accountIds: List<Long> = emptyList(),
    val rollover: Boolean = false
)
