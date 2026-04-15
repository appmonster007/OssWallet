package com.osscabinet.ossWallet.ui.util

import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import java.time.*
import java.time.temporal.TemporalAdjusters

object FilterUtils {

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
}
