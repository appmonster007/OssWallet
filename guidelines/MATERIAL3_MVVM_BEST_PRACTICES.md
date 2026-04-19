# Material 3 Kotlin MVVM Android App - Best Practices

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [MVVM Pattern Implementation](#mvvm-pattern-implementation)
3. [Material 3 Design System](#material-3-design-system)
4. [Kotlin Coroutines & Flow](#kotlin-coroutines--flow)
5. [Dependency Injection](#dependency-injection)
6. [Testing Strategy](#testing-strategy)
7. [Performance Optimization](#performance-optimization)
8. [Security Best Practices](#security-best-practices)

---

## Architecture Overview

### Clean Architecture Layers
```
app/
  ui/          # View layer (Activities, Fragments, Composables)
  presentation/ # ViewModels and UI state
  domain/      # Business logic and use cases
  data/        # Data repositories and data sources
```

### Module Organization
- **app**: Main application module
- **core**: Shared utilities and base classes
- **data**: Data layer implementation
- **domain**: Business logic (pure Kotlin)
- **ui**: UI components and screens
- **feature**: Feature-specific modules

---

## MVVM Pattern Implementation

### ViewModel Best Practices

#### DO: Use StateFlow for UI State
```kotlin
class HomeViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            getTransactionsUseCase()
                .catch { exception -> _uiState.value = HomeUiState.Error(exception) }
                .collect { transactions -> 
                    _uiState.value = HomeUiState.Success(transactions)
                }
        }
    }
}
```

#### DO: Create Coroutines in ViewModel
```kotlin
// GOOD
fun refreshData() {
    viewModelScope.launch {
        _isLoading.value = true
        try {
            val result = repository.getData()
            _uiState.value = result.toUiState()
        } finally {
            _isLoading.value = false
        }
    }
}

// BAD - Don't expose suspend functions from ViewModel
suspend fun refreshData(): UiState {
    return repository.getData().toUiState()
}
```

#### DO: Expose Immutable Types
```kotlin
// GOOD
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// BAD
val uiState = MutableStateFlow<UiState>(UiState.Loading)
```

### Repository Pattern

#### DO: Expose Suspend Functions and Flows
```kotlin
interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: Long)
}
```

#### DO: Make Suspend Functions Main-Safe
```kotlin
class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TransactionRepository {
    
    override suspend fun insertTransaction(transaction: Transaction): Long {
        return withContext(ioDispatcher) {
            transactionDao.insert(transaction.toEntity())
        }
    }
}
```

---

## Material 3 Design System

### Theme Implementation

#### DO: Use Dynamic Color
```kotlin
@Composable
fun OssWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

#### DO: Use Material 3 Components
```kotlin
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(text)
    }
}
```

### Typography System

#### DO: Define Typography Scale
```kotlin
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    )
)
```

### Accessibility

#### DO: Support Dynamic Text Scaling
```kotlin
@Composable
fun AccessibleText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    Text(
        text = text,
        style = style,
        modifier = Modifier.semantics {
            this.text = AnnotatedString(text)
        }
    )
}
```

#### DO: Use High Contrast Colors
```kotlin
val ExtendedColors = Colors(
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)
```

---

## Kotlin Coroutines & Flow

### Dispatcher Injection

#### DO: Inject Dispatchers
```kotlin
class NewsRepository(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend fun loadNews() = withContext(defaultDispatcher) {
        // Implementation
    }
}
```

#### DO: Use viewModelScope
```kotlin
class MyViewModel : ViewModel() {
    fun fetchData() {
        viewModelScope.launch {
            // Automatically cancelled when ViewModel is cleared
            repository.getData()
        }
    }
}
```

### Flow Best Practices

#### DO: Use StateFlow for UI State
```kotlin
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()
```

#### DO: Handle Exceptions Properly
```kotlin
fun getData(): Flow<Result<Data>> {
    return flow {
        emit(repository.getData())
    }.catch { exception ->
        emit(Result.Error(exception))
    }
}
```

#### DO: Use collectAsStateWithLifecycle
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // UI implementation
}
```

---

## Dependency Injection

### Hilt Best Practices

#### DO: Use Constructor Injection
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel()
```

#### DO: Define Scopes Properly
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "oss_wallet.db"
        ).build()
    }
}
```

---

## Testing Strategy

### Unit Testing

#### DO: Test ViewModels
```kotlin
@Test
fun `when loadTransactions called, then update uiState to success`() = runTest {
    // Given
    val transactions = listOf(mockTransaction)
    whenever(getTransactionsUseCase()).thenReturn(flowOf(transactions))
    
    // When
    viewModel.loadTransactions()
    
    // Then
    assertEquals(HomeUiState.Success(transactions), viewModel.uiState.value)
}
```

#### DO: Use Test Dispatchers
```kotlin
@ExperimentalCoroutinesApi
class ViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

### UI Testing

#### DO: Use Compose Testing
```kotlin
@Test
fun `home screen displays transactions`() {
    composeTestRule.setContent {
        OssWalletTheme {
            HomeScreen()
        }
    }
    
    composeTestRule
        .onNodeWithText("Transaction 1")
        .assertIsDisplayed()
}
```

---

## Performance Optimization

### Memory Management

#### DO: Use Lazy Loading
```kotlin
@Composable
fun TransactionList(transactions: List<Transaction>) {
    LazyColumn {
        items(transactions) { transaction ->
            TransactionItem(transaction)
        }
    }
}
```

#### DO: Remember Expensive Calculations
```kotlin
@Composable
fun ExpensiveComputation(data: List<Data>) {
    val result = remember(data) {
        data.map { expensiveCalculation(it) }
    }
    
    Text(result.toString())
}
```

### Database Optimization

#### DO: Use Paging 3 for Large Datasets
```kotlin
@Query("SELECT * FROM transactions ORDER BY timestamp DESC")
fun getTransactionsPaged(): PagingSource<Int, TransactionEntity>
```

#### DO: Optimize Room Queries
```kotlin
@Query("""
    SELECT t.*, a.name as account_name 
    FROM transactions t 
    LEFT JOIN accounts a ON t.account_id = a.id 
    WHERE t.profile_id = :profileId 
    ORDER BY t.timestamp DESC
""")
fun getTransactionsWithAccounts(profileId: Int): Flow<List<TransactionWithAccount>>
```

---

## Security Best Practices

### Data Protection

#### DO: Use EncryptedSharedPreferences
```kotlin
val encryptedSharedPreferences = EncryptedSharedPreferences.create(
    "secure_prefs",
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

#### DO: Validate Input Data
```kotlin
fun validateTransactionInput(amount: String, description: String): ValidationResult {
    return when {
        amount.isBlank() -> ValidationResult.Error("Amount cannot be empty")
        amount.toDoubleOrNull() == null -> ValidationResult.Error("Invalid amount")
        description.length > 100 -> ValidationResult.Error("Description too long")
        else -> ValidationResult.Success
    }
}
```

### Network Security

#### DO: Use HTTPS with Certificate Pinning
```kotlin
val certificatePinner = CertificatePinner.Builder()
    .add("api.osscabinet.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .build()

val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build()
```

---

## Code Quality

### Naming Conventions

#### DO: Use Clear, Descriptive Names
```kotlin
// GOOD
class GetTransactionsByDateRangeUseCase
class TransactionRepositoryImpl
private val _uiState = MutableStateFlow<HomeUiState>

// BAD
class GtdbrUseCase
class TRImpl
private val _state = MutableStateFlow<HomeUiState>
```

### Documentation

#### DO: Document Public APIs
```kotlin
/**
 * Retrieves all transactions for a given profile within the specified date range.
 * 
 * @param profileId The ID of the profile to fetch transactions for
 * @param startDate The start date of the range (inclusive)
 * @param endDate The end date of the range (inclusive)
 * @return Flow of transactions sorted by timestamp in descending order
 */
fun getTransactionsByDateRange(
    profileId: Int,
    startDate: Long,
    endDate: Long
): Flow<List<Transaction>>
```

---

## Build Configuration

### Gradle Best Practices

#### DO: Use Version Catalogs
```kotlin
// libs.versions.toml
[versions]
compose-bom = "2024.02.00"
hilt = "2.48"
room = "2.6.1"

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
```

#### DO: Enable Build Optimizations
```kotlin
android {
    buildFeatures {
        compose = true
        buildConfig = false
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
}
```

---

## Summary

Following these best practices will help you build:
- **Maintainable** code with clear separation of concerns
- **Testable** components with proper dependency injection
- **Performant** apps with optimized resource usage
- **Accessible** UI following Material 3 guidelines
- **Secure** applications with proper data protection

Remember that these guidelines are not rigid rules but recommendations that should be adapted to your specific project requirements and team preferences.
