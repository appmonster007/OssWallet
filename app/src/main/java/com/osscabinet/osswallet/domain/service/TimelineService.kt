package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import java.time.*
import java.time.temporal.TemporalAdjusters

/**
 * Domain service for timeline and filtering business logic.
 * Moved from ui.util.FilterUtils to follow Clean Architecture principles.
 */
object TimelineService {

    /**
     * Gets the date range for a "this period" type.
     * 
     * @param period The period type (today, week, month, etc.)
     * @param firstDayOfWeek The first day of the week (default: Monday)
     * @return Pair of start and end timestamps in milliseconds
     */
    fun getRangeForThis(period: ThisPeriodType, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): Pair<Long, Long> {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val start: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> now.toLocalDate().atStartOfDay(now.zone)
            ThisPeriodType.WEEK -> now.toLocalDate().with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).atStartOfDay(now.zone)
            ThisPeriodType.MONTH -> now.toLocalDate().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(now.zone)
            ThisPeriodType.QUARTER -> {
                val month = now.monthValue
                val firstMonthOfQuarter = ((month - 1) / 3) * 3 + 1
                now.toLocalDate().withMonth(firstMonthOfQuarter).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(now.zone)
            }
            ThisPeriodType.HALF_YEAR -> {
                val firstMonthOfHalf = if (now.monthValue <= 6) 1 else 7
                now.toLocalDate().withMonth(firstMonthOfHalf).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(now.zone)
            }
            ThisPeriodType.YEAR -> now.toLocalDate().with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(now.zone)
        }
        val end: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> now.toLocalDate().atTime(LocalTime.MAX).atZone(now.zone)
            ThisPeriodType.WEEK -> now.toLocalDate().with(TemporalAdjusters.nextOrSame(firstDayOfWeek.plus(6))).atTime(LocalTime.MAX).atZone(now.zone)
            ThisPeriodType.MONTH -> now.toLocalDate().with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(now.zone)
            ThisPeriodType.QUARTER -> {
                val month = now.monthValue
                val lastMonthOfQuarter = ((month - 1) / 3) * 3 + 3
                now.toLocalDate().withMonth(lastMonthOfQuarter).with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(now.zone)
            }
            ThisPeriodType.HALF_YEAR -> {
                val lastMonthOfHalf = if (now.monthValue <= 6) 6 else 12
                now.toLocalDate().withMonth(lastMonthOfHalf).with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(now.zone)
            }
            ThisPeriodType.YEAR -> now.toLocalDate().with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX).atZone(now.zone)
        }
        return Pair(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
    }

    /**
     * Gets the date range for a "last period" type.
     * 
     * @param period The period type (last 24h, last 7 days, etc.)
     * @return Pair of start and end timestamps in milliseconds
     */
    fun getRangeForLast(period: LastPeriodType): Pair<Long, Long> {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val end = now.toLocalDate().atTime(LocalTime.MAX).atZone(now.zone)
        val start: ZonedDateTime = when (period) {
            LastPeriodType.LAST_24H -> now.minusHours(24)
            LastPeriodType.LAST_7_DAYS -> now.minusDays(7).toLocalDate().atStartOfDay(now.zone)
            LastPeriodType.LAST_30_DAYS -> now.minusDays(30).toLocalDate().atStartOfDay(now.zone)
            LastPeriodType.LAST_12_WEEKS -> now.minusWeeks(12).toLocalDate().atStartOfDay(now.zone)
            LastPeriodType.LAST_6_MONTHS -> now.minusMonths(6).toLocalDate().atStartOfDay(now.zone)
            LastPeriodType.LAST_1_YEAR -> now.minusYears(1).toLocalDate().atStartOfDay(now.zone)
            LastPeriodType.LAST_5_YEARS -> now.minusYears(5).toLocalDate().atStartOfDay(now.zone)
        }
        return Pair(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
    }

    /**
     * Filters a list of timestamped items by a timeline range.
     * 
     * @param items The list of items to filter
     * @param timelineRange The timeline range to filter by
     * @return The filtered list of items
     */
    fun <T : com.osscabinet.ossWallet.domain.model.Timestamped> filterByTimelineRange(
        items: List<T>,
        timelineRange: TimelineRange
    ): List<T> {
        return when (timelineRange) {
            is TimelineRange.AllTime -> items
            is TimelineRange.ThisPeriod -> {
                val (start, end) = getRangeForThis(timelineRange.period)
                items.filter { it.timestamp in start..end }
            }
            is TimelineRange.LastPeriod -> {
                val (start, end) = getRangeForLast(timelineRange.period)
                items.filter { it.timestamp in start..end }
            }
            is TimelineRange.CustomRange -> {
                items.filter { it.timestamp in timelineRange.start..timelineRange.end }
            }
        }
    }
}
