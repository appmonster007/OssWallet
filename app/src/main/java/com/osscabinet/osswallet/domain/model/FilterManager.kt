package com.osscabinet.ossWallet.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

/**
 * Lightweight filter manager that maintains multiple independent filters
 * and automatically merges them into a single TransactionFilter.
 * 
 * Uses AND logic for merging - all active filters are combined.
 * Adding/removing filters automatically recomputes the merged filter.
 * 
 * This replaces the complex FilterStack system while maintaining
 * the ability to have multiple filters applied simultaneously.
 */
class FilterManager {
    private val activeFilters = mutableMapOf<String, TransactionFilter>()
    
    private val _mergedFilter = MutableStateFlow(mergeFilters(emptyList()))
    
    /**
     * The merged filter combining all active filters with AND logic.
     * This is automatically recomputed whenever filters are added or removed.
     */
    val mergedFilter: StateFlow<TransactionFilter> = _mergedFilter
    
    /**
     * Add or update a filter with a unique key.
     * If a filter with the same key already exists, it will be replaced.
     * 
     * @param key Unique identifier for this filter (e.g., "account", "category", "timeline")
     * @param filter The filter to add/update
     */
    fun setFilter(key: String, filter: TransactionFilter) {
        activeFilters[key] = filter
        _mergedFilter.value = mergeFilters(activeFilters.values.toList())
    }
    
    /**
     * Remove a filter by key.
     * 
     * @param key The unique identifier of the filter to remove
     */
    fun removeFilter(key: String) {
        activeFilters.remove(key)
        _mergedFilter.value = mergeFilters(activeFilters.values.toList())
    }
    
    /**
     * Check if a filter exists.
     * 
     * @param key The unique identifier of the filter to check
     * @return true if the filter exists, false otherwise
     */
    fun hasFilter(key: String): Boolean = activeFilters.containsKey(key)
    
    /**
     * Get a specific filter.
     * 
     * @param key The unique identifier of the filter to get
     * @return The filter if it exists, null otherwise
     */
    fun getFilter(key: String): TransactionFilter? = activeFilters[key]
    
    /**
     * Clear all filters.
     */
    fun clear() {
        activeFilters.clear()
        _mergedFilter.value = mergeFilters(emptyList())
    }
    
    /**
     * Get all active filter keys.
     * 
     * @return Set of all active filter keys
     */
    fun getFilterKeys(): Set<String> = activeFilters.keys
    
    /**
     * Get the number of active filters.
     * 
     * @return Number of active filters
     */
    fun getFilterCount(): Int = activeFilters.size
    
    /**
     * Merge multiple TransactionFilters using AND logic.
     * 
     * Merge rules:
     * - Lists (categoryIds, accountIds, etc.) are unioned (OR within the field)
     * - Single values (transactionType, hasWarranty) use last non-null value
     * - Ranges (startDate/endDate, minAmount/maxAmount) use INTERSECTION (max start, min end)
     * - Boolean flags (isNoteRegex, isPayeeRegex) use OR logic
     * - Text fields (queryNote, queryPayee) are joined with space
     * 
     * @param filters List of TransactionFilters to merge
     * @return Merged TransactionFilter
     */
    private fun mergeFilters(filters: List<TransactionFilter>): TransactionFilter {
        if (filters.isEmpty()) return TransactionFilter()
        
        return TransactionFilter(
            timelineRange = filters.lastOrNull { it.timelineRange != com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime }?.timelineRange
                ?: com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime,
            queryNote = filters.mapNotNull { it.queryNote }.filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() },
            isNoteRegex = filters.any { it.isNoteRegex },
            queryPayee = filters.mapNotNull { it.queryPayee }.filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() },
            isPayeeRegex = filters.any { it.isPayeeRegex },
            categoryIds = filters.flatMap { it.categoryIds }.distinct(),
            accountIds = filters.flatMap { it.accountIds }.distinct(),
            paymentMethods = filters.flatMap { it.paymentMethods }.distinct(),
            hasWarranty = filters.mapNotNull { it.hasWarranty }.lastOrNull(),
            warrantyExpirationStart = filters.mapNotNull { it.warrantyExpirationStart }.maxOrNull(),
            warrantyExpirationEnd = filters.mapNotNull { it.warrantyExpirationEnd }.minOrNull(),
            minAmount = filters.mapNotNull { it.minAmount }.maxOrNull(),
            maxAmount = filters.mapNotNull { it.maxAmount }.minOrNull(),
            transactionType = filters.mapNotNull { it.transactionType }.lastOrNull(),
            currencies = filters.flatMap { it.currencies }.distinct(),
            statuses = filters.flatMap { it.statuses }.distinct()
        )
    }
}
