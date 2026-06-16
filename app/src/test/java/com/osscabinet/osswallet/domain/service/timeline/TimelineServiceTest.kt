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
            exchangeRates = emptyMap(),
            plannedPayments = planned
        )

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(1, result[0].transactions.size)
        // Header title should be "Today" (since now is today)
        assertEquals("Today", result[0].headerData.title)
    }

    @Test
    fun `getHeaderData handles empty groupTransactions safely`() {
        val now = System.currentTimeMillis()
        val groupKey = TimelineService.formatDate(now) // e.g. "2024-05-20"
        
        val headerData = TimelineService.getHeaderData(
            groupKey = groupKey,
            groupTransactions = emptyList(),
            absoluteHistory = emptyList(),
            groupingType = TransactionGroupingType.BY_DAY,
            baseCurrency = "USD",
            exchangeRates = emptyMap()
        )

        assertNotNull(headerData)
        assertEquals("Today", headerData.title)
        assertEquals(BigDecimal.ZERO.setScale(2), headerData.incomeBase)
        assertEquals(BigDecimal.ZERO.setScale(2), headerData.expenseBase)
    }
}
