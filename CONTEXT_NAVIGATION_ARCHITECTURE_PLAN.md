# Context Navigation Architecture Plan (Simplified & Unified with FilterStack)

## Current State Analysis

### Existing Filter Infrastructure

#### FilterStack (Domain Layer)
- **Purpose**: Stack of filters that can be combined with logical operators
- **Location**: `domain/model/filter/FilterStack.kt`
- **Features**:
  - Combines filters with AND/OR operators
  - Supports instant/transient filters
  - Converts to TransactionFilter via `toTransactionFilter()`
  - Flattens nested CompositeFilters for clean structure

#### UnifiedFilter (Domain Layer)
- **Purpose**: Standard format for all filtering operations
- **Location**: `domain/model/filter/UnifiedFilter.kt`
- **Atomic Filters**:
  - `TextFilter`: Search by text (note/payee)
  - `CategoryFilter`: Filter by category IDs
  - `AccountFilter`: Filter by account IDs
  - `DateRangeFilter`: Filter by timeline/date range
  - `AmountFilter`: Filter by amount range
  - `PaymentMethodFilter`: Filter by payment methods
  - `CurrencyFilter`: Filter by currencies
  - `IncomeTypeFilter`: Filter by income/expense type
  - `WarrantyFilter`: Filter by warranty status
- **Composite Filters**:
  - `CompositeFilter`: Combines multiple filters with logical operator
- **Saved Filter Reference**:
  - `SavedFilterReference`: Reference to saved filter by ID
- **Instant Filters**:
  - `InstantFilter`: Wraps any filter as transient

#### LogicalOperator (Domain Layer)
- **Purpose**: Logical operators for combining filters
- **Location**: `domain/model/filter/LogicalOperator.kt`
- **Types**: AND, OR

### Existing Context Types

#### 1. RecordsFilterContext (TransactionListScreen)
- **Purpose**: Filter transactions when navigating to Records screen
- **Location**: `ui/navigation/RecordsFilterContext.kt`
- **Types**:
  - `None`: No filter
  - `ByAccount(accountId)`: Filter by single account
  - `ByAccountAndType(accountId, isIncome)`: Filter by account + income/expense
  - `ByAccountAndCurrency(accountId, currency)`: Filter by account + currency
  - `Custom(...)`: Arbitrary filter criteria (accountIds, isIncome, dates, categories, currencies, paymentMethods, notes, payee, amounts)

#### 2. AccountsFilterContext (AccountsViewScreen)
- **Purpose**: Filter accounts when navigating to AccountsView screen
- **Location**: `ui/navigation/AccountsFilterContext.kt`
- **Types**:
  - `None`: No filter
  - `ByType(accountType)`: Filter by account type
  - `ByCurrency(currency)`: Filter by currency
  - `Custom(...)`: Arbitrary filter criteria (accountTypes, currencies, balance range, includeHidden)

#### 3. DashboardViewContext (DashboardScreen)
- **Purpose**: Focus on specific section/card in Dashboard
- **Location**: `ui/navigation/DashboardViewContext.kt`
- **Types**:
  - `None`: No focus
  - `FocusCard(cardId)`: Focus on specific card
  - `FocusSection(section)`: Focus on specific section

### Current Usage Patterns

#### Dashboard Screen
```kotlin
// CurrencyBalancesCard
onCurrencyClick = { currency ->
    onRecordsClick(RecordsFilterContext.Custom(currencies = listOf(currency)))
}

// CashFlowCard
onClickIncome = { onRecordsClick(RecordsFilterContext.Custom(isIncome = true)) }
onClickExpense = { onRecordsClick(RecordsFilterContext.Custom(isIncome = false)) }

// BudgetsCard
onBudgetClick = { category -> onEditBudgetClick(category) } // Navigates to EditBudget, not Records
```

#### AccountsView Screen
```kotlin
// CurrencyBalancesCard
onCurrencyClick = { currency ->
    onRecordsClick(RecordsFilterContext.Custom(currencies = listOf(currency)))
}

// CashFlowCard
onClickIncome = { onRecordsClick(RecordsFilterContext.Custom(isIncome = true)) }
onClickExpense = { onRecordsClick(RecordsFilterContext.Custom(isIncome = false)) }
```

#### AccountDetail Screen
```kotlin
// AccountHeroCard
onClick = { onRecordsClick(RecordsFilterContext.ByAccount(accountId)) }

// CashFlowCard
onClickIncome = { onRecordsClick(RecordsFilterContext.ByAccountAndType(accountId, isIncome = true)) }
onClickExpense = { onRecordsClick(RecordsFilterContext.ByAccountAndType(accountId, isIncome = false)) }

// CurrencyBalancesCard
onCurrencyClick = { currency ->
    onRecordsClick(RecordsFilterContext.ByAccountAndCurrency(accountId, currency))
}
```

### Problems & Optimization Points

#### 1. **Duplication Across Screens**
- Same navigation logic repeated in Dashboard, AccountsView, and AccountDetail
- CurrencyBalancesCard and CashFlowCard have identical navigation patterns in different screens
- No reuse of FilterStack creation logic
- **Root Cause**: Cards manually construct context parameters instead of using reusable builders

#### 2. **Tight Coupling**
- Cards directly create context objects with manual parameter construction
- Navigation logic embedded in card components
- Hard to test and maintain
- **Root Cause**: No abstraction for common navigation patterns

#### 3. **Inconsistency**
- Some cards use specific context types (ByAccount, ByAccountAndType)
- Others use generic Custom context with manual parameter lists
- No clear pattern for when to use which
- **Root Cause**: Multiple ways to achieve the same result

#### 4. **Limited Extensibility**
- Adding new screen requires creating new context sealed class
- Adding new card navigation requires modifying multiple files
- **Root Cause**: Context types are screen-specific, not generic

#### 5. **Type Safety Issues**
- Custom context accepts arbitrary parameters but no validation
- CategoryIds stored as String for serialization, converted to Long elsewhere
- Amounts stored as Double, converted to BigDecimal elsewhere
- **Root Cause**: Context parameters use primitive types instead of domain types

#### 6. **Missing Integration**
- FilterStack serialization support missing for navigation
- No direct mapping between context types and FilterStack
- **Root Cause**: Navigation contexts don't leverage existing FilterStack infrastructure

---

## Proposed Architecture (Simplified & Unified with FilterStack)

### Core Principles

1. **Reuse Existing Infrastructure**: Leverage FilterStack, UnifiedFilter, and LogicalOperator
2. **Single Generic Context**: Use FilterStack directly as the universal navigation context
3. **Type Safety**: Strong typing via UnifiedFilter types
4. **Simple Factories**: Generic builder pattern for common navigation patterns
5. **Minimal Changes**: Only add serialization to existing domain models
6. **No Delegates**: Remove unnecessary abstraction layers
7. **Direct Integration**: Cards use FilterStack factories directly

### Architecture Layers (Simplified)

```
┌─────────────────────────────────────────────────────────────┐
│                     UI Layer (Screens)                        │
│  - DashboardScreen, AccountsViewScreen, AccountDetailScreen  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Uses
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Generic FilterStack Builder                      │
│  - Single reusable builder for all navigation patterns        │
│  - Extension functions for common patterns                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Uses
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              FilterStack (Existing Domain Layer)             │
│  - Contains List<UnifiedFilter> + LogicalOperator            │
│  - toTransactionFilter() conversion                          │
│  - Serialization support (to be added)                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Uses
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              UnifiedFilter (Existing Domain Layer)           │
│  - TextFilter, CategoryFilter, AccountFilter                │
│  - DateRangeFilter, AmountFilter, CurrencyFilter            │
│  - IncomeTypeFilter, PaymentMethodFilter, WarrantyFilter     │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Encodes/Decodes
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Navigation Extensions                            │
│  - NavHostController.navigateToRecords(filterStack)        │
│  - Generic navigation for any screen with FilterStack         │
└─────────────────────────────────────────────────────────────┘
```

### Key Simplifications

**Removed:**
- ❌ Screen-specific context sealed classes (RecordsNavigationContext, AccountsNavigationContext)
- ❌ Card navigation delegates (over-engineering)
- ❌ NavigationAction sealed class (unnecessary abstraction)
- ❌ Multiple factories (RecordsFilterStackFactory, AccountsFilterStackFactory, etc.)

**Kept:**
- ✅ FilterStack as the universal navigation context
- ✅ Single generic FilterStack builder
- ✅ Extension functions for common patterns
- ✅ Direct integration with existing domain models

---

## Detailed Design (Simplified & Unified with FilterStack)

### 1. FilterStack Serialization Support

#### Add Serialization to FilterStack
```kotlin
@Serializable
data class FilterStack(
    val activeFilters: List<UnifiedFilter>,
    val logicalOperator: LogicalOperator = LogicalOperator.AND
) {
    // ... existing methods

    companion object {
        val Empty = FilterStack(emptyList(), LogicalOperator.AND)
    }
}
```

#### Add Serialization to UnifiedFilter
```kotlin
@Serializable
sealed class UnifiedFilter {
    // Simply add @Serializable annotation to all existing filter types
    // No structural changes needed
}
```

### 2. Generic FilterStack Builder (Single Reusable Component)

#### FilterStackBuilder with Extension Functions
```kotlin
/**
 * Generic builder for creating FilterStack instances.
 * Reusable across all screens and navigation patterns.
 */
class FilterStackBuilder {
    private val filters = mutableListOf<UnifiedFilter>()
    private var operator: LogicalOperator = LogicalOperator.AND

    fun accountIds(ids: List<String>) = apply {
        filters.add(UnifiedFilter.AccountFilter(ids))
    }

    fun categoryIds(ids: List<Long>) = apply {
        filters.add(UnifiedFilter.CategoryFilter(ids))
    }

    fun isIncome(isIncome: Boolean) = apply {
        filters.add(UnifiedFilter.IncomeTypeFilter(isIncome = isIncome))
    }

    fun currencies(currencies: List<String>) = apply {
        filters.add(UnifiedFilter.CurrencyFilter(currencies))
    }

    fun dateRange(timelineRange: TimelineRange) = apply {
        filters.add(UnifiedFilter.DateRangeFilter(timelineRange))
    }

    fun amountRange(min: BigDecimal?, max: BigDecimal?) = apply {
        filters.add(UnifiedFilter.AmountFilter(min = min, max = max))
    }

    fun search(query: String) = apply {
        filters.add(UnifiedFilter.TextFilter(query))
    }

    fun paymentMethods(methods: List<String>) = apply {
        filters.add(UnifiedFilter.PaymentMethodFilter(methods))
    }

    fun withOperator(op: LogicalOperator) = apply {
        this.operator = op
    }

    fun build(): FilterStack = FilterStack(filters.toList(), operator)
}

// Extension functions for common navigation patterns
fun FilterStack.Companion.forCurrency(currency: String): FilterStack =
    FilterStackBuilder().currencies(listOf(currency)).build()

fun FilterStack.Companion.forIncome(): FilterStack =
    FilterStackBuilder().isIncome(true).build()

fun FilterStack.Companion.forExpense(): FilterStack =
    FilterStackBuilder().isIncome(false).build()

fun FilterStack.Companion.forAccount(accountId: String): FilterStack =
    FilterStackBuilder().accountIds(listOf(accountId)).build()

fun FilterStack.Companion.forAccountAndType(accountId: String, isIncome: Boolean): FilterStack =
    FilterStackBuilder()
        .accountIds(listOf(accountId))
        .isIncome(isIncome)
        .build()

fun FilterStack.Companion.forAccountAndCurrency(accountId: String, currency: String): FilterStack =
    FilterStackBuilder()
        .accountIds(listOf(accountId))
        .currencies(listOf(currency))
        .build()

fun FilterStack.Companion.forCategory(categoryId: Long): FilterStack =
    FilterStackBuilder().categoryIds(listOf(categoryId)).build()
```

### 3. Direct Navigation with FilterStack

#### Navigation Extensions
```kotlin
/**
 * Navigate to RecordsScreen with FilterStack context.
 * FilterStack is serialized and passed directly.
 */
fun NavHostController.navigateToRecords(filterStack: FilterStack) {
    val encodedFilterStack = URLEncoder.encode(
        Json.encodeToString(filterStack),
        StandardCharsets.UTF_8.toString()
    )
    navigate(Screen.Records.createRoute(encodedFilterStack))
}

/**
 * Navigate to AccountsViewScreen with FilterStack context.
 */
fun NavHostController.navigateToAccountsView(filterStack: FilterStack) {
    val encodedFilterStack = URLEncoder.encode(
        Json.encodeToString(filterStack),
        StandardCharsets.UTF_8.toString()
    )
    navigate(Screen.AccountsView.createRoute(encodedFilterStack))
}
```

#### Context Conversion in ViewModels
```kotlin
// In TransactionListScreen ViewModel
fun initializeFromFilterStack(filterStack: FilterStack) {
    val transactionFilter = filterStack.toTransactionFilter()
    applyFilter(transactionFilter)
}

// In AccountsViewScreen ViewModel
fun initializeFromFilterStack(filterStack: FilterStack) {
    // Convert FilterStack to account-specific filter
    applyAccountsFilter(filterStack)
}
```

### 4. Card Component Integration (Simplified)

#### Before (Current)
```kotlin
CashFlowCard(
    onClickIncome = { onRecordsClick(RecordsFilterContext.Custom(isIncome = true)) },
    onClickExpense = { onRecordsClick(RecordsFilterContext.Custom(isIncome = false)) }
)
```

#### After (Simplified - Direct FilterStack)
```kotlin
CashFlowCard(
    onClickIncome = {
        val filterStack = FilterStack.forIncome()
        onRecordsClick(filterStack)
    },
    onClickExpense = {
        val filterStack = FilterStack.forExpense()
        onRecordsClick(filterStack)
    }
)

// For AccountDetailScreen (with accountId context)
CashFlowCard(
    onClickIncome = {
        val filterStack = FilterStack.forAccountAndType(accountId, isIncome = true)
        onRecordsClick(filterStack)
    },
    onClickExpense = {
        val filterStack = FilterStack.forAccountAndType(accountId, isIncome = false)
        onRecordsClick(filterStack)
    }
)

// For CurrencyBalancesCard
CurrencyBalancesCard(
    onCurrencyClick = { currency ->
        val filterStack = FilterStack.forCurrency(currency)
        onRecordsClick(filterStack)
    }
)
```

### 5. Screen Route Updates

#### Update Screen Routes to Accept FilterStack
```kotlin
// In Screen.kt
object Records : Screen("records?filterStack={filterStack}") {
    fun createRoute(filterStack: FilterStack? = null): String {
        val encoded = filterStack?.let {
            URLEncoder.encode(Json.encodeToString(it), StandardCharsets.UTF_8.toString())
        } ?: ""
        return "records?filterStack=$encoded"
    }
}
```

#### Update NavGraph
```kotlin
composable(
    Screen.Records.route,
    arguments = listOf(
        navArgument("filterStack") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )
) { backStackEntry ->
    val filterStackString = backStackEntry.arguments?.getString("filterStack")
    val filterStack = filterStackString?.let {
        try {
            val decoded = URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            Json.decodeFromString<FilterStack>(decoded)
        } catch (e: Exception) {
            FilterStack.Empty
        }
    } ?: FilterStack.Empty

    TransactionListScreen(
        viewModel = transactionViewModel,
        initialFilterStack = filterStack,
        // ... other parameters
    )
}
```

---

## Implementation Plan (Simplified - 4 Phases over 3 Weeks)

### Phase 1: FilterStack Serialization (Week 1)
1. Add `@Serializable` annotation to `FilterStack`
2. Add `@Serializable` annotation to all `UnifiedFilter` types
3. Add `@Serializable` annotation to `LogicalOperator` enum
4. Add serialization support for `TimelineRange` (if needed)
5. Add serialization support for `BigDecimal` (using custom serializer)
6. Add unit tests for serialization/deserialization

### Phase 2: Generic FilterStack Builder (Week 1-2)
1. Create `FilterStackBuilder` class with fluent API
2. Add extension functions to `FilterStack.Companion` for common patterns:
   - `forCurrency()`, `forIncome()`, `forExpense()`
   - `forAccount()`, `forAccountAndType()`, `forAccountAndCurrency()`
   - `forCategory()`, `forSearch()`, etc.
3. Add unit tests for builder and extension functions

### Phase 3: Navigation Integration (Week 2)
1. Update screen routes to accept `FilterStack` parameter
2. Create `navigateToRecords(filterStack)` extension
3. Create `navigateToAccountsView(filterStack)` extension
4. Update ViewModels to accept `initialFilterStack` parameter
5. Add `initializeFromFilterStack()` methods to ViewModels
6. Update NavGraph to decode FilterStack from route parameters

### Phase 4: Card Refactoring (Week 2-3)
1. Update `CashFlowCard` to use FilterStack extension functions
2. Update `CurrencyBalancesCard` to use FilterStack extension functions
3. Update `BudgetsCard` to use FilterStack extension functions
4. Update `AccountsCard` to use FilterStack extension functions
5. Update all screen usages (Dashboard, AccountDetail, AccountsView)
6. Remove old context parameters from cards
7. Deprecate old context classes

---

## Benefits (Simplified Approach)

### 1. **Maximum Code Reuse**
- **Single builder** for all navigation patterns
- **Extension functions** for common patterns
- **No duplication** across screens or cards

### 2. **Minimal Changes**
- Only add `@Serializable` to existing domain models
- Add one `FilterStackBuilder` class
- Add extension functions to `FilterStack.Companion`
- No new context sealed classes needed

### 3. **Type Safety**
- Strong typing via UnifiedFilter (no String/Double conversions)
- BigDecimal maintains precision
- Compile-time validation

### 4. **Simplicity**
- **No delegates** (over-engineering removed)
- **No screen-specific contexts** (use FilterStack directly)
- **No navigation actions** (direct FilterStack passing)
- Cards call extension functions directly

### 5. **Robustness**
- Leverages proven FilterStack infrastructure
- Single source of truth for filtering
- Consistent with existing filter mechanism
- Easy to test and maintain

### 6. **Extensibility**
- Adding new pattern: Add extension function
- Adding new screen: Use existing FilterStack builder
- Adding new filter type: Add to UnifiedFilter (single place)

---

## Migration Strategy (Simplified)

### Backward Compatibility

1. **Keep old context classes** as deprecated during migration
2. **Add conversion methods** from old contexts to FilterStack
3. **Gradual migration** card by card, screen by screen
4. **Remove old classes** after full migration

### Rollout Plan

1. **Phase 1**: Add FilterStack serialization (no breaking changes)
2. **Phase 2**: Add FilterStackBuilder and extensions (no breaking changes)
3. **Phase 3**: Update navigation infrastructure (breaking change)
4. **Phase 4**: Migrate cards (gradual, can be done incrementally)
5. **Phase 5**: Deprecate and remove old contexts

---

## Example: Adding New Navigation Pattern

### Before (Old Way - Screen-Specific Factory)
```kotlin
// Would need to create new factory object
object SomeScreenFilterStackFactory {
    fun forSomePattern(value: String): FilterStack = ...
}

// Then use it in cards
onClick = { onNavigate(SomeScreenFilterStackFactory.forSomePattern(value)) }
```

### After (Simplified Way - Extension Function)
```kotlin
// Just add one extension function
fun FilterStack.Companion.forSomePattern(value: String): FilterStack =
    FilterStackBuilder().someMethod(value).build()

// Use it in cards
onClick = { onNavigate(FilterStack.forSomePattern(value)) }
```

**Effort**: Reduced by ~70% - just add one extension function instead of creating a new factory

---

## Risk Assessment (Simplified)

### Low Risk
- Adding `@Serializable` is additive (no breaking changes)
- FilterStackBuilder is new code (no existing code affected)
- Extension functions are additive
- Proven FilterStack infrastructure

### Medium Risk
- FilterStack serialization complexity (BigDecimal, TimelineRange)
- Migration complexity for existing cards
- Route parameter changes

### Mitigation
- Custom serializers for complex types
- Gradual card-by-card migration
- Comprehensive testing
- Keep old contexts during migration period

---

## Success Metrics (Simplified)

1. **Code Reduction**: 60% reduction in navigation-related code
2. **Duplication**: 95% reduction in duplicate navigation logic
3. **Extensibility**: Adding new pattern takes 70% less time
4. **Test Coverage**: 95% coverage for FilterStack builder and extensions
5. **Bugs**: Zero navigation-related bugs after migration
6. **Performance**: Serialization overhead < 5ms per navigation
7. **Type Safety**: 100% compile-time type safety
8. **Simplicity**: Only 4 phases over 3 weeks (vs 9 phases over 5 weeks)

---

## Conclusion (Simplified & Robust)

This simplified architecture provides a robust solution for context navigation that:

- **Leverages existing FilterStack infrastructure** (single source of truth)
- **Uses a single generic builder** (no screen-specific factories)
- **Provides extension functions** for common patterns (maximum reusability)
- **Eliminates over-engineering** (no delegates, no screen-specific contexts)
- **Direct FilterStack integration** (cards use extension functions directly)
- **Minimal code changes** (only add serialization + builder + extensions)

**Key Advantage**: Maximum code reuse with minimal complexity. A single `FilterStackBuilder` with extension functions replaces multiple factories, delegates, and context wrappers. This is the simplest, most robust solution that eliminates duplication while leveraging the existing, proven FilterStack infrastructure.
