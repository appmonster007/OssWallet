---
baseline_commit: ''
---

# Story 2.1: Dashboard Performance Root Cause Analysis (RCA)

## Status
**Status:** ready-for-dev
**Epic:** Epic 2: Universal UI State & Performance (MVI & Dashboard RCA)
**ID:** 2.1

## Story

As a developer,
I want to identify the technical sources of UI stutter on the dashboard,
so that I can implement targeted optimizations that ensure a 60+ FPS experience.

## Acceptance Criteria

**Given** a dashboard that exhibits "jank" during scrolling and 3D card flips
**When** I perform a deep-trace audit using the Android Studio Profiler and Layout Inspector
**Then** I must identify specific "re-composition loops" or expensive calculation blocks causing frame drops
**And** I must produce a remediation plan identifying specific components that require `@Stable`/`@Immutable` markers or `remember` caching
**And** the root cause of "flip-animation stutter" must be explicitly isolated (e.g., state-update collisions)
**And** findings are documented in `docs/rca/dashboard-performance-rca.md`

## Tasks / Subtasks

- [ ] Task 1 — Static code audit: DashboardViewModel stability and state granularity
  - [ ] 1.1 Audit `DashboardUiState.Success` (16 fields) — identify which fields change independently and would trigger full-tree recomposition when bundled in a single data class
  - [ ] 1.2 Audit multiple `StateFlow` collections in `DashboardScreen` (at least 10 `collectAsStateWithLifecycle` calls) — determine if each triggers a separate recomposition scope or if they are batched
  - [ ] 1.3 Audit `flatMapLatest` chains in `DashboardViewModel` — identify any chains that re-emit on every upstream tick vs. only on genuine state changes; note whether `distinctUntilChanged()` is applied
  - [ ] 1.4 Identify the two hardcoded `Dispatchers.Default` usages (lines 208 and 291 of `DashboardViewModel.kt`) and assess whether they interfere with coroutine cancellation during rapid tab switches
  - [ ] 1.5 Document findings for Task 1 in the RCA file

- [ ] Task 2 — Static code audit: DashboardScreen composable stability
  - [ ] 2.1 Audit `DashboardUiState.Success` for Compose stability — `Map<String, CardRenderData>` is **unstable by default** in Compose (Maps are not recognized as stable); this likely forces recomposition of every composable that consumes `cardDataMap` on every state emission
  - [ ] 2.2 Audit the 10-key `remember(state.profileId, state.allTransactions, ...)` in `DashboardGlanceTab` (line ~957) — determine if `List<Transaction>` equality causes unnecessary cache invalidation on every Flow emission
  - [ ] 2.3 Audit `DashboardGlanceTab`'s `rememberUpdatedState` + `AeonInteractionCallbacks` pattern — confirm it correctly prevents recomposition of child cards when only unrelated state changes
  - [ ] 2.4 Audit `HorizontalPager` with lazy list inside — check if pager offscreen content triggers recomposition while the active tab is animating
  - [ ] 2.5 Document findings for Task 2

- [ ] Task 3 — Static code audit: Card rendering pipeline
  - [ ] 3.1 Audit `AeonCardList.kt` — does the `LazyColumn` set `key=` parameters on card items? Missing keys cause O(n) diffing on reorder and prevent composition reuse
  - [ ] 3.2 Audit `UnifiedCardRenderer.kt` (471 lines, 14+ branches) — is any per-card computation done inline in the composable (not memoized)? Note the `remember(renderData, selectedMetric)` in the pie/treemap branches — confirm other branches have equivalent memoization
  - [ ] 3.3 Audit `AeonExpressiveCard` composable — identify where the 3D flip state is stored (`MutableState<Boolean>` or driven by the ViewModel?), and whether a ViewModel state update during the flip animation causes an extra recomposition mid-animation
  - [ ] 3.4 Audit `CardRenderCache` — `ConcurrentHashMap<Pair<Int, String>, Map<String, CardRenderData>>` with "externally serialized writes via ViewModel Mutex" — verify the cache is actually consulted before triggering card recomputation, and that the cache invalidation on currency change doesn't clear all cards simultaneously (causing mass recomposition)
  - [ ] 3.5 Document findings for Task 3

- [ ] Task 4 — Dynamic profiling on device (requires physical Android device or emulator ≥ API 26)
  - [ ] 4.1 Build a debug APK (`./gradlew assembleDebug`) and install on device (`./gradlew installDebug`)
  - [ ] 4.2 Open Android Studio → **Profiler** → **CPU** → **System Trace** → record during: (a) initial dashboard load, (b) scroll through cards, (c) trigger a 3D card flip
  - [ ] 4.3 Identify frames exceeding 16ms (60 FPS budget) — note which phase (measure, layout, draw, or recomposition) is slowest
  - [ ] 4.4 Open **Layout Inspector** with recomposition counts enabled → interact with dashboard → record which composables show high recomposition counts (> 10× per scroll gesture)
  - [ ] 4.5 Record cold start time: time from app launch to first `DashboardUiState.Success` render (target: < 500ms per NFR3)
  - [ ] 4.6 Document profiler screenshots / raw findings in `docs/rca/` directory

- [ ] Task 5 — Synthesize findings into RCA document
  - [ ] 5.1 Create `docs/rca/dashboard-performance-rca.md` (see template in Dev Notes)
  - [ ] 5.2 Prioritize findings: **High** (confirmed frame drop root cause), **Medium** (suspected contributor), **Low** (preventive improvement)
  - [ ] 5.3 For each High/Medium finding, write a concrete remediation action with the specific file + line and what change is needed — these become Story 2.2 and 2.3 task inputs
  - [ ] 5.4 Explicitly state the flip-animation root cause — options: (a) ViewModel state update during flip → extra recomposition → animation jank, (b) heavy composition tree redrawn during flip, (c) main-thread work colliding with animation frame

- [ ] Task 6 — Update sprint-status.yaml
  - [ ] 6.1 Mark story 2.1 `done` in sprint-status.yaml after RCA document is committed

## Dev Notes

### What This Story Produces

This is a **research and documentation story**. The only code change is creating `docs/rca/dashboard-performance-rca.md`. No Kotlin files are modified.

The RCA document feeds directly into Stories 2.2 (MVI refactor) and 2.3 (rendering optimization). Without accurate RCA findings, Stories 2.2 and 2.3 risk optimizing the wrong bottlenecks.

---

### Known Static Findings (Pre-Identified Before Profiling)

The following issues are already confirmed from code audit and do NOT require profiling to identify:

#### Finding A — `Map<String, CardRenderData>` is Compose-unstable [HIGH]
**Location:** `DashboardUiState.Success.cardDataMap: Map<String, CardRenderData>`
**Problem:** Kotlin's `Map` is not on Compose's stable type list. Any composable that reads `cardDataMap` from the UiState will be recomposed on **every** state emission, even when `cardDataMap` has not changed. With 16 state fields updating at different rates, this likely triggers the most unnecessary recompositions.
**Remediation (Story 2.3):** Wrap in `@Immutable data class` or use `kotlinx.collections.immutable.PersistentMap` (add `org.jetbrains.kotlinx:kotlinx-collections-immutable`). Or break `cardDataMap` into its own `StateFlow` separate from the main `uiState`.

#### Finding B — 10 `collectAsStateWithLifecycle` calls at DashboardScreen root [MEDIUM]
**Location:** `DashboardScreen` composable, lines ~225–240
**Problem:** Each call to `collectAsStateWithLifecycle` creates a new `State<T>` that can independently trigger recomposition. When many of these emit simultaneously (e.g., on profile switch), they can trigger a cascade of recomposition passes rather than a single batched pass. Compose batches state reads within a single snapshot but separate Flow collections via `collectAsStateWithLifecycle` are asynchronous and may arrive in multiple frames.
**Remediation (Story 2.2):** Consolidate into a single `DashboardUiState` that encapsulates all state — the MVI refactor.

#### Finding C — No `@Stable` annotation on `DashboardUiState` [MEDIUM]
**Location:** `DashboardUiState` sealed class (line 391, `DashboardViewModel.kt`)
**Problem:** `DashboardUiState.Success` has 16 fields. Without `@Stable` or `@Immutable`, Compose's compiler treats it conservatively and will recompose consumers even when the same instance is re-emitted. Note: `@Immutable` was removed from domain models in Story 1.2 — but `@Immutable` is CORRECT and APPROPRIATE on ViewModel UiState data classes, since they represent view-state snapshots, not domain models.
**Remediation (Story 2.2):** Mark `DashboardUiState.Success` with `@Immutable`. This is the correct placement.

#### Finding D — `DashboardViewModel` has no unified intent handler [MEDIUM]
**Location:** `DashboardViewModel` — multiple individual public functions (`initializeDashboardView`, `resetToInitialView`, `clearViewFocus`, `quickConfirmById`, etc.)
**Problem:** Not a direct FPS issue, but causes multiple `LaunchedEffect` blocks in the composable triggered by state changes rather than explicit intents — each `LaunchedEffect` is a potential recomposition source.
**Remediation (Story 2.2):** MVI refactor adds `onIntent(DashboardIntent)`.

#### Finding E — Two hardcoded `Dispatchers.Default` [LOW]
**Location:** `DashboardViewModel.kt` lines 208 and 291
**Problem:** Violates AD-9 (dispatcher injection). Not a direct FPS issue but prevents unit testing the ViewModel's async behavior without blocking the test dispatcher.
**Remediation (Story 2.2 or separate AD-9 task):** Replace with `dispatchers.default` from injected `DispatcherProvider`.

#### Finding F — `AeonCardList` LazyColumn key= audit needed [MEDIUM, requires code read]
**Location:** `ui/components/cards/AeonCardList.kt`
**Problem:** If `LazyColumn` items don't set `key=` parameters, Compose performs O(n) item diffing on every state update and cannot reuse composition nodes during card reorder. This is a likely source of scroll jank.
**Remediation (Story 2.3):** Add `key = { card.type.name }` to LazyColumn items.

---

### RCA Document Template

Create at `docs/rca/dashboard-performance-rca.md`:

```markdown
# Dashboard Performance RCA
**Date:** YYYY-MM-DD
**Story:** 2.1
**Status:** COMPLETE

## Executive Summary
[2-3 sentences: primary root cause, estimated FPS impact, recommended first fix]

## Methodology
- Static code audit: DashboardViewModel, DashboardScreen, card rendering pipeline
- Dynamic profiling: [device model, OS version, APK build]
- Tools: Android Studio Profiler (System Trace), Layout Inspector (recomposition counts)

## Findings

### [SEVERITY] Finding Title
- **File:** `path/to/file.kt:line`
- **Description:** What the problem is
- **Evidence:** [profiler screenshot ref / recomposition count / frame time]
- **Remediation:** Specific change needed (Story 2.2 or 2.3 task)

## Cold Start Measurement
- Target: < 500ms (NFR3)
- Measured: Xms
- Gap: [pass / Xms over budget]

## Flip Animation Root Cause
[Explicit isolation: which component causes flip stutter and why]

## Remediation Priority
| Priority | Finding | Story | Estimated FPS Gain |
|---|---|---|---|
| 1 | ... | 2.2 | ... |
```

---

### Testing Requirements

No unit tests for this story. The deliverable is the RCA document.

Validation: `docs/rca/dashboard-performance-rca.md` must exist and contain all required sections before story is marked done.

---

### Architecture Compliance

- **AD-3 (Mandatory MVI):** RCA findings will be the primary input to Story 2.2's scope decisions. Document any evidence that the lack of MVI causes recomposition cascade.
- **AD-8 (Domain-First Testing):** This story is testing/profiling — no domain logic is modified.
- **NFR3:** The 500ms cold start target and 60+ FPS scrolling target are the acceptance thresholds for RCA findings. If measured values are already within budget, document that explicitly.

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
