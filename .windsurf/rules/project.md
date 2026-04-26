---
trigger: always_on
description: 
globs: 
---

# Android Project Rules for Gemini AI

## Project Overview
This project follows modern Android development best practices using Kotlin, Jetpack Compose, and Clean Architecture (MVVM). The rules below provide guidance for maintaining consistent architecture and code quality across any Android application.

## Architecture Principles

### Clean Architecture Layers
The project follows strict Clean Architecture with three distinct layers:

1. **Data Layer** (`data/`)
   - Room database entities in `data/local/entity/`
   - DAOs in `data/local/dao/`
   - Repository implementations in `data/repository/`
   - Never expose entities or DTOs to UI layer

2. **Domain Layer** (`domain/`)
   - Pure Kotlin - NO Android dependencies
   - Domain models in `domain/model/`
   - Repository interfaces in `domain/repository/`
   - Use cases in `domain/usecase/`
   - Services in `domain/service/` 
   - DTOs in `domain/model/dto/`

3. **UI Layer** (`ui/`)
   - ViewModels in `ui/viewmodel/`
   - Screens in `ui/screens/` (or `ui/` subdirectories)
   - Compose components in `ui/components/`
   - Navigation in `ui/navigation/`

### Strict Separation Rules
- **NEVER** import Android classes in the domain layer
- **NEVER** expose Room entities to the UI layer - always map to domain models
- **NEVER** put business logic in ViewModels or Composables - use UseCases
- **NEVER** access Context in repositories - use dependency injection
- **ALWAYS** use immutable data classes for UI state
- **ALWAYS** expose immutable StateFlow from ViewModels (use `.asStateFlow()`)

## Code Style & Patterns

### ViewModels
- Use Unidirectional Data Flow (UDF)
- Expose single `StateFlow<UiState>` for screen state
- Use `SavedStateHandle` for process death handling
- Keep ViewModels thin - delegate logic to UseCases
- Example pattern:
```kotlin
class ExampleViewModel(
    private val useCase: SomeUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()
    
    fun onEvent(event: ExampleEvent) {
        when (event) {
            is ExampleEvent.SomeAction -> handleAction()
        }
    }
}
```

### Use Cases
- Single responsibility - one use case = one action
- Use `operator fun invoke()` pattern
- Keep business logic here, not in ViewModels
- Example pattern:
```kotlin
class GetItemsUseCase(
    private val repository: ItemRepository
) {
    operator fun invoke(params: GetItemsParams): Flow<List<Item>> {
        return repository.getItems(params)
    }
}
```

### Repositories
- Interface in domain layer, implementation in data layer
- Use Flow for reactive data streams
- Handle offline-first logic (local cache priority)
- Map entities to domain models internally

### Database Operations
- Use Room transactions for multi-table operations
- Use `@Transaction` annotation for complex operations
- Ensure data consistency when updating related entities
- Example: `insertWithRelatedEntityUpdate()`

## Data Types & Precision

### Numeric Values
- Use `BigDecimal` for monetary or precise decimal values
- Use appropriate scale for intermediate calculations
- Round to user-specified precision at display time
- Consider using `Double` for simple numeric values where precision is not critical

### Timestamps
- Store timestamps as `Long` (UTC milliseconds)
- Use proper timezone handling when displaying
- Consider using `Instant` or `LocalDateTime` from java.time for complex date operations

## Domain Models

### General Guidelines
- Use data classes for models that need equality checks
- Primary keys: Use `Long` with autoGenerate for auto-incrementing, or `String` (UUID) for business keys
- Include only relevant fields - avoid over-modeling
- Use sealed classes for restricted hierarchies (e.g., state types, result types)

## Services

### Service Layer Purpose
- Services contain business logic that spans multiple use cases
- Services should be stateless and reusable
- Examples: Analytics computation, data transformation, complex business rules
- Services can be injected into use cases or viewmodels

## Concurrency & Threading

- Use `Dispatchers.IO` for database and network operations
- Use `Dispatchers.Default` for CPU-intensive calculations
- Use `Dispatchers.Main` only for UI updates
- Use `collectAsStateWithLifecycle()` in Compose for Flow collection

## Dependency Injection

- Use constructor injection (Hilt or Koin)
- Never create dependencies manually in classes
- Scope repositories and DAOs as singletons
- Inject UseCases into ViewModels

## Anti-Patterns to Avoid

❌ **DO NOT**:
- Put business logic in Composables
- Expose MutableStateFlow from ViewModels
- Pass Context to repositories
- Use hardcoded magic strings
- Block the main thread with database queries
- Create "God" ViewModels with hundreds of lines
- Neglect process death handling
- Access Android APIs in domain layer

✅ **DO**:
- Use UseCases for business logic
- Expose immutable StateFlow
- Use DI for all dependencies
- Use const val or sealed classes for constants
- Use coroutines/Flow properly
- Keep ViewModels focused on UI state
- Use SavedStateHandle for critical state
- Keep domain layer pure Kotlin

## File Naming Conventions

- ViewModels: `[Feature]ViewModel.kt`
- Screens: `[Feature]Screen.kt`
- Use Cases: `[Action]UseCase.kt`
- Repositories: `[Entity]Repository.kt` (interface in domain, impl in data)
- Entities: `[Entity]Entity.kt` (data layer)
- Domain Models: `[Model].kt` (domain layer)
- DTOs: `[Purpose]DTO.kt` (domain/model/dto/)

## Testing Guidelines

- Domain layer should be fully testable without Android dependencies
- Use fake repositories for ViewModel testing
- Test UseCases independently
- Test UI state transitions in ViewModels

## Documentation References

When making changes, refer to project-specific documentation:
- Architecture documentation in `docs/architecture/`
- Best practices in `docs/guidelines/`
- Implementation guides in `docs/implementation/`
