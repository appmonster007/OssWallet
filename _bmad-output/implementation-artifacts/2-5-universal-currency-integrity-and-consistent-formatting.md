---
baseline_commit: 7f5ca2de2038cd3a18f8c8bb730bc1f20c8fbb35
---

# Story 2.5: Universal Currency Integrity & Consistent Formatting

Status: done

## Story

As a user,
I want all charts and cards to display amounts with the correct currency symbol and formatting,
so that I have full confidence in the multi-currency accounting data I see on my dashboard and analytics.

## Context / Audit

A recent bug report highlighted that the **Treemap Card** correcty shows amounts but defaults the currency to USD. A project-wide audit of all 15+ cards and chart components has revealed a systematic pattern of missing currency context and inconsistent fallback behaviors.

**Core Issues Found:**
1. **Missing Props:** Many cards (e.g., `CategoryTreemapCard`) compute data in the correct currency but fail to pass that currency code down to the leaf visualization components (e.g., `AeonTreemap`).
2. **Hardcoded Defaults:** Chart components like `AeonTreemap`, `AeonBarChart`, `AeonPieChart`, and `AeonLineChart` have `currency: String = "USD"` in their function signatures, leading to "silent failures" where USD symbols appear when data is missing context.
3. **Null Inconsistency:** Several call sites for `formatCurrency` or `formatCurrencyAnnotated` pass `null` for the currency code, which suppresses the symbol entirely or defaults to system settings, leading to "naked numbers" without units in legends and tooltips.
4. **Manual Formatting:** Some components manually construct currency strings (e.g., `CategoryTreemapCard` building `symbolText + amountText`) instead of using the centralized `CurrencyReprUi` utilities, leading to layout and styling drift.

---

## Acceptance Criteria

**AC1 — Standardize Chart Component Signatures:**
**Given** the chart components `AeonTreemap`, `AeonBarChart`, `AeonPieChart`, and `AeonLineChart`
**When** this story is complete
**Then** the `currency` parameter in their signatures no longer has a default value of `"USD"`
**And** all call sites in the project are updated to pass the correct currency from the card's `renderData` or `config`

**AC2 — Currency Integrity in Y-Axis and Labels:**
**Given** any chart with a Y-axis or data labels
**When** amounts are formatted for display
**Then** the `currencyCode` passed to `formatCurrency` must match the chart's overall currency
**And** no Y-axis label or data point label should use `null` as a currency code unless it's explicitly a unit-less metric (e.g., percentage)

**AC3 — Legend and Tooltip Consistency:**
**Given** card legends (e.g., `TreemapLegend`, `CategoryLegend`) and chart tooltips
**When** an amount is displayed
**Then** it must include the correct currency symbol according to the active profile's `baseCurrency` or the account's `nativeCurrency`
**And** the `formatCurrency` utility must be used with the correct code passed as an argument

**AC4 — Fix Specific Regressions:**
**Then** the **Treemap Card** (Dashboard/Analytics) shows the user's selected currency symbol (e.g., ₹, €, £) instead of $
**And** the **Account Hero Card** and **Account Cards** show the native currency symbol in the distribution/multi-currency badges
**And** the **Loans Card** shows the currency symbol for the principal amount
**And** the **Bar/Line Charts** show currency symbols on their Y-axis ticks

---

## Tasks / Subtasks

### Priority A — Core Chart Standardization

- [x] **Task 1 — Remove hardcoded "USD" defaults from Chart Components**
  - [x] Update `AeonTreemap` signature and `AeonChartInteractionPopup`
  - [x] Update `AeonBarChart` signature (all overloads)
  - [x] Update `AeonPieChart` signature
  - [x] Update `AeonLineChart` signature
  - [x] Update `AeonBarLineChart` signature

- [x] **Task 2 — Fix Chart Y-Axis and Label Formatting**
  - [x] `AeonBarChart.kt`: Replace `currencyCode = null` with `currency` in Y-axis label formatting
  - [x] `AeonLineChart.kt`: Replace `currencyCode = null` with `currency` in Y-axis label formatting
  - [x] `AeonBarLineChart.kt`: Replace `currencyCode = null` with `currency` in Y-axis label formatting

### Priority B — Fix Card Call Sites & Legends

- [x] **Task 3 — Fix CategoryTreemapCard & Legend**
  - [x] Pass `currency` prop to `AeonTreemap` call
  - [x] Update `CategoryTreemapCard` to use `formatCurrencyAnnotated` for the TOTAL amount instead of manual building
  - [x] Update `TreemapLegend` to take `currency` and pass it to `formatCurrency`

- [x] **Task 4 — Fix CategoryPieChartCard Legend**
  - [x] Update `CategoryLegend` to take `currency` and pass it to `formatCurrency`

- [x] **Task 5 — Fix Account Cards & Hero**
  - [x] `AeonAccountCards.kt`: Pass `curr` (from the loop) to `formatCurrency` in the multi-currency distribution list
  - [x] `AccountHeroCard.kt`: Pass `curr` (from the loop) to `formatCurrencyAnnotated` in the multi-currency distribution list

- [x] **Task 6 — Fix Loans Card**
  - [x] `LoansCard.kt`: Pass `summary.loan.currency` instead of `null` to `formatCurrencyAnnotated` for the principal amount

### Priority C — Audit & Cleanup

- [x] **Task 7 — Audit remaining formatCurrency(amount) calls**
  - [x] Audited all remaining calls; all either pass a correct currency code or are intentionally unit-less (ManagementScopeCard filter amounts where no currency context exists)

### Review Findings

- [x] [Review][Patch] `AeonBarLineChart` constructs `CurrencyNumberFormatter()` inline per Y-axis tick — hoist with `remember` [AeonBarLineChart.kt:263]

---

## Dev Notes

### Utility Reference
Always prefer:
```kotlin
CurrencyReprUi.formatCurrencyAnnotated(
    amount = amount,
    currencyCode = currencyCode, // DO NOT LEAVE NULL
    spec = CurrencyDisplaySpec(...)
)
```
or for plain strings:
```kotlin
formatCurrency(amount, currencyCode) // DO NOT LEAVE NULL
```

### Files Impacted
- `AeonTreemap.kt`, `AeonBarChart.kt`, `AeonPieChart.kt`, `AeonLineChart.kt`, `AeonBarLineChart.kt`
- `CategoryTreemapCard.kt`, `CategoryPieChartCard.kt`
- `AeonAccountCards.kt`, `AccountHeroCard.kt`
- `LoansCard.kt`
- `AeonChartInteractionPopup.kt`

---

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Implementation Notes

**Task 1:** Removed `= "USD"` default from all 7 chart component signatures (`AeonTreemap`, `AeonBarChart` ×2, `AeonPieChart`, `AeonLineChart`, `AeonBarLineChart`, `AeonChartInteractionPopup`). All `@Preview` call sites updated to pass `currency = "USD"` explicitly so they compile.

**Task 2:** Replaced `currencyCode = null` with `currencyCode = currency` in the Y-axis tick formatter in `AeonBarChart`, `AeonLineChart`, and `AeonBarLineChart`. Y-axis labels now show the chart's active currency symbol.

**Task 3:** (a) Added `currency = currency` to the `AeonTreemap(...)` call in `CategoryTreemapCard` — this was the root cause of the Treemap USD regression. (b) Replaced manual `buildAnnotatedString` + `CurrencyService.getCurrencySymbol` total display with `CurrencyReprUi.formatCurrencyAnnotated` using `CurrencyPartStyles(symbol = dimStyle, neutral = contentColor)`. (c) Added `currency: String` param to `TreemapLegend` and fixed `formatCurrency(item.amount)` → `formatCurrency(item.amount, currency)`.

**Task 4:** Added `currency: String` param to `CategoryLegend` and fixed `formatCurrency(item.amount)` → `formatCurrency(item.amount, currency)`. Updated call site to pass `currency`.

**Task 5:** `AeonAccountCards.kt`: `formatCurrency(bal)` → `formatCurrency(bal, curr)`. `AccountHeroCard.kt`: `currencyCode = null` → `currencyCode = curr` in the multi-currency distribution badge.

**Task 6:** `LoansCard.kt`: `currencyCode = null` → `currencyCode = summary.loan.currency` for the principal amount display.

**Task 7:** Full audit of all `formatCurrency` call sites. All calls either pass a non-null currency code or are intentionally unit-less (`ManagementScopeCard` filter min/max amounts, which have no currency context by design).

### File List

- `app/src/main/java/com/osscabinet/ossWallet/ui/components/visualization/charts/AeonTreemap.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/visualization/charts/AeonBarChart.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/visualization/charts/AeonPieChart.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/visualization/charts/AeonLineChart.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/visualization/charts/AeonBarLineChart.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/visualization/charts/AeonChartInteractionPopup.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/types/CategoryTreemapCard.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/types/CategoryPieChartCard.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/accounts/AeonAccountCards.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/types/AccountHeroCard.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/types/LoansCard.kt`
- `_bmad-output/implementation-artifacts/2-5-universal-currency-integrity-and-consistent-formatting.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-06-28: Implemented Story 2.5 — removed `= "USD"` silent defaults from all 5 chart components + interaction popup; fixed Y-axis null currency codes in bar/line/barline charts; passed currency down to AeonTreemap (fixing the Treemap USD regression); replaced manual total formatting with `formatCurrencyAnnotated`; fixed TreemapLegend and CategoryLegend; fixed AccountCards, AccountHero, LoansCard null/missing currency codes; audited all remaining `formatCurrency` calls.
