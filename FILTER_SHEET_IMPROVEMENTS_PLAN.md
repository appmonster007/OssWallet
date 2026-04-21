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
**Status**: ⏳ PENDING
**Root Cause**: ContextIndicator only has a single Reset button that clears all filters to None
**Current State**:
- ContextIndicator shows all fields in expanded view
- Only one Reset button clears everything
- No individual field removal capability
**Resolution**:
1. Add individual remove button to each ContextFilterField
2. When a field is removed:
   - Update localFilter to remove that specific field
   - Call onApplyFilter to update ViewModel
   - Keep other context fields intact
3. Add onRemoveField callback to ContextIndicator
4. Pass callback to ContextFilterField components
**Files to Modify**:
- `ui/components/transactions/FilterSheetContent.kt`
  - Add onRemoveField callback to ContextIndicator
  - Add remove button to ContextFilterField
  - Handle individual field removal logic

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
**Status**: ✅ COMPLETED (PARTIAL)
**Root Cause**: Filter sheet structure has nested scroll containers and no sticky positioning
**Current Structure**:
- FilterSheetContent (Column)
  - AeonPeriodSelector (top)
  - ContextIndicator (if context)
  - ActiveFiltersSummary
  - Column with verticalScroll
    - Header (Title + Reset)
    - Saved Filters
    - FilterFields
    - Apply Button
**Resolution**:
1. ✅ Make the entire FilterSheetContent scrollable using LazyColumn
2. ⏳ Make AeonPeriodSelector sticky at top (NOT YET IMPLEMENTED)
3. ✅ Remove nested verticalScroll from inner Column
4. ✅ Ensure proper spacing and dividers
**Files to Modify**:
- ✅ `ui/components/transactions/FilterSheetContent.kt`
  - ✅ Replace Column with LazyColumn
  - ⏳ Add stickyHeader for AeonPeriodSelector (PENDING)
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

### Issue #9: Currency Filter for Aggregate Currency Balances
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
3. ✅ **Issue #5**: Make filter sheet scrollable (improves usability on small screens) - PARTIAL (sticky header pending)

### Phase 2: Medium Priority (Feature Enhancements) - ✅ COMPLETED
4. ⏳ **Issue #1**: Individual context field removal (enhances flexibility) - PENDING
5. ✅ **Issue #7**: Currency filter implementation (enables complete filtering)
6. ✅ **Issue #8**: Filter dropdowns show unfiltered options (data flow improvement)

### Phase 3: Low Priority (Already Done) - ✅ VERIFIED
7. ✅ **Issue #0**: Verify MVVM pattern (already correct)
8. ✅ **Issue #3**: Verify reset behavior (already correct)
9. ✅ **Issue #6**: Verify card navigation (already correct)

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
- Remaining: Issue #1 (individual context field removal) - 1-2 hours
- Remaining: Issue #5 (sticky header for timeline) - 30 minutes
- **Total Completed**: 5.5-7.5 hours
- **Total Remaining**: 1.5-2.5 hours
