package com.osscabinet.ossWallet.domain.formatter

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Pure domain function for timeline formatting
 * No Android framework dependencies
 * Can be tested independently
 */
object TimelineFormatter {

    fun format(range: TimelineRange): String = when (range) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> range.period.label
        is TimelineRange.LastPeriod -> range.period.label
        is TimelineRange.CustomRange -> {
            val start = range.start?.let { formatDate(it) } ?: "..."
            val end = range.end?.let { formatDate(it) } ?: "..."
            "$start - $end"
        }
    }

    private fun formatDate(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = dateTime.month.name.take(3)
        val day = dateTime.day
        val year = dateTime.year
        return "$month $day, $year"
    }
}
