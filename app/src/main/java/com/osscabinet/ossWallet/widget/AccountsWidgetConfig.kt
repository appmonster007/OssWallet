package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

/**
 * Per-instance config for the Accounts widget. The filter's period drives the "net" line under each
 * account. Accounts are listed in the order set on the Accounts screen, and the list scrolls.
 */
@Serializable
data class AccountsWidgetConfig(
    val filter: TransactionFilter = TransactionFilter(
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH, excludeFuture = true)
    ),
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    /** Frosted-glass background instead of the solid surface. */
    val glass: Boolean = false
) {
    val periodLabel: String get() = when (val tr = filter.timelineRange) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> tr.period.label
        is TimelineRange.LastPeriod -> "Last ${tr.period.label}"
        is TimelineRange.CustomRange -> "Custom Range"
        is TimelineRange.Periodic -> tr.recurrenceRule.formatHuman()
    }
}
