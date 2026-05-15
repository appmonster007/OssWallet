package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.*
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

class BudgetServiceTest {

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.systemDefault()).toInstant().toEpochMilli()

    private fun makeBudget(
        timelineRange: TimelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
        limit: BigDecimal = BigDecimal("1000"),
        categoryIds: List<Long> = emptyList(),
        accountIds: List<String> = emptyList(),
        alertThreshold: Float = 0.8f,
        rollover: Boolean = false,
        currency: String = "USD"
    ) = Budget(
        id = 1L,
        name = "Test Budget",
        currency = currency,
        limitAmount = limit,
        timelineRange = timelineRange,
        categoryIds = categoryIds,
        accountIds = accountIds,
        alertThreshold = alertThreshold,
        rollover = rollover,
        trackTypes = listOf(TransactionType.EXPENSE),
        isArchived = false,
        profileId = 1
    )

    private fun makeTransaction(
        amount: BigDecimal,
        transactionType: TransactionType = TransactionType.EXPENSE,
        currency: String = "USD",
        accountId: String = "acc1",
        categoryId: Long = 1001L,
        timestamp: Long = System.currentTimeMillis()
    ) = Transaction(
        id = 0L,
        name = "Test",
        amount = amount,
        timestamp = timestamp,
        currency = currency,
        transactionType = transactionType,
        accountId = accountId,
        categoryId = categoryId,
        status = TransactionStatus.CLEARED
    )

    @Test
    fun `getCurrentPeriodRange WEEKLY returns correct start and end`() {
        val budget = makeBudget(TimelineRange.ThisPeriod(ThisPeriodType.WEEK))
        val now = epochOf(2024, 3, 13)
        val (start, end) = BudgetService.getCurrentPeriodRange(budget, now)
        assertTrue("Start should be before or equal to now", start <= now)
        assertTrue("End should be after or equal to now", end >= now)
    }

    @Test
    fun `getCurrentPeriodRange MONTHLY covers full month`() {
        val budget = makeBudget(TimelineRange.ThisPeriod(ThisPeriodType.MONTH))
        val now = epochOf(2024, 3, 15)
        val (start, end) = BudgetService.getCurrentPeriodRange(budget, now)
        assertTrue("Start should be on or before March 1", start <= epochOf(2024, 3, 1))
        assertTrue("End should be on or after March 31", end >= epochOf(2024, 3, 31))
    }

    @Test
    fun `getCurrentPeriodRange QUARTERLY covers full quarter`() {
        val budget = makeBudget(TimelineRange.ThisPeriod(ThisPeriodType.QUARTER))
        val now = epochOf(2024, 2, 15)
        val (start, end) = BudgetService.getCurrentPeriodRange(budget, now)
        assertTrue("Start should be on or before Jan 1", start <= epochOf(2024, 1, 1))
        assertTrue("End should be on or after March 31", end >= epochOf(2024, 3, 31))
    }

    @Test
    fun `getCurrentPeriodRange YEARLY covers full year`() {
        val budget = makeBudget(TimelineRange.ThisPeriod(ThisPeriodType.YEAR))
        val now = epochOf(2024, 6, 15)
        val (start, end) = BudgetService.getCurrentPeriodRange(budget, now)
        assertTrue("Start should be on or before Jan 1", start <= epochOf(2024, 1, 1))
        assertTrue("End should be on or after Dec 31", end >= epochOf(2024, 12, 31))
    }

    @Test
    fun `getCurrentPeriodRange CUSTOM uses budget custom dates`() {
        val customStart = epochOf(2024, 3, 1)
        val customEnd = epochOf(2024, 5, 31)
        val budget = makeBudget(TimelineRange.CustomRange(customStart, customEnd))
        val now = epochOf(2024, 4, 15)
        val (start, end) = BudgetService.getCurrentPeriodRange(budget, now)
        assertEquals(customStart, start)
        assertEquals(customEnd, end)
    }

    @Test
    fun `buildProgress NONE when below threshold`() {
        val budget = makeBudget(limit = BigDecimal("1000"), alertThreshold = 0.8f)
        val progress = BudgetService.buildProgress(budget, BigDecimal("500"))
        assertEquals(BudgetAlertLevel.NONE, progress.alertLevel)
        assertEquals(0, BigDecimal("500").compareTo(progress.remaining))
    }

    @Test
    fun `buildProgress WARNING when at threshold`() {
        val budget = makeBudget(limit = BigDecimal("1000"), alertThreshold = 0.8f)
        val progress = BudgetService.buildProgress(budget, BigDecimal("900"))
        assertEquals(BudgetAlertLevel.WARNING, progress.alertLevel)
    }

    @Test
    fun `buildProgress EXCEEDED when over limit`() {
        val budget = makeBudget(limit = BigDecimal("1000"), alertThreshold = 0.8f)
        val progress = BudgetService.buildProgress(budget, BigDecimal("1100"))
        assertEquals(BudgetAlertLevel.EXCEEDED, progress.alertLevel)
        assertEquals(0, BigDecimal.ZERO.compareTo(progress.remaining))
    }

    @Test
    fun `buildProgress percentage correct`() {
        val budget = makeBudget(limit = BigDecimal("1000"))
        val progress = BudgetService.buildProgress(budget, BigDecimal("600"))
        assertEquals(0.6f, progress.percentage, 0.001f)
    }

    @Test
    fun `calculateSpent sums matching transactions in period`() {
        val budget = makeBudget(TimelineRange.ThisPeriod(ThisPeriodType.MONTH))
        val now = epochOf(2024, 3, 15)
        val inPeriod = epochOf(2024, 3, 10)
        val outOfPeriod = epochOf(2024, 2, 10)

        val transactions = listOf(
            makeTransaction(BigDecimal("-200"), timestamp = inPeriod),
            makeTransaction(BigDecimal("-300"), timestamp = inPeriod),
            makeTransaction(BigDecimal("-100"), timestamp = outOfPeriod)
        )

        val spent = BudgetService.calculateSpent(budget, transactions, emptyMap(), "USD", 2, now)
        assertEquals(0, BigDecimal("500").compareTo(spent))
    }

    @Test
    fun `calculateSpent ignores income transactions`() {
        val budget = makeBudget(TimelineRange.ThisPeriod(ThisPeriodType.MONTH))
        val now = epochOf(2024, 3, 15)
        val inPeriod = epochOf(2024, 3, 10)

        val transactions = listOf(
            makeTransaction(BigDecimal("-200"), transactionType = TransactionType.EXPENSE, timestamp = inPeriod),
            makeTransaction(BigDecimal("500"), transactionType = TransactionType.INCOME, timestamp = inPeriod)
        )

        val spent = BudgetService.calculateSpent(budget, transactions, emptyMap(), "USD", 2, now)
        assertEquals(0, BigDecimal("200").compareTo(spent))
    }

    @Test
    fun `calculateSpent filters by categoryId when set`() {
        val budget = makeBudget(TimelineRange.ThisPeriod(ThisPeriodType.MONTH), categoryIds = listOf(1L))
        val now = epochOf(2024, 3, 15)
        val inPeriod = epochOf(2024, 3, 10)

        val transactions = listOf(
            makeTransaction(BigDecimal("-200"), categoryId = 1L, timestamp = inPeriod),
            makeTransaction(BigDecimal("-300"), categoryId = 2L, timestamp = inPeriod)
        )

        val spent = BudgetService.calculateSpent(budget, transactions, emptyMap(), "USD", 2, now)
        assertEquals(0, BigDecimal("200").compareTo(spent))
    }
}
