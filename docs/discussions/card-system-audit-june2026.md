# Discussion: Dashboard Card System Audit — June 2026

> One ongoing topic, continuously refined. Superseded decisions are kept as history, never deleted.

## Metadata

| Field | Value |
|-------|-------|
| Topic | 10-card system audit: perspective toggles, field redundancy, missing features, and logical flaws |
| Status | Active |
| Created | 2026-06-19 |
| Last updated | 2026-06-19 |
| Participants / context | Full code audit of 10 cards across CardConfig, CardRenderData, CardDataComputer, UI/settings-sheet layers. Triggered by developer review of card feature parity and data model design. |

## Summary

A full audit of 10 OssWallet dashboard cards was conducted. Key findings: the `excludeSameCurrencyTransfers` flag was a UI-only display switch with no actual data-layer filtering — it has been replaced with `excludeInternalTransfers`, which filters at the SQL and in-memory layers based on whether a transfer's `targetAccountId` is within the card's effective account scope. All 10 cards correctly expose the `AccountingPerspective` toggle. `totalTransfersInBase` is redundant (derivable from its directional components). The Cash Flow card is the only flow-summary card without a `showComparison` toggle. Additional open gaps (chart overlays, computer copy-paste, CategorySpendingTrend performance) are tracked in D-004/D-005.

## Decision Log Summary

| ID | Decision | Status | Date | Supersedes | Superseded by |
|----|----------|--------|------|-----------|---------------|
| D-001 | All 10 cards have and need the `AccountingPerspective` toggle | ACTIVE | 2026-06-19 | — | — |
| D-002 | `totalTransfersInBase` is derivable; `netInBase` is worth keeping | ACTIVE | 2026-06-19 | — | — |
| D-003 | Cash Flow card needs `showComparison` (highest-priority gap) | ACTIVE | 2026-06-19 | — | — |
| D-004 | Per-card pending features inventory | ACTIVE | 2026-06-19 | — | — |
| D-005 | Logical flaws across computers and UI | ACTIVE | 2026-06-19 | — | — |
| D-006 | Replace `excludeSameCurrencyTransfers` with scope-based `excludeInternalTransfers` | ACTIVE | 2026-06-19 | — | — |

---

## Decisions

### D-001 — All 10 cards have and need the `AccountingPerspective` toggle

- **Status:** ACTIVE
- **Date:** 2026-06-19
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — Every one of the 10 audited cards has `perspective: AccountingPerspective` in its `CardConfig` subclass and exposes `CardPerspectiveSettings` in its settings sheet. All 10 genuinely need it.

**Why** — The toggle controls whether transfers are shown as a separate ledger category (ACCOUNTING: Income / Expense / Transfer three-way split) or folded directionally into cash movement (CASH_FLOW: Inflow = Income + TransferIn, Outflow = Expense + TransferOut). Every card that displays any monetary flow metric changes semantically between the two modes. Removing the toggle from any card would lock users into a single accounting worldview.

**How** — Implemented via `CardPerspectiveSettings` composable shared across all cards. Each config stores `perspective: AccountingPerspective = AccountingPerspective.ACCOUNTING` as default. The computer layers call `.toPerspective(config.perspective)` on `CashFlowResult`, and the bucket-level computers (IncomeExpenseBar, CashFlowComparison) apply the perspective logic inline per-bucket.

**Where** — `CardConfig.kt` (each config class), `CardPerspectiveSettings.kt` (shared UI), all 10 settings-sheet composables, all card computers.

**When** — Applied at compute time. Because perspective is stored in config and re-triggers a card recompute, it is persistent across sessions.

**Alternatives considered**
- Global app-level perspective setting rather than per-card — rejected because different cards on the same screen may legitimately need different views (e.g., Net Worth in accounting mode, a trend chart in cash-flow mode).
- No perspective toggle at all — rejected because users with investment/transfer-heavy profiles (SharedWallet scenarios) need to see cash flow independently of accounting classifications.

**Per-card verdict table:**

| Card | Has `perspective` in config | Shown in settings sheet | Needs it |
|------|----------------------------|------------------------|---------|
| Net Worth | ✅ | ✅ | ✅ — controls whether transfer rows appear separately in the summary |
| Cash Flow | ✅ | ✅ | ✅ — directly changes which rows are shown in the breakdown table |
| Income & Expense Bar | ✅ | ✅ | ✅ — determines whether transfer legs fold into income/expense bars |
| Cash Flow Comparison | ✅ | ✅ | ✅ — changes what val1/val2 mean per time bucket |
| Category Pie Chart | ✅ | ✅ | ✅ — AggregationService.buildCategoryTree uses perspective |
| Category Treemap | ✅ | ✅ | ✅ — same as Pie Chart (identical computer) |
| Category Spending Trend | ✅ | ✅ | ✅ — `shouldInclude` logic branches on perspective |
| Account Hero | ✅ | ✅ | ✅ — `AccountFlowMetrics.toPerspective()` changes displayed values |
| Accounts Summary | ✅ | ✅ | ✅ — `AccountFlowMetrics.toPerspective()` applied to each account's stats |
| Tag UpSet Plot | ✅ | ✅ | ✅ — TagUpSetPlotCardComputer has ACCOUNTING vs CASH_FLOW branching |

**Open questions / risks** — None. All cards are correctly wired.

---

### D-002 — `totalTransfersInBase` is derivable; `netInBase` is worth keeping

- **Status:** ACTIVE
- **Date:** 2026-06-19
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — In `CashFlowResult` (`FinancialModels.kt`):

```kotlin
data class CashFlowResult(
    val totalIncomeInBase: BigDecimal,
    val totalExpensesInBase: BigDecimal,
    val totalTransfersInBase: BigDecimal,          // stored
    val totalTransferInInBase: BigDecimal,          // stored
    val totalTransferOutInBase: BigDecimal,         // stored
    ...
    val netInBase: BigDecimal,                      // stored
)
// Already-computed properties:
val totalNetTransferInBase get() = totalTransferInInBase + totalTransferOutInBase
```

After analysis:
- `totalTransfersInBase` IS derivable: it equals `totalTransferInInBase + totalTransferOutInBase` (i.e., the signed net of all transfer legs converted to base). The class already has a computed property `totalNetTransferInBase` that computes this same value, meaning `totalTransfersInBase` and `totalNetTransferInBase` are semantically and mathematically identical — this is a naming inconsistency.
- `netInBase` IS derivable: `totalIncomeInBase + totalExpensesInBase + totalTransfersInBase`. However, it is used in many UI sites and keeping it precomputed is pragmatically worthwhile.

**Why (totalTransfersInBase)**:
- The stored `totalTransfersInBase` is accumulated from `summary.transfer` per currency; `totalTransferInInBase`/`totalTransferOutInBase` from `summary.transferIn`/`summary.transferOut`. If these fields follow consistent signed arithmetic in the DAO layer, the sum is provably identical.
- The existing computed property `totalNetTransferInBase = totalTransferInInBase + totalTransferOutInBase` already computes this. Having both `totalTransfersInBase` (stored) and `totalNetTransferInBase` (computed) with equal values creates quiet confusion about which to use.
- **Recommendation**: Deprecate `totalTransfersInBase` as a stored field. Migrate all call sites to `totalNetTransferInBase` (the computed property). Validate that the two paths produce identical values via a unit test before removing.
- **Risk of removal**: If `summary.transfer` is ever computed on a different path from `summary.transferIn + summary.transferOut` (e.g., different JOIN strategy in a DAO), removing the stored field silently changes what gets shown. The test gate mitigates this.

**Why (netInBase)**:
- Used in `NetWorthCard.kt`, `CashFlowCard.kt` to drive the main "NET" row. Removing it forces repeated `income + expense + transfer` additions at every consumption site, which is error-prone and verbose.
- The computation is trivial (three BigDecimal additions) so there is no performance argument for keeping it — only ergonomics and readability.
- **Recommendation**: Keep `netInBase` as a stored field. The ergonomics benefit outweighs the redundancy cost.

**How** — Migration path for `totalTransfersInBase`:
1. Add unit test asserting `totalTransfersInBase == totalNetTransferInBase` for all CardAggregationService and AggregationService call sites.
2. Find all usages of `totalTransfersInBase`, replace with `totalNetTransferInBase`.
3. Remove the stored field from `CashFlowResult`.

**Where** — `FinancialModels.kt`, `CardAggregationService.kt`, `AggregationService.kt`, all card computers using CashFlowResult.

**When** — Non-urgent refactor. Should be done before adding new cards that consume `CashFlowResult` to avoid spreading the redundancy further.

**Alternatives considered**
- Keep both fields with documentation — rejected because it perpetuates naming confusion between `totalTransfersInBase` and `totalNetTransferInBase`.
- Remove `netInBase` instead — rejected because it is consumed at far more call sites and its ergonomic value is higher.

**Open questions / risks** — Need to verify via test that `summary.transfer` ≡ `summary.transferIn + summary.transferOut` in all DAO query paths before removing the stored field.

---

### D-003 — Cash Flow card needs `showComparison` (highest-priority feature gap)

- **Status:** ACTIVE
- **Date:** 2026-06-19
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — The Cash Flow card is the only period-flow summary card that does **not** have a `showComparison: Boolean` config field, while Net Worth, Accounts Summary, Category Pie Chart, Category Treemap, and Account Hero all do. When enabled, those cards show the previous-period value alongside the current one, with a delta indicator.

**Why** — The Cash Flow card is arguably the most natural place for period comparison ("income was $X this month vs $Y last month"). Its absence is especially jarring because the computer (`CardAggregationService.computeCashFlowResult`) already supports fetching data for arbitrary date ranges. Adding comparison requires:
1. Add `showComparison: Boolean = true` to `CashFlowCardConfig`.
2. In `CardAggregationService.computeCashFlowCard`, fetch the previous-period CashFlowResult if `showComparison` is true.
3. Add a `previousResult: CashFlowResult?` field to `CardRenderData.CashFlow`.
4. In the settings sheet (`CashFlowSettingsSheet`), add the toggle row.
5. In `CashFlowCard.kt`, render the previous values as secondary text or delta indicators alongside the current values.

**How** — Same pattern used by Net Worth and Account Hero: after computing the current period result, call `TimelineService.getPreviousRange(filter.timelineRange)` and run the same `computeCashFlowResult` for the previous range if `showComparison` is true.

**Where** — `CashFlowCardConfig.kt`, `CardAggregationService.kt`, `CardRenderData.CashFlow`, `CashFlowCard.kt`, `CashFlowSettingsSheet`.

**When** — Should be implemented before the next dashboard feature release. The infrastructure is already present via the pattern in Net Worth.

**Alternatives considered**
- Leave Cash Flow as-is and rely on CashFlowComparison card for comparison — rejected because CashFlowComparison shows a line chart over time, while Cash Flow shows a table breakdown by currency/type. They serve different use cases.

**Open questions / risks** — None. Pattern is well-established.

---

### D-004 — Per-card pending features inventory

- **Status:** ACTIVE
- **Date:** 2026-06-19
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — A card-by-card breakdown of features present in peer cards that are absent in the current card, with justification for whether each should be added.

**Why** — Feature parity across structurally similar cards reduces cognitive load (users expect consistent controls) and reveals real UX gaps versus intentional design asymmetry.

---

#### Card 1 — Net Worth

**Current config fields:** `perspective`, `excludeSameCurrencyTransfers`, `showComparison`, `fontSize`

**Missing vs. peers:**
| Missing feature | Present in | Should add? | Justification |
|----------------|-----------|-------------|---------------|
| `isCumulative` | IncomeExpenseBar, CashFlowComparison, CategorySpendingTrend | ❌ N/A | Net Worth is a point-in-time balance, not a time-series chart. No chart to apply cumulative to. |
| `selectedMetric` | Pie, Treemap, CategoryTrend, TagUpSet | ❌ N/A | Net Worth always shows the full summary (income + expense + transfer rows). Selecting a single metric would hide essential information. |
| `showGridLines` | IncomeExpenseBar, CashFlowComparison, CategorySpendingTrend | ❌ N/A | Net Worth has no chart. |

**Additional missing features worth adding:**
- No "target net worth" or "goal progress" indicator. Would require a separate config field for `targetNetWorth: BigDecimal?`.
- No sparkline trend of net worth over sub-periods. The card currently shows only the current period's starting-point balance.

---

#### Card 2 — Cash Flow

**Current config fields:** `perspective`, `excludeSameCurrencyTransfers`, `showBaseConversion`, `showNetColumn`

**Missing vs. peers:**
| Missing feature | Present in | Should add? | Justification |
|----------------|-----------|-------------|---------------|
| `showComparison` | Net Worth, Accounts, Pie, Treemap, Account Hero | ✅ **YES** | See D-003. Highest-priority gap. |
| `isCumulative` | Chart cards | ❌ N/A | Cash Flow is a table, not a chart. |
| `selectedMetric` | Chart cards | ❌ N/A | The table shows all metrics simultaneously; a metric selector would hide rows. |

---

#### Card 3 — Income & Expense Bar

**Current config fields:** `perspective`, `excludeSameCurrencyTransfers`, `isCumulative`, `showGridLines`

**Missing vs. peers:**
| Missing feature | Present in | Should add? | Justification |
|----------------|-----------|-------------|---------------|
| `selectedMetric` | Pie, Treemap, CategoryTrend, TagUpSet, CashFlowComparison | ❌ No — intentional | The card is named "Income **and** Expense". Showing both is its core value proposition. A metric selector would reduce it to a single-bar chart, which CashFlowComparison already covers for individual metrics. |
| `showComparison` (overlay prev period) | Net Worth, Accounts, etc. | ⚠️ Optional | Could overlay the previous period as dimmed/grouped bars. This is a significant render complexity addition. Not blocking, but worth considering long-term. |
| Net line visibility toggle | — | ✅ Recommended | Currently the net trend line is always rendered. A `showNetLine: Boolean = true` toggle would let users who want a cleaner chart hide it. Low effort. |

---

#### Card 4 — Cash Flow Comparison (Line)

**Current config fields:** `perspective`, `excludeSameCurrencyTransfers`, `isCumulative`, `showGridLines`, `selectedMetric`, `showCurrentPeriod`, `showPreviousPeriod`, `showSamePeriodLastYear`

**Missing vs. peers:**
| Missing feature | Present in | Should add? | Justification |
|----------------|-----------|-------------|---------------|
| In-card metric selector chip | TagUpSet (CompactMetricSelector), CategorySpendingTrend (settings) | ✅ Recommended | `selectedMetric` is stored in config but there is no in-card quick-switch for it. Users must open the settings sheet to change what metric is plotted. Adding a `CompactMetricSelector` to the card header would match TagUpSet's UX pattern. |

**Overall assessment:** Most feature-rich analytics card in the set. No critical gaps.

---

#### Card 5 — Category Pie Chart

**Current config fields:** `perspective`, `excludeSameCurrencyTransfers`, `showComparison`, `selectedMetric`, `showLabels`, `showLegend`, `rounded`, `showFullBreakdown`, `showOnlyActiveCategories`

**Missing vs. peers:**
| Missing feature | Present in | Should add? | Justification |
|----------------|-----------|-------------|---------------|
| `isCumulative` | Chart cards | ❌ N/A | Pie charts are period snapshots. |
| `showGridLines` | Chart cards | ❌ N/A | Pie charts have no grid. |

**Overall assessment:** Feature-complete for its chart type. No gaps.

---

#### Card 6 — Category Treemap

**Current config fields:** Identical to Category Pie Chart.

**Missing vs. peers:**
| Missing feature | Present in | Should add? | Justification |
|----------------|-----------|-------------|---------------|
| (same analysis as Pie Chart) | — | — | — |

**Code duplication issue:** `CategoryTreemapCardComputer` and `CategoryPieChartCardComputer` are 92-line near-exact copies of each other. The shared logic (filter resolution, category expansion, `AggregationService.buildCategoryTree`, previous period computation) should live in a shared internal function. This is a maintenance debt issue, not a feature gap.

---

#### Card 7 — Category Spending Trend

**Current config fields:** `perspective`, `excludeSameCurrencyTransfers`, `selectedMetric`, `selectedNatures`, `isCumulative`, `showGridLines`

**Missing vs. peers:**
| Missing feature | Present in | Should add? | Justification |
|----------------|-----------|-------------|---------------|
| `showComparison` (previous period ghost overlay) | Net Worth, Accounts, Pie, Treemap, Account Hero | ⚠️ Consider | Previous-period stacked area in lighter color would let users see "same categories, did they grow?". Moderately complex to render but the data infrastructure supports it. |
| `maxCategories` cap | TagUpSet (maxTags) | ⚠️ Consider | With many categories, the chart becomes unreadable. A config field `maxCategories: Int = 8` that groups low-value categories into "Other" would match the Pie chart's `showFullBreakdown` behavior. |

**Bug note (see D-005):** CASH_FLOW perspective + TRANSFER metric = silent empty chart.

---

#### Card 8 — Account Hero

**Current config fields:** `perspective`, `excludeSameCurrencyTransfers`, `showComparison`, `showCurrencyBadges`

**Missing vs. peers:**
| Missing feature | Present in | Should add? | Justification |
|----------------|-----------|-------------|---------------|
| Inline trend sparkline | — (unique opportunity) | ✅ Recommended | Account Hero is the hero card for an account detail screen. An embedded mini Income/Expense trend chart (last N periods) would dramatically increase its value. The data pipeline already exists via `IncomeExpenseBarCardComputer` logic. Would require adding a `showTrend: Boolean = false` config field and a small embedded chart composable. |
| `selectedMetric` chip | TagUpSet, CashFlowComparison | ❌ N/A | Account Hero shows all flow metrics in a structured layout; a metric selector would reduce it. |

---

#### Card 9 — Accounts Summary

**Current config fields:** `perspective`, `excludeSameCurrencyTransfers`, `showComparison`, `layout`, `gridColumns`, `showAddAccountCard`

**Missing vs. peers:**
| Missing feature | Present in | Should add? | Justification |
|----------------|-----------|-------------|---------------|
| Account type/group filter | Filter sheet (general) | ⚠️ Consider | Users with many accounts may want to group/filter by type (savings, credit, investment). Currently all active accounts are always shown. A `filterByAccountTypes: Set<AccountType>` config field would enable this. |
| Sort order for accounts | — | ✅ Recommended | No config for account sort order (balance, name, manually reordered). Users with many accounts cannot control display ordering independently of the global account order. |

---

#### Card 10 — Tag UpSet Plot

**Current config fields:** `perspective`, `excludeSameCurrencyTransfers`, `selectedMetric`, `maxTags`, `maxIntersections`, `dynamicFilteringMode`, `tagSortMode`, `intersectionSortMode`

**Missing vs. peers:**
| Missing feature | Present in | Should add? | Justification |
|----------------|-----------|-------------|---------------|
| `showComparison` on set totals | Pie, Treemap, Net Worth, Accounts, Account Hero | ⚠️ Consider | Could show previous-period tag totals as a secondary bar in the set-size chart. The data infrastructure supports it (re-run the same aggregate query with a shifted date range). |
| In-settings metric selector | Pie, Treemap | Already has in-card CompactMetricSelector; consistent | The current pattern (in-card compact selector + store to config) is actually better than the settings-only approach of Pie/Treemap. |

**Computer-level gaps (see D-005):** `maxTags` and `maxIntersections` are UI-only display limits; the computer fetches all tags and intersections regardless of these settings.

---

**Open questions / risks** — The relative priority of these features should be triaged against the roadmap. The Cash Flow `showComparison` gap (D-003) is the only one that is clearly a bug-by-omission rather than a design choice.

---

### D-005 — Logical flaws across computers and UI

- **Status:** ACTIVE
- **Date:** 2026-06-19
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — A catalogue of logic bugs, inconsistencies, and performance issues identified during the audit.

---

#### Flaw 1 — `CashFlowTimePoint.transfer` not zeroed in CASH_FLOW perspective

**Cards affected:** Income & Expense Bar (Card 3), Cash Flow Comparison (Card 4)

**Description:** `CashFlowResult.toPerspective(CASH_FLOW)` explicitly zeroes all transfer fields:
```kotlin
totalTransfersInBase = BigDecimal.ZERO,
totalTransferInInBase = BigDecimal.ZERO,
totalTransferOutInBase = BigDecimal.ZERO,
```
But in the bucket-level computers (`IncomeExpenseBarCardComputer`, `CashFlowComparisonCardComputer`), each `CashFlowTimePoint` is built as:
```kotlin
CashFlowTimePoint(
    income = val1,   // correctly folds transferIn when CASH_FLOW
    expense = val2,  // correctly folds transferOut when CASH_FLOW
    transfer = valTrf,  // BUG: NOT zeroed when perspective == CASH_FLOW
    net = val1.add(val2)  // correct
)
```
The `net` is correct because val1 and val2 already include the folded transfers. But `CashFlowTimePoint.transfer` retains a non-zero value even in CASH_FLOW mode, which is semantically wrong. If any downstream UI or future card reads `transfer` for display, it will show a misleading value.

**Fix:** In both computers, set `transfer = if (perspective == CASH_FLOW) BigDecimal.ZERO else valTrf`.

---

#### Flaw 2 — CategorySpendingTrend: CASH_FLOW + TRANSFER metric = silent empty chart

**Card affected:** Category Spending Trend (Card 7)

**Description:** In `CategorySpendingTrendCardComputer`:
```kotlin
AccountingPerspective.CASH_FLOW -> when (config.selectedMetric) {
    ChartMetric.TRANSFER -> false  // always exclude — no transactions match
}
```
When perspective=CASH_FLOW and selectedMetric=TRANSFER, every transaction is excluded, resulting in an empty chart with no explanation. The user has no feedback that this combination is invalid.

**Fix options:**
1. Disable the TRANSFER metric chip when CASH_FLOW perspective is active (UI-level guard).
2. Show a non-empty state message: "Transfer metric is not applicable in Cash Flow perspective."
3. Snap the metric back to NET when switching to CASH_FLOW if the current metric is TRANSFER.

**Recommendation:** Option 1 (disable chip) + Option 2 (show a tooltip explaining why) is the cleanest UX.

---

#### Flaw 3 — CategoryPieChart and CategoryTreemap computers are copy-pasted

**Cards affected:** Category Pie Chart (Card 5), Category Treemap (Card 6)

**Description:** `CategoryPieChartCardComputer.kt` (92 lines) and `CategoryTreemapCardComputer.kt` (92 lines) are line-for-line identical except for the config type and the `CardRenderData` subtype they produce. Any change to the business logic (e.g., filter resolution, category expansion, previous period computation) must be applied in both files, which is a maintenance hazard.

**Fix:** Extract the shared logic into a private `suspend fun computeCategoryBreakdown(input, filter, config, showComparison, perspective): CategoryBreakdownResult` in a shared `CategoryBreakdownComputer` utility object, and have both computers delegate to it.

---

#### Flaw 4 — TagUpSetPlot: `maxTags` and `maxIntersections` not enforced at computer level

**Card affected:** Tag UpSet Plot (Card 10)

**Description:** In `TagUpSetPlotCardComputer`:
```kotlin
val topTags = tagTotals.map { it.first }.toSet()  // ALL tags, not top maxTags
```
The computer fetches and sends ALL tags to the UI regardless of `config.maxTags`. The UI then applies `maxVisibleRows = config.maxTags` as a display cap. Similarly, `maxIntersections` is not applied at the computer level.

This means:
- For profiles with 50 tags, all 50 tag aggregations and all their pairwise intersections (up to 2^50 theoretically) are computed and serialized, even if only 7 are displayed.
- In practice, the DAO query limits what intersections are materialised, but the Kotlin-side aggregation still processes all of them.

**Fix:** Apply `tagTotals.take(config.maxTags)` before computing `topTags`, so only the top-N tags' intersections are computed and returned. This reduces the intersection computation from O(2^N) to O(2^maxTags) for the Kotlin grouping phase.

---

#### Flaw 5 — AccountHeroCard hard-crashes when no accountId is set

**Card affected:** Account Hero (Card 8)

**Description:**
```kotlin
val targetAccountId = input.filter.accountIds.firstOrNull()
    ?: config.filter.accountIds.firstOrNull()
    ?: error("AccountHeroCard requires an account scope")  // runtime exception
```
If neither the runtime filter nor the persisted config filter has an account ID (e.g., first launch after a fresh install or card config migration), the card crashes the entire card rendering loop rather than showing an empty/error state.

**Fix:** Return a well-defined empty `CardRenderData.AccountHero` rather than throwing. The rendering layer already handles empty states. Alternatively, validate the config at card creation time and prevent placing an AccountHero card without an account scope.

---

#### Flaw 6 — CategorySpendingTrend uses in-memory transaction filtering (performance)

**Card affected:** Category Spending Trend (Card 7)

**Description:** The computer fetches all transactions for the period via `TransactionFilterService.filter(txSource, mergedFilter)` and then performs per-bucket filtering in Kotlin:
```kotlin
val bucketTx = natureTx.filter { tx -> tx.timestamp in bucketStart..bucketEnd }
```
This is an O(N × B) operation where N = transaction count and B = bucket count, executed entirely in memory.

By contrast, `IncomeExpenseBarCardComputer` and `CashFlowComparisonCardComputer` use `TransactionAggregationService.getSummaryByCurrency` (SQL-level GROUP BY aggregation per bucket), which is far more scalable for large datasets.

**Fix:** Refactor `CategorySpendingTrendCardComputer` to use `DatabaseQueryService.aggregate()` with `AggregationDimension.CATEGORY` and `AggregationDimension.TIME_BUCKET`, similar to how `TagUpSetPlotCardComputer` uses `AggregationDimension.TAGS`. This requires the DAO to support a category+time-bucket aggregation query, which may require a new DAO method.

---

#### Flaw 7 — `totalTransfersInBase` and `totalNetTransferInBase` are identical but separately maintained

**Affects:** CashFlowResult (all cards using CashFlowResult)

**Description:** See D-002. `totalNetTransferInBase` (computed property) equals `totalTransferInInBase + totalTransferOutInBase`. `totalTransfersInBase` (stored field) is computed via a separate aggregation path from `summary.transfer`. If these values ever diverge due to a DAO inconsistency, it will be invisible — no assertion or test currently catches the divergence.

**Fix:** Add a unit test asserting `result.totalTransfersInBase.compareTo(result.totalNetTransferInBase) == 0` for all `CashFlowResult` construction paths. This also serves as a prerequisite gate before removing the redundant stored field (per D-002 recommendation).

---

#### Flaw 8 — Net Worth card shows period cash flow but no period-scoped balance timeline

**Card affected:** Net Worth (Card 1)

**Description:** The Net Worth card shows:
- `netWorth` = cumulative balance up to end of period (balance filter)
- `cashFlow.totalIncomeInBase` etc. = flow during the period

This is conceptually correct, but the `netWorth` reflects the true account balance (ALL-TIME cumulative, capped at period end), while `cashFlow` reflects only the selected period. If the selected period is "This Year", the cash flow is for the current year only, but the net worth is the total balance including all prior years. This semantic gap is not documented in the UI and may confuse users who expect net worth to equal income − expenses for the period.

**Fix (doc/UX):** Add a tooltip or subtitle explaining that "Net Worth is your total balance to date, not just the selected period's flow." This is a UX documentation issue, not a code bug.

---

**Open questions / risks**
- Flaw 6 (in-memory CategorySpendingTrend) may require a new `AggregationDimension.TIME_BUCKET` mode or per-bucket DAO query. Impact on query complexity must be evaluated before implementing.
- Flaw 4 (maxTags enforcement) should be tested against profiles with many tags to measure the actual memory impact of the current approach before prioritising the fix.

---

---

### D-006 — Replace `excludeSameCurrencyTransfers` with scope-based `excludeInternalTransfers`

- **Status:** ACTIVE
- **Date:** 2026-06-19
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — The `excludeSameCurrencyTransfers: Boolean` field in all 10 `CardConfig` subclasses and its handling in `CardPerspectiveSettings` are replaced with `excludeInternalTransfers: Boolean`. The new flag filters at the **data layer** (SQL WHERE clause in `TransactionQueryBuilder` and predicate in `TransactionFilterService`) rather than switching between pre-computed sub-fields in the UI layer.

The filtering rule: a transfer leg is excluded if `tx.targetAccountId in effectiveAccountIds` (the card's resolved account scope). Three cases:
- Internal same-currency transfer (A→B, both in scope) → `targetAccountId = B, B in scope` → **excluded** ✓
- Internal cross-currency transfer (A→B, both in scope) → `targetAccountId = B, B in scope` → **excluded** ✓ (improvement: old toggle missed this)
- One-sided / outside-wallet transfer → `targetAccountId = null or outside scope` → **kept** ✓ (improvement: old toggle incorrectly excluded same-currency outside transfers)

**Why** — The old `excludeSameCurrencyTransfers` flag was:
1. Applied **only in the UI display layer** — it switched between `crossCcyTransfer*` and `transfer*` pre-computed sub-fields of `CashFlowResult` and `AccountFlowMetrics`. No actual transaction filtering occurred.
2. Semantically wrong: same-currency was a crude proxy for "internal transfer." Cross-currency internal transfers (e.g., transferring USD from a USD account to an EUR account within the same portfolio) were not excluded even though they should be. Same-currency transfers to outside-wallet accounts were excluded even though they are real boundary flows.
3. The toggle was only shown when `perspective == CASH_FLOW`, hiding it from ACCOUNTING mode where it is equally relevant.

The new scope-based approach is semantically precise: "if both legs are within my reporting boundary, it is an internal movement and should not count as cash flow."

**How** —
- `TransactionFilter`: add `val excludeInternalTransfers: Boolean = false`. Default is `false` (conservative; no filtering unless explicitly set).
- `TransactionQueryBuilder.buildQuery()`: add WHERE clause `AND NOT (transactionType IN ('transfer_inflow','transfer_outflow') AND targetAccountId IN (:accountIds))` when `filter.excludeInternalTransfers == true AND filter.accountIds.isNotEmpty()`. The `accountIds` at this point is always fully resolved (non-empty) because all computers call `.copy(accountIds = ... .ifEmpty { input.effectiveAccountIds })` before querying.
- `TransactionFilterService.matches()`: add predicate `!(filter.excludeInternalTransfers && filter.accountIds.isNotEmpty() && tx.transactionType.isTransfer && tx.targetAccountId in filter.accountIds)`.
- All 10 `CardConfig` subclasses: rename `excludeSameCurrencyTransfers: Boolean` → `excludeInternalTransfers: Boolean = true`. Default is `true` for card configs (users almost always want internal transfers hidden).
- `CardAggregationService` (Net Worth, Cash Flow, Accounts, Account Hero paths): propagate `config.excludeInternalTransfers` onto the merged filter before querying.
- All 6 direct-filter computers (IncomeExpenseBar, CashFlowComparison, CategoryPieChart, CategoryTreemap, CategorySpendingTrend, TagUpSetPlot): propagate `config.excludeInternalTransfers` onto the merged filter.
- `CardPerspectiveSettings`: rename parameter, update label to "Exclude internal transfers", remove the `perspective == CASH_FLOW` guard (the toggle is relevant for both perspectives).
- `CardUpdateHandler`: rename field reference.
- UI card files (NetWorthCard, CashFlowCard, AccountHeroCard, AccountsCard, AeonAccountCards): remove `excludeSameCcy` local variable and all `if (excludeSameCcy) crossCcy* else transfer*` branches — always use the regular `transfer*` fields since filtering is now at the data layer.

**Where** — Affects the full pipeline for all 10 cards: `TransactionFilter` → `TransactionQueryBuilder` / `TransactionFilterService` → card computers → `CardAggregationService` → card configs → settings-sheet UI → display logic.

**When** — Implemented immediately (2026-06-19). JSON backward-compat: old configs with `excludeSameCurrencyTransfers` will have that field silently ignored on deserialization (kotlinx.serialization ignores unknown keys). The new `excludeInternalTransfers` field defaults to `true`, so existing users get the corrected behaviour automatically on upgrade.

**Alternatives considered**
- Keep the old field name, just change how it is applied — rejected because the old name (`excludeSameCurrencyTransfers`) describes a different concept from the new logic (`excludeInternalTransfers`). Keeping the wrong name creates ongoing confusion.
- Apply filtering at the UI layer by checking `targetAccountId` in the pre-fetched transaction list — rejected because the SQL-aggregation path cards (IncomeExpenseBar, CashFlowComparison, TagUpSetPlot) never have individual transactions available in the UI; the exclusion must happen at the query level.
- Add a JOIN in the SQL to determine if the counterpart leg exists — rejected because `targetAccountId` is already a direct column on the transaction, making a JOIN unnecessary.

**Open questions / risks**
- The `crossCcyTransfer*` fields in `CashFlowResult` and `AccountFlowMetrics` were the only sub-fields used for the old UI-level switching. After this change they have no remaining consumers in the card UI. They can be removed in a follow-up cleanup PR, but are left in place here to limit scope.
- The `excludeSameCurrencyTransfers` field in files outside the 10 card configs (TemplateEntity, PlannedPaymentEntity, transaction entry ViewModels, etc.) appears to refer to a different per-transaction concept (e.g., not creating linked legs for same-currency transfers). Those are **not** touched by this change.

---

## Changelog

| Date | Change |
|------|--------|
| 2026-06-19 | Created file; added D-001 through D-005 covering full 10-card audit. |
| 2026-06-19 | Added D-006: scope-based `excludeInternalTransfers` replaces `excludeSameCurrencyTransfers`. Implemented across all layers. |
