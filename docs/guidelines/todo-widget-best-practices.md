# High-Performance Todo Widget Development - Best Practices

## Table of Contents
1. [Todo Widget Architecture](#todo-widget-architecture)
2. [Reconfigurable Widget Views](#reconfigurable-widget-views)
3. [Widget Configuration UI](#widget-configuration-ui)
4. [CRUD Operations in Widgets](#crud-operations-in-widgets)
5. [Performance Optimization](#performance-optimization)
6. [Real-time Synchronization](#real-time-synchronization)
7. [Widget State Management](#widget-state-management)
8. [Advanced Widget Features](#advanced-widget-features)

---

## Todo Widget Architecture

### Microsoft To-Do Inspired Architecture

#### DO: Use Multi-View Widget System
```kotlin
// Widget view types
enum class TodoWidgetView {
    LIST_ONLY,           // Simple todo list
    LIST_WITH_STATS,     // List + completion stats
    COMPACT_DUE_TODAY,   // Only items due today
    GRID_VIEW,           // Grid layout for todos
    KANBAN_BOARD         // Kanban-style widget
}

// Widget configuration data class
data class TodoWidgetConfig(
    val viewMode: TodoWidgetView = TodoWidgetView.LIST_ONLY,
    val maxItems: Int = 5,
    val showCompleted: Boolean = false,
    val showDueDate: Boolean = true,
    val showPriority: Boolean = false,
    val filterCategory: String? = null,
    val sortBy: TodoSortBy = TodoSortBy.DUE_DATE,
    val theme: TodoWidgetTheme = TodoWidgetTheme.LIGHT,
    val refreshInterval: Int = 30 // minutes
)

// Main widget class
class TodoWidget : GlanceAppWidget() {
    companion object {
        private val configKey = PreferencesKey<String>("widget_config")
        private val lastUpdateKey = PreferencesKey<Long>("last_update")
    }
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = loadWidgetConfig(context, id)
        
        provideContent {
            GlanceTheme {
                when (config.viewMode) {
                    TodoWidgetView.LIST_ONLY -> TodoListWidget(config)
                    TodoWidgetView.LIST_WITH_STATS -> TodoListWithStatsWidget(config)
                    TodoWidgetView.COMPACT_DUE_TODAY -> DueTodayWidget(config)
                    TodoWidgetView.GRID_VIEW -> TodoGridWidget(config)
                    TodoWidgetView.KANBAN_BOARD -> TodoKanbanWidget(config)
                }
            }
        }
    }
}
```

#### DO: Implement Clean Widget Repository
```kotlin
interface TodoWidgetRepository {
    suspend fun getTodosForWidget(config: TodoWidgetConfig): Flow<List<TodoItem>>
    suspend fun getTodoStats(config: TodoWidgetConfig): TodoStats
    suspend fun addTodoToWidget(todo: TodoItem): Result<TodoItem>
    suspend fun updateTodoInWidget(todo: TodoItem): Result<TodoItem>
    suspend fun deleteTodoFromWidget(todoId: String): Result<Unit>
    suspend fun toggleTodoCompletion(todoId: String): Result<TodoItem>
    fun observeTodoChanges(): Flow<TodoChange>
}

class TodoWidgetRepositoryImpl @Inject constructor(
    private val todoDao: TodoDao,
    private val todoApi: TodoApiService,
    private val cache: TodoWidgetCache
) : TodoWidgetRepository {
    
    override suspend fun getTodosForWidget(config: TodoWidgetConfig): Flow<List<TodoItem>> {
        return combine(
            todoDao.getTodosFlow(),
            todoApi.getTodosFlow()
        ) { localTodos, remoteTodos ->
            val mergedTodos = mergeTodos(localTodos, remoteTodos)
            applyWidgetFilters(mergedTodos, config)
        }.distinctUntilChanged()
    }
    
    private fun applyWidgetFilters(
        todos: List<TodoItem>,
        config: TodoWidgetConfig
    ): List<TodoItem> {
        return todos
            .let { if (!config.showCompleted) it.filter { !it.isCompleted } else it }
            .let { if (config.filterCategory != null) it.filter { it.category == config.filterCategory } else it }
            .let { if (config.viewMode == TodoWidgetView.COMPACT_DUE_TODAY) it.filter { isDueToday(it.dueDate) } else it }
            .sortedBy { when (config.sortBy) {
                TodoSortBy.DUE_DATE -> it.dueDate
                TodoSortBy.PRIORITY -> it.priority.ordinal
                TodoSortBy.CREATED_DATE -> it.createdAt
                TodoSortBy.ALPHABETICAL -> it.title.lowercase()
            }}
            .take(config.maxItems)
    }
}
```

---

## Reconfigurable Widget Views

### Gear Icon Configuration

#### DO: Implement Gear Icon with Dropdown Menu
```kotlin
@Composable
fun TodoWidgetWithConfig(config: TodoWidgetConfig) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(16.dp)
    ) {
        // Header with gear icon
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.End
        ) {
            Text(
                text = "My Tasks",
                style = MaterialTheme.typography.titleMedium,
                modifier = GlanceModifier.defaultWeight()
            )
            
            // Gear icon for configuration
            Button(
                text = "",
                onClick = actionStartActivity(
                    Intent(context, TodoWidgetConfigActivity::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        putExtra("current_config", config.toJson())
                    }
                ),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = GlanceTheme.colors.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configure widget"
                )
            }
        }
        
        Spacer(GlanceModifier.height(8.dp))
        
        // Widget content based on view mode
        WidgetContent(config)
    }
}
```

#### DO: Create Dropdown Configuration Menu
```kotlin
class TodoWidgetConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: WidgetConfigBinding
    private lateinit var currentConfig: TodoWidgetConfig
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        currentConfig = loadCurrentConfig()
        setupConfigurationUI()
    }
    
    private fun setupConfigurationUI() {
        // View mode dropdown
        setupViewModeDropdown()
        
        // Max items dropdown
        setupMaxItemsDropdown()
        
        // Sort by dropdown
        setupSortByDropdown()
        
        // Theme dropdown
        setupThemeDropdown()
        
        // Toggle switches
        setupToggleSwitches()
        
        // Save button
        binding.saveButton.setOnClickListener {
            saveConfiguration()
        }
    }
    
    private fun setupViewModeDropdown() {
        val viewModes = arrayOf(
            "List View",
            "List with Stats", 
            "Due Today Only",
            "Grid View",
            "Kanban Board"
        )
        
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, viewModes)
        binding.viewModeDropdown.setAdapter(adapter)
        
        binding.viewModeDropdown.setText(
            viewModes[currentConfig.viewMode.ordinal],
            false
        )
        
        binding.viewModeDropdown.setOnItemClickListener { _, _, position, _ ->
            currentConfig = currentConfig.copy(viewMode = TodoWidgetView.values()[position])
            updatePreview()
        }
    }
    
    private fun setupMaxItemsDropdown() {
        val maxItemsOptions = arrayOf("3", "5", "7", "10", "15")
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, maxItemsOptions)
        binding.maxItemsDropdown.setAdapter(adapter)
        
        binding.maxItemsDropdown.setText(
            currentConfig.maxItems.toString(),
            false
        )
        
        binding.maxItemsDropdown.setOnItemClickListener { _, _, position, _ ->
            currentConfig = currentConfig.copy(maxItems = maxItemsOptions[position].toInt())
            updatePreview()
        }
    }
}
```

### Dynamic View Switching

#### DO: Implement View Mode Switching
```kotlin
@Composable
fun WidgetContent(config: TodoWidgetConfig) {
    when (config.viewMode) {
        TodoWidgetView.LIST_ONLY -> TodoListContent(config)
        TodoWidgetView.LIST_WITH_STATS -> TodoListWithStatsContent(config)
        TodoWidgetView.COMPACT_DUE_TODAY -> DueTodayContent(config)
        TodoWidgetView.GRID_VIEW -> TodoGridContent(config)
        TodoWidgetView.KANBAN_BOARD -> TodoKanbanContent(config)
    }
}

@Composable
fun TodoListWithStatsContent(config: TodoWidgetConfig) {
    val todos by produceState<List<TodoItem>>(initialValue = emptyList()) {
        value = todoRepository.getTodosForWidget(config).first()
    }
    
    val stats by produceState<TodoStats>(initialValue = TodoStats()) {
        value = todoRepository.getTodoStats(config)
    }
    
    Column {
        // Stats section
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${stats.completed}/${stats.total}",
                    style = MaterialTheme.typography.titleMedium,
                    color = GlanceTheme.colors.primary
                )
            }
            
            Column {
                Text(
                    text = "Due Today",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stats.dueToday.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (stats.dueToday > 0) Color.Red else GlanceTheme.colors.onSurface
                )
            }
            
            Column {
                Text(
                    text = "Overdue",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stats.overdue.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Red
                )
            }
        }
        
        Spacer(GlanceModifier.height(12.dp))
        
        // Todo list
        LazyColumn {
            items(todos) { todo ->
                TodoItemRow(todo, config)
            }
        }
        
        // Add todo button
        Spacer(GlanceModifier.height(8.dp))
        Button(
            text = "Add Task",
            onClick = actionStartActivity(
                Intent(context, AddTodoActivity::class.java).apply {
                    putExtra("widget_config", config.toJson())
                }
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )
    }
}
```

---

## Widget Configuration UI

### Advanced Configuration Activity

#### DO: Create Rich Configuration Experience
```kotlin
class TodoWidgetConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: WidgetConfigBinding
    private lateinit var currentConfig: TodoWidgetConfig
    private lateinit var widgetPreview: TodoWidgetPreview
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        currentConfig = loadCurrentConfig()
        widgetPreview = TodoWidgetPreview(binding.previewContainer)
        
        setupConfigurationUI()
        setupPreview()
    }
    
    private fun setupConfigurationUI() {
        setupTabs()
        setupBasicConfig()
        setupAdvancedConfig()
        setupAppearanceConfig()
    }
    
    private fun setupTabs() {
        val adapter = TabAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Basic"
                1 -> "Advanced"
                2 -> "Appearance"
                else -> "Basic"
            }
        }.attach()
    }
    
    private fun setupBasicConfig() {
        // View mode selection with cards
        setupViewModeCards()
        
        // Max items slider
        binding.maxItemsSlider.apply {
            value = currentConfig.maxItems.toFloat()
            addOnChangeListener { _, value, _ ->
                currentConfig = currentConfig.copy(maxItems = value.toInt())
                binding.maxItemsText.text = "${value.toInt()} items"
                updatePreview()
            }
        }
        
        // Category filter
        setupCategoryFilter()
    }
    
    private fun setupViewModeCards() {
        val viewModes = listOf(
            ViewModeCard("List", R.drawable.ic_list, TodoWidgetView.LIST_ONLY),
            ViewModeCard("List + Stats", R.drawable.ic_list_stats, TodoWidgetView.LIST_WITH_STATS),
            ViewModeCard("Due Today", R.drawable.ic_calendar, TodoWidgetView.COMPACT_DUE_TODAY),
            ViewModeCard("Grid", R.drawable.ic_grid, TodoWidgetView.GRID_VIEW),
            ViewModeCard("Kanban", R.drawable.ic_kanban, TodoWidgetView.KANBAN_BOARD)
        )
        
        binding.viewModeRecyclerView.adapter = ViewModeCardAdapter(viewModes) { viewMode ->
            currentConfig = currentConfig.copy(viewMode = viewMode)
            updatePreview()
        }
    }
}
```

#### DO: Implement Live Preview
```kotlin
class TodoWidgetPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val previewBinding: WidgetPreviewBinding
    private var currentConfig: TodoWidgetConfig = TodoWidgetConfig()
    
    init {
        previewBinding = WidgetPreviewBinding.inflate(LayoutInflater.from(context), this, true)
    }
    
    fun updatePreview(config: TodoWidgetConfig) {
        currentConfig = config
        renderPreview()
    }
    
    private fun renderPreview() {
        // Generate sample data for preview
        val sampleTodos = generateSampleTodos()
        
        previewBinding.previewContainer.removeAllViews()
        
        when (currentConfig.viewMode) {
            TodoWidgetView.LIST_ONLY -> renderListPreview(sampleTodos)
            TodoWidgetView.LIST_WITH_STATS -> renderListWithStatsPreview(sampleTodos)
            TodoWidgetView.COMPACT_DUE_TODAY -> renderDueTodayPreview(sampleTodos)
            TodoWidgetView.GRID_VIEW -> renderGridPreview(sampleTodos)
            TodoWidgetView.KANBAN_BOARD -> renderKanbanPreview(sampleTodos)
        }
    }
    
    private fun renderListWithStatsPreview(todos: List<TodoItem>) {
        val previewView = ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Stats preview
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatItem("Completed", "5/10")
                                StatItem("Due Today", "3")
                                StatItem("Overdue", "1")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // List preview
                            todos.take(currentConfig.maxItems).forEach { todo ->
                                TodoItemPreview(todo, currentConfig)
                            }
                        }
                    }
                }
            }
        }
        
        previewBinding.previewContainer.addView(previewView)
    }
}
```

---

## CRUD Operations in Widgets

### Add Todo from Widget

#### DO: Implement Quick Add Todo
```kotlin
@Composable
fun QuickAddTodo(config: TodoWidgetConfig) {
    var text by remember { mutableStateOf("") }
    
    Column {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Add new task...",
                modifier = GlanceModifier.defaultWeight()
            )
            
            Button(
                text = "+",
                onClick = actionRunCallback<AddTodoAction>(
                    ActionParameters.Builder()
                        .setString("todo_text", text)
                        .setString("widget_config", config.toJson())
                        .build()
                ),
                enabled = text.isNotBlank(),
                modifier = GlanceModifier.size(40.dp)
            )
        }
    }
}

class AddTodoAction : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoText = parameters["todo_text"] ?: return
        val configJson = parameters["widget_config"] ?: return
        val config = TodoWidgetConfig.fromJson(configJson)
        
        try {
            val todo = TodoItem(
                id = UUID.randomUUID().toString(),
                title = todoText,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
                dueDate = null,
                priority = TodoPriority.MEDIUM
            )
            
            val result = todoRepository.addTodoToWidget(todo)
            
            if (result.isSuccess) {
                // Update widget with new data
                updateWidgetWithNewData(glanceId, config)
                
                // Show success feedback
                showToast(context, "Task added successfully")
            } else {
                showToast(context, "Failed to add task")
            }
        } catch (e: Exception) {
            Log.e("AddTodoAction", "Error adding todo", e)
            showToast(context, "Error adding task")
        }
    }
}
```

### Toggle Todo Completion

#### DO: Implement Quick Toggle
```kotlin
@Composable
fun TodoItemRow(todo: TodoItem, config: TodoWidgetConfig) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(
                actionRunCallback<ToggleTodoAction>(
                    ActionParameters.Builder()
                        .setString("todo_id", todo.id)
                        .setBoolean("is_completed", !todo.isCompleted)
                        .setString("widget_config", config.toJson())
                        .build()
                )
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = null // Handled by click action
        )
        
        Spacer(GlanceModifier.width(12.dp))
        
        // Todo content
        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (todo.isCompleted) {
                    GlanceTheme.colors.onSurface.copy(alpha = 0.6f)
                } else {
                    GlanceTheme.colors.onSurface
                },
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
            )
            
            if (config.showDueDate && todo.dueDate != null) {
                Text(
                    text = formatDueDate(todo.dueDate!!),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue(todo.dueDate!!)) Color.Red else GlanceTheme.colors.onSurfaceVariant
                )
            }
            
            if (config.showPriority && todo.priority != TodoPriority.LOW) {
                Text(
                    text = todo.priority.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = getPriorityColor(todo.priority)
                )
            }
        }
        
        // Action buttons
        Row {
            Button(
                text = "",
                onClick = actionRunCallback<EditTodoAction>(
                    ActionParameters.Builder()
                        .setString("todo_id", todo.id)
                        .build()
                ),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = GlanceTheme.colors.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit task"
                )
            }
            
            Button(
                text = "",
                onClick = actionRunCallback<DeleteTodoAction>(
                    ActionParameters.Builder()
                        .setString("todo_id", todo.id)
                        .build()
                ),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = Color.Red
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task"
                )
            }
        }
    }
}

class ToggleTodoAction : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoId = parameters["todo_id"] ?: return
        val isCompleted = parameters["is_completed"] ?: false
        val configJson = parameters["widget_config"] ?: return
        val config = TodoWidgetConfig.fromJson(configJson)
        
        try {
            val result = todoRepository.toggleTodoCompletion(todoId)
            
            if (result.isSuccess) {
                updateWidgetWithNewData(glanceId, config)
                
                // Haptic feedback
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } catch (e: Exception) {
            Log.e("ToggleTodoAction", "Error toggling todo", e)
        }
    }
}
```

### Edit and Delete Operations

#### DO: Implement Edit/Delete Actions
```kotlin
class EditTodoAction : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoId = parameters["todo_id"] ?: return
        
        // Launch edit activity
        val intent = Intent(context, EditTodoActivity::class.java).apply {
            putExtra("todo_id", todoId)
            putExtra("from_widget", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        context.startActivity(intent)
    }
}

class DeleteTodoAction : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoId = parameters["todo_id"] ?: return
        val configJson = parameters["widget_config"] ?: return
        val config = TodoWidgetConfig.fromJson(configJson)
        
        try {
            val result = todoRepository.deleteTodoFromWidget(todoId)
            
            if (result.isSuccess) {
                updateWidgetWithNewData(glanceId, config)
                showToast(context, "Task deleted")
            } else {
                showToast(context, "Failed to delete task")
            }
        } catch (e: Exception) {
            Log.e("DeleteTodoAction", "Error deleting todo", e)
            showToast(context, "Error deleting task")
        }
    }
}
```

---

## Performance Optimization

### Efficient Data Loading

#### DO: Implement Smart Caching Strategy
```kotlin
class TodoWidgetCache @Inject constructor(
    private val context: Context
) {
    private val cacheTimeout = 15 * 60 * 1000L // 15 minutes
    private val maxCacheSize = 100 // Maximum items to cache
    
    suspend fun getCachedTodos(config: TodoWidgetConfig): List<TodoItem>? {
        val cacheKey = generateCacheKey(config)
        val prefs = context.getSharedPreferences("todo_widget_cache", Context.MODE_PRIVATE)
        
        val timestamp = prefs.getLong("${cacheKey}_timestamp", 0)
        val isExpired = System.currentTimeMillis() - timestamp > cacheTimeout
        
        return if (isExpired) null else {
            val cachedJson = prefs.getString(cacheKey, null)
            cachedJson?.let {
                Json.decodeFromString<List<TodoItem>>(it)
            }
        }
    }
    
    suspend fun cacheTodos(config: TodoWidgetConfig, todos: List<TodoItem>) {
        val cacheKey = generateCacheKey(config)
        val prefs = context.getSharedPreferences("todo_widget_cache", Context.MODE_PRIVATE)
        
        // Limit cache size
        val limitedTodos = todos.take(maxCacheSize)
        
        prefs.edit()
            .putString(cacheKey, Json.encodeToString(limitedTodos))
            .putLong("${cacheKey}_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    private fun generateCacheKey(config: TodoWidgetConfig): String {
        return "${config.viewMode}_${config.maxItems}_${config.filterCategory}_${config.sortBy}"
    }
}
```

#### DO: Use Background Workers for Updates
```kotlin
class TodoWidgetUpdateWorker(
    appContext: Application,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    @Inject
    lateinit var todoRepository: TodoWidgetRepository
    
    @Inject
    lateinit var widgetManager: TodoWidgetManager
    
    override suspend fun doWork(): Result {
        return try {
            val widgetIds = getAllWidgetIds()
            
            widgetIds.forEach { widgetId ->
                val config = loadWidgetConfig(widgetId)
                val todos = todoRepository.getTodosForWidget(config).first()
                
                // Update cache
                todoRepository.cacheTodos(config, todos)
                
                // Update widget
                widgetManager.updateWidget(widgetId, todos, config)
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("TodoWidgetUpdateWorker", "Widget update failed", e)
            Result.retry()
        }
    }
}

class TodoWidgetScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun scheduleWidgetUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false) // Allow updates even when battery is low
            .setRequiresCharging(false)
            .build()
        
        val updateRequest = PeriodicWorkRequestBuilder<TodoWidgetUpdateWorker>(
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
            "todo_widget_updates",
            ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
    }
}
```

### Memory Optimization

#### DO: Optimize Widget Memory Usage
```kotlin
@Composable
fun OptimizedTodoList(config: TodoWidgetConfig) {
    // Use LazyColumn for efficient scrolling
    LazyColumn(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // Limit items to prevent memory issues
        items(
            count = config.maxItems,
            itemContent = { index ->
                // Load data on demand
                val todo = remember(index) {
                    loadTodoItem(index, config)
                }
                
                if (todo != null) {
                    TodoItemRow(todo, config)
                }
            }
        )
    }
}

// Use paging for large datasets
class TodoPagingSource @Inject constructor(
    private val repository: TodoWidgetRepository,
    private val config: TodoWidgetConfig
) : PagingSource<Int, TodoItem>() {
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TodoItem> {
        return try {
            val page = params.key ?: 0
            val pageSize = minOf(params.loadSize, config.maxItems)
            
            val todos = repository.getTodosForWidget(config)
                .first()
                .drop(page * pageSize)
                .take(pageSize)
            
            LoadResult.Page(
                data = todos,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (todos.size < pageSize) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
```

---

## Real-time Synchronization

### Live Data Updates

#### DO: Implement Real-time Updates
```kotlin
class TodoWidgetRealtimeManager @Inject constructor(
    private val repository: TodoWidgetRepository,
    private val widgetManager: TodoWidgetManager
) {
    private val updateJobs = mutableMapOf<String, Job>()
    
    fun startRealtimeUpdates(widgetId: String, config: TodoWidgetConfig) {
        // Cancel existing job for this widget
        updateJobs[widgetId]?.cancel()
        
        // Start new job
        updateJobs[widgetId] = CoroutineScope(Dispatchers.Main).launch {
            repository.observeTodoChanges()
                .collect { change ->
                    when (change.type) {
                        TodoChangeType.ADDED -> handleTodoAdded(widgetId, change.todo, config)
                        TodoChangeType.UPDATED -> handleTodoUpdated(widgetId, change.todo, config)
                        TodoChangeType.DELETED -> handleTodoDeleted(widgetId, change.todoId, config)
                        TodoChangeType.COMPLETION_TOGGLED -> handleCompletionToggled(widgetId, change.todo, config)
                    }
                }
        }
    }
    
    private suspend fun handleTodoAdded(widgetId: String, todo: TodoItem, config: TodoWidgetConfig) {
        val currentTodos = widgetManager.getCurrentTodos(widgetId)
        val updatedTodos = currentTodos + todo
        
        // Apply widget filters
        val filteredTodos = applyWidgetFilters(updatedTodos, config)
        widgetManager.updateWidget(widgetId, filteredTodos, config)
    }
    
    private suspend fun handleTodoUpdated(widgetId: String, todo: TodoItem, config: TodoWidgetConfig) {
        val currentTodos = widgetManager.getCurrentTodos(widgetId)
        val updatedTodos = currentTodos.map { if (it.id == todo.id) todo else it }
        
        widgetManager.updateWidget(widgetId, updatedTodos, config)
    }
}
```

#### DO: Use WebSocket for Real-time Sync
```kotlin
class TodoWebSocketService @Inject constructor(
    private val gson: Gson,
    private val repository: TodoWidgetRepository
) {
    private var webSocket: WebSocket? = null
    private val todoUpdateFlow = MutableSharedFlow<TodoUpdate>()
    
    fun connect() {
        val request = Request.Builder()
            .url("wss://api.todoapp.com/ws")
            .build()
        
        webSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("TodoWebSocket", "Connected")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val update = gson.fromJson(text, TodoUpdate::class.java)
                    CoroutineScope(Dispatchers.IO).launch {
                        todoUpdateFlow.emit(update)
                    }
                } catch (e: Exception) {
                    Log.e("TodoWebSocket", "Error parsing message", e)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("TodoWebSocket", "Connection failed", t)
                // Implement reconnection logic
                scheduleReconnect()
            }
        })
    }
    
    fun observeTodoUpdates(): Flow<TodoUpdate> = todoUpdateFlow.asSharedFlow()
    
    private fun scheduleReconnect() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(5000) // Wait 5 seconds before reconnecting
            connect()
        }
    }
}
```

---

## Widget State Management

### Persistent State Management

#### DO: Use DataStore for Configuration
```kotlin
class TodoWidgetPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val WIDGET_CONFIGS_KEY = stringPreferencesKey("widget_configs")
        private val LAST_UPDATE_KEY = longPreferencesKey("last_update")
    }
    
    suspend fun saveWidgetConfig(widgetId: String, config: TodoWidgetConfig) {
        dataStore.edit { preferences ->
            val currentConfigs = getWidgetConfigs(preferences).toMutableMap()
            currentConfigs[widgetId] = config.toJson()
            preferences[WIDGET_CONFIGS_KEY] = Json.encodeToString(currentConfigs)
            preferences[LAST_UPDATE_KEY] = System.currentTimeMillis()
        }
    }
    
    suspend fun getWidgetConfig(widgetId: String): TodoWidgetConfig? {
        return dataStore.data.map { preferences ->
            val configs = getWidgetConfigs(preferences)
            configs[widgetId]?.let { TodoWidgetConfig.fromJson(it) }
        }.first()
    }
    
    private fun getWidgetConfigs(preferences: Preferences): Map<String, String> {
        return try {
            val configsJson = preferences[WIDGET_CONFIGS_KEY] ?: "{}"
            Json.decodeFromString<Map<String, String>>(configsJson)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
```

#### DO: Implement State Restoration
```kotlin
class TodoWidgetStateManager @Inject constructor(
    private val preferences: TodoWidgetPreferences,
    private val cache: TodoWidgetCache
) {
    suspend fun restoreWidgetState(widgetId: String): WidgetState? {
        return try {
            val config = preferences.getWidgetConfig(widgetId) ?: return null
            val cachedTodos = cache.getCachedTodos(config) ?: return null
            
            WidgetState(
                config = config,
                todos = cachedTodos,
                lastUpdate = System.currentTimeMillis(),
                isLoading = false
            )
        } catch (e: Exception) {
            Log.e("WidgetStateManager", "Error restoring state", e)
            null
        }
    }
    
    suspend fun saveWidgetState(widgetId: String, state: WidgetState) {
        try {
            preferences.saveWidgetConfig(widgetId, state.config)
            cache.cacheTodos(state.config, state.todos)
        } catch (e: Exception) {
            Log.e("WidgetStateManager", "Error saving state", e)
        }
    }
}
```

---

## Advanced Widget Features

### Kanban Board Widget

#### DO: Implement Kanban-style Widget
```kotlin
@Composable
fun TodoKanbanContent(config: TodoWidgetConfig) {
    val todos by produceState<List<TodoItem>>(initialValue = emptyList()) {
        value = todoRepository.getTodosForWidget(config).first()
    }
    
    val columns = listOf(
        KanbanColumn("To Do", todos.filter { !it.isCompleted && !isInProgress(it) }),
        KanbanColumn("In Progress", todos.filter { isInProgress(it) }),
        KanbanColumn("Done", todos.filter { it.isCompleted })
    )
    
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.SpaceEvenly
    ) {
        columns.forEach { column ->
            KanbanColumnWidget(column, config)
        }
    }
}

@Composable
fun KanbanColumnWidget(column: KanbanColumn, config: TodoWidgetConfig) {
    Column(
        modifier = GlanceModifier
            .width(100.dp)
            .background(
                color = getColumnColor(column.title),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Text(
            text = column.title,
            style = MaterialTheme.typography.labelMedium,
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        
        Spacer(GlanceModifier.height(8.dp))
        
        LazyColumn {
            items(column.todos.take(3)) { todo ->
                Card(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        backgroundColor = GlanceTheme.colors.surface
                    )
                ) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = GlanceModifier.padding(4.dp),
                        maxLines = 2
                    )
                }
            }
        }
    }
}
```

### Grid View Widget

#### DO: Implement Grid Layout
```kotlin
@Composable
fun TodoGridContent(config: TodoWidgetConfig) {
    val todos by produceState<List<TodoItem>>(initialValue = emptyList()) {
        value = todoRepository.getTodosForWidget(config).first()
    }
    
    LazyVerticalGrid(
        modifier = GlanceModifier.fillMaxSize(),
        gridCells = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(todos.take(config.maxItems)) { todo ->
            TodoGridItem(todo, config)
        }
    }
}

@Composable
fun TodoGridItem(todo: TodoItem, config: TodoWidgetConfig) {
    Card(
        modifier = GlanceModifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(
                actionRunCallback<ToggleTodoAction>(
                    ActionParameters.Builder()
                        .setString("todo_id", todo.id)
                        .setBoolean("is_completed", !todo.isCompleted)
                        .build()
                )
            ),
        colors = CardDefaults.cardColors(
            backgroundColor = if (todo.isCompleted) {
                GlanceTheme.colors.surfaceVariant
            } else {
                GlanceTheme.colors.surface
            }
        )
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator
            if (config.showPriority && todo.priority != TodoPriority.LOW) {
                Box(
                    modifier = GlanceModifier
                        .size(8.dp)
                        .background(
                            color = getPriorityColor(todo.priority),
                            shape = CircleShape
                        )
                        .align(Alignment.End)
                )
            }
            
            Spacer(GlanceModifier.height(8.dp))
            
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodySmall,
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = 3,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
            )
            
            Spacer(GlanceModifier.defaultWeight())
            
            if (config.showDueDate && todo.dueDate != null) {
                Text(
                    text = formatShortDate(todo.dueDate!!),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue(todo.dueDate!!)) Color.Red else GlanceTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}
```

---

## Summary

Following these best practices will help you build high-performance todo widgets like Microsoft To-Do with:

- **Reconfigurable views** supporting multiple layout modes (list, grid, kanban)
- **Rich configuration UI** with gear icon and dropdown menus
- **Efficient CRUD operations** directly from the widget
- **Real-time synchronization** with live data updates
- **Performance optimization** with smart caching and background updates
- **Advanced features** like kanban boards and grid layouts
- **Robust state management** with persistent configuration
- **Material Design compliance** with proper accessibility

The key is to balance functionality with performance, ensuring widgets remain responsive and battery-friendly while providing rich user experiences.
