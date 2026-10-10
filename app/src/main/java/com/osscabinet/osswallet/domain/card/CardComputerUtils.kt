package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.settings.BarChartGranularity
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object CardComputerUtils {

    private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMM")

    private val yearFormatter = DateTimeFormatter.ofPattern("yyyy")

    /** Resolves AUTO to a concrete bucket size using the same span rules as [resolveGranularity]. */
    fun resolveBarGranularity(
        requested: BarChartGranularity,
        rangeStart: Long,
        rangeEnd: Long
    ): BarChartGranularity = if (requested != BarChartGranularity.AUTO) requested else
        when (resolveGranularity(rangeStart, rangeEnd)) {
            CashFlowComparisonGranularity.DAY -> BarChartGranularity.DAY
            CashFlowComparisonGranularity.WEEK -> BarChartGranularity.WEEK
            CashFlowComparisonGranularity.MONTH -> BarChartGranularity.MONTH
        }

    /** The legacy 3-value granularity kept on render data; QUARTER / YEAR report as MONTH. */
    fun BarChartGranularity.toLegacyGranularity(): CashFlowComparisonGranularity = when (this) {
        BarChartGranularity.DAY -> CashFlowComparisonGranularity.DAY
        BarChartGranularity.WEEK -> CashFlowComparisonGranularity.WEEK
        else -> CashFlowComparisonGranularity.MONTH
    }

    /**
     * Calendar-aligned buckets covering [rangeStart]..[rangeEnd]: weeks start on Monday, months,
     * quarters and years on their first day. Every bucket in the span is returned, with or
     * without data, so a chart can show an empty slot for it. [granularity] must not be AUTO.
     */
    fun generateAlignedBuckets(
        rangeStart: Long,
        rangeEnd: Long,
        granularity: BarChartGranularity
    ): List<Triple<Long, Long, String>> {
        val zone = TimelineService.systemZoneId
        val first = Instant.ofEpochMilli(rangeStart).atZone(zone).toLocalDate()
        val last = Instant.ofEpochMilli(rangeEnd).atZone(zone).toLocalDate()
        if (last.isBefore(first)) return emptyList()

        fun LocalDate.alignedStart(): LocalDate = when (granularity) {
            BarChartGranularity.WEEK -> with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            BarChartGranularity.MONTH -> withDayOfMonth(1)
            BarChartGranularity.QUARTER -> withDayOfMonth(1).withMonth((monthValue - 1) / 3 * 3 + 1)
            BarChartGranularity.YEAR -> withDayOfYear(1)
            else -> this
        }

        fun LocalDate.next(): LocalDate = when (granularity) {
            BarChartGranularity.WEEK -> plusWeeks(1)
            BarChartGranularity.MONTH -> plusMonths(1)
            BarChartGranularity.QUARTER -> plusMonths(3)
            BarChartGranularity.YEAR -> plusYears(1)
            else -> plusDays(1)
        }

        fun LocalDate.label(): String = when (granularity) {
            BarChartGranularity.MONTH -> format(monthFormatter)
            BarChartGranularity.QUARTER -> "Q${(monthValue - 1) / 3 + 1}"
            BarChartGranularity.YEAR -> format(yearFormatter)
            else -> format(dayFormatter)
        }

        val buckets = mutableListOf<Triple<Long, Long, String>>()
        var current = first.alignedStart()
        while (!current.isAfter(last)) {
            val next = current.next()
            buckets.add(
                Triple(
                    current.atStartOfDay(zone).toInstant().toEpochMilli(),
                    next.atStartOfDay(zone).toInstant().toEpochMilli() - 1,
                    current.label()
                )
            )
            current = next
        }
        return buckets
    }

    fun resolveGranularity(rangeStart: Long, rangeEnd: Long): CashFlowComparisonGranularity {
        val spanDays = (rangeEnd - rangeStart) / 86_400_000L
        return when {
            spanDays <= 30L -> CashFlowComparisonGranularity.DAY
            spanDays <= 120L -> CashFlowComparisonGranularity.WEEK
            else -> CashFlowComparisonGranularity.MONTH
        }
    }

    fun generateBuckets(
        rangeStart: Long,
        rangeEnd: Long,
        granularity: CashFlowComparisonGranularity
    ): List<Triple<Long, Long, String>> {
        val zone = TimelineService.systemZoneId
        val buckets = mutableListOf<Triple<Long, Long, String>>()

        when (granularity) {
            CashFlowComparisonGranularity.DAY -> {
                var day = Instant.ofEpochMilli(rangeStart).atZone(zone).toLocalDate()
                val lastDay = Instant.ofEpochMilli(rangeEnd).atZone(zone).toLocalDate()
                while (!day.isAfter(lastDay)) {
                    val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = day.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    buckets.add(Triple(start, end, day.format(dayFormatter)))
                    day = day.plusDays(1)
                }
            }

            CashFlowComparisonGranularity.WEEK -> {
                var current = Instant.ofEpochMilli(rangeStart).atZone(zone).toLocalDate()
                val rangeEndDate = Instant.ofEpochMilli(rangeEnd).atZone(zone).toLocalDate()
                while (!current.isAfter(rangeEndDate)) {
                    val bucketEnd = current.plusDays(6)
                    val start = current.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = bucketEnd.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                    buckets.add(Triple(start, end, current.format(dayFormatter)))
                    current = current.plusWeeks(1)
                }
            }

            CashFlowComparisonGranularity.MONTH -> {
                var month = Instant.ofEpochMilli(rangeStart).atZone(zone).toLocalDate()
                val lastMonth = Instant.ofEpochMilli(rangeEnd).atZone(zone).toLocalDate()
                while (!month.isAfter(lastMonth)) {
                    val start = month.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = month.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    buckets.add(Triple(start, end, month.format(monthFormatter)))
                    month = month.plusMonths(1)
                }
            }
        }

        return buckets
    }
}
