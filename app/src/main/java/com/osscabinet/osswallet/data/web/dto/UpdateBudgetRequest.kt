package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateBudgetRequest(
    val name: String? = null,
    val currency: String? = null,
    val limitAmount: Double? = null,
    val timelineType: String? = null,
    val categoryIds: List<Long>? = null,
    val alertThreshold: Double? = null,
    val rollover: Boolean? = null,
    val isArchived: Boolean? = null
)
