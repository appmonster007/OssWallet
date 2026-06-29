# Story 3.4: Editorial Input Field Solid-Mode RCA & Fix

Status: ready-for-dev

## Story

As a user,
I want input fields to render with a clean, solid container style when glassmorphism is disabled,
so that the app looks polished regardless of which surface mode is active.

## Acceptance Criteria

1. **Given** glassmorphism is disabled in app settings **When** I open any screen with editorial input fields (NOTE, PAYEE, Amount, etc.) **Then** each field renders as a solid filled container — correct background, border radius, and border — with no malformed outline or transparent artifact.
2. **Given** glassmorphism is enabled **When** I view the same screens **Then** the glass rendering is completely unaffected.
3. **Given** the fix is applied **Then** it is implemented at the shared component level so all future call sites inherit the correct behaviour automatically.

## Tasks / Subtasks

- [ ] Fix `EntryEditorialInput` fallback colour in non-glass mode (AC: 1, 3)
  - [ ] In `EntryEditorialInput.kt` line ~128, change `else -> AppColors.surfaceContainerLow` to `else -> AppColors.inputSurface` in the `GlassSurface` fallback colour expression
- [ ] Fix `EntryActionCard` fallback colour in non-glass mode (AC: 1, 3)
  - [ ] In `EntryActionCard.kt` line ~101, apply the same `surfaceContainerLow` → `inputSurface` change
- [ ] Verify no regression in glass mode (AC: 2)
  - [ ] Build debug APK, toggle glassmorphism off and on in settings, inspect NOTE and PAYEE fields on transaction entry and planned payment entry screens
- [ ] Write or update a test (AC: 1)
  - [ ] Add a screenshot/visual unit test or a conditional colour assertion in the existing entry component tests (if any exist in `src/androidTest/`)

## Dev Notes

### Root Cause

`EntryEditorialInput` is a `GlassSurface`-backed text field. When glassmorphism is disabled, `GlassSurface` falls back to a solid colour via its `fallbackColor` parameter. The component was passing `AppColors.surfaceContainerLow` as the fallback, which is the **same colour** as the parent `EntrySection` container. Material 3 filled-field spec requires the field background to be `surfaceContainerHighest` (`AppColors.inputSurface`) to be visually distinct from the surrounding surface.

Result: in solid mode the filled background is invisible, leaving only the faint `outlineVariant@50%` border — the "malformed outline" the user sees.

### Files to Touch

| File | Change |
|------|--------|
| `app/src/main/java/com/osscabinet/ossWallet/ui/components/entry/EntryEditorialInput.kt` | Line ~128: `else -> AppColors.surfaceContainerLow` → `else -> AppColors.inputSurface` |
| `app/src/main/java/com/osscabinet/ossWallet/ui/components/entry/EntryActionCard.kt` | Line ~101: same substitution |

### Colour Tokens

- `AppColors.inputSurface` = `surfaceContainerHighest` (M3 filled-field spec)
- `AppColors.surfaceContainerLow` = the **parent container** colour — wrong for an inner field
- `AppColors.recessedSurface` = tone-on-tone used in the Atelier panels; NOT appropriate for editorial fields

### Affected Screens (verify during QA)

- Transaction entry (note, payee fields)
- Planned payment entry (name, note, payee fields)
- Template entry (name, note, payee fields)
- Any future screen using `EntryEditorialInput` or `EntryActionCard`

### References

- `app/src/main/java/com/osscabinet/ossWallet/ui/components/entry/EntryEditorialInput.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/entry/EntryActionCard.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/GlassSurface.kt` (fallback colour applied at lines 194–209)
- `app/src/main/java/com/osscabinet/ossWallet/ui/theme/Color.kt` — colour token definitions

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
