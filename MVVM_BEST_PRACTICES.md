# MVVM + Clean Architecture Best Practices

This document outlines the ideal architecture and patterns for the OssWallet project, following modern Android development standards.

## 1. Ideal File Structure

A clean, modular structure ensures scalability and testability.

```text
com.osscabinet.osswallet/
├── data/                       # Data Layer (Implementation)
│   ├── local/                  # Room database, DAOs, Entities
│   ├── remote/                 # API interfaces, DTOs, Network clients
│   ├── repository/             # Repository implementations
│   └── mapper/                 # Data <-> Domain mappers
├── domain/                     # Domain Layer (Pure Kotlin, Business Logic)
│   ├── model/                  # Domain Models (POJOs)
│   ├── repository/             # Repository interfaces
│   └── usecase/                # Single-purpose business logic classes
├── ui/                         # Presentation Layer (UI)
│   ├── components/             # Reusable Compose components
│   ├── navigation/             # Navigation graphs and routes
│   ├── screens/                # Screen-level Composable functions
│   ├── theme/                  # Theme, Colors, Typography
│   └── viewmodel/              # ViewModels and UI State models
├── di/                         # Dependency Injection (Hilt/Koin or Manual)
└── util/                       # Shared utility classes
```

## 2. Best Practices & Patterns

### ViewModel & UI State
*   **Unidirectional Data Flow (UDF):** The ViewModel exposes a single `StateFlow` (e.g., `UiState`) representing the entire screen state. Events are sent from the UI to the ViewModel.
*   **Immutability:** Always use immutable data classes for `UiState`.
*   **Process Death Handling:** Use `SavedStateHandle` in ViewModels to persist critical state across process death.
*   **Flow Collection:** Use `collectAsStateWithLifecycle()` in Compose to collect flows safely.

### Domain Layer (Use Cases)
*   **Single Responsibility:** Each UseCase should do one thing (e.g., `GetSortedTransactionsUseCase`).
*   **No Android Dependencies:** The domain layer should be pure Kotlin to remain testable and platform-independent.
*   **Business Logic Location:** Complex logic (e.g., net worth calculation) belongs in UseCases or Domain Models, not in ViewModels or the UI.

### Data Layer
*   **Single Source of Truth:** The repository should decide whether to fetch from local cache or remote API.
*   **Offline-First:** Prioritize local storage (Room) and sync with remote APIs in the background.
*   **Encapsulation:** Never expose Database Entities or API DTOs to the UI layer; map them to Domain Models first.

### Dependency Injection
*   **Inversion of Control:** Dependencies should be provided to classes (constructor injection) rather than created inside them.
*   **Scoped Dependencies:** Use proper scoping for Databases and Repositories (usually singletons).

## 3. Class & Function Structures

### ViewModel Structure
```kotlin
class TransactionViewModel(
    private val getTransactions: GetTransactionsUseCase,
    // ... other use cases
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    fun onEvent(event: TransactionEvent) {
        when (event) {
            is TransactionEvent.Refresh -> fetchData()
            is TransactionEvent.Delete -> delete(event.id)
        }
    }
}
```

### UseCase Structure
```kotlin
class GetTransactionsUseCase(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> {
        return repository.getTransactions().map { /* business logic */ }
    }
}
```

### Repository Interface (Domain)
```kotlin
interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    suspend fun saveAccount(account: Account)
}
```
