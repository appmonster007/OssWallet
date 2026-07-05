# Code Review: stablev46...HEAD
**Date:** 2026-07-05  
**Diff:** `stablev46...HEAD` — 131 files, 6,673 insertions, 2,581 deletions  
**Mode:** no-spec (changes were made without deliberation or discussion with developer)  
**Reviewers:** 12 parallel adversarial agents, cross-validated findings

---

## Executive Summary

This diff introduces significant structural refactoring across the transaction entry, template entry, planned payment entry, and QuickAdd flows, plus a database schema migration adding `profileId` to `categories` and `tags`. The migration has **two compile-time errors** and **two critical database corruption bugs** that will crash production users on upgrade and fresh install. Across all flows, several features that existed in `stablev46` are silently broken at runtime (no crash, no feedback) due to deleted sheet files whose responsibilities were not fully ported. There are 131 files changed and the changes span all architectural layers without a coordinated design decision trail.

**Must-fix before any QA or merge:** 7 CRITICAL findings (2 compile errors, 5 runtime crashes or silent data corruption).

---

## CRITICAL — Compile Errors & Crashes

### C-1 | Stale field name in `PlannedPaymentEntryViewModel`
**File:** `PlannedPaymentEntryViewModel.kt` ~line 260  
The state initialization block uses `transferRate = existing.transferRate` but `PlannedPaymentEntryState` renamed this field to `exchangeRate`. `PlannedPaymentEntryState` has no `transferRate` parameter — this is a **Kotlin compile error**. The correct line (`exchangeRate = existing.transferRate`) is already present at line 281.  
**Fix:** Remove line 260 (`transferRate = existing.transferRate,`).

---

### C-2 | `PlannedPaymentEntryScreen` imports and calls deleted class
**File:** `PlannedPaymentEntryScreen.kt` lines 25 and 93–97  
`PlannedPaymentEntrySheets.kt` was deleted in this diff. The screen still imports and calls `PlannedPaymentEntrySheets(...)`. This is an **unresolved reference compile error**.  
**Fix:** Remove the import on line 25 and the `PlannedPaymentEntrySheets(...)` block on lines 93–97. Sheet handling is now inside `PlannedPaymentFormBody`.

---

### C-3 | `TransactionEntryViewModelTest` will not compile
**File:** `TransactionEntryViewModelTest.kt`  
The test passes `accountRepository`, `categoryRepository`, and `templateRepository` as constructor parameters to `TransactionEntryViewModel`. None of these parameters exist in the actual VM constructor (which takes only `savedStateHandle`, `transactionUseCases`, `transactionManager`, `settingsRepository`, `computationRepository`, `tagRepository`). All 6 tests in this file are dead code.  
**Fix:** Remove the 3 bogus parameters and stub `transactionUseCases.observeEntryCoreData(any())` to return a flow emitting a real `EntryCoreData` instance with test accounts.

---

### C-4 | `MIGRATION_27_28` crashes every upgrade from v27 (missing index rebuild)
**File:** `AppDatabase.kt` `MIGRATION_27_28`  
`CategoryEntity` changed its index from `[Index("name")]` to `[Index("name", "profileId")]`. Room validates the schema after migration. `MIGRATION_27_28` adds the `profileId` column via `ALTER TABLE` but **never drops `index_categories_name` or creates `index_categories_name_profileId`**. After the migration runs, Room's identity hash check finds a mismatch and throws `IllegalStateException: Migration didn't properly handle version 28`, **crashing every user upgrading from v27**.  
**Fix — add to `MIGRATION_27_28`:**
```sql
DROP INDEX IF EXISTS index_categories_name
CREATE INDEX IF NOT EXISTS index_categories_name_profileId ON categories (name, profileId)
```

---

### C-5 | Fresh install crash — `NOT NULL` constraint failure on categories and tags
**File:** `CategoryEntity.kt`, `TagEntity.kt`, `AppDatabase.kt`  
Both entities add `profileId: Int = 1` but without `@ColumnInfo(defaultValue = "1")`. Room does not propagate Kotlin defaults to SQL DDL, so the generated schema has `profileId INTEGER NOT NULL` with no SQL default. `ensureSystemCategories()` and `ensureSystemTags()` both INSERT without specifying `profileId`. **On fresh install, the NOT NULL constraint fails, crashing the app on first launch.**  
**Fix:** Add `@ColumnInfo(defaultValue = "1")` to `profileId` in both `CategoryEntity` and `TagEntity`.

---

### C-6 | Template sheet silently broken (orphaned after sheet file deletion)
**File:** `TransactionFormBody.kt` / `EntrySheetOrchestrator.kt`  
`TransactionEntrySheets.kt` was deleted and its sheet responsibilities redistributed to `EntrySheetOrchestrator`. However, `AeonTemplateSelection` was **not ported**. Tapping the template button fires `ShowTemplateSheet(true)`, the flag flips in state, but nothing renders the sheet. Feature silently broken — no crash, no feedback.  
**Fix:** Add `AeonTemplateSelection` back directly in `TransactionFormBody`, guarded by `if (state.showTemplateSheet)`.

---

### C-7 | Wrong status picker wired for planned payment status
**File:** `PlannedPaymentEntryState.kt`, `PlannedPaymentFormBody.kt`  
`sheetState` maps `showStatus = showStatusSheet`. `EntrySheetOrchestrator` uses `sheetState.showStatus` to show `AeonStatusSelection` — which is the **transaction** status picker (CLEARED/PENDING/RECONCILED). `showStatusSheet` is the planned payment status flag (ACTIVE/PAUSED/COMPLETED). `AeonPlannedPaymentStatusSelection` was in the deleted `PlannedPaymentEntrySheets.kt` and was not ported. Planned payment status is **permanently uneditable** from the form.  
**Fix:** Add `AeonPlannedPaymentStatusSelection` to `PlannedPaymentSheetOrchestrator`, guarded by `state.showStatusSheet`. Remap `sheetState.showStatus` to `showTransactionStatusSheet`.

---

## HIGH — Must Fix Before Merge

### H-1 | Double-save race condition in `QuickAddViewModel`
**File:** `QuickAddViewModel.kt`  
`isSaving` is set **inside** the launched coroutine body, not before it. On Android's main thread, `viewModelScope.launch` schedules the coroutine for the next execution slot. Two rapid taps (same frame) both pass the `if (isSaving) return` guard before either coroutine starts. Despite the commit message claiming this was fixed (`809e78ed`), the fix is incomplete.  
**Fix:** Set `isSaving = true` synchronously before the launch.

### H-2 | Double-save possible in `TransactionEntryViewModel`
**File:** `TransactionEntryViewModel.kt`  
`saveTransaction()` launches a coroutine but never sets an in-flight flag. During the async DB write, `canSave` remains `true` (it only flips when `navTriggered` is set post-save). Back-to-back taps create duplicate transactions.  
**Fix:** Add `isSaving: Boolean = false` to `TransactionEntryState`; guard `saveTransaction()` with `if (_state.value.isSaving) return`; set it `true` synchronously before launch, reset in `finally`.

### H-3 | Selective backup restore destroys accounts/categories/tags
**File:** `BackupRepositoryImpl.kt`  
When the user selects only `TRANSACTIONS` for selective restore, the code adds `ACCOUNTS`, `CATEGORIES`, and `TAGS` to `safeEntities`, then calls `clearAll()` on each before restoring from backup. **All accounts, categories, and tags created after the backup are silently destroyed.** This contradicts the semantics of "selective restore."  
**Fix:** Use `INSERT OR IGNORE`/`upsert` semantics instead of `clearAll(); restore()`.

### H-4 | Tags always written to profile 1 regardless of current profile
**File:** `TagRepositoryImpl.kt`  
Both `insertTag()` and `updateTag()` construct `TagEntity` without passing `profileId`, using the Kotlin default of `1`. Tags created by any non-primary profile are silently assigned to profile 1.  
**Fix:** Pass `tag.profileId` through in both `insertTag()` and `updateTag()`.

### H-5 | `CategoryRepository.categories` leaks cross-profile data to 12+ ViewModels
**Files:** `CategoryRepository.kt` (unscoped `val categories` still present), consumed by `DashboardViewModel`, `TransactionViewModel`, `BudgetViewModel`, `BudgetEntryViewModel`, `LoanViewModel`, `LoanEntryViewModel`, `FilterEntryViewModel`, `AccountDetailViewModel`, `AnalyticsViewModel`, `CsvImportViewModel`, `CategoryViewModel`, `CsvImportServiceImpl`  
The new profile-scoped `getAllCategories(profileId)` was added but the old unscoped `categories` property was not deprecated or removed. All 12+ existing callers continue to use the cross-profile overload.  
**Fix:** Either `@Deprecated` the unscoped `categories` property and migrate all callers to use `getAllCategories(profileId)` via `flatMapLatest` on `settingsRepository.currentProfileId`, or make the property itself internally profile-scoped.

### H-6 | `isRateFixed` not in `Template` domain model — custom rate lost on edit
**Files:** `TemplateEntryViewModel.kt`, `Template.kt`  
When a user creates a template with a fixed custom exchange rate, saves, and reopens it in edit mode, `isRateFixed` always loads as `false`. The first account/currency interaction triggers `recalculateExchangeRate()` which silently overwrites the stored custom rate with the market rate.  
**Fix:** Add `isRateFixed: Boolean = false` to `Template` domain model and persist it in `TemplateEntity` (requires a schema migration).

### H-7 | `CSVParser` resource leak in `parseHeaders`
**File:** `CsvImportServiceImpl.kt` line 246  
`val parser = CSVParser.parse(reader, format)` is created but never closed. `Closeable` not respected.  
**Fix:** Wrap in `.use {}`: `val records = CSVParser.parse(reader, format).use { it.records }`.

### H-8 | `SettingsViewModel.deleteAllData()` deadlock risk
**File:** `SettingsViewModel.kt`  
`_backupMessage` is a rendezvous channel (buffer = 0). `Channel.send()` suspends until a receiver collects. The new code calls `_backupMessage.send("Clearing...")` **before** `backupRepository.deleteAllData()`. If the settings screen is not actively collecting the channel at the moment `deleteAllData()` is invoked (e.g., the screen is partially obscured), the coroutine hangs indefinitely and the delete never executes.  
**Fix:** Either use `Channel(Channel.BUFFERED)`, or call `trySend()` (fire-and-forget), or move the delete before the send.

### H-9 | `EntryEditorialInput` flush mode regression
**File:** `EntryEditorialInput.kt`  
The conditional `hazeStyle` (transparent/0dp blur for flush mode, card style for normal mode) was replaced with an unconditional `GlassTokens.cardStyle().copyStyle(...)`. When `isFlush = true` and glass is enabled, the input now incorrectly applies glass blur, breaking the flush visual contract across all screens that use flush editorial inputs.  
**Fix:** Restore the `if (isFlush) HazeStyle(tint = HazeTint(Color.Transparent), blurRadius = 0.dp) else GlassTokens.cardStyle()` conditional.

---

## MEDIUM — Should Fix Before Merge

### M-1 | `MIGRATION_27_28` schema hash mismatch for `tags_new`
The `tags_new` CREATE TABLE in the migration has `DEFAULT 1` for `profileId`, but `TagEntity` without `@ColumnInfo(defaultValue="1")` generates DDL without `DEFAULT`. SQLite's `sqlite_master` retains the `DEFAULT 1` clause. Room's hash check may detect a mismatch even on fresh upgrades.  
**Fix:** Resolved by adding `@ColumnInfo(defaultValue = "1")` (same fix as C-5).

### M-2 | Tag DAO methods `getTagIdByName`, `getTagsByNames*` — cross-profile
**File:** `TagDao.kt`  
These queries have no `profileId` predicate. With per-profile unique tag names, `getTagIdByName("Food")` may return a tag from the wrong profile when profiles share a name.

### M-3 | `DismissAllSheets` missing 4 flags in `TransactionEntryViewModel`
**File:** `TransactionEntryViewModel.kt`  
The `DismissAllSheets` handler does not reset `showAccountSheet`, `showTargetAccountSheet`, `showCategorySheet`, or `isNumpadVisible`. Sheets left open by these flags survive a `DismissAllSheets` event.

### M-4 | Warranty date picker stays open after user confirms date
**File:** `EntrySheetOrchestrator.kt`  
`AeonDatePickerDialog.onDateSelected` calls the setter callback but does not call `onDismiss`. The picker remains rendered after the user taps "OK."  
**Fix:** Explicitly call `onDismissSheet(null)` inside `onDateSelected`.

### M-5 | `isRateFixed` not reset on type transition in `PlannedPaymentEntryViewModel`
**File:** `PlannedPaymentEntryViewModel.kt` `SetTransactionType` handler  
A TRANSFER→EXPENSE→TRANSFER round-trip leaves the stale pinned rate active. The fix applied to `TransactionEntryViewModel` (`isRateFixed = false` in `state.copy()`) was not ported here.

### M-6 | `recalculateExchangeRate()` race in `PlannedPaymentEntryViewModel`
**File:** `PlannedPaymentEntryViewModel.kt`  
Multiple rapid currency/account changes each launch a new coroutine. The last coroutine to resume wins, potentially overwriting a newer selection with a stale currency pair's rate. The cancellable `rateJob: Job` pattern from `TransactionEntryViewModel` was not ported.

### M-7 | `onDismissSheet` else-branch fires 9–10 state updates per dismiss
**Files:** `TemplateFormBody.kt`, `PlannedPaymentFormBody.kt`  
Each sheet dismiss calls `onEvent(ShowXSheet(false))` for every sheet type in an else-branch. This triggers 9–10 `_state.update` calls and recompositions for a single sheet close.  
**Fix:** Add `data object DismissAllSheets` events in both VMs handled with a single atomic `update {}`.

### M-8 | `observeEntryCoreData` architectural misplacement
**File:** `TransactionUseCases.kt`  
`TransactionUseCases` now holds `AccountRepository`, `CategoryRepository`, `TagRepository`, and `TemplateRepository` solely to serve form initialization. These four repos are unrelated to transaction persistence. This belongs in a dedicated `EntryFormDataUseCase`.

### M-9 | `TransactionEntryViewModel.initialize()` vs `PlannedPaymentEntryViewModel`/`TemplateEntryViewModel` subscribe inconsistency
`TransactionEntryViewModel` calls `observeEntryCoreData(...).first()` (one snapshot). The other two subscribe continuously. If the user creates a new account while a transaction entry form is on the back stack, it won't appear on return.

### M-10 | `CardRenderCache.invalidateProfile(newProfileId)` clears the wrong profile
**File:** `OssWalletApp.kt` line ~93  
`onEach { newProfileId -> cardRenderCache.invalidateProfile(newProfileId) }` evicts the **incoming** profile's cache (which is empty) instead of the **outgoing** profile's entries. Outgoing profile entries accumulate unboundedly.

### M-11 | `GlassSettings.borderWidth` default change is a no-op
**File:** `GlassSettings.kt`  
`SurfaceProperties.borderWidth` default changed from `0.5f` to `1f`. All 7 existing `GlassStyleConfig` presets pass `0.5f` explicitly in positional constructor calls — none use the default. The change has zero visible effect.

### M-12 | `TransactionEntryService` as `object` hides global state
**File:** `TransactionEntryService.kt`  
The service is an `object` but calls `CurrencyService.precision` (an `AtomicReference` mutated at runtime). This creates hidden global state in an apparently pure service. Tests cannot isolate precision independently.

### M-13 | `TemplateEntryViewModel` subscribes to unused `data.templates`
`observeEntryCoreData()` includes `templates` from `templateRepository.getAllTemplates(profileId)`. `TemplateEntryViewModel` never reads this field — a Room subscription running for nothing.

### M-14 | `FormSheetState` missing `@Immutable`
**File:** `EntryCore.kt`  
`FormSheetState` is a `data class` with all stable fields (Boolean, enum). Without `@Immutable`, the Compose compiler may treat it as unstable and force unnecessary recompositions.

### M-15 | `TemplatePrefillDTO` drops all TRANSFER fields
`TemplatePrefillDTO` has no `targetAccountId`, `targetCurrency`, or `exchangeRate` fields. Creating a TRANSFER template from a suggestion via `TemplatePrefillDTO` silently loses the destination account, target currency, and exchange rate.

### M-16 | `stale prefill` survives back-navigation in `SmartSuggestionsScreen`
**File:** `SmartSuggestionsScreen.kt`  
If the user back-navigates from `SmartSuggestionsScreen` before entering `PlannedPaymentEntry`, `plannedPaymentPrefill` stays non-null. On re-entry to the screen, the `LaunchedEffect` fires immediately with the stale value and auto-navigates without user input.

### M-17 | `sections` StateFlow missing `distinctUntilChanged` on search query
**File:** `QuickAddViewModel.kt`  
`_uiState.map { it.searchQuery }` emits on every `_uiState` mutation (every digit typed, every toggle). Without `.distinctUntilChanged()`, the expensive suggestion scan reruns on every keystroke even when the query hasn't changed.

### M-18 | `deleteAllData()` has no error handling
**File:** `SettingsViewModel.kt`  
If `backupRepository.deleteAllData()` throws, the coroutine fails silently. The user sees the "Clearing..." message (already consumed from the channel) but the app is neither killed nor shows an error. Data is in an indeterminate state.

---

## LOW — Track for Backlog

| ID | File | Finding |
|----|------|---------|
| L-1 | `CategoryDao.kt` | `getCategoryByName(name)` and `getSubCategories(parentId)` — no `profileId` filter; cross-profile leaks |
| L-2 | `TagDao.kt` | `getAllTagsSync()` — undocumented cross-profile usage |
| L-3 | `BackupRepositoryImpl.kt` | File I/O inside `database.withTransaction` holds exclusive DB lock during CSV reads |
| L-4 | `BackupRepositoryImpl.kt` | `restoreTransactions` defaults `accountId` to `""` — FK violation crash on bad CSV rows |
| L-5 | `BackupRepositoryImpl.kt` | id=0 defaults for categories/profiles/transactions — Room auto-generates new IDs, breaking FK graph |
| L-6 | `BackupRepositoryImpl.kt` | `parseTagNames` splits on `,` — corrupts tag names containing commas (e.g. "food, dining") |
| L-7 | `CsvImportServiceImpl.kt` | `profileBaseCurrency` defaults to `"INR"` — developer locale artifact, should be `"USD"` constant |
| L-8 | `QuickAddViewModel.kt` | Two independent `getAllTransactions(profileId)` subscriptions — doubles DB cursor load |
| L-9 | `QuickAddViewModel.kt` | Suggestion sort is alphabetical Z→A, not recency (`sortedByDescending { it.label }`) |
| L-10 | `QuickAddViewModel.kt` | Cross-currency transfer counterpart lookup fails (`amount.abs().compareTo()` always mismatches) |
| L-11 | `QuickAddSheet.kt` | Keyboard not hidden when advancing from STEP_TEMPLATE — overlaps STEP_ACCOUNT grid |
| L-12 | `QuickAddAmountStep.kt` | `key(account?.id, toAccount?.id)` resets numpad expression on account change — only `activeAmountSide` should be in the key |
| L-13 | `QuickAddAmountStep.kt` | `formatCurrency` called with blank `currencyCode` for Outside Wallet accounts — malformed display |
| L-14 | `QuickAddAmountStep.kt` | `LongPress` haptic fired on regular tap — heavy vibration on short tap |
| L-15 | `QuickAddTemplateStep.kt` | Mode toggle is icon-only with no `contentDescription` — inaccessible and undiscoverable |
| L-16 | `QuickAddTemplateStep.kt` | `AtelierGrid` `items()` has no `key` lambda — breaks animations, forces full re-bind on list change |
| L-17 | `QuickAddViewModel.kt` | Category becomes `SYSTEM_UNKNOWN` after mode transition instead of `null` — wrong fallback in `selectedCategoryId ?: SYSTEM_OTHERS` |
| L-18 | `QuickAddViewModel.kt` | Nested `flatMapLatest` creates brief stale cross-profile section emissions on profile switch |
| L-19 | `TransactionEntryViewModel.kt` | `buildTransactionRequest` passes `exchangeRates = emptyMap()` and `anchor = "USD"` hardcoded — exchange rate snapshot always empty |
| L-20 | `TransactionEntryService.kt` | `getMarketRate` returns `BigDecimal.ONE` for blank currency — implies 1:1 rate before target account is selected |
| L-21 | `TransactionEntryService.kt` | `syncTransferState` FIXED mode returns `impliedRate = 1.0` when source = 0 — misleading display |
| L-22 | `TransactionEntryService.kt` | `amountOverride` parameter misleadingly named — it is the transfer **target** amount, not an override of source |
| L-23 | `TemplateEntryState.kt` | Dead `?:` operators on non-nullable `targetAmount`/`targetCurrency` — stale from type change |
| L-24 | `PlannedPaymentEntryViewModel.kt` | Stale imports: `AccountRepository`, `CategoryRepository`, `TagRepository` still imported after constructor removal |
| L-25 | `PlannedPaymentEntryState.kt` | `targetCurrency ?: currency` — `targetCurrency` is non-nullable; `?:` is dead |
| L-26 | `EntryActionCard.kt` / `EntryActionChip.kt` | `cardCornerRadius` (28dp) used for chip-sized elements; `Dimensions.kt` mandates `chipCornerRadius` (20dp) |
| L-27 | `EntryEditorialInput.kt` | `borderWidth` `animateDpAsState` is dead — both non-flush branches return identical value; animation never transitions |
| L-28 | `TransactionPrefillDTO.fromTemplate()` | `warrantyExpiration`, `gpsLocation`, `location`, `transactionStatus` dropped when applying template to transaction |
| L-29 | `QuickAddViewModelTest.kt` | `assertEquals(BigDecimal("50"), state.amount)` — scale-mismatch trap; use `.compareTo() == 0` |
| L-30 | Both test files | MockK (`relaxed = true`) used for all repositories — violates CLAUDE.md `Fake*` convention |

---

## Developer Questionnaire

The following decisions require explicit developer answers before fixes can be finalized. Answer each with the letter/number of your choice. Where "Other" is provided, write your answer.

---

### Section A: Database Schema (Migration 27→28)

**A-1. System categories visibility across profiles**  
System categories (IDs 1000–1005) are inserted by `ensureSystemCategories()` without a `profileId` — they land in profile 1. A user on profile 2 will see no system categories in the picker. Which strategy do you want?
- [ ] A. Duplicate system categories for every profile on creation
- [ ] B. `getAllCategories(profileId)` uses `WHERE profileId = :profileId OR isDefault = 1` (UNION approach)
- [ ] C. System categories retain a sentinel `profileId = 0` excluded from profile scoping

**A-2. `categoryRepository.categories` (unscoped) — 12+ callers**  
All existing callers use the unfiltered `categories` property which now leaks cross-profile data. Migration plan:
- [ ] A. Deprecate now; I'll migrate all callers in a follow-up PR (tracks as tech debt)
- [ ] B. Migrate all 12 callers in this same PR before merge
- [ ] C. The existing callers are intentionally cross-profile (explain which ones and why)

**A-3. `isRateFixed` on `Template`**  
Custom exchange rates saved on a template are silently overwritten on first interaction after reload because `isRateFixed` is not persisted.
- [ ] A. Add `isRateFixed` to `Template` domain model + `TemplateEntity` + a schema migration (version 29)
- [ ] B. Templates always re-fetch market rate on reload (document as intentional behavior)

---

### Section B: TransactionEntryService Design

**B-1. `TransactionEntryService` as `object`**  
The service depends on `CurrencyService` global state while appearing pure. This makes unit testing precision-dependent calculations impossible in isolation.
- [ ] A. Convert to a Hilt-injectable `class` receiving `CurrencyService` as a constructor parameter
- [ ] B. Keep as `object`; document the hidden dependency (acceptable for now)

**B-2. `observeEntryCoreData` placement**  
This function lives in `TransactionUseCases` but requires `AccountRepository`, `CategoryRepository`, `TagRepository`, and `TemplateRepository` — none of which are transaction concerns.
- [ ] A. Extract to a dedicated `EntryFormDataUseCase` class
- [ ] B. Keep in `TransactionUseCases` (accept the coupling)

**B-3. `TransactionEntryViewModel.initialize()` — snapshot vs continuous**  
`TransactionEntryViewModel` calls `observeEntryCoreData(...).first()` (one snapshot). `PlannedPaymentEntryViewModel` and `TemplateEntryViewModel` subscribe continuously. A new account created on another screen while the transaction entry form is in the back stack won't appear on return.
- [ ] A. Change `TransactionEntryViewModel` to subscribe continuously (matches the other two)
- [ ] B. Keep `.first()` snapshot behavior (document the limitation)

**B-4. TRANSFER amount signing in `TransactionEntryService.createRequest`**  
The service signs the `TransactionRequest.amount` as negative for TRANSFER outflow. `TransactionManager` must NOT negate this again.
- [ ] A. Confirmed: `TransactionManager.saveTransfer` expects pre-signed (negative) amount — no double-negation
- [ ] B. Found a double-negation — I need to check `TransactionManager` and fix it

**B-5. `amountOverride` parameter naming**  
`createRequest(amountOverride: BigDecimal?)` is used exclusively as the **target** amount for transfers.
- [ ] A. Rename to `targetAmountOverride` or `destinationAmount`
- [ ] B. Keep the current name (acceptable)

---

### Section C: QuickAdd Flow

**C-1. Template always advances to `STEP_ACCOUNT` even when account is pre-filled**  
If a template specifies an account, the user still lands on STEP_ACCOUNT after selection.
- [ ] A. Skip to STEP_AMOUNT when template has a pre-filled account
- [ ] B. Always require STEP_ACCOUNT review (current behavior — intentional)
- [ ] C. Auto-save for fully-specified templates (no step at all)

**C-2. Save animation on success vs cancel**  
On success: `finish()` fires immediately (no slide-down). On cancel/back: 300ms slide-out + fade animation plays.
- [ ] A. Call `animateDismiss()` on success too (consistent)
- [ ] B. Keep snap-close on success (intentional "confirmation" feel)

**C-3. `SUGGESTIONS` mode cycle with blank query**  
Cycling the mode toggle can land on `SUGGESTIONS` with no search query, showing a confusing empty state.
- [ ] A. Skip `SUGGESTIONS` in the cycle when `searchQuery` is blank
- [ ] B. Show an instructional empty state when query is blank in SUGGESTIONS mode (current behavior)

**C-4. `onSuggestionSelected` lambda — 11 parameters**  
The callback has 11 positional parameters with adjacent same-type arguments — any swap is invisible to the compiler.
- [ ] A. Refactor to `(QuickAddResult.SuggestionItem) -> Unit` (type-safe)
- [ ] B. Keep current (acceptable as internal API)

**C-5. Cross-currency transfer counterpart detection**  
The suggestion builder matches transfer counterpart legs by `amount.abs().compareTo()`. For USD→EUR transfers, amounts differ (100 USD vs 85 EUR) — match always fails.
- [ ] A. Match on `linkedTransferId` field (if it exists in the domain model)
- [ ] B. Match on timestamp window + `isTransfer` flag only (ignore amount)
- [ ] C. Remove the suggestion feature for cross-currency transfers

---

### Section D: PlannedPayment Entry Flow

**D-1. PlannedPayment edit mode — financial fields not updated**  
In edit mode, `savePlannedPayment()` updates schedule/status/tags but NOT amount, accountId, currency, categoryId, payee, note, or location on the `PlannedPayment` entity (only the Template is updated). `confirmOccurrence` reads from the `PlannedPayment`, so occurrences reflect stale financial data after an edit.
- [ ] A. Fix: apply all financial field updates to the `PlannedPayment` entity in edit mode
- [ ] B. This is intentional — templates handle financial data, planned payments handle scheduling only

**D-2. Occurrence regeneration on edit**  
When recurrence type or start date changes in edit mode, `nextDueDate` is preserved from the existing schedule — no recalculation.
- [ ] A. Call `PlannedPaymentService.nextDueDate()` to recalculate on edit
- [ ] B. Keep `nextDueDate` as-is on edit (worker recalculates it externally)

---

### Section E: UI Components

**E-1. Corner radius on `EntryActionCard` and `EntryActionChip`**  
Both were changed from `chipCornerRadius` (20dp) to `cardCornerRadius` (28dp). `Dimensions.kt` explicitly mandates `chipCornerRadius` for "small interactive metadata tags."
- [ ] A. Revert to `chipCornerRadius` (follow documented Aeon rules)
- [ ] B. Keep `cardCornerRadius` (visual direction change, override the rule)

**E-2. `EntryField` enum scope in `EntrySheetOrchestrator`**  
8 of 12 sheet types dismiss with `onDismissSheet(null)` because `EntryField` has no entries for currency, tags, status, payment method, location, warranty, exchange rate, or linked transactions.
- [ ] A. Extend `EntryField` enum with all missing cases
- [ ] B. Accept `null` as the discriminator for non-field-error sheets (document the convention)

**E-3. `showStatus` sheet silently no-ops without `transactionStatus`**  
If `transactionStatus == null` (new, unsaved transactions), the status sheet button does nothing.
- [ ] A. Use a default `TransactionStatus.CLEARED` when status is null
- [ ] B. Disable the status chip for new transactions until saved

**E-4. `ExchangeRatePill` icon tint convention**  
`isFixed = true` (manual/locked rate) → full opacity. `isFixed = false` (live/auto rate) → 60% opacity. This is semantically inverted — the live state is the default/preferred state and should be more prominent.
- [ ] A. Swap: full opacity for `isFixed = false` (live), 60% for `isFixed = true` (manual)
- [ ] B. Keep current (locked/manual is the "special" state deserving full emphasis)

---

### Section F: Tests & Coverage

**F-1. Test infrastructure — MockK vs Fake***  
Both new test files use `mockk(relaxed = true)` for repositories. CLAUDE.md mandates `Fake*` implementations backed by `MutableStateFlow`.
- [ ] A. Rewrite tests using `Fake*` implementations (correct approach)
- [ ] B. Accept MockK for ViewModel tests (pragmatic trade-off, document exception)

**F-2. `clearPrefillDto()` coverage**  
The core fix from commit `10168a0c` has zero test coverage.
- [ ] A. Add test: `initialize()` with prefill populates state; `clearPrefillDto()` nulls it
- [ ] B. Defer to a future test sprint

**F-3. Double-save prevention coverage**  
`QuickAddViewModel` double-save guard (`isSaving`) has no test verifying the guard works.
- [ ] A. Add test: second `saveTransaction()` call while `isSaving = true` invokes manager exactly once
- [ ] B. Defer to a future test sprint

---

## Priority Action Table

| Priority | Count | Action |
|----------|-------|--------|
| **P0 — Compile errors** | 3 | C-1, C-2, C-3: Fix before any build attempt |
| **P0 — Crash on upgrade/install** | 2 | C-4, C-5: Fix before any device testing |
| **P0 — Silent broken feature** | 2 | C-6 (template sheet), C-7 (PP status picker) |
| **P1 — Data corruption / loss** | 3 | H-1 double-save, H-3 selective restore destroys data, H-4 tags always profile 1 |
| **P1 — Cross-profile data leak** | 2 | H-5 categories, H-7 CSVParser leak |
| **P1 — UX blocking** | 3 | H-2 double-save TX, H-8 deleteAllData deadlock, H-9 flush mode regression |
| **P2 — Medium** | 18 | M-1 through M-18 |
| **P3 — Low / cleanup** | 30 | L-1 through L-30 |

---

## Positive Changes (Confirmed Correct)

- `javax.inject` → `jakarta.inject` migration: correct for Hilt 2.59.2, complete across all 131 files
- `TransactionSide.amount` default `""` → `"0"`: correct, coordinates with display change
- `TagRepositoryImpl.getAllTags()` now maps `profileId` to domain model: correct fix
- Old `onAddTag` bug in `TemplateEntrySheets.kt` (`SetTags(tags + name)` without persisting): correctly fixed to `AddTag(name, color)` which calls `tagRepository.insertTag()`
- Reactive `observeUniquePayees`/`observeUniqueNotes` overloads: correctly scoped, clean API improvement
- `TransactionViewModel.setPlannedPaymentPrefillFromPattern()`: `profileId = 0` hardcode correctly replaced with `settingsRepository.currentProfileId.first()`
- `CsvProcessor` deleted and replaced with Apache Commons CSV: correct fix for latent parsing bugs
- ZIP Slip validation added to `restoreZipBackup`/`restoreZipBackupSelective`: security improvement
- 1GB size limit guards on restore: correct defensive addition
- `GlassSurface` solid-mode border color fix: targeted and correct
- `AeonTemplateSelection` amount formatting using `CurrencyText`: behavioral improvement (locale-aware grouping separators)
- IME scroll-into-view fix in `EntryEditorialInput` (`BringIntoViewRequester` + 250ms delay): correct implementation, addresses issue 3-6
- `BackupRepositoryImpl`: tag serialization fixed from `Set.toString()` to `joinToString("|")`
- Batched inserts (100 at a time) in restore: performance improvement
