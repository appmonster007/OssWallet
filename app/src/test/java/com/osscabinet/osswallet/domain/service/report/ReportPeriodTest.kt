package com.osscabinet.ossWallet.domain.service.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReportPeriodTest {
    private val zone = ZoneId.of("UTC")
    private fun ms(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0, s: Int = 0) =
        ZonedDateTime.of(y, m, d, h, min, s, 0, zone).toInstant().toEpochMilli()

    private val now = ms(2026, 10, 16, 15, 30)

    @Test
    fun `this month spans the first to the last millisecond of the month`() {
        val r = ReportPeriod.ThisMonth.range(now, zone)
        assertEquals(ms(2026, 10, 1), r.first)
        assertEquals(ms(2026, 11, 1) - 1, r.last)
    }

    @Test
    fun `last month in January is December of the previous year`() {
        val r = ReportPeriod.LastMonth.range(ms(2026, 1, 20), zone)
        assertEquals(ms(2025, 12, 1), r.first)
        assertEquals(ms(2026, 1, 1) - 1, r.last)
    }

    @Test
    fun `last month handles a leap-year February`() {
        val r = ReportPeriod.LastMonth.range(ms(2028, 3, 5), zone)
        assertEquals(ms(2028, 2, 1), r.first)
        assertEquals(ms(2028, 3, 1) - 1, r.last)
    }

    @Test
    fun `last three months starts two months before the current one`() {
        val r = ReportPeriod.LastThreeMonths.range(now, zone)
        assertEquals(ms(2026, 8, 1), r.first)
        assertEquals(ms(2026, 11, 1) - 1, r.last)
    }

    @Test
    fun `last three months crosses a year boundary`() {
        val r = ReportPeriod.LastThreeMonths.range(ms(2026, 2, 10), zone)
        assertEquals(ms(2025, 12, 1), r.first)
    }

    @Test
    fun `this year and last year cover whole years`() {
        val ty = ReportPeriod.ThisYear.range(now, zone)
        assertEquals(ms(2026, 1, 1), ty.first); assertEquals(ms(2027, 1, 1) - 1, ty.last)
        val ly = ReportPeriod.LastYear.range(now, zone)
        assertEquals(ms(2025, 1, 1), ly.first); assertEquals(ms(2026, 1, 1) - 1, ly.last)
    }

    @Test
    fun `custom range includes the whole last day`() {
        val r = ReportPeriod.Custom(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)).range(now, zone)
        assertEquals(ms(2026, 3, 1), r.first)
        assertTrue("a transaction at 23:59 on the last day is inside", ms(2026, 3, 31, 23, 59, 59) in r)
        assertTrue("midnight of the next day is outside", ms(2026, 4, 1) !in r)
    }

    @Test
    fun `a single-day custom range is valid`() {
        val d = LocalDate.of(2026, 5, 5)
        val r = ReportPeriod.Custom(d, d).range(now, zone)
        assertEquals(ms(2026, 5, 5), r.first); assertEquals(ms(2026, 5, 6) - 1, r.last)
    }

    @Test
    fun `an end before the start is rejected`() {
        try {
            ReportPeriod.Custom(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1)); fail("expected rejection")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `ranges follow the supplied time zone, not UTC`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val r = ReportPeriod.ThisMonth.range(ms(2026, 10, 31, 20), tokyo) // 21:00 Oct 31 UTC = Nov 1 05:00 in Tokyo
        assertEquals(ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, tokyo).toInstant().toEpochMilli(), r.first)
    }

    @Test
    fun `daylight saving shifts do not lose or add a day`() {
        val ny = ZoneId.of("America/New_York")
        val r = ReportPeriod.Custom(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8)).range(now, ny) // spring-forward day
        assertEquals(23L * 3600 * 1000, r.last - r.first + 1)
    }

    @Test
    fun `presets are complete and distinct whichever period is touched first`() {
        ReportPeriod.LastYear // touching a subclass first is what used to leave nulls in the list
        val presets = ReportPeriod.presets
        assertEquals(5, presets.size)
        assertEquals(5, presets.map { it.label }.toSet().size)
        presets.forEach { it.range(now, zone) }
    }
}
