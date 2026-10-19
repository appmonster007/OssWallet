package com.osscabinet.ossWallet.domain.service.timeline

import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

class TimelineServiceTest {

    @Test
    fun `getGroupedData handles empty transactions with planned payments without crashing`() {
        val now = System.currentTimeMillis()
        val planned = listOf(
            UpcomingPaymentItem(
                plannedPayment = PlannedPayment(
                    id = 1L,
                    name = "Test Planned",
                    categoryId = 1L,
                    amount = BigDecimal("10.00"),
                    entryMode = EntryMode.EXPENSE,
                    currency = "USD",
                    schedule = PaymentSchedule(
                        recurrenceRule = RecurrenceRule(RecurrenceType.DAILY),
                        startDate = now,
                        nextDueDate = now
                    ),
                    status = PlannedPaymentStatus.ACTIVE,
                    transactionStatus = TransactionStatus.CLEARED
                ),
                scheduledDate = now,
                isOverdue = false
            )
        )

        val result = TimelineService.getGroupedData(
            transactions = emptyList(),
            absoluteHistory = emptyList(),
            groupingType = TransactionGroupingType.BY_DAY,
            accounts = emptyList(),
            categories = emptyList(),
            baseCurrency = "USD",
            exchangeRates = ExchangeRateSnapshot.EMPTY,
            plannedPayments = planned
        )

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(1, result[0].transactions.size)
        // Header title should be "Today" (since now is today)
        assertEquals("Today", result[0].headerData.title)
    }

    @Test
    fun `group with no confirmed transactions reports a Today header with zero income and expense`() {
        val now = System.currentTimeMillis()
        // A group containing only a planned payment (no confirmed transactions) must
        // still produce a valid header with zero income/expense, not crash.
        val planned = listOf(
            UpcomingPaymentItem(
                plannedPayment = PlannedPayment(
                    id = 1L,
                    name = "Test Planned",
                    categoryId = 1L,
                    amount = BigDecimal("10.00"),
                    entryMode = EntryMode.EXPENSE,
                    currency = "USD",
                    schedule = PaymentSchedule(
                        recurrenceRule = RecurrenceRule(RecurrenceType.DAILY),
                        startDate = now,
                        nextDueDate = now
                    ),
                    status = PlannedPaymentStatus.ACTIVE,
                    transactionStatus = TransactionStatus.CLEARED
                ),
                scheduledDate = now,
                isOverdue = false
            )
        )

        val result = TimelineService.getGroupedData(
            transactions = emptyList(),
            absoluteHistory = emptyList(),
            groupingType = TransactionGroupingType.BY_DAY,
            accounts = emptyList(),
            categories = emptyList(),
            baseCurrency = "USD",
            exchangeRates = ExchangeRateSnapshot.EMPTY,
            plannedPayments = planned
        )

        assertEquals(1, result.size)
        val headerData = result[0].headerData
        assertEquals("Today", headerData.title)
        assertEquals(0, headerData.incomeBase.compareTo(BigDecimal.ZERO))
        assertEquals(0, headerData.expenseBase.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun `getRangeForThis BIWEEKLY returns 14-day window`() {
        val now = System.currentTimeMillis()
        val (start, end) = TimelineService.getRangeForThis(
            com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType.BIWEEKLY,
            referenceTime = now
        )
        assertTrue(start <= now)
        assertTrue(end >= now)
        assertTrue(end - start >= 13 * 86400000L)
    }

    @Test
    fun `getDateRange for TimelineRange Periodic calculates correct period bounds and shifts`() {
        val startMs = 1_700_000_000_000L
        val rule = RecurrenceRule(RecurrenceType.EVERY_N_WEEKS, interval = 2)
        val range = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.Periodic(
            recurrenceRule = rule,
            startDate = startMs,
            offset = 0
        )

        val (start, end) = TimelineService.getDateRange(range, referenceTime = startMs + 86400000L * 3)
        assertNotNull(start)
        assertNotNull(end)
        assertTrue(start!! <= startMs + 86400000L * 3)
        assertTrue(end!! >= startMs + 86400000L * 3)

        val shifted = TimelineService.shiftRange(range, 1) as com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.Periodic
        assertEquals(1, shifted.offset)
    }
}
