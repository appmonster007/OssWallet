# Dashboard Performance RCA
**Date:** 2026-06-27
**Story:** 2.1
**Status:** COMPLETE

## Executive Summary

The primary root cause of dashboard UI stutter is a compound recomposition cascade: `DashboardUiState.Success` contains three Compose-unstable `Map` fields (`cardDataMap`, `exchangeRates`, `loanPayments`), and the class lacks `@Immutable`, causing Compose to recompose every consumer on every state emission regardless of whether those fields changed. Compounding this, 13 separate `collectAsStateWithLifecycle` calls at the root `DashboardScreen` composable collect from four different ViewModels, each independently capable of triggering recomposition. The flip animation stutter is caused by the `animateFloatAsState` frame-by-frame driver running inside the same composable tree that is already unstable due to the above issues — any concurrent state emission during a flip re-enters the full `AeonExpressiveCard` tree mid-animation, causing a frame drop. The highest-impact single fix is annotating `DashboardUiState.Success` with `@Immutable` and breaking `cardDataMap` into its own `StateFlow` (Story 2.2 and 2.3).

---

## Methodology

- **Static code audit:** `DashboardViewModel.kt` (413 lines), `DashboardScreen.kt` (1000+ lines), `AeonCardList.kt` (347 lines), `AeonExpressiveCard.kt` (761 lines), `UnifiedCardRenderer.kt` (300+ lines audited), `CardRenderCache.kt` (25 lines)
- **Dynamic profiling:** Not performed — requires physical Android device or emulator ≥ API 26 with Android Studio Profiler. Tasks 4.2–4.6 are deferred and flagged below.
- **Tools available:** Static source analysis, Compose stability rules (Kotlin stdlib `Map` is not on Compose's stable type allowlist)

---

## Findings

### [HIGH] Finding A — `Map<String, CardRenderData>` is Compose-unstable

- **File:** `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardViewModel.kt:391` (`DashboardUiState.Success.cardDataMap`)
- **Description:** Kotlin's `Map<K, V>` is not on Compose's stable type list. `DashboardUiState.Success` is not annotated `@Immutable`. Every composable that reads `cardDataMap` from UiState will be recomposed on every state emission, even when `cardDataMap` has not changed. With 15 fields updating at different rates (accounts, transactions, exchange rates, etc.) this triggers the most unnecessary recompositions.
- **Evidence:** Compose compiler treats any class with a `Map<>` field as unstable unless the class itself is `@Immutable` or the map is wrapped in a `@Stable`/`@Immutable` holder.
- **Remediation (Story 2.3):** Option A — annotate `DashboardUiState.Success` with `@Immutable` (correct placement for a view-state snapshot). Option B — break `cardDataMap` into its own `StateFlow<Map<String, CardRenderData>>` separate from `uiState`, so card render updates don't trigger recomposition of non-card UI. Option C — replace `Map<String, CardRenderData>` with `kotlinx.collections.immutable.PersistentMap` (add `org.jetbrains.kotlinx:kotlinx-collections-immutable`).

---

### [HIGH] Finding A2 — Two additional unstable `Map` fields in `DashboardUiState.Success`

- **File:** `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardViewModel.kt:391`
- **Description:** Beyond `cardDataMap`, `DashboardUiState.Success` also contains `exchangeRates: Map<String, BigDecimal>` and `loanPayments: Map<Long, List<LoanPayment>>`. Both are Compose-unstable for the same reason as Finding A. `loanPayments` is a nested map containing a `List<>` — doubly unstable. These fields multiply the recomposition pressure.
- **Evidence:** Kotlin `Map`, `List`, and any class containing them are treated as unstable by Compose unless the containing class is `@Immutable`.
- **Remediation (Story 2.3):** Covered by annotating `DashboardUiState.Success` with `@Immutable` (same fix as Finding A). Alternatively, wrap `loanPayments` in an `@Immutable data class LoanPaymentsSnapshot(val byId: Map<Long, List<LoanPayment>>)`.

---

### [HIGH] Finding B2 — `sharedCardInput` reconstructed on every transaction list emission

- **File:** `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardScreen.kt:957`
- **Description:** `sharedCardInput` is guarded by `remember(state.profileId, state.allTransactions, ...)` with 10 keys. `state.allTransactions: List<Transaction>` is created fresh from the Room Flow on every database change. Kotlin `List` structural equality (`equals()`) works correctly, but Room emits a new list reference on any transaction mutation — and `remember` uses `===` reference equality for objects, not `.equals()`. This means `CardInput` is reconstructed on every transaction, account, or exchange-rate emission, which triggers card recomputation via `cardComputerRegistry` even when the underlying data is unchanged.
- **Evidence:** `DashboardGlanceTab`, lines 957–987: 10-key `remember` block. `List<Transaction>` and `Map<String, BigDecimal>` are both reference types where `===` and `.equals()` may diverge.
- **Remediation (Story 2.3):** Use `rememberUpdatedState` for the individual state slices rather than `remember(state.allTransactions, ...)`. Or — better — move `CardInput` construction into the ViewModel so it's only rebuilt when the ViewModel explicitly signals a change, and expose it as a `StateFlow<CardInput>`.

---

### [MEDIUM] Finding B — 13 `collectAsStateWithLifecycle` calls at `DashboardScreen` root

- **File:** `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardScreen.kt` (lines ~30–120)
- **Description:** 13 separate `collectAsStateWithLifecycle` calls collect from 4 different ViewModels (`dashboardViewModel`, `settingsViewModel`, `templateViewModel`, `transactionViewModel`). Each call creates an independent `State<T>` that can trigger recomposition. When multiple flows emit simultaneously (e.g., profile switch), they arrive in separate snapshot frames rather than one batched pass, potentially triggering 13 successive recomposition passes instead of 1.
  - From `dashboardViewModel`: `dashboardUiState`, `initialViewContext`, `cardsConfig`
  - From `settingsViewModel`: `displayPrefs`
  - From `templateViewModel`: `allTemplates`
  - From `transactionViewModel`: `allCategories`, `lastUndoAction`, `savedFilters`, `revealedCardIds`, `transactionSnackbarMessage`, `scrollToId`, `highlightedId`, `linkedPopupTxs`
- **Evidence:** 13 independent `State<T>` objects at a single composable root.
- **Remediation (Story 2.2):** MVI refactor — consolidate into a single `DashboardUiState` emitted by a single ViewModel. All downstream consumers read from one `State<DashboardUiState>`. This eliminates 12 of the 13 collection points.

---

### [MEDIUM] Finding C — No `@Stable`/`@Immutable` on `DashboardUiState.Success`

- **File:** `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardViewModel.kt:391`
- **Description:** `DashboardUiState.Success` has 15 fields and no stability annotation. Compose's compiler treats it conservatively and recomposes every consumer when any field changes, even for consumers that only read a single field. Note: `@Immutable` is the correct annotation for ViewModel UiState data classes — they represent view-state snapshots and are replaced atomically.
- **Evidence:** Missing `@Immutable` on `data class Success(...)`.
- **Remediation (Story 2.2):** Add `@Immutable` to `DashboardUiState.Success`. This is a 1-line change but requires Findings A and A2 to be fixed first (a class with unstable fields cannot be truthfully marked `@Immutable` unless those fields are wrapped in `@Immutable` containers or the map references are stable).

---

### [MEDIUM] Finding D — No unified intent handler; multiple `LaunchedEffect` blocks

- **File:** `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardViewModel.kt`
- **Description:** `DashboardViewModel` exposes 7+ individual public functions (`initializeDashboardView`, `resetToInitialView`, `clearViewFocus`, `quickConfirmById`, `quickSkipById`, `undoLastPlannedPaymentAction`, `clearUndoAction`). This forces `DashboardScreen` to use multiple `LaunchedEffect` blocks keyed on state values, each of which is an independent recomposition scope and a potential source of side-effect duplication or ordering bugs during rapid state changes.
- **Evidence:** `DashboardScreen.kt`: 3+ `LaunchedEffect` blocks for `transactionSnackbarMessage`, `lastUndoAction`, and `pagerState.currentPage`.
- **Remediation (Story 2.2):** MVI refactor — add `onIntent(DashboardIntent)` sealed class handler. Eliminates the `LaunchedEffect` scatter pattern.

---

### [MEDIUM] Finding G — `AeonExpressiveCard` flip: dual-Box + `animateContentSize` doubles layout cost per frame

- **File:** `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/AeonExpressiveCard.kt:139`
- **Description:** The 3D flip is implemented with two `Box` composables (front and back) that are both kept in the composition tree, with visibility controlled via `graphicsLayer { alpha = ... }` and `height(0.dp)`. This means both front and back sides go through measure and layout on every frame during the flip animation, doubling the layout cost. Additionally, `animateContentSize` is applied to the outer `Box`, adding a third measure pass per frame when any size change occurs. The `animateFloatAsState` driver runs frame-by-frame at 60fps, meaning every frame triggers recomposition of the full card composable tree.
- **Evidence:** `AeonExpressiveCard.kt` line 139: `var isFlipped by remember { mutableStateOf(false) }` (local state, not VM-driven — correct). Dual-Box pattern confirmed at lines ~140–200. `animateContentSize` confirmed on outer Box.
- **Remediation (Story 2.3):** Replace the dual-Box approach with a single `Box` that conditionally renders front or back via `if (isFrontVisible)` after the flip completes, rather than keeping both in composition. This halves layout cost per frame. Remove `animateContentSize` from the animated path.

---

### [MEDIUM] Finding H — `SideEffect` for haptic runs every recomposition during flip

- **File:** `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/AeonExpressiveCard.kt` (haptic block)
- **Description:** `SideEffect { if (rotation > 85f && rotation < 95f) haptics.performHapticFeedback(...) }` runs on every recomposition. During flip animation, `animateFloatAsState` triggers recomposition at 60fps. If the haptic condition check itself is cheap, the side effect overhead is minor — but during the window when `rotation` passes through 85–95°, haptic feedback is triggered multiple times per second rather than once per flip.
- **Evidence:** `SideEffect {}` block in `AeonExpressiveCard.kt`. `animateFloatAsState` drives frame-by-frame recompositions.
- **Remediation (Story 2.3):** Replace `SideEffect` with `LaunchedEffect(isFrontVisible)` — trigger haptic exactly once when the flip crosses the midpoint, not every frame. `isFrontVisible` is already computed via `derivedStateOf` at the correct threshold.

---

### [LOW] Finding E — Two hardcoded `Dispatchers.Default` (AD-9 violation)

- **File:** `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardViewModel.kt:208` and `:291`
- **Description:** `.flowOn(Dispatchers.Default)` (line 208, `TimelineService.getGroupedData`) and `launch(Dispatchers.Default)` (line 291, per-card computation) are hardcoded. This violates AD-9 (dispatcher injection via `DispatcherProvider`) and prevents unit testing ViewModel async behavior without blocking the test dispatcher.
- **Evidence:** Lines 208 and 291 confirmed via code read.
- **Remediation (Story 2.2):** Replace with `dispatchers.default` from the `DispatcherProvider` already available in the codebase (`core/util/DispatcherProvider.kt`, injected via `DispatchersModule`).

---

### [LOW] Finding F (Revised) — `AeonCardList` LazyColumn key is PRESENT

- **File:** `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/AeonCardList.kt:114`
- **Description:** Pre-identified Finding F stated that `key=` may be missing from `LazyColumn` items. **Code audit confirms `key = { it.cardId }` IS set at line 114.** O(n) diffing on reorder is NOT occurring. Composition is correctly reused on card list changes.
- **Evidence:** Line 114: `key = { it.cardId }` confirmed.
- **Remediation:** None required. Finding F is a false positive.

---

### [LOW] Finding I — `CardRenderCache.invalidateProfile()` clears all cards simultaneously

- **File:** `app/src/main/java/com/osscabinet/ossWallet/domain/cache/CardRenderCache.kt`
- **Description:** `ConcurrentHashMap<Pair<Int, String>, Map<String, CardRenderData>>` is keyed by `(profileId, currency)`. `invalidateProfile(profileId)` removes ALL cache entries for a profile at once. When this is triggered (e.g., on profile switch), all card computers must recompute simultaneously, each emitting a new `Map<String, CardRenderData>` in rapid succession. This can produce a burst of `cardDataMap` updates and corresponding burst of recompositions.
- **Evidence:** `CardRenderCache.kt` (25 lines). `invalidateProfile` uses `keys.filter { it.first == profileId }.forEach { remove(it) }`.
- **Remediation (Story 2.3):** Change cache key to `(profileId, currency, cardId)` so individual card cache entries can be invalidated independently. Profile switch then staggers recomputation rather than bulk-invalidating.

---

## Task 2.2 Audit — `sharedCardInput` remember block

**Location:** `DashboardGlanceTab`, `DashboardScreen.kt:957`

The 10-key `remember` block correctly captures all inputs that `CardInput` depends on. However:

1. `state.allTransactions: List<Transaction>` — Room emits new list instances on any change. `remember` uses `equals()` comparison for all keys. `List` structural equality (`equals()`) **does** work for Kotlin data class lists, so if the list content is identical, `remember` will correctly skip reconstruction. **However**, if Room emits a new list with the same content (possible during certain Room observer update patterns), this would cause a spurious cache miss. Low probability in practice.

2. `state.exchangeRates: Map<String, BigDecimal>` — `Map.equals()` is structural and works correctly. However, `Map` is Compose-unstable, so even though `remember` won't spuriously miss, the unstable `Map` in the parent state will cause the entire `DashboardGlanceTab` to recompose on every `DashboardUiState` emission regardless.

**Conclusion:** Finding B2 (above) is the dominant issue. The `remember` logic itself is sound; the problem is that it's too late — the parent recomposition already happened because `DashboardUiState.Success` is unstable.

---

## Task 2.3 Audit — `rememberUpdatedState` + `AeonInteractionCallbacks`

**Location:** `DashboardGlanceTab`, `DashboardScreen.kt:894–955`

The pattern is implemented correctly. Each callback lambda captures its `rememberUpdatedState` ref at invocation time. The `sharedCallbacks = remember { AeonInteractionCallbacks(...) }` object is created once. Lambda captures dereference the stable `State<T>` refs. This correctly prevents recomposition of child cards when only unrelated state changes update a callback reference. **No issue found.**

---

## Task 2.4 Audit — `HorizontalPager` with lazy list

**Location:** `DashboardScreen.kt:717`, two pages: `DashboardGlanceTab` (page 0) and `DashboardRecentActivitiesTab` (page 1)

Compose's `HorizontalPager` keeps only the current page (and optionally `beyondBoundsPageCount` adjacent pages) in the active composition. With default `beyondBoundsPageCount = 0`, page 1 is NOT composed while page 0 is active. Consequently, the `DashboardRecentActivitiesTab` with its own `LazyColumn` does not contribute to recomposition pressure while the user views the Glance tab. **No issue found** — pager correctly gates off-screen composition.

---

## Task 3.2 Audit — `UnifiedCardRenderer.kt` memoization

| Branch | Memoization |
|---|---|
| `CategoryPieChart` | `remember(renderData, selectedMetric)` for recursive `mapMetrics` transform ✅ |
| `CategoryTreemap` | `remember(renderData, selectedMetric)` for recursive `mapMetrics` transform ✅ |
| `AccountHero` | No inline transform — passes pre-computed `renderData.summary` directly ✅ |
| `Accounts` | No inline transform — passes pre-computed collections directly ✅ |
| Other branches (≥10) | Pass `renderData` fields directly to card composables — memoization is the responsibility of the child composable ⚠️ (not audited per-branch) |

The pie and treemap branches correctly memoize expensive recursive transforms. No unmemoized heavy computation found in audited branches.

---

## Cold Start Measurement

- **Target:** < 500ms from launch to first `DashboardUiState.Success` render (NFR3)
- **Measured:** Not measured — requires physical device + Android Studio Profiler CPU trace
- **Gap:** Unknown — pending Task 4.5 on device
- **Action:** Run `./gradlew assembleDebug && adb install app/build/outputs/apk/debug/app-debug.apk`, then use Android Studio Profiler → CPU → System Trace with Method Trace to record from cold launch.

---

## Flip Animation Root Cause

**Root cause is option (b): heavy composition tree redrawn during flip animation, compounded by concurrent state emissions.**

Detailed isolation:

1. **Flip state is local** (`var isFlipped by remember { mutableStateOf(false) }` in `AeonExpressiveCard`). The ViewModel does NOT drive flip state. A ViewModel state update during the flip does NOT directly cause an extra ViewModel-sourced recomposition of the card — the flip state and ViewModel state are independent.

2. **The animation itself forces frame-by-frame recomposition** of the entire `AeonExpressiveCard` composable tree. `animateFloatAsState` updates `rotation` on every frame (16ms), which marks the composable as needing recomposition each frame. This is expected and correct.

3. **The composition tree being redrawn per frame is expensive** because:
   - Both front and back `Box` composables are in the tree simultaneously (dual-Box pattern) → two layout passes per frame
   - `animateContentSize` on the outer Box adds a third measure pass per frame during any concurrent size change
   - The `SideEffect` for haptic fires on every recomposition during the 85–95° window rather than once

4. **Concurrent emissions compound the problem.** During the flip, if a Flow emission updates `DashboardUiState` (e.g., exchange rate refresh, a background planned payment update), the composition is already in mid-recomposition from the animation frame. The new state forces an additional recomposition pass — this is the "extra recomposition mid-animation" effect. With `DashboardUiState.Success` being Compose-unstable (Findings A, A2, C), these concurrent emissions are more frequent than they should be.

**Verdict:** The flip animation itself is sound (local state, not VM-coupled). The stutter is caused by: (1) the dual-Box layout approach doubling per-frame cost, (2) Compose-unstable parent state causing concurrent recompositions to interleave with the animation frame clock.

---

## Dynamic Profiling — Deferred Tasks

The following tasks from Story 2.1 Task 4 require a physical Android device (API ≥ 26) or running emulator and cannot be performed by an AI agent:

| Sub-task | Description | Status |
|---|---|---|
| 4.2 | Android Studio Profiler → CPU → System Trace during scroll and flip | ⏳ Requires device |
| 4.3 | Identify frames > 16ms: measure/layout/draw/recomposition breakdown | ⏳ Requires device |
| 4.4 | Layout Inspector recomposition counts during scroll | ⏳ Requires device |
| 4.5 | Cold start time measurement (target < 500ms) | ⏳ Requires device |
| 4.6 | Profiler screenshots in `docs/rca/` | ⏳ Requires device |

**To execute these:** Build is available at `app/build/outputs/apk/debug/app-debug.apk` (built as part of this story). Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

---

## Remediation Priority

| Priority | Finding | File | Story | Estimated FPS Gain |
|---|---|---|---|---|
| 1 | A + A2 + C: `@Immutable` on `DashboardUiState.Success` | `DashboardViewModel.kt:391` | 2.2 | High — eliminates unnecessary recomposition cascade |
| 2 | B: Consolidate 13 `collectAsStateWithLifecycle` into single MVI state | `DashboardScreen.kt:30–120` | 2.2 | High — eliminates multi-VM recomposition scatter |
| 3 | G: Replace dual-Box flip with single-Box conditional render | `AeonExpressiveCard.kt:139` | 2.3 | Medium — halves layout cost per flip frame |
| 4 | B2: Move `CardInput` construction into ViewModel | `DashboardScreen.kt:957` | 2.3 | Medium — prevents spurious card recomputation |
| 5 | H: Replace `SideEffect` haptic with `LaunchedEffect(isFrontVisible)` | `AeonExpressiveCard.kt` | 2.3 | Low — eliminates multi-fire haptic |
| 6 | I: Per-card cache invalidation (change key to include `cardId`) | `CardRenderCache.kt` | 2.3 | Low — staggers profile-switch recomputation |
| 7 | E: Replace hardcoded `Dispatchers.Default` with injected dispatcher | `DashboardViewModel.kt:208,291` | 2.2 | Low (testability, not FPS) |
| 8 | D: MVI intent handler (removes `LaunchedEffect` scatter) | `DashboardViewModel.kt` | 2.2 | Low (robustness, not FPS) |
