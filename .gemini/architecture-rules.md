# Android Architecture Rules for Gemini AI

## Layer-Specific Rules

### Data Layer (`data/`)

#### Entity Rules
- All entities must use Room annotations (`@Entity`, `@PrimaryKey`, etc.)
- Primary keys: Use `Long` with autoGenerate for auto-incrementing IDs, or `String` (UUID) for business keys
- Use appropriate data types based on field requirements (BigDecimal for precision, Long or Instant for timestamps, etc.)
- Timestamps: Store as `Long` (UTC milliseconds)
- Relationships: Use foreign keys with proper cascade actions
- Soft delete: Include `isArchived` or similar boolean fields where applicable

#### DAO Rules
- Use `@Query` with Room SQL for complex queries
- Return `Flow<List<T>>` for reactive data streams
- Use `@Transaction` for multi-table operations
- Include sync versions of queries (non-Flow) when needed for one-time operations
- Example: `getAllItems()` returns Flow, `getAllItemsSync()` returns List

#### Repository Implementation Rules
- Implement repository interfaces from domain layer
- Map entities to domain models internally (never expose entities)
- Handle offline-first logic: check local cache first, then remote
- Use appropriate dispatchers: `Dispatchers.IO` for database operations
- Inject DAOs and remote data sources via constructor

### Domain Layer (`domain/`)

#### Strict No-Android Rule
- **ZERO Android imports allowed** in domain layer
- No Context, no Activity/Fragment, no View, no AndroidX classes
- Pure Kotlin only - makes domain layer testable and platform-independent
- If you need Android-specific functionality, move it to data or UI layer

#### Domain Model Rules
- Use immutable data classes
- No Room annotations or database-specific logic
- Use appropriate data types based on domain requirements
- Timestamps: `Long`
- Include computed properties where useful (e.g., calculated totals, derived status)

#### Repository Interface Rules
- Define interfaces in domain layer
- Return Flow for reactive streams
- Use suspend functions for one-time operations
- Keep interfaces clean - no implementation details

#### Use Case Rules
- Single responsibility principle: one use case = one business action
- Use `operator fun invoke()` pattern for clean call syntax
- Accept domain models as parameters, return domain models or Flow
- Contain business logic, calculations, and data transformations
- Example: `GetFilteredItemsUseCase`, `CalculateTotalsUseCase`

#### Service Rules
- For complex business logic that spans multiple use cases
- Stateless classes with pure functions
- Examples: AnalyticsService, DataTransformationService, BusinessLogicService
- Services can be injected into use cases or viewmodels

#### DTO Rules
- Use DTOs in `domain/model/dto/` for data transfer between layers
- Example: ItemRequest for creating/updating items
- Example: ItemPrefillDTO for UI prefilling
- Keep DTOs focused on specific use cases

### UI Layer (`ui/`)

#### ViewModel Rules
- Extend `ViewModel()` from AndroidX
- Use `SavedStateHandle` for process death survival
- Expose immutable StateFlow using `.asStateFlow()`
- Use Unidirectional Data Flow pattern
- Keep thin - delegate to use cases
- Handle UI events in `onEvent()` methods
- Use appropriate dispatchers for different operations

#### Screen Rules
- Use Jetpack Compose for all UI
- Screen functions should be `@Composable`
- Collect state using `collectAsStateWithLifecycle()`
- Keep composables focused on UI rendering only
- Pass lambdas for user actions to ViewModel
- Use proper state hoisting

#### Component Rules
- Create reusable components in `ui/components/`
- Keep components small and focused
- Use parameters for customization, not hardcoded values
- Follow Compose best practices (no side effects, proper state management)

#### Navigation Rules
- Use Compose Navigation
- Define routes as constants or sealed classes
- Pass navigation callbacks to ViewModels
- Handle back stack properly

## Data Flow Patterns

### Data Creation Flow
1. UI collects input → creates `ItemRequest` DTO
2. ViewModel calls `addItem(request)`
3. Use case validates and processes
4. Repository maps to entity and inserts
5. DAO uses transaction to update related entities
6. Flow updates trigger UI recomposition

### Analytics/Computation Flow
1. ViewModel provides `Config`
2. ViewModel calls appropriate service method (e.g., `AnalyticsService.computeMetrics`)
3. Service filters data by config.filter
4. Service applies transformations using other services
5. Service aggregates and computes results
6. UI renders via appropriate component

### Filter Application Flow
1. User modifies filter in UI
2. ViewModel updates `filter` StateFlow
3. Use case observes filter changes
4. Repository builds SQL query or applies in-memory filtering
5. Results flow back to UI

## Dependency Rules

### Allowed Dependencies by Layer

**Data Layer can depend on:**
- Domain layer (interfaces, models)
- Android/Room libraries
- Network libraries (Retrofit, etc.)
- Kotlin stdlib and coroutines

**Domain Layer can depend on:**
- Kotlin stdlib
- Kotlin coroutines
- Java BigDecimal
- NOTHING else (especially no Android)

**UI Layer can depend on:**
- Domain layer (models, use cases, repository interfaces)
- AndroidX libraries (Compose, Navigation, ViewModel, etc.)
- Kotlin stdlib and coroutines

### Dependency Injection
- Use constructor injection exclusively
- Define dependencies in DI module (Hilt/Koin)
- Scope repositories and DAOs as singletons
- Scope ViewModels to navigation or activity lifecycle
- Never use `object` or static access for dependencies

## Common Patterns

### Repository Pattern
```kotlin
// Domain layer
interface ItemRepository {
    fun getItems(params: GetParams): Flow<List<Item>>
    suspend fun saveItem(item: Item)
}

// Data layer
class ItemRepositoryImpl(
    private val itemDao: ItemDao
) : ItemRepository {
    override fun getItems(params: GetParams): Flow<List<Item>> {
        return itemDao.getAllItems(params).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
```

### Use Case Pattern
```kotlin
class GetItemsUseCase(
    private val repository: ItemRepository
) {
    operator fun invoke(params: GetParams, filter: Filter): Flow<List<Item>> {
        return repository.getFilteredItems(params, filter)
    }
}
```

### ViewModel Pattern
```kotlin
class ItemViewModel(
    private val getItems: GetItemsUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItemUiState())
    val uiState: StateFlow<ItemUiState> = _uiState.asStateFlow()
    
    fun onEvent(event: ItemEvent) {
        when (event) {
            is ItemEvent.LoadItems -> loadItems()
        }
    }
    
    private fun loadItems() {
        viewModelScope.launch {
            getItems(params, filter).collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }
}
```

## Architecture Violations to Detect

### Critical Violations
- Android imports in domain layer
- Room entities exposed to UI layer
- Mutable StateFlow exposed from ViewModel
- Context passed to repository
- Business logic in Composable functions
- Blocking operations on main thread

### Warning Violations
- Use cases with multiple responsibilities
- ViewModels with excessive business logic
- Direct database access from UI layer
- Hardcoded strings or magic numbers
- Missing process death handling

## File Organization

### Package Structure
```
com.example.app/
├── data/
│   ├── local/
│   │   ├── entity/        # Room entities
│   │   ├── dao/           # Room DAOs
│   │   └── database/      # Room database setup
│   ├── remote/            # API interfaces (if applicable)
│   └── repository/        # Repository implementations
├── domain/
│   ├── model/             # Domain models
│   ├── model/dto/         # Data transfer objects
│   ├── repository/        # Repository interfaces
│   ├── usecase/           # Use cases
│   └── service/           # Business logic services
└── ui/
    ├── viewmodel/         # ViewModels
    ├── screens/           # Screen composables
    ├── components/        # Reusable components
    ├── navigation/        # Navigation setup
    └── theme/             # Theme, colors, typography
```

## Testing Architecture

### Unit Tests
- Domain layer: Test use cases and services with fake repositories
- Data layer: Test repository implementations with in-memory database
- UI layer: Test ViewModels with fake use cases

### Integration Tests
- Test repository with real database
- Test use case chains
- Test complete flows (e.g., item creation, data processing)

### UI Tests
- Test Compose components
- Test screen navigation
- Test user interactions
