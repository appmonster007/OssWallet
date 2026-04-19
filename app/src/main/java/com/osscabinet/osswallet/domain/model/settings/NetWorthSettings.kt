package com.osscabinet.ossWallet.domain.model.settings

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

@Serializable
data class NetWorthSettings(
    val fontSize: String = "Medium", // "Small", "Medium", "Large", "X-Large"
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val profileId: Int? = null,
    val selectedAccountIds: List<String>? = null
)
