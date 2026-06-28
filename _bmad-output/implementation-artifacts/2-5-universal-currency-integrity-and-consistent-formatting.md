# Story 2.5: Universal Currency Integrity & Consistent Formatting

Status: ready-for-dev

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

- [ ] **Task 1 — Remove hardcoded "USD" defaults from Chart Components**
  - [ ] Update `AeonTreemap` signature and `AeonChartInteractionPopup`
  - [ ] Update `AeonBarChart` signature (all overloads)
  - [ ] Update `AeonPieChart` signature
  - [ ] Update `AeonLineChart` signature
  - [ ] Update `AeonBarLineChart` signature

- [ ] **Task 2 — Fix Chart Y-Axis and Label Formatting**
  - [ ] `AeonBarChart.kt`: Replace `currencyCode = null` with `currency` in Y-axis label formatting
  - [ ] `AeonLineChart.kt`: Replace `currencyCode = null` with `currency` in Y-axis label formatting
  - [ ] `AeonBarLineChart.kt`: Replace `currencyCode = null` with `currency` in Y-axis label formatting

### Priority B — Fix Card Call Sites & Legends

- [ ] **Task 3 — Fix CategoryTreemapCard & Legend**
  - [ ] Pass `currency` prop to `AeonTreemap` call
  - [ ] Update `CategoryTreemapCard` to use `formatCurrencyAnnotated` for the TOTAL amount instead of manual building
  - [ ] Update `TreemapLegend` to take `currency` and pass it to `formatCurrency`

- [ ] **Task 4 — Fix CategoryPieChartCard Legend**
  - [ ] Update `CategoryLegend` to take `currency` and pass it to `formatCurrency`

- [ ] **Task 5 — Fix Account Cards & Hero**
  - [ ] `AeonAccountCards.kt`: Pass `curr` (from the loop) to `formatCurrency` in the multi-currency distribution list
  - [ ] `AccountHeroCard.kt`: Pass `curr` (from the loop) to `formatCurrencyAnnotated` in the multi-currency distribution list

- [ ] **Task 6 — Fix Loans Card**
  - [ ] `LoansCard.kt`: Pass `summary.loan.currency` instead of `null` to `formatCurrencyAnnotated` for the principal amount

### Priority C — Audit & Cleanup

- [ ] **Task 7 — Audit remaining formatCurrency(amount) calls**
  - [ ] `grep -r "formatCurrency([a-zA-Z0-9.]*)" app/src` and ensure any call missing the second argument is either intentional or fixed

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
