package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

@Serializable
data class CashFlowWidgetConfig(
    val timelineRange: TimelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
    val fontSize: String = "Medium",
    val showComparison: Boolean = true
) {
    val timelineLabel: String get() = when (val tr = timelineRange) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> tr.period.label
        is TimelineRange.LastPeriod -> "Last ${tr.period.label}"
        is TimelineRange.CustomRange -> "Custom Range"
    }
}
