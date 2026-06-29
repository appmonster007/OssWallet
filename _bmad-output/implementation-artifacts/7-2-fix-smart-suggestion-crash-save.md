# Story 7.2: Fix Smart Suggestion Crash When Saving as Planned Payment

Status: ready-for-dev

## Story

As a user,
I want to save a Smart Suggestion as a Planned Payment without the app crashing,
so that I can convert AI-detected patterns into recurring payments reliably.

## Acceptance Criteria

1. **Given** I tap "Save as Planned Payment" on any suggestion **When** the save is processed **Then** no unhandled exception is thrown (no NPE, IllegalStateException, etc.).
2. **Given** optional fields on `DiscoveredPattern` (e.g., `accountId`, `recurrenceRule`) are null **When** the PP is constructed **Then** safe defaults are applied (e.g., first available account, MONTHLY recurrence).
3. **Given** the save succeeds **Then** the user receives a confirmation (snackbar or navigation) and is not left on a broken screen.
4. **Note:** As part of Story 7.3, the "save immediately" approach will be replaced with a "prefill + navigate" approach. This story focuses only on removing the crash; the prefill work is in 7.3.

## Tasks / Subtasks

- [ ] Identify the crash cause in `savePlannedPaymentFromPattern` (AC: 1, 2)
  - [ ] Read `TransactionViewModel.savePlannedPaymentFromPattern` (~line 800) and trace the `PlannedPayment(...)` construction
  - [ ] Check `PlannedPayment` domain model for non-null required fields — in particular `accountId` (String vs String?), `categoryId` (Long?), `name` (String)
  - [ ] Confirm `DiscoveredPattern.accountId` is `String?` nullable; if `PlannedPayment.accountId` is non-null, a forced unwrap or non-null assertion would crash
- [ ] Add null-safe guards for all optional pattern fields (AC: 1, 2)
  - [ ] `pattern.accountId`: if null, resolve the first available wallet account from the repository (inject `accountRepository` into `TransactionViewModel` or use `_accounts` StateFlow which is already loaded)
  - [ ] `pattern.categoryId`: if null or 0, default to `Category.SYSTEM_OTHERS`
  - [ ] `pattern.recurrenceRule`: already guarded with `?: RecurrenceRule(RecurrenceType.MONTHLY, 1)` — verify this is sufficient
  - [ ] `pattern.name`: if blank, use `pattern.payee.ifBlank { "Planned Payment" }`
- [ ] Wrap the coroutine body with structured error handling (AC: 1, 3)
  - [ ] Wrap the entire `viewModelScope.launch { withProfile { … } }` body in a `try/catch (e: Exception)` block
  - [ ] On failure: emit a user-visible snackbar via `_snackbarMessage.value = "Failed to save: ${e.message}"`
- [ ] Manual test (AC: 1, 3)
  - [ ] On a device/emulator, navigate to Smart Suggestions, tap "Save Planned" on a suggestion — verify no crash and snackbar appears

## Dev Notes

### Likely Crash Cause

`DiscoveredPattern.accountId` is `String?` (nullable). `PlannedPayment` may require a non-null `accountId`. The current code:
```kotlin
val pp = PlannedPayment(
    …
    accountId = pattern.accountId,  // passes nullable String? into non-null field → crash
    …
)
```
Verify by reading the `PlannedPayment` data class in the domain layer.

### Fix Pattern

```kotlin
fun savePlannedPaymentFromPattern(pattern: DiscoveredPattern) {
    viewModelScope.launch {
        try {
            withProfile { profileId ->
                val safeAccountId = pattern.accountId
                    ?: _accounts.value.firstOrNull()?.id
                    ?: return@withProfile  // no account available — bail gracefully
                val pp = PlannedPayment(
                    name = pattern.name.ifBlank { pattern.payee.ifBlank { "Planned Payment" } },
                    accountId = safeAccountId,
                    categoryId = pattern.categoryId.takeIf { it != 0L } ?: Category.SYSTEM_OTHERS,
                    …
                )
                plannedPaymentRepository.insert(pp)
                _snackbarMessage.value = "Planned payment created"
            }
        } catch (e: Exception) {
            _snackbarMessage.value = "Could not save: ${e.message}"
        }
    }
}
```

### Files to Touch

| File | Change |
|------|--------|
| `app/src/main/java/com/osscabinet/ossWallet/ui/screens/transactions/TransactionViewModel.kt` | Lines ~800–824: null-safe guards + try/catch in `savePlannedPaymentFromPattern` |

### Domain model to read first

- `domain/model/PlannedPayment.kt` — confirm which fields are non-nullable
- `domain/model/dto/PlannedPaymentPrefillDTO.kt` — for Story 7.3 awareness
- `domain/service/discovery/DiscoveredPattern.kt` — field nullability

### References

- `app/.../ui/screens/transactions/TransactionViewModel.kt` lines ~800–824
- `app/.../ui/screens/smartsuggestions/SmartSuggestionsScreen.kt` lines ~138–148 (save callbacks)

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
