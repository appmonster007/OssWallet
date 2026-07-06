# Code Review: stablev46...HEAD
**Date:** 2026-07-05  
**Diff:** `stablev46...HEAD` — 131 files, 6,673 insertions, 2,581 deletions  
**Mode:** no-spec (changes were undiscussed and undeliberated)  
**Method:** 12 parallel adversarial review agents, all findings cross-verified against live source files before inclusion

---

## How To Use This Document

Work through this document **top-to-bottom**. The sections are ordered by urgency:

1. **CRITICAL** — the app will not build or will crash. Fix these before touching anything else.
2. **HIGH** — user-visible data loss or silent broken features. Fix before any QA.
3. **MEDIUM** — wrong behavior under specific conditions, architectural debt. Fix before merge.
4. **LOW** — polish, edge cases, cleanup. Track for backlog.
5. **Developer Questionnaire** — 22 design decisions that can't be resolved by the reviewer alone. Answer each before the corresponding fix is implemented.

At the end is a table of **confirmed correct changes** — things the diff did well.

---

## False Positive Audit

Before presenting findings, all CRITICAL and HIGH items were re-verified against live source files. **Zero false positives found.** Every finding below was confirmed in code. Some findings were upgraded (e.g., H-5 category callers: the document says "12+" — there are actually 14 confirmed call sites). Where a finding is nuanced or commonly misread, a "Why this isn't a false positive" note is included.

---

## CRITICAL — Build Will Fail Or App Will Crash

These 7 issues must be fixed before any build attempt or device testing. They are not theoretical risks — they will happen to every user.

---

### C-1 · Stale field name causes compile error in `PlannedPaymentEntryViewModel`

**File:** `PlannedPaymentEntryViewModel.kt` line 260  
**Verified:** ✅ Line 260 contains `transferRate = existing.transferRate`; line 281 contains the correct `exchangeRate = existing.transferRate`. Both exist simultaneously.

**What went wrong:** When `PlannedPaymentEntryState` was refactored to rename `transferRate` → `exchangeRate`, the edit-mode state initialization block was only partially updated. Line 281 got the right name, but line 260 still references the old one. `PlannedPaymentEntryState` has no `transferRate` constructor parameter, so Kotlin rejects the call.

**What a developer would experience:** Running `./gradlew assembleDebug` fails immediately with `Unresolved reference: transferRate` pointing at line 260. The entire `PlannedPaymentEntry` feature is unshippable.

**Fix (30 seconds):**  
Delete line 260 (`transferRate = existing.transferRate,`). The value is already correctly assigned at line 281 (`exchangeRate = existing.transferRate`).

---

### C-2 · `PlannedPaymentEntryScreen` calls a class that no longer exists

**File:** `PlannedPaymentEntryScreen.kt` lines 25 and 93–97  
**Verified:** ✅ Line 25 imports `PlannedPaymentEntrySheets`, line 93 calls `PlannedPaymentEntrySheets(...)`. The source file `PlannedPaymentEntrySheets.kt` was deleted in this diff.

**What went wrong:** `PlannedPaymentEntrySheets.kt` was deleted and its sheets were redistributed to `PlannedPaymentSheetOrchestrator` inside `PlannedPaymentFormBody`. The Screen file was not updated to reflect this. The sheet logic now lives one level deeper — the screen doesn't need to call it directly anymore — but nobody removed the old call.

**What a developer would experience:** Compile error: `Unresolved reference: PlannedPaymentEntrySheets`. Any PR touching Planned Payments is dead.

**Fix (2 minutes):**  
- Remove `import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components.PlannedPaymentEntrySheets` (line 25)
- Remove the `PlannedPaymentEntrySheets(state = state, onSearch = viewModel::searchTransactions, onEvent = viewModel::onEvent)` block (lines 93–97)

The sheets are already rendered by `PlannedPaymentFormBody` internally via the new `PlannedPaymentSheetOrchestrator`. Removing the screen-level call does not lose any UI.

---

### C-3 · `TransactionEntryViewModelTest` passes constructor parameters that don't exist

**File:** `TransactionEntryViewModelTest.kt` lines 99–108  
**Verified:** ✅ The test passes `accountRepository`, `categoryRepository`, and `templateRepository`. The actual VM constructor at line 51 accepts only: `savedStateHandle`, `transactionUseCases`, `transactionManager`, `settingsRepository`, `computationRepository`, `tagRepository`.

**What went wrong:** The test was written assuming a different VM constructor — one that takes repositories directly. The actual VM was simplified to receive data through `transactionUseCases.observeEntryCoreData()` instead, making three of the injected params redundant. The test was written for the "before" state of the VM, not the "after."

**What a developer would experience:** All 6 tests in this file fail to compile. `./gradlew test` fails entirely. This also masks a deeper problem: even if the constructor mismatch were fixed, the tests would hang (see the questionnaire section F).

**Fix (requires design decision first — see Questionnaire F-1):**  
Remove the 3 bogus parameters. Then stub `transactionUseCases.observeEntryCoreData(any())` to return a `flowOf(EntryCoreData(accounts = listOf(mockAccount, mockToAccount), categories = emptyList(), tags = emptyList(), ...))` so the VM's `initialize()` can proceed.

---

### C-4 · `MIGRATION_27_28` will crash every user upgrading from v27

**File:** `AppDatabase.kt`, `MIGRATION_27_28`  
**Verified:** ✅ The migration only runs `ALTER TABLE categories ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1`. It never touches the existing `index_categories_name` index (created by `MIGRATION_5_6` at line 543). `CategoryEntity` now declares `Index(value = ["name", "profileId"])`, which Room expects as `index_categories_name_profileId`. The mismatch between what's physically in SQLite and what Room expects triggers a hard crash.

**What went wrong:** When you `ALTER TABLE` to add a column, you can add data but you cannot automatically update index definitions. Indexes that include new columns must be explicitly dropped and recreated. The migration was written as if Room would figure this out — it won't.

**Scenario:** A user on v27 launches the updated app. Room opens the database, runs MIGRATION_27_28 successfully, then validates the resulting schema against the entity definitions. It finds `index_categories_name` (single column) in SQLite but expects `index_categories_name_profileId` (two columns). Room throws:
```
java.lang.IllegalStateException: Migration didn't properly handle version 28
Expected: TableInfo{name='categories', columns={..., profileId=Column{name='profileId', ...}}, 
indices={Index{name='index_categories_name_profileId', ...}}}
Found: TableInfo{...indices={Index{name='index_categories_name', ...}}}
```
The app crashes on every launch for this user until they clear app data — which means they lose everything.

**Fix — add these two lines to `MIGRATION_27_28` after the `ALTER TABLE`:**
```kotlin
db.execSQL("DROP INDEX IF EXISTS `index_categories_name`")
db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_name_profileId` ON `categories` (`name`, `profileId`)")
```

---

### C-5 · Fresh install crashes on first launch — NOT NULL constraint violated

**Files:** `CategoryEntity.kt` line 21, `TagEntity.kt` line 16  
**Verified:** ✅ Both entities declare `val profileId: Int = 1` without `@ColumnInfo(defaultValue = "1")`. Room's DDL generator ignores Kotlin default parameter values — it only reads `@ColumnInfo(defaultValue)`.

**What went wrong:** Kotlin defaults and SQL defaults are completely separate. `val profileId: Int = 1` tells Kotlin "if you construct this object without passing `profileId`, use 1." But Room generates the CREATE TABLE SQL from annotations, not from Kotlin constructor defaults. Without `@ColumnInfo(defaultValue = "1")`, Room generates `profileId INTEGER NOT NULL` — no SQL-level default.

`ensureSystemCategories()` and `ensureSystemTags()` use raw `execSQL` INSERT statements that don't mention `profileId`:
```sql
INSERT OR IGNORE INTO categories (id, name, iconName, colorHex, isDefault, parentId, nature) VALUES (...)
```
SQLite can't infer the value. NOT NULL constraint fires. App crashes before the home screen.

**Scenario:** Brand-new user. App installs. On first launch, `onCreate()` is called. Room creates the tables. Then `AppDatabase.Callback.onCreate()` calls `ensureSystemCategories()` which INSERTs the 5 system categories without `profileId`. SQLite rejects all 5 inserts. If any of these run inside a transaction, the transaction rolls back, leaving the database in a broken state. App either crashes hard or launches into a broken state with no categories.

**Fix — add to both entities:**
```kotlin
@ColumnInfo(defaultValue = "1")
val profileId: Int = 1
```
The `@ColumnInfo(defaultValue = "1")` annotation tells Room to emit `DEFAULT 1` in the SQL DDL, so any INSERT that omits `profileId` automatically gets 1.

**Bonus:** This also fixes M-1 (schema hash mismatch from `tags_new` DDL having `DEFAULT 1` while Room's generated DDL doesn't).

---

### C-6 · The "Apply Template" button in Transaction Entry does nothing at runtime

**Files:** `TransactionFormBody.kt` line 149, `EntrySheetOrchestrator.kt` (searched, not present)  
**Verified:** ✅ `TransactionFormBody` fires `ShowTemplateSheet(true)` on button tap. Searching `EntrySheetOrchestrator.kt` and `EntryCore.kt` for `AeonTemplateSelection`, `showTemplateSheet`, and `ShowTemplateSheet` returns zero results.

**What went wrong:** The old `TransactionEntrySheets.kt` contained `AeonTemplateSelection` guarded by `if (state.showTemplateSheet)`. When this file was deleted and its responsibilities were moved to `EntrySheetOrchestrator`, `AeonTemplateSelection` was simply not ported. The event, the state flag, and the ViewModel handler all still exist — but nothing renders the sheet.

**What a user experiences:** They tap "Templates" in the transaction entry screen. Nothing happens. No sheet, no error, no feedback. The feature has silently vanished, and there's no way to apply templates to a transaction.

**Why this isn't just a visual bug:** Templates are a core productivity feature. Users who built workflows around "tap template → auto-fill transaction → save" will find their workflow completely broken. They'll manually re-enter every transaction.

**Fix — add to `TransactionFormBody.kt`, inside the Box/Column that holds the split sheets:**
```kotlin
if (state.showTemplateSheet) {
    AeonTemplateSelection(
        templates = state.templates,
        categories = state.categories,
        onTemplateSelected = { template ->
            if (template != null) onEvent(TransactionEntryEvent.ApplyTemplate(template))
            onEvent(TransactionEntryEvent.ShowTemplateSheet(false))
        },
        onDismiss = { onEvent(TransactionEntryEvent.ShowTemplateSheet(false)) },
        showCreateNew = false
    )
}
```

---

### C-7 · Planned Payment status picker shows the wrong component — status is permanently uneditable

**Files:** `PlannedPaymentEntryState.kt`, `PlannedPaymentFormBody.kt`  
**Verified:** ✅ `sheetState` maps `showStatus = showStatusSheet`. `EntrySheetOrchestrator` uses `sheetState.showStatus` to show `AeonStatusSelection` (CLEARED/PENDING/RECONCILED). The planned payment status picker `AeonPlannedPaymentStatusSelection` (ACTIVE/PAUSED/COMPLETED) was in the deleted `PlannedPaymentEntrySheets.kt` and was not ported.

**What went wrong:** Two completely different concepts share a similar flag name. `showStatusSheet` in `PlannedPaymentEntryState` refers to the planned payment lifecycle status. `showStatus` in `FormSheetState` refers to transaction clearing status. When the bridge code mapped `showStatus = showStatusSheet`, it cross-wired these two unrelated concepts. The wrong picker appears.

**What a user experiences:** Tapping "Status" on a planned payment shows CLEARED/PENDING/RECONCILED options instead of ACTIVE/PAUSED/COMPLETED. Selecting any of them fires `onTransactionStatusChange`, not `SetStatus`, so the planned payment status field is never actually updated. The field is visually interactive but permanently broken.

**Scenario:** A user wants to PAUSE a planned payment (e.g., they're traveling and want to skip automatic occurrence generation for 2 months). They tap "Status," see the wrong picker, tap PENDING, nothing changes on the planned payment. They have no way to pause it. They'd have to delete and recreate it.

**Fix — two parts:**

1. In `PlannedPaymentSheetOrchestrator` (inside `PlannedPaymentFormBody.kt`), add:
```kotlin
if (state.showStatusSheet) {
    AeonPlannedPaymentStatusSelection(
        selectedStatus = state.status,
        onStatusSelected = { status ->
            onEvent(PlannedPaymentEntryEvent.SetStatus(status))
            onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(false))
        },
        onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(false)) }
    )
}
```

2. Change the `sheetState` mapping to NOT use `showStatusSheet` for `FormSheetState.showStatus`. Either add a separate `showTransactionStatusSheet` boolean to `PlannedPaymentEntryState` for the transaction status picker, or map `showStatus = showTransactionStatusSheet`.

---

## HIGH — Data Loss, Silent Broken Features, UX Blockers

These 9 issues won't prevent a build but will cause real harm to real users. Fix before QA.

---

### H-1 · Double-save race condition in `QuickAddViewModel` — the "fix" didn't work

**File:** `QuickAddViewModel.kt` lines 760–772  
**Verified:** ✅ Line 760: `if (_uiState.value.isSaving) return` (guard). Line 772: `_uiState.update { it.copy(isSaving = true) }` (inside the coroutine body, not before launch).

**Why this isn't a false positive:** The guard reads the flag synchronously, but the flag is only SET after the launched coroutine begins executing. On Android, `viewModelScope.launch` schedules work on the main dispatcher for the *next available* frame — it doesn't run immediately. Two taps in the same frame both pass line 760 (both see `isSaving = false`) before any coroutine starts. This is a classic TOCTOU (time-of-check / time-of-use) race.

**Scenario — exactly how this breaks:**
1. User taps "Save" quickly twice (or network is slow and they tap again out of frustration)
2. Call 1: `isSaving` is `false` → passes guard → `viewModelScope.launch { ... }` is *scheduled* (not yet running)
3. Call 2: `isSaving` is still `false` (coroutine hasn't started yet) → passes guard → second launch *scheduled*
4. Coroutine 1 starts: `isSaving = true` — too late, call 2 already passed
5. Both coroutines call `transactionManager.handleSaveTransaction(request)` → two identical transactions created

The commit message on `809e78ed` claims this was fixed. The guard is there, but the atomicity isn't.

**Fix — set the flag synchronously before the launch:**
```kotlin
fun saveTransaction(onSuccess: () -> Unit) {
    if (_uiState.value.isSaving) return
    val s = state.value
    if (!canSave) return
    _uiState.update { it.copy(isSaving = true) }  // ← BEFORE the launch
    viewModelScope.launch {
        runCatching { transactionManager.handleSaveTransaction(...) }
            .onSuccess { _uiState.update { it.copy(isSaving = false) }; onSuccess() }
            .onFailure {
                if (it is CancellationException) throw it
                _uiState.update { it.copy(isSaving = false) }
            }
    }
}
```
This way the flag is set atomically on the main thread before any coroutine is scheduled. A second call on the same frame sees `isSaving = true` and returns immediately.

---

### H-2 · `TransactionEntryViewModel.saveTransaction()` has no in-flight guard

**File:** `TransactionEntryViewModel.kt` line 819  
**Verified:** ✅ `saveTransaction()` checks `canSave` (line 780 and 824) but has no `isSaving` flag. During the DB write, `canSave` remains `true` because it only becomes false when `navTriggered` is set (which happens after the write completes). Rapid double-taps will create duplicate transactions.

**Why this is different from H-1:** `QuickAddViewModel` at least has the guard structure, just implemented wrong. `TransactionEntryViewModel` has no guard structure at all. The save button in TransactionEntry is the highest-value save path in the app — it's where users enter their main financial data.

**Scenario:** User is on slow network or device. They tap "Save," the app appears to freeze for a second (DB write is slow), they tap again. Two transactions. No visual indication of duplication. They may not notice until their balance is wrong.

**Fix options:**
- **Option A (recommended):** Add `isSaving: Boolean = false` to `TransactionEntryState`. Guard `saveTransaction()` the same way as the corrected QuickAdd pattern. This also lets the UI disable the save button during the in-flight period.
- **Option B (minimal):** Use a `MutableStateFlow<Boolean>` as a dedicated saving semaphore, set it atomically with `compareAndSet(false, true)` before launching and reset in `finally`. This avoids touching `TransactionEntryState` but is less observable to the UI.

---

### H-3 · Selecting "Transactions only" in selective restore destroys all accounts, categories, and tags

**File:** `BackupRepositoryImpl.kt` lines 1202–1276  
**Verified:** ✅ Lines 1203–1206 add ACCOUNTS, CATEGORIES, TAGS to `safeEntities` when TRANSACTIONS is selected. Lines 1271–1273 call `clearAll()` on each before restoring from the backup file.

**What the intent was:** Transactions reference accounts and categories by ID. If you restore transactions from a backup but the current account IDs don't match, you'd have foreign key violations. So ACCOUNTS and CATEGORIES are added as dependencies to `safeEntities` — this makes sense as a dependency resolution step.

**What's actually broken:** "Dependency" doesn't mean "destroy and replace." The current implementation calls `accountDao.clearAll()` then `restoreAccounts(it)`. This deletes every account the user has created since the backup, then restores the backup's accounts. Accounts created after the backup date are permanently lost. Categories, tags: same story.

**Scenario — how this causes real harm:**
1. User made a backup in January
2. It's July. User has added 5 new accounts (savings, crypto, credit cards) and dozens of new categories
3. Some transactions look wrong. They use selective restore → pick "Transactions only" to fix specific transactions
4. App deletes all accounts, all categories, all tags → restores January's versions
5. 6 months of account additions: gone. 6 months of category customization: gone
6. The restored transactions now reference January accounts which happen to match, but the user's current financial picture is destroyed

**Fix options:**

- **Option A (recommended — safest):** Change the dependency resolution to use `INSERT OR IGNORE` (upsert) semantics instead of `clearAll(); restore()`. For accounts and categories, only insert rows from the backup that don't already exist (by ID). Rows that exist in the current DB are left untouched. This ensures transaction FKs are satisfied without destroying current data:
```kotlin
if (RestorableEntity.ACCOUNTS in safeEntities) File(tempDir, "accounts.csv").let { 
    if (it.exists()) restoreAccountsUpsert(it) // INSERT OR IGNORE, not clearAll()
}
```

- **Option B (UI warning):** Keep the current behavior but before executing, show a dialog: "Restoring transactions requires replacing your accounts and categories with the backup versions. Any accounts or categories created after the backup will be permanently deleted. Continue?" At least the user can make an informed choice.

- **Option C (change dependency strategy):** Don't add ACCOUNTS/CATEGORIES to `safeEntities` automatically. Instead, at restore time, check FK integrity: if a restored transaction references an account that doesn't exist in the current DB, surface a warning listing the missing accounts. Let the user decide whether to also restore accounts.

---

### H-4 · All newly created tags are silently assigned to profile 1 regardless of current profile

**File:** `TagRepositoryImpl.kt` lines 28 and 63  
**Verified:** ✅ `insertTag` at line 28: `TagEntity(id = tag.id, name = tag.name, colorHex = tag.colorHex)` — `profileId` not passed, Kotlin default of `1` is used. `updateTag` at line 63: identical issue.

**What went wrong:** When `TagEntity` gained `profileId: Int = 1`, the mappers and repositories that construct it were not updated to pass `tag.profileId`. The field is available on the domain `Tag` model — `getAllTags()` correctly maps it — but writes silently drop it.

**Scenario:** A user has two profiles — "Personal" (profile 1) and "Business" (profile 2). They create a tag "deductible" while on their Business profile. The tag is inserted with `profileId = 1`, not `profileId = 2`. When they switch to Personal and view their tags, "deductible" appears there too. When Business profile loads its tags, "deductible" will only appear if using the unscoped `getAllTags()` — if using the scoped `getAllTags(profileId = 2)`, the tag is invisible to the Business profile that created it.

**Fix (one-liner in each method):**
```kotlin
override suspend fun insertTag(tag: Tag) {
    tagDao.insertTag(TagEntity(id = tag.id, name = tag.name, colorHex = tag.colorHex, profileId = tag.profileId))
}

// In updateTag:
tagDao.insertTag(TagEntity(id = newTag.id, name = newTag.name, colorHex = newTag.colorHex, profileId = newTag.profileId))
```

---

### H-5 · 14 ViewModels and services receive categories from all profiles (cross-profile data leak)

**Files:** `CategoryRepository.kt` (unscoped `val categories: Flow<List<Category>>` still in interface), plus 14 confirmed callers  
**Verified:** ✅ 14 active call sites confirmed by grep:

| File | Usage |
|------|-------|
| `LoanViewModel.kt:53` | `categoryRepository.categories` |
| `LoanEntryViewModel.kt:67` | `categoryRepository.categories` in combine |
| `CsvImportViewModel.kt:79` | `categoryRepository.categories` |
| `FilterEntryViewModel.kt:60` | in combine |
| `DashboardViewModel.kt:344` | in combine |
| `DashboardViewModel.kt:691` | `.first()` call |
| `TransactionViewModel.kt:143` | `categoryRepository.categories` |
| `BudgetViewModel.kt:55` | `categoryRepository.categories` |
| `BudgetEntryViewModel.kt:66` | in combine |
| `AccountDetailViewModel.kt:147` | in combine |
| `CategoryViewModel.kt:21` | `categoryRepository.categories` |
| `AnalyticsViewModel.kt:126` | in combine |
| `RecentTransactionsWidgetFetcher.kt:59` | `.first()` |
| `CategoryUseCases.kt:11` | `categoryRepository.categories` |

**What went wrong:** The migration added `getAllCategories(profileId: Int)` to the interface but left `val categories` in place. None of the existing callers were updated. The new scoped method exists but is unused by the UI. Every screen that shows categories will now show categories from all profiles to every user.

**Scenario:** User has "Personal" (profile 1) and "Business" (profile 2). Business has a "Deductible Expenses" category. User switches to Personal and opens the transaction entry screen. "Deductible Expenses" appears in their Personal category picker. They categorize a personal grocery run as "Deductible Expenses" by accident. Their financial reports are now incorrect.

**Fix options:**

- **Option A (recommended for this PR):** Deprecate `val categories` and replace callers with profile-scoped `flatMapLatest`:
```kotlin
// In each ViewModel:
val categories: StateFlow<List<Category>> = settingsRepository.currentProfileId
    .flatMapLatest { profileId -> categoryRepository.getAllCategories(profileId) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

- **Option B (make the property itself profile-scoped in the impl):** Change `CategoryRepositoryImpl.categories` to internally observe the current profile. This means adding a `settingsRepository` dependency to `CategoryRepositoryImpl`, which is an architectural trade-off (data layer depending on settings).

- **Option C (migration PR):** `@Deprecated("Use getAllCategories(profileId)")` the unscoped property, merge this diff as-is, and immediately open a follow-up PR to migrate all 14 callers. Acceptable if you can guarantee the follow-up PR is the very next commit. Risk: it could be forgotten.

See also Questionnaire A-1 and A-2 for decisions about system categories (which this fix affects).

---

### H-6 · Custom exchange rate on a template is silently overwritten the first time you edit

**Files:** `Template.kt`, `TemplateEntryViewModel.kt`, `TemplateEntity`  
**Verified:** ✅ `Template` domain model has no `isRateFixed` field. `TemplateEntity` has no `isRateFixed` column. `TemplateEntryViewModel.initialize()` always loads `isRateFixed = false`. The first account selection, currency change, or account swap calls `recalculateExchangeRate()` which fetches the market rate and overwrites whatever was saved.

**What went wrong:** `isRateFixed` was added to `TemplateEntryState` but not propagated to the domain model and entity. It's ephemeral — it exists for the current editing session only and is lost when the template is saved and reloaded.

**Scenario:**
1. User creates a "Monthly EUR invoice" template. They set a fixed rate of 1.08 because their client pays at a contracted rate regardless of market.
2. User saves the template. `transferRate = 1.08` is saved. But `isRateFixed` is not saved (no field for it).
3. Next month, user reopens the template to edit the amount. `isRateFixed` loads as `false`.
4. They change the amount. This triggers `recalculateExchangeRate()`. Today's market rate is 1.12. The template now shows 1.12 instead of the contracted 1.08.
5. User saves — they've just corrupted their template's contracted rate without realizing it.

**Fix options:**

- **Option A (correct, requires schema migration):** Add `isRateFixed: Boolean = false` to `Template.kt`, `TemplateEntity` with a new schema version (v29), and a `Migration(28, 29)` that runs `ALTER TABLE templates ADD COLUMN isRateFixed INTEGER NOT NULL DEFAULT 0`. This is the only solution that fully solves the problem.

- **Option B (document as intentional):** Remove the "fixed rate" affordance from the template form entirely. Templates always use market rate on load. The user can still set a rate for the current session, but it won't persist. Add a tooltip: "Rate is recalculated from the market on each use." This is honest but removes useful functionality. See Questionnaire A-3.

---

### H-7 · `CSVParser` is never closed in `parseHeaders` — resource leak on every import

**File:** `CsvImportServiceImpl.kt` line 246  
**Verified:** ✅ `val parser = CSVParser.parse(reader, format)` is created and `.records` is read, but `parser.close()` is never called. The file handle leaks.

**What happens in practice:** On Android, open file handles are limited. If the user imports large files repeatedly (or the import fails and is retried), file handles accumulate until the OS kills the process. On some devices, this manifests as subsequent file opens failing with `Too many open files`.

**Fix (trivial — one line change):**
```kotlin
val records = CSVParser.parse(reader, format).use { it.records }
```
`.use {}` is Kotlin's standard `try-with-resources` equivalent. It closes the parser whether or not an exception is thrown.

---

### H-8 · `SettingsViewModel.deleteAllData()` can deadlock and never delete

**File:** `SettingsViewModel.kt` lines 510–513  
**Verified:** ✅ `_backupMessage = Channel<String>()` is a rendezvous channel (no buffer). Line 512: `_backupMessage.send("Clearing...")` suspends until the UI collects. Line 513: `backupRepository.deleteAllData()` only runs after the send completes.

**How Channel works in this context:** `Channel.send()` on a rendezvous channel is a two-party handshake — the sender suspends until a receiver calls `receive()` (or in this case, `receiveAsFlow()` collects). If the settings screen is not actively collecting `backupMessage` at the exact moment `send` is called — for example, if the UI is in the middle of a recomposition, or the screen was partially removed from the composition, or a different coroutine is handling a different channel event — the send suspends indefinitely. The delete never runs.

**What a user experiences:** They tap "Delete All Data." They see the "Clearing..." toast. Nothing else happens. The app doesn't restart. Their data is still there. If they try again, same result. The only escape is force-killing the app.

**Fix options:**

- **Option A (recommended — use a buffered channel):** Change the channel declaration: `private val _backupMessage = Channel<String>(Channel.BUFFERED)`. A buffered channel allows `send()` to return immediately without waiting for a receiver. The message is delivered to the UI whenever it next collects. The delete proceeds immediately after.

- **Option B — use `trySend()`:** `_backupMessage.trySend("Clearing...")` is a non-blocking send that silently drops the message if there's no receiver. The delete always runs. Downside: on a slow device, the user might not see the "Clearing..." message before the kill. Acceptable trade-off.

- **Option C — move delete before send:**
```kotlin
backupRepository.deleteAllData()
_backupMessage.send("Cleared. Restarting...")
withContext(NonCancellable) { delay(1500); killProcess(myPid()) }
```
This way the delete always runs. The message is informational only, sent after the fact.

---

### H-9 · Flush-mode editorial inputs now show glass blur — visual regression across all entry screens

**File:** `EntryEditorialInput.kt` line 189  
**Verified:** ✅ Line 189: `hazeStyle = GlassTokens.cardStyle().copyStyle(bgColor = AppColors.surfaceContainerLow)` — unconditional, no `if (!isFlush)` guard. `isFlush = true` is used on note/payee fields that are mounted flush to the form background. The old code had `if (isFlush) HazeStyle(tint = HazeTint(Color.Transparent), blurRadius = 0.dp) else GlassTokens.cardStyle()`.

**What the user sees:** In any entry form (transaction, template, planned payment), the Note and Payee fields now have a visible glass blur rectangle behind them when glass effects are enabled. Previously they were visually "part of the background." Now they appear as floating cards. On any device with glass enabled (the default), this regression is immediately visible.

**Why this isn't just a minor cosmetic issue:** Flush inputs are specifically designed to be visually integrated with the form background — they look like underlined text fields, not separate card components. Applying card-style glass to them breaks the visual hierarchy and makes the form look cluttered.

**Fix (restore the conditional — same code as before):**
```kotlin
hazeStyle = if (isFlush) HazeStyle(tint = HazeTint(Color.Transparent), blurRadius = 0.dp)
            else GlassTokens.cardStyle().copyStyle(bgColor = AppColors.surfaceContainerLow),
```

---

## MEDIUM — Wrong Behavior Under Specific Conditions

These 18 issues don't crash the app but produce incorrect behavior in discoverable scenarios. Fix before merge where possible; track the rest.

---

### M-1 · `tags_new` DDL in migration includes `DEFAULT 1` — potential Room schema hash mismatch
**Fix:** Resolved by the same `@ColumnInfo(defaultValue = "1")` fix as C-5. No separate action needed.

### M-2 · `getTagIdByName`, `getTagsByNames*` in `TagDao` — cross-profile tag lookup
**File:** `TagDao.kt`  
Tag names are now unique *per profile*, not globally. These queries have no `profileId` predicate. If profile 1 has a tag "Food" (id=5) and profile 2 also has "Food" (id=42), `getTagIdByName("Food")` returns 5 regardless of which profile is active. Transactions on profile 2 would be tagged with profile 1's tag entity.  
**Fix:** Add `profileId: Int` parameter to all three methods and add `AND profileId = :profileId` to their SQL.

### M-3 · `DismissAllSheets` event in `TransactionEntryViewModel` misses 4 sheet flags
**File:** `TransactionEntryViewModel.kt`  
The `DismissAllSheets` handler resets most sheet flags but omits `showAccountSheet`, `showTargetAccountSheet`, `showCategorySheet`, and `isNumpadVisible`. These sheets can survive a `DismissAllSheets` event. In practice the current callers route those specific sheets through their own events, but `DismissAllSheets` is a public event that future code (e.g., system back gesture handler) may call expecting full cleanup.  
**Fix:** Add the missing flags to the `update {}` block.

### M-4 · Warranty date picker stays open after the user taps "OK"
**File:** `EntrySheetOrchestrator.kt`  
`AeonDatePickerDialog.onDateSelected` fires the setter callback but does not call `onDismiss`. `AeonDateTimePicker` (used for transaction date) auto-dismisses — `AeonDatePickerDialog` does not. The warranty date picker stays rendered.  
**Fix:** Call `onDismissSheet(null)` inside `onDateSelected` for the warranty sheet.

### M-5 · `PlannedPaymentEntryViewModel` doesn't reset `isRateFixed` on type transition
**File:** `PlannedPaymentEntryViewModel.kt`  
If a user creates a TRANSFER planned payment with a fixed rate, then changes to EXPENSE (which resets modes), then back to TRANSFER — the stale fixed rate survives. The fix was applied to `TransactionEntryViewModel` (`isRateFixed = false` in `SetTransactionType`'s `state.copy()`) but was not ported to `PlannedPaymentEntryViewModel`.

### M-6 · Exchange rate recalculation race condition in `PlannedPaymentEntryViewModel`
**File:** `PlannedPaymentEntryViewModel.kt`  
Rapid account/currency changes each launch a new `recalculateExchangeRate()` coroutine. The last coroutine to complete wins, potentially writing a stale rate. The cancellable `rateJob: Job` pattern was applied in `TransactionEntryViewModel` but not ported here.  
**Fix:** Mirror the `rateJob` pattern: `rateJob?.cancel(); rateJob = viewModelScope.launch { ... }`.

### M-7 · Single sheet dismiss fires 9–10 `_state.update` calls (Template + PlannedPayment)
**Files:** `TemplateFormBody.kt`, `PlannedPaymentFormBody.kt`  
When `onDismissSheet(null)` is called (for currency, exchange rate, tag, warranty, etc. sheets), the `else` branch fires a `ShowXSheet(false)` event for every sheet type. Only one sheet is ever open, so the behavior is visually correct, but 9–10 state mutations fire for a single user action. Multiply by recomposition frequency.  
**Fix:** Add `DismissAllSheets` events to both VMs, handled with a single `update {}` block that clears all boolean flags atomically.

### M-8 · `observeEntryCoreData` placed in `TransactionUseCases` — wrong abstraction layer
**File:** `TransactionUseCases.kt`  
`TransactionUseCases` now receives `AccountRepository`, `CategoryRepository`, `TagRepository`, and `TemplateRepository` as constructor parameters solely to serve form initialization. None of these repositories are transaction-concern repositories. Any future team member reading `TransactionUseCases` would not expect to find account/category/tag management there.  
**Recommended fix:** Extract `observeEntryCoreData` to a new `EntryFormDataUseCase` class in `di/UseCaseModule.kt`. All three entry ViewModels (`TransactionEntryViewModel`, `PlannedPaymentEntryViewModel`, `TemplateEntryViewModel`) inject it instead of `TransactionUseCases` for form initialization.  
See Questionnaire B-2 for your preference.

### M-9 · `TransactionEntryViewModel` takes a snapshot; the other two VMs subscribe continuously
`TransactionEntryViewModel.initialize()` calls `.first()` on `observeEntryCoreData()`. If you open a transaction entry form, create a new account on another screen via back navigation, then return, the new account doesn't appear. `PlannedPaymentEntryViewModel` and `TemplateEntryViewModel` subscribe continuously and would show the new account. Users editing transfer transactions (which require two accounts) are most likely to notice this.  
See Questionnaire B-3.

### M-10 · `CardRenderCache.invalidateProfile(newProfileId)` evicts the wrong profile's cache
**File:** `OssWalletApp.kt` line 93  
`onEach { newProfileId -> cardRenderCache.invalidateProfile(newProfileId) }` clears the *incoming* profile's cache, which is typically empty or irrelevant. The *outgoing* profile's entries accumulate in the cache unboundedly. After many profile switches, the cache holds stale render data for profiles the user has left.  
**Fix:**
```kotlin
.scan(Pair(-1, -1)) { (_, prev), new -> Pair(prev, new) }
.drop(1)
.onEach { (prevId, _) -> cardRenderCache.invalidateProfile(prevId) }
```

### M-11 · `GlassSettings.borderWidth` default change has zero visual effect
**File:** `GlassSettings.kt`  
`SurfaceProperties.borderWidth` default changed `0.5f → 1f`. All 7 presets (`standard`, `drawer`, `pill`, `card`, `sheet`, `popup`, `item`) pass `0.5f` explicitly as a positional constructor argument. None use the default. No glass surface in the app is visually affected.  
If the intent was thicker borders across all surfaces, all 7 preset lines need updating. See Questionnaire and M-11 action depends on developer intent.

### M-12 · `TransactionEntryService` is an `object` but depends on `CurrencyService` global mutable state
**File:** `TransactionEntryService.kt`  
Functions like `calculateTargetAmountValue` call `CurrencyService.precision` which reads from an `AtomicReference` that changes at runtime when formatting settings change. Unit tests that exercise precision-dependent calculations must set `CurrencyService.setFormatConfig(...)` before each test — creating global state ordering dependencies between tests.  
See Questionnaire B-1.

### M-13 · `TemplateEntryViewModel` subscribes to templates it never reads
`observeEntryCoreData()` includes `templateRepository.getAllTemplates(profileId)` in its combined flow. The template entry VM never uses `data.templates`. A Room subscription (and associated query) runs throughout the template entry session for no benefit.

### M-14 · `FormSheetState` is missing `@Immutable` — unnecessary recompositions
**File:** `EntryCore.kt`  
`FormSheetState` is a `data class` with only `Boolean` and `AmountEditingSide` (enum) fields — all intrinsically stable types. Without `@Immutable`, the Compose compiler conservatively treats it as unstable. Every composable that receives `FormSheetState` as a parameter recomposes whenever the parent recomposes, even when `FormSheetState` hasn't changed.  
**Fix (one annotation):** Add `@Immutable` above the `data class FormSheetState` declaration.

### M-15 · Creating a TRANSFER template from a suggestion loses destination account and currency
`TemplatePrefillDTO` has no `targetAccountId`, `targetCurrency`, or `exchangeRate` fields. When a TRANSFER suggestion is used to prefill a template creation form, only the source account, source currency, and amount are carried over. The user must manually re-enter the transfer destination.  
See Questionnaire and discuss whether TRANSFER prefill via DTO is an intended feature.

### M-16 · `SmartSuggestionsScreen` — stale prefill auto-navigates on re-entry
**File:** `SmartSuggestionsScreen.kt`  
If the user back-navigates from `SmartSuggestionsScreen` without entering `PlannedPaymentEntry`, `plannedPaymentPrefill` in the ViewModel stays non-null. Re-entering the screen fires the `LaunchedEffect` with the stale value and immediately navigates to `PlannedPaymentEntry` without user input.  
**Fix:** Add a `DisposableEffect { onDispose { viewModel.clearPlannedPaymentPrefill() } }` in the `SmartSuggestionsRoute` composable.

### M-17 · `sections` in `QuickAddViewModel` recomputes on every keystroke, not just query changes
**File:** `QuickAddViewModel.kt`  
`_uiState.map { it.searchQuery }` emits whenever any part of `_uiState` changes — amount digit typed, account tapped, anything. Without `.distinctUntilChanged()`, the full suggestion scan (iterating all transactions, grouping, sorting) runs on every single UI state change, not just when the search query actually changes.  
**Fix (one word):** `_uiState.map { it.searchQuery }.distinctUntilChanged()`

### M-18 · `SettingsViewModel.deleteAllData()` silently fails if `deleteAllData()` throws
If `backupRepository.deleteAllData()` throws (SQLCipher error, disk full, constraint violation), `viewModelScope.launch {}` swallows the exception. The user sees the "Clearing..." message (already sent to the channel) but the app neither restarts nor shows an error. Data is in an indeterminate partial-delete state.  
**Fix:** Wrap in `try/catch` and send a failure message: `_backupMessage.trySend("Failed to clear data: ${e.message}")`.

---

## LOW — Polish, Edge Cases, and Cleanup

| ID | File | Finding | Severity |
|----|------|---------|---------|
| L-1 | `CategoryDao.kt` | `getCategoryByName(name)` and `getSubCategories(parentId)` — no `profileId`; cross-profile leaks in import/seed flows | Low |
| L-2 | `TagDao.kt` | `getAllTagsSync()` — cross-profile, undocumented. Add `// NOTE: cross-profile, for backup use only` | Low |
| L-3 | `BackupRepositoryImpl.kt` | File I/O (large CSV reads) inside `database.withTransaction {}` — holds exclusive DB lock during reads. Read all data into memory first, then open transaction | Low |
| L-4 | `BackupRepositoryImpl.kt` | `accountId` defaults to `""` in `restoreTransactions` — FK violation crash on bad CSV rows. Skip and log instead | Low |
| L-5 | `BackupRepositoryImpl.kt` | id=0 defaults for categories/profiles/transactions — Room auto-generates new IDs on `@PrimaryKey Long`, breaking FK graph. Skip records with unparseable IDs | Low |
| L-6 | `BackupRepositoryImpl.kt` | `parseTagNames` splits on `,` — corrupts tag names containing commas (e.g., "food, dining" → ["food", " dining"]) | Low |
| L-7 | `CsvImportServiceImpl.kt` | `profileBaseCurrency` defaults to `"INR"` — developer locale artifact, should be a project constant (`"USD"`) | Low |
| L-8 | `QuickAddViewModel.kt` | Two independent `getAllTransactions(profileId)` Room subscriptions (one in `sections`, one in `state`) — doubles DB cursor load. Hoist to a shared `private val transactions` StateFlow | Low |
| L-9 | `QuickAddViewModel.kt` | Suggestions sorted `sortedByDescending { it.label }` (alphabetical Z→A). Should sort by recency (latest transaction timestamp). `latestTimestamp` is computed but discarded in the `.map()` step | Low |
| L-10 | `QuickAddViewModel.kt` | Cross-currency transfer counterpart lookup: `amount.abs().compareTo()` always fails for USD→EUR pairs (100 ≠ 85). Match on timestamp window + `isTransfer` only | Low |
| L-11 | `QuickAddSheet.kt` | Keyboard not hidden when advancing from STEP_TEMPLATE → STEP_ACCOUNT. The search field on STEP_TEMPLATE can leave IME open, overlapping the account grid | Low |
| L-12 | `QuickAddAmountStep.kt` | `key(account?.id, toAccount?.id)` in numpad forces full numpad recreation on account change, discarding any in-progress expression. Only `activeAmountSide` should be in the key | Low |
| L-13 | `QuickAddAmountStep.kt` | `formatCurrency(closingBalance, accountCurrency)` called with blank `accountCurrency` for Outside Wallet accounts — malformed/empty display in closing balance footer | Low |
| L-14 | `QuickAddAmountStep.kt` | `HapticFeedbackType.LongPress` fired on short tap (onClick) — heavy vibration on what should be a light tap. Use `TextHandleMove` or remove | Low |
| L-15 | `QuickAddTemplateStep.kt` | Mode toggle button is icon-only with label commented out and no `contentDescription` — inaccessible to screen readers, invisible to discoverability | Low |
| L-16 | `QuickAddTemplateStep.kt` | `LazyVerticalGrid items()` has no `key` lambda — breaks item animations on list changes, forces full re-bind | Low |
| L-17 | `QuickAddViewModel.kt` | Category becomes `SYSTEM_UNKNOWN` (not `null`) after mode transition. `selectedCategoryId ?: SYSTEM_OTHERS` fallback doesn't trigger because `SYSTEM_UNKNOWN` is non-null | Low |
| L-18 | `QuickAddViewModel.kt` | Nested `flatMapLatest` in `sections` creates a brief window where stale cross-profile data can be emitted on profile switch | Low |
| L-19 | `TransactionEntryViewModel.kt` | `buildTransactionRequest` passes `exchangeRates = emptyMap()` and `anchor = "USD"` hardcoded — snapshot always empty; any rate conversion inside `createRequest` silently returns 1.0 | Low |
| L-20 | `TransactionEntryService.kt` | `getMarketRate` returns `BigDecimal.ONE` for blank currency — implies 1:1 rate before target account is selected. Should return a sentinel value the UI renders as "—" | Low |
| L-21 | `TransactionEntryService.kt` | `syncTransferState` FIXED mode: `impliedRate = BigDecimal.ONE` when source = 0 — displays "1.00" in the rate field instead of "—" | Low |
| L-22 | `TransactionEntryService.kt` | `amountOverride: BigDecimal?` parameter name is misleading — it is the transfer *target* amount, not an override of source. Rename to `targetAmountOverride` | Low |
| L-23 | `TemplateEntryState.kt` | `state.targetAmount ?: ""` and `state.targetCurrency ?: state.currency` — both fields are now `String` (non-nullable), making `?:` dead code with compiler warnings | Low |
| L-24 | `PlannedPaymentEntryViewModel.kt` | Stale imports: `AccountRepository`, `CategoryRepository`, `TagRepository` still imported after constructor removal | Low |
| L-25 | `PlannedPaymentEntryState.kt` | `state.targetCurrency ?: state.currency` — `targetCurrency` is non-nullable; `?:` branch is dead | Low |
| L-26 | `EntryActionCard.kt` / `EntryActionChip.kt` | `cardCornerRadius` (28dp) used for chip-sized elements. `Dimensions.kt` documents: "use chipCornerRadius for small interactive metadata tags." Should be `chipCornerRadius` (20dp) | Low |
| L-27 | `EntryEditorialInput.kt` | `borderWidth` `animateDpAsState` — both non-flush branches return identical `config.surface.borderWidth.dp`. Animation never transitions between values; overhead with no effect | Low |
| L-28 | `TransactionPrefillDTO.fromTemplate()` | `warrantyExpiration`, `gpsLocation`, `location`, `transactionStatus` dropped when applying template to a transaction. Template data with these fields is silently lost when the template is used | Low |
| L-29 | `QuickAddViewModelTest.kt` | `assertEquals(BigDecimal("50"), state.amount)` uses `equals()` which is scale-sensitive. `BigDecimal("50") != BigDecimal("50.00")`. Use `compareTo() == 0` | Low |
| L-30 | Both test files | `mockk(relaxed = true)` used for all repositories — violates CLAUDE.md convention (Fake* with MutableStateFlow) | Low |

---

## Developer Questionnaire

Answer each question before the corresponding fixes are implemented. Your answers determine which implementation path is correct.

---

### Section A: Database Schema

**A-1. How should system categories (SYSTEM_TRANSFER, SYSTEM_OTHERS, etc.) be visible across profiles?**

System categories are inserted by `ensureSystemCategories()` without a `profileId`, so they land in profile 1. A user on profile 2 will see no "Transfer" or "Initial Balance" categories in their pickers, breaking core functionality.

Which strategy?
- [X] **A. Duplicate per profile:** When creating a new profile, copy the 5 system categories for it with that profile's ID. Simple but creates N copies.
- [ ] **B. UNION query in DAO:** `getAllCategories(profileId)` returns `WHERE profileId = :profileId OR isDefault = 1`. System categories are global, user categories are per-profile. No duplication, but the query is more complex.
- [ ] **C. Sentinel `profileId = 0`:** System categories get `profileId = 0`. The DAO query is `WHERE profileId = :profileId OR profileId = 0`. Clean separation between "system" and "user-owned" categories.

---

**A-2. Migration plan for the 14 ViewModels using unscoped `categoryRepository.categories`**

These 14 callers now receive categories from all profiles to every user (H-5). Fixing them all in this PR is significant scope.

- [X] **A. Fix all 14 in this PR:** Safest from a data integrity standpoint. Each ViewModel gets a `flatMapLatest { profileId -> categoryRepository.getAllCategories(profileId) }` chain. Estimated effort: ~3 hours.
- [ ] **B. Deprecate + track:** Add `@Deprecated("Use getAllCategories(profileId)", ReplaceWith("getAllCategories(profileId)"))` to the interface property now. Open a follow-up PR targeted at migrating all 14. Merge this one with the deprecation annotation only.
- [ ] **C. Some callers are intentionally cross-profile:** Identify which callers (if any) should legitimately see all-profiles categories (e.g., backup/export flows). List them here: ___________

---

**A-3. Should `isRateFixed` be persisted on templates?**

Custom exchange rates on templates are silently lost on the first interaction after reopening (H-6).

- [ ] **A. Yes — add `isRateFixed` to `Template` and `TemplateEntity`:** Requires schema version 29 with a Migration. The correct fix. Templates fully preserve the user's configuration.
- [X] **B. No — templates always use market rate on reload:** Document this as intentional. Remove the "lock rate" toggle from the template form, or show it as session-only with a tooltip "Rate resets to market on next use."

---

### Section B: Architecture Decisions

**B-1. Should `TransactionEntryService` be an `object` or a Hilt-injectable class?**

The service is declared `object TransactionEntryService` but internally reads `CurrencyService.precision` — a global `AtomicReference`. Tests that need specific precision behavior must reach into global state.

- [X] **A. Convert to injectable class:** `@Singleton class TransactionEntryService @Inject constructor(private val currencyService: CurrencyService)`. Tests receive a fake `CurrencyService`. This is the correct architecture per Clean Architecture principles.
- [ ] **B. Keep as `object`:** Accept the hidden dependency. Add a `@VisibleForTesting fun setFormatConfigForTest(config: FormatConfig)` on `CurrencyService` to handle test isolation. Pragmatic if refactoring the DI graph is too risky right now.

---

**B-2. Should `observeEntryCoreData` live in `TransactionUseCases` or a dedicated use case?**

Currently `TransactionUseCases` takes `AccountRepository`, `CategoryRepository`, `TagRepository`, and `TemplateRepository` — none of which are transaction persistence concerns. A future developer reading `TransactionUseCases` will be surprised.

- [X] **A. Extract to `EntryFormDataUseCase`:** New class, all three entry VMs (`TransactionEntryViewModel`, `PlannedPaymentEntryViewModel`, `TemplateEntryViewModel`) inject it. `TransactionUseCases` reverts to its original 2-repository shape.
- [ ] **B. Keep in `TransactionUseCases`:** Accept the coupling. The class is already large; adding a comment `// Also serves form initialization` is sufficient documentation.

---

**B-3. `TransactionEntryViewModel.initialize()` — one snapshot or continuous subscription?**

Currently uses `.first()` (one-time snapshot). The other two VMs subscribe continuously. A new account created while the form is in the back stack won't appear on return.

- [X] **A. Change to continuous:** Add the account/category/tag flows to the ViewModel's `init` block using `combine` + `stateIn`. Form always shows current data. Consistent with the other two VMs.
- [ ] **B. Keep snapshot:** The transaction entry form is not expected to be left open for long periods. Document the limitation with a comment. The user can close and reopen the form to see new accounts.

---

**B-4. Transfer amount signing — verify no double-negation**

`TransactionEntryService.createRequest` signs the TRANSFER outflow amount as negative. `TransactionManager.saveTransfer` must NOT negate it again.

- [ ] **A. Confirmed: `TransactionManager` expects pre-signed amounts.** No double-negation. (Please verify this yourself by checking `saveTransfer`'s handling of `request.amount`.)
- [ ] **B. Double-negation found.** Describe where: ___________
- [X] **C. review and find all the paths, and verify, if found then fix**

---

**B-5. Rename `amountOverride` parameter in `TransactionEntryService.createRequest`?**

The parameter is the transfer *target* amount, not an override of source. Its name suggests it overrides something it doesn't.

- [X] **A. Rename to `targetAmountOverride` (or `destinationAmount`):** Clearer intent, easier for future maintainers.
- [ ] **B. Keep as `amountOverride`:** Acceptable, internal API only.

---

### Section C: QuickAdd Flow Decisions

**C-1. Should selecting a fully-specified template skip STEP_ACCOUNT?**

Currently, selecting any template always lands on STEP_ACCOUNT, even when the template already specifies an account. This means power users who created templates for one-tap entry still have to navigate through STEP_ACCOUNT manually.

- [X] **A. Skip to STEP_AMOUNT when template has an account:** If `template.accountId != null` and the account is valid, jump directly to STEP_AMOUNT. Users can go back if they want to change it. Same if they select Existing transaction suggestion for prefill.
- [ ] **B. Always show STEP_ACCOUNT:** Intentional review step. Users should confirm the account even when pre-filled.
- [ ] **C. Auto-save for fully-specified templates:** If template has account, amount, and type — save immediately without showing any steps. One tap = one transaction.

---

**C-2. Should a successful QuickAdd save play the dismiss animation?**

On success: `finish()` fires immediately (snap close). On cancel/back: 300ms slide-down animation plays first.

- [X] **A. Play `animateDismiss()` on success too:** Consistent and feels intentional. Success could also show a brief "checkmark" animation before closing.
- [ ] **B. Keep snap-close on success:** The abrupt close signals "done, no more action needed." Intentional contrast with the deliberate cancel animation.

---

**C-3. What happens when the mode toggle lands on SUGGESTIONS with a blank query?**

Cycling the mode button can land on SUGGESTIONS mode when the search query is empty, showing a confusing "Search to see suggestions" empty state.

- [ ] **A. Skip SUGGESTIONS in the cycle when query is blank:** Mode toggle only offers ALL → TEMPLATES → CATEGORIES when no query is typed. SUGGESTIONS appears as a mode option only after typing.
- [X] **B. Keep current:** The SUGGESTIONS mode is reachable with blank query and shows instructional text. Acceptable if the empty state copy is clear enough.

---

**C-4. `onSuggestionSelected` callback — 11 positional parameters**

Adjacent `String?` parameters in an 11-parameter lambda — any argument transposition is compiler-invisible.

- [X] **A. Refactor to `(QuickAddResult.SuggestionItem) -> Unit`:** Type-safe, self-documenting. The ViewModel unpacks the model internally. Eliminates the bug class entirely. Similarly, template item.
- [ ] **B. Keep 11-parameter lambda:** It's an internal callback; no external callers at risk. Acceptable technical debt.

---

**C-5. Cross-currency transfer counterpart detection in suggestions**

The suggestion builder tries to find both legs of a transfer using `amount.abs().compareTo()`. For USD→EUR transfers with amounts 100 and 85, this always fails. Suggestions for cross-currency transfers are broken.

- [ ] **A. Match on `linkedTransferId` field:** The cleanest solution if the domain `Transaction` model has this field. Check if it exists: `grep -r "linkedTransferId" core/domain/`.
- [ ] **B. Match on timestamp window + `isTransfer` flag only (ignore amount):** Find the paired leg by proximity in time + transfer type. Less precise but works for all currencies.
- [ ] **C. Disable cross-currency transfer suggestions:** Show a single-leg entry for cross-currency transfers, let the user fill in the destination manually. Simplest fix.
- [X] **D. Show both legs:** remove the custom check 
---

### Section D: PlannedPayment Entry Decisions

**D-1. When a planned payment is edited, should the `PlannedPayment` entity's financial fields be updated?**

Currently, `savePlannedPayment()` in edit mode updates the schedule (recurrence, dates), status, and tags — but does NOT update amount, accountId, currency, categoryId, payee, note, or location on the `PlannedPayment` entity. The linked Template IS updated. However, `confirmOccurrence()` reads from `PlannedPayment.amount` to create the transaction, not from the template.

**Scenario:** User has a "Netflix" planned payment for $14.99. They edit it to $19.99 (price increase). The template now says $19.99. But the next auto-confirmed occurrence uses `PlannedPayment.amount = $14.99`. Wrong amount.

- [ ] **A. Fix: apply all financial fields to `PlannedPayment` entity in edit mode:** `confirmOccurrence` reads correctly, user sees their edited amount in confirmed transactions.
- [ ] **B. This is intentional:** Financial data lives in the Template, not the PlannedPayment. Change `confirmOccurrence` to read from the linked Template instead of `PlannedPayment`. Document this architectural decision clearly.
- [X] **C. All core financial fields must be used from the template:** Planned Payment is to only describe the payment planning and details
---

**D-2. Should `nextDueDate` be recalculated when recurrence or start date changes in edit mode?**

Currently, editing a planned payment preserves the old `nextDueDate`. If the user changes a "monthly on the 1st" payment to "monthly on the 15th," the `nextDueDate` still says "Aug 1."

- [X] **A. Recalculate on edit:** Call `PlannedPaymentService.nextDueDate(rule, startDate)` in `savePlannedPayment()` and use the result.
- [ ] **B. Keep existing `nextDueDate`:** A background worker recalculates this asynchronously. The edit doesn't need to do it synchronously.

---

### Section E: UI Component Decisions

**E-1. Corner radius on `EntryActionCard` and `EntryActionChip` — card vs chip?**

Changed from `chipCornerRadius` (20dp) to `cardCornerRadius` (28dp). `Dimensions.kt` documentation: *"Use chipCornerRadius for small interactive metadata tags."* These components are chip-sized metadata elements.

- [ ] **A. Revert to `chipCornerRadius`:** Follow the documented Aeon dimension system rules. The 28dp radius is too aggressive for chip-sized elements.
- [X] **B. Keep `cardCornerRadius` (28dp):** This is a deliberate visual direction change. Update `Dimensions.kt` documentation to reflect that these components use card radius. If so, explain why: Minor UI design decision, will revisit based on customer feedback

---

**E-2. `EntryField` enum only covers 4 of the 12 sheet types in `EntrySheetOrchestrator`**

8 sheet types (currency, exchange rate, tags, status, payment method, location, warranty date, linked transactions) dismiss with `onDismissSheet(null)`. Callers can't identify which sheet was dismissed from the callback type.

- [X] **A. Extend `EntryField` enum with all 12 cases:** Type-safe, self-documenting, allows future callers to distinguish sheet dismissals.
- [ ] **B. Accept `null` as discriminator for non-field sheets:** Document the convention: `null = non-validation sheet dismissed`. Sufficient for current use cases.

---

**E-3. The status sheet button on Transaction Entry silently does nothing for new (unsaved) transactions**

`showStatus` in `FormSheetState` is only shown when `transactionStatus != null`. New transactions don't have a saved status yet. The status chip button appears in the UI but tapping it opens nothing.

- [X] **A. Default to `TransactionStatus.CLEARED` for new transactions:** Most common status; aligns with bank import expectations. Users who want a different status can tap and change it.
- [ ] **B. Disable/hide the status chip until the transaction is saved:** Cleaner state model but removes the ability to set status before first save.
- [ ] **C. Pre-populate `transactionStatus` in `TransactionEntryState` with `TransactionStatus.CLEARED`:** Same as A but done at state initialization rather than in the UI layer.

---

**E-4. `ExchangeRatePill` icon tint — should "live rate" or "fixed rate" be the prominent state?**

Currently: fixed/locked rate → full opacity (prominent). Live/auto rate → 60% opacity (subdued). This is semantically inverted — the live/market rate is the normal/preferred state and should be prominent.

- [ ] **A. Swap tints:** Full opacity for live rate (the default), 60% for fixed/locked (the "override" state). Matches UX convention: defaults are primary, overrides are secondary.
- [X] **B. Keep current:** The "lock" icon at full opacity signals "this rate is locked and important." The lock icon at 60% would look disabled. Current semantics are deliberate for this specific component.

---

### Section F: Tests and Coverage

**F-1. MockK vs Fake* for ViewModel tests**

Both new test files use `mockk(relaxed = true)`. CLAUDE.md mandates `Fake*` implementations backed by `MutableStateFlow`. The relaxed mock pattern makes tests that return `flowOf()` (never-emitting flows) silently hang on `.first()` calls, as found in C-3.

- [ ] **A. Rewrite using `Fake*` implementations:** `FakeTransactionUseCases`, `FakeSettingsRepository`, `FakeTagRepository`. Each backed by `MutableStateFlow` so you can push test data without mocking. This is the only approach that makes `.first()` and `.collect` work predictably in tests.
- [ ] **B. Keep MockK but stub every flow:** For every `fun returns Flow<X>` in a mock, add `every { mock.method(any()) } returns flowOf(someValue)`. Fragile — you must remember to stub every call or tests hang. Not recommended but acceptable short-term.
- [X] **C. Following the global standard for best practices, suitably make the updates
---

**F-2. `clearPrefillDto()` has zero test coverage**

Commit `10168a0c` ("fix(transaction-entry): implement clearPrefillDto to clear state on dispose") is the fix for a known bug, but the test file doesn't exercise it. If this function regresses, no test will catch it.

- [X] **A. Add coverage in this PR:** Test: after `initialize()` with a prefill DTO, call `clearPrefillDto()`, assert `state.prefillDto == null`. One test, 15 lines.
- [ ] **B. Defer to a dedicated test sprint:** Add to backlog.

---

**F-3. Double-save prevention in `QuickAddViewModel` is untested**

H-1 identifies that the double-save guard is incomplete. Even after the fix, without a test, the guard could be silently broken in any future refactor.

- [X] **A. Add a concurrency test in this PR:** Call `saveTransaction()` twice before `advanceUntilIdle()`. Assert `transactionManager.handleSaveTransaction` was called exactly once.
- [ ] **B. Defer to a dedicated test sprint:** Add to backlog.

---

## Priority Action Table

Work through P0 first — without fixing these, nothing else can be tested.

| Priority | ID | File | Fix Complexity |
|----------|----|------|---------------|
| **P0 — Compile error** | C-1 | `PlannedPaymentEntryViewModel.kt` | Delete 1 line |
| **P0 — Compile error** | C-2 | `PlannedPaymentEntryScreen.kt` | Delete import + 5-line block |
| **P0 — Compile error** | C-3 | `TransactionEntryViewModelTest.kt` | Remove 3 params + stub the flow |
| **P0 — Crash on upgrade** | C-4 | `AppDatabase.kt` MIGRATION_27_28 | Add 2 SQL lines |
| **P0 — Crash on install** | C-5 | `CategoryEntity.kt`, `TagEntity.kt` | Add 1 annotation to each |
| **P0 — Silent broken feature** | C-6 | `TransactionFormBody.kt` | Add ~15 lines of composable |
| **P0 — Silent broken feature** | C-7 | `PlannedPaymentFormBody.kt` + `PlannedPaymentEntryState.kt` | Add status sheet + fix mapping |
| **P1 — Data corruption** | H-1 | `QuickAddViewModel.kt` | Move 1 line before `launch` |
| **P1 — Data corruption** | H-2 | `TransactionEntryViewModel.kt` + `TransactionEntryState.kt` | Add `isSaving` field + guard |
| **P1 — Data destruction** | H-3 | `BackupRepositoryImpl.kt` | Change `clearAll()` to upsert |
| **P1 — Profile data leak** | H-4 | `TagRepositoryImpl.kt` | Pass `profileId` in 2 places |
| **P1 — Profile data leak** | H-5 | 14 ViewModels | `flatMapLatest` migration per Q A-2 |
| **P1 — Silent data loss** | H-6 | `Template.kt` + `TemplateEntity` | Per Q A-3; may need v29 migration |
| **P1 — Resource leak** | H-7 | `CsvImportServiceImpl.kt` | `.use {}` wrapper |
| **P1 — UX blocking** | H-8 | `SettingsViewModel.kt` | Buffered channel or trySend |
| **P1 — Visual regression** | H-9 | `EntryEditorialInput.kt` | Restore `if (isFlush)` conditional |
| **P2 — Medium** | M-1 through M-18 | Various | See individual entries above |
| **P3 — Low/cleanup** | L-1 through L-30 | Various | Backlog |

---

## Confirmed Correct — Good Changes in This Diff

The following were verified and are either clean improvements or correct fixes:

| Change | Why It's Good |
|--------|--------------|
| `javax.inject` → `jakarta.inject` across all 131 files | Correct for Hilt 2.59.2; migration is complete with no mixed-namespace files |
| `TransactionSide.amount` default `""` → `"0"` | Coordinated with display change; `SplitStatus.compute` handles both correctly |
| `TagRepositoryImpl.getAllTags()` now maps `profileId` to domain model | Correct fix; previously `profileId` was fetched but silently dropped in the mapping |
| `onAddTag` in Template entry: `SetTags(tags + name)` → `AddTag(name, color)` | Old code added a tag string to the state without persisting it to the repository. New code correctly calls `tagRepository.insertTag()`. Real bug fixed. |
| `observeUniquePayees` / `observeUniqueNotes` as reactive Flow overloads | Correctly scoped with `profileId`, clean API addition alongside existing suspend variants |
| `TransactionViewModel.setPlannedPaymentPrefillFromPattern()`: `profileId = 0` → `settingsRepository.currentProfileId.first()` | Correct fix; zero was not a valid profile ID |
| `CsvProcessor` deleted, replaced with Apache Commons CSV | The hand-rolled CSV parser had no multi-line value support or proper quote-inside-quote handling. The library is the correct tool. |
| ZIP Slip validation in `restoreZipBackup` / `restoreZipBackupSelective` | Security fix; a malicious ZIP could previously escape the temp directory |
| 1GB size limit guard on restore operations | Defensive addition; prevents OOM on pathological inputs |
| `GlassSurface` solid-mode border color fix (`AppColors.outlineVariant` instead of semi-transparent `glassRimColor`) | Targeted fix, correct for the specific non-glass rendering path |
| `AeonTemplateSelection` amount formatting via `CurrencyText` | Replaces ad-hoc string concatenation with locale-aware formatting; behavioral improvement |
| IME scroll-into-view fix in `EntryEditorialInput` (`BringIntoViewRequester` + 250ms delay + re-focus guard) | Correct implementation of the pattern; addresses issue 3-6 from the story file |
| `BackupRepositoryImpl`: tag serialization `Set.toString()` → `joinToString("|")` | `Set.toString()` produces `[tag1, tag2]` which breaks on import. `joinToString("|")` produces a parseable format |
| Batched inserts (100 rows at a time) in all restore helpers | Prevents SQLite statement limit errors on large backups |
| `TransactionViewModel.setPlannedPaymentPrefillFromPattern()` null-reset before async launch | Correct pattern; clears stale prefill before async construction prevents race with UI reaction |
| `PlannedPaymentEntryViewModel` data loading via `observeEntryCoreData` | Removes 3 direct repository dependencies from the VM constructor; aligns with `TransactionEntryViewModel` pattern |
| Template entry `onCurrencyClick` → opens full currency sheet (was inline `SetCurrency`) | The inline picker bypassed the full currency search sheet; the fix is more complete for users with many currencies |
