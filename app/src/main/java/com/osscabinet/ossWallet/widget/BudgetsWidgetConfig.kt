package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.Serializable

/**
 * Per-instance config for the Budgets widget. The number of visible rows isn't configurable: it
 * follows the widget's height (see [BudgetsRemoteViewsRenderer]), up to [MAX_ROWS]. Each budget
 * is measured over its own period, so the filter only contributes its "exclude future" flag.
 */
@Serializable
data class BudgetsWidgetConfig(
    val filter: TransactionFilter = TransactionFilter(
        timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH, excludeFuture = true)
    ),
    val showArchived: Boolean = false,
    val theme: WidgetTheme = WidgetTheme.SYSTEM
) {
    companion object {
        const val MAX_ROWS = 4
    }
}
