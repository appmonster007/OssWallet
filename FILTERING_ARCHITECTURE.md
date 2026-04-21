# Unified Filtering Architecture

## Current State Analysis

### Existing Filtering Systems

#### 1. TransactionFilter (Domain Model)
**Location:** `domain/model/TransactionFilter.kt`

**Fields:**
- `queryNote: String?` - Note text search (legacy, will be unified with payee)
- `isNoteRegex: Boolean` - Regex flag for note (legacy)
- `queryPayee: String?` - Payee text search (legacy, will be unified with note)
- `isPayeeRegex: Boolean` - Regex flag for payee (legacy)
- `categoryIds: List<Long>` - Category filters
- `accountIds: List<String>` - Account filters
- `startDate: Long?` - Date range start
- `endDate: Long?` - Date range end
- `paymentMethods: List<String>` - Payment method filters
- `hasWarranty: Boolean?` - Warranty presence
- `warrantyExpirationStart: Long?` - Warranty date range start
- `warrantyExpirationEnd: Long?` - Warranty date range end
- `minAmount: BigDecimal?` - Minimum amount
- `maxAmount: BigDecimal?` - Maximum amount
- `isIncome: Boolean?` - Income/expense filter
- `currencies: List<String>` - Currency filters

#### 2. TimelineRange (Separate System)
**Location:** `domain/model/timeline/TimelineRange.kt`

**Types:**
- `AllTime` - No time restriction
- `ThisPeriod(ThisPeriodType)` - Current period
- `LastPeriod(LastPeriodType)` - Last N period
- `CustomRange(start, end)` - Custom date range

**ThisPeriodType:** TODAY, WEEK, MONTH, QUARTER, HALF_YEAR, YEAR
**LastPeriodType:** LAST_24H, LAST_7_DAYS, LAST_30_DAYS, LAST_12_WEEKS, LAST_6_MONTHS, LAST_1_YEAR, LAST_5_YEARS

#### 3. FilterEntity (Database)
**Location:** `data/local/entity/FilterEntity.kt`

Stores saved filters with comma-separated lists for collections.

#### 4. Multiple Filter Instances
**TransactionViewModel:**
- `recordsFilter: StateFlow<TransactionFilter>` - Main transaction list
- `accountDetailFilter: StateFlow<TransactionFilter>` - Account detail screen

**AccountDetailViewModel:**
- Separate filter state for account-specific filtering

### Problems Identified

1. **No Standard Format** - TimelineRange is separate from TransactionFilter, requiring manual conversion
2. **No Filter Stacking** - Cannot combine multiple filters (e.g., saved filter + date range)
3. **Scattered State** - Each screen maintains its own filter StateFlow
4. **No Composition** - Cannot create complex filter combinations (AND/OR logic)
5. **Duplicate Logic** - Similar filtering code in multiple ViewModels
6. **Limited Reusability** - Saved filters cannot be combined with ad-hoc modifications
7. **No Operator Support** - Cannot express "NOT category X" or "category X AND (amount > 100)"

## Proposed Unified Architecture

### Core Design Principles

1. **Backward Compatibility** - All existing functionality must continue to work
2. **Composition Over Inheritance** - Filters compose rather than extend
3. **Immutable State** - Filters are immutable data classes
4. **Type Safety** - Leverage Kotlin's type system for filter operators
5. **Performance** - Efficient database query building
6. **Testability** - Pure functions for filter logic

### New Filter Model

#### 1. UnifiedFilter (Domain Model)

```kotlin
sealed class UnifiedFilter {
    // Atomic filters (single criteria)
    data class TextFilter(
        val query: String,
        val isRegex: Boolean = false,
        val operator: FilterOperator = FilterOperator.CONTAINS
    ) : UnifiedFilter() // Searches both note and payee fields
    
    data class CategoryFilter(
        val categoryIds: List<Long>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()
    
    data class AccountFilter(
        val accountIds: List<String>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()
    
    data class DateRangeFilter(
        val timelineRange: TimelineRange
    ) : UnifiedFilter()
    
    data class AmountFilter(
        val min: BigDecimal? = null,
        val max: BigDecimal? = null
    ) : UnifiedFilter()
    
    data class PaymentMethodFilter(
        val methods: List<String>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()
    
    data class CurrencyFilter(
        val currencies: List<String>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()
    
    data class IncomeTypeFilter(
        val isIncome: Boolean? = null
    ) : UnifiedFilter()
    
    data class WarrantyFilter(
        val hasWarranty: Boolean? = null,
        val expirationStart: Long? = null,
        val expirationEnd: Long? = null
    ) : UnifiedFilter()
    
    // Composite filters (combine multiple filters)
    data class CompositeFilter(
        val filters: List<UnifiedFilter>,
        val operator: LogicalOperator = LogicalOperator.AND
    ) : UnifiedFilter()
    
    // Saved filter reference
    data class SavedFilterReference(
        val filterId: Long
    ) : UnifiedFilter()
}
```

#### 2. Filter Operators

```kotlin
enum class FilterOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    IN,
    NOT_IN,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    BETWEEN,
    REGEX
}

enum class LogicalOperator {
    AND,
    OR
}
```

#### 3. FilterStack (Composition)

```kotlin
data class FilterStack(
    val activeFilters: List<UnifiedFilter>,
    val logicalOperator: LogicalOperator = LogicalOperator.AND
) {
    fun addFilter(filter: UnifiedFilter): FilterStack =
        copy(activeFilters = activeFilters + filter)
    
    fun removeFilter(filter: UnifiedFilter): FilterStack =
        copy(activeFilters = activeFilters - filter)
    
    fun combineWith(other: FilterStack, operator: LogicalOperator): FilterStack =
        FilterStack(
            activeFilters = listOf(
                CompositeFilter(this.activeFilters, this.logicalOperator),
                CompositeFilter(other.activeFilters, other.logicalOperator)
            ),
            logicalOperator = operator
        )
    
    fun toTransactionFilter(): TransactionFilter {
        // Convert unified filter stack to legacy TransactionFilter
        // for backward compatibility
    }
    
    companion object {
        val Empty = FilterStack(emptyList())
    }
}
```

### Migration Strategy

#### Phase 1: Add New Models (Non-Breaking)
- Create `UnifiedFilter`, `FilterOperator`, `LogicalOperator`
- Create `FilterStack` with conversion methods
- Add `toTransactionFilter()` for backward compatibility
- Add `fromTransactionFilter()` for migration

#### Phase 2: Update Repository Layer
- Add `getFilteredTransactions(profileId, filterStack: FilterStack)` to TransactionRepository
- Implement SQL query builder for FilterStack
- Keep existing `getFilteredTransactions(profileId, filter: TransactionFilter)` for compatibility
- Add unit tests for query builder

#### Phase 3: Update Use Cases
- Add `getFilteredTransactions(profileId, filterStack: FilterStack)` to TransactionUseCases
- Keep existing methods for backward compatibility
- Add filter composition helpers

#### Phase 4: Update ViewModels (Gradual)
- Add `filterStack: StateFlow<FilterStack>` alongside existing filters
- Add methods to manipulate FilterStack
- Keep existing filter methods for compatibility
- Gradually migrate screens to use FilterStack

#### Phase 5: Update UI Components
- Add FilterStack-aware UI components
- Keep existing filter UI for compatibility
- Add filter stack visualization
- Add save/load filter combinations

#### Phase 6: Deprecate Old System (After Validation)
- Mark old filter methods as `@Deprecated`
- Provide migration path for saved filters
- Remove old code after validation period

### Implementation Benefits

1. **Unified Text Search**
```kotlin
// Single text search that checks both note and payee fields
val textFilter = TextFilter(query = "coffee", isRegex = false)
// This will find transactions where "coffee" appears in either note or payee
```

2. **Filter Stacking**
```kotlin
// Create a filter stack combining saved filter + date range
val savedFilter = SavedFilterReference(filterId = 1)
val dateFilter = DateRangeFilter(TimelineRange.ThisPeriod(ThisPeriodType.MONTH))
val stack = FilterStack(listOf(savedFilter, dateFilter), LogicalOperator.AND)
```

3. **Complex Compositions**
```kotlin
// Category X OR (Category Y AND Amount > 100)
val categoryX = CategoryFilter(listOf(1L))
val categoryY = CategoryFilter(listOf(2L))
val amount = AmountFilter(min = BigDecimal(100))
val composite = CompositeFilter(listOf(categoryY, amount), LogicalOperator.AND)
val stack = FilterStack(listOf(categoryX, composite), LogicalOperator.OR)
```

4. **Filter Operators**
```kotlin
// NOT in category X
val notCategoryX = CategoryFilter(listOf(1L), FilterOperator.NOT_IN)

// Amount between 50 and 100
val amountRange = AmountFilter(min = BigDecimal(50), max = BigDecimal(100))
```

5. **Backward Compatibility**
```kotlin
// Old code still works
val oldFilter = TransactionFilter(categoryIds = listOf(1L, 2L))

// New code can interoperate
val newStack = FilterStack.fromTransactionFilter(oldFilter)
val convertedBack = newStack.toTransactionFilter()
```

### Database Schema Changes

#### New Table: FilterCombinations

```sql
CREATE TABLE filter_combinations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    filters TEXT NOT NULL, -- JSON serialized UnifiedFilter list
    logical_operator TEXT NOT NULL, -- "AND" or "OR"
    profile_id INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
);
```

#### Migration
- Add migration to create new table
- Migrate existing saved filters to new format
- Keep existing `transaction_filters` table for compatibility

### Performance Considerations

1. **Query Building**
   - Build SQL queries incrementally
   - Use parameterized queries
   - Cache query plans for common filter combinations

2. **Indexing**
   - Ensure proper indexes on filtered columns
   - Consider composite indexes for common filter patterns

3. **Pagination**
   - FilterStack should work with Paging 3
   - Apply filters before pagination for efficiency

### Testing Strategy

1. **Unit Tests**
   - FilterStack composition
   - Filter operator logic
   - TransactionFilter conversion
   - SQL query builder

2. **Integration Tests**
   - Database queries with FilterStack
   - ViewModel filter state management
   - UI filter interactions

3. **UI Tests**
   - Filter stack UI components
   - Filter composition workflows
   - Backward compatibility scenarios

### Migration Timeline

**Week 1-2:** Phase 1 - Add new models and conversion utilities
**Week 3:** Phase 2 - Update repository layer with new methods
**Week 4:** Phase 3 - Update use cases
**Week 5-6:** Phase 4 - Update ViewModels gradually
**Week 7:** Phase 5 - Update UI components
**Week 8:** Phase 6 - Deprecate old system after validation

### Rollback Plan

If issues arise:
1. Revert to previous filter system
2. Keep new models as experimental features
3. Gradually reintroduce with fixes
4. Maintain backward compatibility indefinitely if needed
