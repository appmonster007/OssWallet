package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

@Serializable
data class CashFlowWidgetConfig(
    val timelineRange: TimelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
    val showComparison: Boolean = false,
    val showBaseConversion: Boolean = true,
    val showTotals: Boolean = true
) {
    val timelineLabel: String get() = when (val tr = timelineRange) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> tr.period.label
        is TimelineRange.LastPeriod -> "Last ${tr.period.label}"
        is TimelineRange.CustomRange -> "Custom Range"
    }
}
