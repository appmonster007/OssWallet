# Filter Sheet Improvements Plan

## Root Cause Analysis & Resolution Plan

### Issue #0: Records Screen Data Query Pattern
**Status**: ✅ ALREADY IMPLEMENTED CORRECTLY
**Root Cause**: N/A - Already following MVVM best practices
**Current State**:
- TransactionListScreen collects data from ViewModels via StateFlow
- TransactionViewModel uses flatMapLatest on _recordsFilter to query filtered transactions
- Data flows: ViewModel → DB Query → StateFlow → UI
**Resolution**: No action needed - architecture is correct

---

### Issue #1: Context Filter Individual Field Removal
**Status**: ✅ COMPLETED
**Root Cause**: Context was shown as single chip with reset all functionality
**Current State**:
- Context expanded into individual field chips
- Each chip can be removed independently
- Supports all context types
**Resolution**:
1. ✅ Expand context into individual filter chips
2. ✅ Each field has its own remove button
3. ✅ When a field is removed, calls appropriate onRemoveFilter
4. ✅ Keeps other context fields intact
5. ✅ Uses categoryMap for category names
**Files to Modify**:
- ✅ `ui/components/transactions/FilterSheetContent.kt`
  - ✅ Expand context into individual chips in ActiveFiltersSummary
  - ✅ Support ByAccount, ByAccountAndType, ByAccountAndCurrency, Custom
  - ✅ Each chip calls onRemoveFilter with specific field and value
  - ✅ Use categoryMap for category names in context

---

### Issue #2: Transaction Type Selection UI Glitch
**Status**: ✅ COMPLETED (FIXED)
**Root Cause**: Enabled restriction on "All" button caused UI glitching when switching between types
**Current State** (FilterComponents.kt lines 164-222):
- SingleChoiceSegmentedButtonRow with All/Income/Expense
- Pre-applied indicator shown
- User can click "All" to remove pre-applied filter
**Resolution**:
1. ✅ Removed enabled restriction on "All" button
2. ✅ User can now always select All to clear type filter
3. ✅ Pre-applied indicator still shows for context
4. ✅ Fixes glitching issue when switching between types
5. ✅ Provides better UX by giving user full control
**Files to Modify**:
- ✅ `ui/components/transactions/FilterComponents.kt`
  - ✅ Remove enabled state check on "All" button
  - ✅ Allow user to override pre-applied filter by selecting All

---

### Issue #3: Reset Button Behavior
**Status**: ✅ ALREADY IMPLEMENTED CORRECTLY
**Root Cause**: N/A - Already resets to None (All)
**Current State**:
- Context Reset button clears to TransactionFilter() (None)
- Reset All button also clears to None
- Both call onResetToInitial callback
**Resolution**: No action needed - behavior is correct per requirements

---

### Issue #4: Active Filters Showing IDs Instead of Names
**Status**: ✅ COMPLETED
**Root Cause**: FilterSheetContent uses raw IDs instead of resolving to names
**Current State** (FilterSheetContent.kt line 545):
```kotlin
label = "Category: $categoryId"
```
**Resolution**:
1. ✅ Pass categoryMap to ActiveFiltersSummary
2. ✅ Resolve category IDs to names when displaying
3. Similarly for any other ID-based fields (accounts already use accountNames)
**Files to Modify**:
- ✅ `ui/components/transactions/FilterSheetContent.kt`
  - ✅ Add categoryMap parameter to ActiveFiltersSummary
  - ✅ Update category filter chip to use category names
  - ✅ Ensure all ID-based fields resolve to names

---

### Issue #5: Filter Sheet Scrollability
**Status**: ✅ COMPLETED
**Root Cause**: Filter sheet structure has nested scroll containers and no sticky positioning
**Current Structure**:
- FilterSheetContent (Column)
  - LazyColumn with sticky header
    - AeonPeriodSelector (sticky)
    - ActiveFiltersSummary
    - Filter fields content
**Resolution**:
1. ✅ Make the entire FilterSheetContent scrollable using LazyColumn
2. ✅ Make AeonPeriodSelector sticky at top
3. ✅ Remove nested verticalScroll from inner Column
4. ✅ Ensure proper spacing and dividers
5. ✅ Add ExperimentalFoundationApi opt-in annotation
**Files to Modify**:
- ✅ `ui/components/transactions/FilterSheetContent.kt`
  - ✅ Replace Column with LazyColumn
  - ✅ Add stickyHeader for AeonPeriodSelector
  - ✅ Add ExperimentalFoundationApi import and opt-in
  - ✅ Remove nested verticalScroll
  - ✅ Adjust content padding and dividers

---

### Issue #6: Card Navigation to Filtered Records
**Status**: ✅ ALREADY IMPLEMENTED IN PREVIOUS WORK
**Root Cause**: N/A
**Current State**:
- AccountDetailScreen cards navigate with appropriate filters
- DashboardScreen cards navigate with appropriate filters
- AccountsViewScreen cards navigate with appropriate filters
**Resolution**: No action needed - already implemented

---

### Issue #7: Currency Filter for Aggregate Currency Balances
**Status**: ✅ COMPLETED

---

### Issue #8: Filter Dropdowns Show Unfiltered Options
**Status**: ✅ COMPLETED
**Root Cause**: Currency dropdown was deriving options from accounts list inline, making the data flow implicit
**Current State**:
- Currency dropdown derived from accounts.map { it.currencyCode } inside FilterFields
- Data flow was implicit and not controllable by parent
**Resolution**:
1. ✅ Add allAvailableCurrencies parameter to FilterFields
2. ✅ Pass explicitly from FilterSheetContent (currently from accounts, can be from transactions later)
3. ✅ Use parameter instead of deriving inline
4. ✅ Makes data flow explicit and allows parent to control unfiltered options
**Files to Modify**:
- ✅ `ui/components/transactions/FilterComponents.kt`
  - ✅ Add allAvailableCurrencies parameter
  - ✅ Use parameter in currency dialog with fallback to accounts
- ✅ `ui/components/transactions/FilterSheetContent.kt`
  - ✅ Pass allAvailableCurrencies derived from accounts

---

### Issue #9: Merge Context and Active Filters
**Status**: ✅ COMPLETED
**Root Cause**: Two separate sections for context filter and active filters created redundancy
**Current State**:
- ContextIndicator showed pre-applied filters separately
- ActiveFiltersSummary showed current active filters separately
- Both had reset functionality
**Resolution**:
1. ✅ Remove ContextIndicator composable
2. ✅ Add context information to ActiveFiltersSummary expanded view
3. ✅ Add Reset button to ActiveFiltersSummary header
4. ✅ Reset button clears all filters and updates records correctly
5. ✅ Context shown as filter chip with pre-applied indicator
6. ✅ Simplifies UI by merging similar sections
**Files to Modify**:
- ✅ `ui/components/transactions/FilterSheetContent.kt`
  - ✅ Remove ContextIndicator and ContextFilterField composables
  - ✅ Add initialFilterContext and onResetAll to ActiveFiltersSummary
  - ✅ Show context as filter chip in expanded view
  - ✅ Add Reset button in header
  - ✅ Remove separate ContextIndicator section from LazyColumn

---

### Issue #11: Currency Dropdown Shows All Supported Currencies
**Status**: ✅ COMPLETED
**Root Cause**: Implementation used account.currencyCode instead of account.allowedCurrencies
**Current State**:
- Currency dropdown only showed primary currency of each account
- Users could not filter by currencies in allowedCurrencies but not set as currencyCode
**Resolution**:
1. ✅ Changed from currencyCode to allowedCurrencies for currency derivation
2. ✅ Use flatMap to get union of all allowedCurrencies from all accounts
3. ✅ Fallback to currencyCode if allowedCurrencies is empty for an account
4. ✅ Updated both FilterSheetContent and FilterComponents
**Files to Modify**:
- ✅ `ui/components/transactions/FilterSheetContent.kt`
  - ✅ Change accounts.map { it.currencyCode } to accounts.flatMap { it.allowedCurrencies }
  - ✅ Add fallback logic for empty allowedCurrencies
- ✅ `ui/components/transactions/FilterComponents.kt`
  - ✅ Update fallback logic to use allowedCurrencies

---

### Issue #10: Currency Filter for Aggregate Currency Balances
**Status**: ✅ COMPLETED
**Root Cause**: TransactionFilter doesn't support currency filtering
**Current State**:
- RecordsFilterContext.ByAccountAndCurrency exists but not implemented in query logic
- CurrencyBalancesCard navigates to None filter
**Resolution**:
1. ✅ Add currency field to TransactionFilter (if not already present)
2. ✅ Update TransactionViewModel query logic to filter by currency
3. ✅ Update CurrencyBalancesCard navigation to use currency filter
4. ✅ Test currency filtering across all contexts
**Files to Modify**:
- ✅ Check if TransactionFilter already has currencies field (line 112 in FilterComponents.kt suggests it does)
- ✅ `ui/viewmodel/TransactionViewModel.kt`
  - ✅ Ensure getFilteredTransactions supports currency filtering
- ✅ `ui/dashboard/components/CurrencyBalancesCard.kt`
  - ✅ Update onCurrencyClick to pass currency filter
- ✅ `ui/accountdetail/AccountDetailScreen.kt`
  - ✅ Update CurrencyBalancesCard onCurrencyClick
- ✅ `ui/screens/AccountsViewScreen.kt`
  - ✅ Update CurrencyBalancesCard onCurrencyClick
- ✅ `ui/dashboard/DashboardScreen.kt`
  - ✅ Update CurrencyBalancesCard onCurrencyClick

---

## Implementation Priority

### Phase 1: High Priority (Critical UX Issues) - ✅ COMPLETED
1. ✅ **Issue #4**: Fix category names in active filters (quick win, high visibility)
2. ✅ **Issue #2**: Fix transaction type selection UI (prevents user confusion)
3. ✅ **Issue #5**: Make filter sheet scrollable with sticky header (improves usability)

### Phase 2: Medium Priority (Feature Enhancements) - ✅ COMPLETED
4. ✅ **Issue #1**: Individual context field removal (enhances flexibility)
5. ✅ **Issue #7**: Currency filter implementation (enables complete filtering)
6. ✅ **Issue #8**: Filter dropdowns show unfiltered options (data flow improvement)
7. ✅ **Issue #9**: Merge context and active filters (UI simplification)
8. ✅ **Issue #11**: Currency dropdown shows all supported currencies (multi-currency support)

### Phase 3: Low Priority (Already Done) - ✅ VERIFIED
9. ✅ **Issue #0**: Verify MVVM pattern (already correct)
10. ✅ **Issue #3**: Verify reset behavior (already correct)
11. ✅ **Issue #6**: Verify card navigation (already correct)

---

## Testing Strategy

1. **Unit Tests**: Test filter logic in ViewModel
2. **UI Tests**: Test filter sheet interactions
3. **Integration Tests**: Test navigation with filters
4. **Manual Testing**: Test on different screen sizes

---

## Risk Assessment

- **Low Risk**: ✅ Issues #4 (simple data mapping), #0/#3/#6 (already correct), #8 (data flow improvement)
- **Medium Risk**: ✅ Issues #2 (UI state management), #5 (scroll behavior changes - partial)
- **High Risk**: ✅ Issue #7 (currency filtering may require DB query changes - completed successfully)

---

## Estimated Timeline

- Phase 1: ✅ COMPLETED (2-3 hours)
- Phase 2: ✅ COMPLETED (3-4 hours)
- Phase 3: ✅ COMPLETED (30 minutes verification)
- **Total Completed**: 6-8 hours
- **Total Remaining**: 0 hours
- **ALL ISSUES COMPLETED** ✅
