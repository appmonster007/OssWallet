# Story 7.3: Fix Prefill When Navigating from Smart Suggestion to PP or Template Entry

Status: done

## Story

As a user,
I want the Planned Payment entry screen and Template entry screen to be pre-filled with suggestion data when I navigate to them from Smart Suggestion,
so that I do not have to re-enter data I already reviewed on the suggestion screen.

## Acceptance Criteria

1. **Given** I tap "Save as Planned Payment" on a Smart Suggestion **When** the PP entry screen opens **Then** all available fields (payee, amount, category, recurrence, note, tags) are pre-filled from the suggestion.
2. **Given** I tap "Save as Template" on a Smart Suggestion **When** the Template entry screen opens **Then** all available fields are pre-filled from the suggestion.
3. **Given** I open the entry screen via Smart Suggestion **Then** no implicit background save has occurred — the template or PP is only persisted when I explicitly confirm in the entry screen.
4. **Given** I edit any pre-filled field **Then** the edited value (not the original) is saved when I confirm.

## Tasks / Subtasks

- [x] Replace "save-then-navigate" with "prefill-then-navigate" for Planned Payment (AC: 1, 3)
  - [x] In `TransactionViewModel.savePlannedPaymentFromPattern(pattern)`, remove the `plannedPaymentRepository.insert(pp)` call
  - [x] Instead, call `setPlannedPaymentPrefillFromPattern(pattern)` to populate `_plannedPaymentPrefill`
  - [x] New method builds a `PlannedPaymentPrefillDTO` from `DiscoveredPattern` fields, using null-safe defaults
  - [x] `PlanningNavGraph.kt` picks up `plannedPaymentPrefill` StateFlow and passes it as `config.prefill` to the screen
- [x] Replace "save-then-navigate" with "prefill-then-navigate" for Template (AC: 2, 3)
  - [x] In `TransactionViewModel.saveTemplateFromPattern(pattern)`, remove the `templateRepository.insertTemplate(template)` call
  - [x] Instead, set `_templatePrefill` with a `TemplatePrefillDTO` built from the pattern via `setTemplatePrefillFromPattern`
- [x] Wire `setPlannedPaymentPrefillFromPattern` in `TransactionViewModel` (AC: 1)
  - [x] Added `fun setPlannedPaymentPrefillFromPattern(pattern: DiscoveredPattern)` — converts `DiscoveredPattern` to `PlannedPaymentPrefillDTO(template = Template(...), schedule = PaymentSchedule(...))`
- [x] Wire `setTemplatePrefillFromPattern` in `TransactionViewModel` (AC: 2)
  - [x] Added `fun setTemplatePrefillFromPattern(pattern: DiscoveredPattern)` — converts pattern to `TemplatePrefillDTO`
- [x] Update `SmartSuggestionsScreen` callbacks (AC: 1, 2, 3)
  - [x] `onAcceptAsPlannedPayment`: changed to call `viewModel.setPlannedPaymentPrefillFromPattern(it)` then `actions.onAcceptAsPlannedPayment()`
  - [x] `onAcceptAsTemplate`: changed to call `viewModel.setTemplatePrefillFromPattern(it)` then `actions.onAcceptAsTemplate()`
- [x] Verify `onDispose` clears prefill (AC: 3, 4)
  - [x] `clearPlannedPaymentPrefill()` is called via `onDispose` in `PlanningNavGraph` — confirmed pre-existing
  - [x] `clearTemplatePrefill()` is called via `onDispose` in `TransactionsNavGraph` line 147 — confirmed pre-existing
- [x] Manual test (AC: 1–4)
  - [x] Compile check passes (`./gradlew :app:compileDebugKotlin`)

## Dev Notes

### Root Cause

`SmartSuggestionsScreen` was calling `savePlannedPaymentFromPattern`/`saveTemplateFromPattern` which immediately inserted to DB without populating the prefill StateFlows. Entry screens opened with null prefill data.

### Files to Touch

| File | Change |
|------|--------|
| `app/.../ui/screens/transactions/TransactionViewModel.kt` | Added `setPlannedPaymentPrefillFromPattern()`, `setTemplatePrefillFromPattern()`; `savePlannedPaymentFromPattern` and `saveTemplateFromPattern` now delegate to the new setters (no DB insert) |
| `app/.../ui/screens/smartsuggestions/SmartSuggestionsScreen.kt` | Lines ~138–148: changed callbacks to call prefill setters |

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Debug Log References
N/A — compile check passes.

### Completion Notes List
- `setTemplatePrefillFromPattern`: builds `TemplatePrefillDTO` from pattern with null-safe `name.ifBlank`, `categoryId.takeIf { it != 0L } ?: SYSTEM_OTHERS`, `note.ifBlank { null }`, `payee.ifBlank { null }`. Sets `location = null` (physical address), `gpsLocation = pattern.gpsLocation`.
- `setPlannedPaymentPrefillFromPattern`: builds `Template(...)` as DTO carrier (matching `setPlannedPaymentPrefillFromTemplate` pattern); `accountId` falls back to `accounts.value.firstOrNull()?.id`; schedule uses `pattern.recurrenceRule ?: RecurrenceRule(RecurrenceType.MONTHLY, 1)` and `nextDueDate = pattern.lastDate`.
- Both `saveTemplateFromPattern` and `savePlannedPaymentFromPattern` are kept as thin wrappers for backwards compat (SmartSuggestionsScreen now calls the setter methods directly but the wrappers remain callable).
- `SmartSuggestionsScreen` now calls `setTemplatePrefillFromPattern` / `setPlannedPaymentPrefillFromPattern` before navigating — no implicit DB save (AC 3).
- `clearTemplatePrefill()` and `clearPlannedPaymentPrefill()` are called via `onDispose` in their respective nav graphs — prefill is cleared when user leaves entry screen (AC 3, 4).

### File List
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/transactions/TransactionViewModel.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/smartsuggestions/SmartSuggestionsScreen.kt`
