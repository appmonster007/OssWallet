package com.osscabinet.ossWallet.domain.service.timeline

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Material's date pickers speak in "UTC midnight of the chosen day". A timeline range needs real
 * instants in the user's zone, covering the chosen days whole: from the first day's start to the
 * last day's final millisecond.
 */
object PickedDays {

    /** [startUtcMidnight]/[endUtcMidnight] as picked -> (start of first day, end of last day) in [zone]. */
    fun toRange(startUtcMidnight: Long, endUtcMidnight: Long, zone: ZoneId): Pair<Long, Long> {
        val first = Instant.ofEpochMilli(startUtcMidnight).atZone(ZoneOffset.UTC).toLocalDate()
        val last = Instant.ofEpochMilli(endUtcMidnight).atZone(ZoneOffset.UTC).toLocalDate()
        val (a, b) = if (last.isBefore(first)) last to first else first to last
        val start = a.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = b.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** The inverse for showing an existing range in the picker: the local day, as UTC midnight. */
    fun toPickerMillis(instant: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(instant).atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
