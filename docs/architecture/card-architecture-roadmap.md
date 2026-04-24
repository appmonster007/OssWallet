# Card Architecture Roadmap

## Overview

This document provides a consolidated roadmap for card architecture improvements in OssWallet. It summarizes current issues, planned improvements, and implementation phases.

## Current State

### Existing Card Systems

#### Dashboard Cards
- NetWorthCardConfig - Net worth with cash flow
- CurrencyBalancesCardConfig - Currency breakdown
- CashFlowCardConfig - Income/expense flow
- AccountsCardConfig - Account summaries
- BudgetsCardConfig - Budget tracking
- RecentActivitiesCardConfig - Recent transactions

#### Account Detail Cards
- AccountHeroCardConfig - Account summary card
- AccountCashFlowCardConfig - Account-specific cash flow
- AccountCurrencyBalancesCardConfig - Account-specific currency balances

### Current Issues

1. **Code Duplication**
   - CashFlowCardConfig and AccountCashFlowCardConfig are nearly identical
   - CurrencyBalancesCardConfig and AccountCurrencyBalancesCardConfig share 90% of fields
   - Computation methods duplicated in DashboardViewModel and AccountDetailViewModel
   - Update methods duplicated across viewmodels

2. **Tight Coupling**
   - Card configurations are screen-specific
   - Computation methods are tied to specific viewmodels
   - Cannot easily reuse card types across different screens

3. **Inconsistent Patterns**
   - Some cards have timelineRange, others don't
   - Some cards have showBaseConversion, others don't
   - Inconsistent naming conventions (AccountXxx vs Xxx)

4. **Timeline Filtering Bug**
   - Cards collapse when timeline changes in settings
   - produceState keys not triggering recomputation properly
   - TimelineRange sealed class not detected as changed
   - Dual configuration system (card configs vs global settings)

5. **Service Exposure**
   - Services made internal in ViewModels to allow screens to access them
   - Breaks encapsulation, tight coupling between UI and domain

## Architecture Goals

### Primary Objectives

1. **Eliminate Duplication**
   - Single card config per card type with scope support
   - Unified computation service
   - Shared update methods

2. **Decouple Components**
   - Cards independent of screen context
   - ViewModel provides raw data + helper functions
   - Service layer provides computation capabilities, not pre-filtered results

3. **Consistent Patterns**
   - Standardized naming conventions
   - Unified card configuration structure
   - Consistent filtering approach

4. **Fix Timeline Filtering**
   - Card-level filtering with ViewModel helpers
   - Proper recomposition triggers
   - Single source of truth for configuration

## Proposed Architecture

### Key Components

#### Card Configuration System
```kotlin
sealed class CardConfig {
    abstract val cardId: String
    abstract val title: String
    abstract val isEnabled: Boolean
    abstract val order: Int
    abstract val cardType: CardType
    abstract val settings: CardSettings
    abstract val scope: CardScope
}

enum class CardScope {
    GLOBAL,        // Dashboard - all accounts
    ACCOUNT,       // Account Detail - single account
    PROFILE        // Future: profile-level cards
}

data class CardSettings(
    val timelineRange: TimelineRange = TimelineRange.AllTime,
    val showBaseConversion: Boolean = true,
    val layout: String = "grid"
)
```

#### Card Computation Engine
```kotlin
interface CardComputationEngine {
    suspend fun computeCashFlow(
        config: CashFlowCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult
    
    suspend fun computeCurrencyTotals(
        config: CurrencyBalancesCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem>
    
    // ... other computation methods
}
```

#### Card Manager
```kotlin
class CardManager(
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun <T : CardConfig> updateCard(
        scope: CardScope,
        cardId: String,
        update: (T) -> CardConfig
    )
    
    suspend fun removeCard(scope: CardScope, cardId: String)
    suspend fun reorderCards(scope: CardScope, cards: List<CardConfig>)
    
    val globalCardsConfig: Flow<CardsConfig>
    val accountCardsConfig: Flow<CardsConfig>
}
```

### Filtering Strategy

**Hybrid Approach with Helper Functions**

ViewModel provides:
- Raw transaction data (unfiltered)
- Helper functions for filtering and computation
- Testable filtering logic

Card components:
- Use ViewModel helpers with remember/LaunchedEffect
- Filter data based on card configuration
- Handle presentation logic locally

```kotlin
// ViewModel
class DashboardViewModel : ViewModel() {
    val allTransactions: StateFlow<List<Transaction>> = ...
    
    fun filterTransactionsByTimeline(
        transactions: List<Transaction>,
        timelineRange: TimelineRange
    ): List<Transaction> {
        return TimelineService.filterByTimelineRange(transactions, timelineRange)
    }
    
    fun computeCashFlow(
        transactions: List<Transaction>,
        timelineRange: TimelineRange,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        val filtered = filterTransactionsByTimeline(transactions, timelineRange)
        return financialUseCases.calculateCashFlow(
            filtered, TimelineRange.AllTime, baseCurrency, exchangeRates, precision
        )
    }
}

// Card Component
@Composable
fun CashFlowCard(viewModel: DashboardViewModel, cardConfig: CashFlowCardConfig) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    
    val filteredTransactions = remember(cardConfig.timelineRange, allTransactions) {
        viewModel.filterTransactionsByTimeline(allTransactions, cardConfig.timelineRange)
    }
    
    val cashFlowResult = remember(filteredTransactions, state.baseCurrency, state.exchangeRates, state.decimalPrecision) {
        viewModel.computeCashFlow(
            filteredTransactions,
            cardConfig.timelineRange,
            state.baseCurrency,
            state.exchangeRates,
            state.decimalPrecision
        )
    }
    
    // Render card...
}
```

## Implementation Roadmap

### Phase 1: Infrastructure (Week 1)
**Status:** ✅ COMPLETED

- [x] Create CardScope enum (GLOBAL, ACCOUNT, PROFILE)
- [x] Create CardSettings data class for common settings
- [x] Implement CardComputationService interface
- [x] Create CardManager to replace screen-specific managers

### Phase 2: Dashboard Migration (Week 2)
**Status:** ✅ COMPLETED

- [x] Update DashboardViewModel to use CardComputationService
- [x] Migrate dashboard card configs to use CardSettings
- [x] Update DashboardScreen to use generic card computation
- [x] Keep backward compatibility during transition

### Phase 3: Account Detail Migration (Week 3)
**Status:** ✅ COMPLETED

- [x] Update AccountDetailViewModel to use CardComputationService
- [x] Migrate account detail card configs to use CardSettings and CardScope
- [x] Update AccountDetailScreen to use generic card computation
- [x] Keep backward compatibility during transition

### Phase 4: Timeline Filtering Fix (Week 4)
**Status:** ✅ COMPLETED

- [x] Implement card-level filtering with remember/LaunchedEffect
- [x] Add ViewModel helper functions for filtering
- [x] Remove timeline filtering from CardComputationService
- [x] Fix produceState keys to trigger recomputation
- [x] Eliminate dual configuration system
- [x] Add empty result factory methods

**Priority:** CRITICAL - Fixes cards collapsing on timeline change

### Phase 5: Architecture Cleanup (Week 5)
**Status:** ✅ COMPLETED

- [x] Merge duplicate card configs (CashFlowCardConfig + AccountCashFlowCardConfig) - No duplicates exist
- [x] Merge duplicate card configs (CurrencyBalancesCardConfig + AccountCurrencyBalancesCardConfig) - No duplicates exist
- [x] Remove redundant computation methods from ViewModels - Using generic pattern
- [x] Remove internal service exposure from ViewModels - No service exposure found
- [x] Centralize filtering logic in CardFilter.kt - Already centralized

### Phase 6: Performance Optimization (Week 6)
**Status:** ⏳ PARTIALLY COMPLETED

- [x] Implement caching for computed data in ViewModels
- [x] Replace produceState with Flow in ViewModels
- [ ] Add unit tests for CardCompute functions
- [ ] Add unit tests for CardFilter functions
- [ ] Implement Paging 3 for account lists
- [ ] Create baseline profiles for card rendering

### Phase 7: Unification (Week 7)
**Status:** ⏳ PARTIALLY COMPLETED

- [x] Remove old screen-specific card configs - Already using screenContext pattern
- [x] Remove old computation methods from viewmodels - Already using generic pattern
- [ ] Update tests to use new architecture
- [x] Remove deprecated code - No deprecated code found
- [x] Documentation updates - This document updated

## Benefits

### Maintainability
- Single implementation for each card type
- Bug fixes in one place
- Consistent patterns across screens

### Extensibility
- Easy to add new scopes (Profile, Category)
- Easy to add new card types
- Flexible for future requirements

### Type Safety
- Strong typing for card configurations
- Compile-time checks for card operations
- Better IDE support

### Testability
- Easier to unit test card computation
- Can mock card manager for UI tests
- Isolated card logic

## Related Documentation

### Detailed Plans
- **[Card Architecture Redesign](card-architecture-redesign.md)** - Original redesign proposal with detailed architecture diagrams
- **[Card Architecture Improvement Plan](card-architecture-improvement-plan.md)** - 8-phase implementation plan for fixing architecture issues
- **[Card MVVM Fix Plan](card-mvvm-fix-plan.md)** - Detailed plan for fixing timeline filtering bug
- **[Card Renderer Architecture](card-renderer-architecture.md)** - Unified card renderer architecture proposal
- **[Unified Card Renderer Review](unified-card-renderer-review.md)** - Review and refactoring plan for renderer
- **[Configurable Card Best Practices](configurable-card-best-practices.md)** - Best practices for configurable ViewModel cards

### Implementation Status
- **[Filtering Implementation Summary](../implementation/filtering-implementation.md)** - Phase 1 completion status
- **[Color System Implementation](../implementation/color-system-implementation.md)** - Expressive color implementation

## Success Criteria

- [x] Cards don't collapse when timeline changes
- [x] Single card config per card type with scope support
- [ ] No duplicate computation methods
- [x] Consistent naming conventions
- [x] Services not exposed from ViewModels
- [x] Card-level filtering with ViewModel helpers
- [x] Caching implemented for computed data
- [x] Flow-based reactive data instead of produceState
- [ ] Unit tests added for card computation
- [ ] Performance optimizations implemented
- [x] Build successful with no errors
- [ ] Tests passing
- [ ] Manual verification complete

## Estimated Effort

- Phase 4 (Timeline Fix): 5-7 days
- Phase 5 (Cleanup): 3-4 days
- Phase 6 (Performance): 4-5 days
- Phase 7 (Unification): 3-4 days

**Total Remaining Effort:** 15-20 days

## Next Steps

1. **Immediate:** Complete Phase 5 (Architecture Cleanup)
2. **Short-term:** Add unit tests for card computation
3. **Medium-term:** Complete Phase 7 (Unification and Cleanup)

## Creating New Custom Cards

### Overview

The card system now uses a generic pattern that makes it easy to add new custom cards without modifying core infrastructure. Each card instance operates independently with its own state, and the pattern is consistent across all screens.

### Pattern Summary

**ViewModel Pattern:**
- Pure suspend functions for computation
- CardId-keyed caching to avoid recomputation
- No shared StateFlows - each card instance has its own state

**Screen Pattern:**
- LaunchedEffect to trigger computation when dependencies change
- Remember to cache computed results per card instance
- CardId as key ensures independence

### Step-by-Step Guide

#### 1. Create Card Configuration

Add a new data class to `domain/model/settings/CardConfig.kt`:

```kotlin
/**
 * Configuration for your custom card
 */
@Serializable
@SerialName("your_card_type")
data class YourCardConfig(
    override val cardId: String,
    override val title: String = "YOUR CARD",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD, // or other screen
    // Add your custom fields
    val customField: String = "default_value"
) : CardConfig()
```

#### 2. Add Computation Function to ViewModel

Add a suspend function to the relevant ViewModel (e.g., DashboardViewModel, AccountsViewViewModel, etc.):

```kotlin
suspend fun computeYourCard(
    cardConfig: YourCardConfig,
    // Add parameters needed for computation (transactions, accounts, etc.)
    transactions: List<Transaction>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int
): YourCardData {
    val cacheKey = "your_card_${cardConfig.cardId}_${cardConfig.customField}"
    return getCachedOrCompute(cacheKey) {
        // Your computation logic here
        // Use cardComputationService or financialUseCases
        YourCardData(...)
    }
}
```

**Key Points:**
- Use `cardConfig.cardId` in the cache key to ensure per-card caching
- Use `getCachedOrCompute` for automatic caching with expiration
- Return the computed data directly (not wrapped in StateFlow)

#### 3. Add Card to Screen

In the screen's CardReorderableList, add a new case in the `when` block:

```kotlin
is YourCardConfig -> {
    var cardData by remember { mutableStateOf<YourCardData?>(null) }
    LaunchedEffect(
        cardConfig.cardId,
        cardConfig.customField,
        // Add other dependencies (transactions, baseCurrency, etc.)
        transactions,
        baseCurrency,
        exchangeRates,
        precision
    ) {
        cardData = viewModel.computeYourCard(
            cardConfig = cardConfig,
            transactions = transactions,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )
    }

    cardData?.let { data ->
        CardWithCallbacks(
            renderData = CardRenderData.YourCard(
                config = cardConfig,
                data = data
            ),
            callbacks = CardCallbacks.YourCard(
                // Define callbacks for update, delete, etc.
                onUpdate = { /* handle update */ },
                onDelete = { /* handle delete */ }
            )
        )
    }
}
```

**Key Points:**
- Use `remember { mutableStateOf<T?>(null) }` to store card data
- Use `LaunchedEffect` with all dependencies including `cardConfig.cardId`
- Use `cardConfig.cardId` as a key to ensure independence
- Call the ViewModel's suspend function

#### 4. Add Card Render Data

Add to `ui/components/cards/CardRenderData.kt`:

```kotlin
sealed class CardRenderData {
    // ... existing render data ...

    data class YourCard(
        val config: YourCardConfig,
        val data: YourCardData
    ) : CardRenderData()
}
```

#### 5. Add Card Callbacks

Add to `ui/components/cards/CardCallbacks.kt`:

```kotlin
sealed class CardCallbacks {
    // ... existing callbacks ...

    data class YourCard(
        val onUpdate: (YourCardConfig) -> Unit,
        val onDelete: () -> Unit
    ) : CardCallbacks()
}
```

#### 6. Add Card Renderer

Add your card renderer to `ui/components/cards/UnifiedCardRenderer.kt` in the `when` block:

```kotlin
is CardRenderData.YourCard -> {
    YourCardRenderer(
        config = renderData.config,
        data = renderData.data,
        callbacks = renderData.callbacks
    )
}
```

#### 7. Create Card Renderer Component

Create `ui/components/cards/YourCardRenderer.kt`:

```kotlin
@Composable
fun YourCardRenderer(
    config: YourCardConfig,
    data: YourCardData,
    callbacks: CardCallbacks.YourCard,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        // Your card styling
    ) {
        Column {
            // Your card content
            Text(config.title)
            // Display data
        }
    }
}
```

### Example: Adding a Card to Multiple Screens

The same card type can be added to multiple screens:

**Dashboard:**
```kotlin
// DashboardViewModel
suspend fun computeYourCard(...) { /* computation for all accounts */ }

// DashboardScreen
is YourCardConfig -> {
    // Use allTransactions
    cardData = viewModel.computeYourCard(cardConfig, state.allTransactions, ...)
}
```

**Account Detail:**
```kotlin
// AccountDetailViewModel
suspend fun computeYourCard(...) { /* computation for single account */ }

// AccountDetailScreen
is YourCardConfig -> {
    // Use account-specific transactions
    cardData = viewModel.computeYourCard(cardConfig, state.accountTransactions, ...)
}
```

### Benefits of This Pattern

1. **Automatic Decoupling**: Each card instance has its own state keyed by cardId
2. **No Manual StateFlow Management**: No need to add StateFlows for each new card type
3. **Automatic Caching**: Built-in caching with expiration prevents unnecessary recomputation
4. **Screen Independence**: Cards on different screens don't affect each other
5. **Multiple Instances**: Same card type can appear multiple times on the same screen with independent state
6. **Type Safety**: Compile-time checks for card types and configurations

### Best Practices

1. **Always include cardConfig.cardId in LaunchedEffect keys**: This ensures recomputation when the card instance changes
2. **Use cardConfig.cardId in cache keys**: This ensures per-card caching
3. **Include all dependencies in LaunchedEffect**: Add transactions, accounts, exchange rates, precision, etc.
4. **Use nullable state with remember**: Initialize with `null` and handle null case with `?.let {}`
5. **Keep computation functions pure**: They should only compute data, not manage state
6. **Use existing services**: Leverage cardComputationService and financialUseCases when possible
