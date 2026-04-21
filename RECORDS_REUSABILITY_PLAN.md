# Records Page Reusability Plan

## Objective
Make the TransactionListScreen (Records page) reusable across different contexts with pre-applied filters when opened from various entry points.

## Current State
- TransactionListScreen uses TransactionFilter from ViewModel
- No mechanism to pass initial filter criteria via navigation
- Each entry point would need to manually set filters after navigation
- No clear separation between "initial filter" (from context) and "user-applied filter" (from UI)
- TransactionFilterSheet is the UI component for filter modification

## Target State
- TransactionListScreen accepts navigation arguments for initial filter criteria
- Filters are automatically applied when screen opens
- TransactionFilterSheet respects initial context and shows appropriate UI
- Users can still modify filters via the UI
- Clean separation between context-provided filters and user-applied modifications
- Type-safe navigation with clear filter context

## Architecture Design

### 1. Navigation Model

Create a sealed class to represent different filter contexts:

```kotlin
sealed class RecordsFilterContext {
    data object None : RecordsFilterContext()
    data class ByAccount(val accountId: String) : RecordsFilterContext()
    data class ByAccountAndType(
        val accountId: String,
        val isIncome: Boolean
    ) : RecordsFilterContext()
    data class ByAccountAndCurrency(
        val accountId: String,
        val currency: String
    ) : RecordsFilterContext()
    data class Custom(val filter: TransactionFilter) : RecordsFilterContext()
}
```

### 2. Navigation Route

Add type-safe navigation argument:

```kotlin
// NavigationRoutes.kt
object NavigationRoutes {
    const val RECORDS = "records"
    const val RECORDS_WITH_FILTER = "records?filterContext={filterContext}"
}
```

### 3. ViewModel Enhancement

Add initialization method to TransactionViewModel:

```kotlin
class TransactionViewModel {
    private val _initialFilterContext = MutableStateFlow<RecordsFilterContext>(RecordsFilterContext.None)
    val initialFilterContext: StateFlow<RecordsFilterContext> = _initialFilterContext.asStateFlow()

    /**
     * Initialize records filter from navigation context
     * Called when screen opens with a filter context
     */
    fun initializeRecordsFilter(context: RecordsFilterContext) {
        val initialFilter = when (context) {
            is RecordsFilterContext.None -> TransactionFilter()
            is RecordsFilterContext.ByAccount -> 
                TransactionFilter(accountIds = listOf(context.accountId))
            is RecordsFilterContext.ByAccountAndType ->
                TransactionFilter(
                    accountIds = listOf(context.accountId),
                    isIncome = context.isIncome
                )
            is RecordsFilterContext.ByAccountAndCurrency ->
                TransactionFilter(
                    accountIds = listOf(context.accountId),
                    currencies = listOf(context.currency)
                )
            is RecordsFilterContext.Custom -> context.filter
        }
        
        _recordsFilter.value = initialFilter
        _initialFilterContext.value = context
    }

    /**
     * Check if current filter differs from initial context
     * Used to show/hide "Reset to default" option
     */
    fun hasModifiedFilter(): Boolean {
        val current = _recordsFilter.value
        val initial = when (_initialFilterContext.value) {
            is RecordsFilterContext.None -> TransactionFilter()
            is RecordsFilterContext.ByAccount -> 
                TransactionFilter(accountIds = listOf((_initialFilterContext.value as RecordsFilterContext.ByAccount).accountId))
            is RecordsFilterContext.ByAccountAndType -> {
                val ctx = _initialFilterContext.value as RecordsFilterContext.ByAccountAndType
                TransactionFilter(
                    accountIds = listOf(ctx.accountId),
                    isIncome = ctx.isIncome
                )
            }
            is RecordsFilterContext.ByAccountAndCurrency -> {
                val ctx = _initialFilterContext.value as RecordsFilterContext.ByAccountAndCurrency
                TransactionFilter(
                    accountIds = listOf(ctx.accountId),
                    currencies = listOf(ctx.currency)
                )
            }
            is RecordsFilterContext.Custom -> (_initialFilterContext.value as RecordsFilterContext.Custom).filter
        }
        return current != initial
    }

    /**
     * Reset filter to initial context
     */
    fun resetToInitialFilter() {
        initializeRecordsFilter(_initialFilterContext.value)
    }
}
```

### 4. Screen Integration

Update TransactionListScreen to accept and apply filter context:

```kotlin
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    accountViewModel: AccountViewModel,
    analyticsViewModel: AnalyticsViewModel,
    onAddTransactionClick: (Int?, String?) -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onAccountDetailClick: (String) -> Unit,
    templateViewModel: TemplateViewModel,
    // New parameter for navigation context
    initialFilterContext: RecordsFilterContext = RecordsFilterContext.None
) {
    val filter by viewModel.recordsFilter.collectAsStateWithLifecycle()
    
    // Apply initial filter when screen opens
    LaunchedEffect(initialFilterContext) {
        viewModel.initializeRecordsFilter(initialFilterContext)
    }

    // ... rest of the screen
}
```

### 5. Navigation Implementation

Create navigation extensions for each context:

```kotlin
// NavigationExtensions.kt
fun NavHostController.navigateToRecords(
    context: RecordsFilterContext
) {
    val encodedContext = URLEncoder.encode(
        Json.encodeToString(RecordsFilterContext.serializer(), context),
        StandardCharsets.UTF_8.toString()
    )
    navigate("${NavigationRoutes.RECORDS}?filterContext=$encodedContext")
}

// Usage examples:
// From account hero card:
navController.navigateToRecords(RecordsFilterContext.ByAccount(accountId))

// From cashflow income:
navController.navigateToRecords(
    RecordsFilterContext.ByAccountAndType(accountId, isIncome = true)
)

// From currency balance:
navController.navigateToRecords(
    RecordsFilterContext.ByAccountAndCurrency(accountId, currency)
)
```

### 6. Entry Point Implementations

#### 6.1 Account Hero Card (AccountDetailScreen)

```kotlin
// In AccountDetailScreen, hero card click handler
onClick = {
    navController.navigateToRecords(
        RecordsFilterContext.ByAccount(accountId)
    )
}
```

#### 6.2 Cashflow Income/Expense (AccountDetailScreen)

```kotlin
// In cash flow section
onClickIncome = {
    navController.navigateToRecords(
        RecordsFilterContext.ByAccountAndType(accountId, isIncome = true)
    )
}

onClickExpense = {
    navController.navigateToRecords(
        RecordsFilterContext.ByAccountAndType(accountId, isIncome = false)
    )
}
```

#### 6.3 Currency Balances (Dashboard or AccountDetailScreen)

```kotlin
// In currency balance card
onClick = { currency ->
    navController.navigateToRecords(
        RecordsFilterContext.ByAccountAndCurrency(accountId, currency)
    )
}
```

### 7. UI Enhancements

Add visual indicator when filter is applied from context:

```kotlin
// In TransactionListScreen
if (viewModel.initialFilterContext.value != RecordsFilterContext.None) {
    FilterChip(
        selected = true,
        onClick = { viewModel.resetToInitialFilter() },
        label = { Text("Reset to ${getContextDisplayName(viewModel.initialFilterContext.value)}") }
    )
}

private fun getContextDisplayName(context: RecordsFilterContext): String {
    return when (context) {
        is RecordsFilterContext.None -> "All"
        is RecordsFilterContext.ByAccount -> "Account: ${context.accountId}"
        is RecordsFilterContext.ByAccountAndType ->
            "Account: ${context.accountId} (${if (context.isIncome) "Income" else "Expense"})"
        is RecordsFilterContext.ByAccountAndCurrency ->
            "Account: ${context.accountId} (${context.currency})"
        is RecordsFilterContext.Custom -> "Custom"
    }
}
```

### 8. TransactionFilterSheet Updates

Update TransactionFilterSheet to respect initial filter context and provide appropriate UI:

```kotlin
@Composable
fun TransactionFilterSheet(
    filter: TransactionFilter,
    accounts: List<Account>,
    categories: List<Category>,
    savedFilters: List<FilterEntity>,
    onSetDateRange: (Long?, Long?) -> Unit,
    onUpdateFilter: (TransactionFilter) -> Unit,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
    isExpanded: Boolean,
    backgroundColor: Color,
    // New parameters for context awareness
    initialFilterContext: RecordsFilterContext = RecordsFilterContext.None,
    accountNames: Map<String, String> = emptyMap(),
    showAccountFilter: Boolean = true
) {
    // Determine which filters should be locked/disabled based on context
    val lockAccountId = when (initialFilterContext) {
        is RecordsFilterContext.ByAccount,
        is RecordsFilterContext.ByAccountAndType,
        is RecordsFilterContext.ByAccountAndCurrency -> {
            (initialFilterContext as? RecordsFilterContext.ByAccount)?.accountId
                ?: (initialFilterContext as? RecordsFilterContext.ByAccountAndType)?.accountId
                ?: (initialFilterContext as? RecordsFilterContext.ByAccountAndCurrency)?.accountId
        }
        else -> null
    }

    val lockIsIncome = when (initialFilterContext) {
        is RecordsFilterContext.ByAccountAndType -> (initialFilterContext as RecordsFilterContext.ByAccountAndType).isIncome
        else -> null
    }

    val lockCurrency = when (initialFilterContext) {
        is RecordsFilterContext.ByAccountAndCurrency -> (initialFilterContext as RecordsFilterContext.ByAccountAndCurrency).currency
        else -> null
    }

    Column(modifier = Modifier.background(backgroundColor)) {
        // Context indicator header
        if (initialFilterContext != RecordsFilterContext.None) {
            ContextIndicator(
                context = initialFilterContext,
                accountNames = accountNames,
                onReset = { /* Trigger reset to initial context */ }
            )
            HorizontalDivider()
        }

        // Account filter section
        if (showAccountFilter && lockAccountId == null) {
            // Show interactive account selector
            AccountFilterSelector(
                selectedAccounts = filter.accountIds,
                accounts = accounts,
                onAccountsChanged = { accountIds ->
                    onUpdateFilter(filter.copy(accountIds = accountIds))
                }
            )
        } else if (showAccountFilter && lockAccountId != null) {
            // Show locked account indicator
            LockedAccountIndicator(
                accountId = lockAccountId,
                accountName = accountNames[lockAccountId] ?: lockAccountId
            )
        }

        // Income/Expense filter section
        if (lockIsIncome == null) {
            // Show interactive income/expense toggle
            IncomeExpenseToggle(
                selectedType = filter.isIncome,
                onTypeChanged = { isIncome ->
                    onUpdateFilter(filter.copy(isIncome = isIncome))
                }
            )
        } else {
            // Show locked type indicator
            LockedTypeIndicator(
                isIncome = lockIsIncome
            )
        }

        // Currency filter section
        if (lockCurrency == null) {
            // Show interactive currency selector
            CurrencyFilterSelector(
                selectedCurrencies = filter.currencies,
                onCurrenciesChanged = { currencies ->
                    onUpdateFilter(filter.copy(currencies = currencies))
                }
            )
        } else {
            // Show locked currency indicator
            LockedCurrencyIndicator(
                currency = lockCurrency
            )
        }

        // Other filters (date range, category, payment method, etc.)
        // These remain interactive regardless of context
        DateRangeFilter(
            startDate = filter.startDate,
            endDate = filter.endDate,
            onDateRangeChanged = { start, end ->
                onSetDateRange(start, end)
            }
        )

        CategoryFilterSelector(
            selectedCategories = filter.categoryIds,
            categories = categories,
            onCategoriesChanged = { categoryIds ->
                onUpdateFilter(filter.copy(categoryIds = categoryIds))
            }
        )

        // ... other filter sections
    }
}
```

**New UI Components for TransactionFilterSheet:**

```kotlin
@Composable
fun ContextIndicator(
    context: RecordsFilterContext,
    accountNames: Map<String, String>,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = getContextDisplayName(context),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        TextButton(onClick = onReset) {
            Text("Reset")
        }
    }
}

@Composable
fun LockedAccountIndicator(
    accountId: String,
    accountName: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Account: $accountName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LockedTypeIndicator(
    isIncome: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        FilterChip(
            selected = isIncome,
            onClick = { },
            label = { Text("Income") },
            enabled = false
        )
        FilterChip(
            selected = !isIncome,
            onClick = { },
            label = { Text("Expense") },
            enabled = false
        )
    }
}

@Composable
fun LockedCurrencyIndicator(
    currency: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Currency: $currency",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

**Updated TransactionListScreen Integration:**

```kotlin
@Composable
fun TransactionListScreen(
    // ... existing parameters
    initialFilterContext: RecordsFilterContext = RecordsFilterContext.None
) {
    val filter by viewModel.recordsFilter.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val accountMap = accounts.associate { it.id to it.name }

    // Apply initial filter when screen opens
    LaunchedEffect(initialFilterContext) {
        viewModel.initializeRecordsFilter(initialFilterContext)
    }

    // ... rest of screen

    TransactionFilterSheet(
        filter = filter,
        accounts = accounts,
        categories = categories,
        savedFilters = savedFilters,
        onSetDateRange = { start, end -> viewModel.setRecordsDateRangeFilter(start, end) },
        onUpdateFilter = { viewModel.updateRecordsFilter(it) },
        onToggleExpand = { /* ... */ },
        onClose = { /* ... */ },
        isExpanded = /* ... */,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        // New parameters
        initialFilterContext = viewModel.initialFilterContext.collectAsStateWithLifecycle().value,
        accountNames = accountMap,
        showAccountFilter = initialFilterContext !is RecordsFilterContext.ByAccount &&
                          initialFilterContext !is RecordsFilterContext.ByAccountAndType &&
                          initialFilterContext !is RecordsFilterContext.ByAccountAndCurrency
    )
}
```

### 9. Navigation Graph Update

```kotlin
// NavGraph.kt
composable(
    route = "${NavigationRoutes.RECORDS}?filterContext={filterContext}",
    arguments = listOf(
        navArgument("filterContext") {
            type = NavType.StringType
            defaultValue = null
            nullable = true
        }
    )
) { backStackEntry ->
    val filterContextJson = backStackEntry.arguments?.getString("filterContext")
    val filterContext = if (filterContextJson != null) {
        Json.decodeFromString<RecordsFilterContext>(
            URLDecoder.decode(filterContextJson, StandardCharsets.UTF_8.toString())
        )
    } else {
        RecordsFilterContext.None
    }
    
    TransactionListScreen(
        // ... other params
        initialFilterContext = filterContext
    )
}
```

## Implementation Phases

### Phase 1: Foundation (1-2 days)
- [ ] Create RecordsFilterContext sealed class
- [ ] Add serialization support (kotlinx.serialization)
- [ ] Add navigation route with argument
- [ ] Create navigation extensions

### Phase 2: ViewModel Enhancement (1 day)
- [ ] Add _initialFilterContext StateFlow
- [ ] Implement initializeRecordsFilter()
- [ ] Implement hasModifiedFilter()
- [ ] Implement resetToInitialFilter()

### Phase 3: TransactionFilterSheet Updates (1-2 days)
- [ ] Add context-aware parameters to TransactionFilterSheet
- [ ] Implement ContextIndicator component
- [ ] Implement LockedAccountIndicator component
- [ ] Implement LockedTypeIndicator component
- [ ] Implement LockedCurrencyIndicator component
- [ ] Update filter sections to respect locked fields
- [ ] Test with each context type

### Phase 4: Screen Integration (1 day)
- [ ] Update TransactionListScreen signature
- [ ] Add LaunchedEffect for initial filter
- [ ] Add filter context display chip
- [ ] Pass context to TransactionFilterSheet
- [ ] Test with None context (default behavior)

### Phase 5: Entry Point Integration (1-2 days)
- [ ] Update AccountDetailScreen hero card
- [ ] Update AccountDetailScreen cashflow sections
- [ ] Update currency balance cards
- [ ] Test each navigation path

### Phase 6: Polish & Testing (1 day)
- [ ] Add context display names
- [ ] Test filter modification and reset
- [ ] Test back navigation behavior
- [ ] Verify filter persistence during navigation
- [ ] Add unit tests for filter context logic
- [ ] Test TransactionFilterSheet locked states

**Total Estimated Time: 6-8 days**

## Edge Cases & Considerations

### 1. Filter Modification
- **Issue**: User modifies filter, how to distinguish from initial context?
- **Solution**: Track initial context separately, compare with current filter

### 2. Back Navigation
- **Issue**: Should filters persist when returning to records screen?
- **Solution**: Keep current filter state, re-apply initial context only on fresh navigation

### 3. Multiple Entry Points
- **Issue**: What if user opens records from different contexts in quick succession?
- **Solution**: Each navigation event re-initializes based on new context

### 4. Deep Linking
- **Issue**: Can we support deep links with filter context?
- **Solution**: Yes, URL-encoded JSON in navigation argument

### 5. Filter Combination
- **Issue**: What if user combines initial context with additional filters?
- **Solution**: Allow modification, provide reset button to return to initial context

### 6. TransactionFilterSheet UI Consistency
- **Issue**: How to ensure TransactionFilterSheet shows correct UI for each context?
- **Solution**: Pass context as parameter, lock relevant filter sections, show locked indicators
- **Issue**: Should locked filters be visible but disabled, or hidden entirely?
- **Solution**: Show as locked indicators with lock icon for clarity, users can see what's filtered

### 7. Account Name Display
- **Issue**: Filter context shows accountId, but UI should show account name
- **Solution**: Pass account name in context or lookup from ViewModel

## Alternative Approaches Considered

### Approach A: Direct Filter Parameter
- Pass TransactionFilter directly as navigation argument
- **Pros**: Simple, direct
- **Cons**: Type-safety issues, no context awareness, harder to reset

### Approach B: Intent-Based Navigation
- Use Android Intent extras with filter criteria
- **Pros**: Native Android pattern
- **Cons**: Tightly coupled to Android, not Compose-friendly

### Approach C: Shared ViewModel
- Use shared ViewModel between screens to pass filter
- **Pros**: No serialization needed
- **Cons**: Tight coupling, lifecycle issues, not navigation-friendly

**Selected Approach**: Sealed class context (current plan) - best balance of type-safety, clarity, and Compose navigation compatibility.

## Testing Strategy

### Unit Tests
- [ ] RecordsFilterContext serialization/deserialization
- [ ] initializeRecordsFilter() for each context type
- [ ] hasModifiedFilter() edge cases
- [ ] resetToInitialFilter() behavior

### Integration Tests
- [ ] Navigation from hero card → correct filter applied
- [ ] Navigation from cashflow → correct filter applied
- [ ] Navigation from currency balance → correct filter applied
- [ ] Filter modification → hasModifiedFilter() returns true
- [ ] Reset button → returns to initial context
- [ ] Back navigation → filter state preserved

### UI Tests
- [ ] Filter chip displays correct context name
- [ ] Reset button appears when filter modified
- [ ] Filter sheet shows initial values correctly
- [ ] User can add additional filters on top of initial context

## Performance Considerations

- Filter context is lightweight (sealed class with simple data)
- Serialization cost is negligible (small JSON)
- No additional database queries (filter applied in existing flow)
- StateFlow ensures efficient recomposition

## Migration Impact

- **Breaking Changes**: None (new parameter has default value)
- **Existing Call Sites**: Continue to work (use default None context)
- **New Functionality**: Opt-in via navigation extensions

## Future Enhancements

1. **Saved Filter Contexts**: Allow saving filter contexts as "quick filters"
2. **Filter History**: Track recent filter contexts for quick access
3. **Context Sharing**: Share filter context via deep links
4. **Analytics**: Track which contexts are most used
5. **Filter Templates**: Pre-defined contexts for common use cases

## Success Criteria

- [ ] All 4 entry points (hero, income, expense, currency) work correctly
- [ ] Filters are applied automatically when screen opens
- [ ] Users can modify filters from initial context
- [ ] Reset button returns to initial context
- [ ] No breaking changes to existing navigation
- [ ] Clean separation between context and user modifications
- [ ] Type-safe navigation with compile-time safety
