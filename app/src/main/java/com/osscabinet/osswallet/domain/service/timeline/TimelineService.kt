package com.osscabinet.ossWallet.domain.service.timeline

import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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
    fun getRangeForThis(
        period: ThisPeriodType,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
    ): Pair<Long, Long> {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val start: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> now.toLocalDate().atStartOfDay(now.zone)
            ThisPeriodType.WEEK -> now.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).atStartOfDay(now.zone)

            ThisPeriodType.MONTH -> now.toLocalDate().with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay(now.zone)

            ThisPeriodType.QUARTER -> {
                val month = now.monthValue
                val firstMonthOfQuarter = ((month - 1) / 3) * 3 + 1
                now.toLocalDate().withMonth(firstMonthOfQuarter)
                    .with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(now.zone)
            }

            ThisPeriodType.HALF_YEAR -> {
                val firstMonthOfHalf = if (now.monthValue <= 6) 1 else 7
                now.toLocalDate().withMonth(firstMonthOfHalf)
                    .with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(now.zone)
            }

            ThisPeriodType.YEAR -> now.toLocalDate().with(TemporalAdjusters.firstDayOfYear())
                .atStartOfDay(now.zone)
        }
        val end: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> now.toLocalDate().atTime(LocalTime.MAX).atZone(now.zone)
            ThisPeriodType.WEEK -> now.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(firstDayOfWeek.plus(6))).atTime(LocalTime.MAX)
                .atZone(now.zone)

            ThisPeriodType.MONTH -> now.toLocalDate().with(TemporalAdjusters.lastDayOfMonth())
                .atTime(LocalTime.MAX).atZone(now.zone)

            ThisPeriodType.QUARTER -> {
                val month = now.monthValue
                val lastMonthOfQuarter = ((month - 1) / 3) * 3 + 3
                now.toLocalDate().withMonth(lastMonthOfQuarter)
                    .with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(now.zone)
            }

            ThisPeriodType.HALF_YEAR -> {
                val lastMonthOfHalf = if (now.monthValue <= 6) 6 else 12
                now.toLocalDate().withMonth(lastMonthOfHalf)
                    .with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(now.zone)
            }

            ThisPeriodType.YEAR -> now.toLocalDate().with(TemporalAdjusters.lastDayOfYear())
                .atTime(LocalTime.MAX).atZone(now.zone)
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
     * Extracts the date range from a timeline range.
     * 
     * @param timelineRange The timeline range to extract dates from
     * @return Pair of start and end timestamps (nullable for AllTime)
     */
    fun getDateRange(timelineRange: TimelineRange): Pair<Long?, Long?> {
        return when (timelineRange) {
            is TimelineRange.AllTime -> Pair(null, null)
            is TimelineRange.ThisPeriod -> {
                val (start, end) = getRangeForThis(timelineRange.period)
                Pair(start, end)
            }

            is TimelineRange.LastPeriod -> {
                val (start, end) = getRangeForLast(timelineRange.period)
                Pair(start, end)
            }

            is TimelineRange.CustomRange -> {
                Pair(timelineRange.start, timelineRange.end)
            }
        }
    }

    /**
     * Gets the previous date range for a given timeline range.
     * 
     * @param timelineRange The current timeline range
     * @return Pair of start and end timestamps in milliseconds
     */
    fun getPreviousRange(timelineRange: TimelineRange): Pair<Long?, Long?> {
        val (currentStart, currentEnd) = getDateRange(timelineRange)
        if (currentStart == null || currentEnd == null) return Pair(null, null)

        return when (timelineRange) {
            is TimelineRange.ThisPeriod -> {
                val start = Instant.ofEpochMilli(currentStart).atZone(ZoneId.systemDefault())
                val prevStart: ZonedDateTime = when (timelineRange.period) {
                    ThisPeriodType.TODAY -> start.minusDays(1)
                    ThisPeriodType.WEEK -> start.minusWeeks(1)
                    ThisPeriodType.MONTH -> start.minusMonths(1)
                    ThisPeriodType.QUARTER -> start.minusMonths(3)
                    ThisPeriodType.HALF_YEAR -> start.minusMonths(6)
                    ThisPeriodType.YEAR -> start.minusYears(1)
                }
                val prevEnd: ZonedDateTime = when (timelineRange.period) {
                    ThisPeriodType.TODAY -> prevStart.with(LocalTime.MAX)
                    ThisPeriodType.WEEK -> prevStart.plusDays(6).with(LocalTime.MAX)
                    ThisPeriodType.MONTH -> prevStart.with(TemporalAdjusters.lastDayOfMonth())
                        .with(LocalTime.MAX)

                    ThisPeriodType.QUARTER -> prevStart.plusMonths(2)
                        .with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)

                    ThisPeriodType.HALF_YEAR -> prevStart.plusMonths(5)
                        .with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX)

                    ThisPeriodType.YEAR -> prevStart.with(TemporalAdjusters.lastDayOfYear())
                        .with(LocalTime.MAX)
                }
                Pair(prevStart.toInstant().toEpochMilli(), prevEnd.toInstant().toEpochMilli())
            }

            is TimelineRange.LastPeriod -> {
                val duration = currentEnd - currentStart
                Pair(currentStart - duration - 1000, currentStart - 1000)
            }

            is TimelineRange.CustomRange -> {
                val duration = currentEnd - currentStart
                Pair(currentStart - duration - 1000, currentStart - 1000)
            }

            else -> Pair(null, null)
        }
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
        val (start, end) = getDateRange(timelineRange)

        return if (start == null || end == null) {
            items
        } else {
            items.filter { it.timestamp in start..end }
        }
    }
}
