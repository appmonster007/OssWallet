# Filter System Optimization Proposal

## Current State Analysis

### Multiple Filter Representations (3 parallel systems)

1. **TransactionFilter** (Simple, flat data class)
   - 22 fields in a single data class
   - Used in ViewModel state and repository queries
   - Direct mapping to database via FilterMapper

2. **RecordsFilterContext** (Navigation-specific sealed class)
   - 5 variants: None, ByAccount, ByAccountAndType, ByAccountAndCurrency, Custom
   - Used for navigation parameters
   - Custom variant has similar fields to TransactionFilter but different types
   - Conversion logic in TransactionViewModel.initializeRecordsFilter

3. **FilterStack + UnifiedFilter** (Complex stack-based system)
   - 12+ filter types in UnifiedFilter sealed class
   - 399 lines of conversion logic in FilterStack
   - Supports logical operators (AND/OR)
   - Supports instant/transient filters with wrapping
   - Supports composite filters (nested combinations)
   - Has its own timeline range conversion (duplicates TimelineService)

### Key Complexity Points

1. **Redundant Timeline Conversion**
   - FilterStack.convertSingleFilter (lines 161-176) duplicates TimelineService logic
   - TimelineService.getDateRange() now provides this functionality

2. **Type Inconsistencies**
   - RecordsFilterContext.Custom uses `String` for categoryIds vs TransactionFilter uses `List<Long>`
   - RecordsFilterContext.Custom uses `Double` for amounts vs TransactionFilter uses `BigDecimal`
   - Requires conversion in TransactionViewModel.initializeRecordsFilter

3. **Over-Engineering**
   - FilterStack supports AND/OR logical operators - not needed for simple card navigation
   - InstantFilter wrapping adds complexity for transient filters
   - CompositeFilter allows nested combinations - likely unnecessary for current use cases
   - FilterStack has 200+ lines of conversion logic (toTransactionFilter, convertSingleFilter, convertCompositeFilter)

4. **Multiple Conversion Paths**
   - FilterStack → TransactionFilter (FilterStack.toTransactionFilter)
   - TransactionFilter → FilterStack (FilterStack.fromTransactionFilter)
   - RecordsFilterContext → TransactionFilter (TransactionViewModel.initializeRecordsFilter)
   - TransactionFilter ↔ FilterEntity (FilterMapper)

5. **ViewModel State Complexity**
   - TransactionViewModel has 3 separate filter states:
     - `_recordsFilter` (TransactionFilter)
     - `_accountDetailFilter` (TransactionFilter)
     - `_initialFilterContext` (RecordsFilterContext)

## Proposed Simplifications

### Option A: Minimal Change (Recommended for Card Timeline Navigation)

**Scope:** Only optimize what's needed for the card timeline navigation feature

**Changes:**
1. **Enhance RecordsFilterContext** - Add timeline fields to all context types
   - Add `startDate: Long?` and `endDate: Long?` to ByAccount, ByAccountAndType, ByAccountAndCurrency
   - Use TimelineService.getDateRange() for conversion
   - Remove type inconsistencies (use proper types from TransactionFilter)

2. **Simplify RecordsFilterContext.Custom**
   - Change categoryIds from `List<String>` to `List<Long>` (match TransactionFilter)
   - Change minAmount/maxAmount from `Double` to `BigDecimal` (match TransactionFilter)
   - Remove conversion logic from TransactionViewModel

3. **Update TransactionViewModel.initializeRecordsFilter**
   - Use TimelineService.getDateRange() instead of inline timeline conversion
   - Remove type conversion logic (no longer needed)

**Benefits:**
- Minimal changes to existing code
- Focused on the card navigation use case
- Reuses existing TimelineService.getDateRange()
- Removes type inconsistencies

**Risks:**
- Doesn't address broader FilterStack complexity
- RecordsFilterContext still separate from TransactionFilter

**Files Modified:** 3
- `ui/navigation/RecordsFilterContext.kt`
- `ui/viewmodel/TransactionViewModel.kt`
- No changes to FilterStack (leave as-is for now)

---

### Option B: Moderate Simplification

**Scope:** Unify RecordsFilterContext with TransactionFilter, simplify FilterStack usage

**Changes:**
1. **Replace RecordsFilterContext with TransactionFilter**
   - Remove RecordsFilterContext sealed class entirely
   - Use TransactionFilter directly for navigation parameters
   - TransactionFilter is already serializable and has all needed fields
   - Add convenience extension functions for common patterns:
     ```kotlin
     fun TransactionFilter.forAccount(accountId: String): TransactionFilter
     fun TransactionFilter.forIncome(): TransactionFilter
     fun TransactionFilter.forExpense(): TransactionFilter
     fun TransactionFilter.withTimeline(timelineRange: TimelineRange): TransactionFilter
     ```

2. **Deprecate FilterStack for Simple Use Cases**
   - Keep FilterStack for advanced use cases (saved filters, complex combinations)
   - Add warning that FilterStack is overkill for simple navigation
   - Document that TransactionFilter should be preferred for simple cases

3. **Update Navigation**
   - Change navigation parameter from `filterContext: String?` to `filter: TransactionFilter?`
   - Remove URL encoding/decoding logic
   - Remove initializeRecordsFilter - just use the filter directly

**Benefits:**
- Single source of truth for filters (TransactionFilter)
- Eliminates RecordsFilterContext → TransactionFilter conversion
- Simpler navigation (no serialization/deserialization)
- Fewer conversion paths to maintain

**Risks:**
- Breaking change to navigation API
- Need to update all navigation calls
- TransactionFilter is less type-safe than sealed class (but more flexible)

**Files Modified:** 10+
- Remove: `ui/navigation/RecordsFilterContext.kt`
- Modify: All navigation calls (DashboardScreen, AccountDetailScreen, etc.)
- Modify: TransactionListScreen parameter
- Modify: OssWalletNavGraph
- Add: Extension functions in new file

---

### Option C: Complete Simplification (Aggressive)

**Scope:** Eliminate FilterStack entirely, use only TransactionFilter

**Changes:**
1. **Remove FilterStack and UnifiedFilter**
   - Delete FilterStack.kt (399 lines)
   - Delete UnifiedFilter.kt (171 lines)
   - Delete FilterStackBuilder.kt
   - Remove all FilterStack usage from codebase

2. **Remove RecordsFilterContext**
   - Delete RecordsFilterContext.kt
   - Use TransactionFilter everywhere

3. **Enhance TransactionFilter**
   - Add convenience extension functions for common operations
   - Add builder pattern for complex filter construction:
     ```kotlin
     TransactionFilter {
       accountIds = listOf("acc1")
       categoryIds = listOf(1, 2)
       withTimeline(TimelineRange.ThisMonth)
     }
     ```

4. **Simplify TransactionViewModel**
   - Remove _initialFilterContext (use TransactionFilter directly)
   - Remove initializeRecordsFilter (no longer needed)
   - Remove resetToInitialFilter (use simple reset)
   - Single filter state: _recordsFilter

5. **Update Saved Filters**
   - Store TransactionFilter directly (no FilterStack conversion)
   - Remove FilterStack.fromTransactionFilter/toTransactionFilter

**Benefits:**
- Massive code reduction (~600 lines removed)
- Single filter representation (TransactionFilter)
- No conversion logic needed
- Simpler architecture
- Easier to understand and maintain

**Risks:**
- Very large breaking change
- Lost advanced features (AND/OR logic, composite filters, instant filters)
- Need to verify if any features depend on FilterStack
- Significant testing effort required

**Files Modified:** 20+
- Delete: FilterStack.kt, UnifiedFilter.kt, FilterStackBuilder.kt, RecordsFilterContext.kt
- Modify: All files using FilterStack or RecordsFilterContext
- Modify: TransactionViewModel
- Modify: Saved filter system
- Modify: All navigation

---

## Recommendation for Card Timeline Navigation Plan

**Original Recommendation: Option A (Minimal Change)**

**Rationale:**
1. **Scope-appropriate:** The card timeline navigation feature only needs to pass timeline ranges to RecordsFilterContext
2. **Low risk:** Minimal changes, doesn't disrupt existing FilterStack system
3. **Focused:** Solves the immediate problem without broader refactoring
4. **Reuses infrastructure:** Leverages the new TimelineService.getDateRange() function
5. **Future-proof:** Leaves door open for broader simplification later if needed

---

## Updated Recommendation: Option C Modified (Simplified with Multi-Filter Support)

**User Requirement:**
- Keep functionality to have multiple filters applied simultaneously
- Filters should be removable individually
- When all applied, merge into single combined filter
- Removing any 1 filter should recompute the merged filter

**Modified Option C: Simplified Filter with Lightweight FilterManager**

### Architecture

**1. Remove Complex Systems**
- Delete FilterStack.kt (399 lines)
- Delete UnifiedFilter.kt (171 lines)
- Delete FilterStackBuilder.kt
- Delete RecordsFilterContext.kt

**2. Keep TransactionFilter as Single Source of Truth**
- TransactionFilter is the only filter data class
- All operations use TransactionFilter
- No conversion between different filter types

**3. Introduce Simple FilterManager**
```kotlin
/**
 * Lightweight filter manager that maintains multiple independent filters
 * and automatically merges them into a single TransactionFilter.
 * 
 * Uses AND logic for merging - all active filters are combined.
 * Adding/removing filters automatically recomputes the merged filter.
 */
class FilterManager {
    private val activeFilters = mutableMapOf<String, TransactionFilter>()
    
    /**
     * The merged filter combining all active filters with AND logic
     */
    val mergedFilter: TransactionFilter
        get() = mergeFilters(activeFilters.values.toList())
    
    /**
     * Add or update a filter with a unique key
     * @param key Unique identifier for this filter (e.g., "account", "category", "timeline")
     * @param filter The filter to add/update
     */
    fun setFilter(key: String, filter: TransactionFilter) {
        activeFilters[key] = filter
    }
    
    /**
     * Remove a filter by key
     * @param key The unique identifier of the filter to remove
     */
    fun removeFilter(key: String) {
        activeFilters.remove(key)
    }
    
    /**
     * Check if a filter exists
     */
    fun hasFilter(key: String): Boolean = activeFilters.containsKey(key)
    
    /**
     * Get a specific filter
     */
    fun getFilter(key: String): TransactionFilter? = activeFilters[key]
    
    /**
     * Clear all filters
     */
    fun clear() {
        activeFilters.clear()
    }
    
    /**
     * Get all active filter keys
     */
    fun getFilterKeys(): Set<String> = activeFilters.keys
    
    /**
     * Merge multiple TransactionFilters using AND logic
     * - Lists are unioned (OR within the field, AND across filters)
     * - Single values use last non-null value
     * - Ranges use min start and max end
     */
    private fun mergeFilters(filters: List<TransactionFilter>): TransactionFilter {
        if (filters.isEmpty()) return TransactionFilter()
        
        return TransactionFilter(
            queryNote = filters.mapNotNull { it.queryNote }.filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() },
            isNoteRegex = filters.any { it.isNoteRegex },
            queryPayee = filters.mapNotNull { it.queryPayee }.filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() },
            isPayeeRegex = filters.any { it.isPayeeRegex },
            categoryIds = filters.flatMap { it.categoryIds }.distinct(),
            accountIds = filters.flatMap { it.accountIds }.distinct(),
            startDate = filters.mapNotNull { it.startDate }.minOrNull(),
            endDate = filters.mapNotNull { it.endDate }.maxOrNull(),
            paymentMethods = filters.flatMap { it.paymentMethods }.distinct(),
            hasWarranty = filters.mapNotNull { it.hasWarranty }.lastOrNull(),
            warrantyExpirationStart = filters.mapNotNull { it.warrantyExpirationStart }.minOrNull(),
            warrantyExpirationEnd = filters.mapNotNull { it.warrantyExpirationEnd }.maxOrNull(),
            minAmount = filters.mapNotNull { it.minAmount }.minOrNull(),
            maxAmount = filters.mapNotNull { it.maxAmount }.maxOrNull(),
            transactionType = filters.mapNotNull { it.transactionType }.lastOrNull(),
            currencies = filters.flatMap { it.currencies }.distinct(),
            statuses = filters.flatMap { it.statuses }.distinct()
        )
    }
}
```

**4. Extension Functions for Common Patterns**
```kotlin
// Convenience functions to create filters
fun TransactionFilter.forAccount(accountId: String): TransactionFilter =
    copy(accountIds = listOf(accountId))

fun TransactionFilter.forCategory(categoryId: Long): TransactionFilter =
    copy(categoryIds = listOf(categoryId))

fun TransactionFilter.forIncome(): TransactionFilter =
    copy(transactionType = "income")

fun TransactionFilter.forExpense(): TransactionFilter =
    copy(transactionType = "expense")

fun TransactionFilter.withTimeline(timelineRange: TimelineRange): TransactionFilter {
    val (start, end) = TimelineService.getDateRange(timelineRange)
    return copy(startDate = start, endDate = end)
}

fun TransactionFilter.withCurrency(currency: String): TransactionFilter =
    copy(currencies = listOf(currency))

fun TransactionFilter.withDateRange(start: Long?, end: Long?): TransactionFilter =
    copy(startDate = start, endDate = end)
```

**5. Updated TransactionViewModel**
```kotlin
class TransactionViewModel(...) : ViewModel() {
    // Replace multiple filter states with single FilterManager
    private val filterManager = FilterManager()
    
    // Expose merged filter as StateFlow
    val recordsFilter: StateFlow<TransactionFilter> = filterManager.mergedFilter.asStateFlow()
    
    // Convenience methods for common operations
    fun setAccountFilter(accountId: String?) {
        if (accountId != null) {
            filterManager.setFilter("account", TransactionFilter().forAccount(accountId))
        } else {
            filterManager.removeFilter("account")
        }
    }
    
    fun setCategoryFilter(categoryId: Long?) {
        if (categoryId != null) {
            filterManager.setFilter("category", TransactionFilter().forCategory(categoryId))
        } else {
            filterManager.removeFilter("category")
        }
    }
    
    fun setTimelineFilter(timelineRange: TimelineRange) {
        filterManager.setFilter("timeline", TransactionFilter().withTimeline(timelineRange))
    }
    
    fun setIncomeTypeFilter(isIncome: Boolean?) {
        if (isIncome != null) {
            filterManager.setFilter("type", TransactionFilter().run {
                if (isIncome) forIncome() else forExpense()
            })
        } else {
            filterManager.removeFilter("type")
        }
    }
    
    fun clearFilters() {
        filterManager.clear()
    }
    
    // For backward compatibility with existing UI
    fun updateRecordsFilter(newFilter: TransactionFilter) {
        filterManager.setFilter("manual", newFilter)
    }
}
```

### Benefits of Modified Option C

1. **Simpler than FilterStack**
   - No UnifiedFilter sealed class with 12+ types
   - No logical operators (always AND)
   - No instant/transient filter wrapping
   - No composite filters
   - ~600 lines of complex code removed

2. **Retains Multi-Filter Capability**
   - FilterManager holds multiple independent filters
   - Each filter has a unique key for identification
   - Can add/remove filters individually
   - Automatic recompute on each change

3. **Single Source of Truth**
   - TransactionFilter is the only filter representation
   - No conversion between different types
   - No RecordsFilterContext needed

4. **Clean Navigation**
   - Use TransactionFilter directly for navigation
   - No URL encoding/decoding
   - No initializeRecordsFilter needed

5. **Type Safety**
   - Uses proper types (Long for categoryIds, BigDecimal for amounts)
   - No type conversion needed

### Implementation Plan

**Phase 1: Create FilterManager**
- Create FilterManager class
- Add extension functions for common patterns
- Write unit tests for merge logic

**Phase 2: Update TransactionViewModel**
- Replace filter states with FilterManager
- Update convenience methods
- Test backward compatibility

**Phase 3: Update Navigation**
- Replace RecordsFilterContext with TransactionFilter
- Update all navigation calls
- Remove URL encoding/decoding

**Phase 4: Update Cards**
- Use FilterManager in card navigation
- Pass timeline ranges via extension functions
- Test all card navigation flows

**Phase 5: Remove Old Code**
- Delete FilterStack, UnifiedFilter, FilterStackBuilder
- Delete RecordsFilterContext
- Remove all references

**Phase 6: Update Saved Filters**
- Store TransactionFilter directly
- Remove FilterStack conversion logic
- Test saved filter functionality

### Files Modified

**New Files:**
- `domain/model/FilterManager.kt` - Lightweight filter manager
- `domain/model/TransactionFilterExtensions.kt` - Convenience extension functions

**Deleted Files:**
- `domain/model/filter/FilterStack.kt` (399 lines)
- `domain/model/filter/UnifiedFilter.kt` (171 lines)
- `domain/model/filter/FilterStackBuilder.kt`
- `ui/navigation/RecordsFilterContext.kt`

**Modified Files:**
- `ui/viewmodel/TransactionViewModel.kt`
- `ui/navigation/OssWalletNavGraph.kt`
- `ui/navigation/Screen.kt`
- `ui/screens/TransactionListScreen.kt`
- `ui/dashboard/DashboardScreen.kt`
- `ui/dashboard/components/*` (all card components)
- All files using FilterStack or RecordsFilterContext

### Risks and Mitigations

**Risk:** Large breaking change
**Mitigation:** Phase implementation, thorough testing at each phase

**Risk:** Lost advanced features (AND/OR logic)
**Mitigation:** FilterManager always uses AND, which is sufficient for card navigation. If OR needed, can add later.

**Risk:** Saved filter compatibility
**Mitigation:** Migration script to convert saved FilterStack to TransactionFilter

**Risk:** Testing effort
**Mitigation:** Comprehensive unit tests for FilterManager merge logic

**Specific Changes for Option A:**

1. **RecordsFilterContext.kt**
   ```kotlin
   data class ByAccount(
       val accountId: String,
       val startDate: Long? = null,  // NEW
       val endDate: Long? = null     // NEW
   ) : RecordsFilterContext()
   
   data class ByAccountAndType(
       val accountId: String,
       val isIncome: Boolean,
       val startDate: Long? = null,  // NEW
       val endDate: Long? = null     // NEW
   ) : RecordsFilterContext()
   
   data class ByAccountAndCurrency(
       val accountId: String,
       val currency: String,
       val startDate: Long? = null,  // NEW
       val endDate: Long? = null     // NEW
   ) : RecordsFilterContext()
   
   data class Custom(
       val accountIds: List<String> = emptyList(),
       val isIncome: Boolean? = null,
       val startDate: Long? = null,
       val endDate: Long? = null,
       val categoryIds: List<Long> = emptyList(),  // CHANGED from List<String>
       val currencies: List<String> = emptyList(),
       val paymentMethods: List<String> = emptyList(),
       val queryNote: String? = null,
       val minAmount: BigDecimal? = null,  // CHANGED from Double
       val maxAmount: BigDecimal? = null,  // CHANGED from Double
       val queryPayee: String? = null
   ) : RecordsFilterContext()
   ```

2. **TransactionViewModel.initializeRecordsFilter**
   ```kotlin
   fun initializeRecordsFilter(context: RecordsFilterContext) {
       _initialFilterContext.value = context
       val filter = when (context) {
           is RecordsFilterContext.None -> TransactionFilter()
           is RecordsFilterContext.ByAccount -> {
               TransactionFilter(
                   accountIds = listOf(context.accountId),
                   startDate = context.startDate,
                   endDate = context.endDate
               )
           }
           is RecordsFilterContext.ByAccountAndType -> {
               TransactionFilter(
                   accountIds = listOf(context.accountId),
                   transactionType = if (context.isIncome) "income" else "expense",
                   startDate = context.startDate,
                   endDate = context.endDate
               )
           }
           is RecordsFilterContext.ByAccountAndCurrency -> {
               TransactionFilter(
                   accountIds = listOf(context.accountId),
                   currencies = listOf(context.currency),
                   startDate = context.startDate,
                   endDate = context.endDate
               )
           }
           is RecordsFilterContext.Custom -> {
               TransactionFilter(
                   accountIds = context.accountIds,
                   transactionType = context.isIncome?.let { if (it) "income" else "expense" },
                   startDate = context.startDate,
                   endDate = context.endDate,
                   categoryIds = context.categoryIds,
                   currencies = context.currencies,
                   paymentMethods = context.paymentMethods,
                   queryNote = context.queryNote,
                   minAmount = context.minAmount,
                   maxAmount = context.maxAmount,
                   queryPayee = context.queryPayee
               )
           }
       }
       _recordsFilter.value = filter
       _recordsFilterInitialized.value = true
   }
   ```

3. **DashboardScreen** - Update card navigation calls
   ```kotlin
   // Example for CashFlowCard
   onClickIncome = { 
       val (start, end) = TimelineService.getDateRange(cardConfig.timelineRange)
       onRecordsClick(RecordsFilterContext.Custom(
           isIncome = true,
           startDate = start,
           endDate = end
       ))
   }
   ```

## Should This Be Included in Card Timeline Navigation Plan?

**Yes, include Option A changes:**
- Directly enables the card timeline navigation feature
- Minimal scope, low risk
- Reuses TimelineService.getDateRange() (already implemented)
- Removes type inconsistencies
- No impact on FilterStack (leave for future refactoring)

**Defer Options B and C:**
- These are broader architectural changes
- Should be separate refactoring initiatives
- Require more analysis of FilterStack usage
- Need to verify if advanced features are actually used
- Larger testing effort required

**Decision:** Include Option A in the current plan as Phase 1.5 (between infrastructure and card updates).
