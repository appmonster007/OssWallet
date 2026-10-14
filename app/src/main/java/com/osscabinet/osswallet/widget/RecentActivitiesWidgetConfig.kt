package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

/**
 * Per-instance config for the Recent Activities widget. Mirrors the subset of
 * [com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig]'s display toggles
 * that make sense in a RemoteViews list row (no swipe actions, grouping, or currency breakdown —
 * those need real Compose gestures/layout the widget can't do).
 */
@Serializable
data class RecentActivitiesWidgetConfig(
    val filter: TransactionFilter = TransactionFilter(
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.YEAR, excludeFuture = true)
    ),
    val showAccount: Boolean = true,
    val showCategory: Boolean = true,
    val showTimestamp: Boolean = true,
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    /** Frosted-glass background instead of the solid surface. */
    val glass: Boolean = false
)
