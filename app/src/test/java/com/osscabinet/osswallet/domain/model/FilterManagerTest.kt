package com.osscabinet.ossWallet.domain.model

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FilterManagerTest {
    
    private lateinit var filterManager: FilterManager
    
    @Before
    fun setup() {
        filterManager = FilterManager()
    }
    
    @Test
    fun `initial state should have empty merged filter`() {
        val merged = filterManager.mergedFilter
        assertTrue(merged.categoryIds.isEmpty())
        assertTrue(merged.accountIds.isEmpty())
        assertNull(merged.startDate)
        assertNull(merged.endDate)
        assertNull(merged.transactionType)
    }
    
    @Test
    fun `setFilter should add filter and update merged filter`() {
        val accountFilter = TransactionFilter(accountIds = listOf("acc1"))
        filterManager.setFilter("account", accountFilter)
        
        assertEquals(1, filterManager.getFilterCount())
        assertTrue(filterManager.hasFilter("account"))
        assertEquals(listOf("acc1"), filterManager.mergedFilter.accountIds)
    }
    
    @Test
    fun `setFilter should replace existing filter with same key`() {
        filterManager.setFilter("account", TransactionFilter(accountIds = listOf("acc1")))
        filterManager.setFilter("account", TransactionFilter(accountIds = listOf("acc2")))
        
        assertEquals(1, filterManager.getFilterCount())
        assertEquals(listOf("acc2"), filterManager.mergedFilter.accountIds)
    }
    
    @Test
    fun `removeFilter should remove filter and update merged filter`() {
        filterManager.setFilter("account", TransactionFilter(accountIds = listOf("acc1")))
        filterManager.removeFilter("account")
        
        assertEquals(0, filterManager.getFilterCount())
        assertFalse(filterManager.hasFilter("account"))
        assertTrue(filterManager.mergedFilter.accountIds.isEmpty())
    }
    
    @Test
    fun `clear should remove all filters`() {
        filterManager.setFilter("account", TransactionFilter(accountIds = listOf("acc1")))
        filterManager.setFilter("category", TransactionFilter(categoryIds = listOf(1L)))
        filterManager.clear()
        
        assertEquals(0, filterManager.getFilterCount())
        assertTrue(filterManager.mergedFilter.accountIds.isEmpty())
        assertTrue(filterManager.mergedFilter.categoryIds.isEmpty())
    }
    
    @Test
    fun `mergeFilters should union categoryIds from multiple filters`() {
        filterManager.setFilter("cat1", TransactionFilter(categoryIds = listOf(1L, 2L)))
        filterManager.setFilter("cat2", TransactionFilter(categoryIds = listOf(2L, 3L)))
        
        val merged = filterManager.mergedFilter
        assertEquals(listOf(1L, 2L, 3L), merged.categoryIds)
    }
    
    @Test
    fun `mergeFilters should union accountIds from multiple filters`() {
        filterManager.setFilter("acc1", TransactionFilter(accountIds = listOf("acc1", "acc2")))
        filterManager.setFilter("acc2", TransactionFilter(accountIds = listOf("acc2", "acc3")))
        
        val merged = filterManager.mergedFilter
        assertEquals(listOf("acc1", "acc2", "acc3"), merged.accountIds)
    }
    
    @Test
    fun `mergeFilters should use min start date`() {
        filterManager.setFilter("date1", TransactionFilter(startDate = 1000L, endDate = 5000L))
        filterManager.setFilter("date2", TransactionFilter(startDate = 2000L, endDate = 4000L))
        
        val merged = filterManager.mergedFilter
        assertEquals(1000L, merged.startDate)
        assertEquals(5000L, merged.endDate)
    }
    
    @Test
    fun `mergeFilters should use max end date`() {
        filterManager.setFilter("date1", TransactionFilter(startDate = 1000L, endDate = 3000L))
        filterManager.setFilter("date2", TransactionFilter(startDate = 2000L, endDate = 5000L))
        
        val merged = filterManager.mergedFilter
        assertEquals(1000L, merged.startDate)
        assertEquals(5000L, merged.endDate)
    }
    
    @Test
    fun `mergeFilters should use min amount`() {
        filterManager.setFilter("amount1", TransactionFilter(minAmount = BigDecimal("10"), maxAmount = BigDecimal("100")))
        filterManager.setFilter("amount2", TransactionFilter(minAmount = BigDecimal("5"), maxAmount = BigDecimal("50")))
        
        val merged = filterManager.mergedFilter
        assertEquals(BigDecimal("5"), merged.minAmount)
        assertEquals(BigDecimal("100"), merged.maxAmount)
    }
    
    @Test
    fun `mergeFilters should use max amount`() {
        filterManager.setFilter("amount1", TransactionFilter(minAmount = BigDecimal("10"), maxAmount = BigDecimal("50")))
        filterManager.setFilter("amount2", TransactionFilter(minAmount = BigDecimal("20"), maxAmount = BigDecimal("100")))
        
        val merged = filterManager.mergedFilter
        assertEquals(BigDecimal("10"), merged.minAmount)
        assertEquals(BigDecimal("100"), merged.maxAmount)
    }
    
    @Test
    fun `mergeFilters should use last non-null transactionType`() {
        filterManager.setFilter("type1", TransactionFilter(transactionType = "income"))
        filterManager.setFilter("type2", TransactionFilter(transactionType = "expense"))
        
        val merged = filterManager.mergedFilter
        assertEquals("expense", merged.transactionType)
    }
    
    @Test
    fun `mergeFilters should use last non-null hasWarranty`() {
        filterManager.setFilter("warranty1", TransactionFilter(hasWarranty = true))
        filterManager.setFilter("warranty2", TransactionFilter(hasWarranty = false))
        
        val merged = filterManager.mergedFilter
        assertEquals(false, merged.hasWarranty)
    }
    
    @Test
    fun `mergeFilters should union currencies from multiple filters`() {
        filterManager.setFilter("curr1", TransactionFilter(currencies = listOf("USD", "EUR")))
        filterManager.setFilter("curr2", TransactionFilter(currencies = listOf("EUR", "GBP")))
        
        val merged = filterManager.mergedFilter
        assertEquals(listOf("USD", "EUR", "GBP"), merged.currencies)
    }
    
    @Test
    fun `mergeFilters should union paymentMethods from multiple filters`() {
        filterManager.setFilter("pay1", TransactionFilter(paymentMethods = listOf("Cash", "Card")))
        filterManager.setFilter("pay2", TransactionFilter(paymentMethods = listOf("Card", "Transfer")))
        
        val merged = filterManager.mergedFilter
        assertEquals(listOf("Cash", "Card", "Transfer"), merged.paymentMethods)
    }
    
    @Test
    fun `mergeFilters should join queryNote with space`() {
        filterManager.setFilter("note1", TransactionFilter(queryNote = "coffee"))
        filterManager.setFilter("note2", TransactionFilter(queryNote = "lunch"))
        
        val merged = filterManager.mergedFilter
        assertEquals("coffee lunch", merged.queryNote)
    }
    
    @Test
    fun `mergeFilters should join queryPayee with space`() {
        filterManager.setFilter("payee1", TransactionFilter(queryPayee = "Starbucks"))
        filterManager.setFilter("payee2", TransactionFilter(queryPayee = "Subway"))
        
        val merged = filterManager.mergedFilter
        assertEquals("Starbucks Subway", merged.queryPayee)
    }
    
    @Test
    fun `mergeFilters should use OR logic for isNoteRegex`() {
        filterManager.setFilter("note1", TransactionFilter(queryNote = "test", isNoteRegex = false))
        filterManager.setFilter("note2", TransactionFilter(queryNote = "test", isNoteRegex = true))
        
        val merged = filterManager.mergedFilter
        assertTrue(merged.isNoteRegex)
    }
    
    @Test
    fun `mergeFilters should use OR logic for isPayeeRegex`() {
        filterManager.setFilter("payee1", TransactionFilter(queryPayee = "test", isPayeeRegex = false))
        filterManager.setFilter("payee2", TransactionFilter(queryPayee = "test", isPayeeRegex = true))
        
        val merged = filterManager.mergedFilter
        assertTrue(merged.isPayeeRegex)
    }
    
    @Test
    fun `mergeFilters should handle empty text fields`() {
        filterManager.setFilter("note1", TransactionFilter(queryNote = ""))
        filterManager.setFilter("note2", TransactionFilter(queryNote = "   "))
        
        val merged = filterManager.mergedFilter
        assertNull(merged.queryNote)
    }
    
    @Test
    fun `getFilter should return null for non-existent key`() {
        assertNull(filterManager.getFilter("nonexistent"))
    }
    
    @Test
    fun `getFilter should return filter for existing key`() {
        val filter = TransactionFilter(accountIds = listOf("acc1"))
        filterManager.setFilter("account", filter)
        
        assertEquals(filter, filterManager.getFilter("account"))
    }
    
    @Test
    fun `getFilterKeys should return all active keys`() {
        filterManager.setFilter("account", TransactionFilter())
        filterManager.setFilter("category", TransactionFilter())
        
        val keys = filterManager.getFilterKeys()
        assertEquals(setOf("account", "category"), keys)
    }
    
    @Test
    fun `complex merge with multiple filter types`() {
        filterManager.setFilter("account", TransactionFilter(accountIds = listOf("acc1", "acc2")))
        filterManager.setFilter("category", TransactionFilter(categoryIds = listOf(1L, 2L)))
        filterManager.setFilter("date", TransactionFilter(startDate = 1000L, endDate = 5000L))
        filterManager.setFilter("type", TransactionFilter(transactionType = "income"))
        
        val merged = filterManager.mergedFilter
        assertEquals(listOf("acc1", "acc2"), merged.accountIds)
        assertEquals(listOf(1L, 2L), merged.categoryIds)
        assertEquals(1000L, merged.startDate)
        assertEquals(5000L, merged.endDate)
        assertEquals("income", merged.transactionType)
    }
    
    @Test
    fun `removing one filter should recompute merged filter`() {
        filterManager.setFilter("account", TransactionFilter(accountIds = listOf("acc1")))
        filterManager.setFilter("category", TransactionFilter(categoryIds = listOf(1L)))
        
        assertEquals(listOf("acc1"), filterManager.mergedFilter.accountIds)
        assertEquals(listOf(1L), filterManager.mergedFilter.categoryIds)
        
        filterManager.removeFilter("account")
        
        assertTrue(filterManager.mergedFilter.accountIds.isEmpty())
        assertEquals(listOf(1L), filterManager.mergedFilter.categoryIds)
    }
}
