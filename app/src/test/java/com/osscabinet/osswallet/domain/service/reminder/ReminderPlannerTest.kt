package com.osscabinet.osswallet.domain.service.reminder

import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.settings.ReminderConfig
import com.osscabinet.ossWallet.domain.service.reminder.BudgetAlertInput
import com.osscabinet.ossWallet.domain.service.reminder.ReminderEvent
import com.osscabinet.ossWallet.domain.service.reminder.ReminderKind
import com.osscabinet.ossWallet.domain.service.reminder.ReminderPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ReminderPlannerTest {

    private val zone = ZoneOffset.UTC
    private val day = 24L * 60 * 60 * 1000
    private val noon = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
    private val config = ReminderConfig(leadDays = 2, hourOfDay = 9)

    private fun payment(id: Long, due: Long, status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE) =
        PlannedPayment(
            id = id, name = "Rent", categoryId = 1, amount = BigDecimal("18000.00"),
            entryMode = EntryMode.EXPENSE, currency = "INR", status = status,
            schedule = PaymentSchedule(
                recurrenceRule = RecurrenceRule(RecurrenceType.MONTHLY),
                startDate = noon - 30 * day,
                nextDueDate = due
            )
        )

    private fun loan(id: Long, settle: Long?, status: LoanStatus = LoanStatus.ACTIVE) = Loan(
        id = id, name = "Bike", principalAmount = BigDecimal("500"), currency = "INR",
        startDate = noon - 100 * day, lenderOrBorrowerName = "Sam", loanType = LoanType.BORROWED,
        linkedAccountId = "a", expectedSettlementDate = settle, status = status, profileId = 1
    )

    private fun alert(level: BudgetAlertLevel, id: Long = 1, period: Long = 1000L) =
        BudgetAlertInput(id, "Food", 85, level, period)

    private fun plan(
        payments: List<PlannedPayment> = emptyList(),
        loans: List<Loan> = emptyList(),
        alerts: List<BudgetAlertInput> = emptyList(),
        sent: Set<String> = emptySet(),
        cfg: ReminderConfig = config,
        now: Long = noon
    ) = ReminderPlanner.plan(now, cfg, payments, loans, alerts, sent, zone)

    @Test fun `payment due within lead window is reminded`() {
        val events = plan(payments = listOf(payment(1, noon + 2 * day)))
        assertEquals(1, events.size)
        assertEquals(ReminderKind.PLANNED_PAYMENT, events[0].kind)
        assertTrue(events[0].title.contains("in 2 days"))
        assertEquals("18000 INR", events[0].body)
    }

    @Test fun `payment beyond lead window or already past is skipped`() {
        assertTrue(plan(payments = listOf(payment(1, noon + 3 * day))).isEmpty())
        assertTrue(plan(payments = listOf(payment(2, noon - day))).isEmpty())
    }

    @Test fun `payment due later today still reminds`() {
        val earlierToday = noon - 3 * 60 * 60 * 1000
        assertEquals(1, plan(payments = listOf(payment(1, earlierToday))).size)
    }

    @Test fun `paused payments are ignored`() {
        assertTrue(plan(payments = listOf(payment(1, noon + day, PlannedPaymentStatus.PAUSED))).isEmpty())
    }

    @Test fun `nothing before the configured hour`() {
        val sixAm = ZonedDateTime.of(2026, 10, 15, 6, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertTrue(plan(payments = listOf(payment(1, sixAm + day)), now = sixAm).isEmpty())
    }

    @Test fun `already sent keys are not repeated and new due dates re-arm`() {
        val due = noon + day
        val first = plan(payments = listOf(payment(1, due)))
        assertTrue(plan(payments = listOf(payment(1, due)), sent = first.map { it.key }.toSet()).isEmpty())
        // Next month's occurrence has a different nextDueDate, so it is a different event.
        assertEquals(1, plan(payments = listOf(payment(1, noon + 2 * day)), sent = first.map { it.key }.toSet()).size)
    }

    @Test fun `loan settling soon is reminded and settled loans are not`() {
        assertEquals(1, plan(loans = listOf(loan(1, noon + day))).size)
        assertTrue(plan(loans = listOf(loan(2, noon + day, LoanStatus.SETTLED))).isEmpty())
        assertTrue(plan(loans = listOf(loan(3, null))).isEmpty())
    }

    @Test fun `budget warning and exceeded map to distinct once-per-period keys`() {
        val warn = plan(alerts = listOf(alert(BudgetAlertLevel.WARNING)))
        val over = plan(alerts = listOf(alert(BudgetAlertLevel.EXCEEDED)))
        assertEquals(ReminderKind.BUDGET_WARNING, warn.single().kind)
        assertEquals(ReminderKind.BUDGET_EXCEEDED, over.single().kind)
        assertTrue(warn.single().key != over.single().key)
        // Crossing into EXCEEDED after a warning was sent must still notify.
        assertEquals(1, plan(alerts = listOf(alert(BudgetAlertLevel.EXCEEDED)), sent = setOf(warn.single().key)).size)
        // A new period re-arms the same level.
        assertEquals(1, plan(alerts = listOf(alert(BudgetAlertLevel.WARNING, period = 2000L)), sent = setOf(warn.single().key)).size)
        assertTrue(plan(alerts = listOf(alert(BudgetAlertLevel.NONE))).isEmpty())
    }

    @Test fun `category toggles and master switch are respected`() {
        val p = listOf(payment(1, noon + day))
        val l = listOf(loan(1, noon + day))
        val a = listOf(alert(BudgetAlertLevel.WARNING))
        assertEquals(3, plan(p, l, a).size)
        assertEquals(2, plan(p, l, a, cfg = config.copy(plannedPayments = false)).size)
        assertEquals(2, plan(p, l, a, cfg = config.copy(loans = false)).size)
        assertEquals(2, plan(p, l, a, cfg = config.copy(budgetAlerts = false)).size)
        assertTrue(plan(p, l, a, cfg = config.copy(enabled = false)).isEmpty())
    }

    @Test fun `a planned payment reminder carries the due date it is for`() {
        val due = noon + day
        assertEquals(due, plan(payments = listOf(payment(1, due))).single().dueDate)
        assertEquals(null, plan(loans = listOf(loan(1, noon + day))).single().dueDate)
    }

    @Test fun `an event survives the round trip through intent extras and work data`() {
        val event = plan(payments = listOf(payment(7, noon + day))).single()
        assertEquals(event, ReminderEvent.fromMap(event.toMap()))
        val loanEvent = plan(loans = listOf(loan(3, noon + day))).single()
        assertEquals(loanEvent, ReminderEvent.fromMap(loanEvent.toMap()))
    }

    @Test fun `a damaged event map is rejected rather than half-read`() {
        assertEquals(null, ReminderEvent.fromMap(emptyMap()))
        assertEquals(null, ReminderEvent.fromMap(mapOf("key" to "k", "kind" to "NOPE", "targetId" to "1")))
        assertEquals(null, ReminderEvent.fromMap(mapOf("key" to "k", "kind" to "LOAN", "targetId" to "x")))
    }
}
