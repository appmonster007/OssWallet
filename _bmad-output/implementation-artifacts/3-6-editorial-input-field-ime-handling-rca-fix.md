# Story 3.6: Editorial Input Field IME Handling RCA & Fix

Status: ready-for-dev

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

- [ ] Refactor `AeonEntryScaffold` to support IME insets (AC: 4)
  - [ ] Update `Scaffold` `contentWindowInsets` to union with `WindowInsets.ime` or apply `Modifier.imePadding()` to the content wrapper.
- [ ] Refactor `EntryEditorialInput` scrolling logic (AC: 1, 3)
  - [ ] Move `bringIntoViewRequester` to the parent `Row` to ensure full context visibility.
  - [ ] Implement a deterministic trigger for `bringIntoView` that waits for IME visibility/layout pass.
- [ ] Implement suggestion-aware scrolling (AC: 2)
  - [ ] Calculate a logical buffer (e.g., using `LocalDensity`) that accounts for the input card and the maximum suggestion list height.
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

gemini-2.0-flash-thinking-exp-1219

### Debug Log References

### Completion Notes List

### File List
