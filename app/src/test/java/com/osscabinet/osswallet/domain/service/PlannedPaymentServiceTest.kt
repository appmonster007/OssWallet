package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

class PlannedPaymentServiceTest {

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun makePayment(
        recurrenceType: RecurrenceType,
        interval: Int = 1,
        dayOfMonth: Int? = null,
        dayOfWeek: Int? = null,
        nextDueDate: Long = epochOf(2024, 1, 15),
        startDate: Long = epochOf(2024, 1, 1),
        endDate: Long? = null,
        status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE
    ) = PlannedPayment(
        id = 1L,
        title = "Test",
        amount = BigDecimal("100"),
        currency = "USD",
        transactionType = "expense",
        accountId = "acc1",
        categoryId = 1001L,
        recurrenceRule = RecurrenceRule(
            type = recurrenceType,
            interval = interval,
            dayOfMonth = dayOfMonth,
            dayOfWeek = dayOfWeek
        ),
        startDate = startDate,
        nextDueDate = nextDueDate,
        endDate = endDate,
        status = status
    )

    @Test
    fun `calculateNextDueDate returns null for ONE_TIME`() {
        val payment = makePayment(RecurrenceType.ONE_TIME)
        val result = PlannedPaymentService.calculateNextDueDate(payment, epochOf(2024, 1, 15))
        assertNull(result)
    }

    @Test
    fun `calculateNextDueDate adds 1 day for DAILY`() {
        val payment = makePayment(RecurrenceType.DAILY)
        val from = epochOf(2024, 1, 15)
        val next = PlannedPaymentService.calculateNextDueDate(payment, from)
        assertNotNull(next)
        assertEquals(epochOf(2024, 1, 16), next)
    }

    @Test
    fun `calculateNextDueDate adds 7 days for WEEKLY`() {
        val payment = makePayment(RecurrenceType.WEEKLY)
        val from = epochOf(2024, 1, 15)
        val next = PlannedPaymentService.calculateNextDueDate(payment, from)
        assertNotNull(next)
        assertEquals(epochOf(2024, 1, 22), next)
    }

    @Test
    fun `calculateNextDueDate adds 1 month for MONTHLY`() {
        val payment = makePayment(RecurrenceType.MONTHLY)
        val from = epochOf(2024, 1, 15)
        val next = PlannedPaymentService.calculateNextDueDate(payment, from)
        assertNotNull(next)
        assertEquals(epochOf(2024, 2, 15), next)
    }

    @Test
    fun `calculateNextDueDate adds 1 year for YEARLY`() {
        val payment = makePayment(RecurrenceType.YEARLY)
        val from = epochOf(2024, 1, 15)
        val next = PlannedPaymentService.calculateNextDueDate(payment, from)
        assertNotNull(next)
        assertEquals(epochOf(2025, 1, 15), next)
    }

    @Test
    fun `calculateNextDueDate adds N days for EVERY_N_DAYS`() {
        val payment = makePayment(RecurrenceType.EVERY_N_DAYS, interval = 5)
        val from = epochOf(2024, 1, 15)
        val next = PlannedPaymentService.calculateNextDueDate(payment, from)
        assertNotNull(next)
        assertEquals(epochOf(2024, 1, 20), next)
    }

    @Test
    fun `calculateNextDueDate returns null when next exceeds endDate`() {
        val payment = makePayment(
            RecurrenceType.MONTHLY,
            endDate = epochOf(2024, 1, 31)
        )
        val from = epochOf(2024, 1, 15)
        val next = PlannedPaymentService.calculateNextDueDate(payment, from)
        assertNull(next)
    }

    @Test
    fun `generateUpcoming returns empty for ONE_TIME payment already triggered`() {
        val payment = makePayment(
            RecurrenceType.ONE_TIME,
            nextDueDate = epochOf(2023, 12, 1),
            status = PlannedPaymentStatus.ACTIVE
        )
        val now = epochOf(2024, 1, 1)
        val result = PlannedPaymentService.generateUpcoming(listOf(payment), now, 30)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `generateUpcoming returns occurrences within 30-day window`() {
        val now = epochOf(2024, 1, 1)
        val payment = makePayment(
            RecurrenceType.WEEKLY,
            nextDueDate = epochOf(2024, 1, 5)
        )
        val result = PlannedPaymentService.generateUpcoming(listOf(payment), now, 30)
        assertTrue(result.isNotEmpty())
        result.forEach { item ->
            assertTrue(item.scheduledDate <= now + 30L * 24 * 60 * 60 * 1000)
        }
    }

    @Test
    fun `generateUpcoming marks past items as overdue`() {
        val now = epochOf(2024, 1, 15)
        val payment = makePayment(
            RecurrenceType.DAILY,
            nextDueDate = epochOf(2024, 1, 10)
        )
        val result = PlannedPaymentService.generateUpcoming(listOf(payment), now, 30)
        val overdue = result.filter { it.isOverdue }
        assertTrue(overdue.isNotEmpty())
    }

    @Test
    fun `getOverdue returns only active payments past due`() {
        val now = epochOf(2024, 2, 1)
        val overdueActive = makePayment(RecurrenceType.MONTHLY, nextDueDate = epochOf(2024, 1, 15))
        val futureActive = makePayment(RecurrenceType.MONTHLY, nextDueDate = epochOf(2024, 3, 1))
        val overduePaused = makePayment(
            RecurrenceType.MONTHLY,
            nextDueDate = epochOf(2024, 1, 1),
            status = PlannedPaymentStatus.PAUSED
        )
        val result = PlannedPaymentService.getOverdue(listOf(overdueActive, futureActive, overduePaused), now)
        assertEquals(1, result.size)
        assertEquals(overdueActive, result[0])
    }

    @Test
    fun `shouldComplete returns true for ONE_TIME`() {
        val payment = makePayment(RecurrenceType.ONE_TIME)
        assertTrue(PlannedPaymentService.shouldComplete(payment))
    }

    @Test
    fun `shouldComplete returns false for MONTHLY`() {
        val payment = makePayment(RecurrenceType.MONTHLY)
        assertTrue(!PlannedPaymentService.shouldComplete(payment))
    }
}
