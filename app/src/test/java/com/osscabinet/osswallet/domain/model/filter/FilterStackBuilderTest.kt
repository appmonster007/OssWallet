package com.osscabinet.ossWallet.domain.model.filter

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for FilterStackBuilder and extension functions.
 * Tests ensure that the builder creates correct FilterStack instances for common patterns.
 */
class FilterStackBuilderTest {

    @Test
    fun `FilterStackBuilder builds correct FilterStack`() {
        val filterStack = FilterStackBuilder()
            .accountIds(listOf("acc1", "acc2"))
            .categoryIds(listOf(1L, 2L))
            .isIncome(true)
            .build()

        assertEquals(3, filterStack.activeFilters.size)
        assertEquals(LogicalOperator.AND, filterStack.logicalOperator)
    }

    @Test
    fun `FilterStackBuilder withOperator works`() {
        val filterStack = FilterStackBuilder()
            .accountIds(listOf("acc1"))
            .withOperator(LogicalOperator.OR)
            .build()

        assertEquals(LogicalOperator.OR, filterStack.logicalOperator)
    }

    @Test
    fun `forCurrency creates correct FilterStack`() {
        val filterStack = FilterStack.forCurrency("USD")
        assertEquals(1, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters[0] is UnifiedFilter.CurrencyFilter)
        val currencyFilter = filterStack.activeFilters[0] as UnifiedFilter.CurrencyFilter
        assertEquals(listOf("USD"), currencyFilter.currencies)
    }

    @Test
    fun `forIncome creates correct FilterStack`() {
        val filterStack = FilterStack.forIncome()
        assertEquals(1, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters[0] is UnifiedFilter.IncomeTypeFilter)
        val incomeFilter = filterStack.activeFilters[0] as UnifiedFilter.IncomeTypeFilter
        assertEquals(true, incomeFilter.isIncome)
    }

    @Test
    fun `forExpense creates correct FilterStack`() {
        val filterStack = FilterStack.forExpense()
        assertEquals(1, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters[0] is UnifiedFilter.IncomeTypeFilter)
        val incomeFilter = filterStack.activeFilters[0] as UnifiedFilter.IncomeTypeFilter
        assertEquals(false, incomeFilter.isIncome)
    }

    @Test
    fun `forAccount creates correct FilterStack`() {
        val filterStack = FilterStack.forAccount("acc123")
        assertEquals(1, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters[0] is UnifiedFilter.AccountFilter)
        val accountFilter = filterStack.activeFilters[0] as UnifiedFilter.AccountFilter
        assertEquals(listOf("acc123"), accountFilter.accountIds)
    }

    @Test
    fun `forAccountAndType creates correct FilterStack`() {
        val filterStack = FilterStack.forAccountAndType("acc123", true)
        assertEquals(2, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters.any { it is UnifiedFilter.AccountFilter })
        assertTrue(filterStack.activeFilters.any { it is UnifiedFilter.IncomeTypeFilter })
    }

    @Test
    fun `forAccountAndCurrency creates correct FilterStack`() {
        val filterStack = FilterStack.forAccountAndCurrency("acc123", "USD")
        assertEquals(2, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters.any { it is UnifiedFilter.AccountFilter })
        assertTrue(filterStack.activeFilters.any { it is UnifiedFilter.CurrencyFilter })
    }

    @Test
    fun `forCategory creates correct FilterStack`() {
        val filterStack = FilterStack.forCategory(123L)
        assertEquals(1, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters[0] is UnifiedFilter.CategoryFilter)
        val categoryFilter = filterStack.activeFilters[0] as UnifiedFilter.CategoryFilter
        assertEquals(listOf(123L), categoryFilter.categoryIds)
    }

    @Test
    fun `forCategories creates correct FilterStack`() {
        val filterStack = FilterStack.forCategories(listOf(1L, 2L, 3L))
        assertEquals(1, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters[0] is UnifiedFilter.CategoryFilter)
        val categoryFilter = filterStack.activeFilters[0] as UnifiedFilter.CategoryFilter
        assertEquals(listOf(1L, 2L, 3L), categoryFilter.categoryIds)
    }

    @Test
    fun `forSearch creates correct FilterStack`() {
        val filterStack = FilterStack.forSearch("grocery")
        assertEquals(1, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters[0] is UnifiedFilter.TextFilter)
        val textFilter = filterStack.activeFilters[0] as UnifiedFilter.TextFilter
        assertEquals("grocery", textFilter.query)
    }

    @Test
    fun `forDateRange creates correct FilterStack`() {
        val timelineRange = TimelineRange.CustomRange(1000L, 2000L)
        val filterStack = FilterStack.forDateRange(timelineRange)
        assertEquals(1, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters[0] is UnifiedFilter.DateRangeFilter)
        val dateFilter = filterStack.activeFilters[0] as UnifiedFilter.DateRangeFilter
        assertEquals(timelineRange, dateFilter.timelineRange)
    }

    @Test
    fun `forAmountRange creates correct FilterStack`() {
        val filterStack = FilterStack.forAmountRange(
            min = BigDecimal("100.00"),
            max = BigDecimal("1000.00")
        )
        assertEquals(1, filterStack.activeFilters.size)
        assertTrue(filterStack.activeFilters[0] is UnifiedFilter.AmountFilter)
        val amountFilter = filterStack.activeFilters[0] as UnifiedFilter.AmountFilter
        assertEquals(BigDecimal("100.00"), amountFilter.min)
        assertEquals(BigDecimal("1000.00"), amountFilter.max)
    }

    @Test
    fun `Builder fluent API works correctly`() {
        val filterStack = FilterStackBuilder()
            .accountIds(listOf("acc1"))
            .categoryIds(listOf(1L))
            .isIncome(true)
            .currencies(listOf("USD"))
            .search("test")
            .withOperator(LogicalOperator.AND)
            .build()

        assertEquals(5, filterStack.activeFilters.size)
        assertEquals(LogicalOperator.AND, filterStack.logicalOperator)
    }

    @Test
    fun `Builder with empty list creates empty FilterStack`() {
        val filterStack = FilterStackBuilder().build()
        assertTrue(filterStack.activeFilters.isEmpty())
        assertEquals(LogicalOperator.AND, filterStack.logicalOperator)
    }
}
