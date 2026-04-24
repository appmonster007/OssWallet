# Android Widget Development - Bad Practices to Avoid

## Table of Contents
1. [Architecture Anti-Patterns](#architecture-anti-patterns)
2. [Glance Framework Misuse](#glance-framework-misuse)
3. [Performance Killers](#performance-killers)
4. [Interactive Widget Mistakes](#interactive-widget-mistakes)
5. [Configuration Anti-Patterns](#configuration-anti-patterns)
6. [Data Management Pitfalls](#data-management-pitfalls)
7. [Testing Anti-Patterns](#testing-anti-patterns)
8. [Accessibility Violations](#accessibility-violations)

---

## Architecture Anti-Patterns

### Monolithic Widget Classes

#### AVOID: Putting Everything in One Class
```kotlin
// BAD - Monolithic widget class
class NetWorthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = object : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) {
            // Direct database access
            val database = Room.databaseBuilder(context, AppDatabase::class.java, "db").build()
            val transactions = database.transactionDao().getAll()
            
            // Direct network calls
            val api = Retrofit.Builder().baseUrl("https://api.example.com").build()
            val remoteData = api.getNetWorth()
            
            // Business logic mixed with UI
            val total = transactions.sumOf { it.amount } + remoteData.amount
            
            // UI rendering
            provideContent {
                Column {
                    Text("Net Worth: $total")
                    Button("Refresh") {
                        // Direct API call in UI
                        api.refreshData()
                    }
                }
            }
        }
    }
}
```

#### AVOID: Tight Coupling to App Components
```kotlin
// BAD - Direct dependencies on app components
class BalanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Directly accessing MainActivity
        val mainActivity = context as MainActivity
        
        // Directly accessing ViewModel
        val viewModel = ViewModelProvider(mainActivity)[HomeViewModel::class.java]
        
        // Directly accessing SharedPreferences from MainActivity
        val prefs = mainActivity.getSharedPreferences("main_prefs", Context.MODE_PRIVATE)
        
        provideContent {
            Text("Balance: ${viewModel.balance.value}")
        }
    }
}
```

### Missing Separation of Concerns

#### AVOID: Business Logic in Widget UI
```kotlin
// BAD - Business logic mixed with UI
class TransactionWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val transactions = remember {
                // Heavy computation in UI thread
                val allTransactions = repository.getAllTransactions()
                val filtered = allTransactions.filter { it.amount > 0 }
                val grouped = filtered.groupBy { it.category }
                val summed = grouped.mapValues { it.value.sumOf { t -> t.amount } }
                val sorted = summed.toList().sortedByDescending { it.second }
                sorted.take(5)
            }
            
            LazyColumn {
                items(transactions) { (category, amount) ->
                    Text("$category: $amount")
                }
            }
        }
    }
}
```

---

## Glance Framework Misuse

### State Management Anti-Patterns

#### AVOID: Mutable Shared State
```kotlin
// BAD - Shared mutable state across widget instances
object WidgetState {
    var netWorth: Double = 0.0
    var currency: String = "USD"
    var isLoading: Boolean = false
}

class NetWorthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // All widgets share the same state
            Text("Net Worth: ${WidgetState.netWorth}")
            
            Button("Refresh") {
                // Mutating shared state
                WidgetState.isLoading = true
                // Update logic
                WidgetState.isLoading = false
            }
        }
    }
}
```

#### AVOID: Inefficient State Updates
```kotlin
// BAD - Updating entire state for small changes
class WidgetStateManager {
    suspend fun updateBalance(newBalance: Double) {
        // Updating entire preferences for one value
        GlanceAppWidgetManager(context)
            .getGlanceAppWidget(BalanceWidget::class.java)
            .update(widgetId) {
                PreferencesMutable {
                    this[balanceKey] = newBalance.toString()
                    this[currencyKey] = "USD"
                    this[lastUpdateKey] = System.currentTimeMillis().toString()
                    this[themeKey] = "light"
                    this[showDetailsKey] = "true"
                    // ... updating all keys even if unchanged
                }
            }
    }
}
```

### Layout Anti-Patterns

#### AVOID: Fixed Dimensions Without Responsiveness
```kotlin
// BAD - Fixed dimensions that don't adapt
@Composable
fun FixedSizeWidget() {
    Column(
        modifier = GlanceModifier
            .size(160.dp, 80.dp) // Fixed size
            .background(Color.Blue)
    ) {
        Text("Net Worth")
        Text("$12,345.67") // Might overflow on smaller screens
        Text("+$234.56")  // Might not fit on compact widgets
    }
}
```

#### AVOID: Deep Nesting in Glance
```kotlin
// BAD - Excessive nesting causing performance issues
@Composable
fun DeeplyNestedWidget() {
    Column {
        Row {
            Column {
                Row {
                    Column {
                        Row {
                            Column {
                                Text("Deeply nested")
                                Text("Performance killer")
                            }
                        }
                    }
                }
            }
        }
        Row {
            Column {
                Row {
                    Column {
                        Text("More nesting")
                    }
                }
            }
        }
    }
}
```

---

## Performance Killers

### Inefficient Data Loading

#### AVOID: Blocking Operations in Widget Thread
```kotlin
// BAD - Blocking operations in widget rendering
class SlowWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // This blocks the widget rendering thread
        val networkData = withContext(Dispatchers.IO) {
            // Long-running network call
            val response = api.getTransactions().execute()
            response.body()?.transactions ?: emptyList()
        }
        
        // Heavy computation on main thread
        val processedData = networkData.map { transaction ->
            // Complex calculation
            val tax = transaction.amount * 0.15
            val netAmount = transaction.amount - tax
            val formatted = String.format("%.2f", netAmount)
            TransactionDisplay(transaction.title, formatted)
        }
        
        provideContent {
            LazyColumn {
                items(processedData) { item ->
                    Text(item.title)
                    Text(item.amount)
                }
            }
        }
    }
}
```

#### AVOID: Frequent Unnecessary Updates
```kotlin
// BAD - Updating widget too frequently
class AggressiveWidgetUpdater {
    fun startUpdates() {
        // Updating every 30 seconds - too frequent
        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            30, TimeUnit.SECONDS
        ).build()
        
        workManager.enqueuePeriodicWork("aggressive_updates", updateRequest)
    }
    
    // Also updating on every data change
    fun onDataChanged(data: Any) {
        // This triggers widget update for every single change
        updateAllWidgets()
    }
}
```

### Memory Leaks

#### AVOID: Leaking Context in Singletons
```kotlin
// BAD - Leaking Activity context
object WidgetManager {
    private lateinit var context: Context
    
    fun init(context: Context) {
        this.context = context // Leaks if Activity context passed
    }
    
    fun updateWidget() {
        // Using leaked context
        val intent = Intent(context, MainActivity::class.java)
        // ... widget update logic
    }
}

// BAD usage in Activity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WidgetManager.init(this) // Leaking Activity context
    }
}
```

#### AVOID: Unbounded Collections in Widgets
```kotlin
// BAD - Unbounded data storage
class WidgetDataCache {
    private val cache = mutableMapOf<String, Any>()
    
    fun cacheData(key: String, data: Any) {
        cache[key] = data // Never cleared, grows indefinitely
    }
    
    fun getAllData(): List<Any> {
        return cache.values.toList() // Returns all cached data
    }
}

class TransactionWidget : GlanceAppWidget() {
    private val allTransactions = mutableListOf<Transaction>()
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Adding all transactions without limit
        val newTransactions = repository.getAllTransactions()
        allTransactions.addAll(newTransactions) // Grows indefinitely
        
        provideContent {
            LazyColumn {
                items(allTransactions) { // Could be thousands of items
                    TransactionItem(it)
                }
            }
        }
    }
}
```

### Inefficient Image Handling

#### AVOID: Loading Large Images in Widgets
```kotlin
// BAD - Loading large images in widget
@Composable
fun ImageHeavyWidget() {
    Column {
        // Loading full-resolution images in widget
        Image(
            provider = ImageProvider(R.drawable.large_background_image), // 2MB image
            modifier = GlanceModifier.fillMaxSize()
        )
        
        // Loading multiple images without optimization
        repeat(10) {
            Image(
                provider = ImageProvider(R.drawable.icon_large), // 500KB each
                modifier = GlanceModifier.size(48.dp)
            )
        }
    }
}
```

---

## Interactive Widget Mistakes

### Click Action Anti-Patterns

#### AVOID: Complex Logic in Click Handlers
```kotlin
// BAD - Complex logic in click actions
@Composable
fun ProblematicWidget() {
    Button(
        text = "Process Data",
        onClick = actionRunCallback(object : ActionCallback {
            override suspend fun onRun(
                context: Context,
                glanceId: GlanceId,
                parameters: ActionParameters
            ) {
                // Heavy processing in click handler
                val data = repository.getAllData() // Potentially large dataset
                val processed = data.map { item ->
                    // Complex calculation
                    val result = complexCalculation(item)
                    // Network call
                    val apiResult = api.validate(result)
                    // Database operation
                    database.saveResult(apiResult)
                    apiResult
                }
                
                // UI update
                updateWidget(glanceId, processed)
            }
        })
    )
}
```

#### AVOID: Missing Error Handling in Actions
```kotlin
// BAD - No error handling in widget actions
class RefreshAction : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // No try-catch, no error handling
        val data = api.refreshData() // Could throw exception
        updateWidget(glanceId, data) // Won't execute if above fails
    }
}
```

### Configuration Anti-Patterns

#### AVOID: Inefficient Configuration Storage
```kotlin
// BAD - Inefficient configuration storage
class WidgetConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Creating new SharedPreferences for each widget
        val widgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: return
        
        // Using separate files for each configuration item
        val currencyPrefs = getSharedPreferences("widget_currency_$widgetId", Context.MODE_PRIVATE)
        val themePrefs = getSharedPreferences("widget_theme_$widgetId", Context.MODE_PRIVATE)
        val intervalPrefs = getSharedPreferences("widget_interval_$widgetId", Context.MODE_PRIVATE)
        val detailsPrefs = getSharedPreferences("widget_details_$widgetId", Context.MODE_PRIVATE)
        
        // Multiple file operations
        currencyPrefs.edit().putString("currency", "USD").apply()
        themePrefs.edit().putString("theme", "light").apply()
        intervalPrefs.edit().putInt("interval", 30).apply()
        detailsPrefs.edit().putBoolean("details", true).apply()
    }
}
```

#### AVOID: Missing Configuration Validation
```kotlin
// BAD - No validation of configuration input
class WidgetConfigActivity : AppCompatActivity() {
    private fun saveConfiguration() {
        val currency = binding.currencyInput.text.toString()
        val interval = binding.intervalInput.text.toString().toInt()
        
        // No validation
        saveWidgetConfig(currency, interval) // Could crash if invalid
        
        // No error feedback to user
        finish()
    }
}
```

---

## Data Management Pitfalls

### Repository Anti-Patterns

#### AVOID: Direct Database Access in Widgets
```kotlin
// BAD - Direct database access in widget
class DatabaseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Direct database access - should use repository
        val database = Room.databaseBuilder(context, AppDatabase::class.java, "db").build()
        val transactions = database.transactionDao().getAll() // Blocking call
        
        provideContent {
            LazyColumn {
                items(transactions) { transaction ->
                    Text(transaction.title)
                }
            }
        }
    }
}
```

#### AVOID: Network Operations Without Error Handling
```kotlin
// BAD - Network operations without proper error handling
class NetworkWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // No error handling for network operations
        val response = api.getNetWorth() // Could fail
        val data = response.body()!! // Could be null
        
        provideContent {
            Text("Net Worth: ${data.amount}")
        }
    }
}
```

### Synchronization Issues

#### AVOID: Race Conditions in Updates
```kotlin
// BAD - Race conditions in widget updates
class WidgetUpdater {
    fun updateWidget(widgetId: Int, data: WidgetData) {
        // Multiple concurrent updates can overwrite each other
        viewModelScope.launch {
            val state1 = repository.getState()
            updateWidgetUI(widgetId, state1)
        }
        
        viewModelScope.launch {
            val state2 = repository.getState() // Might get different state
            updateWidgetUI(widgetId, state2) // Overwrites previous update
        }
    }
}
```

#### AVOID: Missing Offline Support
```kotlin
// BAD - No offline support
class OnlineOnlyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            val data = api.getData() // Requires network
            provideContent {
                Text(data.toString())
            }
        } catch (e: Exception) {
            // No fallback for offline scenarios
            provideContent {
                Text("Error loading data")
            }
        }
    }
}
```

---

## Testing Anti-Patterns

### Unit Testing Issues

#### AVOID: Testing Implementation Details
```kotlin
// BAD - Testing internal implementation
@Test
fun `test widget internal state`() {
    val widget = NetWorthWidget()
    
    // Testing private/internal implementation details
    val state = widget.getInternalState() // Exposing internal state
    assertEquals("loading", state.status)
    
    // Using reflection to test private methods
    val method = widget::class.java.getDeclaredMethod("privateMethod")
    method.isAccessible = true
    val result = method.invoke(widget)
    assertEquals(expected, result)
}
```

#### AVOID: Mock Overuse in Widget Tests
```kotlin
// BAD - Mocking everything
@Test
fun `test widget with excessive mocking`() {
    val mockContext = mock<Context>()
    val mockGlanceId = mock<GlanceId>()
    val mockRepository = mock<WidgetRepository>()
    val mockApi = mock<ApiService>()
    val mockDatabase = mock<AppDatabase>()
    val mockDao = mock<TransactionDao>()
    
    // Setting up dozens of mocks
    whenever(mockRepository.getData()).thenReturn(mockData)
    whenever(mockApi.getNetWorth()).thenReturn(mockResponse)
    whenever(mockDatabase.transactionDao()).thenReturn(mockDao)
    whenever(mockDao.getAll()).thenReturn(emptyList())
    
    // Test becomes brittle and tests mocks, not real behavior
    val widget = NetWorthWidget(mockRepository, mockApi, mockDatabase)
    widget.provideGlance(mockContext, mockGlanceId)
    
    verify(mockRepository).getData()
    verify(mockApi).getNetWorth()
    verify(mockDao).getAll()
}
```

### Integration Testing Issues

#### AVOID: Testing Without Real Data
```kotlin
// BAD - Testing with unrealistic data
@Test
fun `test widget with empty data`() {
    val widget = NetWorthWidget()
    
    // Testing with empty or unrealistic data
    widget.updateData(emptyList())
    
    val content = widget.getWidgetContent()
    assertTrue(content.contains("No data")) // Trivial test
}
```

#### AVOID: Missing Edge Case Testing
```kotlin
// BAD - Not testing edge cases
@Test
fun `test widget happy path only`() {
    // Only testing normal scenarios
    val widget = NetWorthWidget()
    widget.updateData(normalData)
    
    assertEquals("Expected content", widget.getWidgetContent())
    
    // Missing tests for:
    // - Network failures
    // - Empty data
    // - Large datasets
    // - Invalid configuration
    // - Widget resizing
}
```

---

## Accessibility Violations

### Missing Content Descriptions

#### AVOID: Unlabeled Widget Elements
```kotlin
// BAD - Missing content descriptions
@Composable
fun InaccessibleWidget() {
    Column {
        Text("$12,345.67") // Screen reader reads "1234567" without context
        
        Button("Refresh") { // Button text might be unclear
            onClick = actionRefresh()
        }
        
        Image(
            provider = ImageProvider(R.drawable.trend_up) // No description
        )
    }
}
```

#### AVOID: Poor Color Contrast
```kotlin
// BAD - Poor color contrast
@Composable
function PoorContrastWidget() {
    Column(
        modifier = GlanceModifier
            .background(Color(0xFFEEEEEE)) // Light gray background
    ) {
        Text(
            text = "Important Information",
            color = Color(0xFFCCCCCC) // Light gray text on light background
        )
        
        Button(
            text = "Action",
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFFFF0F0), // Very light pink
                contentColor = Color(0xFFFFE0E0)  // Even lighter text
            )
        )
    }
}
```

### Dynamic Content Issues

#### AVOID: Unannounced Content Changes
```kotlin
// BAD - Content changes without announcement
class SilentUpdateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val balance = remember { mutableStateOf(0.0) }
            
            // Silent updates without accessibility announcements
            LaunchedEffect(Unit) {
                while (true) {
                    delay(5000)
                    balance.value = repository.getCurrentBalance()
                    // No accessibility announcement of change
                }
            }
            
            Text("Balance: $${balance.value}")
        }
    }
}
```

---

## Build Configuration Anti-Patterns

### Widget Configuration Issues

#### AVOID: Incorrect Widget Metadata
```xml
<!-- BAD - Incorrect widget configuration -->
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="100dp"           <!-- Too small for content -->
    android:minHeight="50dp"           <!-- Too short for readable text -->
    android:updatePeriodMillis="1000"  <!-- Updates every second - battery drain -->
    android:resizeMode="none"          <!-- Can't resize - poor UX -->
    android:widgetCategory="keyguard"> <!-- Only on lock screen - limited use -->
</appwidget-provider>
```

#### AVOID: Missing Widget Features
```xml
<!-- BAD - Missing important widget features -->
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="180dp"
    android:minHeight="180dp"
    <!-- Missing resize mode - can't adapt to different sizes -->
    <!-- Missing configuration activity - no customization -->
    <!-- Missing preview image - poor user experience -->
    <!-- Missing description - unclear purpose -->
    <!-- Missing update period - no automatic updates -->
</appwidget-provider>
```

---

## Summary

Avoiding these bad practices will help you prevent:

- **Performance issues** from inefficient data loading and memory leaks
- **User experience problems** from unresponsive widgets and poor interactions
- **Battery drain** from excessive updates and background work
- **Crashes and instability** from missing error handling and race conditions
- **Accessibility violations** that exclude users with disabilities
- **Maintenance nightmares** from tightly coupled and poorly structured code
- **Testing difficulties** from anti-patterns that make code hard to test

Remember that good widgets should be:

- **Lightweight** and responsive
- **Battery-friendly** with smart update strategies
- **Accessible** to all users
- **Maintainable** with clean architecture
- **Testable** with proper separation of concerns
- **User-friendly** with intuitive interactions

Regular code reviews and adherence to best practices will ensure high-quality widget implementations that provide great user experiences.
