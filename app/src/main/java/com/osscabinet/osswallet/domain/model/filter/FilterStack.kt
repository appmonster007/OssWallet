package com.osscabinet.ossWallet.domain.model.filter

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.service.TimelineService
import java.math.BigDecimal

/**
 * A stack of filters that can be combined with logical operators.
 * This enables stacking multiple filters in sequence and creating complex filter combinations.
 *
 * Redesigned for intuitive filter combination:
 * - OR logic: Union of all matching criteria (e.g., category A OR category B)
 * - AND logic: Intersection of all matching criteria (e.g., category A AND date range)
 * - Conflicting values: When combining with OR, conflicting values (e.g., true vs false) result in "all" (null)
 * - Date/Amount/Warranty ranges: Use min/max for simplified range combination
 */
data class FilterStack(
    val activeFilters: List<UnifiedFilter>,
    val logicalOperator: LogicalOperator = LogicalOperator.AND
) {
    /**
     * Adds a new filter to the stack.
     */
    fun addFilter(filter: UnifiedFilter): FilterStack =
        copy(activeFilters = activeFilters + filter)
    
    /**
     * Adds a new instant/transient filter to the stack.
     * Instant filters are not persisted and only exist until screen changes.
     */
    fun addInstantFilter(filter: UnifiedFilter): FilterStack =
        copy(activeFilters = activeFilters + UnifiedFilter.InstantFilter(filter))
    
    /**
     * Removes a filter from the stack.
     */
    fun removeFilter(filter: UnifiedFilter): FilterStack =
        copy(activeFilters = activeFilters - filter)
    
    /**
     * Removes all filters of a specific type from the stack.
     */
    fun removeFiltersOfType(filterClass: Class<out UnifiedFilter>): FilterStack =
        copy(activeFilters = activeFilters.filterNot { it::class == filterClass })
    
    /**
     * Clears all instant/transient filters from the stack.
     * Keeps permanent (non-instant) filters intact.
     */
    fun clearInstantFilters(): FilterStack {
        val permanentFilters = activeFilters.filterNot { it is UnifiedFilter.InstantFilter }
        return copy(activeFilters = permanentFilters)
    }
    
    /**
     * Clears all filters from the stack.
     */
    fun clear(): FilterStack = FilterStack(emptyList())
    
    /**
     * Returns only the permanent (non-instant) filters.
     */
    fun getPermanentFilters(): List<UnifiedFilter> =
        activeFilters.filterNot { it is UnifiedFilter.InstantFilter }
    
    /**
     * Returns only the instant/transient filters.
     */
    fun getInstantFilters(): List<UnifiedFilter> =
        activeFilters.filter { it is UnifiedFilter.InstantFilter }
    
    /**
     * Checks if the stack has any instant/transient filters.
     */
    fun hasInstantFilters(): Boolean =
        activeFilters.any { it is UnifiedFilter.InstantFilter }
    
    /**
     * Unwraps InstantFilter wrappers to get the actual filters.
     * This is useful when converting to TransactionFilter or for query building.
     */
    private fun unwrapInstantFilters(filters: List<UnifiedFilter>): List<UnifiedFilter> {
        return filters.map { filter ->
            when (filter) {
                is UnifiedFilter.InstantFilter -> filter.filter
                is UnifiedFilter.CompositeFilter -> {
                    // Recursively unwrap composite filters
                    UnifiedFilter.CompositeFilter(
                        filters = unwrapInstantFilters(filter.filters),
                        operator = filter.operator
                    )
                }
                else -> filter
            }
        }
    }
    
    /**
     * Checks if the stack is empty.
     */
    fun isEmpty(): Boolean = activeFilters.isEmpty()
    
    /**
     * Combines this filter stack with another using the specified logical operator.
     */
    fun combineWith(other: FilterStack, operator: LogicalOperator): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.CompositeFilter(this.activeFilters, this.logicalOperator),
                UnifiedFilter.CompositeFilter(other.activeFilters, other.logicalOperator)
            ),
            logicalOperator = operator
        )
    
    /**
     * Converts this FilterStack to a legacy TransactionFilter for backward compatibility.
     * This allows the new system to work with existing code.
     * Instant filters are unwrapped and included in the conversion.
     */
    fun toTransactionFilter(): TransactionFilter {
        var result = TransactionFilter()
        
        // Unwrap instant filters before conversion
        val unwrappedFilters = unwrapInstantFilters(activeFilters)
        
        for (filter in unwrappedFilters) {
            result = when (filter) {
                is UnifiedFilter.TextFilter -> {
                    // TextFilter searches both note and payee fields
                    // Note: isCaseInsensitive will be handled in repository layer SQL query builder
                    result.copy(
                        queryNote = filter.query,
                        isNoteRegex = filter.isRegex,
                        queryPayee = filter.query,
                        isPayeeRegex = filter.isRegex
                    )
                }
                is UnifiedFilter.CategoryFilter -> {
                    result.copy(categoryIds = filter.categoryIds)
                }
                is UnifiedFilter.AccountFilter -> {
                    result.copy(accountIds = filter.accountIds)
                }
                is UnifiedFilter.DateRangeFilter -> {
                    val (start, end) = when (filter.timelineRange) {
                        is TimelineRange.AllTime -> null to null
                        is TimelineRange.ThisPeriod -> {
                            val range = TimelineService.getRangeForThis(filter.timelineRange.period)
                            range.first to range.second
                        }
                        is TimelineRange.LastPeriod -> {
                            val range = TimelineService.getRangeForLast(filter.timelineRange.period)
                            range.first to range.second
                        }
                        is TimelineRange.CustomRange -> {
                            filter.timelineRange.start to filter.timelineRange.end
                        }
                    }
                    result.copy(startDate = start, endDate = end)
                }
                is UnifiedFilter.AmountFilter -> {
                    result.copy(
                        minAmount = filter.min,
                        maxAmount = filter.max
                    )
                }
                is UnifiedFilter.PaymentMethodFilter -> {
                    result.copy(paymentMethods = filter.methods)
                }
                is UnifiedFilter.CurrencyFilter -> {
                    result.copy(currencies = filter.currencies)
                }
                is UnifiedFilter.IncomeTypeFilter -> {
                    result.copy(isIncome = filter.isIncome)
                }
                is UnifiedFilter.WarrantyFilter -> {
                    result.copy(
                        hasWarranty = filter.hasWarranty,
                        warrantyExpirationStart = filter.expirationStart,
                        warrantyExpirationEnd = filter.expirationEnd
                    )
                }
                is UnifiedFilter.CompositeFilter -> {
                    // For composite filters, merge based on logical operator
                    if (filter.operator == LogicalOperator.OR) {
                        // OR logic: union for lists, special handling for conflicting values
                        var orResult = result
                        for (subFilter in filter.filters) {
                            val subResult = FilterStack(listOf(subFilter)).toTransactionFilter()
                            orResult = orResult.copy(
                                // Union for lists
                                categoryIds = (orResult.categoryIds + subResult.categoryIds).distinct(),
                                accountIds = (orResult.accountIds + subResult.accountIds).distinct(),
                                paymentMethods = (orResult.paymentMethods + subResult.paymentMethods).distinct(),
                                currencies = (orResult.currencies + subResult.currencies).distinct(),
                                // For isIncome: if combining income and expense, show all (null)
                                isIncome = if (orResult.isIncome != null && subResult.isIncome != null &&
                                             orResult.isIncome != subResult.isIncome) {
                                    null // Show both income and expense (All)
                                } else {
                                    orResult.isIncome ?: subResult.isIncome
                                },
                                // For date ranges: use min start date and max end date
                                startDate = listOfNotNull(orResult.startDate, subResult.startDate).minOrNull(),
                                endDate = listOfNotNull(orResult.endDate, subResult.endDate).maxOrNull(),
                                // For amount ranges: use min min amount and max max amount
                                minAmount = listOfNotNull(orResult.minAmount, subResult.minAmount).minOrNull(),
                                maxAmount = listOfNotNull(orResult.maxAmount, subResult.maxAmount).maxOrNull(),
                                // For warranty: use min start and max end
                                hasWarranty = if (orResult.hasWarranty != subResult.hasWarranty) null else (orResult.hasWarranty ?: subResult.hasWarranty),
                                warrantyExpirationStart = listOfNotNull(orResult.warrantyExpirationStart, subResult.warrantyExpirationStart).minOrNull(),
                                warrantyExpirationEnd = listOfNotNull(orResult.warrantyExpirationEnd, subResult.warrantyExpirationEnd).maxOrNull(),
                                // For note/payee: OR logic - combine with space
                                queryNote = listOfNotNull(orResult.queryNote, subResult.queryNote).filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() },
                                queryPayee = listOfNotNull(orResult.queryPayee, subResult.queryPayee).filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() },
                                // For regex flags: OR logic
                                isNoteRegex = orResult.isNoteRegex || subResult.isNoteRegex,
                                isPayeeRegex = orResult.isPayeeRegex || subResult.isPayeeRegex
                            )
                        }
                        orResult
                    } else {
                        // AND logic: last one wins (intersection is handled in query layer)
                        var andResult = result
                        for (subFilter in filter.filters) {
                            val subResult = FilterStack(listOf(subFilter)).toTransactionFilter()
                            andResult = andResult.copy(
                                queryNote = subResult.queryNote ?: andResult.queryNote,
                                isNoteRegex = subResult.isNoteRegex || andResult.isNoteRegex,
                                queryPayee = subResult.queryPayee ?: andResult.queryPayee,
                                isPayeeRegex = subResult.isPayeeRegex || andResult.isPayeeRegex,
                                categoryIds = if (subResult.categoryIds.isNotEmpty()) subResult.categoryIds else andResult.categoryIds,
                                accountIds = if (subResult.accountIds.isNotEmpty()) subResult.accountIds else andResult.accountIds,
                                startDate = subResult.startDate ?: andResult.startDate,
                                endDate = subResult.endDate ?: andResult.endDate,
                                paymentMethods = if (subResult.paymentMethods.isNotEmpty()) subResult.paymentMethods else andResult.paymentMethods,
                                hasWarranty = subResult.hasWarranty ?: andResult.hasWarranty,
                                warrantyExpirationStart = subResult.warrantyExpirationStart ?: andResult.warrantyExpirationStart,
                                warrantyExpirationEnd = subResult.warrantyExpirationEnd ?: andResult.warrantyExpirationEnd,
                                minAmount = subResult.minAmount ?: andResult.minAmount,
                                maxAmount = subResult.maxAmount ?: andResult.maxAmount,
                                isIncome = subResult.isIncome ?: andResult.isIncome,
                                currencies = if (subResult.currencies.isNotEmpty()) subResult.currencies else andResult.currencies
                            )
                        }
                        andResult
                    }
                }
                is UnifiedFilter.SavedFilterReference -> {
                    // Cannot convert saved filter reference without loading from database
                    // Return as-is - caller should resolve the reference first
                    result
                }
                is UnifiedFilter.InstantFilter -> {
                    // Should be unwrapped by unwrapInstantFilters, but handle just in case
                    result
                }
            }
        }
        
        return result
    }
    
    companion object {
        /**
         * Empty filter stack with no filters.
         */
        val Empty = FilterStack(emptyList())
        
        /**
         * Creates a FilterStack with a single instant/transient filter.
         * Useful for quick, temporary filtering without saving.
         */
        fun instant(filter: UnifiedFilter): FilterStack =
            FilterStack(listOf(UnifiedFilter.InstantFilter(filter)))
        
        /**
         * Creates a FilterStack with multiple instant/transient filters.
         * Useful for quick, temporary filtering without saving.
         */
        fun instant(filters: List<UnifiedFilter>, operator: LogicalOperator = LogicalOperator.AND): FilterStack =
            FilterStack(filters.map { UnifiedFilter.InstantFilter(it) }, operator)
        
        /**
         * Creates a FilterStack from a legacy TransactionFilter.
         * This allows migration from the old system to the new one.
         */
        fun fromTransactionFilter(filter: TransactionFilter): FilterStack {
            val filters = mutableListOf<UnifiedFilter>()
            
            // Unified text search - if either note or payee query is set, create a TextFilter
            // This searches both fields for a better user experience
            // Default to case-insensitive search for better UX
            if (!filter.queryNote.isNullOrBlank() || !filter.queryPayee.isNullOrBlank()) {
                val query = filter.queryNote ?: filter.queryPayee ?: ""
                val isRegex = filter.isNoteRegex || filter.isPayeeRegex
                filters.add(
                    UnifiedFilter.TextFilter(
                        query = query,
                        isRegex = isRegex,
                        isCaseInsensitive = true // Default to case-insensitive
                    )
                )
            }
            
            if (filter.categoryIds.isNotEmpty()) {
                filters.add(
                    UnifiedFilter.CategoryFilter(
                        categoryIds = filter.categoryIds
                    )
                )
            }
            
            if (filter.accountIds.isNotEmpty()) {
                filters.add(
                    UnifiedFilter.AccountFilter(
                        accountIds = filter.accountIds
                    )
                )
            }
            
            if (filter.startDate != null || filter.endDate != null) {
                val timelineRange = if (filter.startDate != null && filter.endDate != null) {
                    TimelineRange.CustomRange(filter.startDate, filter.endDate)
                } else if (filter.startDate != null) {
                    TimelineRange.CustomRange(filter.startDate, Long.MAX_VALUE)
                } else {
                    TimelineRange.CustomRange(0, filter.endDate!!)
                }
                filters.add(
                    UnifiedFilter.DateRangeFilter(timelineRange)
                )
            }
            
            if (filter.minAmount != null || filter.maxAmount != null) {
                filters.add(
                    UnifiedFilter.AmountFilter(
                        min = filter.minAmount,
                        max = filter.maxAmount
                    )
                )
            }
            
            if (filter.paymentMethods.isNotEmpty()) {
                filters.add(
                    UnifiedFilter.PaymentMethodFilter(
                        methods = filter.paymentMethods
                    )
                )
            }
            
            if (filter.currencies.isNotEmpty()) {
                filters.add(
                    UnifiedFilter.CurrencyFilter(
                        currencies = filter.currencies
                    )
                )
            }
            
            if (filter.isIncome != null) {
                filters.add(
                    UnifiedFilter.IncomeTypeFilter(isIncome = filter.isIncome)
                )
            }
            
            if (filter.hasWarranty != null || filter.warrantyExpirationStart != null || filter.warrantyExpirationEnd != null) {
                filters.add(
                    UnifiedFilter.WarrantyFilter(
                        hasWarranty = filter.hasWarranty,
                        expirationStart = filter.warrantyExpirationStart,
                        expirationEnd = filter.warrantyExpirationEnd
                    )
                )
            }
            
            return FilterStack(filters, LogicalOperator.AND)
        }
    }
}
