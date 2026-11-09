package com.osscabinet.osswallet.domain.service.search

import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.search.AmountSpec
import com.osscabinet.ossWallet.domain.service.search.GlobalSearchService
import com.osscabinet.ossWallet.domain.service.search.SearchQueryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class GlobalSearchTest {

    private fun parse(s: String) = SearchQueryParser.parse(s)

    // ── Query parsing ────────────────────────────────────────────────────────

    @Test fun `words are lower-cased and kept in order`() {
        assertEquals(listOf("laptop", "dell"), parse("Laptop  DELL").words)
    }

    @Test fun `a hash prefix is a tag`() {
        val p = parse("#Holiday flights")
        assertEquals(listOf("holiday"), p.tags)
        assertEquals(listOf("flights"), p.words)
    }

    @Test fun `a lone hash is just text`() {
        assertEquals(listOf("#"), parse("#").words)
    }

    @Test fun `an exact amount is compared to the cent`() {
        val spec = parse("49.99 coffee").amount!!
        assertTrue(spec.matches(BigDecimal("49.990000")))
        assertTrue(spec.matches(BigDecimal("-49.99")))
        assertFalse(spec.matches(BigDecimal("49.98")))
    }

    @Test fun `greater and less than make bounds`() {
        assertEquals(AmountSpec(BigDecimal("100"), null, minInclusive = false), parse(">100").amount)
        assertEquals(AmountSpec(null, BigDecimal("50"), maxInclusive = false), parse("<50").amount)
        val both = parse(">20 <50").amount!!
        assertTrue(both.matches(BigDecimal("30")))
        assertFalse(both.matches(BigDecimal("60")))
        assertFalse(both.matches(BigDecimal("10")))
    }

    @Test fun `greater than leaves the bound out and greater-or-equal keeps it`() {
        assertFalse(parse(">100").amount!!.matches(BigDecimal("100")))
        assertTrue(parse(">100").amount!!.matches(BigDecimal("100.01")))
        assertTrue(parse(">=100").amount!!.matches(BigDecimal("100")))
        assertFalse(parse("<50").amount!!.matches(BigDecimal("50")))
        assertTrue(parse("<=50").amount!!.matches(BigDecimal("50")))
    }

    @Test fun `a dash range is inclusive and order does not matter`() {
        val spec = parse("50-20").amount!!
        assertTrue(spec.matches(BigDecimal("20")))
        assertTrue(spec.matches(BigDecimal("50")))
        assertFalse(spec.matches(BigDecimal("51")))
    }

    @Test fun `currency symbols and thousands separators are understood`() {
        assertTrue(parse("$1,200.50").amount!!.matches(BigDecimal("1200.50")))
        assertTrue(parse(">€1,000").amount!!.matches(BigDecimal("1500")))
    }

    @Test fun `a lone number is both an amount and a word`() {
        val p = parse("15")
        assertTrue(p.numberOnly)
        assertEquals(listOf("15"), p.words)
        assertTrue(p.amount!!.matches(BigDecimal("15")))
    }

    @Test fun `a number next to words is an amount filter, not a word`() {
        val p = parse("laptop 1200")
        assertFalse(p.numberOnly)
        assertEquals(listOf("laptop"), p.words)
        assertTrue(p.amount!!.matches(BigDecimal("1200")))
    }

    @Test fun `a year next to words stays a word`() {
        val p = parse("rent 2026")
        assertEquals(listOf("rent", "2026"), p.words)
        assertNull(p.amount)
    }

    @Test fun `a lone year may still be an amount`() {
        assertTrue(parse("2026").numberOnly)
    }

    @Test fun `blank input is empty`() {
        assertTrue(parse("   ").isEmpty)
    }

    // ── Transactions ─────────────────────────────────────────────────────────

    private fun tx(amount: String, tags: Set<String> = emptySet(), payee: String = "Shop") = Transaction(
        id = 1, name = "x", amount = BigDecimal(amount), timestamp = 0, currency = "USD",
        transactionType = TransactionType.EXPENSE, accountId = "main bank", categoryId = 1, tags = tags, payee = payee, note = "n"
    )

    @Test fun `tags must all be on the transaction, by prefix`() {
        assertTrue(GlobalSearchService.matchesTransaction(parse("#hol"), tx("5", setOf("Holiday"))))
        assertFalse(GlobalSearchService.matchesTransaction(parse("#hol #work"), tx("5", setOf("Holiday"))))
    }

    @Test fun `an amount filter drops transactions outside it`() {
        assertTrue(GlobalSearchService.matchesTransaction(parse("coffee >10"), tx("-12")))
        assertFalse(GlobalSearchService.matchesTransaction(parse("coffee >10"), tx("-8")))
    }

    @Test fun `a lone number keeps a text match even if the amount differs`() {
        assertTrue(GlobalSearchService.matchesTransaction(parse("15"), tx("99.00")))
    }
}
