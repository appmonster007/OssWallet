# Plan: Filter Sheet — Defaults & Activation Rework

> **Companion to** [`filter-sheet-filter-defaults-and-activation.md`](./filter-sheet-filter-defaults-and-activation.md).
> Self-contained execution plan. Read this file + the decisions file and you have full context to implement without re-deriving anything from the conversation.

## 0. Goal (one line)

Make **empty selection** a first-class, literal filter state on the Transactions filter sheet; **default = all-selected** (for OR-logic dimensions); **active = deviation from default**; **Reset** is the only path back to default. Tags follows a special rule (empty = show all, OR/AND toggle with default = OR).

Decisions this implements: **D-001..D-007** in the companion file.

---

## 1. Current state (verified by code inspection on `feature/tagv40`, 2026-06-14)

### 1.1 The silent-fallback bug lives in `FilterFields` (UI layer)

`app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/FilterComponents.kt` — the `FilterFields` composable opens each multi-select sub-sheet using the pattern:

```kotlin
selectedIds = filter.X.ifEmpty { allX },
onSelectionChange = { ids ->
    onFilterChange(filter.copy(X = if (ids == allX || ids.isEmpty()) emptySet() else ids))
}
```

Repeats verbatim for **6 dimensions** at these locations:

| Dimension       | Sheet block start | Coercion line   |
|-----------------|-------------------|-----------------|
| Accounts        | ~L109             | L117 `if (ids == activeAccountIds) emptySet() else ids` |
| Categories      | ~L126             | L132 `if (ids == allCategoryIds \|\| ids.isEmpty()) emptySet()` |
| Currencies      | ~L141             | L156 same shape |
| PaymentMethods  | ~L164             | L169 same shape |
| Statuses        | ~L177             | L182 same shape |
| Types           | ~L190             | L198 same shape |

Each one normalizes "all selected" **and** "none selected" both back to `emptySet()`. That's the bug — the empty user intent is collapsed.

### 1.2 The data layer treats empty as "no constraint" everywhere

This is the bigger blast radius. The convention `empty Set = match all` is hard-coded across all consumers:

- **`domain/model/TransactionFilter.kt`** — comment on `transactionTypes`: `// if empty, matches all`. All set-typed fields default to `emptySet()`.
- **`domain/service/filter/TransactionFilterService.kt`** — every dimension check: `filter.X.isEmpty() || tx.Y in filter.X`. Empty short-circuits to true.
- **`data/local/query/TransactionQueryBuilder.kt`** — every dimension predicate guarded by `if (filter.X.isNotEmpty()) { … IN (…) }`. Empty → no predicate emitted → no constraint.
- **`TransactionFilter.merge` / `mergeSets`** (lines 42–70) — explicit "empty means don't constrain" merge semantics.
- **`ui/components/transactions/FilterUtils.kt:countActiveFilters`** — already uses `filter.X != baseFilter.X` (good — matches D-002), so this helper is mostly OK and will work for empty-as-active if and only if base is non-empty. **But** when the underlying convention changes, "base" must mean "all-selected for that profile," not `TransactionFilter()`'s `emptySet()`.
- **`ui/components/transactions/FilterUtils.kt:removeFilter`** — chip-removal helper subtracts a value from the set. With the new model, removing the last value yields an empty `Set` that must remain empty (not snap to all).

### 1.3 Tags state today

- `tags: Set<String> = emptySet()`, `matchAllTags: Boolean = true` (AND is the current default — **D-003 changes this to OR**).
- Filter logic in `TransactionFilterService` (L78–82):
  ```kotlin
  val matchesTags = if (filter.matchAllTags) {
      tx.tags.containsAll(filter.tags)              // empty set ⇒ containsAll([]) = true ✓
  } else {
      filter.tags.isEmpty() || tx.tags.any { it in filter.tags }
  }
  ```
  Already correct for "empty tags = match all" in both modes. Only the **default mode** needs flipping.
- Query layer for tags: OR-only (`tags LIKE ?` joined with `OR`); the AND branch is handled at the in-memory layer only. **Not in pilot scope** (pilot is accounts); but flagged for the rollout: need an AND-tag SQL path or accept that tag filtering remains post-query.

### 1.4 Filter sheet is **already live-apply**

`FilterSheetContent` (TransactionFilterSheet.kt branch) calls `applyFilterWithDebounce(localFilter)` on every change. The "Save Preset" button at the bottom saves the filter as a preset — it is **not** an Apply button. **D-004's live-apply requirement is already met**; no Apply button to remove. Reset (`FilterEvent.Reset`) is wired to `localFilter = baseFilter` plus the event — that path needs to be updated to set defaults explicitly (see §3).

### 1.5 Empty-result UX today

The Transactions list empty-state composable does not currently distinguish "filter excludes everything" from "no data." Need to locate it during the pilot (likely in `ui/screens/transactions/` or a shared empty-state component) and add the D-005 hint variant.

---

## 2. Architecture (revised per D-008)

> **Note:** an earlier draft of this plan proposed a sealed `Selection<T> = All | Subset(values)`. That was wrong: for accounts, the default is *active-only* (a curated subset), not "no predicate." Treating `All` as "emit no predicate" would have leaked archived transactions whenever the accounts filter sat at its default. D-008 supersedes D-002 with the simpler shape below.

### 2.1 The `Selection<T>` data shape — single class, no variants

```kotlin
// domain/model/filter/Selection.kt  (NEW)
@Serializable
data class Selection<T>(val values: Set<T>) {
    fun matches(value: T): Boolean = value in values     // empty ⇒ never matches
    fun isDefault(defaults: Set<T>): Boolean = values == defaults
}
```

That's the whole type. No sealed hierarchy, no "All" variant. The set is what the user (or default) chose; SQL emits `IN (values)` literally.

### 2.2 Default content is dimension-specific, supplied by a `DefaultSetProvider`

The default *content* lives outside the filter — it depends on live domain data the filter object can't see. Each consuming screen (or its VM) computes the defaults at construction / Reset time:

| Dimension        | Default content |
|------------------|-----------------|
| Accounts         | `accounts.filter { !it.isArchived }.map { it.id }.toSet()` — **active only** |
| Categories       | `allCategories.map { it.id }.toSet()` — full domain |
| Transaction Types| `TransactionType.entries.toSet()` — full domain |
| Currencies       | `allAvailableCurrencies.toSet()` — full domain |
| Payment Methods  | `PaymentMethod.entries.toSet()` — full domain |
| Statuses         | `TransactionStatus.entries.toSet()` — full domain |
| Tags             | `emptySet()` per D-003 — and **not** wrapped in `Selection<T>`; stays as `Set<String>` with the existing empty-means-no-constraint semantics, because that *is* the natural identity for tags. |

A small helper bundle makes this explicit at one site:

```kotlin
data class FilterDefaults(
    val accounts: Set<String>,
    val categories: Set<Long>,
    val types: Set<TransactionType>,
    val currencies: Set<String>,
    val paymentMethods: Set<PaymentMethod>,
    val statuses: Set<TransactionStatus>,
)
```

Constructed once at the screen/VM level from the live domain flows and threaded into the sheet. Activation checks (`countActiveFilters`) and Reset both consume it.

### 2.3 Activation rule — content equality

A dimension is active iff `current.values != defaults`. This includes:

- Subset of the default (e.g. user unchecked 2 of 5 categories) → active.
- Superset (e.g. user included an archived account on top of the active ones) → active.
- Empty (`Selection(emptySet())`) → active (since defaults are non-empty).
- Exactly the default content → **inactive**, even if the user manually ticked each one.

### 2.4 SQL emission — always literal

```kotlin
val ids = filter.accountSelection.values
val placeholders = ids.joinToString(",") { "?" }
where.append(" AND accountId IN ($placeholders)")
args.addAll(ids)
```

- Default state: emits an `IN` clause over the default set (a few extra bound parameters, but explicit). For accounts this means archived transactions are correctly excluded by default.
- Empty state: emits `accountId IN ()`. **Verification step in §3.3:** confirm Android's SQLite wrapper tolerates the empty `IN`; if it errors, fall back to emitting `" AND 0"` whenever `values.isEmpty()`.

### 2.5 Pilot scope

Introduce `Selection<T>` + `FilterDefaults` and migrate **only** `accountIds → accountSelection: Selection<String>`. Other 5 OR-dimensions stay on the old `Set<T>` shape during the pilot. Tags is unchanged in the pilot. Rollout PR migrates the rest (§4).

---

## 3. Pilot — Accounts dimension end-to-end

Follow these steps in order. Each subsection lists the files touched and the exact mutation.

### 3.1 Add the `Selection<T>` type and `FilterDefaults` helper

- **NEW** `app/src/main/java/com/osscabinet/ossWallet/domain/model/filter/Selection.kt` — single `@Serializable data class Selection<T>(val values: Set<T>)` with `matches(value)` and `isDefault(defaults)` helpers (code in §2.1). No sealed hierarchy needed, so `Json.encodeToString` (used by `TransactionFilterSaver` in `FilterSheetContent.kt` L70–73) just round-trips a `{"values":[…]}` payload.
- **NEW (same file or sibling)** `FilterDefaults` data class (§2.2). Only the `accounts` field is populated and consumed in the pilot; the rest are added in the rollout PR.

### 3.2 Migrate `TransactionFilter.accountIds` → `accountSelection`

- **EDIT** `domain/model/TransactionFilter.kt`:
  - Replace `val accountIds: Set<String> = emptySet()` with `val accountSelection: Selection<String> = Selection(emptySet())`.
    > Note: the data-class default is the *empty* selection, **not** the resolved default content. The empty default is harmless at the model level because the screen/VM always seeds the field with `Selection(filterDefaults.accounts)` before the sheet renders. (Old `TransactionFilter()` constructions that flowed straight into queries will now match zero rows on accounts — find those during the cohesive caller audit and patch them to seed defaults explicitly.)
  - Update `merge()` to merge `accountSelection` correctly: `Selection(a) ∧ Selection(b) = Selection(a ∩ b)` if both non-empty; if either is the empty selection, the result is the empty selection (most restrictive wins — consistent with current `mergeSets` semantics).
  - Update extension helpers `forAccount` / `forAccounts` to return `copy(accountSelection = Selection(setOf(...)))`.
- **EDIT every read of `accountIds`** across the codebase. Use `grep -rn "accountIds" app/src/main` — expect ≈20–40 hits. Each must switch to `accountSelection.values` (read) or `accountSelection.matches(...)`.

### 3.3 Update query + in-memory filter for accounts

- **EDIT** `data/local/query/TransactionQueryBuilder.kt` L122–126 — emit the IN clause unconditionally:
  ```kotlin
  // Always emit. Empty values ⇒ "accountId IN ()" ⇒ zero rows (intentional).
  val ids = filter.accountSelection.values
  if (ids.isEmpty()) {
      where.append(" AND 0")              // R-1 fallback: portable & unambiguous
  } else {
      val placeholders = ids.joinToString(",") { "?" }
      where.append(" AND accountId IN ($placeholders)")
      args.addAll(ids)
  }
  ```
  **Verification step:** confirm `IN ()` vs `AND 0` on the actual SQLCipher build during pilot run. The `AND 0` form is portable; if `IN ()` is accepted we can switch to it for symmetry, but `AND 0` is fine to keep.
- **EDIT** `domain/service/filter/TransactionFilterService.matches()` L67:
  ```kotlin
  val matchesAccounts = filter.accountSelection.matches(tx.accountId)
  ```
  Empty selection ⇒ never matches ⇒ zero rows — matches the SQL path.

### 3.4 Rewire the Accounts sub-sheet (remove silent fallback)

- **EDIT** `ui/components/transactions/FilterComponents.kt` lines ~109–124 (the `"Accounts"` block):
  ```kotlin
  "Accounts" -> {
      AeonAccountSelection(
          accounts = accounts,
          selectedAccountIds = filter.accountSelection.values,   // literal — empty stays empty
          onSelectionChange = { ids ->
              // No coercion. Any user change is reflected as-is, including empty.
              onFilterChange(filter.copy(accountSelection = Selection(ids)))
          },
          onDismiss = { activeSheet = null },
          mode = AeonSelectMode.MULTI,
          title = "Filter Accounts"
      )
  }
  ```
  > **Important seeding precondition:** before the sheet is first rendered, the VM/screen must construct the initial `TransactionFilter` with `accountSelection = Selection(filterDefaults.accounts)`. Otherwise the user sees an empty sub-sheet on first open instead of "all active accounts." This is where the per-screen default wiring lands. Add it to the same `LaunchedEffect` that today initializes the filter, or guard with a one-shot seeding effect when `filter.accountSelection.values.isEmpty() && !sheetEverOpened`. Pick whichever fits the existing VM idiom — pilot will reveal the cleanest insertion point.
- **EDIT** the "Accounts Card" value renderer in the same file (~L337–355) — replace `filter.accountIds.isEmpty() || filter.accountIds == activeIds` checks with comparisons against `filterDefaults.accounts`:
  - `accountSelection.values == filterDefaults.accounts` → `"All Active"` (default — content equality).
  - `accountSelection.values.isEmpty()` → `"None"`.
  - `accountSelection.values.size == accounts.size` → `"All Accounts"` (active + archived all ticked).
  - `accountSelection.values.size == 1` → that account's name.
  - else → `"${accountSelection.values.size} Accounts"`.

### 3.5 Active-filter detection & chip rendering

- **EDIT** `ui/components/transactions/FilterUtils.kt:countActiveFilters` L24 — switch to content-equality vs the resolved default:
  ```kotlin
  // New signature: accept FilterDefaults so per-dimension defaults can be compared.
  // During the pilot, only the accounts comparison uses the defaults; the other 5
  // continue to compare against baseFilter as before.
  if (filter.accountSelection.values != defaults.accounts) count++
  ```
  Update every caller of `countActiveFilters` to pass `defaults`. Most call sites are in `FilterSheetContent` and the Transactions screen — small surface.
- **EDIT** `ui/components/transactions/FilterUtils.kt:removeFilter` L61:
  ```kotlin
  "accountIds" -> filter.copy(
      accountSelection = Selection(filter.accountSelection.values - (value as String))
  )
  ```
  Removing the last value yields `Selection(emptySet())` — preserved literally, as required by D-001 / D-008.
- **EDIT** `FilterSheetContent.kt` L286–301 (Account chip rendering) — drive the chip list from `filter.accountSelection.values`. A chip is shown for an id iff it is in `filter.accountSelection.values` and not in `baseFilter.accountSelection.values` (existing base-scope exclusion logic carries over).

### 3.6 Reset action

- **EDIT** `FilterSheetContent.kt` L168–171 and L255–258 — Reset currently does `localFilter = baseFilter`. With D-008, `baseFilter` for accounts must contain `Selection(filterDefaults.accounts)` (or the screen's scoped subset). Two options:
  1. Construct `baseFilter` at the screen level already seeded with the defaults, so `localFilter = baseFilter` continues to work unchanged.
  2. Have Reset call a small `fun resetAccountSelection(defaults): TransactionFilter` helper and merge with `baseFilter`.
  Prefer option 1 — it keeps the Reset action site-agnostic and pushes the dimension-specific default knowledge to the construction site (the screen VM), which already has access to the live accounts list.

### 3.7 Empty-result hint (D-005)

- Locate the Transactions list empty-state composable. Likely `ui/screens/transactions/TransactionsScreen.kt` or a shared `EmptyState*` component — search for "No transactions" / "no results" strings.
- Add a branch: when the resolved transactions list is empty **and** `countActiveFilters(currentFilter, baseFilter) > 0`, render a hint reading "Your filter excludes everything." with a Reset CTA that emits the same `FilterEvent.Reset`.
- Keep wording final-pass to copy review; not blocking.

### 3.8 Build, manual verification checklist

Run `./gradlew assembleDebug`. Then on a device:

- [ ] Open Transactions → open filter sheet → Accounts sub-sheet shows **only active** accounts pre-selected; archived accounts visible and unchecked (D-008 curated default).
- [ ] Default state: list excludes archived-account transactions (no `t4`-style rows) and the active-filter indicator is **off**.
- [ ] Tick an archived account so selection becomes active+archived → list now includes archived-account rows; active-filter indicator turns **on** (deviation from default).
- [ ] Manually tick exactly the set of active accounts (no archived) starting from a deviated state → indicator turns **off** again because content equals default (content-equality activation per D-008).
- [ ] Deselect 1 active account → filter chip appears for the active subset, list updates live (D-004); active indicator on.
- [ ] Deselect ALL accounts in sub-sheet → close → list shows zero rows AND the "filter excludes everything — Reset?" hint (D-001 + D-005).
- [ ] Reopen the filter sheet → Accounts sub-sheet shows the user's empty selection preserved, **not** defaults (D-001).
- [ ] Hit Reset → accounts return to active-only default → list repopulates with active-account rows only → active-filter count goes back to zero (D-008).
- [ ] Switch profile while a non-default filter is active → the new profile's filter state is independent and seeded with its own active-account default (existing profile scoping must continue to hold).
- [ ] Save preset (existing button) with `Selection(emptySet())` → reload preset → empty selection is preserved (serializer round-trips).
- [ ] Verify SQL emission decision: empty selection yields zero rows via either `IN ()` or `AND 0` fallback — pick the one that runs cleanly on SQLCipher and document in the file.

### 3.9 Tests (lightweight, just for the pilot abstractions)

- **NEW** `app/src/test/java/com/osscabinet/ossWallet/domain/model/filter/SelectionTest.kt`:
  - `Selection(emptySet()).matches(x)` always false.
  - `Selection(setOf(a,b)).matches(a)` true; `.matches(c)` false.
  - `Selection(setOf(a,b)).isDefault(setOf(a,b))` true; `.isDefault(setOf(a))` false; `.isDefault(setOf(a,b,c))` false.
  - Serialization round-trip: `Json.encodeToString(Selection(emptySet()))` decodes back to an empty selection (distinct from a selection containing the empty string, etc.).
- **NEW** test in `TransactionFilterServiceTest` (or create if absent):
  - `accountSelection = Selection(setOf(a1,a2))` filters in only transactions with accountId in {a1,a2}.
  - `accountSelection = Selection(emptySet())` filters out every transaction.
  - `accountSelection = Selection(defaults)` content-equals defaults → list matches "default" expectation (active-account rows only).

---

## 4. Cohesive rollout (post-pilot, one PR)

Once pilot is green and edge cases are resolved, replicate the pattern in **one** change:

1. Migrate the remaining 5 OR-dimensions in `TransactionFilter`:
   - `categoryIds: Set<Long>` → `categorySelection: Selection<Long>`
   - `transactionTypes: Set<TransactionType>` → `typeSelection: Selection<TransactionType>`
   - `currencies: Set<String>` → `currencySelection: Selection<String>`
   - `paymentMethods: Set<PaymentMethod>` → `paymentMethodSelection: Selection<PaymentMethod>`
   - `statuses: Set<TransactionStatus>` → `statusSelection: Selection<TransactionStatus>`
2. Expand `FilterDefaults` (introduced in pilot) to populate all five new fields from the live domain (`allCategories`, `TransactionType.entries`, `allAvailableCurrencies`, `PaymentMethod.entries`, `TransactionStatus.entries`). Wire it through the screen VMs that construct filter state.
3. Update `TransactionQueryBuilder`, `TransactionFilterService`, `merge`, all extension helpers (`forCategory`, `withCurrency`, `withPaymentMethod`, etc.), `countActiveFilters` (extend to take all 6 default sets), `removeFilter`, every sub-sheet block in `FilterFields`, every card-value renderer in `FilterFields`, every chip block in `FilterSheetContent`.
4. **Tags** (D-003):
   - Keep `tags: Set<String>` (empty here genuinely means "no tag constraint" per D-003 — Tags is the exception). No `Selection<T>` wrapper needed.
   - Change `matchAllTags: Boolean = true` → `matchAllTags: Boolean = false` (default OR).
   - Active rule for tags: `filter.tags.isNotEmpty()` only. Mode toggle alone (when tags empty) is **not** active (D-003 resolved).
   - Update tag chip / mode-toggle rendering accordingly. Hide the OR/AND chip when tags is empty (since it has no effect).
5. Verify `merge()` semantics for every migrated field — intersection of `values`; if either side is empty, result is empty (most restrictive).
6. Re-run the §3.8 checklist for each migrated dimension (substitute the dimension's curated default — for categories/types/currencies/methods/statuses that's the full domain, not a curated subset). The empty-result hint covers all of them via the unified `countActiveFilters` ≠ 0 path.
7. Audit non-sheet callers that construct `TransactionFilter` with set fields (cards, deep links, saved presets, dashboard cashflow) — every one needs to construct `Selection(values)` explicitly. Callers that used to rely on `emptySet() = no constraint` must now pass the resolved default set.

---

## 5. Out of scope (per D-006)

- `timelineRange`, `minAmount`/`maxAmount`, `queryNote`, `queryPayee`, `hasWarranty`, `warrantyExpirationStart/End`, `linkedTransactionIds` — keep current behavior. These don't have the "selection set with all-selected default" shape.
- Active-filter visual indicator on the Transactions screen (chip/dot/count) — D-006 defers redesign.
- A SQL AND-tag path — current tag filter is OR-only in SQL with AND handled in-memory; acceptable for now.

---

## 6. Risk register

| # | Risk | Mitigation |
|---|------|-----------|
| R-1 | `IN ()` syntax may error on the Android SQLite wrapper used by Room/SQLCipher | Plan adopts the portable `AND 0` form for empty selections (§3.3). Pilot confirms `AND 0` runs cleanly; if `IN ()` also works, optionally switch back for symmetry. |
| R-2 | Serialized presets in users' DBs use the old `accountIds: Set<String>` shape | kotlinx.serialization: when `accountSelection` is absent, deserialize to `Selection(emptySet())`, then the screen seeds it to defaults before render — net effect: old presets open with the dimension at default. Document behavior; test round-trip during pilot. |
| R-3 | Non-sheet callers (cards, dashboard, deep links) construct `TransactionFilter` directly with `accountIds = setOf(...)` expecting old "empty = all" semantics | Field rename forces a compile break at every site. Each site must construct `Selection(values)` explicitly; sites that previously omitted the field (relying on `emptySet() = all`) must now pass the resolved default set. Audit all hits during the pilot diff. |
| R-4 | The domain set grows mid-session (new account added) while a non-default `Selection(values)` is active — should the new account auto-join the selection? | D-008 open question; default = **no migration** (selection is literal). Reset rebases to the new default. Document with a comment near `Selection`. |
| R-5 | A consequence of D-008: a user who manually ticks exactly the default set is treated as inactive, including the chip row disappearing. Could feel "magical" if not surfaced. | Acceptable — content equality is the intended UX. If feedback warrants, consider a subtle "matches default" affordance later (not in scope). |
| R-6 | Users land on empty results by accident and don't see the hint | D-005 hint plus the live-apply feedback loop should make it obvious; revisit if reported. |
| R-7 | The `FilterDefaults` for accounts is computed from the live accounts list; if that list is still loading when the sheet opens, the default is wrong | Gate sheet rendering / seeding on a non-loading accounts state, or re-seed when accounts emits a non-empty value the first time. Existing screen VMs already gate on collected state — verify during pilot. |

---

## 7. File index (everything the implementer needs to open)

**Will be edited (pilot):**
- `app/src/main/java/com/osscabinet/ossWallet/domain/model/TransactionFilter.kt`
- `app/src/main/java/com/osscabinet/ossWallet/data/local/query/TransactionQueryBuilder.kt`
- `app/src/main/java/com/osscabinet/ossWallet/domain/service/filter/TransactionFilterService.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/FilterComponents.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/FilterSheetContent.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/transactions/FilterUtils.kt`
- Transactions empty-state composable (locate during pilot)

**Will be created (pilot):**
- `app/src/main/java/com/osscabinet/ossWallet/domain/model/filter/Selection.kt`
- `app/src/test/java/com/osscabinet/ossWallet/domain/model/filter/SelectionTest.kt`
- (Optional) `app/src/test/java/com/osscabinet/ossWallet/domain/service/filter/TransactionFilterServiceTest.kt` if not present

**Will be edited (rollout):** the same files above for the remaining 5 dimensions + tags, plus every grep hit for `categoryIds` / `transactionTypes` / `currencies` / `paymentMethods` / `statuses` / `matchAllTags` outside the filter sheet (cards, repositories, dashboard, saved-preset migration).

---

## 8. Resume protocol (if returning cold)

1. Read this plan + `filter-sheet-filter-defaults-and-activation.md` (decisions).
2. `git status` and `git log` on `feature/tagv40` (or the active branch) to see what's already landed.
3. Grep for `Selection<` and `accountSelection` to determine pilot progress.
4. If pilot is partially landed: continue from the first §3 subsection whose changes are not visible in the diff.
5. If pilot is fully landed and verified: proceed to §4 rollout.
6. Update the companion file's Changelog with the date and what changed each time you finish a phase.
