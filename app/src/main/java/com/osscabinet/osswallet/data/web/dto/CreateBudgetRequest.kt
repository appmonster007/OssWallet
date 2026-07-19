package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateBudgetRequest(
    val name: String,
    val currency: String,
    val limitAmount: Double,
    val timelineType: String,  // "MONTHLY", "YEARLY", etc.
    val categoryIds: List<Long> = emptyList(),
    val alertThreshold: Double = 0.8,
    val rollover: Boolean = false,
    val accountIds: List<String> = emptyList(),
    val trackTypes: List<String> = emptyList(),
    val filterId: Long? = null
)
