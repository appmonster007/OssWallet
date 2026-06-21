---
name: progressive-dashboard-loading
description: Progressive card loading via channelFlow + stale-while-revalidate cache added to DashboardViewModel in feature/tagv43; Room-backed exchange rate flow added in fe0fc2cd
metadata:
  type: project
---

Progressive dashboard loading implemented in feature/tagv43. Key architectural points:

- `DashboardViewModel` uses `channelFlow` inside `flatMapLatest`: Phase 1 emits structural data, Phase 2 launches parallel card compute behind a `Mutex`.
- `CardRenderCache` (Singleton, `ConcurrentHashMap<Int, Map<String, CardRenderData>>`) added to persist computed cards in memory per profileId across recompositions/profile switches.
- Stale-while-revalidate: on cache hit, Phase 1 immediately emits cached cards (isCardsComputing=false), then Phase 2 silently overwrites.
- `ComputeCardUseCase` and `TransactionUseCases` removed from DashboardViewModel constructor.
- `CardAggregationService` expanded with per-card compute methods (computeBudgetsCard, computeLoansCard, etc).
- Card computers (Budgets, Loans, RecentActivities, CategoryPieChart, CategoryTreemap) now take `DatabaseQueryService` constructor parameter.
- `CurrencyDao.observeAllRates()` Flow added; `CurrencyRepositoryImpl.ratesFlow` and `CurrencyRepository.ratesFlow` interface member added; `ComputationRepositoryImpl` now derives `exchangeRates` and `hasLoadedExchangeRates` from Room directly.
- `CardRenderData.Error` sealed subclass added; `WebMappingUtils.toPayloadDto` handles it with a zeroed-out `CardPayloadDto.NetWorth` fallback.

**Critical architecture issue (confirmed in review fab8df9f):**
`exchangeRates` flow in `ComputationRepositoryImpl` does NOT re-base rates when currency changes. The combine lambda receives `entities` (always stored relative to original fetch base) and returns them unchanged. The comment "Re-base: entities are stored relative to the base currency at fetch time — entities" is misleading — the re-basing is a dead code stub. The `getExchangeRates()` method in `CurrencyRepositoryImpl` does attempt re-basing via division when base mismatches, but `ratesFlow` (which is derived from `observeAllRates()`) does not. Since `ExchangeRateSnapshot.anchor` is set to `baseCurrency` (the selected currency, not the DB fetch base), `CurrencyService.convertAmount` will silently produce wrong values when: (a) DB rates were fetched under EUR, (b) user switches to USD — the rates map still contains EUR-relative values but the anchor is now "USD".

**Known logic regression (from previous review — still present):** `computeBudgetRollover` does not propagate `effectiveFilter.accountIds` to recursive rollover queries.

**LoanPayment profileId isolation gap:** `LoanPaymentDao.getAllPayments()` has no `WHERE profileId` clause. `LoanPaymentEntity` has no `profileId` column — isolation relies on foreign-key cascade from loans table, which is logically correct but not enforced by Room query filter. When profiles share loanIds (edge case), cross-profile leakage is possible. Already flagged.

**How to apply:** When touching exchange rates, always check whether the in-DB base currency matches the selected currency and whether re-basing is applied before building ExchangeRateSnapshot. When touching budget rollover or card compute logic, check that account filters are threaded through all recursive DB queries.
