package com.osscabinet.osswallet.domain.service.search

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.search.FilterSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class FilterSummaryTest {
    @Test fun `an empty filter is just its period`() {
        assertEquals("All Time", FilterSummary.describe(TransactionFilter()))
    }

    @Test fun `each constraint is named once`() {
        val f = TransactionFilter(
            timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
            transactionTypes = setOf(TransactionType.EXPENSE),
            accountIds = setOf("a", "b"),
            categoryIds = setOf(1L),
            tags = setOf("holiday", "family"),
            queryNote = "taxi",
            minAmount = BigDecimal("100.00")
        )
        assertEquals("This Month · Expense · 2 accounts · 1 category · #family + #holiday · note “taxi” · over 100", FilterSummary.describe(f))
    }

    @Test fun `a range and any-of tags read naturally`() {
        val f = TransactionFilter(tags = setOf("b", "a"), matchAllTags = false, minAmount = BigDecimal("20"), maxAmount = BigDecimal("50"))
        assertEquals("All Time · #a or #b · 20–50", FilterSummary.describe(f))
    }
}
