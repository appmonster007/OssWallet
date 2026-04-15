package com.osscabinet.ossWallet.ui.util

import com.osscabinet.ossWallet.ui.components.LastPeriodType
import com.osscabinet.ossWallet.ui.components.ThisPeriodType
import java.time.*
import java.time.temporal.TemporalAdjusters

object FilterUtils {

    fun getRangeForThis(period: ThisPeriodType, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): Pair<Long, Long> {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val start: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> now.toLocalDate().atStartOfDay(now.zone)
            ThisPeriodType.WEEK -> now.toLocalDate().with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).atStartOfDay(now.zone)
            ThisPeriodType.MONTH -> now.toLocalDate().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(now.zone)
            ThisPeriodType.YEAR -> now.toLocalDate().with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(now.zone)
        }
        val end: ZonedDateTime = when (period) {
            ThisPeriodType.TODAY -> now.toLocalDate().atTime(LocalTime.MAX).atZone(now.zone)
            ThisPeriodType.WEEK -> now.toLocalDate().with(TemporalAdjusters.nextOrSame(firstDayOfWeek.plus(6))).atTime(LocalTime.MAX).atZone(now.zone)
            ThisPeriodType.MONTH -> now.toLocalDate().with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX).atZone(now.zone)
            ThisPeriodType.YEAR -> now.toLocalDate().with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX).atZone(now.zone)
        }
        return Pair(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
    }

    fun getRangeForLast(period: LastPeriodType): Pair<Long, Long> {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val end = now.toLocalDate().atTime(LocalTime.MAX).atZone(now.zone)
        val start: ZonedDateTime = when (period) {
            LastPeriodType.LAST_24H -> now.minusHours(24)
            LastPeriodType.LAST_7_DAYS -> now.minusDays(7).toLocalDate().atStartOfDay(now.zone)
            LastPeriodType.LAST_30_DAYS -> now.minusDays(30).toLocalDate().atStartOfDay(now.zone)
            LastPeriodType.LAST_365_DAYS -> now.minusDays(365).toLocalDate().atStartOfDay(now.zone)
        }
        return Pair(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
    }
}
