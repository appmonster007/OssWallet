# Card Architecture Improvement Plan

## Overview
This plan addresses the 5 current architecture issues and implements 8 best practices and optimization recommendations for the card rendering system.

## Current Issues to Fix

### Issue 1: Duplicate Extension Functions (DRY Violation)
**Problem:** CardCompute.kt has duplicate functions for the same card type:
- `CurrencyBalancesCardConfig.computeCurrencyTotals()` (FinancialUseCases)
- `CurrencyBalancesCardConfig.computeCurrencyTotalsWithCardComputationService()` (CardComputationService)
- `CashFlowCardConfig.computeCashFlowWithFinancialUseCases()` (FinancialUseCases)
- `CashFlowCardConfig.computeCashFlowWithCardComputationService()` (CardComputationService)

**Impact:** Confusing, violates DRY, harder to maintain

### Issue 2: Tight Coupling to Services
**Problem:** Extension functions depend on concrete classes (FinancialUseCases, CardComputationService) instead of interfaces

**Impact:** Harder to test, violates Dependency Inversion Principle

### Issue 3: Inconsistent Naming
**Problem:** 
- Some functions have "WithFinancialUseCases" suffix
- Some don't (computeNetWorth, computeAccountStats)
- Parameter naming inconsistent (allTransactions vs transactions)

**Impact:** Reduces code readability, confusing for developers

### Issue 4: Scattered Filtering Logic
**Problem:** Filtering logic is duplicated across multiple functions

**Impact:** Violates Single Responsibility Principle, harder to maintain

### Issue 5: Service Exposure
**Problem:** Services made `internal` in ViewModels to allow screens to access them, breaking encapsulation

**Impact:** Breaks encapsulation, tight coupling between UI and domain

---

## Implementation Plan

### Phase 1: Create CardComputationEngine Interface
**Priority:** HIGH

**Goal:** Create an abstraction for card computation to decouple from concrete service implementations

**Steps:**
1. Create `CardComputationEngine.kt` in `domain/service/`
2. Define interface with methods:
   ```kotlin
   interface CardComputationEngine {
       suspend fun computeNetWorth(
           config: NetWorthCardConfig,
           accounts: List<Account>,
           transactions: List<Transaction>,
           baseCurrency: String,
           exchangeRates: Map<String, BigDecimal>,
           precision: Int
       ): Pair<BigDecimal, CashFlowResult>
       
       suspend fun computeCurrencyTotals(
           config: CurrencyBalancesCardConfig,
           accounts: List<Account>,
           transactions: List<Transaction>,
           baseCurrency: String,
           exchangeRates: Map<String, BigDecimal>,
           precision: Int
       ): List<CurrencyAggregationItem>
       
       suspend fun computeCashFlow(
           config: CashFlowCardConfig,
           transactions: List<Transaction>,
           baseCurrency: String,
           exchangeRates: Map<String, BigDecimal>,
           precision: Int
       ): CashFlowResult
       
       suspend fun computeAccountStats(
           config: AccountsCardConfig,
           accounts: List<Account>,
           transactions: List<Transaction>,
           baseCurrency: String,
           exchangeRates: Map<String, BigDecimal>,
           precision: Int
       ): Pair<List<Account>, Map<String, AccountSummaryItem>>
   }
   ```

3. Make `CardComputationService` implement this interface
4. Create adapter for `FinancialUseCases` to implement this interface
5. Update DI to provide CardComputationEngine

**Files to Create:**
- `domain/service/CardComputationEngine.kt`
- `domain/service/FinancialUseCasesCardComputationAdapter.kt`

**Files to Modify:**
- `domain/service/CardComputationService.kt` (implement interface)
- `di/AppContainer.kt` (provide interface)

---

### Phase 2: Remove Duplicate Extension Functions and Unify Naming
**Priority:** HIGH

**Goal:** Remove duplicate functions and standardize naming conventions

**Steps:**
1. Remove duplicate extension functions from CardCompute.kt:
   - Keep only one version per card type
   - Remove `computeCurrencyTotalsWithCardComputationService`
   - Remove `computeCashFlowWithFinancialUseCases`
   - Remove `computeCashFlowWithCardComputationService`

2. Rename functions for consistency:
   - `computeCurrencyTotals` → `computeCurrencyTotalsWithFinancialUseCases` (or keep as is)
   - `computeNetWorth` → keep as is (already consistent)
   - `computeAccountStats` → keep as is (already consistent)

3. Standardize parameter names:
   - Use `transactions` consistently (not allTransactions)
   - Use `accounts` consistently

4. Update extension functions to use CardComputationEngine interface

**Files to Modify:**
- `ui/components/cards/CardCompute.kt`
- `ui/accountdetail/AccountDetailScreen.kt` (update function calls)
- `ui/screens/AccountsViewScreen.kt` (update function calls)

---

### Phase 3: Centralize Filtering Logic in CardFilter.kt
**Priority:** HIGH

**Goal:** Extract filtering logic into a separate utility file

**Steps:**
1. Create `CardFilter.kt` in `ui/components/cards/`
2. Define extension functions on CardConfig for filtering:
   ```kotlin
   fun CardConfig.filterAccounts(accounts: List<Account>): List<Account> {
       return when (this) {
           is NetWorthCardConfig -> {
               if (selectedAccountIds != null && selectedAccountIds.isNotEmpty()) {
                   accounts.filter { it.id in selectedAccountIds }
               } else {
                   accounts
               }
           }
           is CurrencyBalancesCardConfig -> {
               if (accountId != null) {
                   accounts.filter { it.id == accountId }
               } else {
                   accounts
               }
           }
           else -> accounts
       }
   }
   
   fun CardConfig.filterTransactions(transactions: List<Transaction>): List<Transaction> {
       return when (this) {
           is NetWorthCardConfig -> {
               if (selectedAccountIds != null && selectedAccountIds.isNotEmpty()) {
                   transactions.filter { it.accountId in selectedAccountIds }
               } else {
                   transactions
               }
           }
           is CurrencyBalancesCardConfig, is CashFlowCardConfig -> {
               val accountId = (this as? CurrencyBalancesCardConfig)?.accountId 
                            ?: (this as? CashFlowCardConfig)?.accountId
               if (accountId != null) {
                   transactions.filter { it.accountId == accountId }
               } else {
                   transactions
               }
           }
           else -> transactions
       }
   }
   ```

3. Update CardCompute.kt to use these filtering functions

**Files to Create:**
- `ui/components/cards/CardFilter.kt`

**Files to Modify:**
- `ui/components/cards/CardCompute.kt`

---

### Phase 4: Implement Caching for Computed Data in ViewModels
**Priority:** MEDIUM

**Goal:** Add caching to avoid recomputing card data on every recomposition

**Steps:**
1. Add cache map to ViewModels:
   ```kotlin
   private val cardDataCache = mutableMapOf<String, CachedCardData>()
   
   data class CachedCardData(
       val timestamp: Long,
       val data: Any
   )
   ```

2. Implement cache invalidation based on:
   - Card config changes
   - Data changes (accounts, transactions)
   - Timeline range changes

3. Add cache expiration (e.g., 5 minutes)

4. Update compute functions to check cache first

**Files to Modify:**
- `ui/viewmodel/AccountDetailViewModel.kt`
- `ui/viewmodel/AccountsViewViewModel.kt`
- `ui/viewmodel/DashboardViewModel.kt`

---

### Phase 5: Replace produceState with Flow in ViewModels
**Priority:** MEDIUM

**Goal:** Use Flow for reactive data instead of produceState in screens

**Steps:**
1. Move card data computation from screens to ViewModels
2. Create Flow-based card data streams in ViewModels:
   ```kotlin
   fun getCardData(cardId: String): StateFlow<CardRenderData?> {
       return cardConfigs.map { configs ->
           configs.find { it.cardId == cardId }
       }.flatMapLatest { config ->
           when (config) {
               is NetWorthCardConfig -> flow {
                   emit(CardRenderData.NetWorth(config, computeNetWorthData(config)))
               }
               // ... other card types
           }
       }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
   }
   ```

3. Update screens to use collectAsStateWithLifecycle instead of produceState

**Files to Modify:**
- `ui/viewmodel/AccountDetailViewModel.kt`
- `ui/viewmodel/AccountsViewViewModel.kt`
- `ui/accountdetail/AccountDetailScreen.kt`
- `ui/screens/AccountsViewScreen.kt`

---

### Phase 6: Remove Internal Service Exposure from ViewModels
**Priority:** HIGH

**Goal:** Remove internal service exposure and use proper dependency injection

**Steps:**
1. Change `internal val` back to `private val` in ViewModels:
   - `financialUseCases` in AccountsViewViewModel
   - `cardComputationService` in AccountDetailViewModel and AccountsViewViewModel

2. Pass CardComputationEngine to screens via ViewModel state or method calls

3. Update screens to get computed data from ViewModels instead of calling extension functions directly

**Files to Modify:**
- `ui/viewmodel/AccountDetailViewModel.kt`
- `ui/viewmodel/AccountsViewViewModel.kt`
- `ui/accountdetail/AccountDetailScreen.kt`
- `ui/screens/AccountsViewScreen.kt`

---

### Phase 7: Add Unit Tests for CardCompute Functions
**Priority:** MEDIUM

**Goal:** Ensure filtering and computation logic is correct

**Steps:**
1. Create test directory structure
2. Create test files:
   - `ui/components/cards/CardFilterTest.kt`
   - `ui/components/cards/CardComputeTest.kt`

3. Test scenarios:
   - Filtering with selectedAccountIds
   - Filtering with accountId
   - Filtering without filters
   - Empty data handling
   - Edge cases (null values, empty lists)

**Files to Create:**
- `app/src/test/java/com/osscabinet/ossWallet/ui/components/cards/CardFilterTest.kt`
- `app/src/test/java/com/osscabinet/ossWallet/ui/components/cards/CardComputeTest.kt`

---

### Phase 8: Performance Optimization
**Priority:** LOW

**Goal:** Improve performance for large datasets and rendering

**Steps:**
1. **Paging 3 Integration:**
   - Implement Paging 3 for account lists in AccountsCard
   - Implement Paging 3 for transaction lists if applicable

2. **Baseline Profiles:**
   - Create baseline profiles for card rendering
   - Add to `baselineprofile/src/main/AndroidManifest.xml`

3. **Lazy Loading:**
   - Use LazyColumn for card lists
   - Implement virtual scrolling for large card lists

4. **Memoization:**
   - Use `remember` for expensive computations in Compose
   - Use `derivedStateOf` for derived state

5. **Optimize Recomposition:**
   - Use `@Stable` annotations for data classes
   - Use key parameters in LazyColumn items

**Files to Modify:**
- `ui/dashboard/components/AccountsCard.kt`
- `baselineprofile/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`

---

## Build and Verification

### Final Step: Build and Verify All Changes
**Priority:** HIGH

**Steps:**
1. Run `./gradlew assembleDebug`
2. Run unit tests: `./gradlew testDebugUnitTest`
3. Run instrumentation tests if applicable
4. Manual testing on device/emulator
5. Verify no regressions in card rendering
6. Verify performance improvements

---

## Success Criteria

- [ ] No duplicate extension functions
- [ ] Consistent naming conventions
- [ ] Centralized filtering logic
- [ ] Services not exposed from ViewModels
- [ ] CardComputationEngine interface created and used
- [ ] Caching implemented for computed data
- [ ] Flow-based reactive data instead of produceState
- [ ] Unit tests added for CardCompute and CardFilter
- [ ] Performance optimizations implemented
- [ ] Build successful with no errors
- [ ] Tests passing
- [ ] Manual verification complete

---

## Estimated Effort

- Phase 1: 2-3 hours
- Phase 2: 1-2 hours
- Phase 3: 1-2 hours
- Phase 4: 2-3 hours
- Phase 5: 3-4 hours
- Phase 6: 2-3 hours
- Phase 7: 2-3 hours
- Phase 8: 3-4 hours
- Build & Verify: 1-2 hours

**Total Estimated Effort:** 17-26 hours

---

## Dependencies Between Phases

- Phase 1 must be completed before Phase 2
- Phase 3 can be done independently
- Phase 4 should be done after Phase 2
- Phase 5 should be done after Phase 4
- Phase 6 should be done after Phase 5
- Phase 7 can be done independently
- Phase 8 can be done independently

**Recommended Order:** 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → Build & Verify
