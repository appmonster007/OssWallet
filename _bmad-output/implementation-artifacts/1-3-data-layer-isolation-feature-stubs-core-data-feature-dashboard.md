---
baseline_commit: '81c0b0ff8a81811012d1a8ddad35fc3209a99d38'
---

# Story 1.3: Data Layer Isolation & Feature Stubs (:core:data & :feature:dashboard)

## Status
**Status:** done
**Epic:** Epic 1: Architectural Foundation & Domain Purity
**ID:** 1.3

## Story

As a developer,
I want isolated Data and Feature modules,
so that I can enforce strict boundary rules and prevent "spaghetti" dependencies.

## Acceptance Criteria

**Given** the `:core:domain` module exists (completed in Story 1.2)
**When** I create `:core:data` for Room/Network implementations and `:feature:dashboard` as a UI stub
**Then** `:core:data` implements the Repository interfaces defined in `:core:domain`
**And** `:core:data` has all Room entities, DAOs, mappers, remote API clients, and DI modules
**And** `:feature:dashboard` depends ONLY on `:core:domain` (and `:core:ui` once that module is created)
**And** `Hilt` modules in `:core:data` are automatically aggregated by the `:app` component
**And** the full project compiles: `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL

## Tasks / Subtasks

- [x] Task 1 — Prerequisites: Add missing plugin aliases to `gradle/libs.versions.toml`
  - [x] 1.1 Verify Story 1.2 is `done` (`:core:domain` module must exist at `core/domain/`) before beginning
  - [x] 1.2 Add `android-library` plugin alias: `android-library = { id = "com.android.library", version.ref = "agp" }`
  - [x] 1.3 Add `kotlin-android` plugin alias: `kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }`
  - [x] 1.4 Add `kotlin-serialization` plugin alias: `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }` — consolidates the inline `kotlin("plugin.serialization") version "2.2.10"` in `:app`

- [x] Task 2 — Create `:core:data` Gradle module (Android library, Hilt-enabled)
  - [x] 2.1 Create directory `core/data/src/main/java/` at the project root
  - [x] 2.2 Create `core/data/build.gradle.kts` using the exact template in Dev Notes
  - [x] 2.3 Add `include(":core:data")` to `settings.gradle.kts`
  - [x] 2.4 Verify the empty module compiles (`./gradlew :core:data:compileDebugKotlin`)

- [x] Task 3 — Relocate `@ApplicationScope` qualifier to `:core:data`
  - [x] 3.1 Create `core/data/src/main/java/com/osscabinet/ossWallet/data/di/qualifiers/ApplicationScope.kt` — copy the `@ApplicationScope` qualifier annotation class from `:app`'s `di/` directory
  - [x] 3.2 Update the `@Provides CoroutineScope` function in `:app/di/AppModule.kt` to import `@ApplicationScope` from the new `:core:data` location (package: `com.osscabinet.ossWallet.data.di.qualifiers`)
  - [x] 3.3 Confirm `ComputationRepositoryImpl` (which injects `@ApplicationScope CoroutineScope`) also references the qualifier from `:core:data` location after the move in Task 6

- [x] Task 4 — Move `data/local/` package tree to `:core:data`
  - [x] 4.1 Move ALL 5 root files: `AppDatabase.kt`, `Converters.kt`, `SecurePassphraseManager.kt`, `SettingsDataStore.kt`, `WidgetInvalidationObserver.kt` to `core/data/src/main/java/com/osscabinet/ossWallet/data/local/`
  - [x] 4.2 Move all 14 DAO files from `data/local/dao/` to `core/data/src/main/java/com/osscabinet/ossWallet/data/local/dao/`
  - [x] 4.3 Move all 17 entity files from `data/local/entity/` to `core/data/src/main/java/com/osscabinet/ossWallet/data/local/entity/`
  - [x] 4.4 Move `data/local/query/` (3 files: `DatabaseQueryServiceImpl.kt`, `QueryCache.kt`, `TransactionQueryBuilder.kt`) to `core/data/src/main/java/com/osscabinet/ossWallet/data/local/query/`
  - [x] 4.5 Move `data/local/util/Serializers.kt` to `core/data/src/main/java/com/osscabinet/ossWallet/data/local/util/`
  - [x] 4.6 Verify `:core:data` compiles (`./gradlew :core:data:compileDebugKotlin`)

- [x] Task 5 — Move `data/mapper/` and `data/remote/` to `:core:data`
  - [x] 5.1 Move all 9 mapper files from `data/mapper/` to `core/data/src/main/java/com/osscabinet/ossWallet/data/mapper/`
  - [x] 5.2 Move all 5 remote files from `data/remote/` (including `provider/` subdirectory) to `core/data/src/main/java/com/osscabinet/ossWallet/data/remote/`
  - [x] 5.3 Verify `:core:data` compiles

- [x] Task 6 — Move `data/repository/`, `data/service/` to `:core:data`
  - [x] 6.1 Move all 17 repository impls + `RepositoryExtensions.kt` + `util/CsvProcessor.kt` from `data/repository/` to `core/data/src/main/java/com/osscabinet/ossWallet/data/repository/`
  - [x] 6.2 Move 3 service impls (`BackupSchedulerImpl.kt`, `BudgetBakersAssetLoader.kt`, `CsvImportServiceImpl.kt`) from `data/service/` to `core/data/src/main/java/com/osscabinet/ossWallet/data/service/`
  - [x] 6.3 Verify `:core:data` compiles

- [x] Task 7 — Move and split DI modules
  - [x] 7.1 Move `di/DatabaseModule.kt` to `core/data/src/main/java/com/osscabinet/ossWallet/data/di/DatabaseModule.kt`
  - [x] 7.2 Move `di/RepositoryModule.kt` to `core/data/src/main/java/com/osscabinet/ossWallet/data/di/RepositoryModule.kt`
  - [x] 7.3 Move `di/DispatchersModule.kt` to `core/data/src/main/java/com/osscabinet/ossWallet/data/di/DispatchersModule.kt`
  - [x] 7.4 Move `di/SerializationModule.kt` to `core/data/src/main/java/com/osscabinet/ossWallet/data/di/SerializationModule.kt`
  - [x] 7.5 **Split `di/AppModule.kt`**: Extract data-layer provisions (`SettingsDataStore`, `QueryCache`, `DatabaseQueryService`, `ExchangeRateProviderRegistry`) into a new `core/data/src/main/java/com/osscabinet/ossWallet/data/di/DataModule.kt`. The remaining provisions (`CoroutineScope` @ApplicationScope, `WidgetRefreshTrigger`) stay in `:app/di/AppModule.kt`.
  - [x] 7.6 Verify `:core:data` compiles standalone

- [x] Task 8 — Wire `:app` to depend on `:core:data` and verify full compile
  - [x] 8.1 Add `implementation(project(":core:data"))` to `app/build.gradle.kts` dependencies block
  - [x] 8.2 Remove from `:app/build.gradle.kts` the duplicate deps that are now in `:core:data` (Room, SQLCipher, DataStore, Retrofit, Serialization converter) — leave only what `:app`-specific code still needs directly. See Dev Notes for exact list.
  - [x] 8.3 Run `./gradlew compileDebugKotlin` — fix any unresolved-symbol errors
  - [x] 8.4 Run `./gradlew testDebugUnitTest` — all existing unit tests must pass
  - [x] 8.5 Run `./gradlew compileDebugAndroidTestKotlin` — instrumented test source set must compile

- [x] Task 9 — Create `:feature:dashboard` stub module
  - [x] 9.1 Create directory `feature/dashboard/src/main/java/com/osscabinet/ossWallet/feature/dashboard/`
  - [x] 9.2 Create `feature/dashboard/build.gradle.kts` using the exact template in Dev Notes
  - [x] 9.3 Add `include(":feature:dashboard")` to `settings.gradle.kts`
  - [x] 9.4 Create a placeholder Kotlin file `feature/dashboard/src/main/java/com/osscabinet/ossWallet/feature/dashboard/DashboardPlaceholder.kt` with an `internal object DashboardPlaceholder` — keeps the module non-empty so Gradle doesn't warn
  - [x] 9.5 Add `implementation(project(":feature:dashboard"))` to `app/build.gradle.kts` (so the module is included in the build graph)
  - [x] 9.6 Verify `./gradlew :feature:dashboard:compileDebugKotlin` → BUILD SUCCESSFUL

- [x] Task 10 — Update sprint-status.yaml and story file (housekeeping)
  - [x] 10.1 Mark story 1.3 `done` in sprint-status.yaml

## Dev Notes

### Hard Prerequisite: Story 1.2 Must Be Complete

Story 1.3 depends on `:core:domain` existing. All repository implementations import domain model types (e.g., `com.osscabinet.ossWallet.domain.model.Transaction`) that now live in `:core:domain`. If `:core:domain` does not exist, `:core:data` will fail to compile on the very first repository file.

**Check before starting:** `ls core/domain/src/main/java/com/osscabinet/ossWallet/domain/model/` should list all domain model files. If the directory doesn't exist, do not proceed — complete Story 1.2 first.

---

### Files That Move vs. Stay — Complete Inventory

**MOVES to `:core:data` (89 files across 8 source packages):**

| Source path (in `:app`) | Count | Notes |
|---|---|---|
| `data/local/` (root) | 5 | AppDatabase, Converters, SecurePassphraseManager, SettingsDataStore, WidgetInvalidationObserver |
| `data/local/dao/` | 14 | All 14 DAO interfaces |
| `data/local/entity/` | 17 | All 17 entity classes (including FTS, CrossRef, WithTags) |
| `data/local/query/` | 3 | DatabaseQueryServiceImpl, QueryCache, TransactionQueryBuilder |
| `data/local/util/` | 1 | Serializers.kt |
| `data/mapper/` | 9 | AccountMapper, BudgetMapper, CategoryMapper, FilterMapper, LoanMapper, PlannedPaymentMapper, ProfileMapper, TemplateMapper, TransactionMapper |
| `data/remote/` | 3 | ExchangeRateApi, FrankfurterApi, RetrofitClient |
| `data/remote/provider/` | 2 | ExchangeRateApiProvider, FrankfurterProvider |
| `data/repository/` | 17 | All 17 `*RepositoryImpl.kt` files + RepositoryExtensions.kt |
| `data/repository/util/` | 1 | CsvProcessor.kt |
| `data/service/` | 3 | BackupSchedulerImpl, BudgetBakersAssetLoader, CsvImportServiceImpl |
| `di/DatabaseModule.kt` | 1 | Provides all DAOs + AppDatabase |
| `di/RepositoryModule.kt` | 1 | @Binds all 18 interface→impl pairs |
| `di/DispatchersModule.kt` | 1 | Provides RealDispatcherProvider |
| `di/SerializationModule.kt` | 1 | Provides Json with BigDecimalSerializer |

**NEW FILE in `:core:data`:**
- `data/di/qualifiers/ApplicationScope.kt` — qualifier annotation relocated from `:app`
- `data/di/DataModule.kt` — data-layer @Provides extracted from `:app/di/AppModule.kt`

**STAYS in `:app`:**
| File | Reason |
|---|---|
| `data/web/` (5 files) | Ktor web server — web module scope (story 4.x) |
| `data/worker/AutoBackupWorker.kt` | WorkManager Worker, Android-platform, app-level concern |
| `di/AppModule.kt` (trimmed) | `@ApplicationScope CoroutineScope`, `WidgetRefreshTrigger` remain |
| `di/ManagerModule.kt` | TransactionManager is `:app`-resident (domain service) |
| `di/UseCaseModule.kt` | Use cases stay in `:app` until story 1.x covers domain module |
| `di/WebServerModule.kt` | Web server binding |
| `di/HiltEntryPoints.kt` | Widget/worker entry points are app-level |

---

### `:core:data/build.gradle.kts` — Exact Template

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.osscabinet.ossWallet.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    // Domain module — 'api' so :app gets domain types transitively via :core:data
    api(project(":core:domain"))

    // Hilt (required so @Module / @Inject are processed in this module)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room + SQLCipher
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher)
    implementation(libs.sqlite)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.okhttp)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Security (for SecurePassphraseManager Android Keystore)
    implementation(libs.androidx.security.crypto)

    // CSV (for CsvProcessor / CsvImportServiceImpl)
    implementation(libs.commons.csv)

    // AndroidX Core (for BackupRepositoryImpl)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
}
```

**Hard rule:** Do NOT add `com.google.dagger:hilt-android-gradle-plugin`, `com.android.application`, or any Compose/Glance dep here. `:core:data` has no UI code.

**IMPORTANT — `api` vs `implementation` for `:core:domain`:**
Use `api(project(":core:domain"))` so that `:app` (which `implementation`s `:core:data`) automatically gets access to all domain types. This prevents `:app` from needing to re-declare `implementation(project(":core:domain"))` alongside `implementation(project(":core:data"))`. If you use `implementation` instead, you will get "unresolved reference" errors for domain types in `:app` unless `:app` also adds the explicit `:core:domain` dependency.

---

### `:feature:dashboard/build.gradle.kts` — Exact Template

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.osscabinet.ossWallet.feature.dashboard"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    // Domain — ONLY :core:domain (no :core:data, no direct Room)
    implementation(project(":core:domain"))
    // TODO(story-1.x): Add implementation(project(":core:ui")) when :core:ui is created

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Compose (BOM-managed)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
}
```

---

### `settings.gradle.kts` — Final State After This Story

```kotlin
// ... (pluginManagement, plugins, dependencyResolutionManagement blocks unchanged) ...

rootProject.name = "OssWallet"
include(":app")
include(":core:domain")   // added in Story 1.2
include(":core:data")     // added in this story
include(":feature:dashboard") // added in this story
```

---

### `gradle/libs.versions.toml` — New Plugin Aliases to Add

In the `[plugins]` block (currently has 4 entries), add these 3 entries:

```toml
android-library       = { id = "com.android.library",                          version.ref = "agp"    }
kotlin-android        = { id = "org.jetbrains.kotlin.android",                 version.ref = "kotlin" }
kotlin-serialization  = { id = "org.jetbrains.kotlin.plugin.serialization",    version.ref = "kotlin" }
```

After adding these, update `app/build.gradle.kts` to replace the inline declaration:
```kotlin
// BEFORE (in app/build.gradle.kts):
kotlin("plugin.serialization") version "2.2.10"

// AFTER:
alias(libs.plugins.kotlin.serialization)
```

---

### Hilt Multi-Module: How It Works Here

Hilt discovers `@Module` classes across all Gradle modules that are in the classpath at compile time of `:app`. No special `@Component` or additional entry point declarations are needed for `:core:data` modules to be included in the `SingletonComponent`.

**The rules:**
1. Every `@Module` in `:core:data` must still be annotated `@InstallIn(SingletonComponent::class)` — no change.
2. `:core:data` must declare `implementation(libs.hilt.android)` + `ksp(libs.hilt.compiler)` in its `build.gradle.kts` — this allows Hilt's KSP processor to generate module metadata for `:core:data`.
3. `:app` must keep `@HiltAndroidApp` on `OssWalletApp` — this is the component aggregation root.
4. `:feature:dashboard` modules also need Hilt if they declare `@HiltViewModel` — add to its build.gradle.kts.

**`@ApplicationScope` Qualifier — Critical Relocation:**

`ComputationRepositoryImpl` currently injects `@ApplicationScope CoroutineScope`. After it moves to `:core:data`, it cannot reference a qualifier defined in `:app` (that would create a circular dependency). Solution:

1. Create `core/data/src/main/java/com/osscabinet/ossWallet/data/di/qualifiers/ApplicationScope.kt`:
```kotlin
package com.osscabinet.ossWallet.data.di.qualifiers

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
```

2. Update `ComputationRepositoryImpl` (in `:core:data`) to import from this new package.
3. Update `:app/di/AppModule.kt` `@Provides CoroutineScope` to import `@ApplicationScope` from `com.osscabinet.ossWallet.data.di.qualifiers`.
4. If `:app` code references `@ApplicationScope` anywhere else (e.g., in `OssWalletApp.kt`), update those imports too.

---

### `AppModule.kt` Split: What Moves to `DataModule.kt`

The following `@Provides` functions in `di/AppModule.kt` must be extracted to a new `core/data/src/main/java/com/osscabinet/ossWallet/data/di/DataModule.kt`:

| Provision | Moves? | Reason |
|---|---|---|
| `CoroutineScope` (@ApplicationScope) | STAYS in `:app/AppModule.kt` | Provided by `:app`, used by `:core:data` via Hilt graph — no circular dep |
| `SettingsDataStore` | MOVES to `DataModule.kt` | SettingsDataStore is in `:core:data` |
| `QueryCache` | MOVES to `DataModule.kt` | QueryCache is in `:core:data` |
| `WidgetRefreshTrigger` → `WorkManagerWidgetRefreshTrigger` | STAYS in `:app/AppModule.kt` | WorkManager binding is app-level |
| `DatabaseQueryService` → `DatabaseQueryServiceImpl` | MOVES to `DataModule.kt` | Both are in `:core:data` |
| `ExchangeRateProviderRegistry` | MOVES to `DataModule.kt` | Providers are in `:core:data/remote/` |

After the split, `AppModule.kt` in `:app` should only contain provisions for app-level singletons. Move the `CardRenderCache` provision if it is currently in `AppModule.kt` too — it belongs in `DataModule.kt` since `QueryCache` goes there.

---

### `app/build.gradle.kts` — Dependency Cleanup After Story

After `:core:data` is wired via `api(project(":core:domain"))`, `:app` gets domain types transitively. Remove the explicit `implementation(project(":core:domain"))` from `:app` if it was added in Story 1.2 — it is now redundant (`:core:data` transitively exposes it via `api`).

Dependencies to REMOVE from `:app/build.gradle.kts` (they move to `:core:data`):
- `implementation(libs.androidx.room.runtime)` ✗ — now in `:core:data`
- `implementation(libs.androidx.room.ktx)` ✗
- `ksp(libs.androidx.room.compiler)` ✗ (unless `:app` still has Room annotations — check first)
- `implementation(libs.sqlcipher)` ✗
- `implementation(libs.sqlite)` ✗
- `implementation(libs.androidx.datastore.preferences)` ✗
- `implementation(libs.retrofit)` ✗
- `implementation(libs.retrofit2.kotlinx.serialization.converter)` ✗

Dependencies to KEEP in `:app/build.gradle.kts` (still used directly by `:app`):
- `implementation(libs.hilt.android)` ✓ (for `@HiltAndroidApp`)
- `ksp(libs.hilt.compiler)` ✓ (for component generation)
- `implementation(libs.hilt.navigation.compose)` ✓ (for `@HiltViewModel` in UI)
- All Compose / Glance / Ktor deps ✓ (still `:app`-resident UI/web code)
- `implementation(libs.commons.csv)` — only if still referenced in `:app`; otherwise remove

---

### Package Name Preservation — Zero Import Changes

All files moved to `:core:data` keep their **exact original package declarations**. For example:
- `data/repository/AccountRepositoryImpl.kt` has `package com.osscabinet.ossWallet.data.repository`
- After moving to `core/data/src/main/java/com/osscabinet/ossWallet/data/repository/AccountRepositoryImpl.kt`, the package declaration stays `com.osscabinet.ossWallet.data.repository`

This means **zero import changes** in any `:app` Kotlin file that references these types. Gradle resolves symbols from the dependency graph, not file paths.

---

### Hilt Entry Points — HiltEntryPoints.kt Stays in `:app`

`di/HiltEntryPoints.kt` in `:app` declares entry points for Widgets and Workers:
- `WidgetDataLoaderEntryPoint` — references `SettingsDataStore`, `AppDatabase`, `DatabaseQueryService`, `CategoryRepository`, `AccountRepository`
- `AutoBackupWorkerEntryPoint` — references `BackupRepository`, `SettingsDataStore`
- `PlannedPaymentWorkerEntryPoint` — references `PlannedPaymentRepository`, `SettingsRepository`, `CategoryRepository`, `TransactionManager`

These references all point to types that will be in `:core:data` after this story. The entry points themselves stay in `:app` because:
1. They reference `@HiltAndroidApp` components (only resolvable in `:app`)
2. Widget Providers and Workers are `:app`-level Android components

After the move, `HiltEntryPoints.kt` will need its imports updated to the new package paths — but since packages are preserved, no changes should be needed. Verify during compile.

---

### `BackupRepositoryImpl.kt` — Android Imports Are Fine in `:core:data`

`:core:data` is an Android library module (`com.android.library`), not a pure JVM module. So `android.content.Context`, `android.util.Log`, `androidx.core.net.toUri`, and `androidx.room.withTransaction` are all valid imports. `BackupRepositoryImpl` can move to `:core:data` without any code changes.

---

### Room `exportSchema = false` — No Schema File Migration Needed

`AppDatabase.kt` has `exportSchema = false`, which means no JSON schema files are generated or tracked. There is no `schemas/` directory to move. This is a non-issue for the module migration.

---

### `data/web/` — Intentionally Left in `:app`

The 5 files in `data/web/` (`AuthSessionManager`, `NetworkUtils`, `QRCodeGenerator`, `WebApiRoutes`, `WebMappingUtils`) are Ktor web server implementation files. They are NOT moved in this story. Their scope belongs to Epic 4 (Web UI - Operational Parity). Leave them in place.

---

### File Layout After This Story

```
OssWallet/
├── app/
│   └── src/main/java/com/osscabinet/ossWallet/
│       ├── data/
│       │   └── web/            ← stays (Ktor web server — Epic 4)
│       │   └── worker/         ← stays (WorkManager workers — app-level)
│       ├── di/
│       │   ├── AppModule.kt    ← trimmed (CoroutineScope + WidgetRefreshTrigger only)
│       │   ├── HiltEntryPoints.kt ← stays
│       │   ├── ManagerModule.kt   ← stays
│       │   ├── UseCaseModule.kt   ← stays
│       │   └── WebServerModule.kt ← stays
│       ├── domain/             ← stays (services, usecases, card — story 1.x)
│       ├── ui/                 ← unchanged
│       └── OssWalletApp.kt     ← unchanged (@HiltAndroidApp root)
├── core/
│   ├── domain/                 ← created in Story 1.2
│   └── data/
│       └── src/main/java/com/osscabinet/ossWallet/
│           └── data/
│               ├── di/
│               │   ├── qualifiers/ApplicationScope.kt  ← NEW
│               │   ├── DataModule.kt                   ← NEW (extracted from AppModule)
│               │   ├── DatabaseModule.kt               ← MOVED
│               │   ├── DispatchersModule.kt            ← MOVED
│               │   ├── RepositoryModule.kt             ← MOVED
│               │   └── SerializationModule.kt          ← MOVED
│               ├── local/      ← MOVED (AppDatabase, DAOs, entities, query, util)
│               ├── mapper/     ← MOVED (9 mappers)
│               ├── remote/     ← MOVED (5 files)
│               ├── repository/ ← MOVED (17 impls + RepositoryExtensions + CsvProcessor)
│               └── service/    ← MOVED (BackupSchedulerImpl, BudgetBakersAssetLoader, CsvImportServiceImpl)
└── feature/
    └── dashboard/              ← NEW STUB (just build.gradle.kts + placeholder)
        └── src/main/java/com/osscabinet/ossWallet/feature/dashboard/
            └── DashboardPlaceholder.kt  ← NEW
```

---

### Previous Story Learnings (1.1, 1.2, 1.4)

- **File path casing:** Git tracks files under `com/osscabinet/osswallet/` (lowercase W). Create all new directories and files under the lowercase path `com/osscabinet/osswallet/...` for consistent git tracking. Package declarations still use capital W (`com.osscabinet.ossWallet`).
- **Hilt KSP processing time:** Moving 60+ files and adding Hilt to a new module triggers full KSP recompilation. First `./gradlew compileDebugKotlin` after Task 8 will be slow — normal.
- **Compile-first strategy:** After each task, run `./gradlew :core:data:compileDebugKotlin` before moving to the next task. This catches missing dependencies or import errors immediately rather than accumulating them.
- **`api` vs `implementation` in library modules**: `:core:data` using `api(project(":core:domain"))` means `:app` doesn't need to re-declare `:core:domain`. This is the standard multi-module pattern for "pass-through" dependencies (see AD-2).
- **Serialization plugin inline vs alias**: The current `app/build.gradle.kts` uses `kotlin("plugin.serialization") version "2.2.10"` inline. After adding the `kotlin-serialization` alias to TOML (Task 1.4), update `:app` to use `alias(libs.plugins.kotlin.serialization)` too — avoids version drift.

---

### Architecture Compliance

- **AD-1 (Domain Pureness):** `:core:data` is explicitly the "framework-aware" layer. Having `android.*`, `androidx.room`, and SQLCipher imports here is correct and expected. The domain layer (`:core:domain`) remains clean.
- **AD-2 (Feature-First Modularization):** Creating `:core:data` and `:feature:dashboard` as separate Gradle modules is the direct implementation of this decision. `:feature:dashboard` has no dependencies on `:core:data` — it talks to `:core:domain` interfaces only.
- **AD-9 (Dispatcher Injection):** `DispatchersModule.kt` moves to `:core:data`, ensuring all repository impls that inject `DispatcherProvider` continue to work. Note: `BackupRepositoryImpl` and `CurrencyRepositoryImpl` already inject `DispatcherProvider`. The remaining 15 impls that don't use dispatchers (no async work) remain unchanged.

---

### Testing Requirements

No new test files required for this story — it is a structural Gradle-module refactor, not new business logic.

Validation is compile-time:
1. `./gradlew :core:data:compileDebugKotlin` — `:core:data` compiles standalone
2. `./gradlew :feature:dashboard:compileDebugKotlin` — stub compiles
3. `./gradlew compileDebugKotlin` — full project compiles (`:app` + `:core:data` + `:feature:dashboard`)
4. `./gradlew testDebugUnitTest` — all JVM unit tests pass (no regressions)
5. `./gradlew compileDebugAndroidTestKotlin` — instrumented tests compile

If any unit test fails after the move, check whether the test imports a type that is now in `:core:data`. The test source set in `:app` has `implementation(project(":core:data"))` transitively through the production source set, so this should be automatic. If a test specifically instantiates a moved class (e.g., `DatabaseQueryServiceImpl`), verify the test classpath includes `:core:data`.

---

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
