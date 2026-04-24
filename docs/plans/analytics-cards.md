# Analytics Cards Implementation Plan

## Overview
This document outlines a comprehensive plan to implement three new analytics cards for the Analytics screen that can also be reused on the Dashboard screen. These cards will follow the **generic card architecture** that uses pure suspend functions with LaunchedEffect + remember pattern, as described in `card-architecture-roadmap.md`.

> **Note:** The card system now uses a generic pattern that eliminates the need for shared StateFlows. See the "Creating New Custom Cards" section in `card-architecture-roadmap.md` for the complete guide on adding new cards.

## New Cards to Implement

### 1. Category Breakdown Card (Pie Wheel Chart)
- **Purpose**: Display category-wise breakdown of net balance using an interactive pie wheel chart
- **Visualization**: Interactive pie/donut chart with category segments
- **Interactions**: Tap on segments to see details, legend with colors
- **Data**: Category-wise net balance (income - expense per category)

### 2. Timeline Trajectory Card (Multi-Line Chart)
- **Purpose**: Display timeline-wise balance, expense, and income trajectories
- **Visualization**: Multi-line chart with 3 distinct colored lines:
  - Balance trajectory (blue/primary)
  - Expense trajectory (red/error)
  - Income trajectory (green/success)
- **Interactions**: Tap on data points for details, toggle line visibility
- **Data**: Time-series data for balance, expenses, and income

### 3. Category Expense Treemap Card
- **Purpose**: Display category-wise expense distribution as a graphical treemap
- **Visualization**: Grand perspective-style treemap with rectangular areas proportional to expense amounts
- **Interactions**: Tap on rectangles for category details, color-coded by category
- **Data**: Category-wise expense totals

## Architecture Design

### Decoupled Configuration Architecture

Following the established pattern in the codebase, each screen maintains its own independent card configuration stored in DataStore. This allows for multiple instances of the same card type with different settings across different screens.

**Existing Screen-Specific Configs:**
- `DashboardCardsConfig` - Dashboard screen cards
- `AccountDetailCardsConfig` - Account detail screen cards  
- `AccountsViewCardsConfig` - Accounts view screen cards

**New Screen-Specific Config:**
- `AnalyticsCardsConfig` - Analytics screen cards

**Configuration Storage Pattern:**
```kotlin
// Each screen has its own DataStore key and config class
val DASHBOARD_CARDS_CONFIG_KEY = stringPreferencesKey("dashboard_cards_config")
val ACCOUNT_DETAIL_CARDS_CONFIG_KEY = stringPreferencesKey("account_detail_cards_config")
val ACCOUNTS_VIEW_CARDS_CONFIG_KEY = stringPreferencesKey("accounts_view_cards_config")
val ANALYTICS_CARDS_CONFIG_KEY = stringPreferencesKey("analytics_cards_config")

// Each config wraps a list of CardConfig (sealed class)
@Serializable
data class DashboardCardsConfig(val cards: List<CardConfig> = emptyList())
@Serializable
data class AccountDetailCardsConfig(val cards: List<CardConfig> = emptyList())
@Serializable
data class AccountsViewCardsConfig(val cards: List<CardConfig> = emptyList())
@Serializable
data class AnalyticsCardsConfig(val cards: List<CardConfig> = emptyList())
```

**Benefits of Decoupled Configs:**
- **Independent Customization**: Each screen can have different card instances with unique settings
- **No Cross-Screen Pollution**: Changes to one screen's cards don't affect others
- **Multiple Instances**: Same card type can appear multiple times on different screens with different configurations
- **Scope Flexibility**: Cards can be configured for GLOBAL or ACCOUNT scope independently per screen
- **Isolation**: Adding/removing cards on one screen doesn't impact others

**Example Use Case:**
- Dashboard: Category Breakdown card with `timelineRange = Last30Days` and `showLegend = true`
- Analytics: Category Breakdown card with `timelineRange = AllTime` and `showLegend = false`
- Both cards exist independently with their own settings

### Layer Structure
```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                      │
│  ┌──────────────────┐  ┌──────────────────────────────┐ │
│  │ AnalyticsScreen  │  │    DashboardScreen            │ │
│  │                  │  │                              │ │
│  │ CardReorderable  │  │    CardReorderableList       │ │
│  │     List         │  │                              │ │
│  └──────────────────┘  └──────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                    UI Components Layer                     │
│  ┌──────────────────────────────────────────────────┐   │
│  │  CategoryBreakdownCard (Pie Wheel)               │   │
│  │  TimelineTrajectoryCard (Multi-Line Chart)       │   │
│  │  CategoryExpenseTreemapCard (Treemap)            │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                    ViewModel Layer                        │
│  ┌──────────────────────────────────────────────────┐   │
│  │  AnalyticsViewModel                               │   │
│  │  - Manages card configs                           │   │
│  │  - Delegates to CardComputationService           │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                  Domain Layer (Service)                    │
│  ┌──────────────────────────────────────────────────┐   │
│  │  CardComputationService                          │   │
│  │  - computeCategoryBreakdown()                     │   │
│  │  - computeTimelineTrajectory()                    │   │
│  │  - computeCategoryExpenseTreemap()                │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                  Domain Layer (Use Cases)                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │  FinancialUseCases                                │   │
│  │  - getCategoryAggregation()                        │   │
│  │  - getTimelineData()                              │   │
│  │  - getCategoryExpenseData()                       │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## Implementation Phases

### Phase 1: Domain Layer (Week 1)

#### 1.1 Create Data Models
**File**: `domain/model/analytics/AnalyticsData.kt`

```kotlin
// Category breakdown data for pie chart
@Serializable
data class CategoryBreakdownData(
    val category: String,
    val categoryId: Long?,
    val netBalance: BigDecimal,
    val income: BigDecimal,
    val expense: BigDecimal,
    val colorHex: Int,
    val percentage: Float
)

// Timeline trajectory data for multi-line chart
@Serializable
data class TimelineTrajectoryData(
    val timestamp: Long,
    val balance: BigDecimal,
    val expense: BigDecimal,
    val income: BigDecimal
)

// Category expense data for treemap
@Serializable
data class CategoryExpenseTreemapData(
    val category: String,
    val categoryId: Long?,
    val expense: BigDecimal,
    val colorHex: Int,
    val percentage: Float,
    val children: List<CategoryExpenseTreemapData>? = null // For nested categories
)
```

#### 1.2 Extend CardComputationService
**File**: `domain/service/CardComputationService.kt`

Add new methods to the interface:

```kotlin
interface CardComputationService {
    // ... existing methods ...

    /**
     * Computes category breakdown for pie chart card.
     * Groups transactions by category and calculates net balance per category.
     */
    suspend fun computeCategoryBreakdown(
        config: CategoryBreakdownCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CategoryBreakdownData>

    /**
     * Computes timeline trajectory for multi-line chart card.
     * Generates time-series data for balance, expense, and income.
     */
    suspend fun computeTimelineTrajectory(
        config: TimelineTrajectoryCardConfig,
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<TimelineTrajectoryData>

    /**
     * Computes category expense treemap data.
     * Groups expenses by category and calculates proportions.
     */
    suspend fun computeCategoryExpenseTreemap(
        config: CategoryExpenseTreemapCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CategoryExpenseTreemapData>
}
```

Implement in `CardComputationServiceImpl`:

```kotlin
override suspend fun computeCategoryBreakdown(
    config: CategoryBreakdownCardConfig,
    transactions: List<Transaction>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): List<CategoryBreakdownData> {
    val scope = if (config.accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
    val filteredTransactions = filterTransactionsByScope(transactions, scope, config.accountId)
    
    // Group by category
    val categoryMap = filteredTransactions.groupBy { it.category }
    
    val totalNetBalance = categoryMap.values.sumOf { txs ->
        txs.sumOf { tx ->
            val amountInBase = convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
            if (tx.transactionType == "income") amountInBase else -amountInBase
        }
    }
    
    return categoryMap.map { (category, txs) ->
        val income = txs.filter { it.transactionType == "income" }.sumOf { tx ->
            convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
        }
        val expense = txs.filter { it.transactionType == "expense" }.sumOf { tx ->
            convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
        }
        val netBalance = income - expense
        val percentage = if (totalNetBalance.abs() > BigDecimal.ZERO) {
            (netBalance.abs() / totalNetBalance.abs()).toFloat()
        } else 0f
        
        // Get category color from database or default
        val categoryEntity = categoryDao.getCategoryByName(category)
        val colorHex = categoryEntity?.colorHex ?: Color.Gray.hashCode()
        
        CategoryBreakdownData(
            category = category,
            categoryId = categoryEntity?.id,
            netBalance = netBalance,
            income = income,
            expense = expense,
            colorHex = colorHex,
            percentage = percentage
        )
    }.sortedByDescending { it.netBalance.abs() }
}

override suspend fun computeTimelineTrajectory(
    config: TimelineTrajectoryCardConfig,
    transactions: List<Transaction>,
    accounts: List<Account>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): List<TimelineTrajectoryData> {
    val scope = if (config.accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
    val filteredTransactions = filterTransactionsByScope(transactions, scope, config.accountId)
    
    // Determine time buckets based on timelineRange
    val timeBuckets = getTimeBuckets(config.timelineRange)
    
    return timeBuckets.map { bucket ->
        val bucketTransactions = filteredTransactions.filter { tx ->
            tx.timestamp >= bucket.start && tx.timestamp < bucket.end
        }
        
        val income = bucketTransactions.filter { it.transactionType == "income" }.sumOf { tx ->
            convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
        }
        val expense = bucketTransactions.filter { it.transactionType == "expense" }.sumOf { tx ->
            convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
        }
        
        // Calculate running balance
        val priorTransactions = filteredTransactions.filter { it.timestamp < bucket.start }
        val priorBalance = financialUseCases.calculateNetWorth(
            accounts = if (scope == CardScope.ACCOUNT) accounts.filter { it.id == config.accountId } else accounts,
            allTransactions = priorTransactions,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision,
            timelineRange = config.timelineRange
        )
        
        val balance = priorBalance + income - expense
        
        TimelineTrajectoryData(
            timestamp = bucket.start,
            balance = balance,
            expense = expense,
            income = income
        )
    }
}

override suspend fun computeCategoryExpenseTreemap(
    config: CategoryExpenseTreemapCardConfig,
    transactions: List<Transaction>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): List<CategoryExpenseTreemapData> {
    val scope = if (config.accountId != null) CardScope.ACCOUNT else CardScope.GLOBAL
    val filteredTransactions = filterTransactionsByScope(transactions, scope, config.accountId)
    
    // Filter only expenses
    val expenseTransactions = filteredTransactions.filter { it.transactionType == "expense" }
    
    // Group by category
    val categoryMap = expenseTransactions.groupBy { it.category }
    
    val totalExpense = categoryMap.values.sumOf { txs ->
        txs.sumOf { tx ->
            convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
        }
    }
    
    return categoryMap.map { (category, txs) ->
        val expense = txs.sumOf { tx ->
            convertAmount(tx.amount, tx.currency, baseCurrency, exchangeRates, precision)
        }
        val percentage = if (totalExpense > BigDecimal.ZERO) {
            (expense / totalExpense).toFloat()
        } else 0f
        
        val categoryEntity = categoryDao.getCategoryByName(category)
        val colorHex = categoryEntity?.colorHex ?: Color.Gray.hashCode()
        
        CategoryExpenseTreemapData(
            category = category,
            categoryId = categoryEntity?.id,
            expense = expense,
            colorHex = colorHex,
            percentage = percentage
        )
    }.sortedByDescending { it.expense }
}
```

#### 1.3 Add Card Configurations
**File**: `domain/model/settings/CardConfig.kt`

Add new card config classes:

```kotlin
/**
 * Configuration for Category Breakdown card (Pie Wheel Chart)
 */
@Serializable
@SerialName("category_breakdown")
data class CategoryBreakdownCardConfig(
    override val cardId: String,
    override val title: String = "CATEGORY BREAKDOWN",
    override val isEnabled: Boolean = true,
    override val order: Int = 5,
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val showLabels: Boolean = true,
    val showLegend: Boolean = true,
    val accountId: String? = null // For account scope
) : CardConfig()

/**
 * Configuration for Timeline Trajectory card (Multi-Line Chart)
 */
@Serializable
@SerialName("timeline_trajectory")
data class TimelineTrajectoryCardConfig(
    override val cardId: String,
    override val title: String = "TIMELINE TRAJECTORY",
    override val isEnabled: Boolean = true,
    override val order: Int = 6,
    val timelineRange: TimelineRange = TimelineRange.Last30Days,
    val showBalance: Boolean = true,
    val showExpense: Boolean = true,
    val showIncome: Boolean = true,
    val accountId: String? = null // For account scope
) : CardConfig()

/**
 * Configuration for Category Expense Treemap card
 */
@Serializable
@SerialName("category_expense_treemap")
data class CategoryExpenseTreemapCardConfig(
    override val cardId: String,
    override val title: String = "CATEGORY EXPENSES",
    override val isEnabled: Boolean = true,
    override val order: Int = 7,
    val timelineRange: TimelineRange = TimelineRange.Last30Days,
    val showLabels: Boolean = true,
    val accountId: String? = null // For account scope
) : CardConfig()
```

#### 1.4 Update CardType Enum and Screen Context
**File**: `ui/viewmodel/CardType.kt`

First, add ANALYTICS to the CardScreenContext enum:

```kotlin
enum class CardScreenContext {
    DASHBOARD,
    ACCOUNTS_VIEW,
    ACCOUNT_DETAIL,
    ANALYTICS  // NEW
}
```

Then add new card types:

```kotlin
enum class UnifiedCardType(
    val displayName: String,
    val serialName: String,
    val screenContext: CardScreenContext
) {
    // ... existing cards ...

    // Dashboard cards (new analytics cards available for dashboard)
    DASHBOARD_CATEGORY_BREAKDOWN("Category Breakdown", "category_breakdown", CardScreenContext.DASHBOARD),
    DASHBOARD_TIMELINE_TRAJECTORY("Timeline Trajectory", "timeline_trajectory", CardScreenContext.DASHBOARD),
    DASHBOARD_CATEGORY_EXPENSE_TREEMAP("Category Expenses", "category_expense_treemap", CardScreenContext.DASHBOARD),

    // Analytics cards (primary screen for these cards)
    ANALYTICS_CATEGORY_BREAKDOWN("Category Breakdown", "category_breakdown", CardScreenContext.ANALYTICS),
    ANALYTICS_TIMELINE_TRAJECTORY("Timeline Trajectory", "timeline_trajectory", CardScreenContext.ANALYTICS),
    ANALYTICS_CATEGORY_EXPENSE_TREEMAP("Category Expenses", "category_expense_treemap", CardScreenContext.ANALYTICS);

    companion object {
        fun fromSerialName(serialName: String, screenContext: CardScreenContext): UnifiedCardType? {
            return entries.find { it.serialName == serialName && it.screenContext == screenContext }
        }

        fun getAvailableForScreen(screenContext: CardScreenContext): List<UnifiedCardType> {
            return entries.filter { it.screenContext == screenContext }
        }
    }
}
```

**Note**: The same card types are available for both DASHBOARD and ANALYTICS contexts, allowing the same card to be added to either screen with independent configurations.

### Phase 2: UI Components (Week 2)

#### 2.1 Reusable Timeline Filter Component

The new analytics cards will use the existing `TimelineFilterBar` component from `AeonTimelineFilter.kt`, ensuring consistency with dashboard cards.

**Existing Component**: `ui/components/AeonTimelineFilter.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineFilterBar(
    currentRange: TimelineRange,
    onRangeSelected: (TimelineRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // Primary Selection (Segmented Control)
        AeonSegmentedControl(
            options = listOf(
                AeonTimelineOption("All", Icons.Default.History),
                AeonTimelineOption("This", Icons.Default.CalendarToday),
                AeonTimelineOption("Last", Icons.Default.Schedule),
                AeonTimelineOption("Custom", Icons.Default.DateRange)
            ),
            selectedOption = when (currentRange) {
                is TimelineRange.AllTime -> 0
                is TimelineRange.ThisPeriod -> 1
                is TimelineRange.LastPeriod -> 2
                is TimelineRange.CustomRange -> 3
            },
            onOptionSelected = { index: Int ->
                when (index) {
                    0 -> onRangeSelected(TimelineRange.AllTime)
                    1 -> onRangeSelected(TimelineRange.ThisPeriod(ThisPeriodType.MONTH))
                    2 -> onRangeSelected(TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS))
                    3 -> showDatePicker = true
                }
            },
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Dropdown for This/Last
        AnimatedVisibility(
            visible = currentRange is TimelineRange.ThisPeriod || 
                       currentRange is TimelineRange.LastPeriod || 
                       currentRange is TimelineRange.CustomRange,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                when (currentRange) {
                    is TimelineRange.ThisPeriod -> {
                        AeonDropdown(
                            label = "Select Period",
                            options = ThisPeriodType.entries.toList(),
                            selectedOption = currentRange.period,
                            onOptionSelected = { period: ThisPeriodType -> 
                                onRangeSelected(TimelineRange.ThisPeriod(period)) 
                            },
                            labelProvider = { it.label }
                        )
                    }
                    is TimelineRange.LastPeriod -> {
                        AeonDropdown(
                            label = "Select Range",
                            options = LastPeriodType.entries.toList(),
                            selectedOption = currentRange.period,
                            onOptionSelected = { period: LastPeriodType -> 
                                onRangeSelected(TimelineRange.LastPeriod(period)) 
                            },
                            labelProvider = { it.label }
                        )
                    }
                    is TimelineRange.CustomRange -> {
                        // Custom range display with edit button
                        OutlinedCard(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "CUSTOM RANGE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "${formatter.format(Date(currentRange.start))} - ${formatter.format(Date(currentRange.end))}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    if (showDatePicker) {
        AeonDateRangePicker(
            initialStart = (currentRange as? TimelineRange.CustomRange)?.start,
            initialEnd = (currentRange as? TimelineRange.CustomRange)?.end,
            onDismiss = { showDatePicker = false },
            onConfirmed = { start: Long, end: Long ->
                onRangeSelected(TimelineRange.CustomRange(start, end))
                showDatePicker = false
            }
        )
    }
}
```

**Integration Pattern for Analytics Cards:**

Each analytics card will include the `TimelineFilterBar` in its settings section:

```kotlin
@Composable
fun CategoryBreakdownCard(
    config: CategoryBreakdownCardConfig,
    data: List<CategoryBreakdownData>,
    modifier: Modifier = Modifier,
    onUpdate: (CategoryBreakdownCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedTimelineRange by remember { mutableStateOf(config.timelineRange) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with settings button
            CardHeader(
                title = config.title,
                onSettingsClick = { showSettings = true },
                onDeleteClick = onDelete
            )

            // Timeline Filter (using reusable component)
            TimelineFilterBar(
                currentRange = selectedTimelineRange,
                onRangeSelected = { newRange ->
                    selectedTimelineRange = newRange
                    onUpdate(config.copy(timelineRange = newRange))
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Pie Chart
            CategoryBreakdownPieChart(
                data = data,
                modifier = Modifier.fillMaxWidth()
            )

            // Legend
            if (config.showLegend) {
                CategoryLegend(data = data)
            }
        }
    }

    if (showSettings) {
        CardSettingsDialog(
            onDismiss = { showSettings = false },
            config = config,
            onUpdate = onUpdate
        )
    }
}
```

**Benefits of Reusing TimelineFilterBar:**
- **Consistency**: Same timeline selection UX across all screens
- **Maintainability**: Single source of truth for timeline logic
- **Feature Parity**: All timeline features (This/Last/Custom) available immediately
- **User Familiarity**: Users see the same interface regardless of screen
- **Reduced Code**: No need to reimplement timeline selection logic

#### 2.2 Create Chart Components using Vico

**File**: `ui/components/charts/CategoryBreakdownPieChart.kt`

```kotlin
@Composable
fun CategoryBreakdownPieChart(
    data: List<CategoryBreakdownData>,
    modifier: Modifier = Modifier,
    onSegmentClick: (CategoryBreakdownData) -> Unit = {}
) {
    val pieChartData = remember(data) {
        pieChartData(
            slices = data.map { item ->
                pieSlice(
                    slice = item.netBalance.abs().toFloat(),
                    color = Color(item.colorHex)
                )
            }
        )
    }

    Chart(
        chart = pieChart(
            pieChartData,
            sliceLabel = { slice, _ ->
                val index = pieChartData.slices.indexOf(slice)
                data.getOrNull(index)?.category ?: ""
            }
        ),
        chartProjection = pieChartProjection(
            sliceLabelSpacing = 12.dp,
            startAngle = 270f
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}
```

**File**: `ui/components/charts/TimelineTrajectoryLineChart.kt`

```kotlin
@Composable
fun TimelineTrajectoryLineChart(
    data: List<TimelineTrajectoryData>,
    modifier: Modifier = Modifier,
    showBalance: Boolean = true,
    showExpense: Boolean = true,
    showIncome: Boolean = true,
    onDataPointClick: (TimelineTrajectoryData) -> Unit = {}
) {
    val balanceLine = remember(data) {
        lineChartData(
            lines = listOf(
                lineSeries(
                    points = data.map { it.balance.toDouble() to it.timestamp.toDouble() }
                )
            )
        )
    }

    Chart(
        chart = lineChart(
            lineChartData,
            lineSpec = listOf(
                lineSpec(color = MaterialTheme.colorScheme.primary)
            )
        ),
        chartProjection = lineChartProjection(
            padding = lineChartPadding(16.dp),
            xAxis = lineChartXAxis(),
            yAxis = lineChartYAxis()
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}
```

**File**: `ui/components/charts/CategoryExpenseTreemap.kt`

```kotlin
@Composable
fun CategoryExpenseTreemap(
    data: List<CategoryExpenseTreemapData>,
    modifier: Modifier = Modifier,
    onRectClick: (CategoryExpenseTreemapData) -> Unit = {}
) {
    // Custom treemap implementation using Box with weights
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        TreemapLayout(
            data = data,
            modifier = Modifier.fillMaxSize(),
            onItemClick = onRectClick
        ) { item, weight ->
            TreemapRect(
                data = item,
                weight = weight,
                onClick = { onRectClick(item) }
            )
        }
    }
}

@Composable
private fun TreemapRect(
    data: CategoryExpenseTreemapData,
    weight: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() },
        color = Color(data.colorHex).copy(alpha = 0.8f)
    ) {
        Box(
            modifier = Modifier.padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = data.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(data.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}
```

#### 2.3 Create Card Components with TimelineFilterBar Integration

**File**: `ui/analytics/components/CategoryBreakdownCard.kt`

```kotlin
@Composable
fun CategoryBreakdownCard(
    config: CategoryBreakdownCardConfig,
    data: List<CategoryBreakdownData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (CategoryBreakdownCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedTimelineRange by remember { mutableStateOf(config.timelineRange) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with settings button
            CardHeader(
                title = config.title,
                modifier = headerModifier,
                onSettingsClick = { showSettings = true },
                onDeleteClick = onDelete
            )

            // Timeline Filter (using reusable component)
            TimelineFilterBar(
                currentRange = selectedTimelineRange,
                onRangeSelected = { newRange ->
                    selectedTimelineRange = newRange
                    onUpdate(config.copy(timelineRange = newRange))
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Pie Chart
            CategoryBreakdownPieChart(
                data = data,
                modifier = Modifier.fillMaxWidth()
            )

            // Legend
            if (config.showLegend) {
                CategoryLegend(data = data)
            }
        }
    }

    if (showSettings) {
        CategoryBreakdownCardSettingsDialog(
            onDismiss = { showSettings = false },
            config = config,
            onUpdate = onUpdate
        )
    }
}
```

**File**: `ui/analytics/components/TimelineTrajectoryCard.kt`

```kotlin
@Composable
fun TimelineTrajectoryCard(
    config: TimelineTrajectoryCardConfig,
    data: List<TimelineTrajectoryData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (TimelineTrajectoryCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedTimelineRange by remember { mutableStateOf(config.timelineRange) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with settings button
            CardHeader(
                title = config.title,
                modifier = headerModifier,
                onSettingsClick = { showSettings = true },
                onDeleteClick = onDelete
            )

            // Timeline Filter (using reusable component)
            TimelineFilterBar(
                currentRange = selectedTimelineRange,
                onRangeSelected = { newRange ->
                    selectedTimelineRange = newRange
                    onUpdate(config.copy(timelineRange = newRange))
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Line Chart
            TimelineTrajectoryLineChart(
                data = data,
                modifier = Modifier.fillMaxWidth(),
                showBalance = config.showBalance,
                showExpense = config.showExpense,
                showIncome = config.showIncome
            )

            // Legend
            TimelineLegend(
                showBalance = config.showBalance,
                showExpense = config.showExpense,
                showIncome = config.showIncome
            )
        }
    }

    if (showSettings) {
        TimelineTrajectoryCardSettingsDialog(
            onDismiss = { showSettings = false },
            config = config,
            onUpdate = onUpdate
        )
    }
}
```

**File**: `ui/analytics/components/CategoryExpenseTreemapCard.kt`

```kotlin
@Composable
fun CategoryExpenseTreemapCard(
    config: CategoryExpenseTreemapCardConfig,
    data: List<CategoryExpenseTreemapData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (CategoryExpenseTreemapCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedTimelineRange by remember { mutableStateOf(config.timelineRange) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with settings button
            CardHeader(
                title = config.title,
                modifier = headerModifier,
                onSettingsClick = { showSettings = true },
                onDeleteClick = onDelete
            )

            // Timeline Filter (using reusable component)
            TimelineFilterBar(
                currentRange = selectedTimelineRange,
                onRangeSelected = { newRange ->
                    selectedTimelineRange = newRange
                    onUpdate(config.copy(timelineRange = newRange))
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Treemap
            CategoryExpenseTreemap(
                data = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
    }

    if (showSettings) {
        CategoryExpenseTreemapCardSettingsDialog(
            onDismiss = { showSettings = false },
            config = config,
            onUpdate = onUpdate
        )
    }
}
```

### Phase 3: ViewModel Integration (Week 2-3)

#### 3.1 Create Analytics ViewModel
**File**: `ui/viewmodel/AnalyticsViewModel.kt`

```kotlin
@HiltViewModel
class AnalyticsViewModel(
    private val cardComputationService: CardComputationService,
    private val settingsDataStore: SettingsDataStore,
    private val transactionUseCases: TransactionUseCases,
    private val accountRepository: AccountRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    val analyticsCardsConfig: StateFlow<AnalyticsCardsConfig> = settingsDataStore.analyticsCardsConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsCardsConfig())

    val decimalPrecision: StateFlow<Int> = settingsDataStore.decimalPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    init {
        loadAnalyticsData()
    }

    private fun loadAnalyticsData() {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            val transactions = transactionUseCases.getTransactions(profileId).first()
            val accounts = accountRepository.getAllAccounts(profileId).first()
            val exchangeRates = emptyMap<String, BigDecimal>() // Load from analytics
            val baseCurrency = "USD" // Load from settings
            val precision = decimalPrecision.first()

            // Compute card data
            val configs = analyticsCardsConfig.value.cards
            val cardData = mutableMapOf<String, Any>()

            configs.forEach { config ->
                when (config) {
                    is CategoryBreakdownCardConfig -> {
                        cardData[config.cardId] = cardComputationService.computeCategoryBreakdown(
                            config, transactions, baseCurrency, exchangeRates, precision
                        )
                    }
                    is TimelineTrajectoryCardConfig -> {
                        cardData[config.cardId] = cardComputationService.computeTimelineTrajectory(
                            config, transactions, accounts, baseCurrency, exchangeRates, precision
                        )
                    }
                    is CategoryExpenseTreemapCardConfig -> {
                        cardData[config.cardId] = cardComputationService.computeCategoryExpenseTreemap(
                            config, transactions, baseCurrency, exchangeRates, precision
                        )
                    }
                    else -> {} // Handle other card types
                }
            }

            _uiState.value = _uiState.value.copy(
                cardData = cardData,
                transactions = transactions,
                accounts = accounts
            )
        }
    }

    fun updateCardConfig(cardId: String, update: (CardConfig) -> CardConfig) {
        viewModelScope.launch {
            val currentConfig = analyticsCardsConfig.value.cards.find { it.cardId == cardId }
            if (currentConfig != null) {
                val updatedConfig = update(currentConfig)
                settingsDataStore.updateAnalyticsCardsConfig { config ->
                    config.copy(cards = config.cards.map { if (it.cardId == cardId) updatedConfig else it })
                }
                loadAnalyticsData() // Reload data with new config
            }
        }
    }

    fun removeCard(cardId: String) {
        viewModelScope.launch {
            settingsDataStore.updateAnalyticsCardsConfig { config ->
                config.copy(cards = config.cards.filter { it.cardId != cardId })
            }
            loadAnalyticsData()
        }
    }

    fun reorderCards(cards: List<CardConfig>) {
        viewModelScope.launch {
            settingsDataStore.updateAnalyticsCardsConfig { config ->
                config.copy(cards = cards)
            }
        }
    }
}

data class AnalyticsUiState(
    val cardData: Map<String, Any> = emptyMap(),
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList()
)

@Serializable
data class AnalyticsCardsConfig(
    val cards: List<CardConfig> = emptyList()
)
```

#### 3.2 Add Analytics Config to SettingsDataStore
**File**: `data/local/SettingsDataStore.kt`

Following the established pattern for screen-specific card configurations, add analytics cards config:

```kotlin
// Add to existing DataStore keys
private val ANALYTICS_CARDS_CONFIG_KEY = stringPreferencesKey("analytics_cards_config")

// Add to existing Flow properties
val analyticsCardsConfig: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[ANALYTICS_CARDS_CONFIG_KEY] ?: ""
}

// Add to existing save methods
suspend fun saveAnalyticsCardsConfig(config: String) {
    context.dataStore.edit { preferences ->
        preferences[ANALYTICS_CARDS_CONFIG_KEY] = config
    }
}

// Add update method following the pattern of other settings
suspend fun updateAnalyticsCardsConfig(update: (AnalyticsCardsConfig) -> AnalyticsCardsConfig) {
    context.dataStore.edit { preferences ->
        val current = preferences[ANALYTICS_CARDS_CONFIG_KEY]?.let {
            try {
                Json.decodeFromString<AnalyticsCardsConfig>(it)
            } catch (e: Exception) {
                defaultAnalyticsCardsConfig()
            }
        } ?: defaultAnalyticsCardsConfig()
        preferences[ANALYTICS_CARDS_CONFIG_KEY] = Json.encodeToString(update(current))
    }
}

private fun defaultAnalyticsCardsConfig(): AnalyticsCardsConfig {
    val defaultCards = listOf(
        CategoryBreakdownCardConfig(
            cardId = "category_breakdown_1",
            order = 0,
            timelineRange = TimelineRange.Last30Days
        ),
        TimelineTrajectoryCardConfig(
            cardId = "timeline_trajectory_1",
            order = 1,
            timelineRange = TimelineRange.Last30Days
        ),
        CategoryExpenseTreemapCardConfig(
            cardId = "category_expense_treemap_1",
            order = 2,
            timelineRange = TimelineRange.Last30Days
        )
    )
    return AnalyticsCardsConfig(defaultCards)
}
```

**SettingsDataStore Pattern Summary:**

Each screen's card configuration follows this consistent pattern:

1. **Unique DataStore Key**: Each screen has its own `stringPreferencesKey`
2. **Flow for Reading**: Exposes configuration as a `Flow<String>` for reactive updates
3. **Save Method**: Direct save method for initial configuration
4. **Update Method**: Functional update method for partial updates
5. **Default Config**: Fallback configuration for new installations

This ensures:
- **Isolation**: Analytics cards config is independent of Dashboard, Account Detail, etc.
- **Consistency**: Same API pattern across all screen configs
- **Type Safety**: Each config is strongly typed with its own data class
- **Reactivity**: Changes automatically propagate to UI via Flow

### Phase 4: Screen Integration (Week 3)

#### 4.1 Update Analytics Screen
**File**: `ui/screens/AnalyticsScreen.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cardsConfig by viewModel.analyticsCardsConfig.collectAsStateWithLifecycle()
    val decimalPrecision by viewModel.decimalPrecision.collectAsStateWithLifecycle()

    var navTriggered by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { if (!navTriggered) { navTriggered = true; onBackClick() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Show add card dialog */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Card")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                AnalyticsHeroSection(uiState.currentBalance, uiState.selectedCurrency, decimalPrecision)
            }

            // Analytics Cards (Reorderable)
            item {
                CardReorderableList(
                    cards = cardsConfig.cards.sortedBy { it.order },
                    cardData = uiState.cardData,
                    onReorder = { viewModel.reorderCards(it) },
                    onUpdate = { cardId, update -> viewModel.updateCardConfig(cardId, update) },
                    onDelete = { viewModel.removeCard(it) }
                )
            }
        }
    }
}

@Composable
fun CardReorderableList(
    cards: List<CardConfig>,
    cardData: Map<String, Any>,
    onReorder: (List<CardConfig>) -> Unit,
    onUpdate: (String, (CardConfig) -> CardConfig) -> Unit,
    onDelete: (String) -> Unit
) {
    ReorderableLazyColumn(
        items = cards,
        onReorder = onReorder,
        key = { it.cardId }
    ) { card, isDragging, dragModifier ->
        when (card) {
            is CategoryBreakdownCardConfig -> {
                val data = cardData[card.cardId] as? List<CategoryBreakdownData> ?: emptyList()
                CategoryBreakdownCard(
                    config = card,
                    data = data,
                    modifier = Modifier.fillMaxWidth(),
                    headerModifier = dragModifier,
                    onUpdate = { onUpdate(card.cardId) { it } },
                    onDelete = { onDelete(card.cardId) }
                )
            }
            is TimelineTrajectoryCardConfig -> {
                val data = cardData[card.cardId] as? List<TimelineTrajectoryData> ?: emptyList()
                TimelineTrajectoryCard(
                    config = card,
                    data = data,
                    modifier = Modifier.fillMaxWidth(),
                    headerModifier = dragModifier,
                    onUpdate = { onUpdate(card.cardId) { it } },
                    onDelete = { onDelete(card.cardId) }
                )
            }
            is CategoryExpenseTreemapCardConfig -> {
                val data = cardData[card.cardId] as? List<CategoryExpenseTreemapData> ?: emptyList()
                CategoryExpenseTreemapCard(
                    config = card,
                    data = data,
                    modifier = Modifier.fillMaxWidth(),
                    headerModifier = dragModifier,
                    onUpdate = { onUpdate(card.cardId) { it } },
                    onDelete = { onDelete(card.cardId) }
                )
            }
            else -> {}
        }
    }
}
```

#### 4.2 Update Dashboard Screen (Optional - Add Analytics Cards)
**File**: `ui/dashboard/DashboardScreen.kt`

Add analytics cards to the dashboard card list:

```kotlin
// In DashboardViewModel, add analytics cards to the available card types
val availableCardTypes = listOf(
    UnifiedCardType.DASHBOARD_NET_WORTH,
    UnifiedCardType.DASHBOARD_CURRENCY_BALANCES,
    UnifiedCardType.DASHBOARD_CASH_FLOW,
    UnifiedCardType.DASHBOARD_ACCOUNTS,
    UnifiedCardType.DASHBOARD_BUDGETS,
    // New analytics cards
    UnifiedCardType.DASHBOARD_CATEGORY_BREAKDOWN,
    UnifiedCardType.DASHBOARD_TIMELINE_TRAJECTORY,
    UnifiedCardType.DASHBOARD_CATEGORY_EXPENSE_TREEMAP
)
```

### Phase 5: Dependency Injection (Week 3)

#### 5.1 Update DI Module
**File**: `di/AppContainer.kt`

```kotlin
@Provides
@Singleton
fun provideAnalyticsViewModel(
    cardComputationService: CardComputationService,
    settingsDataStore: SettingsDataStore,
    transactionUseCases: TransactionUseCases,
    accountRepository: AccountRepository,
    categoryDao: CategoryDao
): AnalyticsViewModel {
    return AnalyticsViewModel(
        cardComputationService = cardComputationService,
        settingsDataStore = settingsDataStore,
        transactionUseCases = transactionUseCases,
        accountRepository = accountRepository,
        categoryDao = categoryDao
    )
}
```

### Phase 6: Testing (Week 4)

#### 6.1 Unit Tests
- Test `CardComputationService` methods for each new card
- Test data transformation logic
- Test card config serialization/deserialization

#### 6.2 Integration Tests
- Test card computation with real transaction data
- Test card config persistence in DataStore
- Test card reorder functionality

#### 6.3 UI Tests
- Test card rendering with sample data
- Test card interactions (tap, drag)
- Test card settings dialog

## Technical Considerations

### Decoupled Configuration Architecture
- **Pattern**: Each screen maintains independent card configuration in DataStore
- **Benefits**: Multiple instances of same card type with different settings across screens
- **Storage**: Separate DataStore keys per screen (DASHBOARD_CARDS_CONFIG_KEY, ANALYTICS_CARDS_CONFIG_KEY, etc.)
- **Serialization**: Uses kotlinx.serialization for type-safe config persistence
- **Reactivity**: Flow-based updates ensure UI reflects config changes immediately

### Timeline Filter Component Reuse
- **Component**: `TimelineFilterBar` from `ui/components/AeonTimelineFilter.kt`
- **Features**: All/This/Last/Custom timeline selection with segmented control
- **Integration**: Each analytics card includes TimelineFilterBar for timeline range selection
- **Consistency**: Same UX across Dashboard, Analytics, and other screens
- **No Duplication**: Single implementation reduces maintenance burden

### Chart Library: Vico
- **Version**: 2.0.0-beta.3 (already included in project)
- **Benefits**: Compose-native, highly customizable, supports multiple chart types
- **Usage**: Use for pie charts and line charts
- **Treemap**: Custom implementation using Compose layout

### Performance Optimization
- Use `produceState` for reactive card data computation
- Cache computed data where appropriate
- Use `remember` for chart data transformation
- Implement lazy loading for large datasets
- Debounce timeline range changes to avoid excessive recomputation

### Data Caching
- Cache computed card data in ViewModel
- Invalidate cache when transactions change
- Use Flow-based reactive updates
- Implement intelligent cache invalidation based on timeline range changes

### Accessibility
- Add content descriptions for chart elements
- Support talkback for chart interactions
- Ensure color contrast meets WCAG standards
- Provide alternative text for visual data
- Ensure TimelineFilterBar is accessible with screen readers

### Responsive Design
- Charts should adapt to different screen sizes
- Use `fillMaxWidth` with aspect ratio constraints
- Support landscape orientation
- Optimize for tablets and foldables
- TimelineFilterBar already responsive from existing implementation

## File Structure

```
app/src/main/java/com/osscabinet/ossWallet/
├── domain/
│   ├── model/
│   │   ├── analytics/
│   │   │   └── AnalyticsData.kt (NEW)
│   │   └── settings/
│   │       └── CardConfig.kt (UPDATE - add new configs)
│   └── service/
│       └── CardComputationService.kt (UPDATE - add new methods)
├── data/
│   └── local/
│       └── SettingsDataStore.kt (UPDATE - add analytics config)
├── ui/
│   ├── analytics/
│   │   └── components/ (NEW)
│   │       ├── CategoryBreakdownCard.kt
│   │       ├── TimelineTrajectoryCard.kt
│   │       └── CategoryExpenseTreemapCard.kt
│   ├── components/
│   │   └── charts/ (NEW)
│   │       ├── CategoryBreakdownPieChart.kt
│   │       ├── TimelineTrajectoryLineChart.kt
│   │       └── CategoryExpenseTreemap.kt
│   ├── screens/
│   │   └── AnalyticsScreen.kt (UPDATE)
│   ├── viewmodel/
│   │   ├── CardType.kt (UPDATE)
│   │   └── AnalyticsViewModel.kt (NEW)
│   └── dashboard/
│       └── DashboardScreen.kt (OPTIONAL UPDATE)
└── di/
    └── AppContainer.kt (UPDATE)
```

## Migration Strategy

### Step 1: Add New Card Types to Analytics Screen
- Implement all three cards for Analytics screen only
- Test independently on Analytics screen

### Step 2: Enable Dashboard Integration
- Add card types to Dashboard's available card list
- Ensure cards work with Dashboard's card manager
- Test cross-screen functionality

### Step 3: Backward Compatibility
- Existing cards continue to work unchanged
- New cards are opt-in via card manager
- No breaking changes to existing configs

## Success Criteria

- [ ] All three cards render correctly on Analytics screen
- [ ] Cards are reorderable using CardReorderableList
- [ ] Card settings persist correctly in DataStore
- [ ] Analytics cards config is decoupled from other screen configs
- [ ] Same card type can exist on multiple screens with different settings
- [ ] TimelineFilterBar is integrated into all analytics cards
- [ ] Timeline selection works consistently across all cards
- [ ] Charts are interactive (tap for details)
- [ ] Cards can be added to Dashboard screen
- [ ] Performance is acceptable (< 500ms computation time)
- [ ] Accessibility requirements met
- [ ] Unit tests pass (80%+ coverage)
- [ ] Integration tests pass
- [ ] No breaking changes to existing functionality

## Timeline

- **Week 1**: Domain layer implementation (data models, service methods, card configs)
- **Week 2**: UI components implementation (chart components, card components)
- **Week 3**: ViewModel integration and screen integration
- **Week 4**: Testing, optimization, and documentation

## Risks and Mitigations

### Risk 1: Vico Chart Library Learning Curve
- **Mitigation**: Start with simple chart implementations, refer to Vico documentation and examples

### Risk 2: Performance with Large Datasets
- **Mitigation**: Implement pagination for timeline data, use efficient algorithms for aggregation

### Risk 3: Treemap Complexity
- **Mitigation**: Use simplified squarified treemap algorithm, consider third-party library if needed

### Risk 4: Cross-Screen Compatibility
- **Mitigation**: Thoroughly test card configs serialization/deserialization, ensure scope filtering works correctly

## Future Enhancements

1. **Advanced Chart Interactions**
   - Zoom and pan for timeline charts
   - Drill-down on pie chart segments
   - Animated transitions

2. **Customizable Visualizations**
   - User-selectable color schemes
   - Custom chart themes
   - Font size adjustments

3. **Export Functionality**
   - Export charts as images
   - Export data as CSV
   - Share analytics reports

4. **Predictive Analytics**
   - Forecast future trends
   - Anomaly detection
   - Budget recommendations

## Conclusion

This implementation plan provides a structured approach to adding three powerful analytics cards to the OssWallet application. Key architectural decisions include:

1. **Decoupled Configuration Architecture**: Each screen maintains its own independent card configuration in DataStore, allowing multiple instances of the same card type with different settings across screens. This follows the established pattern of DashboardCardsConfig, AccountDetailCardsConfig, and AccountsViewCardsConfig.

2. **Reusable Timeline Filter Component**: All analytics cards integrate the existing `TimelineFilterBar` component from `AeonTimelineFilter.kt`, ensuring consistency with dashboard cards and providing users with a familiar timeline selection interface across all screens.

3. **Cross-Screen Reusability**: The same card types are available for both Analytics and Dashboard contexts, enabling users to customize their analytics experience independently on each screen.

By leveraging the existing card architecture, Vico charting library, and reusable UI components, we can create maintainable, performant, and visually appealing analytics visualizations that work seamlessly across multiple screens while maintaining configuration isolation.
