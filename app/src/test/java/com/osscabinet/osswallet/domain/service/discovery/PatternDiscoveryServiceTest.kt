package com.osscabinet.osswallet.domain.service.discovery

import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.discovery.PatternDiscoveryService
import com.osscabinet.ossWallet.domain.service.discovery.RecurringSuggestionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class PatternDiscoveryServiceTest {

    private val zone = ZoneId.of("UTC")
    private fun ms(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private var nextId = 1L

    private fun tx(
        date: Long,
        payee: String = "Netflix",
        amount: String = "15.99",
        category: Long = 10,
        type: TransactionType = TransactionType.EXPENSE,
        currency: String = "USD",
        planned: Long? = null
    ) = Transaction(
        id = nextId++, name = payee, amount = BigDecimal(amount), timestamp = date, currency = currency,
        transactionType = type, accountId = "acc", categoryId = category, payee = payee, sourcePlannedPaymentId = planned
    )

    private fun monthly(payee: String, day: Int, months: List<Pair<Int, Int>>, amount: String = "15.99") =
        months.map { (y, m) -> tx(ms(y, m, day), payee, amount) }

    private val now = ms(2026, 10, 20)
    private fun discover(txs: List<Transaction>) = PatternDiscoveryService.discoverPatterns(txs, now, zone)

    @Test fun `a bill on the same day every month is a monthly pattern with that day`() {
        val p = discover(monthly("Netflix", 5, listOf(2026 to 6, 2026 to 7, 2026 to 8, 2026 to 9, 2026 to 10))).single()
        assertEquals(RecurrenceType.MONTHLY, p.recurrenceRule!!.type)
        assertEquals(5, p.recurrenceRule!!.dayOfMonth)
        assertEquals(5, p.occurrences)
        assertEquals(ms(2026, 11, 5), p.nextExpectedDate)
        assertTrue(p.confidence > 0.8f)
    }

    @Test fun `weekly spending on the same weekday is weekly`() {
        val start = ms(2026, 9, 1) // a Tuesday
        val txs = (0..5).map { tx(start + it * 7L * 86_400_000L, "Yoga Studio", "20.00") }
        val p = discover(txs).single()
        assertEquals(RecurrenceType.WEEKLY, p.recurrenceRule!!.type)
    }

    @Test fun `every three months is a quarterly pattern`() {
        val txs = listOf(ms(2025, 10, 3), ms(2026, 1, 3), ms(2026, 4, 3), ms(2026, 7, 3)).map { tx(it, "Water Board", "60.00") }
        val p = discover(txs).single()
        assertEquals(RecurrenceType.EVERY_N_MONTHS, p.recurrenceRule!!.type)
        assertEquals(3, p.recurrenceRule!!.interval)
        assertEquals(ms(2026, 10, 3), p.nextExpectedDate)
    }

    @Test fun `every six months is detected`() {
        val txs = listOf(ms(2025, 4, 10), ms(2025, 10, 10), ms(2026, 4, 10)).map { tx(it, "Insurance", "300.00") }
        val p = discover(txs).single()
        assertEquals(6, p.recurrenceRule!!.interval)
    }

    @Test fun `two payments a year apart make an annual subscription`() {
        val txs = listOf(ms(2025, 10, 20), ms(2026, 10, 19)).map { tx(it, "Domain Registrar", "12.00") }
        val p = discover(txs + emptyList()).single()
        assertEquals(RecurrenceType.YEARLY, p.recurrenceRule!!.type)
        assertEquals(2, p.occurrences)
    }

    @Test fun `two payments a month apart are not enough`() {
        assertTrue(discover(listOf(tx(ms(2026, 8, 5)), tx(ms(2026, 9, 5)))).isEmpty())
    }

    @Test fun `a pattern that stopped months ago is not suggested`() {
        val stopped = monthly("Old Gym", 5, listOf(2025, 2025, 2025, 2025).zip(listOf(1, 2, 3, 4)))
        assertTrue(discover(stopped).isEmpty())
    }

    @Test fun `a payment slightly late is still suggested`() {
        // Last on Sep 5, next expected Oct 5, today Oct 20: late by 15 days, within the grace period.
        val p = discover(monthly("Netflix", 5, listOf(2026 to 7, 2026 to 8, 2026 to 9)))
        assertEquals(1, p.size)
    }

    @Test fun `irregular spending at one shop is not a pattern`() {
        val dates = listOf(ms(2026, 1, 3), ms(2026, 1, 9), ms(2026, 3, 28), ms(2026, 4, 2), ms(2026, 7, 19), ms(2026, 9, 30))
        assertTrue(discover(dates.map { tx(it, "Corner Shop", "8.40") }).isEmpty())
    }

    @Test fun `a utility bill that varies is flagged as varying with a recent average`() {
        val amounts = listOf("80.00", "95.00", "120.00", "70.00", "90.00")
        val months = listOf(6, 7, 8, 9, 10)
        val txs = months.zip(amounts).map { (m, a) -> tx(ms(2026, m, 12), "Power Co", a) }
        val p = discover(txs).single()
        assertTrue(p.amountVaries)
        assertEquals(BigDecimal("93.33"), p.averageAmount)
    }

    @Test fun `a steady amount is not flagged as varying and uses the usual amount`() {
        val p = discover(monthly("Netflix", 5, listOf(2026 to 6, 2026 to 7, 2026 to 8, 2026 to 9))).single()
        assertFalse(p.amountVaries)
        assertEquals(BigDecimal("15.99"), p.averageAmount)
    }

    @Test fun `card number noise in the payee does not split a pattern`() {
        val txs = listOf(
            tx(ms(2026, 7, 5), "NETFLIX*12345678"), tx(ms(2026, 8, 5), "Netflix  "), tx(ms(2026, 9, 5), "netflix#99999")
        )
        assertEquals(3, discover(txs).single().occurrences)
    }

    @Test fun `transfers and already planned transactions are ignored`() {
        val transfers = monthly("Savings", 1, listOf(2026 to 7, 2026 to 8, 2026 to 9)).map { it.copy(transactionType = TransactionType.TRANSFER_OUTFLOW) }
        val generated = listOf(7, 8, 9).map { tx(ms(2026, it, 5), "Rent", "900.00", planned = 4) }
        assertTrue(discover(transfers + generated).isEmpty())
    }

    @Test fun `future dated transactions are not history`() {
        val past = monthly("Netflix", 5, listOf(2026 to 8, 2026 to 9, 2026 to 10))
        val future = monthly("Netflix", 5, listOf(2026 to 11, 2026 to 12, 2027 to 1))
        val p = discover(past + future).single()
        assertEquals(3, p.occurrences)
        assertEquals(ms(2026, 11, 5), p.nextExpectedDate)
        assertTrue(discover(future).isEmpty())
    }

    @Test fun `different currencies are different patterns`() {
        val usd = monthly("Spotify", 3, listOf(2026 to 8, 2026 to 9, 2026 to 10))
        val eur = listOf(8, 9, 10).map { tx(ms(2026, it, 3), "Spotify", "9.99", currency = "EUR") }
        assertEquals(2, discover(usd + eur).size)
    }

    @Test fun `the key survives a price change`() {
        val before = discover(monthly("Netflix", 5, listOf(2026 to 7, 2026 to 8, 2026 to 9)))
        val after = discover(monthly("Netflix", 5, listOf(2026 to 7, 2026 to 8, 2026 to 9), amount = "17.99"))
        assertEquals(before.single().key, after.single().key)
    }

    @Test fun `the planned payment built from a pattern starts at the next expected date`() {
        val p = discover(monthly("Netflix", 5, listOf(2026 to 7, 2026 to 8, 2026 to 9, 2026 to 10))).single()
        val planned = PatternDiscoveryService.toPlannedPayment(p)!!
        assertEquals(ms(2026, 11, 5), planned.schedule.nextDueDate)
    }

    // ── RecurringSuggestionService ───────────────────────────────────────────

    private val netflix = monthly("Netflix", 5, listOf(2026 to 7, 2026 to 8, 2026 to 9, 2026 to 10))

    private fun plan(name: String, payee: String?, category: Long = 10, status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE) =
        PlannedPayment(
            name = name, categoryId = category, amount = BigDecimal("15.99"), entryMode = EntryMode.EXPENSE,
            payee = payee, currency = "USD", status = status,
            schedule = PaymentSchedule(RecurrenceRule(RecurrenceType.MONTHLY), ms(2026, 1, 1), nextDueDate = ms(2026, 11, 5))
        )

    private fun template(name: String, payee: String?, category: Long = 10) = Template(
        name = name, categoryId = category, amount = BigDecimal("15.99"), entryMode = EntryMode.EXPENSE,
        accountId = "acc", payee = payee, currency = "USD"
    )

    private fun suggest(
        plans: List<PlannedPayment> = emptyList(),
        templates: List<Template> = emptyList(),
        dismissed: Set<String> = emptySet()
    ) = RecurringSuggestionService.suggest(netflix, plans, templates, dismissed, now, zone)

    @Test fun `an unscheduled pattern is suggested`() {
        assertEquals(1, suggest().size)
    }

    @Test fun `a pattern that already has a planned payment is no longer suggested`() {
        assertTrue(suggest(plans = listOf(plan("Netflix", "Netflix"))).isEmpty())
    }

    @Test fun `a planned payment matches by name when it has no payee`() {
        assertTrue(suggest(plans = listOf(plan("netflix", null))).isEmpty())
    }

    @Test fun `a paused planned payment still counts`() {
        assertTrue(suggest(plans = listOf(plan("Netflix", "Netflix", status = PlannedPaymentStatus.PAUSED))).isEmpty())
    }

    @Test fun `a completed planned payment does not hide the suggestion`() {
        assertEquals(1, suggest(plans = listOf(plan("Netflix", "Netflix", status = PlannedPaymentStatus.COMPLETED))).size)
    }

    @Test fun `a planned payment in another category is a different thing`() {
        assertEquals(1, suggest(plans = listOf(plan("Netflix", "Netflix", category = 99))).size)
    }

    @Test fun `a different price does not stop it matching an existing plan`() {
        val dearer = plan("Netflix", "Netflix").copy(amount = BigDecimal("22.00"))
        assertTrue(suggest(plans = listOf(dearer)).isEmpty())
    }

    @Test fun `an existing template is flagged but the planned payment suggestion stays`() {
        val s = suggest(templates = listOf(template("Netflix", "Netflix"))).single()
        assertTrue(s.existsAsTemplate)
        assertFalse(s.existsAsPlannedPayment)
    }

    @Test fun `a dismissed pattern stays hidden, even after the amount changes`() {
        val key = suggest().single().key
        assertTrue(suggest(dismissed = setOf(key)).isEmpty())
        val repriced = RecurringSuggestionService.suggest(
            monthly("Netflix", 5, listOf(2026 to 7, 2026 to 8, 2026 to 9, 2026 to 10), amount = "19.99"),
            emptyList(), emptyList(), setOf(key), now, zone
        )
        assertTrue(repriced.isEmpty())
    }

    @Test fun `suggestions come back strongest first`() {
        val weak = listOf(ms(2026, 5, 3), ms(2026, 6, 20), ms(2026, 8, 6), ms(2026, 9, 25)).map { tx(it, "Maybe Co") }
        val result = RecurringSuggestionService.suggest(netflix + weak, emptyList(), emptyList(), emptySet(), now, zone)
        assertTrue(result.isNotEmpty())
        assertEquals(result.sortedByDescending { it.confidence }, result)
        assertNotNull(result.firstOrNull { it.name == "Netflix" })
        assertNull(result.firstOrNull { it.name == "Maybe Co" && it.confidence > result.first().confidence })
    }
}
