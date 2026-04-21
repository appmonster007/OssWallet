package com.osscabinet.ossWallet.domain.model.filter

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.service.TimelineService
import java.math.BigDecimal

/**
 * A stack of filters that can be combined with logical operators.
 * This enables stacking multiple filters in sequence and creating complex filter combinations.
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
     * Clears all filters from the stack.
     */
    fun clear(): FilterStack = FilterStack(emptyList())
    
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
     */
    fun toTransactionFilter(): TransactionFilter {
        var result = TransactionFilter()
        
        for (filter in activeFilters) {
            result = when (filter) {
                is UnifiedFilter.TextFilter -> {
                    // TextFilter searches both note and payee fields
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
                    // For composite filters, we merge the conditions
                    // Note: This is a simplification - proper handling would need to respect logical operators
                    var compositeResult = result
                    for (subFilter in filter.filters) {
                        compositeResult = FilterStack(listOf(subFilter)).toTransactionFilter()
                        // Merge fields (last one wins for now - this is a limitation of the legacy model)
                        result = result.copy(
                            queryNote = compositeResult.queryNote ?: result.queryNote,
                            isNoteRegex = compositeResult.isNoteRegex || result.isNoteRegex,
                            queryPayee = compositeResult.queryPayee ?: result.queryPayee,
                            isPayeeRegex = compositeResult.isPayeeRegex || result.isPayeeRegex,
                            categoryIds = if (compositeResult.categoryIds.isNotEmpty()) compositeResult.categoryIds else result.categoryIds,
                            accountIds = if (compositeResult.accountIds.isNotEmpty()) compositeResult.accountIds else result.accountIds,
                            startDate = compositeResult.startDate ?: result.startDate,
                            endDate = compositeResult.endDate ?: result.endDate,
                            paymentMethods = if (compositeResult.paymentMethods.isNotEmpty()) compositeResult.paymentMethods else result.paymentMethods,
                            hasWarranty = compositeResult.hasWarranty ?: result.hasWarranty,
                            warrantyExpirationStart = compositeResult.warrantyExpirationStart ?: result.warrantyExpirationStart,
                            warrantyExpirationEnd = compositeResult.warrantyExpirationEnd ?: result.warrantyExpirationEnd,
                            minAmount = compositeResult.minAmount ?: result.minAmount,
                            maxAmount = compositeResult.maxAmount ?: result.maxAmount,
                            isIncome = compositeResult.isIncome ?: result.isIncome,
                            currencies = if (compositeResult.currencies.isNotEmpty()) compositeResult.currencies else result.currencies
                        )
                    }
                    result
                }
                is UnifiedFilter.SavedFilterReference -> {
                    // Cannot convert saved filter reference without loading from database
                    // Return as-is - caller should resolve the reference first
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
         * Creates a FilterStack from a legacy TransactionFilter.
         * This allows migration from the old system to the new one.
         */
        fun fromTransactionFilter(filter: TransactionFilter): FilterStack {
            val filters = mutableListOf<UnifiedFilter>()
            
            // Unified text search - if either note or payee query is set, create a TextFilter
            // This searches both fields for a better user experience
            if (!filter.queryNote.isNullOrBlank() || !filter.queryPayee.isNullOrBlank()) {
                val query = filter.queryNote ?: filter.queryPayee ?: ""
                val isRegex = filter.isNoteRegex || filter.isPayeeRegex
                filters.add(
                    UnifiedFilter.TextFilter(
                        query = query,
                        isRegex = isRegex
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
