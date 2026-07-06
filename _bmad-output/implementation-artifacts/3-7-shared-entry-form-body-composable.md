# Story 3.7: Shared Entry Form Body Composable (Tier 3 Entry Form Consolidation)

Status: backlog

## Story

As a developer maintaining the OssWallet codebase,
I want a shared `BaseEntryFormBody` composable that encapsulates the common scaffolding of the Transaction, PlannedPayment, and Template entry forms,
so that structural changes to the form layout (scroll container, tap-to-dismiss, padding) only need to happen in one place and there is no risk of the three forms drifting apart.

## Background / Context

This is **Tier 3** of a three-tier entry-form consolidation effort:

- **Tier 1 (done):** Renamed inconsistent state fields (`noteUserPart`→`note`, `selectedTagNames`→`selectedTags`, `availableTags`→`allTags`, etc.) across `PlannedPaymentEntryState` and `TemplateEntryState`.
- **Tier 2 (done):** Extracted `BaseEntryViewModel` (`ui/util/BaseEntryViewModel.kt`) that provides a single `entryFormSnapshot: StateFlow<EntryFormSnapshot>` combining profile core data, shortlisted currencies, and exchange rates. All three ViewModels now extend `BaseEntryViewModel` and subscribe to the snapshot in `init`.
- **Tier 3 (this story):** Extract a `BaseEntryFormBody` composable that owns the outer scaffold shared by all three forms (Box + Column + scroll + IME + tap-to-dismiss + top/bottom padding spacers), leaving only form-specific content as composable lambdas.

## Acceptance Criteria

1. **Given** a new `BaseEntryFormBody` composable exists in `ui/components/entry/`, **When** all three form-body files (`TransactionFormBody`, `PlannedPaymentFormBody`, `TemplateFormBody`) are updated to delegate to it, **Then** the outer Box/Column/scroll/imePadding/statusBarsPadding/pointerInput scaffolding code lives in exactly one place.
2. **Given** `BaseEntryFormBody` accepts `headerContent`, `contextContent`, `detailContent`, and `sheetContent` composable lambdas, **When** each form passes its own sections, **Then** the rendered output is visually identical to the current per-form implementation (no regressions).
3. **Given** `TransactionFormBody`, `PlannedPaymentFormBody`, and `TemplateFormBody` are updated, **When** their file sizes are compared to pre-Tier-3 versions, **Then** each loses at least 30 lines of boilerplate without any AC or functionality regression.
4. **Given** the split-mode section in `TransactionFormBody` is unique to that form, **When** `BaseEntryFormBody` is designed, **Then** that section remains in `TransactionFormBody` (either as an additional `extraContent` lambda slot or simply left after the `BaseEntryFormBody` call).
5. **Given** the schedule-builder section in `PlannedPaymentFormBody` is unique to that form, **When** `BaseEntryFormBody` is designed, **Then** it is handled the same way as AC 4 — a slot or left outside the base.

## Tasks / Subtasks

- [ ] Audit the three form bodies for shared scaffolding (AC: 1)
  - [ ] Diff `TransactionFormBody.kt`, `PlannedPaymentFormBody.kt`, `TemplateFormBody.kt` side-by-side; document every duplicated line and every unique line.
  - [ ] Document which unique sections need dedicated lambda slots in `BaseEntryFormBody`.
- [ ] Create `ui/components/entry/BaseEntryFormBody.kt` (AC: 1, 2)
  - [ ] Outer `Box(fillMaxSize, statusBarsPadding, imePadding, pointerInput tap-to-dismiss)`.
  - [ ] Inner `Column(verticalScroll, horizontalPadding, spacedBy)`.
  - [ ] Top/bottom `Spacer` driven by `contentPadding`.
  - [ ] Accept `headerContent`, `contextContent`, `detailContent`, `extraContent`, `sheetContent` lambda slots.
  - [ ] Keep the component stateless — no `state` or `onEvent` params; callers pass pre-built lambdas.
- [ ] Refactor `TransactionFormBody.kt` to use `BaseEntryFormBody` (AC: 2, 3, 4)
  - [ ] Remove duplicated scaffolding code.
  - [ ] Pass split-mode section via `extraContent` lambda slot (or append it after the base call).
- [ ] Refactor `PlannedPaymentFormBody.kt` to use `BaseEntryFormBody` (AC: 2, 3, 5)
  - [ ] Remove duplicated scaffolding code.
  - [ ] Pass schedule-builder section via `extraContent` lambda or append it appropriately.
- [ ] Refactor `TemplateFormBody.kt` to use `BaseEntryFormBody` (AC: 2, 3)
  - [ ] Remove duplicated scaffolding code.
- [ ] Verify no visual/behavioral regressions (AC: 2)
  - [ ] Run the app and navigate all three entry forms; verify scroll, IME handling, tap-to-dismiss, and sheet orchestration are unaffected.

## Dev Notes

### Scope Boundaries

- **In scope:** Outer Box/Column/scroll scaffolding and Spacer padding. The `EntrySheetOrchestrator` / `TemplateSheetOrchestrator` / `PlannedPaymentSheetOrchestrator` calls at the bottom of each form body.
- **Out of scope:** Sheet orchestrator contents (those are already handled through `FormSheetState` and `EntrySheetOrchestrator`), ViewModel logic, state types.

### Current Duplicate Scaffolding (as of Tier 1/2 merge)

All three form bodies share this pattern verbatim:

```kotlin
Box(
    modifier = modifier
        .fillMaxSize()
        .statusBarsPadding()
        .imePadding()
        .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AeonTheme.dimensions.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
    ) {
        Spacer(Modifier.height(contentPadding.calculateTopPadding()))
        // ... form sections ...
        Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
    }
    // Sheet orchestrator call
}
```

### Files to Modify

| File | Change |
|------|--------|
| `ui/components/entry/BaseEntryFormBody.kt` | **NEW** — shared scaffolding composable |
| `ui/screens/transactions/entry/components/TransactionFormBody.kt` | Delegate to `BaseEntryFormBody` |
| `ui/screens/plannedpayments/entry/components/PlannedPaymentFormBody.kt` | Delegate to `BaseEntryFormBody` |
| `ui/screens/templates/entry/components/TemplateFormBody.kt` | Delegate to `BaseEntryFormBody` |

### Predecessor Work

- `BaseEntryViewModel` lives at `ui/util/BaseEntryViewModel.kt` (Tier 2, done).
- `EntryFormSnapshot` lives at `ui/components/entry/EntryFormSnapshot.kt` (Tier 2, done).
- All entry state field names are now normalized (Tier 1, done): `note`, `selectedTags`, `allTags`, `allPayees`, `allNotes`.

### Design System Constraints

- Scaffolding uses `AeonTheme.dimensions.screenPadding` and `AeonTheme.dimensions.spacingLg` — do not hardcode pixel values.
- `AeonTheme` tokens must be read inside the composable (they are `@Composable`-scoped). The `BaseEntryFormBody` is a `@Composable` function, so this is safe.

### Testing

Manual smoke test:
1. Open Transaction Entry → create new expense → verify scroll, keyboard, sheet.
2. Open PlannedPayment Entry → create new payment → verify schedule builder still visible.
3. Open Template Entry → create new template → verify exchange rate disclaimer row.
No unit-testable logic is introduced; this is pure composable refactoring.

### References

- Current form files: `app/src/main/java/com/osscabinet/ossWallet/ui/screens/transactions/entry/components/TransactionFormBody.kt`
- Current form files: `app/src/main/java/com/osscabinet/ossWallet/ui/screens/plannedpayments/entry/components/PlannedPaymentFormBody.kt`
- Current form files: `app/src/main/java/com/osscabinet/ossWallet/ui/screens/templates/entry/components/TemplateFormBody.kt`
- `BaseEntryViewModel`: `app/src/main/java/com/osscabinet/ossWallet/ui/util/BaseEntryViewModel.kt`
- `EntryFormSnapshot`: `app/src/main/java/com/osscabinet/ossWallet/ui/components/entry/EntryFormSnapshot.kt`
- Aeon design tokens: `app/src/main/java/com/osscabinet/ossWallet/ui/theme/`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
