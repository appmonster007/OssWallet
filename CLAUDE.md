# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all JVM unit tests
./gradlew test

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.osscabinet.ossWallet.domain.service.BudgetServiceTest"

# Run instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Run a single instrumented test class
./gradlew connectedDebugAndroidTest --tests "com.osscabinet.ossWallet.data.local.BudgetDaoTest"

# Lint check
./gradlew lint

# Install debug build on connected device
./gradlew installDebug
```

## Local Configuration

Create `local.properties` with:
```
sdk.dir=/path/to/Android/sdk
CURRENCY_API_KEY=your_api_key_here
BUY_ME_A_COFFEE_URL=https://buymeacoffee.com/your_page_name
```

`CURRENCY_API_KEY` is required for currency exchange rate fetching. The build will fail without it.

`BUY_ME_A_COFFEE_URL` is optional — it backs the periodic donation prompt (`BuildConfig.BUY_ME_A_COFFEE_URL`). Omitting it falls back to `https://buymeacoffee.com/osscabinet`; the build does not fail.

`ENABLE_BUY_ME_A_COFFEE` is optional — set to `false` to compile out the donation popup and Settings > Support Us page entirely, for every build variant. Defaults to `true`.

`ENABLE_BUY_ME_A_COFFEE_RELEASE` is optional and release-only — overrides `ENABLE_BUY_ME_A_COFFEE` for `release` builds specifically (e.g. your own locally-signed build), without touching the default debug/global behavior. Unset by default, so release follows `ENABLE_BUY_ME_A_COFFEE` like everything else.

## Key Versions

- AGP: 9.2.1 · Kotlin: 2.2.10 · Compose BOM: 2026.04.01
- Room: 2.7.0 · SQLCipher: 4.6.1
- Min SDK: 26 · Target/Compile SDK: 36 · Java: 11
- All dependency versions are in `gradle/libs.versions.toml`

---

## Architecture

Single-module Android app (`com.osscabinet.ossWallet`) following Clean Architecture with MVVM.

**Dependency rule:** `ui → domain ← data`

| Layer | Responsibility |
|-------|---------------|
| `domain/` | Pure Kotlin only — no `android.*` or `androidx.*` imports. Interfaces, models, use cases, services. |
| `data/` | Android-aware implementations of domain interfaces. Room entities, DAOs, Retrofit, mappers. Never expose entities or DTOs outside this layer. |
| `ui/` | Compose screens + ViewModels. ViewModels depend on `domain/` only — never on `data/` directly. |

### Dependency Injection

**Hilt** — `OssWalletApp` is annotated `@HiltAndroidApp`. ViewModels use `@HiltViewModel`. Repositories and services are wired in `di/` modules (e.g., `RepositoryModule`, `UseCaseModule`, `WebServerModule`). When adding a new repository, declare the binding in the appropriate `@Module` in `di/`.

### Database

Room (`AppDatabase`) encrypted with **SQLCipher**. Current schema version: **1**. Passphrase managed by `SecurePassphraseManager` — never instantiate `AppDatabase` directly; use `@Inject` via Hilt. Migrations live in `AppDatabase.kt` companion object (none currently — pre-release, so the schema is reset to v1 whenever it changes rather than accumulating a migration chain; this resets once the app ships).

**Hard rules:**
- Never use `fallbackToDestructiveMigration()`.
- Pre-release, there are no real installs to preserve: every schema change keeps `version = 1` (no `Migration` object), plus updates `@Database(entities=[...])`, the abstract DAO accessor, and deletes the stale `core/data/schemas/.../1.json` snapshot so it regenerates. Once the app ships, this flips back to the normal rule — incremented version + a real `Migration` object per change — and `fallbackToDestructiveMigration()` stays forbidden either way.
- All queries must include `profileId` — missing it leaks data across profiles.
- Store monetary values as `TEXT` via `BigDecimalConverter`, never as `REAL`.

### ViewModels

All ViewModels extend `BaseViewModel`. State is `StateFlow<UiState>` collected with `collectAsStateWithLifecycle()`. Always use `SharingStarted.WhileSubscribed(5000)` with `stateIn()`. Profile-scoped data always uses `flatMapLatest` on `settingsRepository.currentProfileId`.

### Navigation

Single-activity (`MainActivity`). All routes are in `ui/navigation/OssWalletNavGraph.kt`. Routes are `@Serializable` classes/objects in `ui/navigation/NavigationRoutes.kt`. Pass navigation as lambdas into screens — never use `NavController` directly inside a `@Composable`.

**`AppNavigator`** (`ui/navigation/AppNavigator.kt`) is the single place navigation intents should flow through for drawer taps, back-press, and widget deep links, so `NavOptions` follow from the route's category rather than being chosen per call site. Routes implementing `TopLevelRoute` (Dashboard/Records/Analytics/ExchangeRates) are sibling tab switches: they save/restore back stack state and use the shorter `SIBLING_SWITCH_DURATION` fade (`NavigationTransitions.kt`); everything else is a hierarchical push using the expressive slide. Both are single-top.

**Known scope gap, not yet addressed:** the ~80 `navController.navigate(...)` calls inside `ui/navigation/graphs/*.kt` (account detail, transaction entry, budget/loan/planned-payment flows, settings sub-screens) do **not** go through `AppNavigator` — they're push-style detail navigations, not rapid sibling-switch taps, so they don't exhibit the bug class `AppNavigator` was built to fix, and migrating ~80 call sites in one pass was judged too large a regression surface to bundle in alongside that fix. If a similar "tap didn't register" / back-stack-duplication report ever surfaces on a *detail* screen (not the drawer), migrating its call site to `AppNavigator.navigateTo(...)` is the fix — don't re-invent a local patch for it.

---

## Dashboard Card System

Cards use a registry-based plugin architecture. **Adding a new `CardType` requires updating exactly 15 files in order** — missing any causes a silent runtime crash on card reorder (not a compile error). See `docs/guides/adding-new-card.md` for full step-by-step examples.

1. `domain/card/CardType.kt` — add enum entry
2. `domain/model/settings/CardConfig.kt` — add `@Serializable data class XxxCardConfig : CardConfig()`
3. `domain/card/CardRenderData.kt` — add `data class Xxx : CardRenderData()`
4. `domain/card/computers/XxxCardComputer.kt` — NEW FILE, implements `CardDataComputer`
5. `domain/card/CardComputerRegistry.kt` — add to `DEFAULT` list
6. `domain/card/CardUpdateHandler.kt` — add `updateCardXxxConfig()`
7. `ui/components/cards/CardCallbacks.kt` — add `data class Xxx : CardCallbacks()`
8. `ui/components/cards/CardWithCallbacks.kt` — add `data class Xxx : CardWithCallbacks()`
9. `ui/components/cards/CardUiBridge.kt` — add **two** `when` branches (compute + pair)
10. `ui/dashboard/components/XxxCard.kt` — NEW FILE, the Compose composable
11. `ui/components/cards/UnifiedCardRenderer.kt` — add `is CardWithCallbacks.Xxx -> XxxCard(...)`
12. `data/repository/CardRepositoryImpl.kt` — add branch in **all three** `reorder*` `when` blocks
13. `ui/viewmodel/CardLayoutViewModel.kt` — add `CardType.XXX ->` in `addDashboardCard()`
14. `ui/navigation/Screen.kt` — add routes for new screens (if any)
15. `ui/navigation/OssWalletNavGraph.kt` — wire new screens

`CardInput` invariant: every field must have a default value; always use named parameters when constructing `CardInput`.

---

## Aeon Design System

All reusable UI components are prefixed `Aeon` and live in `ui/components/`. Check there before creating new UI primitives.

| Need | File |
|------|------|
| Buttons | `AeonButtons.kt` |
| Cards / surfaces | `AeonCards.kt` |
| Text inputs | `AeonInputs.kt` |
| Dialogs | `AeonAlertDialog.kt` |
| Charts | `ui/components/visualization/charts/Aeon*Chart.kt` |
| Glass surfaces | `GlassSurface.kt` — `GlassSurface`, `AeonGlassHost`, `AeonHazeProvider`, `AeonGlassSheet` |

Color tokens live in `ui/theme/Color.kt` — never hardcode hex values in composables. Color layering rules are in `.gemini/color-usage-guide.md`.

### Glass / Glassmorphism

All glass surfaces use `GlassSurface` — never write `hazeEffect` inline. Dashboard card composables use `AeonExpressiveCard` as their shell, which wraps `GlassSurface + AeonGlassHost` automatically. To make a glass container blurable by its children, wrap its content in `AeonGlassHost`. Full details in `.gemini/glassmorphism.md`.

---

## Glance Widgets

Widget files live in `widget/`. Glance composables use `androidx.glance.*` — never `androidx.compose.*`. All data fetching happens **before** `provideContent {}`. Always wrap data loading in `runCatching` so `provideContent {}` is always reached. Use `WidgetChartRenderer` to produce `Bitmap`-based charts (Canvas-based Compose charts do not work in Glance).

---

## Key Domain Services

| Service | Purpose |
|---------|---------|
| `AggregationService` | Account balances, net worth, grouped summaries |
| `TransactionManager` | Transaction creation with linked-transfer support |
| `CurrencyService` | Amount conversion using cached exchange rates |
| `FilterService` / `TransactionFilterService` | Applies `TransactionFilter` predicates |
| `TimelineService` / `TimelineGroupingService` | Groups transactions by time range |
| `PlannedPaymentService` | Recurrence calculation and occurrence generation |

---

## Financial Arithmetic

- Always `BigDecimal` for money — never `Float` or `Double`.
- Construct from `String`: `BigDecimal("10.50")`, not `BigDecimal(10.5)`.
- Store as `TEXT` in Room via `BigDecimalConverter`; use `setScale(n, RoundingMode.HALF_UP)` before display.

---

## Testing

**JVM unit tests** (`src/test/`): domain services with plain JUnit4, no Android runtime. ViewModels with `kotlinx-coroutines-test` and `MainDispatcherRule`.

**Instrumented tests** (`src/androidTest/`): Room DAOs use `Room.inMemoryDatabaseBuilder` — no passphrase needed. Always `db.close()` in `@After`.

**Fakes over mocks**: use manual `Fake*` implementations backed by `MutableStateFlow`, not Mockito/MockK, for repository test doubles.

**BigDecimal assertions**: use `.isEqualByComparingTo()` (Google Truth) — not `assertEquals` — to avoid scale-mismatch failures.

---

## Debug Features

`ENABLE_SEEDING` BuildConfig flag is `true` in debug builds. `data/seed/SeedingConfig.kt` populates the database with test data on first run.

---

## Deeper Reference

The `.gemini/` directory contains detailed directives consulted when making architectural decisions:

- `.gemini/architecture.md` — layer rules, ViewModel patterns, card system, navigation
- `.gemini/coding.md` — Kotlin style, coroutines, error handling, naming conventions
- `.gemini/database.md` — entity checklist, migration template, DAO patterns, SQLCipher rules
- `.gemini/design-ui.md` — Aeon components, card rendering, Glance constraints, recomposition checklist
- `.gemini/glassmorphism.md` — glass effect architecture, `AeonHazeProvider`, `AeonGlassHost`, `GlassSurface`, patterns and pitfalls
- `.gemini/testing.md` — test structure, fake repository pattern, coverage expectations
