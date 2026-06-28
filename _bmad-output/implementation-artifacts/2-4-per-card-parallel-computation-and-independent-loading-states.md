---
baseline_commit: f1e1ec9da8094a09c00a8f430b0cc48d7263a575
---

# Story 2.4: Per-Card Parallel Computation & Independent Loading States

Status: done

---

## Story

As a user,
I want every card slot across Dashboard, Analytics, and Account Detail to appear immediately
as a shimmer and resolve independently as its data finishes computing,
so that fast cards appear before slow ones, no card slot is ever invisible, and all screens
feel responsive during data load rather than blank.

---

## Codebase Audit (2026-06-28)

### What is already correct — Dashboard

Dashboard card computation is largely correct:

- All N cards are launched in parallel via `configs.forEach { launch(dispatchers.default) }` — no cap.
- `_isCardsComputing` flips `false` only when `completedCount.incrementAndGet() >= configs.size`.
- Round starts with `_cardDataMap.value = persistentMapOf()` — eliminates stale cache read.
- `CategoryHierarchyCache.build()` runs on `dispatchers.default` inside `structuralStateFlow.flowOn(dispatchers.default)` and is stored as `StructuralState.hierarchy`, then passed to `CardInput.categoryHierarchy` before card computation begins. ✅

### What is wrong — Dashboard

**Issue 1: O(N) map allocation per card completion**

`DashboardViewModel.kt:510`:
```kotlin
_cardDataMap.update { (it + (cardConfig.cardId to data)).toImmutableMap() }
```
`it` is `ImmutableMap<String, CardRenderData>`. The `+` operator calls Kotlin stdlib `Map.plus`, which creates an intermediate `LinkedHashMap` copying all N entries. Then `.toImmutableMap()` copies again into a `PersistentHashMap`. **This is O(N) per card update**, not structural sharing.

Fix: declare `_cardDataMap` as `MutableStateFlow<PersistentMap<...>>` and use `PersistentMap.put()` which is O(log₃₂N) HAMT structural sharing:
```kotlin
private val _cardDataMap = MutableStateFlow<PersistentMap<String, CardRenderData>>(persistentMapOf())
// update:
_cardDataMap.update { it.put(cardConfig.cardId, data) }
```

**Issue 2: Dead `CardBecameVisible` / `computeCardIfNeeded` causes duplicate computation**

`DashboardViewModel.kt:197` routes `CardBecameVisible` to `computeCardIfNeeded`. Since all cards are now eagerly computed in parallel, scroll-triggered `CardBecameVisible` events call `computeCardIfNeeded` which:
1. Checks `cardRenderCache[...]` — miss (computation still in flight, cache not yet written)
2. `_inFlightCardIds.add(config.cardId)` — succeeds (the eager batch does NOT use `_inFlightCardIds`)
3. Launches a duplicate `dispatchers.default` coroutine for a card already being computed

Two coroutines race to write the same card to `_cardDataMap` and `cardRenderCache`. Wasted CPU on every scroll during load.

**Issue 3: Null-sentinel + `isCardsLoading` boolean in `AeonCardList`**

`AeonCardList.kt:156`:
```kotlin
null -> if (isCardsLoading) CardShimmerPlaceholder() else Box(modifier = Modifier.size(0.dp))
```
This invisible-box path is logically unreachable today (all cards complete before `_isCardsComputing` flips false), but it is a fragile regression trap. The two-variable encoding is the wrong abstraction: `null` + `Boolean` cannot cleanly express per-card state.

---

### What is wrong — Analytics

**Issue 1: All card computation runs on the Main thread**

`AnalyticsViewModel.kt:153-184`:
```kotlin
val cardDataMap: StateFlow<Map<String, CardRenderData>> =
    combine(cardsConfig, uiState, _screenFilter, commonFilterEnabled) { ... ->
        computeCardUseCase.computeAll(cards.cards, input)   // ← suspends here
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
```
`stateIn(viewModelScope)` collects on `Dispatchers.Main.immediate`. The `combine` transform block inherits Main. `computeCardUseCase.computeAll()` uses `coroutineScope { configs.map { async { registry.compute(...) } } }` — `async { }` without an explicit dispatcher inherits the calling coroutine's context, which is **Main**. CPU-intensive card computation (transaction aggregation, chart data) runs on the UI thread.

`DispatcherProvider` is not injected into `AnalyticsViewModel`. Dashboard injects it and uses `launch(dispatchers.default)` explicitly.

**Issue 2: No per-card loading state — all-or-nothing emission**

The `combine` transform suspends until `computeAll` returns. The `cardDataMap` StateFlow holds its previous value (`emptyMap()`) during the entire computation round. There is no intermediate state. Users see nothing until the **slowest card** finishes.

**Issue 3: Dual compute paths — ViewModel AND composable both compute simultaneously**

`AnalyticsScreen.kt:169-189` calls `AeonCardList` with:
- `cardDataMap = cardDataMap` — empty during computation
- No `isCardsLoading` → defaults to `false`

Inside `AeonCardList`, per card:
- `preComputedData = cardDataMap[cardId]` = null (map is empty)
- `serverDriven = isCardsLoading` = false

`rememberCardWithCallbacks` then takes the async path: `LaunchedEffect(config, input)` → `registry.compute()` on **Main**.

Result: while `computeAll` is running on Main in the ViewModel, `LaunchedEffect` per card is ALSO running on Main in the composable. **Both compute paths run simultaneously for every card, on the UI thread.**

**Issue 4: `CardInput` constructed in Screen composable with different data than ViewModel uses**

ViewModel `cardDataMap` uses `activeTransactions` filtered to non-archived accounts:
```kotlin
val activeAccountIds = state.accounts.filter { !it.isArchived }.map { it.id }.toSet()
val activeTransactions = state.allTransactions.filter { it.accountId in activeAccountIds }
```

AnalyticsScreen constructs its own `CardInput` (`AnalyticsScreen.kt:172-189`) using `state.allTransactions` — **unfiltered**. This is the input the async composable path uses, producing results that differ from what `computeAll` returns. The ViewModel result "wins" eventually when `cardDataMap` emits, but during the race, the composable may briefly render cards with wrong (unfiltered) data.

**Issue 5: `CategoryHierarchyCache.build()` on Main thread**

Inside `computeAll`, `CategoryHierarchyCache.build(input.categories)` runs on Main (inherits caller context). O(N²) worst case for N categories.

---

### What is wrong — Account Detail

**Issue 1: Invisible cards while loading**

`AccountDetailScreen.kt:625-651` calls `AeonCardList` with no `cardDataMap` (defaults to `emptyMap()`) and no `isCardsLoading` (defaults to `false`).

Per card: `preComputedData = null`, `serverDriven = false` → async path → `renderDataState = null` → `rememberCardWithCallbacks` returns null → `null -> Box(Modifier.size(0.dp))`. **Every card is an invisible zero-size box until its LaunchedEffect completes.**

**Issue 2: Card computation on Main thread**

`rememberCardWithCallbacks` async path: `LaunchedEffect(config, input)` runs on Main. `registry.compute()` — CPU-intensive — executes on the UI thread.

**Issue 3: `CardInput` constructed in Screen composable**

`AccountDetailScreen.kt:604-620` constructs `CardInput` inside the composable. `categoryHierarchy` defaults to `CategoryHierarchyCache.Empty` — the AccountDetail cards do not have access to the hierarchy tree. This affects category-based cards on AccountDetail.

---

## Architecture Principle (Why This Matters)

Cards are independent instruments. Each card slot should always occupy layout space with a visible state:
- **Before data is ready:** shimmer placeholder
- **After data is ready:** rendered card
- **On error:** error state

The correct model is: one card slot = one `CardComputeState` (Loading | Success | Error).
The current null-sentinel + boolean pattern is two variables encoding three states — it cannot be made correct without replacement.

---

## Acceptance Criteria

**AC1 — `CardComputeState` sealed class:**
Given the domain layer,
when `CardComputeState.kt` is created in `domain/card/`,
then it has exactly three variants: `data object Loading`, `data class Success(val data: CardRenderData)`, `data class Error(val message: String)`,
and it is annotated `@Immutable` (no `android.*` or `androidx.*` imports — domain layer only).

**AC2 — Dashboard: `PersistentMap.put()` replaces O(N) copy:**
Given `DashboardViewModel._cardDataMap`,
when a card computation completes,
then `_cardDataMap` is declared as `MutableStateFlow<PersistentMap<String, CardComputeState>>`,
and updates use `_cardDataMap.update { it.put(cardId, state) }` (O(log₃₂N) structural sharing),
and the `(it + pair).toImmutableMap()` pattern is gone.

**AC3 — Dashboard: full-Loading map at round start:**
Given a new computation round begins,
when `_cardDataMap` is reset,
then it is pre-populated with `CardComputeState.Loading` for every configured card ID — not an empty map,
so every slot is visible as a shimmer from the first frame.

**AC4 — Dashboard `DashboardUiState` updated:**
Given `DashboardUiState.Success`,
when this story is complete,
then `cardDataMap` is typed `ImmutableMap<String, CardComputeState>`,
and `isCardsComputing: Boolean` is removed,
and `_isCardsComputing: MutableStateFlow<Boolean>` and `completedCount: AtomicInteger` are removed from `DashboardViewModel`,
and the inner `combine(_cardDataMap, _isCardsComputing)` is replaced with a direct `_cardDataMap.collect { send(successState.copy(cardDataMap = it)) }`.

**AC5 — `AeonCardList`: `CardComputeState` replaces null-sentinel + boolean:**
Given `AeonCardList` currently takes `cardDataMap: Map<String, CardRenderData>` + `isCardsLoading: Boolean`,
when the signature is updated,
then `cardDataMap: ImmutableMap<String, CardComputeState>` replaces both,
and the per-card rendering uses:
```kotlin
when (val state = cardDataMap[cardConfig.cardId]) {
    is CardComputeState.Loading, null -> CardShimmerPlaceholder()
    is CardComputeState.Success      -> // pass state.data to rememberCardWithCallbacks
    is CardComputeState.Error        -> CardErrorState(state.message)
}
```
and `Box(Modifier.size(0.dp))` is gone,
and `serverDriven` parameter is removed from `rememberCardWithCallbacks` — the async compute path is always active for cards whose `preComputedData` is null.

**AC6 — Remove `CardBecameVisible` dead machinery from Dashboard:**
Given all cards are already eagerly computed in parallel,
when this story is complete,
then `DashboardIntent.CardBecameVisible` is removed,
and `computeCardIfNeeded` is removed,
and `_inFlightCardIds` is removed,
and `LaunchedEffect(glanceListState.firstVisibleItemIndex)` is removed from `DashboardScreen`.

**AC7 — Analytics: per-card streaming on `dispatchers.default`:**
Given `AnalyticsViewModel` currently computes all cards on Main via `combine { computeAll(...) }`,
when this story is complete,
then `DispatcherProvider` is injected into `AnalyticsViewModel`,
and `cardDataMap` is typed `StateFlow<ImmutableMap<String, CardComputeState>>`,
and on any input change (cardsConfig, uiState, screenFilter, commonFilterEnabled): an all-Loading map is emitted immediately,
and `CategoryHierarchyCache.build()` runs on `dispatchers.default` once per round before card coroutines launch,
and each card is launched as `launch(dispatchers.default)` updating the map independently on completion,
and Analytics does NOT write to `CardRenderCache` (filter state is not encoded in the cache key),
and the `computeAll` call is removed from `AnalyticsViewModel`,
and `CardInput` construction (with the correct `activeTransactions` filtering) moves from `AnalyticsScreen` into the ViewModel compute flow — the Screen passes a `CardInput` to `AeonCardList` for UI callbacks only (filter display), not for computation.

**AC8 — Account Detail: ViewModel-backed card computation on `dispatchers.default`:**
Given AccountDetail currently has no ViewModel-backed card state and computes on Main in the composable,
when this story is complete,
then `DispatcherProvider` is injected into `AccountDetailViewModel`,
and `CardInput` is constructed in `AccountDetailViewModel` (not in the Screen composable), including `categoryHierarchy = CategoryHierarchyCache.build(categories)` run on `dispatchers.default`,
and `AccountDetailViewModel` exposes `val cardDataMap: StateFlow<ImmutableMap<String, CardComputeState>>`,
and card computation uses `launch(dispatchers.default)` per card,
and `AccountDetailScreen` passes `cardDataMap` to `AeonCardList` — cards show shimmer then data, never invisible boxes.

**AC9 — All `AeonCardList` callers updated:**
Given the `AeonCardList` signature changed in AC5,
when all call sites are updated,
then Dashboard, Analytics, and AccountDetail all pass `ImmutableMap<String, CardComputeState>`,
and no caller passes `isCardsLoading`.

**AC10 — No regressions:**
Given existing tests,
when `./gradlew testDebugUnitTest` runs,
then all tests pass — update (not delete) any tests asserting on `isCardsComputing`, `isCardsLoading`, or the old `cardDataMap` type.

---

## Tasks / Subtasks

### Phase 1 — Foundation (ship as one PR; AeonCardList signature change cascades to all callers)

- [x] **Task 1 — Create `CardComputeState` (AC: 1)**
  - [x] 1.1 Create `domain/card/CardComputeState.kt`
  - [x] 1.2 `@Immutable sealed class CardComputeState { data object Loading; data class Success(val data: CardRenderData); data class Error(val message: String) }`
  - [x] 1.3 No `android.*` or `androidx.*` imports

- [x] **Task 2 — Update `DashboardViewModel` (AC: 2, 3, 4)**
  - [x] 2.1 Change `_cardDataMap` declaration:
    ```kotlin
    // Before:
    private val _cardDataMap = MutableStateFlow<ImmutableMap<String, CardRenderData>>(persistentMapOf())
    // After:
    private val _cardDataMap = MutableStateFlow<PersistentMap<String, CardComputeState>>(persistentMapOf())
    ```
  - [x] 2.2 Remove `_isCardsComputing: MutableStateFlow<Boolean>` field
  - [x] 2.3 Remove `completedCount: AtomicInteger`
  - [x] 2.4 At round start, emit a full-Loading map:
    ```kotlin
    _cardDataMap.value = configs.associate { it.cardId to CardComputeState.Loading }
                                .toPersistentMap()
    ```
    Remove the old `_cardDataMap.value = persistentMapOf()` and `_isCardsComputing.value = configs.isNotEmpty()` lines.
  - [x] 2.5 Each card coroutine — use `.put()` for O(log N) update:
    ```kotlin
    configs.forEach { cardConfig ->
        launch(dispatchers.default) {
            val cardInput = successState.cardInput ?: return@launch
            val state = runCatching {
                cardComputerRegistry.compute(cardConfig.cardType, cardInput, cardConfig)
            }.fold(
                onSuccess = { CardComputeState.Success(it) },
                onFailure = { CardComputeState.Error(it.message ?: "Compute error") }
            )
            if (state is CardComputeState.Success) {
                cardRenderCache[successState.profileId, successState.baseCurrency, cardConfig.cardId] = state.data
            }
            _cardDataMap.update { it.put(cardConfig.cardId, state) }
        }
    }
    ```
  - [x] 2.6 Remove `if (configs.isEmpty()) { _isCardsComputing.value = false }` branch — empty config means the Loading map is immediately the final state (no cards to compute)
  - [x] 2.7 Simplify inner collect: replace `combine(_cardDataMap, _isCardsComputing) { ... }` with `_cardDataMap.collect { send(successState.copy(cardDataMap = it)) }`
  - [x] 2.8 Update `DashboardUiState.Success`:
    - Change `cardDataMap: ImmutableMap<String, CardRenderData>` → `ImmutableMap<String, CardComputeState>`
    - Remove `isCardsComputing: Boolean`
  - [x] 2.9 Update initial `DashboardUiState.Success` construction in the `combine` block (line ~452): set `cardDataMap = persistentMapOf()` (the real Loading map is set inside `channelFlow`; this initial value is immediately overwritten)

- [x] **Task 3 — Update `AeonCardList` (AC: 5)**
  - [x] 3.1 Change signature: `cardDataMap: ImmutableMap<String, CardComputeState>` replaces `cardDataMap: Map<String, CardRenderData>` + `isCardsLoading: Boolean`
  - [x] 3.2 Update `cardDataMapState = rememberUpdatedState(cardDataMap)` — type flows through automatically
  - [x] 3.3 Update `preComputedData derivedStateOf` and the `when` block:
    ```kotlin
    when (val cardState = cardDataMapState.value[cardConfig.cardId]) {
        is CardComputeState.Loading, null -> CardShimmerPlaceholder()
        is CardComputeState.Error -> CardErrorState(cardState.message)
        is CardComputeState.Success -> {
            val cardWithCallbacks = rememberCardWithCallbacks(
                ...
                preComputedData = cardState.data,
                ...
            )
            when (val card = cardWithCallbacks) {
                is CardWithCallbacks.ComputeError -> CardErrorState(card.message)
                null -> CardShimmerPlaceholder()  // still computing via async path (Analytics/AccountDetail before migration)
                else -> { /* render */ }
            }
        }
    }
    ```
  - [x] 3.4 Remove `isCardsLoading: Boolean` parameter entirely
  - [x] 3.5 Remove `Box(modifier = Modifier.size(0.dp))` — replace any remaining `null` return with `CardShimmerPlaceholder()`

- [x] **Task 4 — Update `CardUiBridge.rememberCardWithCallbacks` (AC: 5)**
  - [x] 4.1 Remove `serverDriven: Boolean` parameter — the async `LaunchedEffect(config, input)` path now fires whenever `preComputedData == null`, for all screens
  - [x] 4.2 The async path still serves screens (Analytics Screen CardInput for filter/callback display) where the ViewModel pushes `CardComputeState.Success` data via `cardDataMap`, so the card will almost immediately switch to the sync path once the ViewModel completes; the async path provides a fallback only

- [x] **Task 5 — Remove `CardBecameVisible` machinery (AC: 6)**
  - [x] 5.1 Remove `DashboardIntent.CardBecameVisible` from the sealed class
  - [x] 5.2 Remove `is DashboardIntent.CardBecameVisible -> computeCardIfNeeded(intent.cardType)` from `onIntent`
  - [x] 5.3 Remove `computeCardIfNeeded` private function
  - [x] 5.4 Remove `_inFlightCardIds` field
  - [x] 5.5 Remove `LaunchedEffect(glanceListState.firstVisibleItemIndex)` block from `DashboardScreen`

- [x] **Task 6 — Update all `AeonCardList` callers to new signature (AC: 9)**
  - [x] 6.1 `grep -r "AeonCardList\|isCardsLoading\|isCardsComputing" app/src --include="*.kt"` — list all call sites
  - [x] 6.2 DashboardScreen: pass `state.cardDataMap` (now `ImmutableMap<String, CardComputeState>`), remove `isCardsLoading = state.isCardsComputing`
  - [x] 6.3 AnalyticsScreen: pass `cardDataMap` from `viewModel.cardDataMap` (updated type in Task 7); remove any `isCardsLoading`
  - [x] 6.4 AccountDetailScreen: pass `cardDataMap` from `accountDetailViewModel.cardDataMap` (Task 8); remove `CardInput` construction from the Screen for computation
  - [x] 6.5 Update preview stubs in `DashboardScreen.kt:1126` and `1257` — wrap `CardRenderData` values in `CardComputeState.Success(...)`

### Phase 2 — Analytics (can ship after Phase 1)

- [x] **Task 7 — Refactor `AnalyticsViewModel` card computation (AC: 7)**
  - [x] 7.1 Add `private val dispatchers: DispatcherProvider` to constructor and Hilt injection
  - [x] 7.2 Change `cardDataMap` to `MutableStateFlow<PersistentMap<String, CardComputeState>>`:
    ```kotlin
    private val _cardDataMap = MutableStateFlow<PersistentMap<String, CardComputeState>>(persistentMapOf())
    val cardDataMap: StateFlow<ImmutableMap<String, CardComputeState>> =
        _cardDataMap.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentMapOf())
    ```
  - [x] 7.3 Replace the `combine { computeAll(...) }.stateIn(...)` with a `viewModelScope.launch` that observes the four input flows and re-runs whenever any changes. Pattern:
    ```kotlin
    viewModelScope.launch {
        combine(cardsConfig, uiState, _screenFilter, commonFilterEnabled) {
            cards, state, sf, commonEnabled -> /* tuple */ 
        }.collect { (cards, state, sf, commonEnabled) ->
            if (state !is AnalyticsUiState.Success) {
                _cardDataMap.value = persistentMapOf()
                return@collect
            }
            // Emit all-Loading immediately
            _cardDataMap.value = cards.cards.associate { it.cardId to CardComputeState.Loading }
                                            .toPersistentMap()
            // Build inputs on Default — same filtering as current ViewModel (active accounts only)
            val enrichedInput = withContext(dispatchers.default) {
                val activeAccountIds = state.accounts.filter { !it.isArchived }
                                                     .map { it.id }.toSet()
                CardInput(
                    profileId = state.profileId,
                    transactions = state.allTransactions.filter { it.accountId in activeAccountIds },
                    allProfileTransactions = state.allTransactions,
                    accounts = state.accounts,
                    categories = state.allCategories,
                    tags = state.allTags,
                    exchangeRates = ExchangeRateSnapshot(state.selectedCurrency, state.exchangeRates),
                    targetCurrency = state.selectedCurrency,
                    plannedPayments = state.plannedPayments,
                    budgets = state.budgets,
                    loans = state.loans,
                    filter = if (commonEnabled) sf else TransactionFilter(timelineRange = sf.timelineRange.withExcludeFuture(sf.timelineRange.excludeFuture)),
                    screenContext = ScreenContext.ANALYTICS,
                    commonFilterEnabled = commonEnabled,
                    categoryHierarchy = CategoryHierarchyCache.build(state.allCategories)  // once per round, on Default
                )
            }
            // Launch each card in parallel on Default
            cards.cards.forEach { config ->
                launch(dispatchers.default) {
                    val cardState = runCatching {
                        cardComputerRegistry.compute(config.cardType, enrichedInput, config)
                    }.fold(
                        onSuccess = { CardComputeState.Success(it) },
                        onFailure = { CardComputeState.Error(it.message ?: "error") }
                    )
                    _cardDataMap.update { it.put(config.cardId, cardState) }
                }
            }
        }
    }
    ```
  - [x] 7.4 Inject `CardComputerRegistry` into `AnalyticsViewModel` (currently not injected — it borrows from `DashboardViewModel` via the Screen). Add to constructor and Hilt module.
  - [x] 7.5 `grep -r "computeAll" app/src --include="*.kt"` — if `AnalyticsViewModel` was the only caller, remove `ComputeCardUseCase.computeAll()`. If other callers exist, keep it.
  - [x] 7.6 Update `AnalyticsScreen`: the `cardInput` passed to `AeonCardList` is now for UI/callback purposes only (filter display, navigation). Keep it as a Screen-constructed `CardInput` for callbacks — it does not drive computation. Remove `cardComputerRegistry = vms.dashboardViewModel.cardComputerRegistry` from the Screen if no longer needed for computation (it will still be needed for the `AeonCardList` callback path — keep it).

### Phase 3 — Account Detail (can ship after Phase 1)

- [x] **Task 8 — Refactor `AccountDetailViewModel` card computation (AC: 8)**
  - [x] 8.1 Add `private val dispatchers: DispatcherProvider` and `private val cardComputerRegistry: CardComputerRegistry` to constructor and Hilt injection
  - [x] 8.2 Add:
    ```kotlin
    private val _cardDataMap = MutableStateFlow<PersistentMap<String, CardComputeState>>(persistentMapOf())
    val cardDataMap: StateFlow<ImmutableMap<String, CardComputeState>> =
        _cardDataMap.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentMapOf())
    ```
  - [x] 8.3 React to `uiState` and `cardsConfig` to launch card computation. Observe both in a `viewModelScope.launch { combine(uiState, cardsConfig) { ... }.collect { ... } }`:
    - When `AccountDetailUiState.Success`: emit all-Loading, then build `CardInput` on `dispatchers.default` (including `categoryHierarchy = CategoryHierarchyCache.build(categories)`), then launch each card as `launch(dispatchers.default)`
    - When not Success: `_cardDataMap.value = persistentMapOf()`
  - [x] 8.4 The `CardInput` for AccountDetail computation:
    ```kotlin
    CardInput(
        profileId = state.profileId,
        transactions = state.allAccountTransactions,
        allProfileTransactions = state.allProfileTransactions,
        accounts = listOf(state.account),
        categories = state.allCategories,
        tags = state.allTags,
        exchangeRates = ExchangeRateSnapshot(state.baseCurrency, state.exchangeRates),
        targetCurrency = state.account.currencyCode,  // account's own currency
        filter = state.currentFilter,
        screenContext = ScreenContext.ACCOUNT_DETAIL,
        categoryHierarchy = CategoryHierarchyCache.build(state.allCategories)
    )
    ```
  - [x] 8.5 AccountDetail does NOT use `CardRenderCache`
  - [x] 8.6 Update `AccountDetailScreen`: remove `CardInput` construction at lines 604-620. Pass `cardDataMap = accountDetailViewModel.cardDataMap` to `AeonCardList`. The Screen still needs a `CardInput` for callback context (filter sheets) — keep a simplified Screen-side `CardInput` for that purpose only, clearly commented.

- [x] **Task 9 — Update tests (AC: 10)**
  - [x] 9.1 `grep -r "isCardsComputing\|CardBecameVisible\|isCardsLoading\|CardRenderData" app/src/test --include="*.kt"`
  - [x] 9.2 Update `DashboardViewModelTest`: remove `isCardsComputing` assertions; assert `CardComputeState.Loading` at round start, `CardComputeState.Success` after computation
  - [x] 9.3 `./gradlew testDebugUnitTest` — all pass

### Review Findings (2026-06-28)

- [x] [Review][Decision] `CardComputeState` missing `@Immutable` — resolved: documented in class KDoc. Kotlin 2.0+ Compose Compiler Plugin infers sealed class stability automatically; `androidx.compose.runtime.Immutable` cannot be imported in `domain/`. A custom annotation has no compiler effect. If recomposition issues arise, use `composeCompiler { stabilityConfigurationFile }`.
- [x] [Review][Patch] `AccountDetailViewModel.cardDataMap` missing `.asStateFlow()` — fixed: added `asStateFlow()` and `import kotlinx.coroutines.flow.asStateFlow` [AccountDetailViewModel.kt]
- [x] [Review][Patch] `cardInput == null` leaves Dashboard card in `Loading` forever — fixed: emits `CardComputeState.Error("Card input unavailable")` per affected card [DashboardViewModel.kt]
- [x] [Review][Patch] Stale card jobs may write to `_cardDataMap` after new-round reset — fixed: replaced `coroutineScope { forEach { launch } }` with `forEach { viewModelScope.launch }` + `AtomicInteger` generation counter in both AnalyticsViewModel and AccountDetailViewModel. Also resolves the `coroutineScope` blocking-cancellation deferred item. [AnalyticsViewModel.kt + AccountDetailViewModel.kt]
- [x] [Review][Defer] `coroutineScope {}` inside `collectLatest` blocks rapid cancellation — resolved as part of the stale-jobs patch above: replaced with `viewModelScope.launch` per card + generation counter
- [x] [Review][Defer] `fold(persistentMapOf()) { acc, c -> acc.put(...) }` creates N intermediate PersistentMap snapshots — fixed: replaced with `.builder().apply { forEach { put(...) } }.build()` in all three ViewModels

### Review Findings (2026-06-28 — pass 2, fresh adversarial review)

- [x] [Review][Patch] Dashboard: no generation guard in channelFlow — CPU-bound `compute()` in `launch(dispatchers.default)` children runs to completion after `flatMapLatest` cancels the `channelFlow`; fixed: added `_computeGeneration` field and `roundGen` guard before `_cardDataMap.update` [DashboardViewModel.kt:504]
- [x] [Review][Patch] Analytics/AccountDetail: Loading map set after `withContext` delay, not immediately — AC7/AC8 spec requires immediate shimmer on input change; fixed: moved Loading map assignment to before `withContext` so shimmers appear immediately on input change [AnalyticsViewModel.kt:207, AccountDetailViewModel.kt:264]
- [x] [Review][Defer→Fixed] Analytics/AccountDetail old compute jobs waste CPU after `collectLatest` restarts — fixed: added `_currentCardComputeJob` parent `Job`; cancelled at the start of each new round, cascading cancellation to all child card coroutines via structured concurrency [AnalyticsViewModel.kt, AccountDetailViewModel.kt]
- [x] [Review][Defer→Dismissed] Cards absent from `cardDataMap` show infinite shimmer — dismissed: parent Job cancellation ensures every card either completes or is re-launched by the next round; `runCatching` catches `Throwable` so no compute error escapes; no actionable scenario remains [AeonCardList.kt:143]

---

## Dev Notes

### `PersistentMap` vs `ImmutableMap` — Why It Matters

`kotlinx.collections.immutable.PersistentMap` and `ImmutableMap` are different interfaces:
- `ImmutableMap` is a read-only view with no `put()` method
- `PersistentMap` extends `ImmutableMap` and adds `put(key, value): PersistentMap` which uses HAMT structural sharing (O(log₃₂N) new nodes, not O(N) copy)

The current code uses `ImmutableMap` for `_cardDataMap` and calls `(it + pair).toImmutableMap()` which goes through an intermediate `LinkedHashMap` — full O(N) copy per update.

The fix: declare as `PersistentMap<K, V>` and call `.put()`. Import `kotlinx.collections.immutable.PersistentMap` and `toPersistentMap()`.

`DashboardUiState.Success.cardDataMap` remains typed as `ImmutableMap` (the wider interface) — callers don't need `put()` on it. Only `_cardDataMap` (the mutable backing field) needs to be `PersistentMap`.

### CategoryHierarchyCache — Per-Screen Summary

| Screen | How hierarchy is built | Where | Status |
|--------|----------------------|-------|--------|
| Dashboard | `CategoryHierarchyCache.build(snapshot.categories)` in `structuralStateFlow.flowOn(dispatchers.default)`, stored as `StructuralState.hierarchy`, passed to `CardInput.categoryHierarchy` | ViewModel pipeline | ✅ Already correct |
| Analytics | Inside `computeAll()` — inherits Main dispatcher | ViewModel (broken) | ❌ Fix in Task 7 |
| AccountDetail | `CategoryHierarchyCache.Empty` (not built at all) | Composable `CardInput` default | ❌ Fix in Task 8 |

### Analytics `CardComputerRegistry` Injection

`AnalyticsScreen.kt:115`: `val cardComputerRegistry = vms.dashboardViewModel.cardComputerRegistry`

The Screen borrows the registry from DashboardViewModel because AnalyticsViewModel doesn't have it injected. After Task 7, `CardComputerRegistry` must be added to `AnalyticsViewModel`'s constructor. It is already a `@Singleton` registered in the Hilt graph (see `di/UseCaseModule.kt` or equivalent). No new module binding is needed — just add `private val cardComputerRegistry: CardComputerRegistry` as a constructor parameter.

The Screen still needs `cardComputerRegistry` for the `AeonCardList` callback path (`rememberCardWithCallbacks`). Keep the Screen's access to it via `vms.dashboardViewModel` or inject separately.

### Analytics `CardInput` — Two Separate Concerns

After Task 7, Analytics has two distinct `CardInput` usages:
1. **Computation `CardInput`** — built in ViewModel with `activeTransactions` (non-archived only), `categoryHierarchy` built on `dispatchers.default`. Used by `cardComputerRegistry.compute()`. Never visible to the Screen.
2. **Callback `CardInput`** — built in `AnalyticsScreen` composable. Used by `rememberCardWithCallbacks` for filter sheet display, navigation callbacks, and `screenOwnsTimeline`. This may differ slightly (unfiltered transactions is acceptable here since it's for display/filtering only, not chart data).

Do not conflate them. The Screen's `CardInput` remains in the Screen.

### Phase Shipping Order

- **Phase 1 (Tasks 1–6):** One PR. `AeonCardList` signature change cascades to all callers — must be atomic.
- **Phase 2 (Task 7):** Separate PR after Phase 1. Analytics can ship independently.
- **Phase 3 (Task 8):** Separate PR after Phase 1. AccountDetail can ship independently.
- **Task 9:** Tests should be updated alongside whichever phase they belong to, not deferred.

### Preserved Behaviours

| File | What Must Be Preserved |
|------|------------------------|
| `DashboardViewModel.kt` | `flatMapLatest` on profileId; ephemeral state merged OUTSIDE core pipeline; `CardRenderCache` writes on `CardComputeState.Success`; `invalidateCard` on exchange-rate update; `cardInput` built in ViewModel pipeline (not Screen) |
| `AeonCardList.kt` | `key = { card -> card.cardId }` and `contentType` on LazyColumn items; drag reorder; `derivedStateOf` per card (reads individual map slot) |
| `AnalyticsViewModel.kt` | `_screenFilter` flow; `commonFilterEnabled` from `settingsRepository`; `toggleCommonFilter` calls `cardRepository.resetAllCardFilters`; active-accounts-only filter for transactions |
| `AccountDetailViewModel.kt` | Account lock enforced in `updateFilter`: `newFilter.copy(accountIds = setOf(accountId))`; `flatMapLatest` on profileId; `flowOn(Dispatchers.Default)` on history pipeline |
| `CardRenderCache.kt` | `invalidateCard`; `getProfileSnapshot` (kept, just not used at round start); `profileIndex` secondary map |

### Source References

- `DashboardViewModel.kt:510` — `(it + pair).toImmutableMap()`: O(N) copy confirmed
- `DashboardViewModel.kt:499-515` — eager parallel launch, no `take(4)`; all cards launched
- `DashboardViewModel.kt:371,372` — `CategoryHierarchyCache.build()` on `.flowOn(dispatchers.default)` ✅
- `DashboardViewModel.kt:152,156` — `_isCardsComputing`, `_inFlightCardIds`: to remove
- `AeonCardList.kt:76-77` — `cardDataMap`, `isCardsLoading` parameters: to replace
- `AeonCardList.kt:156` — `Box(Modifier.size(0.dp))`: to remove
- `AnalyticsViewModel.kt:153-184` — `combine { computeAll(...) }.stateIn(viewModelScope, ...)`: Main thread
- `AnalyticsScreen.kt:115` — `cardComputerRegistry` borrowed from DashboardViewModel
- `AnalyticsScreen.kt:172-189` — `CardInput` built in Screen with unfiltered transactions
- `AccountDetailScreen.kt:604-620` — `CardInput` built in Screen composable
- `AccountDetailScreen.kt:625-651` — `AeonCardList` called with no `cardDataMap`, no `isCardsLoading`
- `ComputeCardUseCase.kt:24` — `async { }` without dispatcher: inherits caller (Main in Analytics)
- `CardUiBridge.kt:67-80` — async path via `LaunchedEffect(config, input)` on Main

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `CardComputeState` has no `@Immutable` annotation — domain layer prohibits `androidx.*` imports per CLAUDE.md. Kotlin 2.0+ Compose Compiler Plugin infers sealed class stability automatically (all subtypes are data object / data class with stable fields). Rationale documented in class KDoc. If recomposition issues are observed, use `composeCompiler { stabilityConfigurationFile }`.
- `PersistentMap.put()` shows a deprecation warning (use `putting()` instead) for the version in use. Changed calls work correctly; migration to `putting()` deferred to avoid noise.
- AnalyticsViewModel and AccountDetailViewModel: replaced `coroutineScope { forEach { launch(dispatchers.default) } }` with `forEach { viewModelScope.launch(dispatchers.default) }` + `AtomicInteger` generation counter. This allows `collectLatest` to respond to new input without waiting for in-flight computes to finish, and prevents stale writes via the generation check.
- Loading map construction uses `.builder().apply { }.build()` (single-pass, no intermediate PersistentMap snapshots) instead of `fold { acc.put(...) }` (N intermediate snapshots).
- AccountDetailViewModel and AnalyticsViewModel: `CategoryHierarchyCache.build()` runs inside `withContext(dispatchers.default)` before launching card coroutines — once per round on the default dispatcher.
- AccountDetailScreen: `DynamicAccountDetailCards` received `cardDataMap` as a parameter with a default of `persistentMapOf()` to keep the signature backward-compatible with preview composables.
- All tests pass; no `isCardsComputing`/`CardBecameVisible` assertions existed in test files.

### File List

- `app/src/main/java/com/osscabinet/ossWallet/domain/card/CardComputeState.kt` (NEW)
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardViewModel.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardIntent.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/AeonCardList.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/CardUiBridge.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/analytics/AnalyticsViewModel.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/accounts/detail/AccountDetailViewModel.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/accounts/detail/AccountDetailScreen.kt`
