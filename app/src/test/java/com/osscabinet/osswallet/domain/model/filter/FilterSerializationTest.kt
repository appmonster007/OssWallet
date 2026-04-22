package com.osscabinet.ossWallet.domain.model.filter

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for FilterStack, UnifiedFilter, and LogicalOperator serialization.
 * Tests ensure that filters can be serialized and deserialized correctly for navigation.
 */
class FilterSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `LogicalOperator serialization works`() {
        val and = LogicalOperator.AND
        val serialized = json.encodeToString(and)
        val deserialized = json.decodeFromString<LogicalOperator>(serialized)
        assertEquals(and, deserialized)
    }

    @Test
    fun `FilterOperator serialization works`() {
        val operator = FilterOperator.CONTAINS
        val serialized = json.encodeToString(operator)
        val deserialized = json.decodeFromString<FilterOperator>(serialized)
        assertEquals(operator, deserialized)
    }

    @Test
    fun `TextFilter serialization works`() {
        val filter = UnifiedFilter.TextFilter(
            query = "test",
            isRegex = false,
            isCaseInsensitive = true,
            operator = FilterOperator.CONTAINS
        )
        val serialized = json.encodeToString(filter)
        val deserialized = json.decodeFromString<UnifiedFilter.TextFilter>(serialized)
        assertEquals(filter.query, deserialized.query)
        assertEquals(filter.isRegex, deserialized.isRegex)
        assertEquals(filter.isCaseInsensitive, deserialized.isCaseInsensitive)
    }

    @Test
    fun `CategoryFilter serialization works`() {
        val filter = UnifiedFilter.CategoryFilter(
            categoryIds = listOf(1L, 2L, 3L),
            operator = FilterOperator.IN
        )
        val serialized = json.encodeToString(filter)
        val deserialized = json.decodeFromString<UnifiedFilter.CategoryFilter>(serialized)
        assertEquals(filter.categoryIds, deserialized.categoryIds)
        assertEquals(filter.operator, deserialized.operator)
    }

    @Test
    fun `AccountFilter serialization works`() {
        val filter = UnifiedFilter.AccountFilter(
            accountIds = listOf("acc1", "acc2"),
            operator = FilterOperator.IN
        )
        val serialized = json.encodeToString(filter)
        val deserialized = json.decodeFromString<UnifiedFilter.AccountFilter>(serialized)
        assertEquals(filter.accountIds, deserialized.accountIds)
        assertEquals(filter.operator, deserialized.operator)
    }

    @Test
    fun `CurrencyFilter serialization works`() {
        val filter = UnifiedFilter.CurrencyFilter(
            currencies = listOf("USD", "EUR"),
            operator = FilterOperator.IN
        )
        val serialized = json.encodeToString(filter)
        val deserialized = json.decodeFromString<UnifiedFilter.CurrencyFilter>(serialized)
        assertEquals(filter.currencies, deserialized.currencies)
        assertEquals(filter.operator, deserialized.operator)
    }

    @Test
    fun `IncomeTypeFilter serialization works`() {
        val filter = UnifiedFilter.IncomeTypeFilter(isIncome = true)
        val serialized = json.encodeToString(filter)
        val deserialized = json.decodeFromString<UnifiedFilter.IncomeTypeFilter>(serialized)
        assertEquals(filter.isIncome, deserialized.isIncome)
    }

    @Test
    fun `AmountFilter serialization works`() {
        val filter = UnifiedFilter.AmountFilter(
            min = BigDecimal("100.50"),
            max = BigDecimal("1000.00")
        )
        val serialized = json.encodeToString(filter)
        val deserialized = json.decodeFromString<UnifiedFilter.AmountFilter>(serialized)
        assertEquals(filter.min, deserialized.min)
        assertEquals(filter.max, deserialized.max)
    }

    @Test
    fun `CompositeFilter serialization works`() {
        val filter = UnifiedFilter.CompositeFilter(
            filters = listOf(
                UnifiedFilter.CategoryFilter(listOf(1L)),
                UnifiedFilter.IncomeTypeFilter(true)
            ),
            operator = LogicalOperator.AND
        )
        val serialized = json.encodeToString(filter)
        val deserialized = json.decodeFromString<UnifiedFilter.CompositeFilter>(serialized)
        assertEquals(filter.filters.size, deserialized.filters.size)
        assertEquals(filter.operator, deserialized.operator)
    }

    @Test
    fun `InstantFilter serialization works`() {
        val innerFilter = UnifiedFilter.CategoryFilter(listOf(1L))
        val filter = UnifiedFilter.InstantFilter(innerFilter)
        val serialized = json.encodeToString(filter)
        val deserialized = json.decodeFromString<UnifiedFilter.InstantFilter>(serialized)
        assertTrue(deserialized.filter is UnifiedFilter.CategoryFilter)
    }

    @Test
    fun `FilterStack serialization works`() {
        val filterStack = FilterStack(
            activeFilters = listOf(
                UnifiedFilter.CategoryFilter(listOf(1L, 2L)),
                UnifiedFilter.IncomeTypeFilter(true)
            ),
            logicalOperator = LogicalOperator.AND
        )
        val serialized = json.encodeToString(filterStack)
        val deserialized = json.decodeFromString<FilterStack>(serialized)
        assertEquals(filterStack.activeFilters.size, deserialized.activeFilters.size)
        assertEquals(filterStack.logicalOperator, deserialized.logicalOperator)
    }

    @Test
    fun `FilterStack Empty serialization works`() {
        val filterStack = FilterStack.Empty
        val serialized = json.encodeToString(filterStack)
        val deserialized = json.decodeFromString<FilterStack>(serialized)
        assertTrue(deserialized.activeFilters.isEmpty())
        assertEquals(LogicalOperator.AND, deserialized.logicalOperator)
    }

    @Test
    fun `Complex FilterStack serialization works`() {
        val filterStack = FilterStack(
            activeFilters = listOf(
                UnifiedFilter.CompositeFilter(
                    filters = listOf(
                        UnifiedFilter.CategoryFilter(listOf(1L)),
                        UnifiedFilter.CurrencyFilter(listOf("USD"))
                    ),
                    operator = LogicalOperator.OR
                ),
                UnifiedFilter.IncomeTypeFilter(true)
            ),
            logicalOperator = LogicalOperator.AND
        )
        val serialized = json.encodeToString(filterStack)
        val deserialized = json.decodeFromString<FilterStack>(serialized)
        assertEquals(filterStack.activeFilters.size, deserialized.activeFilters.size)
        assertEquals(filterStack.logicalOperator, deserialized.logicalOperator)
    }
}
