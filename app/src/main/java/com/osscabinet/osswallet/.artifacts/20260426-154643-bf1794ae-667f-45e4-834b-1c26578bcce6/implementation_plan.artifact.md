# Codebase Refactoring and Documentation Implementation Plan

## Goal
The goal was to create comprehensive architecture documentation and refactor the codebase to eliminate logic duplication, optimize function signatures, and adhere to Clean Architecture principles.

## Proposed Changes

### Data Layer
- **Documentation**: Exhaustive documentation of all entities, DAOs, and repositories in `docs/architecture/complete-architecture-documentation.md`.

### Domain Layer
- **New DTOs**:
  - `TransactionRequest.kt`: Consolidates ~15 parameters for transaction creation/updates.
  - `ComputationContext.kt`: Groups `baseCurrency`, `exchangeRates`, and `precision` for card computations.
- **Centralized Logic**:
  - `TransactionManager.kt`: Domain service for transaction lifecycle (clone, toggle status).
  - `FilterService.kt`: Centralized in-memory filtering and previous period resolution.
- **Utility**:
  - `CacheUtils.kt`: Centralized `CachedCardData` wrapper to eliminate redeclarations.

### UI Layer
- **ViewModel Refactoring**:
  - `TransactionViewModel.kt`: Delegated transaction actions to `TransactionManager`.
  - `AnalyticsViewModel.kt` & `DashboardViewModel.kt`: Delegated complex computations to `CardComputationService` using `ComputationContext`.
  - Unified all card compute call sites in `AccountDetailViewModel.kt` and `AccountsViewViewModel.kt`.
- **Screen Refactoring**:
  - `TransactionEntryScreen.kt`: Simplified `onSave` logic using `TransactionRequest`.
  - `DashboardScreen.kt`: Optimized account computation by destructuring combined result from `computeAccountStats`.

---

## Verification Plan

### Automated Tests
- `./gradlew app:assembleDebug`: Verified that the refactoring didn't break compilation and all signatures match.

### Manual Verification
- Verified code structure follows Clean Architecture (Domain layer doesn't depend on UI/Data details).
- Ensured DRY rule is followed by centralizing shared logic in Domain Services.
