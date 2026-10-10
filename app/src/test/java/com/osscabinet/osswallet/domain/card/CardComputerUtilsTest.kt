package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.settings.BarChartGranularity
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CardComputerUtilsTest {

    private val zone = TimelineService.systemZoneId

    private fun start(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun end(date: LocalDate) = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    private fun buckets(from: LocalDate, to: LocalDate, granularity: BarChartGranularity) =
        CardComputerUtils.generateAlignedBuckets(start(from), end(to), granularity)

    @Test
    fun `day buckets cover every day in the range, empty or not`() {
        val result = buckets(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 2), BarChartGranularity.DAY)

        assertEquals(listOf("28/09", "29/09", "30/09", "01/10", "02/10"), result.map { it.third })
    }

    @Test
    fun `week buckets start on Monday even when the range starts mid-week`() {
        // 2026-10-07 is a Wednesday; its week starts Monday 2026-10-05.
        val result = buckets(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 20), BarChartGranularity.WEEK)

        assertEquals(listOf(start(LocalDate.of(2026, 10, 5)),
            start(LocalDate.of(2026, 10, 12)),
            start(LocalDate.of(2026, 10, 19))), result.map { it.first })
    }

    @Test
    fun `week buckets are labelled with the ISO week number`() {
        // 2026-10-05 is a Monday in ISO week 41; 2026-12-28 starts ISO week 53; 2027-01-04 starts week 1.
        val result = buckets(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 20), BarChartGranularity.WEEK)
        val yearEnd = buckets(LocalDate.of(2026, 12, 29), LocalDate.of(2027, 1, 5), BarChartGranularity.WEEK)

        assertEquals(listOf("W41", "W42", "W43"), result.map { it.third })
        assertEquals(listOf("W53", "W01"), yearEnd.map { it.third })
    }

    @Test
    fun `month buckets align to the first of the month and label with three letters`() {
        val result = buckets(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 10), BarChartGranularity.MONTH)

        assertEquals(listOf("Jan", "Feb", "Mar"), result.map { it.third })
        assertEquals(start(LocalDate.of(2026, 1, 1)), result.first().first)
    }

    @Test
    fun `quarter buckets align to calendar quarters`() {
        val result = buckets(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 8, 1), BarChartGranularity.QUARTER)

        assertEquals(listOf("Q1", "Q2", "Q3"), result.map { it.third })
        assertEquals(start(LocalDate.of(2026, 4, 1)), result[1].first)
    }

    @Test
    fun `year buckets are labelled with the year`() {
        val result = buckets(LocalDate.of(2024, 6, 1), LocalDate.of(2026, 2, 1), BarChartGranularity.YEAR)

        assertEquals(listOf("2024", "2025", "2026"), result.map { it.third })
    }

    @Test
    fun `buckets are contiguous with no gaps or overlaps`() {
        BarChartGranularity.entries.filter { it != BarChartGranularity.AUTO }.forEach { granularity ->
            val result = buckets(LocalDate.of(2025, 11, 20), LocalDate.of(2026, 10, 10), granularity)

            result.zipWithNext().forEach { (a, b) ->
                assertEquals(a.second + 1, b.first)
            }
        }
    }

    @Test
    fun `range that ends before it starts has no buckets`() {
        val result = CardComputerUtils.generateAlignedBuckets(
            start(LocalDate.of(2026, 10, 10)), start(LocalDate.of(2026, 10, 1)), BarChartGranularity.DAY
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `AUTO resolves from the range span and explicit choices are kept`() {
        val weekStart = start(LocalDate.of(2026, 10, 1))

        assertEquals(BarChartGranularity.DAY, CardComputerUtils.resolveBarGranularity(BarChartGranularity.AUTO, weekStart, weekStart + 10 * DAY_MS))
        assertEquals(BarChartGranularity.WEEK, CardComputerUtils.resolveBarGranularity(BarChartGranularity.AUTO, weekStart, weekStart + 90 * DAY_MS))
        assertEquals(BarChartGranularity.MONTH, CardComputerUtils.resolveBarGranularity(BarChartGranularity.AUTO, weekStart, weekStart + 300 * DAY_MS))
        assertEquals(BarChartGranularity.YEAR, CardComputerUtils.resolveBarGranularity(BarChartGranularity.YEAR, weekStart, weekStart + 10 * DAY_MS))
    }

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}
