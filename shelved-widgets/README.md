# Shelved widgets

> **Update:** Budgets was restored and redesigned (see `app/.../widget/Budgets*`); ignore it in the lists below.

Everything here was pulled out of the active build on 2026-10-08, keeping only the
**Net Worth** home screen widget wired up. Nothing was deleted — these are the exact files
that used to live under `app/src/main/`, just moved aside so they don't compile/package.

## What's here

- `java/` — every widget `.kt` file except Net Worth's own triad and the shared infra
  (`BaseWidgetProvider`, `BaseWidgetConfigActivity`, `WidgetContract`, `WidgetDataLoader`,
  `WidgetPrefsStore`, `WidgetRefreshWorker`, `WidgetConfigComponents`,
  `WidgetConfigScreenPreviews`, `WorkManagerWidgetRefreshTrigger`, which all stayed active).
  Covers: Cash Flow, Recent Transactions, Budgets, Planned Payments (+ its action
  `BroadcastReceiver`), Currency Balances, Loans, Accounts, Account Hero, Income/Expense Bar,
  Category Pie Chart, Category Treemap, Cash Flow Comparison, Category Spending Trend, Tag
  UpSet Plot, and every shared chart-bitmap renderer (`WidgetSparklineRenderer`,
  `WidgetLineAreaChartRenderer`, `WidgetBarChartRenderer`, `WidgetStackedBarChartRenderer`,
  `WidgetDonutChartRenderer`, `WidgetTreemapRenderer`, `WidgetUpSetPlotRenderer`,
  `CategoryChartDataMapper`).
- `res/layout/` — every `widget_*.xml` layout except the `widget_net_worth*.xml` ones.
- `res/xml/` — every `*_widget_info.xml` except `net_worth_widget_info.xml`.
- `res/drawable/` — widget drawables not referenced by the Net Worth layouts (Net Worth keeps
  `widget_dashed_divider.xml`, `widget_metric_tile_bg.xml`, `widget_netflow_pill_bg.xml` in
  `app/src/main/res/drawable/`).
- `restore.patch` — a `git diff` of the shelving commit. Reverse-apply it to bring everything
  back in one shot.

## What was trimmed in the active code (not moved, edited in place)

- `app/src/main/AndroidManifest.xml` — removed the `<receiver>`/`<activity>` entries for every
  widget except `NetWorthWidgetProvider`/`NetWorthWidgetConfigActivity` (and removed the
  `PlannedPaymentsWidgetActionReceiver` entry, since that whole widget is shelved).
- `app/src/main/java/.../widget/WidgetRefreshWorker.kt` — `doWork()` now only calls
  `refreshWidget(...)` for Net Worth.
- `app/src/main/java/.../widget/WidgetConfigScreenPreviews.kt` — removed the Cash Flow /
  Recent Transactions `@Preview` composables (they referenced shelved `ConfigScreen` functions).

Not touched: `HiltEntryPoints.kt` (the extra repository accessors it gained for the
shelved widgets — `budgetRepository`, `loanRepository`, `plannedPaymentRepository` — are
harmless unused members, left in place rather than risk a bad edit), `res/values/colors.xml`
(the widget color tokens added for the shelved widgets are unused but harmless).

## Restoring

Two ways:

1. **Reverse-apply the patch** (fastest, does everything in one step):
   ```
   git apply -R shelved-widgets/restore.patch
   ```
   from the repo root, then delete the now-empty `shelved-widgets/` directory.

2. **By hand**: `git mv` each file in `shelved-widgets/java/` back to
   `app/src/main/java/com/osscabinet/ossWallet/widget/`, same for `res/layout/`, `res/xml/`,
   `res/drawable/`; re-add the manifest `<receiver>`/`<activity>` entries and
   `WidgetRefreshWorker`/`WidgetConfigScreenPreviews` wiring (see the git history right before
   this shelving commit for the exact content, or just use the patch).

Either way, run `./gradlew :app:assembleDebug` afterward to confirm everything still compiles.
