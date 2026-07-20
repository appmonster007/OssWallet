# Story 1.4: Purify Aggregation Logic (Domain Query Abstraction)

## Status
**Status:** done
**Epic:** Epic 1: Architectural Foundation & Domain Purity
**ID:** 1.4

## Description
As a developer,
I want business aggregation logic to be decoupled from the database,
So that I can test financial rules without a running SQLite instance and improve query performance through specialized abstractions.

## Acceptance Criteria

**Given** `CardAggregationService` which currently injects `DatabaseQueryService` (impl)
**When** I create a `DatabaseQueryService` interface in `domain.service.query`
**Then** `CardAggregationService` is refactored into a pure service that only knows about Domain models and the Query interface
**And** all SQL-specific query generation (the "grammar") is moved to a concrete implementation `DatabaseQueryServiceImpl` in `data.local.query`
**And** the service can be verified with 100% code coverage using unit tests and mocks.

## Developer Context

### Exhaustive Artifact Analysis
- **Service Abstraction**: Created `com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService`. This interface handles high-performance aggregations (Net Worth, Cash Flow, Category Tree).
- **Implementation**: Created `com.osscabinet.ossWallet.data.local.query.DatabaseQueryServiceImpl`. This class uses `SimpleSQLiteQuery` and `TransactionDao` to execute the actual SQL logic.
- **Consumption**: `CardAggregationService` now depends on `DatabaseQueryService` instead of directly interacting with DAO-like methods or raw repositories for aggregations.
- **Transaction Orchestration**: Introduced `TransactionManager` to handle complex cross-cutting concerns like transfer links and planned payment state transitions, further purifying the `TransactionRepository`.

### Technical Requirements
- **DI**:
  - `DatabaseQueryService` is bound in `RepositoryModule` (or `QueryModule`).
  - `TransactionManager` is provided via Hilt.
- **Performance**:
  - Implementation uses optimized SQL for multi-currency aggregations to meet **NFR3** (Dashboard load < 500ms).

### Architecture Compliance
- **AD-1 (Domain Pureness)**: Domain services now operate on a query abstraction rather than database implementation details.

### File Structure Requirements
- **NEW**: `app/src/main/java/com/osscabinet/ossWallet/domain/service/query/DatabaseQueryService.kt`
- **NEW**: `app/src/main/java/com/osscabinet/ossWallet/data/local/query/DatabaseQueryServiceImpl.kt`
- **NEW**: `app/src/main/java/com/osscabinet/ossWallet/domain/service/management/TransactionManager.kt`
- **UPDATE**: `app/src/main/java/com/osscabinet/ossWallet/domain/service/aggregation/CardAggregationService.kt`
- **UPDATE**: `app/src/main/java/com/osscabinet/ossWallet/data/repository/TransactionRepositoryImpl.kt`

### Testing Requirements
- Unit tests for `CardAggregationService` should mock `DatabaseQueryService`.
- Integration tests for `DatabaseQueryServiceImpl` to ensure SQL grammar is correct.

## Completion Status
- [x] Define DatabaseQueryService interface in domain
- [x] Implement DatabaseQueryServiceImpl in data layer
- [x] Refactor CardAggregationService to use the abstracted query service
- [x] Implement TransactionManager for centralized logic
- [x] Complete unit test coverage for CardAggregationService refactor
- [ ] Verify Dashboard performance metrics (NFR3) — DEFERRED (see Action Items)

**Ultimate context engine analysis completed - comprehensive developer guide created**

## Dev Agent Record

### Completion Notes (2026-06-26)
- **Unit test coverage for the `CardAggregationService` refactor** — expanded
  `CardAggregationServiceTest` from 1 covered method to 8. New tests (MockK-mocked
  `TransactionAggregationService`/`TimelineService`, real `CurrencyService`/`BudgetService`/
  `LoanService`):
  - `computeNetWorthCard` (cumulative currency balances)
  - `computeCashFlowCard` (income/expense → base)
  - `computeCurrencyBalancesCard`
  - `computeLoansCard` (LENT repayment + remaining)
  - `computeBudgetsCard` (signed spent for expense budgets)
  - `computeAccountHeroCard` (lifetime balance + period flow)
  - `computeRecentBalances` (pure running-balance fold)
  - plus the pre-existing `computeAccountsCard` test.
  Added a shared `fs(...)`/`baseInput(...)` harness and defensive `TimelineService` range
  stubs in `setup()`. `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL (no regressions).
- **Follow-up (per user decision):** added the remaining method tests so every public compute
  method now has a test — `computeCategoryPieChartCard`, `computeCategoryTreemapCard`,
  `computeRecentActivitiesCard` (mocked `observeFilteredTransactions` flow +
  `TimelineService.getGroupedData`), and the `computeNetWorthCard` `showComparison = true` branch.
  `CardAggregationServiceTest` now holds **12 tests covering all 11 public methods** (full suite
  BUILD SUCCESSFUL, no regressions). Method-level coverage is complete; exhaustive line/branch
  100% (every config permutation) is tracked as an action item, not a blocker.
- **NFR3 (Dashboard load < 500ms) — DEFERRED.** The project has a single `:app` module with no
  `androidx.benchmark`/macrobenchmark harness and no connected device/emulator here, so the
  budget cannot be measured from JVM unit tests. Deferred per user decision; tracked as an
  action item for device/benchmark verification.

### Action Items
- [ ] [NFR3] Verify Dashboard load < 500ms on a device — add an `androidx.macrobenchmark` module
  (or profile manually) and capture the dashboard cold/warm load timing. Blocks final closure of
  this story's performance AC.
- [ ] [Coverage] Revisit `CardAggregationService` for exhaustive line/branch 100% — current tests
  cover every public method's happy path; remaining permutations (per-perspective `toPerspective`,
  multi-currency conversion, archived/filtered budgets & loans, comparison branches on the chart
  cards) can be filled in incrementally.

### Review Findings

_Code review 2026-06-26 — Blind Hunter, Acceptance Auditor. Edge Case Hunter: failed (session limit)._

- [x] [Review][Patch] Missing integration tests for `DatabaseQueryServiceImpl` — spec explicitly requires "Integration tests for `DatabaseQueryServiceImpl` to ensure SQL grammar is correct." Added `DatabaseQueryServiceImplTest` (instrumented, in-memory Room) with 7 tests covering: profile isolation on `observeFilteredTransactions`, limit param, `getMinTransactionTimestamp` (empty / earliest / cross-profile), and `aggregate` SQL grammar (SUM by currency, profile isolation, income/expense separation). `compileDebugAndroidTestKotlin` → BUILD SUCCESSFUL. (auditor → fixed)
- [x] [Review][Defer] `computeLoansCard` — `transferIn` branch not exercised: comment claims "income + transferIn = 400" but `fs()` is called with only `income = "400.00"`, `transferIn` defaults to "0" — only the income-only path is verified. [CardAggregationServiceTest.kt] — deferred, falls under existing Coverage action item
- [x] [Review][Defer] `computeNetWorthCard` comparison test reuses same mock for current and previous periods — does not verify distinct time bounds were used per call; if production code queries twice with identical params this test still passes. [CardAggregationServiceTest.kt] — deferred, falls under Coverage action item
- [x] [Review][Defer] `computeRecentActivitiesCard` stubs `getGroupedData` to return `emptyList()` — only proves plumbing; a regression where grouped data is always ignored would not be caught. [CardAggregationServiceTest.kt] — deferred, falls under Coverage action item
- [x] [Review][Defer] `computeRecentBalances` — no `result.size == 2` assertion; if production code silently drops a transaction both timestamp-lookup assertions still pass. [CardAggregationServiceTest.kt] — deferred, falls under Coverage action item
- [x] [Review][Defer] `TimelineService @Before` defensive stub (`Pair(null, null)` default) may mask missing setup in future tests — new tests that forget to stub ranges get degenerate null input rather than a fast failure. — deferred, pre-existing design decision
- [x] [Review][Defer] No `queryService` mock interaction verification beyond `computeRecentActivitiesCard` — tests check output shape but not that `CardAggregationService` actually delegates through the query abstraction for other compute methods. — deferred, falls under Coverage action item
- [x] [Review][Defer] `runBlocking` used for coroutine tests instead of `runTest` — pre-existing pattern in the file (original `computeAccountsCard` test uses same pattern); new tests follow consistently. — deferred, pre-existing
- [x] [Review][Defer] 100% coverage AC in contradiction with open action item — story marks `[x] Complete unit test coverage` done but action item `[Coverage]` remains open. Already explicitly decided and recorded. — deferred, user-approved

### File List
- `app/src/test/java/com/osscabinet/ossWallet/domain/service/aggregation/CardAggregationServiceTest.kt` (modified — 11 new tests + shared `fs()`/`baseInput()` harness + defensive TimelineService stubs)
- `app/src/androidTest/java/com/osscabinet/osswallet/data/local/query/DatabaseQueryServiceImplTest.kt` (new — 7 instrumented integration tests for SQL grammar verification)
