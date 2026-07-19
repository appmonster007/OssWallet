# Coding Standards & Conventions — OssWallet

**Scope:** Kotlin style, coroutines, error handling, naming, safety, and code quality rules.

## 1. Kotlin Language Rules

### 1.1 Nullability
- Use `?.let`, `?:`, and `requireNotNull()` over `!!`. The double-bang is banned outside test code.
- Prefer non-null types; use `?` only when null represents a meaningful domain state.

### 1.2 Immutability
- UI state and Domain models must be `data class` with `val` properties.
- Use `copy()` for updates. Avoid `MutableList` in exposed types.

### 1.3 Sealed Classes
- `UiState` hierarchies must follow the pattern: `Loading`, `Success(val data: T)`, and `Error(val message: String)`.

---

## 2. Coroutines & Flow
- **Scopes**: Only use `viewModelScope` in ViewModels. Never use `GlobalScope`.
- **Dispatchers**: 
    - `Dispatchers.IO` for Database/Network.
    - `Dispatchers.Default` for heavy math/sorting.
- **Reactive Data**: Prefer `flow { }` + `collect` for continuous updates.

---

## 3. Error Handling
- Handle errors at ViewModel boundaries.
- Map exceptions to user-facing strings in `UiState.Error`.
- Do not swallow exceptions with empty `catch` blocks.

---

## 4. Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Classes / interfaces | PascalCase | `TransactionManager` |
| Functions / variables | camelCase | `calculateSpent()` |
| Room entities | `XxxEntity` suffix | `TransactionEntity` |
| Domain models | No suffix | `Transaction` |
| DAOs | `XxxDao` suffix | `TransactionDao` |
| Composables | PascalCase | `TransactionCard` |

---

## 5. Security Guardrails
- **No hardcoded secrets**: All keys must come from `local.properties` or `SecurePassphraseManager`.
- **No SQL injection**: Always use Room's `@Query` parameter binding.
- **Sensitive Data**: Gate sensitive logging with `if (BuildConfig.DEBUG)`.

---

## 6. Financial Integrity
- **ALWAYS** use `BigDecimal` for monetary values.
- Construct via `String`: `BigDecimal("1.23")`.
- Store in DB as `TEXT` via TypeConverter.
