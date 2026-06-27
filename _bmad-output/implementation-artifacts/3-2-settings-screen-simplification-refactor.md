---
baseline_commit: ''
---

# Story 3.2: Settings Screen Simplification & Refactor

## Status
**Status:** ready-for-dev
**Epic:** Epic 3: Design System Consolidation ("H" Identity)
**ID:** 3.2

## Story

As a user,
I want a clean, legible, and simple Settings interface,
so that I can manage my preferences without being overwhelmed by "over-designed" clutter.

## Acceptance Criteria

**Given** the current "messy" and inconsistent Settings screen
**When** I refactor the screen to use a high-fidelity "Editorial" list pattern
**Then** all non-essential decorative elements are removed
**And** the screen strictly adheres to the "H" Light/Dark/AMOLED palettes via `AppColors` tokens
**And** it uses standard `AeonTopAppBar` and modular section headers
**And** all interactive controls (switches, sliders) are unified into a consistent design language using existing Aeon components
**And** `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL with no regressions

## Tasks / Subtasks

- [ ] Task 1 — Read and document the current Settings screen
  - [ ] 1.1 Read `ui/screens/settings/SettingsScreen.kt` (full file) — document every private composable: `HeroHeader`, `SettingsFooter`, `SettingsNavigationItem`, `SettingsDivider` and what they render
  - [ ] 1.2 Read the corresponding `SettingsViewModel.kt` — what state does it expose? What flows does the screen collect?
  - [ ] 1.3 List all sections in the settings screen (e.g., Appearance, Data, Security, Backup, About) and their current composables
  - [ ] 1.4 Identify all "non-essential decorative elements" — elements that add visual complexity without functional value (large gradient headers, decorative icons for every row, unnecessary dividers, etc.)

- [ ] Task 2 — Audit existing Aeon components for settings use cases
  - [ ] 2.1 Check `ui/components/` for any `AeonSettingsSection`, `AeonSettingsItem`, or `AeonListItem` components — the audit found `AeonSettingsSection` and `AeonSettingsItem` already exist in the file; confirm their current design matches the H-editorial standard
  - [ ] 2.2 Check `AeonInputs.kt` for switch/toggle components — what switch or checkbox components exist?
  - [ ] 2.3 Check `AeonButtons.kt` for any secondary action buttons used in settings rows
  - [ ] 2.4 If existing `AeonSettingsSection`/`AeonSettingsItem` components are already H-editorial compliant, the refactor is primarily about removing the decorative `HeroHeader` and streamlining the screen structure

- [ ] Task 3 — Remove or simplify `HeroHeader` (line 223 of `SettingsScreen.kt`)
  - [ ] 3.1 Read `HeroHeader` composable — identify what it renders (likely a large decorative header with app name/logo or gradient)
  - [ ] 3.2 Decision: If `HeroHeader` is a large decorative element → replace with a standard `AeonTopAppBar` (pill-layout variant matching the H-design standard used on other screens). If it provides functional content (e.g., profile avatar, theme switcher), simplify it to a compact functional header
  - [ ] 3.3 Do NOT remove functionality — only decorative excess. Any functional control in `HeroHeader` must be relocated to the appropriate settings section

- [ ] Task 4 — Migrate from `Column + verticalScroll` to `LazyColumn`
  - [ ] 4.1 The current Settings screen uses `Column` with `verticalScroll(rememberScrollState())` — this is fine for small lists but becomes expensive as settings rows grow (all rows composed at once)
  - [ ] 4.2 Refactor the settings content to use `LazyColumn` with section headers as `stickyHeader` items — this enables the settings rows to be composed lazily
  - [ ] 4.3 Use `key = { "section_${sectionId}" }` for section header items and `key = { "item_${itemId}" }` for individual setting rows
  - [ ] 4.4 Exception: if the settings list is short (< 20 items), `Column + verticalScroll` is acceptable and this task can be skipped with a note

- [ ] Task 5 — Standardize interactive controls
  - [ ] 5.1 Audit all `Switch`, `Slider`, `Checkbox`, `RadioButton` usages in `SettingsScreen.kt` — are they raw M3 components or wrapped Aeon components?
  - [ ] 5.2 If raw M3 components are used directly: wrap them in thin `AeonSwitch`, `AeonSlider` wrappers that apply `AppColors.switchTrackOn/Off` and `AppColors.switchThumbOn/Off` tokens (these tokens exist in `Color.kt`)
  - [ ] 5.3 Ensure all switch/toggle rows have consistent layout: icon (optional) + label + subtitle (optional) + control, with consistent `AeonSize` padding
  - [ ] 5.4 Ensure tappable rows have correct minimum touch target (48dp height minimum per Material guidelines)

- [ ] Task 6 — Apply H-palette to all colors in Settings screen
  - [ ] 6.1 Search `SettingsScreen.kt` for any hardcoded `Color(0xFF...)` — replace with `AppColors.*` tokens
  - [ ] 6.2 Verify section headers use `AppColors.contentSecondary` (overline/label style) with `AppTextStyles.sectionOverline`
  - [ ] 6.3 Verify navigation items use `AppColors.contentPrimary` for labels and `AppColors.contentSecondary` for subtitles with `AppTextStyles.listItemTitle`/`listItemSubtitle`
  - [ ] 6.4 Verify `SettingsDivider` uses `AppColors.divider` (not hardcoded color)
  - [ ] 6.5 Verify footer text uses `AppColors.contentSecondary` with `AppTextStyles.caption`

- [ ] Task 7 — Apply dimension tokens from Story 3.1
  - [ ] 7.1 Replace hardcoded padding/margin values with `AeonTheme.dimensions.screenPadding` and `AeonSize.*` values where they represent structural layout dimensions
  - [ ] 7.2 Fine-tuning offsets (e.g., `+ 4.dp`) are acceptable as literals — do not over-tokenize
  - [ ] 7.3 Ensure list item height conforms to the H-editorial standard (read what `AeonSettingsItem` enforces for height)

- [ ] Task 8 — Visual smoke test
  - [ ] 8.1 Build debug APK and open the Settings screen
  - [ ] 8.2 Verify: no large gradient header, clean list layout, consistent switches/controls, H-palette colors
  - [ ] 8.3 Switch theme (Dark → Light → AMOLED) — confirm all colors update correctly via `AppColors`
  - [ ] 8.4 Navigate to all sub-settings screens to confirm no navigation regressions

- [ ] Task 9 — Compile and test verification
  - [ ] 9.1 `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL
  - [ ] 9.2 `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL

- [ ] Task 10 — Update sprint-status.yaml
  - [ ] 10.1 Mark story 3.2 `done` in sprint-status.yaml

## Dev Notes

### Current Settings Screen — Known Structure

**File:** `ui/screens/settings/SettingsScreen.kt`

**Key findings from audit:**
- 5 composable functions: `SettingsScreen` (public), `HeroHeader` (private), `SettingsFooter` (private), `SettingsNavigationItem` (private), `SettingsDivider` (private)
- Uses `Column + verticalScroll(rememberScrollState())` — NOT LazyColumn
- Uses `AeonHazeProvider` for glass blur background
- Uses M3 `Scaffold` with `containerColor = Color.Transparent` (correct — lets Haze show through)
- Uses `AeonTopAppBar` with `AeonTopBarNavTitlePill` / `AeonTopBarSection` — the top bar pattern is already correct
- Uses `AeonSettingsSection` and `AeonSettingsItem` — custom components already exist
- Uses `collectAsStateWithLifecycle` for currency and currency provider state
- Does NOT use `LazyColumn`

**The existing `AeonTopAppBar` usage is already correct** — do not change it. The primary cleanup targets are:
1. `HeroHeader` (large decorative header — line 223)
2. Any direct M3 switch/control usages that should be Aeon-wrapped
3. Any hardcoded colors

---

### `AeonSettingsSection` and `AeonSettingsItem` — These Already Exist

Before creating any new components, read their current implementations. They may already conform to the H-editorial standard. If they do, the refactor is mainly about:
1. Removing `HeroHeader` 
2. Ensuring all settings rows go through `AeonSettingsItem`
3. Color token compliance

If `AeonSettingsItem` or `AeonSettingsSection` have design issues (wrong colors, wrong padding), fix them — do NOT create new components with different names.

---

### `HeroHeader` — Decision Framework

Read the `HeroHeader` composable (line 223). Common patterns that should be REMOVED:
- Large gradient background banner
- App logo/mascot illustration
- Decorative text with custom fonts at large sizes
- Non-functional visual decoration

Patterns that should be SIMPLIFIED (not removed):
- Profile avatar/name display → move to a compact `AeonSettingsSection` header row
- Active theme preview → keep as a small interactive element within Appearance section
- App version display → move to `SettingsFooter`

The H-editorial standard uses: clean top bar + immediate content list. No hero header.

---

### Switch/Toggle Token Mapping

The H-palette tokens in `Color.kt` for interactive controls:
```kotlin
// In AppColors object:
val switchTrackOn: Color   // use for active/checked track
val switchTrackOff: Color  // use for inactive/unchecked track
val switchThumbOn: Color   // use for active thumb
val switchThumbOff: Color  // use for inactive thumb
```

A minimal `AeonSwitch` wrapper:
```kotlin
@Composable
fun AeonSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedTrackColor = AppColors.switchTrackOn,
            uncheckedTrackColor = AppColors.switchTrackOff,
            checkedThumbColor = AppColors.switchThumbOn,
            uncheckedThumbColor = AppColors.switchThumbOff,
        )
    )
}
```

Check `ui/components/` first — this wrapper may already exist under a different name.

---

### Text Style Mapping for Settings Rows

| Element | `AppTextStyles` token | `AppColors` token |
|---|---|---|
| Section header | `AppTextStyles.sectionOverline` | `AppColors.contentSecondary` |
| Row title | `AppTextStyles.listItemTitle` | `AppColors.contentPrimary` |
| Row subtitle | `AppTextStyles.listItemSubtitle` | `AppColors.contentSecondary` |
| Footer text | `AppTextStyles.caption` | `AppColors.contentSecondary` |
| Navigation badge | `AppTextStyles.chipLabel` | `AppColors.accent` |

---

### Architecture Compliance

- **AD-10 (Component-Level Glass Policy):** The Settings screen uses `AeonHazeProvider` for the glass background — this is correct. Individual settings rows are opaque hairline panels (not glass). This matches the "Opaque Hairline" panel aesthetic for editorial screens.
- **UX-DR2 (H-Editorial Instrument Consistency):** Settings is called out as "over-designed" in the epics. This story directly addresses it by removing decorative excess and applying editorial simplicity.

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
