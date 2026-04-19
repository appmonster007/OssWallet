# Material 3 Kotlin MVVM Android App - Bad Practices to Avoid

## Table of Contents
1. [Architecture Anti-Patterns](#architecture-anti-patterns)
2. [MVVM Implementation Mistakes](#mvvm-implementation-mistakes)
3. [Material 3 Design System Misuse](#material-3-design-system-misuse)
4. [Coroutines & Flow Pitfalls](#coroutines--flow-pitfalls)
5. [Dependency Injection Anti-Patterns](#dependency-injection-anti-patterns)
6. [Testing Anti-Patterns](#testing-anti-patterns)
7. [Performance Killers](#performance-killers)
8. [Security Vulnerabilities](#security-vulnerabilities)

---

## Architecture Anti-Patterns

### God Classes

#### AVOID: Putting Everything in One Class
```kotlin
// BAD - God ViewModel doing everything
class HomeViewModel : ViewModel() {
    // Database operations
    private val db = Room.databaseBuilder(context, AppDatabase::class.java, "db").build()
    
    // Network operations
    private val api = Retrofit.Builder().baseUrl("https://api.example.com").build()
    
    // Business logic
    private val calculator = TransactionCalculator()
    
    // UI state management
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState
    
    // All mixed together
    fun saveTransaction(amount: Double, category: String) {
        viewModelScope.launch {
            // Direct database access
            val transaction = TransactionEntity(amount = amount, category = category)
            db.transactionDao().insert(transaction)
            
            // Direct network call
            api.syncTransaction(transaction)
            
            // Business logic mixed with UI
            val total = calculator.calculateTotal(db.transactionDao().getAll())
            _uiState.value = UiState.Success(total)
        }
    }
}
```

### Tight Coupling

#### AVOID: Direct Dependencies Between Layers
```kotlin
// BAD - ViewModel directly accessing database
class TransactionViewModel : ViewModel() {
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "db").build()
    
    fun getTransactions() {
        // Direct database access - should use repository
        database.transactionDao().getAll().observeForever { transactions ->
            // Handle data directly
        }
    }
}
```

### Missing Abstraction

#### AVOID: Concrete Types in Dependencies
```kotlin
// BAD - Depending on concrete implementation
class TransactionViewModel(
    private val repository: TransactionRepositoryImpl // Should be interface
) : ViewModel()

// GOOD
class TransactionViewModel(
    private val repository: TransactionRepository // Interface
) : ViewModel()
```

---

## MVVM Implementation Mistakes

### ViewModel Anti-Patterns

#### AVOID: Exposing Mutable Types
```kotlin
// BAD - Exposing mutable state
class HomeViewModel : ViewModel() {
    val uiState = MutableStateFlow<UiState>(UiState.Loading)
    
    // Anyone can modify this from outside
}

// GOOD
class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```

#### AVOID: Long-Running Operations in ViewModel
```kotlin
// BAD - Heavy operations in ViewModel
class HomeViewModel : ViewModel() {
    fun processData() {
        viewModelScope.launch {
            // Heavy computation should be in use case
            val result = complexCalculation(data)
            _uiState.value = result
        }
    }
}

// GOOD
class HomeViewModel(
    private val processDataUseCase: ProcessDataUseCase
) : ViewModel() {
    fun processData() {
        viewModelScope.launch {
            val result = processDataUseCase(data)
            _uiState.value = result
        }
    }
}
```

#### AVOID: Context in ViewModel
```kotlin
// BAD - Using Context in ViewModel
class HomeViewModel : ViewModel() {
    private val context = getApplication<Application>().applicationContext
    
    fun saveData() {
        // Don't use Context in ViewModel
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    }
}
```

### Repository Anti-Patterns

#### AVOID: Blocking Operations in Repository
```kotlin
// BAD - Blocking operations without proper dispatcher
class TransactionRepositoryImpl : TransactionRepository {
    override suspend fun getTransactions(): List<Transaction> {
        // This blocks the calling thread
        return database.transactionDao().getAllSync() // Blocking call
    }
}
```

#### AVOID: Multiple Responsibilities
```kotlin
// BAD - Repository doing too much
class TransactionRepositoryImpl : TransactionRepository {
    override suspend fun getTransactions(): List<Transaction> {
        // Network calls
        val remoteData = api.getTransactions()
        
        // Database operations
        database.transactionDao().insertAll(remoteData)
        
        // Business logic
        val filtered = remoteData.filter { it.amount > 0 }
        
        // UI formatting
        return filtered.map { it.toUiModel() }
    }
}
```

---

## Material 3 Design System Misuse

### Theme Anti-Patterns

#### AVOID: Hardcoded Colors
```kotlin
// BAD - Hardcoded colors
@Composable
fun TransactionItem() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2196F3) // Hardcoded blue
        )
    ) {
        Text(
            text = "Transaction",
            color = Color.White // Hardcoded white
        )
    }
}

// GOOD - Use theme colors
@Composable
fun TransactionItem() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = "Transaction",
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
```

#### AVOID: Inconsistent Typography
```kotlin
// BAD - Random font sizes
@Composable
fun BadTypography() {
    Text(
        text = "Title",
        fontSize = 24.sp, // Random size
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Body",
        fontSize = 14.sp, // Random size
        fontWeight = FontWeight.Normal
    )
}

// GOOD - Use typography system
@Composable
fun GoodTypography() {
    Text(
        text = "Title",
        style = MaterialTheme.typography.headlineSmall
    )
    Text(
        text = "Body",
        style = MaterialTheme.typography.bodyMedium
    )
}
```

### Layout Anti-Patterns

#### AVOID: Fixed Dimensions
```kotlin
// BAD - Fixed dimensions
@Composable
fun FixedLayout() {
    Column(
        modifier = Modifier
            .width(360.dp) // Fixed width
            .height(640.dp) // Fixed height
    ) {
        Text("Content")
    }
}

// GOOD - Responsive layout
@Composable
fun ResponsiveLayout() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Text("Content")
    }
}
```

#### AVOID: Deep Nesting
```kotlin
// BAD - Deeply nested layout
@Composable
fun DeepNesting() {
    Column {
        Row {
            Column {
                Row {
                    Column {
                        Text("Deeply nested")
                    }
                }
            }
        }
    }
}

// GOOD - Flatten hierarchy
@Composable
fun FlatHierarchy() {
    Column {
        Text("Deeply nested")
    }
}
```

---

## Coroutines & Flow Pitfalls

### Dispatcher Misuse

#### AVOID: Hardcoded Dispatchers
```kotlin
// BAD - Hardcoded dispatchers
class NewsRepository {
    suspend fun fetchNews() = withContext(Dispatchers.IO) {
        // Implementation
    }
}

// GOOD - Inject dispatchers
class NewsRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun fetchNews() = withContext(ioDispatcher) {
        // Implementation
    }
}
```

#### AVOID: GlobalScope
```kotlin
// BAD - Using GlobalScope
class DataManager {
    fun syncData() {
        GlobalScope.launch {
            // This survives beyond the app's lifecycle
            repository.syncData()
        }
    }
}

// GOOD - Use appropriate scope
class DataManager {
    fun syncData() {
        viewModelScope.launch {
            // Properly scoped to ViewModel
            repository.syncData()
        }
    }
}
```

### Flow Anti-Patterns

#### AVOID: Blocking Operations in Flow
```kotlin
// BAD - Blocking operations in flow
fun getTransactions(): Flow<List<Transaction>> {
    return flow {
        // This blocks the flow collector
        val data = database.transactionDao().getAllSync()
        emit(data)
    }
}

// GOOD - Use proper suspend functions
fun getTransactions(): Flow<List<Transaction>> {
    return database.transactionDao().getAllFlow()
}
```

#### AVOID: Exception Swallowing
```kotlin
// BAD - Swallowing exceptions
fun getData(): Flow<Data> {
    return flow {
        try {
            emit(repository.getData())
        } catch (e: Exception) {
            // Swallowing exception - no error handling
        }
    }
}

// GOOD - Proper error handling
fun getData(): Flow<Result<Data>> {
    return flow {
        emit(Result.Success(repository.getData()))
    }.catch { exception ->
        emit(Result.Error(exception))
    }
}
```

---

## Dependency Injection Anti-Patterns

### Service Locator Pattern

#### AVOID: Manual Dependency Resolution
```kotlin
// BAD - Service locator
class HomeViewModel : ViewModel() {
    private val repository = ServiceLocator.getTransactionRepository()
    private val useCase = ServiceLocator.getGetTransactionsUseCase()
    
    fun loadData() {
        // Using manually resolved dependencies
    }
}
```

### Constructor Injection Violations

#### AVOID: Field Injection
```kotlin
// BAD - Field injection
class HomeViewModel : ViewModel() {
    @Inject lateinit var repository: TransactionRepository
    @Inject lateinit var useCase: GetTransactionsUseCase
    
    init {
        // Injection happens after constructor
        // Can lead to NPE if not properly initialized
    }
}

// GOOD - Constructor injection
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val useCase: GetTransactionsUseCase
) : ViewModel()
```

### Over-Engineering

#### AVOID: Unnecessary Abstractions
```kotlin
// BAD - Over-engineered
interface TransactionProvider {
    fun provideTransactions(): TransactionProvider
}

interface TransactionFetcher {
    fun fetchTransactions(): List<Transaction>
}

interface TransactionRepository : TransactionProvider, TransactionFetcher

// For simple cases, this is unnecessary complexity
```

---

## Testing Anti-Patterns

### Test Implementation Issues

#### AVOID: Testing Implementation Details
```kotlin
// BAD - Testing private methods
@Test
fun `test private method`() {
    val viewModel = HomeViewModel()
    // Using reflection to test private method
    val method = viewModel::class.java.getDeclaredMethod("privateMethod")
    method.isAccessible = true
    method.invoke(viewModel)
}

// GOOD - Test public behavior
@Test
fun `when load data called, then ui state updates`() {
    val viewModel = HomeViewModel(mockUseCase)
    viewModel.loadData()
    assertEquals(UiState.Success, viewModel.uiState.value)
}
```

#### AVOID: Mock Overuse
```kotlin
// BAD - Mocking everything
@Test
fun `test with too many mocks`() {
    val mockRepository = mock<TransactionRepository>()
    val mockUseCase = mock<GetTransactionsUseCase>()
    val mockDispatcher = mock<CoroutineDispatcher>()
    val mockLogger = mock<Logger>()
    
    // Too many mocks make tests brittle
}

// GOOD - Use real implementations where possible
@Test
fun `test with real implementations`() {
    val repository = FakeTransactionRepository() // Real but simple implementation
    val useCase = GetTransactionsUseCase(repository)
    
    // More realistic test
}
```

### UI Testing Issues

#### AVOID: Implementation-Specific Tests
```kotlin
// BAD - Testing implementation details
@Test
fun `test specific Compose node`() {
    composeTestRule
        .onNodeWithTag("transaction_item_123") // Implementation-specific
        .assertIsDisplayed()
}

// GOOD - Test user-visible behavior
@Test
fun `test transaction item displays amount`() {
    composeTestRule
        .onNodeWithText("$100.00") // User-visible text
        .assertIsDisplayed()
}
```

---

## Performance Killers

### Memory Leaks

#### AVOID: Leaking Context
```kotlin
// BAD - Leaking context in singleton
object DatabaseManager {
    private lateinit var context: Context
    
    fun init(context: Context) {
        this.context = context // Leaks context
        database = Room.databaseBuilder(context, AppDatabase::class.java, "db").build()
    }
}

// GOOD - Use application context
object DatabaseManager {
    private lateinit var application: Application
    
    fun init(application: Application) {
        this.application = application // Safe to hold
        database = Room.databaseBuilder(application, AppDatabase::class.java, "db").build()
    }
}
```

#### AVOID: Unbounded Flows
```kotlin
// BAD - Collecting flow without proper lifecycle
class HomeFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // This will leak the fragment
        viewModel.uiState.collect { state ->
            updateUI(state)
        }
    }
}

// GOOD - Use lifecycle-aware collection
class HomeFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }
}
```

### Inefficient Compose Usage

#### AVOID: Unnecessary Recomposition
```kotlin
// BAD - Unstable parameters cause recomposition
@Composable
fun TransactionItem(transaction: Transaction) {
    // Transaction is data class but contains mutable list
    val total = remember(transaction.items) {
        transaction.items.sumOf { it.amount }
    }
    
    Text("Total: $total")
}

// GOOD - Use stable parameters
@Composable
fun TransactionItem(transaction: Transaction) {
    val total = remember {
        transaction.items.sumOf { it.amount }
    }
    
    Text("Total: $total")
}
```

#### AVOID: Heavy Operations in Composable
```kotlin
// BAD - Heavy operations in composable
@Composable
fun TransactionList(transactions: List<Transaction>) {
    val processedTransactions = remember {
        // Heavy computation in composable
        transactions.map { 
            complexCalculation(it) 
        }
    }
    
    LazyColumn {
        items(processedTransactions) { transaction ->
            TransactionItem(transaction)
        }
    }
}

// GOOD - Pre-process in ViewModel
@Composable
fun TransactionList(transactions: List<Transaction>) {
    // Data already processed in ViewModel
    LazyColumn {
        items(transactions) { transaction ->
            TransactionItem(transaction)
        }
    }
}
```

---

## Security Vulnerabilities

### Data Exposure

#### AVOID: Sensitive Data in Logs
```kotlin
// BAD - Logging sensitive data
class LoginViewModel : ViewModel() {
    fun login(username: String, password: String) {
        Log.d("Login", "Attempting login for $username with password $password")
        // Password exposed in logs
    }
}

// GOOD - Secure logging
class LoginViewModel : ViewModel() {
    fun login(username: String, password: String) {
        Log.d("Login", "Attempting login for $username")
        // Password not logged
    }
}
```

#### AVOID: Weak Encryption
```kotlin
// BAD - Weak encryption
class SecureStorage {
    private val key = "mysecretkey123" // Hardcoded weak key
    
    fun encrypt(data: String): String {
        // Using weak encryption algorithm
        return data.toByteArray().toString()
    }
}

// GOOD - Use Android Keystore
class SecureStorage {
    private val keyAlias = "my_secure_key"
    
    fun encrypt(data: String): String {
        // Use Android Keystore for proper encryption
        return encryptWithKeystore(data, keyAlias)
    }
}
```

### Network Security

#### AVOID: Insecure Network Configuration
```kotlin
// BAD - Accepting all certificates
val okHttpClient = OkHttpClient.Builder()
    .hostnameVerifier { _, _ -> true } // Accept all hosts
    .sslSocketFactory(
        SSLContext.getInstance("TLS").socketFactory,
        TrustAllManager() // Accept all certificates
    )
    .build()

// GOOD - Proper certificate validation
val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(
        CertificatePinner.Builder()
            .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()
    )
    .build()
```

---

## Code Quality Issues

### Naming Anti-Patterns

#### AVOID: Unclear Names
```kotlin
// BAD - Unclear naming
class D {
    fun p(d: List<T>): List<R> {
        return d.map { t ->
            val r = c(t)
            r
        }
    }
}

// GOOD - Clear naming
class DataProcessor {
    fun processData(transactions: List<Transaction>): List<Result> {
        return transactions.map { transaction ->
            val result = calculateResult(transaction)
            result
        }
    }
}
```

### Documentation Anti-Patterns

#### AVOID: Useless Comments
```kotlin
// BAD - Useless comments
// Get transactions
fun getTransactions(): List<Transaction> {
    // Return transactions
    return repository.getTransactions()
}

// GOOD - Meaningful documentation
/**
 * Retrieves all transactions for the current user.
 * 
 * @return List of transactions sorted by date in descending order
 * @throws NetworkException if unable to connect to server
 */
fun getTransactions(): List<Transaction> {
    return repository.getTransactions()
}
```

---

## Build Configuration Anti-Patterns

### Gradle Issues

#### AVOID: Version Inconsistencies
```kotlin
// BAD - Inconsistent versions
dependencies {
    implementation "androidx.compose.ui:ui:1.4.0"
    implementation "androidx.compose.material:material:1.3.0"
    implementation "androidx.compose.ui:ui-tooling:1.5.0"
}

// GOOD - Use BOM for consistent versions
dependencies {
    implementation platform("androidx.compose:compose-bom:2024.02.00")
    implementation "androidx.compose.ui:ui"
    implementation "androidx.compose.material:material"
    implementation "androidx.compose.ui:ui-tooling"
}
```

#### AVOID: ProGuard Configuration Issues
```kotlin
// BAD - Too aggressive ProGuard rules
-keep class com.example.** { *; }
-keepnames class com.example.** { *; }

// GOOD - Specific, necessary rules only
-keep class com.example.data.model.** { *; }
-keep class com.example.data.repository.** { *; }
```

---

## Summary

Avoiding these bad practices will help you prevent:
- **Maintenance nightmares** from tightly coupled code
- **Performance issues** from inefficient patterns
- **Security vulnerabilities** from poor data handling
- **Testing difficulties** from anti-patterns
- **Code quality degradation** from poor practices

Remember that the key is to write code that is:
- **Readable** and maintainable by other developers
- **Testable** with proper separation of concerns
- **Performant** with efficient resource usage
- **Secure** with proper data protection
- **Scalable** for future growth

Regular code reviews and adherence to these guidelines will help maintain high code quality throughout the project lifecycle.
