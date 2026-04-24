# UnifiedCardRenderer Architecture Review & Refactoring Plan

**Document Version:** 2.0  
**Date:** April 24, 2026  
**Author:** Senior Android Architect  
**Status:** Revised - Simplified Approach

---

## Executive Summary

The current `UnifiedCardRenderer` implementation successfully centralizes card rendering logic across the application, but contains unnecessary screen-specific card variants that violate DRY principles. This document provides a comprehensive analysis and proposes a simplified refactoring approach focused on eliminating duplication by having screens handle data filtering and configuration, while the renderer remains purely presentational.

**Key Findings:**
- ✅ **Strengths:** Type-safe sealed classes, separation of data/callbacks, successful consolidation of rendering logic
- ⚠️ **Concerns:** Screen-specific card variants (NetWorth vs AccountsViewNetWorth), data duplication, unnecessary complexity
- 🎯 **Recommendation:** Eliminate screen-specific card variants, use unified card types with screen-specific data filtering

---

## Current Architecture Analysis

### 1. Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Screens (UI Layer)                        │
│  AnalyticsScreen, DashboardScreen, AccountDetailScreen,    │
│  AccountsViewScreen                                         │
│  - Filter data based on screen context                      │
│  - Set appropriate config values                             │
│  - Pass filtered data to renderer                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Uses
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              UnifiedCardRenderer (UI Layer)                 │
│  - Single composable with when statement                     │
│  - Directly imports card composables                         │
│  - Handles card types (no screen-specific variants)          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Uses
                       ▼
┌─────────────────────────────────────────────────────────────┐
│         CardRenderData & CardCallbacks (UI Layer)           │
│  - Sealed class hierarchies for type safety                  │
│  - Unified card types (no screen-specific variants)          │
│  - Shared CardConfig across screens                          │
└─────────────────────────────────────────────────────────────┘
```

### 2. File Structure

| File | Lines | Purpose | Issues |
|------|-------|---------|--------|
| `UnifiedCardRenderer.kt` | 553 | Central rendering logic | Screen-specific card variants, data duplication |
| `CardRenderData.kt` | 246 | Data contracts | Screen-specific variants (NetWorth vs AccountsViewNetWorth) |
| `CardCallbacks.kt` | 190 | Callback contracts | Screen-specific variants, inconsistent signatures |

---

## Architectural Issues

### Critical Issues (Must Fix)

#### 1. **Screen-Specific Card Variants (DRY Violation)**
**Problem:** Same card types exist with screen-specific variants:
- `NetWorth` vs `AccountsViewNetWorth` (both render `NetWorthCard`)
- `CurrencyBalances` vs `AccountCurrencyBalances` vs `AccountsViewCurrencyBalances`
- `CashFlow` vs `AccountCashFlow` vs `AccountsViewCashFlow`
- `Accounts` vs `AccountsViewAccountsList`

**Impact:** 
- Code duplication and maintenance burden
- Potential divergence between variants
- Unnecessary complexity in renderer
- Violates DRY principle

**Current Code:**
```kotlin
// CardRenderData.kt
data class NetWorth(...) : CardRenderData()
data class AccountsViewNetWorth(...) : CardRenderData()

// Both use the same card component but with different data structures
```

#### 2. **Screen-Specific CardConfig Duplication**
**Problem:** Same card has multiple config classes:
- `NetWorthCardConfig` vs `AccountsViewNetWorthCardConfig`
- `CurrencyBalancesCardConfig` vs `AccountCurrencyBalancesCardConfig` vs `AccountsViewCurrencyBalancesCardConfig`

**Impact:** 
- Settings management complexity
- Cannot share settings across screens
- Redundant configuration logic

#### 3. **Screen-Specific Callback Duplication**
**Problem:** Same card has multiple callback types:
- `WithFontSizeAndTimeline` vs `AccountsViewNetWorth`
- `CurrencyBalances` vs `AccountCurrencyBalances` vs `AccountsViewCurrencyBalances`

**Impact:** 
- Callback signature inconsistency
- Unnecessary type casting complexity
- Harder to maintain

### Moderate Issues (Should Fix)

#### 4. **Data Filtering Responsibility Unclear**
**Problem:** Currently unclear whether data filtering happens:
- In the card data class
- In the renderer
- In the screen
- In the ViewModel

**Impact:** Inconsistent patterns, potential performance issues.

#### 5. **Callback Signature Inconsistency**
**Problem:** Callback signatures vary inconsistently across card types.

#### 6. **Type Safety Issues with Fallback Logic**
**Problem:** Extensive use of `as?` casting with fallback logic in renderer.

### Minor Issues (Nice to Fix)

#### 7. **Missing Common Interface**
**Problem:** No base interface for cards.

#### 8. **Hardcoded Values**
**Problem:** Hardcoded strings in renderer (theme, settings titles).

#### 9. **Inadequate Documentation**
**Problem:** Minimal KDoc, no architecture diagrams.

#### 10. **No Preview Support**
**Problem:** Preview is empty placeholder.

---

## Proposed Simplified Architecture

### Core Principle

**Screens are responsible for data filtering and configuration, renderer is purely presentational.**

```
┌─────────────────────────────────────────────────────────────┐
│                    Screens (UI Layer)                        │
│  - Filter data based on screen context (account, date, etc)  │
│  - Set appropriate config values (layout, visibility, etc)   │
│  - Create unified CardRenderData with filtered data          │
│  - Create unified CardCallbacks with appropriate handlers     │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Passes filtered data
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              UnifiedCardRenderer (UI Layer)                 │
│  - Pure presentational component                              │
│  - No data filtering logic                                    │
│  - No screen-specific variants                               │
│  - Single when statement for each card type                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Uses
                       ▼
┌─────────────────────────────────────────────────────────────┐
│         CardRenderData & CardCallbacks (UI Layer)           │
│  - Unified card types (no screen-specific variants)          │
│  - Shared CardConfig across all screens                      │
│  - Callbacks match actual card component signatures          │
└─────────────────────────────────────────────────────────────┘
```

### Example: NetWorth Card Across Screens

**DashboardScreen:**
```kotlin
// Filter data: All accounts, global timeline
val filteredAccounts = allAccounts
val filteredTimeline = globalTimelineRange

val netWorthData = CardRenderData.NetWorth(
    uiState = computeNetWorthState(filteredAccounts, filteredTimeline),
    cardId = config.cardId,
    fontSize = config.fontSize,
    timelineRange = filteredTimeline
)

val callbacks = CardCallbacks.WithFontSizeAndTimeline(
    onFontSizeChange = { cardId, fontSize -> 
        viewModel.updateCardConfig(cardId, fontSize) 
    },
    onTimelineRangeChange = { cardId, range ->
        viewModel.updateTimelineRange(range)
    },
    onDelete = { viewModel.removeCard(config.cardId) }
)

UnifiedCardRenderer(netWorthData, callbacks, ...)
```

**AccountsViewScreen:**
```kotlin
// Filter data: Specific account(s), account-specific timeline
val filteredAccounts = selectedAccounts
val filteredTimeline = accountTimelineRange

val netWorthData = CardRenderData.NetWorth(
    uiState = computeNetWorthState(filteredAccounts, filteredTimeline),
    cardId = config.cardId,
    fontSize = config.fontSize,
    timelineRange = filteredTimeline
)

val callbacks = CardCallbacks.WithFontSizeAndTimeline(
    onFontSizeChange = { cardId, fontSize -> 
        viewModel.updateCardConfig(cardId, fontSize) 
    },
    onTimelineRangeChange = { cardId, range ->
        viewModel.updateTimelineRange(range)
    },
    onDelete = { viewModel.removeCard(config.cardId) }
)

UnifiedCardRenderer(netWorthData, callbacks, ...)
```

**Key Insight:** Both screens use the exact same `CardRenderData.NetWorth` and `CardCallbacks.WithFontSizeAndTimeline`, but with different filtered data and config values.

---

## Consolidation Plan

### Phase 1: Consolidate CardConfig (Week 1)

**Goal:** Remove screen-specific config classes, use shared config across all screens.

**Current State:**
```kotlin
data class NetWorthCardConfig(...)
data class AccountsViewNetWorthCardConfig(...)

data class CurrencyBalancesCardConfig(...)
data class AccountCurrencyBalancesCardConfig(...)
data class AccountsViewCurrencyBalancesCardConfig(...)

data class CashFlowCardConfig(...)
data class AccountCashFlowCardConfig(...)
data class AccountsViewCashFlowCardConfig(...)

data class AccountsCardConfig(...)
data class AccountsViewAccountsListCardConfig(...)
```

**Target State:**
```kotlin
data class NetWorthCardConfig(...)
data class CurrencyBalancesCardConfig(...)
data class CashFlowCardConfig(...)
data class AccountsCardConfig(...)
```

**Actions:**
- [ ] Keep `NetWorthCardConfig`, remove `AccountsViewNetWorthCardConfig`
- [ ] Keep `CurrencyBalancesCardConfig`, remove `AccountCurrencyBalancesCardConfig` and `AccountsViewCurrencyBalancesCardConfig`
- [ ] Keep `CashFlowCardConfig`, remove `AccountCashFlowCardConfig` and `AccountsViewCashFlowCardConfig`
- [ ] Keep `AccountsCardConfig`, remove `AccountsViewAccountsListCardConfig`
- [ ] Update CardManagerViewModel to use shared config classes
- [ ] Update screens to set config values appropriately for their context

### Phase 2: Consolidate CardRenderData (Week 1-2)

**Goal:** Remove screen-specific variants, use unified card types with shared configs.

**Current State:**
```kotlin
data class NetWorth(...) : CardRenderData()
data class AccountsViewNetWorth(...) : CardRenderData()

data class CurrencyBalances(...) : CardRenderData()
data class AccountCurrencyBalances(...) : CardRenderData()
data class AccountsViewCurrencyBalances(...) : CardRenderData()

data class CashFlow(...) : CardRenderData()
data class AccountCashFlow(...) : CardRenderData()
data class AccountsViewCashFlow(...) : CardRenderData()

data class Accounts(...) : CardRenderData()
data class AccountsViewAccountsList(...) : CardRenderData()
```

**Target State:**
```kotlin
data class NetWorth(
    val config: NetWorthCardConfig,
    val uiState: NetWorthUiState
) : CardRenderData()

data class CurrencyBalances(
    val config: CurrencyBalancesCardConfig,
    val currencyTotals: List<CurrencyAggregationItem>,
    val baseCurrency: String,
    val precision: Int
) : CardRenderData()

data class CashFlow(
    val config: CashFlowCardConfig,
    val cashFlowResult: CashFlowResult,
    val baseCurrency: String,
    val precision: Int
) : CardRenderData()

data class Accounts(
    val config: AccountsCardConfig,
    val accounts: List<Account>,
    val accountStats: Map<String, AccountSummaryItem>,
    val baseCurrency: String,
    val currentTheme: String,
    val precision: Int
) : CardRenderData()
```

**Actions:**
- [ ] Update `NetWorth` to use `NetWorthCardConfig` instead of individual fields
- [ ] Remove `AccountsViewNetWorth`
- [ ] Update `CurrencyBalances` to use `CurrencyBalancesCardConfig` instead of individual fields
- [ ] Remove `AccountCurrencyBalances` and `AccountsViewCurrencyBalances`
- [ ] Update `CashFlow` to use `CashFlowCardConfig` instead of individual fields
- [ ] Remove `AccountCashFlow` and `AccountsViewCashFlow`
- [ ] Update `Accounts` to use `AccountsCardConfig` (already has it)
- [ ] Remove `AccountsViewAccountsList`
- [ ] Update AccountDetailScreen to use unified types with account-filtered data
- [ ] Update AccountsViewScreen to use unified types with account-filtered data

### Phase 3: Consolidate CardCallbacks (Week 2)

**Goal:** Remove screen-specific callback types, use unified callbacks.

**Current State:**
```kotlin
data class WithFontSizeAndTimeline(...)
data class AccountsViewNetWorth(...)

data class CurrencyBalances(...)
data class AccountCurrencyBalances(...)
data class AccountsViewCurrencyBalances(...)
```

**Target State:**
```kotlin
data class WithFontSizeAndTimeline(...)
data class CurrencyBalances(...)
```

**Actions:**
- [ ] Keep `WithFontSizeAndTimeline`, remove `AccountsViewNetWorth`
- [ ] Keep `CurrencyBalances`, remove `AccountCurrencyBalances` and `AccountsViewCurrencyBalances`
- [ ] Keep `CashFlow`, remove `AccountCashFlow` and `AccountsViewCashFlow`
- [ ] Keep `Accounts`, remove `AccountsViewAccountsList`
- [ ] Update UnifiedCardRenderer to use unified callbacks
- [ ] Update screens to use unified callbacks with appropriate handlers

### Phase 4: Update UnifiedCardRenderer (Week 2)

**Goal:** Remove screen-specific rendering cases and update to use config-based data.

**Actions:**
- [ ] Remove `AccountsViewNetWorth` case
- [ ] Remove `AccountCurrencyBalances` case
- [ ] Remove `AccountsViewCurrencyBalances` case
- [ ] Remove `AccountCashFlow` case
- [ ] Remove `AccountsViewCashFlow` case
- [ ] Remove `AccountsViewAccountsList` case
- [ ] Update `NetWorth` case to read from `config.cardId`, `config.fontSize`, `config.timelineRange`
- [ ] Update `CurrencyBalances` case to read from `config.cardId`, `config.layout`, `config.showBaseConversion`, `config.timelineRange`
- [ ] Update `CashFlow` case to read from `config.cardId`, `config.showBaseConversion`, `config.timelineRange`
- [ ] Update `Accounts` case to read from `config.cardId`, `config.gridColumns`, `config.layout`, `config.timelineRange`, `config.showAddAccountCard`
- [ ] Ensure remaining cases handle all screen contexts through data/config differences

### Phase 5: Update Screens with Data Filtering (Week 3)

**Goal:** Implement screen-specific data filtering logic and use shared configs.

**DashboardScreen:**
```kotlin
// Use global data (all accounts, global timeline)
val netWorthData = CardRenderData.NetWorth(
    config = netWorthCardConfig, // Shared config
    uiState = viewModel.netWorthState // Computed from all accounts
)
```

**AccountDetailScreen:**
```kotlin
// Use account-specific data (single account, account timeline)
val netWorthData = CardRenderData.NetWorth(
    config = netWorthCardConfig, // Shared config with account-specific values
    uiState = viewModel.accountNetWorthState // Computed from single account
)
```

**AccountsViewScreen:**
```kotlin
// Use selected accounts data (filtered accounts, account timeline)
val netWorthData = CardRenderData.NetWorth(
    config = netWorthCardConfig, // Shared config with accounts-view-specific values
    uiState = viewModel.selectedAccountsNetWorthState // Computed from selected accounts
)
```

**Actions:**
- [ ] Update DashboardScreen to use shared configs with global data
- [ ] Update AccountDetailScreen to use shared configs with account-filtered data
- [ ] Update AccountsViewScreen to use shared configs with selected-accounts-filtered data
- [ ] Ensure ViewModels provide appropriate filtered data computation
- [ ] Ensure screens set config values appropriately for their context

### Phase 6: Build and Verify (Week 3)

**Actions:**
- [ ] Build application
- [ ] Fix compilation errors
- [ ] Test DashboardScreen with all cards
- [ ] Test AccountDetailScreen with all cards
- [ ] Test AccountsViewScreen with all cards
- [ ] Verify data filtering works correctly for each screen context
- [ ] Verify config values are set correctly for each screen context

---

## Benefits of Simplified Architecture

### Maintainability
- ✅ DRY Principle: No duplicate card variants
- ✅ Single Source of Truth: One CardRenderData per card type
- ✅ Shared Config: One CardConfig per card type
- ✅ Clear Responsibility: Screens filter data, renderer presents data

### Extensibility
- ✅ Easy to Add Screens: New screens use existing card types with different data
- ✅ Easy to Add Cards: Add one CardRenderData, one CardConfig, one renderer case
- ✅ No Renderer Changes for New Screens: Screens just filter data differently

### Testability
- ✅ Unit Testable: Each card type testable once
- ✅ Screen Testable: Data filtering logic testable per screen
- ✅ Config Testable: Config logic testable independently

### Developer Experience
- ✅ Clear Patterns: Screens filter, renderer presents
- ✅ Less Code: Fewer files, fewer classes
- ✅ Consistent: Same card type always uses same structure

---

## Migration Strategy

### Incremental Approach
1. **Consolidate CardRenderData First:** Remove screen-specific data classes
2. **Update Screens to Use Unified Data:** Screens handle filtering
3. **Consolidate CardConfig:** Remove screen-specific configs
4. **Consolidate CardCallbacks:** Remove screen-specific callbacks
5. **Update Renderer:** Remove screen-specific cases
6. **Test Thoroughly:** Verify each screen context works correctly

### Backward Compatibility
- No breaking changes to card component APIs
- Screens gradually adopt unified types
- Can test each screen independently

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Data filtering errors | Medium | High | Thorough testing per screen |
| Config value errors | Low | Medium | Clear documentation, examples |
| Performance regression | Low | Low | Data filtering is cheap |
| Breaking existing screens | Medium | High | Incremental migration, testing |

---

## Success Metrics

### Code Quality
- Reduce CardRenderData from 24 to 15 types (eliminate 9 screen-specific variants)
- Reduce CardConfig variants by 50%
- Reduce CardCallbacks variants by 50%
- Reduce UnifiedCardRenderer cases by 25%

### Developer Experience
- Add new screen with existing cards in <1 hour
- Clear data filtering patterns documented
- No screen-specific card types needed

### Performance
- No increase in memory usage
- No increase in build time
- Data filtering overhead <5ms

---

## Conclusion

The simplified architecture eliminates screen-specific card variants by making screens responsible for data filtering and configuration. This aligns with DRY principles and reduces complexity while maintaining type safety and separation of concerns.

**Recommendation:** Proceed with Phase 1 (Consolidate CardRenderData) immediately as it provides the foundation for all subsequent phases with minimal risk. The full consolidation should be completed over 3 weeks with careful testing at each phase.

---

## Appendix

### A. Card Type Consolidation Map

| Card Type | Current Variants | Unified Type | Variants to Remove |
|-----------|----------------|--------------|-------------------|
| NetWorth | NetWorth, AccountsViewNetWorth | NetWorth | AccountsViewNetWorth |
| CurrencyBalances | CurrencyBalances, AccountCurrencyBalances, AccountsViewCurrencyBalances | CurrencyBalances | AccountCurrencyBalances, AccountsViewCurrencyBalances |
| CashFlow | CashFlow, AccountCashFlow, AccountsViewCashFlow | CashFlow | AccountCashFlow, AccountsViewCashFlow |
| Accounts | Accounts, AccountsViewAccountsList | Accounts | AccountsViewAccountsList |

### B. Data Filtering Examples

**DashboardScreen - Global Data:**
```kotlin
val allAccounts = accountRepository.getAllAccounts()
val globalTimeline = viewModel.globalTimelineRange
val netWorthState = computeNetWorth(allAccounts, globalTimeline)
```

**AccountDetailScreen - Single Account Data:**
```kotlin
val account = accountRepository.getAccountById(accountId)
val accountTimeline = viewModel.accountTimelineRange
val netWorthState = computeNetWorth(listOf(account), accountTimeline)
```

**AccountsViewScreen - Selected Accounts Data:**
```kotlin
val selectedAccounts = accountRepository.getAccountsByIds(selectedAccountIds)
val accountsViewTimeline = viewModel.accountsViewTimelineRange
val netWorthState = computeNetWorth(selectedAccounts, accountsViewTimeline)
```

### C. References
- DRY Principle (Don't Repeat Yourself)
- Single Responsibility Principle
- Clean Architecture by Robert C. Martin
- Jetpack Compose Best Practices
 */
interface CardCallbacks {
    val onDelete: () -> Unit
}

/**
 * Enum for card type identification
 */
enum class CardType {
    NET_WORTH,
    CURRENCY_BALANCES,
    CASH_FLOW,
    ACCOUNTS,
    BUDGETS,
    ACCOUNT_HERO,
    CATEGORY_BREAKDOWN,
    TIMELINE_TRAJECTORY,
    CATEGORY_EXPENSE_TREEMAP,
    INCOME_EXPENSES_SUMMARY,
    PERIOD_COMPARISON,
    CASH_FLOW_TREND,
    CASH_FLOW_PROGRESS,
    BALANCE_FORECAST,
    PLANNED_PAYMENTS
}

/**
 * Interface for card rendering strategy
 */
interface CardRendererStrategy<T : CardData, C : CardCallbacks> {
    val cardType: CardType
    val supportedDataClass: KClass<T>
    val supportedCallbacksClass: KClass<C>
    
    @Composable
    fun render(
        data: T,
        callbacks: C,
        modifier: Modifier,
        headerModifier: Modifier,
        showTitle: Boolean,
        currentTheme: String
    )
}
```

### Phase 2: Strategy Pattern Implementation

**Goal:** Extract each card type into its own renderer strategy.

```
┌─────────────────────────────────────────────────────────────┐
│                    CardRendererRegistry                     │
│  - Map<CardType, CardRendererStrategy>                      │
│  - register(strategy)                                       │
│  - getRenderer(cardType)                                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Uses
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              CardRendererStrategy Interface                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Implemented by
                       ▼
┌──────────────────┬──────────────────┬────────────────────┐
│ NetWorthRenderer │ AccountsRenderer │ AnalyticsRenderer  │
└──────────────────┴──────────────────┴────────────────────┘
```

**Example Implementation:**
```kotlin
class NetWorthCardRenderer : CardRendererStrategy<CardRenderData.NetWorth, CardCallbacks.WithFontSizeAndTimeline> {
    override val cardType = CardType.NET_WORTH
    override val supportedDataClass = CardRenderData.NetWorth::class
    override val supportedCallbacksClass = CardCallbacks.WithFontSizeAndTimeline::class
    
    @Composable
    override fun render(
        data: CardRenderData.NetWorth,
        callbacks: CardCallbacks.WithFontSizeAndTimeline,
        modifier: Modifier,
        headerModifier: Modifier,
        showTitle: Boolean,
        currentTheme: String
    ) {
        NetWorthCard(
            uiState = data.uiState,
            cardId = data.cardId,
            fontSize = data.fontSize,
            timelineRange = data.timelineRange,
            onFontSizeChange = callbacks.onFontSizeChange,
            onTimelineRangeChange = callbacks.onTimelineRangeChange,
            onDelete = { callbacks.onDelete() },
            onClickIncome = {},
            onClickExpense = {},
            modifier = modifier.fillMaxWidth(),
            headerModifier = headerModifier
        )
    }
}
```

### Phase 3: Dependency Injection Integration

**Goal:** Use Hilt to manage renderer lifecycle and registration.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CardRendererModule {
    
    @Provides
    @IntoMap
    @CardRendererKey(CardType.NET_WORTH)
    fun provideNetWorthRenderer(): CardRendererStrategy<*, *> = NetWorthCardRenderer()
    
    @Provides
    @IntoMap
    @CardRendererKey(CardType.CURRENCY_BALANCES)
    fun provideCurrencyBalancesRenderer(): CardRendererStrategy<*, *> = CurrencyBalancesCardRenderer()
    
    // ... other renderers
    
    @Provides
    @Singleton
    fun provideCardRendererRegistry(
        renderers: Map<CardType, @JvmSuppressWildcards CardRendererStrategy<*, *>>
    ): CardRendererRegistry = CardRendererRegistry(renderers)
}
```

### Phase 4: UnifiedCardRenderer Simplification

**Goal:** Reduce renderer to simple dispatch logic.

```kotlin
@Composable
fun UnifiedCardRenderer(
    renderData: CardRenderData,
    callbacks: CardCallbacks,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    showTitle: Boolean = false,
    currentTheme: String = MaterialTheme.colorScheme.toThemeString(),
    registry: CardRendererRegistry = inject()
) {
    val renderer = registry.getRenderer(renderData.cardType)
    
    renderer.render(
        data = renderData,
        callbacks = callbacks,
        modifier = modifier,
        headerModifier = headerModifier,
        showTitle = showTitle,
        currentTheme = currentTheme
    )
}
```

**Result:** Renderer reduced from 500+ lines to ~20 lines.

### Phase 5: Data Layer Consolidation

**Goal:** Eliminate data duplication through composition.

```kotlin
// Instead of:
data class NetWorth(...)
data class AccountsViewNetWorth(...)

// Use:
data class NetWorthCardData(
    val uiState: NetWorthUiState,
    val config: NetWorthCardConfig
) : CardData {
    override val cardId = config.cardId
    override val cardType = CardType.NET_WORTH
}

// Screens can create the same data type with different configs
val netWorthData = NetWorthCardData(
    uiState = computedUiState,
    config = dashboardConfig // or accountsViewConfig
)
```

---

## Refactoring Roadmap

### Phase 1: Foundation (Week 1-2)
- [ ] Define `CardData`, `CardCallbacks` base interfaces
- [ ] Create `CardType` enum
- [ ] Define `CardRendererStrategy` interface
- [ ] Update existing `CardRenderData` subtypes to implement `CardData`
- [ ] Update existing `CardCallbacks` subtypes to implement `CardCallbacks`
- [ ] Add unit tests for interface contracts
- **Risk:** Low - additive changes only

### Phase 2: Strategy Extraction (Week 3-4)
- [ ] Create `NetWorthCardRenderer` strategy
- [ ] Create `CurrencyBalancesCardRenderer` strategy
- [ ] Create `CashFlowCardRenderer` strategy
- [ ] Create `AccountsCardRenderer` strategy
- [ ] Create `BudgetsCardRenderer` strategy
- [ ] Create `AccountHeroCardRenderer` strategy
- [ ] Create analytics card renderers (9 strategies)
- [ ] Create AccountsView card renderers (4 strategies)
- [ ] Add unit tests for each strategy
- **Risk:** Medium - requires careful testing of each extracted renderer

### Phase 3: Registry Implementation (Week 5)
- [ ] Create `CardRendererRegistry`
- [ ] Implement manual registration
- [ ] Update `UnifiedCardRenderer` to use registry
- [ ] Add integration tests
- **Risk:** Medium - changes core dispatch logic

### Phase 4: Dependency Injection (Week 6)
- [ ] Add Hilt annotations to strategies
- [ ] Create `CardRendererModule`
- [ ] Define `CardRendererKey` map key
- [ ] Configure DI container
- [ ] Update registry to use DI
- [ ] Add DI tests
- **Risk:** Low - standard Hilt integration

### Phase 5: Data Consolidation (Week 7-8)
- [ ] Identify duplicated card data types
- [ ] Create unified data classes
- [ ] Update screens to use unified data
- [ ] Remove deprecated data classes
- [ ] Add migration tests
- **Risk:** High - requires careful data migration

### Phase 6: Cleanup & Polish (Week 9)
- [ ] Remove hardcoded strings
- [ ] Add comprehensive KDoc
- [ ] Create architecture diagrams
- [ ] Add preview support
- [ ] Add error handling
- [ ] Performance optimization
- **Risk:** Low - polish work

---

## Benefits of Proposed Architecture

### Maintainability
- ✅ Single Responsibility: Each renderer handles one card type
- ✅ Open/Closed: Add cards without modifying core renderer
- ✅ DRY: Eliminated data duplication
- ✅ Clear separation of concerns

### Extensibility
- ✅ Plugin-based: New cards = new strategy + DI registration
- ✅ No core changes: Adding cards doesn't touch `UnifiedCardRenderer`
- ✅ Type-safe: Compile-time checking of strategy registration

### Testability
- ✅ Unit testable: Each strategy testable in isolation
- ✅ Mockable: Can mock strategies for integration tests
- ✅ Previewable: Each card can have its own preview

### Performance
- ✅ Lazy loading: Strategies loaded via DI
- ✅ No reflection: Type-safe dispatch
- ✅ Composable-friendly: No recomposition issues

### Developer Experience
- ✅ Clear contracts: Interfaces define expectations
- ✅ Consistent patterns: All strategies follow same structure
- ✅ Easy onboarding: New developers understand pattern quickly

---

## Migration Strategy

### Incremental Approach
1. **Parallel Implementation:** Keep old renderer alongside new during migration
2. **Feature Flags:** Use flags to switch between old/new per screen
3. **Gradual Rollout:** Migrate one screen at a time
4. **Monitoring:** Track performance and errors during rollout

### Backward Compatibility
```kotlin
@Deprecated("Use strategy-based renderer", ReplaceWith("UnifiedCardRendererV2"))
@Composable
fun UnifiedCardRendererLegacy(...) {
    // Keep old implementation temporarily
}
```

### Testing Strategy
1. **Snapshot Testing:** Compare old vs new renderer output
2. **Integration Testing:** Ensure screens work with both renderers
3. **Performance Testing:** Measure recomposition counts
4. **User Testing:** Beta test with power users

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Breaking existing functionality | Medium | High | Comprehensive testing, gradual rollout |
| Performance regression | Low | Medium | Benchmark before/after, lazy loading |
| Increased complexity | Medium | Medium | Clear documentation, training |
| DI configuration errors | Low | High | Compile-time checks, integration tests |
| Data migration issues | High | High | Thorough migration testing, rollback plan |

---

## Success Metrics

### Code Quality
- Reduce `UnifiedCardRenderer` from 553 to <50 lines
- Reduce `CardRenderData` from 24 to <15 types (eliminate duplication)
- Achieve 80%+ test coverage for strategies
- Zero compilation warnings

### Developer Experience
- Add new card type in <30 minutes (vs current 2+ hours)
- No changes to core renderer for new cards
- Clear, searchable documentation

### Performance
- No increase in APK size
- No increase in memory usage
- No increase in build time
- Maintain or improve recomposition counts

---

## Conclusion

The current `UnifiedCardRenderer` successfully achieves its goal of centralizing card rendering, but the monolithic implementation will become a maintenance burd
en as the application grows. The proposed plugin-based architecture with dependency injection addresses all identified issues while maintaining the benefits of type safety and separation of concerns.

**Recommendation:** Proceed with Phase 1 (Foundation) immediately as it provides the groundwork for all subsequent phases with minimal risk. The full refactoring should be completed over 9 weeks with careful testing at each phase.

---

## Appendix

### A. Current Card Types Summary
| Category | Card Types | Count |
|----------|-----------|-------|
| Dashboard | NetWorth, CurrencyBalances, CashFlow, Accounts, Budgets | 5 |
| Account Detail | AccountHero, AccountCashFlow, AccountCurrencyBalances | 3 |
| Analytics | CategoryBreakdown, TimelineTrajectory, CategoryExpenseTreemap, IncomeExpensesSummary, PeriodComparison, CashFlowTrend, CashFlowProgress, BalanceForecast, PlannedPayments | 9 |
| Accounts View | AccountsViewNetWorth, AccountsViewCurrencyBalances, AccountsViewCashFlow, AccountsViewAccountsList | 4 |
| **Total** | | **21** |

### B. References
- Clean Architecture by Robert C. Martin
- SOLID Principles
- Strategy Pattern (GoF)
- Jetpack Compose Best Practices
- Hilt Documentation

### C. Glossary
- **SRP:** Single Responsibility Principle
- **OCP:** Open/Closed Principle
- **DI:** Dependency Injection
- **MVI:** Model-View-Intent
- **UDF:** Unidirectional Data Flow
