---
baseline_commit: ''
---

# Story 2.3: Dashboard Rendering Optimization (Lazy Loading & Caching)

## Status
**Status:** ready-for-dev
**Epic:** Epic 2: Universal UI State & Performance (MVI & Dashboard RCA)
**ID:** 2.3

## Story

As a developer,
I want to implement high-performance rendering for the Dashboard cards,
so that the UI remains fluid even with complex financial aggregations.

## Acceptance Criteria

**Given** the performance remediation plan from Story 2.1 and the MVI structure from Story 2.2
**When** I implement lazy loading for off-screen cards and optimize the card-render cache
**Then** the dashboard initial load must complete in under 500ms (NFR3)
**And** scrolling must maintain a consistent 60+ FPS on mid-range devices
**And** 3D card flips must occur without visible frame drops during the state transition
**And** `./gradlew testDebugUnitTest` passes with no regressions

## Tasks / Subtasks

- [ ] Task 1 — Verify prerequisites
  - [ ] 1.1 Confirm Story 2.1 RCA document exists at `docs/rca/dashboard-performance-rca.md` and has a complete Findings section
  - [ ] 1.2 Confirm Story 2.2 is `done` — `DashboardUiState.Success` is `@Immutable`, `cardDataMap` is stable (`ImmutableMap` or `@Immutable` wrapper), `DashboardViewModel.onIntent()` exists

- [ ] Task 2 — Add `key=` parameters to `AeonCardList` LazyColumn
  - [ ] 2.1 Open `ui/components/cards/AeonCardList.kt` — find the `LazyColumn` `items(...)` call
  - [ ] 2.2 Add `key = { card -> card.type.name }` (or equivalent stable String key) to the `items` block — this enables Compose to reuse composition nodes on card reorder instead of re-creating them O(n)
  - [ ] 2.3 Verify that `CardWithCallbacks` (or the card data type used in the list) has a stable unique identifier — `CardType.name` (enum `name` property) is a stable String, use it
  - [ ] 2.4 Run a quick manual test: reorder two dashboard cards and confirm no flicker

- [ ] Task 3 — Add `contentType=` parameters to `AeonCardList` LazyColumn
  - [ ] 3.1 In the same `items(...)` call, add `contentType = { card -> card.type }` — this allows Compose to reuse composition slots of the same card type when the list changes, further reducing recomposition cost
  - [ ] 3.2 Verify `CardType` enum is accessible from `AeonCardList.kt`

- [ ] Task 4 — Audit and fix `UnifiedCardRenderer` per-branch memoization
  - [ ] 4.1 Read `UnifiedCardRenderer.kt` (471 lines) fully — for each of the 14+ `when(cardWithCallbacks)` branches, check if per-render data transformations are wrapped in `remember(...)`
  - [ ] 4.2 The `CategoryPieChart` and `CategoryTreemap` branches already have `remember(renderData, selectedMetric)` for `mapMetrics` — confirm this pattern
  - [ ] 4.3 For any other branch that performs an inline data transform (e.g., list filtering, sorting, currency conversion), add `remember(renderData) { transform(renderData) }` wrappers
  - [ ] 4.4 Do NOT add `remember` to branches that only forward data with zero transformation — it adds overhead without benefit

- [ ] Task 5 — Decouple flip animation state from ViewModel state
  - [ ] 5.1 Locate `AeonExpressiveCard.kt` (the card flip composable in `ui/components/`) — identify how the flip state is stored
  - [ ] 5.2 If flip state is driven by `DashboardUiState` (i.e., a field like `isFlipped: Boolean` in `DashboardUiState.Success`), this causes the entire uiState to change during the flip animation, forcing recomposition of ALL card consumers mid-animation. Move flip state to `remember { mutableStateOf(false) }` local state inside the card composable — flip state is ephemeral UI state, not business state
  - [ ] 5.3 If flip state is already local (`remember`-based), confirm no ViewModel reads/writes during flip — just verify and document
  - [ ] 5.4 Verify the fix: trigger a 3D card flip while other cards are loading — no stutter should occur

- [ ] Task 6 — Optimize `CardRenderCache` invalidation granularity
  - [ ] 6.1 Read `domain/cache/CardRenderCache.kt` — current invalidation: `invalidateProfile(profileId)` removes ALL cards for a profile at once
  - [ ] 6.2 When exchange rates change, the current code likely calls `invalidateProfile()` invalidating all card caches simultaneously — this triggers a mass recomputation of all cards in a single frame, spiking CPU
  - [ ] 6.3 Implement `fun invalidateCard(profileId: Int, currency: String, cardType: String)` for per-card granular invalidation — recompute cards in a staggered fashion rather than all at once
  - [ ] 6.4 In `DashboardViewModel`, after currency change, enqueue card recomputation sequentially (`asSequence().forEach`) rather than firing all `launch` blocks simultaneously
  - [ ] 6.5 If the staggered approach is too complex for this story, defer and document the specific lines that need changing as an action item

- [ ] Task 7 — Implement on-demand card computation (lazy loading for off-screen cards)
  - [ ] 7.1 Review how `DashboardViewModel` computes card data — does it compute ALL cards on every state update, or only the visible ones?
  - [ ] 7.2 If all cards are computed eagerly: identify the `CardComputerRegistry` dispatch loop in `DashboardViewModel` (line ~291, `launch(Dispatchers.Default)`)
  - [ ] 7.3 Instead of computing all cards on load, compute only the first N cards (default: 4, matching typical screen height) immediately; compute remaining cards as the user scrolls
  - [ ] 7.4 Use `LazyListState.firstVisibleItemIndex` observable (already stored in `DashboardGlanceTab` as `rememberLazyListState()`) to trigger computation of cards as they approach the viewport — emit `DashboardIntent.CardBecameVisible(cardType)` from `LaunchedEffect(lazyListState.firstVisibleItemIndex)`
  - [ ] 7.5 Add `DashboardIntent.CardBecameVisible(cardType: CardType)` to `DashboardIntent` sealed class (Story 2.2 created this file)

- [ ] Task 8 — Cold start optimization: prioritize critical-path data
  - [ ] 8.1 Profile the `DashboardViewModel.uiState` pipeline — which repository calls block the `Loading → Success` transition?
  - [ ] 8.2 `accountRepository.observeAccounts()` is on the critical path — ensure it emits quickly without waiting for exchange rate load
  - [ ] 8.3 `ComputationRepository.computationContext` emits null until exchange rates load — ensure the UI shows partial content (`isCardsComputing = true`) while rates load rather than staying on `Loading`
  - [ ] 8.4 The existing `isCardsComputing: Boolean` field in `DashboardUiState.Success` is the right mechanism — verify it is set to `true` during card computation and `false` after, and that the UI shows appropriate shimmer placeholders via `CardShimmerPlaceholder` in `AeonCardList`

- [ ] Task 9 — Verify performance targets
  - [ ] 9.1 Build debug APK and install on device
  - [ ] 9.2 Measure cold start: time from launch to first meaningful content render — target < 500ms (NFR3)
  - [ ] 9.3 Scroll through all dashboard cards — confirm no jank (target: 60+ FPS sustained)
  - [ ] 9.4 Trigger 3D card flip — confirm animation is smooth (no frame skip)
  - [ ] 9.5 Record measurements in story Completion Notes

- [ ] Task 10 — Run test suite
  - [ ] 10.1 `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL
  - [ ] 10.2 `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL

- [ ] Task 11 — Update sprint-status.yaml
  - [ ] 11.1 Mark story 2.3 `done` in sprint-status.yaml

## Dev Notes

### Prerequisites: What Story 2.2 Must Have Delivered

Before Task 2 begins, verify these are in place:
- `DashboardUiState.Success` is annotated `@Immutable`
- `cardDataMap` is `ImmutableMap<String, CardRenderData>` or equivalent stable type
- `DashboardViewModel.onIntent(DashboardIntent)` exists
- `DashboardViewModel` injects `DispatcherProvider`, `dispatchers.default` used at lines 208 and 291

If any of these are missing, the rendering optimizations will not achieve their intended effect (Compose will still recompose on unstable types).

---

### `AeonCardList` — `key=` and `contentType=` Pattern

```kotlin
// Before (assumed current state — no key/contentType):
items(cards) { card ->
    UnifiedCardRenderer(cardWithCallbacks = card)
}

// After:
items(
    items = cards,
    key = { card -> card.type.name },        // stable String key → Compose reuses nodes
    contentType = { card -> card.type }      // same ContentType → slot reuse on list changes
) { card ->
    UnifiedCardRenderer(cardWithCallbacks = card)
}
```

**Why this matters:** Without `key=`, when card order changes (e.g., user reorders cards via drag-and-drop), Compose discards all composition nodes and rebuilds from scratch. With `key=`, Compose moves the existing nodes to their new positions, preserving remembered state (like flip animation state) and avoiding full recomposition.

---

### AeonExpressiveCard Flip — Local State Pattern

The 3D flip animation should use local `remember` state, not ViewModel state:

```kotlin
@Composable
fun AeonExpressiveCard(
    // ...
) {
    // CORRECT: flip state is local, ViewModel has no knowledge of it
    var isFlipped by remember { mutableStateOf(false) }

    // The flip animation runs on AnimatedContent or custom rotation modifier
    // driven by this local state — no ViewModel emission during animation
}
```

If the current implementation stores `isFlipped` in `DashboardUiState.Success`, the entire `uiState` StateFlow emits a new value on every flip toggle. This triggers recomposition of `DashboardScreen` (which collects `uiState`) and all its child composables — while the animation is running. This is the most likely root cause of flip-animation stutter.

---

### Lazy Card Computation — Intent Pattern

```kotlin
// In DashboardIntent.kt (add to sealed class):
data class CardBecameVisible(val cardType: CardType) : DashboardIntent()

// In DashboardGlanceTab composable:
val firstVisible = lazyListState.firstVisibleItemIndex
LaunchedEffect(firstVisible) {
    val visibleCards = cards.drop(firstVisible).take(PREFETCH_AHEAD_COUNT)
    visibleCards.forEach { card ->
        onIntent(DashboardIntent.CardBecameVisible(card.type))
    }
}

// In DashboardViewModel.onIntent:
is DashboardIntent.CardBecameVisible -> computeCardIfNeeded(intent.cardType)
```

`PREFETCH_AHEAD_COUNT = 2` is a reasonable initial value — prefetch 2 cards ahead of the viewport.

---

### `CardRenderCache` Staggered Invalidation

If you implement per-card granular cache invalidation (Task 6), the new method should be:

```kotlin
fun invalidateCard(profileId: Int, currency: String, cardType: String) {
    cache.remove(Triple(profileId, currency, cardType))
}
```

This requires changing the cache key from `Pair<Int, String>` (profileId, currency) to `Triple<Int, String, String>` (profileId, currency, cardType). Update all usages in `DashboardViewModel` accordingly.

If this scope change is too large, defer it as a `TODO(story-2.3-followup)` comment at the relevant lines.

---

### What to Measure and Document

In the story Completion Notes, record:
- Cold start time measured (was it < 500ms?)
- Scroll FPS (subjective: smooth / minor jank / significant jank)
- Flip animation smoothness (smooth / single frame skip / visible stutter)
- Which tasks were deferred and why

This data closes NFR3 ("Dashboard initial load must complete in under 500ms") from the RCA.

---

### Architecture Compliance

- **AD-3 (MVI):** This story builds on Story 2.2's MVI foundation. The `CardBecameVisible` intent ensures lazy loading follows the intent-based UDF pattern.
- **AD-4 (Cache Lifecycle):** The `CardRenderCache` changes in Task 6 move toward more granular invalidation — precursor to Story 5.2's `CacheCoordinator`.
- **NFR3 (< 500ms):** Cold start measurement in Task 9 directly validates NFR3.

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
