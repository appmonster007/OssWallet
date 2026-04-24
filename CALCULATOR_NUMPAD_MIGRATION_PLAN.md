# Calculator Numpad Migration Plan

## Objective
Transform all numpads in the app to be full calculators that support negative values, with automatic transaction type toggling based on value sign. Expenses will be stored as negative amounts in the database.

## Current State Analysis

### Numpad Usage Locations
1. **OnboardingScreen.kt** - Uses `CompactNumpad` for initial balance entry
2. **TransactionEntryScreen.kt** - Uses `Numpad.kt` for transaction amount entry
3. **AddTemplateScreen.kt** - Uses `Numpad.kt` for template amount entry
4. **EditBudgetScreen.kt** - Uses `Numpad.kt` for budget amount entry

### Transaction Model
- `amount: BigDecimal` - Currently always positive
- `transactionType: String` - "income" or "expense" (explicitly set)
- Type and amount are independent

### Current Numpad Layout (Numpad.kt)
- Row 1: C, ÷, ×, DEL
- Row 2: 7, 8, 9, −
- Row 3: 4, 5, 6, +
- Row 4-5: 1, 2, 3, ., 0, 00, DONE/=
- Supports basic arithmetic (+, −, ×, ÷)
- Always returns positive values (uses `.abs()`)

### Current CompactNumpad Layout (OnboardingScreen.kt)
- Row 1: C, ÷, ×, DEL
- Row 2: 7, 8, 9, +/-
- Row 3: 4, 5, 6, −
- Row 4: 1, 2, 3, +
- Row 5: ., 0, 00, DONE
- Has +/- toggle button
- Returns signed values

## Target State

### New Numpad Layout (Unified)
| | Col 1 | Col 2 | Col 3 | Col 4 |
| :--- | :--- | :--- | :--- | :--- |
| **Row 1** | `C` | `()` | `%` | `÷` |
| **Row 2** | `7` | `8` | `9` | `×` |
| **Row 3** | `4` | `5` | `6` | `−` |
| **Row 4** | `1` | `2` | `3` | `+` |
| **Row 5** | `.` | `0` | `⌫` | `=` |

### New Behavior
1. **Full Calculator Support**
   - Parentheses `()` for grouping operations
   - Percentage `%` for percentage calculations
   - Backspace `⌫` for deleting last character
   - All arithmetic operations support negative results

2. **Transaction Type Auto-Toggle**
   - If final value > 0: transactionType = "income"
   - If final value < 0: transactionType = "expense"
   - If final value = 0: keep current transactionType or default to "income"

3. **Database Storage**
   - Expenses stored as negative amounts
   - Income stored as positive amounts
   - `transactionType` field still maintained for backward compatibility and filtering

## Migration Strategy

### Phase 1: Create New CalculatorNumpad Component
**Priority: HIGH**
- Create `CalculatorNumpad.kt` in `ui/components/`
- Implement new layout with C, (), %, ÷, 7, 8, 9, ×, 4, 5, 6, −, 1, 2, 3, +, ., 0, ⌫, =
- Implement full calculator logic with parentheses support
- Implement percentage calculations
- Support negative intermediate and final results
- Return signed values (can be negative)
- Add callback for value changes to trigger transaction type updates

### Phase 2: Update Transaction Type Logic
**Priority: HIGH**
- Create utility function to determine transaction type from value sign:
  ```kotlin
  fun determineTransactionType(amount: BigDecimal): String {
      return if (amount.signum() >= 0) "income" else "expense"
  }
  ```
- Update ViewModels to auto-update transaction type when amount changes
- Update UI to show transaction type based on amount sign

### Phase 3: Update Database Schema (if needed)
**Priority: MEDIUM**
- Verify if database schema changes are required
- If amount field is already BigDecimal, no schema change needed
- Add migration to convert existing data:
  - Expenses: Keep amount positive, but logic will treat as negative
  - OR: Migrate existing expenses to negative amounts
- **Decision**: Keep existing data as-is, use transactionType for backward compatibility
- New transactions will store signed amounts

### Phase 4: Update OnboardingScreen
**Priority: HIGH**
- Replace `CompactNumpad` with new `CalculatorNumpad`
- Update `BalanceEditSheet` to handle signed values
- Update balance display to show negative values properly
- Remove redundant +/- button logic (now handled by calculator)
- Update initial balance transaction creation logic (already done in previous commit)

### Phase 5: Update TransactionEntryScreen
**Priority: HIGH**
- Replace `Numpad.kt` with new `CalculatorNumpad`
- Add transaction type auto-toggle logic in ViewModel
- Update UI to show transaction type based on amount sign
- Remove manual transaction type toggle button (or keep as override)
- Update amount display to show negative values

### Phase 6: Update AddTemplateScreen
**Priority: MEDIUM**
- Replace `Numpad.kt` with new `CalculatorNumpad`
- Templates typically use positive amounts
- Add validation to ensure template amounts are positive (or allow negative for expense templates)

### Phase 7: Update EditBudgetScreen
**Priority: MEDIUM**
- Replace `Numpad.kt` with new `CalculatorNumpad`
- Budgets typically use positive amounts (limits)
- Add validation to ensure budget amounts are positive

### Phase 8: Update Display Logic Across All Screens
**Priority: HIGH**
- Update transaction list displays to show negative amounts for expenses
- Update dashboard cards to handle negative values
- Update analytics calculations to handle signed amounts
- Update account balance calculations
- Update all places where transaction amounts are displayed

### Phase 9: Update Calculation Logic
**Priority: HIGH**
- Update balance calculations to use signed amounts
- Update income/expense summaries to use sign detection
- Update percentage calculations
- Update trend calculations
- Update filter logic (transaction type filters still work, but can also filter by sign)

### Phase 10: Testing & Validation
**Priority: HIGH**
- Test all numpad operations with negative values
- Test transaction creation with negative amounts
- Test initial balance setup with negative values
- Test template creation
- Test budget editing
- Test all display screens
- Test analytics calculations
- Test filtering and sorting
- Test data export/import

## Risk Assessment

### High Risk Areas
1. **Balance Calculations** - All balance calculations need to account for signed amounts
2. **Analytics** - Income/expense summaries need to use sign detection instead of transactionType
3. **Backward Compatibility** - Existing data has positive amounts with transactionType
4. **Display Logic** - All transaction displays need to show negative values properly

### Mitigation Strategies
1. **Backward Compatibility Layer**
   - Keep transactionType field
   - Use transactionType for existing data
   - Use sign for new data
   - Gradually migrate to sign-based logic

2. **Comprehensive Testing**
   - Unit tests for calculator logic
   - Integration tests for transaction creation
   - UI tests for all screens
   - Regression tests for existing features

3. **Gradual Rollout**
   - Start with numpad component
   - Then update one screen at a time
   - Test thoroughly before moving to next

## Implementation Order

1. Create CalculatorNumpad component
2. Update OnboardingScreen (isolated, good for testing)
3. Update TransactionEntryScreen (main use case)
4. Update display logic
5. Update calculation logic
6. Update other screens (templates, budgets)
7. Comprehensive testing

## Data Migration Strategy

### Option A: Keep Existing Data As-Is
- Existing transactions keep positive amounts
- transactionType field determines direction
- New transactions use signed amounts
- Display logic handles both cases
- Pros: Safer, less data migration
- Cons: Mixed data model, more complex logic

### Option B: Migrate All Data to Signed Amounts
- Convert all expense transactions to negative amounts
- Remove reliance on transactionType for direction
- Simpler logic going forward
- Pros: Cleaner model, simpler logic
- Cons: Risky data migration, potential data loss

### Recommended: Option A (Hybrid Approach)
- Keep existing data as-is for safety
- Use transactionType for existing transactions
- Use sign for new transactions
- Display logic handles both cases
- Gradually migrate over time
- Future: Consider data migration after validation

## Success Criteria

1. All numpads use new CalculatorNumpad with unified layout
2. Negative values are supported throughout the app
3. Transaction type auto-toggles based on value sign
4. Expenses stored as negative amounts (new transactions)
5. All displays show negative values correctly
6. All calculations handle signed amounts correctly
7. Existing features continue to work (backward compatibility)
8. No data loss or corruption
9. All tests pass
