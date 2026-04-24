# Best Practices: Configurable ViewModel Cards with Lists

## Scenario
You have a ViewModel card that displays a list of information. The card has an updatable configuration that determines what data to show (e.g., timeline range, filters, sorting, etc.). The question is: **Where is the best place to use the config to update the information shown in the list?**

## Three Architectural Approaches

### Approach 1: ViewModel-Level Filtering (Push-Based)

**Pattern:** ViewModel filters data based on config, pushes filtered result to UI

```kotlin
// ViewModel
class CardViewModel : ViewModel() {
    private val _config = MutableStateFlow<CardConfig>(defaultConfig)
    val config: StateFlow<CardConfig> = _config.asStateFlow()
    
    val filteredItems: StateFlow<List<Item>> = _config
        .flatMapLatest { config ->
            allItems.map { items ->
                items.filter { item ->
                    matchesConfig(item, config)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun updateConfig(newConfig: CardConfig) {
        _config.value = newConfig
    }
}

// UI
@Composable
fun CardScreen(viewModel: CardViewModel) {
    val items by viewModel.filteredItems.collectAsStateWithLifecycle()
    
    LazyColumn {
        items(items) { item ->
            ItemRow(item)
        }
    }
}
```

**When to Use:**
- Data filtering is complex business logic
- Multiple UI components need the same filtered data
- Filtering requires access to repositories or use cases
- Performance is critical (filter once, use everywhere)
- Filtering logic should be testable independently of UI

**Pros:**
- Single source of truth for filtered data
- Efficient (filter once, multiple consumers)
- Testable business logic in ViewModel
- UI remains dumb (just displays what ViewModel provides)
- Consistent filtering across all UI components

**Cons:**
- ViewModel becomes more complex
- Tight coupling between ViewModel and filtering logic
- Harder to customize per-component
- Less flexible for UI-specific transformations

---

### Approach 2: UI-Level Filtering (Pull-Based)

**Pattern:** ViewModel provides raw data, UI filters based on config

```kotlin
// ViewModel
class CardViewModel : ViewModel() {
    private val _config = MutableStateFlow<CardConfig>(defaultConfig)
    val config: StateFlow<CardConfig> = _config.asStateFlow()
    
    val allItems: StateFlow<List<Item>> = itemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun updateConfig(newConfig: CardConfig) {
        _config.value = newConfig
    }
}

// UI
@Composable
fun CardScreen(viewModel: CardViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    
    // Filter at UI level
    val filteredItems = remember(config, allItems) {
        allItems.filter { item ->
            matchesConfig(item, config)
        }
    }
    
    LazyColumn {
        items(filteredItems) { item ->
            ItemRow(item)
        }
    }
}
```

**When to Use:**
- Filtering is simple presentation logic
- Each UI component needs different filtering
- Filtering is UI-specific (e.g., visual filters, sorting)
- You want components to be self-contained
- Compose philosophy (smart components)

**Pros:**
- UI components are self-contained and reusable
- Each component can have custom filtering
- ViewModel remains simple (just provides raw data)
- Aligns with Compose's component-based architecture
- Easy to add new components with different filters

**Cons:**
- Redundant computation if multiple components filter same data
- Business logic in UI (harder to test)
- Performance concerns with large datasets
- Inconsistent filtering across components

---

### Approach 3: Hybrid Approach (Best of Both)

**Pattern:** ViewModel provides raw data + helper functions, UI uses helpers for filtering

```kotlin
// ViewModel
class CardViewModel : ViewModel() {
    private val _config = MutableStateFlow<CardConfig>(defaultConfig)
    val config: StateFlow<CardConfig> = _config.asStateFlow()
    
    val allItems: StateFlow<List<Item>> = itemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Helper function for filtering (testable, reusable)
    fun filterItems(items: List<Item>, config: CardConfig): List<Item> {
        return items.filter { item ->
            matchesConfig(item, config)
        }
    }
    
    fun updateConfig(newConfig: CardConfig) {
        _config.value = newConfig
    }
}

// UI
@Composable
fun CardScreen(viewModel: CardViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    
    // Use ViewModel's helper function
    val filteredItems = remember(config, allItems) {
        viewModel.filterItems(allItems, config)
    }
    
    LazyColumn {
        items(filteredItems) { item ->
            ItemRow(item)
        }
    }
}
```

**When to Use:**
- You want testable filtering logic
- You need UI-level flexibility
- Filtering logic is moderately complex
- You want to avoid ViewModel complexity

**Pros:**
- Filtering logic is testable (in ViewModel)
- UI has flexibility to use or not use helpers
- ViewModel remains relatively simple
- Best balance of both approaches

**Cons:**
- Still some complexity in ViewModel
- Need to decide when to use helpers vs inline filtering

---

## Decision Matrix

| Factor | ViewModel-Level | UI-Level | Hybrid |
|--------|----------------|----------|--------|
| **Performance** | Best (filter once) | Good (with caching) | Good (with caching) |
| **Testability** | Excellent | Poor | Good |
| **Flexibility** | Low | High | Medium |
| **ViewModel Complexity** | High | Low | Medium |
| **UI Complexity** | Low | High | Medium |
| **Reusability** | Low (per-VM) | High (per-component) | Medium |
| **Compose Alignment** | Poor | Excellent | Good |
| **Best For** | Complex business logic | Simple presentation logic | Moderate complexity |

---

## Best Practices by Scenario

### Scenario 1: Simple Presentation Filters
**Example:** Timeline range, sort order, show/hide specific fields

**Recommendation:** UI-Level Filtering

```kotlin
@Composable
fun CashFlowCard(
    transactions: List<Transaction>,
    timelineRange: TimelineRange
) {
    val filteredTransactions = remember(timelineRange, transactions) {
        TimelineService.filterByTimelineRange(transactions, timelineRange)
    }
    
    LazyColumn {
        items(filteredTransactions) { tx ->
            TransactionRow(tx)
        }
    }
}
```

**Why:**
- Filtering is simple and declarative
- Each card can have its own timeline
- Aligns with Compose philosophy
- Performance is acceptable with caching

---

### Scenario 2: Complex Business Logic Filtering
**Example:** Complex category rules, multi-field filters, calculations

**Recommendation:** ViewModel-Level Filtering

```kotlin
class AnalyticsViewModel : ViewModel() {
    val filteredAnalytics = combine(
        analyticsData,
        filterConfig
    ) { data, config ->
        analyticsUseCases.computeFilteredAnalytics(data, config)
    }.stateIn(...)
}
```

**Why:**
- Business logic should be testable
- Complex filtering requires access to use cases
- Multiple UI components need same filtered data
- Performance is critical

---

### Scenario 3: Per-Component Customization
**Example:** Dashboard with multiple cards showing different views of same data

**Recommendation:** UI-Level Filtering with Helper Functions

```kotlin
// ViewModel provides helper
fun filterByCategory(items: List<Item>, category: String): List<Item> {
    return items.filter { it.category == category }
}

// Each card uses helper differently
@Composable
fun Card1(viewModel: CardViewModel) {
    val filtered = remember(config, items) {
        viewModel.filterByCategory(items, config.category)
    }
}

@Composable
fun Card2(viewModel: CardViewModel) {
    val filtered = remember(config, items) {
        viewModel.filterByDateRange(items, config.dateRange)
    }
}
```

**Why:**
- Each card needs different filtering
- Filtering logic is testable (in ViewModel)
- Cards are independent and reusable
- Flexibility to customize per card

---

### Scenario 4: Large Datasets
**Example:** >10,000 items, complex filtering, performance critical

**Recommendation:** ViewModel-Level Filtering with Paging

```kotlin
class LargeListViewModel : ViewModel() {
    fun getFilteredItems(config: FilterConfig): Flow<PagingData<Item>> {
        return repository.getItems()
            .map { pagingData ->
                pagingData.filter { matchesConfig(it, config) }
            }
            .cachedIn(viewModelScope)
    }
}
```

**Why:**
- Performance is critical
- Paging library handles large datasets efficiently
- Filter once, use everywhere
- ViewModel manages complexity

---

## Best Practices Summary

### 1. Match Approach to Complexity
- **Simple filtering** → UI-level
- **Complex business logic** → ViewModel-level
- **Moderate complexity** → Hybrid

### 2. Consider Performance
- **Small datasets** (<1,000 items) → UI-level is fine
- **Medium datasets** (1,000-10,000 items) → Use caching with `remember`
- **Large datasets** (>10,000 items) → ViewModel-level with Paging

### 3. Consider Reusability
- **Single consumer** → Either approach works
- **Multiple consumers** → ViewModel-level for consistency
- **Different filters per consumer** → UI-level or Hybrid

### 4. Consider Testability
- **Business logic** → ViewModel-level (testable)
- **Presentation logic** → UI-level (UI tests)
- **Mixed** → Hybrid (ViewModel helpers)

### 5. Follow Compose Philosophy
- Use `remember` with proper keys for caching
- Use `LaunchedEffect` for async operations
- Keep components self-contained when possible
- Prefer declarative over imperative

### 6. State Management Best Practices
```kotlin
// Good: Proper keys for remember
val filtered = remember(config, items) {
    items.filter { matchesConfig(it, config) }
}

// Good: Use derivedStateOf for derived state
val filtered by remember {
    derivedStateOf {
        items.filter { matchesConfig(it, config.value) }
    }
}

// Bad: No keys, won't recompose
val filtered = remember {
    items.filter { matchesConfig(it, config) }
}
```

### 7. Error Handling
```kotlin
// ViewModel-level: Handle errors before UI
val filteredItems = config
    .mapLatest { config ->
        try {
            filterItems(allItems.value, config)
        } catch (e: Exception) {
            Timber.e(e, "Filtering error")
            emptyList()
        }
    }
    .stateIn(...)

// UI-level: Handle errors in component
val filtered = remember(config, items) {
    try {
        items.filter { matchesConfig(it, config) }
    } catch (e: Exception) {
        Timber.e(e, "Filtering error")
        emptyList()
    }
}
```

### 8. Loading States
```kotlin
// ViewModel-level: Include in state
sealed class FilteredState {
    object Loading : FilteredState()
    data class Success(val items: List<Item>) : FilteredState()
    data class Error(val message: String) : FilteredState()
}

// UI-level: Local loading state
var isLoading by remember { mutableStateOf(false) }
var filteredItems by remember { mutableStateOf<List<Item>?>(null) }

LaunchedEffect(config) {
    isLoading = true
    filteredItems = filterItems(allItems, config)
    isLoading = false
}
```

---

## Recommendation for Your Scenario

Based on the card timeline filtering issue in your project:

**Use UI-Level Filtering with Helper Functions**

**Rationale:**
1. **Cards are independent components**: Each card should be self-contained
2. **Timeline filtering is simple**: It's a presentation-level concern
3. **Flexibility needed**: Different cards may need different timeline behaviors
4. **Compose alignment**: Matches Compose's component-based philosophy
5. **Performance is acceptable**: Transaction lists are small, filtering is fast

**Implementation:**
```kotlin
// ViewModel provides raw data and helper
class DashboardViewModel : ViewModel() {
    val allTransactions: StateFlow<List<Transaction>> = ...
    
    // Helper for filtering (testable)
    fun filterByTimeline(
        transactions: List<Transaction>,
        timelineRange: TimelineRange
    ): List<Transaction> {
        return TimelineService.filterByTimelineRange(transactions, timelineRange)
    }
}

// Card filters locally
@Composable
fun CashFlowCard(
    viewModel: DashboardViewModel,
    cardConfig: CashFlowCardConfig
) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    
    val filteredTransactions = remember(cardConfig.timelineRange, allTransactions) {
        viewModel.filterByTimeline(allTransactions, cardConfig.timelineRange)
    }
    
    // Use filteredTransactions for display
}
```

**Benefits:**
- Cards are self-contained and reusable
- Filtering logic is testable (in ViewModel helper)
- ViewModel remains simple
- Performance is good with caching
- Aligns with your card-level filtering architecture
