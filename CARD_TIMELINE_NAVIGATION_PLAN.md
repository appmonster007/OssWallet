# Card Timeline-Aware Navigation Plan

## Overview
Update all dashboard cards to pass timeline range information when navigating to the Transaction List Screen (records view). Currently, cards navigate to records with context filters (account, currency, income/expense) but do not include the timeline range selected on the card. This plan ensures that when users click on a card element, the Transaction List Screen opens with both the context filter AND the timeline range from that card.

## Current State Analysis

### Cards with Timeline Support
All cards already have `timelineRange` parameter and display timeline labels:
- **NetWorthCard** - Has timelineRange, but NO click handlers for navigation
- **CashFlowCard** - Has timelineRange, onClickIncome/onClickExpense navigate with isIncome filter but NO timeline
- **CurrencyBalancesCard** - Has timelineRange, onCurrencyClick navigates with currency filter but NO timeline
- **AccountsCard** - Has timelineRange, onAccountClick uses separate callback (not RecordsFilterContext)
- **BudgetsCard** - Has NO timelineRange, onBudgetClick uses separate callback (not RecordsFilterContext)

### Navigation Infrastructure
- **RecordsFilterContext.Custom** - Already supports `startDate` and `endDate` fields
- **TransactionListScreen** - Already accepts `filterContext: String?` parameter and initializes filters from it
- **TimelineRange** - Sealed class with: AllTime, ThisPeriod, LastPeriod, CustomRange

### Current Navigation Patterns
1. **CashFlowCard**: `onRecordsClick(RecordsFilterContext.Custom(isIncome = true/false))`
2. **CurrencyBalancesCard**: `onRecordsClick(RecordsFilterContext.Custom(currencies = listOf(currency)))`
3. **AccountsCard**: `onAccountClick(accountId)` - Separate callback, goes to AccountDetailScreen
4. **BudgetsCard**: `onBudgetClick(category)` - Separate callback, goes to BudgetEditScreen
5. **NetWorthCard**: No click handlers

## Plan Summary

### Phase 1: Infrastructure Enhancement

**1.1 Add Date Range Extraction to TimelineService**
- Add `getDateRange(timelineRange: TimelineRange): Pair<Long?, Long?>` function to TimelineService
- Handle AllTime (returns null, null)
- Handle ThisPeriod (calculate start/end based on period type using existing getRangeForThis)
- Handle LastPeriod (calculate start/end based on period type using existing getRangeForLast)
- Handle CustomRange (return start, end directly)
- Refactor existing `filterByTimelineRange` to use this new function for consistency
- File: `domain/service/TimelineService.kt`
- Status: ✅ COMPLETED

**1.2 Create FilterManager with Multi-Filter Support**
- Create lightweight FilterManager class to replace FilterStack
- Maintain multiple independent filters with unique keys
- Automatic merge using AND logic when filters are combined
- Individual add/remove operations with automatic recompute
- Simple merge logic: lists are unioned, ranges use min/max, single values use last non-null
- File: `domain/model/FilterManager.kt`
- Status: ✅ COMPLETED

**1.3 Add TransactionFilter Extension Functions**
- Create convenience extension functions for common filter patterns
- `forAccount(accountId)`, `forCategory(categoryId)`, `forIncome()`, `forExpense()`
- `withTimeline(timelineRange)`, `withCurrency(currency)`, `withDateRange(start, end)`
- File: `domain/model/TransactionFilterExtensions.kt`
- Status: ✅ COMPLETED

**1.4 Write Unit Tests for FilterManager**
- Test merge logic for all filter field types
- Test add/remove operations
- Test edge cases (empty filters, conflicting values)
- File: `app/src/test/java/com/osscabinet/ossWallet/domain/model/FilterManagerTest.kt`
- Status: ✅ COMPLETED

**1.5 Update TransactionViewModel to Use FilterManager**
- Replace multiple filter states (_recordsFilter, _accountDetailFilter) with single FilterManager
- Add convenience methods: setAccountFilter, setCategoryFilter, setTimelineFilter, setIncomeTypeFilter
- Expose mergedFilter as StateFlow
- Maintain backward compatibility with RecordsFilterContext
- File: `ui/viewmodel/TransactionViewModel.kt`
- Status: ✅ COMPLETED

**1.6 DEFERRED: Replace RecordsFilterContext with TransactionFilter**
- NOTE: Deferred to Phase 4 (Cleanup) to minimize breaking changes
- Will be done after card navigation is working correctly
- For now, keep RecordsFilterContext for backward compatibility

### Phase 2: Card Component Updates

#### 2.1 CashFlowCard
- Update `onClickIncome` callback signature to include `timelineRange: TimelineRange`
- Update `onClickExpense` callback signature to include `timelineRange: TimelineRange`
- When clicked, create RecordsFilterContext.Custom with timeline:
  ```kotlin
  onClickIncome = { timelineRange ->
      val (start, end) = TimelineService.getDateRange(timelineRange)
      onRecordsClick(RecordsFilterContext.Custom(isIncome = true, startDate = start, endDate = end))
  }
  onClickExpense = { timelineRange ->
      val (start, end) = TimelineService.getDateRange(timelineRange)
      onRecordsClick(RecordsFilterContext.Custom(isIncome = false, startDate = start, endDate = end))
  }
  ```
- File: `ui/dashboard/components/CashFlowCard.kt`

#### 2.2 CurrencyBalancesCard
- Update `onCurrencyClick` callback signature to include `timelineRange: TimelineRange`
- When clicked, create RecordsFilterContext.Custom with timeline:
  ```kotlin
  onCurrencyClick = { currency, timelineRange ->
      val (start, end) = TimelineService.getDateRange(timelineRange)
      onRecordsClick(RecordsFilterContext.Custom(currencies = listOf(currency), startDate = start, endDate = end))
  }
  ```
- File: `ui/dashboard/components/CurrencyBalancesCard.kt`

#### 2.3 AccountsCard
- Add `onRecordsClick: ((RecordsFilterContext) -> Unit)?` callback parameter
- Keep `onAccountClick` for backward compatibility (or remove if fully migrating)
- When account is clicked, if `onRecordsClick` is provided, call it with:
  ```kotlin
  onAccountClick = { accountId, timelineRange ->
      val (start, end) = TimelineService.getDateRange(timelineRange)
      onRecordsClick(RecordsFilterContext.Custom(accountIds = listOf(accountId), startDate = start, endDate = end))
  }
  ```
- File: `ui/dashboard/components/AccountsCard.kt`

#### 2.4 BudgetsCard
- Add `timelineRange: TimelineRange` parameter (currently missing)
- Add timeline display in header (like other cards)
- Add `onRecordsClick: ((RecordsFilterContext) -> Unit)?` callback parameter
- Update `onBudgetClick` to use `onRecordsClick` with:
  ```kotlin
  onBudgetClick = { categoryId, timelineRange ->
      val (start, end) = TimelineService.getDateRange(timelineRange)
      onRecordsClick(RecordsFilterContext.Custom(categoryIds = listOf(categoryId.toString()), startDate = start, endDate = end))
  }
  ```
- File: `ui/dashboard/components/BudgetsCard.kt`

#### 2.5 NetWorthCard
- Add clickable income badge with `onClickIncome: (TimelineRange) -> Unit` callback
- Add clickable expense badge with `onClickExpense: (TimelineRange) -> Unit` callback
- When clicked, navigate with:
  ```kotlin
  onClickIncome = { timelineRange ->
      val (start, end) = TimelineService.getDateRange(timelineRange)
      onRecordsClick(RecordsFilterContext.Custom(isIncome = true, startDate = start, endDate = end))
  }
  onClickExpense = { timelineRange ->
      val (start, end) = TimelineService.getDateRange(timelineRange)
      onRecordsClick(RecordsFilterContext.Custom(isIncome = false, startDate = start, endDate = end))
  }
  ```
- File: `ui/dashboard/components/NetWorthCard.kt`

### Phase 3: DashboardScreen Integration

#### 3.1 Update Card Instantiation Calls
Update all card calls in DashboardScreen to pass timeline ranges to navigation callbacks:

**CashFlowCard:**
```kotlin
onClickIncome = { timelineRange ->
    val (start, end) = TimelineService.getDateRange(timelineRange)
    onRecordsClick(RecordsFilterContext.Custom(isIncome = true, startDate = start, endDate = end))
}
onClickExpense = { timelineRange ->
    val (start, end) = TimelineService.getDateRange(timelineRange)
    onRecordsClick(RecordsFilterContext.Custom(isIncome = false, startDate = start, endDate = end))
}
```

**CurrencyBalancesCard:**
```kotlin
onCurrencyClick = { currency ->
    val (start, end) = TimelineService.getDateRange(cardConfig.timelineRange)
    onRecordsClick(RecordsFilterContext.Custom(currencies = listOf(currency), startDate = start, endDate = end))
}
```

**AccountsCard:**
```kotlin
onAccountClick = { accountId ->
    val (start, end) = TimelineService.getDateRange(cardConfig.timelineRange)
    onRecordsClick(RecordsFilterContext.Custom(accountIds = listOf(accountId), startDate = start, endDate = end))
}
```

**BudgetsCard:**
```kotlin
onBudgetClick = { categoryId ->
    val (start, end) = TimelineService.getDateRange(cardConfig.timelineRange)
    onRecordsClick(RecordsFilterContext.Custom(categoryIds = listOf(categoryId.toString()), startDate = start, endDate = end))
}
```

**NetWorthCard:**
```kotlin
onClickIncome = { timelineRange ->
    val (start, end) = TimelineService.getDateRange(timelineRange)
    onRecordsClick(RecordsFilterContext.Custom(isIncome = true, startDate = start, endDate = end))
}
onClickExpense = { timelineRange ->
    val (start, end) = TimelineService.getDateRange(timelineRange)
    onRecordsClick(RecordsFilterContext.Custom(isIncome = false, startDate = start, endDate = end))
}
```

### Phase 4: Cleanup

#### 4.1 Delete Old Filter System Files
- Delete `domain/model/filter/FilterStack.kt` (399 lines)
- Delete `domain/model/filter/UnifiedFilter.kt` (171 lines)
- Delete `domain/model/filter/FilterStackBuilder.kt`

#### 4.2 Replace RecordsFilterContext with TransactionFilter
- Remove RecordsFilterContext sealed class
- Update navigation to use TransactionFilter directly
- Remove URL encoding/decoding logic
- Update TransactionListScreen parameter from filterContext to filter
- Update all navigation calls to use TransactionFilter
- Files: `ui/navigation/RecordsFilterContext.kt` (delete), `ui/screens/TransactionListScreen.kt`, `ui/navigation/OssWalletNavGraph.kt`, `ui/navigation/Screen.kt`

#### 4.3 Update Saved Filter System
- Store TransactionFilter directly instead of FilterStack
- Remove FilterStack conversion logic
- Add migration script to convert existing saved filters
- File: `data/local/dao/FilterDao.kt`

#### 4.4 Remove All References
- Search and remove all usages of FilterStack, UnifiedFilter
- Update all imports
- Update all test files

### Phase 5: Testing
- Test each card's navigation to verify timeline range is preserved
- Verify TransactionListScreen applies filter correctly
- Test FilterManager merge logic with multiple filters
- Test add/remove operations with automatic recompute
- Test edge cases: AllTime timeline, custom ranges, period-based ranges
- Test backward compatibility with existing UI

## Files to Modify

### New Files
1. `domain/model/FilterManager.kt` - Lightweight filter manager with multi-filter support
2. `domain/model/TransactionFilterExtensions.kt` - Convenience extension functions
3. `app/src/test/java/com/osscabinet/ossWallet/domain/model/FilterManagerTest.kt` - Unit tests for FilterManager

### Modified Files
1. `domain/service/TimelineService.kt` - Add getDateRange() function, refactor filterByTimelineRange to use it (✅ COMPLETED)
2. `ui/viewmodel/TransactionViewModel.kt` - Replace filter states with FilterManager
3. `ui/screens/TransactionListScreen.kt` - Replace filterContext parameter with filter parameter
4. `ui/navigation/OssWalletNavGraph.kt` - Update navigation to use TransactionFilter
5. `ui/navigation/Screen.kt` - Update route definitions
6. `ui/dashboard/components/CashFlowCard.kt` - Update callback signatures to use TransactionFilter
7. `ui/dashboard/components/CurrencyBalancesCard.kt` - Update callback signatures to use TransactionFilter
8. `ui/dashboard/components/AccountsCard.kt` - Add onRecordsClick callback with timeline
9. `ui/dashboard/components/BudgetsCard.kt` - Add timelineRange and onRecordsClick with TransactionFilter
10. `ui/dashboard/components/NetWorthCard.kt` - Add click handlers with TransactionFilter
11. `ui/dashboard/DashboardScreen.kt` - Update all card instantiation calls
12. `data/local/dao/FilterDao.kt` - Update saved filter system to use TransactionFilter

### Deleted Files
1. `domain/model/filter/FilterStack.kt` (399 lines)
2. `domain/model/filter/UnifiedFilter.kt` (171 lines)
3. `domain/model/filter/FilterStackBuilder.kt`
4. `ui/navigation/RecordsFilterContext.kt`

## Expected Outcome

After implementation:
- Clicking on Income in CashFlowCard → Opens TransactionList with isIncome=true AND card's timeline range
- Clicking on Currency in CurrencyBalancesCard → Opens TransactionList with currency filter AND card's timeline range
- Clicking on Account in AccountsCard → Opens TransactionList with account filter AND card's timeline range
- Clicking on Budget in BudgetsCard → Opens TransactionList with category filter AND card's timeline range
- Clicking on Income/Expense badges in NetWorthCard → Opens TransactionList with type filter AND card's timeline range

All navigation from dashboard cards will preserve the timeline context selected by the user, providing a seamless drill-down experience.
