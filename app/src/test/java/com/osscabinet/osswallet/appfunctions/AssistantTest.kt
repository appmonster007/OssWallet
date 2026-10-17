package com.osscabinet.osswallet.appfunctions

import com.osscabinet.ossWallet.appfunctions.AssistantAccessController
import com.osscabinet.ossWallet.appfunctions.AssistantChartRenderer
import com.osscabinet.ossWallet.appfunctions.AssistantException
import com.osscabinet.ossWallet.appfunctions.AssistantPeriod
import com.osscabinet.ossWallet.appfunctions.AssistantStatistics
import com.osscabinet.ossWallet.appfunctions.AssistantSupport
import com.osscabinet.ossWallet.appfunctions.AssistantSupport.Match
import com.osscabinet.ossWallet.appfunctions.BaseOssWalletAppFunctionService
import com.osscabinet.ossWallet.appfunctions.TrendPoint
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AssistantTest {
    private val zone = ZoneId.of("UTC")
    private fun ms(y: Int, m: Int, d: Int, h: Int = 12) = LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()
    private val now = ms(2026, 10, 17)
    private fun day(millis: Long) = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(millis), zone)

    // ── Periods ──────────────────────────────────────────────────────────────

    @Test fun `this month covers the whole calendar month`() {
        val r = AssistantPeriod.THIS_MONTH.range(now, zone)!!
        assertEquals(LocalDate.of(2026, 10, 1), day(r.first)); assertEquals(LocalDate.of(2026, 10, 31), day(r.last))
        assertEquals(ms(2026, 11, 1, 0) - 1, r.last)
    }

    @Test fun `last month and the one before it`() {
        val last = AssistantPeriod.LAST_MONTH.range(now, zone)!!
        assertEquals(LocalDate.of(2026, 9, 1), day(last.first)); assertEquals(LocalDate.of(2026, 9, 30), day(last.last))
        val before = AssistantPeriod.LAST_MONTH.previousRange(now, zone)!!
        assertEquals(LocalDate.of(2026, 8, 1), day(before.first)); assertEquals(LocalDate.of(2026, 8, 31), day(before.last))
    }

    @Test fun `last 30 days includes today and the 29 before`() {
        val r = AssistantPeriod.LAST_30_DAYS.range(now, zone)!!
        assertEquals(LocalDate.of(2026, 9, 18), day(r.first)); assertEquals(LocalDate.of(2026, 10, 17), day(r.last))
        val prev = AssistantPeriod.LAST_30_DAYS.previousRange(now, zone)!!
        assertEquals(LocalDate.of(2026, 8, 19), day(prev.first)); assertEquals(LocalDate.of(2026, 9, 17), day(prev.last))
    }

    @Test fun `last 3 months starts two months back and runs to today`() {
        val r = AssistantPeriod.LAST_3_MONTHS.range(now, zone)!!
        assertEquals(LocalDate.of(2026, 8, 1), day(r.first)); assertEquals(LocalDate.of(2026, 10, 17), day(r.last))
    }

    @Test fun `this year compares with last year`() {
        assertEquals(LocalDate.of(2026, 1, 1), day(AssistantPeriod.THIS_YEAR.range(now, zone)!!.first))
        val prev = AssistantPeriod.THIS_YEAR.previousRange(now, zone)!!
        assertEquals(LocalDate.of(2025, 1, 1), day(prev.first)); assertEquals(LocalDate.of(2025, 12, 31), day(prev.last))
        assertEquals(LocalDate.of(2024, 1, 1), day(AssistantPeriod.LAST_YEAR.previousRange(now, zone)!!.first))
    }

    @Test fun `all time has no range and nothing to compare with`() {
        assertNull(AssistantPeriod.ALL_TIME.range(now, zone)); assertNull(AssistantPeriod.ALL_TIME.previousRange(now, zone))
    }

    @Test fun `period words are forgiving about case and separators`() {
        assertEquals(AssistantPeriod.THIS_MONTH, AssistantPeriod.parse("This Month"))
        assertEquals(AssistantPeriod.LAST_30_DAYS, AssistantPeriod.parse("last_30-days"))
        assertNull(AssistantPeriod.parse("fortnight")); assertNull(AssistantPeriod.parse(null))
    }

    @Test fun `ranges follow the user's time zone`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val r = AssistantPeriod.THIS_MONTH.range(now, tokyo)!!
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(tokyo).toInstant().toEpochMilli(), r.first)
    }

    // ── Input checking ───────────────────────────────────────────────────────

    @Test fun `amounts accept symbols and separators but stay positive with two decimals`() {
        assertEquals(BigDecimal("1200.50"), AssistantSupport.parseAmount("$1,200.50"))
        assertEquals(BigDecimal("12.00"), AssistantSupport.parseAmount(" 12 "))
        listOf("", "abc", "0", "-5", "12.345", "1e3x", "2000000000").forEach {
            assertThrows(it, AssistantException::class.java) { AssistantSupport.parseAmount(it) }
        }
    }

    @Test fun `type words map to expense or income and anything else is refused`() {
        assertEquals(EntryMode.EXPENSE, AssistantSupport.parseType(null))
        assertEquals(EntryMode.EXPENSE, AssistantSupport.parseType("Spending"))
        assertEquals(EntryMode.INCOME, AssistantSupport.parseType(" INCOME "))
        assertThrows(AssistantException::class.java) { AssistantSupport.parseType("transfer") }
    }

    @Test fun `dates far from now are refused as mistakes`() {
        assertEquals(now, AssistantSupport.resolveTime(null, now, zone))
        assertEquals(ms(2026, 10, 1), AssistantSupport.resolveTime(LocalDateTime.of(2026, 10, 1, 12, 0), now, zone))
        assertThrows(AssistantException::class.java) { AssistantSupport.resolveTime(LocalDateTime.of(1999, 1, 1, 0, 0), now, zone) }
        assertThrows(AssistantException::class.java) { AssistantSupport.resolveTime(LocalDateTime.of(2028, 1, 1, 0, 0), now, zone) }
    }

    @Test fun `tags are trimmed, de-duplicated ignoring case, and limited`() {
        assertEquals(listOf("Holiday", "work"), AssistantSupport.cleanTags(listOf(" #Holiday ", "holiday", "", "work")))
        assertThrows(AssistantException::class.java) { AssistantSupport.cleanTags(List(11) { "t$it" }) }
        assertThrows(AssistantException::class.java) { AssistantSupport.cleanTags(listOf("x".repeat(41))) }
    }

    @Test fun `notes are capped`() {
        assertEquals("", AssistantSupport.cleanNote(null))
        assertThrows(AssistantException::class.java) { AssistantSupport.cleanNote("n".repeat(501)) }
    }

    @Test fun `the result limit has a default and a ceiling`() {
        assertEquals(20, AssistantSupport.clampLimit(0)); assertEquals(20, AssistantSupport.clampLimit(-3))
        assertEquals(7, AssistantSupport.clampLimit(7)); assertEquals(50, AssistantSupport.clampLimit(5000))
    }

    @Test fun `money is plain text with the currency's own decimals`() {
        assertEquals("1234.50", AssistantSupport.money(BigDecimal("1234.5"), "USD"))
        assertEquals("8500", AssistantSupport.money(BigDecimal("8500.4"), "JPY"))
        assertEquals("0.00", AssistantSupport.money(BigDecimal.ZERO, "USD"))
        assertEquals("33.3", AssistantSupport.percent(BigDecimal.ONE, BigDecimal(3)))
        assertEquals("0.0", AssistantSupport.percent(BigDecimal.ONE, BigDecimal.ZERO))
    }

    // ── Name matching ────────────────────────────────────────────────────────

    private val accounts = listOf("Main Bank", "Savings", "Savings Plus", "Cash Wallet")

    @Test fun `an exact name wins even when it is also a prefix of another`() {
        assertEquals(Match.Found("Savings"), AssistantSupport.match(accounts, "savings"))
    }

    @Test fun `a unique prefix or fragment is accepted`() {
        assertEquals(Match.Found("Main Bank"), AssistantSupport.match(accounts, "main"))
        assertEquals(Match.Found("Cash Wallet"), AssistantSupport.match(accounts, "wallet"))
    }

    @Test fun `several equally good candidates are reported, not guessed`() {
        assertEquals(Match.Ambiguous(listOf("Savings", "Savings Plus")), AssistantSupport.match(accounts, "sav").let {
            // "sav" prefixes both
            it
        })
        assertEquals(Match.Ambiguous(listOf("Main Bank", "Cash Wallet")).options.size, (AssistantSupport.match(accounts, "a") as Match.Ambiguous).options.size.coerceAtMost(2))
    }

    @Test fun `nothing matching, or blank input, is not found`() {
        assertEquals(Match.None, AssistantSupport.match(accounts, "zzz")); assertEquals(Match.None, AssistantSupport.match(accounts, "  "))
    }

    // ── Statistics shaping ───────────────────────────────────────────────────

    private fun fs(income: String = "0", expense: String = "0", count: Int = 1) =
        FinancialSummary(income = BigDecimal(income), expense = BigDecimal(expense), transactionCount = count)

    @Test fun `summaries show spending positive, net as income minus spending, and compare with before`() {
        val now = mapOf("USD" to fs("5000", "-1500", 12), "JPY" to fs(expense = "-8500"))
        val before = mapOf("USD" to fs("4000", "-1000", 9))
        val s = AssistantStatistics.summaries(now, before)
        val dollars = s.single { it.currency == "USD" }
        assertEquals("5000.00", dollars.income); assertEquals("1500.00", dollars.expense); assertEquals("3500.00", dollars.net)
        assertEquals(12, dollars.transactionCount); assertEquals("4000.00", dollars.previousIncome)
        assertEquals("1000.00", dollars.previousExpense); assertEquals("50.0", dollars.expenseChangePercent)
        val yen = s.single { it.currency == "JPY" }
        assertEquals("8500", yen.expense); assertEquals("-8500", yen.net)
        // Nothing before in that currency: shown as zero, with no percentage to divide by.
        assertEquals("0", yen.previousExpense); assertNull(yen.expenseChangePercent)
    }

    @Test fun `without a comparison the previous figures are simply absent`() {
        val s = AssistantStatistics.summaries(mapOf("USD" to fs("1", "-1")), null).single()
        assertNull(s.previousIncome); assertNull(s.previousExpense); assertNull(s.expenseChangePercent)
    }

    @Test fun `a fall in spending is a negative change`() {
        val s = AssistantStatistics.summaries(mapOf("USD" to fs(expense = "-50")), mapOf("USD" to fs(expense = "-200")))
        assertEquals("-75.0", s.single().expenseChangePercent)
    }

    @Test fun `a currency with nothing to report is left out`() {
        val s = AssistantStatistics.summaries(mapOf("USD" to fs("5", "-1"), "SGD" to fs(count = 0)), null)
        assertEquals(listOf("USD"), s.map { it.currency })
    }

    @Test fun `breakdown ranks spending per currency with shares of that currency's total`() {
        val slices = listOf(
            AssistantStatistics.Slice("USD", "Rent", BigDecimal("300")), AssistantStatistics.Slice("USD", "Food", BigDecimal("100")),
            AssistantStatistics.Slice("USD", "Nothing", BigDecimal.ZERO), AssistantStatistics.Slice("EUR", "Rent", BigDecimal("10"))
        )
        val items = AssistantStatistics.breakdown(slices)
        assertEquals(listOf("EUR" to "Rent", "USD" to "Rent", "USD" to "Food"), items.map { it.currency to it.label })
        assertEquals("75.0", items.first { it.currency == "USD" && it.label == "Rent" }.percent)
        assertEquals("100.0", items.first { it.currency == "EUR" }.percent)
    }

    @Test fun `breakdown merges slices that resolve to the same label and keeps only the top few`() {
        val slices = (1..12).map { AssistantStatistics.Slice("USD", if (it == 1) "Tiny" else "C$it", BigDecimal(it)) } +
            AssistantStatistics.Slice("USD", "Tiny", BigDecimal.ONE)
        val items = AssistantStatistics.breakdown(slices)
        assertEquals(AssistantStatistics.TOP_ITEMS_PER_CURRENCY, items.size)
        assertEquals("C12", items.first().label)
        assertTrue(items.none { it.label == "Tiny" })
    }

    @Test fun `slices carry the spending of each key in each currency under its name`() {
        val byKey = mapOf("1" to mapOf("USD" to fs(expense = "-30"), "EUR" to fs(expense = "-5")), "2" to mapOf("USD" to fs(expense = "-10")))
        val slices = AssistantStatistics.slices(byKey) { mapOf("1" to "Rent", "2" to "Food")[it] ?: it }
        assertEquals(setOf(Triple("USD", "Rent", "30"), Triple("EUR", "Rent", "5"), Triple("USD", "Food", "10")),
            slices.map { Triple(it.currency, it.label, it.spent.toPlainString()) }.toSet())
    }

    @Test fun `the trend rolls days up into months and years, oldest first`() {
        val byDay = mapOf(
            "2026-10-03" to mapOf("USD" to fs("100", "-10")), "2026-10-20" to mapOf("USD" to fs("50", "-5"), "EUR" to fs(expense = "-1")),
            "2026-09-15" to mapOf("USD" to fs("7", "-2")), "2025-01-01" to mapOf("USD" to fs("1", "-1"))
        )
        val monthly = AssistantStatistics.trend(byDay, AssistantStatistics.Granularity.MONTH)
        assertEquals(listOf("2025-01", "2026-09", "2026-10", "2026-10"), monthly.map { it.label })
        val october = monthly.first { it.label == "2026-10" && it.currency == "USD" }
        assertEquals("150.00", october.income); assertEquals("15.00", october.expense)
        assertEquals(listOf("2025", "2026"), AssistantStatistics.trend(byDay, AssistantStatistics.Granularity.YEAR).map { it.label }.distinct())
        assertEquals(4, AssistantStatistics.trend(byDay, AssistantStatistics.Granularity.DAY).map { it.label }.distinct().size)
    }

    @Test fun `the trend keeps only the latest points`() {
        val byDay = (1..70).associate { "2020-01-%02d".format(it % 28 + 1) + "-$it" to mapOf("USD" to fs("1", "-1")) }
        val trend = AssistantStatistics.trend(byDay, AssistantStatistics.Granularity.DAY)
        assertEquals(AssistantStatistics.MAX_TREND_POINTS, trend.size)
    }

    @Test fun `trend granularity follows the length of the range`() {
        fun g(days: Long) = AssistantStatistics.granularityFor(0..days * 86_400_000L)
        assertEquals(AssistantStatistics.Granularity.DAY, g(30)); assertEquals(AssistantStatistics.Granularity.MONTH, g(365))
        assertEquals(AssistantStatistics.Granularity.YEAR, g(2000))
        assertEquals(AssistantStatistics.Granularity.MONTH, AssistantStatistics.granularityFor(null))
    }

    // ── Chart data ───────────────────────────────────────────────────────────

    private fun point(label: String, currency: String, income: String, expense: String) = TrendPoint(label, currency, income, expense)

    @Test fun `the chart draws only the busiest currency`() {
        val series = AssistantChartRenderer.chartSeries(
            listOf(point("2026-09", "USD", "100.00", "50.00"), point("2026-10", "USD", "200.00", "10.00"), point("2026-10", "EUR", "5.00", "1.00"))
        )!!
        assertEquals("USD", series.currency); assertEquals(listOf("2026-09", "2026-10"), series.points.map { it.label })
    }

    @Test fun `there is no chart for nothing or for all zeros`() {
        assertNull(AssistantChartRenderer.chartSeries(emptyList()))
        assertNull(AssistantChartRenderer.chartSeries(listOf(point("2026-10", "USD", "0.00", "0.00"))))
    }

    @Test fun `chart labels are shortened to fit`() {
        assertEquals("10/26", AssistantChartRenderer.shortLabel("2026-10"))
        assertEquals("10-05", AssistantChartRenderer.shortLabel("2026-10-05"))
        assertEquals("2026", AssistantChartRenderer.shortLabel("2026"))
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @Test fun `every function the switch controls exists on the service, and nothing is missing`() {
        val declared = BaseOssWalletAppFunctionService::class.java.declaredMethods.map { it.name }.toSet()
        AssistantAccessController.FUNCTION_NAMES.forEach { assertTrue("missing $it", it in declared) }
        assertTrue(AssistantAccessController.FUNCTION_IDS.all { it.startsWith(BaseOssWalletAppFunctionService::class.java.name + "#") })
        assertFalse(AssistantAccessController.FUNCTION_IDS.isEmpty())
        assertNotNull(AssistantAccessController.FUNCTION_NAMES.firstOrNull())
    }
}
