# Deferred Work

## Deferred from: code review of story 1-1-core-infrastructure-baseline-serialization-concurrency (2026-06-26)

- Network DTO exchange rates use `Double` (precision loss for financial values) — `data/remote/ExchangeRateApi.kt`, `data/remote/FrankfurterApi.kt`. Pre-existing; types unchanged by story 1.1 (only Moshi→kotlinx annotations migrated). Consider `BigDecimal` (contextual serializer already registered in `networkJson`) if these rates ever feed financial math directly.
- Duplicate `Serializers.kt` — `data/local/util/Serializers.kt` vs `domain/model/util/Serializers.kt`. Pre-existing; confirm whether the data-layer copy is redundant and consolidate.
- Disk path `com/osscabinet/ossWallet` vs git-tracked lowercase `com/osscabinet/osswallet`. Pre-existing repo-wide (612 files tracked lowercase, package `com.osscabinet.ossWallet`); builds pass on CI because Kotlin does not require dir==package. 3 stray files ARE tracked under capital `ossWallet` — worth a one-time repo-wide path normalization to remove the case ambiguity.
- AD-9 dispatcher injection is partial — `data/web/WebServerManager.kt:167`, `service/WebServerService.kt:31`, `widget/WidgetRefreshWorker.kt`, `widget/NetWorthWidgetProvider.kt`, `widget/RecentTransactionsWidgetProvider.kt`, `ui/components/entry/AeonLocationSelection.kt` still hardcode `Dispatchers.IO`. Deferred: **needs broader refactor** — widget/server/UI dispatcher injection touches Hilt entry points and WorkManager wiring; warrants its own story.
- `TimelineServiceTest` asserts group header title `"Today"` — could flake at a day boundary or under a runner in a different timezone. Pre-existing pattern (test #1 asserts the same). Consider injecting a fixed clock/reference time into `TimelineService` for deterministic tests.

## Deferred from: code review of 1-2-establish-pure-domain-module-core-domain (2026-06-27)

- `FilterMapper.kt` uses `filter.` on extension receiver — pre-existing, not introduced by module split.
- `ExchangeRateApi.kt` / `RetrofitClient.kt` have unstaged modifications not reviewed here — belong to a different story scope.
- `DispatcherProvider.kt` / `DispatchersModule.kt` staged additions not reviewed here — belong to Story 1.1 infrastructure scope.
- Magic `1000L` silent fallback on unparseable category ID in `CsvImportServiceImpl.kt:619` — pre-existing.
- `GroupHeaderData.title` / `subtitle` are locale-sensitive display strings living in a JVM-only module — pre-existing design decision; acceptable for now but worth revisiting if :core:domain needs full i18n.
- Duplicate filter-building logic between `TransactionQueryBuilder` and `TransactionRepositoryImpl` — pre-existing structural duplication.
- `AccountingPerspective.labelsFor()` `else` branch silently swallows future subtypes in `CardConfig.kt:413` — pre-existing; use exhaustive `when` in a future cleanup pass.
- `PlannedPaymentsCardConfig.daysAhead` is `@Transient` and not read by the computer — pre-existing dead API field.
- `SimpleDateFormat` (non-thread-safe, legacy) in `TimelineRange.toDisplayString()` — pre-existing.
All items above resolved in commit 81c0b0ff except the two below:

- **Duplicate filter-building logic** between `TransactionQueryBuilder` and `TransactionRepositoryImpl.buildFilteredQuery()` — the immediate bug (`status != 'DELETED'` missing from `buildFilteredQuery`) was fixed; full consolidation requires injecting `TransactionQueryBuilder` into `TransactionRepositoryImpl` and is a dedicated refactor task.
- **FilterMapper receiver naming**: `Filter.filter: TransactionFilter` property shares the name of the extension receiver — valid Kotlin but visually confusing; defer to a domain naming cleanup pass.
- `ExchangeRateApi.kt` / `RetrofitClient.kt` — confirmed clean (Story 1.1 migration); `DispatcherProvider.kt` / `DispatchersModule.kt` — confirmed committed in Story 1.1.
- Spec deviations (CardType, CardFilterRules, ExchangeRateSnapshot, GroupHeaderData moved to `:core:domain`) — necessary and architecturally correct; update epic/story spec template to anticipate transitive JVM-only dependencies.

## Deferred from: code review of 1-4-purify-aggregation-logic-domain-query-abstraction (2026-06-26)

- `computeLoansCard` — `transferIn` branch not exercised: comment in test claims "income + transferIn = 400" but only the income-only path is verified (`transferIn` defaults to 0). Falls under story 1.4 Coverage action item.
- `computeNetWorthCard` comparison test reuses same mock for current and previous periods — does not verify distinct time bounds were used per call. Falls under Coverage action item.
- `computeRecentActivitiesCard` stubs `getGroupedData` to return `emptyList()` — only proves plumbing; regression where grouped data is always ignored wouldn't be caught. Falls under Coverage action item.
- `computeRecentBalances` — no `result.size == 2` assertion; silent drop of a transaction would still pass. Falls under Coverage action item.
- `TimelineService @Before` defensive stub returns `Pair(null, null)` as default — future tests that forget to stub ranges get degenerate null input rather than fast failure. Pre-existing design decision.
- No `queryService` mock interaction `verify` calls beyond `computeRecentActivitiesCard` — tests check output shape but not architectural delegation. Falls under Coverage action item.
- `runBlocking` used for coroutine tests instead of `runTest` — pre-existing pattern in the file; new tests follow consistently. Consider migrating to `runTest` + `TestDispatcher` in a future coverage pass.
