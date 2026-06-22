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
