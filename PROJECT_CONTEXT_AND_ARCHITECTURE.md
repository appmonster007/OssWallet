# OssWallet — Project Context & Architecture (Consolidated)
**Date:** June 2026 · **Status:** Active Development · **Build**: SDK 36 (target), 26 (min)

This document is the rigorous single source of truth for OssWallet's implementation and architecture.

---

## 1. Core Foundation
- **Package**: `com.osscabinet.ossWallet`
- **DI**: Hilt (`@HiltAndroidApp`, `@Singleton`). Modules: `AppModule`, `DatabaseModule`, `RepositoryModule`, `UseCaseModule`, `ManagerModule`, `WebServerModule`.
- **Networking**: 
    - **APIs**: Retrofit + Moshi. Supports dynamic base-currency fetch.
    - **Embedded Server**: Ktor (CIO engine) on Port 8192 for the "Open in Web" companion. Supports compression (Gzip), security headers (CSP, HSTS-like), and auto-idle timeout (2 hours).
- **UI Framework**: Jetpack Compose (BOM).
- **Security**: 
    - **Encryption**: SQLCipher (256-bit AES) via `SupportOpenHelperFactory`.
    - **Key Management**: `SecurePassphraseManager` generates a 32-byte key (`SecureRandom`) stored in `SharedPreferences` (`secure_prefs`).
- **Initialization**: `OssWalletApp` handles `osmdroid` config, notification channels, `PlannedPaymentWorker` scheduling, and `WidgetInvalidationObserver` startup.

---

## 2. Data Layer

### 2.1 Database (v27)
- **Entities**: 16 core entities including `TransactionEntity`, `AccountEntity`, `FilterEntity`, `PlannedPaymentEntity`, `BudgetEntity`, `LoanEntity`, and `TemplateEntity`.
- **FTS Search (FTS4)**: 
    - **Status**: Currently implemented as **FTS4** (as of Migration 26->27) to maintain compatibility with Room's `@Fts4` and avoid `docid` mismatch issues found in FTS5.
    - **Sync**: Manual SQLite triggers (`room_fts_content_sync_transactions_fts_AFTER_...`) handle real-time indexing of `note`, `payee`, and `name` from the `transactions` table.
- **Migrations**: 
    - **19->20**: Composite PK on `currency_rates` (currencyCode, baseCurrency) for multi-anchor support.
    - **21->26**: Progressive attempts at FTS5; **27** reverted to stable FTS4 using `docid`.
- **Profile Isolation**: 
    - Most DAOs (`TransactionDao`, `AccountDao`, etc.) enforce `profileId` scoping. `CategoryEntity` and `TagEntity` (system-defined) are shared/enforced via `RoomDatabase.Callback`.
- **Custom Query Logic**: `TransactionQueryBuilder` generates dynamic SQL for aggregations. It uses `CAST(FUNC(CAST(col AS REAL)) AS TEXT)` for aggregate metrics to prevent floating-point rounding errors.

### 2.2 Currency Subsystem
- **Math**: `BigDecimal` with 10 decimal places and `HALF_UP` rounding. `Double` is forbidden.
- **Snapshot**: `ExchangeRateSnapshot` encapsulates an `anchor` currency and its rates.
- **Re-basing**: Logic in `CurrencyService` ensures data integrity. Conversion uses an `intermediatePrecision` of 10.
- **Formatting**: `CurrencyService` uses an atomic `FormatConfig` (`AtomicReference`) for thread-safe consistent precision/system across UI and background tasks.

---

## 3. UI & Design System ("H — Editorial Instrument")

### 3.1 Visual Identity
- **Palettes**: Warm-black/ivory/champagne (dark) and ivory/ink/bronze (light). Supported via `UiIdentity.H` in `Theme.kt`. `dynamicAccent` overrides `primary` but preserves H neutrals.
- **Typography**: 
    - **Serif**: Fraunces (amounts/headings/hero-text).
    - **Sans**: Inter (body/labels).
    - **Mono**: JetBrains Mono (micro-readouts/tabular numbers/meta-labels).
    - **Implementation**: Variable fonts with `wght` axis mapping (SDK 26+). `AppTextStyles` provides a single semantic entry point.
- **Glass Policy**: Selective glassmorphism (via **Haze**). Cards are **opaque hairline panels** (`aeonHairlineBorder`). Glass is reserved for floating chrome: `AeonTopAppBar` (pills), `TransactionFilterPill`, and navigation sheets.

### 3.2 Dashboard & Card Architecture
- **Framework**: "Phase 2" engine using `CardInput` (God object) and `CardDataComputer` strategy.
- **Optimization**: `CardAggregationService` performs bucket-based computation. `computeBudgetRollover` uses parallel coroutines to resolve up to 12 past periods in a single DB batch.
- **Interaction**:
    - **AeonExpressiveCard**: Supports a 3D flip animation (180° rotation) to reveal `AeonExpressiveCardConfig` on the back.
    - **Floating Header**: `AeonTopAppBar` uses a modular pill-based layout (left, center, right slots). Support for integrated search and tab-based sub-navigation pills.
    - **Filters**: `TransactionFilter` with set-based filtering. **Empty set semantics**: An empty set denotes "No Constraint" (matches all).
- **Caches**: `QueryCache` and `CardRenderCache` store computed results; invalidated process-wide on profile switch (`OssWalletApp`).

---

## 4. Feature Implementation Details

### 4.1 Web UI (Open in Web)
- **Tech Stack**: Ktor CIO + `kotlinx.serialization`.
- **Auth**: Passphrase-based access code + Session tokens. QR codes generated via `QRCodeGenerator`.
- **Persistence**: Runs as a Foreground Service (`WebServerService`) with a 2-hour idle timeout tracker plugin.
- **Endpoints**: `WebApiRoutes` exposes transactions, accounts, categories, and card computations.

### 4.2 Search & Discovery
- **Unified Search**: Combines FTS prefix matching (`term*`) with `LIKE %term%` fallbacks for categories.
- **Regex Search**: Post-query in-memory filtering via `applyRegexFilter` in `TransactionRepositoryImpl` supports `toSmartRegex()`.

---

## 5. Nitty-Gritty & Rules
1. **Never use `Double`** for currency math. Always `BigDecimal`.
2. **Never pass unanchored rates**; always use `ExchangeRateSnapshot`.
3. **Always scope DAOs** with `profileId` unless the entity is global (Categories, Tags).
4. **Use `AppTextStyles`** for all Compose text; avoid direct `MaterialTheme.typography` access.
5. **FTS is currently FTS4** — use `docid` for triggers and Room compatibility.
6. **Cards are Opaque** — do not apply glassmorphism to `AeonExpressiveCard` content.






---
