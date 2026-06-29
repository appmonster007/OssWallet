# Story 7.1: Suppress Duplicate Numpad Launch on Planned Payment Entry Screen

Status: ready-for-dev

## Story

As a user,
I want the calculator numpad to open exactly once when I enter the Planned Payment entry screen,
so that I am not forced to dismiss a duplicate keyboard before I can start entering data.

## Acceptance Criteria

1. **Given** I navigate to a new Planned Payment entry **When** the screen fully opens **Then** the calculator numpad appears exactly once.
2. **Given** the numpad is visible **When** I dismiss it and then tap the amount field **Then** it opens exactly once again.
3. **Given** the fix is applied **Then** numpad behaviour on the standard Transaction entry screen is unaffected.

## Tasks / Subtasks

- [ ] Reproduce the double-open and identify the trigger (AC: 1)
  - [ ] Navigate to Planned Payment entry from the Planned Payments list (Add button) — does the numpad double-open?
  - [ ] Navigate from Smart Suggestion screen → "Save as Planned Payment" — does the numpad double-open?
  - [ ] Add a temporary log inside `PlannedPaymentEntryEvent.ShowNumpad(show=true)` processing in `PlannedPaymentEntryViewModel.kt` (~line 553) to count how many times it fires on screen open
- [ ] Identify root cause from logs
  - [ ] Check `PlanningNavGraph.kt` line ~93: `val prefill by vms.transactionViewModel.plannedPaymentPrefill.collectAsStateWithLifecycle()` — if `prefill` changes from null to non-null after navigation, `LaunchedEffect(config.plannedPaymentId, config.prefill)` re-fires `viewModel.initialize(...)`. Trace whether `initialize` triggers a second `ShowNumpad`
  - [ ] Check `AeonImpactHeaderCard` for any `LaunchedEffect` or focus side-effect that calls `onValueClick` on initial composition
  - [ ] Check if `PlannedPaymentEntryViewModel.initialize` auto-fires `ShowNumpad(true)` for new entries (no `plannedPaymentId`) — compare with TransactionEntryViewModel behaviour
- [ ] Apply the narrowest fix that prevents the second open (AC: 1, 2)
  - [ ] If the cause is a `LaunchedEffect` firing twice due to `prefill` recomposition: add a `var numpadAutoOpened by remember { mutableStateOf(false) }` guard in the screen, or deduplicate in the ViewModel using a flag
  - [ ] If the cause is `initialize` calling `ShowNumpad(true)`: add an idempotency guard (e.g., `if (state.showNumpad) return`) before firing
  - [ ] Do NOT suppress the numpad entirely for new PP entries — it should auto-open once on new entry if that is the current transaction entry screen behaviour
- [ ] Regression check (AC: 3)
  - [ ] Open Transaction entry → confirm numpad still opens once (if it auto-opens) or once on tap

## Dev Notes

### Suspect #1 — Prefill StateFlow recomposition

`PlanningNavGraph.kt` line ~93:
```kotlin
val prefill by vms.transactionViewModel.plannedPaymentPrefill.collectAsStateWithLifecycle()
config = PlannedPaymentEntryConfig(plannedPaymentId = …, prefill = prefill)
```
`LaunchedEffect(config.plannedPaymentId, config.prefill)` in `PlannedPaymentEntryScreen.kt` line 53 re-runs `viewModel.initialize(…)` every time `prefill` changes. If `prefill` transitions from `null → null → someValue` (or vice versa), two calls to `initialize` occur. Check whether `initialize` side-effects `ShowNumpad`.

The `initialize` guard at line ~107:
```kotlin
if (lastInitParams == (plannedPaymentId to prefill)) return
if (lastInitParams?.second != null && prefill == null) return
```
May not prevent double-fire if the prefill arrives concurrently with the navigation.

### Suspect #2 — Auto-open in initialize for new entries

If `PlannedPaymentEntryViewModel.initialize` calls `onEvent(PlannedPaymentEntryEvent.ShowNumpad(true, …))` in the new-entry path (no `plannedPaymentId`), a second recomposition-driven call would open the numpad twice. Search the full body of `initialize` for `ShowNumpad`.

### Files to Touch

| File | Purpose |
|------|---------|
| `app/src/main/java/com/osscabinet/ossWallet/ui/screens/plannedpayments/entry/PlannedPaymentEntryViewModel.kt` | Primary fix location |
| `app/src/main/java/com/osscabinet/ossWallet/ui/screens/plannedpayments/entry/PlannedPaymentEntryScreen.kt` | Possible guard in LaunchedEffect |
| `app/src/main/java/com/osscabinet/ossWallet/ui/navigation/graphs/PlanningNavGraph.kt` | If prefill recomposition is the cause |

### References

- `PlannedPaymentEntryScreen.kt` line 53 — `LaunchedEffect(config.plannedPaymentId, config.prefill)`
- `PlannedPaymentEntryViewModel.kt` line ~106 — `initialize()`
- `PlannedPaymentEntryViewModel.kt` line ~553 — `ShowNumpad` event handler
- `PlannedPaymentEntrySheets.kt` line ~150 — `if (state.showNumpad) AeonCalculatorSheet(...)`
- `PlanningNavGraph.kt` line ~93 — `prefill` StateFlow collection

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
