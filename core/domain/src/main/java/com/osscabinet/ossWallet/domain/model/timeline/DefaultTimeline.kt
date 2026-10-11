package com.osscabinet.ossWallet.domain.model.timeline

/**
 * Timeline that screens (Records, Analytics, Account Detail) and account-hero cards start on.
 *
 * This is a *seed* value only. [TimelineRange.AllTime] remains the "unset" sentinel in filter
 * merge/resolution, budgets, comparisons and web DTOs, so it must not be replaced as a global default.
 * Balance and running-balance calculations use their own explicit [TimelineRange.AllTime].
 */
object DefaultTimeline {
    val INITIAL: TimelineRange = TimelineRange.ThisPeriod(
        period = ThisPeriodType.YEAR,
        excludeFuture = true
    )
}
