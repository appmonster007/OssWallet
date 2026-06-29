# Story 6.3: Fix Double-Negative Amount Display & Global Audit

Status: ready-for-dev

## Story

As a user,
I want amount values to display with a single correct sign (e.g., `–220.00 INR`, not `--220.00 INR`),
so that I can read my financial data without confusing formatting artefacts.

## Acceptance Criteria

1. **Given** an expense template amount **When** it is displayed in the Link Template sheet **Then** it shows exactly one negative prefix (e.g., `-220.00 INR`), never double-negative (`--220.00 INR`).
2. **Given** any transaction, template, or planned payment amount anywhere in the app **When** it is rendered **Then** it shows at most one sign prefix; positive amounts show no prefix or `+`.
3. **Given** the fix is applied at the display layer **Then** it does not alter the stored value in the database (storage stays correct — only display is fixed).
4. **Given** a global search for remaining double-sign occurrences **Then** no other screen shows the same artefact.

## Tasks / Subtasks

- [ ] Fix `AeonTemplateSelection.kt` double-negative on EXPENSE amounts (AC: 1, 2)
  - [ ] In `AeonTemplateSelection.kt` line ~172, change:
    ```kotlin
    "$prefix${template.amount} ${template.currency ?: ""}"
    ```
    to:
    ```kotlin
    "$prefix${template.amount.abs()} ${template.currency ?: ""}"
    ```
  - [ ] Verify `prefix` for INCOME is `"+"` and for EXPENSE is `"-"`; with `.abs()` on the amount this always produces a single sign
- [ ] Audit all other amount display sites for the same pattern (AC: 2, 4)
  - [ ] Search for `"$prefix${` or `"\$sign\${` or any concatenation of a sign string with a raw `amount` field that may be negative
  - [ ] Check: `AeonTransactionItem`, `AeonTransactionSplitSheet`, `PlannedPaymentDetailScreen`, planned payment list composables, and Quick Add confirmation step
  - [ ] Fix any additional occurrences found
- [ ] Verify database values are not changed (AC: 3)
  - [ ] Confirm no `.abs()` or sign-change is applied before `transactionDao.insertTransaction` or `templateRepository.insertTemplate`
- [ ] Verify positive and zero amounts are unaffected (AC: 2)
  - [ ] `BigDecimal.abs()` on a positive or zero amount is a no-op — confirm in a quick unit test

## Dev Notes

### Root Cause

Expense template amounts are stored as negative `BigDecimal` values (e.g., `-220.00`) in the database. `AeonTemplateSelection.kt` already prepends a `"-"` prefix for the EXPENSE case:
```kotlin
val (prefix, color) = when (template.entryMode) {
    EntryMode.INCOME -> "+" to PositiveAmountColor
    EntryMode.TRANSFER -> "⇄" to AccentBlue
    else -> "-" to AppColors.error   // prefix = "-"
}
Text("$prefix${template.amount} ${template.currency ?: ""}") // → "--220.00 INR"
```
The raw `template.amount` is already `-220.00`, so concatenation produces `--220.00`.

The fix is to display `template.amount.abs()` (the magnitude) and let `prefix` carry the sign.

### Audit Scope

Search codebase for patterns like:
- `"$sign"` or `"${prefix}"` concatenated directly with a raw `amount` or `BigDecimal` field
- Any usage of `template.amount`, `tx.amount`, `pattern.averageAmount`, `existing.amount` in a Text composable without `.abs()`

Key files to check:
- `app/.../ui/components/entry/AeonTemplateSelection.kt` (confirmed bug — line ~172)
- `app/.../ui/screens/transactions/components/` (transaction list items)
- `app/.../ui/screens/plannedpayments/` (planned payment list/detail)
- `app/.../ui/screens/dashboard/quickadd/` (Quick Add confirmation)

### Files to Touch

| File | Change |
|------|--------|
| `app/src/main/java/com/osscabinet/ossWallet/ui/components/entry/AeonTemplateSelection.kt` | Line ~172: `template.amount` → `template.amount.abs()` |
| Any additional display sites found during audit | Same `.abs()` fix |

### Testing

- Unit test: construct a `Template` with `amount = BigDecimal("-220.00")`, `entryMode = EXPENSE`; assert the rendered string is `"-220.00 INR"` not `"--220.00 INR"`
- Visual check: open Link Template sheet in the app with an expense template; confirm single sign

### References

- `app/src/main/java/com/osscabinet/ossWallet/ui/components/entry/AeonTemplateSelection.kt` line ~166–175
- `app/src/main/java/com/osscabinet/ossWallet/ui/theme/` — `PositiveAmountColor`, `AccentBlue`, `AppColors.error` for sign colour tokens

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
