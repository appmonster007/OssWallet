---
baseline_commit: '059d934467650d693fba0d1b09b9219f5f6a9e08'
---

# Story 1.2: Establish Pure Domain Module (:core:domain)

## Status
**Status:** done
**Epic:** Epic 1: Architectural Foundation & Domain Purity
**ID:** 1.2

## Story

As a developer,
I want a pure Kotlin domain module,
so that my business logic is completely isolated from Android and Database frameworks.

## Acceptance Criteria

**Given** a monolithic `:app` module  
**When** I create a `:core:domain` module and move core models (`Transaction`, `Account`, `Category`) and repository interfaces into it  
**Then** these models are stripped of all Android framework annotations (`@Immutable` from `androidx.compose.runtime`)  
**And** `:core:domain` has zero dependencies on `androidx.room`, `android.*`, or `com.google.dagger:hilt-android`  
**And** the project compiles with `:app` depending on `:core:domain`  
**And** `WebServerRepository` (which references `android.graphics.Bitmap`) stays in `:app` with an inline TODO marking it for story 1.3 cleanup  

## Tasks / Subtasks

- [x] Task 1 — Fix Android framework contamination in domain/model/ (AC: Android-free)
  - [x] 1.1 Remove `import androidx.compose.runtime.Immutable` and `@Immutable` annotation from `domain/model/Transaction.kt`
  - [x] 1.2 Remove `import androidx.compose.runtime.Immutable` and `@Immutable` annotation from `domain/model/RecordUiModel.kt`
  - [x] 1.3 Verify `./gradlew compileDebugKotlin` still passes after removing `@Immutable` (Compose stability is inferred from `data class` + `val` + stable property types)

- [x] Task 2 — Create `:core:domain` Gradle module (AC: zero-android module exists)
  - [x] 2.1 Create directory `core/domain/src/main/java/` at the project root
  - [x] 2.2 Create `core/domain/build.gradle.kts` — `java-library` + `kotlin("jvm")` + serialization plugin; dependencies: `kotlinx-coroutines-core`, `kotlinx-serialization-json`. No android, no hilt.
  - [x] 2.3 Add `include(":core:domain")` to `settings.gradle.kts`
  - [x] 2.4 Verify the empty module compiles (`./gradlew :core:domain:compileKotlin`)

- [x] Task 3 — Move `domain/model/` package tree into `:core:domain` (AC: models in new module)
  - [x] 3.1 Move ALL files under `app/src/main/java/com/osscabinet/ossWallet/domain/model/` (and all sub-directories) to `core/domain/src/main/java/com/osscabinet/ossWallet/domain/model/`. Package declarations stay identical — no import updates needed in `:app`.
  - [x] 3.2 Verify `:core:domain` compiles standalone (`./gradlew :core:domain:compileKotlin`)

- [x] Task 4 — Move `domain/repository/` interfaces (excluding `WebServerRepository`) into `:core:domain` (AC: interfaces in new module)
  - [x] 4.1 Move all files from `app/src/main/java/com/osscabinet/ossWallet/domain/repository/` EXCEPT `WebServerRepository.kt` to `core/domain/src/main/java/com/osscabinet/ossWallet/domain/repository/`
  - [x] 4.2 Leave `WebServerRepository.kt` in `:app` at its current path; add a comment: `// TODO(story-1.3): Replace android.graphics.Bitmap return type with a platform-neutral type and move to :core:domain`
  - [x] 4.3 Verify `:core:domain` still compiles after adding repository interfaces

- [x] Task 5 — Wire `:app` to depend on `:core:domain` and verify full compile (AC: project compiles)
  - [x] 5.1 Add `implementation(project(":core:domain"))` to `app/build.gradle.kts` dependencies block
  - [x] 5.2 Run `./gradlew compileDebugKotlin` — fix any unresolved-symbol or duplicate-class errors (should be none if package names were preserved)
  - [x] 5.3 Run `./gradlew testDebugUnitTest` — all existing unit tests must pass, no regressions
  - [x] 5.4 Run `./gradlew compileDebugAndroidTestKotlin` — instrumented test source set must compile

- [x] Task 6 — Update sprint-status.yaml and story file (housekeeping)
  - [x] 6.1 Mark story 1.2 complete in sprint-status.yaml after all tasks pass

## Dev Notes

### Pre-Implementation Audit Results

An exhaustive audit of the `domain/` package (129 files) found it to be **96% clean** — no Room, no Hilt, no `Context` references. Only 7 files have Android framework contamination, scoped to two categories:

**Category A — In scope for story 1.2 (must fix before move):**
| File | Violation | Fix |
|---|---|---|
| `domain/model/Transaction.kt` | `import androidx.compose.runtime.Immutable` + `@Immutable` annotation | Remove both. Compose stability is inferred. |
| `domain/model/RecordUiModel.kt` | Same as above | Remove both. |

**Category B — Out of scope (stay in `:app` until story 1.3+):**
| File | Violation | Action |
|---|---|---|
| `domain/repository/WebServerRepository.kt` | `android.graphics.Bitmap` return type on `generateQrCode()` | Leave in `:app`; add TODO comment |
| `domain/service/currency/NumberFormatter.kt` | `android.icu.text.*`, `android.icu.util.*` | Left in `:app`'s service layer (not moved in this story) |
| `domain/service/currency/NumberShortener.kt` | Same | Same |
| `domain/service/currency/CurrencyService.kt` | `android.icu.util.ULocale` | Same |
| `domain/usecase/ComputeCardUseCase.kt` | `android.util.Log` | Same |

### Module Scope: What Moves vs What Stays

**MOVES to `:core:domain` (this story):**
- `domain/model/` — all 43+ files including all sub-packages (`settings/`, `timeline/`, `query/`, `web/`, etc.)
- `domain/repository/` — 16 of 17 interfaces (all except `WebServerRepository.kt`)

**STAYS in `:app` (story 1.3 or later):**
- `domain/service/` — 19 files; 3 have `android.icu.*` contamination → story 1.3 cleanup
- `domain/card/` — 26 files (computers, registry, etc.)
- `domain/usecase/` — 4 files; 1 has `android.util.Log` → needs logger interface
- `domain/util/` — 7 files
- `domain/cache/`, `domain/calculator/`, `domain/formatter/` — small utility classes
- `domain/repository/WebServerRepository.kt` — has `Bitmap` dependency

### Package Name Preservation (Critical)

**DO NOT rename packages.** The files in `:core:domain` must keep their original package declarations (`com.osscabinet.ossWallet.domain.model`, `com.osscabinet.ossWallet.domain.repository`). This means **zero import changes** are needed anywhere in `:app` — Gradle resolves the symbols from the dependency graph, not file paths.

### `:core:domain` build.gradle.kts — Exact Requirements

```kotlin
plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)           // from libs.versions.toml
    alias(libs.plugins.kotlin.serialization) // kotlinx.serialization compiler plugin
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)      // for Flow / suspend
    implementation(libs.kotlinx.serialization.json)   // for @Serializable models
}
```

**Hard rule:** No `com.google.dagger`, `hilt-android`, `androidx.*`, or `android.*` in this build file, ever.

### Verify libs.versions.toml Has Required Entries

Before creating the module, confirm these aliases exist in `gradle/libs.versions.toml`:
- `libs.plugins.kotlin.jvm` — for pure JVM Kotlin projects
- `libs.kotlinx.coroutines.core` — core coroutines (not android variant)
- `libs.kotlinx.serialization.json` — already added in story 1.1

If `kotlin.jvm` plugin alias is missing, use the literal: `kotlin("jvm") version "2.2.10"` matching the project's Kotlin version.

### @Immutable Removal — Compose Stability Impact

`@Immutable` (from `androidx.compose.runtime`) is an **optimization hint** to the Compose compiler, not a correctness requirement. Data classes where all properties are `val` and of stable types are already treated as stable by the compiler's stability inference (Kotlin 2.0+ with Compose 1.6+). Removing `@Immutable` from `Transaction` and `RecordUiModel` has zero functional impact — the Compose compiler will still correctly skip recomposition for these types.

If recomposition skipping degrades in the future, the correct fix is to apply `@Immutable` at the **ViewModel UiState level** (where it legitimately belongs), not on domain models.

### settings.gradle.kts Change

Add exactly one line at the end:
```kotlin
include(":core:domain")
```

The module name is `:core:domain` (matching the `core/domain/` directory structure and the Epic's target naming convention).

### DI Boundary — Hilt Stays in `:app`

The Hilt modules (`di/RepositoryModule.kt`, `di/UseCaseModule.kt`, etc.) bind `:app`-resident implementations to `:core:domain`-resident interfaces. They **stay in `:app`** — this is correct. Hilt entry points and bindings are `:app`-level concerns because they require `@HiltAndroidApp` and the Android runtime.

Do NOT add Hilt to `:core:domain`.

### File Layout After This Story

```
OssWallet/
├── app/
│   └── src/main/java/com/osscabinet/ossWallet/
│       ├── domain/
│       │   ├── service/        ← stays in :app
│       │   ├── card/           ← stays in :app  
│       │   ├── usecase/        ← stays in :app
│       │   ├── util/           ← stays in :app
│       │   ├── cache/          ← stays in :app
│       │   ├── calculator/     ← stays in :app
│       │   ├── formatter/      ← stays in :app
│       │   └── repository/
│       │       └── WebServerRepository.kt  ← stays (has Bitmap)
│       ├── data/               ← unchanged
│       ├── ui/                 ← unchanged
│       └── di/                 ← unchanged
└── core/
    └── domain/
        └── src/main/java/com/osscabinet/ossWallet/
            ├── domain/
            │   ├── model/      ← MOVED from :app
            │   └── repository/ ← MOVED from :app (minus WebServerRepository)
```

### Previous Story Learnings (1.1 + 1.4)

- **File path vs package name:** Git tracks files under `com/osscabinet/osswallet/` (lowercase) on this macOS repo. When creating new files under `core/domain/`, follow the same lowercase-directory convention for git tracking. Package declarations remain `com.osscabinet.ossWallet` (capital W).
- **Hilt codegen:** Any change to source file locations can trigger full Hilt recompilation — expect `./gradlew compileDebugKotlin` to take longer than usual on first run.
- **BigDecimal:** Already stored as TEXT via `BigDecimalConverter` in Room. The `BigDecimalSerializer` registered in `SerializationModule` (story 1.1) covers the serialization side. Domain models using `BigDecimal` fields need no changes.
- **Test pattern:** JVM unit tests in `src/test/` reference domain types directly. Since package names don't change, all existing tests will continue to resolve after the move.

### Testing Requirements

- No new test files required for this story — it is a structural refactor, not new business logic.
- Validation is compile-time: `./gradlew compileDebugKotlin`, `./gradlew testDebugUnitTest`, `./gradlew compileDebugAndroidTestKotlin` must all BUILD SUCCESSFUL.
- If any test fails post-move, it indicates a missing dependency in `core/domain/build.gradle.kts` — add the missing lib rather than reverting the move.

### Architecture Compliance

- **AD-1 (Domain Pureness):** This story directly enforces it at the Gradle level — `:core:domain` cannot import Android or data-layer types by construction.
- **AD-2 (Feature-First Modularization):** `:core:domain` is the foundation layer that future `:feature:*` modules will depend on. Keep the module name `:core:domain` as specified.
- **AD-9 (Dispatcher Injection):** Repository interfaces use `suspend fun` and `Flow<T>` — both are Kotlin Coroutines primitives available in `kotlinx-coroutines-core` (no android dependency).

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- Serialization plugin fix: `kotlin("plugin.serialization")` fails without a version in submodule; added `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }` to `libs.versions.toml` and used `alias(libs.plugins.kotlin.serialization)` in build file. `kotlin("jvm")` works without version since it's already on the classpath via AGP.
- `GroupHeaderData` was a nested class inside `TimelineService` with `@Immutable` annotation; extracted to standalone `domain/model/timeline/GroupHeaderData.kt` in `:core:domain`. Updated `Transaction.kt`, `TimelineService.kt`, `TransactionListItem.kt`, and `TransactionListContent.kt` accordingly.
- Cross-module smart cast errors: When domain types moved to `:core:domain`, Kotlin can no longer smart cast `val` properties (cross-module API). Fixed ~15 instances across `TransactionQueryBuilder`, `FilterMapper`, `TransactionRepositoryImpl`, `CsvImportServiceImpl`, `TransactionFilterService`, `PlannedPaymentService`, `ManagementScopeCard`, `CsvImportSourceSelection`, `TransactionEntryState` by capturing nullable properties in local `val`s before null checks.
- `CardType.kt`, `CardFilterRules.kt`, and `ExchangeRateSnapshot.kt` were also moved to `:core:domain` to satisfy dependencies from `CardConfig.kt` and repository interfaces. These are pure JVM data types — the move is architecturally correct even though the story only explicitly called out `domain/model/` and `domain/repository/`.

### Review Findings (2026-06-27)

**Layers run:** Blind Hunter · Edge Case Hunter · Acceptance Auditor  
**Target:** Group A (build wiring + 12 modified source files); uncommitted changes  
**Outcome:** 0 decision-needed · 1 patch applied · 12 deferred · 8 dismissed

**Patch applied:**
- [x] `PlannedPaymentService.kt` — `scheduleEndDate` was captured inside the `while` loop on every iteration; hoisted to before the loop (introduced by smart-cast fix in this story)

**Deferred (pre-existing or out-of-scope):**
- FilterMapper.kt `filter.` usage on extension receiver (pre-existing)
- ExchangeRateApi.kt / RetrofitClient.kt unstaged modifications (different story scope)
- DispatcherProvider.kt / DispatchersModule.kt staged additions (Story 1.1 infrastructure, different scope)
- Magic `1000L` fallback in CsvImportServiceImpl (pre-existing)
- GroupHeaderData title/subtitle as display strings in JVM module (pre-existing design)
- Duplicate filter-building logic in TransactionQueryBuilder + TransactionRepositoryImpl (pre-existing)
- AccountingPerspective.labelsFor() `else` branch defeats exhaustiveness (pre-existing in CardConfig.kt)
- PlannedPaymentsCardConfig.daysAhead @Transient dead field (pre-existing)
- SimpleDateFormat in TimelineRange.toDisplayString() (pre-existing)
- Orphaned ScreenContext KDoc in CardConfig.kt:163 (pre-existing in source file)
- :core:domain has no src/test/ (waived by story spec; story 1.x concern)
- Spec deviations (CardType, CardFilterRules, ExchangeRateSnapshot, GroupHeaderData moved to :core:domain) — necessary for compilation; spec gap not an implementation error

**Dismissed (false positives / confirmed done):**
- Duplicate domain model classes — git shows :app copies as `D` (deleted); compile confirmed
- @Immutable / Compose stability regression — Kotlin 2.2.10 + Compose BOM 2026 stability inference covers data class; @Immutable on TransactionListItem wrapper maintains skip guarantee
- WebServerRepository Bitmap violation — intentional per spec; TODO(story-1.3) added as required
- TimelineService callsite audit — compile confirmed all 4 callers updated
- kotlinx-serialization dep "unused" — domain models (CardConfig, etc.) use @Serializable
- settings.gradle.kts trailing newline — trivial
- Task 6 sprint-status not in diff — confirmed done in session
- TODO comment before package declaration — syntactically valid Kotlin

### File List

**Created:**
- `core/domain/build.gradle.kts`
- `core/domain/src/main/java/com/osscabinet/ossWallet/domain/model/**` (45 files moved)
- `core/domain/src/main/java/com/osscabinet/ossWallet/domain/model/timeline/GroupHeaderData.kt` (extracted from TimelineService)
- `core/domain/src/main/java/com/osscabinet/ossWallet/domain/repository/**` (16 files moved)
- `core/domain/src/main/java/com/osscabinet/ossWallet/domain/card/CardType.kt` (moved)
- `core/domain/src/main/java/com/osscabinet/ossWallet/domain/card/CardFilterRules.kt` (moved)
- `core/domain/src/main/java/com/osscabinet/ossWallet/domain/service/currency/ExchangeRateSnapshot.kt` (moved)

**Modified:**
- `settings.gradle.kts` — added `include(":core:domain")`
- `gradle/libs.versions.toml` — added `kotlin-serialization` plugin alias
- `app/build.gradle.kts` — added `implementation(project(":core:domain"))`
- `app/src/main/java/com/osscabinet/ossWallet/domain/model/Transaction.kt` — removed @Immutable, updated GroupHeaderData import
- `app/src/main/java/com/osscabinet/ossWallet/domain/model/RecordUiModel.kt` — removed @Immutable
- `app/src/main/java/com/osscabinet/ossWallet/domain/repository/WebServerRepository.kt` — added TODO comment
- `app/src/main/java/com/osscabinet/ossWallet/domain/service/timeline/TimelineService.kt` — removed nested GroupHeaderData, import from model
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/transactions/model/TransactionListItem.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/transactions/list/TransactionListContent.kt`
- `app/src/main/java/com/osscabinet/ossWallet/data/local/query/TransactionQueryBuilder.kt`
- `app/src/main/java/com/osscabinet/ossWallet/data/mapper/FilterMapper.kt`
- `app/src/main/java/com/osscabinet/ossWallet/data/repository/TransactionRepositoryImpl.kt`
- `app/src/main/java/com/osscabinet/ossWallet/data/service/CsvImportServiceImpl.kt`
- `app/src/main/java/com/osscabinet/ossWallet/domain/service/filter/TransactionFilterService.kt`
- `app/src/main/java/com/osscabinet/ossWallet/domain/service/management/PlannedPaymentService.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/components/management/ManagementScopeCard.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/import_csv/steps/CsvImportSourceSelection.kt`
- `app/src/main/java/com/osscabinet/ossWallet/ui/screens/transactions/entry/TransactionEntryState.kt`

**Deleted from :app:**
- `app/src/main/java/com/osscabinet/ossWallet/domain/model/**` (all files — moved to :core:domain)
- `app/src/main/java/com/osscabinet/ossWallet/domain/repository/**` (except WebServerRepository.kt — moved to :core:domain)
- `app/src/main/java/com/osscabinet/ossWallet/domain/card/CardType.kt`
- `app/src/main/java/com/osscabinet/ossWallet/domain/card/CardFilterRules.kt`
- `app/src/main/java/com/osscabinet/ossWallet/domain/service/currency/ExchangeRateSnapshot.kt`
