# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all unit tests
./gradlew test

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.osscabinet.ossWallet.ExpressionEvaluatorTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint check
./gradlew lint

# Clean build
./gradlew clean assembleDebug

# Install debug build on connected device
./gradlew installDebug
```

## Local Configuration

Copy `local.properties.example` (if present) or create `local.properties` with:
```
sdk.dir=/path/to/Android/sdk
CURRENCY_API_KEY=your_api_key_here
```

`CURRENCY_API_KEY` is required for currency exchange rate fetching. Without it, the build will fail.

## Architecture

This is a single-module Android app (`com.osscabinet.ossWallet`) following Clean Architecture with MVVM.

**Layer responsibilities:**
- `domain/` — Pure Kotlin. No Android imports. Contains interfaces, models, use cases, and services. Business logic lives here.
- `data/` — Implements domain repository interfaces. Owns Room entities, DAOs, Retrofit API, and mappers.
- `ui/` — Compose screens, ViewModels, and reusable components. ViewModels depend on domain, not data.

**Dependency direction:** `ui → domain ← data`

### Dependency Injection

Manual DI via `di/AppContainer.kt` — **no Hilt or Dagger**. `AppContainer` is instantiated in `OssWalletApp` and accessed via `(application as OssWalletApp).container` in ViewModel factories. When adding a new repository or service, wire it through `AppContainer`.

### Navigation

Single-activity (`MainActivity`). All navigation is in `ui/navigation/OssWalletNavGraph.kt` using Jetpack Compose Navigation. Routes are defined in `ui/navigation/Screen.kt`. Context objects (`DashboardViewContext`, `RecordsFilterContext`, `AccountsFilterContext`) are passed through the nav graph for screen-to-screen state sharing.

### Database

Room database (`AppDatabase`, currently v11) encrypted with **SQLCipher**. Passphrase managed by `SecurePassphraseManager`. When adding a new entity or modifying schema, increment the database version and add a migration to `AppDatabase.kt`. Never use `fallbackToDestructiveMigration()`.

### ViewModels

All ViewModels extend `BaseViewModel`. Use `GenericViewModelFactory` for simple VMs. Complex VMs have their own factory (e.g., `TransactionViewModelFactory`). State is exposed as `StateFlow`, subscribed with `WhileSubscribed(5000)`.

### Dashboard Card System

Cards on the dashboard use a registry-based plugin architecture:
- `CardType` enum defines card types
- `CardDataComputer` interface defines computation contract
- `CardComputerRegistry` maps types to computers
- `CardInput`/`CardRenderData` are the input/output data classes

**Adding a new card type requires updating exactly 15 files.** See `.claude/agents/android-architect.md` for the full ordered checklist. The highest-risk files are `CardRepositoryImpl.kt` (3 non-exhaustive `when` blocks that won't cause compile errors if missed — they cause silent runtime crashes on card reorder) and `CardUiBridge.kt` (requires two `when` branches per new type).

**`CardInput` invariant:** Every field added to `CardInput` must have a default value so existing call sites compile without changes. Always use named parameters when constructing `CardInput`.

### UI Components (Aeon Design System)

Reusable composables live in `ui/components/` and are prefixed with `Aeon` (e.g., `AeonButtons`, `AeonCards`, `AeonInputs`). Use these before creating new UI primitives. Charts are built with **Vico** and wrapped in `ui/components/visualization/charts/`.

### Key Services

- `AggregationService` — computes account balances, net worth, and grouped summaries
- `TransactionManager` — handles transaction creation with linked transfer support
- `CurrencyConversionService` — converts amounts between currencies using cached rates
- `FilterService` / `TransactionFilterService` — applies `TransactionFilter` predicates to queries
- `TimelineService` / `TimelineGroupingService` — groups transactions by time range for analytics

## Key Versions

- AGP: 9.1.1
- Kotlin: 2.2.10
- Compose BOM: 2024.09.00
- Java compatibility: 11
- Min SDK: 26, Target/Compile SDK: 36
- Room: 2.7.0 (with SQLCipher 4.6.1)

All dependency versions are managed in `gradle/libs.versions.toml`.

## Debug-Only Features

`ENABLE_SEEDING` BuildConfig flag is `true` in debug builds. `data/seed/SeedingConfig.kt` populates the database with test data on first run in debug mode.

## Specialized Agent Guides

For deeper guidance on specific areas, use the agents in `.claude/agents/`:

- **`android-architect.md`** — layer boundary rules, card system 15-file checklist, ViewModel patterns, when to create a new service vs extending existing
- **`android-db.md`** — Room entity checklist, migration templates, SQL type mapping, SQLCipher passphrase rules, DAO naming conventions
- **`android-compose.md`** — Aeon design system components, card rendering rules, navigation patterns, Glance widget constraints, recomposition checklist
- **`android-testing.md`** — test location convention, domain service tests (JUnit4), ViewModel tests (coroutines-test), Room DAO tests (in-memory), fake repository pattern
