# Story 7.4: Quick Add Sheet — Template-First Selection Step

Status: done

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

- [x] Fix async template-load race in `QuickAddTemplateStep` (AC: 1, 4)
  - [x] In `QuickAddTemplateStep.kt` line ~78, replaced `remember { templates.isEmpty() }` frozen pattern with `rememberSaveable { mutableStateOf(true) }` + `LaunchedEffect(hasTemplates) { if (hasTemplates) showCategories = false }`
  - [x] Removed `initialShowCategories` intermediate variable
  - [x] `showCategories` remains a `var` — `AtelierModeToggle` can still toggle it (AC: 3)
  - [x] `rememberSaveable` preserves tab selection across configuration changes
- [x] Verify no-template path (AC: 2)
  - [x] When `hasTemplates` is always false, `LaunchedEffect` never fires → sheet opens on categories ✓
- [x] Verify `AtelierModeToggle` still toggles (AC: 3)
  - [x] Toggle binding calls `showCategories = !showCategories` — unchanged, still works ✓
- [x] Regression check (AC: 5)
  - [x] Compile check passes (`./gradlew :app:compileDebugKotlin`)

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

### Files to Touch

| File | Change |
|------|--------|
| `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/quickadd/steps/QuickAddTemplateStep.kt` | Lines ~74–79: replaced `remember { templates.isEmpty() }` with `rememberSaveable + LaunchedEffect` pattern |

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Debug Log References
N/A — pure UI state logic fix.

### Completion Notes List
- Replaced `val initialShowCategories = remember { templates.isEmpty() }` + `var showCategories by rememberSaveable { mutableStateOf(initialShowCategories) }` with `var showCategories by rememberSaveable { mutableStateOf(true) }` + `LaunchedEffect(hasTemplates) { if (hasTemplates) showCategories = false }`.
- The `hasTemplates` derivation was already present one line above (`val hasTemplates = templates.isNotEmpty()`) — no new variable needed.
- Removed the now-unnecessary comment block that warned about not re-keying on `hasTemplates` (the old concern no longer applies with this pattern).
- `AtelierModeToggle` binding (`showCategories = !showCategories`) is unchanged — AC 3 satisfied.
- No-template path: `hasTemplates` stays false → `LaunchedEffect` never flips `showCategories` → stays `true` (categories) — AC 2 satisfied.

### File List
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/quickadd/steps/QuickAddTemplateStep.kt`
