# Comprehensive Refactoring Plan: OssWallet "Cards System"

This document outlines the strategic refactoring of the OssWallet Cards System to ensure **SOLID compliance**, **DRY efficiency**, and **Android Homescreen Widget (Glance) Readiness**.

---

## 1. Core Architectural Issues Identified

1.  **Tight Coupling to UI Lifecycle**: Card computation is currently triggered via `LaunchedEffect` in Composable screens or tied to specific ViewModels.
2.  **God Classes**: ViewModels like `DashboardViewModel` and `AnalyticsViewModel` are bloated with complex business logic for multiple card types.
3.  **Fragmented Logic**: Filtering and computation logic are duplicated or scattered across different `computeXxx` methods.
4.  **Open/Closed Principle Violation**: Adding a new card requires modifying multiple ViewModels and UI files instead of simply adding a new calculator and renderer.

---

## 2. Refactoring Phases

### Phase 1: Domain Logic Abstraction (Calculator Pattern)
**Goal**: Decouple business logic from ViewModels and UI.

*   **Action**: Create a `CardCalculator` interface in `domain/calculator/`.
*   **Action**: Implement concrete calculators for each card type (e.g., `NetWorthCalculator`, `CashFlowCalculator`).
*   **Action**: Create a `CardComputationEngine` that acts as a registry for these calculators.

**Reasoning**: This allows logic to be reused across the App UI and Homescreen Widgets without duplication. It follows the **Strategy Pattern**.

### Phase 2: Data Pipeline Optimization
**Goal**: Centralize data ingestion and filtering.

*   **Action**: Define a `RawData` DTO that encapsulates `List<Account>`, `List<Transaction>`, and `Map<String, BigDecimal>` (exchange rates).
*   **Action**: Centralize all filtering logic into `CardFilter.kt` extension functions.
*   **Action**: Pass `RawData` into the `CardComputationEngine` for "one-shot" computation.

**Reasoning**: Reduces redundant database calls and ensures consistent filtering across all contexts.

### Phase 3: ViewModel Refactoring (Reactive Transformation)
**Goal**: ViewModels should only orchestrate data streams.

*   **Action**: Replace `suspend fun computeXxx` methods with a single reactive `Flow`.
*   **Action**: Use `combine` to merge `CardConfig` flows with `RawData` flows, then map them through the `CardComputationEngine`.

**Reasoning**: Removes side-effects from the UI layer and makes the UI a "Passive View" that simply renders whatever the flow emits.

### Phase 4: Widget Integration (Glance Readiness)
**Goal**: Enable logic to run in background workers for Widgets.

*   **Action**: Ensure `CardRenderData` is serializable and contains no Compose-specific types.
*   **Action**: Inject the `CardComputationEngine` into a `GlanceWorker`.

---

## 3. Specific "Dos" and "Don'ts"

### ✅ DO
*   **DO** use the **Strategy Pattern** for card calculators. Each calculator should handle exactly one type of `CardConfig`.
*   **DO** keep `CardRenderData` pure. It should be a simple DTO that any UI (Compose or RemoteViews) can interpret.
*   **DO** inject `CardComputationEngine` via Hilt/Koin for better testability.
*   **DO** perform computations on `Dispatchers.Default` within the `Flow.map` block.
*   **DO** use **memoization** or caching within the `CardComputationEngine` if multiple cards process identical data.

### ❌ DON'T
*   **DON'T** use `LaunchedEffect` to trigger data computation in screens. This makes the logic inaccessible to Widgets.
*   **DON'T** put business logic (BigDecimal math, grouping, filtering) inside ViewModel methods.
*   **DON'T** pass `ViewModel` instances to card Composables.
*   **DON'T** use Android-specific classes (like `Context` or `Color`) inside the `domain/calculator` package. Use Hex strings or theme-agnostic IDs.
*   **DON'T** hardcode "default" configurations in ViewModels. Use a `DefaultConfigFactory`.

---

## 4. Implementation Example: Net Worth Card

### The New Data Flow

1.  **Config**: `NetWorthCardConfig(timelineRange = LAST_30_DAYS)`
2.  **Raw Data**: Fetched from `AccountRepository` and `TransactionRepository`.
3.  **Calculator**: `NetWorthCalculator.calculate(config, rawData)` -> `CardRenderData.NetWorth`
4.  **UI**: `UnifiedCardRenderer` takes the pre-computed `NetWorth` data.

### Widget Readiness Code Snippet (Conceptual)

```kotlin
// Inside a Glance Worker
val rawData = fetchRawData()
val config = fetchWidgetConfig()
val renderData = computationEngine.compute(config, rawData)

// Update the widget UI
WidgetManager.update(renderData)
```

---

## 5. Expected Outcomes

*   **100% Portability**: The same code that computes the chart on your Analytics screen will compute the chart on your Homescreen Widget.
*   **High Scalability**: Adding a new card type takes ~15 minutes (add Config, add Calculator, add Composable).
*   **Testability**: Each `CardCalculator` can be unit tested in isolation without mocking the whole ViewModel or the Compose environment.
