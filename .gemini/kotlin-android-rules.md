# Kotlin & Android Coding Standards for Android

## Kotlin Language Standards

### Naming Conventions

**Classes/Interfaces:**
- PascalCase: `ItemRepository`, `ItemViewModel`, `DataService`

**Functions/Properties:**
- camelCase: `getItems()`, `insertItem()`, `calculateTotals`

**Constants:**
- UPPER_SNAKE_CASE or PascalCase for object properties:
```kotlin
const val DEFAULT_VALUE = "default"
object Constants {
    val MAX_ITEMS = 100
}
```

**Private properties:**
- Prefix with underscore for backing properties: `_uiState`, `_transactions`

### Code Style

**Data Classes:**
- Use `data class` for models that need `equals()`, `hashCode()`, `copy()`
- Use `value class` for wrapping single values (when needed for performance)
- Keep data classes immutable (`val` only)

**Sealed Classes:**
- Use for restricted class hierarchies
- Example: `Filter`, `Range`, `ItemType`
- Enable exhaustive `when` expressions

**Extension Functions:**
- Use for utility functions that extend existing classes
- Place in files named like `*.extensions.kt`
- Example: `BigDecimal.toFormattedString()`

**Null Safety:**
- Avoid nullable types when possible
- Use `?` only when null is a valid state
- Use `!!` sparingly and only when you're certain
- Prefer `requireNotNull()` or `checkNotNull()` for validation

### Coroutines & Flow

**Dispatchers:**
- `Dispatchers.IO` for database and network operations
- `Dispatchers.Default` for CPU-intensive calculations
- `Dispatchers.Main.immediate` for UI updates in ViewModels
- Never use `Dispatchers.Main` for blocking operations

**Flow Collection:**
- Use `collectAsStateWithLifecycle()` in Compose
- Use `viewModelScope.launch` for collecting in ViewModels
- Use `flow { }` builder for creating flows
- Use operators like `map`, `filter`, `combine` for transformations

**StateFlow vs SharedFlow:**
- Use `StateFlow` for single source of truth state
- Use `SharedFlow` for events (one-time occurrences)
- Always expose `StateFlow` as immutable using `.asStateFlow()`

**Coroutine Scope:**
- Use `viewModelScope` in ViewModels (auto-cancels on ViewModel clear)
- Use `lifecycleScope` in Activities/Fragments (auto-cancels on lifecycle)
- Use `CoroutineScope(Dispatchers.IO)` for background work

### Collections

**List vs Sequence:**
- Use `List` for small collections or when you need random access
- Use `Sequence` for large collections or chained operations to avoid intermediate allocations

**Immutable Collections:**
- Prefer `List`, `Set`, `Map` over mutable variants
- Use `toList()`, `toSet()`, `toMap()` to create immutable copies
- Use `buildList`, `buildSet` for building mutable collections then converting

**Collection Operations:**
- Use functional operators: `map`, `filter`, `flatMap`, `reduce`, `fold`
- Use `associateBy`, `groupBy` for transformations
- Use `distinct`, `distinctBy` for removing duplicates

## Android-Specific Standards

### Jetpack Compose

**Composable Functions:**
- Prefix with `@Composable`
- Use PascalCase for composable names
- Keep functions small and focused
- Avoid side effects in composable bodies

**State Management:**
- Use `remember` for local state
- Use `rememberSaveable` for state that survives process death
- Use `mutableStateOf` for mutable state
- Use `derivedStateOf` for computed state
- Hoist state to the lowest common ancestor

**Side Effects:**
- Use `LaunchedEffect` for one-time effects
- Use `rememberCoroutineScope` for coroutine scopes in composables
- Use `SideEffect` for effects that need to execute on every recomposition
- Use `DisposableEffect` for cleanup

**Modifiers:**
- Chain modifiers in order of specificity
- Use standard modifiers before custom ones
- Example: `Modifier.fillMaxWidth().padding(16.dp).clickable { }`

**Preview:**
- Use `@Preview` annotation for composable previews
- Create multiple previews for different states
- Use `@PreviewParameter` for parameterized previews

### Room Database

**Entity Definitions:**
- Use `@Entity` with `tableName` for custom table names
- Use `@PrimaryKey` with `autoGenerate = true` for auto-incrementing IDs
- Use `@ColumnInfo` for custom column names
- Use `@Ignore` for fields that shouldn't be stored
- Use `@Embedded` for nested objects
- Use `@Relation` for relationships

**Type Converters:**
- Create converters for non-standard types (BigDecimal, Date, enums)
- Use `@TypeConverters` annotation on entities, DAOs, or database
- Example: `BigDecimalConverter`, `RangeConverter`

**DAO Queries:**
- Use `@Query` with SQL for complex queries
- Use parameter binding: `:paramName`
- Use `@Transaction` for multi-table operations
- Return `Flow` for reactive queries
- Use `suspend` for insert/update/delete operations

**Database Migration:**
- Use `Migration` class for schema changes
- Test migrations with `MigrationTestHelper`
- Use fallback to destructive migration only in debug builds

### Dependency Injection (Hilt)

**Module Setup:**
- Create modules in `di/` package
- Use `@Module` and `@InstallIn` annotations
- Use `@Provides` for providing dependencies
- Use `@Binds` for interface implementations

**Scoping:**
- Use `@Singleton` for application-wide dependencies
- Use `@ViewModelScoped` for ViewModels
- Use `@ActivityScoped` for Activity-specific dependencies
- Use custom scopes for feature-specific dependencies

**Injection Points:**
- Use `@Inject constructor` for class injection
- Use `@AndroidEntryPoint` for Activities, Fragments, ViewModels
- Use `@HiltViewModel` for ViewModels
- Use `@Inject` for field injection (rarely needed)

### ViewModels

**Lifecycle:**
- Extend `ViewModel()` from AndroidX
- Use `viewModelScope` for coroutines (auto-cancels)
- Use `SavedStateHandle` for process death survival
- Clean up resources in `onCleared()` override

**State Management:**
- Use `MutableStateFlow` for mutable state
- Expose as immutable `StateFlow` using `.asStateFlow()`
- Use single `UiState` data class when possible
- Use `update { }` extension for updating immutable state

**Event Handling:**
- Use sealed classes for events
- Handle events in `onEvent()` method
- Use `SharedFlow` for one-time events (navigation, toasts)

### Navigation

**Compose Navigation:**
- Use `NavHost` with `navController`
- Define routes as constants or sealed classes
- Use `navController.navigate()` for navigation
- Use `navController.popBackStack()` for back navigation
- Pass arguments as part of route or in `NavOptions`

**Type-Safe Navigation:**
- Consider using type-safe navigation libraries
- Define navigation models for type safety
- Use sealed classes for route definitions

## Performance Standards

### Memory Management

**Avoid Memory Leaks:**
- Never hold references to Activities/Contexts in singletons
- Use weak references when necessary
- Clear references in `onCleared()` or lifecycle callbacks
- Use `viewLifecycleOwner` in Fragments instead of `this`

**Object Allocation:**
- Reuse objects when possible (e.g., Paint, Path)
- Use object pools for frequently created objects
- Avoid object allocation in hot paths (draw loops, scroll)

**Lazy Initialization:**
- Use `by lazy` for deferred initialization
- Use `lateinit` for properties that will be initialized before use
- Consider `lazy` for expensive computations

### Database Performance

**Query Optimization:**
- Use indexes on frequently queried columns
- Avoid `SELECT *` - select only needed columns
- Use `@Query` with proper WHERE clauses
- Consider pagination for large datasets

**Transaction Usage:**
- Use `@Transaction` for multi-table operations
- Batch inserts/updates when possible
- Avoid nested transactions

### UI Performance

**Compose Performance:**
- Use `remember` to avoid recomputation
- Use `key()` in lists to stabilize item identity
- Avoid expensive operations in composable bodies
- Use `derivedStateOf` for computed state that depends on multiple states

**RecyclerView/Lists:**
- Use proper `DiffUtil` implementations
- Implement view holders efficiently
- Avoid complex layouts in list items

## Testing Standards

### Unit Tests

**Structure:**
- Use Given-When-Then pattern in test names
- Use descriptive test names
- Arrange-Act-Assert pattern in test bodies
- Use `@Before` and `@After` for setup/teardown

**Mocking:**
- Use MockK for mocking
- Mock external dependencies (repositories, services)
- Don't mock data classes or simple objects
- Test behavior, not implementation details

**Coroutines Testing:**
- Use `runTest` for coroutine tests
- Use `TestDispatcher` for controlling coroutine execution
- Use `advanceUntilIdle()` to wait for async operations

### Instrumented Tests

**Room Testing:**
- Use `inMemoryDatabaseBuilder` for fast tests
- Use `MigrationTestHelper` for migration tests
- Test DAO methods independently

**UI Testing:**
- Use Compose Testing for UI tests
- Use `composeTestRule` for test assertions
- Test user interactions and state changes
- Use semantic tags for component identification

## Security Standards

### Data Security

**Encryption:**
- Use EncryptedSharedPreferences for sensitive data
- Use EncryptedFile for sensitive files
- Use Android Keystore for cryptographic operations

**Network Security:**
- Use HTTPS for all network calls
- Use certificate pinning for critical APIs
- Validate and sanitize all inputs

### Permission Handling

**Runtime Permissions:**
- Request permissions at runtime
- Handle permission denials gracefully
- Explain why permissions are needed
- Use ActivityResultContracts for permissions

## Code Quality

### Code Organization

**File Length:**
- Keep files under 300 lines when possible
- Split large classes into multiple files if needed
- Use `private` functions to reduce complexity

**Function Length:**
- Keep functions under 50 lines
- Extract complex logic into separate functions
- Use descriptive function names

**Complexity:**
- Avoid deep nesting (max 3-4 levels)
- Use early returns to reduce nesting
- Extract complex conditions to named functions

### Documentation

**KDoc:**
- Document public APIs with KDoc
- Use `@param` for parameters
- Use `@return` for return values
- Use `@throws` for exceptions

**Comments:**
- Use comments to explain WHY, not WHAT
- Avoid obvious comments
- Keep comments up to date with code

### Error Handling

**Exceptions:**
- Use specific exception types
- Catch exceptions at appropriate levels
- Never catch `Exception` or `Throwable` broadly
- Use `Result` type for operations that can fail

**Logging:**
- Use Timber for logging
- Use appropriate log levels (DEBUG, INFO, WARN, ERROR)
- Don't log sensitive information
- Remove or disable debug logs in production

## Build Configuration

### Gradle

**Dependencies:**
- Use version catalog (libs.versions.toml)
- Keep dependencies up to date
- Use stable versions in production
- Document why specific versions are pinned

**Build Variants:**
- Use debug and release variants
- Configure ProGuard/R8 for release builds
- Use different configurations for different variants

**Code Quality Tools:**
- Enable detekt for static analysis
- Enable ktlint for code formatting
- Configure Android Lint checks
- Use strict compilation checks
