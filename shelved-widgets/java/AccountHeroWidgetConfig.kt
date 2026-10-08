package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

/**
 * Account Hero is account-pinned per widget instance ([accountId]), unlike every other widget in
 * this phase which is timeline-scoped only — it mirrors the Compose card's `alwaysPresent`,
 * account-detail-screen-only scoping (`docs/card-data-summary.md` ACCOUNT_HERO section).
 */
@Serializable
data class AccountHeroWidgetConfig(
    val accountId: String? = null,
    val filter: TransactionFilter = TransactionFilter(
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH, excludeFuture = true)
    )
) {
    val periodLabel: String get() = when (val tr = filter.timelineRange) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> tr.period.label
        is TimelineRange.LastPeriod -> "Last ${tr.period.label}"
        is TimelineRange.CustomRange -> "Custom Range"
    }
}
