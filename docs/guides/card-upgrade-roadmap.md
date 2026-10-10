# Card upgrade roadmap

Planned (not yet implemented) upgrades for the dashboard cards, beyond the scrollable charts work.
Each card has a matching `TODO(card-upgrade)` comment above its composable that points back here.

Grep for open items:

```bash
grep -rn "TODO(card-upgrade)" app core
```

## How to work through this

Do one card (or one tightly coupled pair) at a time and finish it before starting the next:

1. Read the card's composable, config and computer.
2. Write an HTML mock and a short list of decisions, and agree them before coding.
3. Implement: config field with a default that keeps today's behavior, settings UI, computer/UI change.
4. Verify on a device or emulator, add JVM tests for any new pure logic, then commit.

Adding a config field does not need a Room migration if the config is `@Serializable` and the new field has a
default (configs are decoded with `ignoreUnknownKeys` and missing fields fall back to their defaults).
Adding a new `CardType` is a different matter and needs the 15-file checklist in `adding-new-card.md`;
none of the items below add a card type.

Settings UI should reuse what the bar chart cards already use in `ui/components/cards/AeonCardSettingsScaffold.kt`:
`CardSettingsSection`, `CardSettingsSegmentedRow` (2 to 3 short options), `CardSettingsChipGrid` (more options), and
`AeonSettingsToggle`. Do not add new dropdown styles.

## Order

| # | Card | Upgrade | Effort |
|---|------|---------|--------|
| 1 | Category Pie and Treemap | Configurable grouping, legend rows and sort, per-category change | Medium |
| 2 | Net Worth | Trend line (done) | Medium |
| 3 | Budgets | Pace marker and over-budget styling (done) | Medium |
| 4 | Planned Payments | Cash needed in the next 7 or 30 days (done) | Small |
| 5 | Loans | Settled filter, sort, segmented type (done; projected payoff left out) | Medium |
| 6 | Currency Balances and Accounts | Sort and hide-zero options | Small |
| 7 | TagUpSet Plot | "How to read this" hint | Small |

Effort for items 2 to 7 is a rough guess. Only the pie and treemap code has been read in detail.

---

## 1. Category Pie and Treemap

Files: `ui/components/cards/types/CategoryPieChartCard.kt`, `CategoryTreemapCard.kt`, `CategoryChartData.kt`,
`domain/service/aggregation/CardAggregationService.kt` (builds the data), configs `CategoryPieChartCardConfig` and
`CategoryTreemapCardConfig` in `core/domain/.../settings/CardConfig.kt`.

### Already there (do not rebuild)

- Slices below 2% merge into a "More Categories" slice that can be tapped to drill in.
- Parent categories drill into subcategories, with breadcrumbs.
- Tapping a slice or legend row opens its records.
- The total shows its change against the previous period.
- The legend shows the top 5 rows, or everything with the `showFullBreakdown` switch.

### Upgrades

1. **Configurable small-slice grouping.** The 2% threshold is a hardcoded literal (`0.02f`) in both cards.
   Add a config field (for example `groupBelowPercent`: Off, 1%, 2%, 5%) with a default of 2% so nothing changes.
2. **Legend rows and sort.** The legend uses a hardcoded `take(5)`. Add a row count (5, 10, All) and a sort
   (Amount, Biggest change, A to Z). Default stays 5 rows by amount. `showFullBreakdown` can map onto "All".
3. **Per-category change.** `CategoryChartData.previousAmount` is already filled for each category but only summed
   into the total. Show a change chip on each legend row (spending up in the error colour, down in the success
   colour). Decisions to confirm: default on or off, and "Biggest change" ranked by absolute amount (recommended)
   or by percentage.

### Notes

- The grouping logic is duplicated in both cards (about 40 lines each). Extract one shared function (taking the
  threshold) before adding the setting, so the two cards cannot drift apart. It is pure Kotlin and should have
  unit tests: threshold boundaries, a single small slice (not merged), previous amounts summed into the group.
- Keep change chips out of the treemap tiles (too small). Legend only.
- Mock: the interactive preview agreed in the design discussion (grouping, rows, sort and change controls).

---

## 2. Net Worth (done)

Files: `NetWorthCard.kt`, `NetWorthCardConfig.showTrendLine`, `domain/card/NetWorthTrend.kt`,
`CardAggregationService.computeNetWorthTrend`, shared `ui/components/visualization/AeonSparkline.kt`.

- A trend line under the figure shows the running balance across the card's period. Dragging along it replaces the
  figure with the value on that date (the comparison line shows the date); releasing snaps back.
- There is no stored balance history. The line is the opening balance (net flow before the period) plus the running
  net flow per bucket, built from one day-by-day query, and uses the same balance semantics as the headline figure.
- **Known limit:** every point is converted with today's exchange rates, because the app keeps only one rate set.
  For multi-currency data the line shows today's value of past holdings, not their value at the time. The setting
  description says so. Fixing it needs stored daily rates.
- The Cash Flow perspective draws the same balance line: the headline figure is the balance in both perspectives.
- `AccountHeroCard` now uses the shared `AeonSparkline` (no scrubbing there).
- Tests: `NetWorthTrendTest`. Not done: tap-through to a full chart (no such screen exists).

---

## 3. Budgets (done)

Files: `BudgetsCard.kt`, `BudgetService.elapsedFraction` / `paceStatus`, `BudgetsCardConfig` (`showPaceMarker`,
`showWarningStyle`, `sortBy`), `BudgetProgress.elapsedFraction`.

- A tick on each bar shows how far through the period you are today, with an ON TRACK / AHEAD OF PACE / NEAR LIMIT / OVER chip.
  Ahead of pace means spending is more than 10 points ahead of the elapsed share. All Time budgets have no pace.
- Amber styling uses each budget's own alert threshold (previously only "exceeded" was styled).
- Order setting (As saved / Most used / Over pace) sorts before the "max items" cut.
- Tests: `BudgetPaceTest`.

## 4. Planned Payments (done)

Files: `PlannedPaymentsCard.kt` (`PlannedPaymentsSummaryStrip`), `domain/card/PlannedPaymentsSummary.kt`,
`PlannedPaymentsCardConfig` (`summaryDays`, `countOverdueInSummary`).

- A strip above the list shows Out, In and Net for the next 7 or 30 days (default 7, can be off), worked out from every
  payment due in the window, not just the rows shown. Transfers and paused payments are left out.
- Overdue expenses are shown beside the heading by default and can be counted into Out.
- Tests: `PlannedPaymentsSummaryTest`.

## 5. Loans (done, without projected payoff)

Files: `LoansCard.kt`, `LoanService` (`sortByDueDate`, `sortByName`, `sortByOwed`), `LoansCardConfig` (`showSettled`, `sortBy`).

- Settled loans are hidden by default (toggle to show them, with a SETTLED chip).
- Show Type is now the same segmented control as other cards; new Order setting applied before the "max items" cut.
- **Not built, on purpose:** a projected payoff date. If it is added later, use the loan's expected settlement date and
  fall back to the repayment pace over the last 6 months; the pace needs one extra day-by-day query per loan.
- The paid progress bar and principal already existed.
- Tests: `LoanOrderingTest` and a Loans case in `CardAggregationServiceTest`.

## 6. Currency Balances and Accounts

Files: `CurrencyBalancesCard.kt`, `AccountsCard.kt`, and their configs.

- Sort options (by balance, name, or recent activity) on both.
- Hide zero balances on Currency Balances. This is the cheapest item on the list.
- Currency Balances config today: `layout`, `gridColumns`, `showBaseConversion`.

---

## 7. TagUpSet Plot

Files: `TagUpSetPlotCard.kt`, `AeonUpSetPlot.kt`.

- Already highly configurable. Add only a short "how to read this" hint, since UpSet plots are unfamiliar.
- Do not add more settings.

---

## Cross-cutting ideas

- **Tap-through everywhere.** The bar and line charts open Records from the tooltip, and the pie does from a slice.
  Treemap tiles, Budgets rows and Loans rows could do the same where they do not already.
- **Empty states.** Replace blank space with a short message that says what is missing and what to try
  (for example widen the time range).
- **Reuse the chart scroll helpers** (`AeonChartScroll.kt`: scroll state, Y-axis zoom) for any new time-series chart,
  such as a budget-over-time chart.
- **Web payload.** `data/web/WebMappingUtils.kt` and `CardDataDto.kt` mirror card configs for the companion web
  client. New config fields are not sent unless added there. The chart settings added so far were left out on purpose.
