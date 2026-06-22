# Architectural Flaws and Bugs Report - OssWallet

## 1. Currency & Exchange Rate Architecture

### 1.1 Semantic Anchor Mismatch (Critical Bug)
There is a fundamental logical conflict between how exchange rates are prepared and how they are consumed.
*   **The Flaw**: `ComputationRepositoryImpl` attempts to re-base rates to the `selectedCurrency`. If the `pivotRate` is missing (e.g., a partial network fetch), it returns the original rate map (usually USD-based). However, `DashboardViewModel` always labels this map with the `selectedCurrency` as the anchor in the `ExchangeRateSnapshot`.
*   **The Impact**: `CurrencyService.convertAmount` assumes that the `anchor` is always `1.0`. If re-basing fails, the service treats both the old base (USD, which is `1.0` in the map) and the new base (EUR, which is the `anchor`) as having the same value. This results in **all financial data being silently incorrect** until a successful refresh occurs.

### 1.2 Split-Brain Repository Responsibility
The responsibility for fetching, caching, and re-basing rates is fragmented across two repositories with inconsistent implementations.
*   **The Flaw**: `CurrencyRepositoryImpl` uses `Double` and `Map<String, Double>` for its logic, while `ComputationRepositoryImpl` uses `BigDecimal` and `Flow<Map<String, BigDecimal>>`.
*   **The Impact**: Logic is duplicated and divergent. A fix in the re-basing math for `BigDecimal` won't apply to the `Double` path used in other parts of the system, leading to inconsistent calculation results across different screens.

### 1.3 Unanchored Data Flows
The data stream for exchange rates is "blind" to its own context.
*   **The Flaw**: `CurrencyRepository.ratesFlow` emits a `Map<String, BigDecimal>` but does not include the `baseCurrency` used to fetch those rates. 
*   **The Impact**: Downstream consumers (like `ComputationRepository`) have to assume the rates are relative to a specific currency. If the DB contains rates relative to GBP but the app thinks they are USD-based, every conversion in the app will be wrong.

### 1.4 Global Mutable State (Anti-pattern)
Formatting configuration is handled via static side effects.
*   **The Flaw**: `CurrencyService` stores `precision` and `activeSystem` (formatting style) as global `var` properties. These are updated by `MainActivity` via a `LaunchedEffect`.
*   **The Impact**: This is a classic anti-pattern that makes the system fragile. It creates a hidden dependency where UI components or background services might use the "wrong" precision if they execute before `MainActivity` initializes the service.

### 1.5 Silent Failures and Swallowed Exceptions
The system prioritizes "not crashing" over "data integrity."
*   **The Flaw**: In `ComputationRepositoryImpl`, currency updates are triggered in an `init` block using `runCatching`. 
*   **The Impact**: Failures to fetch new rates upon a base currency change are swallowed. The user is never notified that the dashboard is showing stale or mis-anchored data.

### 1.6 Hardcoded "USD" Time-Bombs
*   **The Flaw**: Multiple deprecated overloads in `CurrencyService` hardcode "USD" as the anchor when an `ExchangeRateSnapshot` isn't provided.
*   **The Impact**: Any code accidentally using the `Map` overload instead of the `Snapshot` overload will produce incorrect results for any user whose base currency is not USD.

### 1.7 Weak Gating in Dashboard Rendering
*   **The Flaw**: `DashboardViewModel` gates the high-performance "Phase 2" rendering on `hasLoadedExchangeRates`, which only checks `isNotEmpty()`. 
*   **The Impact**: It does not verify if the rates are relevant to the current `selectedCurrency`, causing the UI to flash incorrect values (using previous currency's rates) before the new rates are fetched.

## 2. Over-engineering and Pattern Abuse

### 2.1 The "Aggregation Framework" Over-engineering
The project implements a complex "Query/Aggregation Engine" (`AggregationPlanner`, `TransactionQueryBuilder`, `DatabaseQueryServiceImpl`) that mimics a full database analytical layer.
*   **The Flaw**: Instead of using simple, optimized DAO methods, it uses a generic system where queries are built at runtime based on "Aggregation Dimensions" and "Metric Configs."
*   **The Impact**: This adds layers of abstraction that make the code harder to follow, test, and optimize. It also introduces overhead for simple queries that could be handled by Room's standard `@Query` annotations.

### 2.2 Stale-While-Recompute Cache Inconsistency
The `CardRenderCache` is used to provide immediate UI feedback, but it lacks data versioning.
*   **The Flaw**: When a transaction is modified, the `DashboardViewModel` immediately emits the stale data from `CardRenderCache` while starting a background recomputation.
*   **The Impact**: The user sees the old balance/totals for several hundred milliseconds after saving a transaction, followed by a jump to the new value. This "flicker" breaks the illusion of immediate reactivity and can be confusing.

### 2.3 God Objects: CardInput & BaseViewModel
*   **The Flaw**: `CardInput` is a "God Object" that carries almost the entire database state to every single card computer. Similarly, `BaseViewModel` injects global settings and exposes them to all ViewModels regardless of need.
*   **The Impact**: High memory pressure and unnecessary recomputations. A change to a single `Loan` triggers a recomputation of the `NetWorthCard` even though `NetWorthCard` might only need the aggregated balance, because `CardInput` (the dependency) changed.

### 2.4 Leaky Aggregation Abstraction
*   **The Flaw**: The aggregation layer returns a generic `AggregatedData` object (basically a `Map<String, Any>`).
*   **The Impact**: This forces domain services like `TransactionAggregationService` to manually map magic strings (e.g., `"income"`, `"expense"`) back to `FinancialSummary` objects. If the SQL alias in `TransactionQueryBuilder` changes, the mapping silently fails or returns zeros.

### 2.5 Inefficient Search and Tagging (Database Anti-pattern)
*   **The Flaw**: The search logic and tag filtering use `%LIKE%` queries on unindexed columns.
*   **The Impact**: As the transaction table grows to thousands of rows, searching by note or filtering by tag will cause full table scans, leading to UI jank and battery drain. The project lacks FTS (Full Text Search) implementation which is the standard for this scale.

### 2.6 Fragmented "Service" Layer
The project has `CardAggregationService`, `TransactionAggregationService`, `AggregationService`, `BudgetService`, and `LoanService`.
*   **The Flaw**: These are implemented as `object` (singletons) rather than injected classes. 
*   **The Impact**: This makes unit testing extremely difficult (requiring static mocking) and leads to procedural code that bypasses Hilt's dependency management, making it harder to track state and lifecycle.

### 2.7 Complex "Phase-based" ViewModel Logic
*   **The Flaw**: `DashboardViewModel` manages its data loading in "Phases" (Structural vs. Card Compute) using a complex `channelFlow` with nested `launch` blocks and manual `AtomicInteger` counters.
*   **The Impact**: This is highly prone to race conditions and memory leaks. It bypasses the safety of structured concurrency provided by standard `Flow` operators like `combine` and `flatMapLatest`.
