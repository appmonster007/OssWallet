package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionDisplayPreference {
    CATEGORY_FIRST,
    NOTE_FIRST
}

@Serializable
data class RecentTransactionsWidgetConfig(
    val filter: TransactionFilter = TransactionFilter(
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
    ),
    val limit: Int = 10,
    val showCategoryIcon: Boolean = true,
    val primaryDisplay: TransactionDisplayPreference = TransactionDisplayPreference.CATEGORY_FIRST
) {
    val timelineRange: TimelineRange get() = filter.timelineRange

    val timelineLabel: String get() = when (val tr = filter.timelineRange) {
        is TimelineRange.AllTime    -> "All Time"
        is TimelineRange.ThisPeriod -> tr.period.label
        is TimelineRange.LastPeriod -> "Last ${tr.period.label}"
        is TimelineRange.CustomRange -> "Custom Range"
    }
}
