# Deferred Work

## Deferred from: code review (2026-06-27)
- AeonExpressiveCard layout shift tuning [AeonExpressiveCard.kt] — isFrontVisible threshold at 90f might cause a layout jump during flips if front/back content heights differ significantly.

## Deferred from: code review of 2-2 and 2-3 (2026-06-27)
- `quickCloneTransaction`/`quickDeleteTransaction` no existence check [DashboardViewModel.kt:185-200] — pre-existing pattern; silent no-op or repository error when id is stale
- `addTag` no duplicate name check [DashboardViewModel.kt:179] — pre-existing domain concern; silently inserts duplicate tags
- Idempotency check in `quickConfirmById` only scans `DASHBOARD_TRANSACTION_FEED_LIMIT=100` transactions [DashboardViewModel.kt:516] — existing transactions beyond feed limit not checked; may allow duplicate confirmations

## Deferred from: code review of 2-4 (2026-06-28) — RESOLVED
- ~~`coroutineScope {}` inside `collectLatest` blocks rapid cancellation~~ — FIXED in pass 2: replaced with parent `Job` pattern; old jobs now cancelled on new round [AnalyticsViewModel.kt, AccountDetailViewModel.kt]
- ~~`fold(persistentMapOf()) { acc, c -> acc.put(...) }` creates N intermediate PersistentMap snapshots~~ — FIXED in pass 1: replaced with `.builder().apply { }.build()` [AnalyticsViewModel.kt + AccountDetailViewModel.kt + DashboardViewModel.kt]

## Deferred from: code review of 2-4 (2026-06-28 — pass 2) — RESOLVED
- ~~Analytics/AccountDetail old compute jobs waste CPU after `collectLatest` restarts~~ — FIXED: added `_currentCardComputeJob` parent Job; cancelled on each new round, cascading to all child card coroutines [AnalyticsViewModel.kt, AccountDetailViewModel.kt]
- ~~Cards absent from `cardDataMap` show infinite shimmer~~ — DISMISSED: parent Job fix makes this scenario unreachable; `runCatching` covers all error paths [AeonCardList.kt:143]

## Deferred from: code review of 2-2 and 2-3 (2026-06-28)
- `QuickCloneTransaction` intent declared but unreachable from Screen — D1 navigation-action decision artifact; clean up in future sprint
- `cardInput: CardInput?` in `DashboardUiState.Success` exposes domain object to UI state — pre-existing design concern
- `searchTransactions()` called directly from Screen without `onIntent` — Flow-only, no state mutation; minor AC deviation
- Orphaned compute coroutines can write `_cardDataMap` after `flatMapLatest` cancels old scope — cooperative cancellation window; stale flash is brief and self-correcting
- No undo path for `quickDeleteTransaction` / `quickCloneTransaction` — pre-existing pattern across codebase
- Search query whitespace not trimmed before repository call [DashboardViewModel.kt:729]
- Uncaught exceptions in `changeTransactionStatus`, `quickDeleteTransaction`, `quickCloneTransaction`, `addTag` propagate silently — pre-existing pattern

## Deferred from: code review of 2-4 (2026-06-28 — pass 3)
- `coroutineScope` blocks `collectLatest` re-entry in Analytics/AccountDetail — generation counter prevents stale writes but compute runs to completion; `viewModelScope.launch` per card would fix; documented tradeoff [AnalyticsViewModel.kt, AccountDetailViewModel.kt]
- All cards shimmer-reset on any upstream re-emission — `collectLatest` unconditionally resets to all-Loading; delta-check would avoid flicker; intentional correctness-over-perf choice [AnalyticsViewModel.kt, AccountDetailViewModel.kt]
- `navigationStack` in category chart cards not reset when compute data changes — stale drill-down after recompute; `remember(data)` key would fix [CategoryPieChartCard.kt, CategoryTreemapCard.kt]
- Nested `combine` in `AnalyticsViewModel` allocates intermediate `Triple` — use 4-arg `combine` instead [AnalyticsViewModel.kt]
- `mapCategoryPieChart`/`mapCategoryTreemap` silently discard `onUpdate` on wrong config type — pre-existing guard pattern [CardUiBridge.kt]
- `LaunchedEffect(filteredData)` in category chart cards causes one-frame stale data on recompose — pre-existing [CategoryPieChartCard.kt, CategoryTreemapCard.kt]

## Deferred from: code review of EntryEditorialInput / TemplateFormBody / TransactionFormBody (2026-06-30)

- `onRemoveLink` fires with `-1L` parent ID for unsaved records [TransactionFormBody.kt:222, TemplateFormBody.kt:188] — handler ignores the parent ID today; guard with `state.transactionId?.let { ... }` if handler ever writes to DB
- `bringIntoViewRequester` uses zero-width magic-constant rect `Rect(0,0,0,1000f)` [EntryEditorialInput.kt:287] — intentional workaround; proper fix requires layout-coordinate plumbing
- `remember(value, suggestions)` key uses list reference — callers passing new list instance per recompose defeat memoization [EntryEditorialInput.kt:119] — callers must stabilize their suggestion lists
- `statusBarsPadding()` on root Box may double-apply status bar inset if caller passes Scaffold's `innerPadding` [TemplateFormBody.kt, TransactionFormBody.kt] — needs caller-side audit
- `borderWidth` `animateDpAsState` both branches return the same value — dead animation [EntryEditorialInput.kt] — design intent unclear (highlighted state may have been meant to use wider border)
- `targetCurrency ?: fallback` may be unreachable if `targetCurrency` is non-nullable `String` [TemplateFormBody.kt:102, TransactionFormBody.kt:105] — verify state types before patching

## Deferred from: code review of 7-1-suppress-duplicate-numpad-planned-payment (2026-06-29)

- `PlannedPaymentEntryViewModel.initialize()` idempotency guards (`lastInitParams` checks) are now dead code after `LaunchedEffect(Unit)` change — safe but misleading. Consider removing or replacing with an assertion. [PlannedPaymentEntryViewModel.kt:107-108]

## Deferred from: code review of Quick Add Refactor (2026-07-02)
- "EXTERNAL" Label Naming [QuickAddAmountStep.kt:423] — functional but string can be refined to better status indicator.
- Transfer Rate Fallback [TransactionEntryService.kt:179] — returning `BigDecimal.ONE` for zero-input rates is an arbitrary fallback but prevents crashes.
- Inconsistent Corner Radius [QuickAddAtelier.kt] — tiles use 10dp, panel uses 20dp. Intentional design variation for now.

## Deferred from: meta-review of fix commits C9–C16 / M1–M11 / L1–L5 (2026-07-04)

### Patches — RESOLVED
- ~~`profileId = 1` hardcoded in planned-payment prefill~~ — FIXED: `setPlannedPaymentPrefillFromPattern` now launches coroutine, reads `settingsRepository.currentProfileId.first()` [TransactionViewModel.kt]
- ~~Stale `plannedPaymentPrefill` triggers navigation with wrong pattern~~ — FIXED: prefill cleared to null before early-return; SmartSuggestionsScreen navigates reactively via `LaunchedEffect(plannedPaymentPrefill)` [TransactionViewModel.kt, SmartSuggestionsScreen.kt]
- ~~Warranty date picker defaults to today when `warrantyDateMillis = null`~~ — FIXED: default is now `currentTimeMillis + 365 days` [EntrySheetOrchestrator.kt]
- ~~6-flow `combine` exceeds typed-lambda API limit~~ — FIXED: rewritten as nested 4+2 combines using `Triple` intermediary [TransactionUseCases.kt]
- ~~`activeAmountSide = TARGET` persists after TRANSFER→EXPENSE mode switch~~ — FIXED: `applyFocusGuard` now resets `activeAmountSide = SOURCE` for all non-TRANSFER modes [QuickAddViewModel.kt]
- `AeonDateTimePicker` change desynchronizes linked transfer-leg timestamps — DISMISSED: entry screen saves both legs with the same `selectedDateMillis`; 5-second pairing in QuickAdd is for pre-fill discovery only, not for the financial link

### Deferred
- `onExpandedChange = { if (!it) expanded = false }` blocks dropdown re-open after focus+dismiss [EntryEditorialInput.kt:167] — intentional M7 fix trade-off; expansion via LaunchedEffect on refocus still works; acceptable for autocomplete
- `accountId = ""` fallback for null transfer-leg accountId [QuickAddViewModel.kt:238–239] — pre-existing blank-string convention; `resolvePrefill` handles via `takeIf { isNotBlank() }`; other `!= null`-only checks are pre-existing risk
- `reset()` causes transient blank account/currency window [QuickAddViewModel.kt:807] — intentional simplification; repository combine re-populates within frames on all normal devices
- `delay(1500)` inside `withContext(NonCancellable)` can't be cancelled [SettingsViewModel.kt:497–500, 514–517] — theoretical only; path is user-triggered backup/clear; acceptable for the use case
