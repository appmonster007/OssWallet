# Code Review: stablev46..HEAD
**Date:** 2026-07-04  
**Mode:** no-spec  
**Reviewer:** bmad-code-review (Blind Hunter + Edge Case Hunter)

---

## Legend
- ✅ Applied
- ❌ Dismissed (false positive or intentional design)
- ⏸ Deferred (pre-existing, low-risk, or out of scope)
- 🔲 Pending patch

---

## Group A — Transaction Entry
**Files:** TransactionEntryViewModel, TransactionEntryState, TransactionEntryScreen, TransactionEntryEvent, TransactionFormBody, TransactionEntryService  
**Commits:** `cf94ed91` (A-patches), `10168a0c` (amount defaults + A-8 dead code)

### Applied

| ID | Sev | Finding | Fix |
|----|-----|---------|-----|
| A-1 | HIGH | Split entry sheets unreachable after `TransactionEntrySheets.kt` deleted — split-mode validation active with no way to enter splits. | Added `AeonTransactionSplitSheet`, `AeonCategorySelection`, `AeonCalculatorSheet` directly in `TransactionFormBody.kt`. |
| A-2 | HIGH | Silent data loss: `gpsLocation`, `location`, `warrantyExpiration`, `paymentMethod` dropped on every non-split save. | Added `.copy(gpsLocation, location, warrantyExpiration, paymentMethod)` after `createRequest()`. |
| A-4 | HIGH | `ExchangeRateSnapshot` wrong anchor in `SetAmount`/`SetTargetAmount` handlers — live cross-currency display wrong for non-USD source. | Fixed anchor to `s.selectedCurrency` with `rates.ifEmpty { mapOf(s.targetCurrency to s.exchangeRate) }`. |
| A-5 | MEDIUM | `isRateFixed` not reset on type transition — stale pinned rate survives TRANSFER→EXPENSE→TRANSFER round-trip. | Added `isRateFixed = false` to `SetTransactionType` handler. |
| A-6 | MEDIUM | `recalculateExchangeRate()` coroutine race — last-wins overwrites with stale pair's rate. | Replaced with cancellable `rateJob: Job` pattern with staleness re-check after suspension. |
| A-7 | MEDIUM | `showDeleteConfirmation` lost on configuration change — dialog disappears on rotation mid-confirmation. | Changed to `rememberSaveable`. |
| A-8 | MEDIUM | Dead `else ""` / `else "0"` branch — `transferRate` is always > 0 (domain invariant), branch unreachable. | Removed dead `if/else`, simplified to direct `calculateSourceAmount()` call. |
| A-10 | MEDIUM | `onDismissSheet` else-branch fires 10 separate `_state.update()` calls — up to 10 StateFlow emissions per dismiss. | Added `DismissAllSheets` event; single atomic `update {}` clears all sheet flags. |
| A-11 | MEDIUM | `baseCurrency` fallback uses arbitrary list ordering instead of profile base currency. | Load `settingsRepository.currency.first()` in `initialize()`; stored in `TransactionEntryState.baseCurrency`. |
| — | LOW | Amount defaults: 6 locations used `""` as initial amount outside domain models. | Standardized all to `"0"`. |

### Dismissed

| ID | Sev | Finding | Reason |
|----|-----|---------|--------|
| A-3 | HIGH | `jakarta.inject.Inject` wrong import. | False positive — project intentionally migrated; Hilt 2.48+ supports it. |
| A-9 | MEDIUM | `clearPrefillDto()` no-op leaves stale prefill. | False positive — each nav destination gets a fresh VM instance; guard is dead code. |

### Deferred

| ID | Sev | Finding | Reason |
|----|-----|---------|--------|
| A-12 | MEDIUM | `observeEntryCoreData().first()` one-shot — background changes not reflected. | Pre-existing pattern across codebase. |
| A-13 | MEDIUM | TOCTOU between `update {}` and subsequent `_state.value` read. | Pre-existing; low risk on main thread. |
| A-14 | MEDIUM | `targetCurrencies` fallback allocates full currency list on every recompose. | Pre-existing performance concern, not a correctness regression. |
| A-15 | LOW | Zero-amount transaction can pass `canSave` via `"0."` input. | Minor UX edge case; no data-integrity regression. |

---

## Group B — Quick Add Flow
**Files:** QuickAddViewModel, QuickAddSheet, QuickAddAmountStep, QuickAddAccountStep, QuickAddTemplateStep  
**Commit:** `809e78ed`

### Applied

| ID | Sev | Finding | Fix |
|----|-----|---------|-----|
| B-1 | HIGH | Double-save race — `canSave` doesn't check `isSaving`; two rapid taps both launch coroutines before `isSaving = true` propagates. | Added `if (_uiState.value.isSaving) return` as first line of `saveTransaction()`. |
| B-2 | MEDIUM | `runCatching` in `saveTransaction()` swallows `CancellationException`. | Added `if (it is CancellationException) throw it` in `.onFailure`. |

### Dismissed

| ID | Sev | Finding | Reason |
|----|-----|---------|--------|
| B-3 | MEDIUM | Same-account self-transfer not rejected in `validateTransferAccounts`. | Intentional design — same-account transfers are allowed. |
| B-4 | LOW | `onTemplateSelected` reads potentially-empty `state.value.accounts` during cold-start. | Correct by design — `resolvePrefill` with empty list returns `AccountRef.Unselected`, which is intended. |

---

## Group C — Template & Planned Payment Entry
**Files:** TemplateEntryViewModel, TemplateEntrySheets, TemplateFormBody, PlannedPaymentEntryViewModel, PlannedPaymentEntryScreen, PlannedPaymentEntrySheets, PlannedPaymentFormBody

### Findings

| ID | Sev | Finding | Status |
|----|-----|---------|--------|
| C-1 | CRITICAL | `PlannedPaymentEntryViewModel.kt` lines 260 & 281: stale `transferRate` parameter passed alongside new `exchangeRate` parameter — `PlannedPaymentEntryState` has no `transferRate` field, compile error. | 🔲 |
| C-2 | CRITICAL | `PlannedPaymentEntryScreen.kt` lines 25, 93–97: still imports and calls deleted `PlannedPaymentEntrySheets` — unresolved reference compile error. | 🔲 |
| C-3 | CRITICAL | `TemplateEntryScreen.kt` lines 12, 61–65: still imports and calls deleted `TemplateEntrySheets` — unresolved reference compile error. | 🔲 |

**Fixes:**
- **C-1:** Remove line 260 (`transferRate = existing.transferRate,`) — the renamed field `exchangeRate` at line 281 is the correct replacement.
- **C-2:** Remove the `import PlannedPaymentEntrySheets` and the `PlannedPaymentEntrySheets(...)` call block — sheets are handled by `PlannedPaymentSheetOrchestrator` inside `PlannedPaymentFormBody`.
- **C-3:** Remove the `import TemplateEntrySheets` and the `TemplateEntrySheets(...)` call block — sheets are handled by `TemplateSheetOrchestrator` inside `TemplateFormBody`.

---

## Group D — Entry UI Components
**Files:** AeonImpactHeaderCard, EntryEditorialInput, EntrySheetOrchestrator, EntryCore, EntryActionChip, EntryAmountDisplay, EntrySection, AeonDetailSection, AeonTemplateSelection, EntryActionCard, AeonInputs, CalculatorDialog, CalculatorNumpad, CalculatorState, CurrencyDisplay, GlassSurface

### Findings

| ID | Sev | Finding | Status |
|----|-----|---------|--------|
| D-1 | HIGH | `AeonImpactHeaderCard.kt` lines 203–232: `EntryMode` convenience overload accepts `isRateFixed` and `onToggleRateMode` but never forwards them to the generic overload — callers always get `isFixed = false` and `onToggleMode = {}` in `ExchangeRatePill` regardless of what they pass. | 🔲 |
| D-2 | MEDIUM | `EntryEditorialInput.kt` line 189: `hazeStyle` set unconditionally to `cardStyle()` for all inputs including `isFlush = true` — adds unwanted glass background layer inside `AeonImpactHeaderCard`'s name field; old code had an `isFlush` guard. | 🔲 |
| D-3 | MEDIUM | `EntryEditorialInput.kt` lines 197–199: highlight `Box(Modifier.fillMaxSize())` is inside a vertically-unbounded `BoxScope` — resolves to height 0, making the color wash permanently invisible. | 🔲 |
| D-4 | LOW | `EntryEditorialInput.kt` lines 155–160: both non-flush branches of `borderWidth` `when` produce identical values — `else` branch is dead code. | 🔲 |

**Fixes:**
- **D-1:** Add `isRateFixed = isRateFixed` and `onToggleRateMode = onToggleRateMode` to the delegation call at lines 223–224 of `AeonImpactHeaderCard.kt`.
- **D-2:** Restore the `isFlush` conditional: `hazeStyle = if (isFlush) HazeStyle(tint = HazeTint(Color.Transparent), blurRadius = 0.dp) else GlassTokens.cardStyle().copyStyle(bgColor = AppColors.surfaceContainerLow)`.
- **D-3:** Replace `fillMaxSize()` with `matchParentSize()`, or move the color wash inside the `Row` where the `64.dp` height is defined.
- **D-4:** Collapse `borderWidth` to two branches: `isFlush -> 0.dp` / `else -> config.surface.borderWidth.dp`.

---

## Group E — Data Layer
**Files:** AppDatabase, CategoryRepositoryImpl, TagRepositoryImpl, TagDao, BackupRepositoryImpl, CsvImportServiceImpl, DatabaseSeeder

### Findings

| ID | Sev | Finding | Status |
|----|-----|---------|--------|
| E-1 | CRITICAL | `AppDatabase.kt` MIGRATION_27_28: drops old single-column index on `categories` but never creates the new composite `(name, profileId)` index — Room schema validation crashes on upgrade. | 🔲 |
| E-2 | CRITICAL | `CategoryRepositoryImpl.kt` lines 17–18: `categories` Flow calls unscoped `getAllCategories()` (no profileId filter) — leaks all profiles' categories to every subscriber. | 🔲 |
| E-3 | CRITICAL | `TagRepositoryImpl.kt` lines 18–20: `getAllTags()` calls unscoped `tagDao.getAllTags()` — leaks all profiles' tags. | 🔲 |
| E-4 | CRITICAL | `TagRepositoryImpl.kt` lines 28, 33, 63: three `TagEntity` constructions omit `profileId`, storing every tag under hardcoded profile 1 regardless of active profile. | 🔲 |
| E-5 | HIGH | `BackupRepositoryImpl.kt` lines 212–222, 824–838: categories CSV export/restore omits `profileId` — multi-profile backup silently drops profile associations; all restored categories land in profile 1. | 🔲 |
| E-6 | HIGH | `BackupRepositoryImpl.kt` lines 433–434, 1005–1012: same double-bug as E-5 but for tags. | 🔲 |
| E-7 | HIGH | `TagDao.kt` lines 22–32: three tag lookup queries have no `profileId` filter — can return/match tags from any profile. | 🔲 |
| E-8 | HIGH | `CsvImportServiceImpl.kt` line 1500: `importGenericCsv` uses unscoped `getAllCategoriesSync()` — imported transaction can be assigned a categoryId from a different profile. | 🔲 |

**Fixes:**
- **E-1:** Add to MIGRATION_27_28 after the ALTER TABLE: `db.execSQL("DROP INDEX IF EXISTS \`index_categories_name\`")` and `db.execSQL("CREATE INDEX IF NOT EXISTS \`index_categories_name_profileId\` ON \`categories\` (\`name\`, \`profileId\`)")`.
- **E-2:** Drive `categories` property through `flatMapLatest` on current profileId, or force callers to use `getAllCategories(profileId)`.
- **E-3:** Same pattern as E-2 for tags.
- **E-4:** Pass `profileId = tag.profileId` in all three `TagEntity(...)` constructions.
- **E-5:** Add `"profileId"` to the categories CSV header and `it.profileId` to values; add `profileId = p["profileId"]?.toIntOrNull() ?: 1` to restore.
- **E-6:** Same as E-5 for tags.
- **E-7:** Add `AND profileId = :profileId` to the three unscoped tag queries; thread profileId from callers.
- **E-8:** Add `getAllCategoriesSync(profileId: Int)` DAO method; call with active profileId in `importGenericCsv`.

---

## Group F — Domain + DI
**Files:** TransactionEntryService (core/domain), TransactionManager, TransactionUseCases, Category.kt, Tag.kt, di/ modules

### Findings

| ID | Sev | Finding | Status |
|----|-----|---------|--------|
| F-1 | HIGH | `TransactionEntryService.kt` lines 263–273: `calculateTransferRate` called with `destAmount = BigDecimal.ZERO` when `amountOverride` is zero — produces `transferRate = 0`, violating the `transferRate > 0` domain invariant. Root cause: `TransactionSide.amount` default changed from `""` to `"0"`, so destination `amountValue` is `BigDecimal.ZERO` when user hasn't entered a target amount; `canSave` has no guard requiring `destination.amountValue > 0`. | 🔲 |
| F-2 | MEDIUM | `TransactionEntryService.kt` lines 118–120: `transitionType` else-branch uses `""` fallback for empty amount — violates `"0"` = default convention; the parallel TRANSFER branch correctly uses `"0"`. | 🔲 |
| F-3 | LOW | `Category.kt` line 26, `Tag.kt` line 4: `profileId: Int = 1` default silently assigns entities to profile 1 for all callers that don't supply the new field. | 🔲 |

**Fixes:**
- **F-1:** In `createRequest`, replace `amountOverride` with `amountOverride?.takeIf { it.signum() != 0 }`. Also add `destination.amountValue > 0` guard to `canSave` for transfers, or add a `if (targetAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE` guard in `calculateTransferRate`.
- **F-2:** Change line 119 fallback from `""` to `"0"` to match the TRANSFER branch.
- **F-3:** Remove `= 1` default from `profileId` in both `Category` and `Tag` so missed call sites fail at compile time instead of silently defaulting.

---

## Group G — Tests, Artifacts & Other ViewModels
**Files:** TransactionEntryViewModelTest, QuickAddViewModelTest, SettingsViewModel, SmartSuggestionsScreen, sprint-status.yaml, deferred-work.md

### Findings

| ID | Sev | Finding | Status |
|----|-----|---------|--------|
| G-1 | CRITICAL | `TransactionEntryViewModelTest.kt` lines 99–109: test constructs `TransactionEntryViewModel` with `accountRepository`, `categoryRepository`, `templateRepository` — none exist in the actual constructor; all 7 tests fail to compile and never run. | 🔲 |
| G-2 | MEDIUM | `QuickAddViewModelTest.kt` lines 120, 426, 462: three `assertEquals` on `BigDecimal` values — scale-sensitive, will fail if ViewModel arithmetic changes scale (e.g., `BigDecimal("50")` vs `BigDecimal("50.00")`). | 🔲 |
| G-3 | MEDIUM | `QuickAddViewModelTest.kt` line 98: stubs `categoryRepository.categories` but ViewModel calls `categoryRepository.getAllCategories(profileId)` — unstubbed call returns cold flow; categories StateFlow stays empty, silently defeating category-dependent tests. | 🔲 |
| G-4 | LOW | `QuickAddViewModelTest.kt` line 459: `awaitState` predicate uses `==` (scale-sensitive) on `BigDecimal.ZERO` — test can hang indefinitely if combine re-emit produces `BigDecimal("0.00")`. | 🔲 |
| G-5 | LOW | `SettingsViewModel.kt` lines 510–519: `deleteAllData()` calls `backupRepository.deleteAllData()` without try-catch — uncaught exception skips the `withContext(NonCancellable)` kill block, leaving app running with a partially-cleared database. | 🔲 |

**Fixes:**
- **G-1:** Remove `accountRepository`, `categoryRepository`, `templateRepository` from the constructor call. The ViewModel loads these via `transactionUseCases.observeEntryCoreData`.
- **G-2 & G-4:** Replace all `assertEquals(BigDecimal.X, ...)` and `it.amount == BigDecimal.X` with `assertThat(state.amount).isEqualByComparingTo(BigDecimal.X)` / `it.amount.compareTo(BigDecimal.X) == 0`.
- **G-3:** Replace `every { categoryRepository.categories } returns ...` with `every { categoryRepository.getAllCategories(any()) } returns MutableStateFlow(emptyList())`.
- **G-5:** Wrap `backupRepository.deleteAllData()` in `runCatching`; on failure send error message and skip the kill block (same pattern as `importBackup()`).

---

## Summary

| Group | CRITICAL | HIGH | MEDIUM | LOW | Total | Status |
|-------|----------|------|--------|-----|-------|--------|
| A | 0 | 3 | 6 | 1 | 10 | ✅ All applied |
| B | 0 | 1 | 1 | 0 | 2 | ✅ All applied |
| C | 3 | 0 | 0 | 0 | 3 | 🔲 Pending |
| D | 0 | 1 | 2 | 1 | 4 | 🔲 Pending |
| E | 4 | 4 | 0 | 0 | 8 | 🔲 Pending |
| F | 0 | 1 | 1 | 1 | 3 | 🔲 Pending |
| G | 1 | 0 | 2 | 2 | 5 | 🔲 Pending |
| **Total** | **8** | **10** | **12** | **5** | **35** | 12 applied, 23 pending |
