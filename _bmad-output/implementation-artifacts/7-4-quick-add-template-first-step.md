# Story 7.4: Quick Add Sheet — Template-First Selection Step

Status: ready-for-dev

## Story

As a user,
I want the Quick Add sheet to show my saved templates first when I have any,
so that I can log a recurring transaction in one tap instead of re-selecting category and payee every time.

## Acceptance Criteria

1. **Given** I open the Quick Add sheet and have at least one template **When** the sheet renders **Then** the template selection tab is active by default (not the category tab).
2. **Given** I open the Quick Add sheet and have no templates **When** the sheet renders **Then** it opens directly on the category tab (current behaviour).
3. **Given** the template tab is active **Then** I can toggle to the category tab via the existing `AtelierModeToggle`.
4. **Given** the templates list is loaded asynchronously **When** templates arrive after first composition **Then** the tab switches to the template tab automatically (not stuck on category).
5. **Given** the fix is applied **Then** no other Quick Add step (account, amount, confirmation) is affected.

## Tasks / Subtasks

- [ ] Fix async template-load race in `QuickAddTemplateStep` (AC: 1, 4)
  - [ ] In `QuickAddTemplateStep.kt` line ~78, the current code is:
    ```kotlin
    val initialShowCategories = remember { templates.isEmpty() }
    ```
    `remember { }` freezes the value on first composition. If templates load asynchronously (ViewModel emits empty list first), this is always `true` and the UI stays on categories even when templates arrive.
  - [ ] Replace with a derived state that reacts when templates first become non-empty:
    ```kotlin
    var showCategories by rememberSaveable { mutableStateOf(true) }
    val hasTemplates = templates.isNotEmpty()
    // Automatically switch to template tab the first time templates arrive
    LaunchedEffect(hasTemplates) {
        if (hasTemplates && showCategories) showCategories = false
    }
    ```
    This uses `LaunchedEffect(hasTemplates)` so it fires once when `hasTemplates` transitions from false → true, and not again.
  - [ ] Keep `showCategories` as a `var` so the `AtelierModeToggle` can still toggle it manually (AC: 3)
  - [ ] Use `rememberSaveable` so the selected tab survives configuration changes (optional but recommended)
- [ ] Verify no-template path (AC: 2)
  - [ ] When `templates` is empty and `hasTemplates` is always false, the `LaunchedEffect` never switches tabs → sheet opens on category tab ✓
- [ ] Verify `AtelierModeToggle` still toggles (AC: 3)
  - [ ] Confirm the toggle calls `showCategories = !showCategories` (or equivalent) and the new derivation doesn't fight it
- [ ] Regression check (AC: 5)
  - [ ] Complete a Quick Add flow (template → account → amount → confirm) and a category flow (category → account → amount → confirm); confirm both work end to end

## Dev Notes

### Root Cause

`QuickAddTemplateStep.kt` line ~78:
```kotlin
val initialShowCategories = remember { templates.isEmpty() }
```
`remember { … }` runs the lambda only on the first composition and never recalculates. If `templates` is an asynchronously loaded `StateFlow` (typical for Room-backed repositories), its first emission may be an empty list. The `remember` freezes `initialShowCategories = true` (show categories), and when templates arrive on the next emission, nothing updates this value.

### Fix Pattern

```kotlin
// Before (frozen on first composition):
val initialShowCategories = remember { templates.isEmpty() }
var showCategories by remember { mutableStateOf(initialShowCategories) }

// After (reactive to first template arrival):
var showCategories by rememberSaveable { mutableStateOf(true) }
val hasTemplates = templates.isNotEmpty()
LaunchedEffect(hasTemplates) {
    if (hasTemplates) showCategories = false  // switch to template tab once
}
```

The `LaunchedEffect(hasTemplates)` key fires when `hasTemplates` changes. The `if (hasTemplates)` guard ensures we only auto-switch when templates exist; if they are later removed (e.g., deleted), the user keeps whatever tab they're on.

### Files to Touch

| File | Change |
|------|--------|
| `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/quickadd/steps/QuickAddTemplateStep.kt` | Lines ~74–79: replace `remember { templates.isEmpty() }` with `rememberSaveable + LaunchedEffect` pattern |

### References

- `app/.../ui/screens/dashboard/quickadd/steps/QuickAddTemplateStep.kt` lines ~56–160
- `app/.../ui/screens/dashboard/quickadd/QuickAddSheet.kt` line ~95 — `val templates by viewModel.templates.collectAsState()`
- `app/.../ui/screens/dashboard/quickadd/steps/QuickAddAtelier.kt` — `AtelierModeToggle` definition

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
