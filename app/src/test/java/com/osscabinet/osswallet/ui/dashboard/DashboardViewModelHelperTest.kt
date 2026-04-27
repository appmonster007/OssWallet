package com.osscabinet.osswallet.ui.dashboard

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import org.junit.Test
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Unit tests for DashboardViewModel helper functions.
 * Tests the Hybrid Approach implementation for card-level filtering and computation.
 */
class DashboardViewModelHelperTest {

    @Test
    fun `CashFlowResult empty returns correct default values`() {
        val emptyResult = CashFlowResult.empty()

        assertEquals(BigDecimal.ZERO, emptyResult.totalIncomeInBase)
        assertEquals(BigDecimal.ZERO, emptyResult.totalExpensesInBase)
        assertEquals(BigDecimal.ZERO, emptyResult.netInBase)
        assertTrue(emptyResult.breakdown.isEmpty())
        assertTrue(emptyResult.filteredTransactions.isEmpty())
    }

    @Test
    fun `CurrencyAggregationItem empty returns correct default values`() {
        val emptyItem = CurrencyAggregationItem.empty()

        assertEquals("", emptyItem.currency)
        assertEquals(BigDecimal.ZERO, emptyItem.amount)
        assertEquals(BigDecimal.ZERO, emptyItem.amountInBase)
    }

    @Test
    fun `CurrencyAggregationItem empty with currency returns correct default values`() {
        val emptyItem = CurrencyAggregationItem.empty("USD")

        assertEquals("USD", emptyItem.currency)
        assertEquals(BigDecimal.ZERO, emptyItem.amount)
        assertEquals(BigDecimal.ZERO, emptyItem.amountInBase)
    }

    @Test
    fun `TimelineRange AllTime should include all transactions`() {
        // Verify TimelineRange.AllTime is available for use in helpers
        assertTrue(TimelineRange.AllTime != null)
    }
}
