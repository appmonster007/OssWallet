---
baseline_commit: ed4fb3231269d2bdbd1920b704745e9b1f4744e6
---

# Story 1.1: Core Infrastructure Baseline (Serialization & Concurrency)

## Status
**Status:** done
**Epic:** Epic 1: Architectural Foundation & Domain Purity
**ID:** 1.1

## Description
As a developer,
I want a standardized serialization and concurrency framework,
So that I can write thread-safe, modular code without framework-specific leakage.

## Acceptance Criteria

**Given** a project using Moshi and hardcoded `Dispatchers.IO`
**When** I implement a global `kotlinx.serialization` configuration and an injected `DispatcherProvider`
**Then** all `BigDecimal` math uses a consistent Kotlinx serializer
**And** `Retrofit` is configured with the `kotlinx-serialization-converter`
**And** all Repositories receive their dispatchers via Hilt constructor injection
**And** Moshi dependencies are completely removed from the project.

## Developer Context

### Exhaustive Artifact Analysis
- **Serialization Migration**: The project currently has a mix of Moshi and Kotlinx Serialization. We must move entirely to Kotlinx Serialization to satisfy **FR3**. This involves removing `libs.converter.moshi` and `libs.moshi.kotlin.codegen` from `build.gradle.kts`.
- **BigDecimal Handling**: `BigDecimal` is used extensively for financial calculations. A `BigDecimalSerializer` exists in `com.osscabinet.ossWallet.domain.model.util.Serializers.kt`. All DTOs and Domain models with `BigDecimal` fields must use this serializer.
- **Retrofit Configuration**: `RetrofitClient.kt` currently uses `MoshiConverterFactory`. This must be replaced with `asConverterFactory` from `kotlinx-serialization-converter`.
- **Concurrency (AD-9)**: Hardcoded `Dispatchers.IO` is used in `CurrencyRepositoryImpl.kt`, `BackupRepositoryImpl.kt`, and `CsvImportServiceImpl.kt`. This is banned. We need to inject a `DispatcherProvider` to enable reliable unit testing with `StandardTestDispatcher`.

### Technical Requirements
- **Dependencies**: 
  - Add `libs.retrofit.kotlinx.serialization` (com.squareup.retrofit2:converter-kotlinx-serialization:2.12.0 or 3.0.0).
  - Ensure `kotlinx.serialization.json` version matches (1.8.0).
- **Core Util**:
  - Create `com.osscabinet.ossWallet.core.util.DispatcherProvider` interface.
  - Create `com.osscabinet.ossWallet.core.util.RealDispatcherProvider` implementation.
- **DI**:
  - Create `com.osscabinet.ossWallet.di.DispatchersModule` to provide `DispatcherProvider`.
  - Update `AppModule.kt` if necessary (e.g., provide a global `Json` instance).

### Architecture Compliance
- **AD-1 (Domain Pureness)**: Ensure no DTO-specific logic leaks into the domain.
- **AD-9 (Dispatcher Injection)**: Mandatory injection for all asynchronous operations.

### File Structure Requirements
- **NEW**: `app/src/main/java/com/osscabinet/ossWallet/core/util/DispatcherProvider.kt`
- **NEW**: `app/src/main/java/com/osscabinet/ossWallet/di/DispatchersModule.kt`
- **UPDATE**: `app/src/main/java/com/osscabinet/ossWallet/data/remote/RetrofitClient.kt`
- **UPDATE**: `app/src/main/java/com/osscabinet/ossWallet/data/remote/ExchangeRateApi.kt` (Refactor DTOs)
- **UPDATE**: `app/src/main/java/com/osscabinet/ossWallet/data/remote/FrankfurterApi.kt` (Refactor DTOs)
- **UPDATE**: `app/src/main/java/com/osscabinet/ossWallet/data/repository/CurrencyRepositoryImpl.kt` (Inject dispatcher)
- **UPDATE**: `app/src/main/java/com/osscabinet/ossWallet/data/repository/BackupRepositoryImpl.kt` (Inject dispatcher)

### Testing Requirements
- Unit tests for Repositories must be updated to inject a `TestDispatcherProvider` or mock the provider.
- Verify that `CurrencyRepositoryImplTest.kt` still passes after the refactor.

## Project Context Reference
- **Project Name**: OssWallet
- **Architecture**: Clean Architecture + MVI
- **Design System**: Aeon (H — Editorial Instrument)

## Completion Status
- [x] Implement DispatcherProvider and DI module
- [x] Configure global Json instance with BigDecimal serializers
- [x] Refactor RetrofitClient to use Kotlinx Serialization
- [x] Migrate ExchangeRateApi and FrankfurterApi DTOs
- [x] Refactor CurrencyRepositoryImpl to use injected dispatchers
- [x] Remove Moshi dependencies and generated code
- [x] Verify build and existing tests

**Ultimate context engine analysis completed - comprehensive developer guide created**

## Dev Agent Record

### Implementation Plan
The branch already contained partial work for this story (DispatcherProvider + DI module,
RetrofitClient on kotlinx-serialization, ExchangeRateApi DTOs migrated, build/version
catalog already had Moshi removed and `converter-kotlinx-serialization` added). The dev
pass completed the remaining gaps:

1. Migrated the last Moshi-based DTO (`FrankfurterApi.FrankfurterResponse`) to
   `@Serializable`/`@SerialName`. After this, a project-wide grep for `com.squareup.moshi`
   returns zero results — satisfying the AC "Moshi dependencies are completely removed".
2. Applied AD-9 dispatcher injection: added a `DispatcherProvider` constructor parameter to
   `CurrencyRepositoryImpl`, `BackupRepositoryImpl`, and `CsvImportServiceImpl`, replacing
   every hardcoded `withContext(Dispatchers.IO)` with `withContext(dispatcherProvider.io)`.
   All three are `@Binds` constructor-injected, so Hilt supplies the provider automatically
   via `DispatchersModule` (verified: `hiltJavaCompileDebug` succeeded).
3. Updated the two manual constructor sites in instrumented tests
   (`BackupRepositoryPruneTest`, `CsvImportServiceInstrumentedTest`) to pass
   `RealDispatcherProvider()`, preserving their prior real-IO behavior.

### Completion Notes
- The "global Json with BigDecimal serializers" requirement is met by
  `RetrofitClient.networkJson`, a contextual `Json` registering `BigDecimalSerializer`
  (in `domain/model/util/Serializers.kt`) and used by both Retrofit converters.
- Added `TestDispatcherProvider` test util (routes all dispatchers to one `TestDispatcher`)
  for deterministic future repository tests, plus `DispatcherProviderTest` covering the
  `RealDispatcherProvider` contract, the test-double wiring, and the injected-dispatcher
  `withContext` pattern.
- **Out-of-scope fix (approved by user):** `TimelineServiceTest.kt` was already broken on
  the baseline commit (referenced the removed `TimelineService.formatDate`/`getHeaderData`,
  fallout from the parallel in-progress story 1-4 TimelineService refactor). It blocked the
  entire unit-test source set from compiling, so 1.1's tests could not run. Rewrote its
  second test to assert the same intent (empty-transaction group → "Today" header with zero
  income/expense) through the public `getGroupedData` API, using comparison-based BigDecimal
  assertions.
- Verification: `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL (all unit tests pass, no
  regressions); `./gradlew compileDebugAndroidTestKotlin` → BUILD SUCCESSFUL; main source
  set + Hilt codegen compiled cleanly.
- Note: new files are tracked by git under the case-insensitive macOS path
  `com/osscabinet/osswallet/...`; the Kotlin `package` declarations are correctly
  `com.osscabinet.ossWallet`. Worth normalizing the git path before pushing to a
  case-sensitive CI.

### File List
- `app/src/main/java/com/osscabinet/ossWallet/core/util/DispatcherProvider.kt` (new, pre-staged)
- `app/src/main/java/com/osscabinet/ossWallet/di/DispatchersModule.kt` (new, pre-staged)
- `app/src/main/java/com/osscabinet/ossWallet/data/remote/RetrofitClient.kt` (modified, pre-staged)
- `app/src/main/java/com/osscabinet/ossWallet/data/remote/ExchangeRateApi.kt` (modified, pre-staged)
- `app/src/main/java/com/osscabinet/ossWallet/data/remote/FrankfurterApi.kt` (modified — Moshi → kotlinx)
- `app/src/main/java/com/osscabinet/ossWallet/data/repository/CurrencyRepositoryImpl.kt` (modified — dispatcher injection)
- `app/src/main/java/com/osscabinet/ossWallet/data/repository/BackupRepositoryImpl.kt` (modified — dispatcher injection)
- `app/src/main/java/com/osscabinet/ossWallet/data/service/CsvImportServiceImpl.kt` (modified — dispatcher injection)
- `app/build.gradle.kts` (modified, pre-staged — Moshi removed, kotlinx converter added)
- `gradle/libs.versions.toml` (modified, pre-staged — Moshi removed, kotlinx converter added)
- `app/src/androidTest/java/com/osscabinet/osswallet/data/repository/BackupRepositoryPruneTest.kt` (modified — pass dispatcher)
- `app/src/androidTest/java/com/osscabinet/osswallet/data/service/CsvImportServiceInstrumentedTest.kt` (modified — pass dispatcher)
- `app/src/test/java/com/osscabinet/ossWallet/core/util/TestDispatcherProvider.kt` (new — test util)
- `app/src/test/java/com/osscabinet/ossWallet/core/util/DispatcherProviderTest.kt` (new — unit tests)
- `app/src/test/java/com/osscabinet/ossWallet/domain/service/timeline/TimelineServiceTest.kt` (modified — fix pre-existing breakage)

### Change Log
- 2026-06-26: Completed Core Infrastructure Baseline (Serialization & Concurrency). Migrated
  final Moshi DTO to kotlinx-serialization (Moshi fully removed); injected `DispatcherProvider`
  into `CurrencyRepositoryImpl`, `BackupRepositoryImpl`, `CsvImportServiceImpl` (AD-9); added
  `TestDispatcherProvider` + `DispatcherProviderTest`; fixed pre-existing `TimelineServiceTest`
  compile breakage. All unit tests pass; all source sets compile.
- 2026-06-26: Adversarial code review (Blind Hunter / Edge Case Hunter / Acceptance Auditor).
  Triage: 3 decision-needed, 1 patch, 4 deferred, 5 dismissed as noise.
- 2026-06-26: Applied 3 review patches. (1) Global `Json`: new `di/SerializationModule` provides a
  single app-wide `Json` (contextual `BigDecimalSerializer`, lenient decode, `explicitNulls` default
  preserved so backup encode output is unchanged); wired through the Retrofit path (`RetrofitClient`
  now builds APIs from an injected `Json` via `AppModule`) and `BackupRepositoryImpl` (18 call sites
  migrated off the bare default `Json`). (2) Added `CurrencyRepositoryImplTest` exercising the repo
  through an injected `TestDispatcherProvider` (+ local `FakeCurrencyDao`/`FakeSettingsRepository`/
  `FakeExchangeRateProvider`). (3) Removed the hang-trap default param from `TestDispatcherProvider`.
  Deferred: AD-9 non-repository dispatcher sites (own story). `./gradlew testDebugUnitTest` and
  `compileDebugAndroidTestKotlin` both BUILD SUCCESSFUL.

#### Files added/changed during review patches
- `app/src/main/java/com/osscabinet/ossWallet/di/SerializationModule.kt` (new — global `Json`)
- `app/src/main/java/com/osscabinet/ossWallet/data/remote/RetrofitClient.kt` (modified — APIs built from injected `Json`)
- `app/src/main/java/com/osscabinet/ossWallet/di/AppModule.kt` (modified — inject `Json` into Retrofit provider)
- `app/src/main/java/com/osscabinet/ossWallet/data/repository/BackupRepositoryImpl.kt` (modified — inject + use shared `Json`)
- `app/src/test/java/com/osscabinet/ossWallet/core/util/TestDispatcherProvider.kt` (modified — removed default param)
- `app/src/test/java/com/osscabinet/ossWallet/data/repository/CurrencyRepositoryImplTest.kt` (new — repo test via injected `TestDispatcherProvider`)
- `app/src/androidTest/java/com/osscabinet/osswallet/data/repository/BackupRepositoryPruneTest.kt` (modified — pass `json`)

### Review Findings

_Code review 2026-06-26 — Blind Hunter, Edge Case Hunter, Acceptance Auditor._

- [x] [Review][Patch] Provide a global/consistent kotlinx `Json` — the only contextual,
  `BigDecimalSerializer`-aware `Json` is `RetrofitClient.networkJson` (private, network scope).
  `BackupRepositoryImpl` (~20 `Json.encodeToString`/`decodeFromString` calls) and other consumers
  (`SettingsDataStore`, `CardRepositoryImpl`, `data/local/util/Converters`, `WebServerManager.serverJson`,
  widget pref stores) each use ad-hoc default `Json {}`. **Decision: fix now** — introduce a DI-provided
  global `Json` and migrate consumers. (auditor)
- [x] [Review][Defer] AD-9 dispatcher injection is partial — `data/web/WebServerManager.kt:167`
  (data layer) plus `service/WebServerService.kt:31`, `widget/WidgetRefreshWorker.kt`,
  `widget/NetWorthWidgetProvider.kt`, `widget/RecentTransactionsWidgetProvider.kt`,
  `ui/components/entry/AeonLocationSelection.kt` still hardcode `Dispatchers.IO`. The 3 spec-named
  repositories/services were converted (narrow "all Repositories" clause is met). — deferred:
  **needs broader refactor** — widget/server/UI dispatcher injection touches Hilt entry points and
  WorkManager wiring; warrants its own story. (edge+auditor)
- [x] [Review][Patch] Add repository-level dispatcher test — spec testing requirement names
  `CurrencyRepositoryImplTest.kt` (does not exist) and asks repo unit tests to inject a
  `TestDispatcherProvider`. Only a standalone `DispatcherProviderTest` + util exist so far.
  **Decision: add now** — write `CurrencyRepositoryImplTest` exercising the repo through an injected
  `TestDispatcherProvider` (+ fakes). (auditor)
- [x] [Review][Patch] `TestDispatcherProvider` no-arg default scheduler is a hang trap — default
  `StandardTestDispatcher()` builds its own scheduler; a future `TestDispatcherProvider()` injected
  into a repo inside `runTest {}` schedules work on a scheduler `advanceUntilIdle()` cannot drive →
  coroutines never run, tests hang or silently pass. Fix: drop the default param so callers must pass
  `StandardTestDispatcher(testScheduler)`. [app/src/test/java/com/osscabinet/ossWallet/core/util/TestDispatcherProvider.kt] (edge)
- [x] [Review][Defer] Network DTO exchange rates use `Double` (precision loss for financial values) [app/src/main/java/com/osscabinet/ossWallet/data/remote/ExchangeRateApi.kt] — deferred, pre-existing (types unchanged by this story; only annotations migrated).
- [x] [Review][Defer] Duplicate `Serializers.kt` (`data/local/util` vs `domain/model/util`) [app/src/main/java/com/osscabinet/ossWallet/data/local/util/Serializers.kt] — deferred, pre-existing.
- [x] [Review][Defer] Disk path `com/osscabinet/ossWallet` vs git-tracked lowercase `osswallet` — deferred, pre-existing repo-wide (612 files tracked lowercase; new files follow that convention; CI builds pass). 3 stray capital-tracked files worth a one-time repo normalization.
- [x] [Review][Defer] `TimelineServiceTest` asserts header title "Today" — day-boundary/timezone flake risk [app/src/test/java/com/osscabinet/ossWallet/domain/service/timeline/TimelineServiceTest.kt] — deferred, pre-existing pattern (test #1 asserts the same).

_Dismissed as noise (5): kotlin serialization plugin "not applied" (FALSE — `app/build.gradle.kts:8` applies it, build passes); "other Moshi DTOs will break" (FALSE — zero Moshi remains in source); "duplicate test files committed → CI duplicate-class" (FALSE — git tracks exactly one copy each under lowercase `osswallet`; the dual-casing in the diff was a case-insensitive-FS artifact); dead contextual `BigDecimalSerializer` in `networkJson` (harmless, intentional forward config); `coerceInputValues`/`explicitNulls` null-tolerance (parity with prior Moshi behavior; provider callers wrap in try/catch returning `Result`)._
