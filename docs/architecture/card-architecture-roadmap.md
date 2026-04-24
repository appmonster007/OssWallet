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
**Status:** ⏳ IN PROGRESS

- [ ] Implement card-level filtering with remember/LaunchedEffect
- [ ] Add ViewModel helper functions for filtering
- [ ] Remove timeline filtering from CardComputationService
- [ ] Fix produceState keys to trigger recomputation
- [ ] Eliminate dual configuration system
- [ ] Add empty result factory methods

**Priority:** CRITICAL - Fixes cards collapsing on timeline change

### Phase 5: Architecture Cleanup (Week 5)
**Status:** ⏳ PENDING

- [ ] Merge duplicate card configs (CashFlowCardConfig + AccountCashFlowCardConfig)
- [ ] Merge duplicate card configs (CurrencyBalancesCardConfig + AccountCurrencyBalancesCardConfig)
- [ ] Remove redundant computation methods from ViewModels
- [ ] Remove internal service exposure from ViewModels
- [ ] Centralize filtering logic in CardFilter.kt

### Phase 6: Performance Optimization (Week 6)
**Status:** ⏳ PENDING

- [ ] Implement caching for computed data in ViewModels
- [ ] Replace produceState with Flow in ViewModels
- [ ] Add unit tests for CardCompute functions
- [ ] Add unit tests for CardFilter functions
- [ ] Implement Paging 3 for account lists
- [ ] Create baseline profiles for card rendering

### Phase 7: Unification (Week 7)
**Status:** ⏳ PENDING

- [ ] Remove old screen-specific card configs
- [ ] Remove old computation methods from viewmodels
- [ ] Update tests to use new architecture
- [ ] Documentation updates
- [ ] Remove deprecated code

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

- [ ] Cards don't collapse when timeline changes
- [ ] Single card config per card type with scope support
- [ ] No duplicate computation methods
- [ ] Consistent naming conventions
- [ ] Services not exposed from ViewModels
- [ ] Card-level filtering with ViewModel helpers
- [ ] Caching implemented for computed data
- [ ] Flow-based reactive data instead of produceState
- [ ] Unit tests added for card computation
- [ ] Performance optimizations implemented
- [ ] Build successful with no errors
- [ ] Tests passing
- [ ] Manual verification complete

## Estimated Effort

- Phase 4 (Timeline Fix): 5-7 days
- Phase 5 (Cleanup): 3-4 days
- Phase 6 (Performance): 4-5 days
- Phase 7 (Unification): 3-4 days

**Total Remaining Effort:** 15-20 days

## Next Steps

1. **Immediate:** Complete Phase 4 (Timeline Filtering Fix) - Critical bug fix
2. **Short-term:** Complete Phase 5 (Architecture Cleanup)
3. **Medium-term:** Complete Phase 6 (Performance Optimization)
4. **Long-term:** Complete Phase 7 (Unification and Cleanup)
