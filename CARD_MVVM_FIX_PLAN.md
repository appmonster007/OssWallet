# Card MVVM Architecture Fix Plan

## Executive Summary

**Issue:** When changing the selected timeline in the cards setting/config panel, the card collapses and goes empty. The timeline filtering system is not functioning correctly.

**Root Cause:** Multiple architectural violations and implementation bugs in the card MVVM system that cause cards to render empty when timeline changes.

**Impact:** Affects all screens using cards (Dashboard, AccountDetail, AccountsView, Analytics).

**Architectural Decision:** Timeline filtering should use the **Hybrid Approach** with helper functions. ViewModel provides raw data + helper functions, UI uses helpers for filtering. This approach:
- Keeps cards independent and self-contained
- Allows each card to have custom filtering logic
- Aligns with Compose's component-based architecture
- Follows the principle that ViewModels should hold raw data, not pre-filtered data
- Makes filtering logic testable (in ViewModel helpers)
- Provides flexibility for UI-level customization

---

## Root Cause Analysis

### 1. Critical Bug: produceState Key Not Triggering Recomputation

**Location:** `DashboardScreen.kt`, `AccountDetailScreen.kt`, `AccountsViewScreen.kt`

**Problem:**
```kotlin
val cashFlowResult by produceState<CashFlowResult?>(
    initialValue = null, 
    key1 = cardConfig.cardId, 
    key2 = cardConfig.timelineRange,  // ❌ BUG: TimelineRange is a sealed class
    key3 = state
) {
    value = dashboardViewModel.computeCashFlowForCard(...)
}
```

**Issue:** 
- `TimelineRange` is a sealed class with data classes (e.g., `LastPeriod(period)`, `CustomRange(start, end)`)
- When the timeline changes, the object reference changes, but produceState may not detect the change properly
- If recomputation fails or returns null, the card renders as `Box(modifier = Modifier.size(0.dp))` - empty

**Evidence:**
- Lines 416, 461, 496, 527, 585, 609, 634 in DashboardScreen.kt
- Lines 399, 434 in AccountDetailScreen.kt
- Similar pattern in AccountsViewScreen.kt

---

### 2. MVVM Pattern Violation: Dual Configuration System

**Problem:** Two parallel configuration systems causing confusion and bugs:

**System 1 - Card Configs (CardManagerViewModel):**
```kotlin
// Per-card configuration
data class CashFlowCardConfig(
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    ...
)
```

**System 2 - Global Settings (SettingsDataStore):**
```kotlin
// Global section settings
data class DashboardSettings(
    val cashFlowSection: CashFlowSectionSettings(
        val timelineRange: TimelineRange = TimelineRange.AllTime
    )
)
```

**Issue:**
- DashboardViewModel has BOTH card-specific update methods AND global update methods:
  - `updateCardCashFlowTimelineRange(cardId, range)` - updates card config
  - `updateCashFlowTimelineRange(range)` - updates global settings
- The UI uses card configs from `cardManagerViewModel.cardsConfig`
- But some computation might use global settings
- This creates inconsistency and confusion

**Evidence:**
- DashboardViewModel.kt lines 178-182 (global update)
- DashboardViewModel.kt lines 258-264 (card-specific update)
- Same pattern in AccountDetailViewModel.kt

---

### 3. Incomplete Timeline Filtering Implementation

**Location:** `CardComputationService.kt`

**Problem:**
```kotlin
override suspend fun computeCashFlow(
    config: CashFlowCardConfig,
    transactions: List<Transaction>,
    ...
): CashFlowResult {
    val filteredTransactions = filterTransactionsByScope(...)
    
    return financialUseCases.calculateCashFlow(
        allTransactions = filteredTransactions,  // ❌ BUG: Service doing filtering
        timelineRange = config.timelineRange,
        ...
    )
}
```

**Issue:**
- Service layer is doing timeline filtering before passing data to cards
- This violates the principle that cards should be self-contained
- Creates tight coupling between service and card logic
- Makes cards dependent on service's filtering implementation
- Hard to customize filtering per card type

**Evidence:**
- CardComputationService.kt lines 268-269, 360-361, 410-411 (explicit filtering in service)
- CardComputationService.kt lines 144-150 (delegates to use case with filtering)

**Architectural Decision:**
Timeline filtering should happen at the card component level, not in the service layer.

---

### 4. Missing Error Handling in produceState

**Problem:**
```kotlin
val cashFlowResult by produceState<CashFlowResult?>(initialValue = null, ...) {
    value = dashboardViewModel.computeCashFlowForCard(...)  // ❌ No try-catch
}
```

**Issue:**
- If computation throws an exception, produceState fails silently
- Result remains null, card renders empty
- No user feedback about what went wrong
- Hard to debug

---

### 5. Redundant Computation

**Problem:**
- DashboardViewModel has computation methods that duplicate logic from CardComputationService
- Lines 307-375 in DashboardViewModel.kt: computeNetWorthForCard, computeCurrencyTotalsForCard, etc.
- These methods just delegate to CardComputationService
- But DashboardViewModel ALSO has ViewState computation methods (lines 380-570) that do the same work differently
- This creates confusion about which method to use

---

### 6. State Synchronization Issues

**Problem:**
- CardManagerViewModel.cardsConfig is a StateFlow that updates when card configs change
- DashboardViewModel.uiState is a separate StateFlow that depends on settingsDataStore
- When card config changes, cardManagerViewModel updates
- But the produceState in the screen uses both cardConfig AND state as keys
- If state doesn't change when card config changes, recomputation might not trigger properly

---

## Architectural Decision: Card-Level Filtering

### Justification for Card-Level Filtering Approach

**Decision:** Timeline filtering should happen in card component files, not in the service layer or ViewModel.

**Rationale:**

1. **Component Independence**
   - Each card should be self-contained and responsible for its own data transformation
   - Cards can be reused across different screens without depending on pre-filtered data
   - Easier to test cards in isolation

2. **Compose Philosophy**
   - Compose encourages component-based architecture where components manage their own state
   - Aligns with Compose's `produceState` and `LaunchedEffect` patterns for local computation
   - Follows the principle of "smart components" that handle their own logic

3. **Flexibility & Customization**
   - Each card type can have custom filtering logic
   - Cards can implement different timeline behaviors without changing shared services
   - Easy to add new card types with unique filtering requirements

4. **Separation of Concerns**
   - ViewModel holds raw data (single source of truth)
   - Service layer provides raw computation capabilities (not pre-filtered results)
   - Card components handle presentation logic including filtering
   - Clear boundary: data vs. presentation

5. **Reactive Updates**
   - When timeline changes, only the affected card recomputes
   - No need to re-filter data in service layer for all cards
   - More granular reactivity

### Comparison: Service-Level vs UI-Level vs Hybrid Filtering

| Aspect | Service-Level | UI-Level | Hybrid |
|--------|-------------|----------|--------|
| **Data Flow** | Service filters → Card displays | ViewModel provides raw → Card filters | ViewModel provides raw + helpers → Card uses helpers |
| **Coupling** | High (service knows card logic) | Low (card knows its own logic) | Medium (shared helpers) |
| **Reusability** | Low (pre-filtered data specific) | High (raw data reusable) | High (helpers reusable) |
| **Performance** | Best (filter once) | Good (with caching) | Good (with caching) |
| **Flexibility** | Low (changes affect all) | High (per-card customization) | Medium (helpers + customization) |
| **Testability** | Good (service tests) | Poor (UI tests only) | Excellent (helper tests) |
| **Compose Alignment** | Poor (imperative) | Excellent (declarative) | Good (declarative with helpers) |
| **ViewModel Complexity** | High | Low | Medium |
| **Best For** | Complex business logic | Simple presentation | Moderate complexity |

### Performance Considerations

**Concern:** Filtering data in each card could be inefficient if multiple cards use the same data.

**Mitigation:**
1. Use `remember` with proper keys to cache filtered results
2. Implement memoization for timeline filtering
3. Consider using `derivedStateOf` for derived data
4. Profile performance and optimize only if needed

**Reality Check:**
- Transaction lists are typically small (<10,000 items)
- Timeline filtering is fast (O(n) operation)
- Modern phones can handle this easily
- Performance impact is negligible compared to benefits

### Best Practices for Hybrid Approach with Helper Functions

1. **ViewModel Provides Helper Functions**
   ```kotlin
   class DashboardViewModel : ViewModel() {
       val allTransactions: StateFlow<List<Transaction>> = ...

       // Helper function for filtering (testable, reusable)
       fun filterByTimeline(
           transactions: List<Transaction>,
           timelineRange: TimelineRange
       ): List<Transaction> {
           return TimelineService.filterByTimelineRange(transactions, timelineRange)
       }
   }
   ```

2. **UI Uses ViewModel Helpers with remember**
   ```kotlin
   @Composable
   fun CashFlowCard(viewModel: DashboardViewModel, cardConfig: CashFlowCardConfig) {
       val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

       // Use ViewModel's helper function
       val filteredTransactions = remember(cardConfig.timelineRange, allTransactions) {
           viewModel.filterByTimeline(allTransactions, cardConfig.timelineRange)
       }
   }
   ```

3. **Implement Proper Keys for Recomposition**
   ```kotlin
   val filteredData = remember(cardConfig.cardId, cardConfig.timelineRange, allData) {
       viewModel.filterByTimeline(allData, cardConfig.timelineRange)
   }
   ```

4. **Add Loading States**
   ```kotlin
   var isLoading by remember { mutableStateOf(false) }
   var filteredData by remember { mutableStateOf<List<Transaction>?>(null) }

   LaunchedEffect(cardConfig.timelineRange, allTransactions) {
       isLoading = true
       filteredData = viewModel.filterByTimeline(allTransactions, cardConfig.timelineRange)
       isLoading = false
   }
   ```

5. **Handle Empty States Gracefully**
   ```kotlin
   if (filteredData.isEmpty()) {
       EmptyStateCard(message = "No transactions for this period")
   } else {
       // Show card content
   }
   ```

6. **Cache Computation Results**
   ```kotlin
   val computedResult = remember(key1, key2, key3) {
       viewModel.computeExpensiveOperation(key1, key2, key3)
   }
   ```

### How This Request Can Be Improved

**Current Request:** "Filtering should happen in the files associated with the cards"

**Improvement 1: Specify the Layer**
- Be explicit about WHERE in the card file the filtering happens
- Recommendation: In the card Composable function, using `remember`/`LaunchedEffect`

**Improvement 2: Define the Data Flow**
- Clarify what data is passed from ViewModel to card
- Recommendation: ViewModel passes unfiltered transaction list, card filters locally

**Improvement 3: Address Performance Concerns**
- Acknowledge the performance trade-off
- Provide mitigation strategies (caching, memoization)

**Improvement 4: Define Error Handling Strategy**
- Specify how errors in filtering should be handled
- Recommendation: Try-catch in card, show error state

**Improved Request Statement:**
> "Timeline filtering should use the Hybrid Approach with helper functions. The ViewModel should provide raw transaction data along with helper functions for filtering. Each card's Composable function should use these helper functions with Compose's state management (remember/LaunchedEffect) to filter data based on its configuration. This approach provides testable filtering logic (in ViewModel helpers) while maintaining UI-level flexibility. It should include proper caching using remember keys and error handling to maintain performance."

---

## Architectural Violations

### 1. Violation of Single Source of Truth
- Two configuration systems (card configs vs global settings)
- No clear hierarchy or relationship between them
- UI doesn't know which one to use

### 2. Violation of Unidirectional Data Flow
- Card updates go through CardManagerViewModel
- But computation depends on both card configs AND global state
- Circular dependency between configs and state

### 3. Violation of Separation of Concerns
- CardComputationService should handle ALL computation logic
- But some filtering is done in service, some in use cases
- DashboardViewModel has its own computation methods that duplicate service logic

### 4. Violation of Immutability
- Card configs are updated via copy(), which is correct
- But the update flow is not reactive - changes don't automatically trigger recomputation in all places

---

## Fix Plan (Hybrid Approach with Helper Functions)

### Phase 1: Immediate Bug Fixes (Critical - 1-2 days)

#### Fix 1.1: Implement Hybrid Approach with Helper Functions
**Files:** DashboardScreen.kt, AccountDetailScreen.kt, AccountsViewScreen.kt, AnalyticsScreen.kt

**Current (Service-Level Filtering):**
```kotlin
val cashFlowResult by produceState<CashFlowResult?>(initialValue = null, key1 = cardConfig.cardId, key2 = cardConfig.timelineRange, key3 = state) {
    value = dashboardViewModel.computeCashFlowForCard(
        cardConfig = cardConfig,
        allTransactions = state.allTransactions,  // Service filters this
        ...
    )
}
```

**Fix (Hybrid Approach with Helper Functions):**

**Step 1: Add Helper Functions to ViewModel**
```kotlin
// In DashboardViewModel.kt
class DashboardViewModel : ViewModel() {
    val allTransactions: StateFlow<List<Transaction>> = ...
    val baseCurrency: StateFlow<String> = ...
    val exchangeRates: StateFlow<Map<String, BigDecimal>> = ...

    // Helper function for timeline filtering (testable, reusable)
    fun filterTransactionsByTimeline(
        transactions: List<Transaction>,
        timelineRange: TimelineRange
    ): List<Transaction> {
        return TimelineService.filterByTimelineRange(transactions, timelineRange)
    }

    // Helper function for cash flow computation (testable, reusable)
    fun computeCashFlowFromFiltered(
        filteredTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        return try {
            financialUseCases.calculateCashFlow(
                allTransactions = filteredTransactions,
                timelineRange = TimelineRange.AllTime,  // Already filtered
                baseCurrency = baseCurrency,
                exchangeRates = exchangeRates,
                precision = precision
            )
        } catch (e: Exception) {
            Timber.e(e, "Error computing cash flow")
            CashFlowResult.empty()
        }
    }
}
```

**Step 2: Use Helpers in Card Component**
```kotlin
// In DashboardScreen.kt - CashFlowCard
is CashFlowCardConfig -> {
    val allTransactions by dashboardViewModel.allTransactions.collectAsStateWithLifecycle()

    // Use ViewModel's helper for filtering
    val filteredTransactions = remember(cardConfig.timelineRange, allTransactions) {
        dashboardViewModel.filterTransactionsByTimeline(allTransactions, cardConfig.timelineRange)
    }

    // Use ViewModel's helper for computation
    val cashFlowResult = remember(filteredTransactions, state.baseCurrency, state.exchangeRates, state.decimalPrecision) {
        dashboardViewModel.computeCashFlowFromFiltered(
            filteredTransactions = filteredTransactions,
            baseCurrency = state.baseCurrency,
            exchangeRates = state.exchangeRates,
            precision = state.decimalPrecision
        )
    }

    cashFlowResult?.let { result ->
        CashFlowCard(
            cashFlowResult = result,
            baseCurrency = state.baseCurrency,
            currentTheme = currentTheme,
            cardId = cardConfig.cardId,
            timelineRange = cardConfig.timelineRange,
            onTimelineRangeChange = { cardId, range -> dashboardViewModel.updateCardCashFlowTimelineRange(cardId, range) },
            ...
        )
    } ?: Box(modifier = Modifier.size(0.dp))
}
```

**Benefits:**
- Filtering logic is testable (in ViewModel helpers)
- Cards are self-contained with their own filtering logic
- Clear separation: ViewModel provides helpers, cards use them
- Never returns null, card never collapses
- Better debugging and testing
- Reusable helpers across multiple cards

---

#### Fix 1.2: Add Empty Result Factory Methods
**File:** Create or update CashFlowResult, CurrencyAggregationItem, etc.

**Add:**
```kotlin
companion object {
    fun empty() = CashFlowResult(
        totalIncomeInBase = BigDecimal.ZERO,
        totalExpensesInBase = BigDecimal.ZERO,
        breakdown = emptyList()
    )
}
```

**Benefits:**
- Cards always have valid data structure
- UI can show "No data for this period" instead of collapsing
- Consistent behavior across all cards

---

### Phase 2: Architecture Cleanup (High Priority - 3-5 days)

#### Fix 2.1: Eliminate Dual Configuration System
**Decision:** Remove global settings timeline ranges, use ONLY card configs

**Rationale:**
- Cards are meant to be independent and configurable
- Global settings create confusion
- Card configs already have all necessary fields
- Aligns with card-level filtering approach

**Implementation:**
1. Remove timelineRange from DashboardSettings section settings
2. Keep card configs as single source of truth
3. Update DashboardViewModel to remove global timeline update methods:
   - Remove `updateCashFlowTimelineRange(range)`
   - Remove `updateCurrencyBalancesTimelineRange(range)`
   - Remove `updateAccountsTimelineRange(range)`
   - Remove `updateNetWorthTimelineRange(range)`
4. Keep ONLY card-specific methods:
   - Keep `updateCardCashFlowTimelineRange(cardId, range)`
   - Keep `updateCardCurrencyBalancesTimelineRange(cardId, range)`
   - etc.

**Files:**
- DashboardViewModel.kt
- SettingsDataStore.kt (remove timeline from section settings)
- AccountDetailViewModel.kt (same pattern)

---

#### Fix 2.2: Add Helper Functions to ViewModels and Remove Timeline Filtering from CardComputationService
**Decision:** ViewModels provide helper functions, CardComputationService does NOT filter by timeline

**Rationale:**
- ViewModel helpers make filtering logic testable
- Service layer should provide raw computation capabilities
- Cards use ViewModel helpers for filtering
- Keeps service layer generic and reusable
- Aligns with Hybrid Approach

**Implementation:**

**Step 1: Add Helper Functions to ViewModels**
```kotlin
// In DashboardViewModel.kt
class DashboardViewModel : ViewModel() {
    // Timeline filtering helper
    fun filterTransactionsByTimeline(
        transactions: List<Transaction>,
        timelineRange: TimelineRange
    ): List<Transaction> {
        return TimelineService.filterByTimelineRange(transactions, timelineRange)
    }

    // Currency aggregation helper
    fun computeCurrencyAggregation(
        accounts: List<Account>,
        transactions: List<Transaction>,
        timelineRange: TimelineRange,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem> {
        val filtered = filterTransactionsByTimeline(transactions, timelineRange)
        return financialUseCases.getCurrencyAggregation(
            accounts, filtered, TimelineRange.AllTime, baseCurrency, exchangeRates, precision
        )
    }

    // Cash flow computation helper
    fun computeCashFlow(
        transactions: List<Transaction>,
        timelineRange: TimelineRange,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        val filtered = filterTransactionsByTimeline(transactions, timelineRange)
        return financialUseCases.calculateCashFlow(
            filtered, TimelineRange.AllTime, baseCurrency, exchangeRates, precision
        )
    }

    // Add similar helpers for other card types...
}
```

**Step 2: Remove Timeline Filtering from CardComputationService**
```kotlin
// In CardComputationService.kt
override suspend fun computeCashFlow(
    config: CashFlowCardConfig,
    transactions: List<Transaction>,  // Already filtered by card using ViewModel helper
    ...
): CashFlowResult {
    // NO timeline filtering here - card already filtered using ViewModel helper
    val filteredByScope = filterTransactionsByScope(transactions, config.scope, config.accountId)

    return financialUseCases.calculateCashFlow(
        allTransactions = filteredByScope,
        timelineRange = TimelineRange.AllTime,  // Use AllTime since already filtered
        ...
    )
}
```

**Step 3: Remove Explicit Timeline Filtering from Service Methods**
- Remove from computeCategoryBreakdown (lines 268-269)
- Remove from computeTimelineTrajectory (lines 360-361)
- Remove from computeCategoryExpenseTreemap (lines 410-411)

**Step 4: Update ALL Computation Methods Consistently**
- computeCashFlow
- computeCurrencyTotals
- computeNetWorth
- computeAccountStats
- computeCategoryBreakdown
- computeTimelineTrajectory
- computeCategoryExpenseTreemap

**Files:**
- DashboardViewModel.kt (add helpers)
- AccountDetailViewModel.kt (add helpers)
- AccountsViewViewModel.kt (add helpers)
- CardComputationService.kt (remove timeline filtering)

---

#### Fix 2.3: Replace Computation Methods with Helper Functions
**Decision:** Remove old computation methods, replace with testable helper functions

**Rationale:**
- Old methods were tightly coupled to card-specific logic
- Helper functions are testable and reusable
- Cards use helpers with remember for flexibility
- Reduces ViewModel complexity while maintaining testability

**Implementation:**

**Step 1: Remove Old Computation Methods from DashboardViewModel.kt**
- Remove computeNetWorthForCard (lines 307-323)
- Remove computeCurrencyTotalsForCard (lines 325-341)
- Remove computeCashFlowForCard (lines 343-357)
- Remove computeAccountStatsForCard (lines 359-375)
- Remove computeCategoryBreakdownForCard (lines 573-585)
- Remove computeTimelineTrajectoryForCard (lines 587-601)
- Remove computeCategoryExpenseTreemapForCard (lines 603-615)

**Step 2: Remove ViewState Computation Methods (lines 380-570)**
- Remove computeNetWorthCardViewState
- Remove computeCurrencyBalancesCardViewState
- Remove computeCashFlowCardViewState
- Remove computeAccountsCardViewState
- Remove computeBudgetsCardViewState

**Step 3: Add Helper Functions (already done in Fix 2.2)**
- filterTransactionsByTimeline
- computeCurrencyAggregation
- computeCashFlow
- computeNetWorth
- computeAccountStats
- computeCategoryBreakdown
- computeTimelineTrajectory
- computeCategoryExpenseTreemap

**Step 4: Update Screens to Use Helpers**
```kotlin
// In DashboardScreen.kt
is CashFlowCardConfig -> {
    val allTransactions by dashboardViewModel.allTransactions.collectAsStateWithLifecycle()

    // Use ViewModel's helper for filtering
    val filteredTransactions = remember(cardConfig.timelineRange, allTransactions) {
        dashboardViewModel.filterTransactionsByTimeline(allTransactions, cardConfig.timelineRange)
    }

    // Use ViewModel's helper for computation
    val cashFlowResult = remember(filteredTransactions, state.baseCurrency, state.exchangeRates, state.decimalPrecision) {
        dashboardViewModel.computeCashFlow(
            filteredTransactions,
            cardConfig.timelineRange,
            state.baseCurrency,
            state.exchangeRates,
            state.decimalPrecision
        )
    }

    // Use result...
}
```

**Files:**
- DashboardViewModel.kt
- AccountDetailViewModel.kt
- AccountsViewViewModel.kt

---

#### Fix 2.4: Fix State Synchronization with Helper Functions
**Decision:** Make card config changes trigger recomposition using ViewModel helpers

**Implementation:**
1. Ensure cards collect cardConfig from cardManagerViewModel:
```kotlin
val cardsConfig by cardManagerViewModel.cardsConfig.collectAsStateWithLifecycle()
```

2. In card component, use ViewModel helpers with proper remember keys:
```kotlin
val allTransactions by dashboardViewModel.allTransactions.collectAsStateWithLifecycle()

// Use ViewModel's helper with proper keys
val filteredTransactions = remember(cardConfig.cardId, cardConfig.timelineRange, allTransactions) {
    dashboardViewModel.filterTransactionsByTimeline(allTransactions, cardConfig.timelineRange)
}

val cashFlowResult = remember(filteredTransactions, state.baseCurrency, state.exchangeRates, state.decimalPrecision) {
    dashboardViewModel.computeCashFlow(
        filteredTransactions,
        cardConfig.timelineRange,
        state.baseCurrency,
        state.exchangeRates,
        state.decimalPrecision
    )
}
```

3. When timeline changes:
   - CardManagerViewModel updates card config
   - Card component detects cardConfig.timelineRange change
   - remember recomputes filtered transactions using ViewModel helper
   - remember recomputes cash flow result using ViewModel helper
   - Card recomputes and updates UI

**Benefits:**
- Single reactive flow
- Card config changes automatically trigger recomputation
- ViewModel helpers ensure consistent filtering logic
- No need for complex produceState keys
- Testable filtering logic in ViewModel

---

### Phase 3: Robust Error Handling (Medium Priority - 2-3 days)

#### Fix 3.1: Add Comprehensive Error Handling in ViewModel Helpers and Cards
**Implementation:**

**Step 1: Add Error Handling in ViewModel Helpers**
```kotlin
// In DashboardViewModel.kt
fun computeCashFlow(
    transactions: List<Transaction>,
    timelineRange: TimelineRange,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): CashFlowResult {
    return try {
        val filtered = filterTransactionsByTimeline(transactions, timelineRange)
        financialUseCases.calculateCashFlow(
            filtered, TimelineRange.AllTime, baseCurrency, exchangeRates, precision
        )
    } catch (e: Exception) {
        Timber.e(e, "Error computing cash flow")
        CashFlowResult.empty()
    }
}
```

**Step 2: Add Error Handling in Cards (as fallback)**
```kotlin
val cashFlowResult = remember(filteredTransactions, state.baseCurrency, state.exchangeRates, state.decimalPrecision) {
    dashboardViewModel.computeCashFlow(
        filteredTransactions,
        cardConfig.timelineRange,
        state.baseCurrency,
        state.exchangeRates,
        state.decimalPrecision
    )
}

if (cashFlowResult.totalIncomeInBase == BigDecimal.ZERO &&
    cashFlowResult.totalExpensesInBase == BigDecimal.ZERO) {
    EmptyStateCard(message = "No transactions for this period")
} else {
    // Show card content
}
```

---

#### Fix 3.2: Add Loading States
**Implementation:**
1. Add isLoading state in card component
2. Show loading spinner while computing
3. Show error message if computation fails

```kotlin
var isLoading by remember { mutableStateOf(true) }
var cashFlowResult by remember { mutableStateOf<CashFlowResult?>(null) }

LaunchedEffect(filteredTransactions) {
    isLoading = true
    cashFlowResult = try {
        financialUseCases.calculateCashFlow(...)
    } catch (e: Exception) {
        CashFlowResult.empty()
    }
    isLoading = false
}

if (isLoading) {
    LoadingSpinner()
} else if (cashFlowResult != null) {
    // Show card
}
```

---

### Phase 4: Testing & Validation (Medium Priority - 2-3 days)

#### Fix 4.1: Add Unit Tests for Card-Level Filtering
**Tests needed:**
1. Test TimelineService.filterByTimelineRange with all range types
2. Test that remember keys trigger recomputation correctly
3. Test that empty filtered lists don't cause crashes
4. Test error handling in card components

---

#### Fix 4.2: Add Integration Tests
**Tests needed:**
1. End-to-end card rendering with timeline changes
2. Timeline change in settings panel updates card config
3. Card filters data correctly and updates UI
4. Multiple cards with different timelines work independently

---

#### Fix 4.3: Manual Testing Checklist
- [ ] Change timeline on CashFlow card - card filters locally, updates, doesn't collapse
- [ ] Change timeline on CurrencyBalances card - card filters locally, updates, doesn't collapse
- [ ] Change timeline on Accounts card - card filters locally, updates, doesn't collapse
- [ ] Change timeline on NetWorth card - card filters locally, updates, doesn't collapse
- [ ] Change timeline on CategoryBreakdown card - card filters locally, updates, doesn't collapse
- [ ] Change timeline on TimelineTrajectory card - card filters locally, updates, doesn't collapse
- [ ] Change timeline on CategoryExpenseTreemap card - card filters locally, updates, doesn't collapse
- [ ] Test with no transactions in selected period - shows "No data" message
- [ ] Test with error in computation - shows error message, doesn't crash
- [ ] Test on Dashboard screen - all cards work independently
- [ ] Test on AccountDetail screen - cards filter account transactions
- [ ] Test on AccountsView screen - cards work with account scope
- [ ] Test on Analytics screen - analytics cards filter correctly
- [ ] Test performance with many cards - no noticeable lag

---

## Implementation Order

### Week 1: Critical Fixes
1. Fix 1.1: Implement card-level filtering with remember (all screens)
2. Fix 1.2: Add empty result factory methods
3. Test on Dashboard screen with timeline changes

### Week 2: Architecture Cleanup
4. Fix 2.1: Eliminate dual configuration system
5. Fix 2.2: Remove timeline filtering from CardComputationService
6. Fix 2.3: Remove redundant computation methods from ViewModels
7. Fix 2.4: Fix state synchronization
8. Test all screens with independent card filtering

### Week 3: Robustness
9. Fix 3.1: Add comprehensive error handling in cards
10. Fix 3.2: Add loading states
11. Test error scenarios and empty states

### Week 4: Testing
12. Fix 4.1: Add unit tests for card-level filtering
13. Fix 4.2: Add integration tests
14. Fix 4.3: Manual testing checklist
15. Documentation updates

---

## Files to Modify

### Critical Files (Must Modify)
1. `app/src/main/java/com/osscabinet/ossWallet/ui/dashboard/DashboardScreen.kt` - Add card-level filtering
2. `app/src/main/java/com/osscabinet/ossWallet/ui/accountdetail/AccountDetailScreen.kt` - Add card-level filtering
3. `app/src/main/java/com/osscabinet/ossWallet/ui/screens/AccountsViewScreen.kt` - Add card-level filtering
4. `app/src/main/java/com/osscabinet/ossWallet/ui/screens/AnalyticsScreen.kt` - Add card-level filtering
5. `app/src/main/java/com/osscabinet/ossWallet/domain/service/CardComputationService.kt` - Remove timeline filtering

### High Priority Files
6. `app/src/main/java/com/osscabinet/ossWallet/ui/dashboard/DashboardViewModel.kt` - Remove computation methods
7. `app/src/main/java/com/osscabinet/ossWallet/ui/viewmodel/AccountDetailViewModel.kt` - Remove computation methods
8. `app/src/main/java/com/osscabinet/ossWallet/ui/viewmodel/AccountsViewViewModel.kt` - Remove computation methods
9. `app/src/main/java/com/osscabinet/ossWallet/data/local/SettingsDataStore.kt` - Remove timeline from global settings

### Medium Priority Files
10. `app/src/main/java/com/osscabinet/ossWallet/domain/usecase/CashFlowResult.kt` - Add empty() factory method
11. `app/src/main/java/com/osscabinet/ossWallet/domain/usecase/CurrencyAggregationItem.kt` - Add empty() factory method
12. Card component files (CashFlowCard.kt, CurrencyBalancesCard.kt, etc.) - Add filtering logic
13. Test files for TimelineService

---

## Success Criteria

1. ✅ Timeline changes in card settings panel update card data correctly
2. ✅ Cards filter data independently at component level
3. ✅ Cards never collapse to empty when timeline changes
4. ✅ Cards show "No data for this period" when appropriate
5. ✅ Error handling prevents crashes
6. ✅ Single source of truth for card configurations (card configs only)
7. ✅ Service layer provides raw computation, no timeline filtering
8. ✅ Cards are self-contained with their own filtering logic
9. ✅ No redundant computation code in ViewModels
10. ✅ Proper state synchronization with remember keys
11. ✅ All tests pass
12. ✅ Manual testing checklist complete

---

## Risks & Mitigations

### Risk 1: Performance Regression from Card-Level Filtering
**Mitigation:**
- Use `remember` with proper keys to cache filtered results
- Profile performance with multiple cards
- Implement memoization if needed
- Reality: Transaction lists are small, filtering is fast (O(n))

### Risk 2: Breaking Existing Functionality
**Mitigation:**
- Implement fixes incrementally
- Test each phase before proceeding
- Keep backup of working code

### Risk 3: Inconsistent Filtering Across Cards
**Mitigation:**
- Use TimelineService consistently in all cards
- Add unit tests for TimelineService
- Document the filtering pattern for future cards

---

## Documentation Updates

After fixes are complete:
1. Update CARD_ARCHITECTURE_REDESIGN.md with card-level filtering architecture
2. Update MVVM_BEST_PRACTICES.md with card-level filtering best practices
3. Add CARD_LEVEL_FILTERING_GUIDE.md with implementation guide for card-level filtering
4. Update AGENTS.md with new patterns for self-contained card components

---

## Conclusion

This fix plan addresses the root causes of the timeline filtering issue using a **card-level filtering approach**:

1. **Immediate bug fixes** to prevent cards from collapsing using proper state management
2. **Architecture cleanup** to eliminate dual configuration system and remove service-level filtering
3. **Card-level filtering implementation** to make cards self-contained and independent
4. **Removal of redundant computation** from ViewModels to simplify architecture
5. **Robust error handling** in card components for reliability
6. **Comprehensive testing** for validation

The plan follows best practices aligned with Compose philosophy:
- **Component independence**: Each card is self-contained with its own filtering logic
- **Single source of truth**: Card configs only, no global timeline settings
- **Unidirectional data flow**: ViewModel provides raw data, cards filter and display
- **Separation of concerns**: Service layer provides raw computation, cards handle presentation
- **Compose alignment**: Uses `remember`, `LaunchedEffect`, and proper keys for reactivity
- **Performance**: Uses caching with `remember` to mitigate any performance concerns

Implementation will be incremental with testing at each phase to ensure stability. The card-level filtering approach aligns with modern Compose architecture patterns and provides better flexibility, testability, and maintainability.
