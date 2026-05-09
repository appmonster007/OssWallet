package com.osscabinet.ossWallet.domain.formatter

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Pure domain function for timeline formatting
 * No Android framework dependencies
 * Can be tested independently
 */
object TimelineFormatter {
    private val clock = Clock.System
    
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
        // Pure Kotlin date formatting without Android dependencies
        // Using kotlinx-datetime library
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = dateTime.month.name.take(3)
        val day = dateTime.dayOfMonth
        val year = dateTime.year
        return "$month $day, $year"
    }
}
