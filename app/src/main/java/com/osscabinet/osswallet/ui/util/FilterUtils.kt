package com.osscabinet.ossWallet.ui.util

import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.service.TimelineService
import java.time.DayOfWeek

/**
 * UI utility object for filtering operations.
 * Business logic has been moved to TimelineService in the domain layer.
 * This object delegates to TimelineService for backward compatibility.
 */
@Deprecated("Use TimelineService directly instead")
object FilterUtils {

    @Deprecated("Use TimelineService.getRangeForThis instead")
    fun getRangeForThis(period: ThisPeriodType, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): Pair<Long, Long> {
        return TimelineService.getRangeForThis(period, firstDayOfWeek)
    }

    @Deprecated("Use TimelineService.getRangeForLast instead")
    fun getRangeForLast(period: LastPeriodType): Pair<Long, Long> {
        return TimelineService.getRangeForLast(period)
    }

    @Deprecated("Use TimelineService.filterByTimelineRange instead")
    fun <T : com.osscabinet.ossWallet.domain.model.Timestamped> filterByTimelineRange(
        items: List<T>,
        timelineRange: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
    ): List<T> {
        return TimelineService.filterByTimelineRange(items, timelineRange)
    }
}
