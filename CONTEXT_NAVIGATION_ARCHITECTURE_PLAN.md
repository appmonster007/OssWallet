# Context Navigation Architecture Plan (Unified with FilterStack)

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
- FilterStack infrastructure exists but not leveraged for navigation contexts

#### 2. **Tight Coupling**
- Cards directly create context objects with manual parameter construction
- Navigation logic embedded in card components
- Hard to test and maintain
- FilterStack not used in navigation layer despite being the domain filter standard

#### 3. **Inconsistency**
- Some cards use specific context types (ByAccount, ByAccountAndType)
- Others use generic Custom context with manual parameter lists
- No clear pattern for when to use which
- FilterStack exists in domain but navigation contexts use different structure

#### 4. **Limited Extensibility**
- Adding new screen requires creating new context sealed class
- Adding new card navigation requires modifying multiple files
- No abstraction for common navigation patterns
- FilterStack's composition capabilities not utilized for navigation

#### 5. **Type Safety Issues**
- Custom context accepts arbitrary parameters but no validation
- CategoryIds stored as String for serialization, converted to Long elsewhere
- Amounts stored as Double, converted to BigDecimal elsewhere
- FilterStack has strong typing but navigation contexts bypass it

#### 6. **Missing Integration**
- FilterStack serialization support missing for navigation
- No direct mapping between context types and FilterStack
- Context parameters manually converted to TransactionFilter instead of using FilterStack.toTransactionFilter()
- Existing UnifiedFilter types not leveraged for navigation

#### 7. **Missing Contexts**
- No context for LoansScreen navigation
- No context for AnalyticsScreen navigation
- No context for BudgetsScreen navigation with pre-filtered budget

---

## Proposed Architecture (Unified with FilterStack)

### Core Principles

1. **Reuse Existing Infrastructure**: Leverage FilterStack, UnifiedFilter, and LogicalOperator
2. **Separation of Concerns**: Cards should not know about navigation logic
3. **Type Safety**: Strong typing via UnifiedFilter types
4. **Composition**: Build complex FilterStacks using existing composition
5. **Extensibility**: Easy to add new screens using FilterStack factories
6. **Testability**: Pure functions for FilterStack creation
7. **Single Source of Truth**: FilterStack is the domain filter standard, use it everywhere

### Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                     UI Layer (Screens)                        │
│  - DashboardScreen, AccountsViewScreen, AccountDetailScreen  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Uses
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Navigation Context Factory                       │
│  - RecordsFilterStackFactory (Builds FilterStack)            │
│  - AccountsFilterStackFactory (Builds FilterStack)           │
│  - BudgetsFilterStackFactory (Builds FilterStack) (NEW)      │
│  - LoansFilterStackFactory (Builds FilterStack) (NEW)        │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Builds
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
│  - CompositeFilter, SavedFilterReference, InstantFilter      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Encodes/Decodes
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Navigation Extensions                            │
│  - NavHostController.navigateToRecords(filterStack)        │
│  - NavHostController.navigateToBudgets(filterStack)          │
│  - Serialization adapters for FilterStack                   │
└─────────────────────────────────────────────────────────────┘
```

---

## Detailed Design (Unified with FilterStack)

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
    // ... existing filters with @Serializable annotation

    @Serializable
    data class TextFilter(
        val query: String,
        val isRegex: Boolean = false,
        val isCaseInsensitive: Boolean = true,
        val operator: FilterOperator = FilterOperator.CONTAINS
    ) : UnifiedFilter()

    @Serializable
    data class CategoryFilter(
        val categoryIds: List<Long>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()

    @Serializable
    data class AccountFilter(
        val accountIds: List<String>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()

    @Serializable
    data class DateRangeFilter(
        val timelineRange: TimelineRange
    ) : UnifiedFilter()

    @Serializable
    data class AmountFilter(
        val min: BigDecimal? = null,
        val max: BigDecimal? = null
    ) : UnifiedFilter()

    @Serializable
    data class PaymentMethodFilter(
        val methods: List<String>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()

    @Serializable
    data class CurrencyFilter(
        val currencies: List<String>,
        val operator: FilterOperator = FilterOperator.IN
    ) : UnifiedFilter()

    @Serializable
    data class IncomeTypeFilter(
        val isIncome: Boolean? = null
    ) : UnifiedFilter()

    @Serializable
    data class WarrantyFilter(
        val hasWarranty: Boolean? = null,
        val expirationStart: Long? = null,
        val expirationEnd: Long? = null
    ) : UnifiedFilter()

    @Serializable
    data class CompositeFilter(
        val filters: List<UnifiedFilter>,
        val operator: LogicalOperator = LogicalOperator.AND
    ) : UnifiedFilter()

    @Serializable
    data class SavedFilterReference(
        val filterId: Long
    ) : UnifiedFilter()

    @Serializable
    data class InstantFilter(
        val filter: UnifiedFilter
    ) : UnifiedFilter()
}
```

### 2. FilterStack Factory Pattern

#### RecordsFilterStackFactory
```kotlin
/**
 * Factory for creating FilterStack instances for transaction navigation.
 * Leverages existing UnifiedFilter types and FilterStack composition.
 */
object RecordsFilterStackFactory {
    // Common patterns using UnifiedFilter

    fun forCurrency(currency: String): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.CurrencyFilter(listOf(currency))
            )
        )

    fun forIncome(): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.IncomeTypeFilter(isIncome = true)
            )
        )

    fun forExpense(): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.IncomeTypeFilter(isIncome = false)
            )
        )

    fun forAccount(accountId: String): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.AccountFilter(listOf(accountId))
            )
        )

    fun forAccountAndType(accountId: String, isIncome: Boolean): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.AccountFilter(listOf(accountId)),
                UnifiedFilter.IncomeTypeFilter(isIncome = isIncome)
            ),
            logicalOperator = LogicalOperator.AND
        )

    fun forAccountAndCurrency(accountId: String, currency: String): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.AccountFilter(listOf(accountId)),
                UnifiedFilter.CurrencyFilter(listOf(currency))
            ),
            logicalOperator = LogicalOperator.AND
        )

    fun forCategory(categoryId: Long): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.CategoryFilter(listOf(categoryId))
            )
        )

    fun forCategories(categoryIds: List<Long>): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.CategoryFilter(categoryIds)
            )
        )

    fun forDateRange(timelineRange: TimelineRange): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.DateRangeFilter(timelineRange)
            )
        )

    fun forAmountRange(min: BigDecimal?, max: BigDecimal?): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.AmountFilter(min = min, max = max)
            )
        )

    fun forSearch(query: String): FilterStack =
        FilterStack(
            activeFilters = listOf(
                UnifiedFilter.TextFilter(query)
            )
        )

    // Custom builder for complex filters
    fun build(builder: RecordsFilterStackBuilder.() -> Unit): FilterStack =
        RecordsFilterStackBuilder().apply(builder).build()

    // Combine multiple FilterStacks
    fun combine(stacks: List<FilterStack>, operator: LogicalOperator = LogicalOperator.AND): FilterStack {
        val allFilters = stacks.flatMap { it.activeFilters }
        return FilterStack(allFilters, operator)
    }
}

class RecordsFilterStackBuilder {
    private val filters = mutableListOf<UnifiedFilter>()
    private var operator: LogicalOperator = LogicalOperator.AND

    fun accountIds(ids: List<String>) {
        filters.add(UnifiedFilter.AccountFilter(ids))
    }

    fun categoryIds(ids: List<Long>) {
        filters.add(UnifiedFilter.CategoryFilter(ids))
    }

    fun isIncome(isIncome: Boolean) {
        filters.add(UnifiedFilter.IncomeTypeFilter(isIncome = isIncome))
    }

    fun currencies(currencies: List<String>) {
        filters.add(UnifiedFilter.CurrencyFilter(currencies))
    }

    fun dateRange(timelineRange: TimelineRange) {
        filters.add(UnifiedFilter.DateRangeFilter(timelineRange))
    }

    fun amountRange(min: BigDecimal?, max: BigDecimal?) {
        filters.add(UnifiedFilter.AmountFilter(min = min, max = max))
    }

    fun search(query: String) {
        filters.add(UnifiedFilter.TextFilter(query))
    }

    fun paymentMethods(methods: List<String>) {
        filters.add(UnifiedFilter.PaymentMethodFilter(methods))
    }

    fun withOperator(op: LogicalOperator) {
        this.operator = op
    }

    fun build(): FilterStack = FilterStack(filters.toList(), operator)
}
```

#### AccountsFilterStackFactory
```kotlin
/**
 * Factory for creating FilterStack instances for account navigation.
 * Note: Accounts use different filter criteria than transactions.
 */
object AccountsFilterStackFactory {
    fun forType(accountType: String): FilterStack =
        FilterStack(
            activeFilters = listOf(
                // AccountTypeFilter would need to be added to UnifiedFilter
                // For now, use a generic filter or add AccountTypeFilter to UnifiedFilter
                UnifiedFilter.AccountFilter(listOf()) // Placeholder
            )
        )

    fun forCurrency(currency: String): FilterStack =
        FilterStack(
            activeFilters = listOf(
                // CurrencyFilter for accounts would need specific implementation
                UnifiedFilter.CurrencyFilter(listOf(currency))
            )
        )

    fun build(builder: AccountsFilterStackBuilder.() -> Unit): FilterStack =
        AccountsFilterStackBuilder().apply(builder).build()
}

class AccountsFilterStackBuilder {
    private val filters = mutableListOf<UnifiedFilter>()
    private var operator: LogicalOperator = LogicalOperator.AND

    fun accountTypes(types: List<String>) {
        // Would need AccountTypeFilter in UnifiedFilter
        // For now, filter by account IDs after fetching
    }

    fun currencies(currencies: List<String>) {
        filters.add(UnifiedFilter.CurrencyFilter(currencies))
    }

    fun balanceRange(min: BigDecimal?, max: BigDecimal?) {
        // Would need BalanceRangeFilter in UnifiedFilter
    }

    fun withOperator(op: LogicalOperator) {
        this.operator = op
    }

    fun build(): FilterStack = FilterStack(filters.toList(), operator)
}
```

### 3. Screen-Specific Context Wrappers

#### RecordsNavigationContext
```kotlin
/**
 * Navigation context for RecordsScreen using FilterStack.
 * This replaces the existing RecordsFilterContext.
 */
@Serializable
sealed class RecordsNavigationContext {
    @Serializable
    data object None : RecordsNavigationContext()

    @Serializable
    data class WithFilterStack(
        val filterStack: FilterStack
    ) : RecordsNavigationContext()

    // Convenience methods for backward compatibility
    fun toFilterStack(): FilterStack = when (this) {
        is None -> FilterStack.Empty
        is WithFilterStack -> filterStack
    }
}
```

#### AccountsNavigationContext
```kotlin
/**
 * Navigation context for AccountsViewScreen using FilterStack.
 * This replaces the existing AccountsFilterContext.
 */
@Serializable
sealed class AccountsNavigationContext {
    @Serializable
    data object None : AccountsNavigationContext()

    @Serializable
    data class WithFilterStack(
        val filterStack: FilterStack
    ) : AccountsNavigationContext()

    fun toFilterStack(): FilterStack = when (this) {
        is None -> FilterStack.Empty
        is WithFilterStack -> filterStack
    }
}
```

### 4. Card Navigation Delegates

#### CardNavigationDelegate Interface
```kotlin
interface CardNavigationDelegate {
    fun onCurrencyClick(currency: String): NavigationAction
    fun onIncomeClick(): NavigationAction
    fun onExpenseClick(): NavigationAction
    fun onAccountClick(accountId: String): NavigationAction
    fun onCategoryClick(categoryId: Long): NavigationAction
    fun onBudgetClick(category: String): NavigationAction
}

sealed class NavigationAction {
    data class NavigateToRecords(val filterStack: FilterStack) : NavigationAction()
    data class NavigateToBudgets(val filterStack: FilterStack) : NavigationAction()
    data class NavigateToLoans(val filterStack: FilterStack) : NavigationAction()
    data class NavigateToAnalytics(val filterStack: FilterStack) : NavigationAction()
    data class NavigateToEditBudget(val category: String) : NavigationAction()
}
```

#### Screen-Specific Delegates

```kotlin
// DashboardNavigationDelegate
class DashboardNavigationDelegate : CardNavigationDelegate {
    override fun onCurrencyClick(currency: String) =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forCurrency(currency)
        )

    override fun onIncomeClick() =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forIncome()
        )

    override fun onExpenseClick() =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forExpense()
        )

    override fun onAccountClick(accountId: String) =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forAccount(accountId)
        )

    override fun onCategoryClick(categoryId: Long) =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forCategory(categoryId)
        )

    override fun onBudgetClick(category: String) =
        NavigationAction.NavigateToEditBudget(category)
}

// AccountDetailNavigationDelegate
class AccountDetailNavigationDelegate(
    private val accountId: String
) : CardNavigationDelegate {
    override fun onCurrencyClick(currency: String) =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forAccountAndCurrency(accountId, currency)
        )

    override fun onIncomeClick() =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forAccountAndType(accountId, isIncome = true)
        )

    override fun onExpenseClick() =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forAccountAndType(accountId, isIncome = false)
        )

    override fun onAccountClick(accountId: String) =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forAccount(accountId)
        )

    override fun onCategoryClick(categoryId: Long) =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.build {
                accountIds(listOf(accountId))
                categoryIds(listOf(categoryId))
            }
        )

    override fun onBudgetClick(category: String) =
        NavigationAction.NavigateToEditBudget(category)
}

// AccountsViewNavigationDelegate
class AccountsViewNavigationDelegate : CardNavigationDelegate {
    override fun onCurrencyClick(currency: String) =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forCurrency(currency)
        )

    override fun onIncomeClick() =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forIncome()
        )

    override fun onExpenseClick() =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forExpense()
        )

    override fun onAccountClick(accountId: String) =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forAccount(accountId)
        )

    override fun onCategoryClick(categoryId: Long) =
        NavigationAction.NavigateToRecords(
            RecordsFilterStackFactory.forCategory(categoryId)
        )

    override fun onBudgetClick(category: String) =
        NavigationAction.NavigateToEditBudget(category)
}
```

### 5. Navigation Extensions

#### FilterStack Navigation Extensions
```kotlin
/**
 * Navigate to RecordsScreen with FilterStack context.
 */
fun NavHostController.navigateToRecords(filterStack: FilterStack) {
    val context = RecordsNavigationContext.WithFilterStack(filterStack)
    val encodedContext = URLEncoder.encode(
        Json.encodeToString(context),
        StandardCharsets.UTF_8.toString()
    )
    navigate(Screen.Records.createRoute(encodedContext))
}

/**
 * Navigate to AccountsViewScreen with FilterStack context.
 */
fun NavHostController.navigateToAccountsView(filterStack: FilterStack) {
    val context = AccountsNavigationContext.WithFilterStack(filterStack)
    val encodedContext = URLEncoder.encode(
        Json.encodeToString(context),
        StandardCharsets.UTF_8.toString()
    )
    navigate(Screen.AccountsView.createRoute(encodedContext))
}

/**
 * Navigate to BudgetsScreen with FilterStack context.
 */
fun NavHostController.navigateToBudgets(filterStack: FilterStack) {
    // Similar pattern for budgets
}
```

#### Context Conversion in ViewModels
```kotlin
// In TransactionListScreen ViewModel
fun initializeFromContext(context: RecordsNavigationContext) {
    val filterStack = context.toFilterStack()
    val transactionFilter = filterStack.toTransactionFilter()
    applyFilter(transactionFilter)
}

// In AccountsViewScreen ViewModel
fun initializeFromContext(context: AccountsNavigationContext) {
    val filterStack = context.toFilterStack()
    // Convert to account-specific filter
    applyAccountsFilter(filterStack)
}
```

### 6. Extension for New Screens

#### Adding BudgetsScreen Context
```kotlin
/**
 * Navigation context for BudgetsScreen using FilterStack.
 */
@Serializable
sealed class BudgetsNavigationContext {
    @Serializable
    data object None : BudgetsNavigationContext()

    @Serializable
    data class WithFilterStack(
        val filterStack: FilterStack
    ) : BudgetsNavigationContext()

    @Serializable
    data class ByCategory(val category: String) : BudgetsNavigationContext()

    fun toFilterStack(): FilterStack = when (this) {
        is None -> FilterStack.Empty
        is WithFilterStack -> filterStack
        is ByCategory -> FilterStack(
            activeFilters = listOf(
                // Would need CategoryFilter for budgets
                UnifiedFilter.CategoryFilter(listOf())
            )
        )
    }
}

object BudgetsFilterStackFactory {
    fun forCategory(category: String): FilterStack =
        FilterStack(
            activeFilters = listOf(
                // Budget-specific filter
            )
        )

    fun build(builder: BudgetsFilterStackBuilder.() -> Unit): FilterStack =
        BudgetsFilterStackBuilder().apply(builder).build()
}
```

---

## Implementation Plan (Unified with FilterStack)

### Phase 1: FilterStack Serialization (Week 1)
1. Add `@Serializable` annotation to `FilterStack`
2. Add `@Serializable` annotation to all `UnifiedFilter` types
3. Add `@Serializable` annotation to `LogicalOperator` enum
4. Add serialization support for `TimelineRange` (if needed)
5. Add serialization support for `BigDecimal` (using custom serializer)
6. Add unit tests for serialization/deserialization

### Phase 2: FilterStack Factories (Week 1-2)
1. Create `RecordsFilterStackFactory` with common patterns:
   - `forCurrency()`, `forIncome()`, `forExpense()`
   - `forAccount()`, `forAccountAndType()`, `forAccountAndCurrency()`
   - `forCategory()`, `forCategories()`, `forDateRange()`, `forAmountRange()`, `forSearch()`
2. Create `RecordsFilterStackBuilder` for complex filters
3. Create `AccountsFilterStackFactory` with account-specific patterns
4. Create `AccountsFilterStackBuilder` for complex account filters
5. Add unit tests for factories and builders

### Phase 3: Navigation Context Wrappers (Week 2)
1. Create `RecordsNavigationContext` sealed class:
   - `None`, `WithFilterStack(filterStack)`
   - Add `toFilterStack()` conversion method
2. Create `AccountsNavigationContext` sealed class:
   - `None`, `WithFilterStack(filterStack)`
   - Add `toFilterStack()` conversion method
3. Add backward compatibility adapters:
   - Convert old `RecordsFilterContext` to `RecordsNavigationContext`
   - Convert old `AccountsFilterContext` to `AccountsNavigationContext`
4. Add unit tests for context conversion

### Phase 4: Navigation Delegates (Week 2-3)
1. Create `CardNavigationDelegate` interface
2. Create `NavigationAction` sealed class with FilterStack
3. Implement `DashboardNavigationDelegate`:
   - Uses `RecordsFilterStackFactory` for all patterns
4. Implement `AccountDetailNavigationDelegate`:
   - Includes accountId in all filters
5. Implement `AccountsViewNavigationDelegate`:
   - Similar to Dashboard but for accounts view
6. Add unit tests for delegates

### Phase 5: Card Refactoring (Week 3)
1. Refactor `CashFlowCard`:
   - Add `navigationDelegate` parameter
   - Add `onAction` callback
   - Remove direct context creation
2. Refactor `CurrencyBalancesCard`:
   - Add `navigationDelegate` parameter
   - Add `onAction` callback
3. Refactor `BudgetsCard`:
   - Add `navigationDelegate` parameter
   - Add `onAction` callback
4. Refactor `AccountsCard`:
   - Add `navigationDelegate` parameter
   - Add `onAction` callback
5. Update all screen usages:
   - DashboardScreen: Create delegate, pass to cards
   - AccountDetailScreen: Create delegate, pass to cards
   - AccountsViewScreen: Create delegate, pass to cards

### Phase 6: Navigation Extensions (Week 3-4)
1. Create `navigateToRecords(filterStack)` extension:
   - Convert FilterStack to RecordsNavigationContext
   - Encode and navigate
2. Create `navigateToAccountsView(filterStack)` extension:
   - Convert FilterStack to AccountsNavigationContext
   - Encode and navigate
3. Update NavGraph to use new navigation methods
4. Update ViewModels to handle new context format:
   - `initializeFromContext()` using FilterStack
   - Convert FilterStack to TransactionFilter via `toTransactionFilter()`

### Phase 7: Context Migration (Week 4)
1. Migrate `RecordsFilterContext` usage:
   - Replace old context creation with factory calls
   - Update context encoding/decoding
2. Migrate `AccountsFilterContext` usage:
   - Replace old context creation with factory calls
   - Update context encoding/decoding
3. Deprecate old context classes:
   - Mark as `@Deprecated`
   - Add migration notes
4. Update all navigation calls:
   - Use new FilterStack-based navigation
5. Add integration tests for navigation flows

### Phase 8: New Contexts (Week 4-5)
1. Create `BudgetsNavigationContext`:
   - `None`, `WithFilterStack(filterStack)`, `ByCategory(category)`
2. Create `BudgetsFilterStackFactory`:
   - `forCategory()`, `forStatus()`, etc.
3. Create `LoansNavigationContext`:
   - Similar pattern for loans
4. Create `LoansFilterStackFactory`:
   - Loan-specific filter patterns
5. Add navigation extensions for new contexts
6. Add unit and integration tests

### Phase 9: Documentation & Testing (Week 5)
1. Document FilterStack serialization:
   - How to add new filter types
   - Custom serializers for complex types
2. Document factory usage:
   - Common patterns
   - Builder pattern usage
3. Document delegate implementation:
   - How to create screen-specific delegates
   - How to handle custom navigation actions
4. Add comprehensive integration tests:
   - End-to-end navigation flows
   - Context serialization/deserialization
   - ViewModel context handling
5. Add performance tests:
   - Serialization overhead
   - FilterStack composition performance

---

## Benefits (Unified with FilterStack)

### 1. **Leverages Existing Infrastructure**
- Reuses FilterStack, UnifiedFilter, and LogicalOperator
- No duplication of filter types
- Single source of truth for filtering logic
- Uses existing `toTransactionFilter()` conversion

### 2. **Reduced Duplication**
- Common navigation patterns defined once in factories
- Delegates reused across screens
- No repeated context creation logic
- FilterStack composition eliminates manual filter combination

### 3. **Type Safety**
- Strong typing via UnifiedFilter types
- Compile-time validation
- No runtime type errors
- BigDecimal maintains precision (no Double conversion issues)

### 4. **Extensibility**
- Adding new screen: Create context wrapper + factory + delegate
- Adding new navigation pattern: Add method to factory
- Adding new filter type: Add to UnifiedFilter (single place)
- No need to modify existing cards or contexts

### 5. **Testability**
- Pure functions for FilterStack creation
- Delegates can be unit tested
- FilterStack serialization can be tested
- Mock delegates for UI testing

### 6. **Maintainability**
- Clear separation of concerns
- Single source of truth for navigation logic
- FilterStack is the domain filter standard, used everywhere
- Easy to locate and modify navigation patterns

### 7. **Consistency**
- All cards use same navigation pattern via delegates
- All contexts built using FilterStack factories
- Uniform API across screens
- Consistent with existing filter mechanism

### 8. **Performance**
- FilterStack composition is optimized
- Serialization is efficient
- No redundant conversions
- Leverages existing FilterStack optimizations

---

## Migration Strategy (Unified with FilterStack)

### Backward Compatibility

1. **Keep old context classes** as deprecated:
   - `RecordsFilterContext` → marked `@Deprecated("Use RecordsNavigationContext with FilterStack")`
   - `AccountsFilterContext` → marked `@Deprecated("Use AccountsNavigationContext with FilterStack")`

2. **Add conversion methods**:
   - `RecordsFilterContext.toNavigationContext(): RecordsNavigationContext`
   - `AccountsFilterContext.toNavigationContext(): AccountsNavigationContext`
   - Convert old Custom contexts to FilterStack using factories

3. **Gradual migration** screen by screen:
   - DashboardScreen first (most impact)
   - AccountsViewScreen second
   - AccountDetailScreen third
   - Other screens as needed

4. **Remove old classes** after full migration:
   - After all screens migrated
   - After testing confirms no issues
   - After deprecation period (at least one release)

### Rollout Plan

1. **Phase 1**: Add FilterStack serialization support (no breaking changes)
2. **Phase 2**: Create factories and context wrappers (no breaking changes)
3. **Phase 3**: Migrate DashboardScreen (breaking change, but backward compatible via adapters)
4. **Phase 4**: Migrate AccountsViewScreen (breaking change)
5. **Phase 5**: Migrate AccountDetailScreen (breaking change)
6. **Phase 6**: Deprecate old context classes
7. **Phase 7**: Remove old context classes (after deprecation period)

---

## Example: Adding New Screen

### Before (Current - without FilterStack)
1. Create new context sealed class with custom parameters
2. Add context parameter to screen
3. Add navigation extension with encoding/decoding
4. Modify cards to create context with manual parameter construction
5. Update NavGraph
6. Add conversion logic to TransactionFilter

### After (Proposed - with FilterStack)
1. Create new context wrapper (e.g., `LoansNavigationContext`):
   ```kotlin
   @Serializable
   sealed class LoansNavigationContext {
       data object None : LoansNavigationContext()
       data class WithFilterStack(val filterStack: FilterStack) : LoansNavigationContext()
   }
   ```

2. Create factory for FilterStack (e.g., `LoansFilterStackFactory`):
   ```kotlin
   object LoansFilterStackFactory {
       fun forLoanType(type: String): FilterStack = FilterStack(...)
       fun forLoanStatus(status: String): FilterStack = FilterStack(...)
   }
   ```

3. Create delegate for screen (if cards need navigation):
   ```kotlin
   class LoansNavigationDelegate : CardNavigationDelegate {
       override fun onLoanClick(loanId: String) =
           NavigationAction.NavigateToLoans(LoansFilterStackFactory.forLoan(loanId))
   }
   ```

4. Add navigation extension:
   ```kotlin
   fun NavHostController.navigateToLoans(filterStack: FilterStack) {
       val context = LoansNavigationContext.WithFilterStack(filterStack)
       // encode and navigate
   }
   ```

5. Pass delegate to cards (if needed)
6. Update NavGraph with new route

**Effort**: Reduced by ~50% due to:
- Reusing FilterStack infrastructure
- Reusing UnifiedFilter types
- Reusing serialization logic
- Reusing `toTransactionFilter()` conversion
- No need to create custom filter types

---

## Risk Assessment (Unified with FilterStack)

### Low Risk
- Adding FilterStack serialization is additive (no breaking changes)
- FilterStack infrastructure is already proven and tested
- Factory pattern is well-established
- Delegates are simple interfaces
- UnifiedFilter types are already used throughout the app

### Medium Risk
- FilterStack serialization complexity (BigDecimal, TimelineRange)
- Migration complexity for existing contexts
- Ensuring all navigation flows work with new approach
- Testing all edge cases in serialization

### High Risk
- Breaking changes if serialization fails
- Performance impact of serialization
- Backward compatibility issues during migration

### Mitigation
- Comprehensive unit tests for serialization/deserialization
- Custom serializers for complex types (BigDecimal, TimelineRange)
- Integration tests for navigation flows
- Gradual rollout with backward compatibility adapters
- Performance testing for serialization overhead
- Rollback plan (keep old contexts during migration)
- Feature flags for new navigation system

---

## Success Metrics (Unified with FilterStack)

1. **Code Reduction**: 40% reduction in navigation-related code (reuses FilterStack)
2. **Duplication**: 90% reduction in duplicate navigation logic (factories + delegates)
3. **Extensibility**: Adding new screen takes 60% less time (reuses FilterStack infrastructure)
4. **Test Coverage**: 95% coverage for navigation logic (factories, delegates, serialization)
5. **Bugs**: Zero navigation-related bugs after migration
6. **Performance**: Serialization overhead < 5ms per navigation
7. **Type Safety**: 100% compile-time type safety (no String/Double conversions)
8. **Integration**: 100% integration with existing FilterStack infrastructure

---

## Conclusion (Unified with FilterStack)

This architecture provides a robust, extensible solution for context navigation that:

- **Leverages existing infrastructure**: Reuses FilterStack, UnifiedFilter, and LogicalOperator
- **Eliminates duplication**: Common patterns defined once in factories
- **Provides type safety**: Strong typing via UnifiedFilter (no String/Double conversions)
- **Enables easy addition**: New screens reuse FilterStack infrastructure
- **Improves testability**: Pure functions for FilterStack creation
- **Maintains backward compatibility**: Gradual migration with adapters
- **Single source of truth**: FilterStack is the domain filter standard everywhere

The FilterStack-based approach combined with factories and delegates creates a clear separation between UI, navigation logic, and domain filter models, making the codebase more maintainable and scalable while fully leveraging the existing, proven filter infrastructure.

**Key Advantage**: This approach unifies navigation contexts with the existing FilterStack mechanism, eliminating the need for separate filter models and ensuring consistency across the entire application.
