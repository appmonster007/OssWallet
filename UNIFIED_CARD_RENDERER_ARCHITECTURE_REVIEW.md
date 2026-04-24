# UnifiedCardRenderer Architecture Review & Refactoring Plan

**Document Version:** 1.0  
**Date:** April 24, 2026  
**Author:** Senior Android Architect  
**Status:** Review Complete

---

## Executive Summary

The current `UnifiedCardRenderer` implementation successfully centralizes card rendering logic across the application, but suffers from several architectural anti-patterns that will impact long-term maintainability, extensibility, and testability. This document provides a comprehensive analysis and proposes a refactoring roadmap aligned with Clean Architecture and SOLID principles.

**Key Findings:**
- ✅ **Strengths:** Type-safe sealed classes, separation of data/callbacks, successful consolidation of rendering logic
- ⚠️ **Concerns:** Monolithic renderer (500+ lines), tight coupling, no extension mechanism, callback signature inconsistency
- 🎯 **Recommendation:** Incremental refactoring to plugin-based architecture with dependency injection

---

## Current Architecture Analysis

### 1. Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Screens (UI Layer)                        │
│  AnalyticsScreen, DashboardScreen, AccountDetailScreen,    │
│  AccountsViewScreen                                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Uses
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              UnifiedCardRenderer (UI Layer)                 │
│  - Single composable with 500+ line when statement          │
│  - Directly imports 20+ card composables                     │
│  - Handles 24 card types, 18 callback types                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Uses
                       ▼
┌─────────────────────────────────────────────────────────────┐
│         CardRenderData & CardCallbacks (UI Layer)           │
│  - Sealed class hierarchies for type safety                  │
│  - 24 CardRenderData subtypes                                │
│  - 18 CardCallbacks subtypes                                 │
└─────────────────────────────────────────────────────────────┘
```

### 2. File Structure

| File | Lines | Purpose | Issues |
|------|-------|---------|--------|
| `UnifiedCardRenderer.kt` | 553 | Central rendering logic | Monolithic, violates SRP |
| `CardRenderData.kt` | 246 | Data contracts | Data duplication, no common interface |
| `CardCallbacks.kt` | 190 | Callback contracts | Inconsistent signatures, no base interface |

---

## Architectural Issues

### Critical Issues (Must Fix)

#### 1. **Violation of Single Responsibility Principle**
**Problem:** `UnifiedCardRenderer` handles:
- Type dispatching
- Callback casting with fallback logic
- Data mapping
- Direct composable calls
- Theme handling
- Modifier application

**Impact:** Any change to one concern risks breaking others. Testing individual concerns is impossible.

**Current Code:**
```kotlin
@Composable
fun UnifiedCardRenderer(
    renderData: CardRenderData,
    callbacks: CardCallbacks,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    showTitle: Boolean = false,
    currentTheme: String = "light"
) {
    when (renderData) {
        is CardRenderData.NetWorth -> {
            val cb = callbacks as? CardCallbacks.WithFontSizeAndTimeline 
                ?: CardCallbacks.WithFontSizeAndTimeline(...) // Fallback logic
            NetWorthCard(...) // Direct call
        }
        // ... 23 more cases
    }
}
```

#### 2. **Tight Coupling to Card Implementations**
**Problem:** Renderer directly imports and calls 20+ specific card composables:
- `NetWorthCard`, `CurrencyBalancesCard`, `CashFlowCard`
- `AccountsCard`, `BudgetsCard`
- `AccountHeroCard`
- 9 Analytics cards
- 4 AccountsView cards

**Impact:** 
- Cannot add cards without modifying renderer
- Cannot mock card implementations for testing
- Breaking changes propagate through entire chain

#### 3. **No Extension Mechanism (Open/Closed Principle Violation)**
**Problem:** Adding a new card type requires:
1. Adding subtype to `CardRenderData`
2. Adding subtype to `CardCallbacks`
3. Adding case to `UnifiedCardRenderer` when statement
4. Adding imports to renderer
5. Updating all screens that might use it

**Impact:** High friction for adding features, encourages code duplication.

#### 4. **Callback Signature Inconsistency**
**Problem:** Callback signatures vary inconsistently:

| Callback Type | Signature Pattern |
|--------------|-------------------|
| `WithFontSizeAndTimeline` | `(String, String) -> Unit`, `(String, TimelineRange) -> Unit` |
| `Accounts` | `(Int) -> Unit`, `(String) -> Unit`, `(TimelineRange) -> Unit` |
| `AccountCashFlow` | `(Boolean) -> Unit`, `(TimelineRange) -> Unit` |
| `AccountsViewAccountsList` | `(String, Int) -> Unit`, `(String, String) -> Unit` |

**Impact:** Cognitive load for developers, prone to errors, difficult to generate automatically.

#### 5. **Type Safety Issues with Fallback Logic**
**Problem:** Extensive use of `as?` casting with fallback:
```kotlin
val cb = callbacks as? CardCallbacks.WithFontSizeAndTimeline 
    ?: CardCallbacks.WithFontSizeAndTimeline(
        onFontSizeChange = { _, _ -> },
        onTimelineRangeChange = { _, _ -> },
        onDelete = {}
    )
```

**Impact:** 
- Silent failures when wrong callback type is passed
- Masking of real bugs
- Empty fallbacks may cause unexpected behavior

### Moderate Issues (Should Fix)

#### 6. **Data Duplication**
**Problem:** Similar card types duplicated:
- `NetWorth` vs `AccountsViewNetWorth` (both use `NetWorthCard`)
- `CurrencyBalances` vs `AccountCurrencyBalances` vs `AccountsViewCurrencyBalances`
- `CashFlow` vs `AccountCashFlow` vs `AccountsViewCashFlow`

**Impact:** Code duplication, maintenance burden, potential divergence.

#### 7. **Missing Common Interface**
**Problem:** No base interface for cards. Cannot:
- Treat cards polymorphically
- Define common operations (e.g., `cardId`, `config`)
- Create generic card containers

#### 8. **Hardcoded Values**
**Problem:** Hardcoded strings in renderer:
```kotlin
currentTheme = "light"
settingsTitle = "Dashboard Settings"
settingsTitle = "Accounts View Settings"
```

**Impact:** No theming support, no internationalization, maintenance issues.

#### 9. **No State Management Strategy**
**Problem:** Renderer is purely functional but screens still use `remember`, `LaunchedEffect`, `produceState` for data fetching.

**Impact:** Inconsistent patterns, potential performance issues, no clear data flow.

### Minor Issues (Nice to Fix)

#### 10. **Inadequate Documentation**
**Problem:** Minimal KDoc, no architecture diagrams, no usage examples.

#### 11. **No Preview Support**
**Problem:** Preview is empty placeholder, cannot visually test cards in isolation.

#### 12. **No Error Handling**
**Problem:** No explicit error states, no loading states, no empty states.

---

## Proposed Architecture

### Phase 1: Interface-Based Abstraction (Foundation)

**Goal:** Define clear contracts without changing implementation.

```kotlin
/**
 * Base interface for all card render data
 */
interface CardData {
    val cardId: String
    val cardType: CardType
}

/**
 * Base interface for all card callbacks
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

The current `UnifiedCardRenderer` successfully achieves its goal of centralizing card rendering, but the monolithic implementation will become a maintenance burden as the application grows. The proposed plugin-based architecture with dependency injection addresses all identified issues while maintaining the benefits of type safety and separation of concerns.

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
