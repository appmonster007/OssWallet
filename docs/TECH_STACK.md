# Technical Stack & Conventions — OssWallet

## 1. Core Development Toolkit

| Category | Technology | Version | Usage |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | `2.2.10` | Modern expressive syntax with strict null-safety. |
| **UI Framework**| Jetpack Compose | `2026.04.01` (BOM) | Declarative UI for high-performance rendering. |
| **DI** | Hilt (Dagger) | `2.59.2` | Standard dependency injection for Android. |
| **Async** | Coroutines | `1.10.2` | Structured concurrency for non-blocking IO. |
| **Serialization**| Kotlinx Serialization| `1.8.0` | Type-safe JSON parsing for Ktor and DataStore. |

---

## 2. Persistence & Security

### 2.1 Room & SQLCipher
- **Room Persistence**: version `2.8.4`
- **Encryption**: **SQLCipher** `4.6.1`
    - **Implementation**: Uses `SupportOpenHelperFactory` with a 256-bit AES passphrase.
    - **Key Management**: Passphrase is generated as a 32-byte secure random array and persisted in encrypted `SharedPreferences`.
    - **Full Text Search**: Uses **FTS4** via SQLite's `USING fts4` module for indexing transaction notes and payees.

### 2.2 DataStore
- **Version**: `1.1.7`
- **Usage**: Manages "Settings" (Currency, Theme, Precision) and "WebServerPreferences". 
- **Pattern**: Exposes cold `Flow` streams that are transformed into hot `StateFlow` in the ViewModel.

---

## 3. Networking & Web Server

### 3.1 Retrofit (Exchange Rates)
- **Version**: `2.12.0`
- **Interceptor**: `LoggingInterceptor` `4.10.0` for debug-level network transparency.
- **Provider**: Frankfurter and ExchangeRate-API fallback logic.

### 3.2 Ktor Embedded Server (Companion)
- **Engine**: **CIO** (Coroutine-based IO) version `3.1.3`.
- **Features**:
    - `ContentNegotiation` with Kotlinx Serialization.
    - `Compression` (Gzip) enabled for JSON payloads.
    - `DefaultHeaders` (CSP, X-Content-Type, X-Frame-Options) for security.
    - `CORS` enabled for secure cross-origin desktop access.

---

## 4. UI & Graphics

### 4.1 Vico Charts
- **Version**: `2.0.0-beta.3`
- **Implementation**: Used for Cashflow (Bar charts) and Net Worth (Line charts).
- **Customization**: Uses `OssWalletTheme` colors to dynamically theme chart lines and markers based on the user's selected UI Identity.

### 4.2 Haze (Glassmorphism)
- **Version**: `1.7.2`
- **Usage**: Provides real-time blur and tint for top bars and navigation pills.
- **Performance**: Carefully applied using `LocalGlassmorphismEnabled` to allow users on lower-end devices to disable effects.
- **Glass Policy**: Glassmorphism is reserved for floating chrome (e.g., `AeonTopAppBar` pills, `TransactionFilterPill`). Main content cards are **opaque hairline panels** to maintain readability.

---

## 5. Design System ("H — Editorial Instrument")

### 5.1 Typography
OssWallet uses variable fonts (SDK 26+) for a premium editorial feel:
- **Serif**: **Fraunces** (used for amounts, hero headings, and large displays).
- **Sans**: **Inter** (used for standard body text, labels, and secondary info).
- **Mono**: **JetBrains Mono** (used for micro-readouts, meta-labels, and tabular data).

### 5.2 Visual Identity
- **Palettes**: Implements `UiIdentity.H` with two primary modes:
    - **Dark**: Warm-black, ivory, and champagne neutrals.
    - **Light**: Ivory, ink, and bronze neutrals.
- **Interaction**: `AeonExpressiveCard` supports a 3D flip animation (180° rotation) to reveal configuration options on the back.

---

## 5. Engineering Standards

- **Precision**: `CurrencyService` uses `RoundingMode.HALF_UP` and a scale of 10 for intermediate sums to prevent floating-point errors.
- **UI Architecture**: Every screen must have a dedicated `ViewModel` extending `BaseViewModel`. Direct repo access from Composables is strictly prohibited.
- **Resource Naming**: 
    - Icons: PascalCase (e.g., `AccountBalance`).
    - Routes: PascalCase with `Route` suffix (e.g., `DashboardRoute`).
