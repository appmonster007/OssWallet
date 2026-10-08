package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

@Serializable
data class BudgetsWidgetConfig(
    val filter: TransactionFilter = TransactionFilter(
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH, excludeFuture = true)
    ),
    val showComparison: Boolean = true,
    val showArchived: Boolean = false,
    val maxItems: Int = 5
) {
    val timelineRange: TimelineRange get() = filter.timelineRange

    val periodLabel: String get() = when (val tr = filter.timelineRange) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> tr.period.label
        is TimelineRange.LastPeriod -> "Last ${tr.period.label}"
        is TimelineRange.CustomRange -> "Custom Range"
    }
}
