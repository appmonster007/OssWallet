# Architectural Refactor: Decoupled "Cards" System & Widget Readiness

**Author**: Senior Android Architect
**Status**: Comprehensive Plan
**Target**: Decouple business logic from UI, ensure SOLID/DRY compliance, and enable Homescreen Widget (Glance) integration.

---

## 1. Executive Summary

The OssWallet "Cards System" currently faces **Architectural Lifecycle Entanglement**. Business logic for financial metrics is trapped within ViewModels and UI-bound `LaunchedEffect` blocks. This makes the system fragile, difficult to test, and—most critically—**incompatible with Android Homescreen Widgets (Glance)**, which require off-screen, worker-based computation.

This plan moves OssWallet to a **Calculator-Registry Pattern** within a Clean Architecture framework, turning ViewModels into passive orchestrators and the UI into a pure rendering layer.

---

## 2. Current State Analysis: The "Tech Debt" Audit

### A. The "God ViewModel" & SRP Violation
*   **Files**: `DashboardViewModel.kt`, `AccountsViewViewModel.kt`
*   **Issue**: These classes handle navigation focus, repository orchestration, AND complex mathematical computations (e.g., `computeNetWorth`, `computeCashFlow`).
*   **Impact**: Violates **Single Responsibility Principle (SRP)**. Adding a new card type increases complexity exponentially, leading to "Spaghetti ViewModels."

### B. Lifecycle-Bound Computation (The Widget Block)
*   **File**: `DashboardScreen.kt` (lines 430-480)
*   **Issue**: Computation is triggered by `LaunchedEffect` inside the `CardReorderableList`.
*   **Impact**: **Widgets do not have a persistent Compose lifecycle.** They cannot trigger `LaunchedEffect`. Logic must be runnable in a `WorkManager` or `GlanceWorker` to update `RemoteViews`.

### C. Open/Closed Principle (OCP) Violation
*   **Files**: `DashboardScreen.kt`, `UnifiedCardRenderer.kt`, `CardManagerViewModel.kt`
*   **Issue**: To add a new card, a developer must modify massive `when` blocks in 4+ files.
*   **Impact**: High risk of regression. The system is "Closed for Extension" but "Open for Modification."

### D. Redundant Data Processing
*   **Issue**: Multiple cards independently filter the same raw transaction list.
*   **Impact**: Performance bottleneck. Parallel filters on every recomposition cause UI jank as transaction history grows.

---

## 3. Target Architecture: The "Calculator Registry"

We will transition from **"ViewModel-as-a-Processor"** to **"ViewModel-as-an-Orchestrator"**.

### 3.1 The Domain Layer (Pure Kotlin)
1.  **`RawData` (DTO)**: A single container for `Accounts`, `Transactions`, `Rates`, and `Budgets`.
2.  **`CardCalculator<C, R>` (Interface)**: A strategy interface where `C` is the Config and `R` is the Result.
3.  **`CardComputationEngine` (Registry)**: A singleton engine that routes `CardConfig` to the appropriate `CardCalculator`.

### 3.2 The UI Layer (Passive)
1.  **`UnifiedCardRenderer`**: Receives **pre-computed** `CardRenderData`.
2.  **No Logic in Composables**: Screens iterate over a `Flow<List<CardRenderData>>` emitted by the ViewModel.

---

## 4. Detailed Implementation Roadmap

### Phase 1: Logic Extraction (The Strategy Pattern)
*   **Create `CardCalculator<C, R>`**: Define the contract for computation.
*   **Implement Concrete Calculators**: Move logic from `DashboardViewModel` to:
    *   `NetWorthCalculator`
    *   `CashFlowCalculator`
    *   `CurrencyBalancesCalculator`
*   **Why**: Makes math testable with standard JUnit tests—no Android/Compose dependencies required.

### Phase 2: The Computation Engine
*   **Implement `CardComputationEngine`**: A central hub that takes `List<CardConfig>` + `RawData` and returns `List<CardRenderData>`.
*   **Centralize Filtering**: Use the existing `CardFilter.kt` extension functions within the calculators to ensure a single point of truth for data filtering.

### Phase 3: Reactive Pipeline Refactor
*   **Refactor ViewModels**:
    *   Remove all `suspend fun compute...` methods.
    *   Expose `val cardStates: StateFlow<List<CardRenderData>>`.
    *   Use `combine(configFlow, rawDataFlow) { config, raw -> engine.compute(config, raw) }`.
*   **Why**: UI becomes a "Passive View." Performance improves as computations are centralized and handled on `Dispatchers.Default`.

### Phase 4: Widget Integration (Glance)
*   **`WidgetDataWorker`**: A WorkManager task that fetches `RawData`, invokes the `CardComputationEngine`, and updates the `GlanceStateDefinition`.
*   **Why**: Shared logic ensures the Widget and App always show identical data.

---

## 5. Coding Standards: Dos and Don'ts

### ❌ THE "DON'Ts"
1.  **DON'T use `LaunchedEffect` for computation logic.** It ties business logic to screen visibility.
2.  **DON'T put `BigDecimal` math or `groupBy` logic in the ViewModel.** Move it to a `Calculator`.
3.  **DON'T pass `ViewModel` into a Card Composable.** Use `CardRenderData` and `CardCallbacks`.
4.  **DON'T use `com.compose.ui.graphics.Color` in Domain/Calculator.** Use Hex strings or theme-aware IDs.
5.  **DON'T duplicate filtering logic.** If "Income" logic changes, it should change in one `Calculator`, not five ViewModels.

### ✅ THE "DOs"
1.  **DO use Unidirectional Data Flow (UDF).** Expose state via `StateFlow`.
2.  **DO make `CardRenderData` serializable.** Required for `Glance` and process death.
3.  **DO centralize "Conversion to Base Currency".** Use `TransactionsUtilityService` for all rounding and conversion logic.
4.  **DO implement a `DefaultCardConfigFactory`.** Ensures consistency when users add new cards.
5.  **DO use pure Kotlin in the Domain layer.** This ensures portability to any UI framework or background worker.

---

## 6. Success Criteria

1.  **Zero `LaunchedEffect` blocks** used for data processing in `DashboardScreen`.
2.  **`DashboardViewModel`** line count reduced by **>40%**.
3.  **90%+ Unit Test Coverage** for all financial calculators.
4.  **Widget implementation** achieved by calling the same `engine.compute()` method used by the app.

---

## 7. Next Steps (Action Items)

1.  **Action**: Define `CardCalculator` interface in `com.osscabinet.ossWallet.domain.calculator`.
2.  **Action**: Implement `NetWorthCalculator` using logic extracted from `DashboardViewModel.kt`.
3.  **Action**: Register calculators in a new `CardComputationEngine` singleton.
4.  **Action**: Update `DashboardViewModel` to use the engine-driven flow.
