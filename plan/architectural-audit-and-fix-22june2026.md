# OssWallet — Comprehensive Architectural Audit & Remediation Plan
**Branch:** `feature/tagv43` · **Date:** 2026-06-22 · **Scope:** whole codebase

---

## Context

This document audits OssWallet for architectural flaws, anti-patterns, bugs, and bad practices beyond the dashboard-performance items already fixed on this branch. It was created in response to the user's observation that the change of base currency does not properly reflect across the app — that single bug exposed deeper structural issues across the currency subsystem, the data layer, the card framework, the ViewModel design, the cache strategy, and the service layer.

The audit was performed by three parallel exploration agents covering:
1. Currency / repository / database / DTO layers
2. ViewModels / card system / Compose UI / navigation / DI
3. Domain services / aggregation framework / use cases / tests / build / widgets / web server / backup

The goal is a single backlog the team can execute issue-by-issue, with enough context per item to estimate effort, justify the change, and verify the fix.

---

## How to read this plan

Each issue carries:
- **ID** — stable identifier (`ARC-NN`).
- **Severity** — `P0` (crash / data corruption / security) · `P1` (functional bug) · `P2` (correctness / maintainability risk) · `P3` (code smell).
- **Difficulty** — `trivial` (<30 min) · `easy` (<2 h) · `medium` (half day) · `hard` (multi-day refactor).
- **File** — primary path + line where known.
- **RCA** — why the issue exists.
- **Why fix** — the user-visible or business reason to invest the effort.
- **Impact** — side effects if left unfixed.
- **Fix** — concrete change.
- **Best practice** — the pattern the fix realizes.
- **Side effects of fix** — migrations / breaking changes / test rewrites.
- **Verify by** — how to confirm the fix works.

Items prefixed with **(UNVERIFIED)** are based on agent exploration but should be double-checked against the current file state before estimating.

Issues already fixed on this branch (currency snapshot propagation, FTS5 migration, ExchangeRateSnapshot adoption, CardRenderCache currency key, etc.) are intentionally omitted — git history records them.

---

## Curator's reassessment (added after honest review)

This section was added after re-reviewing the plan against the actual usage patterns of OssWallet (single-user mobile + optional web companion, solo-dev FOSS pace, low card-add frequency). Several entries in the catalog below are textbook "best practice" that don't justify their cost for *this* project. Where I gave you the dogmatic answer, I record the simpler pragmatic alternative here. **Nothing in the catalog is deleted** — this section reframes priority and adds caveats so you can pick the right answer instead of just executing the list.

### Items where the right answer is simpler than what the catalog says

- **ARC-08 (composite PK on `currency_rates`)** — **do the composite PK.** Multi-base caching is a product requirement. ARC-08 + ARC-09 + ARC-10 land together.
- **ARC-43 (convert every `object` service to DI)** — **over-broad**. The only service with a real problem is `CurrencyService` (mutable state — ARC-05). Pure-function services like `TimelineService`, `TransactionFilterService`, `BudgetService` (post-snapshot fix) gain nothing from DI. Fix ARC-05; leave the rest as `object` until a concrete need arises.
- **ARC-21 / ARC-22 (FilterEntity primitives only)** — **probably skip**. Room + a kotlinx-serialization TypeConverter for sealed classes is a common, supported pattern. The CLAUDE.md "no domain types in entities" rule is slightly dogmatic. Verify a TypeConverter for `TimelineRange` exists and works; if so, leave `FilterEntity` alone.
- **ARC-30 (split `CardInput` into typed subsets)** — **defer**. After the recent `distinctUntilChanged` + recompute-key tightening, the marginal win is allocation pressure only. Hard refactor for small ROI. Revisit only if memory profiling shows allocation is the bottleneck.

### Items explicitly postponed (decided 2026-06-23)

These are real issues with confirmed intent, but the user has chosen to defer them to a future cycle. Keep the catalog entries below for context; do not schedule into any current sprint.

- **ARC-31** — plugin pattern for cards. User wants it eventually; not now.
- **ARC-44** — simplify / replace the aggregation framework. User confirms it's overkill and causes bulky DB calls; tackling later.

**Note:** ARC-45 (typed `MetricKey` for SQL aliases) stays **in scope**. It's a silent-corruption bug that can be fixed independently of the ARC-44 framework decision — the same alias-to-extractor coupling is needed whether the framework lives or is replaced.

### Items where I led with the wrong option

- **ARC-75 (pie/treemap mapping in composable):** lead with **Option B** (wrap with `remember(renderData, selectedMetric) { ... }`) — 3 lines, no data-model change, ~95% of the benefit. Option A (move into the computer) only if you want cleaner architecture for unrelated reasons.
- **ARC-78 (`graphicsLayer` gating):** lead with **Option B** (`derivedStateOf` so the layer body short-circuits when `rotation == 0f`). Option A (conditionally apply the whole layer) risks breaking the flip animation's first frame. Drop or de-emphasize A.
- **ARC-76 (`.sample(80)` on `cardDataFlow`):** ~~the fix is correct but `sample` drops trailing emissions~~ **WITHDRAWN 2026-06-23**. Finance app — every computed value should be visible the instant it exists, no debounce delay. The actual scroll-jank root causes (ARC-74/75/77) plus per-card `derivedStateOf` slicing in `AeonCardList` already make per-card emissions cheap.

### Resolved product calls (decided 2026-06-22)

- **ARC-18 (per-profile vs shared categories):** profiles = different contexts for the same user → categories stay shared (current behavior is correct). Action narrows to documenting this decision in code + `.gemini/database.md`; no migration work.
- **ARC-29 (versioned backup):** option (a) — full backward-compat with up-migration on import. Effort bumps from `medium` to `hard`; introduces a `BackupMigration_NM_to_(N+1)M` chain.

### Items where I probably overstated severity

- **ARC-25 (`AccountDao.upsertAccount` race):** confirmed P3. Offline-only app, single human writer at a time — race is theoretical. Action: doc-comment the single-writer assumption; no code change.
- **ARC-49 ("multiple aggregation services overlap"):** vague — may be fine. Probably P3 or drop.
- **ARC-55 (Aeon naming):** P3 is right; honestly cosmetic; likely never worth doing.

### Items to drop / verify before acting

- **ARC-54 (`CardWithCallbacks.ComputeError` "possibly undefined"):** confirmed it IS handled at `AeonCardList.kt:154`. Drop — agent hallucination I didn't catch.
- All entries tagged **(UNVERIFIED)** in the catalog — confirm file/line still match before estimating; some are agent inferences from limited reads.

### Recommended actual execution order (overrides the Sprint A–E table below)

1. **Data isolation profileId fixes** (ARC-13 → ARC-19) — non-negotiable.
2. **Currency anchoring** (ARC-01, ARC-02, ARC-04, ARC-09, ARC-12) — finishes the work already started this session.
3. **Scroll-jank trio in one PR** (ARC-74 + ARC-77 + **ARC-75 Option B**) — one afternoon, biggest user-visible improvement.
4. **ARC-76** — withdrawn. No debounce on financial data emissions.
5. **ARC-05** (remove `CurrencyService` mutable state) — important; skip the rest of ARC-43.
6. **ARC-28** `@Transaction` audit — easy correctness win.
7. **ARC-39** (channelFlow race) — before any further dashboard work.
8. **ARC-45** (typed `MetricKey`) — small, self-contained, kills the silent-zero bug; doesn't depend on the postponed ARC-44.
9. Everything else: backlog, revisit only when symptoms appear.

---

## Cross-cutting themes

Many individual issues are symptoms of a small number of root architectural decisions. Fixing the roots is more valuable than fixing each symptom.

1. **Anchorless rate transport.** `CurrencyRepository.ratesFlow` emits a bare `Map<String, BigDecimal>` without the base currency that produced it. Every downstream assumption about what that map means is a guess. (See ARC-01, ARC-02, ARC-03.)
2. **"Sync" DAO overloads without profileId.** A whole class of DAO methods (`getAllAccountsSync`, `getStarredTemplates`, `getAllPayments`, `getTransactionById`, etc.) bypass the profile filter. (See ARC-13 — ARC-19.)
3. **Object-singleton services.** Almost every domain service is a Kotlin `object`. Some hold global mutable state (`CurrencyService.precision`). This blocks DI, fakes, and dispatcher injection. (See ARC-43.)
4. **CardType "ceremony" (15 files per new card).** CardType / CardConfig / CardRenderData / CardComputer / CardCallbacks / CardWithCallbacks / CardUpdateHandler / CardUiBridge / UnifiedCardRenderer / CardRepositoryImpl / CardLayoutViewModel — each sealed-class hierarchy must be hand-edited. The plugin pattern is implemented six times instead of once. (See ARC-30 — ARC-34.)
5. **Imperative `channelFlow` + `AtomicInteger` for card emission.** Reinvents what `MutableSharedFlow`/`merge`/`combine` does, and currently has a real race window. (See ARC-39.)
6. **God objects.** `CardInput` (14 fields), `TransactionViewModel` (~759 lines), `BaseViewModel` (cross-cutting settings). (See ARC-30, ARC-36, ARC-38.)
7. **Silent failure culture.** `runCatching {}.getOrDefault(emptyMap())` appears throughout repos, services, widgets, and backup code. Failures degrade silently into wrong values. (See ARC-04, ARC-46, ARC-58, ARC-60.)

---

# Layer 1 — Currency & Financial Correctness

## ARC-01 · `CurrencyRepository.ratesFlow` is unanchored [P0 · medium]
- **File:** `data/repository/CurrencyRepositoryImpl.kt:25-28`
- **RCA:** `dao.observeAllRates().map { ... currencyCode to rate }` discards the `baseCurrency` column on every row. The Flow emits a `Map<String, BigDecimal>` with no indication of what currency the rates are denominated in.
- **Why fix:** Every downstream consumer (ComputationRepositoryImpl, BudgetService, LoanService, CardAggregationService) has to *guess* the anchor. Most guess "USD" via the deprecated overload, which silently corrupts numbers for users whose base ≠ USD.
- **Impact:** Conversion factors computed against the wrong anchor produce financial values off by the cross-rate (often 10–30%).
- **Fix:** Change `ratesFlow` to `Flow<ExchangeRateSnapshot>` (or `Flow<Pair<String, Map<...>>>`). Read the `baseCurrency` column from any one row (they share it after the per-base partitioning fix in ARC-13).
- **Best practice:** Side-channel context that's required to interpret a value travels *with* that value, not in parallel.
- **Side effects of fix:** Touches every caller of `ratesFlow`; small but wide. Combines naturally with ARC-13.
- **Verify by:** unit test — write rates with base="EUR", observe flow, assert `snapshot.anchor == "EUR"`.

## ARC-02 · Re-base failure silently returns wrong-anchored map [P0 · easy]
- **File:** `data/repository/ComputationRepositoryImpl.kt:30-38`
- **RCA:** If `pivotRate` is null or zero, `combine` returns the *original* `rateMap` (still anchored to whatever the DB was last fetched with) but `ComputationRepositoryImpl.exchangeRatesSnapshot` now labels it with `selectedCurrency` as the anchor. Producer and label disagree.
- **Why fix:** This is the literal cause of "changing base currency doesn't reflect on the dashboard" when the new base currency is missing from the rate map.
- **Impact:** Silent financial miscalculation until a successful re-fetch.
- **Fix:** When rebasing fails, emit `emptyMap()` (caller already gates UI on `hasLoadedExchangeRates`) and surface the failure via a new `Flow<RateRefreshError?>` on the repository. Do **not** return a stale map labeled with the new anchor.
- **Best practice:** Fail closed; report the failure; never return mislabeled data.
- **Side effects of fix:** UI gate must wait for next refresh; consider showing a banner when error flow fires.
- **Verify by:** unit test — feed rates without the new pivot, assert downstream `exchangeRatesSnapshot.rates.isEmpty()` and that an error is emitted.

## ARC-03 · `CurrencyRepositoryImpl.getExchangeRates()` parallel `Double` path [P1 · medium]
- **File:** `data/repository/CurrencyRepositoryImpl.kt:59-101`
- **RCA:** Returns `Map<String, Double>` and performs its own re-basing with `it.value / newBaseRate` — no rounding mode, double precision. ComputationRepositoryImpl uses `BigDecimal.divide(..., 10, HALF_UP)`. Two divergent implementations of the same math.
- **Why fix:** Fixes applied to one path don't apply to the other. Web routes and the legacy `convert()` API silently disagree with the rest of the app.
- **Impact:** Inconsistent conversions across screens; potential cents-of-rounding mismatch between Web UI and Android UI for the same transaction.
- **Fix:** Deprecate `getExchangeRates(...): Map<String, Double>` and `convert(...): Double`. Web routes should use `ComputationRepository.exchangeRatesSnapshot` instead. Once no caller remains, delete.
- **Best practice:** Single source of truth for financial math. `BigDecimal` everywhere; never `Double`.
- **Side effects of fix:** Web `BudgetRoutes` needs the same snapshot pattern recently applied to `BudgetService`. Provider classes that return `Map<String, Double>` need BigDecimal versions.
- **Verify by:** grep for `getExchangeRates(` returning `Double`; should be zero hits after refactor.

## ARC-04 · `runCatching {}.getOrDefault(emptyMap())` swallows rate-fetch failures [P1 · easy]
- **File:** `data/repository/ComputationRepositoryImpl.kt:56-61`
- **RCA:** The reactive collector that refetches on currency change does `runCatching { currencyRepository.getExchangeRates(currency) }` with no logging, no UI surface, no error flow.
- **Why fix:** A user who changes base currency on a flaky network sees the wrong dashboard with no indication anything failed.
- **Impact:** Combined with ARC-02 this is the second leg of the silent-wrong-data problem.
- **Fix:** Replace with `.onFailure { Timber.e(it, "rate refresh") }` and emit to a `MutableSharedFlow<RateRefreshError>` consumed by a snackbar host in the UI.
- **Best practice:** Errors should be either retried (auto), surfaced (UI), or logged (telemetry). Never silently dropped.
- **Side effects of fix:** New shared flow, new UI component to display errors.
- **Verify by:** disable network → change base currency → snackbar appears.

## ARC-05 · `CurrencyService` global mutable `precision` / `activeSystem` [P1 · medium]
- **File:** `domain/service/currency/CurrencyService.kt` (top-of-file `var` properties)
- **RCA:** Singleton `object` with mutable state set imperatively by `MainActivity` via `LaunchedEffect`. Anything that runs before that effect (widgets, workers, deeplinks) reads the default.
- **Why fix:** Test ordering bugs, widget format bugs, background-task currency formatting that doesn't match user preference.
- **Impact:** Widget shows 2-decimal amount when user has set 0 decimals; backup export has different precision than displayed.
- **Fix:** Convert `CurrencyService` to a `@Singleton class` injected via Hilt. Replace `precision` with a constructor-injected `Flow<FormatConfig>` (settings-backed). All callers thread the config through.
- **Best practice:** No process-wide mutable singletons; configuration travels through DI, not statics.
- **Side effects of fix:** Wide. Every callsite (`CurrencyService.precision`, `CurrencyService.formatAmount(...)`) becomes instance-based.
- **Verify by:** start a widget worker with the app process cold — verify precision matches user setting (currently fails).

## ARC-06 · Deprecated `Map<String, BigDecimal>` overloads still exist [P1 · easy]
- **File:** `domain/service/currency/CurrencyService.kt:215-291` (`@Deprecated("Pass ExchangeRateSnapshot")`)
- **RCA:** Each deprecated overload constructs `ExchangeRateSnapshot(anchor = "USD", rates = map)`. The anchor is wrong for any non-USD user.
- **Why fix:** As long as these exist, code can quietly slip back to the broken path (a single missed callsite re-introduces the bug ARC-02 was about).
- **Impact:** Latent silent-wrong-value time bomb.
- **Fix:** Mark `error` level on `@Deprecated`. Migrate any remaining callers (web routes, providers, tests). Then delete the overloads.
- **Best practice:** "Deprecated" only buys time. Set a removal deadline and enforce it.
- **Side effects of fix:** Build break on remaining callers — desired (forces migration).
- **Verify by:** `grep -rn "exchangeRates: Map<String, BigDecimal>" --include="*.kt"` → zero hits.

## ARC-07 · `ExchangeRateSnapshot.EMPTY` hardcodes `anchor = "USD"` [P3 · trivial]
- **File:** `domain/service/currency/ExchangeRateSnapshot.kt:10`
- **RCA:** Sentinel value chose USD arbitrarily. With an empty `rates` map all conversions short-circuit to identity, so it doesn't currently produce wrong values — but it primes the same bug if `rates` is ever non-empty while anchor remains the sentinel.
- **Why fix:** Sentinel values that lie about their type are a refactor hazard.
- **Fix:** Make `EMPTY` private; expose `fun empty(anchor: String) = ExchangeRateSnapshot(anchor, emptyMap())`. Callers pass the current `selectedCurrency`.
- **Best practice:** Sentinels should be unambiguous (e.g., `anchor = ""`) or parameterized.
- **Side effects of fix:** Few callers; trivial.
- **Verify by:** grep `ExchangeRateSnapshot.EMPTY` → migrate.

## ARC-08 · `CurrencyRateEntity` PK = `currencyCode` only [P0 · medium]
> **Curator note (user-confirmed 2026-06-22):** multi-base caching IS a requirement. Do the composite PK + migration. ARC-09 (scoped delete) and ARC-10 (index) become *mandatory companions*, not alternatives — without them the composite PK is unusable.
- **File:** `data/local/entity/CurrencyRateEntity.kt:8-10`
- **RCA:** Primary key is just `currencyCode`. The `baseCurrency` column is stored but cannot uniquely identify the row. With `OnConflictStrategy.REPLACE` (which `replaceRates` uses), fetching EUR rates overwrites the row that USD rates wrote earlier under the same key.
- **Why fix:** Foundational correctness for the rate cache. Without it, ARC-02 cannot be fully fixed.
- **Impact:** Concurrent fetches for different base currencies clobber each other; cache is never coherent.
- **Fix:** Composite primary key `(currencyCode, baseCurrency)`, or surrogate `id` with unique index on the pair. Add migration to drop+recreate (cache is regeneratable from network).
- **Best practice:** Primary key must include every dimension that distinguishes rows.
- **Side effects of fix:** Migration (cache-wipe acceptable — rates are re-fetchable).
- **Verify by:** insert USD-based and EUR-based snapshots in sequence; assert both survive.

## ARC-09 · `CurrencyDao.replaceRates()` deletes globally [P0 · easy]
- **File:** `data/local/dao/CurrencyDao.kt` (`replaceRates` / `deleteAllRates`)
- **RCA:** Whole-table `DELETE` followed by re-insert. Two concurrent base-currency fetches race; one wipes the other's freshly-written rows.
- **Why fix:** Even after ARC-08, the wipe still nukes the wrong rows.
- **Fix:** Replace with `@Transaction` containing `DELETE FROM currency_rates WHERE baseCurrency = :base` then insert. Or use upsert semantics on the composite key.
- **Best practice:** Mutations are scoped to the context that produced them. No "delete everything, write new" patterns on shared tables.
- **Side effects of fix:** New DAO signature; existing callers pass `baseCurrency`.
- **Verify by:** instrumented test — fetch EUR, fetch USD concurrently, assert both rate sets persist.

## ARC-10 · No index on `currency_rates.baseCurrency` [P2 · trivial]
- **File:** `data/local/entity/CurrencyRateEntity.kt`
- **RCA:** After ARC-09's `WHERE baseCurrency = ?` is added, queries scan the table.
- **Fix:** Add `@Index("baseCurrency")` (or composite once PK is reworked).
- **Verify by:** `EXPLAIN QUERY PLAN` shows index usage.

## ARC-11 · `CurrencyRateEntity.rate: String` parsed without validation [P2 · trivial]
- **File:** `data/local/entity/CurrencyRateEntity.kt:12-13` & `CurrencyRepositoryImpl.kt:27`
- **RCA:** `it.rate.toBigDecimal()` throws on a non-numeric DB value. No TypeConverter, no try-catch.
- **Why fix:** Backup imports or schema corruption become app-startup crashes.
- **Fix:** Use a TypeConverter that returns `null` on parse failure; treat null as missing rate.
- **Verify by:** insert a row with `rate = "abc"`; cold start should not crash.

## ARC-12 · `getExchangeRates()` silently serves stale on fetch failure [P1 · easy]
- **File:** `data/repository/CurrencyRepositoryImpl.kt:84-100`
- **RCA:** If the network fetch fails the function returns whatever `localRates` had — possibly 30 days old — and the caller cannot distinguish "fresh" from "stale-because-network-down".
- **Why fix:** Financial apps must communicate staleness.
- **Fix:** Return `Result<RatesSnapshot>` where `RatesSnapshot` carries a `fetchedAt` timestamp; caller decides whether stale is acceptable.
- **Verify by:** disable network → invoke `getExchangeRates` → result contains `fetchedAt` ≪ now.

---

# Layer 2 — Data Isolation & ProfileId

## ARC-13 · `AccountDao.getAccountById(id)` missing profileId [P0 · easy]
- **File:** `data/local/dao/AccountDao.kt:15-16` (UNVERIFIED — agent finding; confirm line)
- **RCA:** Account IDs are strings; nothing forces them globally unique across profiles. Lookup by ID alone may return another profile's record.
- **Why fix:** Cross-profile read can silently authorize a cross-profile write (edit/delete via the returned entity's id).
- **Fix:** `suspend fun getAccountById(id: String, profileId: Int): AccountEntity?` with `WHERE id = :id AND profileId = :profileId`.
- **Best practice:** Every retrieval is profile-scoped. ID lookups carry profile.
- **Side effects of fix:** Update `AccountRepositoryImpl` and call sites in `TransactionEntryViewModel`, etc.
- **Verify by:** instrumented test — create same accountId in two profiles; lookup with profile A returns only A.

## ARC-14 · `TransactionDao.getTransactionById(id)` missing profileId [P0 · easy]
- **File:** `data/local/dao/TransactionDao.kt:55-56` (UNVERIFIED)
- **RCA:** Same shape as ARC-13.
- **Fix:** Add `AND profileId = :profileId` and a `profileId` parameter.
- **Verify by:** test as above.

## ARC-15 · Multiple `*Sync()` DAO methods omit profileId [P0 · easy each]
- **Files (UNVERIFIED — verify lines exist):**
  - `AccountDao.getAllAccountsSync()`
  - `BudgetDao.getAllBudgetsSync()`
  - `LoanDao.getAllLoansSync()`
  - `PlannedPaymentDao.getAllPlannedPaymentsSync()`
- **RCA:** Originally added for backup/export code paths that "needed everything"; now they exist as a footgun.
- **Why fix:** A future caller (backup, debug tool, seed) gets all profiles' data without realizing.
- **Fix:** Either (a) require `profileId` parameter, OR (b) keep them but rename to `dangerously_GetAcrossAllProfiles_Sync` and document that only the backup-of-everything path may call them. Option (a) is preferred.
- **Verify by:** grep usages; ensure every call passes profileId.

## ARC-16 · `LoanPaymentDao.getAllPayments()` no profile scope [P0 · trivial]
- **File:** `data/local/dao/LoanPaymentDao.kt:19-20` (UNVERIFIED)
- **Fix:** Drop the unsafe overload; use `getAllPaymentsByProfile(profileId)`.
- **Verify by:** grep — no callers of `getAllPayments()` remain.

## ARC-17 · `TemplateDao.getStarredTemplates()` no profile scope [P0 · trivial]
- **File:** `data/local/dao/TemplateDao.kt:17-18` (UNVERIFIED)
- **Fix:** Same as ARC-16.

## ARC-18 · `CategoryDao` has no profile dimension at all [P1 · hard]
> **Curator note (user-confirmed 2026-06-22): RESOLVED — categories stay shared.** Profiles represent different contexts for the *same user* (personal / business / travel etc.), not different people. Shared categories are the correct design. **Action:** document this decision in `CategoryEntity` KDoc and in `.gemini/database.md` so a future contributor doesn't re-open the question. Drop the engineering work; keep the documentation task only.
- **File:** `data/local/dao/CategoryDao.kt`, `data/local/entity/CategoryEntity.kt`
- **RCA:** Categories are global. System categories (IDs <1000) are intentionally shared. Custom categories should arguably be per-profile but aren't.
- **Why fix:** Two profiles with the same custom "Food" category create id collisions in cards keyed by `categoryId`. The dashboard's category card silently aggregates across profiles for shared ids if it ever queries the global list.
- **Fix:** Add nullable `profileId: Int?` to `CategoryEntity` (null = system); migrate to add column. Queries become `WHERE profileId = :pid OR profileId IS NULL`.
- **Best practice:** Decide and *document* whether each entity is system-wide, profile-scoped, or both. Don't leave it ambiguous.
- **Side effects of fix:** Migration; query rewrites; profile-switch caches need clearing.
- **Verify by:** create same-name category in two profiles; each profile sees only its own.

## ARC-19 · `QueryCache` not cleared on profile switch [P1 · medium]
- **File:** `di/AppModule.kt` (provider) + callers (UNVERIFIED — confirm singleton lifetime)
- **RCA:** Process-singleton cache keyed by query parameters. Profile switch doesn't trigger eviction; if two profiles happen to produce the same query key, the second profile reads the first's results.
- **Why fix:** Subtle cross-profile bleed especially with similar account/category IDs.
- **Fix:** Wire `QueryCache` to observe `settingsRepository.currentProfileId` and `cache.clear()` on change. Or include `profileId` in the cache key (better — same fix as `CardRenderCache`).
- **Verify by:** integration test — query in profile A, switch to B, query same shape, assert distinct result.

## ARC-20 · `CardRenderCache` flicker on save (already partial mitigation) [P2 · medium]
- **File:** `domain/cache/CardRenderCache.kt` + `DashboardViewModel.kt:297-305`
- **RCA:** "Show stale, then jump to fresh" pattern. Currency-key fix landed in this session, but the timestamp/version is not exposed to UI; user sees totals flip without explanation.
- **Why fix:** Breaks perceived reactivity right after the user saves a transaction.
- **Fix:** Add `lastComputedAt: Long` per (profileId, currency) entry. UI shows a subtle "Updating…" pill or shimmer overlay on cards whose data is older than the user's latest transaction write.
- **Best practice:** Stale-while-revalidate must signal "revalidating" to the user.
- **Verify by:** save a transaction; assert UI shows updating indicator until cardDataFlow completes.

---

# Layer 3 — Database Schema, DAOs, Migrations

## ARC-21 · `FilterEntity` embeds domain types directly [P2 · medium]
> **Curator note:** probably skip. Room + kotlinx-serialization TypeConverters supports sealed-class storage well. The "no domain types in entities" CLAUDE.md rule is slightly dogmatic. Verify the TypeConverter exists; if so, leave as-is.
- **File:** `data/local/entity/FilterEntity.kt`
- **RCA:** Stores `TransactionType`, `PaymentMethod`, `TransactionStatus`, `TimelineRange` as fields. Per CLAUDE.md the data layer must not import domain models.
- **Why fix:** Domain enum renames force a schema migration. Tight coupling prevents independent evolution.
- **Fix:** Store as serialized strings; map in `FilterMapper`. Sealed `TimelineRange` → JSON via TypeConverter.
- **Best practice:** Entities hold primitives + ids; mappers translate.
- **Side effects of fix:** Migration to alter column types (or recreate with copy).
- **Verify by:** delete every `import com.osscabinet.ossWallet.domain.model` from `data/local/entity/` — should compile.

## ARC-22 · `FilterEntity.timelineRange: TimelineRange` requires a TypeConverter not obviously registered [P2 · easy]
- **File:** `data/local/entity/FilterEntity.kt:15` + `data/local/Converters.kt` (UNVERIFIED)
- **Fix:** Confirm the converter exists. If not, replace with `timelineRangeJson: String`.
- **Verify by:** room schema export shows the column as TEXT.

## ARC-23 · Missing indices on hot WHERE columns [P2 · trivial]
- **Files:** `TransactionEntity` (`currency`), `BudgetEntity` (`status`), `LoanEntity` (`status`), `PlannedPaymentOccurrenceEntity` (`status`).
- **Why fix:** Filters on these columns table-scan today.
- **Fix:** Add `@Index(value=["currency"])` etc. plus migration.
- **Verify by:** `EXPLAIN QUERY PLAN SELECT ... WHERE status = ?` shows USING INDEX.

## ARC-24 · `TransactionDao.getTransactionsWithTag()` uses `LIKE '%x%'` on denormalized tags [P2 · medium]
- **File:** `data/local/dao/TransactionDao.kt:112-116`
- **RCA:** Legacy field `tags: String` (comma-joined) survives even though `transaction_tag_cross_ref` exists.
- **Why fix:** Full table scan per tag lookup; the proper normalized table is already there.
- **Fix:** Rewrite to `JOIN transaction_tag_cross_ref ON ... JOIN tags ON ... WHERE tags.name = :name AND transactions.profileId = :pid` (note: also fixes the missing profileId, ISS-03 from earlier audit).
- **Side effects of fix:** Eventually drop the legacy column (ARC-26).
- **Verify by:** EXPLAIN shows index seeks on `transaction_tag_cross_ref`.

## ARC-25 · `AccountDao.upsertAccount` race [P1 · medium]
> **Curator note (user-confirmed 2026-06-22): downgrade to P3.** OssWallet is offline-only; the human user interacts from one surface at a time (mobile UI OR web UI). Bulk imports run from one source. The concurrent-upsert race is theoretical, not practical. **Action:** add a one-line code comment near `upsertAccount` documenting "single-writer assumption" so it isn't accidentally violated when (if) sync ever lands; otherwise no work.
- **File:** `data/local/dao/AccountDao.kt:18-22` (UNVERIFIED)
- **RCA:** String PK + REPLACE; concurrent edits silently last-write-wins.
- **Fix:** Mutex per accountId in `AccountRepositoryImpl`, OR add `version: Int` column with optimistic locking (`WHERE id=? AND version=?`).
- **Verify by:** instrumented test launches two concurrent upserts; both observable effects survive (with optimistic lock) or one fails cleanly.

## ARC-26 · `TransactionRepositoryImpl.syncTransactionTags()` not atomic, leaves dual representation [P2 · medium]
- **File:** `data/repository/TransactionRepositoryImpl.kt:188-220`
- **RCA:** Tags exist both as the legacy `tags: Set<String>` on `TransactionEntity` and the normalized `transaction_tag_cross_ref` table. Sync is two separate DAO calls; partial failure leaves them divergent.
- **Fix:** Wrap in `@Transaction`. Then delete the legacy `tags` column entirely (migration).
- **Best practice:** Single source of truth per concept. Eliminate redundant representations.
- **Verify by:** instrumented test — kill the coroutine mid-sync; subsequent read shows consistent state from cross-ref only.

## ARC-27 · `BudgetRepositoryImpl.delete()` manually cleans filter that CASCADE already handles [P2 · trivial]
- **File:** `data/repository/BudgetRepositoryImpl.kt:44-50`
- **RCA:** Manual `getFilterById → deleteFilter` after `deleteBudget`. Foreign key on `BudgetEntity → FilterEntity` already has `ON DELETE CASCADE` (per agent finding — confirm).
- **Fix:** Delete the manual cleanup.
- **Verify by:** delete a budget; the filter row disappears from `transaction_filters`.

## ARC-28 · No `@Transaction` on multi-DAO writes (backup import, transfer create, planned-payment confirm) [P0 · easy each]
- **Files (UNVERIFIED — confirm):** `data/repository/BackupRepositoryImpl.kt:150-400`, `domain/service/management/TransactionManager.kt` (transfer + linked transaction), `PlannedPaymentService` (occurrence + transaction insert).
- **RCA:** Sequential DAO calls; crash mid-sequence leaves orphans.
- **Why fix:** Data integrity around the most user-visible operations (saving a transfer, restoring a backup).
- **Fix:** Wrap each multi-step write in `db.withTransaction { ... }`.
- **Verify by:** force-throw between steps; assert all rows reverted.

## ARC-29 · Backup ZIP carries no `schema_version` [P1 · medium]
> **Curator note (user-confirmed 2026-06-22): option (a) — full backward-compat with up-migration on import.** Reasoning recorded: users may restore old backups across app upgrades; refusing them is unkind. **Implementation outline:**
> 1. Embed `schema_version.txt` in the export ZIP (DB version + app version + export format version).
> 2. On import: parse versions; if older, run a chain of in-memory "backup migration" transformations (each one bumps the data to the next version) before insertion.
> 3. If newer than the running app: refuse with a clear error ("Backup made with newer app version; please update").
> 4. Bump migration effort up — this is now `hard`, not `medium`. Maintain a `BackupMigration_NM_to_(N+1)M` chain alongside the Room `MIGRATION_N_(N+1)` chain.
> 5. Tests: round-trip every supported old version → import → assert all rows present with expected current-version shape.
- **File:** `data/repository/BackupRepositoryImpl.kt` (export + import)
- **RCA:** Export writes CSV; import assumes columns match. Adding a column means old backups silently drop the field, NULL-fill it, or crash.
- **Fix:** Add `schema_version.txt` to the ZIP with the current DB version. On import, refuse if newer; offer up-migration if older.
- **Best practice:** Versioned data formats. Treat backups like API payloads.
- **Verify by:** export at v18; bump DB to v19; import — should reject or migrate, not silently lossy.

---

# Layer 4 — Cache, Concurrency, Coroutines

## ARC-30 · `CardInput` god object drives unnecessary recomputes [P2 · hard]
> **Curator note:** defer. After the recent `distinctUntilChanged` + recompute-key tightening, the remaining win is mostly allocation pressure. Hard refactor for small ROI. Revisit only if memory profiling shows allocation as the bottleneck.
- **File:** `domain/card/CardInput.kt`
- **RCA:** 14 fields. Any change in any field re-equates the data class as non-equal (despite `data class` equality, new list references from Room emissions trip equality). Every card recomputes.
- **Why fix:** Renaming an account triggers the `TagUpSetPlot` to recompute. Memory pressure from per-emission allocation.
- **Fix:** Split into `CardInputCore` (profileId, filter, screenContext) + `CardInputData` (transactions, accounts, categories, tags) + `CardInputPlanning` (budgets, loans, planned payments) + `CardInputRates`. Each computer declares which inputs it consumes. Registry routes only the needed subset.
- **Best practice:** Inputs are typed by need; computers are honest about their dependencies.
- **Side effects of fix:** Touches every computer + registry + DashboardViewModel assembly.
- **Verify by:** add a Log to each computer's compute; rename an account → only computers consuming accounts should fire.

## ARC-31 · `CardType` plugin pattern spread across 15 files [P2 · hard]
> **Curator note: POSTPONED (decided 2026-06-23).** User confirms plugin pattern is desired (future contributors should be able to add custom cards), but defers the work to a future cycle. Catalog entry kept for context.
- **File:** CLAUDE.md explicitly documents this. Files: `CardType.kt`, `CardConfig.kt`, `CardRenderData.kt`, `CardComputer.kt` (new), `CardComputerRegistry.kt`, `CardUpdateHandler.kt`, `CardCallbacks.kt`, `CardWithCallbacks.kt`, `CardUiBridge.kt`, `UnifiedCardRenderer.kt`, `XxxCard.kt` (new), `CardRepositoryImpl.kt` (three `reorder*` branches), `CardLayoutViewModel.kt`, plus nav routes.
- **RCA:** Sealed-class polymorphism repeated six times — each subsystem (config, render, compute, update, callbacks, renderer) has its own parallel sealed hierarchy keyed by the same `CardType`.
- **Why fix:** Adding a card is a multi-hour ceremony with a silent runtime-only failure mode (CLAUDE.md notes "missing any causes a silent runtime crash on card reorder, not a compile error").
- **Fix:** One per-card "module" object that owns config codec, computer, callbacks factory, and renderer composable. Registry holds `Map<CardType, CardModule>`. Adding a card becomes one new file + one registry entry.
- **Best practice:** True plugin architecture; per-type definition lives in one place.
- **Side effects of fix:** Significant refactor; preserve behavior with integration tests.
- **Verify by:** delete one card's entry from the registry; compile error (currently: runtime crash).

## ARC-32 · `CardCallbacks` / `CardWithCallbacks` / `UnifiedCardRenderer` triple duplication [P2 · medium]
> **Curator note (2026-06-23): DEFERRED to a focused session.** Real refactor — needs signature changes on every card composable (NetWorthCard, CashFlowCard, etc.), inline lambda fields into 15 CardWithCallbacks subtypes, rewrite CardUiBridge's construction site, and re-validate UnifiedCardRenderer's dispatch. ~3-4 hours of focused work. ROI is purely DX (one fewer file to edit per new card); no user-visible behavior change. Better as its own PR than mixed into broader Sprint B work where it could mask regressions. Pre-requisite: a Konsist parity test (ARC-31-alt) to catch missing branches before/after.
- **File:** `ui/components/cards/CardCallbacks.kt`, `CardWithCallbacks.kt`, `UnifiedCardRenderer.kt`
- **RCA:** Three sealed hierarchies enumerating the same 15 card variants. Changing one signature requires three edits.
- **Fix:** Collapse: `CardWithCallbacks` already pairs config + render data + callbacks. Make it the only sealed hierarchy. `UnifiedCardRenderer` dispatches on `cardWithCallbacks`. Drop `CardCallbacks` as a separate type — its variants are accessible through their containing `CardWithCallbacks`.
- **Verify by:** grep `sealed class CardCallbacks` → zero hits.

## ARC-33 · `CardUpdateHandler` has 34+ per-card methods [P1 · medium]
- **File:** `domain/card/CardUpdateHandler.kt`
- **RCA:** One `updateXxxConfig(...)` per per-card setting. Adding a setting to a card means adding a method.
- **Fix:** Generic `<C : CardConfig> updateCard(cardId: String, transform: (C) -> C)` with reified type bound. Repository's typed `when` becomes one cast.
- **Verify by:** file line count drops to <50; new card needs zero edits to this file.

## ARC-34 · CardConfig sealed subclasses contain duplicate/dead fields [P3 · trivial each]
- **Files:** `domain/model/settings/CardConfig.kt`
  - `NetWorthCardConfig.profileId` — unused (line ~134) (UNVERIFIED)
  - `CategoryPieChartCardConfig` and `CategoryTreemapCardConfig` — identical field sets, should share a base
  - `PlannedPaymentsCardConfig.daysAhead` `@Transient` — purpose unclear
  - `PlannedPaymentsCardConfig.showOverdue` — legacy duplicating `viewMode`
  - `AccountHeroCardConfig.filterRules = CardFilterRules()` (empty) where every other card uses `.DEFAULT`
- **Fix:** Remove or document each. Migration only if the field is persisted in `cards.json`.
- **Verify by:** delete; rebuild; run app; nothing references missing field.

## ARC-35 · `ScreenContext` enum used in 56 `when`/`if` sites [P2 · medium]
- **File:** `domain/model/settings/ScreenContext.kt`
- **RCA:** Adding a screen requires touching every dispatch site. Compiler doesn't help.
- **Fix:** Convert to sealed class. Some variants can carry data (e.g., `AccountDetail(val accountId: String)`). Exhaustive `when` flags missing branches.
- **Verify by:** add a new `data object` variant; compiler points to every unhandled branch.

## ARC-36 · `BaseViewModel` exposes settings/breakdown flags every VM doesn't need [P2 · easy]
- **File:** `ui/util/BaseViewModel.kt`
- **RCA:** `recentActivitiesBreakdown`, `shortlistedCurrencies`, etc., live in the base class. Most ViewModels don't use them.
- **Fix:** Move these to specific ViewModels or to a `DashboardSettingsCompositor`.
- **Verify by:** `BaseViewModel` only contains profile-id, dispatcher, and protected helpers.

## ARC-37 · `cardScreenContext` + `CardUpdateHandler` mixin pollutes non-card ViewModels [P2 · medium]
- **File:** `ui/util/BaseViewModel.kt` plus VM hierarchy
- **RCA:** ISP violation. Settings VM has no cards, but might be forced to implement card handler if it extends BaseViewModel.
- **Fix:** Extract `CardAwareViewModel` interface; only `DashboardViewModel`, `AnalyticsViewModel`, `AccountDetailViewModel` implement.
- **Verify by:** non-card VMs don't reference `CardUpdateHandler`.

## ARC-38 · `TransactionViewModel` is a 759-line god class [P1 · hard]
- **File:** `ui/screens/transactions/TransactionViewModel.kt`
- **RCA:** Manages persistent + transient filter modes, transaction listing, search, suggestions, upcoming payments, revealed-card state.
- **Why fix:** Hard to test, easy to break unrelated features when changing one.
- **Fix:** Split — `TransactionFilterViewModel`, `TransactionListViewModel`, `TransactionSearchViewModel`. Share via parent `ViewModelStoreOwner` or a state-holder injected into each.
- **Verify by:** each new VM has <300 lines and one clear responsibility.

## ARC-39 · `channelFlow + AtomicInteger` card emission has a closed-channel race [P0 · hard]
- **File:** `ui/screens/dashboard/DashboardViewModel.kt:232-356`
- **RCA:** N `launch` coroutines each increment `completedCount`. If the outer `flatMapLatest` cancels (currency change, profile change) mid-completion, late completers can write to a closed channel.
- **Why fix:** Crashes (ClosedSendChannelException) on profile/currency switch under load.
- **Fix:** Replace pattern. Either `merge(*cards.map { cardFlow(it) })` collected into a single emission, or convert computers to return `Flow<CardRenderData>` and combine. The recent shift to `MutableStateFlow.update {}` for `cardDataFlow` reduces this but the channel `send` for the parent state still has the race.
- **Best practice:** Structured concurrency. Let Flow operators manage completion; don't hand-roll counters.
- **Verify by:** stress test — rapid profile/currency toggle; no exceptions.

## ARC-40 · `DashboardCoreData` / `RatesData` / `ProfileData` wrappers add ceremony [P3 · easy]
- **File:** `ui/screens/dashboard/DashboardViewModel.kt:124-145`
- **RCA:** Three private data classes only ever destructured locally. They obscure the data flow.
- **Fix:** Inline destructuring in combines, or keep one wrapper if it genuinely simplifies the signature.
- **Verify by:** read the assembly top-to-bottom; flow should be self-explanatory.

## ARC-41 · `ComputeCardUseCase` swallows compute failures with `mapNotNull` [P1 · easy]
- **File:** `domain/usecase/ComputeCardUseCase.kt:24-26`
- **RCA:** `try { compute() } catch { null }` then `mapNotNull`. Failed card just disappears.
- **Fix:** Return `CardRenderData.Error(cardId, message)`; UI shows error state instead of blank.
- **Verify by:** make a computer throw; UI shows error card instead of vanishing.

## ARC-42 · `AnalyticsViewModel` has no loading state during currency-change recompute [P2 · easy]
- **File:** `ui/screens/analytics/AnalyticsViewModel.kt`
- **RCA:** Combine reruns synchronously; old values stay on screen until recompute lands.
- **Fix:** Wrap the recompute in a `flatMapLatest` that first emits a `Loading` marker, then the new result.
- **Verify by:** change base currency on analytics screen; brief shimmer or loading indicator.

---

# Layer 5 — Service Layer (Singletons, Aggregation Framework)

## ARC-43 · Domain services are `object` singletons [P1 · hard]
> **Curator note:** over-broad. Pure-function services (`TimelineService`, `TransactionFilterService`, `BudgetService` post-snapshot fix) gain nothing from DI. The *only* genuinely problematic singleton is `CurrencyService` because of its mutable state — that's ARC-05. Fix ARC-05; leave the rest as `object`.
- **Files:** `domain/service/aggregation/TransactionAggregationService.kt`, `CardAggregationService.kt`, `AggregationService.kt`, `currency/CurrencyService.kt`, `management/BudgetService.kt`, `LoanService.kt`, `TransactionManager.kt`, `PlannedPaymentService.kt`, `timeline/TimelineService.kt`, `filter/TransactionFilterService.kt`.
- **RCA:** Convenience choice early in the project. Now blocks dispatcher injection, testing with fakes, configuration via DI, and feature flags.
- **Why fix:** Tests resort to `mockkObject(...)` which has known fragility and forbids parallel test execution. CurrencyService specifically holds mutable state (see ARC-05).
- **Fix:** Per service: `class XxxService @Inject constructor(...)` + Hilt module binding. Existing pure-utility helpers stay as `companion object` static helpers if truly stateless.
- **Best practice:** DI for everything that has dependencies; static only for true math utilities.
- **Side effects of fix:** Wide; do one service at a time. Each ViewModel/Repository that calls the service needs to inject it.
- **Verify by:** unit test injects a fake `TransactionAggregationService` without `mockkObject`.

## ARC-44 · `AggregationPlanner` / `TransactionQueryBuilder` over-engineered for actual usage [P1 · hard]
> **Curator note: POSTPONED (decided 2026-06-23).** User confirms the framework is overkill and causes frequent bulky DB calls (priority bumped to P1 to reflect that), but defers the simplification to a future cycle. Catalog entry kept for context.
- **Files:** `data/local/query/TransactionQueryBuilder.kt`, `domain/service/query/AggregationPlanner.kt`, `query/DatabaseQueryServiceImpl.kt`
- **RCA:** A bespoke analytical-query DSL built on top of Room. Each call constructs SQL at runtime via `AggregationDimension`/`MetricConfig`. The flexibility exceeds the actual call patterns (which are a handful of fixed shapes).
- **Why fix:** Hard to optimize (you can't see the SQL until runtime), hard to test (need fakes for the whole pipeline), produces magic-string aliases (`m1`, `m2`, `d1`, …) decoded positionally downstream (ARC-45).
- **Fix:** For each card's actual query shape, write a typed DAO method. Keep the planner for genuinely dynamic cases (e.g., user-defined pivot tables). Most cards need 5–10 plain `@Query` methods.
- **Best practice:** Pay the cost of abstraction only where the flexibility is exercised.
- **Side effects of fix:** Lots of code deletion (welcome). Migration card-by-card.
- **Verify by:** count `@Query` annotations in DAOs (will grow); count `MetricConfig` constructions (should shrink).

## ARC-45 · Magic string aliases (`m1`, `d1`, …) decoded positionally [P1 · medium]
> **Curator note (user-confirmed 2026-06-23): IN SCOPE — fix independently.** ARC-44 is postponed, but this alias-to-extractor coupling produces silent zeros on alias reordering. Fix even while the framework remains. The typed `MetricKey` abstraction is small and self-contained.
- **File:** `data/local/query/TransactionQueryBuilder.kt:76-129`
- **RCA:** `AggregatedData(Map<String, Any>)` returns rows keyed by ordinal aliases. The mapping back to `FinancialSummary` (`mapToFinancialSummary`) relies on the alias names matching the order. A reordering silently produces zeros.
- **Why fix:** Silent data corruption mode (cards show $0 instead of an error).
- **Fix:** Replace string aliases with a typed `MetricKey` enum or sealed class that owns both its SQL expression and its extraction.
- **Verify by:** swap order of two metric aliases; tests should fail (currently pass).

## ARC-46 · `AggregationPlanner` silently drops dimensions beyond max [P2 · trivial]
- **File:** `domain/service/query/AggregationPlanner.kt:26-72`
- **Fix:** `require(dimensions.size <= MAX) { "..." }`.
- **Verify by:** test passes 5 dimensions when MAX=4; expect exception.

## ARC-47 · Empty filter list ≡ "all" ambiguity [P1 · easy]
- **File:** `data/local/query/TransactionQueryBuilder.kt:133-157` + `domain/service/aggregation/CardAggregationService.kt:232-236`
- **RCA:** Caller can't say "match nothing" — empty `accountIds` defaults to "all accounts". Two callsites paper over this differently.
- **Fix:** Make filter list fields `Set<X>?` where `null` = match-all and `emptySet()` = match-none. Update callers.
- **Verify by:** unit test asserts `filter.copy(accountIds = emptySet())` yields zero rows.

## ARC-48 · `TransactionManager.handleSaveSplitTransactions` recursion lacks depth guard [P2 · easy]
- **File:** `domain/service/management/TransactionManager.kt:77`
- **RCA:** Recursive call without depth bound.
- **Fix:** `require(depth <= 1)` (splits don't contain splits) or validate at input.
- **Verify by:** craft a recursive split request; should throw, not stack-overflow.

## ARC-49 · Multiple aggregation services duplicate concerns [P2 · medium]
> **Curator note:** vague entry — may be fine as-is. Realistically P3 or drop entirely. Don't act unless you encounter the confusion in practice.
- **Files:** `CardAggregationService`, `TransactionAggregationService`, `AggregationService`
- **RCA:** Naming and responsibilities overlap. Newcomers don't know which to call.
- **Fix:** Audit usages; merge or clearly delineate (e.g., "TransactionAggregationService = DB-facing; CardAggregationService = card-shaped wrappers").
- **Verify by:** README or KDoc states the boundary; pick a target and migrate stragglers.

---

# Layer 6 — Compose UI Stability

## ARC-50 · `CardCallbacks` data class subtypes not `@Stable` [P2 · easy]
- **File:** `ui/components/cards/CardCallbacks.kt`
- **RCA:** Top-level sealed class is `@Stable` (per recent fix), but each `data class` subtype with lambda fields is not. Compose treats `Function` types as unstable; missing annotations let recomposition cascade.
- **Fix:** `@Stable` on every subtype that has only stable+lambda fields.
- **Verify by:** enable Compose compiler metrics; subtypes show as `stable`.

## ARC-51 · Domain model classes lack `@Immutable` [P3 · trivial]
- **Files:** `domain/model/*` data classes (Account, Transaction, Category, Budget, Loan, Tag, etc.)
- **Fix:** Add `@Immutable` to each pure data class.
- **Verify by:** Compose compiler report shows `stable class Account`.

## ARC-52 · `rememberCardWithCallbacks` builds ~120 lambdas inside one `remember` [P2 · medium]
- **File:** `ui/components/cards/CardUiBridge.kt:88-220`
- **RCA:** All branches of the `when` are eagerly compiled into the same `remember` block. Even after the recent key tightening, the block recreates many lambdas per recomposition for one card.
- **Fix:** Move per-card lambda construction into the specific `when` branch; only the branch matching `config` runs. Use nested `remember`s keyed by the inputs each branch actually uses.
- **Verify by:** Layout Inspector — recomposition count for sibling cards = 0 on unrelated state changes.

## ARC-53 · `LaunchedEffect(config, input)` key fragility [P2 · easy]
- **File:** `ui/components/cards/CardUiBridge.kt:73-81`
- **RCA:** `input: CardInput` is large and unstable. Effect refires often.
- **Fix:** Combined with ARC-30 split; key only on what the computer actually uses (e.g., `input.profileId`, `input.filter`, `config`).
- **Verify by:** add a log to the effect; should fire only when meaningful inputs change.

## ARC-54 · `CardWithCallbacks.ComputeError` referenced but possibly undefined [P3 · trivial]
> **Curator note: DROP.** Confirmed during reassessment — the variant exists and IS handled at `AeonCardList.kt:154` (`is CardWithCallbacks.ComputeError -> CardErrorState(card.message)`). Original agent finding was inaccurate.
- **File:** `ui/components/cards/CardUiBridge.kt:58-59` references it; verify it exists in `CardWithCallbacks.kt`.
- **Fix:** If missing, add; ensure `UnifiedCardRenderer` has an `is ComputeError ->` branch.
- **Verify by:** compile check.

## ARC-55 · Inconsistent component naming (`Aeon*` vs `*Card`) [P3 · medium]
> **Curator note:** cosmetic. Honestly P4. Likely never worth the rename cost.
- **Files:** `ui/components/`, `ui/screens/dashboard/components/`
- **RCA:** No documented convention. Mix of design-system-prefixed and domain-named composables.
- **Fix:** Convention: `Aeon*` = design-system primitive; `*Card`/`*Sheet`/`*Row` = domain component. Rename outliers.
- **Verify by:** convention documented in `.gemini/design-ui.md`; CI lint optional.

---

# Layer 6.5 — Dashboard Scroll Jank (verified 2026-06-22)

User-observed symptom: records-list flings smoothly; dashboard cards stutter. Each issue below was confirmed against current `feature/tagv43` source. The cards stutter because every visible card carries 4–5 per-frame work sources that compound during fling, whereas the records list is a flat row layout. Fix order matters: ARC-74 + ARC-75 + ARC-77 give the biggest win and were landed together. ARC-76 was withdrawn — see its entry.

## ARC-74 · Double `animateContentSize` per card [P1 · trivial]
- **Files:** `ui/components/cards/AeonCardList.kt:163` AND `ui/components/cards/AeonExpressiveCard.kt:206`
- **RCA:** Every list item wraps its card in `Box(Modifier.animateContentSize(tween(250)))`; the card shell `AeonExpressiveCard` *also* wraps its content in `Modifier.animateContentSize(tween(400))`. Two `SizeAnimationModifier`s per item.
- **Why fix:** `animateContentSize` forces the `LazyColumn` to track delta height per item *every measurement pass*. Doubled, it doubles measure cost. During a fling Compose runs measure for items entering the viewport at full frame rate — this is the single biggest per-card cost.
- **Impact:** Visible stutter on fling; dropped frames most pronounced on mid-tier devices.
- **Fix:** Keep ONE — preferably the inner one on `AeonExpressiveCard` (it's the surface that actually changes height when content reflows). Delete the wrapping `Box` in `AeonCardList.kt:163-173`. If size-change animation is desired for whole-card insert/delete, use `LazyColumn`'s `Modifier.animateItem()` instead.
- **Best practice:** One animation modifier per concept per item. Item-level animation belongs to the list (`animateItem`), content-level animation belongs to the leaf composable.
- **Side effects of fix:** Card height transitions become slightly less "rubber-band". Likely imperceptible.
- **Verify by:** Layout Inspector → recomposition count on each card during fling; or use Macrobenchmark `MeasureRepeated { swipe down }` and confirm `frame.durationMs < 16.6` improves measurably.

## ARC-75 · Heavy recursive sort inside `CategoryPieChart`/`CategoryTreemap` composables [P1 · easy]
> **Curator note: lead with Option B (`remember`).** Three lines, no `CardRenderData` shape change, no test rewrite, ~95% of the benefit. Reserve Option A (move into the computer) for when you're separately cleaning up those computers.
- **File:** `ui/components/cards/UnifiedCardRenderer.kt:109-179` (Pie) and `181-251` (Treemap)
- **RCA:** Both branches define a local `fun mapMetrics(...)` that recurses into `metric.subcategoryData.mapNotNull(::mapMetrics).sortedByDescending { ... }`. The recursion + sort runs **every recomposition** of the card. There is no `remember`. With 50 categories × N subcategories, this is hundreds of `BigDecimal.abs()` compares + multiple list allocations per recompose.
- **Why fix:** When the card enters the viewport during fling, this work runs on the main thread mid-scroll.
- **Impact:** Dropped frames specifically when Pie/Treemap cards appear; worse if the user has many categories.
- **Fix:** Two options:
  - **A (recommended)** — move the entire `mapMetrics` transformation into `CategoryPieChartCardComputer` / `CategoryTreemapCardComputer`. The computer already runs on `Dispatchers.Default`. Push `CategoryPieChartData`/`CategoryTreemapData` lists into `CardRenderData.CategoryPieChart` / `.CategoryTreemap` so the composable only consumes the ready-to-draw model.
  - **B (interim)** — wrap the call site with `val uiData = remember(renderData, selectedMetric) { renderData.items.mapNotNull(::mapMetrics).sortedByDescending { it.amount.abs() } }`.
- **Best practice:** Composables describe UI, not compute it. Anything more than a `when`/`map` belongs upstream.
- **Side effects of fix:** Option A modifies `CardRenderData.CategoryPieChart`/`.CategoryTreemap` shape (breaking) and the computers. Tests on those computers may need new fixtures.
- **Verify by:** add `SideEffect { println("piechart map") }` in the current code, fling — see the spam; after fix it fires only on data/metric change.

## ARC-76 · `DashboardViewModel` emits per-card-completion with no debounce [P1 · easy]
> **Curator note (revised 2026-06-23): WITHDRAWN.** Finance-app correctness wins over scroll smoothness — the user should see every computed value the instant it exists, with no debounce-induced delay (even ~80 ms). The actual scroll-jank root causes are already addressed by ARC-74 (double `animateContentSize`), ARC-75 Option B (memoized pie/treemap mapping), and ARC-77 (`remember`-d `effectiveCard`), combined with `AeonCardList`'s per-card `derivedStateOf` slicing which makes Compose recompose only the card whose data changed. The original ~15 cardDataFlow emissions during cold-load are now harmless because each one drives recomposition of at most one card composable, not the whole list.
- **File:** `ui/screens/dashboard/DashboardViewModel.kt:316-342`
- **RCA:** A single collector calls `send(buildSuccess(cardData, isComputing))` on every `cardDataFlow.update`. `MutableStateFlow.collect` conflates only what the consumer can't keep up with; on dashboard load 10–15 cards complete within milliseconds, producing 10–15 distinct emissions, each reconstructing `DashboardUiState.Success` with a fresh `Map`. If the user starts scrolling at that moment, the list recomposes per emission.
- **Why fix:** This is the second compounding factor (with ARC-74) for fling jank during initial load — and any time the cache is invalidated.
- **Impact:** Brief but reproducible stutter immediately after profile/currency change, dashboard cold-load, and after saving a transaction.
- **Fix:** Window the emissions with `cardDataFlow.sample(80)` (or `debounce`) before the collector. Always emit a final post-all-complete state once `completedCount == configs.size` so the UI doesn't sit on stale data if the last emission was sampled-away.
- **Best practice:** Coalesce high-frequency UI state updates within one frame budget (~16 ms) plus headroom. 80 ms is a safe compromise between perceived responsiveness and frame budget.
- **Side effects of fix:** Cards appear in a single batch ≤ 80 ms later than they would individually; barely perceptible. Requires a guaranteed terminal emission to avoid the "last card never shown" trap.
- **Verify by:** add an emission counter in the collector; on cold load it should drop from ~15 to ~2 (initial cached + final).

## ARC-77 · `effectiveCard` allocated outside `remember` per recomposition [P2 · trivial]
- **File:** `ui/components/cards/AeonCardList.kt:157-161`
- **RCA:** `val effectiveCard = card.copyWithRules(...)` runs on every recomposition. Each call allocates a new `CardWithCallbacks` subtype, layered filter rules object, and (because rules layering touches lambdas) potentially new lambdas inside. Downstream `UnifiedCardRenderer` receives a different reference each time; Compose can't apply skip-skipping.
- **Why fix:** Adds allocation pressure on the hot recomposition path; defeats the recent `@Stable` work on CardCallbacks.
- **Impact:** Smaller than ARC-74/75/76 individually but compounds with them — extra GC during fling.
- **Fix:**
  ```kotlin
  val effectiveCard = remember(card, screenRules, baseFilter, disableFilterSheet) {
      card.copyWithRules(
          rules = card.filterRules.layeredWith(screenRules),
          baseFilter = baseFilter,
          filterSheetEnabled = !disableFilterSheet
      )
  }
  ```
- **Best practice:** Any non-trivial allocation in an item composable should be `remember`-keyed on its actual inputs.
- **Side effects of fix:** Zero — pure performance.
- **Verify by:** Layout Inspector → `UnifiedCardRenderer` recomposition count drops sharply when scrolling.

## ARC-78 · `graphicsLayer` per-card transform math (cameraDistance, rotation, elevation, alpha) [P2 · medium]
> **Curator note: lead with Option B (`derivedStateOf`).** Keeps the layer always applied (preserving the flip animation's first-frame correctness) but the lambda body short-circuits when `rotation == 0f`. Option A (conditionally apply the layer) risks visible flip-animation glitches.
- **File:** `ui/components/cards/AeonExpressiveCard.kt:184-199`
- **RCA:** Each card's `graphicsLayer` block computes `cameraDistance = 20f * density`, `shadowElevation = (2.dp + (8.dp * elevationProgress)).toPx()`, `alpha = 1f - (elevationProgress * 0.15f)` per draw, *for every card visible*, even when no card is being flipped (i.e., `rotation == 0f` — the math runs anyway).
- **Why fix:** With 5–8 cards on screen during fling, the per-draw layer math is duplicated work. `cameraDistance` in particular changes nothing while rotation is 0 but is still issued to the layer each frame.
- **Impact:** Less severe than ARC-74/75/76 but contributes to the cumulative per-frame cost.
- **Fix:** Two ways:
  - **A** — gate the entire `graphicsLayer` block on `isFlipped || rotation != 0f`. When the card is in normal state, skip the layer entirely (or use a much cheaper one with just `clip` + `shape`).
  - **B** — `derivedStateOf` the computed values so the layer block reads from already-evaluated state, only when rotation changes.
- **Best practice:** Layer transforms should be cheap or guarded. Don't pay for animations no one can see.
- **Side effects of fix:** Need to verify the flip animation still feels smooth after the gate (the first frame of the flip must apply the layer). Easiest: keep the layer but make its lambda body short-circuit when `rotation == 0f`.
- **Verify by:** Profile GPU rendering (developer options) on dashboard scroll — the green bars (issue draw commands) should drop.

---

# Layer 7 — Web Server, Widget, Backup, Misc

## ARC-56 · Web routes can leak internal exception messages [P1 · easy]
- **File:** `data/web/WebApiRoutes.kt:70-102` and individual route files
- **RCA:** Route handlers throw; framework converts to error response including `e.message`. SQL/schema details escape.
- **Why fix:** Information leak (security) + bad UX.
- **Fix:** Top-level error-mapping middleware: log full stack, respond with stable `code` and generic message.
- **Verify by:** trigger a known DB error in a route; client gets `{"code":"INTERNAL"}` not the SQLite message.

## ARC-57 · Web DTOs use `.toDouble()` for amounts [P2 · easy]
- **File:** `data/web/WebMappingUtils.kt:152, 234, 345, 366` and several route files
- **RCA:** Convenience; lossy.
- **Fix:** Serialize `BigDecimal` as JSON string; document API contract.
- **Best practice:** Money over JSON travels as a string.
- **Verify by:** API consumer receives `"1234.5678"`, not `1234.56`.

## ARC-58 · Widget fetcher synchronous + `runCatching` swallows [P2 · medium]
- **File:** `widget/NetWorthWidgetFetcher.kt:14-46`
- **RCA:** Long query inside `updateState`; failures hidden; Glance ~10s budget.
- **Fix:** `withTimeout(5_000)`; return a `Result` distinguishing success/timeout/error; widget renders a "stale" badge with last-good timestamp.
- **Verify by:** simulate slow DB (e.g., 30s sleep in test); widget renders stale, doesn't crash.

## ARC-59 · Widget config not validated after deserialization [P2 · easy]
- **File:** `widget/NetWorthWidgetFetcher.kt:20-22`
- **Fix:** `config.validate()` post-load; coerce invalid values to defaults.
- **Verify by:** persist `fontSize = -1`; widget renders default size, not crash.

## ARC-60 · Backup CSV write via `joinToString(",")` [P1 · easy]
- **File:** `data/repository/BackupRepositoryImpl.kt:130-147`
- **RCA:** No escaping; transaction notes with commas/newlines corrupt the file.
- **Fix:** Use Apache Commons CSV (already in `libs.versions.toml`).
- **Verify by:** write a transaction with note `"a, b\n c"`; export+import round-trips identical text.

## ARC-61 · Backup ZIP uses default compression [P3 · trivial]
- **File:** `data/repository/BackupRepositoryImpl.kt:155`
- **Fix:** `Deflater.BEST_COMPRESSION`.

## ARC-62 · Nominatim requests missing `User-Agent` [P1 · trivial]
- **File:** `ui/components/entry/AeonLocationSelection.kt` (UNVERIFIED)
- **RCA:** OpenStreetMap usage policy requires identification. Anonymous requests get rate-limited / blocked.
- **Fix:** Set header `User-Agent: OssWallet/<version> (+contact)`.
- **Verify by:** inspect outbound request.

## ARC-63 · `CURRENCY_API_KEY` not validated at build [P1 · easy]
- **File:** `app/build.gradle.kts:29`
- **RCA:** Missing key compiles fine; runtime fetches fail silently.
- **Fix:** `require(project.hasProperty("CURRENCY_API_KEY")) { "Set CURRENCY_API_KEY in local.properties" }` in the build script (or `BuildConfig.CURRENCY_API_KEY.ifBlank { error(...) }` at app start).
- **Verify by:** delete the key from `local.properties`; build fails.

## ARC-64 · (UNVERIFIED) ProGuard rules don't keep Room entities [P0 · trivial]
- **File:** `app/proguard-rules.pro`
- **RCA:** Agent reports R8 may obfuscate entity field names; if so, release crashes on first DB op.
- **Why fix:** Catastrophic if true; cosmetic if Room's own ProGuard rules already cover.
- **Fix:** Verify present; if missing add `-keepclassmembers @androidx.room.Entity class * { *; }` and similar for `@androidx.room.Dao`.
- **Verify by:** `./gradlew assembleRelease` then run on device; perform a DB op without crash.

---

# Layer 8 — Testing & Build

## ARC-65 · `runBlocking` used in unit tests instead of `runTest` [P2 · easy]
- **File:** e.g. `src/test/.../CardAggregationServiceTest.kt:54` (and likely others)
- **RCA:** Legacy pattern; blocks real time; hides race conditions; doesn't compose with `TestDispatcher`.
- **Fix:** `runTest { ... }` from `kotlinx-coroutines-test`. Inject `TestDispatcher` per CLAUDE.md (`MainDispatcherRule`).
- **Verify by:** grep `runBlocking` in `src/test/` → zero hits.

## ARC-66 · No fake / in-memory `DatabaseQueryService` for service tests [P2 · hard]
- **File:** `src/test/.../*ServiceTest.kt`
- **RCA:** Tests mock `DatabaseQueryService` per call. Mocks don't catch SQL/column-name bugs.
- **Fix:** Create `FakeDatabaseQueryService` backed by `Room.inMemoryDatabaseBuilder` for integration-flavored service tests.
- **Verify by:** rename a column; test breaks (currently passes against mocks).

## ARC-67 · No test exercises the "add a new card type" ceremony [P2 · medium]
- **Files:** `src/test/.../CardRepository*Test.kt`
- **RCA:** Each of the 15 files is checked by a separate test (or none). No single test fails if you forget step 13.
- **Fix:** Property-based test that iterates `CardType.values()` and asserts: a config can be created, computed by the registry, paired into CardWithCallbacks, and reorder-roundtripped through CardRepository.
- **Verify by:** delete one `when` branch in CardRepository; test fails.

## ARC-68 · Glance widget has no integration test [P2 · hard]
- **Files:** `widget/` + `src/androidTest/`
- **Fix:** Robolectric-based test that triggers `WidgetRefreshTrigger`, asserts WorkManager schedule + preference write.
- **Verify by:** test exists in CI.

## ARC-69 · No migration tests covering 4→5 (FTS), 14→15 (categories), 18→19 (FTS5) [P1 · medium]
- **Files:** `src/androidTest/.../AppDatabaseMigrationTest.kt` (UNVERIFIED — confirm presence)
- **Fix:** Per CLAUDE.md, add Room `MigrationTestHelper` cases for each migration.
- **Verify by:** `./gradlew connectedDebugAndroidTest --tests "*MigrationTest"` passes.

## ARC-70 · `libs.versions.toml` dependency drift [P3 · easy]
- **File:** `gradle/libs.versions.toml`
- **RCA:** Verify all Compose/Room/Hilt/Kotlin versions are pinned and aligned (e.g., Kotlin 2.2.10 + Compose compiler version).
- **Fix:** Document the alignment rules in `CLAUDE.md`; periodic `./gradlew dependencyUpdates`.

## ARC-71 · KAPT still used for Moshi codegen [P3 · easy]
- **File:** build log shows `Kapt support in Moshi Kotlin Code Gen is deprecated`.
- **Fix:** Migrate Moshi codegen to KSP (everything else is already KSP).
- **Verify by:** no KAPT tasks in `./gradlew tasks --all`.

---

# Layer 9 — Documentation & Conventions

## ARC-72 · No ADRs for major decisions [P3 · easy]
- **File:** `docs/adr/`
- **RCA:** Decisions like "no destructive migrations", "fakes over mocks", "Aeon prefix" exist in CLAUDE.md but not as discoverable, dated records.
- **Fix:** Add lightweight ADRs (one page each) for: aggregation framework, card plugin pattern, SQLCipher, currency anchoring strategy.

## ARC-73 · CLAUDE.md flags 15-file ceremony but no lint enforces it [P3 · medium]
- **File:** `.gradle/lints/` (would need to be created)
- **Fix:** Custom detekt rule or Konsist test asserting parity across the sealed hierarchies.

---

# Implementation roadmap

Sequenced to minimize risk and maximize value per sprint. Each sprint should land in its own PR(s).

## Sprint A — Stop the bleeding (P0 currency + isolation + scroll-jank quick wins)
Goal: eliminate silent-wrong-value and cross-profile leak paths; land the highest-value scroll-jank fixes since they're tiny.

| # | Issue | Effort |
|---|---|---|
| 1 | ARC-08 + ARC-09 + ARC-10 — `currency_rates` PK + scoped delete + index (one migration) | medium |
| 2 | ARC-01 — `ratesFlow` carries anchor | medium |
| 3 | ARC-02 + ARC-04 — fail-closed rebasing + surfaced error flow | easy |
| 4 | ARC-13 + ARC-14 + ARC-15 + ARC-16 + ARC-17 — profileId everywhere on DAO | easy each, bulk |
| 5 | ARC-19 — clear `QueryCache` on profile change | medium |
| 6 | ARC-39 — replace `channelFlow + AtomicInteger` with safe Flow primitives | hard |
| 7 | ARC-64 — verify ProGuard Room rules | trivial |
| 8 | **ARC-74 + ARC-77 — dashboard scroll jank pair** (drop double animateContentSize, remember effectiveCard). ARC-76 withdrawn — finance data emissions must not be debounced. | trivial / easy |
| 9 | **ARC-75 — move pie/treemap mapping into the computer** | easy |

## Sprint B — Architectural hygiene (services + cards) — landed except ARC-32 / ARC-30
Goal: undo the singleton lock-in; collapse the card plugin duplication.

| # | Issue | Effort | Status |
|---|---|---|---|
| 8 | ARC-05 — atomic `CurrencyService.FormatConfig` setter (rest of ARC-43 dropped per curator note) | easy | done |
| 9 | ARC-32 — collapse CardCallbacks duplication | medium | **deferred** (focused-session refactor; see entry note) |
| 10 | ARC-33 — generic `updateCardConfig<C>` | easy | done |
| 11 | ARC-30 — split CardInput into typed subsets | hard | **deferred** per curator note |
| 12 | ARC-41 — surface compute errors instead of `null` | easy | done |

## Sprint C — Data integrity & developer experience
| # | Issue | Effort |
|---|---|---|
| 13 | ARC-28 — `@Transaction` on backup import + transfer + planned-payment confirm | easy |
| 14 | ARC-26 — atomic tag sync + drop legacy column | medium |
| 15 | ARC-21 + ARC-22 — FilterEntity primitives only | medium |
| 16 | ARC-29 + ARC-60 — backup schema version + CSV escape | medium |
| 17 | ARC-56 + ARC-57 — web error envelope + BigDecimal-as-string | easy |
| 18 | ARC-18 — categories profile scoping (design + migration) | hard |

## Sprint D — UI stability + service refactor continuation
| # | Issue | Effort |
|---|---|---|
| 19 | ARC-38 — split `TransactionViewModel` | hard |
| 20 | ARC-50 + ARC-51 — `@Stable`/`@Immutable` pass | easy |
| 21 | ARC-52 + ARC-53 — `CardUiBridge` lambda hygiene | medium |
| 22 | ARC-45 — typed `MetricKey` (independent of ARC-44; fixes silent-zero bug) | medium |
| 23 | ARC-43 cont — finish singleton → DI for remaining services | hard |

## Sprint E — Polish & guardrails
| # | Issue | Effort |
|---|---|---|
| 24 | ARC-58 + ARC-59 — widget timeouts + config validation | medium |
| 25 | ARC-65 + ARC-66 + ARC-67 + ARC-69 — test infrastructure | medium |
| 26 | ARC-3, ARC-6, ARC-7, ARC-11, ARC-12 — close the deprecated currency surface | easy |
| 27 | ARC-78 — gate `AeonExpressiveCard` graphicsLayer math when rotation == 0 | medium |
| 28 | ARC-23, ARC-24, ARC-25, ARC-27, ARC-34, ARC-35, ARC-36, ARC-37, ARC-40, ARC-42, ARC-46, ARC-47, ARC-48, ARC-49, ARC-54, ARC-55, ARC-61, ARC-62, ARC-63, ARC-70, ARC-71, ARC-72, ARC-73 — backlog mop-up | varies |

> **Postponed (not scheduled):** ARC-31 (card plugin pattern), ARC-44 (aggregation framework simplification). See Curator's reassessment for the deferral rationale.

---

# Verification, in aggregate

End-to-end checks beyond the per-issue verification lines:

- **Currency correctness:** with a user base currency ≠ USD, every screen and the widget show identical totals; backup→import round-trips identical totals.
- **Profile isolation:** automated test creates colliding string-id accounts in two profiles; every screen, every widget, the web API, and the backup file shows only the active profile's data.
- **Cache coherence:** rapid currency / profile switches do not flash incorrect totals; no `ClosedSendChannelException` in `adb logcat`.
- **Adding a card:** integration test asserts new card type fires through every layer; deleting any one registration produces a *compile* error (not runtime).
- **Migration:** Room `MigrationTestHelper` covers v4→v19 cleanly; backup from v18 imports into v19 with explicit migration notes (or refusal).
- **Build hygiene:** release build runs on device end-to-end (DB ops, currency conversion, widget update, backup export+import) without crashes.

---

# Items deferred / explicitly NOT in this plan

- Performance items already tracked and largely completed on this branch (composite indices, FTS5, batched bucket queries, conversion-factor caching, recompute-key tightening, `MutableStateFlow.update` for card emission). Git log is authoritative.
- Visual/UX redesigns (out of scope for an architectural audit).
- Multi-device sync / cloud features (not yet in the product).
