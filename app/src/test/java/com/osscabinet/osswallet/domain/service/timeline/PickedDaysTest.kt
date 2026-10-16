package com.osscabinet.ossWallet.domain.service.timeline

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class PickedDaysTest {
    private fun utcMidnight(y: Int, m: Int, d: Int) =
        LocalDate.of(y, m, d).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun local(zone: ZoneId, y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDate.of(y, m, d).atTime(h, min).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `range covers whole local days including the last one`() {
        for (id in listOf("UTC", "Asia/Kolkata", "America/Los_Angeles", "Pacific/Auckland")) {
            val zone = ZoneId.of(id)
            val (start, end) = PickedDays.toRange(utcMidnight(2026, 3, 1), utcMidnight(2026, 3, 31), zone)
            assertEquals(id, local(zone, 2026, 3, 1, 0, 0), start)
            assertEquals(id, local(zone, 2026, 4, 1, 0, 0) - 1, end)
        }
    }

    @Test
    fun `a transaction late on the last day is inside the range`() {
        val zone = ZoneId.of("Asia/Kolkata")
        val (start, end) = PickedDays.toRange(utcMidnight(2026, 3, 1), utcMidnight(2026, 3, 31), zone)
        val lateOnLastDay = local(zone, 2026, 3, 31, 23, 30)
        assert(lateOnLastDay in start..end)
    }

    @Test
    fun `a single day is that whole day`() {
        val zone = ZoneId.of("Europe/Berlin")
        val d = utcMidnight(2026, 6, 15)
        val (start, end) = PickedDays.toRange(d, d, zone)
        assertEquals(local(zone, 2026, 6, 15, 0, 0), start)
        assertEquals(local(zone, 2026, 6, 16, 0, 0) - 1, end)
    }

    @Test
    fun `reversed picks are put in order`() {
        val zone = ZoneId.of("UTC")
        val a = PickedDays.toRange(utcMidnight(2026, 3, 31), utcMidnight(2026, 3, 1), zone)
        val b = PickedDays.toRange(utcMidnight(2026, 3, 1), utcMidnight(2026, 3, 31), zone)
        assertEquals(b, a)
    }

    @Test
    fun `picker millis round trip to the same local day`() {
        val zone = ZoneId.of("America/Los_Angeles")
        val (start, end) = PickedDays.toRange(utcMidnight(2026, 3, 1), utcMidnight(2026, 3, 31), zone)
        assertEquals(utcMidnight(2026, 3, 1), PickedDays.toPickerMillis(start, zone))
        assertEquals(utcMidnight(2026, 3, 31), PickedDays.toPickerMillis(end, zone))
    }
}
