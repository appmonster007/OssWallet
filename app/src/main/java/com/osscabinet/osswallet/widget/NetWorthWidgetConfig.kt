package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

/**
 * Mirrors every setting [NetWorthCardConfig][com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig]
 * exposes on the real dashboard card — comparison toggle, perspective, exclude
 * internal transfers, split transfers, and the filter (including "Advanced Filters", shown via
 * [WidgetConfigFiltersSection]) — so the widget can be configured identically to the card and
 * [NetWorthWidgetFetcher] can build a real `NetWorthCardConfig` straight from this.
 */
@Serializable
data class NetWorthWidgetConfig(
    val filter: TransactionFilter = TransactionFilter(
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
    ),
    val showComparison: Boolean = false,
    val perspective: AccountingPerspective = AccountingPerspective.Accounting,
    val excludeInternalTransfers: Boolean = true,
    val splitTransfers: Boolean = false,
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    /** Opacity (0..100 %) of the widget's background; 100 is fully opaque. */
    val backgroundOpacity: Int = 100
) {
    val timelineRange: TimelineRange get() = filter.timelineRange

    val timelineLabel: String get() = when (val tr = filter.timelineRange) {
        is TimelineRange.AllTime    -> "All Time"
        is TimelineRange.ThisPeriod -> tr.period.label
        is TimelineRange.LastPeriod -> "Last ${tr.period.label}"
        is TimelineRange.CustomRange -> "Custom Range"
    }
}
