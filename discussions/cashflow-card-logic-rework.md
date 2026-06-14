# Discussion: Cash Flow Card — Logic & UI Rework

> One ongoing topic, continuously refined. Superseded decisions are kept as history, never deleted.

## Metadata

| Field | Value |
|-------|-------|
| Topic | Cash Flow card multi-currency computation & display rework |
| Status | Active |
| Created | 2026-06-14 |
| Last updated | 2026-06-14 |
| Participants / context | Reconstructed from a prior Claude session whose transcript was lost. Triggered by user observing wrong "THIS MONTH" totals on the dashboard cash-flow card in a multi-currency setup, then escalating into a full logic + UI rethink. |

## Summary

The cash-flow card was producing misleading totals because it collapsed all currencies into a single base-currency net and treated transfers as a third additive type — which double-counts inflow/outflow in single-currency setups and hides the real cash created/destroyed in cross-currency transfers. The rework reframes the card around a **per-currency table**, exposes a **dual display mode** (Type vs Inflow/Outflow) toggleable from card settings, and folds transfer handling differently in each mode. Same-currency transfers are accounting noise and can be excluded via a config toggle in Inflow/Outflow mode. A Σ totals row at the top + optional NET column give a one-glance read of the base-currency picture without lying about per-currency reality. The records screen surfaces the same notion via a `FlowDirection` filter so users can drill from any cell back into the matching transactions.

## Decision Log Summary

| ID | Decision | Status | Date | Supersedes | Superseded by |
|----|----------|--------|------|-----------|---------------|
| D-001 | Reframe cash-flow card as a per-currency table, not a single base-currency net | ACTIVE | 2026-06-14 | — | — |
| D-002 | Reject "fold transfer net into income/expense by sign" as the global fix | ACTIVE | 2026-06-14 | — | — |
| D-003 | Introduce two display modes (Mode A: Type / Mode B: Inflow-Outflow) toggleable in card settings | ACTIVE | 2026-06-14 | — | — |
| D-004 | Same-currency transfers — Mode-B-only toggle to exclude them | ACTIVE | 2026-06-14 | — | — |
| D-005 | Column visibility rules: Mode A follows the transaction-type filter; Mode B is always INFLOW + OUTFLOW | ACTIVE | 2026-06-14 | — | — |
| D-006 | Add a Σ totals row at the top of the table + optional NET column | ACTIVE | 2026-06-14 | — | — |
| D-007 | Records screen / drill-down: surface inflow/outflow via a `FlowDirection` filter | ACTIVE | 2026-06-14 | — | — |

---

## Decisions

### D-001 — Reframe cash-flow card as a per-currency table

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — The card stops presenting a single "income / expense / net in base currency" headline as its primary truth. Its primary UI becomes a tabular breakdown where each row is a currency and the columns describe the flow types or directions for that currency. The base-currency view becomes a derived Σ row, not the central narrative.

**Why** — A "THIS MONTH" dry-run revealed that the old single-line totals are structurally wrong for multi-currency wallets:

- Income and expense in different currencies were summed only after FX conversion, masking per-currency reality.
- Transfers were added on top as a third additive bucket, but cross-currency transfers create real new cash in one currency while removing it in another — that signal vanished after FX-collapsing.
- The cell click-through filter was meaningless because the displayed number didn't correspond to any single coherent slice of transactions.

A per-currency table is the smallest representation that doesn't lie: each row is internally consistent in one currency, and the FX-converted Σ row sits on top for users who want the one-glance base-currency read.

**How** — `CashFlowResult.breakdown: List<CashFlowBreakdownItem>` becomes the primary data structure. `CashFlowCard` renders a `Column` of header → Σ row → per-currency rows, with `horizontalScroll` for the value columns and a sticky CCY column on the left. Each cell shows the native amount and (optionally) the `≈ baseCurrency` conversion underneath.

**Where** — `domain/card/computers/CashFlowCardComputer.kt`, `domain/model/analytics/CashFlowResult.kt`, `domain/service/aggregation/CardAggregationService.computeCashFlowCard(...)`, `ui/screens/dashboard/components/CashFlowCard.kt`.

**When** — Adopted in the rework branch; it is the foundation all later decisions build on.

**Alternatives considered**
- Keep the single-line summary card, fix only the FX math — rejected because the bug isn't only FX; the *model* of "one number per type across all currencies" is what's lying.
- Show only the base-currency Σ row, hide per-currency breakdown — rejected because that throws away the diagnostic value (the user discovered the bug *because* they could see the currencies); cash-flow truth lives at the currency level.

**Open questions / risks** — Horizontal-scroll cells can feel awkward on narrow screens with many columns; mitigated by the column-width tier in [[D-005]] and by keeping the CCY column outside the scroll viewport.

---

### D-002 — Reject "fold transfer net into income/expense by sign"

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — The proposal "sum all transfer legs; if the net is positive add it to income, if negative add it to expense" is rejected as the global fix, though its underlying intuition feeds Mode B in [[D-003]].

**Why** — The proposal resolves the *base-currency* line item (because at base-currency level cross-currency transfers can mathematically net to non-zero and that net is "real" gain/loss). But it fails at the currency-level table required by [[D-001]]:

- At currency level, transfers in the same currency do net to zero by construction — folding them into income/expense would mark a 0 cell as "income/expense" which is wrong semantically.
- At currency level, cross-currency transfer legs are *real* per-currency inflows and outflows. Calling the USD-leg of a USD→EUR transfer "income" because the FX-converted sum is positive is a category error: the USD leg is a transfer-out, full stop. The "gain" only exists when you re-collapse to base currency.
- It also breaks the click-through-to-records filter: a cell labeled "Income" that actually contains transfer transactions would mis-filter the records screen.

**How** — N/A — this is a rejection. The accepted treatment of transfers per mode is described in [[D-003]] and [[D-004]].

**Where** — Applies to the computer logic in `CashFlowCardComputer` and `CardAggregationService.computeCashFlowCard`.

**When** — Decided once it became clear the per-currency table is the correct frame ([[D-001]]).

**Alternatives considered**
- Use the sign-folding rule *only* for the base-currency Σ row, keep per-currency rows untouched — rejected as worse than the current outcome: the Σ row would no longer be the literal sum of the per-currency rows, breaking the invariant that lets users mentally reconcile the table.

**Open questions / risks** — _None._

---

### D-003 — Introduce two display modes: Type vs Inflow/Outflow

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — The card supports two mutually exclusive display modes, toggled in card settings:

- **Mode A — `TYPE_DISTINCTION`**: columns describe the transaction *type* (INCOME / EXPENSE / TRANSFER). The Transfer column is signed (`+` net inflow, `−` net outflow) per currency.
- **Mode B — `INFLOW_OUTFLOW`**: columns describe the *direction* of flow (INFLOW / OUTFLOW). Transfer legs are folded into INFLOW/OUTFLOW by sign at the per-currency level (a USD transfer-in goes to USD's INFLOW; a USD transfer-out goes to USD's OUTFLOW). Income contributes to INFLOW, Expense to OUTFLOW.

**Why** — Two different mental models are equally legitimate and neither dominates:

- *Accounting / categorical*: "how much income vs expense vs transfer activity happened this month" — Mode A serves this. The Transfer column reveals cross-currency rebalancing as a signed quantity per currency.
- *Treasury / liquidity*: "how much cash actually moved in and out of each wallet currency" — Mode B serves this. The user only cares about direction, not whether the cash arrived via salary or a transfer leg.

A toggle resolves the long debate without forcing one camp's frame on the other. The toggle lives in card config so users can also have *two cash-flow cards* on the dashboard, one in each mode.

**How** — `CashFlowDisplayMode { TYPE_DISTINCTION, INFLOW_OUTFLOW }` enum in `CardConfig.kt`. `CashFlowCardConfig.displayMode` defaults to `TYPE_DISTINCTION` (matches the old card's mental model). UI builds the `columns: List<TableColumn>` differently per mode in `CashFlowCard.CashFlowTable`. Mode segmented-button lives in the settings sheet under "Layout".

**Where** — `domain/model/settings/CardConfig.kt` (enum + config field), `ui/screens/dashboard/components/CashFlowCard.kt` (`columns` builder + settings sheet).

**When** — Toggle takes effect immediately on change; no recompute needed because the underlying `CashFlowBreakdownItem` already carries the raw `income / expense / transferIn / transferOut` per currency.

**Alternatives considered**
- Single mode (force the team to pick) — rejected because the discussion never converged on one being "more correct"; they answer different questions.
- Three modes (also expose a base-currency-only single-line mode for nostalgia / glance views) — rejected to keep the card honest; users who want the base-currency glance get the Σ row and optional NET column ([[D-006]]).

**Open questions / risks** — Click-through filter targets differ per mode (see [[D-007]]); must keep them aligned with what the cell semantically represents.

---

### D-004 — Same-currency transfers: Mode-B-only exclusion toggle

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — A `excludeSameCurrencyTransfers: Boolean` toggle on `CashFlowCardConfig`. When ON, same-currency transfers are excluded from the INFLOW and OUTFLOW computations (both per currency and in the Σ). The toggle is **only meaningful in Mode B**; it is shown disabled in Mode A.

**Why** — Same-currency transfers are pure accounting noise in a treasury frame: moving USD 100 from one USD account to another adds 100 to INFLOW and 100 to OUTFLOW for the same currency, inflating both columns symmetrically and making the totals less informative.

- Cross-currency transfers are *not* noise — they create real per-currency inflow on one side and outflow on the other, which is exactly what a liquidity view should show.
- In Mode A the noise problem doesn't exist: the Transfer column shows the signed *net* per currency, so a same-currency round-trip nets to 0 naturally and doesn't inflate anything.

Hence the toggle is gated to Mode B. Default OFF so behavior matches the pure aggregation by default and the user opts in to the cleaner view.

**How** — `excludeSameCurrencyTransfers: Boolean = false` on `CashFlowCardConfig`. `CashFlowBreakdownItem` exposes both `transferIn/Out` and `crossCcyTransferIn/Out` (plus their base-currency variants); the table selects one set or the other inside the column builder for Mode B. The settings-sheet switch is rendered with `enabled = (displayMode == INFLOW_OUTFLOW)` and subtitle "Only used in Inflow / Outflow mode" to explain why it's greyed out otherwise.

**Where** — `domain/model/settings/CardConfig.kt` (config field), `domain/model/analytics/CashFlowBreakdownItem.kt` (parallel cross-ccy fields), `CardAggregationService.computeCashFlowCard` (compute both sets), `CashFlowCard.kt` (`txIn / txInBase / txOut / txOutBase` selector + settings UI).

**When** — Toggle takes effect immediately; no recompute needed because both gross and cross-ccy totals are pre-computed in `CashFlowResult`.

**Alternatives considered**
- Always exclude same-currency transfers in Mode B (no toggle) — rejected because a power user might want to see the raw flow including same-currency movements (e.g. validating that a sweep ran).
- Also expose the toggle in Mode A — rejected as redundant ([[D-003]] reasoning: Mode A's Transfer column already nets same-currency transfers to 0).

**Open questions / risks** — _None._

---

### D-005 — Column visibility rules per mode

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** —

- **Mode A (Type)**: the columns shown are determined by `config.filter.transactionTypes`. If the user's filter includes INCOME, show the INCOME column; same for EXPENSE and TRANSFER. With all three selected the table is 3 columns; with only INCOME, 1 column. If the set is empty, render an empty-state ("No transaction types selected").
- **Mode B (Inflow/Outflow)**: INFLOW and OUTFLOW are **always** shown — two fixed columns — even if a column would compute to all zeros. The `transactionTypes` filter still gates which underlying buckets contribute (e.g. unchecking TRANSFER removes transfer legs from both columns but the columns remain).

**Why** —

- Mode A is the type-aware view: hiding type columns the user explicitly de-selected reduces noise, especially on narrow screens. The filter already exists and is the natural lever.
- Mode B is the direction-aware view: its identity is "two columns, inflow and outflow". Making columns appear/disappear here would break the visual stability of the table and confuse users who chose this mode precisely *to stop thinking about types*.
- Cell width auto-adjusts based on the visible column count (130dp for 1–2 cols, 120dp for 3, 110dp for 4) to keep amounts readable on phone widths without truncation.

**How** — `columns: List<TableColumn>` is built inside a `remember(...)` keyed on `config, types, excludeSameCcy, result, colors`. In Mode B the list is unconditionally `[INFLOW, OUTFLOW]`; the `nativeOf` / `baseOf` lambdas internally gate by `showIncome / showExpense / showTransfer` so unchecking types zeroes contributions without removing the column. Adding the NET column ([[D-006]]) extends the list by 1.

**Where** — `CashFlowCard.CashFlowTable.columns` builder.

**When** — Settled together with [[D-003]].

**Alternatives considered**
- Mode B: hide INFLOW or OUTFLOW if it would be all-zero — rejected: visual instability, breaks the mode's identity, and a zero column is informative in itself ("we received nothing this month").
- Mode A: always show all 3 columns regardless of filter — rejected: violates the filter contract users expect from every other filterable card.

**Open questions / risks** — _None._

---

### D-006 — Σ totals row at top + optional NET column

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** —

- A **Σ row** is rendered immediately under the header, before the per-currency rows. It shows each visible column's total in *base currency* (because summing across currencies only makes sense after FX conversion). It uses a lightly emphasised background to read as a "totals" row.
- An **optional NET column** (toggle `showNetColumn` in config) appears as the last column. NET subtracts EXPENSE/OUTFLOW contributions from INCOME/INFLOW/TRANSFER contributions on a per-row and per-Σ basis. NET cells are signed (`+` / `−` / `—` for zero) and not clickable (no single coherent records-filter for "net").

**Why** —

- Users still want a one-glance answer to "what was my month?" — the Σ row delivers it in base currency without overriding the per-currency truth in the body rows.
- Placing Σ *above* the body (not below) reads as a summary masthead and stays visible when many currencies push the body downwards — better than a footer that scrolls off.
- The NET column makes the row-by-row "did this currency end up positive or negative this month" reading trivially available, but only some users want it; default OFF keeps the table compact.

**How** —

- Σ row pulls each column's `totalInBase` (already pre-computed in the column builder).
- NET column is appended via `columns + listOfNotNull(netColumn)` where `netColumn.nativeOf / baseOf` fold the other columns' values with sign by column-key (`exp` / `out` subtract; everything else adds).
- Zero values render as `—` for visual quietness; signed columns prefix `+` for clarity.

**Where** — `CashFlowCard.CashFlowTable` (Σ-row composable + NET column builder), `CashFlowCardConfig.showNetColumn` + settings-sheet switch.

**When** — Σ row is always on. NET column is opt-in per card.

**Alternatives considered**
- Σ row at the bottom — rejected, see "Why" above.
- Always-on NET column — rejected: takes a column slot many users don't need; on narrow screens it pushes table to 4+ columns and cells get tight.
- Show NET in native + base like body cells — rejected: NET only makes sense after FX, native NET per currency would just be the row's own per-currency arithmetic which the user can do by eye.

**Open questions / risks** — _None._

---

### D-007 — Records screen drill-down via `FlowDirection` filter

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — Cells in the cash-flow table are clickable and navigate to the records screen with a `TransactionFilter` pre-applied. The filter shape depends on the cell:

- Mode A cells set `transactionTypes` to the column's single type (INCOME / EXPENSE / TRANSFER), `currencies` to the row's currency, and `flowDirection = FlowDirection.ANY`.
- Mode B cells set `transactionTypes` to the union of the types feeding that column (INCOME + TRANSFER for INFLOW; EXPENSE + TRANSFER for OUTFLOW, gated by what's visible per the filter), `currencies` to the row's currency, and `flowDirection = FlowDirection.IN` / `FlowDirection.OUT`.

`FlowDirection` is a new top-level filter dimension (enum `IN | OUT | ANY`) so the records screen can faithfully reproduce the Mode B partition: filter to "transfers where this currency *received* funds" without the user having to think about source vs destination accounts.

**Why** —

- Without this, Mode B is a dead-end: a user clicking INFLOW on a EUR row would land on a records screen that can't express "show me transfers where EUR was the destination leg" using only `transactionTypes` — the filter would over-include transfers where EUR was the source.
- A `FlowDirection` filter is also useful in other surfaces (e.g. a planned "inflow / outflow" toggle on the records screen header), so it justifies being a first-class filter dimension rather than a cash-flow-card-only mechanism.
- Setting `FlowDirection.ANY` for Mode A keeps that mode's drill-down semantics simple: "show me everything of this type in this currency".

**How** —

- New enum `domain/model/FlowDirection.kt` with `IN`, `OUT`, `ANY`.
- New field on `TransactionFilter`. Persisted in card config filter where present.
- `TransactionFilterService` (or wherever predicates are applied) interprets `FlowDirection.IN` / `OUT` for TRANSFER transactions by checking destination vs source account-currency vs filtered-currency.
- Cell click callbacks in Mode B (`onClickInflow` / `onClickOutflow`) build the `TransactionFilter` with the correct direction; in Mode A (`onClickIncome` / `onClickExpense` / `onClickTransfer`) they leave `flowDirection = ANY`.

**Where** — `domain/model/FlowDirection.kt` (new), `domain/model/TransactionFilter.kt` (new field), filter-service applying it, `ui/components/cards/CardCallbacks.kt` (CashFlow callbacks), `CashFlowCard.kt` (cell `onClick` wiring).

**When** — Adopted with the rework. Users get the new filter as soon as they update; the records screen needs the matching filter UI (chip / segmented control) to make the dimension discoverable without going via a card cell.

**Alternatives considered**
- Use only `transactionTypes` and rely on `currencies` to disambiguate — rejected: doesn't work for transfers because a transfer touches two currencies and `currencies` alone can't say "this side, not that side".
- Pass a pre-resolved transaction-ID list instead of a filter — rejected: breaks the records screen's "live filter" model and would go stale on new transactions.

**Open questions / risks** — Records-screen UI for the `FlowDirection` chip is a follow-up; in the meantime the dimension is reachable only via card click-through.

## Changelog

| Date | Change |
|------|--------|
| 2026-06-14 | Created file. Reconstructed prior discussion from user-supplied message log. Added D-001 through D-007. |
