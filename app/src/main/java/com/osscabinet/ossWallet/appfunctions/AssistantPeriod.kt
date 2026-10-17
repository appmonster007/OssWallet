package com.osscabinet.ossWallet.appfunctions

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The standard periods an assistant can ask about. Ranges are whole local days; the end is the last millisecond. */
enum class AssistantPeriod(val label: String) {
    THIS_MONTH("this month"),
    LAST_MONTH("last month"),
    LAST_30_DAYS("last 30 days"),
    LAST_3_MONTHS("last 3 months"),
    THIS_YEAR("this year"),
    LAST_YEAR("last year"),
    ALL_TIME("all time");

    /** The covered range, or null for all time. */
    fun range(now: Long, zone: ZoneId): LongRange? {
        val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone)
        return when (this) {
            THIS_MONTH -> days(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()), zone)
            LAST_MONTH -> {
                val first = today.withDayOfMonth(1).minusMonths(1)
                days(first, first.withDayOfMonth(first.lengthOfMonth()), zone)
            }
            LAST_30_DAYS -> days(today.minusDays(29), today, zone)
            LAST_3_MONTHS -> days(today.withDayOfMonth(1).minusMonths(2), today, zone)
            THIS_YEAR -> days(today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()), zone)
            LAST_YEAR -> {
                val first = today.withDayOfYear(1).minusYears(1)
                days(first, first.withDayOfYear(first.lengthOfYear()), zone)
            }
            ALL_TIME -> null
        }
    }

    /** The period to compare with: the one before, of a comparable length. Null when there isn't one. */
    fun previousRange(now: Long, zone: ZoneId): LongRange? {
        val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone)
        return when (this) {
            THIS_MONTH -> LAST_MONTH.range(now, zone)
            LAST_MONTH -> {
                val first = today.withDayOfMonth(1).minusMonths(2)
                days(first, first.withDayOfMonth(first.lengthOfMonth()), zone)
            }
            THIS_YEAR -> LAST_YEAR.range(now, zone)
            LAST_YEAR -> {
                val first = today.withDayOfYear(1).minusYears(2)
                days(first, first.withDayOfYear(first.lengthOfYear()), zone)
            }
            LAST_30_DAYS, LAST_3_MONTHS -> shiftBack(range(now, zone)!!, zone)
            ALL_TIME -> null
        }
    }

    companion object {
        /** Accepts "this month", "THIS_MONTH", "this-month" and the like; null for anything else. */
        fun parse(text: String?): AssistantPeriod? {
            val key = text?.trim()?.lowercase()?.replace(Regex("[\\s_-]+"), " ") ?: return null
            return entries.firstOrNull { it.label == key }
        }

        val choices: String get() = entries.joinToString(", ") { it.label }

        private fun days(first: LocalDate, last: LocalDate, zone: ZoneId): LongRange =
            first.atStartOfDay(zone).toInstant().toEpochMilli()..(last.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1)

        /** The stretch just before [range], as many whole days long. */
        private fun shiftBack(range: LongRange, zone: ZoneId): LongRange {
            val first = LocalDate.ofInstant(Instant.ofEpochMilli(range.first), zone)
            val last = LocalDate.ofInstant(Instant.ofEpochMilli(range.last), zone)
            val length = java.time.temporal.ChronoUnit.DAYS.between(first, last) + 1
            return days(first.minusDays(length), first.minusDays(1), zone)
        }
    }
}
