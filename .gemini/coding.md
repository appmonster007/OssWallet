# OssWallet — Coding Standards Directive

**Scope:** Kotlin style, coroutines, error handling, naming, safety, and code quality rules for the entire codebase.

---

## Goals

- Produce correct, readable, and minimal code — no speculative abstractions.
- Eliminate common safety issues (null crashes, resource leaks, concurrency bugs).
- Maintain consistent naming so future contributors can navigate the codebase without explanation.
- Never introduce security vulnerabilities: no SQL injection, no command injection, no hardcoded secrets.

---

## How to Achieve Them

Write the simplest code that satisfies the requirement. Add abstractions only when the same logic appears three or more times with meaningful variation. Validate at system boundaries (user input, external APIs, database reads); trust internal contracts everywhere else.

---

## Kotlin Language Rules

### Nullability

- Prefer non-null types; use `?` only when null is a meaningful domain state.
- Use `?.let`, `?:`, and `requireNotNull()` over `!!` — the double-bang is banned outside test code.
- Use `checkNotNull()` / `require()` for preconditions in service and ViewModel code.

### Data Classes and Immutability

- Domain models and UI state must be `data class` with `val` properties only.
- Use `copy()` for updates — never mutate a shared domain object.
- Avoid `var` in `data class`; avoid `MutableList` in exposed types.

### Sealed Classes

- `UiState` hierarchies must have `Loading`, `Success(val data: T)`, and `Error(val message: String)` variants.
- Always handle all branches in `when` — never use an `else` catch-all on a sealed class unless the intention is truly "ignore all remaining".

### Functions

- Keep functions under 30 lines. If a function grows larger, it has more than one responsibility.
- Prefer expression bodies (`= ...`) for single-expression functions.
- Prefer extension functions for utilities that operate on a single type.

---

## Coroutines

```kotlin
// Correct: launch from viewModelScope, no raw Thread or AsyncTask
viewModelScope.launch {
    val result = repo.fetchData()
    _uiState.value = XxxUiState.Success(result)
}

// Correct: suspend functions in the data layer, not callbacks
suspend fun fetchData(): List<Item> = withContext(Dispatchers.IO) {
    dao.getAll()
}

// Wrong: blocking the main thread
fun fetchData() = runBlocking { dao.getAll() }  // never do this
```

- All repository suspend functions run on `Dispatchers.IO` (Room and Retrofit already do this internally).
- `viewModelScope` is the only coroutine scope in ViewModels — never create a raw `CoroutineScope` in a ViewModel.
- Cancel-safe: prefer `flow { }` + `collect` over one-shot `async`/`await` chains for reactive data.

---

## Error Handling

```kotlin
// Correct: map errors to UiState
viewModelScope.launch {
    _uiState.value = XxxUiState.Loading
    runCatching { repo.fetchData() }
        .onSuccess { _uiState.value = XxxUiState.Success(it) }
        .onFailure { _uiState.value = XxxUiState.Error(it.message ?: "Unknown error") }
}
```

- Handle errors at ViewModel boundaries, not inside domain services (services throw, ViewModels catch).
- Do not swallow exceptions with an empty `catch` block.
- Do not expose raw `Throwable` stack traces in `UiState.Error` — use a user-facing message string.
- In Glance widgets, always wrap data loading in `runCatching` so `provideContent {}` is always reached.

---

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Classes / interfaces | PascalCase | `TransactionManager`, `BudgetRepository` |
| Functions / variables | camelCase | `calculateSpent()`, `profileId` |
| Constants | SCREAMING_SNAKE | `MAX_RETRY_COUNT` |
| Room entities | `XxxEntity` suffix | `TransactionEntity` |
| Domain models | No suffix | `Transaction`, `Account` |
| DTOs | `XxxDto` suffix | `ExchangeRateDto` |
| DAOs | `XxxDao` suffix | `TransactionDao` |
| Composables | PascalCase | `TransactionCard`, `AeonButton` |
| Test classes | `XxxTest` suffix | `BudgetServiceTest` |
| Fake test doubles | `FakeXxx` prefix | `FakeBudgetRepository` |

---

## Financial Arithmetic

- Always use `BigDecimal` for monetary values — never `Float` or `Double`.
- Construct `BigDecimal` from `String`, not from floating-point literals: `BigDecimal("10.50")` not `BigDecimal(10.5)`.
- Use `setScale(precision, RoundingMode.HALF_UP)` before displaying amounts.
- Never store amounts as `REAL` in SQLite — store as `TEXT` via TypeConverter.

---

## Security Rules

- **No hardcoded secrets.** API keys, passphrases, and credentials must come from `local.properties`, environment variables, or `SecurePassphraseManager` — never from source code.
- **No SQL string concatenation.** All DAO queries use Room's `@Query` parameter binding (`:param` syntax). Never build SQL strings with user input.
- **No logging of sensitive data.** Do not log account balances, passphrases, currency amounts, or user identifiers in `Log.d`/`Log.e` in release builds. Gate with `BuildConfig.DEBUG`.
- **No `byteArrayOf()` as the database passphrase in production.** That opens a separate unencrypted database. Only use it in instrumented test in-memory builders.
- **No implicit trust of external data.** Validate and sanitize all data received from Retrofit before passing to the domain layer.

---

## Comments

- Write comments only when the *why* is non-obvious: a hidden constraint, a workaround for a known library bug, or a subtle invariant.
- Do not comment what the code does — well-named identifiers do that.
- Do not reference issue numbers, PR numbers, or caller names in inline comments.

---

## Best Practices

- Prefer `listOf()`, `mapOf()`, and `setOf()` (immutable) over their mutable equivalents for return types and function parameters.
- Prefer `data class` over `Pair`/`Triple` when returning multiple values from a function.
- Prefer `when` expression over `if/else if` chains with more than two branches.
- Use `@JvmOverloads` on `@Composable` functions only when required for interop.
- Prefer `object` declarations for stateless services over companion object methods.

---

## What Not To Do

- Do not use `!!` (non-null assertion) in production code.
- Do not use `runBlocking` in ViewModels or Composables.
- Do not use `Float` or `Double` for financial amounts.
- Do not log sensitive user data outside `BuildConfig.DEBUG` guards.
- Do not build SQL queries via string concatenation.
- Do not create `MutableStateFlow` properties that are public — expose only `StateFlow`.
- Do not use `GlobalScope` anywhere in the app.
- Do not add unused parameters, dead code, or placeholder comments (`// TODO` without a ticket reference).
- Do not introduce abstractions for a single call site.
