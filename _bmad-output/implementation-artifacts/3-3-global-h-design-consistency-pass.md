---
baseline_commit: ''
---

# Story 3.3: Global "H" Design Consistency Pass

## Status
**Status:** ready-for-dev
**Epic:** Epic 3: Design System Consolidation ("H" Identity)
**ID:** 3.3

## Story

As a user,
I want a visually cohesive experience across all screens (Accounts, Records, Analytics),
so that the app feels like a single, unified "premium instrument."

## Acceptance Criteria

**Given** the Accounts, Records, and Analytics screens which may have minor design deviations
**When** I perform a global design sweep applying the "H" standards
**Then** all screens use the same modular `AeonTopAppBar` pill layouts
**And** all data containers are unified as `AeonExpressiveCard` instances
**And** the **Component-Level Glass Policy** (AD-10) is applied (opaque cards, glass popups/sheets)
**And** the navigation transitions between these screens are fluid and consistent
**And** `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL with no regressions

## Tasks / Subtasks

- [ ] Task 1 — Read and audit the Accounts screens
  - [ ] 1.1 Read `ui/screens/accounts/list/AccountListScreen.kt` fully — document: top bar type, card components used, color usages, any hardcoded colors/dimensions
  - [ ] 1.2 Read `ui/screens/accounts/detail/AccountDetailScreen.kt` — same audit
  - [ ] 1.3 Read `ui/screens/accounts/detail/components/AccountHeroCard.kt` — this file has 2 hardcoded colors (from Story 3.1 audit); also evaluate if the card uses `AeonExpressiveCard` or a raw surface
  - [ ] 1.4 Document deviations from H-standard: wrong top bar variant, raw `Card` instead of `AeonExpressiveCard`, hardcoded colors, inconsistent padding

- [ ] Task 2 — Read and audit the Records / Transactions screens
  - [ ] 2.1 Read `ui/screens/transactions/list/TransactionListScreen.kt` and `TransactionListContent.kt` — document top bar, list items, card components, color tokens used
  - [ ] 2.2 Read `ui/screens/transactions/entry/TransactionEntryScreen.kt` — document form layout, input components, button usage
  - [ ] 2.3 Document deviations from H-standard

- [ ] Task 3 — Read and audit Analytics screens (if they exist)
  - [ ] 3.1 Search for analytics/charts screens: `find ui/screens/ -name "*Analytic*" -o -name "*Chart*" -o -name "*Stats*"` — list all found screens
  - [ ] 3.2 Read each analytics screen — document top bar pattern, chart card containers, color tokens
  - [ ] 3.3 Document deviations

- [ ] Task 4 — Apply `AeonTopAppBar` pill layout uniformly
  - [ ] 4.1 Any screen that uses raw `TopAppBar` or `CenterAlignedTopAppBar` from M3 directly (not `AeonTopAppBar`) → replace with `AeonTopAppBar` + appropriate `AeonTopBarNavTitlePill`/`AeonTopBarSection` configuration
  - [ ] 4.2 Verify the back-navigation button uses consistent styling across all screens (same icon, same ripple color)
  - [ ] 4.3 Verify `Scaffold` uses `containerColor = Color.Transparent` on all screens that should show the Haze glass background (matching the pattern in Settings and Dashboard)

- [ ] Task 5 — Apply `AeonExpressiveCard` to data containers
  - [ ] 5.1 Any `Card(...)`, `ElevatedCard(...)`, or `Surface(shape = RoundedCornerShape(...))` used as a data container → evaluate replacing with `AeonExpressiveCard`
  - [ ] 5.2 `AeonExpressiveCard` is the shell for Dashboard cards (wraps `GlassSurface + AeonGlassHost`) — use it for any card-shaped container in Accounts/Records/Analytics that represents a data summary or KPI display
  - [ ] 5.3 Do NOT replace every `Surface` — only data container cards that represent financial information panels. UI shells (drawers, bottom sheets, dialogs) use their own components (`AeonGlassSheet`, `AeonAlertDialog`)

- [ ] Task 6 — Apply Component-Level Glass Policy (AD-10)
  - [ ] 6.1 **Opaque hairline panels** (cards, list items, form sections): use `AppColors.cardSurface` as background — no glass effect. Ensures readability.
  - [ ] 6.2 **Glass components** (popups, tooltips, context menus, bottom sheets): use `AeonGlassSheet` / `GlassSurface` with `AeonGlassHost` — opt-in glass depth
  - [ ] 6.3 Audit `AccountDetailScreen.kt` — does the `AccountHeroCard` use glass or opaque? Per AD-10, hero cards on detail screens MAY opt-in to glass for aesthetic fit — document the decision made
  - [ ] 6.4 Audit any popups or filter chips on Records screens — these should use glass treatment
  - [ ] 6.5 Do NOT add `hazeEffect` inline — always use `GlassSurface` (per CLAUDE.md Aeon design system rules)

- [ ] Task 7 — Apply H-palette colors to all audited screens
  - [ ] 7.1 Replace all hardcoded `Color(0xFF...)` found in Accounts and Records screens with `AppColors.*` tokens (continuing the work from Story 3.1)
  - [ ] 7.2 Replace all hardcoded `Color(0xFF7FBF8E)` and `Color(0xFF2C6E49)` in `AccountHeroCard.kt:104` with `AppColors.positive` and a suitable container color
  - [ ] 7.3 Ensure text on data panels uses `AppTextStyles.transactionAmount`, `AppTextStyles.heroAmount`, `AppTextStyles.listItemTitle` etc. — not hardcoded `TextStyle(fontSize = X.sp)` constructions

- [ ] Task 8 — Verify navigation transitions
  - [ ] 8.1 Navigate: Dashboard → Account Detail → Back → Dashboard — confirm the transition is smooth (default M3 navigation transition or custom enter/exit specs)
  - [ ] 8.2 Navigate: Records → Transaction Detail → Back — confirm transition
  - [ ] 8.3 If navigation transitions are missing (abrupt cuts with no animation), add `enterTransition` and `exitTransition` specs to the `NavGraph` entries for these routes
  - [ ] 8.4 The project uses `OssWalletNavGraph.kt` — transitions are set per route in the `composable(route = Screen.XXX.route, enterTransition = ..., exitTransition = ...)` calls

- [ ] Task 9 — Compile and visual smoke test
  - [ ] 9.1 `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL
  - [ ] 9.2 `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL
  - [ ] 9.3 Manual visual review: open each audited screen in both Light and Dark themes — confirm palette consistency

- [ ] Task 10 — Update sprint-status.yaml
  - [ ] 10.1 Mark story 3.3 `done` in sprint-status.yaml

## Dev Notes

### Screens In Scope

| Screen | Primary File | Status |
|---|---|---|
| Account List | `ui/screens/accounts/list/AccountListScreen.kt` | To audit |
| Account Detail | `ui/screens/accounts/detail/AccountDetailScreen.kt` | To audit |
| Account Entry | `ui/screens/accounts/entry/AccountEntryScreen.kt` | To audit |
| Transaction List | `ui/screens/transactions/list/TransactionListScreen.kt` | To audit |
| Transaction List Content | `ui/screens/transactions/list/TransactionListContent.kt` | To audit |
| Transaction Entry | `ui/screens/transactions/entry/TransactionEntryScreen.kt` | To audit |
| Analytics screens | Search required (see Task 3) | Unknown |

**Screens NOT in scope for this story:**
- Dashboard (covered by Stories 2.x)
- Settings (covered by Story 3.2)
- Web companion UI (covered by Epic 4)
- Loan, Budget, Planned Payment screens (not mentioned in AC — treat as out-of-scope unless deviations are extreme)

---

### Aeon Component Reference Quick-Guide

| Need | Component | File |
|---|---|---|
| Top app bar | `AeonTopAppBar` | `ui/components/` |
| Top bar pill with title | `AeonTopBarNavTitlePill` | `ui/components/` |
| Data container card | `AeonExpressiveCard` | `ui/components/` |
| Opaque card surface | `AppColors.cardSurface` | `ui/theme/Color.kt` |
| Glass bottom sheet | `AeonGlassSheet` | `ui/components/GlassSurface.kt` |
| Glass popup / tooltip | `GlassSurface` + `AeonGlassHost` | `ui/components/GlassSurface.kt` |
| Dialog | `AeonAlertDialog` | `ui/components/AeonAlertDialog.kt` |
| Text input | `AeonInputs.kt` | `ui/components/AeonInputs.kt` |
| Primary button | (check AeonButtons.kt) | `ui/components/AeonButtons.kt` |
| Switch | `AeonSwitch` (created in Story 3.2) | `ui/components/` |

**Hard rule from CLAUDE.md:** Never write `hazeEffect` inline — always use `GlassSurface`. Never create raw M3 `Card` for data containers — use `AeonExpressiveCard`. Check `ui/components/` before creating any new UI primitives.

---

### Component-Level Glass Policy (AD-10) — Decision Table

| Component type | Glass? | Rationale |
|---|---|---|
| Dashboard cards (AeonExpressiveCard) | YES — optional glass | Premium aesthetic, card can blur over background |
| Data list items (transaction rows, account rows) | NO — opaque hairline | Readability; high-density list items must be legible |
| Filter chips, tag pills | YES — light glass | Overlays on scroll content benefit from depth |
| Bottom sheets | YES — `AeonGlassSheet` | Transient overlays use glass for visual hierarchy |
| Dialogs | NO — opaque `AeonAlertDialog` | Destructive/critical confirmations need clear contrast |
| Detail page hero cards | YES — optional glass | Hero cards represent a "feature card" aesthetic fit |
| Form sections (entry screens) | NO — opaque panels | Form inputs need maximum readability |

When in doubt: opaque for content density, glass for transient/overlay/hero elements.

---

### Navigation Transitions — Pattern

If adding transitions to `OssWalletNavGraph.kt`:

```kotlin
// Standard enter/exit for lateral navigation (Accounts, Records):
composable(
    route = Screen.AccountDetail.route,
    enterTransition = {
        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300))
    },
    exitTransition = {
        slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300))
    },
    popEnterTransition = {
        slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300))
    },
    popExitTransition = {
        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
    }
) { ... }
```

The 300ms duration and 1/3 parallax offset match the H-editorial "slide with depth" pattern. Consistent application across all screen routes gives the cohesive transition feel.

---

### `AccountHeroCard.kt` — Hardcoded Colors Fix

From Story 3.1 audit, line 104 has:
```kotlin
if (isDarkBg) Color(0xFF7FBF8E) else Color(0xFF2C6E49)
```

These are positive-balance indicator colors. Replacement:
```kotlin
if (isDarkBg) AppColors.positive else HPositiveLight  // or AppColors.positive from the dark/light context
```

Since `AppColors.positive` is already a `@Composable @ReadOnlyComposable` semantic alias that resolves to the correct palette-aware value, simply:
```kotlin
AppColors.positive  // resolves to HPositiveDark or HPositiveLight based on current theme
```

---

### Previous Story Learnings

- **Story 3.1** established the color token system — use `AppColors.*` for ALL color references. Never add new `Color(0xFF...)` values; if a token doesn't exist, add it to `Color.kt` and use the token.
- **Story 3.2** established the Settings layout pattern — use it as the reference for top bar + list layout on any screen that is primarily a list of items.
- **Git path casing:** Files tracked lowercase on macOS — no impact here since no new modules are being created.

---

### Architecture Compliance

- **AD-10 (Component-Level Glass Policy):** This story explicitly enforces AD-10 across the Accounts, Records, and Analytics screens.
- **UX-DR1/DR2 (H-Editorial Instrument):** Applying `AeonTopAppBar` pill layout and `AeonExpressiveCard` directly implements the H-editorial card aesthetic across all major screens.
- **UX-DR3 (3D flip performance during MVI state updates):** The flip animation cleanup from Story 2.2 (local flip state) is a prerequisite — verify it is in place before applying `AeonExpressiveCard` broadly, since `AeonExpressiveCard` hosts the flip animation.
- **UX-DR4 (Glassmorphism policy):** The glass component decision table in Dev Notes implements UX-DR4 at the component level.

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
