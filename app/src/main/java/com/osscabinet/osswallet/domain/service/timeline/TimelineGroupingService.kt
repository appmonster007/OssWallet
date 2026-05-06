package com.osscabinet.ossWallet.domain.service.timeline

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.FinancialMetrics
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Service for timeline grouping and time-based operations on transactions.
 *
 * This service is stateless and pure Kotlin (no Android dependencies).
 * All methods are independently testable.
 */
object TimelineGroupingService {

    val systemZoneId: ZoneId = ZoneId.systemDefault()
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    /**
     * Generates a list of time buckets (start/end pairs) for a given range.
     * Useful for chart generation.
     *
     * @param timelineRange The timeline range to generate buckets for
     * @param numPoints Number of points/buckets to generate
     * @return List of TimeBucket objects representing time intervals
     */
    fun getTimeBuckets(timelineRange: TimelineRange, numPoints: Int = 30): List<TimeBucket> {
        val (start, end) = TimelineService.getDateRange(timelineRange)
        val actualStart = start ?: 0L
        val actualEnd = end ?: System.currentTimeMillis()

        val duration = actualEnd - actualStart
        val step = if (numPoints > 0) duration / numPoints else 0L

        return if (step > 0) {
            (0 until numPoints).map { i ->
                TimeBucket(actualStart + (i * step), actualStart + ((i + 1) * step))
            }
        } else listOf(TimeBucket(actualStart, actualEnd))
    }

    /**
     * Calculates a cumulative sum (running total) across a list of financial metrics.
     * Useful for trajectory charts where each point represents the balance at that time.
     *
     * @param items List of financial metrics (e.g., DateBreakdown)
     * @param initialBalance Starting balance before the first item
     * @return List of TrajectoryPoint items with cumulative balance
     */
    fun <K> calculateCumulativeSum(
        items: List<FinancialMetrics<K>>,
        initialBalance: BigDecimal = BigDecimal.ZERO
    ): List<TrajectoryPoint<K>> {
        var currentBalance = initialBalance
        return items.map { item ->
            currentBalance += item.totalAmount
            TrajectoryPoint(
                key = item.key,
                income = item.income,
                expense = item.expense,
                totalAmount = item.totalAmount,
                cumulativeBalance = currentBalance,
                transactionCount = item.transactionCount
            )
        }
    }

    /**
     * Formats a timestamp as a month string (yyyy-MM).
     *
     * @param timestamp The timestamp in milliseconds
     * @return Formatted month string
     */
    fun formatMonth(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp).atZone(systemZoneId).format(monthFormatter)
    }

    /**
     * Converts a timestamp to a date string (yyyy-MM-dd).
     *
     * @param timestamp The timestamp in milliseconds
     * @return Formatted date string
     */
    fun formatDate(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp).atZone(systemZoneId).toLocalDate().toString()
    }
}

/**
 * Represents a range of time for grouping transactions.
 */
data class TimeBucket(val start: Long, val end: Long)

/**
 * Represents a point in a trajectory chart.
 */
data class TrajectoryPoint<K>(
    val key: K,
    val income: BigDecimal,
    val expense: BigDecimal,
    val totalAmount: BigDecimal,
    val cumulativeBalance: BigDecimal,
    val transactionCount: Int
)
