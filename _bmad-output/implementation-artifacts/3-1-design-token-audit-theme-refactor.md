---
baseline_commit: ''
---

# Story 3.1: Design Token Audit & Theme Refactor

## Status
**Status:** ready-for-dev
**Epic:** Epic 3: Design System Consolidation ("H" Identity)
**ID:** 3.1

## Story

As a developer,
I want a centralized and strictly enforced design token system,
so that I can ensure visual consistency across all feature modules without code duplication.

## Acceptance Criteria

**Given** a codebase with some hardcoded colors and inconsistent dimension usage
**When** I audit the `ui/theme` package and refactor `OssWalletTheme` to be the single source of truth for the "H" identity
**Then** all hardcoded `Color(0xFF...)` hex values in `ui/` files outside `ui/theme/` are replaced with tokens from `AppColors`
**And** the theme supports dynamic accent switching while preserving the "H" neutral palettes
**And** `CompositionLocalProvider` is used to inject all design dimensions and scales globally (already implemented — verify)
**And** `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL with no regressions

## Tasks / Subtasks

- [ ] Task 1 — Audit hardcoded `Color(0xFF...)` values in `ui/` outside `ui/theme/`
  - [ ] 1.1 Search for all hardcoded color values: `grep -rn "Color(0x" --include="*.kt" app/src/main/java/com/osscabinet/ossWallet/ui/ | grep -v "/ui/theme/"` — the audit found at minimum 10 occurrences; get the full list
  - [ ] 1.2 For each occurrence, classify:
    - **Semantic per-type color** (e.g., account structure type colors in `AccountStructureTypeUi.kt`) → needs a named color token in `ui/theme/Color.kt` or a new mapping object
    - **Inline UI detail** (e.g., `Color(0xFF1A1A1A)` in `ColorUtils.kt`) → replace with nearest `AppColors.*` token
    - **Chart/visualization color** → check if already covered by `ChartColor*` tokens in `Color.kt`
  - [ ] 1.3 Document findings: file path, line number, hardcoded value, recommended replacement token

- [ ] Task 2 — Add missing color tokens for account structure types
  - [ ] 2.1 `AccountStructureTypeUi.kt` has 8 hardcoded colors for account types (BANK, SAVINGS, CASH, DIGITAL_WALLET, CREDIT, INVESTMENT, CRYPTO, OTHER) — these are semantic per-type colors that belong in a token map
  - [ ] 2.2 Add an `AccountStructureColors` token object to `ui/theme/Color.kt` (or a new `ui/theme/AccountTheme.kt`):
    ```kotlin
    object AccountStructureColors {
        val bank = Color(0xFF2196F3)
        val savings = Color(0xFF4CAF50)
        val cash = Color(0xFF8BC34A)
        val digitalWallet = Color(0xFF00BCD4)
        val credit = Color(0xFFF44336)
        val investment = Color(0xFFFF9800)
        val crypto = Color(0xFFFFC107)
        val other = Color(0xFF9E9E9E)
    }
    ```
  - [ ] 2.3 Update `AccountStructureTypeUi.kt` to reference `AccountStructureColors.bank` etc. instead of inline `Color(0xFF...)`

- [ ] Task 3 — Fix inline hardcoded colors in utility files
  - [ ] 3.1 `ColorUtils.kt:191` — `Color(0xFF1A1A1A)` (ink color for dark backgrounds) → replace with `AppColors.contentPrimary` or a new `AppColors.inkOnDark` semantic token
  - [ ] 3.2 `AccountHeroCard.kt:104` — two hardcoded colors for positive balance display → replace with `AppColors.positive` and `AppColors.positiveContainer` (or nearest equivalent from `HPositiveDark`/`HPositiveLight` semantic tokens already in `Color.kt`)
  - [ ] 3.3 Search for any remaining occurrences found in Task 1.1 and replace using the same pattern

- [ ] Task 4 — Audit `ui/theme/` for completeness and document the token system
  - [ ] 4.1 Verify all 7 theme files exist and serve their documented purpose:
    - `Color.kt` — color tokens + `AppColors` semantic aliases ✓
    - `Type.kt` — `AppTextStyles` (50 tokens) + `AppFont` enum ✓
    - `Dimensions.kt` — `AeonDimensions` spacing/padding tokens
    - `AeonSize.kt` — standardized size constants
    - `GlassTokens.kt` — glass blur/opacity values
    - `Theme.kt` — `OssWalletTheme` composable + `AeonTheme` object
    - `ThemePreviews.kt` — preview composables
  - [ ] 4.2 Read `Dimensions.kt` and `AeonSize.kt` — note what dimension tokens exist (these will inform Story 3.2 and 3.3's dp-token replacement work)
  - [ ] 4.3 Confirm `CompositionLocalProvider` in `Theme.kt` provides: `LocalAeonDimensions`, `LocalUiScale`, `LocalAppColorScheme`, `LocalThemeMode`, `LocalUiIdentity`, `LocalAppFont`, `LocalGlassSettings` — these are the 7 injection points. If any are missing, add them.

- [ ] Task 5 — Verify dynamic accent switching works with H-palette
  - [ ] 5.1 In `Theme.kt`, the `applyIdentityToColorScheme(base, identity, themeMode, accentOverride?)` function handles H-palette layering — read it to confirm accent override is wired correctly
  - [ ] 5.2 Test: switch accent color in app Settings → confirm card accents update without affecting neutral H-palette backgrounds
  - [ ] 5.3 If accent override is not wired from Settings → `OssWalletTheme` → `applyIdentityToColorScheme`, identify the gap and wire it

- [ ] Task 6 — Create dimension token usage guide (lightweight)
  - [ ] 6.1 With 1,969 hardcoded `dp`/`sp` values outside `ui/theme/`, a full replacement is not in scope for this story — but create `docs/design/dimension-token-guide.md` listing what `AeonSize` and `AeonDimensions` tokens exist, so Stories 3.2 and 3.3 can use them consistently
  - [ ] 6.2 The guide should cover: available `AeonSize` values, available `AeonDimensions` spacing tokens, when to use literal `dp` (fine for one-off offsets like `+ 8.dp`) vs. when a token is required (fixed component sizes)

- [ ] Task 7 — Compile verification
  - [ ] 7.1 `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL
  - [ ] 7.2 `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL (no color/theme logic in unit tests, so this is mostly a smoke-check)

- [ ] Task 8 — Update sprint-status.yaml
  - [ ] 8.1 Mark story 3.1 `done` in sprint-status.yaml

## Dev Notes

### Token System — Current State (Very Strong Foundation)

The theme system is already well-developed:

**`Color.kt`:**
- 80+ raw color constants (AMOLED, H Dark, H Light, H AMOLED palettes)
- `AppColors` object with 60+ semantic aliases (`accent`, `positive`, `negative`, `screenBackground`, `cardSurface`, `contentPrimary`, `contentSecondary`, `divider`, `border`, etc.)
- Semantic feedback tokens: `HPositiveDark`, `HNegativeDark`, `HPositiveLight`, `HNegativeLight`
- Chart tokens: `ChartColorBlue`, `ChartColorGreen`, `ChartColorRed`, `ChartColorOrange`
- Glass tokens: `GlassBlurLight/Dark`, `GlassSurfaceOpacity*`, `GlassBorderOpacity*`

**`Type.kt`:**
- `AppTextStyles` object with 50 named text style tokens covering: transactions, cards, forms, charts, navigation, numerals, metadata
- 3 font families: `FrauncesFamily` (display), `InterFamily` (body), `JetBrainsMonoFamily` (mono)
- `AppFont` enum with 4 options

**`Theme.kt`:**
- `OssWalletTheme` composable handles AMOLED/dark/light mode switching
- H-identity layering via `applyIdentityToColorScheme`
- `CompositionLocalProvider` already injects 7 composition locals

**The primary gap:** A small number of hardcoded `Color(0xFF...)` values in utility/component files, and no named tokens for account structure type colors.

---

### Hardcoded Color Locations Found in Audit

| File | Line(s) | Values | Replacement |
|---|---|---|---|
| `ui/util/AccountStructureTypeUi.kt` | 30–37 | 8 account type colors | New `AccountStructureColors` object |
| `ui/util/ColorUtils.kt` | 191 | `Color(0xFF1A1A1A)` ink on dark | `AppColors.contentPrimary` or new token |
| `ui/screens/accounts/detail/components/AccountHeroCard.kt` | 104 | 2 positive-balance colors | `AppColors.positive` / `HPositiveDark` |

Run the full grep from Task 1.1 to get the complete list — there may be more. The audit above represents the confirmed minimum.

---

### `AccountStructureColors` Placement Decision

Two options:
1. **Add to `Color.kt`** — keeps all tokens in one file, simple
2. **New `ui/theme/AccountTheme.kt`** — better separation if account-related design tokens grow (e.g., account icons, gradient stops)

Recommend option 1 (add to `Color.kt`) for this story since we're only adding 8 colors.

---

### Dimension Tokens — Not In Scope for Bulk Replacement

The 1,969 hardcoded `dp`/`sp` values represent years of iterative UI tuning. Bulk-replacing them risks introducing visual regressions. For this story:

- **In scope:** Ensure `AeonSize` and `AeonDimensions` tokens exist and are documented
- **Not in scope:** Replacing existing `8.dp`, `16.dp` literals in component files unless they represent fixed component sizes that should be standardized (e.g., minimum touch target `48.dp`)
- **Stories 3.2 and 3.3** will apply dimension tokens to specific screens during their redesign work

---

### `CompositionLocalProvider` — Verify These 7 Are Present

From `Theme.kt` audit, these composition locals must be provided by `OssWalletTheme`:

1. `LocalAeonDimensions` — spacing/layout dimensions
2. `LocalUiScale` — user-adjustable UI scale factor
3. `LocalAppColorScheme` — the resolved H/AMOLED/Light/Dark color scheme
4. `LocalThemeMode` — current theme mode string
5. `LocalUiIdentity` — `UiIdentity.H` or others
6. `LocalAppFont` — `AppFont` enum selection
7. `LocalGlassSettings` — glass blur configuration

If any are missing, add them now — they are required for Stories 3.2 and 3.3 to use them confidently.

---

### Architecture Compliance

- **AD-10 (Component-Level Glass Policy):** `GlassTokens.kt` stores the glass parameters; `CompositionLocalProvider` injects `LocalGlassSettings` — this story verifies these are wired correctly as a prerequisite for Story 3.3's glass policy implementation.
- **UX-DR1/DR2 (H Editorial Instrument):** The H-palette tokens (`HDark*`, `HLight*`, `HAmoled*`) in `Color.kt` are the implementation of the H design identity. Replacing hardcoded colors with these tokens directly implements UX-DR2.

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
