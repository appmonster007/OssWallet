package com.osscabinet.ossWallet.ui.components.transactions

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange

sealed interface FilterEvent {
    data class Apply(val filter: TransactionFilter) : FilterEvent
    data class UpdateTimeline(val range: TimelineRange) : FilterEvent
    data class UpdateSettings(val settings: TransactionSectionSettings) : FilterEvent
    data object Reset : FilterEvent
}
