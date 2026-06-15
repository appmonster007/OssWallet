# Discussion: Filter Sheet — Filter Defaults and Activation

> One ongoing topic, continuously refined. Superseded decisions are kept as history, never deleted.

## Metadata

| Field | Value |
|-------|-------|
| Topic | Filter sheet defaults, empty-selection semantics, and active-filter definition |
| Status | Active |
| Created | 2026-06-14 |
| Last updated | 2026-06-15 |
| Participants / context | User-driven rework of the Transactions filter sheet to give explicit control over filter membership and to define a precise "active filter" rule. |
| Implementation plan | [`filter-sheet-filter-defaults-and-activation.plan.md`](./filter-sheet-filter-defaults-and-activation.plan.md) — self-contained execution plan derived from D-001..D-007. |

## Summary

Today the filter sheet auto-substitutes default values when a multi-select dimension is empty, which silently strips the user's ability to express an intentional empty set. The new direction makes empty selection a **first-class, literal** filter state: it actually filters everything out. Defaults are restored only by an explicit Reset. A filter is "active" iff its current content differs from the **dimension-specific resolved default** (content equality, not identity — manually ticking exactly the default is treated as inactive). The default is curated per dimension: **accounts → active accounts only** (archived excluded unless the user opts them in); categories, transaction types, currencies, payment methods, statuses → the full domain set. Tags is the exception — it carries an OR/AND mode toggle, defaults to **empty selection** (which means "show all, including untagged"), and only becomes constraining once at least one tag is picked. The data shape is a single `data class Selection<T>(values: Set<T>)`; there is no "no constraint" variant — SQL always emits `IN (values)`. Selections apply live, Reset applies immediately, and when a filter excludes everything the UI surfaces a hint with a Reset CTA. Date/amount/search are deferred and keep current behavior.

## Decision Log Summary

| ID | Decision | Status | Date | Supersedes | Superseded by |
|----|----------|--------|------|-----------|---------------|
| D-001 | Empty selection is a valid, literal filter state — no silent fallback to defaults | ACTIVE | 2026-06-14 | — | — |
| D-002 | OR-logic dimensions default to all-selected; active = deviation from default; reset is the only path back to default/inactive | DEPRECATED | 2026-06-14 | — | D-008 |
| D-003 | Tags: OR/AND mode toggle; empty tag selection always means "show all (including untagged)"; any selected tag excludes untagged | ACTIVE | 2026-06-14 | — | — |
| D-004 | Sheet open/apply behavior: reopen shows current state, selections apply live, Reset applies immediately | ACTIVE | 2026-06-14 | — | — |
| D-005 | When the active filter yields zero results, show an inline "your filter excludes everything — Reset?" hint | ACTIVE | 2026-06-14 | — | — |
| D-006 | Date range, amount range, search text are out of scope for this rework; active-filter indicator surface revisited later | ACTIVE | 2026-06-14 | — | — |
| D-007 | Rollout: per-dimension incremental pilot (start with accounts), then one cohesive change to roll the rest | ACTIVE | 2026-06-14 | — | — |
| D-008 | Default is a dimension-specific curated baseline (active-only for accounts; full domain for others); activation is content-equality vs that resolved default; `Selection<T>` simplifies to a single `data class Selection<T>(values: Set<T>)` — no "All" variant | ACTIVE | 2026-06-15 | D-002 | — |

---

## Decisions

### D-001 — Empty selection is a valid, literal filter state

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — For multi-select OR-logic dimensions on the Transactions filter sheet (accounts, categories, types, currencies, payment status, payment method), an empty selection is honored literally: zero values means zero matches on that dimension. The system must not auto-substitute defaults to "rescue" an empty set.

**Why** — Today, deselecting all values triggers a silent re-population with defaults, which removes the user's ability to express "match nothing on this dimension." The user wants complete, explicit control over filter membership. Empty must be expressible as a deliberate user choice and must be distinguishable from "all selected."

**How** — Remove the fallback in the filter-state reducer / VM that swaps empty sets for defaults. The query layer (`TransactionQueryBuilder` / `TransactionFilterService`) already supports literal `IN ()` semantics; the change is in how the UI/VM produces the filter object — never coerce empty → default. Defaults are restored only via the Reset action (see D-002).

**Where** — Transactions filter sheet and its backing ViewModel. Applies to all OR-logic multi-selects on that sheet: accounts, categories, types, currencies, payment status, payment method. Does not apply to tags (D-003) or to date/amount/search (D-006).

**When** — Applies whenever the user mutates a multi-select inside the sheet. The empty state is preserved across sheet close/reopen until the user changes it or hits Reset.

**Alternatives considered**
- Keep auto-fallback to defaults when empty — rejected because it overrides user intent and makes "empty" inexpressible.
- Treat empty as "no constraint" (equivalent to all-selected) — rejected because it collapses two distinct user intents (deliberately filter to nothing vs. apply no filter) into one, and contradicts the new activation rule in D-002.

**Open questions / risks** — Users who land in empty-by-accident will see zero results; mitigated by D-005's inline hint.

---

### D-002 — Default = all-selected for OR-logic dimensions; active = deviation; Reset is the only path back

- **Status:** DEPRECATED
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** D-008

_Deprecated because: "default = all options selected" is too coarse — for accounts the curated default is **active-only**, not the full domain (archived accounts must not leak into the default result). The activation rule and the role of Reset still hold, but they are restated more precisely in D-008 alongside the simplified `Selection<T>` shape (no "All" variant)._

**What** — The default state for each OR-logic dimension (accounts, categories, types, currencies, payment status, payment method) is **all options selected**. A filter dimension is considered "active" iff its current state differs from this default. Therefore: all-selected = inactive; any subset (including empty) = active. The Reset button is the only mechanism that returns the dimension to the all-selected default state and clears its active status.

**Why** — All-selected is semantically equivalent to "no constraint imposed on this dimension," so it is the correct identity / inactive baseline. Defining active as "any deviation" (subset *or* empty) keeps the rule simple, deterministic, and consistent with D-001 — empty is a real, distinct user choice and must light up as active. Making Reset the single source of default restoration removes ambiguous code paths and prevents the silent-fallback bug from re-emerging.

**How** —
- VM seeds each dimension with the full domain set at the moment the sheet is first opened (or after Reset).
- Active-state computation: compare current selection set to the default set; not-equal ⇒ active.
- The aggregate "filter is active" flag = OR across all dimensions' active flags (plus tags per D-003, plus deferred dimensions per D-006 keeping their current rule).
- Reset action restores every dimension to its default and emits an inactive aggregate state.

**Where** — Filter sheet ViewModel + the active-filter chip/indicator consumer. Query layer is unaffected — it already takes whatever set it is given.

**When** — Active-state recomputed on every selection mutation. Defaults are evaluated at sheet open (using the currently available domain options for that profile) and on Reset.

**Alternatives considered**
- Default = empty for OR-logic dimensions — rejected because empty would then mean both "default" and "user-deselected everything," collapsing two intents.
- Active = "non-empty subset only" (i.e., empty treated as inactive) — rejected because empty is a deliberate user choice (D-001) and must be visible as an applied filter.
- Apply-button to commit Reset — rejected per D-004 (live apply model).

**Open questions / risks** — When the underlying domain set grows (e.g., a new account is added) while a filter is active, decide whether to auto-include the new option in the active selection or leave it excluded. Not resolved here; flagged for follow-up.

---

### D-003 — Tags: OR/AND mode toggle; empty tag selection = show all (including untagged)

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — Tags is a special dimension with its own mode toggle (OR vs AND) and inverted default semantics relative to the other multi-selects:
- **Empty tag selection in either mode** ⇒ show all transactions, *including untagged ones* (i.e., tags impose no constraint).
- **OR mode + ≥1 tag selected** ⇒ transaction must carry **at least one** of the selected tags; untagged transactions are excluded.
- **AND mode + ≥1 tag selected** ⇒ transaction must carry **all** selected tags; untagged transactions are excluded.

Default state for tags = empty selection. Active state for tags = any tag is selected (and optionally: mode toggled away from its default — see open question).

**Why** — Tags are fundamentally different from accounts/categories/types: they are an optional, many-per-row attribute, and many transactions legitimately have no tags. "All tags selected" would not mean "no constraint" — it would mean "must carry every tag in the system," which is essentially never true. The natural identity / no-constraint state for tags is therefore empty. Once the user actively picks tags, they have signaled they want to constrain on tag membership, and untagged rows correctly drop out under both OR and AND. The mode toggle gives the user precise control over multi-tag intent without overloading selection semantics.

**How** — VM holds `tagSelection: Set<TagId>` and `tagMode: AND | OR`. Filter service translates `(tagSelection, tagMode)` into the WHERE clause:
- empty ⇒ no tag predicate emitted (every row passes, tagged or not).
- non-empty + OR ⇒ `EXISTS (... tag IN (...))`.
- non-empty + AND ⇒ one `EXISTS` per selected tag (all must hold).
Active flag for tags = `tagSelection.isNotEmpty()` (mode-change-only activation deferred — see open question).

**Where** — Filter sheet (Tags section + mode toggle), filter ViewModel, `TransactionFilterService` / `TransactionQueryBuilder` tag predicate emission.

**When** — Applies on every mutation of the tag set or the mode toggle. Reset clears the tag set to empty and restores the default mode.

**Alternatives considered**
- Make tags follow the OR-logic default (all selected) — rejected because "all tags selected" has no useful real-world semantic and would always return ~zero transactions.
- Treat empty as "untagged only" — rejected because it conflates "no constraint" with "constrain to untagged"; untagged-only is a separate feature that, if needed, should be a dedicated toggle.
- Single mode (OR only) — rejected because the user explicitly wants AND-mode capability for compound tag queries.

**Resolved**
- Default mode on first open / after Reset = **OR**.
- Toggling the mode while the tag set is empty is **not** active — empty-set behavior is identical under both modes, so the toggle has no observable effect and must not light up the active indicator.

**Open questions / risks**
- UI affordance for the mode toggle inside the Tags section needs design (chip pair, segmented control, etc.).

---

### D-004 — Sheet open shows current state; selections apply live; Reset applies immediately

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** —
- When the user reopens the filter sheet while a filter is active, the sheet renders the **current active state**, not the defaults, so refinement is possible without losing context.
- There is no Apply button — selections **apply live** to the underlying list as the user toggles them.
- The Reset button **applies immediately**: it restores all defaults and updates the list in one shot, with no separate confirm step.

**Why** — Live application matches modern Android filter UX, removes a redundant click, and makes the result of each selection immediately visible (which also makes the D-005 empty-result hint usable as a feedback loop). Showing current state on reopen prevents the user from having to reconstruct their filter from scratch when they want to tweak it.

**How** — VM exposes a single filter `StateFlow` that the sheet binds bidirectionally; each user gesture mutates it, and the list collector recomputes downstream. Reset is a single action on the same StateFlow.

**Where** — Filter sheet composable + its ViewModel; transactions list collector.

**When** — Live apply runs on every user gesture inside the sheet. Reset runs on its single click.

**Alternatives considered**
- Apply button with a draft model — rejected as unnecessary friction and inconsistent with the live-feedback intent.
- Reset to draft + require Apply to commit — rejected for the same reason.

**Open questions / risks** — None for this scope.

---

### D-005 — Empty-result hint: "your filter excludes everything — Reset?"

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — When the active filter combination yields zero transactions, the Transactions list shows an inline hint reading roughly *"your filter excludes everything — Reset?"* with the Reset action attached as a CTA.

**Why** — D-001 makes empty-set filtering possible, which means a user can land on a zero-row list either deliberately or accidentally. The hint provides a graceful escape hatch without overriding the user's choice (no silent fallback) and disambiguates "no data" from "filter is too tight."

**How** — Transactions list empty state checks whether the aggregate filter is active; if active *and* the result set is empty, render the hint variant of the empty state with the Reset CTA wired to the same Reset action used by the sheet.

**Where** — Transactions screen empty state.

**When** — Whenever the visible result set is empty and at least one dimension is in an active (non-default) state.

**Alternatives considered**
- Toast / snackbar on first zero-result render — rejected as transient and easy to miss.
- Auto-reset after a delay — rejected because it would re-introduce the silent override that D-001 explicitly removes.
- Plain "no transactions" empty state — rejected because it fails to distinguish filter-caused emptiness from genuinely empty data.

**Open questions / risks** — Final copy and visual treatment to be designed.

---

### D-006 — Out-of-scope for this rework: date/amount/search; active-indicator UI surface

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — Date range, amount range, and search text on the filter sheet keep their **existing** logic and are not part of this rework. The visual surface that indicates "filter is active" (chip / dot / count) is also out of scope and will be revisited later.

**Why** — Those dimensions do not fit the "selection set with all-selected default" model and need their own design pass. Bundling them here would dilute the rework. The active-indicator visual is a separable concern from the activation *rule*, which is what this discussion settles.

**How** — Leave current implementations untouched; do not block on them.

**Where** — Filter sheet (deferred sections), active-filter indicator on Transactions screen.

**When** — Future iteration, no committed date.

**Alternatives considered**
- Redesign date/amount/search in this pass — rejected to keep scope tight and shippable.

**Open questions / risks** — Eventually need a consistent "default vs deviation" rule for range-style inputs (e.g., default = unbounded range; active = any bound set).

---

### D-007 — Rollout strategy: incremental pilot then cohesive rollout

- **Status:** ACTIVE
- **Date:** 2026-06-14
- **Supersedes:** — &nbsp;&nbsp; **Superseded by:** —

**What** — Land the rework in two phases:
1. **Pilot (incremental):** apply the new rules to a single dimension first — **accounts** — end-to-end (VM seeding, sheet UI, activation rule, query path, empty-state hint). Use it as a live testbed to surface edge cases (empty-set query, sheet reopen state, Reset behavior, active-indicator semantics, profile-switch interaction, domain-set growth handling from D-002's open risk).
2. **Cohesive rollout:** once the pilot is stable and edge cases resolved, apply the same pattern to the remaining OR-logic dimensions (categories, types, currencies, payment status, payment method) and to tags (per D-003) in one cohesive change.

**Why** — The activation rule is global, but the failure modes (silent fallback, profile interactions, query-builder behavior on empty `IN ()`, indicator flicker) are easier to find and fix on one dimension than on six simultaneously. A vertical pilot also lets us pin down the shared abstractions (default-set provider, activation comparator, reset action shape) before they get copy-pasted across dimensions. Doing the rest as one cohesive change avoids a long-lived half-migrated state.

**How** —
- Pilot PR: accounts only. Includes shared abstractions in a form that can be reused.
- Validate against checklist: empty selection persists across reopen, Reset restores all-selected + inactive, active-indicator flips correctly, empty-result hint appears, no profile-cross-contamination, query returns 0 rows on empty set.
- Rollout PR: apply to categories, types, currencies, payment status, payment method, tags in one change reusing the pilot's abstractions.

**Where** — Filter sheet, filter ViewModel, `TransactionFilter`, `TransactionFilterService`, `TransactionQueryBuilder`, Transactions list empty-state.

**When** — Pilot first; rollout gated on pilot stability (manual verification at minimum; ideally a couple of unit/UI tests around the activation rule).

**Alternatives considered**
- All dimensions in one change — rejected because edge cases compound and rollback becomes harder.
- Per-dimension across many small PRs (one per dimension) — rejected as churn; once the pilot validates the pattern, a single rollout is cleaner.

**Open questions / risks** — Need to decide where the shared "default-set provider" lives (VM vs domain service); pilot will surface the right home.

---

### D-008 — Default is a dimension-specific curated baseline; activation is content-equality vs that resolved default; `Selection<T>` simplifies to a single data class

- **Status:** ACTIVE
- **Date:** 2026-06-15
- **Supersedes:** D-002 &nbsp;&nbsp; **Superseded by:** —

**What** — Two coupled refinements to the activation model and the data shape:

1. **Default is dimension-specific and curated.** It is not "every option in the domain." A `DefaultSetProvider` per dimension yields the baseline content:
   - **Accounts** → active accounts only (`accounts.filter { !it.isArchived }.map { it.id }.toSet()`). Archived accounts are visible in the sub-sheet (unchecked) and the user can opt them in, but they are **excluded by default** so their transactions never leak into "all".
   - **Categories, transaction types, currencies, payment methods, statuses** → the full domain set (no curation; default = everything).
   - **Tags** is governed by D-003 (different rule entirely; default = empty).
2. **Activation is decided by content equality with the resolved default.** A dimension is active iff `current.values != defaultProvider().values`. If the user manually ticks exactly the default set (e.g. exactly the active accounts on the accounts sub-sheet), the dimension is treated as **inactive** — same observable state ⇒ same activation indicator.
3. **`Selection<T>` data shape is simplified to a single class:**
   ```kotlin
   @Serializable data class Selection<T>(val values: Set<T>)
   ```
   There is no `All` variant. SQL is always emitted as `IN (?…?)` over `values` (empty → `IN ()` / `AND 0` per pilot risk R-1). The "no constraint" intent is expressed as a Selection whose values equal the resolved default; the query is still emitted, it just constrains to the default set (which for accounts is the right behavior, and for full-domain dimensions is a few more bound parameters — acceptable).

**Why** — The original D-002 wording ("default = all options selected") collapsed two distinct cases:
- Dimensions where "the default content" equals the full domain (categories, types, …).
- Dimensions where "the default content" is a curated subset (accounts: active-only).

Treating them uniformly under `Selection.All = no predicate` caused archived transactions to bleed through whenever the accounts filter sat at its default. Making default content **explicit per dimension** removes that bug at the source and keeps the SQL contract uniform. Dropping the `All` variant removes a footgun (you can no longer accidentally emit "no predicate" when you meant "the default predicate") and makes the type system simpler. Content-equality activation matches user expectations: two states that produce identical results should look identical in the active-filter UI.

**How** —
- Introduce `Selection<T>` as the simplified data class.
- Introduce a per-dimension `DefaultSetProvider` (in the VM/composable layer that has access to the live domain — e.g. accounts list, categories list).
- `Selection.isDefault(defaults: Set<T>): Boolean = values == defaults` — used by `countActiveFilters` and by the active-indicator surface.
- Reset action assigns `Selection(defaultProvider())` for the dimension (per screen scope — some screens pass a non-default `baseFilter` and reset returns to that).
- SQL emitter always emits `IN (values)` with the literal `values` (empty allowed).

**Where** — `TransactionFilter`, the new `Selection<T>` type, `TransactionQueryBuilder`, `TransactionFilterService`, `FilterFields` sub-sheets, `FilterSheetContent` chip rendering, `countActiveFilters`, the screen-level default provider wiring.

**When** — Replaces D-002 for the pilot (accounts only) and the cohesive rollout (other 5 OR dimensions). Tags continues under D-003 unchanged.

**Alternatives considered**
- Keep `Selection.All` and resolve it to the default at SQL-emit time — rejected because the "All" name suggests "no constraint," misleading anyone reading the code; better to make the default content explicit at every layer.
- Identity-based activation (any explicit Subset is active even if its content equals the default) — rejected because it would mark a manually-ticked "all active accounts" as active despite being observationally identical to the default; confusing UX.
- A boolean `isDefault` flag on `TransactionFilter` per dimension — rejected as redundant when content equality is cheap and unambiguous.

**Open questions / risks**
- For dimensions whose domain is unbounded or expensive to enumerate (none today, but flagged for future): emitting `IN (full-domain)` may bloat queries. Not a concern at current scales.
- Carries forward D-002's open question: when the domain grows mid-session (e.g. a new account is added) while a filter is active, the default content changes. Should the active filter's `values` migrate? Default behavior: **no migration** — the user's explicit selection is preserved verbatim; only Reset rebases to the new default.

---

## Changelog

| Date | Change |
|------|--------|
| 2026-06-14 | Created file; added D-001 through D-006 capturing the empty-selection rework, OR-logic defaults, tags AND/OR semantics, live-apply behavior, empty-result hint, and out-of-scope deferrals. |
| 2026-06-14 | Resolved D-003 open questions: tags default mode = OR; mode-only toggle on empty set is not active. Added D-007 for incremental-pilot-then-cohesive-rollout strategy. |
| 2026-06-14 | Added [`filter-sheet-filter-defaults-and-activation.plan.md`](./filter-sheet-filter-defaults-and-activation.plan.md) — concrete execution plan with verified file:line references, `Selection<T>` sealed-wrapper architecture, accounts-only pilot steps, validation checklist, cohesive rollout steps, risk register, and resume protocol. |
| 2026-06-15 | Deprecated D-002 and superseded by D-008 after worked-example review surfaced that "default = all options" leaks archived account transactions. D-008 introduces a dimension-specific `DefaultSetProvider`, content-equality activation, and a simplified `data class Selection<T>(values: Set<T>)` (no `All` variant). Plan §2 and §3 rewritten to match. |
