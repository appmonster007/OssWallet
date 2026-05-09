---
name: Cash Flow Widget Implementation
description: Key decisions and conventions established when implementing the CashFlowGlanceWidget
type: project
---

The Cash Flow widget was added alongside the existing Net Worth widget. Key facts that are non-obvious from the code:

**Work name**: `WidgetRefreshWorker.WORK_NAME` = `"net_worth_widget_refresh"` — this single WorkManager unique work name covers BOTH the Net Worth and Cash Flow widgets. Both widgets are refreshed inside the same `doWork()` run. Do not create a separate work name for Cash Flow.

**No config activity**: Cash Flow widget is intentionally config-free. It always shows the current month (`TimelineRange.ThisPeriod(ThisPeriodType.MONTH)`). If a configurable period is needed later, follow the `NetWorthWidgetConfigActivity` + `NetWorthWidgetPrefsStore` pattern.

**Per-currency breakdown format**: The `CURRENCY_LIST` pref key holds a JSON array string produced with `org.json.JSONArray` (no external lib). Each entry has `"code"` (ISO currency) and `"amount"` (pre-formatted, sign-prefixed). Parsed back via `parseCurrencyList()` inside `CashFlowGlanceWidget.kt`.

**Sign convention**: `NET_POSITIVE = "true"` when `netInBase.signum() >= 0`. The net amount stored in `NET_AMOUNT` is always the absolute value, formatted with `+`/`-` prefix prepended by the worker. Green color for positive, red for negative via `R.color.widget_income_color` / `R.color.widget_expense_color`.

**Data source**: `CardAggregationService.computeCashFlowCard()` with a fixed `CashFlowCardConfig(cardId = "cf_widget")`. Common data loaded via the existing `WidgetDataLoader.loadCommon()`.

**Why:** Cash Flow widget shares the single WorkManager work slot so burst transaction saves coalesce into one refresh for all widgets, not one per widget type.

**How to apply:** When adding future widgets, extend the same `WidgetRefreshWorker.doWork()` block — do not create additional unique work names unless the widget has genuinely independent refresh timing.
