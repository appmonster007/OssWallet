package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class NetWorthTrendTest {

    private val zone = TimelineService.systemZoneId
    private val usd = ExchangeRateSnapshot(anchor = "USD", rates = emptyMap())

    private fun start(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Three daily buckets: Oct 1, 2 and 3. */
    private val buckets = (1..3).map { day ->
        val d = LocalDate.of(2026, 10, day)
        Triple(start(d), start(d.plusDays(1)) - 1, "0$day/10")
    }


    @Test
    fun `running balance starts from the opening balance and adds each bucket`() {
        val points = NetWorthTrend.build(
            buckets = buckets,
            openingByCurrency = mapOf("USD" to BigDecimal("1000")),
            dailyNetByCurrency = mapOf(
                "2026-10-01" to mapOf("USD" to BigDecimal("100")),
                "2026-10-03" to mapOf("USD" to BigDecimal("-250"))
            ),
            targetCurrency = "USD",
            exchangeRates = usd
        )

        assertEquals(listOf("1100.00", "1100.00", "850.00"), points.map { it.value.setScale(2).toPlainString() })
        assertEquals(listOf("01/10", "02/10", "03/10"), points.map { it.label })
    }

    @Test
    fun `several days inside one bucket are summed`() {
        val weekBuckets = listOf(
            Triple(start(LocalDate.of(2026, 10, 1)), start(LocalDate.of(2026, 10, 8)) - 1, "W40"),
            Triple(start(LocalDate.of(2026, 10, 8)), start(LocalDate.of(2026, 10, 15)) - 1, "W41")
        )
        val points = NetWorthTrend.build(
            buckets = weekBuckets,
            openingByCurrency = emptyMap(),
            dailyNetByCurrency = mapOf(
                "2026-10-02" to mapOf("USD" to BigDecimal("10")),
                "2026-10-05" to mapOf("USD" to BigDecimal("5")),
                "2026-10-09" to mapOf("USD" to BigDecimal("1"))
            ),
            targetCurrency = "USD",
            exchangeRates = usd
        )

        assertEquals(listOf("15.00", "16.00"), points.map { it.value.setScale(2).toPlainString() })
    }

    @Test
    fun `each currency keeps its own running total before conversion`() {
        val rates = ExchangeRateSnapshot(anchor = "USD", rates = mapOf("EUR" to BigDecimal("0.5")))
        val points = NetWorthTrend.build(
            buckets = buckets,
            openingByCurrency = mapOf("USD" to BigDecimal("100"), "EUR" to BigDecimal("50")),
            dailyNetByCurrency = mapOf("2026-10-02" to mapOf("EUR" to BigDecimal("50"))),
            targetCurrency = "USD",
            exchangeRates = rates
        )

        // 50 EUR at 0.5 EUR per USD = 100 USD, so the first point is 100 + 100 and the second 100 + 200.
        assertEquals(listOf("200.00", "300.00", "300.00"), points.map { it.value.setScale(2).toPlainString() })
    }

    @Test
    fun `days outside every bucket are ignored`() {
        val points = NetWorthTrend.build(
            buckets = buckets,
            openingByCurrency = emptyMap(),
            dailyNetByCurrency = mapOf(
                "2026-09-30" to mapOf("USD" to BigDecimal("999")),
                "not-a-date" to mapOf("USD" to BigDecimal("999"))
            ),
            targetCurrency = "USD",
            exchangeRates = usd
        )

        assertEquals(listOf("0.00", "0.00", "0.00"), points.map { it.value.setScale(2).toPlainString() })
    }

    @Test
    fun `fewer than two buckets gives no line`() {
        assertTrue(NetWorthTrend.build(buckets.take(1), emptyMap(), emptyMap(), "USD", usd).isEmpty())
        assertTrue(NetWorthTrend.build(emptyList(), emptyMap(), emptyMap(), "USD", usd).isEmpty())
    }
}
