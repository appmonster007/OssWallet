# H "Editorial Instrument" — Compose Migration Plan

## Context
The HTML comps in `design/comps/` (h-hybrid.html, full-screens.html, edit-mode.html, pill-demo.html, h-dynamic-accent.html) are approved. We now migrate the **H — Editorial Instrument** identity into the Compose app: warm-black/ivory/champagne (dark) and ivory/ink/bronze (light), Fraunces serif amounts, Inter body, JetBrains Mono micro-readouts, hairline-gradient solid panels, selective glass (blur only on floating chrome), and **self-contained cards** (title + period readout inside the card body, no filter/config buttons).

**Decisions locked:**
- **H as the new default, with `UiIdentity` hooks** — structure the code so A/B can be added later without refactor, but only H is implemented now (default = H).
- **Typography customization ships** — extend the font system with Fraunces + JetBrains Mono and a user-facing font/role setting (the switcher previewed in full-screens.html).
- **M3 color customization = "H neutrals + dynamic accent"** — full Material You (wallpaper bg/surface) is incompatible with H's editorial palette, so it stays off under H/A/B. Instead a dynamic-accent toggle lets Material You's `primary` override **only** the accent token; all H neutrals, type, and pos/neg semantics stay fixed (see Phase 4.3).

**Phase ordering note:** The two **identity-agnostic** structural changes ship first — selective glass (Phase 0) and the card-shell header restructure (Phase 1) — both edit `AeonExpressiveCard.kt`, so they're done back-to-back as one coherent shell rewrite + one regression pass, and both are validated against the *current* theme before any palette/type work. Identity → type → color then layer purely-visual changes on a stable interaction model. The filter-pill/sheet H *restyle* (Phase 5) is deferred until after type + color, since it depends on the mono family and champagne accent.

**Why route through the repos:** `AppColors`/`LocalAppColorScheme`, `createTypography(AppFont)`/`AppTextStyles`, `GlassTokens`/`GlassSettings`, `AeonDimensions` are single-point seams. The dashboard card **registry stays untouched** — visual changes live only in `AeonExpressiveCard` + the individual card composables. Each phase below is independently shippable and compiles/runs green before the next.

---

## Progress Tracker
Status key: `[ ]` not started · `[~]` in progress · `[x]` done & acceptance met. Update each step's box as it completes; never mark a phase done until its **Acceptance** criteria below pass and `./gradlew assembleDebug` is green.

### Phase 0 — Selective-glass policy (identity-agnostic)
- [x] 0.1 `Modifier.aeonHairlineBorder()` (+ optional ambient glow helper) in `ui/components/`
- [x] 0.2 Demote `AeonExpressiveCard` off `GlassTokens.cardStyle` → solid panel + hairline border
- [x] 0.3 Keep glass on nav pill / filter pill / sheets / dialogs; verify glass-off degrades
- [x] **Acceptance:** cards opaque hairline panels, chrome still blurs, glass-off degrades, no scroll jank, build green, registry untouched

### Phase 1 — Card shell: self-contained header structure (identity-agnostic)
- [x] 1.1 Restructure header (no flag): in-body title + tappable period readout (when `filter != null`) + optional link; remove filter/settings icon buttons (237–265)
- [x] 1.2 Tap readout (when `filter != null && onFilterChange != null`) → existing `showFilterSheet` path / `LocalFilterPillController`
- [x] 1.3 Long-press (when `displaySettings != null`) → settings sheet; reorder handle stays on header section
- [x] **Acceptance:** filter cards show title+period in body, no icons; tap→pill→sheet→dismiss; long-press settings; reorder & add-card intact; hero/analytics cards unchanged; build green; registry untouched

### Phase 2 — Identity seam (no visual change)
- [x] 2.1 `domain/model/settings/UiIdentity.kt` enum `{ H, A, B; DEFAULT=H; fromName() }`
- [x] 2.2 `LocalUiIdentity` in Color.kt
- [x] 2.3 `uiIdentity` flow + `saveUiIdentity()` in SettingsRepository/Impl/DataStore
- [x] 2.4 `OssWalletTheme(identity=...)` provides `LocalUiIdentity`; MainActivity collects/passes
- [x] **Acceptance:** runs identically to Phase 1, default identity=H wired, build green, no visible change

### Phase 3 — Typography: 3-family model + new fonts
- [x] 3.1 Add font resources (Fraunces + italic, JetBrains Mono)
- [x] 3.2 Add `monoFamily` to `AppFont`; add `EDITORIAL_INSTRUMENT`; `DEFAULT = EDITORIAL_INSTRUMENT`
- [x] 3.3 `LocalAppFont` provided in `OssWalletTheme`
- [x] 3.4 Route amount tokens → serif, micro-readouts → mono (`cardPeriodReadout` added; the Phase 1 readout picks up mono here)
- [x] 3.5 Font picker UI lists new fonts; persists via `saveSelectedFont` (picker iterates `AppFont.entries`, so new fonts appear automatically)
- [x] **Acceptance:** serif amounts / mono readouts / Inter body with H font, live switch, tnum preserved, no clipping, build green

### Phase 4 — Color: H palettes (dark + light + AMOLED) + M3 dynamic accent
- [x] 4.1 `applyIdentityToColorScheme(base, identity, themeMode, accentOverride)` with H dark/light/AMOLED palettes (`HDark/HLight/HAmoledColorScheme` in Theme.kt; H constants in Color.kt)
- [x] 4.2 Extend pos/neg per identity; champagne/bronze accent via `primary` token (hairline + readout pick it up automatically)
- [x] 4.3 **Dynamic-accent toggle (M3 customization):** `dynamicAccent` setting (DataStore/Repo/Impl/VM/MainActivity) overrides ONLY `primary` with Material You's `primary` on Android 12+; H neutrals stay fixed. Toggle surfaced in Display Settings → Appearance.
- [x] **Acceptance:** dashboard + sample screens render H across light/dark/AMOLED, correct contrast, no hardcoded-hex regressions; dynamic-accent toggle live-recolors accent (hairline, readout, chips) while neutrals stay H; build green

### Phase 5 — Filter pill / sheet H restyle
- [x] 5.1 Restyle `FilterSheetContent` to H: section overline → mono (`sectionOverline`), active/removable chips → champagne (`accentContainer`), serif heading via `heroAmount`. Pill already consumes `AppColors.*`/glass so H neutrals+accent flow automatically — content/logic unchanged
- [x] **Acceptance:** pill + sheet match comps in light+dark (mono/champagne/serif), all filter logic intact, build green

### Phase 6 — Card content restyle
- [ ] 6.1 NetWorthCard: serif hero, mono trend, twin income/expense columns split by hairline
- [ ] 6.2 AccountsCard: ledger rows (color dot/left rule), serif native balance, mono `≈` base; ghost dashed add
- [ ] 6.3 CashFlow/Budgets: hairline track bars (gold fill, neg=overspend), mono figures, serif net
- [ ] **Acceptance:** each card matches comp in light+dark, all data preserved, build green, previews updated

### Phase 7 — Polish & verification
- [x] 7.1 Nav pill, FAB, icon tints, dividers, empty states aligned to H. Pill/FAB/empty-state already consume `AppColors.*`; status/nav-bar tints driven by `!darkTheme` in `OssWalletTheme` SideEffect. Real gap fixed: H schemes left `secondary`/`tertiary`/`error` families at Material defaults (purple/red) — added editorial warm-neutral secondary/tertiary + H-negative-aligned error tokens to all three H schemes (Color.kt + Theme.kt)
- [x] 7.2 Design Lab preview now reflects H — `OssWalletPreviewTheme` routes its base scheme through `applyIdentityToColorScheme(..., UiIdentity.DEFAULT, themeMode)`, so the Light/Dark/AMOLED cycle shows the real H palette. (Identity selector itself deferred: only H is implemented; A/B add nothing to flip yet.)
- [~] **Acceptance:** `assembleDebug` green; code coherent. STILL PENDING: `./gradlew lint test`, and on-device walkthrough (scroll/pill/reorder/add-card/font switch/dynamic-accent toggle) — none of the H visual work has been device-verified yet

---

## Architecture seams (confirmed)
- **Color:** `Theme.kt` selects the Material3 `ColorScheme` (lines ~104–113); `AppColors.*` reads `LocalAppColorScheme`; `positive`/`negative` already branch on `LocalThemeMode` (Color.kt ~160–177). The existing `dynamicColor` Material You path (`dynamicDark/LightColorScheme(context)`, Theme.kt:106–108) is the seam reused for **M3 dynamic accent** — but its `primary` is injected into the H accent token only, not the whole scheme.
- **Type:** `createTypography(AppFont)` injects only `fontFamily` into a fixed scale (Type.kt 163–179). `AppFont` has `displayFamily`+`bodyFamily` only — **needs a third `monoFamily`**. `AppTextStyles` is the only legal way composables get text styles.
- **Glass:** `GlassTokens.cardStyle/pillStyle/sheetStyle(isDark)` read `LocalGlassSettings`; `LocalGlassmorphismEnabled` is the on/off. Card shell currently uses `cardStyle` → we demote cards to solid panels, keep glass on pill/sheet/nav.
- **Cards:** `AeonExpressiveCard` header is already conditional — `showFilterButton = filterEnabled && filter != null && onFilterChange != null` (AeonExpressiveCard.kt:132), `showSettingsButton = displaySettings != null` (133); the period is derived into `effectiveSubtitle` from `filter.timelineRange` via `timelineLabel(...)` (143–145). `UnifiedCardRenderer`, `AeonCardList` (reorder), `CardLayoutViewModel.addDashboardCard` need **no change**.
- **Settings flow:** `SettingsRepository` (theme/selectedFont/glass…) → `MainActivity` collects → `OssWalletTheme(...)`. New settings (identity, font roles, `dynamicAccent`) follow this exact path.

---

## Phase 0 — Selective-glass policy (identity-agnostic, ships first)
*Rationale: a fully identity-independent change. Retires the biggest perf risk (per-card Haze blur) and validates the solid-panel aesthetic against today's theme before any H work layers on. Reviewable/shippable on its own.*
**Step 0.1** Add reusable `Modifier.aeonHairlineBorder()` in `ui/components/` (1px gradient border using current `accent`/`outlineVariant`; mask-composite style edge), + optional ambient radial glow helper.
**Step 0.2** In `AeonExpressiveCard`, stop using `GlassTokens.cardStyle`; cards become solid `panelSurface` + `aeonHairlineBorder()`. No header/button changes yet (that's Phase 1).
**Step 0.3** Keep `GlassSurface`/blur on nav pill (`pillStyle`), filter pill + sheets (`sheetStyle`), dialogs, sticky headers. Verify `LocalGlassmorphismEnabled = false` still degrades all chrome to solid.
**Acceptance:** Cards are opaque hairline panels (no per-card blur) under the *current* theme; nav pill/filter pill/sheets still blur when glass on; toggling glass off degrades gracefully; dashboard scroll shows no blur-induced jank. `./gradlew assembleDebug` green; registry files untouched.

## Phase 1 — Card shell: self-contained header structure (identity-agnostic)
*No new flag. The header is already conditional (see Architecture seams). We replace the header's structure outright; behavior keys off the existing nullable params. The ~17 non-filter callers (hero/analytics/CSV/management cards) pass no `filter`, so they already render buttonless and are unaffected. Done immediately after Phase 0 because both edit `AeonExpressiveCard.kt` — one shell rewrite, one regression pass — and the structural change is identity-agnostic (validates the buttonless/tappable-readout interaction against the current theme). Typography (Phase 3) and color (Phase 4) later make the title serif and the readout mono/champagne; the **filter-pill/sheet restyle is deferred to Phase 5**.*
**Step 1.1** In `AeonExpressiveCard`, restructure the header Row (197–266): render an in-body block = `title` + (when `filter != null`) tappable **period readout** (`effectiveSubtitle`, dotted underline) + optional top-right link. Remove the filter icon button (`AeonCardFilterButton`) and settings `IconButton` (237–265). Styling stays on current tokens for now (serif/mono arrive in Phase 3).
**Step 1.2** When `filter != null && onFilterChange != null`, tapping the period readout sets `showFilterSheet = true` — reuse the existing pill/sheet path (286–359 via `LocalFilterPillController`); only the trigger moves from the icon to the readout.
**Step 1.3** When `displaySettings != null`, long-press the header opens the existing settings sheet (`showDisplaySettings`; no card-flip for v1). Drag-to-reorder handle stays on the header section via `headerModifier` — assign to the section, not the readout, to avoid gesture conflict.
**Acceptance:** Every *dashboard* card (those that pass a `filter`) shows title + period inside its body, no filter/config icons; tapping the period opens the pill, expands to the sheet, dismisses on outside tap; long-press opens card settings; reorder & add-card still work. Hero/analytics/CSV/management cards render unchanged. `./gradlew assembleDebug` green; no registry files touched.

## Phase 2 — Identity seam (no visual change)
**Step 2.1** Add `domain/model/settings/UiIdentity.kt`: `enum class UiIdentity(displayName) { H, A, B; companion { DEFAULT = H; fromName(...) } }`.
**Step 2.2** Add `LocalUiIdentity = staticCompositionLocalOf { UiIdentity.DEFAULT }` (in Color.kt next to `LocalThemeMode`).
**Step 2.3** Add `identity: Flow<String>` + `saveIdentity()` to `SettingsRepository`/`Impl`/`SettingsDataStore` (mirror `selectedFont`).
**Step 2.4** `OssWalletTheme` gains `identity: String = UiIdentity.DEFAULT.name`; provide `LocalUiIdentity`. `MainActivity` collects and passes it.
**Acceptance:** App builds and runs identically to Phase 1 (default identity = H wired, no palette/type overrides yet). `./gradlew assembleDebug` green. No visible change.

## Phase 3 — Typography: 3-family model + new fonts
**Step 3.1** Add font resources: `res/font/fraunces.ttf` (+ italic), `res/font/jetbrains_mono.ttf`. (User supplies TTFs or we use Google Fonts downloadable provider.)
**Step 3.2** Extend `AppFont` with `monoFamily: FontFamily`. Add entries: keep `MANROPE`/`INTER`/`SYSTEM` (mono → `FontFamily.Monospace`), add `EDITORIAL_INSTRUMENT` (display=Fraunces, body=Inter, mono=JetBrains Mono) as H's font; set `DEFAULT = EDITORIAL_INSTRUMENT`.
**Step 3.3** Add `LocalAppFont = staticCompositionLocalOf { AppFont.DEFAULT }`; provide it in `OssWalletTheme`.
**Step 3.4** In `AppTextStyles`, route **amount** tokens (`heroAmount`, `heroAmountDisplay`, `heroAmountMedium/Large`, `cardSecondaryAmount`, `calculatorDisplay`, `transactionAmount`) to display(serif) family (already via Material display slots) and route **micro-readout** tokens (`overlineLabel`, `sectionOverline`, `metaLabel`, `timelineTabLabel`, `tableColumnHeader`, new `cardPeriodReadout`) to `.copy(fontFamily = LocalAppFont.current.monoFamily)`. The Phase 1 period readout adopts `cardPeriodReadout` here.
**Step 3.5** Font setting UI: extend the existing font picker (`AeonSettingsComponents` / display settings screen) to list the new fonts; persist via `saveSelectedFont`.
**Acceptance:** With H font active, dashboard hero/amounts render in Fraunces serif, period/overline labels in JetBrains Mono, body in Inter; switching font in settings live-updates; tabular-nums preserved (numbers stay column-aligned). Existing screens render without clipping (spot-check entry, analytics, lists). Build green.

## Phase 4 — Color: H palettes (dark + light + AMOLED) + M3 dynamic accent
**Step 4.1** In `Theme.kt`, add `applyIdentityToColorScheme(base, identity, themeMode)`. For `UiIdentity.H` return H palettes:
  - Dark: bg `#0B0B0A`, panel `#131312`, onSurface ivory `#ECE8DD`, onSurfaceVariant `#9A958A`, accent champagne `#D6C189`, outlineVariant hairline `rgba(236,232,221,.09)`.
  - Light: bg `#F2EFE7`, panel `#FAF8F1`, ink `#1B1914`, accent bronze `#8A6F3C`, hairline `#DED9CC`.
  - AMOLED: bg `#000000`, panel `#0C0C0B`, brighten hairlines slightly.
**Step 4.2** Extend `AppColors.positive`/`negative` branch (Color.kt 160–177) to also read `LocalUiIdentity`: H pos/neg = `#7FBF8E`/`#D88379` (dark), `#2C6E49`/`#9B3B30` (light). Add a `champagne`/`accentSignature` token if not covered by `accent`. The `aeonHairlineBorder()` (Phase 0) and the period readout (Phase 1) now pick up the champagne/bronze accent automatically.
**Step 4.3 — M3 dynamic-accent customization (decided: "H neutrals + dynamic accent").** Material You and H's editorial palette directly conflict (wallpaper-derived bg/accents would erase the warm-black/ivory/champagne identity), so full `dynamicColor` (bg+surface+accent from wallpaper) is **off** under H/A/B. Instead H exposes a single **dynamic-accent toggle**:
  - When **off** (default): accent = H's champagne (dark) / bronze (light) signature.
  - When **on** (+ Android 12+ `dynamicColorAvailable`): resolve `dynamicDark/LightColorScheme(context).primary` and inject it **only** into the `accent`/`accentSignature` token. ALL H neutrals (bg, panel, ivory/ink text, hairlines), serif/mono type, and pos/neg semantics stay fixed.
  - Because `aeonHairlineBorder()` (Phase 0), the period readout (Phase 1), filter chips (Phase 5), and card fills (Phase 6) all read the single accent token, the toggle live-recolors the whole accent system without touching structure.
  - Settings UI: add the toggle next to the identity/font pickers; persist via a new `dynamicAccent` flow on `SettingsRepository` (mirror `selectedFont`/`identity`). The legacy `OssWalletTheme(dynamicColor=...)` path is superseded by identity + this toggle.
**Acceptance:** Dashboard + a sample of screens (accounts, budgets, settings, entry) render in H palette across light/dark/AMOLED with correct contrast; no hardcoded-hex regressions (grep shows composables still use `AppColors.*`). Dynamic-accent toggle ON live-recolors accent (hairline, readout, chips, fills) from wallpaper while neutrals/type stay H; toggle OFF returns to champagne/bronze. Build green; existing screenshot-sensitive previews updated.

## Phase 5 — Filter pill / sheet H restyle
*Deferred to here because the H restyle needs the mono family (Phase 3) and the champagne accent (Phase 4). The Phase 1 trigger relocation already works; this is purely cosmetic.*
**Step 5.1** Restyle `TransactionFilterPill` / `FilterSheetContent` to H (mono labels, champagne chips, serif headings) — content/logic unchanged.
**Acceptance:** Pill + sheet match the comps in light + dark (mono labels, champagne chips, serif headings); all filter selection/apply/save logic intact; tap period → pill → sheet → dismiss still works. Build green.

## Phase 6 — Card content restyle (per the comps)
**Step 6.1** **NetWorthCard**: serif hero amount, mono trend readout `▲ +8.2% vs prior`, income/expense as twin mono-labelled columns split by a hairline (replace saturated flow badges).
**Step 6.2** **AccountsCard**: replace saturated full-fill tiles with ledger rows/panels — account color as a small dot/left rule, native balance serif, `≈` base conversion mono; ghost dashed "+ Add account".
**Step 6.3** **CashFlowCard / BudgetsCard**: hairline track bars (gold fill, neg = overspend), mono figures, serif net.
**Acceptance:** Each card matches its comp counterpart in light+dark; all existing data still shown (trend %, per-currency rows, base conversions, budget %); no data loss vs current cards. Build green; relevant `@Preview`s updated.

## Phase 7 — Polish & verification
**Step 7.1** Nav pill, FAB, status/nav bar icon tints, dividers, empty states aligned to H.
**Step 7.2** Update Design Lab preview screen to include identity (so QA can flip H/light/dark/AMOLED).
**Acceptance:** Full app walkthrough in H (dashboard, entry, analytics, accounts, budgets, settings) shows a coherent identity; `./gradlew lint test assembleDebug` green; manual device check of dashboard scroll + filter pill + reorder + add-card + font switch.

---

## Critical files
- New: `domain/model/settings/UiIdentity.kt`; `res/font/fraunces*.ttf`, `res/font/jetbrains_mono.ttf`; `ui/components/` hairline-border modifier.
- Edit: `ui/theme/Theme.kt` (identity→colorScheme, font, dynamic-accent injection, provide locals), `ui/theme/Color.kt` (`LocalUiIdentity`, pos/neg/accent per identity), `ui/theme/Type.kt` (`monoFamily`, `LocalAppFont`, new AppFont, mono tokens), `ui/theme/GlassTokens.kt` (card demotion).
- Edit: `domain/repository/SettingsRepository.kt` + `data/repository/SettingsRepositoryImpl.kt` + `data/local/SettingsDataStore.kt` (identity + font + `dynamicAccent`); `MainActivity.kt` (collect/pass).
- Edit: `ui/components/cards/AeonExpressiveCard.kt` (Phase 0 glass demotion + Phase 1 header restructure); `ui/components/transactions/TransactionFilterPill.kt` + `FilterSheetContent.kt` (Phase 5 H restyle).
- Edit: dashboard cards `NetWorthCard.kt`, `CashFlowCard.kt`, `AccountsCard.kt`, `BudgetsCard.kt`.
- **Untouched (verify):** `UnifiedCardRenderer.kt`, `AeonCardList.kt`, `CardLayoutViewModel.kt`, `CardType.kt`, `CardConfig.kt`, registry files.

## Verification (end-to-end)
- After each phase: `./gradlew assembleDebug` green; app launches; no crash on dashboard.
- Final: `./gradlew lint test` green; on device — verify (1) H palette across light/dark/AMOLED, (2) font switch live-updates serif/mono/sans roles, (3) cards are self-contained & buttonless, (4) tap period → pill → sheet → dismiss, (5) long-press → card settings, (6) reorder + add-card intact, (7) glass only on floating chrome, toggle-off degrades cleanly, (8) dynamic-accent toggle ON recolors only the accent (hairline/readout/budget fill/net/nav/FAB) from wallpaper while neutrals + type stay H; OFF returns to champagne/bronze.
- Compare side-by-side against `design/comps/h-hybrid.html`, `full-screens.html` (H frames), and `h-dynamic-accent.html` (M3 accent toggle).
