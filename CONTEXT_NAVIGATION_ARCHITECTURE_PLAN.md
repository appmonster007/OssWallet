# Context Navigation Architecture Plan

## Current State Analysis

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
- No reuse of context creation logic

#### 2. **Tight Coupling**
- Cards directly create context objects
- Navigation logic embedded in card components
- Hard to test and maintain

#### 3. **Inconsistency**
- Some cards use specific context types (ByAccount, ByAccountAndType)
- Others use generic Custom context
- No clear pattern for when to use which

#### 4. **Limited Extensibility**
- Adding new screen requires creating new context sealed class
- Adding new card navigation requires modifying multiple files
- No abstraction for common navigation patterns

#### 5. **Type Safety Issues**
- Custom context accepts arbitrary parameters but no validation
- CategoryIds stored as String for serialization, converted to Long elsewhere
- Amounts stored as Double, converted to BigDecimal elsewhere

#### 6. **Missing Contexts**
- No context for LoansScreen navigation
- No context for AnalyticsScreen navigation
- No context for BudgetsScreen navigation with pre-filtered budget

---

## Proposed Architecture

### Core Principles

1. **Separation of Concerns**: Cards should not know about navigation logic
2. **Type Safety**: Strong typing for all navigation parameters
3. **Composition**: Build complex contexts from simple building blocks
4. **Extensibility**: Easy to add new screens and contexts
5. **Testability**: Pure functions for context creation

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
│  - RecordsContextFactory                                     │
│  - AccountsContextFactory                                    │
│  - BudgetsContextFactory (NEW)                               │
│  - LoansContextFactory (NEW)                                │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Builds
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Navigation Context Models                        │
│  - RecordsFilterContext (Refactored)                         │
│  - AccountsFilterContext (Refactored)                        │
│  - BudgetsFilterContext (NEW)                                │
│  - LoansFilterContext (NEW)                                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Encodes/Decodes
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Navigation Extensions                            │
│  - NavHostController.navigateToRecords(context)              │
│  - NavHostController.navigateToBudgets(context)              │
└─────────────────────────────────────────────────────────────┘
```

---

## Detailed Design

### 1. Unified Context Building Blocks

#### FilterCriteria (Base Interface)
```kotlin
@Serializable
sealed class FilterCriteria {
    data object None : FilterCriteria()
    
    data class ById(val id: String) : FilterCriteria()
    data class ByIds(val ids: List<String>) : FilterCriteria()
    data class ByType(val type: String) : FilterCriteria()
    data class ByTypes(val types: List<String>) : FilterCriteria()
    data class ByRange(
        val min: Double? = null,
        val max: Double? = null
    ) : FilterCriteria()
    data class ByDateRange(
        val startDate: Long? = null,
        val endDate: Long? = null
    ) : FilterCriteria()
    data class ByQuery(val query: String) : FilterCriteria()
}
```

#### ScreenContext (Generic Base)
```kotlin
@Serializable
sealed class ScreenContext<T> {
    data object None : ScreenContext<Nothing>()
    
    data class Single(
        val criteria: FilterCriteria
    ) : ScreenContext<T>()
    
    data class Composite(
        val criteria: List<FilterCriteria>,
        val operator: LogicalOperator = LogicalOperator.AND
    ) : ScreenContext<T>()
}

enum class LogicalOperator { AND, OR }
```

### 2. Context Factory Pattern

#### RecordsContextFactory
```kotlin
object RecordsContextFactory {
    // Common patterns
    fun forCurrency(currency: String): RecordsFilterContext =
        RecordsFilterContext.Composite(
            criteria = listOf(FilterCriteria.ByIds(listOf(currency))),
            operator = LogicalOperator.AND
        )
    
    fun forIncome(): RecordsFilterContext =
        RecordsFilterContext.Composite(
            criteria = listOf(FilterCriteria.ByType("income")),
            operator = LogicalOperator.AND
        )
    
    fun forExpense(): RecordsFilterContext =
        RecordsFilterContext.Composite(
            criteria = listOf(FilterCriteria.ByType("expense")),
            operator = LogicalOperator.AND
        )
    
    fun forAccount(accountId: String): RecordsFilterContext =
        RecordsFilterContext.Composite(
            criteria = listOf(FilterCriteria.ById(accountId)),
            operator = LogicalOperator.AND
        )
    
    fun forAccountAndType(accountId: String, isIncome: Boolean): RecordsFilterContext =
        RecordsFilterContext.Composite(
            criteria = listOf(
                FilterCriteria.ById(accountId),
                FilterCriteria.ByType(if (isIncome) "income" else "expense")
            ),
            operator = LogicalOperator.AND
        )
    
    fun forAccountAndCurrency(accountId: String, currency: String): RecordsFilterContext =
        RecordsFilterContext.Composite(
            criteria = listOf(
                FilterCriteria.ById(accountId),
                FilterCriteria.ByIds(listOf(currency))
            ),
            operator = LogicalOperator.AND
        )
    
    // Custom builder
    fun build(builder: RecordsContextBuilder.() -> Unit): RecordsFilterContext =
        RecordsContextBuilder().apply(builder).build()
}

class RecordsContextBuilder {
    private val criteria = mutableListOf<FilterCriteria>()
    private var operator: LogicalOperator = LogicalOperator.AND
    
    fun accountIds(ids: List<String>) {
        criteria.add(FilterCriteria.ByIds(ids))
    }
    
    fun isIncome(isIncome: Boolean) {
        criteria.add(FilterCriteria.ByType(if (isIncome) "income" else "expense"))
    }
    
    fun currencies(currencies: List<String>) {
        criteria.add(FilterCriteria.ByIds(currencies))
    }
    
    fun dateRange(start: Long?, end: Long?) {
        criteria.add(FilterCriteria.ByDateRange(start, end))
    }
    
    fun amountRange(min: Double?, max: Double?) {
        criteria.add(FilterCriteria.ByRange(min, max))
    }
    
    fun query(query: String) {
        criteria.add(FilterCriteria.ByQuery(query))
    }
    
    fun operator(op: LogicalOperator) {
        this.operator = op
    }
    
    fun build(): RecordsFilterContext =
        RecordsFilterContext.Composite(criteria.toList(), operator)
}
```

### 3. Card Navigation Delegates

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
    data class NavigateToRecords(val context: RecordsFilterContext) : NavigationAction()
    data class NavigateToBudgets(val context: BudgetsFilterContext) : NavigationAction()
    data class NavigateToLoans(val context: LoansFilterContext) : NavigationAction()
    data class NavigateToAnalytics(val context: AnalyticsFilterContext) : NavigationAction()
    data class NavigateToEditBudget(val category: String) : NavigationAction()
}
```

#### Screen-Specific Delegates

```kotlin
// DashboardNavigationDelegate
class DashboardNavigationDelegate(
    private val onRecordsClick: (RecordsFilterContext) -> Unit,
    private val onEditBudgetClick: (String) -> Unit
) : CardNavigationDelegate {
    override fun onCurrencyClick(currency: String) =
        NavigationAction.NavigateToRecords(
            RecordsContextFactory.forCurrency(currency)
        )
    
    override fun onIncomeClick() =
        NavigationAction.NavigateToRecords(
            RecordsContextFactory.forIncome()
        )
    
    override fun onExpenseClick() =
        NavigationAction.NavigateToRecords(
            RecordsContextFactory.forExpense()
        )
    
    override fun onAccountClick(accountId: String) =
        NavigationAction.NavigateToRecords(
            RecordsContextFactory.forAccount(accountId)
        )
    
    override fun onBudgetClick(category: String) =
        NavigationAction.NavigateToEditBudget(category)
    
    override fun onCategoryClick(categoryId: Long) =
        NavigationAction.NavigateToRecords(
            RecordsContextFactory.build { 
                accountIds(listOf(categoryId.toString()))
            }
        )
}

// AccountDetailNavigationDelegate
class AccountDetailNavigationDelegate(
    private val accountId: String,
    private val onRecordsClick: (RecordsFilterContext) -> Unit
) : CardNavigationDelegate {
    override fun onCurrencyClick(currency: String) =
        NavigationAction.NavigateToRecords(
            RecordsContextFactory.forAccountAndCurrency(accountId, currency)
        )
    
    override fun onIncomeClick() =
        NavigationAction.NavigateToRecords(
            RecordsContextFactory.forAccountAndType(accountId, isIncome = true)
        )
    
    override fun onExpenseClick() =
        NavigationAction.NavigateToRecords(
            RecordsContextFactory.forAccountAndType(accountId, isIncome = false)
        )
    
    override fun onAccountClick(accountId: String) =
        NavigationAction.NavigateToRecords(
            RecordsContextFactory.forAccount(accountId)
        )
    
    // ... other methods
}
```

### 4. Card Component Refactoring

#### Before (Current)
```kotlin
CashFlowCard(
    onClickIncome = { onRecordsClick(RecordsFilterContext.Custom(isIncome = true)) },
    onClickExpense = { onRecordsClick(RecordsFilterContext.Custom(isIncome = false)) }
)
```

#### After (Proposed)
```kotlin
CashFlowCard(
    navigationDelegate = navigationDelegate
)

// Inside CashFlowCard
@Composable
fun CashFlowCard(
    navigationDelegate: CardNavigationDelegate,
    // ... other parameters
) {
    // ...
    onClickIncome = { 
        when (val action = navigationDelegate.onIncomeClick()) {
            is NavigationAction.NavigateToRecords -> onRecordsClick(action.context)
            else -> {}
        }
    }
}
```

Or even simpler with action handler:

```kotlin
CashFlowCard(
    navigationDelegate = navigationDelegate,
    onAction = { action ->
        when (action) {
            is NavigationAction.NavigateToRecords -> onRecordsClick(action.context)
            is NavigationAction.NavigateToEditBudget -> onEditBudgetClick(action.category)
            // ... other actions
        }
    }
)
```

### 5. Extension for New Screens

#### Adding BudgetsScreen Context
```kotlin
// New file: BudgetsFilterContext.kt
@Serializable
sealed class BudgetsFilterContext {
    data object None : BudgetsFilterContext()
    
    data class ByCategory(val category: String) : BudgetsFilterContext()
    
    data class ByStatus(val status: BudgetStatus) : BudgetsFilterContext()
    
    data class Custom(
        val categories: List<String> = emptyList(),
        val statuses: List<BudgetStatus> = emptyList(),
        val minProgress: Double? = null,
        val maxProgress: Double? = null
    ) : BudgetsFilterContext()
}

// BudgetsContextFactory
object BudgetsContextFactory {
    fun forCategory(category: String): BudgetsFilterContext =
        BudgetsFilterContext.ByCategory(category)
    
    fun build(builder: BudgetsContextBuilder.() -> Unit): BudgetsFilterContext =
        BudgetsContextBuilder().apply(builder).build()
}
```

---

## Implementation Plan

### Phase 1: Foundation (Week 1)
1. Create `FilterCriteria` sealed class
2. Create `ScreenContext` generic base
3. Create `LogicalOperator` enum
4. Add unit tests for context building

### Phase 2: Context Factories (Week 1-2)
1. Create `RecordsContextFactory` with all common patterns
2. Create `AccountsContextFactory` with all common patterns
3. Create builder classes for complex contexts
4. Add unit tests for factories

### Phase 3: Navigation Delegates (Week 2)
1. Create `CardNavigationDelegate` interface
2. Create `NavigationAction` sealed class
3. Implement `DashboardNavigationDelegate`
4. Implement `AccountDetailNavigationDelegate`
5. Implement `AccountsViewNavigationDelegate`
6. Add unit tests for delegates

### Phase 4: Card Refactoring (Week 3)
1. Refactor `CashFlowCard` to use delegate
2. Refactor `CurrencyBalancesCard` to use delegate
3. Refactor `BudgetsCard` to use delegate
4. Refactor `AccountsCard` to use delegate
5. Update all screen usages

### Phase 5: Context Migration (Week 3-4)
1. Migrate existing `RecordsFilterContext` to use `FilterCriteria`
2. Migrate existing `AccountsFilterContext` to use `FilterCriteria`
3. Update context encoding/decoding logic
4. Update view models to handle new context format
5. Add backward compatibility layer

### Phase 6: New Contexts (Week 4)
1. Create `BudgetsFilterContext`
2. Create `LoansFilterContext`
3. Create `AnalyticsFilterContext`
4. Add corresponding factories
5. Add navigation extensions

### Phase 7: Documentation & Testing (Week 4-5)
1. Document all context types
2. Document factory usage patterns
3. Document delegate implementation guide
4. Add integration tests
5. Add navigation flow tests

---

## Benefits

### 1. **Reduced Duplication**
- Common navigation patterns defined once in factories
- Delegates reused across screens
- No repeated context creation logic

### 2. **Type Safety**
- Strong typing for all filter criteria
- Compile-time validation
- No runtime type errors

### 3. **Extensibility**
- Adding new screen: Create context + factory + delegate
- Adding new navigation pattern: Add method to factory
- No need to modify existing cards

### 4. **Testability**
- Pure functions for context building
- Delegates can be unit tested
- Mock delegates for UI testing

### 5. **Maintainability**
- Clear separation of concerns
- Single source of truth for navigation logic
- Easy to locate and modify navigation patterns

### 6. **Consistency**
- All cards use same navigation pattern
- All contexts built using same factory pattern
- Uniform API across screens

---

## Migration Strategy

### Backward Compatibility

1. **Keep old context classes** as deprecated
2. **Add conversion methods** between old and new formats
3. **Gradual migration** screen by screen
4. **Remove old classes** after full migration

### Rollout Plan

1. **Phase 1**: Implement new architecture alongside old
2. **Phase 2**: Migrate DashboardScreen
3. **Phase 3**: Migrate AccountsViewScreen
4. **Phase 4**: Migrate AccountDetailScreen
5. **Phase 5**: Remove old implementation

---

## Example: Adding New Screen

### Before (Current)
1. Create new context sealed class
2. Add context parameter to screen
3. Add navigation extension
4. Modify cards to create context
5. Update NavGraph

### After (Proposed)
1. Create new context sealed class
2. Create factory for context
3. Create delegate for screen
4. Pass delegate to cards
5. Update NavGraph

**Effort**: Reduced by ~40% due to reusable patterns

---

## Risk Assessment

### Low Risk
- Adding new architecture alongside old
- Factory pattern is well-established
- Delegates are simple interfaces

### Medium Risk
- Migration complexity
- Ensuring all navigation flows work
- Testing all edge cases

### Mitigation
- Comprehensive unit tests
- Integration tests for navigation flows
- Gradual rollout with feature flags
- Rollback plan

---

## Success Metrics

1. **Code Reduction**: 30% reduction in navigation-related code
2. **Duplication**: 80% reduction in duplicate navigation logic
3. **Extensibility**: Adding new screen takes 50% less time
4. **Test Coverage**: 90% coverage for navigation logic
5. **Bugs**: Zero navigation-related bugs after migration

---

## Conclusion

This architecture provides a robust, extensible solution for context navigation that:

- Eliminates duplication across screens
- Provides type-safe context building
- Enables easy addition of new screens and cards
- Improves testability and maintainability
- Maintains backward compatibility during migration

The factory pattern combined with delegates creates a clear separation between UI, navigation logic, and context models, making the codebase more maintainable and scalable.
