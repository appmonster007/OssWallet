# Android Widget Development - Best Practices

## Table of Contents
1. [Widget Architecture Overview](#widget-architecture-overview)
2. [Glance Framework Best Practices](#glance-framework-best-practices)
3. [Performance Optimization](#performance-optimization)
4. [Interactive Widget Design](#interactive-widget-design)
5. [Reconfigurable Widgets](#reconfigurable-widgets)
6. [Data Management & Synchronization](#data-management--synchronization)
7. [Testing Widget Components](#testing-widget-components)
8. [Accessibility & Material Design](#accessibility--material-design)

---

## Widget Architecture Overview

### Modern Widget Stack

#### DO: Use Glance Framework
```kotlin
@Composable
fun NetWorthWidget() {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .padding(16.dp)
        ) {
            Text(
                text = "Net Worth",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$12,345.67",
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}
```

#### DO: Implement Proper Widget Provider
```kotlin
class NetWorthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NetWorthWidget()
}
```

### Widget Configuration Structure

#### DO: Use Clean Architecture for Widgets
```
widget/
  providers/          # Widget receiver classes
  ui/                # Widget UI components
  data/              # Widget data sources
  config/            # Widget configuration activities
  update/            # Widget update management
```

---

## Glance Framework Best Practices

### State Management

#### DO: Use StateFlow for Widget State
```kotlin
class NetWorthWidget : GlanceAppWidget() {
    companion object {
        private val widgetStateKey = PreferencesKey<String>("widget_state")
    }
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val preferences = currentState<Preferences>()
            val state = preferences[widgetStateKey] ?: "loading"
            
            NetWorthWidgetContent(state = state)
        }
    }
}
```

#### DO: Implement Efficient State Updates
```kotlin
class WidgetUpdateManager @Inject constructor(
    private val context: Context
) {
    suspend fun updateWidget(widgetId: GlanceId, newState: String) {
        GlanceAppWidgetManager(context)
            .getGlanceAppWidget(NetWorthWidget::class.java)
            .update(widgetId) {
                PreferencesMutable {
                    this[widgetStateKey] = newState
                }
            }
    }
}
```

### Responsive Design

#### DO: Support Multiple Sizes
```kotlin
@Composable
fun ResponsiveNetWorthWidget() {
    val size = LocalSize.current
    val isCompact = size.width < 150.dp
    
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(if (isCompact) 8.dp else 16.dp)
    ) {
        if (isCompact) {
            CompactWidgetLayout()
        } else {
            FullWidgetLayout()
        }
    }
}
```

#### DO: Handle Dynamic Resizing
```kotlin
@Composable
fun DynamicWidgetContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetLayoutId(R.layout.widget_net_worth)
    ) {
        val availableHeight = LocalSize.current.height
        val showDetails = availableHeight > 100.dp
        
        Text("Net Worth")
        Text("$12,345.67")
        
        if (showDetails) {
            Text("+$234.56 today")
            Text("Last updated: 2 min ago")
        }
    }
}
```

---

## Performance Optimization

### Efficient Data Loading

#### DO: Use Background Workers for Updates
```kotlin
class WidgetUpdateWorker(
    appContext: Application,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val data = repository.getWidgetData()
            updateAllWidgets(data)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

#### DO: Implement Caching Strategy
```kotlin
class WidgetDataCache @Inject constructor(
    private val context: Context
) {
    private val cacheTimeout = 15 * 60 * 1000L // 15 minutes
    
    suspend fun getCachedData(): WidgetData? {
        val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
        val timestamp = prefs.getLong("cache_timestamp", 0)
        val isExpired = System.currentTimeMillis() - timestamp > cacheTimeout
        
        return if (isExpired) null else {
            prefs.getString("cached_data", null)?.let {
                Json.decodeFromString<WidgetData>(it)
            }
        }
    }
    
    suspend fun cacheData(data: WidgetData) {
        val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("cache_timestamp", System.currentTimeMillis())
            .putString("cached_data", Json.encodeToString(data))
            .apply()
    }
}
```

### Memory Management

#### DO: Use Lazy Loading for Complex Widgets
```kotlin
@Composable
fun LazyTransactionWidget() {
    val transactions = remember { mutableStateListOf<Transaction>() }
    
    LaunchedEffect(Unit) {
        // Load data in background
        withContext(Dispatchers.IO) {
            val data = repository.getRecentTransactions()
            transactions.addAll(data.take(5)) // Limit items
        }
    }
    
    LazyColumn(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        items(transactions) { transaction ->
            TransactionItem(transaction)
        }
    }
}
```

#### DO: Optimize Image Loading
```kotlin
@Composable
fun OptimizedImageWidget() {
    Image(
        provider = ImageProvider(R.drawable.widget_icon),
        modifier = GlanceModifier
            .size(24.dp)
            .contentScale(ContentScale.Fit),
        contentDescription = "Widget Icon"
    )
}
```

### Update Frequency Optimization

#### DO: Use Smart Update Strategy
```kotlin
class SmartWidgetUpdater @Inject constructor(
    private val workManager: WorkManager
) {
    fun scheduleWidgetUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            30, // Repeat interval in minutes
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                WorkRequest.MAX_BACKOFF_MILLIS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "widget_updates",
            ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
    }
}
```

---

## Interactive Widget Design

### Click Actions

#### DO: Implement Proper Click Handling
```kotlin
@Composable
fun InteractiveBalanceWidget() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("open_tab", "balance")
                    }
                )
            )
    ) {
        Text("Current Balance")
        Text("$5,432.10")
        
        Button(
            text = "Add Transaction",
            onClick = actionStartActivity(
                Intent(context, AddTransactionActivity::class.java)
            )
        )
    }
}
```

#### DO: Use Action Callbacks
```kotlin
class NetWorthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column {
                Text("Net Worth")
                Text("$12,345.67")
                
                Button(
                    text = "Refresh",
                    onClick = actionRunCallback<RefreshAction>()
                )
            }
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Refresh data
        val data = repository.refreshData()
        updateWidget(glanceId, data)
    }
}
```

### User Input Handling

#### DO: Support Configuration Activities
```kotlin
class NetWorthWidgetConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.widget_config_layout)
        
        setupConfigurationUI()
    }
    
    private fun setupConfigurationUI() {
        binding.currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCurrency = currencies[position]
                saveWidgetConfiguration(selectedCurrency)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun saveWidgetConfiguration(currency: String) {
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        
        val prefs = context.getSharedPreferences("widget_config_$appWidgetId", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("currency", currency)
            .apply()
        
        // Update widget
        updateWidget(appWidgetId, currency)
        
        // Return result
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}
```

---

## Reconfigurable Widgets

### Widget Configuration

#### DO: Use Proper Widget Info XML
```xml
<!-- res/xml/net_worth_widget_info.xml -->
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:initialLayout="@layout/widget_net_worth_loading"
    android:minWidth="165dp"
    android:minHeight="55dp"
    android:minResizeWidth="165dp"
    android:minResizeHeight="55dp"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="0"
    android:configure="com.osscabinet.ossWallet.ui.widget.config.NetWorthWidgetConfigActivity"
    android:widgetCategory="home_screen"
    android:description="@string/widget_description">
</appwidget-provider>
```

#### DO: Implement Configuration Persistence
```kotlin
class WidgetConfigurationManager @Inject constructor(
    private val context: Context
) {
    fun saveWidgetConfiguration(
        widgetId: Int,
        config: WidgetConfiguration
    ) {
        val prefs = context.getSharedPreferences("widget_config_$widgetId", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("currency", config.currency)
            .putString("theme", config.theme)
            .putBoolean("show_details", config.showDetails)
            .putInt("refresh_interval", config.refreshInterval)
            .apply()
    }
    
    fun loadWidgetConfiguration(widgetId: Int): WidgetConfiguration {
        val prefs = context.getSharedPreferences("widget_config_$widgetId", Context.MODE_PRIVATE)
        return WidgetConfiguration(
            currency = prefs.getString("currency", "USD") ?: "USD",
            theme = prefs.getString("theme", "light") ?: "light",
            showDetails = prefs.getBoolean("show_details", true),
            refreshInterval = prefs.getInt("refresh_interval", 30)
        )
    }
}
```

### Dynamic Widget Updates

#### DO: Support Real-time Configuration Changes
```kotlin
class ConfigurableNetWorthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = getWidgetId(id)
        val config = WidgetConfigurationManager(context).loadWidgetConfiguration(widgetId)
        
        provideContent {
            ConfigurableWidgetContent(config = config)
        }
        
        // Schedule updates based on configuration
        scheduleUpdates(config.refreshInterval)
    }
    
    private fun scheduleUpdates(intervalMinutes: Int) {
        val workManager = WorkManager.getInstance(context)
        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            intervalMinutes.toLong(),
            TimeUnit.MINUTES
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            "widget_update_$widgetId",
            ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
    }
}
```

---

## Data Management & Synchronization

### Efficient Data Sources

#### DO: Use Repository Pattern for Widgets
```kotlin
interface WidgetRepository {
    suspend fun getNetWorthData(currency: String): NetWorthData
    suspend fun getRecentTransactions(limit: Int = 5): List<Transaction>
    suspend fun getAccountBalances(): List<AccountBalance>
    fun observeDataChanges(): Flow<WidgetData>
}

class WidgetRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val currencyConverter: CurrencyConverter
) : WidgetRepository {
    
    override suspend fun getNetWorthData(currency: String): NetWorthData {
        val accounts = accountDao.getAllAccounts()
        val totalBalance = accounts.sumOf { 
            currencyConverter.convert(it.balance, it.currency, currency)
        }
        
        return NetWorthData(
            totalBalance = totalBalance,
            currency = currency,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    override fun observeDataChanges(): Flow<WidgetData> {
        return combine(
            transactionDao.getAllTransactionsFlow(),
            accountDao.getAllAccountsFlow()
        ) { transactions, accounts ->
            WidgetData(
                transactions = transactions.take(5),
                accounts = accounts,
                netWorth = accounts.sumOf { it.balance }
            )
        }
    }
}
```

### Background Synchronization

#### DO: Implement Smart Sync Strategy
```kotlin
class WidgetSyncManager @Inject constructor(
    private val repository: WidgetRepository,
    private val workManager: WorkManager
) {
    fun syncWidgetData(widgetId: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<WidgetSyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf("widget_id" to widgetId)
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "widget_sync_$widgetId",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}

class WidgetSyncWorker(
    appContext: Application,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    @Inject
    lateinit var repository: WidgetRepository
    
    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt("widget_id", -1)
        
        return try {
            val data = repository.getWidgetData()
            updateWidgetWithNewData(widgetId, data)
            Result.success()
        } catch (e: Exception) {
            Log.e("WidgetSyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
```

### Offline Support

#### DO: Handle Offline Scenarios
```kotlin
class OfflineWidgetManager @Inject constructor(
    private val repository: WidgetRepository,
    private val cache: WidgetDataCache
) {
    suspend fun getWidgetData(widgetId: Int): WidgetData {
        return try {
            // Try to get fresh data
            val freshData = repository.getWidgetData()
            cache.cacheData(freshData)
            freshData
        } catch (e: NetworkException) {
            // Fall back to cached data
            cache.getCachedData() ?: WidgetData.empty()
        }
    }
    
    suspend fun isDataFresh(): Boolean {
        val lastSync = repository.getLastSyncTimestamp()
        val now = System.currentTimeMillis()
        return (now - lastSync) < 15 * 60 * 1000L // 15 minutes
    }
}
```

---

## Testing Widget Components

### Unit Testing

#### DO: Test Widget Logic
```kotlin
@Test
fun `when widget data loaded, then update widget state`() = runTest {
    // Given
    val mockRepository = mock<WidgetRepository>()
    val testData = NetWorthData(totalBalance = 12345.67, currency = "USD")
    whenever(mockRepository.getNetWorthData("USD")).thenReturn(testData)
    
    val widget = NetWorthWidget(mockRepository)
    
    // When
    val result = widget.loadData("USD")
    
    // Then
    assertEquals("$12,345.67", result.formattedBalance)
    verify(mockRepository).getNetWorthData("USD")
}
```

#### DO: Test Configuration Management
```kotlin
@Test
fun `when widget configuration saved, then can be retrieved`() {
    // Given
    val config = WidgetConfiguration(
        currency = "EUR",
        theme = "dark",
        showDetails = true,
        refreshInterval = 60
    )
    
    val configManager = WidgetConfigurationManager(context)
    
    // When
    configManager.saveWidgetConfiguration(123, config)
    val retrievedConfig = configManager.loadWidgetConfiguration(123)
    
    // Then
    assertEquals(config, retrievedConfig)
}
```

### Integration Testing

#### DO: Test Widget Updates
```kotlin
@Test
fun `when data changes, then widget updates correctly`() = runTest {
    // Given
    val testRepository = FakeWidgetRepository()
    val widgetManager = WidgetUpdateManager(context, testRepository)
    
    // When
    testRepository.emitNewData(NetWorthData(totalBalance = 25000.0))
    
    // Then
    val widgetContent = widgetManager.getWidgetContent(123)
    assertTrue(widgetContent.contains("$25,000.00"))
}
```

### UI Testing

#### DO: Test Widget UI Components
```kotlin
@Test
fun `widget displays correct information`() {
    composeTestRule.setContent {
        GlanceTheme {
            NetWorthWidgetContent(
                data = NetWorthData(totalBalance = 12345.67, currency = "USD")
            )
        }
    }
    
    composeTestRule
        .onNodeWithText("$12,345.67")
        .assertIsDisplayed()
    
    composeTestRule
        .onNodeWithText("USD")
        .assertIsDisplayed()
}
```

---

## Accessibility & Material Design

### Accessibility Best Practices

#### DO: Provide Content Descriptions
```kotlin
@Composable
fun AccessibleWidgetContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Net worth widget showing current balance"
            }
    ) {
        Text(
            text = "Net Worth",
            modifier = GlanceModifier.semantics {
                this.contentDescription = "Net worth label"
            }
        )
        
        Text(
            text = "$12,345.67",
            modifier = GlanceModifier.semantics {
                this.contentDescription = "Current balance: twelve thousand three hundred forty-five dollars and sixty-seven cents"
            }
        )
    }
}
```

#### DO: Support High Contrast
```kotlin
@Composable
fun HighContrastWidget() {
    val colorScheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFF90CAF9),
            onPrimary = Color(0xFF000000),
            surface = Color(0xFF121212),
            onSurface = Color(0xFFFFFFFF)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1976D2),
            onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF000000)
        )
    }
    
    GlanceTheme(colors = colorScheme) {
        WidgetContent()
    }
}
```

### Material Design Compliance

#### DO: Follow Material 3 Guidelines
```kotlin
@Composable
fun Material3Widget() {
    GlanceTheme {
        Surface(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(
                    color = GlanceTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Net Worth",
                    style = MaterialTheme.typography.titleMedium,
                    color = GlanceTheme.colors.onSurface
                )
                
                Spacer(GlanceModifier.height(8.dp))
                
                Text(
                    text = "$12,345.67",
                    style = MaterialTheme.typography.headlineLarge,
                    color = GlanceTheme.colors.primary
                )
                
                Spacer(GlanceModifier.height(16.dp))
                
                Button(
                    text = "View Details",
                    onClick = actionStartActivity(mainActivityIntent)
                )
            }
        }
    }
}
```

#### DO: Use Proper Typography Scale
```kotlin
@Composable
fun TypographyWidget() {
    Column(
        modifier = GlanceModifier.padding(16.dp)
    ) {
        Text(
            text = "Net Worth",
            style = MaterialTheme.typography.titleMedium
        )
        
        Text(
            text = "$12,345.67",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Text(
            text = "Last updated: 2 min ago",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
```

---

## Summary

Following these best practices will help you build widgets that are:

- **Performant** with efficient data loading and caching
- **Interactive** with proper click handling and user input
- **Reconfigurable** with flexible configuration options
- **Accessible** following Material Design guidelines
- **Maintainable** with clean architecture and proper testing
- **Reliable** with robust error handling and offline support

Remember that widgets should be:
- **Lightweight** and responsive
- **Battery-friendly** with smart update strategies
- **User-friendly** with intuitive interactions
- **Consistent** with your app's design language

Regular testing and monitoring of widget performance will ensure a great user experience across all device types and configurations.
