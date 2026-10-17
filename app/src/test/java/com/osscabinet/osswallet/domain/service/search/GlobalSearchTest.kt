package com.osscabinet.osswallet.domain.service.search

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Goal
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.search.AmountSpec
import com.osscabinet.ossWallet.domain.service.search.GlobalSearchService
import com.osscabinet.ossWallet.domain.service.search.SearchData
import com.osscabinet.ossWallet.domain.service.search.SearchQueryParser
import com.osscabinet.ossWallet.domain.service.search.SearchSection
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

    // ── Records other than transactions ──────────────────────────────────────

    private fun account(name: String, id: String = name.lowercase()) =
        Account(id = id, name = name, currencyCode = "USD", structureType = AccountStructureType.BANK, colorHex = "#fff", icon = "x")

    private fun category(id: Long, name: String) = Category(id, name, "x", 0, false, null)

    private fun goal(name: String, target: String = "1200") = Goal(
        name = name, targetAmount = BigDecimal(target), currency = "USD", profileId = 1, createdAt = 0
    )

    private fun loan(name: String, who: String, amount: String = "500") = Loan(
        name = name, principalAmount = BigDecimal(amount), currency = "USD", startDate = 0, lenderOrBorrowerName = who,
        loanType = LoanType.BORROWED, linkedAccountId = "a", profileId = 1
    )

    private fun template(name: String, payee: String?, tags: Set<String> = emptySet(), amount: String = "10") = Template(
        name = name, categoryId = 1, amount = BigDecimal(amount), entryMode = EntryMode.EXPENSE, accountId = "a",
        payee = payee, currency = "USD", tags = tags
    )

    private fun planned(name: String, amount: String = "15.99") = PlannedPayment(
        name = name, categoryId = 1, amount = BigDecimal(amount), entryMode = EntryMode.EXPENSE, currency = "USD",
        schedule = PaymentSchedule(RecurrenceRule(RecurrenceType.MONTHLY), 0, nextDueDate = 0)
    )

    private val data = SearchData(
        accounts = listOf(account("Main Bank"), account("Laptop Fund")),
        categories = listOf(category(1, "Electronics"), category(2, "Groceries")),
        tags = listOf(Tag(name = "holiday"), Tag(name = "work")),
        loans = listOf(loan("Laptop loan", "Sam")),
        goals = listOf(goal("Laptop")),
        templates = listOf(template("Coffee", "Starbucks", tags = setOf("Work"))),
        plannedPayments = listOf(planned("Netflix"))
    )

    private fun search(q: String) = GlobalSearchService.searchRecords(parse(q), data)

    @Test fun `a word finds matching records across types`() {
        val r = search("laptop")
        assertEquals(setOf(SearchSection.ACCOUNTS, SearchSection.LOANS, SearchSection.GOALS), r.keys)
        assertEquals("Laptop Fund", r[SearchSection.ACCOUNTS]!!.single().title)
    }

    @Test fun `every word has to match`() {
        assertTrue(search("laptop sam").keys == setOf(SearchSection.LOANS))
        assertTrue(search("laptop zzz").isEmpty())
    }

    @Test fun `a hash tag finds the tag and anything carrying it`() {
        val r = search("#work")
        assertEquals(listOf("work"), r[SearchSection.TAGS]!!.map { it.id })
        assertEquals("Coffee", r[SearchSection.TEMPLATES]!!.single().title)
        assertFalse(r.containsKey(SearchSection.ACCOUNTS))
    }

    @Test fun `an amount matches records that have one`() {
        val r = search("15.99")
        assertEquals("Netflix", r[SearchSection.PLANNED_PAYMENTS]!!.single().title)
        assertFalse(r.containsKey(SearchSection.ACCOUNTS))
    }

    @Test fun `words plus an amount need both`() {
        assertEquals(setOf(SearchSection.GOALS), search("laptop 1200").keys)
        assertTrue(search("laptop 99").isEmpty())
    }

    @Test fun `names starting with the word come first`() {
        val d = SearchData(accounts = listOf(account("My Savings"), account("Savings Plus")))
        val r = GlobalSearchService.searchRecords(parse("savings"), d)
        assertEquals("Savings Plus", r[SearchSection.ACCOUNTS]!!.first().title)
    }

    @Test fun `an empty query finds nothing`() {
        assertTrue(search("").isEmpty())
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

    @Test fun `transaction hits show the payee and where it was spent`() {
        val hit = GlobalSearchService.transactionHits(
            listOf(tx("-5")), mapOf("main bank" to "Main Bank"), mapOf(1L to "Electronics")
        ).single()
        assertEquals("Shop", hit.title)
        assertEquals("Electronics · Main Bank · n", hit.subtitle)
    }
}
