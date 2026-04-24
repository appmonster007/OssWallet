# High-Performance Todo Widget Development - Bad Practices to Avoid

## Table of Contents
1. [Architecture Anti-Patterns](#architecture-anti-patterns)
2. [Configuration UI Mistakes](#configuration-ui-mistakes)
3. [CRUD Operation Pitfalls](#crud-operation-pitfalls)
4. [Performance Killers](#performance-killers)
5. [Synchronization Issues](#synchronization-issues)
6. [State Management Anti-Patterns](#state-management-anti-patterns)
7. [Widget UI Anti-Patterns](#widget-ui-anti-patterns)
8. [Testing and Debugging Issues](#testing-and-debugging-issues)

---

## Architecture Anti-Patterns

### Monolithic Widget Design

#### AVOID: Single Widget Class for All Views
```kotlin
// BAD - One giant class handling all widget types
class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Everything mixed together
            Column {
                // Configuration UI logic
                if (showConfig) {
                    ConfigurationDropdown()
                }
                
                // All view modes in one place
                when (currentViewMode) {
                    LIST -> TodoListContent()
                    GRID -> TodoGridContent()
                    KANBAN -> TodoKanbanContent()
                    // ... more views
                }
                
                // CRUD operations mixed with UI
                Button("Add") {
                    // Direct database access
                    database.insertTodo(newTodo)
                    // Direct widget update
                    updateWidget()
                }
                
                // Network operations in UI
                LaunchedEffect(Unit) {
                    // Blocking network call
                    val todos = api.getTodos().execute()
                    updateUI(todos)
                }
            }
        }
    }
}
```

#### AVOID: Tight Coupling to Main App
```kotlin
// BAD - Widget directly accessing main app components
class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Directly accessing MainActivity
        val mainActivity = context as MainActivity
        
        // Directly accessing main ViewModel
        val mainViewModel = ViewModelProvider(mainActivity)[TodoViewModel::class.java]
        
        // Directly accessing main database
        val mainDatabase = (context.application as TodoApp).database
        
        // Directly accessing main preferences
        val mainPrefs = context.getSharedPreferences("main_prefs", Context.MODE_PRIVATE)
        
        provideContent {
            Text("Todos: ${mainViewModel.todoCount.value}")
        }
    }
}
```

### Missing Abstraction Layers

#### AVOID: No Repository Pattern for Widgets
```kotlin
// BAD - Direct data access in widget
class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Direct database access
        val database = Room.databaseBuilder(context, TodoDatabase::class.java, "todos.db").build()
        val todoDao = database.todoDao()
        val todos = todoDao.getAll() // Blocking call
        
        // Direct API access
        val retrofit = Retrofit.Builder().baseUrl("https://api.todoapp.com").build()
        val api = retrofit.create(TodoApi::class.java)
        val remoteTodos = api.getTodos().execute().body() ?: emptyList()
        
        // Manual data merging
        val allTodos = (todos + remoteTodos).distinctBy { it.id }
        
        provideContent {
            LazyColumn {
                items(allTodos) { todo ->
                    TodoItem(todo)
                }
            }
        }
    }
}
```

---

## Configuration UI Mistakes

### Poor Configuration Experience

#### AVOID: Basic Configuration UI
```kotlin
// BAD - Minimal configuration options
class TodoWidgetConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Only basic options
        setContentView(R.layout.basic_config_layout)
        
        val titleInput = findViewById<EditText>(R.id.title_input)
        val saveButton = findViewById<Button>(R.id.save_button)
        
        saveButton.setOnClickListener {
            // Only saving title, no other options
            val title = titleInput.text.toString()
            saveWidgetConfig(title)
            finish()
        }
    }
}
```

#### AVOID: Missing Live Preview
```kotlin
// BAD - Configuration without preview
class TodoWidgetConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupConfigurationOptions()
        
        // No preview of how widget will look
        saveButton.setOnClickListener {
            saveConfiguration()
            // User has to go back to home screen to see changes
            finish()
        }
    }
    
    private fun setupConfigurationOptions() {
        // Static dropdowns without visual feedback
        val viewModeSpinner = findViewById<Spinner>(R.id.view_mode_spinner)
        val maxItemsSpinner = findViewById<Spinner>(R.id.max_items_spinner)
        
        // No preview updates when selection changes
        viewModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // No preview update - user has to imagine how it will look
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
```

### Inefficient Configuration Storage

#### AVOID: Multiple SharedPreferences Files
```kotlin
// BAD - Using separate files for each configuration aspect
class WidgetConfigManager {
    fun saveWidgetConfig(widgetId: Int, config: TodoWidgetConfig) {
        // Creating multiple files - inefficient
        val viewModePrefs = context.getSharedPreferences("widget_viewmode_$widgetId", Context.MODE_PRIVATE)
        val maxItemsPrefs = context.getSharedPreferences("widget_maxitems_$widgetId", Context.MODE_PRIVATE)
        val themePrefs = context.getSharedPreferences("widget_theme_$widgetId", Context.MODE_PRIVATE)
        val categoryPrefs = context.getSharedPreferences("widget_category_$widgetId", Context.MODE_PRIVATE)
        
        // Multiple file operations
        viewModePrefs.edit().putString("viewmode", config.viewMode.name).apply()
        maxItemsPrefs.edit().putInt("maxitems", config.maxItems).apply()
        themePrefs.edit().putString("theme", config.theme.name).apply()
        categoryPrefs.edit().putString("category", config.filterCategory).apply()
    }
    
    fun loadWidgetConfig(widgetId: Int): TodoWidgetConfig {
        // Multiple file reads
        val viewModePrefs = context.getSharedPreferences("widget_viewmode_$widgetId", Context.MODE_PRIVATE)
        val maxItemsPrefs = context.getSharedPreferences("widget_maxitems_$widgetId", Context.MODE_PRIVATE)
        val themePrefs = context.getSharedPreferences("widget_theme_$widgetId", Context.MODE_PRIVATE)
        val categoryPrefs = context.getSharedPreferences("widget_category_$widgetId", Context.MODE_PRIVATE)
        
        return TodoWidgetConfig(
            viewMode = TodoWidgetView.valueOf(viewModePrefs.getString("viewmode", "LIST")!!),
            maxItems = maxItemsPrefs.getInt("maxitems", 5),
            theme = TodoWidgetTheme.valueOf(themePrefs.getString("theme", "LIGHT")!!),
            filterCategory = categoryPrefs.getString("category", null)
        )
    }
}
```

---

## CRUD Operation Pitfalls

### Inefficient CRUD Operations

#### AVOID: Blocking Operations in Widget
```kotlin
// BAD - Blocking CRUD operations
class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column {
                Button(
                    text = "Add Todo",
                    onClick = actionRunCallback(object : ActionCallback {
                        override suspend fun onRun(
                            context: Context,
                            glanceId: GlanceId,
                            parameters: ActionParameters
                        ) {
                            // Blocking database operation
                            val database = Room.databaseBuilder(context, TodoDatabase::class.java, "todos.db").build()
                            val todoDao = database.todoDao()
                            
                            // Insert on main thread
                            val newTodo = TodoItem(title = "New Task", isCompleted = false)
                            todoDao.insert(newTodo)
                            
                            // Blocking network operation
                            val api = Retrofit.Builder().baseUrl("https://api.todoapp.com").build()
                                .create(TodoApi::class.java)
                            api.syncTodo(newTodo).execute()
                            
                            // Direct widget update without error handling
                            updateWidget(glanceId)
                        }
                    })
                )
            }
        }
    }
}
```

#### AVOID: Missing Error Handling
```kotlin
// BAD - CRUD operations without error handling
class AddTodoAction : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoText = parameters["todo_text"] ?: return
        
        // No error handling
        val todo = TodoItem(title = todoText, isCompleted = false)
        val result = todoRepository.addTodo(todo) // Could fail
        
        // No null/exception checking
        updateWidget(glanceId, result) // Might crash
        
        // No user feedback
    }
}

class DeleteTodoAction : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoId = parameters["todo_id"] ?: return
        
        // No validation
        todoRepository.deleteTodo(todoId) // Could throw exception
        
        // No feedback to user
        // No rollback on failure
    }
}
```

### Poor User Experience

#### AVOID: No Feedback for CRUD Operations
```kotlin
// BAD - No user feedback for operations
@Composable
fun TodoItemRow(todo: TodoItem) {
    Row {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = null // No visual feedback
        )
        
        Text(todo.title)
        
        Button("Delete") {
            // Silent deletion - no confirmation
            deleteTodo(todo.id)
        }
        
        Button("Edit") {
            // No loading state
            editTodo(todo.id)
        }
    }
}
```

#### AVOID: Complex CRUD in Widget UI
```kotlin
// BAD - Complex CRUD operations directly in widget UI
@Composable
fun ComplexTodoWidget() {
    var todos by remember { mutableStateOf<List<TodoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    Column {
        // Complex add form in widget
        var newTodoText by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("") }
        var selectedPriority by remember { mutableStateOf("MEDIUM") }
        var dueDate by remember { mutableStateOf("") }
        
        Row {
            TextField(value = newTodoText, onValueChange = { newTodoText = it })
            DropdownMenu(selectedCategory, categories) { selectedCategory = it }
            DropdownMenu(selectedPriority, priorities) { selectedPriority = it }
            TextField(value = dueDate, onValueChange = { dueDate = it })
        }
        
        Button("Add") {
            // Complex validation in UI
            if (newTodoText.isBlank()) {
                error = "Title cannot be empty"
                return@Button
            }
            
            if (dueDate.isNotBlank() && !isValidDate(dueDate)) {
                error = "Invalid date format"
                return@Button
            }
            
            isLoading = true
            
            // Complex creation logic in UI
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val todo = TodoItem(
                        title = newTodoText,
                        category = selectedCategory,
                        priority = TodoPriority.valueOf(selectedPriority),
                        dueDate = parseDate(dueDate),
                        createdAt = System.currentTimeMillis()
                    )
                    
                    val result = todoRepository.addTodo(todo)
                    
                    if (result.isSuccess) {
                        // Update local state
                        todos = todos + result.getOrNull()!!
                        newTodoText = ""
                        selectedCategory = ""
                        selectedPriority = "MEDIUM"
                        dueDate = ""
                    } else {
                        error = "Failed to add todo"
                    }
                } catch (e: Exception) {
                    error = "Error: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
        
        // Error display
        error?.let {
            Text(it, color = Color.Red)
        }
        
        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator()
        }
        
        // Todo list
        LazyColumn {
            items(todos) { todo ->
                ComplexTodoItem(todo)
            }
        }
    }
}
```

---

## Performance Killers

### Inefficient Data Loading

#### AVOID: Loading All Data in Widget
```kotlin
// BAD - Loading entire todo database in widget
class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Loading all todos - could be thousands
        val allTodos = todoRepository.getAllTodos() // Expensive operation
        
        // Processing all todos in widget
        val processedTodos = allTodos.map { todo ->
            // Complex calculation for each todo
            val stats = calculateTodoStats(todo)
            val formatted = formatTodoDisplay(todo, stats)
            TodoDisplayItem(todo, formatted, stats)
        }
        
        // No filtering or limiting
        provideContent {
            LazyColumn {
                items(processedTodos) { item ->
                    ComplexTodoItem(item)
                }
            }
        }
    }
    
    private fun calculateTodoStats(todo: TodoItem): TodoStats {
        // Heavy computation for each todo
        val completionRate = calculateCompletionRate(todo.id)
        val timeSpent = calculateTimeSpent(todo.id)
        val priorityScore = calculatePriorityScore(todo)
        val categoryStats = getCategoryStats(todo.category)
        
        return TodoStats(completionRate, timeSpent, priorityScore, categoryStats)
    }
}
```

#### AVOID: Frequent Unnecessary Updates
```kotlin
// BAD - Updating widget too frequently
class AggressiveWidgetUpdater {
    fun startAggressiveUpdates() {
        // Updating every 30 seconds - battery drain
        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            30, TimeUnit.SECONDS
        ).build()
        
        workManager.enqueuePeriodicWork("aggressive_updates", updateRequest)
        
        // Also updating on every data change
        todoRepository.observeTodoChanges().collect { change ->
            updateAllWidgets() // Updates all widgets for every single change
        }
        
        // Also updating on timer
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateAllWidgets()
            }
        }, 0, 10000) // Every 10 seconds
    }
}
```

### Memory Leaks

#### AVOID: Unbounded Data Storage
```kotlin
// BAD - Storing unlimited data in widget
class TodoWidget : GlanceAppWidget() {
    private val allTodos = mutableListOf<TodoItem>()
    private val todoHistory = mutableListOf<TodoHistoryEntry>()
    private val cachedStats = mutableMapOf<String, Any>()
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Adding all todos without limit
        val newTodos = todoRepository.getAllTodos()
        allTodos.addAll(newTodos) // Grows indefinitely
        
        // Adding to history without cleanup
        todoHistory.add(TodoHistoryEntry(System.currentTimeMillis(), newTodos))
        
        // Caching everything without cleanup
        cachedStats["total_todos"] = allTodos.size
        cachedStats["completed_todos"] = allTodos.count { it.isCompleted }
        cachedStats["categories"] = allTodos.groupBy { it.category }
        cachedStats["priority_distribution"] = allTodos.groupBy { it.priority }
        
        provideContent {
            LazyColumn {
                items(allTodos) { // Could be thousands of items
                    TodoItem(it)
                }
            }
        }
    }
}
```

#### AVOID: Leaking Context in Callbacks
```kotlin
// BAD - Leaking context in long-running operations
class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Button(
                text = "Sync Todos",
                onClick = actionRunCallback(object : ActionCallback {
                    override suspend fun onRun(
                        context: Context,
                        glanceId: GlanceId,
                        parameters: ActionParameters
                    ) {
                        // Storing context in long-running operation
                        val appContext = context.applicationContext
                        
                        // Long-running operation that holds context reference
                        CoroutineScope(Dispatchers.IO).launch {
                            // This could run for minutes, leaking context
                            val todos = syncWithRemoteServer(appContext)
                            
                            // More long operations
                            processTodos(appContext, todos)
                            updateDatabase(appContext, todos)
                            updateWidgets(appContext, todos)
                            
                            // Context leaked throughout the operation
                        }
                    }
                })
            )
        }
    }
}
```

---

## Synchronization Issues

### Race Conditions

#### AVOID: Concurrent Update Conflicts
```kotlin
// BAD - Race conditions in widget updates
class TodoWidgetManager {
    private val widgetStates = mutableMapOf<String, WidgetState>()
    
    fun updateWidget(widgetId: String, todos: List<TodoItem>) {
        // Multiple concurrent updates can overwrite each other
        CoroutineScope(Dispatchers.IO).launch {
            val currentState = widgetStates[widgetId] ?: WidgetState()
            val newState = currentState.copy(todos = todos)
            widgetStates[widgetId] = newState
            
            // Another coroutine might modify state between these operations
            updateWidgetUI(widgetId, newState)
        }
        
        // Another concurrent update
        CoroutineScope(Dispatchers.IO).launch {
            val currentState = widgetStates[widgetId] ?: WidgetState()
            val newState = currentState.copy(lastUpdate = System.currentTimeMillis())
            widgetStates[widgetId] = newState // Overwrites previous update
            
            updateWidgetUI(widgetId, newState) // Overwrites previous UI update
        }
    }
}
```

#### AVOID: Missing Conflict Resolution
```kotlin
// BAD - No conflict resolution for concurrent edits
class TodoWidgetRepository {
    suspend fun updateTodo(todo: TodoItem): Result<TodoItem> {
        // No version checking or conflict resolution
        val localTodo = todoDao.getTodoById(todo.id)
        
        // Direct overwrite - could lose changes from other sources
        todoDao.updateTodo(todo)
        
        // Sync to remote without conflict checking
        val result = api.updateTodo(todo)
        
        return if (result.isSuccessful) {
            Result.success(todo)
        } else {
            // No rollback or conflict resolution
            Result.failure(Exception("Update failed"))
        }
    }
    
    suspend fun addTodo(todo: TodoItem): Result<TodoItem> {
        // No duplicate checking
        val existingTodo = todoDao.getTodoByTitle(todo.title)
        
        // Could create duplicates
        val newTodo = todo.copy(id = UUID.randomUUID().toString())
        todoDao.insertTodo(newTodo)
        
        return Result.success(newTodo)
    }
}
```

### Offline Sync Issues

#### AVOID: Poor Offline Support
```kotlin
// BAD - No offline support
class OnlineOnlyTodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            // Requires network connection
            val todos = api.getTodos().execute().body() ?: emptyList()
            
            provideContent {
                LazyColumn {
                    items(todos) { todo ->
                        TodoItem(todo)
                    }
                }
            }
        } catch (e: Exception) {
            // No fallback - widget shows error
            provideContent {
                Text("Error loading todos")
            }
        }
    }
}

class TodoWidgetRepository {
    suspend fun getTodos(): List<TodoItem> {
        // Always tries to get from network
        return api.getTodos().execute().body() ?: emptyList()
        // No local cache or fallback
    }
    
    suspend fun addTodo(todo: TodoItem): Result<TodoItem> {
        // Direct network call - fails offline
        return try {
            val result = api.addTodo(todo).execute()
            if (result.isSuccessful) {
                Result.success(result.body()!!)
            } else {
                Result.failure(Exception("Failed to add todo"))
            }
        } catch (e: Exception) {
            // No local storage for offline
            Result.failure(e)
        }
    }
}
```

---

## State Management Anti-Patterns

### Inefficient State Updates

#### AVOID: Updating Entire State for Small Changes
```kotlin
// BAD - Updating entire widget state for small changes
class TodoWidgetStateManager {
    suspend fun updateTodoCompletion(todoId: String, isCompleted: Boolean) {
        // Loading entire state
        val currentConfig = loadWidgetConfig(widgetId)
        val currentTodos = loadAllTodos(widgetId)
        val currentStats = loadWidgetStats(widgetId)
        
        // Updating entire state for one todo change
        val updatedTodos = currentTodos.map { todo ->
            if (todo.id == todoId) {
                todo.copy(isCompleted = isCompleted)
            } else {
                todo
            }
        }
        
        // Recalculating everything
        val updatedStats = calculateStats(updatedTodos)
        
        // Saving entire state
        saveWidgetConfig(widgetId, currentConfig)
        saveAllTodos(widgetId, updatedTodos)
        saveWidgetStats(widgetId, updatedStats)
        
        // Updating entire widget
        updateEntireWidget(widgetId)
    }
}
```

#### AVOID: Missing State Persistence
```kotlin
// BAD - No state persistence
class TodoWidget : GlanceAppWidget() {
    private var currentTodos = mutableListOf<TodoItem>()
    private var currentConfig = TodoWidgetConfig()
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Loading state every time
        currentTodos = todoRepository.getTodos().toMutableList()
        currentConfig = loadWidgetConfig()
        
        provideContent {
            TodoWidgetContent(currentTodos, currentConfig)
        }
    }
    
    // State lost on process death
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // No state restoration
        currentTodos.clear() // State lost
        currentConfig = TodoWidgetConfig() // Reset to default
    }
}
```

### Poor Configuration Management

#### AVOID: Hardcoded Configuration Values
```kotlin
// BAD - Hardcoded configuration
class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Hardcoded values - no customization
        val maxItems = 5
        val viewMode = TodoWidgetView.LIST_ONLY
        val showCompleted = false
        val theme = TodoWidgetTheme.LIGHT
        val refreshInterval = 30
        
        val todos = todoRepository.getTodos()
            .filter { !it.isCompleted || showCompleted }
            .take(maxItems)
        
        provideContent {
            when (viewMode) {
                TodoWidgetView.LIST_ONLY -> TodoListContent(todos)
                // No other view modes supported
            }
        }
    }
}
```

---

## Widget UI Anti-Patterns

### Poor User Interface Design

#### AVOID: Cluttered Widget UI
```kotlin
// BAD - Too much information in small widget space
@Composable
fun ClutteredTodoWidget() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(2.dp) // Too much content, too little padding
    ) {
        // Header with multiple elements
        Row {
            Text("My Tasks") // Title
            Spacer(GlanceModifier.width(2.dp))
            Button("Config") { } // Config button
            Spacer(GlanceModifier.width(2.dp))
            Button("Refresh") { } // Refresh button
            Spacer(GlanceModifier.width(2.dp))
            Text("5/10") // Stats
        }
        
        // Quick add form in widget
        Row {
            TextField(value = "", onValueChange = { }, modifier = GlanceModifier.width(80.dp))
            Button("Add") { }
        }
        
        // Todo list with too much information
        LazyColumn {
            items(todos) { todo ->
                Row {
                    Checkbox(checked = todo.isCompleted, onCheckedChange = null)
                    Column {
                        Text(todo.title)
                        Text(todo.description) // Too much text
                        Text("Due: ${todo.dueDate}") // Date
                        Text("Priority: ${todo.priority}") // Priority
                        Text("Category: ${todo.category}") // Category
                    }
                    Button("Edit") { }
                    Button("Delete") { }
                }
            }
        }
        
        // Footer with more information
        Row {
            Text("Last sync: 2 min ago")
            Button("View All") { }
        }
    }
}
```

#### AVOID: Inconsistent Design Patterns
```kotlin
// BAD - Inconsistent design across different views
@Composable
fun InconsistentTodoWidget() {
    Column {
        // Different styling for similar elements
        when (currentViewMode) {
            LIST -> {
                // List view with cards
                LazyColumn {
                    items(todos) { todo ->
                        Card( // Cards for list items
                            modifier = GlanceModifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Text(todo.title)
                        }
                    }
                }
            }
            GRID -> {
                // Grid view without cards
                LazyVerticalGrid {
                    items(todos) { todo ->
                        Column( // No cards for grid items
                            modifier = GlanceModifier.padding(4.dp)
                        ) {
                            Text(todo.title)
                        }
                    }
                }
            }
            KANBAN -> {
                // Kanban view with different styling
                Row {
                    Column {
                        Text("To Do")
                        todos.filter { !it.isCompleted }.forEach { todo ->
                            Box( // Boxes for kanban items
                                modifier = GlanceModifier.background(Color.Blue)
                            ) {
                                Text(todo.title)
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### Poor Accessibility

#### AVOID: Missing Accessibility Support
```kotlin
// BAD - No accessibility support
@Composable
fun InaccessibleTodoWidget() {
    Column {
        // No content descriptions
        Text("$12,345.67") // Screen reader reads "1234567" without context
        
        // Unlabeled buttons
        Button("") { // Empty button text
            onClick = actionToggleTodo()
        }
        
        // Poor color contrast
        Text(
            text = "Important Task",
            color = Color(0xFFEEEEEE) // Light gray on white background
        )
        
        // No announcements for state changes
        Button("Toggle") {
            onClick = actionToggleTodo() // No accessibility announcement
        }
        
        // No semantic information
        LazyColumn {
            items(todos) { todo ->
                Row {
                    Checkbox(todo.isCompleted, null)
                    Text(todo.title)
                }
                // No grouping or semantic structure
            }
        }
    }
}
```

---

## Testing and Debugging Issues

### Poor Test Coverage

#### AVOID: Testing Only Happy Paths
```kotlin
// BAD - Only testing normal scenarios
@Test
fun `test widget happy path`() {
    val widget = TodoWidget()
    val normalTodos = listOf(
        TodoItem("Task 1", false),
        TodoItem("Task 2", true)
    )
    
    widget.updateTodos(normalTodos)
    
    val content = widget.getWidgetContent()
    assertTrue(content.contains("Task 1"))
    assertTrue(content.contains("Task 2"))
    
    // Missing tests for:
    // - Empty todo list
    // - Network failures
    // - Invalid configuration
    // - Large datasets
    // - Concurrent updates
    // - Error states
}
```

#### AVOID: Mocking Everything
```kotlin
// BAD - Over-mocking in tests
@Test
fun `test widget with excessive mocking`() {
    val mockContext = mock<Context>()
    val mockGlanceId = mock<GlanceId>()
    val mockRepository = mock<TodoWidgetRepository>()
    val mockApi = mock<TodoApiService>()
    val mockDatabase = mock<TodoDatabase>()
    val mockDao = mock<TodoDao>()
    val mockPreferences = mock<SharedPreferences>()
    val mockWorkManager = mock<WorkManager>()
    
    // Setting up dozens of mocks
    whenever(mockRepository.getTodos()).thenReturn(flowOf(mockTodos))
    whenever(mockApi.getTodos()).thenReturn(Response.success(mockTodos))
    whenever(mockDatabase.todoDao()).thenReturn(mockDao)
    whenever(mockDao.getAll()).thenReturn(mockTodoEntities)
    whenever(mockPreferences.getString("config", null)).thenReturn(mockConfigJson)
    
    // Test becomes about testing mocks, not real behavior
    val widget = TodoWidget(mockRepository, mockApi, mockDatabase, mockPreferences, mockWorkManager)
    widget.provideGlance(mockContext, mockGlanceId)
    
    verify(mockRepository).getTodos()
    verify(mockApi).getTodos()
    verify(mockDao).getAll()
    verify(mockPreferences).getString("config", null)
}
```

### Debugging Anti-Patterns

#### AVOID: Poor Logging
```kotlin
// BAD - Insufficient logging
class TodoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            val todos = todoRepository.getTodos()
            
            // No logging for debugging
            provideContent {
                LazyColumn {
                    items(todos) { todo ->
                        TodoItem(todo)
                    }
                }
            }
        } catch (e: Exception) {
            // No error logging
            provideContent {
                Text("Error")
            }
        }
    }
}

class AddTodoAction : ActionCallback {
    override suspend fun onRun(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoText = parameters["todo_text"]
        
        // No logging for debugging
        val result = todoRepository.addTodo(TodoItem(todoText))
        
        if (result.isSuccess) {
            updateWidget(glanceId)
        }
        // No error logging or debugging information
    }
}
```

---

## Summary

Avoiding these bad practices will help you prevent:

- **Performance issues** from inefficient data loading and memory leaks
- **Poor user experience** from cluttered UI and missing feedback
- **Synchronization problems** from race conditions and missing conflict resolution
- **Battery drain** from excessive updates and background work
- **Crashes and instability** from missing error handling and poor state management
- **Maintenance nightmares** from tightly coupled and poorly structured code
- **Accessibility violations** that exclude users with disabilities
- **Testing difficulties** from anti-patterns that make code hard to test

Remember that good todo widgets should be:

- **Lightweight** and responsive
- **User-friendly** with intuitive configuration
- **Efficient** with smart data loading and caching
- **Reliable** with proper error handling and offline support
- **Accessible** to all users
- **Maintainable** with clean architecture
- **Testable** with proper separation of concerns

Following best practices and avoiding these anti-patterns will ensure high-performance todo widgets that provide excellent user experiences while maintaining system efficiency.
