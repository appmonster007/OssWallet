Dashboard Performance Audit & Fix Tracking

Branch: feature/tagv43 | Date: 2026-06-22 | Status: Planning

 ---
Root Cause Summary

The dashboard is laggy because three compounding failure modes cascade from DB to UI:

1. DB layer — No composite indices for the most common query patterns. Full table scans on every card compute. Per-bucket queries multiply DB calls 12–36× per card.
2. Computation layer — CardInput ships the entire database to every card computer as one 8–15 MB object. Several computers have O(n²) patterns. A single transaction change
   triggers all 15 cards to recompute in parallel.
3. UI layer — Compose's skip optimization is disabled on every card (unstable lambda types). The card map reference change causes all 15 cards to re-render when any single
   card updates. 13 independent state subscriptions cause screen-wide recomposition thrashing.

Net effect: insert one transaction → 15 CardInput rebuilds → 15 sets of DB calls (12–36 per card) → 15 rapid UI emissions → 15 full card re-renders → GPU thrashing.

 ---
Issue Registry

Each issue is numbered for tracking. Status: [ ] open · [x] fixed · [~] in progress.

 ---
LAYER 0: CRASH / DATA INTEGRITY

 ---
ISS-01 · INVALID (audit correction) · recentActivitiesBreakdown is NOT missing

- Severity: N/A — No crash. Initial audit was incorrect.
- Audit Note: The original audit claimed dashboardViewModel.recentActivitiesBreakdown was never declared. Code inspection of
  ui/util/BaseViewModel.kt line 20 shows the property IS defined there as StateFlow<Boolean>, and DashboardViewModel
  (ui/screens/dashboard/DashboardViewModel.kt line 89) extends BaseViewModel, so the property is accessible via inheritance.
  This issue does NOT exist and requires no fix.
- [x] Status: CLOSED — invalid finding

 ---
ISS-02 · SECURITY / DATA INTEGRITY · Cross-profile template data leak

- Severity: P0 — Returns data across all profiles with no profileId filter
- Files:
    - data/local/dao/TemplateDao.kt line 14 (getAllTemplates()), line 17 (getStarredTemplates())
- Root Cause: Both queries use SELECT * FROM templates with no WHERE profileId = :profileId clause. In a multi-profile setup, profile A sees profile B's templates.
- Fix:
  a. Add profileId: Long parameter to both DAO methods
  b. Add WHERE profileId = :profileId to both SQL strings
  c. Add composite index (profileId, isStarred) to TemplateEntity
  d. Update all call sites to pass the active profileId
- Acceptance: A template created in Profile A does NOT appear in Profile B's template list.
- Status

 ---
ISS-03 · SECURITY · TransactionDao.getTransactionsWithTag() has no profileId filter

- Severity: P1 — Cross-profile data visible in tag searches
- File: data/local/dao/TransactionDao.kt line 112
- Root Cause: WHERE tagId = :tagId only — no profileId clause. Tags are globally shared but their association with transactions is profile-scoped.
- Fix: Add AND t.profileId = :profileId to the query, add profileId parameter to the DAO method signature.
- Acceptance: Searching by tag in Profile A returns only Profile A transactions.
- Status

 ---
LAYER 1: DATABASE

 ---
ISS-04 · CRITICAL PERF · Missing composite index on transactions(profileId, timestamp)

- Severity: P1 — Full table scan on every time-range dashboard query
- File: data/local/entity/TransactionEntity.kt lines 28–34
- Root Cause: The most frequent query pattern is WHERE profileId = ? AND timestamp BETWEEN ? AND ?. Only single-column indices exist. SQLite cannot use a single-column index
  efficiently for compound predicates — it performs a full scan of all transactions for the profile.
- Fix:
  @Entity(
  tableName = "transactions",
  indices = [
  Index(value = ["profileId", "timestamp"]),   // ADD — covers date-range queries
  Index(value = ["profileId", "accountId"]),   // ADD — covers account filter queries
  // keep existing single-column indices
  ]
  )
- Add a Room migration in AppDatabase.kt:
  val MIGRATION_9_10 = Migration(9, 10) { db ->
  db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_profileId_timestamp ON transactions(profileId, timestamp)")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_profileId_accountId ON transactions(profileId, accountId)")
  }
- Increment @Database(version = 10).
- Acceptance: EXPLAIN QUERY PLAN SELECT * FROM transactions WHERE profileId = 1 AND timestamp BETWEEN x AND y shows USING INDEX not SCAN TABLE.
- Status

 ---
ISS-05 · HIGH PERF · Missing composite index on templates(profileId, isStarred)

- Severity: P1
- File: data/local/entity/TemplateEntity.kt line 22
- Root Cause: Current index: Index(value = ["categoryId"]). Query WHERE profileId = ? AND isStarred = 1 performs a full table scan.
- Fix: Add Index(value = ["profileId", "isStarred"]) and include in migration (can be combined with ISS-04 migration).
- Acceptance: Template queries show index usage in EXPLAIN QUERY PLAN.
- Status

 ---
ISS-06 · HIGH PERF · No DB-level pagination — UI applies take(20) after full load

- Severity: P1 — Entire transaction history loaded for a 20-item feed
- Files:
    - data/local/dao/TransactionDao.kt — dashboard feed query
    - ui/screens/dashboard/DashboardViewModel.kt — caller
- Root Cause: The DAO query fetches all recent transactions. DashboardViewModel passes limit = DASHBOARD_TRANSACTION_FEED_LIMIT as a constant but this is applied in Kotlin
  (.take(limit)) not in SQL.
- Fix: Add LIMIT :limit to the SQL query string. Pass DASHBOARD_TRANSACTION_FEED_LIMIT as a bind parameter to the DAO.
- Acceptance: SQLite query for the dashboard feed returns exactly N rows, not all rows.
- Status

 ---
ISS-07 · MEDIUM PERF · LIKE queries on unindexed note and payee columns

- Severity: P2 — Full scan on every search keystroke
- File: data/local/dao/TransactionDao.kt lines 130–138
- Root Cause: WHERE note LIKE ? COLLATE NOCASE and WHERE payee LIKE ? COLLATE NOCASE. COLLATE NOCASE prevents index use even on indexed columns. Full table scan.
- Fix (Phase A — quick): Ensure search is triggered only after 2+ characters and debounced 300ms to reduce scan frequency.
- Fix (Phase B — proper): Create an FTS5 virtual table for note + payee, replace LIKE queries with FTS match syntax. Requires Room migration.
- Acceptance: Search query plan shows FTS match, not SCAN TABLE.
- Status

 ---
ISS-08 · MEDIUM PERF · Missing composite index on plannedPaymentOccurrences(plannedPaymentId, scheduledDate)

- Severity: P2
- File: data/local/entity/PlannedPaymentOccurrenceEntity.kt lines 20–23
- Root Cause: Two separate single-column indices exist. The query WHERE plannedPaymentId = ? AND scheduledDate = ? requires a composite index for the optimizer to use a
  single index range scan.
- Fix: Replace the two single-column indices with a single composite Index(value = ["plannedPaymentId", "scheduledDate"]).
- Acceptance: EXPLAIN QUERY PLAN for the occurrence lookup shows the composite index.
- Status

 ---
ISS-09 · MEDIUM · In-memory UNION DISTINCT for search results

- Severity: P2
- File: data/repository/TransactionRepositoryImpl.kt line 366
- Root Cause:
  (ftsResults + categoryResults).distinctBy { it.id }
- Two separate Flows (FTS search + category-name search) are combined with combine(), concatenated, and deduplicated in Kotlin. Both full result sets are loaded before
  deduplication.
- Fix: Use a single SQL query with UNION (which deduplicates at DB level), or at minimum, convert result lists to LinkedHashSet for O(1) deduplication instead of distinctBy.
- Acceptance: Search combining FTS + category results does not load duplicate rows from DB.
- Status

 ---
ISS-10 · LOW · SELECT * on all DAOs — over-fetching columns

- Severity: P3
- Root Cause: All DAO queries fetch all columns even when consumers only need 3–4 fields (e.g., id, amount, timestamp).
- Fix (opportunistic): For the hottest paths (dashboard feed, budget aggregation), create projection data classes and use SELECT id, amount, timestamp, type, ... instead of
  SELECT *.
- Note: Only address for the highest-volume queries; wholesale refactoring is high risk for marginal gain.
- Status

 ---
LAYER 2: CARD COMPUTATION

 ---
ISS-11 · CRITICAL PERF · All 15 cards recompute on ANY upstream Flow emission

- Severity: P1 — The single most expensive behavior in the dashboard
- File: ui/screens/dashboard/DashboardViewModel.kt lines 161–189, 319–338
- Root Cause: A single combine() of 5 Flows (accounts, transactions, categories, exchange rates, selected currency) emits a new CardInput on any change. This triggers the
  flatMapLatest which launches parallel computation for ALL registered cards. There is no differentiation between which cards are affected by which data changes.

- Example: User renames an account → accounts Flow emits → ALL 15 cards recompute, including PlannedPaymentsCard (doesn't use account names), TagUpSetPlotCard (doesn't need
  account names), etc.
- Fix — Phase A (immediate, low risk): Add .distinctUntilChanged() before flatMapLatest:
  combine(...)
  .distinctUntilChanged()  // ADD: suppress if CardInput is structurally identical
  .flatMapLatest { ... }
- Requires CardInput fields to implement structural equals (data classes do this automatically).
- Fix — Phase B (architectural): Add fun inputKey(input: CardInput): Any to CardDataComputer. Each computer returns a minimal snapshot of only the fields it reads. The
  registry checks if a computer's key changed before re-invoking it:
  interface CardDataComputer<C, R> {
  fun inputKey(input: CardInput): Any  // ADD
  suspend fun compute(config: C, input: CardInput): R
  }
- CardComputerRegistry.compute() caches the last inputKey per cardType; only recomputes if the key changed.
- Acceptance: Add one transaction. Verify (via logging or Layout Inspector) that only NetWorthCard, CashFlowCard, RecentActivitiesCard, BudgetsCard recompute — not
  PlannedPaymentsCard or LoansCard.
- Status

 ---
ISS-12 · CRITICAL PERF · CardInput is a 8–15 MB god object passed to all card computers

- Severity: P1
- File: domain/card/CardInput.kt lines 22–72
- Root Cause: CardInput carries:
    - transactions: List<Transaction> — potentially thousands
    - allProfileTransactions: List<Transaction> — all-time, unfiltered
    - accounts: List<Account> — all accounts
    - categories: List<Category> — all categories
    - tags: List<Tag> — all tags
    - budgets: List<Budget> — all budgets
    - loans: List<Loan> — all loans
    - loanPayments: Map<Long, List<LoanPayment>> — all loan payments
    - plannedPayments: List<PlannedPayment> — all planned payments

Every card computer receives all of this even if it only uses 1–2 collections. Object is reconstructed and GC'd on every upstream change.
- allProfileTransactions duplication (immediate fix): DashboardViewModel line 214 sets allProfileTransactions = transactions (same value). This field doubles the memory
  footprint. Either remove the duplication or make it lazy.
- Fix — Phase A (quick win): Change effectiveAccounts and effectiveAccountIds from get() properties to val fields computed once at construction. Currently these re-allocate
  on every access:
  // BEFORE (bad):
  val effectiveAccounts: List<Account>
  get() = accounts.filter { !it.isArchived }      // new List on every call
  val effectiveAccountIds: Set<String>
  get() = effectiveAccounts.map { it.id }.toSet() // new Set on every call

// AFTER (fix):
val effectiveAccounts: List<Account> = accounts.filter { !it.isArchived }
val effectiveAccountIds: Set<String> = effectiveAccounts.mapTo(HashSet()) { it.id }
- Fix — Phase B (architectural): Split into CardInputMeta (scalars: profileId, filter, date range, rates, currency) and keep full CardInput for computers that need
  transaction/loan/budget lists. Route card computers to the appropriate tier.
- Acceptance: CardInput allocation rate visible in Memory Profiler drops by ≥50% during scrolling.
- Status

 ---
ISS-13 · CRITICAL PERF · O(n²) category lookups in CategorySpendingTrendCardComputer

- Severity: P1
- File: domain/card/computers/CategorySpendingTrendCardComputer.kt lines 86, 100
- Root Cause: For every result item, a linear scan of input.categories is done:
  val cat = input.categories.find { it.id == catId } ?: return@mapNotNull null
- With 50 result categories × 200 categories in input.categories = 10,000 comparisons per compute.
- Fix:
  // ADD before the loop:
  val categoryById = input.categories.associateBy { it.id }

// REPLACE in loop:
val cat = categoryById[catId] ?: return@mapNotNull null
- Acceptance: Unit test with 200 categories + 50 results completes in <5ms (vs ~50ms with linear scan).
- Status

 ---
ISS-14 · CRITICAL PERF · 12–36 DB round-trips per card compute (bucket-based cards)

- Severity: P1
- Files:
    - domain/card/computers/CategorySpendingTrendCardComputer.kt lines 51–96 (12 queries for 12 months)
    - domain/card/computers/CashFlowComparisonCardComputer.kt lines 76–131 (3 series × 12 = 36 queries)
- Root Cause: For each time bucket, a separate DB query is executed:
  val buckets = rawBuckets.map { (bucketStart, bucketEnd, label) ->
  val bucketFilter = mergedFilter.copy(
  timelineRange = TimelineRange.CustomRange(bucketStart, bucketEnd)
  )
  val summariesByCategory = TransactionAggregationService.getSummaryByCategoryAndCurrency(
  queryService, input.profileId, bucketFilter, excludeInternal  // ← 1 DB query per bucket
  ).getOrDefault(emptyMap())
- For a 12-month chart: 12 queries for CategorySpendingTrend. For CashFlowComparison with current + previous + last-year series: 36 queries. Each recompute (triggered by any
  transaction change) re-fires all queries.
- Fix:
  a. Add a new DAO method getTransactionSummaryInRange(profileId, start, end) that returns all transactions in the outer date range.
  b. In the computer, fetch once for the full outer range.
  c. Partition into buckets in-memory using filter { it.timestamp in bucketStart..bucketEnd }.
  d. Aggregate per bucket in memory.

This collapses 12 DB calls → 1, and 36 → 3 (one per series, not per bucket).
- Acceptance: Systrace shows CategorySpendingTrend.compute() making exactly 1 DB call, not 12.
- Status

 ---
ISS-15 · CRITICAL PERF · Double-sort in CardAggregationService.computeRecentBalances()

- Severity: P1
- File: domain/service/aggregation/CardAggregationService.kt lines 616–643
- Root Cause:
  return transactions.asSequence()
  .sortedBy { it.timestamp }           // ← sort #1 (O(n log n)) — WASTED
  .groupBy { it.accountId }
  .flatMap { (accId, txs) -> ... }
  .sortedByDescending { it.transaction.timestamp }  // ← sort #2 overrides #1
  .toList()
- The first .sortedBy sorts all transactions ascending by timestamp. The groupBy + flatMap destroys that order anyway. The final .sortedByDescending then re-sorts everything
  descending. The first sort produces zero benefit.
- Fix: Remove .sortedBy { it.timestamp } (the first sort). Keep only the final .sortedByDescending.
- Acceptance: Unit test with 1,000 transactions verifies identical output before and after the fix. CPU profile shows function executes in ~half the time.
- Status

 ---
ISS-16 · CRITICAL PERF · O(n) list membership tests in budget/payment filtering

- Severity: P1
- Files:
    - domain/service/aggregation/CardAggregationService.kt lines 295–299
    - domain/card/computers/PlannedPaymentsCardComputer.kt lines 31–36
- Root Cause: mergedFilter.accountIds, .categoryIds, and .tags are List<Long> / List<String>. The in operator on a List is O(n). Inside a .filter {} loop over
  budgets/payments:
  budget.accountIds.any { it in mergedFilter.accountIds }  // ← O(n) per budget.accountId
  pp.tags.any { it in mergedFilter.tags }                  // ← O(n) per tag
- 50 budgets × 10 accountIds × 10 filter accountIds = 5,000 comparisons.
- Fix: Convert filter collections to HashSet before entering any filter loop:
  val accountIdSet = mergedFilter.accountIds.toHashSet()
  val categoryIdSet = mergedFilter.categoryIds.toHashSet()
  val tagSet = mergedFilter.tags.toHashSet()
  val currencySet = mergedFilter.currencies.toHashSet()

val filtered = budgets.filter { budget ->
(accountIdSet.isEmpty() || budget.accountIds.any { it in accountIdSet }) &&
(categoryIdSet.isEmpty() || budget.categoryIds.any { it in categoryIdSet }) &&
(currencySet.isEmpty() || budget.currency in currencySet) &&
(tagSet.isEmpty() || budget.tags.any { it in tagSet })
}
- Acceptance: Filter with 50 budgets and 10 filter criteria runs in <1ms.
- Status

 ---
ISS-17 · HIGH PERF · TagUpSetPlotCardComputer — triple traversal with two re-groups and two sorts

- Severity: P1
- File: domain/card/computers/TagUpSetPlotCardComputer.kt lines 82–260
- Root Cause: Data flows through:
  a. groupBy { it.tags } (line 82)
  b. .filter { it.key != "unspecified" } — done AFTER groupBy, wastes work on "unspecified" data
  c. .toList() + sort (lines 141–157)
  d. combinationData.groupBy { it.tags } — RE-GROUPS after the first grouping (line 220)
  e. Another sort (lines 255–258)

Three full collection traversals where one would suffice. Filtering "unspecified" after grouping processes data that will be discarded.
- Fix:
  a. Move the != "unspecified" filter to BEFORE the first groupBy (filter individual transactions, not the grouped result).
  b. Eliminate the second groupBy by building the intersection map during the first grouping pass.
  c. Apply a single sort at the end.
- Acceptance: Unit test with 500 tagged transactions shows identical output. Execution time measured before/after.
- Status

 ---
ISS-18 · HIGH PERF · PlannedPaymentsCardComputer — nested O(n²) search for upcoming payments

- Severity: P1
- File: domain/card/computers/PlannedPaymentsCardComputer.kt line 64
- Root Cause:
  allUpcoming.firstOrNull { it.plannedPayment.id == payment.id && !it.isOverdue }
- For each active payment, linearly scans all upcoming items. With 100 planned payments: 100 × 100 = 10,000 comparisons.
- Fix: Pre-build a Map<Long, UpcomingPaymentItem> from allUpcoming keyed by plannedPayment.id before the loop:
  val upcomingById = allUpcoming
  .filter { !it.isOverdue }
  .associateBy { it.plannedPayment.id }

// In loop:
val upcoming = upcomingById[payment.id]
- Acceptance: With 100 planned payments, compute runs in <2ms.
- Status

 ---
ISS-19 · HIGH PERF · CardAggregationService linear account lookup inside groupBy loop

- Severity: P1
- File: domain/service/aggregation/CardAggregationService.kt line 621
- Root Cause:
  val account = accounts.find { it.id == accId }  // ← O(n) lookup per account group
- Called once per distinct account in the result set. Fine with 5 accounts, measurable with 50+.
- Fix: Pre-build val accountById = accounts.associateBy { it.id } before the groupBy, replace .find with accountById[accId].
- Acceptance: Code change is trivial; verified by code review.
- Status

 ---
ISS-20 · HIGH PERF · CategorySpendingTrendCardComputer — per-bucket DB queries (separate from ISS-14 root cause detail)

- Severity: P1 (covered under ISS-14 — tracked separately for per-computer attribution)
- File: domain/card/computers/CategorySpendingTrendCardComputer.kt lines 51–96
- Fix: See ISS-14.
- Status (resolve with ISS-14)

 ---
ISS-21 · HIGH PERF · CashFlowComparisonCardComputer — 36 DB queries per compute

- Severity: P1 (covered under ISS-14 — tracked separately for per-computer attribution)
- File: domain/card/computers/CashFlowComparisonCardComputer.kt lines 76–131
- Root Cause: 3 series (current, previous period, last year) × 12 buckets each = 36 DB round-trips per card compute.
- Fix: See ISS-14. For CashFlowComparison specifically: fetch one date range per series (3 queries total), partition buckets in-memory.
- Status (resolve with ISS-14)

 ---
ISS-22 · MEDIUM PERF · BigDecimal arithmetic in inner loops without precision caching

- Severity: P2
- Files:
    - domain/card/computers/CashFlowComparisonCardComputer.kt lines 217–219
    - domain/card/computers/IncomeExpenseBarCardComputer.kt lines 128–130
- Root Cause: CurrencyService.convertAmount() is called per currency per bucket per series. Each call does a BigDecimal.multiply() + setScale(). With 36 buckets × 3
  currencies = 108 BigDecimal operations.
- Fix: Cache exchangeRates[currency] as a local val outside the inner loop. Consider pre-converting all amounts to base currency once at the start of each computer's
  compute() call, rather than re-converting inside summation loops.
- Acceptance: Profiler shows fewer BigDecimal allocations in card compute path.
- Status

 ---
ISS-23 · MEDIUM PERF · IncomeExpenseBarCardComputer.todayBucketIndex uses linear scan

- Severity: P2
- File: domain/card/computers/IncomeExpenseBarCardComputer.kt line 73
- Root Cause:
  val todayBucketIndex = buckets.indexOfLast { (bucketStart, _, _) -> bucketStart <= today }
- Linear scan from start. Buckets are sorted chronologically — binary search would be O(log n).
- Fix: Use buckets.binarySearchBy(today) { it.first } (with Kotlin stdlib binarySearchBy), or keep the linear scan and note it's negligible for <52 buckets.
- Acceptance: Trivial fix; verify by code review.
- Status

 ---
ISS-24 · LOW · mutableMapOf() without initial capacity in TagUpSetPlotCardComputer

- Severity: P3
- File: domain/card/computers/TagUpSetPlotCardComputer.kt lines 183–184, 203–204
- Root Cause: Maps allocated without capacity hint, causing resize operations as entries are added.
- Fix: mutableMapOf<String, BigDecimal>() → estimate capacity based on input.tags.size or input.accounts.size.
- Status

 ---
LAYER 3: UI / VIEWMODEL

 ---
ISS-25 · CRITICAL PERF · Lambda recreation storm in CardUiBridge.rememberCardWithCallbacks()

- Severity: P1 — Disables Compose's skip optimization for ALL card composables
- File: ui/components/cards/CardUiBridge.kt lines 100–220
- Root Cause: The rememberCardWithCallbacks() function constructs 8–15 lambdas inside a remember() block. Because the remember keys include config and input (large objects
  that change frequently), the entire callback set is recreated on nearly every recomposition. Fresh lambda instances are passed to child composables, which treat them as
  "changed," triggering recomposition of the card body even when the displayed data is identical.

- Example for one card:
  onClickIncome = { drillDown ->
  onRecordsClick(resolveEffectiveFilter(...).merge(drillDown))
  }
  // ← NEW function object every recomposition
- Fix — Phase A: Move lambda construction outside the when block so they are constructed once per card instance:
  val resolvedFilter by rememberUpdatedState(resolveEffectiveFilter(input.filter, config.filter, ...))
  val onClickIncomeStable = remember(resolvedFilter) { { drillDown: DrillDown ->
  onRecordsClick(resolvedFilter.merge(drillDown))
  }}
- Then pass onClickIncomeStable (stable reference) into the callback object.
- Fix — Phase B (proper): Create a rememberStableCallbacks(cardType, onRecordsClick, onDelete) factory that produces stable functional interface wrappers, not data class
  lambdas.
- Acceptance: Layout Inspector shows zero recompositions in card bodies when non-data state (e.g., lastUndoAction) changes.
- Status

 ---
ISS-26 · CRITICAL PERF · CardInput remember-keyed on entire DashboardUiState object

- Severity: P1
- File: ui/screens/dashboard/DashboardScreen.kt lines 880–898
- Root Cause:
  val sharedCardInput = remember(state) { CardInput(...) }
- state is a DashboardUiState.Success — a large sealed class with 20+ fields. If cardsConfig, lastUndoAction, or any display flag changes, state is a new object, remember
  invalidates, and CardInput is fully reconstructed, triggering all LaunchedEffect(config, input) blocks in rememberCardWithCallbacks(), which recompute all 15 cards.
- Fix: Key remember on only the fields that actually change the card computation:
  val sharedCardInput = remember(
  state.profileId,
  state.allTransactions.size,     // size change = new/deleted transactions
  state.allAccounts.size,
  state.allCategories.size,
  state.filter,
  state.selectedCurrency,
  state.exchangeRates.entries.size // rate update
  ) {
  CardInput(
  transactions = state.allTransactions,
  ...
  )
  }
- Note: Size-based keys miss content changes (e.g., rename). A more robust approach is to include a dataVersion: Long counter incremented on actual data mutations at the
  ViewModel level.
- Acceptance: Toggling a card animation setting (a display flag) does NOT trigger card recomputation.
- Status

 ---
ISS-27 · HIGH PERF · 13 independent collectAsStateWithLifecycle() calls at screen root

- Severity: P1
- File: ui/screens/dashboard/DashboardScreen.kt lines 221–238
- Root Cause: Each of 13 Flow subscriptions is an independent recomposition trigger. When lastUndoAction (a Boolean?) changes, the screen's entire composition scope is
  re-entered. Compose cannot skip the outer composable because it has state reads from 13 different sources.

- Current subscriptions:
  a. dashboardUiState — DashboardViewModel
  b. initialViewContext — DashboardViewModel
  c. cardsConfig — DashboardViewModel
  d. currentProfile — SettingsViewModel
  e. cardAnimationEnabled — SettingsViewModel
  f. expandableFabEnabled — SettingsViewModel
  g. allTemplates — TemplateViewModel
  h. allCategories — TransactionViewModel
  i. lastUndoAction — DashboardViewModel
  j. savedFilters — TransactionViewModel
  k. revealedCardIds — TransactionViewModel
  l. cardDataMap — DashboardViewModel
  m. (implied others)
- Fix: Combine logically related read-only display state into a single DashboardDisplayState data class exposed as one StateFlow from the ViewModel. Screen subscribes once:
  data class DashboardDisplayState(
  val cardAnimationEnabled: Boolean,
  val expandableFabEnabled: Boolean,
  val currentProfile: Profile?,
  // ...
  )

val displayState: StateFlow<DashboardDisplayState> = combine(
settingsRepository.cardAnimationEnabled,
settingsRepository.expandableFabEnabled,
settingsRepository.currentProfile,
// ...
) { ... }.stateIn(...)
- Acceptance: Adding one transaction causes ≤3 recompositions of DashboardScreen (one per logical state group), not 13.
- Status

 ---
ISS-28 · HIGH PERF · cardDataMap reference change invalidates all card remember blocks

- Severity: P1
- Files:
    - ui/screens/dashboard/DashboardViewModel.kt lines 319–338
    - ui/components/cards/AeonCardList.kt line 133
- Root Cause: When card #3 finishes computing, the ViewModel emits a new Map<String, CardRenderData> with card #3's data added. AeonCardList passes cardDataMap[card.cardId]
  as a remember key to each rememberCardWithCallbacks(). Since the Map reference changed, all 15 cards' remember keys are considered "potentially changed" — the runtime must
  re-evaluate all card renders.
- Fix — Phase A: Add .distinctUntilChanged() on the cardDataMap state before it reaches the UI. Since it's a Map<String, CardRenderData> of data classes, structural equality
  should work automatically. This prevents re-emission when the content is identical.
- Fix — Phase B: Deliver card updates as individual events (StateFlow<Map<CardType, CardRenderData>> with update { it + (cardType to newData) }). Cards subscribe to their own
  slice via derivedStateOf { cardDataMap[myType] }.
- Acceptance: Card #8 finishing computation does NOT trigger a recomposition of cards #1–7 or #9–15.
- Status

 ---
ISS-29 · HIGH · Missing @Stable / @Immutable on CardCallbacks and CardWithCallbacks

- Severity: P1 — Compose's stability analysis marks ALL card composables as "always unstable"
- Files:
    - ui/components/cards/CardCallbacks.kt
    - ui/components/cards/CardWithCallbacks.kt
- Root Cause: CardCallbacks is a sealed class with lambda properties (e.g., onClickIncome: ((DrillDown) -> Unit)?). Kotlin functions are not @Stable by Compose's compiler
  analysis. Without @Stable on the container class, Compose cannot apply parameter skipping to any composable that receives a CardCallbacks subtype.
- Fix: Annotate both sealed classes with @Stable:
  @Stable
  sealed class CardCallbacks {
  @Stable
  data class NetWorth(
  val onClickNet: ((TransactionFilter) -> Unit)? = null,
  // ...
  ) : CardCallbacks()
  }
- Important caveat: @Stable is a promise to the compiler that the type's observable behavior won't change between compositions when its fields haven't changed. This is safe
  only if lambda references are themselves stable (not capturing mutable state directly). Verify this is true before adding the annotation.
- Acceptance: Compose compiler metrics (enabled with freeCompilerArgs += ["-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=..."]) show CardCallbacks
  subtypes as stable.
- Status

 ---
ISS-30 · HIGH PERF · channelFlow sends 15+ emissions during parallel card compute phase

- Severity: P1 — UI recomposes 15+ times in rapid succession during load
- File: ui/screens/dashboard/DashboardViewModel.kt lines 319–338
- Root Cause: The ViewModel's channelFlow calls send(buildSuccess(...)) once per card as it finishes computing (one launch per card). With 15 cards computing in parallel, 15
  emissions fire in rapid succession. Each emission triggers a full recomposition of DashboardScreen with the updated cardDataMap.
- Fix: Batch card completion emissions with a short debounce:
  // Replace individual send() calls with:
  val cardDataFlow = MutableStateFlow<Map<String, CardRenderData>>(emptyMap())

cards.forEach { config ->
launch(Dispatchers.Default) {
val result = registry.compute(config.cardType, input, config)
cardDataFlow.update { it + (config.cardId to result) }  // atomic update, single emission
}
}
- MutableStateFlow.update {} coalesces concurrent updates into a single emission automatically.
- Acceptance: Loading the dashboard from cold state produces ≤3 UI emissions, not 15.
- Status

 ---
ISS-31 · HIGH PERF · No key {} lambda in card list or account list items

- Severity: P1
- Files:
    - ui/components/cards/AeonCardList.kt — items(cards) call
    - ui/screens/dashboard/components/AccountsCard.kt lines 103–169 — items(accounts) call and forEach
- Root Cause: Without key = { it.id }, Compose uses positional identity. Deleting card #2 or account #3 forces Compose to recompose all items after that position because
  their positional index shifted.
- Fix:
  // AeonCardList:
  items(cards, key = { it.cardId }) { card -> ... }

// AccountsCard:
items(accounts, key = { it.id }) { account -> ... }
- Acceptance: Deleting account #3 triggers recomposition of only the deleted item's slot, not all subsequent accounts.
- Status

 ---
ISS-32 · MEDIUM PERF · sharedCallbacks remember block has 19 dependency keys

- Severity: P2
- File: ui/screens/dashboard/DashboardScreen.kt lines 819–878
- Root Cause: remember(onManageAccountsClick, onAccountClick, ..., onAddTag) with 19 lambda parameters. If any one of these 19 lambdas is captured from a parent composable
  scope (non-stable reference), the entire AeonInteractionCallbacks tree is rebuilt, reconstructing 7 nested action objects.
- Fix: Wrap each callback in rememberUpdatedState() at the call site, producing stable State<() -> Unit> holders. Or consolidate into a stable callback contract class passed
  from MainActivity.
- Acceptance: Navigating back to the dashboard (which may re-create parent lambdas) does NOT rebuild sharedCallbacks unless the actual action logic changed.
- Status

 ---
ISS-33 · MEDIUM PERF · Inline onClick lambdas in BudgetsCard row items

- Severity: P2
- File: ui/screens/dashboard/components/BudgetsCard.kt lines 88–94
- Root Cause:
  items.forEachIndexed { index, progress ->
  BudgetProgressRow(
  onClick = { callbacks.onBudgetClick(progress.budget.id) }  // ← new lambda per iteration
  )
  }
- Each iteration creates a fresh lambda. BudgetProgressRow cannot skip recomposition because its onClick is always a new function object.
- Fix:
  val onBudgetClickStable = callbacks::onBudgetClick  // stable method reference
  items.forEachIndexed { index, progress ->
  BudgetProgressRow(
  budgetId = progress.budget.id,
  onClick = onBudgetClickStable  // stable, same reference across recompositions
  )
  }
- Move budgetId as a parameter; the row calls onClick(budgetId) internally.
- Acceptance: Budget card items do not show recomposition in Layout Inspector when non-budget state changes.
- Status

 ---
ISS-34 · MEDIUM PERF · Inline Modifier.clickable {} lambda in NetWorthCard

- Severity: P2
- File: ui/screens/dashboard/components/NetWorthCard.kt lines 136–142
- Root Cause:
  modifier = Modifier.clickable {
  if (config.perspective is ...) {
  callbacks.onClickNet?.invoke(config.filter)
  } else {
  callbacks.onClickNetFlow?.invoke(config.filter)
  }
  }
- New lambda on every recomposition. The Text composable with this modifier recomposes even when the net worth value is unchanged.
- Fix: Hoist lambda creation to a remember(config.perspective, config.filter, callbacks) block.
- Acceptance: Net worth Text composable shows 0 recompositions in Layout Inspector when display preferences change.
- Status

 ---
ISS-35 · MEDIUM PERF · CashFlowCard table columns remember-keyed on Color objects

- Severity: P2
- File: ui/screens/dashboard/components/CashFlowCard.kt line 145
- Root Cause:
  val columns = remember(config, types, result, positiveColor, negativeColor, accentColor) { ... }
- Color instances from MaterialTheme.colorScheme are typically recreated as new objects on composition even if the actual ARGB value is unchanged. remember uses referential
  equality, so the table columns are rebuilt every recomposition.
- Fix: Key on color integer values:
  remember(config, types, result, positiveColor.value, negativeColor.value, accentColor.value) { ... }
- Or extract the color values to local vals with val positiveColorValue = positiveColor.value and use those as keys.
- Acceptance: CashFlow table does not rebuild when theme is re-evaluated but color values haven't changed.
- Status

 ---
ISS-36 · MEDIUM PERF · No distinctUntilChanged() on individual card data emissions

- Severity: P2
- File: ui/screens/dashboard/DashboardViewModel.kt lines 319–338
- Root Cause: When a card recomputes and produces the same result as before (e.g., a transaction was added to a different currency than the card displays), the ViewModel
  still emits a new cardDataMap. The UI cannot distinguish "data changed" from "same data, new object."
- Fix: Before updating the cardDataMap state, compare the new CardRenderData with the existing entry using == (structural equality of data classes):
  cardDataFlow.update { current ->
  if (current[cardId] == newData) current  // no change, no emission
  else current + (cardId to newData)
  }
- Acceptance: Adding a USD transaction does not trigger recomposition of the EUR-only CurrencyBalanceCard.
- Status

 ---
ISS-37 · MEDIUM PERF · AeonCardList drag callback uses rememberUpdatedState indirection

- Severity: P3 — Minor overhead
- File: ui/components/cards/AeonCardList.kt lines 82–83
- Root Cause: rememberUpdatedState adds one State dereference on every drag event. Not significant in isolation, but contributes to drag jank if combined with other issues.
- Fix: Pass the reorderable state management through a stable hoisted callback class rather than per-composition State holders.
- Acceptance: Drag-and-drop reorder is smooth (no dropped frames) when 15 cards are visible.
- Status

 ---
ISS-38 · LOW · Debug Log.d calls in hot recomposition paths

- Severity: P3 — Causes unnecessary string allocations in debug builds; could mask production performance profiles
- Files: Search with grep -r "Log.d\|println" app/src/main/java/com/osscabinet/ossWallet/ui/screens/dashboard/
- Fix: Wrap with if (BuildConfig.DEBUG) or remove.
- Acceptance: Profiler shows no String allocation spikes from log calls during dashboard scrolling.
- Status

 ---
LAYER 4: DATA CORRECTNESS / SECURITY (new issues discovered in code review)

 ---
ISS-39 · CRITICAL · Exchange rate pivot fallback emits stale un-rebased map

- Severity: P0 — Wrong exchange rates applied to all multi-currency card computations
- File: data/repository/ComputationRepositoryImpl.kt (exchange rate rebase logic)
- Root Cause:
  val pivotRate = rateMap[targetBase] ?: return@combine rateMap  // emits stale un-rebased map
  When the target base currency's rate is not found in the fetched map (e.g., rate service returned
  a partial update), the operator silently returns the original rateMap without rebasing it. All
  downstream card computers then use rates expressed in the wrong base currency, producing silently
  incorrect totals and charts.
- Fix: Replace the early return with an explicit error emission or a logged warning + use a cached
  last-known-good rebase result. Never silently return an un-rebased map when rebasing fails:
  val pivotRate = rateMap[targetBase]
  if (pivotRate == null) {
      Timber.w("Exchange rate pivot not found for $targetBase, using last good rates")
      return@combine lastValidRebased  // cached from previous successful rebase
  }
- Acceptance: When the target base rate is missing, card computations use the last successfully
  rebased rate, not the raw un-rebased map. A logcat warning appears.
- [ ] Status

 ---
ISS-40 · HIGH · Unbounded CardRenderCache grows indefinitely with profiles

- Severity: P1 — Memory leak for multi-profile users
- File: domain/cache/CardRenderCache.kt
- Root Cause: CardRenderCache is a singleton ConcurrentHashMap<Int, Map<String, CardRenderData>>
  keyed by profileId. Entries are never evicted. A user who switches between 5 profiles will
  accumulate 5 × (15 cards × CardRenderData size) indefinitely. No TTL, no max size, no cleanup
  on profile deletion.
- Fix: Bound the cache to the most recent N profiles (e.g., 2) using a LinkedHashMap with
  removeEldestEntry, or use an LRU cache with max size 3. Also wire a cache invalidation call
  to profile deletion events.
- Acceptance: Memory Profiler shows CardRenderCache size stays bounded regardless of how many
  profiles are accessed.
- [ ] Status

 ---
ISS-41 · MEDIUM · resolveTimeBoundsFromDb duplicated across 3 card computers

- Severity: P2 — Code smell; divergence risk when logic needs updating
- Files:
    - domain/card/computers/CategorySpendingTrendCardComputer.kt (private method lines 119–131)
    - domain/card/computers/CashFlowComparisonCardComputer.kt (private top-level function line 29)
    - domain/card/computers/IncomeExpenseBarCardComputer.kt (private top-level function line 29)
- Root Cause: Identical resolveTimeBoundsFromDb function copied 3 times. Any fix to its logic
  (e.g., ISS-52 below) must be applied in 3 places or one copy silently diverges.
- Fix: Extract to a shared utility object (e.g., CardComputerUtils.resolveTimeBoundsFromDb()) and
  replace all 3 private copies with a call to the shared version.
- Acceptance: Single definition, zero duplication. ISS-52 fix only needs one change.
- [ ] Status

 ---
ISS-42 · HIGH SECURITY · LoanPaymentDao.getAllPayments() lacks profileId filter

- Severity: P1 — Cross-profile loan payment data visible
- File: data/local/dao/LoanPaymentDao.kt
- Root Cause: getAllPayments() uses SELECT * FROM loan_payments with no WHERE profileId = :profileId
  clause. Loan payments from all profiles are returned together. Any screen or card that calls this
  DAO exposes data across profile boundaries.
- Fix: Add profileId: Long parameter and WHERE profileId = :profileId to the query. Update all
  call sites to pass the active profileId. Add composite index (profileId, loanId).
- Acceptance: Loan payments created in Profile A do NOT appear in Profile B's loan cards.
- [ ] Status

 ---
ISS-43 · CRITICAL DATA INTEGRITY · toBalanceFilter() leaks category/tag filters into account balance queries

- Severity: P0 — Account balances silently wrong when any category or tag filter is active
- File: domain/service/aggregation/CardAggregationService.kt lines 802–813
- Root Cause: toBalanceFilter() only strips the timeline range portion of the filter. It does NOT
  strip categoryIds, tags, or currencies:
  private fun TransactionFilter.toBalanceFilter(): TransactionFilter {
      return when (val range = this.timelineRange) {
          is TimelineRange.AllTime -> this
          else -> {
              val (_, end) = TimelineService.getDateRange(range)
              this.copy(
                  timelineRange = if (end != null) TimelineRange.CustomRange(end = end, ...)
                                 else TimelineRange.AllTime(...)
              )
          }
      }
  }
  When a card filter has categoryIds = ["Food"] set, the balance query runs with that category
  filter active, so only "Food" transactions count toward the account balance. Balances appear
  artificially low or zero for accounts that have no "Food" transactions.
- Fix: Strip all non-temporal filter fields when computing a balance baseline:
  private fun TransactionFilter.toBalanceFilter(): TransactionFilter = this.copy(
      categoryIds = emptyList(),
      tags = emptyList(),
      currencies = emptyList(),
      paymentMethods = emptyList(),
      statuses = emptyList()
  )
- Acceptance: Account balances shown in balance-baseline cards are identical regardless of which
  category/tag filter is active.
- [ ] Status

 ---
ISS-44 · CRITICAL UX · First-launch infinite loading when app is offline

- Severity: P0 — App appears permanently broken on first install without network
- Files:
    - data/repository/ComputationRepositoryImpl.kt — hasLoadedExchangeRates definition
    - ui/screens/dashboard/DashboardViewModel.kt line 309
- Root Cause:
  override val hasLoadedExchangeRates: Flow<Boolean> =
      currencyRepository.ratesFlow.map { it.isNotEmpty() }
  On first install, the DB has no exchange rates. If the device is offline, the rate fetch fails
  silently and ratesFlow never emits a non-empty list. DashboardViewModel gates all card rendering:
  if (!hasLoadedExchangeRates) return@channelFlow
  Cards never render. The user sees a permanent loading spinner with no feedback.
- Fix: Treat empty rates as "rates not yet available" but not as a blocker. Fall back to 1:1
  conversion (or use the app's last-known base currency as a single-currency fallback):
  val effectiveRates = if (exchangeRates.isEmpty()) {
      mapOf(selectedCurrency to BigDecimal.ONE)  // single-currency fallback
  } else exchangeRates
  Emit a UI warning banner ("exchange rates unavailable") but render cards with local-currency data.
- Acceptance: On first install with no network, dashboard cards render within 2 seconds showing
  local-currency totals, with a subtle "rates unavailable" indicator.
- [ ] Status

 ---
ISS-45 · MEDIUM · Card animations default silently flipped to false

- Severity: P2 — Regression: card animations disabled for all users after update
- File: data/local/SettingsDataStore.kt
- Root Cause: The DataStore preference read uses:
  preferences[CARD_ANIMATION_ENABLED_KEY] ?: false  // was ?: true
  The default was changed from true to false. All existing users who never explicitly set this
  preference now have animations disabled without any notification or opt-in.
- Fix: Restore the default to true. If the intent was to disable animations, add a one-time
  migration flag that shows users a "we turned off animations — re-enable in settings" prompt.
- Acceptance: A fresh install shows card animations enabled by default.
- [ ] Status

 ---
ISS-46 · HIGH DATA INTEGRITY · Self-transfer validation removed (validateAccountSelection deleted)

- Severity: P1 — User can submit a transfer from Account A to Account A, creating a phantom
  transaction that adds balance to the account without a corresponding debit
- Files: ui/screens/transactions/entry/ (validateAccountSelection no longer exists)
- Root Cause: The validateAccountSelection function that checked source != target was deleted
  during the refactor. No replacement validation exists.
- Fix: Add guard in TransactionEntryViewModel or TransactionEntryEvent processing:
  if (isTransfer && selectedAccount?.id == targetAccount?.id && selectedAccount != null) {
      emitEffect(TransactionEntryEffect.ShowError("Source and target accounts cannot be the same"))
      return
  }
- Acceptance: Attempting to save a transfer with identical source and target accounts shows an
  inline error and does not create the transaction.
- [ ] Status

 ---
ISS-47 · MEDIUM UX · Target account picker does not exclude the source account

- Severity: P2 — Users can accidentally pick the same account for both sides of a transfer
  (separate from ISS-46 which handles the save-time guard; this is a UX-layer issue)
- File: ui/screens/transactions/entry/components/TransactionEntrySheets.kt lines 83–98
- Root Cause: Target account list is built as listOf(Account.OUTSIDE_WALLET) + state.accounts
  with no filter excluding the currently selected source account.
- Fix: Filter out the source account from the target account picker list:
  val filteredTargetAccounts = (listOf(Account.OUTSIDE_WALLET) + state.accounts)
      .filter { it.id != state.selectedAccount?.id }
- Acceptance: When Account B is selected as source, Account B does not appear in the target
  account dropdown.
- [ ] Status

 ---
ISS-48 · HIGH DATA INTEGRITY · Outside-to-Outside transfers allowed (Wallet Minimum Rule removed)

- Severity: P1 — A transfer with source = OUTSIDE_WALLET and target = OUTSIDE_WALLET creates a
  record that moves money out of nowhere into nowhere, corrupting net worth calculations
- Files: ui/screens/transactions/entry/ (wallet minimum rule enforcement removed)
- Root Cause: The original validation that required at least one side of a transfer to be an
  internal wallet account was removed with no replacement.
- Fix: Re-add the guard before saving:
  if (isTransfer && selectedAccount == Account.OUTSIDE_WALLET && targetAccount == Account.OUTSIDE_WALLET) {
      emitEffect(TransactionEntryEffect.ShowError("At least one account must be a wallet account"))
      return
  }
- Acceptance: Attempting to save OUTSIDE → OUTSIDE transfer shows an error. Internal → OUTSIDE
  and OUTSIDE → Internal transfers remain allowed.
- [ ] Status

 ---
ISS-49 · MEDIUM UX · Cannot clear target account (clearTargetAccount deleted)

- Severity: P2 — Once a target account is set, the user has no way to remove it; the only
  escape is switching transaction type away from Transfer and back
- Files: ui/screens/transactions/entry/ (clearTargetAccount function removed)
- Root Cause: clearTargetAccount() was deleted with no replacement. The target account field
  has no clear/remove action.
- Fix: Re-add TransactionEntryEvent.ClearTargetAccount and handle it in the ViewModel to set
  targetAccount = null. Wire a "×" or "clear" button in the target account field UI.
- Acceptance: Tapping the clear icon on the target account field resets it to unset state.
- [ ] Status

 ---
ISS-50 · HIGH UX · PredictiveBackHandler in QuickAddSheet always dismisses the full sheet

- Severity: P1 — Navigating back on Step 3 of Quick Add discards all entered data and closes
  the sheet instead of returning to Step 2
- File: ui/screens/dashboard/quickadd/QuickAddSheet.kt lines 128–141
- Root Cause: The PredictiveBackHandler is wired to call animateDismiss() unconditionally,
  regardless of which step is currently active. The code comment explicitly acknowledges this:
  // system/gesture back always dismisses the whole sheet, regardless of step.
- Fix: Check the current step index before deciding to dismiss or step back:
  PredictiveBackHandler(enabled = visibleState.targetState) { progress ->
      try {
          progress.collect { event -> backProgress = event.progress }
          if (currentStep > 0) {
              goToPreviousStep()  // step back
          } else {
              animateDismiss()   // on first step, dismiss
          }
      } catch (_: Exception) { backProgress = 0f }
  }
- Acceptance: Back gesture on Step 3 returns to Step 2 with data intact. Back on Step 1 dismisses.
- [ ] Status

 ---
ISS-51 · HIGH UX · Quick Add category search LaunchedEffect removed — search conflicts with drilldown navigation

- Severity: P1 — Opening a sub-category from search results in QuickAdd navigates to the
  sub-category screen but loses the previously selected parent, breaking the category selection flow
- Files: ui/screens/dashboard/quickadd/ (LaunchedEffect that cleared drilldown stack on search removed)
- Root Cause: A LaunchedEffect that cleared the category drill-down navigation stack when the
  user started a new search was removed during the refactor. Now, if the user drills into a
  subcategory, then searches, the search results coexist with the previous drill-down state,
  causing navigation inconsistency.
- Fix: Re-add the LaunchedEffect that resets drilldown state when the search query changes to
  a non-empty string:
  LaunchedEffect(searchQuery) {
      if (searchQuery.isNotBlank()) {
          categoryNavController.popBackStack(root, inclusive = false)
      }
  }
- Acceptance: Typing in the search box clears the subcategory drilldown. Clearing the search
  returns the user to the last manual navigation position.
- [ ] Status

 ---
ISS-52 · HIGH DATA INTEGRITY · resolveTimeBoundsFromDb uses global minimum timestamp, ignoring card filter scope

- Severity: P1 — Time-series charts (CategorySpendingTrend, CashFlowComparison,
  IncomeExpenseBar) show data from ALL TIME even when a date-scoped filter is applied to the card
- Files:
    - domain/card/computers/CategorySpendingTrendCardComputer.kt lines 119–131
    - domain/card/computers/CashFlowComparisonCardComputer.kt line 29
    - domain/card/computers/IncomeExpenseBarCardComputer.kt line 29
- Root Cause: resolveTimeBoundsFromDb calls getMinTransactionTimestamp(profileId) which returns
  the absolute earliest transaction date across the entire profile. When a card has a filter like
  "last 6 months", the bounds resolution ignores that filter and expands to "all time", causing
  the chart to display more history than the user configured.
- Fix: Pass the card's effective filter into resolveTimeBoundsFromDb and apply it when querying
  the min timestamp:
  suspend fun resolveTimeBoundsFromDb(
      queryService: DatabaseQueryService,
      profileId: Long,
      effectiveFilter: TransactionFilter  // ADD
  ): Pair<Long, Long> {
      val minTs = queryService.getMinTransactionTimestamp(profileId, effectiveFilter)
      ...
  }
  Or, more correctly: if the card config specifies an explicit time range, never override it with
  a DB-derived bound — only fall back to the DB bound when the config says "all time".
- Acceptance: A card configured for "last 6 months" shows exactly 6 months of data, not data
  from 3 years ago.
- [ ] Status

 ---
ISS-53 · CRITICAL DATA INTEGRITY · Loan interest calculated but never added to remaining balance

- Severity: P0 — Loan "remaining balance" shown in the Loans card is wrong for all interest-bearing loans
- File: domain/service/aggregation/CardAggregationService.kt lines 421–422
- Root Cause:
  val remaining = (loan.principalAmount - totalPaid).coerceAtLeast(BigDecimal.ZERO)
  val interest = calculateAccruedInterest(loan, totalPaid)
  // interest is computed but never used — remaining is returned without interest
- The interest variable is computed but the method returns remaining without adding interest,
  so all interest-bearing loans show the principal-only balance as if interest is $0.
- Fix:
  val principalRemaining = (loan.principalAmount - totalPaid).coerceAtLeast(BigDecimal.ZERO)
  val interest = calculateAccruedInterest(loan, principalRemaining)
  val remaining = principalRemaining + interest
- Note: The coerceAtLeast should be applied BEFORE passing to interest calculation so negative
  principal (overpaid) doesn't cause negative interest.
- Acceptance: A $10,000 loan at 5% annual interest after 1 year with $0 paid shows a remaining
  balance of approximately $10,500, not $10,000.
- [ ] Status

 ---
ISS-54 · CRITICAL DATA INTEGRITY · Running balance drift in Recent Activities card

- Severity: P0 — Cumulative running balances in the Recent Activities timeline are wrong when
  any non-temporal filter (category, tag, account) is active
- File: domain/service/aggregation/CardAggregationService.kt lines 449–463
- Root Cause: The computation mixes two incompatible data scopes:
  - startingBalances is fetched with a bare TransactionFilter(timelineRange = ...) — no
    category/account/tag filters applied
  - txs (the transactions to display) is fetched with effectiveFilter — which includes all
    active filters (category, tag, account, etc.)
  The running balance adds filtered transaction deltas on top of an unfiltered absolute baseline,
  producing numbers that drift further from reality with each transaction shown.
  Example: If the filter is "Food category only", startingBalances includes ALL transactions
  up to the start date (food + rent + salary), but the running deltas only add food transactions.
  The balance line starts too high and each food transaction moves it by the wrong amount.
- Fix: Apply the same effective filter to both the starting balance query AND the delta
  transactions. If the filter is category-scoped, both the baseline and deltas must be category-
  scoped:
  val startingBalances = queryService.getBalanceAtDate(
      profileId,
      startDate,
      effectiveFilter  // same filter as txs
  )
- Acceptance: With a "Food" filter, the running balance line starts at $0 (no food balance
  prior to the range) and correctly tracks cumulative food spending.
- [ ] Status

 ---
Issue Count by Priority

┌──────────┬───────┬───────────────────────────────────────────────────────────────────────────────────────┐
│ Priority │ Count │ Description                                                                           │
├──────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────┤
│ INVALID  │ 1     │ ISS-01 (audit correction — not a crash)                                               │
├──────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────┤
│ P0       │ 6     │ ISS-02 (template leak), ISS-39 (exchange rate fallback), ISS-43 (balance filter leak),│
│          │       │ ISS-44 (offline loading), ISS-53 (loan interest), ISS-54 (running balance drift)      │
├──────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────┤
│ P1       │ 24    │ Critical perf + security + data integrity issues                                      │
│          │       │ (ISS-03,04,05,06,11,12,13,14,15,16,17,18,19,20,21,25,26,27,28,29,30,31,42,46,48,      │
│          │       │ 50,51,52)                                                                             │
├──────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────┤
│ P2       │ 13    │ Medium perf + UX issues                                                               │
│          │       │ (ISS-07,08,09,22,23,32,33,34,35,36,40,41,45,47,49)                                   │
├──────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────┤
│ P3       │ 4     │ Low / cleanup (ISS-10,24,37,38)                                                       │
├──────────┼───────┼───────────────────────────────────────────────────────────────────────────────────────┤
│ Total    │ 53    │ (54 entries, 1 invalid)                                                               │
└──────────┴───────┴───────────────────────────────────────────────────────────────────────────────────────┘

 ---
Implementation Roadmap

Sprint 1 — Stop the Bleeding (P0 data integrity + quick P1 wins)

Goal: Fix silent data corruption, data leaks, and cheapest high-impact perf improvements.
Note: ISS-01 was INVALID — removed. Added new P0 data integrity fixes.

┌─────┬────────────────────────────────────────────────────────────────────────────────┬─────────────┬──────┐
│  #  │                                   Issue                                        │ Est. Effort │ Risk │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 1   │ ISS-53 — add loan interest to remaining balance (1-line fix, P0 data bug)      │ 15min       │ None │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 2   │ ISS-43 — fix toBalanceFilter() to strip category/tag filters (P0 data bug)     │ 30min       │ Low  │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 3   │ ISS-54 — apply same filter to startingBalances as to txs (P0 data bug)         │ 1h          │ Low  │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 4   │ ISS-46 — re-add self-transfer guard (validateAccountSelection)                 │ 30min       │ None │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 5   │ ISS-48 — re-add outside-to-outside transfer guard (Wallet Minimum Rule)        │ 30min       │ None │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 6   │ ISS-02, ISS-03 — add profileId filters to TemplateDao / TransactionDao         │ 2h          │ Low  │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 7   │ ISS-42 — add profileId filter to LoanPaymentDao.getAllPayments()                │ 1h          │ Low  │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 8   │ ISS-04 — add composite DB index (profileId, timestamp) + migration             │ 2h          │ Low  │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 9   │ ISS-05 — add composite DB index on templates                                   │ 1h          │ Low  │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 10  │ ISS-13 — fix O(n²) in CategorySpendingTrendCardComputer                        │ 30min       │ None │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 11  │ ISS-15 — remove double-sort in computeRecentBalances                           │ 30min       │ None │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 12  │ ISS-16 — convert filter lists to HashSet in aggregation/filter loops           │ 1h          │ None │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 13  │ ISS-18 — fix O(n²) in PlannedPaymentsCardComputer                              │ 30min       │ None │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 14  │ ISS-19 — pre-build account map in computeRecentBalances                        │ 15min       │ None │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 15  │ ISS-12 (Phase A) — change effectiveAccounts/effectiveAccountIds to val         │ 15min       │ None │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼──────┤
│ 16  │ ISS-31 — add key = { it.id } to all list items                                 │ 30min       │ None │
└─────┴────────────────────────────────────────────────────────────────────────────────┴─────────────┴──────┘

 ---
Sprint 2 — UI Stability (P1 UI issues)

Goal: Prevent cascading recompositions across all cards.

┌─────┬────────────────────────────────────────────────────────────────────┬─────────────┬────────┐
│  #  │                               Issue                                │ Est. Effort │  Risk  │
├─────┼────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 12  │ ISS-11 (Phase A) — add distinctUntilChanged() before card compute  │ 1h          │ Low    │
├─────┼────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 13  │ ISS-26 — fix remember(state) key in DashboardScreen                │ 1h          │ Low    │
├─────┼────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 14  │ ISS-29 — add @Stable to CardCallbacks and CardWithCallbacks        │ 2h          │ Medium │
├─────┼────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 15  │ ISS-30 — replace channelFlow.send() with MutableStateFlow.update{} │ 2h          │ Medium │
├─────┼────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 16  │ ISS-36 — add distinctUntilChanged on card data emissions           │ 1h          │ Low    │
├─────┼────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 17  │ ISS-33, ISS-34 — hoist inline lambdas in BudgetsCard, NetWorthCard │ 1h          │ None   │
├─────┼────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 18  │ ISS-35 — fix Color object remember keys in CashFlowCard            │ 30min       │ None   │
└─────┴────────────────────────────────────────────────────────────────────┴─────────────┴────────┘

 ---
Sprint 3 — Database Query Batching (P1 DB)

Goal: Collapse 12–36 DB calls per card to 1–3.

┌─────┬───────────────────────────────────────────────────────────────────────────────────────────────┬─────────────┬────────┐
│  #  │                                             Issue                                             │ Est. Effort │  Risk  │
├─────┼───────────────────────────────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 19  │ ISS-14, ISS-20, ISS-21 — batch bucket queries in CategorySpendingTrend and CashFlowComparison │ 4h          │ Medium │
├─────┼───────────────────────────────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 20  │ ISS-06 — push pagination to DB layer                                                          │ 2h          │ Low    │
├─────┼───────────────────────────────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 21  │ ISS-08 — fix composite index on plannedPaymentOccurrences                                     │ 1h          │ Low    │
├─────┼───────────────────────────────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 22  │ ISS-09 — SQL UNION for search results                                                         │ 2h          │ Medium │
└─────┴───────────────────────────────────────────────────────────────────────────────────────────────┴─────────────┴────────┘

 ---
Sprint 4 — Architectural Refactor (P1/P2 architectural)

Goal: Eliminate root cause of "all cards recompute on any change."

┌─────┬────────────────────────────────────────────────────────────────────────────────┬─────────────┬────────┐
│  #  │                                     Issue                                      │ Est. Effort │  Risk  │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 23  │ ISS-11 (Phase B) — per-card inputKey() scoping                                 │ 6h          │ High   │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 24  │ ISS-27 — consolidate 13 Flow subscriptions into combined DashboardDisplayState │ 4h          │ Medium │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 25  │ ISS-28 (Phase B) — per-card StateFlow slices in AeonCardList                   │ 4h          │ High   │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 26  │ ISS-25 — stable lambda factory in CardUiBridge                                 │ 3h          │ Medium │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 27  │ ISS-12 (Phase B) — split CardInput into CardInputMeta + CardInputFull          │ 6h          │ High   │
├─────┼────────────────────────────────────────────────────────────────────────────────┼─────────────┼────────┤
│ 28  │ ISS-17 — refactor TagUpSetPlotCardComputer traversal                           │ 3h          │ Medium │
└─────┴────────────────────────────────────────────────────────────────────────────────┴─────────────┴────────┘

 ---
Sprint 5 — Polish (P2/P3)

┌─────┬──────────────────────────────────────────────┬─────────────┬────────┐
│  #  │                    Issue                     │ Est. Effort │  Risk  │
├─────┼──────────────────────────────────────────────┼─────────────┼────────┤
│ 29  │ ISS-07 — FTS5 table for search               │ 4h          │ Medium │
├─────┼──────────────────────────────────────────────┼─────────────┼────────┤
│ 30  │ ISS-22 — BigDecimal conversion caching       │ 2h          │ Low    │
├─────┼──────────────────────────────────────────────┼─────────────┼────────┤
│ 31  │ ISS-23 — binary search for todayBucketIndex  │ 30min       │ None   │
├─────┼──────────────────────────────────────────────┼─────────────┼────────┤
│ 32  │ ISS-32 — stable sharedCallbacks with 19 deps │ 2h          │ Medium │
├─────┼──────────────────────────────────────────────┼─────────────┼────────┤
│ 33  │ ISS-38 — remove debug logs from hot paths    │ 1h          │ None   │
└─────┴──────────────────────────────────────────────┴─────────────┴────────┘

 ---
Verification Checklist

Per-Issue Verification

- ISS-02/03/42: Profile A templates/transactions/loan payments not visible in Profile B
- ISS-04/05: EXPLAIN QUERY PLAN shows index usage for filtered queries
- ISS-06: Dashboard feed DAO query returns exactly DASHBOARD_TRANSACTION_FEED_LIMIT rows
- ISS-11: Adding 1 transaction recomputes only data-dependent cards (verify via logcat tag on each computer's compute())
- ISS-13: CategorySpendingTrendCardComputer unit test with 200 categories + 50 results < 5ms
- ISS-14: Systrace shows CategorySpendingTrend making 1 DB call, not 12; CashFlowComparison 3, not 36
- ISS-15: Unit test for computeRecentBalances with 1,000 transactions produces identical output before/after; executes in ≤ 50% of original time
- ISS-16: Budget filter with 50 budgets × 10 filter IDs runs in < 1ms
- ISS-25/29: Layout Inspector recomposition count on all card bodies = 0 when lastUndoAction toggles
- ISS-30: Cold dashboard load produces ≤ 3 UI emissions (visible via Flow emission logging)
- ISS-31: Delete account #3; verify accounts #4+ do not recompose (Layout Inspector)
- ISS-39: When target base rate is missing, log warning appears and last valid rates are used
- ISS-43: Account balances identical regardless of active category/tag filter
- ISS-44: On first install with no network, cards render within 2s with "rates unavailable" indicator
- ISS-46/48: Attempting self-transfer or outside-to-outside transfer shows inline error
- ISS-50: Back gesture on Step 2+ of QuickAdd returns to previous step, not dismiss
- ISS-52: Card configured for "last 6 months" shows exactly 6 months of data
- ISS-53: $10,000 loan at 5% interest shows ~$10,500 remaining after 1 year, not $10,000
- ISS-54: Running balance with "Food" filter starts from filtered baseline, not global account balance                                                                                
                                                                                                                                                                    
End-to-End Performance Targets                                                                                                                                      
                                                                                                                                                                    
- Dashboard cold load: first meaningful paint < 500ms                                                                                                               
- Add transaction → dashboard updates in < 300ms                                                                                                                    
- Scroll through 15 cards: 0 dropped frames (Systrace / Perfetto)                                                                                                   
- Memory during dashboard idle: < 80 MB heap for card render data                                                                                                   
                                                                                                                                                                    
Test Suite                                                                                                                                                          
                                                                                                                                                                    
# All unit tests must pass after each sprint                                                                                                                        
./gradlew testDebugUnitTest                                                                                                                                         
                                                                                                                                                                    
# Specific card computer tests                                                                                                                                      
./gradlew testDebugUnitTest --tests "*.CategorySpendingTrendCardComputerTest"                                                                                       
./gradlew testDebugUnitTest --tests "*.CashFlowComparisonCardComputerTest"                                                                                          
./gradlew testDebugUnitTest --tests "*.PlannedPaymentsCardComputerTest"                                                                                             
./gradlew testDebugUnitTest --tests "*.CardAggregationServiceTest"                                                                                                  
                                                                                                                                                                    
# DB migration test (must pass for ISS-04, ISS-05, ISS-08)                                                                                                          
./gradlew connectedDebugAndroidTest --tests "*.AppDatabaseMigrationTest"                                                                                            
                                                                                                                                                                    
# Build                                                                                                                                                             
./gradlew assembleDebug                                                                                                                                             
