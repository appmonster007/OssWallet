# Unified Card Renderer Architecture Plan

## Current State Analysis

### Screen-by-Screen Patterns

**AnalyticsScreen** (Already Refactored)
- Data fetched in screen, passed to renderer
- Card types: CategoryBreakdown, TimelineTrajectory, CategoryExpenseTreemap, IncomeExpensesSummary, PeriodComparison, CashFlowTrend, CashFlowProgress, BalanceForecast, PlannedPayments
- Callbacks: onUpdate, onDelete (with coroutineScope)

**DashboardScreen**
- Data fetching embedded in when statement using `remember`
- Card types: NetWorth, CurrencyBalances, CashFlow, Budgets, Accounts
- Callbacks: onFontSizeChange, onTimelineRangeChange, onDelete, onClickIncome, onClickExpense, onCurrencyClick
- Uses: dashboardViewModel, cardManagerViewModel

**AccountsViewScreen**
- Data fetching embedded in when statement using `LaunchedEffect` + `remember`
- Card types: AccountsViewNetWorth, AccountsViewCurrencyBalances, AccountsViewCashFlow
- Callbacks: onFontSizeChange, onTimelineRangeChange, onDelete, onCurrencyClick
- Uses: accountsViewViewModel, cardManagerViewModel

**AccountDetailScreen**
- Data fetching embedded in when statement using `produceState`
- Card types: AccountHero, AccountCashFlow, AccountCurrencyBalances
- Callbacks: onShowCurrencyBadgesChange, onShowBaseConversionChange, onTimelineRangeChange, onDelete, onClick, onClickIncome, onClickExpense, onCurrencyClick
- Uses: accountDetailViewModel

## Problems with Current Approach

1. **Data Fetching Coupling**: Each screen has different data fetching patterns (remember, LaunchedEffect, produceState)
2. **ViewModel Coupling**: Each screen uses different ViewModels with different method signatures
3. **Callback Duplication**: Similar callbacks (onTimelineRangeChange, onDelete, etc.) but with different signatures
4. **Card Config Types**: Different card config types for each screen (NetWorthCardConfig vs AccountsViewNetWorthCardConfig)
5. **Redundant Parameters**: Common parameters like baseCurrency, precision, modifier passed repeatedly

## Proposed Architecture

### Core Principles

1. **Separation of Concerns**: Separate data fetching from rendering
2. **Type Safety**: Use sealed classes for card types and callbacks
3. **Callback Interface**: Define common callback interface with type-safe variants
4. **Generic Renderer**: Single renderer that handles all card types via sealed class dispatch
5. **No Redundancy**: Pass minimal required data, compute derived values internally

### Architecture Design

```
┌─────────────────────────────────────────────────────────────┐
│                      Screen Layer                             │
│  - Fetches data (using ViewModel methods)                  │
│  - Creates CardRenderData for each card                     │
│  - Passes data to UnifiedCardRenderer                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              UnifiedCardRenderer                             │
│  - Accepts CardRenderData (sealed class)                    │
│  - Accepts CardCallbacks (sealed class)                     │
│  - Dispatches to appropriate card component                 │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Card Components                                 │
│  - NetWorthCard, CurrencyBalancesCard, etc.                 │
│  - Each card component is unchanged                         │
└─────────────────────────────────────────────────────────────┘
```

### Data Structures

```kotlin
// Unified card data container
sealed class CardRenderData {
    data class NetWorth(
        val uiState: NetWorthUiState,
        val cardId: String,
        val fontSize: FontSize,
        val timelineRange: TimelineRange
    ) : CardRenderData()
    
    data class CurrencyBalances(
        val currencyTotals: List<CurrencyAggregationItem>,
        val baseCurrency: String,
        val cardId: String,
        val precision: Int,
        val layout: LayoutType,
        val showBaseConversion: Boolean,
        val timelineRange: TimelineRange
    ) : CardRenderData()
    
    data class CategoryBreakdown(
        val data: List<CategoryBreakdownData>
    ) : CardRenderData()
    
    // ... other card types
}

// Unified callbacks container
sealed class CardCallbacks {
    data class Common(
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    data class WithTimeline(
        val onTimelineRangeChange: (TimelineRange) -> Unit,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    data class WithClickActions(
        val onClickIncome: (() -> Unit)? = null,
        val onClickExpense: (() -> Unit)? = null,
        val onCurrencyClick: ((String, TimelineRange) -> Unit)? = null,
        val onDelete: () -> Unit
    ) : CardCallbacks()
    
    // ... other callback variants
}
```

### Implementation Strategy

#### Phase 1: Define Core Interfaces
1. Create `CardRenderData` sealed class hierarchy
2. Create `CardCallbacks` sealed class hierarchy
3. Create `UnifiedCardRenderer` composable

#### Phase 2: Extract Data Fetching
1. Move data fetching logic from when statements to screen level
2. Create helper functions in ViewModels for data fetching
3. Screens fetch data and create CardRenderData objects

#### Phase 3: Implement Unified Renderer
1. Implement UnifiedCardRenderer with when statement
2. Map CardRenderData + CardCallbacks to card components
3. Add previews for each card type

#### Phase 4: Refactor Screens
1. DashboardScreen: Extract data fetching, use UnifiedCardRenderer
2. AccountsViewScreen: Extract data fetching, use UnifiedCardRenderer
3. AccountDetailScreen: Extract data fetching, use UnifiedCardRenderer
4. AnalyticsScreen: Migrate to UnifiedCardRenderer
5. Remove AnalyticsCardRenderer

#### Phase 5: Clean Up
1. Remove old renderer files
2. Remove duplicate code
3. Build and verify

### Benefits

1. **Single Source of Truth**: One renderer for all cards
2. **Type Safety**: Compile-time guarantees on card types
3. **Testability**: Can test renderer independently with mock data
4. **Maintainability**: Changes to card rendering in one place
5. **Extensibility**: Easy to add new card types
6. **No Redundancy**: Common parameters passed once
7. **Flexibility**: Screens can fetch data however they want

### File Structure

```
ui/components/cards/
├── UnifiedCardRenderer.kt
├── CardRenderData.kt
└── CardCallbacks.kt
```

### Example Usage

```kotlin
// In screen
val netWorthData = remember(cardConfig, state) {
    viewModel.computeNetWorth(...)
}

val renderData = CardRenderData.NetWorth(
    uiState = netWorthData,
    cardId = cardConfig.cardId,
    fontSize = cardConfig.fontSize,
    timelineRange = cardConfig.timelineRange
)

val callbacks = CardCallbacks.WithClickActions(
    onClickIncome = { /* ... */ },
    onClickExpense = { /* ... */ },
    onDelete = { viewModel.removeCard(cardConfig.cardId) }
)

UnifiedCardRenderer(
    renderData = renderData,
    callbacks = callbacks,
    modifier = Modifier.fillMaxWidth(),
    headerModifier = dragModifier
)
```

## Next Steps

1. Review and approve this architecture
2. Create the core data structures
3. Implement UnifiedCardRenderer
4. Refactor screens one by one
5. Test and verify
