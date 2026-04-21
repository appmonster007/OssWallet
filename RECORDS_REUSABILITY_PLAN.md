# Records Page Reusability Plan

## Objective
Make the TransactionListScreen (Records page) reusable across different contexts with pre-applied filters when opened from various entry points. Also apply similar patterns to AccountsViewScreen and Dashboard screen for consistent navigation experience.

## Current State
- TransactionListScreen uses TransactionFilter from ViewModel
- No mechanism to pass initial filter criteria via navigation
- Each entry point would need to manually set filters after navigation
- No clear separation between "initial filter" (from context) and "user-applied filter" (from UI)
- TransactionFilterSheet is the UI component for filter modification
- AccountsViewScreen and Dashboard screen have similar filtering needs but no context-aware navigation

## Target State
- TransactionListScreen accepts navigation arguments for initial filter criteria
- Filters are automatically applied when screen opens
- TransactionFilterSheet respects initial context and shows pre-applied indicators
- Pre-applied filters are REMOVABLE - users can remove them if they choose
- Users can still modify filters via the UI
- Clean separation between context-provided filters and user-applied modifications
- Type-safe navigation with clear filter context
- AccountsViewScreen supports similar context-aware navigation
- Dashboard screen supports context-aware navigation to filtered views

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

### 6. Entry Point Implementations - TransactionListScreen

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

### 7. Entry Point Implementations - AccountsViewScreen

Create similar context-aware navigation for AccountsViewScreen:

```kotlin
sealed class AccountsFilterContext {
    data object None : AccountsFilterContext()
    data class ByType(val accountType: String) : AccountsFilterContext()
    data class ByCurrency(val currency: String) : AccountsFilterContext()
    data class Custom(val filter: AccountFilter) : AccountsFilterContext()
}
```

#### 7.1 Dashboard Account Type Cards

```kotlin
// In Dashboard, clicking on account type card (e.g., "Bank Accounts")
onClick = {
    navController.navigateToAccountsView(
        AccountsFilterContext.ByType("Bank")
    )
}
```

#### 7.2 Currency Balance Cards (Dashboard)

```kotlin
// In Dashboard currency balances section
onClick = { currency ->
    navController.navigateToAccountsView(
        AccountsFilterContext.ByCurrency(currency)
    )
}
```

### 8. Entry Point Implementations - Dashboard Screen

Create context-aware navigation for Dashboard screen to open with specific views focused:

```kotlin
sealed class DashboardViewContext {
    data object None : DashboardViewContext()
    data class FocusCard(val cardId: String) : DashboardViewContext()
    data class FocusSection(val section: DashboardSection) : DashboardViewContext()
}
```

#### 8.1 From Records Screen

```kotlin
// In Records screen, clicking on account in transaction
onClick = { accountId ->
    navController.navigateToDashboard(
        DashboardViewContext.FocusSection(DashboardSection.ACCOUNTS)
    )
}
```

#### 8.2 From Budgets Screen

```kotlin
// In Budgets screen, clicking on budget
onClick = {
    navController.navigateToDashboard(
        DashboardViewContext.FocusSection(DashboardSection.BUDGETS)
    )
}
```

### 9. UI Enhancements

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

### 10. TransactionFilterSheet Updates

Update TransactionFilterSheet to respect initial filter context and provide appropriate UI:

**Key Design Change:** Pre-applied filters are NOT locked - users can remove them if they choose. The UI shows which filters were pre-applied via navigation context with a navigation arrow icon, and provides a "Remove filter" chip for easy removal.

**Applied Filters Display Strategy:**

The TransactionFilterSheet should display applied filters in a clear, hierarchical manner:

1. **Context Header (Top)** - Shows the navigation context that brought user to this screen
   - Icon: FilterList icon
   - Text: Context description (e.g., "Account: Savings (Income)")
   - Action: Reset button to return to initial context

2. **Active Filters Summary** - Collapsible section showing all currently active filters
   - Shows count of active filters (e.g., "3 filters active")
   - Lists each active filter with its value
   - Distinguishes between pre-applied (with arrow icon) and user-applied filters
   - Each filter has a clear button to remove it individually

3. **Filter Sections (Interactive)** - Each filter type has its own section
   - Account Filter - Shows selected accounts with pre-applied indicator
   - Type Filter - Shows income/expense selection with pre-applied indicator
   - Currency Filter - Shows selected currencies with pre-applied indicator
   - Date Range Filter - Shows selected date range
   - Category Filter - Shows selected categories
   - Payment Method Filter - Shows selected payment methods
   - Note/Text Filter - Shows text search query
   - Amount Range Filter - Shows min/max amounts

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
    // Determine which filters were pre-applied based on context
    // These are NOT locked - users can remove them if they choose
    val preAppliedAccountId = when (initialFilterContext) {
        is RecordsFilterContext.ByAccount,
        is RecordsFilterContext.ByAccountAndType,
        is RecordsFilterContext.ByAccountAndCurrency -> {
            (initialFilterContext as? RecordsFilterContext.ByAccount)?.accountId
                ?: (initialFilterContext as? RecordsFilterContext.ByAccountAndType)?.accountId
                ?: (initialFilterContext as? RecordsFilterContext.ByAccountAndCurrency)?.accountId
        }
        else -> null
    }

    val preAppliedIsIncome = when (initialFilterContext) {
        is RecordsFilterContext.ByAccountAndType -> (initialFilterContext as RecordsFilterContext.ByAccountAndType).isIncome
        else -> null
    }

    val preAppliedCurrency = when (initialFilterContext) {
        is RecordsFilterContext.ByAccountAndCurrency -> (initialFilterContext as RecordsFilterContext.ByAccountAndCurrency).currency
        else -> null
    }

    // Count active filters
    val activeFilterCount = countActiveFilters(filter)

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

        // Active filters summary (collapsible)
        ActiveFiltersSummary(
            filter = filter,
            preAppliedAccountId = preAppliedAccountId,
            preAppliedIsIncome = preAppliedIsIncome,
            preAppliedCurrency = preAppliedCurrency,
            accountNames = accountNames,
            onRemoveFilter = { filterField, value ->
                removeFilter(filter, filterField, value, onUpdateFilter)
            }
        )

        HorizontalDivider()

        // Account filter section
        if (showAccountFilter) {
            // Show interactive account selector with pre-applied indicator
            AccountFilterSelector(
                selectedAccounts = filter.accountIds,
                preAppliedAccountId = preAppliedAccountId,
                accounts = accounts,
                onAccountsChanged = { accountIds ->
                    onUpdateFilter(filter.copy(accountIds = accountIds))
                }
            )
        }

        // Income/Expense filter section
        IncomeExpenseToggle(
            selectedType = filter.isIncome,
            preAppliedIsIncome = preAppliedIsIncome,
            onTypeChanged = { isIncome ->
                onUpdateFilter(filter.copy(isIncome = isIncome))
            }
        )

        // Currency filter section
        CurrencyFilterSelector(
            selectedCurrencies = filter.currencies,
            preAppliedCurrency = preAppliedCurrency,
            onCurrenciesChanged = { currencies ->
                onUpdateFilter(filter.copy(currencies = currencies))
            }
        )

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

**New Component: ActiveFiltersSummary**

```kotlin
@Composable
fun ActiveFiltersSummary(
    filter: TransactionFilter,
    preAppliedAccountId: String?,
    preAppliedIsIncome: Boolean?,
    preAppliedCurrency: String?,
    accountNames: Map<String, String>,
    onRemoveFilter: (String, Any?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = "Active filters",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${countActiveFilters(filter)} filters active",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Account filters
                filter.accountIds.forEach { accountId ->
                    ActiveFilterChip(
                        label = accountNames[accountId] ?: accountId,
                        isPreApplied = preAppliedAccountId == accountId,
                        onRemove = { onRemoveFilter("accountIds", accountId) }
                    )
                }

                // Type filter
                if (filter.isIncome != null) {
                    ActiveFilterChip(
                        label = if (filter.isIncome == true) "Income" else "Expense",
                        isPreApplied = preAppliedIsIncome == filter.isIncome,
                        onRemove = { onRemoveFilter("isIncome", null) }
                    )
                }

                // Currency filters
                filter.currencies.forEach { currency ->
                    ActiveFilterChip(
                        label = currency,
                        isPreApplied = preAppliedCurrency == currency,
                        onRemove = { onRemoveFilter("currencies", currency) }
                    )
                }

                // Date range filter
                if (filter.startDate != null || filter.endDate != null) {
                    ActiveFilterChip(
                        label = formatDateRange(filter.startDate, filter.endDate),
                        isPreApplied = false,
                        onRemove = { onRemoveFilter("dateRange", null) }
                    )
                }

                // Category filters
                filter.categoryIds.forEach { categoryId ->
                    ActiveFilterChip(
                        label = "Category: $categoryId",
                        isPreApplied = false,
                        onRemove = { onRemoveFilter("categoryIds", categoryId) }
                    )
                }

                // Other filters...
            }
        }
    }
}

@Composable
fun ActiveFilterChip(
    label: String,
    isPreApplied: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isPreApplied) {
                Icon(
                    imageVector = Icons.Default.NavigationArrow,
                    contentDescription = "Pre-applied",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove filter",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

fun countActiveFilters(filter: TransactionFilter): Int {
    var count = 0
    count += filter.accountIds.size
    if (filter.isIncome != null) count++
    count += filter.currencies.size
    if (filter.startDate != null || filter.endDate != null) count++
    count += filter.categoryIds.size
    count += filter.paymentMethodIds.size
    if (filter.note != null) count++
    if (filter.minAmount != null) count++
    if (filter.maxAmount != null) count++
    return count
}

fun removeFilter(
    filter: TransactionFilter,
    field: String,
    value: Any?,
    onUpdateFilter: (TransactionFilter) -> Unit
) {
    val updatedFilter = when (field) {
        "accountIds" -> filter.copy(accountIds = filter.accountIds - (value as String))
        "isIncome" -> filter.copy(isIncome = null)
        "currencies" -> filter.copy(currencies = filter.currencies - (value as String))
        "dateRange" -> filter.copy(startDate = null, endDate = null)
        "categoryIds" -> filter.copy(categoryIds = filter.categoryIds - (value as String))
        "paymentMethodIds" -> filter.copy(paymentMethodIds = filter.paymentMethodIds - (value as String))
        "note" -> filter.copy(note = null)
        "minAmount" -> filter.copy(minAmount = null)
        "maxAmount" -> filter.copy(maxAmount = null)
        else -> filter
    }
    onUpdateFilter(updatedFilter)
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Applied filter",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = getContextDisplayName(context),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        TextButton(onClick = onReset) {
            Text("Reset")
        }
    }
}

@Composable
fun AccountFilterSelector(
    selectedAccounts: List<String>,
    preAppliedAccountId: String?,
    accounts: List<Account>,
    onAccountsChanged: (List<String>) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleSmall
            )
            if (preAppliedAccountId != null && selectedAccounts.contains(preAppliedAccountId)) {
                AssistChip(
                    onClick = {
                        // Remove the pre-applied account
                        onAccountsChanged(selectedAccounts - preAppliedAccountId)
                    },
                    label = { Text("Remove filter") },
                    leadingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        // Show account chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { account ->
                FilterChip(
                    selected = selectedAccounts.contains(account.id),
                    onClick = {
                        if (selectedAccounts.contains(account.id)) {
                            onAccountsChanged(selectedAccounts - account.id)
                        } else {
                            onAccountsChanged(selectedAccounts + account.id)
                        }
                    },
                    label = { Text(account.name) },
                    leadingIcon = if (preAppliedAccountId == account.id && selectedAccounts.contains(account.id)) {
                        {
                            Icon(
                                Icons.Default.NavigationArrow,
                                contentDescription = "Pre-applied",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
fun IncomeExpenseToggle(
    selectedType: Boolean?,
    preAppliedIsIncome: Boolean?,
    onTypeChanged: (Boolean?) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Type",
                style = MaterialTheme.typography.titleSmall
            )
            if (preAppliedIsIncome != null && selectedType == preAppliedIsIncome) {
                AssistChip(
                    onClick = {
                        // Remove the pre-applied type filter
                        onTypeChanged(null)
                    },
                    label = { Text("Remove filter") },
                    leadingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == true,
                onClick = {
                    onTypeChanged(if (selectedType == true) null else true)
                },
                label = { Text("Income") },
                leadingIcon = if (preAppliedIsIncome == true && selectedType == true) {
                    {
                        Icon(
                            Icons.Default.NavigationArrow,
                            contentDescription = "Pre-applied",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
            FilterChip(
                selected = selectedType == false,
                onClick = {
                    onTypeChanged(if (selectedType == false) null else false)
                },
                label = { Text("Expense") },
                leadingIcon = if (preAppliedIsIncome == false && selectedType == false) {
                    {
                        Icon(
                            Icons.Default.NavigationArrow,
                            contentDescription = "Pre-applied",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
fun CurrencyFilterSelector(
    selectedCurrencies: List<String>,
    preAppliedCurrency: String?,
    onCurrenciesChanged: (List<String>) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Currency",
                style = MaterialTheme.typography.titleSmall
            )
            if (preAppliedCurrency != null && selectedCurrencies.contains(preAppliedCurrency)) {
                AssistChip(
                    onClick = {
                        // Remove the pre-applied currency
                        onCurrenciesChanged(selectedCurrencies - preAppliedCurrency)
                    },
                    label = { Text("Remove filter") },
                    leadingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        // Show currency chips with pre-applied indicator
        // Similar to AccountFilterSelector
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

### 11. Navigation Graph Update

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

// AccountsViewScreen navigation graph
composable(
    route = "${NavigationRoutes.ACCOUNTS_VIEW}?filterContext={filterContext}",
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
        Json.decodeFromString<AccountsFilterContext>(
            URLDecoder.decode(filterContextJson, StandardCharsets.UTF_8.toString())
        )
    } else {
        AccountsFilterContext.None
    }

    AccountsViewScreen(
        // ... other params
        initialFilterContext = filterContext
    )
}

// Dashboard navigation graph
composable(
    route = "${NavigationRoutes.DASHBOARD}?viewContext={viewContext}",
    arguments = listOf(
        navArgument("viewContext") {
            type = NavType.StringType
            defaultValue = null
            nullable = true
        }
    )
) { backStackEntry ->
    val viewContextJson = backStackEntry.arguments?.getString("viewContext")
    val viewContext = if (viewContextJson != null) {
        Json.decodeFromString<DashboardViewContext>(
            URLDecoder.decode(viewContextJson, StandardCharsets.UTF_8.toString())
        )
    } else {
        DashboardViewContext.None
    }

    DashboardScreen(
        // ... other params
        initialViewContext = viewContext
    )
}
```

## Implementation Phases

### Phase 1: Foundation (1-2 days)
- [ ] Create RecordsFilterContext sealed class
- [ ] Create AccountsFilterContext sealed class
- [ ] Create DashboardViewContext sealed class
- [ ] Add serialization support (kotlinx.serialization) for all contexts
- [ ] Add navigation routes with arguments for all screens
- [ ] Create navigation extensions for all screens

### Phase 2: ViewModel Enhancement (1 day)
- [ ] Add _initialFilterContext StateFlow
- [ ] Implement initializeRecordsFilter()
- [ ] Implement hasModifiedFilter()
- [ ] Implement resetToInitialFilter()

### Phase 3: TransactionFilterSheet Updates (1-2 days)
- [ ] Add context-aware parameters to TransactionFilterSheet
- [ ] Implement ContextIndicator component with filter icon
- [ ] Implement ActiveFiltersSummary component (collapsible)
- [ ] Implement ActiveFilterChip component with pre-applied indicator
- [ ] Implement countActiveFilters() helper function
- [ ] Implement removeFilter() helper function
- [ ] Implement AccountFilterSelector with pre-applied indicator and remove button
- [ ] Implement IncomeExpenseToggle with pre-applied indicator and remove button
- [ ] Implement CurrencyFilterSelector with pre-applied indicator and remove button
- [ ] Update filter sections to show pre-applied filters (NOT locked)
- [ ] Test filter removal functionality for each context type
- [ ] Test ActiveFiltersSummary expansion and filter removal

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

### Phase 7: AccountsViewScreen Implementation (1-2 days)
- [ ] Add _initialFilterContext StateFlow to AccountsViewModel
- [ ] Implement initializeAccountsFilter()
- [ ] Update AccountsViewScreen signature with context parameter
- [ ] Add LaunchedEffect for initial filter application
- [ ] Update account filter UI to show pre-applied indicators
- [ ] Implement navigation from Dashboard account type cards
- [ ] Implement navigation from Dashboard currency balance cards
- [ ] Test AccountsViewScreen context-aware navigation

### Phase 8: Dashboard Screen Implementation (1 day)
- [ ] Add _initialViewContext StateFlow to DashboardViewModel
- [ ] Implement initializeDashboardView()
- [ ] Update DashboardScreen signature with context parameter
- [ ] Add LaunchedEffect for initial view focus
- [ ] Implement section/card focus based on context
- [ ] Implement navigation from Records screen to Dashboard
- [ ] Implement navigation from Budgets screen to Dashboard
- [ ] Test Dashboard context-aware navigation

### Phase 9: Cross-Screen Integration & Polish (1 day)
- [ ] Test navigation flow between all three screens
- [ ] Ensure consistent UI patterns across screens
- [ ] Add context indicators to all screens
- [ ] Test deep linking with context parameters
- [ ] Verify back stack behavior with contexts
- [ ] Add unit tests for all context types
- [ ] Performance testing for context serialization

**Total Estimated Time: 10-13 days**

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
- **Solution**: Pass context as parameter, show pre-applied indicators with navigation arrow icon
- **Issue**: Should pre-applied filters be removable or locked?
- **Solution**: Pre-applied filters are REMOVABLE - users can remove them if they choose. Show "Remove filter" chip for easy removal.

### 7. Account Name Display
- **Issue**: Filter context shows accountId, but UI should show account name
- **Solution**: Pass account name in context or lookup from ViewModel

### 8. AccountsViewScreen Context Consistency
- **Issue**: How to handle account type vs currency filtering conflicts?
- **Solution**: Allow both filters to apply, show combined context indicator
- **Issue**: Should account type filter be removable?
- **Solution**: Yes, apply same removable pattern as TransactionListScreen

### 9. Dashboard View Focus
- **Issue**: How to visually indicate which section/card is focused?
- **Solution**: Use highlight animation or scroll to position with visual indicator
- **Issue**: What if focused card is not currently visible?
- **Solution**: Auto-scroll to focused card/section with smooth animation
- **Issue**: Should view focus persist when navigating away?
- **Solution**: Reset on fresh navigation, keep during screen changes

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
- [ ] AccountsFilterContext serialization/deserialization
- [ ] DashboardViewContext serialization/deserialization
- [ ] initializeRecordsFilter() for each context type
- [ ] initializeAccountsFilter() for each context type
- [ ] initializeDashboardView() for each context type
- [ ] hasModifiedFilter() edge cases
- [ ] resetToInitialFilter() behavior

### Integration Tests
- [ ] Navigation from hero card → correct filter applied (TransactionListScreen)
- [ ] Navigation from cashflow → correct filter applied (TransactionListScreen)
- [ ] Navigation from currency balance → correct filter applied (TransactionListScreen)
- [ ] Navigation from account type card → correct filter applied (AccountsViewScreen)
- [ ] Navigation from currency balance → correct filter applied (AccountsViewScreen)
- [ ] Navigation from Records → correct section focused (Dashboard)
- [ ] Navigation from Budgets → correct section focused (Dashboard)
- [ ] Filter modification → hasModifiedFilter() returns true
- [ ] Reset button → returns to initial context
- [ ] Back navigation → filter state preserved

### UI Tests
- [ ] Filter chip displays correct context name (all screens)
- [ ] Reset button appears when filter modified (all screens)
- [ ] Filter sheet shows initial values correctly
- [ ] User can add additional filters on top of initial context
- [ ] Pre-applied filter indicators show navigation arrow icon
- [ ] "Remove filter" chip works correctly
- [ ] ActiveFiltersSummary shows correct filter count
- [ ] ActiveFiltersSummary expands/collapses correctly
- [ ] ActiveFilterChip shows pre-applied indicator correctly
- [ ] Individual filter removal from summary works
- [ ] Dashboard scrolls to focused section/card
- [ ] Focused section has visual highlight

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
