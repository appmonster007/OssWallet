# Deferred Work

## Deferred from: code review (2026-06-27)
- AeonExpressiveCard layout shift tuning [AeonExpressiveCard.kt] — isFrontVisible threshold at 90f might cause a layout jump during flips if front/back content heights differ significantly.

## Deferred from: code review of 2-2 and 2-3 (2026-06-27)
- `quickCloneTransaction`/`quickDeleteTransaction` no existence check [DashboardViewModel.kt:185-200] — pre-existing pattern; silent no-op or repository error when id is stale
- `addTag` no duplicate name check [DashboardViewModel.kt:179] — pre-existing domain concern; silently inserts duplicate tags
- Idempotency check in `quickConfirmById` only scans `DASHBOARD_TRANSACTION_FEED_LIMIT=100` transactions [DashboardViewModel.kt:516] — existing transactions beyond feed limit not checked; may allow duplicate confirmations

## Deferred from: code review of 2-2 and 2-3 (2026-06-28)
- `QuickCloneTransaction` intent declared but unreachable from Screen — D1 navigation-action decision artifact; clean up in future sprint
- `cardInput: CardInput?` in `DashboardUiState.Success` exposes domain object to UI state — pre-existing design concern
- `searchTransactions()` called directly from Screen without `onIntent` — Flow-only, no state mutation; minor AC deviation
- Orphaned compute coroutines can write `_cardDataMap` after `flatMapLatest` cancels old scope — cooperative cancellation window; stale flash is brief and self-correcting
- No undo path for `quickDeleteTransaction` / `quickCloneTransaction` — pre-existing pattern across codebase
- Search query whitespace not trimmed before repository call [DashboardViewModel.kt:729]
- Uncaught exceptions in `changeTransactionStatus`, `quickDeleteTransaction`, `quickCloneTransaction`, `addTag` propagate silently — pre-existing pattern
