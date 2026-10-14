package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

/**
 * Per-instance config for the Budgets widget. Each budget is measured over its own period, so the
 * filter only contributes its "exclude future" flag. The list scrolls, so every budget is shown.
 */
@Serializable
data class BudgetsWidgetConfig(
    val filter: TransactionFilter = TransactionFilter(
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH, excludeFuture = true)
    ),
    val showArchived: Boolean = false,
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    /** Frosted-glass background instead of the solid surface. */
    val glass: Boolean = false
)
