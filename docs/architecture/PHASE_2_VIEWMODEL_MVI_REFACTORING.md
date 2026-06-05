# Phase 2: ViewModel MVI Refactoring & Side Effects

This document outlines the strategy for refactoring the ViewModel layer of OssWallet to a standardized MVI (Model-View-Intent) pattern.

## Why: The Need for Refactoring

As the project has grown, many ViewModels (especially `SettingsViewModel`) have accumulated a high number of independent `StateFlow`s and public methods. This leads to:
1.  **State Inconsistency**: It becomes harder to ensure that multiple state updates atomic and consistent.
2.  **Boilerplate Bloat**: Screens have to collect many individual flows.
3.  **Testing Complexity**: Verifying side effects and state transitions requires tracking multiple disparate streams.
4.  **Architectural Drift**: Phase 1 standardized the UI parameters (`Config`, `Actions`). Phase 2 aligns the ViewModel logic to support this cleaner UI layer.

## What: The Core Pattern

Every ViewModel will be refactored to follow this structure:

### 1. Unified State (`UiState`)
A single immutable data class representing the entire UI state.
```kotlin
data class ExampleUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### 2. Unified Intent (`Action` / `Event`)
A single entry point for all UI-to-ViewModel communication.
```kotlin
sealed class ExampleAction {
    data class OnItemClick(val id: String) : ExampleAction()
    object OnRefresh : ExampleAction()
}
```

### 3. Side Effects (`Effect`)
A stream for one-time events that shouldn't be part of the persistent state (e.g., navigation, toasts).
```kotlin
sealed class ExampleEffect {
    data class ShowToast(val message: String) : ExampleEffect()
    data class NavigateToDetail(val id: String) : ExampleEffect()
}
```

## How: Implementation Details

### Base Structure
We will introduce a lightweight pattern (optionally a `BaseViewModel`) to manage the `uiState` and `effect` streams.

```kotlin
abstract class MviViewModel<S, A, E>(initialState: S) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    abstract fun onAction(action: A)

    protected fun updateState(reducer: (S) -> S) {
        _uiState.update(reducer)
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
```

### Mapping to Phase 1 UI Patterns
- **Screen `Config`**: Derived from or passed to the initial `UiState`.
- **Screen `Actions`**: These will now directly map to `viewModel.onAction(action)`.

## Target ViewModels for Refactoring

| Priority | ViewModel | Key Challenges |
| :--- | :--- | :--- |
| **P0** | `SettingsViewModel` | Merging 25+ independent `StateFlow`s. |
| **P1** | `AccountViewModel` | Combining list state with balance maps. |
| **P1** | `AnalyticsViewModel` | Syncing filters with complex chart data. |
| **P2** | `DashboardViewModel` | Transitioning to the optimized query engine (from the Optimization Plan). |
| **P2** | `TransactionListViewModel` | Handling pagination and complex search states. |

## When: Timeline

- **Step 1: Foundation (Current)**: Define `MviViewModel` and core contracts.
- **Step 2: Settings Migration**: Refactor `SettingsViewModel` and `SettingsScreen`.
- **Step 3: Core Flow Migration**: Refactor `Account` and `Transaction` ViewModels.
- **Step 4: Analytics & Optimization**: Refactor `Analytics` and integrate with the database aggregation service.
- **Step 5: Final Review**: Cleanup legacy methods and perform project-wide verification.

## Success Metrics
- **Predictability**: A single source of truth for all UI state.
- **Stability**: Fewer recompositions caused by multiple flow updates.
- **Testability**: ViewModel tests become simple "Action -> Expected State/Effect" assertions.
