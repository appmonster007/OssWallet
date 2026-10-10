package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PlannedPaymentsSummaryTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L
    private val usd = ExchangeRateSnapshot(anchor = "USD", rates = emptyMap())

    private fun payment(
        id: Long,
        amount: String,
        mode: EntryMode,
        dueInDays: Double,
        currency: String = "USD",
        status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE,
        type: RecurrenceType = RecurrenceType.ONE_TIME
    ): PlannedPayment {
        val due = now + (dueInDays * day).toLong()
        return PlannedPayment(
            id = id,
            name = "p$id",
            categoryId = 1,
            amount = BigDecimal(amount),
            entryMode = mode,
            currency = currency,
            schedule = PaymentSchedule(RecurrenceRule(type), startDate = due, nextDueDate = due),
            status = status
        )
    }

    private fun build(
        payments: List<PlannedPayment>,
        days: Int = 7,
        countOverdue: Boolean = false,
        rates: ExchangeRateSnapshot = usd
    ): PlannedPaymentsSummary {
        val overdue = payments.filter { it.schedule.nextDueDate < now }
        return PlannedPaymentsSummary.build(payments, overdue, now, days, countOverdue, "USD", rates)
    }

    private fun BigDecimal.s() = setScale(2).toPlainString()

    @Test
    fun `sums expenses and income due inside the window and leaves out later ones`() {
        val summary = build(
            listOf(
                payment(1, "100", EntryMode.EXPENSE, 2.0),
                payment(2, "40", EntryMode.EXPENSE, 6.5),
                payment(3, "500", EntryMode.INCOME, 3.0),
                payment(4, "999", EntryMode.EXPENSE, 8.0)
            )
        )

        assertEquals("-140.00", summary.outflow.s())
        assertEquals("500.00", summary.inflow.s())
        assertEquals("360.00", summary.net.s())
    }

    @Test
    fun `a longer window picks up more payments`() {
        val payments = listOf(
            payment(1, "100", EntryMode.EXPENSE, 2.0),
            payment(2, "300", EntryMode.EXPENSE, 20.0)
        )

        assertEquals("-100.00", build(payments, days = 7).outflow.s())
        assertEquals("-400.00", build(payments, days = 30).outflow.s())
    }

    @Test
    fun `recurring payments count every occurrence in the window`() {
        val summary = build(listOf(payment(1, "10", EntryMode.EXPENSE, 1.0, type = RecurrenceType.DAILY)))

        // Due on days 1 to 7 of a 7 day window.
        assertEquals("-70.00", summary.outflow.s())
    }

    @Test
    fun `transfers and inactive payments are ignored`() {
        val summary = build(
            listOf(
                payment(1, "200", EntryMode.TRANSFER, 1.0),
                payment(2, "50", EntryMode.EXPENSE, 1.0, status = PlannedPaymentStatus.PAUSED)
            )
        )

        assertEquals("0.00", summary.outflow.s())
        assertEquals("0.00", summary.inflow.s())
    }

    @Test
    fun `overdue expenses are shown separately unless counted`() {
        val payments = listOf(
            payment(1, "60", EntryMode.EXPENSE, 2.0),
            payment(2, "1850", EntryMode.EXPENSE, -3.0)
        )

        val separate = build(payments, countOverdue = false)
        assertEquals("-60.00", separate.outflow.s())
        assertEquals("-1850.00", separate.overdueOutflow.s())

        val counted = build(payments, countOverdue = true)
        assertEquals("-1910.00", counted.outflow.s())
    }

    @Test
    fun `amounts in other currencies are converted to the card currency`() {
        val rates = ExchangeRateSnapshot(anchor = "USD", rates = mapOf("EUR" to BigDecimal("0.5")))
        val summary = build(listOf(payment(1, "50", EntryMode.EXPENSE, 1.0, currency = "EUR")), rates = rates)

        // 50 EUR at 0.5 EUR per USD is 100 USD.
        assertEquals("-100.00", summary.outflow.s())
    }
}
