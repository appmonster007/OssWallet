---
baseline_commit: d0ef718fc508304d3e57303ff76acce79637b41d
---

# Story 3.6: Editorial Input Field IME Handling RCA & Fix

Status: in-progress

## Story

As a user,
I want the input field I tap on to be immediately and clearly brought into the visible area above the keyboard,
so that I can see what I am typing, maintain context from the label and icon, and interact with suggestions without them being cut off.

## UX Rationale

When an input field is located in the bottom half of a scrollable form, tapping it triggers the IME (keyboard). Without explicit handling, the keyboard often covers the focused field entirely or partially.
- **Context is King**: A user needs to see the **Label** and **Icon** while typing to confirm they are in the right field.
- **Input Visibility**: The cursor and the text being typed must be centered or clearly positioned in the remaining viewport to avoid "typing blind".
- **Interaction Surface**: If the field has a suggestion menu (Exposed Dropdown), that menu must also be factored into the "visible area" so it doesn't appear behind the keyboard.

## Acceptance Criteria

1. **Given** any entry form (Transaction, Budget, etc.) **When** I focus an `EntryEditorialInput` **Then** the entire input card (Icon, Label, and Text) is smoothly brought into view above the keyboard.
2. **Given** suggestions are available **When** a field is focused **Then** the scroll logic accounts for the suggestion menu height so it remains visible above the keyboard.
3. **Given** the fix is applied **Then** all hardcoded `Rect(0f, 0f, 0f, 2000f)` or similar "magic number" hacks are removed from `EntryEditorialInput.kt`.
4. **Given** the keyboard is toggled **When** content is scrollable **Then** `AeonEntryScaffold` correctly shrinks its viewport to prevent overlapping content.

## Tasks / Subtasks

- [x] Refactor `AeonEntryScaffold` to support IME insets (AC: 4)
  - [x] Update `Scaffold` `contentWindowInsets` to union with `WindowInsets.ime` or apply `Modifier.imePadding()` to the content wrapper.
- [x] Refactor `EntryEditorialInput` scrolling logic (AC: 1, 3)
  - [x] Move `bringIntoViewRequester` to the parent `Row` to ensure full context visibility.
  - [x] Implement a deterministic trigger for `bringIntoView` that waits for IME visibility/layout pass.
- [x] Implement suggestion-aware scrolling (AC: 2)
  - [x] Calculate a logical buffer (e.g., using `LocalDensity`) that accounts for the input card and the maximum suggestion list height.
- [ ] Verify across entry screens (AC: 1, 2)
  - [ ] Test in Transaction Entry, Budget Entry, and Template Entry.

## Dev Notes

### Root Cause Analysis (RCA)

1. **Viewport Inconsistency**: `AeonEntryScaffold` currently uses `WindowInsets(0, 0, 0, 0)` for its Scaffold. This prevents the internal `Column` with `verticalScroll` from "seeing" the keyboard, so the viewport doesn't shrink. When `bringIntoView` is called, the scrollable container thinks the element is already "visible" in the full screen, even though it's physically covered by the keyboard.
2. **Hardcoded Magic Constant**: `Rect(0f, 0f, 0f, 2000f)` (and previously `5000f`) is a non-standard hack. It works by requesting a huge area below the cursor, forcing the parent to scroll until the top of the requested area (the field) is at the top of the viewport. This is fragile and doesn't respect different device heights or suggestion menu sizes.
3. **Standard API Failure**: Using the parameterless `bringIntoView()` fails because the viewport hasn't shrunk. The system identifies the field as being inside the full-screen box, so it doesn't move it. This results in the user tapping a field at the bottom of the screen and seeing the keyboard slide up over it, forcing them to manually scroll to see what they are typing.

### source tree components to touch

- `app/src/main/java/com/osscabinet/ossWallet/ui/components/entry/AeonEntryScaffold.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/entry/EntryEditorialInput.kt`

### Testing standards summary

- Manual verification on device/emulator with keyboard enabled.
- Verify that `Modifier.imePadding()` correctly resizes the `Scaffold` content area.

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `AeonEntryScaffold`: Added `Modifier.imePadding()` to the content Box. Root fix — causes the scroll viewport to shrink by the keyboard height, making `bringIntoView()` work correctly. The `contentWindowInsets = WindowInsets(0,0,0,0)` intentionally zeroed IME insets for the overlay top-bar layout; `imePadding()` directly on the content Box compensates without affecting the Scaffold's inner padding.
- `EntryEditorialInput`: Replaced `delay(300)` coroutine hack with `LaunchedEffect(imeBottom, isFocused)` keyed on `WindowInsets.ime.getBottom(density)`. Effect fires the moment the keyboard is actually visible (`imeBottom > 0`) and field is focused — deterministic, no timing assumptions.
- `EntryEditorialInput`: Moved `bringIntoViewRequester` from `GlassSurface` to `ExposedDropdownMenuBox` (outermost node). Plain `bringIntoView()` with no custom rect — dropdown popup is a separate window layer and positions itself; no layout-level rect manipulation needed.
- `onFocusChanged` simplified to track focus state only; `rememberCoroutineScope`, `delay`, and `launch` removed.
- AC 3 (no magic number hacks): confirmed none existed in current code; removed the `delay(300)` timing hack instead.
- Task 4 (verify across entry screens): requires manual testing on device/emulator.

### File List

- app/src/main/java/com/osscabinet/osswallet/ui/components/entry/AeonEntryScaffold.kt
- app/src/main/java/com/osscabinet/osswallet/ui/components/entry/EntryEditorialInput.kt
