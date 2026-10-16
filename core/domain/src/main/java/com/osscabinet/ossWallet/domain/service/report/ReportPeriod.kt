package com.osscabinet.ossWallet.domain.service.report

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A reporting period, resolved against "now" so presets stay correct whenever they are used. */
sealed class ReportPeriod {
    abstract val label: String
    abstract fun range(nowMs: Long, zone: ZoneId): LongRange

    data object ThisMonth : ReportPeriod() {
        override val label = "This month"
        override fun range(nowMs: Long, zone: ZoneId) = monthsBack(nowMs, zone, 0)
    }

    data object LastMonth : ReportPeriod() {
        override val label = "Last month"
        override fun range(nowMs: Long, zone: ZoneId): LongRange {
            val first = LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), zone).withDayOfMonth(1).minusMonths(1)
            return span(first, first.plusMonths(1).minusDays(1), zone)
        }
    }

    /** The current month and the two before it. */
    data object LastThreeMonths : ReportPeriod() {
        override val label = "Last 3 months"
        override fun range(nowMs: Long, zone: ZoneId) = monthsBack(nowMs, zone, 2)
    }

    data object ThisYear : ReportPeriod() {
        override val label = "This year"
        override fun range(nowMs: Long, zone: ZoneId): LongRange {
            val today = LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), zone)
            return span(today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()), zone)
        }
    }

    data object LastYear : ReportPeriod() {
        override val label = "Last year"
        override fun range(nowMs: Long, zone: ZoneId): LongRange {
            val jan1 = LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), zone).withDayOfYear(1).minusYears(1)
            return span(jan1, jan1.withDayOfYear(jan1.lengthOfYear()), zone)
        }
    }

    /** Whole days from [startDay] to [endDay], both inclusive. */
    data class Custom(val startDay: LocalDate, val endDay: LocalDate) : ReportPeriod() {
        init {
            require(!endDay.isBefore(startDay)) { "The end date can't be before the start date" }
        }

        override val label: String get() = "${startDay.format(FMT)} – ${endDay.format(FMT)}"
        override fun range(nowMs: Long, zone: ZoneId) = span(startDay, endDay, zone)
    }

    companion object {
        private val FMT = DateTimeFormatter.ofPattern("d MMM yyyy")

        /**
         * A getter on purpose: as a stored value it is built while the sealed class initialises, and
         * if a subclass object was touched first it would capture that object as null.
         */
        val presets: List<ReportPeriod> get() = listOf(ThisMonth, LastMonth, LastThreeMonths, ThisYear, LastYear)

        /** First instant of [first] through the last millisecond of [last]. */
        private fun span(first: LocalDate, last: LocalDate, zone: ZoneId): LongRange {
            val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = last.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return start..end
        }

        private fun monthsBack(nowMs: Long, zone: ZoneId, back: Long): LongRange {
            val today = LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), zone)
            val first = today.withDayOfMonth(1).minusMonths(back)
            return span(first, today.withDayOfMonth(today.lengthOfMonth()), zone)
        }
    }
}
