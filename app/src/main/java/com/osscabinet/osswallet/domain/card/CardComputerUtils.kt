package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.time.Instant
import java.time.format.DateTimeFormatter

object CardComputerUtils {

    private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMM")

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
