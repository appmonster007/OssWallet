l# Plan: Gemini AppFunctions Integration for OssWallet

## Context

The user wants to control OssWallet entirely through the Gemini AI app using natural language — add/edit/delete transactions, budgets, loans, planned payments, filters, and query analytics — **without ever opening the app**. 

The correct technology is **Android AppFunctions** (announced Google I/O 2026), which is Android's on-device MCP equivalent. It lets Gemini discover and invoke app-exposed suspend functions directly, in the background, on-device. Google describes it as "MCP for Android." OssWallet is already perfectly positioned: it targets SDK 36, uses Hilt, and has clean repository interfaces ready to wire up.

> **Not** App Actions / shortcuts.xml — that opens the app UI. AppFunctions runs headless.

---

## How AppFunctions Works

1. You annotate `suspend fun`s with `@AppFunction` and parameter/return types with `@AppFunctionSerializable`
2. KDoc on every function/field becomes the natural-language description Gemini reads to know what to call and when
3. KSP compiler generates discovery metadata at build time
4. `OssWalletApp` implements `AppFunctionConfiguration.Provider` to wire Hilt-injected classes
5. Gemini calls functions in background — no UI opened, no `MainActivity` needed
6. Available from Android 16 (API 36) onwards — OssWallet already targets 36 ✓

---

## Dependencies to Add (`libs.versions.toml` + `build.gradle.kts`)

```toml
# libs.versions.toml
[versions]
appfunctions = "1.0.0-alpha09"

[libraries]
androidx-appfunctions = { module = "androidx.appfunctions:appfunctions", version.ref = "appfunctions" }
androidx-appfunctions-service = { module = "androidx.appfunctions:appfunctions-service", version.ref = "appfunctions" }
androidx-appfunctions-compiler = { module = "androidx.appfunctions:appfunctions-compiler", version.ref = "appfunctions" }
```

```kotlin
// app/build.gradle.kts
ksp {
    arg("appfunctions:aggregateAppFunctions", "true")
}
dependencies {
    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.appfunctions.service)
    ksp(libs.androidx.appfunctions.compiler)
}
```

---

## Files to Create / Modify

### 1. New function class files (in `appfunctions/` package at app layer)

> Placed at `app/src/main/java/com/osscabinet/ossWallet/appfunctions/` — they depend on domain repositories but are not domain logic themselves.

| File | Responsibility |
|---|---|
| `appfunctions/TransactionAppFunctions.kt` | CRUD for transactions |
| `appfunctions/BudgetAppFunctions.kt` | CRUD + progress for budgets |
| `appfunctions/LoanAppFunctions.kt` | CRUD + summary for loans |
| `appfunctions/PlannedPaymentAppFunctions.kt` | CRUD + upcoming/overdue |
| `appfunctions/AnalyticsAppFunctions.kt` | Cash flow, net worth, breakdowns |
| `appfunctions/AccountAppFunctions.kt` | List/create/update accounts |
| `appfunctions/FilterAppFunctions.kt` | Save/apply/list filters (later iteration) |

### 2. `OssWalletApp.kt` — implement `AppFunctionConfiguration.Provider`

Add interface + `@Inject` fields for each `*AppFunctions` class + `appFunctionConfiguration` override that registers them all with `addEnclosingClassFactory`.

### 3. `gradle/libs.versions.toml` + `app/build.gradle.kts` — add dependencies (above)

---

## Capability Design per Function Class

### `TransactionAppFunctions`
```kotlin
/** Creates a new financial transaction in OssWallet. */
@AppFunction(isDescribedByKDoc = true)
suspend fun createTransaction(ctx: AppFunctionContext, params: CreateTransactionParams): TransactionResult

/** Returns a list of transactions matching the given filters. */
@AppFunction(isDescribedByKDoc = true)
suspend fun getTransactions(ctx: AppFunctionContext, params: GetTransactionsParams): TransactionListResult

/** Updates an existing transaction by ID. */
@AppFunction(isDescribedByKDoc = true)
suspend fun updateTransaction(ctx: AppFunctionContext, params: UpdateTransactionParams): TransactionResult

/** Deletes a transaction by ID. */
@AppFunction(isDescribedByKDoc = true)
suspend fun deleteTransaction(ctx: AppFunctionContext, params: DeleteByIdParams): SuccessResult

/** Clones an existing transaction. */
@AppFunction(isDescribedByKDoc = true)
suspend fun cloneTransaction(ctx: AppFunctionContext, params: DeleteByIdParams): TransactionResult
```

Key params: `amount` (String for BigDecimal safety), `currencyCode`, `accountName`, `categoryName`, `type` (income/expense/transfer), `note`, `date` (ISO string), `timePeriod` (today/this_week/this_month/this_year), `limit`.

### `BudgetAppFunctions`
```kotlin
/** Lists all active budgets with current spending progress. */
@AppFunction(isDescribedByKDoc = true)
suspend fun getBudgets(ctx: AppFunctionContext, params: EmptyParams): BudgetListResult

/** Creates a new budget. */
@AppFunction(isDescribedByKDoc = true)
suspend fun createBudget(ctx: AppFunctionContext, params: CreateBudgetParams): BudgetResult

/** Updates a budget's limit or categories. */
@AppFunction(isDescribedByKDoc = true)
suspend fun updateBudget(ctx: AppFunctionContext, params: UpdateBudgetParams): BudgetResult

/** Deletes a budget by ID. */
@AppFunction(isDescribedByKDoc = true)
suspend fun deleteBudget(ctx: AppFunctionContext, params: DeleteByIdParams): SuccessResult
```

Uses `BudgetRepository` + `BudgetService.calculateSpent()` + `AggregationService` for progress.

### `LoanAppFunctions`
```kotlin
/** Lists all loans with remaining balance and interest accrued. */
@AppFunction(isDescribedByKDoc = true)
suspend fun getLoans(ctx: AppFunctionContext, params: EmptyParams): LoanListResult

/** Creates a new loan. */
@AppFunction(isDescribedByKDoc = true)
suspend fun createLoan(ctx: AppFunctionContext, params: CreateLoanParams): LoanResult

/** Records a loan payment. */
@AppFunction(isDescribedByKDoc = true)
suspend fun recordLoanPayment(ctx: AppFunctionContext, params: RecordLoanPaymentParams): LoanResult

/** Settles (closes) a loan. */
@AppFunction(isDescribedByKDoc = true)
suspend fun settleLoan(ctx: AppFunctionContext, params: DeleteByIdParams): LoanResult
```

Uses `LoanRepository` + `LoanService.buildSummary()`.

### `PlannedPaymentAppFunctions`
```kotlin
/** Lists upcoming planned payments for the next N days. */
@AppFunction(isDescribedByKDoc = true)
suspend fun getUpcomingPayments(ctx: AppFunctionContext, params: GetUpcomingParams): PaymentListResult

/** Creates a new planned payment with recurrence rule. */
@AppFunction(isDescribedByKDoc = true)
suspend fun createPlannedPayment(ctx: AppFunctionContext, params: CreatePlannedPaymentParams): PaymentResult

/** Skips the next occurrence of a planned payment. */
@AppFunction(isDescribedByKDoc = true)
suspend fun skipNextOccurrence(ctx: AppFunctionContext, params: DeleteByIdParams): SuccessResult

/** Deletes a planned payment. */
@AppFunction(isDescribedByKDoc = true)
suspend fun deletePlannedPayment(ctx: AppFunctionContext, params: DeleteByIdParams): SuccessResult
```

Uses `PlannedPaymentRepository` + `PlannedPaymentService.generateUpcoming()` + `getOverdue()`.

### `AnalyticsAppFunctions`
```kotlin
/** Returns net worth across all accounts. */
@AppFunction(isDescribedByKDoc = true)
suspend fun getNetWorth(ctx: AppFunctionContext, params: EmptyParams): NetWorthResult

/** Returns income vs expense cash flow for a time period. */
@AppFunction(isDescribedByKDoc = true)
suspend fun getCashFlow(ctx: AppFunctionContext, params: TimePeriodParams): CashFlowResult

/** Returns spending breakdown by category for a time period. */
@AppFunction(isDescribedByKDoc = true)
suspend fun getCategoryBreakdown(ctx: AppFunctionContext, params: TimePeriodParams): CategoryBreakdownResult

/** Returns the top N spending categories. */
@AppFunction(isDescribedByKDoc = true)
suspend fun getTopSpendingCategories(ctx: AppFunctionContext, params: TopCategoriesParams): CategoryBreakdownResult
```

Uses `AggregationService` + `TimelineService` + `ComputationRepository` for exchange rates.

### `AccountAppFunctions`
```kotlin
/** Lists all accounts with current balance. */
@AppFunction(isDescribedByKDoc = true)
suspend fun getAccounts(ctx: AppFunctionContext, params: EmptyParams): AccountListResult

/** Creates a new account. */
@AppFunction(isDescribedByKDoc = true)
suspend fun createAccount(ctx: AppFunctionContext, params: CreateAccountParams): AccountResult
```

---

## Data Isolation — Critical Rule

Every function **must** call `settingsRepository.currentProfileId.first()` at the top and pass it to every repository call. This matches the existing `flatMapLatest` pattern used in ViewModels. Never query without `profileId`.

## BigDecimal Safety

All monetary amounts in params/results use `String` type (not `Double`). Parse with `BigDecimal(params.amount)` inside the function. Return formatted as `amount.toPlainString()`.

---

## `OssWalletApp.kt` Change

```kotlin
@HiltAndroidApp
class OssWalletApp : Application(), AppFunctionConfiguration.Provider {

    @Inject lateinit var transactionFunctions: TransactionAppFunctions
    @Inject lateinit var budgetFunctions: BudgetAppFunctions
    @Inject lateinit var loanFunctions: LoanAppFunctions
    @Inject lateinit var plannedPaymentFunctions: PlannedPaymentAppFunctions
    @Inject lateinit var analyticsFunctions: AnalyticsAppFunctions
    @Inject lateinit var accountFunctions: AccountAppFunctions

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(TransactionAppFunctions::class.java) { transactionFunctions }
            .addEnclosingClassFactory(BudgetAppFunctions::class.java) { budgetFunctions }
            .addEnclosingClassFactory(LoanAppFunctions::class.java) { loanFunctions }
            .addEnclosingClassFactory(PlannedPaymentAppFunctions::class.java) { plannedPaymentFunctions }
            .addEnclosingClassFactory(AnalyticsAppFunctions::class.java) { analyticsFunctions }
            .addEnclosingClassFactory(AccountAppFunctions::class.java) { accountFunctions }
            .build()
}
```

Each `*AppFunctions` class is `@Inject constructor(...)` with the repositories it needs — Hilt handles the graph automatically.

---

## Verification

1. **Build check**: `./gradlew assembleDebug` — KSP generates metadata, no compile errors.
2. **Discover functions via ADB**:
   ```bash
   adb shell cmd app_function list-app-functions | grep com.osscabinet.ossWallet
   ```
   Should list all `@AppFunction` methods with their generated IDs.
3. **Invoke via ADB** (simulates Gemini call):
   ```bash
   adb shell cmd app_function call-app-function \
     com.osscabinet.ossWallet \
     com.osscabinet.ossWallet.appfunctions.TransactionAppFunctions#createTransaction \
     '{"amount":"42.50","type":"expense","categoryName":"Food","note":"Lunch"}'
   ```
4. **Gemini live test** (requires EAP enrollment): Ask Gemini "Add a 50 dollar grocery expense in OssWallet" — Gemini calls `createTransaction` without opening the app.
5. **Unit tests**: Test each `*AppFunctions` class with fake repositories (following existing `Fake*` pattern in `src/test/`).

---

## Status

- [ ] Add dependencies to `libs.versions.toml` + `build.gradle.kts`
- [ ] Create `appfunctions/TransactionAppFunctions.kt`
- [ ] Create `appfunctions/BudgetAppFunctions.kt`
- [ ] Create `appfunctions/LoanAppFunctions.kt`
- [ ] Create `appfunctions/PlannedPaymentAppFunctions.kt`
- [ ] Create `appfunctions/AnalyticsAppFunctions.kt`
- [ ] Create `appfunctions/AccountAppFunctions.kt`
- [ ] Modify `OssWalletApp.kt` to implement `AppFunctionConfiguration.Provider`
- [ ] Write unit tests for each function class

---

## Notes

- AppFunctions is **alpha** (`1.0.0-alpha09`) — API may change. Pin the version.
- Gemini integration is currently in **private preview** with trusted testers as of May 2026. The functions will be discoverable via ADB now, but live Gemini invocation requires EAP enrollment with Google.
- No `MainActivity` changes needed — AppFunctions run headless.
- No `shortcuts.xml` or `capability.xml` needed — the KSP compiler generates all metadata.
- Filter AppFunctions (`FilterAppFunctions`) can be added in a later iteration once core CRUD is working.

## References

- [Overview of AppFunctions](https://developer.android.com/ai/appfunctions)
- [Add the AppFunctions API to your app](https://developer.android.com/ai/appfunctions/add-appfunctions)
- [AppFunctions Jetpack releases](https://developer.android.com/jetpack/androidx/releases/appfunctions)
- [Google details AppFunctions for Gemini](https://9to5google.com/2026/02/25/android-appfunctions-gemini/)