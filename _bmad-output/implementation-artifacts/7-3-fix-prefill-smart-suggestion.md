# Story 7.3: Fix Prefill When Navigating from Smart Suggestion to PP or Template Entry

Status: ready-for-dev

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

- [ ] Replace "save-then-navigate" with "prefill-then-navigate" for Planned Payment (AC: 1, 3)
  - [ ] In `TransactionViewModel.savePlannedPaymentFromPattern(pattern)`, remove the `plannedPaymentRepository.insert(pp)` call
  - [ ] Instead, call `setPlannedPaymentPrefillFromPattern(pattern)` (new method or adapt existing `setPlannedPaymentPrefillFromTemplate`) to populate `_plannedPaymentPrefill`
  - [ ] New method should build a `PlannedPaymentPrefillDTO` from `DiscoveredPattern` fields, using null-safe defaults (same guards as Story 7.2)
  - [ ] The `actions.onAcceptAsPlannedPayment()` nav call in `SmartSuggestionsScreen.kt` already navigates to `PlannedPaymentEntryRoute()` — the `PlanningNavGraph` picks up `plannedPaymentPrefill` StateFlow and passes it as `config.prefill` to the screen (line ~93-99 of `PlanningNavGraph.kt`)
- [ ] Replace "save-then-navigate" with "prefill-then-navigate" for Template (AC: 2, 3)
  - [ ] In `TransactionViewModel.saveTemplateFromPattern(pattern)`, remove the `templateRepository.insertTemplate(template)` call
  - [ ] Instead, set `_templatePrefill` with a `TemplatePrefillDTO` built from the pattern
  - [ ] Check how `TemplateEntryRoute()` composable reads `templatePrefill` from `TransactionViewModel` (look in the nav graph composable for `TemplateEntryRoute`) and confirm the flow is equivalent to the PP flow
- [ ] Wire `setPlannedPaymentPrefillFromPattern` in `TransactionViewModel` (AC: 1)
  - [ ] Add `fun setPlannedPaymentPrefillFromPattern(pattern: DiscoveredPattern)` — converts `DiscoveredPattern` to `PlannedPaymentPrefillDTO(template = Template(...), schedule = PaymentSchedule(...))`
  - [ ] Use the existing `Template` domain model as the DTO carrier (same shape as `setPlannedPaymentPrefillFromTemplate`)
- [ ] Wire `setTemplatePrefillFromPattern` in `TransactionViewModel` (AC: 2)
  - [ ] Add `fun setTemplatePrefillFromPattern(pattern: DiscoveredPattern)` — converts pattern to `TemplatePrefillDTO`
  - [ ] Read how `_templatePrefill` is consumed in the Template entry nav composable and ensure the DTO shape matches
- [ ] Update `SmartSuggestionsScreen` callbacks (AC: 1, 2, 3)
  - [ ] `onAcceptAsPlannedPayment`: change to `{ viewModel.setPlannedPaymentPrefillFromPattern(it); actions.onAcceptAsPlannedPayment() }` (remove the old `savePlannedPaymentFromPattern` call)
  - [ ] `onAcceptAsTemplate`: change to `{ viewModel.setTemplatePrefillFromPattern(it); actions.onAcceptAsTemplate() }` (remove the old `saveTemplateFromPattern` call)
- [ ] Verify `onDispose` clears prefill (AC: 3, 4)
  - [ ] `PlannedPaymentEntryActions.onDispose` already calls `vms.transactionViewModel.clearPlannedPaymentPrefill()` — confirm it is called when the PP entry screen is popped
  - [ ] Confirm equivalent `clearTemplatePrefill()` is called on Template entry screen dispose
- [ ] Manual test (AC: 1–4)
  - [ ] Smart Suggestion → "Save as Planned Payment" → PP entry opens with all fields pre-filled; edit one field; confirm → only one PP saved with edited value
  - [ ] Smart Suggestion → "Save as Template" → Template entry opens pre-filled; confirm → template saved once

## Dev Notes

### Root Cause

`SmartSuggestionsScreen` currently calls:
```kotlin
// Planned Payment path
onAcceptAsPlannedPayment = {
    viewModel.savePlannedPaymentFromPattern(it)   // IMMEDIATELY saves to DB — no prefill set
    actions.onAcceptAsPlannedPayment()             // navigates to PP entry (now reads null prefill)
}
// Template path
onAcceptAsTemplate = {
    viewModel.saveTemplateFromPattern(it)          // IMMEDIATELY saves to DB — no prefill set
    actions.onAcceptAsTemplate()                   // navigates to Template entry (now reads null prefill)
}
```
`_plannedPaymentPrefill` and `_templatePrefill` are never populated, so the entry screens open with no pre-fill data. The immediate DB saves also create an unintended "implicit save" before the user confirms.

### Existing Infrastructure (reuse, don't reinvent)

**PP prefill:**
- `_plannedPaymentPrefill: MutableStateFlow<PlannedPaymentPrefillDTO?>` — already in `TransactionViewModel`
- `setPlannedPaymentPrefillFromTemplate(template)` — already exists (~line 826) — adapt for pattern
- `clearPlannedPaymentPrefill()` — already exists (~line 837)
- `PlanningNavGraph.kt` line ~93: already reads `plannedPaymentPrefill` and passes to `PlannedPaymentEntryConfig.prefill`

**Template prefill:**
- `_templatePrefill: MutableStateFlow<TemplatePrefillDTO?>` — already in `TransactionViewModel`
- `clearTemplatePrefill()` — already exists
- Find how the Template entry nav composable reads `templatePrefill` (check the nav graph for `TemplateEntryRoute`)

### `DiscoveredPattern` → `Template` mapping

```kotlin
fun setPlannedPaymentPrefillFromPattern(pattern: DiscoveredPattern) {
    val template = Template(
        id = 0,
        name = pattern.name.ifBlank { pattern.payee.ifBlank { "Planned Payment" } },
        categoryId = pattern.categoryId.takeIf { it != 0L } ?: Category.SYSTEM_OTHERS,
        amount = pattern.averageAmount,
        entryMode = pattern.entryMode,
        accountId = pattern.accountId,
        currency = pattern.currency,
        note = pattern.note,
        payee = pattern.payee,
        profileId = 0,  // not persisted yet — placeholder
        tags = pattern.tags,
        paymentMethod = pattern.paymentMethod,
        location = pattern.gpsLocation
    )
    _plannedPaymentPrefill.value = PlannedPaymentPrefillDTO(
        template = template,
        schedule = PaymentSchedule(
            recurrenceRule = pattern.recurrenceRule ?: RecurrenceRule(RecurrenceType.MONTHLY, 1),
            startDate = System.currentTimeMillis(),
            nextDueDate = pattern.lastDate
        )
    )
}
```

### Files to Touch

| File | Change |
|------|--------|
| `app/.../ui/screens/transactions/TransactionViewModel.kt` | Add `setPlannedPaymentPrefillFromPattern()`, `setTemplatePrefillFromPattern()`; remove save logic from existing `savePlannedPaymentFromPattern` and `saveTemplateFromPattern` |
| `app/.../ui/screens/smartsuggestions/SmartSuggestionsScreen.kt` | Lines ~138–148: change callbacks to call prefill setters instead of save methods |

### References

- `app/.../ui/screens/transactions/TransactionViewModel.kt` lines ~772–840
- `app/.../ui/screens/smartsuggestions/SmartSuggestionsScreen.kt` lines ~133–148
- `app/.../ui/navigation/graphs/PlanningNavGraph.kt` lines ~86–107
- `app/.../ui/navigation/graphs/SettingsNavGraph.kt` lines ~150–164

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
