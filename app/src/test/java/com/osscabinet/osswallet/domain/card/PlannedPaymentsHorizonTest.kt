package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.card.computers.PlannedPaymentsCardComputer
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PlannedPaymentsHorizonTest {

    private val day = 86_400_000L

    private fun input(filter: TransactionFilter, payments: List<PlannedPayment>) = CardInput(
        accounts = emptyList(),
        categories = emptyList(),
        exchangeRates = ExchangeRateSnapshot(anchor = "USD", rates = mapOf("USD" to BigDecimal.ONE)),
        targetCurrency = "USD",
        filter = filter,
        // Analytics with the common filter on: the screen's timeline overrides the card's own.
        screenContext = ScreenContext.ANALYTICS,
        commonFilterEnabled = true,
        plannedPayments = payments
    )

    private fun payment(now: Long) = PlannedPayment(
        id = 1L,
        name = "Rent",
        categoryId = 1L,
        amount = BigDecimal("100"),
        entryMode = EntryMode.EXPENSE,
        currency = "USD",
        schedule = PaymentSchedule(
            recurrenceRule = RecurrenceRule(type = RecurrenceType.ONE_TIME),
            startDate = now - 30 * day,
            nextDueDate = now + 5 * day
        )
    )

    private fun upcomingCount(range: TimelineRange): Int = runBlocking {
        val now = System.currentTimeMillis()
        val config = PlannedPaymentsCardConfig(cardId = "pp")
        PlannedPaymentsCardComputer()
            .compute(input(TransactionFilter(timelineRange = range), listOf(payment(now))), config)
            .items.size
    }

    @Test
    fun `upcoming payments are listed even when the range is capped at now`() {
        val now = System.currentTimeMillis()
        val range = TimelineRange.CustomRange(
            start = now - 10 * day,
            end = now + 30 * day,
            excludeFuture = true
        )
        assertEquals(1, upcomingCount(range))
    }

    @Test
    fun `upcoming payments outside the selected period are still left out`() {
        val now = System.currentTimeMillis()
        val range = TimelineRange.CustomRange(
            start = now - 10 * day,
            end = now + 2 * day,
            excludeFuture = true
        )
        assertEquals(0, upcomingCount(range))
    }
}
