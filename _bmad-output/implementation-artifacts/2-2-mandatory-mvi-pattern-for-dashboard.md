---
baseline_commit: ''
---

# Story 2.2: Mandatory MVI Pattern for Dashboard

## Status
**Status:** ready-for-dev
**Epic:** Epic 2: Universal UI State & Performance (MVI & Dashboard RCA)
**ID:** 2.2

## Story

As a developer,
I want the Dashboard to follow a strict Unidirectional Data Flow (MVI),
so that UI state is deterministic, testable, and optimized for Compose skip-conventions.

## Acceptance Criteria

**Given** a Dashboard currently using fragmented reactive flows and multiple individual handler functions
**When** I refactor the Dashboard ViewModel to expose a single immutable `DashboardUiState` and a single `onIntent(DashboardIntent)` entry point
**Then** all local state mutations in the UI are removed
**And** the ViewModel uses a `Reducer`-like pattern to handle intents and produce state
**And** all data classes used in the `UiState` are marked with `@Immutable` to maximize Compose re-composition skipping
**And** `./gradlew testDebugUnitTest` passes with no regressions
**And** `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL

## Tasks / Subtasks

- [ ] Task 1 — Define `DashboardIntent` sealed class
  - [ ] 1.1 Create `ui/screens/dashboard/DashboardIntent.kt` with a `sealed class DashboardIntent` capturing all dashboard user actions (see Dev Notes for full list)
  - [ ] 1.2 Ensure each intent carries only the data needed for the handler — no UI-specific types (no `Context`, no `View`, no composable lambdas)

- [ ] Task 2 — Annotate `DashboardUiState` with `@Immutable`
  - [ ] 2.1 Add `@Immutable` (from `androidx.compose.runtime`) to `DashboardUiState.Success` data class
  - [ ] 2.2 Add `@Immutable` to `DashboardUiState.Error` data class
  - [ ] 2.3 Verify `DashboardUiState.Loading` (object) does not need annotation (singletons are already stable)
  - [ ] 2.4 Note: `@Immutable` is CORRECT here (ViewModel UiState), distinct from Story 1.2 which removed it from **domain models**. UiState data classes are view-layer snapshots and legitimately benefit from the Compose stability hint.

- [ ] Task 3 — Consolidate `Map<String, CardRenderData>` stability
  - [ ] 3.1 Wrap `cardDataMap: Map<String, CardRenderData>` as `@Immutable data class CardDataSnapshot(val cards: Map<String, CardRenderData>)` — OR change the field type to `ImmutableMap<String, CardRenderData>` using `kotlinx-collections-immutable`
  - [ ] 3.2 If using `kotlinx-collections-immutable`: add `org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8` to `app/build.gradle.kts`; verify the alias or add it to `gradle/libs.versions.toml`
  - [ ] 3.3 Update all usages in `DashboardGlanceTab`, `UnifiedCardRenderer`, and any composable that reads `cardDataMap` from the state

- [ ] Task 4 — Add `onIntent(DashboardIntent)` to `DashboardViewModel`
  - [ ] 4.1 Add `fun onIntent(intent: DashboardIntent)` as the **only** public mutation entry point in `DashboardViewModel`
  - [ ] 4.2 Implement with a `when(intent)` dispatch block that calls the existing private handler functions (do NOT delete the handler logic — just make it private and route through `onIntent`)
  - [ ] 4.3 Keep the handler functions as `private fun` — this minimizes diff risk and keeps the PR reviewable
  - [ ] 4.4 Replace all existing public handler function calls in `DashboardScreen.kt` with `onIntent(DashboardIntent.XXX)` equivalents

- [ ] Task 5 — Inject `DispatcherProvider` into `DashboardViewModel` (AD-9)
  - [ ] 5.1 Add `private val dispatchers: DispatcherProvider` to `DashboardViewModel`'s `@Inject constructor` parameter list
  - [ ] 5.2 Replace `Dispatchers.Default` at lines 208 and 291 of `DashboardViewModel.kt` with `dispatchers.default`
  - [ ] 5.3 Verify `DispatcherProvider` is already provided via `DispatchersModule.kt` (moved to `:core:data` in Story 1.3) — no new DI wiring needed

- [ ] Task 6 — Update `DashboardScreen.kt` to use `onIntent`
  - [ ] 6.1 Replace all direct ViewModel function calls in `DashboardScreen` (e.g., `dashboardViewModel.initializeDashboardView(...)`) with `dashboardViewModel.onIntent(DashboardIntent.InitializeView(...))`
  - [ ] 6.2 Ensure `DashboardActions` data class (line 164 of `DashboardScreen.kt`) is updated to pass `onIntent` as a single lambda `(DashboardIntent) -> Unit` — OR keep individual lambdas that each delegate to `onIntent`. Prefer the single lambda approach for future-proofing.
  - [ ] 6.3 Verify no public ViewModel methods remain that are called directly from the composable except `onIntent`

- [ ] Task 7 — Consolidate fragmented StateFlow collections in `DashboardScreen`
  - [ ] 7.1 Audit the 10+ `collectAsStateWithLifecycle` calls in `DashboardScreen` — identify which flows are secondary (e.g., `cardsConfig`, `lastUndoAction`, `initialViewContext`) vs. primary state
  - [ ] 7.2 Decision point: fold `cardsConfig` into `DashboardUiState.Success.cardsConfig` field — this reduces the number of separate collection points and enables Compose to batch them in a single recomposition pass
  - [ ] 7.3 `lastUndoAction` and `initialViewContext` may stay as separate `StateFlow`s if they represent transient overlay state (acceptable — the goal is to reduce, not eliminate, secondary flows)
  - [ ] 7.4 Document what stayed separate and why in the story's Completion Notes

- [ ] Task 8 — Add ViewModel unit tests for intent handling
  - [ ] 8.1 Create or extend `DashboardViewModelTest.kt` in `src/test/`
  - [ ] 8.2 Use `MainDispatcherRule` and `TestDispatcherProvider` (from the project's existing test infrastructure — see story 1.1 learnings)
  - [ ] 8.3 Write at least 3 tests: `onIntent(ClearViewFocus)` → state transitions to correct value, `onIntent(QuickConfirmById)` → delegates to PlannedPaymentRepository, `onIntent(UndoLastAction)` → `lastUndoAction` resets
  - [ ] 8.4 Run `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL

- [ ] Task 9 — Verify compile and no regressions
  - [ ] 9.1 Run `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL
  - [ ] 9.2 Run `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL
  - [ ] 9.3 Run `./gradlew compileDebugAndroidTestKotlin` → BUILD SUCCESSFUL

- [ ] Task 10 — Update sprint-status.yaml
  - [ ] 10.1 Mark story 2.2 `done` in sprint-status.yaml

## Dev Notes

### Current State — What Exists Today

**`DashboardViewModel.kt`** (413 lines, `ui/screens/dashboard/DashboardViewModel.kt`):
- `@HiltViewModel` with 13 `@Inject` constructor dependencies
- Main state: `val uiState: StateFlow<DashboardUiState>` — single StateFlow, good
- Additional flows: `cardsConfig: StateFlow<DashboardCardsConfig>`, `initialViewContext: StateFlow<DashboardViewContext>`, `lastUndoAction: StateFlow<UndoAction?>`
- Uses `flatMapLatest` on `settingsRepository.currentProfileId` (line ~166) — correct pattern
- **No intent handler** — public mutation functions are: `initializeDashboardView`, `resetToInitialView`, `clearViewFocus`, `quickConfirmById`, `quickSkipById`, `undoLastPlannedPaymentAction`, `clearUndoAction`
- Hardcodes `Dispatchers.Default` at lines 208 and 291
- Coroutine: `viewModelScope`

**`DashboardScreen.kt`** (1328 lines, `ui/screens/dashboard/DashboardScreen.kt`):
- Main composable: `DashboardScreen`, `DashboardScreenLayout`, `DashboardContent`, `DashboardGlanceTab`, `DashboardRecentActivitiesTab`
- `DashboardActions` data class (line 164) holds all callbacks as individual lambdas
- `DashboardConfig` data class (line 157) holds display-mode configuration
- 10+ `collectAsStateWithLifecycle` calls at the `DashboardScreen` level
- `remember(state.profileId, state.allTransactions, ...)` with 10 keys for `CardInput` memoization (line ~957)

**`DashboardUiState`** (sealed class, lines 391–412):
- `Loading` object
- `Success` data class — 16 fields including unstable `Map<String, CardRenderData>`
- `Error` data class

---

### `DashboardIntent` — Complete List of Intents to Define

```kotlin
sealed class DashboardIntent {
    data class InitializeView(
        val section: DashboardSection,
        val accountId: String? = null,
        val categoryId: Long? = null,
        val tagId: Long? = null,
    ) : DashboardIntent()

    object ResetToInitialView : DashboardIntent()
    object ClearViewFocus : DashboardIntent()

    data class QuickConfirmPlannedPayment(val plannedPaymentId: Long) : DashboardIntent()
    data class QuickSkipPlannedPayment(val plannedPaymentId: Long) : DashboardIntent()
    object UndoLastPlannedPaymentAction : DashboardIntent()
    object ClearUndoAction : DashboardIntent()
}
```

Map each existing public function to one of these intents. The function bodies become the `when(intent)` branches.

---

### `@Immutable` on UiState — Why This Is Correct Here

Story 1.2 removed `@Immutable` from **domain model** classes (`Transaction`, `RecordUiModel`). That was correct — domain models should have no Compose dependency.

Story 2.2 **adds** `@Immutable` to **ViewModel UiState** classes. This is the architecturally correct placement per AD-3 (Mandatory MVI). `DashboardUiState.Success` is a view-layer snapshot: all its fields are set once at creation and never mutated — the entire object is replaced when state changes. This satisfies the `@Immutable` contract exactly.

```kotlin
import androidx.compose.runtime.Immutable

@Immutable
data class DashboardUiState.Success(
    val profileId: Int,
    // ... all 16 fields
) : DashboardUiState()
```

---

### `kotlinx-collections-immutable` — If Used for `cardDataMap`

If you choose to use `ImmutableMap` instead of the `@Immutable` wrapper approach:

```toml
# In gradle/libs.versions.toml:
[versions]
kotlinxCollectionsImmutable = "0.3.8"

[libraries]
kotlinx-collections-immutable = { module = "org.jetbrains.kotlinx:kotlinx-collections-immutable", version.ref = "kotlinxCollectionsImmutable" }
```

```kotlin
// In DashboardUiState:
val cardDataMap: ImmutableMap<String, CardRenderData>

// In DashboardViewModel (when building state):
val snapshot = existingMap.toImmutableMap()
```

This is the more principled approach — prefer it if the library is acceptable.

---

### `DashboardActions` — Refactoring Strategy

Current `DashboardActions` (line 164) holds N individual lambdas. Two acceptable approaches:

**Option A — Single `onIntent` lambda (recommended):**
```kotlin
data class DashboardActions(
    val onIntent: (DashboardIntent) -> Unit,
    // Keep non-ViewModel actions (navigation) as separate lambdas:
    val onNavigateToTransactions: (filter: TransactionFilter) -> Unit,
    val onNavigateToAccounts: () -> Unit,
    // ... other navigation/routing callbacks
)
```

**Option B — Keep individual lambdas, each delegates to `onIntent`:**
This avoids changing `DashboardActions` but requires N individual `{ dashboardViewModel.onIntent(DashboardIntent.XXX) }` lambda allocations at the call site — slightly less clean but valid.

Prefer **Option A** for cleaner intent routing.

---

### Avoiding Common Mistakes

1. **Do NOT delete private handler logic** — just wrap it in the `when(intent)` block. The real risk here is accidentally dropping a handler branch.
2. **Do NOT mark domain models `@Immutable`** — only `DashboardUiState` data classes.
3. **Do NOT move `cardsConfig` into the main `uiState` unless tested** — it may introduce recomposition cycles if `cardsConfig` is computed on a different coroutine than `uiState`. Move it only after confirming the pipeline.
4. **`flatMapLatest` chains must stay** — do not simplify them; they correctly handle profile switching.
5. **Do NOT remove `SharingStarted.WhileSubscribed(5000)` from `stateIn()`** — this is mandated by CLAUDE.md.

---

### Previous Story Learnings (1.1, 1.2, 1.4)

- **`DispatcherProvider` pattern**: `DispatcherProvider` interface and `RealDispatcherProvider` are in `core/util/DispatcherProvider.kt`. Inject via constructor as `private val dispatchers: DispatcherProvider`. Use `dispatchers.default` / `dispatchers.io` / `dispatchers.main`. In tests, use `TestDispatcherProvider`.
- **`MainDispatcherRule`**: used in all ViewModel tests (`@get:Rule val mainDispatcherRule = MainDispatcherRule()`). Pattern is established from Story 1.1.
- **`runTest` vs `runBlocking`**: prefer `runTest` with `TestCoroutineScheduler` for new ViewModel tests (deferred item from story 1.4 review).

---

### Testing Requirements

- New file: `app/src/test/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardViewModelTest.kt`
- At minimum 3 intent-handling tests (see Task 8)
- No UI/Compose tests required for this story — MVI correctness is verified at ViewModel level
- `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL

---

### Architecture Compliance

- **AD-3 (Mandatory MVI):** This story directly implements AD-3 for the Dashboard screen — single `UiState`, single `onIntent` entry point, Reducer dispatch.
- **AD-9 (Dispatcher Injection):** Task 5 injects `DispatcherProvider` into `DashboardViewModel` — closes the two hardcoded `Dispatchers.Default` violations.
- **NFR3 (Dashboard < 500ms):** The `@Immutable` annotation on `DashboardUiState.Success` and `ImmutableMap` for `cardDataMap` are prerequisite stabilization steps for the rendering optimization in Story 2.3.

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
